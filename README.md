# CNPJ Finder API

## Visão Geral

API RESTful em Java (Spring Boot) para consultar informações de empresas brasileiras a partir do CNPJ. A aplicação consulta serviços externos (BrasilAPI e ReceitaWS) e mantém uma persistência local em H2 (arquivo em `./data/cnpjdb`) para cache e estatísticas de uso.

Principais pontos técnicos:
- Validação do CNPJ: formato (14 dígitos numéricos) e algoritmo de dígitos verificadores implementados no controller.
- Fallback entre APIs externas: a aplicação tenta primeiro a BrasilAPI; em caso de erro da API, faz fallback para a ReceitaWS (implementado de forma sequencial via try/catch no serviço).
- Cache: `@Cacheable` em `CnpjService.consultarCnpj` com `spring.cache.type=simple` configurado.
- H2: uso em modo de arquivo (`jdbc:h2:file:./data/cnpjdb`) com H2 Console habilitado.

---

### Stack
- Linguagem: Java 21
- Framework / Runtime: Spring Boot 3.2.5
- Build: Gradle (Gradle Wrapper incluído)
- Bibliotecas notáveis: Spring Web, Spring Data JPA, Spring Cache, SpringDoc OpenAPI (Swagger), H2, Lombok

---

## Organização do projeto

Estrutura relevante (top-level):

```
README.md
HELP.md
build.gradle
settings.gradle
gradle/
gradlew
gradlew.bat
data/                 # pasta usada pelo H2 (arquivo de banco)
src/
  main/
    java/
      com.cnpjfinder/
        controller/    # controllers REST (CnpjController)
        service/       # lógica de negócio (CnpjService)
        client/        # clientes para APIs externas (BrasilApiClient, ReceitaWsClient)
        repository/    # Spring Data JPA repositories (EmpresaRepository)
        model/         # entidades e DTOs (Empresa, Estatisticas)
        exception/     # exceções customizadas
        config/        # configurações (se houver)
    resources/
      application.properties
  test/
```

Como se encaixa: o `CnpjController` valida o CNPJ e delega para `CnpjService`. O serviço verifica a fonte de verdade local (EmpresaRepository); na ausência do registro tenta consultar `BrasilApiClient` e, se necessário, `ReceitaWsClient`. Resultados novos são salvos no H2 e o contador de consultas é atualizado.

---

## Endpoints principais
Base: `/api/cnpj`

1) Consultar um CNPJ
- Método: GET
- Path: `/api/cnpj/{cnpj}`
- Path variable: `cnpj` — exatamente 14 dígitos numéricos
- Validações:
  - formato: `^\d{14}$`
  - algoritmo: verificação dos dígitos implementada no controller
- Respostas (implementadas no código):
  - `200 OK` — retorna o objeto `Empresa`
  - `400 Bad Request` — formato inválido ou algoritmo inválido
  - `404 Not Found` — CNPJ não encontrado nas fontes externas nem no banco local
  - `503 Service Unavailable` — quando APIs externas apresentarem indisponibilidade (conforme mapeamento de exceções)

Exemplo:
```sh
curl -s http://localhost:8080/api/cnpj/01234567000189
```

2) Buscar empresas na base local
- Método: GET
- Path: `/api/cnpj/buscar`
- Query params (pelo menos um é obrigatório): `razaoSocial`, `municipio`, `uf`
- Observação importante: o controller verifica os parâmetros em ordem e retorna resultados baseados no primeiro parâmetro não-nulo (ordem: `razaoSocial` → `municipio` → `uf`). Se múltiplos parâmetros forem enviados, somente o primeiro na ordem mencionada será utilizado.
- Regras:
  - `uf` deve ter exatamente 2 caracteres (caso contrário, 400 Bad Request)
- Respostas:
  - `200 OK` — lista (possivelmente vazia) de `Empresa`
  - `400 Bad Request` — nenhum parâmetro fornecido ou `uf` com tamanho inválido

Exemplo:
```sh
curl -s "http://localhost:8080/api/cnpj/buscar?razaoSocial=empresa"
```

3) Obter estatísticas
- Método: GET
- Path: `/api/cnpj/estatisticas`
- Retorno (200 OK): objeto `Estatisticas` contendo:
  - `totalEmpresas` (Long)
  - `totalConsultas` (Long)
  - `empresasMaisConsultadas` (lista das top 10 por quantidade de consultas)

Exemplo:
```sh
curl -s http://localhost:8080/api/cnpj/estatisticas
```

---

## Configuração e comportamento importante
Arquivo principal: `src/main/resources/application.properties`.

Principais propriedades observadas no projeto:

```
spring.application.name=desafio-dio-spring
server.port=8080
spring.datasource.url=jdbc:h2:file:./data/cnpjdb
spring.datasource.username=sa
spring.datasource.password=password
spring.h2.console.enabled=true
spring.jpa.hibernate.ddl-auto=update
spring.cache.type=simple
spring.cache.cache-names=cnpjCache
logging.level.com.cnpjfinder=DEBUG
```

- H2 em modo arquivo: os dados persistem em `./data/cnpjdb` entre execuções.
- H2 Console: habilitado — URL padrão `/h2-console` (use as credenciais do `application.properties`).
- Cache: `simple` (local, em memória) e método `consultarCnpj` possui `@Cacheable(value = "cnpjCache", key = "#cnpj")`.
- Fallback: implementado de forma sequencial via `try/catch` no serviço (`CnpjService`). Não há uso explícito de bibliotecas como Resilience4j no código atual.

---

## Requisitos
- Java 21 (JDK)
- Git

---

## Como executar

1. Clone o repositório:
```sh
git clone https://github.com/joaoalbertorsc/desafio-dio-api-consulta-cnpj
cd desafio-dio-api-consulta-cnpj
```

2. Build:
```sh
./gradlew build
```

3. Executar:
- Com Gradle:
```sh
./gradlew bootRun
```
- Ou executando o JAR gerado (substitua pelo nome real do JAR gerado em `build/libs`):
```sh
java -jar build/libs/*.jar
```

4. Executar testes:
```sh
./gradlew test
```

5. Acessos úteis após subir a aplicação:
- API: `http://localhost:8080`
- Swagger UI (SpringDoc): `http://localhost:8080/swagger-ui/index.html`
- H2 Console: `http://localhost:8080/h2-console` (credenciais conforme `application.properties`)

---

## Exemplos rápidos (curl)

- Consultar CNPJ:
```sh
curl -s http://localhost:8080/api/cnpj/01234567000189
```

- Buscar por razão social:
```sh
curl -s "http://localhost:8080/api/cnpj/buscar?razaoSocial=empresa"
```

- Estatísticas:
```sh
curl -s http://localhost:8080/api/cnpj/estatisticas
```

---

## Contribuindo
Pull requests são bem-vindos. Para mudanças maiores, abra uma issue descrevendo a proposta.
