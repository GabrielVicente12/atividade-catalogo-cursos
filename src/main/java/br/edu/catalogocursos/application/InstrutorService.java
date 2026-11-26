package br.edu.catalogocursos.application;

import br.edu.catalogocursos.api.dto.*;
import br.edu.catalogocursos.api.exception.EmailDuplicadoException;
import br.edu.catalogocursos.api.exception.RecursoNaoEncontradoException;
import br.edu.catalogocursos.api.mapper.CursoMapper;
import br.edu.catalogocursos.api.mapper.InstrutorMapper;
import br.edu.catalogocursos.domain.Instrutor;
import br.edu.catalogocursos.repository.CursoRepository;
import br.edu.catalogocursos.repository.InstrutorRepository;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InstrutorService {
    private final InstrutorRepository instrutores;
    private final CursoRepository cursos;
    private final InstrutorMapper instrutorMapper;
    private final CursoMapper cursoMapper;

    public InstrutorService(
            InstrutorRepository instrutores,
            CursoRepository cursos,
            InstrutorMapper instrutorMapper,
            CursoMapper cursoMapper) {
        this.instrutores = instrutores;
        this.cursos = cursos;
        this.instrutorMapper = instrutorMapper;
        this.cursoMapper = cursoMapper;
    }

    @Transactional
    public InstrutorResponse cadastrar(InstrutorRequest request) {
        if (instrutores.existsByEmail(request.email())) {
            throw new EmailDuplicadoException();
        }
        try {
            Instrutor salvo = instrutores.saveAndFlush(instrutorMapper.paraEntidade(request));
            return instrutorMapper.paraResposta(salvo);
        } catch (DataIntegrityViolationException exception) {
            if (exception.getCause() instanceof ConstraintViolationException violacao
                    && "uk_instrutor_email".equals(violacao.getConstraintName())) {
                throw new EmailDuplicadoException();
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public InstrutorResponse buscarPorId(Long id) {
        return instrutorMapper.paraResposta(obterEntidade(id));
    }

    @Transactional(readOnly = true)
    public List<CursoResumoResponse> listarCursos(Long id) {
        if (!instrutores.existsById(id)) {
            throw new RecursoNaoEncontradoException("Instrutor não encontrado");
        }
        return cursos.findByInstrutorIdOrderByTituloAsc(id).stream()
                .map(cursoMapper::paraResumo)
                .toList();
    }

    @Transactional(readOnly = true)
    public br.edu.catalogocursos.api.dto.PaginaResponse<InstrutorResponse> listar(
            int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > 100)
            throw new IllegalArgumentException("Paginação inválida");
        var page =
                instrutores.findAll(
                        org.springframework.data.domain.PageRequest.of(
                                pagina,
                                tamanho,
                                org.springframework.data.domain.Sort.by("nome")
                                        .ascending()
                                        .and(org.springframework.data.domain.Sort.by("id"))));
        return br.edu.catalogocursos.api.dto.PaginaResponse.de(
                page.map(instrutorMapper::paraResposta));
    }

    @Transactional
    public InstrutorResponse atualizar(Long id, InstrutorRequest request) {
        Instrutor instrutor = obterEntidade(id);
        if (instrutores.existsByEmailAndIdNot(request.email(), id))
            throw new EmailDuplicadoException();
        instrutor.atualizar(request.nome(), request.email());
        instrutores.flush();
        return instrutorMapper.paraResposta(instrutor);
    }

    @Transactional
    public void excluir(Long id) {
        Instrutor instrutor = obterEntidade(id);
        if (cursos.existsByInstrutorId(id)) throw new ConflitoException("Instrutor possui cursos");
        instrutores.delete(instrutor);
    }

    @Transactional(readOnly = true)
    Instrutor obterEntidade(Long id) {
        return instrutores
                .findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Instrutor não encontrado"));
    }
}
