package br.edu.catalogocursos.application;

import br.edu.catalogocursos.api.dto.TokenResponse;
import br.edu.catalogocursos.config.*;
import br.edu.catalogocursos.domain.*;
import br.edu.catalogocursos.repository.*;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.*;
import java.util.*;

@Service
public class AutenticacaoService {
    private final ContaAcessoRepository contas;
    private final SessaoRepository sessoes;
    private final AuditoriaService auditoria;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwt;
    private final JwtProperties jwtProperties;
    private final RefreshProperties refreshProperties;
    private final ProtecaoProperties protecao;
    private final Clock clock;
    private final String hashFicticio;

    public AutenticacaoService(
            ContaAcessoRepository contas,
            SessaoRepository sessoes,
            AuditoriaService auditoria,
            JdbcTemplate jdbc,
            PasswordEncoder encoder,
            JwtEncoder jwt,
            JwtProperties jwtProperties,
            RefreshProperties refreshProperties,
            ProtecaoProperties protecao,
            Clock clock) {
        this.contas = contas;
        this.sessoes = sessoes;
        this.auditoria = auditoria;
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.jwt = jwt;
        this.jwtProperties = jwtProperties;
        this.refreshProperties = refreshProperties;
        this.protecao = protecao;
        this.clock = clock;
        this.hashFicticio = encoder.encode(TokenHash.gerar());
    }

    @Transactional(noRollbackFor = CredenciaisInvalidasException.class)
    public TokenResponse login(String login, String senha, String origem) {
        String normalizado = ContaAcesso.normalizar(login);
        String hash = TokenHash.hash(normalizado);
        jdbc.update(
                "insert into protecao_autenticacao(login_hash) values (?) on conflict do nothing",
                hash);
        ProtecaoAutenticacao estado =
                jdbc.queryForObject(
                        "select * from protecao_autenticacao where login_hash=? for update",
                        (r, n) ->
                                new ProtecaoAutenticacao(
                                        r.getInt("falhas"),
                                        instante(r.getTimestamp("inicio_janela")),
                                        instante(r.getTimestamp("bloqueado_ate"))),
                        hash);
        Instant agora = clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        if (estado.bloqueada(agora)) {
            auditoria.registrar("LOGIN_BLOQUEADO", null, hash, origem);
            throw new CredenciaisInvalidasException();
        }
        ContaAcesso conta = contas.findByLogin(normalizado).orElse(null);
        if (conta != null) conta = contas.buscarComLock(conta.getId()).orElseThrow();
        boolean confere =
                encoder.matches(senha, conta == null ? hashFicticio : conta.getSenhaHash());
        if (conta == null || !confere || !conta.isAtivo()) {
            estado.registrarFalha(
                    agora,
                    protecao.maxFailedAttempts(),
                    protecao.failureWindow(),
                    protecao.lockDuration());
            salvarProtecao(hash, estado);
            auditoria.registrar("LOGIN_FALHOU", null, hash, origem);
            throw new CredenciaisInvalidasException();
        }
        estado.limpar();
        salvarProtecao(hash, estado);
        auditoria.registrar("LOGIN_SUCESSO", conta.getId(), hash, origem);
        return emitir(
                conta,
                UUID.randomUUID(),
                agora,
                agora.plus(refreshProperties.absoluteExpiration()));
    }

    @Transactional(noRollbackFor = CredenciaisInvalidasException.class)
    public TokenResponse renovar(String token) {
        String hash = TokenHash.hash(token);
        List<Long> ids =
                jdbc.query(
                        "select conta_id from sessao_refresh where token_hash=?",
                        (r, n) -> r.getLong(1),
                        hash);
        if (ids.isEmpty()) throw new CredenciaisInvalidasException();
        ContaAcesso conta =
                contas.buscarComLock(ids.getFirst())
                        .orElseThrow(CredenciaisInvalidasException::new);
        SessaoRefreshToken sessao =
                sessoes.buscarComLock(hash).orElseThrow(CredenciaisInvalidasException::new);
        Instant agora = clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        if (sessao.usado()
                || sessao.revogada()
                || sessao.expirada(agora, refreshProperties.idleTimeout())
                || !conta.isAtivo()) {
            sessoes.revogarFamilia(sessao.familia());
            auditoria.registrar("REFRESH_REJEITADO", conta.getId(), null, null);
            throw new CredenciaisInvalidasException();
        }
        sessoes.usar(sessao.id());
        auditoria.registrar("REFRESH_SUCESSO", conta.getId(), null, null);
        return emitir(conta, sessao.familia(), agora, sessao.expiraEm());
    }

    @Transactional
    public void logout(String token) {
        String hash = TokenHash.hash(token);
        List<Long> ids =
                jdbc.query(
                        "select conta_id from sessao_refresh where token_hash=?",
                        (r, n) -> r.getLong(1),
                        hash);
        if (ids.isEmpty()) return;
        ContaAcesso conta = contas.buscarComLock(ids.getFirst()).orElseThrow();
        sessoes.buscarComLock(hash).ifPresent(s -> sessoes.revogarFamilia(s.familia()));
        auditoria.registrar("LOGOUT", conta.getId(), null, null);
    }

    @Transactional
    public void logoutTodos(String login) {
        ContaAcesso conta =
                contas.findByLogin(login).orElseThrow(CredenciaisInvalidasException::new);
        contas.buscarComLock(conta.getId()).orElseThrow();
        sessoes.revogarConta(conta.getId());
        auditoria.registrar("LOGOUT_TODOS", conta.getId(), null, null);
    }

    private TokenResponse emitir(ContaAcesso conta, UUID familia, Instant agora, Instant limite) {
        String refresh = TokenHash.gerar();
        sessoes.salvar(
                new SessaoRefreshToken(
                        UUID.randomUUID(),
                        familia,
                        conta.getId(),
                        TokenHash.hash(refresh),
                        agora,
                        agora,
                        limite,
                        false,
                        false));
        Instant exp = agora.plus(jwtProperties.expiration());
        var claims =
                JwtClaimsSet.builder()
                        .issuer(jwtProperties.issuer())
                        .subject(conta.getLogin())
                        .issuedAt(agora)
                        .expiresAt(exp)
                        .claim(
                                "perfis",
                                conta.getPerfis().stream().map(Enum::name).sorted().toList())
                        .claim("sid", familia.toString())
                        .build();
        String acesso =
                jwt.encode(
                                JwtEncoderParameters.from(
                                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                        .getTokenValue();
        return new TokenResponse(
                acesso, "Bearer", jwtProperties.expiration().toSeconds(), exp, refresh, limite);
    }

    private void salvarProtecao(String hash, ProtecaoAutenticacao p) {
        jdbc.update(
                "update protecao_autenticacao set falhas=?,inicio_janela=?,bloqueado_ate=? where"
                        + " login_hash=?",
                p.getFalhas(),
                timestamp(p.getInicioJanela()),
                timestamp(p.getBloqueadoAte()),
                hash);
    }

    private static Timestamp timestamp(Instant i) {
        return i == null ? null : Timestamp.from(i);
    }

    private static Instant instante(Timestamp t) {
        return t == null ? null : t.toInstant();
    }
}
