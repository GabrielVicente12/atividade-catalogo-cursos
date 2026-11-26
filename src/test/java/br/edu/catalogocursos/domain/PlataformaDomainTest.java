package br.edu.catalogocursos.domain;

import static org.assertj.core.api.Assertions.*;

import br.edu.catalogocursos.application.ContaAcessoService;
import br.edu.catalogocursos.application.TokenHash;
import br.edu.catalogocursos.config.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

class PlataformaDomainTest {
    final Instant agora = Instant.parse("2026-01-01T12:00:00Z");

    @Test
    void cursoPreservaCodigoDataEValorAoReservar() {
        Curso c = new Curso("Java", 40, NivelCurso.INICIANTE);
        String codigo = c.getCodigo();
        c.atualizar("Java avançado", 60, NivelCurso.AVANCADO, new BigDecimal("99.90"), true);
        c.adicionarVagas(2);
        c.reservarVaga();
        c.retirarVagas(1);
        assertThat(c.getCodigo()).isEqualTo(codigo);
        assertThat(c.getVagas()).isZero();
        assertThat(c.getDataCriacao()).isEqualTo(LocalDate.now());
        assertThat(c.getPreco()).isEqualByComparingTo("99.90");
        assertThatThrownBy(c::reservarVaga).isInstanceOf(IllegalStateException.class);
        c.adicionarVagas(1);
        c.alterarStatus(false);
        assertThatThrownBy(c::reservarVaga).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejeitaMovimentacoesNaoPositivas(int quantidade) {
        Curso c = new Curso("Java", 40, NivelCurso.INICIANTE);
        assertThatThrownBy(() -> c.adicionarVagas(quantidade))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> c.retirarVagas(quantidade))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejeitaRetiradaAcimaDaDisponibilidade() {
        Curso c = new Curso("Java", 40, NivelCurso.INICIANTE);
        c.adicionarVagas(1);
        assertThatThrownBy(() -> c.retirarVagas(2)).isInstanceOf(IllegalStateException.class);
        assertThat(c.getVagas()).isEqualTo(1);
    }

    @Test
    void cursoRejeitaCamposInvalidos() {
        Curso c = new Curso("Java", 40, NivelCurso.INICIANTE);
        for (String titulo : Arrays.asList(null, "", "A", "x".repeat(151)))
            assertThatThrownBy(
                            () ->
                                    c.atualizar(
                                            titulo,
                                            40,
                                            NivelCurso.INICIANTE,
                                            BigDecimal.ZERO,
                                            true))
                    .isInstanceOf(IllegalArgumentException.class);
        for (Integer carga : Arrays.asList(null, 0, 401))
            assertThatThrownBy(
                            () ->
                                    c.atualizar(
                                            "Java",
                                            carga,
                                            NivelCurso.INICIANTE,
                                            BigDecimal.ZERO,
                                            true))
                    .isInstanceOf(IllegalArgumentException.class);
        for (BigDecimal preco : Arrays.asList(null, new BigDecimal("-1"), new BigDecimal("1.111")))
            assertThatThrownBy(() -> c.atualizar("Java", 40, NivelCurso.INICIANTE, preco, true))
                    .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> c.atualizar("Java", 40, null, BigDecimal.ZERO, true))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void instrutorValidaNomeEProtegeColecao() {
        for (String nome : Arrays.asList(null, "", "A", "x".repeat(121)))
            assertThatThrownBy(() -> new Instrutor(nome, "teste@example.com"))
                    .isInstanceOf(IllegalArgumentException.class);
        Instrutor i = new Instrutor("Marina", "marina@example.com");
        assertThatThrownBy(() -> i.getCursos().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void contaNormalizaLoginEProtegePerfis() {
        var pessoa = new Pessoa("Pessoa", "p@example.com");
        var conta = new ContaAcesso(pessoa, " LOGIN ", "hash", Set.of(PerfilAcesso.OPERADOR));
        assertThat(conta.getLogin()).isEqualTo("login");
        assertThat(conta.isAtivo()).isTrue();
        assertThatThrownBy(() -> conta.getPerfis().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> conta.alterarPerfis(Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> conta.alterarPerfis(null))
                .isInstanceOf(IllegalArgumentException.class);
        conta.alterarPerfis(Set.of(PerfilAcesso.ADMINISTRADOR));
        conta.alterarStatus(false);
        conta.alterarSenha("novoHash");
        assertThat(conta.isAtivo()).isFalse();
        assertThat(conta.getSenhaHash()).isEqualTo("novoHash");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "curta", "12345678901"})
    void senhaCurtaRejeitada(String senha) {
        assertThatThrownBy(() -> ContaAcessoService.validarSenha(senha))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void senhaValidaLimiteEmBytes() {
        assertThatThrownBy(() -> ContaAcessoService.validarSenha(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ContaAcessoService.validarSenha("á".repeat(37)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> ContaAcessoService.validarSenha("TesteSeguro123!"))
                .doesNotThrowAnyException();
    }

    @Test
    void protecaoBloqueiaNaQuintaFalhaESoltaNoPrazo() {
        var p = new ProtecaoAutenticacao(0, null, null);
        for (int i = 0; i < 4; i++)
            p.registrarFalha(agora, 5, Duration.ofMinutes(15), Duration.ofMinutes(15));
        assertThat(p.bloqueada(agora)).isFalse();
        p.registrarFalha(agora, 5, Duration.ofMinutes(15), Duration.ofMinutes(15));
        assertThat(p.bloqueada(agora.plusSeconds(899))).isTrue();
        assertThat(p.bloqueada(agora.plusSeconds(900))).isFalse();
        p.limpar();
        assertThat(p.getFalhas()).isZero();
        assertThat(p.getInicioJanela()).isNull();
    }

    @Test
    void janelaDeFalhasReiniciaNoLimite() {
        var p = new ProtecaoAutenticacao(4, agora, null);
        p.registrarFalha(agora.plusSeconds(900), 5, Duration.ofMinutes(15), Duration.ofMinutes(15));
        assertThat(p.getFalhas()).isEqualTo(1);
        assertThat(p.getBloqueadoAte()).isNull();
    }

    @Test
    void sessaoExpiraPorInatividadeOuLimiteAbsoluto() {
        var s =
                new SessaoRefreshToken(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        1L,
                        "hash",
                        agora,
                        agora,
                        agora.plusSeconds(3600),
                        false,
                        false);
        assertThat(s.expirada(agora.plusSeconds(899), Duration.ofMinutes(15))).isFalse();
        assertThat(s.expirada(agora.plusSeconds(900), Duration.ofMinutes(15))).isTrue();
        assertThat(s.expirada(agora.plusSeconds(3600), Duration.ofDays(1))).isTrue();
    }

    @Test
    void hashDeterministicoETokenImprevisivel() {
        assertThat(TokenHash.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        assertThat(TokenHash.gerar()).hasSize(43).isNotEqualTo(TokenHash.gerar());
    }

    @Test
    void configuracoesRejeitamDuracoesInvalidas() {
        assertThatThrownBy(() -> new RefreshProperties(null, Duration.ofDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RefreshProperties(Duration.ZERO, Duration.ofDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RefreshProperties(Duration.ofSeconds(-1), Duration.ofDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RefreshProperties(Duration.ofMinutes(1), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () -> new RefreshProperties(Duration.ofMinutes(1), Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                new ProtecaoProperties(
                                        0, Duration.ofMinutes(1), Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class);
        for (Duration d : Arrays.asList(null, Duration.ZERO, Duration.ofSeconds(-1))) {
            assertThatThrownBy(() -> new ProtecaoProperties(5, d, Duration.ofMinutes(1)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new ProtecaoProperties(5, Duration.ofMinutes(1), d))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new JwtProperties("teste", d, "abc"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> new JwtProperties(null, Duration.ofMinutes(1), "abc"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtProperties("", Duration.ofMinutes(1), "abc"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
