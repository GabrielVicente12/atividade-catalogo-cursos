# Verificação da entrega

Verificação executada em 26/11/2026 com Java 21.0.12, Maven Wrapper 3.9.16, Spring Boot 4.0.7 e PostgreSQL 18.6.

## Build e testes

Com `TESTAR_POSTGRES=true` e o perfil de teste apontando para uma base exclusiva, foi executado:

```text
./mvnw.cmd clean verify
Tests run: 39, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| Medida | Resultado | Limite |
| --- | --- | --- |
| Cobertura de linhas | 97.85% | 70% |
| Cobertura de branches | 92.52% | 40% |

O JaCoCo analisou todo o conjunto da aplicação. Nenhuma exclusão de classes foi configurada para elevar a cobertura. O HTML é gerado em `target/site/jacoco/index.html`.

## Riscos cobertos pelos testes

- Associação bidirecional sem duplicação de instância e coleção protegida.
- Rejeição de instrutor inexistente e validação de entradas HTTP.
- CRUD, filtros, paginação, email duplicado e restrições de exclusão.
- Matrícula com preço histórico e reserva de vaga.
- Rollback da vaga quando a gravação da matrícula falha.
- Duas matrículas concorrentes disputando uma vaga.
- Dois cancelamentos concorrentes devolvendo apenas uma vaga.
- Pessoa com papéis de aluno e colaborador por composição.
- Swagger público em desenvolvimento e contrato com rotas e esquema Bearer.
- Acesso anônimo e token inválido retornando 401.
- Operador sem privilégios administrativos recebendo 403.
- Alteração de senha, perfis e status com eventos de auditoria.
- Preservação do último administrador ativo.
- Senha armazenada por hash e refresh armazenado somente por SHA-256.
- Rotação de refresh, replay, logout local e logout global.
- Duas renovações simultâneas com detecção de reutilização.
- Expiração por inatividade e limite absoluto no domínio.
- Quinta falha de login, bloqueio e liberação no instante exato.
- Conta ausente com a mesma resposta de credenciais inválidas.
- Validação das propriedades e guarda do banco descartável de referência.

## Verificação HTTP fora dos testes MockMvc

O JAR foi iniciado com o perfil `dev` em uma cópia migrada da base anterior. O primeiro administrador foi provisionado com senha e chave JWT aleatórias locais. Login, consulta dos cursos antigos, Swagger e OpenAPI responderam por HTTP.

As 31 requisições da coleção foram executadas por um cliente HTTP automatizado, usando os mesmos métodos, URLs, corpos e variáveis da coleção. Foram confirmados os retornos 200, 201, 204, 400, 401, 403, 404 e 409. As variáveis exportadas de senha e tokens estão vazias.

A interface Swagger foi inspecionada no navegador. A captura está em [imagens/swagger.png](imagens/swagger.png). O documento gerado está em [openapi.json](openapi.json). A URL de servidor presente nessa captura é a porta temporária 18088 usada na validação. Em uma execução normal, o documento gerado pela aplicação usa a URL do ambiente atual.

## Migração e rollback

A atualização de uma cópia com quatro migrações preservou três instrutores e três cursos. O histórico passou para onze migrações. Os códigos antigos foram preenchidos e os campos originais foram comparados por hash.

Também foram executados o rollback das sete migrações novas e a reaplicação, exclusivamente na cópia descartável. A análise do diff e as diferenças remanescentes estão em [EVOLUCAO-DO-BANCO.md](EVOLUCAO-DO-BANCO.md).

## Produção e etapas versionadas

O JAR também iniciou com o perfil prod. Health retornou 200 e as rotas de Swagger e OpenAPI ficaram indisponíveis, com HTTP 401 na política de segurança desse ambiente.

Os três incrementos anteriores à entrega final foram exportados em pastas separadas e compilados com Maven Wrapper em modo offline. As etapas de ambientes/migrações, domínio e API compilaram individualmente.

## Material para execução

- [Comandos e fluxo da API](EXECUCAO.md)
- [Domínio, autorização e decisões de segurança](ARQUITETURA.md)
- [Coleção Postman](../postman/Catalogo-Cursos.postman_collection.json)

As instâncias usadas na verificação HTTP foram temporárias. Para iniciar a aplicação no seu ambiente, siga o guia de execução e configure suas próprias credenciais.
