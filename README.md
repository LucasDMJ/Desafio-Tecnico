# Customer Service — Desafio Técnico (Java Pleno)

Microsserviço REST para gerenciamento de clientes, com consulta de score em um serviço externo (simulado com WireMock).

**Stack:** Java 17 · Spring Boot 3.3 · Maven · H2 (em memória) · Spring Data JPA · JdbcTemplate · Spring Security (Basic Auth) · Bean Validation · RestClient · JUnit 5 / Mockito / MockMvc / WireMock

---

## 1. Requisitos para execução

- JDK 17+
- Maven 3.9+ (ou use o Maven do IDE)
- Para o mock do score, **uma** das opções: Docker **ou** Java (jar do WireMock)

## 2. Como iniciar

### 2.1 Subir o serviço de score (mock)

O mock responde a `GET /scores/{cpf}` na porta **8089** usando os arquivos de `mock/wiremock/mappings`.

**Opção A — Docker**

```bash
docker compose up -d
```

**Opção B — JAR do WireMock (sem Docker)**

```bash
curl -L -o wiremock-standalone-3.9.1.jar \
  https://repo1.maven.org/maven2/org/wiremock/wiremock-standalone/3.9.1/wiremock-standalone-3.9.1.jar

java -jar wiremock-standalone-3.9.1.jar --port 8089 --root-dir mock/wiremock
```

CPFs de exemplo configurados no mock:

| CPF           | Comportamento do serviço externo                         | Resposta de `/customers/{id}/score` |
|---------------|----------------------------------------------------------|-------------------------------------|
| `12345678901` | 200 — score 750 / `LOW_RISK`                             | 200                                 |
| `98765432100` | 200 — score 320 / `HIGH_RISK`                            | 200                                 |
| `44444444444` | 404                                                      | 404                                 |
| `11111111111` | 500                                                      | 502                                 |
| `22222222222` | demora 10s (estoura o timeout)                           | 504                                 |
| `33333333333` | 200 com JSON de estrutura inesperada                     | 502                                 |
| `55555555555` | 200 com corpo que não é JSON                             | 502                                 |
| qualquer outro| 404                                                      | 404                                 |

Para testar **serviço indisponível (503)**: derrube o mock (`docker compose down`) e chame o endpoint de score.

### 2.2 Subir a aplicação

```bash
mvn spring-boot:run
```

A API fica em `http://localhost:8080`.

## 3. Como executar os testes

```bash
mvn test
```

Os testes **não dependem** do mock externo: os testes de score sobem um WireMock embutido em porta aleatória.

| Classe | O que cobre |
|---|---|
| `CustomerServiceTest` | Regras de negócio (unitário, Mockito): CPF duplicado, inexistente, status padrão |
| `CustomerApiIntegrationTest` | API completa com H2 + Spring Security: CRUD, validações, 401/403/404/409, filtro por status, busca por nome (native query), resumo (JdbcTemplate) |
| `CustomerScoreIntegrationTest` | `/customers/{id}/score` com WireMock: sucesso, 404, 500, timeout, JSON inválido, payload inesperado, CPF divergente |
| `ScoreHttpClientConnectionTest` | Serviço externo fora do ar (connection refused → 503) |

## 4. Configurações (`application.yml`)

Todas podem ser sobrescritas por variáveis de ambiente.

| Propriedade | Variável de ambiente | Padrão |
|---|---|---|
| `server.port` | `SERVER_PORT` | `8080` |
| `score-service.base-url` | `SCORE_SERVICE_URL` | `http://localhost:8089` |
| `score-service.connect-timeout` | `SCORE_SERVICE_CONNECT_TIMEOUT` | `2s` |
| `score-service.read-timeout` | `SCORE_SERVICE_READ_TIMEOUT` | `3s` |
| usuário USER | `APP_USER_USERNAME` / `APP_USER_PASSWORD` | `user` / `user123` |
| usuário ADMIN | `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD` | `admin` / `admin123` |

## 5. Segurança

Basic Authentication, stateless, com usuários em memória (senhas armazenadas com BCrypt).

| Perfil | Permissões |
|---|---|
| `USER`  | Apenas consultas (`GET`) |
| `ADMIN` | Consultas + `POST`, `PUT`, `DELETE` |

Credenciais padrão: `user / user123` e `admin / admin123`.

## 6. Endpoints

