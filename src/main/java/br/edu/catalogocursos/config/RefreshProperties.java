package br.edu.catalogocursos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.security.refresh")
public record RefreshProperties(Duration idleTimeout, Duration absoluteExpiration) {
    public RefreshProperties {
        if (idleTimeout == null
                || idleTimeout.isNegative()
                || idleTimeout.isZero()
                || absoluteExpiration == null
                || absoluteExpiration.compareTo(idleTimeout) <= 0)
            throw new IllegalArgumentException("Validades de refresh inválidas");
    }
}
