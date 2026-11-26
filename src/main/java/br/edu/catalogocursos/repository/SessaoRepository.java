package br.edu.catalogocursos.repository;

import br.edu.catalogocursos.domain.SessaoRefreshToken;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.*;

@Repository
public class SessaoRepository {
    private final JdbcTemplate jdbc;

    public SessaoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<SessaoRefreshToken> buscarComLock(String hash) {
        return jdbc
                .query(
                        "select * from sessao_refresh where token_hash=? for update",
                        (r, n) ->
                                new SessaoRefreshToken(
                                        r.getObject("id", UUID.class),
                                        r.getObject("familia_id", UUID.class),
                                        r.getLong("conta_id"),
                                        r.getString("token_hash"),
                                        r.getTimestamp("criada_em").toInstant(),
                                        r.getTimestamp("ultimo_uso").toInstant(),
                                        r.getTimestamp("expira_em").toInstant(),
                                        r.getBoolean("usado"),
                                        r.getBoolean("revogada")),
                        hash)
                .stream()
                .findFirst();
    }

    public void salvar(SessaoRefreshToken s) {
        jdbc.update(
                "insert into"
                    + " sessao_refresh(id,familia_id,conta_id,token_hash,criada_em,ultimo_uso,expira_em,usado,revogada)"
                    + " values (?,?,?,?,?,?,?,?,?)",
                s.id(),
                s.familia(),
                s.contaId(),
                s.tokenHash(),
                Timestamp.from(s.criadaEm()),
                Timestamp.from(s.ultimoUso()),
                Timestamp.from(s.expiraEm()),
                s.usado(),
                s.revogada());
    }

    public void usar(UUID id) {
        jdbc.update("update sessao_refresh set usado=true where id=?", id);
    }

    public void revogarFamilia(UUID familia) {
        jdbc.update("update sessao_refresh set revogada=true where familia_id=?", familia);
    }

    public void revogarConta(Long id) {
        jdbc.update("update sessao_refresh set revogada=true where conta_id=?", id);
    }
}
