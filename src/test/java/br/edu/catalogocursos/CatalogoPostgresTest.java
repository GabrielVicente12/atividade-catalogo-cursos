package br.edu.catalogocursos;

import br.edu.catalogocursos.api.dto.CursoRequest;
import br.edu.catalogocursos.api.dto.CursoResumoResponse;
import br.edu.catalogocursos.api.dto.InstrutorRequest;
import br.edu.catalogocursos.application.CursoService;
import br.edu.catalogocursos.application.InstrutorService;
import br.edu.catalogocursos.domain.NivelCurso;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "TESTAR_POSTGRES", matches = "true")
class CatalogoPostgresTest {
    @Autowired
    private InstrutorService instrutores;

    @Autowired
    private CursoService cursos;

    @Test
    void listaApenasCursosDoInstrutorEmOrdemDeTitulo() {
        var marina = instrutores.cadastrar(new InstrutorRequest("Marina Costa", UUID.randomUUID() + "@example.com"));
        var bruno = instrutores.cadastrar(new InstrutorRequest("Bruno Lima", UUID.randomUUID() + "@example.com"));
        cursos.cadastrar(new CursoRequest("Zoologia", 20, NivelCurso.INICIANTE, marina.id()));
        cursos.cadastrar(new CursoRequest("Algoritmos", 40, NivelCurso.INTERMEDIARIO, marina.id()));
        cursos.cadastrar(new CursoRequest("Banco de Dados", 30, NivelCurso.AVANCADO, bruno.id()));

        assertThat(instrutores.listarCursos(marina.id()))
                .extracting(CursoResumoResponse::titulo)
                .containsExactly("Algoritmos", "Zoologia");
    }
}
