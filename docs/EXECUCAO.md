# Execução e teste

O projeto usa Java 21 e o Maven Wrapper. Crie bancos PostgreSQL separados para desenvolvimento e testes. O Liquibase cria e evolui as tabelas na inicialização. O Hibernate usa `validate` nos perfis normais.

## Desenvolvimento no PowerShell

Na raiz do projeto, configure as variáveis abaixo. Ajuste host, porta e usuário para sua instalação.

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21.0.12'
$env:DB_URL = 'jdbc:postgresql://localhost:5432/catalogocursos_dev'
$env:DB_USERNAME = Read-Host 'Usuário PostgreSQL'
$env:DB_PASSWORD = Read-Host 'Senha PostgreSQL' -MaskInput
$env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
$env:INITIAL_ADMIN_ENABLED = 'true'
$env:INITIAL_ADMIN_LOGIN = Read-Host 'Login do administrador'
$env:INITIAL_ADMIN_PASSWORD = Read-Host 'Senha inicial (mínimo de 12 caracteres)' -MaskInput
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

O exemplo de leitura de senha usa PowerShell 7. No Windows PowerShell 5.1, defina as variáveis de ambiente pela configuração de execução da IDE. O arquivo `.env` também pode ser usado e está ignorado pelo Git. O `.env.example` contém apenas referências para preenchimento.

O primeiro administrador só pode ser provisionado em uma base sem contas. Depois do primeiro boot, defina `INITIAL_ADMIN_ENABLED=false`. Preserve a mesma `JWT_SECRET` nos reinícios em seu ambiente local. Gere uma chave diferente para cada ambiente e mantenha as credenciais fora do repositório.

No Bash, use `export` para as mesmas variáveis e execute `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`.

## Swagger

Abra [Swagger UI](http://localhost:8080/swagger-ui/index.html). O contrato JSON fica em [OpenAPI](http://localhost:8080/v3/api-docs) e o YAML em `/v3/api-docs.yaml`.

1. Execute `POST /api/autenticacao/login` com o login e a senha que definiu no primeiro boot.
2. Copie o campo `accessToken`.
3. Clique em **Authorize** e cole o token no esquema `bearerAuth`.
4. Use **Try it out** para executar as operações.

As rotas de negócio exigem Bearer token. Para executar uma consulta pela barra do navegador, use um cliente que envie o cabeçalho `Authorization`. A interface Swagger e o health são públicos nos ambientes de estudo. O perfil `prod` desabilita OpenAPI e Swagger.

## Fluxo de cadastro

Cadastre um instrutor com `POST /api/instrutores`. Cadastre um curso em `POST /api/cursos`, informando o `instrutorId` retornado. O contrato inicial de cadastro foi mantido.

Atualize título, nível, carga horária, preço e status com `PUT /api/cursos/{id}`. Adicione vagas com `POST /api/cursos/{id}/vagas/entradas`. Para retirar vagas disponíveis, use `POST /api/cursos/{id}/vagas/saidas`. O corpo dessas movimentações é `{"quantidade": 10}`.

Cadastre uma pessoa em `POST /api/pessoas` e atribua o papel de aluno em `POST /api/alunos`, usando `{"pessoaId": 1}`. A mesma pessoa pode ter o papel de colaborador em `POST /api/colaboradores`, com `pessoaId` e `cargo`.

Confirme a matrícula em `POST /api/matriculas`, com `alunoId` e `cursoId`. A operação reserva uma vaga e registra o preço daquele momento. O cancelamento em `POST /api/matriculas/{id}/cancelamento` devolve a vaga uma única vez.

Os IDs de cada tabela têm sequência própria e começam em 1 em uma base vazia. Transações revertidas podem deixar lacunas. Use sempre os IDs e os cabeçalhos `Location` devolvidos pelos cadastros.

## Filtros e paginação

`GET /api/cursos?titulo=java&nivel=INICIANTE&ativo=true&pagina=0&tamanho=20` retorna `conteudo`, `pagina`, `tamanho`, `totalElementos` e `totalPaginas`. A ordenação é por título e ID. A página começa em zero e o tamanho permitido vai de 1 a 100.

`GET /api/instrutores` usa o mesmo envelope de paginação. `GET /api/instrutores/{id}/cursos` preserva o contrato original de lista ordenada.

## Testes e cobertura

Use exclusivamente um banco de teste. Os testes HTTP limpam os dados desse banco para manter os cenários independentes.

```powershell
$env:TEST_DB_URL = 'jdbc:postgresql://localhost:5432/catalogocursos_test'
$env:TEST_DB_USERNAME = Read-Host 'Usuário de teste'
$env:TEST_DB_PASSWORD = Read-Host 'Senha de teste' -MaskInput
$env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
$env:TESTAR_POSTGRES = 'true'
$env:INITIAL_ADMIN_ENABLED = 'false'
.\mvnw.cmd clean verify
```

No Bash, use as mesmas variáveis exportadas e `./mvnw clean verify`. A suíte usa o perfil `test`. Sem `TESTAR_POSTGRES=true`, os testes que precisam de PostgreSQL são pulados. Para a validação completa, a variável deve estar habilitada.

O JaCoCo gera `target/site/jacoco/index.html`. O build exige 70% de linhas e 40% de branches no conjunto da aplicação, conforme a Aula 15. Os valores são um ponto de partida acadêmico e os testes também verificam concorrência, rollback, HTTP e autorização. O relatório gerado fica fora do Git.

Importe `postman/Catalogo-Cursos.postman_collection.json` para executar o fluxo no cliente HTTP. Preencha login e senhas em variáveis locais e evite exportá-las. Login e refresh atualizam automaticamente as variáveis de tokens.
