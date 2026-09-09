package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O ADMIN_CLINICA — o administrador de UMA clinica.
 *
 * <p>Ate a V12 ele nao existia: {@code t_clyvo_usuario} ligava a um tutor ou a um
 * veterinario, e a nada mais. O escopo por clinica era transitivo (usuario →
 * veterinario → clinica), e quem gerenciava servicos, profissionais e agenda era o
 * ADMIN da plataforma, que enxerga todas as clinicas.</p>
 *
 * <p>O risco do perfil novo cabe numa frase: <b>ele abre verbos que antes eram so do
 * ADMIN</b>. A regra de rota nao sabe de qual clinica e o recurso — quem sabe e o
 * {@code @PreAuthorize} do controller. Esta classe existe para provar que a segunda
 * metade esta no lugar, porque sem ela o perfil vira um caminho para mexer na
 * concorrente.</p>
 */
class AdminDaClinicaTest extends TesteDeApi {

    private static final String DA_VETCARE = "admin.vetcare@clinica.test";
    private static final String DA_PETMED = "admin.petmed@clinica.test";

    private String vetcare;
    private String petmed;

    @BeforeEach
    void criarOsDoisAdministradores() throws Exception {
        vetcare = tokenAdminDaClinica(DA_VETCARE, SeedV2.CLINICA_VETCARE);
        petmed = tokenAdminDaClinica(DA_PETMED, SeedV2.CLINICA_PETMED);
    }

    // ================================================================
    // O vinculo, no cadastro
    // ================================================================

