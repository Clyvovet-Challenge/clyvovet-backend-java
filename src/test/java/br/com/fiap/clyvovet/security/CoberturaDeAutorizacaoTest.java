package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.support.TesteDeApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Todo endpoint que ESCREVE precisa decidir de quem e o registro.
 *
 * As oito falhas de autorizacao das duas revisoes tiveram a mesma forma: alguem
 * acrescentou uma rota, a regra de rota respondeu "qual perfil entra aqui", e
 * ninguem respondeu "de quem e este registro". A decisao mora em tres lugares
 * -- 32 regras de rota, as anotacoes @PreAuthorize e as checagens dentro dos
 * services -- e nenhum deles obriga os outros.
 *
 * Este teste nao move a decisao de lugar. Ele torna o ESQUECIMENTO impossivel:
 * um metodo POST, PUT, PATCH ou DELETE ou carrega @PreAuthorize, ou esta na
 * lista abaixo com o motivo escrito. Rota nova sem uma das duas coisas quebra
 * o build, e nao a proxima revisao de seguranca.
 *
 * A lista nao e uma excecao: e o registro de uma decisao. Se o motivo nao
 * couber numa linha, provavelmente a rota precisa mesmo da anotacao.
 */
class CoberturaDeAutorizacaoTest extends TesteDeApi {

    /**
     * Endpoints de escrita que passam sem @PreAuthorize, com o porque.
     * Chave: "NomeDoController#metodo".
     */
    private static final Map<String, String> DECIDIDOS_FORA_DA_ANOTACAO = Map.ofEntries(
            // --- Publicos por natureza ---
            Map.entry("AuthController#login", "publico: e o que emite o token"),
            Map.entry("AuthController#refresh", "publico: valida o refresh token no corpo"),
            Map.entry("AuthController#logout", "publico: revoga o token do proprio corpo"),
            Map.entry("AuthController#registrar", "publico: auto-cadastro, sempre cria TUTOR"),

            // --- Recurso da plataforma, fechado por ROLE na regra de rota ---
            // Nao ha "dono" a verificar: entrar e sair da plataforma e decisao do
            // negocio, e a rota ja exige ADMIN.
            //
            // A lista era MAIOR aqui. Nove entradas sairam quando o ADMIN_CLINICA
            // passou a existir: editar clinica, cadastrar e editar veterinario e
            // mexer no catalogo deixaram de ser "ADMIN na regra de rota" e ganharam
            // @PreAuthorize, porque a rota passou a admitir dois perfis e nao sabe
            // de QUAL clinica e o recurso. As entradas continuaram aqui dizendo o
            // contrario, e o teste as aceitava.
            Map.entry("AuthController#criarUsuario", "ADMIN na regra de rota"),
            Map.entry("ClinicaController#criar", "ADMIN na regra de rota"),
            Map.entry("ClinicaController#deletar", "ADMIN na regra de rota"),

            // --- Corpo clinico, sem dono por recurso ---
            Map.entry("TutorController#criar", "cadastro novo: nao ha dono anterior a verificar"),
            Map.entry("TutorController#deletar", "VETERINARIO/ADMIN na rota; FK barra tutor com animal"),

            // --- Decidido DENTRO do service ---
            // Aqui a checagem existe, so nao cabe numa anotacao: ou depende de
            // uma entidade que o controller nao tem, ou vale por campo.
            Map.entry("AgendaController#criarFaixa", "service: garantirQueEDonoDaAgenda"),
            Map.entry("AgendaController#removerFaixa", "service: garantirQueEDonoDaAgenda"),
            Map.entry("AgendaController#criarBloqueio", "service: garantirQueEDonoDaAgenda"),
            Map.entry("AgendaController#removerBloqueio", "service: garantirQueEDonoDaAgenda"),
            Map.entry("HistoricoController#desativarAlerta", "service: garantirQuePodeDesativar"),
            Map.entry("HistoricoController#revogar", "service: so o tutor dono da autorizacao"),
            Map.entry("HistoricoController#emergencia", "quebra de vidro: rota limita a VETERINARIO, service audita"),
            Map.entry("EventoClinicoController#criar", "service: garantirQueEDaPropriaClinica"),
            Map.entry("RetornoController#marcarFaltas", "varredura da propria clinica, sem alvo individual")
    );

