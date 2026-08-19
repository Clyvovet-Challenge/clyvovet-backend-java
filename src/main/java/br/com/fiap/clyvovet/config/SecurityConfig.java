package br.com.fiap.clyvovet.config;

import br.com.fiap.clyvovet.security.JwtAuthenticationFilter;
import br.com.fiap.clyvovet.security.RateLimitFilter;
import br.com.fiap.clyvovet.security.RespostaErroSeguranca;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity   // habilita @PreAuthorize nos controllers
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String VETERINARIO = "VETERINARIO";
    private static final long UM_ANO_EM_SEGUNDOS = 31_536_000L;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final RespostaErroSeguranca respostaErroSeguranca;

    /** Origens permitidas para CORS. Nunca "*" combinado com credenciais. */
    @Value("${clyvovet.cors.origens:http://localhost:3000,http://localhost:8081}")
    private String[] origensPermitidas;

    /** Console do H2 so e liberado onde ele existe (perfis dev e h2). */
    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleHabilitado;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ----------------------------------------------------------------
            // CSRF desabilitado — decisao consciente, nao esquecimento.
            //
            // O ataque CSRF depende do navegador anexar credenciais
            // AUTOMATICAMENTE a uma requisicao cross-site, o que acontece com
            // cookie de sessao. Esta API e stateless e autentica por token no
            // header Authorization, que so e enviado se o cliente o colocar
            // explicitamente — um site malicioso nao consegue faze-lo.
            //
            // GATILHO PARA REATIVAR: se a Sprint 3 adicionar form login com
            // sessao (frontend Thymeleaf), o vetor passa a existir e o CSRF
            // deve ser habilitado para as rotas baseadas em sessao.
            // ----------------------------------------------------------------
            .csrf(AbstractHttpConfigurer::disable)

            .cors(Customizer.withDefaults())

            // Sem sessao: cada requisicao se sustenta pelo proprio token.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .headers(this::configurarHeaders)

            // 401 e 403 em JSON, no mesmo formato dos demais erros da API
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(respostaErroSeguranca)
                    .accessDeniedHandler(respostaErroSeguranca))

            .authorizeHttpRequests(this::configurarRotas)

            // Rate limit antes da autenticacao: uma rajada de tentativas de
            // login precisa ser barrada antes de custar um BCrypt por request.
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(jwtAuthenticationFilter, RateLimitFilter.class);

        return http.build();
    }

    private void configurarRotas(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rotas) {
        rotas
            // --- Publico ---
            .requestMatchers(api("/auth/login", "/auth/refresh", "/auth/logout", "/auth/registrar")).permitAll()
            // Swagger publico: e por ele que a API e avaliada e testada.
            // Fica fora do api(): o WebConfig so prefixa os @RestController da
            // aplicacao, entao as rotas do springdoc seguem na raiz.
            .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll();

        if (h2ConsoleHabilitado) {
            rotas.requestMatchers("/h2-console/**").permitAll();
        }

        rotas
            // --- Somente ADMIN ---
            .requestMatchers(api("/auth/usuarios")).hasRole(ADMIN)
            .requestMatchers(HttpMethod.POST,   api("/clinicas", "/veterinarios")).hasRole(ADMIN)
            .requestMatchers(HttpMethod.PUT,    api("/clinicas/**", "/veterinarios/**")).hasRole(ADMIN)
            .requestMatchers(HttpMethod.PATCH,  api("/clinicas/**", "/veterinarios/**")).hasRole(ADMIN)
            .requestMatchers(HttpMethod.DELETE, api("/clinicas/**", "/veterinarios/**")).hasRole(ADMIN)

            // --- Corpo clinico: quem registra atendimento e cobranca ---
            .requestMatchers(HttpMethod.POST,   api("/eventos-clinicos", "/pagamentos")).hasAnyRole(VETERINARIO, ADMIN)
            .requestMatchers(HttpMethod.PUT,    api("/eventos-clinicos/**", "/pagamentos/**")).hasAnyRole(VETERINARIO, ADMIN)
            .requestMatchers(HttpMethod.PATCH,  api("/eventos-clinicos/**", "/pagamentos/**")).hasAnyRole(VETERINARIO, ADMIN)
            .requestMatchers(HttpMethod.DELETE, api("/eventos-clinicos/**", "/pagamentos/**")).hasAnyRole(VETERINARIO, ADMIN)

            // Listar tutores expoe CPF e e-mail de terceiros: nao e para tutor.
            .requestMatchers(HttpMethod.GET,    api("/tutores")).hasAnyRole(VETERINARIO, ADMIN)
            .requestMatchers(HttpMethod.POST,   api("/tutores")).hasAnyRole(VETERINARIO, ADMIN)
            .requestMatchers(HttpMethod.DELETE, api("/tutores/**")).hasAnyRole(VETERINARIO, ADMIN)

            // Animal: tutor cria e edita os proprios; o ownership em si e
            // verificado por @PreAuthorize no controller.
            .requestMatchers(api("/animais/**")).authenticated()

            // Fecha por padrao: rota nova nasce protegida, nao aberta.
            .anyRequest().authenticated();
    }

    /**
     * Prefixa rotas da aplicacao com a versao da API.
     *
     * O prefixo e aplicado aos controllers pelo {@link WebConfig}, entao os
     * matchers precisam enxergar o caminho ja prefixado. Concentrar isso aqui
     * evita repetir "/api/v1" em cada linha e garante que os dois lados usem a
     * mesma constante -- se divergissem, uma rota ficaria aberta em silencio.
     */
    private static String[] api(String... rotas) {
        return Arrays.stream(rotas)
                .map(rota -> WebConfig.PREFIXO_API + rota)
                .toArray(String[]::new);
    }

    private void configurarHeaders(HeadersConfigurer<HttpSecurity> headers) {
        headers
            .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(UM_ANO_EM_SEGUNDOS))
            .contentTypeOptions(Customizer.withDefaults())
            .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; frame-ancestors 'none'"));

        // O console do H2 roda dentro de um frame proprio; DENY o quebraria.
        if (h2ConsoleHabilitado) {
            headers.frameOptions(frame -> frame.sameOrigin());
        } else {
            headers.frameOptions(frame -> frame.deny());
        }
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(origensPermitidas));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Retry-After"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
