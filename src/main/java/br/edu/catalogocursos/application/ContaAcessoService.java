package br.edu.catalogocursos.application;

import br.edu.catalogocursos.api.exception.RecursoNaoEncontradoException;
import br.edu.catalogocursos.domain.*;
import br.edu.catalogocursos.repository.*;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class ContaAcessoService {
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final ContaAcessoRepository contas;
    private final PessoaRepository pessoas;
    private final PasswordEncoder encoder;
    private final SessaoRepository sessoes;
    private final AuditoriaService auditoria;

    public ContaAcessoService(
            ContaAcessoRepository contas,
            PessoaRepository pessoas,
            PasswordEncoder encoder,
            SessaoRepository sessoes,
            AuditoriaService auditoria,
            org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.contas = contas;
        this.pessoas = pessoas;
        this.encoder = encoder;
        this.sessoes = sessoes;
        this.auditoria = auditoria;
    }

    public record ContaResponse(
            Long id, Long pessoaId, String login, boolean ativo, Set<PerfilAcesso> perfis) {
        public static ContaResponse de(ContaAcesso c) {
            return new ContaResponse(
                    c.getId(),
                    c.getPessoa().getId(),
                    c.getLogin(),
                    c.isAtivo(),
                    Set.copyOf(c.getPerfis()));
        }
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ContaResponse criar(Long pessoaId, String login, String senha) {
        validarSenha(senha);
        Pessoa p =
                pessoas.findById(pessoaId)
                        .orElseThrow(
                                () -> new RecursoNaoEncontradoException("Pessoa não encontrada"));
        ContaAcesso conta =
                contas.saveAndFlush(
                        new ContaAcesso(
                                p, login, encoder.encode(senha), Set.of(PerfilAcesso.OPERADOR)));
        auditoria.registrar("CONTA_CRIADA", conta.getId(), null, null);
        return ContaResponse.de(conta);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ContaResponse buscarPorId(Long id) {
        return ContaResponse.de(
                contas.findById(id)
                        .orElseThrow(
                                () -> new RecursoNaoEncontradoException("Conta não encontrada")));
    }

    @Transactional(readOnly = true)
    public ContaResponse minhaConta(String login) {
        return ContaResponse.de(porLogin(login));
    }

    @Transactional
    public void alterarSenha(String login, String senhaAtual, String novaSenha) {
        ContaAcesso ref = porLogin(login);
        ContaAcesso c = obter(ref.getId());
        validarSenha(novaSenha);
        if (!encoder.matches(senhaAtual, c.getSenhaHash()))
            throw new CredenciaisInvalidasException();
        c.alterarSenha(encoder.encode(novaSenha));
        sessoes.revogarConta(c.getId());
        auditoria.registrar("SENHA_ALTERADA", c.getId(), null, null);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ContaResponse perfis(Long id, Set<PerfilAcesso> perfis) {
        bloquearAdministracao();
        ContaAcesso c = obter(id);
        protegerUltimoAdministrador(c, c.isAtivo(), perfis);
        c.alterarPerfis(perfis);
        sessoes.revogarConta(id);
        auditoria.registrar("PERFIS_ALTERADOS", id, null, null);
        return ContaResponse.de(c);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ContaResponse status(Long id, boolean ativo) {
        bloquearAdministracao();
        ContaAcesso c = obter(id);
        protegerUltimoAdministrador(c, ativo, c.getPerfis());
        c.alterarStatus(ativo);
        sessoes.revogarConta(id);
        auditoria.registrar("STATUS_ALTERADO", id, null, null);
        return ContaResponse.de(c);
    }

    private void bloquearAdministracao() {
        // A transaction-scoped lock serializes changes to the set of active administrators.
        jdbc.execute("select pg_advisory_xact_lock(2026, 14)");
    }

    private void protegerUltimoAdministrador(
            ContaAcesso conta, boolean novoAtivo, Set<PerfilAcesso> novosPerfis) {
        if (novosPerfis == null || novosPerfis.isEmpty())
            throw new IllegalArgumentException("Informe ao menos um perfil");
        boolean removendo = !novoAtivo || !novosPerfis.contains(PerfilAcesso.ADMINISTRADOR);
        if (conta.isAtivo()
                && conta.getPerfis().contains(PerfilAcesso.ADMINISTRADOR)
                && removendo) {
            long quantidade =
                    jdbc.queryForObject(
                            "select count(*) from conta_acesso c join conta_perfil p on"
                                    + " p.conta_id=c.id where c.ativo=true and"
                                    + " p.perfil='ADMINISTRADOR'",
                            Long.class);
            if (quantidade <= 1)
                throw new ConflitoException("O último administrador ativo deve ser mantido");
        }
    }

    private ContaAcesso obter(Long id) {
        return contas.buscarComLock(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada"));
    }

    private ContaAcesso porLogin(String login) {
        return contas.findByLogin(login).orElseThrow(CredenciaisInvalidasException::new);
    }

    public static void validarSenha(String senha) {
        if (senha == null
                || senha.length() < 12
                || senha.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException(
                    "Senha deve ter ao menos 12 caracteres e até 72 bytes");
    }
}
