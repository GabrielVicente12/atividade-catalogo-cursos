package br.edu.catalogocursos.domain;

import jakarta.persistence.*;

import java.util.*;

@Entity
@Table(name = "conta_acesso")
public class ContaAcesso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "pessoa_id", nullable = false, unique = true)
    private Pessoa pessoa;

    @Column(nullable = false, unique = true, length = 160)
    private String login;

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    @Column(nullable = false)
    private boolean ativo = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "conta_perfil", joinColumns = @JoinColumn(name = "conta_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "perfil", nullable = false, length = 30)
    private Set<PerfilAcesso> perfis = new HashSet<>();

    protected ContaAcesso() {}

    public ContaAcesso(Pessoa pessoa, String login, String senhaHash, Set<PerfilAcesso> perfis) {
        this.pessoa = Objects.requireNonNull(pessoa);
        this.login = normalizar(login);
        if (this.login.isBlank() || this.login.length() > 160)
            throw new IllegalArgumentException("Login inválido");
        this.senhaHash = Objects.requireNonNull(senhaHash);
        alterarPerfis(perfis);
    }

    public static String normalizar(String login) {
        return login.strip().toLowerCase(Locale.ROOT);
    }

    public void alterarPerfis(Set<PerfilAcesso> novos) {
        if (novos == null || novos.isEmpty())
            throw new IllegalArgumentException("Informe ao menos um perfil");
        perfis.clear();
        perfis.addAll(novos);
    }

    public void alterarSenha(String hash) {
        senhaHash = Objects.requireNonNull(hash);
    }

    public void alterarStatus(boolean ativo) {
        this.ativo = ativo;
    }

    public Long getId() {
        return id;
    }

    public Pessoa getPessoa() {
        return pessoa;
    }

    public String getLogin() {
        return login;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Set<PerfilAcesso> getPerfis() {
        return Collections.unmodifiableSet(perfis);
    }
}
