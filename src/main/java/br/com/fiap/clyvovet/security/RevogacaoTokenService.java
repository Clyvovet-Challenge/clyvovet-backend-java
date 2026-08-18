package br.com.fiap.clyvovet.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Deny-list de refresh tokens revogados, guardada pelo claim "jti".
 *
 * Em memoria, e nao no banco, de proposito: e uma lista curta e volatil —
 * cada entrada expira sozinha quando o token que ela bloqueia jah teria
 * expirado por conta propria. Guardar isso no banco trocaria uma checagem
 * O(1) em cache por escrita/leitura de linha a cada refresh, sem ganhar nada
 * (a entrada nao precisa sobreviver a um restart: um token revogado antes do
 * restart tambem nao seria mais aceito depois, ja que expira pelo prazo do
 * proprio JWT).
 *
 * LIMITACAO CONHECIDA: como o RateLimitFilter, o estado e local ao processo.
 * Com mais de uma replica, um logout numa instancia nao revoga o token nas
 * demais; a correcao seria mover isto para um cache compartilhado (Redis).
 */
@Component
public class RevogacaoTokenService {

    private final Cache<String, Boolean> revogados;

    public RevogacaoTokenService(@Value("${clyvovet.jwt.refresh-token-dias:7}") long refreshDias) {
        this.revogados = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofDays(refreshDias))
                .maximumSize(100_000)
                .build();
    }

    public void revogar(String jti) {
        revogados.put(jti, Boolean.TRUE);
    }

    public boolean estaRevogado(String jti) {
        return revogados.getIfPresent(jti) != null;
    }
}
