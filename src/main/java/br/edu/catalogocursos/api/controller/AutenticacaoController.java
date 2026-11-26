package br.edu.catalogocursos.api.controller;

import br.edu.catalogocursos.api.dto.TokenResponse;
import br.edu.catalogocursos.application.AutenticacaoService;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/autenticacao")
@Tag(name = "Autenticação")
public class AutenticacaoController {
    private final AutenticacaoService service;

    public AutenticacaoController(AutenticacaoService service) {
        this.service = service;
    }

    public record LoginRequest(
            @NotBlank @Size(max = 160) String login, @NotBlank @Size(max = 1000) String senha) {}

    public record RefreshRequest(@NotBlank @Size(max = 256) String refreshToken) {}

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Autenticar e emitir access e refresh tokens")
    public TokenResponse login(@Valid @RequestBody LoginRequest r, HttpServletRequest http) {
        return service.login(r.login(), r.senha(), http.getRemoteAddr());
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Renovar com rotação de uso único")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest r) {
        return service.renovar(r.refreshToken());
    }

    @PostMapping("/logout")
    @SecurityRequirements
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revogar a família de uma sessão")
    public void logout(@Valid @RequestBody RefreshRequest r) {
        service.logout(r.refreshToken());
    }

    @PostMapping("/logout-todos")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revogar todas as sessões da conta")
    public void logoutTodos(Authentication auth) {
        service.logoutTodos(auth.getName());
    }
}
