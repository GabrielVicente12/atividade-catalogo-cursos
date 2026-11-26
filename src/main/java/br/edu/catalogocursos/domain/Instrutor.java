package br.edu.catalogocursos.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "instrutor")
public class Instrutor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 160)
    private String email;

    @OneToMany(mappedBy = "instrutor", fetch = FetchType.LAZY)
    private List<Curso> cursos = new ArrayList<>();

    protected Instrutor() {}

    public Instrutor(String nome, String email) {
        atualizar(nome, email);
    }

    public void adicionarCurso(Curso curso) {
        Objects.requireNonNull(curso, "Curso é obrigatório");
        curso.vincularInstrutor(this);
        if (!cursos.contains(curso)) {
            cursos.add(curso);
        }
    }

    public void atualizar(String nome, String email) {
        if (nome == null || nome.isBlank() || nome.length() < 3 || nome.length() > 120)
            throw new IllegalArgumentException("Nome deve ter entre 3 e 120 caracteres");
        this.nome = nome;
        this.email = Objects.requireNonNull(email);
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public List<Curso> getCursos() {
        return Collections.unmodifiableList(cursos);
    }
}
