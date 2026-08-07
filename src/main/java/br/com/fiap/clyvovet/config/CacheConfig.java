package br.com.fiap.clyvovet.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Troca o ConcurrentMapCacheManager padrao por Caffeine.
 *
 * O default do Spring Boot nao tem TTL nem limite de tamanho: uma entrada
 * cacheada permanecia ate a proxima escrita da entidade, e o mapa crescia
 * indefinidamente conforme apareciam novas combinacoes de filtro e paginacao.
 *
 * Com o filtro por tutor introduzido na autorizacao, o numero de chaves
 * possiveis passou a crescer junto com a base de usuarios — o que torna
 * o limite de tamanho necessario, nao apenas desejavel.
 */
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "tutores", "animais", "clinicas", "veterinarios", "eventos", "pagamentos");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(10))
                .maximumSize(1_000));
        return cacheManager;
    }
}
