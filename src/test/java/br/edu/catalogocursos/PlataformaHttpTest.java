package br.edu.catalogocursos;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.edu.catalogocursos.application.*;
import br.edu.catalogocursos.domain.*;
import br.edu.catalogocursos.repository.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "TESTAR_POSTGRES", matches = "true")
class PlataformaHttpTest {
    static final String SENHA = "SenhaSomenteTeste!2026";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired ContaAcessoRepository contas;
    @Autowired PessoaRepository pessoas;
    @Autowired AlunoRepository alunos;
    @Autowired CursoRepository cursos;

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    MatriculaRepository matriculas;

    @Autowired InstrutorRepository instrutores;
    @Autowired PasswordEncoder encoder;
    @Autowired AcademicoService academico;
    @Autowired AutenticacaoService autenticacao;
    @Autowired Relogio relogio;
    String admin;
    Long adminId;

    static class Relogio extends Clock {
        volatile Instant agora = Instant.now();

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }

    @TestConfiguration
    static class TempoConfig {
        @Bean
        @Primary
        Relogio relogio() {
            return new Relogio();
        }
    }

    @BeforeEach
    void preparar() throws Exception {
        relogio.agora = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        jdbc.execute(
                "TRUNCATE evento_seguranca, protecao_autenticacao, sessao_refresh, conta_perfil,"
                        + " conta_acesso, matricula, aluno, colaborador, pessoa, curso, instrutor"
                        + " RESTART IDENTITY CASCADE");
        Pessoa pessoa = pessoas.save(new Pessoa("Admin Teste", "admin@example.com"));
        var conta =
                contas.save(
                        new ContaAcesso(
                                pessoa,
                                "admin",
                                encoder.encode(SENHA),
                                Set.of(PerfilAcesso.ADMINISTRADOR, PerfilAcesso.OPERADOR)));
        adminId = conta.getId();
        admin = login("admin", SENHA).get("accessToken").asText();
    }

    JsonNode login(String login, String senha) throws Exception {
        return enviar(
                "POST",
                "/api/autenticacao/login",
                Map.of("login", login, "senha", senha),
                null,
                200);
    }

    JsonNode enviar(String metodo, String path, Object corpo, String token, int esperado)
            throws Exception {
        var builder =
                switch (metodo) {
                    case "POST" -> post(path);
                    case "PUT" -> put(path);
                    case "DELETE" -> delete(path);
                    default -> get(path);
                };
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (corpo != null)
            builder.contentType("application/json").content(json.writeValueAsString(corpo));
        var resposta =
                mvc.perform(builder).andExpect(status().is(esperado)).andReturn().getResponse();
        return resposta.getContentAsByteArray().length == 0
                ? json.createObjectNode()
                : json.readTree(resposta.getContentAsByteArray());
    }

    Long instrutor() throws Exception {
        return enviar(
                        "POST",
                        "/api/instrutores",
                        Map.of("nome", "Marina Costa", "email", "marina@example.com"),
                        admin,
                        201)
                .get("id")
                .asLong();
    }

    Long curso(Long instrutor, String titulo) throws Exception {
        return enviar(
                        "POST",
                        "/api/cursos",
                        Map.of(
                                "titulo",
                                titulo,
                                "cargaHoraria",
                                40,
                                "nivel",
                                "INICIANTE",
                                "instrutorId",
                                instrutor),
                        admin,
                        201)
                .get("id")
                .asLong();
    }

    Long pessoa(String email) throws Exception {
        return enviar(
                        "POST",
                        "/api/pessoas",
                        Map.of("nome", "Pessoa Teste", "email", email),
                        admin,
                        201)
                .get("id")
                .asLong();
    }

    Long aluno(String email) throws Exception {
        return enviar("POST", "/api/alunos", Map.of("pessoaId", pessoa(email)), admin, 201)
                .get("id")
                .asLong();
    }

    JsonNode atualizar(Long id, String titulo, int preco, boolean ativo) throws Exception {
        return enviar(
                "PUT",
                "/api/cursos/" + id,
                Map.of(
                        "titulo",
                        titulo,
                        "cargaHoraria",
                        40,
                        "nivel",
                        "INTERMEDIARIO",
                        "preco",
                        preco,
                        "ativo",
                        ativo),
                admin,
                200);
    }

