package br.edu.catalogocursos.api.controller;

import br.edu.catalogocursos.application.ContaAcessoService;
import br.edu.catalogocursos.application.ContaAcessoService.ContaResponse;
import br.edu.catalogocursos.domain.PerfilAcesso;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/contas-acesso")
@Tag(name = "Contas de acesso")
public class ContaAcessoController {
    private final ContaAcessoService service;

    public ContaAcessoController(ContaAcessoService service) {
        this.service = service;
    }

    public record ContaRequest(
            @NotNull @Positive Long pessoaId,
            @NotBlank @Size(max = 160) String login,
            @NotBlank @Size(min = 12, max = 72) String senha) {}

    public record SenhaRequest(
            @NotBlank String senhaAtual, @NotBlank @Size(min = 12, max = 72) String novaSenha) {}

    public record PerfisRequest(@NotEmpty Set<@NotNull PerfilAcesso> perfis) {}

    public record StatusRequest(@NotNull Boolean ativo) {}

    @PostMapping
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    @Operation(summary = "Criar conta (administrador)")
    public org.springframework.http.ResponseEntity<ContaResponse> criar(
            @Valid @RequestBody ContaRequest r) {
        var conta = service.criar(r.pessoaId(), r.login(), r.senha());
        return org.springframework.http.ResponseEntity.created(
                        java.net.URI.create("/api/contas-acesso/" + conta.id()))
                .body(conta);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar conta por ID (administrador)")
    public ContaResponse porId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping({"/minha", "/me"})
    @Operation(summary = "Consultar a própria conta")
    public ContaResponse minha(Authentication auth) {
        return service.minhaConta(auth.getName());
    }

    @PutMapping({"/minha/senha", "/me/senha"})
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    @Operation(summary = "Alterar a própria senha")
    public void senha(Authentication auth, @Valid @RequestBody SenhaRequest r) {
        service.alterarSenha(auth.getName(), r.senhaAtual(), r.novaSenha());
    }

    @PutMapping("/{id}/perfis")
    @Operation(summary = "Alterar perfis (administrador)")
    public ContaResponse perfis(@PathVariable Long id, @Valid @RequestBody PerfisRequest r) {
        return service.perfis(id, r.perfis());
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Alterar status da conta (administrador)")
    public ContaResponse status(@PathVariable Long id, @Valid @RequestBody StatusRequest r) {
        return service.status(id, r.ativo());
    }
}