    private static final Set<RequestMethod> ESCRITA = Set.of(
            RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE);

    // Qualificado: o Actuator registra um segundo bean deste tipo.
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mapeamento;

    @Test
    @DisplayName("todo endpoint de escrita decide de quem é o registro")
    void todoEndpointDeEscritaDecideODono() {
        List<String> semDecisao = new ArrayList<>();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> rota : mapeamento.getHandlerMethods().entrySet()) {
            HandlerMethod handler = rota.getValue();
            String classe = handler.getBeanType().getSimpleName();

            if (!handler.getBeanType().getPackageName().startsWith("br.com.fiap.clyvovet.controller")) {
                continue;
            }
            if (!escreve(rota.getKey())) {
                continue;
            }
            if (handler.getMethodAnnotation(PreAuthorize.class) != null
                    || handler.getBeanType().getAnnotation(PreAuthorize.class) != null) {
                continue;
            }

            String chave = classe + "#" + handler.getMethod().getName();
            if (!DECIDIDOS_FORA_DA_ANOTACAO.containsKey(chave)) {
                semDecisao.add(chave);
            }
        }

        assertThat(semDecisao)
                .as("Endpoints de escrita sem @PreAuthorize e sem motivo registrado. "
                        + "Acrescente a anotacao, ou entre na lista de "
                        + "CoberturaDeAutorizacaoTest dizendo onde a decisao e tomada")
                .isEmpty();
    }

    /**
     * A lista precisa envelhecer junto com o codigo: entrada que sobra vira
     * permissao esquecida para uma rota que nao existe mais.
     */
    @Test
    @DisplayName("a lista não guarda entrada morta")
    void listaSemEntradaMorta() {
        List<String> existentes = new ArrayList<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> rota : mapeamento.getHandlerMethods().entrySet()) {
            HandlerMethod handler = rota.getValue();
            if (handler.getBeanType().getPackageName().startsWith("br.com.fiap.clyvovet.controller")) {
                existentes.add(handler.getBeanType().getSimpleName() + "#" + handler.getMethod().getName());
            }
        }

        assertThat(DECIDIDOS_FORA_DA_ANOTACAO.keySet())
                .as("entradas que nao correspondem a nenhum endpoint")
                .isSubsetOf(existentes);
    }

    /**
     * Entrada na lista para um endpoint que JA tem @PreAuthorize e pior que
     * redundante.
     *
     * <p>Ela e uma isencao permanente: no dia em que alguem apagar a anotacao, o
     * primeiro teste desta classe continua verde, porque a chave esta na lista. O
     * guarda deixa de guardar exatamente o endpoint que mais mudou de mao.</p>
     *
     * <p>Nao e hipotese. Nove entradas ficaram assim quando o ADMIN_CLINICA passou a
     * existir, todas dizendo "ADMIN na regra de rota" sobre rotas que passaram a
     * aceitar dois perfis — e nada apontou para elas.</p>
     */
    @Test
    @DisplayName("a lista não isenta quem já decide pela anotação")
    void listaSemEntradaRedundante() {
        List<String> jaDecidemPelaAnotacao = new ArrayList<>();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> rota : mapeamento.getHandlerMethods().entrySet()) {
            HandlerMethod handler = rota.getValue();
            if (!handler.getBeanType().getPackageName().startsWith("br.com.fiap.clyvovet.controller")) {
                continue;
            }
            boolean anotado = handler.getMethodAnnotation(PreAuthorize.class) != null
                    || handler.getBeanType().getAnnotation(PreAuthorize.class) != null;
            String chave = handler.getBeanType().getSimpleName() + "#" + handler.getMethod().getName();
            if (anotado && DECIDIDOS_FORA_DA_ANOTACAO.containsKey(chave)) {
                jaDecidemPelaAnotacao.add(chave);
            }
        }

        assertThat(jaDecidemPelaAnotacao)
                .as("estes endpoints tem @PreAuthorize e continuam na lista de excecoes: "
                        + "tire-os de lá, senao a lista os isenta caso a anotacao seja removida")
                .isEmpty();
    }

    private boolean escreve(RequestMappingInfo info) {
        return info.getMethodsCondition().getMethods().stream().anyMatch(ESCRITA::contains);
    }
}