    Long operador() throws Exception {
        return enviar(
                        "POST",
                        "/api/contas-acesso",
                        Map.of(
                                "pessoaId",
                                pessoa("operador@example.com"),
                                "login",
                                "operador",
                                "senha",
                                SENHA,
                                "perfis",
                                List.of("OPERADOR")),
                        admin,
                        201)
                .get("id")
                .asLong();
    }

    @Test
    void crudCatalogoFiltrosErrosELocation() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
        mvc.perform(
                        post("/api/instrutores")
                                .header("Authorization", "Bearer " + admin)
                                .contentType("application/json")
                                .content(
                                        json.writeValueAsString(
                                                Map.of(
                                                        "nome",
                                                        "Marina Costa",
                                                        "email",
                                                        "marina@example.com"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/instrutores/1"));
        Long id = 1L;
        enviar(
                "POST",
                "/api/instrutores",
                Map.of("nome", "Marina Costa", "email", "marina@example.com"),
                admin,
                409);
        enviar("POST", "/api/instrutores", Map.of("nome", "A", "email", "invalido"), admin, 400);
        enviar("GET", "/api/instrutores/999", null, admin, 404);
        enviar("GET", "/api/instrutores?pagina=-1", null, admin, 400);
        enviar("GET", "/api/instrutores?pagina=0&tamanho=101", null, admin, 400);
        assertThat(
                        enviar("GET", "/api/instrutores", null, admin, 200)
                                .get("totalElementos")
                                .asInt())
                .isEqualTo(1);
        enviar(
                "PUT",
                "/api/instrutores/1",
                Map.of("nome", "Marina Atualizada", "email", "nova@example.com"),
                admin,
                200);
        Long c = curso(id, "Zoologia");
        Long outro = curso(id, "Algoritmos");
        enviar("GET", "/api/cursos/" + c, null, admin, 200);
        enviar("GET", "/api/cursos/999", null, admin, 404);
        atualizar(c, "Zoologia Atualizada", 99, true);
        var filtro =
                enviar(
                        "GET",
                        "/api/cursos?titulo=zoologia&nivel=INTERMEDIARIO&ativo=true",
                        null,
                        admin,
                        200);
        assertThat(filtro.get("totalElementos").asInt()).isEqualTo(1);
        assertThat(filtro.get("conteudo").get(0).get("preco").asInt()).isEqualTo(99);
        enviar("GET", "/api/cursos?pagina=-1", null, admin, 400);
        enviar("GET", "/api/cursos?tamanho=0", null, admin, 400);
        enviar("POST", "/api/cursos/" + c + "/vagas", Map.of("quantidade", 2), admin, 200);
        enviar("POST", "/api/cursos/" + c + "/vagas", Map.of("quantidade", 0), admin, 400);
        enviar("PUT", "/api/cursos/" + c, Map.of("titulo", "A"), admin, 400);
        enviar(
                "POST",
                "/api/cursos",
                Map.of(
                        "titulo",
                        "Teste",
                        "cargaHoraria",
                        40,
                        "nivel",
                        "INICIANTE",
                        "instrutorId",
                        999),
                admin,
                404);
        enviar(
                "POST",
                "/api/cursos",
                Map.of(
                        "titulo",
                        "Teste",
                        "cargaHoraria",
                        401,
                        "nivel",
                        "INVALIDO",
                        "instrutorId",
                        id),
                admin,
                400);
        enviar("GET", "/api/cursos/abc", null, admin, 400);
        var lista = enviar("GET", "/api/instrutores/1/cursos", null, admin, 200);
        assertThat(lista.get(0).get("titulo").asText()).isEqualTo("Algoritmos");
        enviar("DELETE", "/api/instrutores/1", null, admin, 409);
        enviar("DELETE", "/api/cursos/" + c, null, admin, 204);
        enviar("DELETE", "/api/cursos/" + outro, null, admin, 204);
        enviar("DELETE", "/api/instrutores/1", null, admin, 204);
        enviar("GET", "/api/instrutores/1/cursos", null, admin, 404);
        enviar("DELETE", "/api/cursos/999", null, admin, 404);
    }

    @Test
    void matriculaMantemPrecoHistoricoCancelaEDevolveVaga() throws Exception {
        Long c = curso(instrutor(), "Curso Java");
        Long a = aluno("aluno@example.com");
        atualizar(c, "Curso Java", 120, true);
        enviar("POST", "/api/cursos/" + c + "/vagas", Map.of("quantidade", 1), admin, 200);
        var m = enviar("POST", "/api/matriculas", Map.of("alunoId", a, "cursoId", c), admin, 201);
        Long id = m.get("id").asLong();
        assertThat(m.get("valor").asInt()).isEqualTo(120);
        atualizar(c, "Curso Java", 200, true);
        assertThat(enviar("GET", "/api/matriculas/" + id, null, admin, 200).get("valor").asInt())
                .isEqualTo(120);
        enviar("POST", "/api/matriculas", Map.of("alunoId", a, "cursoId", c), admin, 409);
        enviar("DELETE", "/api/cursos/" + c, null, admin, 409);
        enviar("POST", "/api/matriculas/" + id + "/cancelamento", Map.of(), admin, 200);
        enviar("POST", "/api/matriculas/" + id + "/cancelamento", Map.of(), admin, 409);
        assertThat(cursos.findById(c).orElseThrow().getVagas()).isEqualTo(1);
        Long pessoa = pessoa("ambos@example.com");
        enviar("POST", "/api/alunos", Map.of("pessoaId", pessoa), admin, 201);
        enviar(
                "POST",
                "/api/colaboradores",
                Map.of("pessoaId", pessoa, "cargo", "Atendimento"),
                admin,
                201);
        enviar("POST", "/api/alunos", Map.of("pessoaId", pessoa), admin, 409);
        enviar(
                "POST",
                "/api/colaboradores",
                Map.of("pessoaId", pessoa, "cargo", "Atendimento"),
                admin,
                409);
        enviar("GET", "/api/pessoas/" + pessoa, null, admin, 200);
        enviar("GET", "/api/pessoas/999", null, admin, 404);
        enviar("GET", "/api/matriculas/999", null, admin, 404);
        enviar("POST", "/api/matriculas", Map.of("alunoId", 999, "cursoId", c), admin, 404);
        enviar("POST", "/api/matriculas", Map.of("alunoId", a, "cursoId", 999), admin, 404);
    }

    @Test
    void ultimaVagaSoPodeSerOcupadaUmaVez() throws Exception {
        Long c = curso(instrutor(), "Curso concorrido");
        Long a = aluno("aluno1@example.com"), b = aluno("aluno2@example.com");
        enviar("POST", "/api/cursos/" + c + "/vagas", Map.of("quantidade", 1), admin, 200);
        var inicio = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> resultados = new ArrayList<>();
            for (Long id : List.of(a, b))
                resultados.add(
                        pool.submit(
                                () -> {
                                    inicio.await();
                                    try {
                                        academico.matricular(id, c);
                                        return true;
                                    } catch (IllegalStateException e) {
                                        return false;
                                    }
                                }));
            inicio.countDown();
            int sucesso = 0;
            for (var f : resultados) if (f.get(10, TimeUnit.SECONDS)) sucesso++;
            assertThat(sucesso).isEqualTo(1);
        }
        assertThat(cursos.findById(c).orElseThrow().getVagas()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from matricula", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void swaggerPublicoEDocumentaRotasESeguranca() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        var docs = enviar("GET", "/v3/api-docs", null, null, 200);
        assertThat(docs.get("info").get("title").asText()).isEqualTo("Catálogo de Cursos");
        for (String path :
                List.of(
                        "/api/cursos",
                        "/api/instrutores",
                        "/api/matriculas",
                        "/api/autenticacao/refresh",
                        "/api/auditoria-seguranca"))
            assertThat(docs.get("paths").has(path)).isTrue();
        assertThat(docs.get("components").get("securitySchemes").has("bearerAuth")).isTrue();
        enviar("GET", "/api/cursos", null, null, 401);
        enviar("GET", "/api/cursos", null, "invalido", 401);
    }

    @Test
    void operadorPodeOperarMasNaoAdministrar() throws Exception {
        operador();
        String token = login("OPERADOR", SENHA).get("accessToken").asText();
        enviar("GET", "/api/cursos", null, token, 200);
        enviar("GET", "/api/auditoria-seguranca", null, token, 403);
        enviar(
                "POST",
                "/api/contas-acesso",
                Map.of(
                        "pessoaId",
                        1,
                        "login",
                        "outro",
                        "senha",
                        SENHA,
                        "perfis",
                        List.of("ADMINISTRADOR")),
                token,
                403);
        enviar("DELETE", "/api/cursos/999", null, token, 403);
        enviar("PUT", "/api/contas-acesso/1/status", Map.of("ativo", false), token, 403);
        enviar("GET", "/api/contas-acesso/minha", null, token, 200);
    }

    @Test
    void administraContasPerfisStatusESenha() throws Exception {
        Long id = operador();
        enviar("GET", "/api/contas-acesso/" + id, null, admin, 200);
        enviar("GET", "/api/contas-acesso/999", null, admin, 404);
        enviar(
                "PUT",
                "/api/contas-acesso/" + id + "/perfis",
                Map.of("perfis", List.of("ADMINISTRADOR", "OPERADOR")),
                admin,
                200);
        enviar("PUT", "/api/contas-acesso/" + id + "/status", Map.of("ativo", false), admin, 200);
        enviar(
                "POST",
                "/api/autenticacao/login",
                Map.of("login", "operador", "senha", SENHA),
                null,
                401);
        enviar("PUT", "/api/contas-acesso/" + id + "/status", Map.of("ativo", true), admin, 200);
        String token = login("operador", SENHA).get("accessToken").asText();
        enviar(
                "PUT",
                "/api/contas-acesso/minha/senha",
                Map.of("senhaAtual", "incorreta", "novaSenha", SENHA + "Nova"),
                token,
                401);
        enviar(
                "PUT",
                "/api/contas-acesso/minha/senha",
                Map.of("senhaAtual", SENHA, "novaSenha", SENHA + "Nova"),
                token,
                204);
        login("operador", SENHA + "Nova");
        enviar("PUT", "/api/contas-acesso/999/status", Map.of("ativo", false), admin, 404);
        enviar(
                "POST",
                "/api/contas-acesso",
                Map.of(
                        "pessoaId",
                        999,
                        "login",
                        "ausente",
                        "senha",
                        SENHA,
                        "perfis",
                        List.of("OPERADOR")),
                admin,
                404);
        enviar(
                "POST",
                "/api/contas-acesso",
                Map.of(
                        "pessoaId",
                        1,
                        "login",
                        "admin",
                        "senha",
                        SENHA,
                        "perfis",
                        List.of("OPERADOR")),
                admin,
                409);
        var eventos =
                enviar("GET", "/api/auditoria-seguranca?tipo=SENHA_ALTERADA", null, admin, 200);
        assertThat(eventos.get("totalElementos").asInt()).isEqualTo(1);
        var eventoAdmin =
                enviar("GET", "/api/auditoria-seguranca?tipo=PERFIS_ALTERADOS", null, admin, 200)
                        .get("conteudo")
                        .get(0);
        assertThat(eventoAdmin.get("atorContaId").asLong()).isEqualTo(adminId);
        assertThat(eventoAdmin.get("contaId").asLong()).isEqualTo(id);
        enviar("GET", "/api/auditoria-seguranca?pagina=-1", null, admin, 400);
    }

    @Test
    void refreshRotacionaEReplayRevogaTodaAFamilia() throws Exception {
        var par = login("admin", SENHA);
        String antigo = par.get("refreshToken").asText();
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from sessao_refresh where token_hash=?",
                                Integer.class,
                                antigo))
                .isZero();
        var novo =
                enviar(
                        "POST",
                        "/api/autenticacao/refresh",
                        Map.of("refreshToken", antigo),
                        null,
                        200);
        assertThat(novo.get("refreshToken").asText()).isNotEqualTo(antigo);
        enviar("POST", "/api/autenticacao/refresh", Map.of("refreshToken", antigo), null, 401);
        enviar(
                "POST",
                "/api/autenticacao/refresh",
                Map.of("refreshToken", novo.get("refreshToken").asText()),
                null,
                401);
        enviar(
                "POST",
                "/api/autenticacao/refresh",
                Map.of("refreshToken", "inexistente"),
                null,
                401);
    }

    @Test
    void logoutLocalGlobalEContaInativaRevogamRefresh() throws Exception {
        var par = login("admin", SENHA);
        enviar(
                "POST",
                "/api/autenticacao/logout",
                Map.of("refreshToken", par.get("refreshToken").asText()),
                null,
                204);
        enviar("GET", "/api/cursos", null, par.get("accessToken").asText(), 200);
        enviar(
                "POST",
                "/api/autenticacao/refresh",
                Map.of("refreshToken", par.get("refreshToken").asText()),
                null,
                401);
        enviar(
                "POST",
                "/api/autenticacao/logout",
                Map.of("refreshToken", "desconhecido"),
                null,
                204);
        par = login("admin", SENHA);
        enviar("POST", "/api/autenticacao/logout-todos", Map.of(), admin, 204);
        enviar(
                "POST",
                "/api/autenticacao/refresh",
                Map.of("refreshToken", par.get("refreshToken").asText()),
                null,
                401);
        Long id = operador();
        par = login("operador", SENHA);
        enviar("PUT", "/api/contas-acesso/" + id + "/status", Map.of("ativo", false), admin, 200);
        enviar(
                "POST",
                "/api/autenticacao/refresh",
                Map.of("refreshToken", par.get("refreshToken").asText()),
                null,
                401);
    }

    @Test
    void refreshExpiraExatamenteNoLimiteDeInatividade() throws Exception {
        var par = login("admin", SENHA);
        relogio.agora = relogio.agora.plus(Duration.ofMinutes(30));
        enviar(
                "POST",
                "/api/autenticacao/refresh",
                Map.of("refreshToken", par.get("refreshToken").asText()),
                null,
                401);
    }

    @Test
    void quintaFalhaBloqueiaESucessoLimpaNoPrazo() throws Exception {
        operador();
        for (int i = 0; i < 5; i++)
            enviar(
                    "POST",
                    "/api/autenticacao/login",
                    Map.of("login", " OPERADOR ", "senha", "errada"),
                    null,
                    401);
        enviar(
                "POST",
                "/api/autenticacao/login",
                Map.of("login", "operador", "senha", SENHA),
                null,
                401);
        assertThat(
                        jdbc.queryForObject(
                                "select falhas from protecao_autenticacao where login_hash=?",
                                Integer.class,
                                TokenHash.hash("operador")))
                .isEqualTo(5);
        relogio.agora = relogio.agora.plus(Duration.ofMinutes(15));
        login("operador", SENHA);
        assertThat(
                        jdbc.queryForObject(
                                "select falhas from protecao_autenticacao where login_hash=?",
                                Integer.class,
                                TokenHash.hash("operador")))
                .isZero();
        var ausente =
                enviar(
                        "POST",
                        "/api/autenticacao/login",
                        Map.of("login", "naoexiste", "senha", "errada"),
                        null,
                        401);
        assertThat(ausente.get("message").asText()).isEqualTo("Credenciais inválidas");
        String eventos =
                json.writeValueAsString(
                        enviar("GET", "/api/auditoria-seguranca", null, admin, 200));
        assertThat(eventos).doesNotContain(SENHA).doesNotContain("refreshToken");
    }

    @Test
    void ultimoAdministradorAtivoNaoPodeSerRemovido() throws Exception {
        enviar(
                "PUT",
                "/api/contas-acesso/" + adminId + "/status",
                Map.of("ativo", false),
                admin,
                409);
        enviar(
                "PUT",
                "/api/contas-acesso/" + adminId + "/perfis",
                Map.of("perfis", List.of("OPERADOR")),
                admin,
                409);
        assertThat(contas.findById(adminId).orElseThrow().isAtivo()).isTrue();
    }

    @Test
    void vagasStatusEPapeisConsultaveis() throws Exception {
        Long c = curso(instrutor(), "Controle de vagas");
        enviar("POST", "/api/cursos/" + c + "/vagas/entradas", Map.of("quantidade", 3), admin, 200);
        enviar("POST", "/api/cursos/" + c + "/vagas/saidas", Map.of("quantidade", 2), admin, 200);
        enviar("POST", "/api/cursos/" + c + "/vagas/saidas", Map.of("quantidade", 2), admin, 409);
        enviar("PUT", "/api/cursos/" + c + "/status", Map.of("ativo", false), admin, 200);
        Long a = aluno("outro@example.com");
        enviar("POST", "/api/matriculas", Map.of("alunoId", a, "cursoId", c), admin, 409);
        enviar("GET", "/api/alunos/" + a, null, admin, 200);
        Long pessoa = pessoa("colaborador@example.com");
        Long colaborador =
                enviar(
                                "POST",
                                "/api/colaboradores",
                                Map.of("pessoaId", pessoa, "cargo", "Secretaria"),
                                admin,
                                201)
                        .get("id")
                        .asLong();
        enviar("GET", "/api/colaboradores/" + colaborador, null, admin, 200);
        enviar("GET", "/api/alunos/999", null, admin, 404);
        enviar("GET", "/api/colaboradores/999", null, admin, 404);
    }

    @Test
    void renovacoesConcorrentesDetectamReutilizacao() throws Exception {
        String refresh = login("admin", SENHA).get("refreshToken").asText();
        var inicio = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> resultados = new ArrayList<>();
            for (int i = 0; i < 2; i++)
                resultados.add(
                        pool.submit(
                                () -> {
                                    inicio.await();
                                    try {
                                        autenticacao.renovar(refresh);
                                        return true;
                                    } catch (CredenciaisInvalidasException e) {
                                        return false;
                                    }
                                }));
            inicio.countDown();
            int sucesso = 0;
            for (var f : resultados) if (f.get(10, TimeUnit.SECONDS)) sucesso++;
            assertThat(sucesso).isEqualTo(1);
        }
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from sessao_refresh where revogada=false and"
                                        + " familia_id=(select familia_id from sessao_refresh where"
                                        + " token_hash=?)",
                                Integer.class,
                                TokenHash.hash(refresh)))
                .isZero();
    }

    @Test
    void cancelamentosConcorrentesDevolvemSomenteUmaVaga() throws Exception {
        Long c = curso(instrutor(), "Curso cancelamento");
        Long a = aluno("cancelamento@example.com");
        enviar("POST", "/api/cursos/" + c + "/vagas", Map.of("quantidade", 1), admin, 200);
        Long id =
                enviar("POST", "/api/matriculas", Map.of("alunoId", a, "cursoId", c), admin, 201)
                        .get("id")
                        .asLong();
        var inicio = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> resultados = new ArrayList<>();
            for (int i = 0; i < 2; i++)
                resultados.add(
                        pool.submit(
                                () -> {
                                    inicio.await();
                                    try {
                                        academico.cancelar(id);
                                        return true;
                                    } catch (IllegalStateException e) {
                                        return false;
                                    }
                                }));
            inicio.countDown();
            int sucesso = 0;
            for (var f : resultados) if (f.get(10, TimeUnit.SECONDS)) sucesso++;
            assertThat(sucesso).isEqualTo(1);
        }
        assertThat(cursos.findById(c).orElseThrow().getVagas()).isEqualTo(1);
    }

    @Test
    void falhaAoPersistirMatriculaReverteReservaDeVaga() throws Exception {
        Long c = curso(instrutor(), "Curso rollback");
        Long a = aluno("rollback@example.com");
        enviar("POST", "/api/cursos/" + c + "/vagas", Map.of("quantidade", 1), admin, 200);
        org.mockito.Mockito.doThrow(
                        new org.springframework.dao.DataIntegrityViolationException(
                                "Falha simulada de persistência"))
                .when(matriculas)
                .saveAndFlush(org.mockito.ArgumentMatchers.any(Matricula.class));
        assertThatThrownBy(() -> academico.matricular(a, c))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(cursos.findById(c).orElseThrow().getVagas()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from matricula", Integer.class)).isZero();
    }
}
