package br.edu.catalogocursos.api.controller;

import br.edu.catalogocursos.api.dto.PaginaResponse;
import br.edu.catalogocursos.application.AuditoriaService;
import br.edu.catalogocursos.application.AuditoriaService.EventoResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auditoria-seguranca")
@Tag(name = "Auditoria")
public class AuditoriaController {
    private final AuditoriaService service;

    public AuditoriaController(AuditoriaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Consultar eventos com filtro e paginação (administrador)")
    public PaginaResponse<EventoResponse> listar(
            @RequestParam(required = false) String tipo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return service.listar(tipo, pagina, tamanho);
    }
}
