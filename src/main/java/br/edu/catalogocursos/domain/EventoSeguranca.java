package br.edu.catalogocursos.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "evento_seguranca")
public class EventoSeguranca {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String tipo;

    @Column(name = "conta_id")
    private Long contaId;

    @Column(name = "ator_conta_id")
    private Long atorContaId;

    @Column(name = "login_hash", length = 64)
    private String loginHash;

    @Column(length = 100)
    private String origem;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected EventoSeguranca() {}
}
