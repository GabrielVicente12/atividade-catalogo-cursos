package br.edu.catalogocursos.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.edu.catalogocursos.domain.*;
import br.edu.catalogocursos.repository.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Base64;

class ConfiguracaoTest {
    @Test
    void referenciaSoAceitaBancoDescartavel() {
        var guard = new SchemaReferenceGuard();
        var env = new MockEnvironment();
        assertThatCode(() -> guard.postProcessEnvironment(env, null)).doesNotThrowAnyException();
        env.setActiveProfiles("schema-reference");
        assertThatThrownBy(() -> guard.postProcessEnvironment(env, null))
                .isInstanceOf(IllegalStateException.class);
        env.setProperty("DB_REFERENCE_URL", "jdbc:postgresql://localhost:5432/catalogocursos_dev");
        assertThatThrownBy(() -> guard.postProcessEnvironment(env, null))
                .isInstanceOf(IllegalStateException.class);
        env.setProperty(
                "DB_REFERENCE_URL", "jdbc:postgresql://localhost:5432/catalogocursos_reference");
        assertThatCode(() -> guard.postProcessEnvironment(env, null)).doesNotThrowAnyException();
    }

    @Test
    void chaveJwtRejeitaMaterialInsuficiente() {
        var config = new SecurityConfig();
        assertThatThrownBy(
                        () ->
                                config.secretKey(
                                        new JwtProperties(
                                                "teste", Duration.ofMinutes(1), "invalido**")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                config.secretKey(
                                        new JwtProperties(
                                                "teste",
                                                Duration.ofMinutes(1),
                                                Base64.getEncoder().encodeToString(new byte[16]))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void administradorInicialExigeHabilitacaoEBaseSemContas() throws Exception {
        var contas = mock(ContaAcessoRepository.class);
        var pessoas = mock(PessoaRepository.class);
        var encoder = mock(PasswordEncoder.class);
        new AdministradorInicial(contas, pessoas, encoder, false, "", "").run(null);
        verifyNoInteractions(contas, pessoas, encoder);
        when(contas.count()).thenReturn(1L);
        assertThatThrownBy(
                        () ->
                                new AdministradorInicial(
                                                contas,
                                                pessoas,
                                                encoder,
                                                true,
                                                "admin",
                                                "SenhaDeTeste!123")
                                        .run(null))
                .isInstanceOf(IllegalStateException.class);
        when(contas.count()).thenReturn(0L);
        assertThatThrownBy(
                        () ->
                                new AdministradorInicial(
                                                contas,
                                                pessoas,
                                                encoder,
                                                true,
                                                "",
                                                "SenhaDeTeste!123")
                                        .run(null))
                .isInstanceOf(IllegalArgumentException.class);
        Pessoa pessoa = new Pessoa("Admin Teste", "admin@example.com");
        when(pessoas.save(any())).thenReturn(pessoa);
        when(encoder.encode(any())).thenReturn("hash");
        new AdministradorInicial(contas, pessoas, encoder, true, "admin", "SenhaDeTeste!123")
                .run(null);
        verify(contas)
                .save(
                        argThat(
                                c ->
                                        c.getPerfis().contains(PerfilAcesso.ADMINISTRADOR)
                                                && c.getPerfis().contains(PerfilAcesso.OPERADOR)
                                                && c.getSenhaHash().equals("hash")));
    }
}
