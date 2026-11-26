package br.edu.catalogocursos.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
public class Matricula {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Aluno aluno;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(nullable = false, length = 20)
    private String status;

    protected Matricula() {}

    public Matricula(Aluno aluno, Curso curso, Instant agora) {
        this.aluno = aluno;
        this.curso = curso;
        this.valor = curso.getPreco();
        this.criadaEm = agora;
        this.status = "CONFIRMADA";
    }

    public void cancelar() {
        if (!"CONFIRMADA".equals(status)) throw new IllegalStateException("Matrícula já cancelada");
        curso.adicionarVagas(1);
        status = "CANCELADA";
    }

    public Long getId() {
        return id;
    }

    public Aluno getAluno() {
        return aluno;
    }

    public Curso getCurso() {
        return curso;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public String getStatus() {
        return status;
    }
}
