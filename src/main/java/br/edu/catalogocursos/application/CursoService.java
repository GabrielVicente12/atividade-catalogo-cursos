package br.edu.catalogocursos.application;

import br.edu.catalogocursos.api.dto.CursoRequest;
import br.edu.catalogocursos.api.dto.CursoResponse;
import br.edu.catalogocursos.api.exception.RecursoNaoEncontradoException;
import br.edu.catalogocursos.api.mapper.CursoMapper;
import br.edu.catalogocursos.domain.Curso;
import br.edu.catalogocursos.domain.Instrutor;
import br.edu.catalogocursos.repository.CursoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CursoService {
    private final CursoRepository cursos;
    private final InstrutorService instrutores;
    private final CursoMapper mapper;

    public CursoService(CursoRepository cursos, InstrutorService instrutores, CursoMapper mapper) {
        this.cursos = cursos;
        this.instrutores = instrutores;
        this.mapper = mapper;
    }

    @Transactional
    public CursoResponse cadastrar(CursoRequest request) {
        Instrutor instrutor = instrutores.obterEntidade(request.instrutorId());
        Curso curso = mapper.paraEntidade(request);
        instrutor.adicionarCurso(curso);
        return mapper.paraResposta(cursos.save(curso));
    }

    @Transactional(readOnly = true)
    public CursoResponse buscarPorId(Long id) {
        return cursos.findById(id)
                .map(mapper::paraResposta)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Curso não encontrado"));
    }
}
