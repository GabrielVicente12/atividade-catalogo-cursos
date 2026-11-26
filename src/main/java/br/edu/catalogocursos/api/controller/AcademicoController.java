package br.edu.catalogocursos.api.controller;

import br.edu.catalogocursos.api.dto.InstrutorRequest;
import br.edu.catalogocursos.application.AcademicoService;
import br.edu.catalogocursos.application.AcademicoService.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api")
@Tag(name = "Acadêmico", description = "Identidades, papéis e matrículas")
public class AcademicoController {
    private final AcademicoService service;

    public AcademicoController(AcademicoService service) {
        this.service = service;
    }

    public record PapelRequest(@NotNull @Positive Long pessoaId) {}

    public record ColaboradorRequest(
            @NotNull @Positive Long pessoaId, @NotBlank @Size(max = 100) String cargo) {}

    public record MatriculaRequest(
            @NotNull @Positive Long alunoId, @NotNull @Positive Long cursoId) {}

    @PostMapping("/pessoas")
    @Operation(summary = "Cadastrar pessoa")
    public ResponseEntity<PessoaResponse> pessoa(@Valid @RequestBody InstrutorRequest r) {
        var p = service.criarPessoa(r.nome(), r.email());
        return ResponseEntity.created(URI.create("/api/pessoas/" + p.id())).body(p);
    }

    @GetMapping("/pessoas/{id}")
    @Operation(summary = "Consultar pessoa")
    public PessoaResponse pessoa(@PathVariable Long id) {
        return service.buscarPessoa(id);
    }

    @PostMapping("/alunos")
    @Operation(summary = "Atribuir papel de aluno")
    public ResponseEntity<PapelResponse> aluno(@Valid @RequestBody PapelRequest r) {
        var a = service.criarAluno(r.pessoaId());
        return ResponseEntity.created(URI.create("/api/alunos/" + a.id())).body(a);
    }

    @PostMapping("/colaboradores")
    @Operation(summary = "Atribuir papel de colaborador")
    public ResponseEntity<PapelResponse> colaborador(@Valid @RequestBody ColaboradorRequest r) {
        var c = service.criarColaborador(r.pessoaId(), r.cargo());
        return ResponseEntity.created(URI.create("/api/colaboradores/" + c.id())).body(c);
    }

    @GetMapping("/alunos/{id}")
    @Operation(summary = "Consultar papel de aluno")
    public PapelResponse aluno(@PathVariable Long id) {
        return service.buscarAluno(id);
    }

    @GetMapping("/colaboradores/{id}")
    @Operation(summary = "Consultar papel de colaborador")
    public PapelResponse colaborador(@PathVariable Long id) {
        return service.buscarColaborador(id);
    }

    @PostMapping("/matriculas")
    @Operation(summary = "Confirmar matrícula e reservar uma vaga atomicamente")
    public ResponseEntity<MatriculaResponse> matricular(@Valid @RequestBody MatriculaRequest r) {
        var m = service.matricular(r.alunoId(), r.cursoId());
        return ResponseEntity.created(URI.create("/api/matriculas/" + m.id())).body(m);
    }

    @GetMapping("/matriculas/{id}")
    @Operation(summary = "Consultar matrícula e valor histórico")
    public MatriculaResponse matricula(@PathVariable Long id) {
        return service.buscarMatricula(id);
    }

    @PostMapping("/matriculas/{id}/cancelamento")
    @Operation(summary = "Cancelar matrícula e devolver a vaga")
    public MatriculaResponse cancelar(@PathVariable Long id) {
        return service.cancelar(id);
    }
}
