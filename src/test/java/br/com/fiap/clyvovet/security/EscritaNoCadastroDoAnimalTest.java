package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Quem pode ESCREVER no cadastro do animal.
 *
 * <p>Estes testes existem porque a resposta estava errada, e o erro foi provado
 * contra a pilha local. Um veterinário autenticado, <b>sem nenhuma autorização do
 * tutor</b>, conseguia:</p>
 *
 * <pre>
 *   PATCH  /api/v1/animais/{de outro tutor}  -> 200, e a coluna mudava no banco
 *   DELETE /api/v1/animais/{de outro tutor}  -> 204, e a linha sumia
 *   SELECT COUNT(*) FROM t_clyvo_autorizacao_acesso  -> 0
 * </pre>
 *
 * <p>A causa era o {@code @PreAuthorize} das rotas de escrita usar
 * {@code podeAcessarAnimal}, que passa por {@code temVisaoAmpla()} e devolve
 * {@code true} para todo VETERINARIO. Leitura ampla é intencional — o
 * profissional precisa do cadastro para atender. Escrita não era.</p>
 *
 * <p>A regra do produto é que o veterinário só altera os dados do animal
 * <i>"desde que tenha uma confirmação e autorização do dono"</i>. Esse
 * consentimento de mutação <b>não existe</b>: o {@code AutorizacaoAcesso} de hoje
 * libera leitura do histórico e nasce dentro do agendamento. Sem mecanismo de
 * autorização, a resposta correta é negar.</p>
 *
 * <p>A separação leitura/escrita é o que estes testes travam. Quando o fluxo de
 * pedido-e-aprovação existir, é aqui que ele ganha o caso "com autorização, pode".</p>
 */
class EscritaNoCadastroDoAnimalTest extends TesteDeApi {

    private static final String CORPO_COMPLETO = """
            {"nome":"Bolinha","raca":"Poodle","especie":"CACHORRO","porte":"PEQUENO",
             "cor":"Preto","sexo":"MACHO","dataNascimento":"2020-03-15","tutorId":"%s"}"""
            .formatted(SeedV2.TUTOR_LUCAS);

    // ================================================================
    // O veterinário LÊ, e isso continua valendo
    // ================================================================

    @Test
    @DisplayName("veterinario le o cadastro de qualquer animal")
    void veterinarioLeQualquerAnimal() throws Exception {
        // Leitura ampla é deliberada: sem o cadastro do paciente não há como
        // atender. É a escrita que precisava ser fechada.
        buscar("/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS, tokenVeterinaria())
                .andExpect(status().isOk());
    }

    // ================================================================
    // Mas NÃO escreve sem ser o dono
    // ================================================================

    @Test
    @DisplayName("veterinario NAO altera o cadastro de animal de outro tutor por PATCH")
    void veterinarioNaoAlteraCadastroPorPatch() throws Exception {
        atualizarParcialmente("/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS,
                tokenVeterinaria(), """
                        {"cor":"Alterado sem autorizacao"}""")
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("veterinario NAO substitui o cadastro de animal de outro tutor por PUT")
    void veterinarioNaoSubstituiCadastro() throws Exception {
        atualizar("/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS,
                tokenVeterinaria(), CORPO_COMPLETO)
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("veterinario NAO exclui animal de outro tutor")
    void veterinarioNaoExcluiAnimal() throws Exception {
        // O mais grave dos três: era 204, e o pet desaparecia do banco.
        remover("/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS, tokenVeterinaria())
                .andExpect(status().isForbidden());
    }

    // ================================================================
    // Quem continua podendo
    // ================================================================

    @Test
    @DisplayName("o dono altera o proprio pet")
    void donoAlteraOProprioPet() throws Exception {
        String lucas = tokenTutor(LUCAS);
        String url = "/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS;

        atualizarParcialmente(url, lucas, """
                {"cor":"Caramelo"}""").andExpect(status().isOk());

        // Devolve ao estado do seed, que os demais testes assumem.
        atualizarParcialmente(url, lucas, """
                {"cor":"Preto"}""").andExpect(status().isOk());
    }

    @Test
    @DisplayName("o ADMIN da plataforma altera qualquer pet")
    void adminAlteraQualquerPet() throws Exception {
        String url = "/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS;

        atualizarParcialmente(url, tokenAdmin(), """
                {"cor":"Caramelo"}""").andExpect(status().isOk());

        atualizarParcialmente(url, tokenAdmin(), """
                {"cor":"Preto"}""").andExpect(status().isOk());
    }
}
