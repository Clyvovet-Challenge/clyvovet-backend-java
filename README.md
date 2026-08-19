# CLYVO VET — Backend Java

API REST desenvolvida em Spring Boot para o sistema de saúde contínua de pets **CLYVO VET**, como parte do Challenge FIAP 2026 — 1º Semestre.

---

## Integrantes do Grupo

| Nome | RM |
|---|---|
| Fabrício Henrique Pereira | RM 563237 |
| Leonardo José Pereira | RM 563065 |
| Miguel Henrique Oliveira Dias | RM 565492 |
| Pedro Henrique de Oliveira | RM 562312 |

---

## Descrição do Projeto

O CLYVO VET é uma plataforma digital de saúde animal que conecta tutores de pets, veterinários e clínicas parceiras. A solução promove a continuidade do cuidado preventivo, centralizando o histórico clínico dos animais, o agendamento de eventos e o controle de pagamentos.

Este repositório contém o **backend Java/Spring Boot**, responsável por expor uma API REST completa com persistência em banco Oracle (FIAP) ou H2 (desenvolvimento local).

### Benefícios para o Negócio

- Centralização do histórico clínico do pet, acessível para tutores e clínicas
- Redução do gap de continuidade no cuidado preventivo
- Agendamento e controle de eventos clínicos com rastreabilidade completa
- Geração de dados longitudinais para decisões clínicas mais assertivas
- Escalável para múltiplas clínicas e hospitais parceiros

---

## Arquitetura Macro

```
┌─────────────┐     HTTP/REST      ┌──────────────────────────┐
│  Front-end  │ ────────────────►  │  Spring Boot API (8080)  │
│  / Mobile   │                    │                          │
└─────────────┘                    │  Controller              │
                                   │  Service (Cache)         │
                                   │  Repository (JPA)        │
                                   │  Entity (@Column map)    │
                                   └────────────┬─────────────┘
                                                │
                                   ┌────────────▼─────────────┐
                                   │   Oracle 19c (FIAP)      │
                                   │   ou H2 (dev local)      │
                                   └──────────────────────────┘
```

**Stack:** Java 17 · Spring Boot 3.5 · JPA/Hibernate 6.6 · Oracle 19c · H2 · Swagger/OpenAPI · Bean Validation · Spring Cache · Lombok

---

## Documentação Técnica

A documentação completa do projeto está na pasta [`docs/`](docs/):

| Documento | Conteúdo |
|---|---|
| [Arquitetura](docs/01-arquitetura.md) | Camadas, fluxo de requisição, cache, tratamento de erros |
| [Modelo de Dados](docs/02-modelo-de-dados.md) | Entidades, relacionamentos, enums, schema Oracle |
| [API REST](docs/03-api-rest.md) | Os 30 endpoints em detalhe, filtros, contratos, erros |
| [Configuração](docs/04-configuracao.md) | Perfis Spring, propriedades, como rodar |
| [Deploy](docs/05-deploy.md) | Docker, docker-compose, provisionamento Azure |
| [Guia de Desenvolvimento](docs/06-guia-de-desenvolvimento.md) | Convenções, como adicionar entidades, testes |
| [Pendências e Divergências](docs/07-pendencias-e-divergencias.md) | Inconsistências conhecidas entre código, banco e docs |

---

## Pré-requisitos

- Java 17+
- Maven 3.8+
- Acesso ao Oracle FIAP **ou** rodar localmente com H2

---

## Instalação e Execução

### 1. Clonar o repositório

```bash
git clone https://github.com/Clyvovet-Challenge/clyvovet-backend-java.git
cd clyvovet-backend-java
```

### 2. Configurar o banco de dados

#### Opção A — Oracle FIAP (produção)

Antes de iniciar a aplicação, execute o script SQL no SQL Developer:

```
src/main/resources/db/db-oracle.sql
```

