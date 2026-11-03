package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.exception.RegraDeNegocioException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * As regras sobre o arquivo anexado ao prontuario: tamanho, formato, titulo e
 * nome. Funcoes puras, sem banco e sem usuario.
 *
 * <p>Moravam no {@code DocumentoClinicoService}, misturadas com o fluxo e a
 * autorizacao. Separadas, cada uma se le e se testa sozinha.</p>
 */
final class ValidadorDeArquivo {

    /**
     * Teto por arquivo. Laudo em PDF e foto de exame cabem; video de ultrassom
     * nao, e nao deve caber — e o que mantem honesta a decisao de guardar o byte
     * no banco (ver o cabecalho da V17).
     *
     * <p>O limite do Spring em {@code comum.properties} e maior de proposito:
     * ele existe para o absurdo, e este aqui para o usuario. Se fossem iguais, o
     * arquivo de 8,1 MB morreria no parser do multipart e a pessoa leria um erro
     * de infraestrutura em vez de "o arquivo passa de 8 MB".</p>
     */
    static final long TAMANHO_MAXIMO = 8L * 1024 * 1024;

    /** O que a coluna {@code titulo} da V17 aceita. */
    private static final int TAMANHO_DO_TITULO = 150;

    private ValidadorDeArquivo() {
    }

    static byte[] bytesDe(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraDeNegocioException("arquivo", "Envie um arquivo.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new RegraDeNegocioException("arquivo",
                    "O arquivo passa de " + (TAMANHO_MAXIMO / 1024 / 1024) + " MB.");
        }
        try {
            return arquivo.getBytes();
        } catch (IOException e) {
            throw new RegraDeNegocioException("arquivo", "Nao foi possivel ler o arquivo enviado.");
        }
    }

    /**
     * O tipo lido dos primeiros bytes do arquivo — a assinatura do formato.
     *
     * <p>O {@code Content-Type} da requisicao <b>nao participa</b> da decisao.
     * Ele e escolhido por quem envia, e um cliente que quisesse gravar um
     * executavel no banco mandaria {@code application/pdf} com prazer. A
     * assinatura de PDF, JPEG e PNG e curta, estavel desde os anos 90 e nao
     * depende do nome do arquivo nem da boa-fe do cliente.</p>
     *
     * <p>O tipo devolvido aqui e o que vai para a coluna, e a coluna tem CHECK
     * com esta mesma lista (V17): duas travas, uma na aplicacao e uma no
     * banco.</p>
     */
    static String tipoRealDe(byte[] conteudo) {
        if (comeca(conteudo, new byte[]{'%', 'P', 'D', 'F'})) {
            return "application/pdf";
        }
        if (comeca(conteudo, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})) {
            return "image/jpeg";
        }
        if (comeca(conteudo, new byte[]{(byte) 0x89, 'P', 'N', 'G'})) {
            return "image/png";
        }
        throw new RegraDeNegocioException("arquivo", "Formato nao aceito. Envie PDF, JPG ou PNG.");
    }

    static String sha256De(byte[] conteudo) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(conteudo));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 e obrigatorio em toda JVM; se faltar, o ambiente esta quebrado.
            throw new IllegalStateException("SHA-256 indisponivel nesta JVM", e);
        }
    }

    static String tituloValido(String titulo) {
        String limpo = titulo == null ? "" : titulo.trim();
        if (limpo.isEmpty()) {
            throw new RegraDeNegocioException("titulo", "De um nome ao documento.");
        }
        // Truncar em silencio guardaria um titulo cortado no meio da palavra;
        // recusar diz a pessoa o que fazer.
        if (limpo.length() > TAMANHO_DO_TITULO) {
            throw new RegraDeNegocioException("titulo",
                    "O nome do documento passa de " + TAMANHO_DO_TITULO + " caracteres.");
        }
        return limpo;
    }

    /**
     * O nome do arquivo, sem o que ele pode carregar de perigoso.
     *
     * <p>Este texto volta no {@code Content-Disposition} do download e vira nome
     * de arquivo no telefone de quem baixa. A barra e o {@code ..} viriam de um
     * cliente que quisesse escrever fora da pasta de destino; a quebra de linha
     * permitiria injetar um segundo cabecalho na resposta HTTP.</p>
     */
    static String nomeSeguro(String original, String tipo) {
        String base = original == null ? "" : original.trim();
        base = base.replaceAll("[\\r\\n\"\\\\/]", "").replace("..", "");

        if (base.isEmpty()) {
            base = "documento" + extensaoDe(tipo);
        }
        return base.length() > 255 ? base.substring(base.length() - 255) : base;
    }

    private static boolean comeca(byte[] conteudo, byte[] assinatura) {
        if (conteudo.length < assinatura.length) {
            return false;
        }
        for (int i = 0; i < assinatura.length; i++) {
            if (conteudo[i] != assinatura[i]) {
                return false;
            }
        }
        return true;
    }

    private static String extensaoDe(String tipo) {
        return switch (tipo) {
            case "application/pdf" -> ".pdf";
            case "image/png" -> ".png";
            default -> ".jpg";
        };
    }
}
