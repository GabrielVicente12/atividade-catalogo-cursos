package br.edu.catalogocursos.application;

import br.edu.catalogocursos.api.exception.RecursoNaoEncontradoException;
import br.edu.catalogocursos.domain.*;
import br.edu.catalogocursos.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class AcademicoService {
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    private final PessoaRepository pessoas;
    private final AlunoRepository alunos;
    private final ColaboradorRepository colaboradores;
    private final CursoRepository cursos;
    private final MatriculaRepository matriculas;
    private final Clock clock;

    public AcademicoService(
            PessoaRepository pessoas,
            AlunoRepository alunos,
            ColaboradorRepository colaboradores,
            CursoRepository cursos,
            MatriculaRepository matriculas,
            Clock clock) {
        this.pessoas = pessoas;
        this.alunos = alunos;
        this.colaboradores = colaboradores;
        this.cursos = cursos;
        this.matriculas = matriculas;
        this.clock = clock;
    }

    public record PessoaResponse(Long id, String nome, String email) {}

    public record PapelResponse(Long id, Long pessoaId) {}

    public record MatriculaResponse(
            Long id,
            Long alunoId,
            Long cursoId,
            java.math.BigDecimal valor,
            java.time.Instant criadaEm,
            String status) {
        public static MatriculaResponse de(Matricula m) {
            return new MatriculaResponse(
                    m.getId(),
                    m.getAluno().getId(),
                    m.getCurso().getId(),
                    m.getValor(),
                    m.getCriadaEm(),
                    m.getStatus());
        }
    }

    @Transactional
    public PessoaResponse criarPessoa(String nome, String email) {
        Pessoa p = pessoas.save(new Pessoa(nome, email));
        return new PessoaResponse(p.getId(), p.getNome(), p.getEmail());
    }

    @Transactional(readOnly = true)
    public PessoaResponse buscarPessoa(Long id) {
        Pessoa p = obterPessoa(id);
        return new PessoaResponse(p.getId(), p.getNome(), p.getEmail());
    }

    @Transactional
    public PapelResponse criarAluno(Long pessoaId) {
        if (alunos.existsByPessoaId(pessoaId)) throw new ConflitoException("Pessoa já é aluno");
        Aluno a = alunos.save(new Aluno(obterPessoa(pessoaId)));
        return new PapelResponse(a.getId(), pessoaId);
    }

    @Transactional
    public PapelResponse criarColaborador(Long pessoaId, String cargo) {
        if (colaboradores.existsByPessoaId(pessoaId))
            throw new ConflitoException("Pessoa já é colaborador");
        Colaborador c = colaboradores.save(new Colaborador(obterPessoa(pessoaId), cargo));
        return new PapelResponse(c.getId(), pessoaId);
    }

    @Transactional(readOnly = true)
    public PapelResponse buscarAluno(Long id) {
        Aluno a =
                alunos.findById(id)
                        .orElseThrow(
                                () -> new RecursoNaoEncontradoException("Aluno não encontrado"));
        return new PapelResponse(a.getId(), a.getPessoa().getId());
    }

    @Transactional(readOnly = true)
    public PapelResponse buscarColaborador(Long id) {
        Colaborador c =
                colaboradores
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new RecursoNaoEncontradoException(
                                                "Colaborador não encontrado"));
        return new PapelResponse(c.getId(), c.getPessoa().getId());
    }

    @Transactional
    public MatriculaResponse matricular(Long alunoId, Long cursoId) {
        Aluno aluno =
                alunos.findById(alunoId)
                        .orElseThrow(
                                () -> new RecursoNaoEncontradoException("Aluno não encontrado"));
        Curso curso =
                cursos.buscarComLock(cursoId)
                        .orElseThrow(
                                () -> new RecursoNaoEncontradoException("Curso não encontrado"));
        if (matriculas.existsByAlunoIdAndCursoId(alunoId, cursoId))
            throw new ConflitoException("Aluno já possui matrícula neste curso");
        curso.reservarVaga();
        Matricula m = matriculas.saveAndFlush(new Matricula(aluno, curso, clock.instant()));
        return MatriculaResponse.de(m);
    }

    @Transactional(readOnly = true)
    public MatriculaResponse buscarMatricula(Long id) {
        return MatriculaResponse.de(obterMatricula(id));
    }

    @Transactional
    public MatriculaResponse cancelar(Long id) {
        // Lock ordering is always course first, then the enrollment row.
        Matricula referencia = obterMatricula(id);
        cursos.buscarComLock(referencia.getCurso().getId()).orElseThrow();
        Matricula m = matriculas.buscarComLock(id).orElseThrow();
        entityManager.refresh(m);
        m.cancelar();
        return MatriculaResponse.de(m);
    }

    private Pessoa obterPessoa(Long id) {
        return pessoas.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa não encontrada"));
    }

    private Matricula obterMatricula(Long id) {
        return matriculas
                .findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Matrícula não encontrada"));
    }
}
