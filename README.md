# CNPJ Finder API

## Visão Geral

Este projeto é uma API RESTful desenvolvida com Spring Boot para consultar informações de empresas brasileiras a partir de seu CNPJ. A API integra-se com serviços externos (BrasilAPI e ReceitaWS) para obter os dados e utiliza um banco de dados local (H2) como cache para otimizar consultas futuras.

A aplicação foi projetada com foco em robustez, validação de dados e tratamento de erros, oferecendo uma interface clara e consistente para os consumidores da API.

## Funcionalidades

- **Consulta de CNPJ**: Obtém dados detalhados de uma empresa através de seu CNPJ.
- **Validação de Formato na Entrada**: A API valida se o CNPJ recebido está no formato de 14 dígitos numéricos.
- **Fallback de API Externa**: Utiliza a BrasilAPI como fonte primária de dados e, em caso de falha, recorre à ReceitaWS como fonte secundária.
- **Cache em Banco de Dados**: Armazena os resultados de consultas bem-sucedidas em um banco de dados local para acelerar requisições futuras e reduzir a dependência dos serviços externos.
- **Busca Local**: Permite filtrar e buscar empresas já armazenadas no banco de dados por Razão Social, Município ou UF.
- **Estatísticas de Uso**: Oferece um endpoint que retorna estatísticas sobre as consultas realizadas, como o número total de empresas no banco e as mais consultadas.
- **Documentação de API**: Integra o SpringDoc OpenAPI para gerar uma interface Swagger UI, facilitando a exploração e o teste dos endpoints.
- **Tratamento de Erros**: Respostas de erro padronizadas para requisições inválidas, recursos não encontrados ou falhas nos serviços externos.

## Tecnologias Utilizadas

- Java 21
- Spring Boot 3.2.5
- Spring Web
- Spring Data JPA
- Spring Cache
- H2 Database (In-Memory)
- Lombok
- SpringDoc OpenAPI (Swagger)
- Gradle

## Documentação da API

Após iniciar a aplicação, a documentação completa da API pode ser acessada através do Swagger UI no seguinte endereço:

http://localhost:8080/swagger-ui/index.html

### Endpoints Principais

#### 1. Consultar um CNPJ

Obtém os dados de uma empresa específica.

- **Método**: `GET`
- **Path**: `/api/cnpj/{cnpj}`

- **Path Variable**:
  - `cnpj` (String): O número do CNPJ a ser consultado, contendo exatamente 14 dígitos numéricos (`XXXXXXXXXXXXXX`).

- **Respostas de Sucesso**:
  - `200 OK`: Retorna um objeto JSON com os dados da empresa.

- **Respostas de Erro**:
  - `400 Bad Request`: Se o CNPJ fornecido não tiver 14 dígitos numéricos ou se falhar na validação do algoritmo (dígitos verificadores).
  - `404 Not Found`: Se o CNPJ for válido, mas não for encontrado em nenhuma das fontes de dados (APIs externas ou banco local).
  - `503 Service Unavailable`: Se ambas as APIs externas estiverem indisponíveis ou retornarem erros inesperados.

#### 2. Buscar Empresas na Base Local

Realiza uma busca por empresas que já foram consultadas e estão salvas no banco de dados local.

- **Método**: `GET`
- **Path**: `/api/cnpj/buscar`

- **Query Parameters**:
  - `razaoSocial` (String, opcional): Filtra empresas por parte da razão social (case-insensitive).
  - `municipio` (String, opcional): Filtra empresas por parte do nome do município (case-insensitive).
  - `uf` (String, opcional): Filtra empresas pela sigla do estado (UF). Deve conter exatamente 2 caracteres.

- **Regras**:
  - Pelo menos um dos três parâmetros de busca deve ser fornecido.

- **Respostas de Sucesso**:
  - `200 OK`: Retorna uma lista (potencialmente vazia) de objetos de empresas que correspondem ao critério de busca.

- **Respostas de Erro**:
  - `400 Bad Request`: Se nenhum parâmetro de busca for fornecido ou se o parâmetro `uf` não tiver 2 caracteres.

#### 3. Obter Estatísticas

Retorna estatísticas de uso da API baseadas nos dados armazenados localmente.

- **Método**: `GET`
- **Path**: `/api/cnpj/estatisticas`

- **Respostas de Sucesso**:
  - `200 OK`: Retorna um objeto JSON contendo:
    - `totalEmpresas`: O número total de empresas únicas no banco de dados.
    - `totalConsultas`: A soma de todas as consultas realizadas (incluindo consultas repetidas ao mesmo CNPJ).
    - `empresasMaisConsultadas`: Uma lista com as 10 empresas mais consultadas.

## Como Executar o Projeto

### Pré-requisitos

- **Java JDK 21** ou superior.
- Gradle 7.x ou superior (ou utilize o Gradle Wrapper incluído no projeto).

### Passos

1. **Clone o repositório:**
   ```sh
   git clone https://github.com/joaoalbertorsc/desafio-dio-api-consulta-cnpj
   cd desafio-dio-spring
   ```

2. **Construa o projeto com Gradle:**
   No terminal, na raiz do projeto, execute:
   ```sh
   ./gradlew build
   ```
   (ou `gradlew.bat build` no Windows)

3. **Execute a aplicação:**
   Você pode executar a aplicação de duas maneiras:

   - **Usando o Gradle:**
     ```sh
     ./gradlew bootRun
     ```

   - **Executando o arquivo JAR (após a construção):**
     ```sh
     java -jar build/libs/desafio-dio-spring-1.0.0.jar
     ```

4. **Acesse a aplicação:**
   A API estará disponível em `http://localhost:8080`.
