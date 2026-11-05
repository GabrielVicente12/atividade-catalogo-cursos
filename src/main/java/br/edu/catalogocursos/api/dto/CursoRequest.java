package br.edu.catalogocursos.api.dto;

import br.edu.catalogocursos.domain.NivelCurso;
import jakarta.validation.constraints.*;

public record CursoRequest(
        @NotBlank @Size(min = 3, max = 150) String titulo,
        @NotNull @Min(1) @Max(400) Integer cargaHoraria,
        @NotNull NivelCurso nivel,
        @NotNull @Positive Long instrutorId) {}
