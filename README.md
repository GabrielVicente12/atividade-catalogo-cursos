# Catálogo de Cursos

API REST para gerenciar instrutores, cursos, alunos e matrículas. A plataforma controla as vagas disponíveis, mantém o valor histórico das matrículas e oferece autenticação com JWT, perfis de acesso e auditoria.

O projeto foi desenvolvido com base nas aulas 00 a 17 da disciplina, adaptando os exemplos para o domínio de cursos.

## Tecnologias

- Java 21 e Spring Boot 4.0.7
- Maven Wrapper
- Spring Web MVC, Spring Data JPA e Bean Validation
- PostgreSQL e Liquibase
- Spring Security e OAuth2 Resource Server
- OpenAPI e Swagger UI com springdoc
- JUnit, Mockito, MockMvc e JaCoCo

## Funcionalidades

- Cadastro, consulta, atualização e exclusão de instrutores e cursos.
- Filtros de cursos por título, nível e status, com paginação.
- Ativação e inativação de cursos e movimentação de vagas.
- Cadastro de pessoas e atribuição dos papéis de aluno e colaborador.
- Matrículas com reserva de vaga e registro do preço no momento da confirmação.
- Cancelamento de matrícula com devolução da vaga.
- Controle de concorrência para impedir ocupação acima das vagas disponíveis.
- Login com senha protegida por BCrypt e emissão de JWT.
- Perfis OPERADOR e ADMINISTRADOR.
- Renovação de tokens com rotação, detecção de reutilização e revogação.
- Bloqueio temporário após falhas de autenticação.
- Auditoria de autenticação e alterações administrativas.

## Organização

Os controllers recebem as requisições e delegam os casos de uso aos serviços. O domínio concentra as regras e os repositories acessam a persistência. A API usa DTOs para separar os contratos HTTP das entidades JPA.

Um instrutor pode ministrar vários cursos. Cada curso pertence a um instrutor. A matrícula associa aluno e curso e registra valor, data e status.

As alterações de estrutura são aplicadas pelo Liquibase. O Hibernate valida o schema com 'ddl-auto=validate' nos ambientes de desenvolvimento, teste e produção.

## Executar localmente

Instale o JDK 21 e o PostgreSQL. Crie um banco exclusivo para desenvolvimento, por exemplo:

    createdb -U seu_usuario catalogocursos_dev

Na raiz do projeto, configure as variáveis abaixo no PowerShell 7. Ajuste JAVA_HOME, host e porta para sua instalação.

~~~powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21.0.12'
$env:DB_URL = 'jdbc:postgresql://localhost:5432/catalogocursos_dev'
$env:DB_USERNAME = Read-Host 'Usuário PostgreSQL'
$env:DB_PASSWORD = Read-Host 'Senha PostgreSQL' -MaskInput
$env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
$env:INITIAL_ADMIN_ENABLED = 'true'
$env:INITIAL_ADMIN_LOGIN = Read-Host 'Login do administrador'
$env:INITIAL_ADMIN_PASSWORD = Read-Host 'Senha inicial com pelo menos 12 caracteres' -MaskInput
./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
~~~

Preserve a chave JWT nos próximos reinícios. Após criar a primeira conta, defina INITIAL_ADMIN_ENABLED como false. O provisionamento inicial exige uma base sem contas de acesso.

As variáveis também podem ser definidas em um arquivo .env local. Os nomes estão no [.env.example](.env.example), e o arquivo preenchido é ignorado pelo Git.

No Linux ou macOS, exporte as mesmas variáveis e execute:

~~~bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
~~~

A aplicação utiliza a porta 8080. O Liquibase aplica as migrações pendentes na inicialização.

## Swagger e autenticação

