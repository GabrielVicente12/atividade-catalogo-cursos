package br.edu.catalogocursos.api;

import br.edu.catalogocursos.api.controller.InstrutorController;
import br.edu.catalogocursos.api.exception.ApiExceptionHandler;
import br.edu.catalogocursos.application.InstrutorService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InstrutorControllerTest {
    @Test
    void cadastroInvalidoRetorna400() throws Exception {
        var service = mock(InstrutorService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new InstrutorController(service))
                .setControllerAdvice(new ApiExceptionHandler()).build();

        mvc.perform(post("/api/instrutores").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"A","email":"sem-arroba"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/instrutores"));

        verifyNoInteractions(service);
    }
}
