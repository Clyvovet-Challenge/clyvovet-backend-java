package br.com.fiap.clyvovet.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.method.HandlerTypePredicate;

/**
 * Convencoes da API: prefixo de versao e formato das respostas paginadas.
 *
 * <h2>Prefixo</h2>
 * O prefixo e aplicado aqui, e nao em cada {@code @RequestMapping}, para que os
 * controllers continuem declarando so o proprio recurso ({@code /tutores}) e a
 * versao viva num lugar unico. Trocar para {@code /api/v2} um dia e mudar esta
 * constante.
 *
 * A alternativa seria {@code server.servlet.context-path}, mas ela move o
 * Swagger e o console do H2 junto.
 *
 * O predicado e por PACOTE, e nao por {@code @RestController}: o springdoc
 * tambem anota suas classes com {@code @RestController}, entao filtrar pela
 * anotacao levava {@code /v3/api-docs} para {@code /api/v1/v3/api-docs} e o
 * Swagger parava de abrir. Restrito ao pacote de controllers da aplicacao,
 * {@code /swagger-ui.html}, {@code /v3/api-docs} e {@code /h2-console} seguem
 * na raiz, onde as ferramentas esperam encontra-los.
 *
 * <h2>Paginacao</h2>
 * Sem {@code VIA_DTO} o Spring serializa o {@code PageImpl} inteiro e avisa no
 * boot que "there is no guarantee about the stability of the resulting JSON
 * structure" — a resposta carregava mais de vinte campos internos do framework
 * e podia mudar de forma num upgrade, sem nada no codigo mudar.
 *
 * Com {@code VIA_DTO} o contrato passa a ser {@code content} mais um objeto
 * {@code page} com {@code size}, {@code number}, {@code totalElements} e
 * {@code totalPages}.
 */
@Configuration
@EnableSpringDataWebSupport(
        pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class WebConfig implements WebMvcConfigurer {

    /** Prefixo de todos os endpoints da aplicacao. */
    public static final String PREFIXO_API = "/api/v1";

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(PREFIXO_API,
                HandlerTypePredicate.forBasePackage("br.com.fiap.clyvovet.controller"));
    }
}
