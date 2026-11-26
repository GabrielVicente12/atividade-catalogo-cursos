package br.edu.catalogocursos.config;

import br.edu.catalogocursos.application.ContaAcessoService;
import br.edu.catalogocursos.domain.*;
import br.edu.catalogocursos.repository.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
public class AdministradorInicial implements ApplicationRunner {
    private final ContaAcessoRepository contas;
    private final PessoaRepository pessoas;
    private final PasswordEncoder encoder;
    private final boolean enabled;
    private final String login;
    private final String senha;

    public AdministradorInicial(
            ContaAcessoRepository contas,
            PessoaRepository pessoas,
            PasswordEncoder encoder,
            @Value("${app.security.initial-admin.enabled}") boolean enabled,
            @Value("${app.security.initial-admin.login}") String login,
            @Value("${app.security.initial-admin.password}") String senha) {
        this.contas = contas;
        this.pessoas = pessoas;
        this.encoder = encoder;
        this.enabled = enabled;
        this.login = login;
        this.senha = senha;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) return;
        if (contas.count() != 0)
            throw new IllegalStateException(
                    "Desabilite o provisionamento inicial: já existem contas");
        ContaAcessoService.validarSenha(senha);
        if (login.isBlank() || login.length() > 160)
            throw new IllegalArgumentException("Login inicial inválido");
        Pessoa p =
                pessoas.save(
                        new Pessoa(
                                "Administrador inicial", "administrador-inicial@example.invalid"));
        contas.save(
                new ContaAcesso(
                        p,
                        login,
                        encoder.encode(senha),
                        Set.of(PerfilAcesso.OPERADOR, PerfilAcesso.ADMINISTRADOR)));
    }
}
