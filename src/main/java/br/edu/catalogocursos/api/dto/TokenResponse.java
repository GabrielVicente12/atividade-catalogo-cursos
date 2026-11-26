package br.edu.catalogocursos.api.dto;

import java.time.Instant;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Instant expiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt) {}
