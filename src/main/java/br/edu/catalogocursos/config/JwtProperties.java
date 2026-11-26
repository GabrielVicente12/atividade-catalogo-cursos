package br.edu.catalogocursos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.security.jwt")
public record JwtProperties(String issuer, Duration expiration, String secret) {
    public JwtProperties {
        if (issuer == null
                || issuer.isBlank()
                || expiration == null
                || expiration.isNegative()
                || expiration.isZero())
            throw new IllegalArgumentException("Configuração JWT inválida");
    }
}
