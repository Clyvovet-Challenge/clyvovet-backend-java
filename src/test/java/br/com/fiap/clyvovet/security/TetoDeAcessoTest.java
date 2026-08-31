package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Os tetos de leitura do historico clinico — regras C6 e C22 da spec 08.
 *
 * O QUE ELES PROTEGEM, E CONTRA O QUE
 * O rate limit por IP ja existia e protege a infraestrutura contra rajada:
 * conta requisicoes e nao sabe nada sobre pacientes. Estes tetos protegem os
 * pacientes contra coleta: contam ANIMAIS DISTINTOS por profissional por dia. E
 * a diferenca entre "muitas chamadas" e "muitos prontuarios", que sao coisas
 * diferentes e so a segunda diz alguma coisa sobre privacidade.
 */
class TetoDeAcessoTest extends TesteDeApi {

    private String animalComChip(String chip) throws Exception {
        ResultActions animal = criar("/api/v1/animais", tokenTutor(LUCAS), """
                {"nome":"Paciente %s","raca":"SRD","especie":"Canina","porte":"MEDIO",
                 "cor":"Caramelo","sexo":"MACHO","dataNascimento":"2022-01-10",
                 "tutorId":"%s","microchip":"%s"}"""
                .formatted(chip, SeedV2.TUTOR_LUCAS, chip));
        animal.andExpect(status().isCreated());
        String id = idDe(animal);
        removerDepois("/api/v1/animais/" + id);
        return id;
    }

    @Test
    @DisplayName("reabrir o mesmo prontuário não consome teto")
    void mesmoAnimalNaoConsomeTeto() throws Exception {
        // A distincao central do desenho: o teto conta ANIMAIS, nao chamadas. Um
        // veterinario que reabre o mesmo resumo durante uma cirurgia longa nao
        // pode ser tratado como quem varre a base.
        animalComChip("900000000070001");
        String vet = tokenVeterinaria();

        for (int i = 0; i < 40; i++) {
            buscar("/api/v1/animais/resumo?microchip=900000000070001", vet)
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("a auditoria agrega as leituras do dia numa linha só")
    void auditoriaContaAnimalUmaVez() throws Exception {
        String animalId = animalComChip("900000000070002");
        String vet = tokenVeterinaria();

        buscar("/api/v1/animais/resumo?microchip=900000000070002", vet).andExpect(status().isOk());
        buscar("/api/v1/animais/resumo?microchip=900000000070002", vet).andExpect(status().isOk());

        // Uma linha, contador em 2 — e o que faz a contagem do teto significar
        // "animais distintos" sem precisar de um SELECT DISTINCT caro.
        var acessos = corpoDe(buscar("/api/v1/animais/" + animalId + "/acessos", tokenTutor(LUCAS)));
        assertThat(acessos.size()).isEqualTo(1);
        assertThat(acessos.get(0).get("vezes").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("a quebra de vidro nunca é bloqueada, por mais que se repita")
    void quebraDeVidroNuncaBloqueia() throws Exception {
        // A decisao mais importante do desenho. Travar o acesso numa emergencia
        // cobraria a conta do paciente, nao de quem abusa: em algum atendimento
        // o veterinario abriria a tela e receberia 429 no lugar do historico. O
        // controle e outro — passa, e o alarme sobe para a revisao do admin.
        String vet = tokenVeterinaria();
        for (int i = 1; i <= 8; i++) {
            String id = animalComChip("90000000008000" + i);
            criar("/api/v1/animais/" + id + "/acesso-emergencial", vet, """
                    {"motivo":"Animal em choque, tutor não localizado"}""")
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("o admin vê quem acionou quebra de vidro no período")
    void adminRevisaQuebrasDeVidro() throws Exception {
        String id = animalComChip("900000000090001");
        criar("/api/v1/animais/" + id + "/acesso-emergencial", tokenVeterinaria(), """
                {"motivo":"Convulsão, sem histórico disponível"}""")
                .andExpect(status().isOk());

        ResultActions revisao = buscar("/api/v1/auditoria/quebras-de-vidro?dias=1", tokenAdmin());
        revisao.andExpect(status().isOk());

        // Sem esta consulta, os tetos produziriam apenas linhas de log — e log
        // que ninguem abre nao e controle, e registro.
        assertThat(corpoDe(revisao).size()).isGreaterThan(0);
        assertThat(corpoDe(revisao).get(0).get("emergencial").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("a revisão de tetos é só do administrador da plataforma")
    void revisaoEhSoDoAdmin() throws Exception {
        // Nas maos do corpo clinico, a lista vira o mapa de como nao ser notado.
        buscar("/api/v1/auditoria/excessos", tokenVeterinaria()).andExpect(status().isForbidden());
        buscar("/api/v1/auditoria/excessos", tokenTutor(LUCAS)).andExpect(status().isForbidden());
        buscar("/api/v1/auditoria/quebras-de-vidro", tokenVeterinaria()).andExpect(status().isForbidden());

        buscar("/api/v1/auditoria/excessos", tokenAdmin()).andExpect(status().isOk());
    }

    @Test
    @DisplayName("a revisão de leitura não devolve quem ficou abaixo do teto")
    void revisaoIgnoraVolumeNormal() throws Exception {
        // Um punhado de consultas e atendimento, nao coleta. Se o volume normal
        // aparecesse na lista, o admin pararia de le-la — e o controle morreria
        // por ruido, nao por falha.
        animalComChip("900000000090002");
        buscar("/api/v1/animais/resumo?microchip=900000000090002", tokenVeterinaria())
                .andExpect(status().isOk());

        ResultActions revisao = buscar("/api/v1/auditoria/excessos?dias=1", tokenAdmin());
        revisao.andExpect(status().isOk());
        assertThat(corpoDe(revisao).size()).isEqualTo(0);
    }
}
