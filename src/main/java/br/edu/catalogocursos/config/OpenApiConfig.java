package br.edu.catalogocursos.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.*;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.*;
import java.util.Set;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenApiCustomizer errosDocumentados() {
        return api -> {
            var erro = new ObjectSchema()
                .addProperty("status", new IntegerSchema().example(404))
                .addProperty("message", new StringSchema().example("Curso não encontrado"))
                .addProperty("path", new StringSchema().example("/api/cursos/999"));
            api.getComponents().addSchemas("Erro", erro);
            var content = new Content().addMediaType("application/json",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/Erro")));
            Set<String> criacoesRelacionadas=Set.of("/api/cursos","/api/matriculas","/api/alunos","/api/colaboradores","/api/contas-acesso");
            for(var caminho:api.getPaths().entrySet()){
                String path=caminho.getKey();
                if(path.equals("/api/health")) continue;
                for(var entrada:caminho.getValue().readOperationsMap().entrySet()){
                    var method=entrada.getKey();
                    var responses=entrada.getValue().getResponses();
                    boolean autenticacao=path.startsWith("/api/autenticacao/");
                    responses.addApiResponse("401",new ApiResponse().description("Autenticação ausente, expirada ou inválida"));
                    if(!autenticacao) responses.addApiResponse("403",new ApiResponse().description("Perfil sem permissão"));
                    if(method==PathItem.HttpMethod.POST || method==PathItem.HttpMethod.PUT)
                        responses.addApiResponse("400",new ApiResponse().description("Dados inválidos").content(content));
                    if(path.contains("{id}") || criacoesRelacionadas.contains(path) && method==PathItem.HttpMethod.POST)
                        responses.addApiResponse("404",new ApiResponse().description("Recurso inexistente").content(content));
                    if(method!=PathItem.HttpMethod.GET && !autenticacao)
                        responses.addApiResponse("409",new ApiResponse().description("Conflito de integridade ou regra de negócio").content(content));
                }
            }
        };
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info().title("Catálogo de Cursos").version("1.0")
                .description("Instrutores, cursos, matrículas e autenticação"))
            .components(new Components().addSecuritySchemes("bearerAuth",
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
