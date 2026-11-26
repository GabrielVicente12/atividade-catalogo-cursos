package br.edu.catalogocursos.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "curso")
public class Curso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(name = "carga_horaria", nullable = false)
    private Integer cargaHoraria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NivelCurso nivel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrutor_id", nullable = false)
    private Instrutor instrutor;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo = java.util.UUID.randomUUID().toString();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco = BigDecimal.ZERO;

    @Column(nullable = false)
    private int vagas;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "data_criacao", nullable = false)
    private LocalDate dataCriacao = LocalDate.now();

    protected Curso() {}

    public void atualizar(
            String titulo,
            Integer cargaHoraria,
            NivelCurso nivel,
            BigDecimal preco,
            boolean ativo) {
        if (titulo == null || titulo.isBlank() || titulo.length() < 3 || titulo.length() > 150)
            throw new IllegalArgumentException("Título deve ter de 3 a 150 caracteres");
        if (cargaHoraria == null || cargaHoraria < 1 || cargaHoraria > 400)
            throw new IllegalArgumentException("Carga horária deve ficar entre 1 e 400");
        if (preco == null || preco.signum() < 0 || preco.scale() > 2)
            throw new IllegalArgumentException("Preço inválido");
        this.titulo = titulo;
        this.cargaHoraria = cargaHoraria;
        this.nivel = Objects.requireNonNull(nivel);
        this.preco = preco;
        this.ativo = ativo;
    }

    public void adicionarVagas(int quantidade) {
        if (quantidade < 1) throw new IllegalArgumentException("Quantidade deve ser positiva");
        if (quantidade > Integer.MAX_VALUE - vagas) {
            throw new IllegalArgumentException("Quantidade supera o limite de vagas");
        }
        vagas += quantidade;
    }

    public void retirarVagas(int quantidade) {
        if (quantidade < 1) throw new IllegalArgumentException("Quantidade deve ser positiva");
        if (quantidade > vagas)
            throw new IllegalStateException("Quantidade supera as vagas disponíveis");
        vagas -= quantidade;
    }

    public void alterarStatus(boolean ativo) {
        this.ativo = ativo;
    }

    public void reservarVaga() {
        if (!ativo || vagas < 1) throw new IllegalStateException("Curso inativo ou sem vagas");
        vagas--;
    }

    public String getCodigo() {
        return codigo;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public int getVagas() {
        return vagas;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public LocalDate getDataCriacao() {
        return dataCriacao;
    }

    public Curso(String titulo, Integer cargaHoraria, NivelCurso nivel) {
        atualizar(titulo, cargaHoraria, nivel, BigDecimal.ZERO, true);
    }

    void vincularInstrutor(Instrutor instrutor) {
        Objects.requireNonNull(instrutor, "Instrutor é obrigatório");
        if (this.instrutor != null && this.instrutor != instrutor) {
            throw new IllegalStateException("O curso já possui um instrutor");
        }
        this.instrutor = instrutor;
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public Integer getCargaHoraria() {
        return cargaHoraria;
    }

    public NivelCurso getNivel() {
        return nivel;
    }

    public Instrutor getInstrutor() {
        return instrutor;
    }
}
