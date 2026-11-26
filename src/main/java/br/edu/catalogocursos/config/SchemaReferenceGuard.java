package br.edu.catalogocursos.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Arrays;

public class SchemaReferenceGuard implements EnvironmentPostProcessor, Ordered {
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        if (Arrays.asList(env.getActiveProfiles()).contains("schema-reference")) {
            String url = env.getProperty("DB_REFERENCE_URL", "");
            if (!url.matches("jdbc:postgresql://[^/]+/catalogocursos_reference(?:[?].*)?"))
                throw new IllegalStateException(
                        "O perfil schema-reference exige banco descartável"
                                + " catalogocursos_reference");
        }
    }
}
