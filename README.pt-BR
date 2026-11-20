[English](README.md) | **Português (Brasil)**

# CNPJ Finder API

API REST em Java 21 e Spring Boot para consultar empresas brasileiras pelo CNPJ, com fallback entre
fontes externas, cache e persistência local.

## Contexto

Consultar uma empresa pelo CNPJ depende de APIs públicas externas, que podem ficar lentas, limitar
requisições ou sair do ar. Chamar essas APIs a cada requisição também repete trabalho para CNPJs que
já foram consultados. O objetivo: validar a entrada antes de qualquer chamada externa, continuar
respondendo quando uma fonte falha e evitar chamadas desnecessárias em consultas repetidas.

## Ação

- **Validação primeiro:** o CNPJ precisa ter exatamente 14 dígitos e dígitos verificadores
  válidos; entradas inválidas retornam `400 Bad Request` sem chegar a nenhuma API externa.
- **Fallback entre fontes:** o serviço tenta primeiro a **BrasilAPI** e, se ela falhar, consulta a
  **ReceitaWS**.
- **Fonte de verdade local:** empresas já encontradas ficam salvas no **H2** e são lidas dali antes
  de qualquer chamada externa.
- **Cache:** `@Cacheable` no método de consulta (`cnpjCache`), para que consultas repetidas sejam
  respondidas da memória.
- **Erros claros:** `404` quando o CNPJ não é encontrado em nenhuma fonte e `503` quando as APIs
  externas estão indisponíveis.
- **Estatísticas de consulta:** total de empresas, total de consultas e as 10 empresas mais
  consultadas.

```mermaid
flowchart LR
    client([Cliente]) --> controller[CnpjController<br/>validação]
    controller --> service[CnpjService<br/>@Cacheable]
    service --> repo[(H2<br/>dados locais)]
    service -->|1ª| brasil[BrasilAPI]
    service -->|fallback| receita[ReceitaWS]
```

## Resultado

- CNPJs inválidos são rejeitados antes de qualquer chamada de rede.
- Se a BrasilAPI falhar, a consulta segue pela ReceitaWS, sem mudança para o cliente.
- Consultas repetidas são respondidas pelo cache ou pelo banco local, sem chamar as APIs externas
  de novo.
- API documentada com Swagger UI e exemplos de requisição e resposta abaixo.

## Endpoints

Caminho base: `/api/cnpj`

| Método | Caminho | Descrição |
|---|---|---|
| GET | `/api/cnpj/{cnpj}` | Consulta uma empresa pelo CNPJ (14 dígitos) |
| GET | `/api/cnpj/buscar?razaoSocial=&municipio=&uf=` | Busca na base local (pelo menos um parâmetro) |
| GET | `/api/cnpj/estatisticas` | Estatísticas de consulta |

### Consultar um CNPJ

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

Outras respostas: `404 Not Found` (não encontrado em nenhuma fonte) e `503 Service Unavailable`
(APIs externas indisponíveis).

### Buscar na base local

```sh
curl -s "http://localhost:8080/api/cnpj/buscar?razaoSocial=empresa"
```

Parâmetros: `razaoSocial`, `municipio` ou `uf` (`uf` deve ter exatamente 2 caracteres). Retorna
`200 OK` com uma lista (possivelmente vazia) ou `400 Bad Request` quando nenhum parâmetro é
informado.

### Estatísticas

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

## Decisões de design e limitações conhecidas

- **Fallback sequencial (try/catch):** simples e suficiente para duas fontes. Um circuit breaker
  (Resilience4j) seria o próximo passo para parar de chamar uma fonte que continua falhando.
- **Cache em memória (`simple`):** atende uma única instância; com várias instâncias, seria
  necessário um cache distribuído, como o Redis.
- **H2 em modo arquivo:** mantém os dados entre execuções no desenvolvimento local; em produção, o
  ideal é um banco relacional como o PostgreSQL.
- **Busca por um filtro de cada vez:** a busca usa o primeiro parâmetro informado
  (`razaoSocial` → `municipio` → `uf`); combinar filtros é uma melhoria planejada.

## Tecnologias

Java 21, Spring Boot 3.2.5, Spring Web, Spring Data JPA, Spring Cache, SpringDoc OpenAPI (Swagger),
H2, Lombok e Gradle (wrapper incluído).

## Como rodar

Requisitos: Java 21 (JDK) e Git.

```sh
git clone https://github.com/joaoalbertorsc/api-consulta-cnpj.git
cd api-consulta-cnpj
./gradlew bootRun
```

Endereços úteis:
- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- H2 Console (só em desenvolvimento): `http://localhost:8080/h2-console`

Testes:
```sh
./gradlew test
```

A configuração fica em `src/main/resources/application.properties`. Os valores ali são para
desenvolvimento local; em qualquer outro ambiente, as credenciais devem vir de variáveis de
ambiente.

## Estrutura do repositório

```
src/main/java/com/cnpjfinder/
  controller/   # controller REST (CnpjController)
  service/      # regras de negócio (CnpjService)
  client/       # clientes das APIs externas (BrasilApiClient, ReceitaWsClient)
  repository/   # repositórios Spring Data JPA (EmpresaRepository)
  model/        # entidades e DTOs (Empresa, Estatisticas)
  exception/    # exceções customizadas
src/main/resources/application.properties
```

## Licença

[MIT](LICENSE)
