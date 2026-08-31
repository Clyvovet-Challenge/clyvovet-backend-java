package br.com.fiap.clyvovet.crud;

import br.com.fiap.clyvovet.support.TesteDeApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A probe que o Azure Container Apps consulta para saber se a instância pode
 * receber tráfego, e que o CI usa para provar que a imagem sobe.
 */
class HealthCheckTest extends TesteDeApi {

    @Test
    @DisplayName("/actuator/health responde sem autenticação")
    void healthEhPublico() throws Exception {
        // Precisa responder antes de existir token: uma probe que exige login
        // reporta a instância como morta desde o primeiro segundo.
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("os demais endpoints do actuator não vazam, nem com token de admin")
    void soHealthEstaExposto() throws Exception {
        // O padrão do actuator publica versão, beans e configuração — um mapa
        // da aplicação para quem alcançar a porta.
        //
        // Duas barreiras, e as duas verificadas. Sem token a regra de rota
        // barra antes de o actuator responder (401); com token de ADMIN, o
        // actuator responde 404 porque o endpoint não está exposto. Uma só
        // bastaria, mas testar as duas é o que impede que afrouxar a regra de
        // rota amanhã abra os endpoints sem ninguém perceber.
        mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/beans")).andExpect(status().isUnauthorized());

        String admin = tokenAdmin();
        buscar("/actuator/env", admin).andExpect(status().isNotFound());
        buscar("/actuator/beans", admin).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("o health não detalha o estado interno")
    void healthNaoDetalha() throws Exception {
        // show-details=never: sem isso a resposta traz o banco, a URL de conexão
        // e o espaço em disco, sem pedir credencial nenhuma.
        mockMvc.perform(get("/actuator/health"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }
}
