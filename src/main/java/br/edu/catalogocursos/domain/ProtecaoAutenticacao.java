package br.edu.catalogocursos.domain;

import java.time.*;

@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "protecao_autenticacao")
public class ProtecaoAutenticacao {
    @jakarta.persistence.Id
    @jakarta.persistence.Column(name = "login_hash", length = 64)
    private String loginHash;

    @jakarta.persistence.Column(nullable = false)
    private int falhas;

    @jakarta.persistence.Column(name = "inicio_janela")
    private Instant inicioJanela;

    @jakarta.persistence.Column(name = "bloqueado_ate")
    private Instant bloqueadoAte;

    protected ProtecaoAutenticacao() {}

    public ProtecaoAutenticacao(int falhas, Instant inicioJanela, Instant bloqueadoAte) {
        this.falhas = falhas;
        this.inicioJanela = inicioJanela;
        this.bloqueadoAte = bloqueadoAte;
    }

    public boolean bloqueada(Instant agora) {
        return bloqueadoAte != null && agora.isBefore(bloqueadoAte);
    }

    public void registrarFalha(Instant agora, int maximo, Duration janela, Duration duracao) {
        if (inicioJanela == null || !agora.isBefore(inicioJanela.plus(janela))) {
            falhas = 0;
            inicioJanela = agora;
        }
        falhas++;
        if (falhas >= maximo) bloqueadoAte = agora.plus(duracao);
    }

    public void limpar() {
        falhas = 0;
        inicioJanela = null;
        bloqueadoAte = null;
    }

    public int getFalhas() {
        return falhas;
    }

    public Instant getInicioJanela() {
        return inicioJanela;
    }

    public Instant getBloqueadoAte() {
        return bloqueadoAte;
    }
}
