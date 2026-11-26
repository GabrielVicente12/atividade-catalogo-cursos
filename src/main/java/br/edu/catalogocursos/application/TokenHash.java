package br.edu.catalogocursos.application;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

public final class TokenHash {
    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenHash() {}

    public static String gerar() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String valor) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
