package br.edu.catalogocursos.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class InstrutorTest {
    @Test
    void vinculaOsDoisLadosSemDuplicarAInstancia() {
        var instrutor = new Instrutor("Marina Costa", "marina@example.com");
        var curso = new Curso("Java", 40, NivelCurso.INICIANTE);

        instrutor.adicionarCurso(curso);
        instrutor.adicionarCurso(curso);

        assertThat(curso.getInstrutor()).isSameAs(instrutor);
        assertThat(instrutor.getCursos()).containsExactly(curso);
    }

    @Test
    void rejeitaCursoNuloEImpedeTrocaDeInstrutor() {
        var marina = new Instrutor("Marina Costa", "marina@example.com");
        var bruno = new Instrutor("Bruno Lima", "bruno@example.com");
        var curso = new Curso("Java", 40, NivelCurso.INICIANTE);
        marina.adicionarCurso(curso);

        assertThatNullPointerException().isThrownBy(() -> marina.adicionarCurso(null));
        assertThatIllegalStateException().isThrownBy(() -> bruno.adicionarCurso(curso));
        assertThat(marina.getCursos()).containsExactly(curso);
        assertThat(bruno.getCursos()).isEmpty();
        assertThat(curso.getInstrutor()).isSameAs(marina);
    }
}
