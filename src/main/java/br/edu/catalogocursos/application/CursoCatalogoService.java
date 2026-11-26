package br.edu.catalogocursos.application;

import br.edu.catalogocursos.api.dto.*;
import br.edu.catalogocursos.api.exception.RecursoNaoEncontradoException;
import br.edu.catalogocursos.domain.*;
import br.edu.catalogocursos.repository.*;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CursoCatalogoService {
    private final CursoRepository cursos;
    private final MatriculaRepository matriculas;

    public CursoCatalogoService(CursoRepository cursos, MatriculaRepository matriculas) {
        this.cursos = cursos;
        this.matriculas = matriculas;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<CursoDetalheResponse> listar(
            String titulo, NivelCurso nivel, Boolean ativo, int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > 100)
            throw new IllegalArgumentException("Paginação inválida");
        Specification<Curso> filtro = (r, q, b) -> b.conjunction();
        if (titulo != null)
            filtro =
                    filtro.and(
                            (r, q, b) ->
                                    b.like(
                                            b.lower(r.get("titulo")),
                                            "%" + titulo.toLowerCase(java.util.Locale.ROOT) + "%"));
        if (nivel != null) filtro = filtro.and((r, q, b) -> b.equal(r.get("nivel"), nivel));
        if (ativo != null) filtro = filtro.and((r, q, b) -> b.equal(r.get("ativo"), ativo));
        return PaginaResponse.de(
                cursos.findAll(
                                filtro,
                                PageRequest.of(
                                        pagina,
                                        tamanho,
                                        Sort.by("titulo").ascending().and(Sort.by("id"))))
                        .map(CursoDetalheResponse::de));
    }

    @Transactional
    public CursoDetalheResponse atualizar(Long id, CursoAtualizacaoRequest r) {
        Curso c = obter(id);
        c.atualizar(r.titulo(), r.cargaHoraria(), r.nivel(), r.preco(), r.ativo());
        return CursoDetalheResponse.de(c);
    }

    @Transactional
    public CursoDetalheResponse vagas(Long id, int quantidade) {
        Curso c = obter(id);
        c.adicionarVagas(quantidade);
        return CursoDetalheResponse.de(c);
    }

    @Transactional
    public CursoDetalheResponse retirarVagas(Long id, int quantidade) {
        Curso c = obter(id);
        c.retirarVagas(quantidade);
        return CursoDetalheResponse.de(c);
    }

    @Transactional
    public CursoDetalheResponse status(Long id, boolean ativo) {
        Curso c = obter(id);
        c.alterarStatus(ativo);
        return CursoDetalheResponse.de(c);
    }

    @Transactional
    public void excluir(Long id) {
        Curso c = obter(id);
        if (matriculas.existsByCursoId(id)) throw new ConflitoException("Curso possui matrículas");
        cursos.delete(c);
    }

    private Curso obter(Long id) {
        return cursos.buscarComLock(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Curso não encontrado"));
    }
}