Este script cria as tabelas, índices, views, procedures e dados de seed. Execute com **Run Script (F5)** conectado ao Oracle da FIAP com suas credenciais.

Ative o perfil Oracle:

```properties
# application.properties
spring.profiles.active=oracle
```

Configure suas credenciais em `application-oracle.properties`:

```properties
spring.datasource.url=jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL
spring.datasource.username=SEU_RM
spring.datasource.password=SUA_SENHA
```

#### Opção B — H2 (desenvolvimento local, sem Oracle)

Ative o perfil H2:

```properties
# application.properties
spring.profiles.active=h2
```

### 3. Executar

```bash
mvn spring-boot:run
```

A aplicação sobe em `http://localhost:8080`

### 4. Acessar o Swagger

```
http://localhost:8080/swagger-ui.html
```

---

## Endpoints da API

### Tutores — `/tutores`

| Método | Rota | Descrição |
|---|---|---|
| GET | `/tutores` | Lista todos (paginado). Filtros: nome, cidade |
| GET | `/tutores/{id}` | Busca por ID |
| POST | `/tutores` | Cadastra novo tutor |
| PUT | `/tutores/{id}` | Atualiza tutor |
| DELETE | `/tutores/{id}` | Remove tutor |

### Animais — `/animais`

| Método | Rota | Descrição |
|---|---|---|
| GET | `/animais` | Lista todos (paginado). Filtros: nome, especie |
| GET | `/animais/{id}` | Busca por ID |
| POST | `/animais` | Cadastra novo animal |
| PUT | `/animais/{id}` | Atualiza animal |
| DELETE | `/animais/{id}` | Remove animal |

### Clínicas — `/clinicas`

| Método | Rota | Descrição |
|---|---|---|
| GET | `/clinicas` | Lista todas (paginado). Filtros: nome, cidade |
| GET | `/clinicas/{id}` | Busca por ID |
| POST | `/clinicas` | Cadastra nova clínica |
| PUT | `/clinicas/{id}` | Atualiza clínica |
| DELETE | `/clinicas/{id}` | Remove clínica |

### Veterinários — `/veterinarios`

| Método | Rota | Descrição |
|---|---|---|
| GET | `/veterinarios` | Lista todos (paginado). Filtros: nome, especialidade |
| GET | `/veterinarios/{id}` | Busca por ID |
| POST | `/veterinarios` | Cadastra novo veterinário |
| PUT | `/veterinarios/{id}` | Atualiza veterinário |
| DELETE | `/veterinarios/{id}` | Remove veterinário |

### Eventos Clínicos — `/eventos-clinicos`

| Método | Rota | Descrição |
|---|---|---|
| GET | `/eventos-clinicos` | Lista todos (paginado). Filtros: tipoEvento, animalNome |
| GET | `/eventos-clinicos/{id}` | Busca por ID |
| POST | `/eventos-clinicos` | Cadastra novo evento |
| PUT | `/eventos-clinicos/{id}` | Atualiza evento |
| DELETE | `/eventos-clinicos/{id}` | Remove evento |

Valores válidos para `tipoEvento`: `CONSULTA` `RETORNO` `VACINA` `EXAME` `CIRURGIA` `OUTRO`

### Pagamentos — `/pagamentos`

| Método | Rota | Descrição |
|---|---|---|
| GET | `/pagamentos` | Lista todos (paginado). Filtros: statusPagamento, formaPagamento |
| GET | `/pagamentos/{id}` | Busca por ID |
| POST | `/pagamentos` | Registra novo pagamento |
| PUT | `/pagamentos/{id}` | Atualiza pagamento |
| DELETE | `/pagamentos/{id}` | Remove pagamento |

Valores válidos para `statusPagamento`: `PENDENTE` `PAGO` `CANCELADO` `REEMBOLSADO`

Valores válidos para `formaPagamento`: `PIX` `CARTAO` `DINHEIRO` `BOLETO`

---

## Exemplos de Uso

