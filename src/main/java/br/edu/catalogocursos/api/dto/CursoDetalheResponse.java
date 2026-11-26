package br.edu.catalogocursos.api.dto;

import br.edu.catalogocursos.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CursoDetalheResponse(
        Long id,
        String codigo,
        String titulo,
        Integer cargaHoraria,
        NivelCurso nivel,
        BigDecimal preco,
        int vagas,
        boolean ativo,
        LocalDate dataCriacao,
        Long instrutorId) {
    public static CursoDetalheResponse de(Curso c) {
        return new CursoDetalheResponse(
                c.getId(),
                c.getCodigo(),
                c.getTitulo(),
                c.getCargaHoraria(),
                c.getNivel(),
                c.getPreco(),
                c.getVagas(),
                c.isAtivo(),
                c.getDataCriacao(),
                c.getInstrutor().getId());
    }
}
