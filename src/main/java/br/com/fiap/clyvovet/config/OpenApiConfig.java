package br.com.fiap.clyvovet.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declara o esquema bearer para o Swagger, o que habilita o botao "Authorize".
 * Sem isso a UI nao envia o header Authorization e todas as chamadas voltam 401.
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA = "bearerAuth";

    @Bean
    public OpenAPI clyvovetOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CLYVO VET API")
                        .version("v1")
                        .description("""
                                API de saude continua para pets.

                                Autenticacao: obtenha um token em POST /auth/login e informe-o
                                em Authorize, no formato do proprio campo (sem o prefixo Bearer).

                                Perfis: TUTOR ve apenas os proprios pets; VETERINARIO registra
                                atendimentos e enxerga a base clinica; ADMIN administra o cadastro."""))
                .components(new Components().addSecuritySchemes(ESQUEMA,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA));
    }
}
