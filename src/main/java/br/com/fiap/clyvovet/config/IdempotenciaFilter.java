package br.com.fiap.clyvovet.config;

import br.com.fiap.clyvovet.dto.exception.ErroValidacao;
import br.com.fiap.clyvovet.security.SegurancaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Header {@code Idempotency-Key} nos POST que criam cobranca e agendamento.
 *
 * <p>O app nao repete pedido sozinho, mas a pessoa repete: a API na Render dorme,
 * o celular desiste por timeout, o servidor termina de gravar -- e o segundo
 * toque criava uma segunda cobranca PENDENTE. Com a chave, o segundo toque
 * recebe a resposta do primeiro e nada e executado de novo.</p>
 *
 * <ul>
 *   <li>A chave e gravada ANTES de executar. Quem decide entre dois pedidos
 *       simultaneos e a chave primaria (usuario, chave) do banco, nao um SELECT.</li>
 *   <li>Mesma chave, mesmo corpo, ja respondido: devolve a resposta guardada.</li>
 *   <li>Mesma chave, ainda em processamento: 409.</li>
 *   <li>Mesma chave, outro corpo: 422 -- e erro de quem chama.</li>
 *   <li>So respostas 2xx ficam guardadas. Um 4xx ou 5xx apaga a chave, para que
 *       a nova tentativa execute de novo: um "horario ocupado" de agora pode nao
 *       valer daqui a um minuto.</li>
 *   <li>Sem o header, nada muda: Swagger, tela web e clientes antigos seguem
 *       como antes.</li>
 * </ul>
 *
 * <p>Roda depois da cadeia do Spring Security (filtro sem {@code @Order}): a
 * chave e por usuario, e o usuario so e conhecido depois da autenticacao.</p>
 */
@Component
@RequiredArgsConstructor
public class IdempotenciaFilter extends OncePerRequestFilter {

    public static final String HEADER = "Idempotency-Key";

    private static final Set<String> ROTAS = Set.of("/api/v1/pagamentos", "/api/v1/agendamentos");

    private final JdbcTemplate jdbc;
    private final SegurancaService seguranca;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest requisicao) {
        return !"POST".equals(requisicao.getMethod())
                || requisicao.getHeader(HEADER) == null
                || !ROTAS.contains(rotaDe(requisicao));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao,
                                    HttpServletResponse resposta,
                                    FilterChain corrente) throws ServletException, IOException {
        UUID usuarioId = seguranca.usuarioAutenticadoId();
        if (usuarioId == null) {
            // Sem usuario a seguranca ja recusou, ou a rota e publica; nao ha
            // a quem vincular a chave.
            corrente.doFilter(requisicao, resposta);
            return;
        }
        String chave = requisicao.getHeader(HEADER).trim();
        if (!ehUuid(chave)) {
            erro(resposta, HttpServletResponse.SC_BAD_REQUEST, "O Idempotency-Key precisa ser um UUID.");
            return;
        }

        CorpoRelido relido = new CorpoRelido(requisicao);
        String rota = rotaDe(requisicao);
        String hash = sha256(relido.corpo);

        try {
            jdbc.update("""
                    INSERT INTO t_clyvo_idempotencia (usuario_id, chave, rota, hash_corpo)
                    VALUES (?, ?, ?, ?)""", usuarioId.toString(), chave, rota, hash);
        } catch (DuplicateKeyException repetida) {
            responderRepeticao(resposta, usuarioId, chave, rota, hash);
            return;
        }

        ContentCachingResponseWrapper guardada = new ContentCachingResponseWrapper(resposta);
        boolean guardou = false;
        try {
            corrente.doFilter(relido, guardada);
            int status = guardada.getStatus();
            if (status >= 200 && status < 300) {
                jdbc.update("""
                        UPDATE t_clyvo_idempotencia SET status_http = ?, corpo = ?
                        WHERE usuario_id = ? AND chave = ?""",
                        status, new String(guardada.getContentAsByteArray(), StandardCharsets.UTF_8),
                        usuarioId.toString(), chave);
                guardou = true;
            }
        } finally {
            if (!guardou) {
                jdbc.update("DELETE FROM t_clyvo_idempotencia WHERE usuario_id = ? AND chave = ?",
                        usuarioId.toString(), chave);
            }
            guardada.copyBodyToResponse();
        }
    }

    private void responderRepeticao(HttpServletResponse resposta, UUID usuarioId, String chave,
                                    String rota, String hash) throws IOException {
        List<Map<String, Object>> linhas = jdbc.queryForList("""
                SELECT rota, hash_corpo, status_http, corpo FROM t_clyvo_idempotencia
                WHERE usuario_id = ? AND chave = ?""", usuarioId.toString(), chave);
        if (linhas.isEmpty()) {
            // O primeiro falhou e apagou a chave entre o INSERT e esta leitura.
            erro(resposta, HttpServletResponse.SC_CONFLICT,
                    "O pedido anterior com esta chave falhou. Tente de novo.");
            return;
        }
        Map<String, Object> linha = linhas.get(0);
        if (!rota.equals(linha.get("rota")) || !hash.equals(linha.get("hash_corpo"))) {
            erro(resposta, 422, "Este Idempotency-Key já foi usado com outro pedido.");
            return;
        }
        Object status = linha.get("status_http");
        if (status == null) {
            erro(resposta, HttpServletResponse.SC_CONFLICT,
                    "Um pedido com esta chave ainda está sendo processado.");
            return;
        }
        resposta.setStatus(((Number) status).intValue());
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resposta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resposta.setHeader("Idempotent-Replayed", "true");
        Object corpo = linha.get("corpo");
        resposta.getWriter().write(corpo instanceof java.sql.Clob clob ? lerClob(clob) : String.valueOf(corpo));
    }

    private void erro(HttpServletResponse resposta, int status, String mensagem) throws IOException {
        resposta.setStatus(status);
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resposta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(resposta.getWriter(), new ErroValidacao(HEADER, mensagem));
    }

    private static String rotaDe(HttpServletRequest requisicao) {
        return requisicao.getRequestURI().substring(requisicao.getContextPath().length());
    }

    private static boolean ehUuid(String valor) {
        try {
            return UUID.fromString(valor).toString().equalsIgnoreCase(valor);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String sha256(byte[] dados) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(dados));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String lerClob(java.sql.Clob clob) throws IOException {
        try {
            return clob.getSubString(1, (int) clob.length());
        } catch (java.sql.SQLException e) {
            throw new IOException(e);
        }
    }

    /** O corpo lido uma vez para o hash, e servido de novo ao controller. */
    private static final class CorpoRelido extends HttpServletRequestWrapper {

        private final byte[] corpo;

        CorpoRelido(HttpServletRequest requisicao) throws IOException {
            super(requisicao);
            this.corpo = requisicao.getInputStream().readAllBytes();
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bytes = new ByteArrayInputStream(corpo);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return bytes.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int read() {
                    return bytes.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
