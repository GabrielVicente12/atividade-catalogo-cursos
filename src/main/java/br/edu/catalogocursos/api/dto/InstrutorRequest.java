package br.edu.catalogocursos.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InstrutorRequest(
        @io.swagger.v3.oas.annotations.media.Schema(example = "Marina Costa")
                @NotBlank
                @Size(min = 3, max = 120)
                String nome,
        @io.swagger.v3.oas.annotations.media.Schema(example = "marina@example.com")
                @NotBlank
                @Email
                @Size(max = 160)
                String email) {}