    @Test
    @DisplayName("ADMIN_CLINICA sem clinica e recusado")
    void semClinicaERecusado() throws Exception {
        criar("/api/v1/auth/usuarios", tokenAdmin(), """
                {"email":"sem.clinica@test.com","senha":"%s","perfil":"ADMIN_CLINICA"}"""
                .formatted(SENHA_ADMIN_DE_CLINICA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.campo").value("clinicaId"));
    }

    /**
     * O buraco que a validacao antiga escondia.
     *
     * <p>Ela era escrita aos pares — "TUTOR sem tutor", "TUTOR com veterinario" — e
     * nunca verificava o ADMIN. Nada impedia cria-lo apontando para um tutor, e a
     * partir dai {@code UsuarioAutenticado.getTutorId()} devolveria esse id: um
     * ADMIN que, em qualquer regra que pergunte "voce e dono disto?", responderia
     * pelo tutor alheio.</p>
     */
    @Test
    @DisplayName("ADMIN da plataforma nao pode nascer vinculado a um tutor")
    void adminNaoAceitaVinculoDeTutor() throws Exception {
        criar("/api/v1/auth/usuarios", tokenAdmin(), """
                {"email":"admin.com.tutor@test.com","senha":"%s","perfil":"ADMIN","tutorId":"%s"}"""
                .formatted(SENHA_ADMIN_DE_CLINICA, SeedV2.TUTOR_LUCAS))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.campo").value("tutorId"));
    }

    @Test
    @DisplayName("ADMIN_CLINICA nao pode nascer vinculado a um veterinario")
    void adminDeClinicaNaoAceitaVinculoDeVeterinario() throws Exception {
        criar("/api/v1/auth/usuarios", tokenAdmin(), """
                {"email":"hibrido@test.com","senha":"%s","perfil":"ADMIN_CLINICA",
                 "clinicaId":"%s","veterinarioId":"%s"}"""
                .formatted(SENHA_ADMIN_DE_CLINICA, SeedV2.CLINICA_VETCARE, SeedV2.VET_CAMILA))
                .andExpect(status().isConflict());
    }

    // ================================================================
    // A propria clinica, e so ela
    // ================================================================

    @Test
    @DisplayName("edita a propria clinica")
    void editaAPropriaClinica() throws Exception {
        atualizarParcialmente("/api/v1/clinicas/" + SeedV2.CLINICA_VETCARE, vetcare, """
                {"telefone":"1133224455"}""")
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("NAO edita a clinica concorrente")
    void naoEditaAConcorrente() throws Exception {
        atualizarParcialmente("/api/v1/clinicas/" + SeedV2.CLINICA_PETMED, vetcare, """
                {"telefone":"1199998888"}""")
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("nao cria nem exclui clinica -- quem entra e sai da plataforma e o ADMIN")
    void naoCriaNemExcluiClinica() throws Exception {
        criar("/api/v1/clinicas", vetcare, """
                {"nome":"Clinica Nova","cnpj":"11222333000181","telefone":"1133334444",
                 "email":"nova@clinica.com","endereco":{"logradouro":"Rua A","numero":"1",
                 "bairro":"Centro","cidade":"Sao Paulo","estado":"SP","cep":"01001000"}}""")
                .andExpect(status().isForbidden());

        remover("/api/v1/clinicas/" + SeedV2.CLINICA_VETCARE, vetcare)
                .andExpect(status().isForbidden());
    }

    // ================================================================
    // Catalogo de servicos
    // ================================================================

    @Test
    @DisplayName("cadastra servico na propria clinica")
    void cadastraServicoNaPropria() throws Exception {
        String id = idDe(criar("/api/v1/servicos", vetcare, """
                {"clinicaId":"%s","nome":"Banho terapeutico %s","tipoEvento":"OUTRO",
                 "preco":90.00,"duracaoMinutos":45}"""
                .formatted(SeedV2.CLINICA_VETCARE, System.nanoTime()))
                .andExpect(status().isCreated()));
        removerDepois("/api/v1/servicos/" + id);
    }

    /**
     * O caso que a regra de rota sozinha nao pega: o verbo esta liberado para o
     * perfil, e o corpo aponta para outra clinica.
     */
    @Test
    @DisplayName("NAO cadastra servico no catalogo da concorrente")
    void naoCadastraServicoNaConcorrente() throws Exception {
        criar("/api/v1/servicos", vetcare, """
                {"clinicaId":"%s","nome":"Servico intruso %s","tipoEvento":"CONSULTA",
                 "preco":10.00,"duracaoMinutos":30}"""
                .formatted(SeedV2.CLINICA_PETMED, System.nanoTime()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("NAO altera nem apaga servico da concorrente")
    void naoMexeEmServicoAlheio() throws Exception {
        String doPetmed = idDe(criar("/api/v1/servicos", petmed, """
                {"clinicaId":"%s","nome":"Consulta PetMed %s","tipoEvento":"CONSULTA",
                 "preco":120.00,"duracaoMinutos":30}"""
                .formatted(SeedV2.CLINICA_PETMED, System.nanoTime()))
                .andExpect(status().isCreated()));
        removerDepois("/api/v1/servicos/" + doPetmed);

        atualizar("/api/v1/servicos/" + doPetmed, vetcare, """
                {"clinicaId":"%s","nome":"Sequestrado","tipoEvento":"CONSULTA",
                 "preco":1.00,"duracaoMinutos":30}""".formatted(SeedV2.CLINICA_PETMED))
                .andExpect(status().isForbidden());

        remover("/api/v1/servicos/" + doPetmed, vetcare)
                .andExpect(status().isForbidden());
    }

    /**
     * Desativar deixou de ser sem volta.
     *
     * <p>{@code ServicoRequest} nao tem o campo {@code ativo}, entao nem o PUT trazia
     * o servico de volta — e a listagem escondia o desativado, o que tirava da tela o
     * que receberia o clique. A clinica que parasse de oferecer banho e voltasse atras
     * precisava cadastrar OUTRO servico, com outro id, partindo em dois o historico de
     * preco de tudo que ja tinha sido cobrado.</p>
     */
    @Test
    @DisplayName("desativa, ve no catalogo de gestao, e reativa")
    void desativaEReativa() throws Exception {
        String id = idDe(criar("/api/v1/servicos", vetcare, """
                {"clinicaId":"%s","nome":"Banho e tosa %s","tipoEvento":"OUTRO",
                 "preco":70.00,"duracaoMinutos":60}"""
                .formatted(SeedV2.CLINICA_VETCARE, System.nanoTime()))
                .andExpect(status().isCreated()));
        removerDepois("/api/v1/servicos/" + id);

        remover("/api/v1/servicos/" + id, vetcare).andExpect(status().isNoContent());
        assertThat(contem(catalogo(vetcare, false), id)).isFalse();
        assertThat(contem(catalogo(vetcare, true), id)).isTrue();

        criar("/api/v1/servicos/" + id + "/reativar", vetcare, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));
        assertThat(contem(catalogo(vetcare, false), id)).isTrue();
    }

    /** O catalogo de gestao mostra preco e o que foi tirado do ar: nao e do tutor. */
    @Test
    @DisplayName("o catalogo com desativados nao e de quem nao administra")
    void catalogoDeGestaoEDeQuemAdministra() throws Exception {
        buscar("/api/v1/clinicas/" + SeedV2.CLINICA_VETCARE + "/servicos?incluirInativos=true",
                tokenTutor(LUCAS))
                .andExpect(status().isForbidden());
        // Na concorrente, nem o administrador de clinica passa.
        buscar("/api/v1/clinicas/" + SeedV2.CLINICA_PETMED + "/servicos?incluirInativos=true", vetcare)
                .andExpect(status().isForbidden());
        // E o catalogo normal continua aberto a quem vai agendar.
        buscar("/api/v1/clinicas/" + SeedV2.CLINICA_VETCARE + "/servicos", tokenTutor(LUCAS))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("o tutor nao reativa servico nenhum")
    void tutorNaoReativa() throws Exception {
        criar("/api/v1/servicos/" + SeedV2.ID_INEXISTENTE + "/reativar", tokenTutor(LUCAS), "")
                .andExpect(status().isForbidden());
    }

    // ================================================================
    // Agenda dos profissionais da casa
    // ================================================================

    /**
     * "A minha equipe" so existe com o filtro por clinica.
     *
     * <p>Sem ele a tela teria de puxar a plataforma inteira, pagina a pagina, e
     * filtrar no cliente — e mostrar o numero errado enquanto nao terminasse.</p>
     */
    @Test
    @DisplayName("lista os profissionais da casa, e so eles")
    void listaAEquipeDaCasa() throws Exception {
        JsonNode equipe = corpoDe(buscar(
                "/api/v1/veterinarios?clinicaId=" + SeedV2.CLINICA_VETCARE + "&size=50", vetcare)
                .andExpect(status().isOk()));

        assertThat(totalDe(equipe)).isPositive();
        for (JsonNode profissional : equipe.get("content")) {
            assertThat(profissional.get("clinicaId").asText()).isEqualTo(SeedV2.CLINICA_VETCARE);
        }
        // Sem o filtro, a listagem traz a plataforma toda -- e continua trazendo.
        assertThat(totalDe(buscar("/api/v1/veterinarios?size=50", vetcare)))
                .isGreaterThan(totalDe(equipe));
    }


    @Test
    @DisplayName("gerencia a grade de um veterinario da casa")
    void gerenciaAGradeDaCasa() throws Exception {
        String id = idDe(criar("/api/v1/disponibilidades", vetcare, """
                {"veterinarioId":"%s","diaSemana":"DOMINGO","horaInicio":"08:00","horaFim":"12:00",
                 "vigenciaInicio":"%s"}"""
                .formatted(SeedV2.VET_CAMILA, LocalDate.now()))
                .andExpect(status().isCreated()));
        removerDepois("/api/v1/disponibilidades/" + id);
    }

    /**
     * O exploit que {@code podeGerenciarAgendaDe} ja documentava para o veterinario,
     * agora fechado tambem para o perfil novo: sem grade, a clinica inteira some da
     * busca por vagas.
     */
    @Test
    @DisplayName("NAO mexe na grade de veterinario da concorrente")
    void naoMexeNaGradeAlheia() throws Exception {
        criar("/api/v1/disponibilidades", vetcare, """
                {"veterinarioId":"%s","diaSemana":"DOMINGO","horaInicio":"08:00","horaFim":"12:00",
                 "vigenciaInicio":"%s"}"""
                .formatted(SeedV2.VET_RAFAEL_DA_PETMED, LocalDate.now()))
                .andExpect(status().isForbidden());
    }

    // ================================================================
    // A conta a receber da casa
    // ================================================================

    /**
     * A lista de inadimplencia acompanha o painel.
     *
     * <p>O painel responde "quanto esta em aberto"; sozinho, esse numero nao vira
     * ligacao nenhuma. Quem responde "de quem" e esta lista — e ela ja nasce
     * recortada pela clinica de quem pergunta, dentro do service.</p>
     */
    @Test
    @DisplayName("ve os devedores da propria clinica")
    void veAInadimplenciaDaCasa() throws Exception {
        buscar("/api/v1/pagamentos/inadimplencia", vetcare).andExpect(status().isOk());
    }

    /** Cobrar e do caixa; confirmar e estornar continuam sendo do corpo clinico. */
    @Test
    @DisplayName("nao confirma nem estorna pagamento")
    void naoMexeNoPagamento() throws Exception {
        criar("/api/v1/pagamentos/" + SeedV2.ID_INEXISTENTE + "/confirmar", vetcare,
                """
                {"dataPagamento":"2026-01-10"}""")
                .andExpect(status().isForbidden());
    }

    // ================================================================
    // O que administrar o negocio NAO concede
    // ================================================================

    /**
     * Administrar um estabelecimento nao e o mesmo que atender. O perfil ficou de
     * fora de {@code temVisaoAmpla} de proposito: aquilo abre o cadastro de todos os
     * tutores da plataforma, com CPF e e-mail, a quem so precisa gerir uma clinica.
     */
    @Test
    @DisplayName("nao enxerga o cadastro de tutores da plataforma")
    void naoListaTutores() throws Exception {
        buscar("/api/v1/tutores", vetcare).andExpect(status().isForbidden());
    }

    /**
     * A listagem de animais era a porta lateral do mesmo cadastro.
     *
     * <p>Por id, {@code podeAcessarAnimal} ja recusava este perfil. A listagem, nao:
     * ela filtra apenas por {@code tutorId}, que aqui e nulo, e nulo significa "sem
     * recorte". Verificado contra a pilha local antes da correcao — 403 para abrir um
     * animal, e 26 animais de todas as clinicas na lista, cada um com o nome e o id
     * do tutor.</p>
     */
    private JsonNode catalogo(String token, boolean incluirInativos) throws Exception {
        return corpoDe(buscar("/api/v1/clinicas/" + SeedV2.CLINICA_VETCARE + "/servicos"
                + (incluirInativos ? "?incluirInativos=true" : ""), token)
                .andExpect(status().isOk()));
    }

    private boolean contem(JsonNode catalogo, String servicoId) {
        for (JsonNode servico : catalogo) {
            if (servicoId.equals(servico.get("id").asText())) {
                return true;
            }
        }
        return false;
    }

    @Test
    @DisplayName("nao lista o cadastro de animais da plataforma")
    void naoListaAnimais() throws Exception {
        buscar("/api/v1/animais", vetcare).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("nao le o historico de um animal que nunca passou pela clinica")
    void naoLeHistoricoAlheio() throws Exception {
        buscar("/api/v1/animais/" + SeedV2.ANIMAL_MIMI_DA_MARIA + "/historico", vetcare)
                .andExpect(status().isConflict());
    }
}