Com a aplicação iniciada, abra [Swagger UI](http://localhost:8080/swagger-ui/index.html). O contrato fica disponível em [/v3/api-docs](http://localhost:8080/v3/api-docs).

Faça login em POST /api/autenticacao/login usando as credenciais configuradas no primeiro boot. Copie o accessToken retornado e informe-o em **Authorize**, no esquema bearerAuth.

As rotas de negócio exigem token Bearer. O operador executa as atividades do catálogo e das matrículas. O administrador também pode criar contas, alterar perfis e status, excluir registros e consultar a auditoria.

O perfil prod desabilita Swagger e OpenAPI. Os detalhes sobre duração dos tokens, logout e bloqueio estão em [Arquitetura e segurança](docs/ARQUITETURA.md).

## Principais rotas

| Método | Rota | Operação |
| --- | --- | --- |
| GET | /api/health | Verificar a aplicação |
| POST | /api/autenticacao/login | Autenticar |
| POST | /api/autenticacao/refresh | Renovar tokens |
| POST | /api/autenticacao/logout | Encerrar uma sessão |
| POST | /api/autenticacao/logout-todos | Encerrar todas as sessões da conta |
| GET, POST | /api/instrutores | Listar e cadastrar instrutores |
| GET, PUT, DELETE | /api/instrutores/{id} | Consultar, atualizar e excluir instrutor |
| GET | /api/instrutores/{id}/cursos | Listar os cursos de um instrutor |
| GET, POST | /api/cursos | Pesquisar e cadastrar cursos |
| GET, PUT, DELETE | /api/cursos/{id} | Consultar, atualizar e excluir curso |
| PUT | /api/cursos/{id}/status | Ativar ou inativar curso |
| POST | /api/cursos/{id}/vagas/entradas | Adicionar vagas |
| POST | /api/cursos/{id}/vagas/saidas | Retirar vagas disponíveis |
| POST | /api/pessoas | Cadastrar pessoa |
| POST | /api/alunos | Atribuir papel de aluno |
| POST | /api/colaboradores | Atribuir papel de colaborador |
| POST | /api/matriculas | Confirmar matrícula |
| GET | /api/matriculas/{id} | Consultar matrícula |
| POST | /api/matriculas/{id}/cancelamento | Cancelar matrícula |
| POST | /api/contas-acesso | Criar conta de operador |
| GET | /api/contas-acesso/minha | Consultar a própria conta |
| GET | /api/auditoria-seguranca | Consultar eventos |

O contrato completo, com corpos das requisições e respostas, está no Swagger. O [guia de execução](docs/EXECUCAO.md) descreve a ordem dos cadastros e os parâmetros de paginação.

## Testes

Crie um banco separado para os testes. Os testes HTTP limpam os dados desse banco durante a execução.

~~~powershell
$env:TEST_DB_URL = 'jdbc:postgresql://localhost:5432/catalogocursos_test'
$env:TEST_DB_USERNAME = Read-Host 'Usuário de teste'
$env:TEST_DB_PASSWORD = Read-Host 'Senha de teste' -MaskInput
$env:TESTAR_POSTGRES = 'true'
$env:INITIAL_ADMIN_ENABLED = 'false'
./mvnw.cmd clean verify
~~~

Mantenha JWT_SECRET configurada para executar a suíte. Sem TESTAR_POSTGRES=true, os testes que dependem de PostgreSQL são pulados.

O JaCoCo gera o relatório em target/site/jacoco/index.html e exige pelo menos 70% de cobertura de linhas e 40% de branches. A validação registrada da entrega executou 39 testes sem falhas, com cobertura de 97,85% de linhas e 92,52% de branches.

## Documentação

- [Execução e testes da API](docs/EXECUCAO.md)
- [Domínio, transações e segurança](docs/ARQUITETURA.md)
- [Migração e comparação do schema](docs/EVOLUCAO-DO-BANCO.md)
- [Evidências da validação](docs/EVIDENCIAS.md)
- [Coleção Postman](postman/Catalogo-Cursos.postman_collection.json)
- [Contrato OpenAPI exportado](docs/openapi.json)
- [Aulas de referência](https://github.com/jeffersonarpasserini/suporteos2026/tree/main/docs)
