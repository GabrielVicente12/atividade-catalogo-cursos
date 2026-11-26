# Migração assistida e conferência do schema

O laboratório da Aula 06 usa dois bancos descartáveis. O banco `catalogocursos_reference` recebe o schema gerado pelo Hibernate. Uma cópia de uma base antiga recebe as migrações oficiais. Os bancos originais permanecem preservados.

## Procedimento

1. Crie `catalogocursos_reference` e um segundo banco vazio para o diff.
2. Restaure nesse segundo banco uma cópia da versão anterior, com as quatro migrações iniciais e alguns registros.
3. Defina `DB_REFERENCE_URL`, `DB_REFERENCE_USERNAME` e `DB_REFERENCE_PASSWORD` para o banco de referência.
4. Defina `DB_DIFF_URL`, `DB_DIFF_USERNAME` e `DB_DIFF_PASSWORD` para a cópia descartável.
5. Gere uma `JWT_SECRET` local e execute o perfil exclusivo de referência.

```powershell
./mvnw.cmd -DskipTests package
java -jar target/catalogocursos-0.0.1-SNAPSHOT.jar --spring.profiles.active=schema-reference
./mvnw.cmd liquibase:diff
```

O perfil `schema-reference` usa `ddl-auto=create`, exige o nome exato `catalogocursos_reference` e encerra após gerar o esquema. Desenvolvimento, teste e produção continuam com `ddl-auto=validate`.

O rascunho é gerado em `target/liquibase-diff/changelog-gerado.yaml`. Revise-o antes de escrever a migração oficial. Guarde o rascunho fora do Git e mova o arquivo antes de gerar outro diff.

```powershell
./mvnw.cmd liquibase:update
./mvnw.cmd liquibase:diff
```

O plugin usa o mesmo caminho lógico de changelog da aplicação, `db/changelog/db.changelog-master.yaml`. O `searchPath` aponta para `src/main/resources`. Isso evita tratar uma migração já aplicada como nova por diferença entre um caminho de arquivo e um caminho de classpath.

## Problemas encontrados no rascunho

| Sugestão automática | Problema | Decisão aplicada |
| --- | --- | --- |
| Acrescentar código obrigatório em curso | Registros antigos ainda não tinham código | Adicionar opcional, preencher com CURSO-id e só depois restringir |
| Remover a unicidade do email do instrutor | A anotação original não descrevia essa constraint do banco | Preservar `uk_instrutor_email` |
| Gerar somente a estrutura dos atributos JPA | Regras de vagas, preço e índices não são inferidas de métodos Java | Manter CHECKs e índices na migração revisada |
| Remover a FK da sessão | O modelo de sessão guarda a referência por ID e usa JDBC para operações bloqueantes | Preservar a FK de conta no banco |

O Liquibase administra o schema oficial. O diff serve para detectar diferenças e orientar a revisão.

## Diferenças remanescentes justificadas

Depois de aplicar as migrações, o diff encontrou apenas sugestões de remoção de proteções e padrões presentes no schema oficial. Todas foram preservadas.

| Objeto | Diferença em relação ao schema JPA | Justificativa |
| --- | --- | --- |
| `sessao_refresh_conta_id_fkey` | FK adicional | Impede sessão órfã mesmo com referência armazenada como ID |
| `uk_instrutor_email` | UNIQUE adicional | Regra obrigatória do catálogo inicial |
| `uk_matricula_aluno_curso` | UNIQUE adicional | Impede matrícula duplicada por aluno e curso |
| `ix_evento_ator`, `ix_evento_criado`, `ix_evento_tipo` | Índices adicionais | Apoiam consultas de auditoria |
| `ix_sessao_familia` | Índice adicional | Apoia revogação da família |
| `conta_acesso.ativo`, `curso.ativo` | DEFAULT TRUE | Preserva estado inicial também em inserções SQL |
| `curso.data_criacao` | DEFAULT CURRENT_DATE | Preenche registros antigos sem data |
| `protecao_autenticacao.falhas` | DEFAULT 0 | Permite upsert atômico da proteção |
| `curso.preco`, `curso.vagas` | DEFAULT 0 | Backfill compatível com cursos existentes |
| `sessao_refresh.revogada`, `sessao_refresh.usado` | DEFAULT FALSE | Estado inicial da sessão |

O diff final não apontou tabelas ou colunas faltantes nem incompatibilidade de tipos. Os padrões do banco complementam os valores iniciais em Java. O Hibernate validou o schema oficial nos testes.

## Evidência executada

O laboratório restaurou uma cópia de `catalogocursos_dev_serial` em `catalogocursos_migration17`. A base de origem tinha três instrutores, três cursos e quatro migrações aplicadas. Foram comparados hashes dos campos originais antes e depois da atualização.

As sete migrações novas foram aplicadas, os códigos dos três cursos foram preenchidos e o histórico passou para onze migrações. Os campos originais permaneceram iguais. O banco de origem foi preservado.

O rollback de sete migrações foi executado na cópia descartável, retornando ao histórico de quatro migrações com os três instrutores e três cursos. Em seguida, as sete migrações foram reaplicadas. Esse teste valida a ordem das dependências e a preservação da baseline. Dados criados nas funcionalidades novas exigem backup antes de qualquer rollback.
