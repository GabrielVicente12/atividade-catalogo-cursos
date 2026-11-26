package br.edu.catalogocursos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.security.protection")
public record ProtecaoProperties(
        int maxFailedAttempts, Duration failureWindow, Duration lockDuration) {
    public ProtecaoProperties {
        if (maxFailedAttempts < 1
                || failureWindow == null
                || failureWindow.isNegative()
                || failureWindow.isZero()
                || lockDuration == null
                || lockDuration.isNegative()
                || lockDuration.isZero())
            throw new IllegalArgumentException("Política de proteção inválida");
    }
}
