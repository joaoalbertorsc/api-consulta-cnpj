**English** | [Português (Brasil)](README.pt-BR.md)

# CNPJ Finder API

REST API in Java 21 and Spring Boot to look up Brazilian companies by CNPJ, with a fallback between
external data sources, caching and local persistence.

## Context

A company lookup by CNPJ (the Brazilian company registry number) depends on public external APIs
that can be slow, rate-limited or temporarily down. Calling them for every request also repeats
work for CNPJs that were already looked up. The goal: validate the input before any external
call, keep answering when one source fails and avoid unnecessary calls for repeated lookups.

## Action

- **Validation first:** the CNPJ must have exactly 14 digits and valid check digits; invalid
  input returns `400 Bad Request` without reaching any external API.
- **Fallback between sources:** the service tries **BrasilAPI** first and falls back to
  **ReceitaWS** when it fails.
- **Local source of truth:** companies already found are stored in **H2** and read from there
  before any external call.
- **Cache:** `@Cacheable` on the lookup method (`cnpjCache`), so repeated lookups are served from
  memory.
- **Clear errors:** `404` when the CNPJ is not found in any source and `503` when the external
  APIs are unavailable.
- **Lookup statistics:** total companies, total lookups and the 10 most queried companies.

```mermaid
flowchart LR
    client([Client]) --> controller[CnpjController<br/>validation]
    controller --> service[CnpjService<br/>@Cacheable]
    service --> repo[(H2<br/>local data)]
    service -->|1st| brasil[BrasilAPI]
    service -->|fallback| receita[ReceitaWS]
```

## Result

- Invalid CNPJs are rejected before any network call.
- If BrasilAPI fails, the lookup continues through ReceitaWS, with no change for the client.
- Repeated lookups are answered by the cache or the local database, without calling the external
  APIs again.
- Documented API with Swagger UI and request/response examples below.

## Endpoints

Base path: `/api/cnpj`

| Method | Path | Description |
|---|---|---|
| GET | `/api/cnpj/{cnpj}` | Looks up a company by CNPJ (14 digits) |
| GET | `/api/cnpj/buscar?razaoSocial=&municipio=&uf=` | Searches the local database (at least one parameter) |
| GET | `/api/cnpj/estatisticas` | Lookup statistics |

### Look up a CNPJ

```sh
curl -s http://localhost:8080/api/cnpj/01234567000189
```

`200 OK`:
```json
{
  "cnpj": "01234567000189",
  "razaoSocial": "Empresa Exemplo SA",
  "nomeFantasia": "Empresa Exemplo",
  "uf": "SP",
  "municipio": "São Paulo",
  "atividadePrincipal": "Atividade de exemplo",
  "endereco": "Rua Exemplo, 123",
  "telefone": "(11) 1234-5678"
}
```

`400 Bad Request`:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "CNPJ inválido: deve conter exatamente 14 dígitos numéricos"
}
```

Other responses: `404 Not Found` (not found in any source) and `503 Service Unavailable` (external
APIs unavailable).

### Search the local database

```sh
curl -s "http://localhost:8080/api/cnpj/buscar?razaoSocial=empresa"
```

Parameters: `razaoSocial`, `municipio` or `uf` (`uf` must have exactly 2 characters). Returns
`200 OK` with a list (possibly empty) or `400 Bad Request` when no parameter is given.

### Statistics

```sh
curl -s http://localhost:8080/api/cnpj/estatisticas
```

```json
{
  "totalEmpresas": 123,
  "totalConsultas": 4567,
  "empresasMaisConsultadas": [
    { "cnpj": "01234567000189", "razaoSocial": "Empresa Exemplo SA", "consultas": 123 },
    { "cnpj": "98765432000100", "razaoSocial": "Outra Empresa LTDA", "consultas": 90 }
  ]
}
```

## Design decisions and known limitations

- **Sequential fallback (try/catch):** simple and enough for two sources. A circuit breaker
  (Resilience4j) would be the next step to stop calling a source that keeps failing.
- **In-memory cache (`simple`):** fits a single instance; with several instances, a distributed
  cache such as Redis would be needed.
- **H2 in file mode:** keeps data between runs in local development; production would use a
  relational database such as PostgreSQL.
- **Search by one filter at a time:** the search uses the first parameter given
  (`razaoSocial` → `municipio` → `uf`); combining filters is a planned improvement.

## Tech stack

Java 21, Spring Boot 3.2.5, Spring Web, Spring Data JPA, Spring Cache, SpringDoc OpenAPI (Swagger),
H2, Lombok and Gradle (wrapper included).

## Run it

Requirements: Java 21 (JDK) and Git.

```sh
git clone https://github.com/joaoalbertorsc/api-consulta-cnpj.git
cd api-consulta-cnpj
./gradlew bootRun
```

Useful URLs:
- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- H2 Console (development only): `http://localhost:8080/h2-console`

Tests:
```sh
./gradlew test
```

Configuration is in `src/main/resources/application.properties`. The values there are for local
development; credentials should come from environment variables in any other environment.

## Repository layout

```
src/main/java/com/cnpjfinder/
  controller/   # REST controller (CnpjController)
  service/      # business logic (CnpjService)
  client/       # external API clients (BrasilApiClient, ReceitaWsClient)
  repository/   # Spring Data JPA repositories (EmpresaRepository)
  model/        # entities and DTOs (Empresa, Estatisticas)
  exception/    # custom exceptions
src/main/resources/application.properties
```

## License

[MIT](LICENSE)
