package br.edu.catalogocursos.api.dto;

import br.edu.catalogocursos.domain.NivelCurso;

import jakarta.validation.constraints.*;

public record CursoRequest(
        @io.swagger.v3.oas.annotations.media.Schema(example = "Java com Spring Boot")
                @NotBlank
                @Size(min = 3, max = 150)
                String titulo,
        @io.swagger.v3.oas.annotations.media.Schema(example = "40") @NotNull @Min(1) @Max(400)
                Integer cargaHoraria,
        @io.swagger.v3.oas.annotations.media.Schema(example = "INICIANTE") @NotNull
                NivelCurso nivel,
        @io.swagger.v3.oas.annotations.media.Schema(example = "1") @NotNull @Positive
                Long instrutorId) {}
