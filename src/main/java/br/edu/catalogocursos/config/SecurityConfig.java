package br.edu.catalogocursos.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Clock;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({
    JwtProperties.class,
    RefreshProperties.class,
    ProtecaoProperties.class
})
public class SecurityConfig {
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecretKey secretKey(JwtProperties p) {
        byte[] key = Base64.getDecoder().decode(p.secret());
        if (key.length < 32)
            throw new IllegalArgumentException("JWT_SECRET requer ao menos 32 bytes em Base64");
        return new SecretKeySpec(key, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey key) {
        return NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey key, JwtProperties p) {
        var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(p.issuer()));
        return decoder;
    }

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
    public SecurityFilterChain security(HttpSecurity http) throws Exception {
        var roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("perfis");
        roles.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return http.csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        a ->
                                a.requestMatchers(
                                                "/api/health",
                                                "/swagger-ui.html",
                                                "/swagger-ui/**",
                                                "/v3/api-docs",
                                                "/v3/api-docs/**",
                                                "/v3/api-docs.yaml")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/api/autenticacao/login",
                                                "/api/autenticacao/refresh",
                                                "/api/autenticacao/logout")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.GET, "/api/contas-acesso/{id:[0-9]+}")
                                        .hasRole("ADMINISTRADOR")
                                        .requestMatchers("/api/auditoria-seguranca/**")
                                        .hasRole("ADMINISTRADOR")
                                        .requestMatchers(HttpMethod.POST, "/api/contas-acesso")
                                        .hasRole("ADMINISTRADOR")
                                        .requestMatchers(
                                                "/api/contas-acesso/*/perfis",
                                                "/api/contas-acesso/*/status")
                                        .hasRole("ADMINISTRADOR")
                                        .requestMatchers(HttpMethod.DELETE, "/api/**")
                                        .hasRole("ADMINISTRADOR")
                                        .requestMatchers("/api/**")
                                        .hasAnyRole("OPERADOR", "ADMINISTRADOR")
                                        .anyRequest()
                                        .denyAll())
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter)))
                .build();
    }
}
