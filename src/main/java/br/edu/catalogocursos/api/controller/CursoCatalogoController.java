package br.edu.catalogocursos.api.controller;

import br.edu.catalogocursos.api.dto.*;
import br.edu.catalogocursos.application.CursoCatalogoService;
import br.edu.catalogocursos.domain.NivelCurso;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cursos")
@Tag(name = "Catálogo", description = "Filtros, atualização e controle de vagas")
public class CursoCatalogoController {
    private final CursoCatalogoService service;

    public CursoCatalogoController(CursoCatalogoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar cursos com filtros e paginação")
    public PaginaResponse<CursoDetalheResponse> listar(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) NivelCurso nivel,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return service.listar(titulo, nivel, ativo, pagina, tamanho);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar curso")
    public CursoDetalheResponse atualizar(
            @PathVariable Long id, @Valid @RequestBody CursoAtualizacaoRequest r) {
        return service.atualizar(id, r);
    }

    public record VagasRequest(@Min(1) int quantidade) {}

    @PostMapping({"/{id}/vagas", "/{id}/vagas/entradas"})
    @Operation(summary = "Adicionar vagas ao curso")
    public CursoDetalheResponse vagas(@PathVariable Long id, @Valid @RequestBody VagasRequest r) {
        return service.vagas(id, r.quantidade());
    }

    @PostMapping("/{id}/vagas/saidas")
    @Operation(summary = "Retirar vagas disponíveis")
    public CursoDetalheResponse retirarVagas(
            @PathVariable Long id, @Valid @RequestBody VagasRequest r) {
        return service.retirarVagas(id, r.quantidade());
    }

    public record StatusRequest(@jakarta.validation.constraints.NotNull Boolean ativo) {}

    @PutMapping("/{id}/status")
    @Operation(summary = "Ativar ou inativar curso")
    public CursoDetalheResponse status(@PathVariable Long id, @Valid @RequestBody StatusRequest r) {
        return service.status(id, r.ativo());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir curso sem matrículas")
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}
