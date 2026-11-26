package br.edu.catalogocursos.api.controller;

import br.edu.catalogocursos.api.dto.CursoResumoResponse;
import br.edu.catalogocursos.api.dto.InstrutorRequest;
import br.edu.catalogocursos.api.dto.InstrutorResponse;
import br.edu.catalogocursos.application.InstrutorService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Instrutores")
@RestController
@RequestMapping("/api/instrutores")
public class InstrutorController {
    private final InstrutorService service;

    public InstrutorController(InstrutorService service) {
        this.service = service;
    }

    @GetMapping
    public br.edu.catalogocursos.api.dto.PaginaResponse<InstrutorResponse> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return service.listar(pagina, tamanho);
    }

    @PutMapping("/{id}")
    @io.swagger.v3.oas.annotations.Operation(summary = "Atualizar instrutor")
    public InstrutorResponse atualizar(
            @PathVariable Long id, @Valid @RequestBody InstrutorRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @io.swagger.v3.oas.annotations.Operation(summary = "Excluir instrutor sem cursos")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }

    @PostMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "Cadastrar instrutor")
    public ResponseEntity<InstrutorResponse> cadastrar(
            @Valid @RequestBody InstrutorRequest request) {
        InstrutorResponse instrutor = service.cadastrar(request);
        return ResponseEntity.created(URI.create("/api/instrutores/" + instrutor.id()))
                .body(instrutor);
    }

    @GetMapping("/{id}")
    @io.swagger.v3.oas.annotations.Operation(summary = "Consultar instrutor")
    public InstrutorResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/{id}/cursos")
    @io.swagger.v3.oas.annotations.Operation(summary = "Listar cursos do instrutor")
    public List<CursoResumoResponse> listarCursos(@PathVariable Long id) {
        return service.listarCursos(id);
    }
}