### Paginação, ordenação e filtros

```
GET /animais?page=0&size=5&sort=nome,asc
GET /tutores?nome=Lucas&page=0&size=10
GET /veterinarios?especialidade=Cardiologia
GET /eventos-clinicos?tipoEvento=VACINA
GET /pagamentos?statusPagamento=PENDENTE
GET /pagamentos?formaPagamento=PIX
```

### Exemplo de POST — Criar Tutor

```json
POST /tutores
{
  "nome": "João Silva",
  "cpf": "12345678989",
  "email": "silva@email.com",
  "telefone": "11999999990",
  "sexo": "MASCULINO",
  "dataNascimento": "1990-01-15",
  "endereco": {
    "logradouro": "Av. Paulista",
    "numero": "1000",
    "bairro": "Bela Vista",
    "cidade": "São Paulo",
    "estado": "SP",
    "cep": "01310100"
  }
}
```

### Exemplo de POST — Criar Animal

```json
POST /animais
{
  "nome": "Thor",
  "raca": "Golden Retriever",
  "especie": "CACHORRO",
  "porte": "GRANDE",
  "cor": "Dourado",
  "sexo": "MACHO",
  "dataNascimento": "2021-03-10",
  "observacao": "Animal saudável",
  "tutorId": "uuid-do-tutor"
}
```

Valores válidos para `sexo` (animal): `MACHO` `FEMEA` `DESCONHECIDO`

### Exemplo de POST — Criar Evento Clínico

```json
POST /eventos-clinicos
{
  "data": "2025-05-20",
  "hora": "10:00",
  "descricao": "Consulta de rotina",
  "tipoEvento": "CONSULTA",
  "veterinarioId": "uuid-do-veterinario",
  "animalId": "uuid-do-animal",
  "clinicaId": "uuid-da-clinica"
}
```

### Exemplo de POST — Criar Pagamento

```json
POST /pagamentos
{
  "formaPagamento": "PIX",
  "valor": 150.00,
  "dataPagamento": "2025-05-20",
  "descricao": "Pagamento consulta de rotina",
  "statusPagamento": "PAGO",
  "eventoClinicoId": "uuid-do-evento"
}
```

---

## Requisitos Técnicos Implementados

| Requisito | Status |
|---|---|
| Bean Validation nos Requests | ✅ todas as 6 entidades |
| Paginação de resultados | ✅ todas as 6 entidades |
| Ordenação de resultados | ✅ todas as 6 entidades |
| Busca com parâmetros | ✅ todas as 6 entidades |
| Cache para otimizar requisições | ✅ todas as 6 entidades |
| Tratamento de erros/exceções | ✅ GlobalExceptionHandler + EntityNotFoundException |
| DTOs (Request/Response) | ✅ todas as 6 entidades |
| Documentação com Swagger | ✅ /swagger-ui.html |

---

## Estrutura do Projeto

