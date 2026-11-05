package br.edu.catalogocursos.application;

import br.edu.catalogocursos.api.dto.CursoRequest;
import br.edu.catalogocursos.api.exception.RecursoNaoEncontradoException;
import br.edu.catalogocursos.api.mapper.CursoMapper;
import br.edu.catalogocursos.api.mapper.InstrutorMapper;
import br.edu.catalogocursos.domain.NivelCurso;
import br.edu.catalogocursos.repository.CursoRepository;
import br.edu.catalogocursos.repository.InstrutorRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CursoServiceTest {
    @Test
    void rejeitaCadastroQuandoInstrutorNaoExiste() {
        var instrutores = mock(InstrutorRepository.class);
        var cursos = mock(CursoRepository.class);
        var mapper = new CursoMapper();
        var instrutorService = new InstrutorService(instrutores, cursos, new InstrutorMapper(), mapper);
        var service = new CursoService(cursos, instrutorService, mapper);
        when(instrutores.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cadastrar(new CursoRequest("Java", 40, NivelCurso.INICIANTE, 999L)))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Instrutor não encontrado");
        verifyNoInteractions(cursos);
    }
}
