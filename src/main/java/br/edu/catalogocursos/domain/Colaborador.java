package br.edu.catalogocursos.domain;

import jakarta.persistence.*;

@Entity
public class Colaborador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "pessoa_id", nullable = false, unique = true)
    private Pessoa pessoa;

    @Column(nullable = false, length = 100)
    private String cargo;

    protected Colaborador() {}

    public Colaborador(Pessoa pessoa, String cargo) {
        this.pessoa = java.util.Objects.requireNonNull(pessoa);
        this.cargo = cargo;
    }

    public Long getId() {
        return id;
    }

    public Pessoa getPessoa() {
        return pessoa;
    }

    public String getCargo() {
        return cargo;
    }
}
