package br.edu.catalogocursos.api.dto;

import br.edu.catalogocursos.domain.NivelCurso;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CursoAtualizacaoRequest(
        @NotBlank @Size(min = 3, max = 150) String titulo,
        @NotNull @Min(1) @Max(400) Integer cargaHoraria,
        @NotNull NivelCurso nivel,
        @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal preco,
        @NotNull Boolean ativo) {}
