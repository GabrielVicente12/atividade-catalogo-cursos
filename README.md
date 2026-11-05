# Catálogo de Cursos

API REST independente para cadastrar instrutores e seus cursos livres. Usa Java 21, Maven Wrapper, Spring Boot 4.0.7, Spring Data JPA, Bean Validation, PostgreSQL e Liquibase.

## Executar localmente

1. Confirme que `java -version` aponta para um JDK 21.
2. Crie um banco PostgreSQL exclusivo e vazio, chamado `catalogocursos_dev`. Use um usuário com permissão para criar tabelas no banco. Por exemplo, rode `createdb -U seu_usuario catalogocursos_dev`.
3. Defina as variáveis no terminal que iniciará o projeto. O arquivo `.env.example` documenta os nomes. A aplicação lê as variáveis do processo.

No PowerShell:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/catalogocursos_dev'
$env:DB_USERNAME = Read-Host 'Usuário PostgreSQL'
$env:DB_PASSWORD = Read-Host 'Senha PostgreSQL' -MaskInput
.\mvnw.cmd spring-boot:run
```

No Linux ou macOS:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/catalogocursos_dev
read -r -p 'Usuário PostgreSQL: ' DB_USERNAME
read -rs -p 'Senha PostgreSQL: ' DB_PASSWORD
export DB_USERNAME DB_PASSWORD
./mvnw spring-boot:run
```

O Liquibase aplica o changelog na inicialização. O Hibernate valida o esquema com `ddl-auto=validate`. A API fica disponível em `http://localhost:8080`.

## Contrato REST

| Método | Rota | Sucesso |
| --- | --- | --- |
| POST | `/api/instrutores` | 201, com `Location` |
| POST | `/api/cursos` | 201, com `Location` |
| GET | `/api/cursos/{id}` | 200, com resumo do instrutor |
| GET | `/api/instrutores/{id}/cursos` | 200, somente os cursos do instrutor em ordem de título |
| GET | `/api/instrutores/{id}` | 200, destino do `Location` de cadastro |

Envie JSON com `Content-Type: application/json`. No cadastro de instrutor, use:

```json
{"nome":"Marina Costa","email":"marina.costa@example.com"}
```

No cadastro de curso, substitua `instrutorId` pelo ID retornado no cadastro do instrutor:

```json
{"titulo":"APIs REST com Spring Boot","cargaHoraria":40,"nivel":"INTERMEDIARIO","instrutorId":1}
```

O curso retorna `instrutor: {"id":1,"nome":"Marina Costa"}`. Um instrutor cadastrado sem cursos tem lista vazia. A API retorna 400 para dados inválidos, 404 para um recurso ausente e 409 para email já cadastrado. Os erros contêm `status`, `message` e `path`.

## Testes

Com as variáveis do banco exclusivo definidas, habilite o teste real de persistência e execute a suíte completa:

```powershell
$env:TESTAR_POSTGRES = 'true'
.\mvnw.cmd clean test
```

No Linux ou macOS, use `TESTAR_POSTGRES=true ./mvnw clean test`. O teste PostgreSQL usa uma transação revertida ao final. Sem `TESTAR_POSTGRES=true`, o Maven executa os testes de domínio, serviço e HTTP e pula o teste PostgreSQL.

## Respostas da atividade

- **Entidade proprietária:** `Curso`. Seu atributo `instrutor` tem `@ManyToOne`. A coleção inversa usa `mappedBy = "instrutor"`.
- **Chave estrangeira:** a coluna obrigatória `curso.instrutor_id` referencia `instrutor.id`.
- **Uso de DTOs:** o JSON do curso traz apenas ID e nome do instrutor. Isso evita o ciclo curso → instrutor → cursos e mantém o contrato HTTP separado da persistência.
- **Banco utilizado:** `catalogocursos_dev`.

## Material de referência

[Aulas 00 a 14 da disciplina](https://github.com/jeffersonarpasserini/suporteos2026/tree/main/docs). O catálogo aplica os padrões de projeto independente, modelagem de domínio, persistência com Liquibase, serviços transacionais e contratos REST das aulas ao escopo definido no enunciado.
