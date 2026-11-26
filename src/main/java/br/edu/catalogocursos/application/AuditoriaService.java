package br.edu.catalogocursos.application;

import br.edu.catalogocursos.api.dto.PaginaResponse;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.*;

@Service
public class AuditoriaService {
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public AuditoriaService(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    public void registrar(String tipo, Long contaId, String loginHash, String origem) {
        String limpa = origem == null ? null : origem.replaceAll("[\r\n\t]", " ");
        if (limpa != null && limpa.length() > 100) limpa = limpa.substring(0, 100);
        jdbc.update(
                "insert into"
                    + " evento_seguranca(tipo,conta_id,login_hash,origem,criado_em,ator_conta_id)"
                    + " values (?,?,?,?,?,?)",
                tipo,
                contaId,
                loginHash,
                limpa,
                Timestamp.from(clock.instant()),
                ator(contaId));
    }

    private Long ator(Long alvo) {
        var authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext()
                        .getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return alvo;
        var ids =
                jdbc.query(
                        "select id from conta_acesso where login=?",
                        (r, n) -> r.getLong(1),
                        authentication.getName());
        return ids.isEmpty() ? alvo : ids.getFirst();
    }

    public record EventoResponse(
            Long id,
            String tipo,
            Long contaId,
            String loginHash,
            String origem,
            Instant criadoEm,
            Long atorContaId) {}

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PaginaResponse<EventoResponse> listar(String tipo, int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > 100)
            throw new IllegalArgumentException("Paginação inválida");
        String filtro = tipo == null ? "" : " where tipo = ?";
        Object[] countArgs = tipo == null ? new Object[] {} : new Object[] {tipo};
        long total =
                jdbc.queryForObject(
                        "select count(*) from evento_seguranca" + filtro, Long.class, countArgs);
        Object[] args =
                tipo == null
                        ? new Object[] {tamanho, (long) pagina * tamanho}
                        : new Object[] {tipo, tamanho, (long) pagina * tamanho};
        var lista =
                jdbc.query(
                        "select * from evento_seguranca"
                                + filtro
                                + " order by criado_em desc,id desc limit ? offset ?",
                        (r, n) ->
                                new EventoResponse(
                                        r.getLong("id"),
                                        r.getString("tipo"),
                                        r.getObject("conta_id", Long.class),
                                        r.getString("login_hash"),
                                        r.getString("origem"),
                                        r.getTimestamp("criado_em").toInstant(),
                                        r.getObject("ator_conta_id", Long.class)),
                        args);
        return new PaginaResponse<>(
                lista, pagina, tamanho, total, (int) ((total + tamanho - 1) / tamanho));
    }
}
