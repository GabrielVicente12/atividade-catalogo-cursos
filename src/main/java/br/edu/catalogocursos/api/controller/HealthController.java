package br.edu.catalogocursos.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

import org.springframework.web.bind.annotation.*;

@RestController
public class HealthController {
    @GetMapping("/api/health")
    @SecurityRequirements
    @Operation(summary = "Verificar se a aplicação está respondendo")
    public String health() {
        return "OK";
    }
}
