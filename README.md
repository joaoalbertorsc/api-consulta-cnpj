# CNPJ Finder API

## Visão Geral

API RESTful em Java (Spring Boot) para consultar informações de empresas brasileiras a partir do CNPJ. A aplicação consulta serviços externos (BrasilAPI e ReceitaWS) e mantém uma persistência local em H2 (em modo arquivo) para cache/registro de consultas.

Principais pontos técnicos:
- Validação do CNPJ: formato (14 dígitos numéricos) e algoritmo de dígitos verificadores implementados no controller.
- Fallback entre APIs externas: a aplicação tenta primeiro a BrasilAPI; em caso de erro da API, faz fallback para a ReceitaWS (implementado de forma sequencial via try/catch no serviço).
- Cache: `@Cacheable` em `CnpjService.consultarCnpj` com `spring.cache.type=simple` configurado.
- H2: uso em modo de arquivo (`jdbc:h2:file:./data/cnpjdb`) com H2 Console habilitado para desenvolvimento.

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

Como se encaixa: o `CnpjController` valida o CNPJ e delega para `CnpjService`. O serviço verifica a fonte de verdade local (`EmpresaRepository`); na ausência do registro tenta consultar `BrasilApiClient` e, se necessário, `ReceitaWsClient`. Resultados válidos podem ser persistidos localmente.

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

Exemplo de requisição:
```sh
curl -s http://localhost:8080/api/cnpj/01234567000189
```

Exemplo de resposta (200):
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

Exemplo de erro (400):
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "CNPJ inválido: deve conter exatamente 14 dígitos numéricos"
}
```

2) Buscar empresas na base local
- Método: GET
- Path: `/api/cnpj/buscar`
- Query params (pelo menos um é obrigatório): `razaoSocial`, `municipio`, `uf`
- Observação importante: o controller verifica os parâmetros em ordem e retorna resultados baseados no primeiro parâmetro não-nulo (ordem: `razaoSocial` → `municipio` → `uf`). Se o comportamento desejado for combinar filtros (AND), recomenda-se ajustar a implementação do controller.
- Regras:
  - `uf` deve ter exatamente 2 caracteres (caso contrário, 400 Bad Request)
- Respostas:
  - `200 OK` — lista (possivelmente vazia) de `Empresa`
  - `400 Bad Request` — nenhum parâmetro fornecido ou `uf` com tamanho inválido

Exemplo:
```sh
curl -s "http://localhost:8080/api/cnpj/buscar?razaoSocial=empresa"
```

Exemplo de resposta (200):
```json
[
  {
    "cnpj": "01234567000189",
    "razaoSocial": "Empresa Exemplo SA",
    "municipio": "São Paulo",
    "uf": "SP"
  }
]
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

Exemplo de resposta (200):
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

---

## Configuração e comportamento importante
Arquivo principal: `src/main/resources/application.properties`.

As propriedades abaixo são exemplos para desenvolvimento local. NÃO use-as como configuração de produção sem revisar mecanismos de segurança, gestão de segredos e permissões.

Exemplo (dev):

```
spring.application.name=cnpj-finder
server.port=8080
# H2 em modo arquivo — dados persistem em ./data/cnpjdb
spring.datasource.url=jdbc:h2:file:./data/cnpjdb
spring.datasource.username=sa
spring.datasource.password=password
spring.h2.console.enabled=true
spring.jpa.hibernate.ddl-auto=update
spring.cache.type=simple
spring.cache.cache-names=cnpjCache
logging.level.com.cnpjfinder=DEBUG
```

Boas práticas de configuração:
- Trate `spring.datasource.username` e `spring.datasource.password` como exemplos: use variáveis de ambiente ou um arquivo `application-local.properties` (não comitado) para sobrepor credenciais.
- Em produção, utilize um banco relacional adequado (Postgres, MySQL, etc.) e desative o H2 Console.
- Para segredos (chaves de API, tokens), utilize gerenciadores de segredos ou variáveis de ambiente; nunca commit esses valores no repositório.

Observações:
- H2 em modo arquivo: os dados persistem em `./data/cnpjdb` entre execuções; inclua essa pasta no `.gitignore` se não quiser versionar dados locais.
- Cache: `simple` (local, em memória) e método `consultarCnpj` possui `@Cacheable(value = "cnpjCache", key = "#cnpj")`.
- Fallback: implementado de forma sequencial via `try/catch` no serviço (`CnpjService`). Não há uso explícito de bibliotecas como Resilience4j no código atual.

---

## Requisitos
- Java 21 (JDK)
- Git

---

## Como executar (desenvolvimento)

1. Clone o repositório:
```sh
git clone https://github.com/joaoalbertorsc/api-consulta-cnpj.git
cd api-consulta-cnpj
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
- H2 Console: `http://localhost:8080/h2-console` (credenciais conforme `application.properties`, apenas em desenvolvimento)

---

## Segurança e boas práticas antes de publicar
- Revise o repositório com ferramentas de detecção de segredos (ex.: `git-secrets`, `truffleHog`) antes de tornar o repositório público.
- Não exponha tokens ou chaves de API no README ou em arquivos versionados.
- Considere adicionar rate-limiting e/ou autenticação para evitar uso indevido das APIs externas.

---

## Docker (opcional)
Se desejar empacotar com Docker, adicione um Dockerfile ao repositório. Exemplo mínimo (não incluído por padrão neste repo):

```
FROM eclipse-temurin:21-jre
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

---

## Observações e recomendações
- Corrigido o comando de `cd` após o clone para o nome correto do repositório.
- A busca `/buscar` atualmente utiliza um parâmetro por vez (comportamento do controller). Se desejar combinar filtros (AND), atualize a implementação no controller e repository.
- Adicione exemplos reais de request/response JSON (alguns foram incluídos acima). Para entradas/saídas completas, consulte os DTOs em `src/main/java/com/cnpjfinder/model`.

---

## Contribuindo
Contribuições são bem-vindas. Para mudanças maiores, abra uma issue descrevendo a proposta.

Básico para PRs:
- Fork -> Branch com nome descritivo -> Commit -> Pull Request
- Escreva testes quando aplicar mudanças comportamentais
- Atualize o README quando adicionar novas configurações ou endpoints

---

## Licença
Este repositório não contém um arquivo LICENSE por padrão. Para publicar publicamente, adicione um arquivo `LICENSE` apropriado (por exemplo, MIT, Apache-2.0) e mencione a licença aqui.

---

Se quiser, eu posso também:
- criar um `LICENSE` (MIT or Apache-2.0) e adicionar badges de CI se você me informar os detalhes do CI;
- abrir um PR em vez de commitar na branch padrão;
- gerar um diff/patch em vez de commitar diretamente.