```
clyvovet-backend-java/
│
│  ── O que o código faz hoje. Acompanha o código e é atualizada com ele.
├── docs/
│   ├── README.md                       # Índice desta pasta
│   ├── 01-arquitetura.md               # Camadas, fluxo de requisição, cache, erros
│   ├── 02-modelo-de-dados.md           # Entidades JPA, relacionamentos, enums
│   ├── 03-api-rest.md                  # Endpoints, contratos, status codes
│   ├── 04-configuracao.md              # Perfis, migrations, variáveis de ambiente
│   ├── 05-deploy.md                    # Docker, docker-compose, VM Azure
│   ├── 06-guia-de-desenvolvimento.md   # Convenções, como adicionar entidades
│   ├── 07-pendencias-e-divergencias.md # Divergências conhecidas e seu status
│   └── 08-seguranca.md                 # JWT, perfis de acesso, ownership
│
│  ── O que o Challenge exige. Registro congelado, extraído dos PDFs oficiais.
├── specs/
│   ├── README.md
│   ├── 01-sprint-1-2.md                # Requisitos do 1º semestre + pontuação
│   ├── 02-sprint-3.md                  # Frontend, Flyway e Spring Security
│   ├── 03-sprint-4.md                  # Entrega final
│   ├── 04-dependencias-externas.md     # O que outras disciplinas exigem daqui
│   ├── 05-plano-de-implementacao.md    # Backlog derivado
│   └── 06-checklist-pre-sprint-3.md
│
│  ── Artefatos de entrega. O nome da pasta é exigido pela rubrica da FIAP.
├── documentos/
│   ├── Cronograma_CLYVOVET.pdf
│   ├── Diagrama_De_Classes.pdf
│   └── Post_*.png                      # Capturas dos POSTs testados
│
├── src/
│   ├── main/
│   │   ├── java/br/com/fiap/clyvovet/
│   │   │   ├── config/                 # Cache, OpenAPI, seed de desenvolvimento
│   │   │   ├── controller/             # Controllers REST
│   │   │   ├── dto/                    # DTOs de Request e Response
│   │   │   ├── exception/              # GlobalExceptionHandler
│   │   │   ├── mapper/                 # Conversão Entity ↔ DTO
│   │   │   ├── model/                  # Entidades JPA + enums
│   │   │   ├── repository/             # JPA Repositories
│   │   │   ├── security/               # JWT, filtros, ownership
│   │   │   ├── service/                # Regras de negócio + cache
│   │   │   └── ClyvovetApplication.java
│   │   └── resources/
│   │       ├── db/
│   │       │   ├── db-oracle.sql       # DDL original, hoje só referência histórica
│   │       │   └── migration/          # Schema versionado pelo Flyway
│   │       │       ├── README.md       # Por que há dois conjuntos
│   │       │       ├── oracle/         # V1..V4 — Oracle e H2 (MODE=Oracle)
│   │       │       └── mysql/          # V1..V4 — Azure Database for MySQL
│   │       ├── application.properties
│   │       ├── application-oracle.properties
│   │       ├── application-mysql.properties
│   │       ├── application-h2.properties
│   │       └── application-dev.properties
│   └── test/java/br/com/fiap/clyvovet/
│       ├── crud/                       # CRUD e integração por entidade
│       ├── mapper/
│       ├── migration/                  # Migrations MySQL sobre H2 em MODE=MySQL
│       ├── security/                   # JWT, bloqueio de conta, ownership
│       └── support/
│
├── Dockerfile
├── docker-compose.yml
├── deploy.sh                           # Script Azure CLI para deploy em VM Linux
├── pom.xml
└── mvnw / mvnw.cmd / .mvn/             # Maven Wrapper
```

As três pastas de documentação existem por motivos diferentes e não devem ser
fundidas: `specs/` é o que foi **pedido** (congelado), `docs/` é o que foi
**construído** (vivo, acompanha o código) e `documentos/` são os **artefatos de
entrega** — cujo nome a rubrica do Challenge fixa.

---

## Testando os Endpoints

> ⚠️ **A coleção exportada do Insomnia/Postman ainda não está no repositório.** A
> pasta `documentos/` tem apenas as capturas de tela dos POSTs. A rubrica do
> Challenge pede o export **na pasta `documentos/`** e vale até 10 pontos — ver
> [`specs/06-checklist-pre-sprint-3.md`](specs/06-checklist-pre-sprint-3.md).

Enquanto isso, teste pelo Swagger em:

```
http://localhost:8080/swagger-ui.html
```

---

## Deploy em Nuvem (DevOps)

O projeto inclui suporte completo para containerização e deploy na Azure:

```bash
# Build e execução local com Docker
docker-compose up --build

# Deploy em VM Linux na Azure (Azure CLI necessário)
bash deploy.sh
```

O `deploy.sh` provisiona automaticamente uma VM Linux na Azure, instala Docker e Git, e sobe a aplicação via `docker-compose`.