| Método | Rota | Perfil | Descrição | Sucesso |
|---|---|---|---|---|
| POST | `/customers` | ADMIN | Cadastra cliente | 201 + `Location` |
| PUT | `/customers/{id}` | ADMIN | Altera cliente | 200 |
| DELETE | `/customers/{id}` | ADMIN | Exclui cliente | 204 |
| GET | `/customers/{id}` | USER/ADMIN | Consulta por id | 200 |
| GET | `/customers` | USER/ADMIN | Lista todos | 200 |
| GET | `/customers?status=ACTIVE` | USER/ADMIN | Filtra por status (`ACTIVE`, `INACTIVE`, `BLOCKED`) | 200 |
| GET | `/customers/search?name=joao` | USER/ADMIN | Busca por parte do nome (case-insensitive) | 200 |
| GET | `/customers/{id}/score` | USER/ADMIN | Consulta score no serviço externo | 200 |
| GET | `/customers/summary` | USER/ADMIN | (extra) Quantidade de clientes por status | 200 |

### Códigos de erro

| Situação | HTTP |
|---|---|
| Campos obrigatórios ausentes / inválidos, JSON malformado, `status` inválido | 400 |
| Não autenticado / credenciais inválidas | 401 |
| Autenticado sem permissão (ex.: USER fazendo POST) | 403 |
| Cliente inexistente (consulta, alteração, exclusão, score) · score não encontrado para o CPF | 404 |
| CPF já cadastrado | 409 |
| Serviço de score fora do ar / conexão recusada | 503 |
| Serviço de score respondeu erro (5xx), JSON inválido ou payload inesperado | 502 |
| Serviço de score demorou além do timeout | 504 |

Formato padrão de erro:

```json
{
  "timestamp": "2026-10-24T15:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/customers",
  "details": ["cpf: cpf must contain exactly 11 digits (numbers only)"]
}
```

## 7. Exemplos de uso (curl)

```bash
# Criar cliente (ADMIN)
curl -i -u admin:admin123 -X POST http://localhost:8080/customers \
  -H "Content-Type: application/json" \
  -d '{"name":"João da Silva","cpf":"12345678901","email":"joao@email.com","status":"ACTIVE"}'

# Alterar (ADMIN)
curl -i -u admin:admin123 -X PUT http://localhost:8080/customers/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"João da Silva Jr","cpf":"12345678901","email":"joao.jr@email.com","status":"INACTIVE"}'

# Consultar por id (USER)
curl -u user:user123 http://localhost:8080/customers/1

# Listar todos / filtrar por status / buscar por nome
curl -u user:user123 http://localhost:8080/customers
curl -u user:user123 "http://localhost:8080/customers?status=ACTIVE"
curl -u user:user123 "http://localhost:8080/customers/search?name=joao"

# Score (precisa do mock no ar)
curl -u user:user123 http://localhost:8080/customers/1/score
# {"customerId":1,"cpf":"12345678901","score":750,"classification":"LOW_RISK"}

# Resumo por status
curl -u user:user123 http://localhost:8080/customers/summary

# Excluir (ADMIN)
curl -i -u admin:admin123 -X DELETE http://localhost:8080/customers/1

# Exemplos de erro
curl -i http://localhost:8080/customers                                  # 401
curl -i -u user:user123 -X DELETE http://localhost:8080/customers/1      # 403
```

## 8. Decisões de projeto

- **Camadas:** controller → service → repository; DTOs (`record`) separados da entidade.
- **Spring Data JPA** para o CRUD; **Native Query** em `CustomerRepository.searchByName`; **JdbcTemplate** em `CustomerStatsRepository` (contagem por status).
- **CPF:** validado como 11 dígitos numéricos (o CPF do exemplo do enunciado, `12345678901`, não passa no cálculo de dígito verificador; por isso não usei `@CPF`). Unicidade garantida por checagem no service **e** por constraint única no banco (tratada como 409 em caso de concorrência).
- **Integração externa (`ScoreHttpClient`):** `RestClient` com timeouts de conexão e leitura configuráveis; toda falha é traduzida para `ScoreServiceException` com o status HTTP adequado (tabela acima). A resposta é validada (campos obrigatórios e CPF coincidente).
- **Chamada externa fora de transação:** `ScoreService` não é `@Transactional`, para não segurar conexão do banco durante a chamada HTTP.
- **Segurança:** erros 401/403 retornam JSON no mesmo formato dos demais erros.
- **Evoluções possíveis:** retry com backoff e circuit breaker (Resilience4j), paginação nas listagens, Flyway, Swagger/OpenAPI, usuários em banco, soft delete.

## 9. Publicar no Git

```bash
git init
git add .
git commit -m "feat: customer service com consulta de score"
git branch -M main
git remote add origin <URL-DO-SEU-REPOSITORIO>
git push -u origin main
```
