package br.edu.catalogocursos.domain;

import jakarta.persistence.*;

import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "sessao_refresh")
public class SessaoRefreshToken {
    @Id private UUID id;

    @Column(name = "familia_id", nullable = false)
    private UUID familia;

    @Column(name = "conta_id", nullable = false)
    private Long contaId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "ultimo_uso", nullable = false)
    private Instant ultimoUso;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(nullable = false)
    private boolean usado;

    @Column(nullable = false)
    private boolean revogada;

    protected SessaoRefreshToken() {}

    public SessaoRefreshToken(
            UUID id,
            UUID familia,
            Long contaId,
            String tokenHash,
            Instant criadaEm,
            Instant ultimoUso,
            Instant expiraEm,
            boolean usado,
            boolean revogada) {
        this.id = id;
        this.familia = familia;
        this.contaId = contaId;
        this.tokenHash = tokenHash;
        this.criadaEm = criadaEm;
        this.ultimoUso = ultimoUso;
        this.expiraEm = expiraEm;
        this.usado = usado;
        this.revogada = revogada;
    }

    public boolean expirada(Instant agora, Duration inatividade) {
        return !agora.isBefore(expiraEm) || !agora.isBefore(ultimoUso.plus(inatividade));
    }

    public UUID id() {
        return id;
    }

    public UUID familia() {
        return familia;
    }

    public Long contaId() {
        return contaId;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public Instant criadaEm() {
        return criadaEm;
    }

    public Instant ultimoUso() {
        return ultimoUso;
    }

    public Instant expiraEm() {
        return expiraEm;
    }

    public boolean usado() {
        return usado;
    }

    public boolean revogada() {
        return revogada;
    }
}
