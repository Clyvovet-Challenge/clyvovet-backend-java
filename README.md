# CLYVO VET — Backend Java

API REST em Spring Boot para o CLYVO VET, plataforma de saúde contínua para pets.
Challenge FIAP 2026 — 2º ano ADS.

**Stack:** Java 17 · Spring Boot 3.5 · Spring Security (JWT) · JPA/Hibernate 6.6 ·
Flyway · Oracle 19c / MySQL / H2 · OpenAPI 3

---

## Integrantes do Grupo

| Nome | RM |
|---|---|
| Fabrício Henrique Pereira | RM 563237 |
| Leonardo José Pereira | RM 563065 |
| Miguel Henrique Oliveira Dias | RM 565492 |
| Pedro Henrique de Oliveira | RM 562312 |

---

## Começando

Para rodar localmente **não é preciso configurar nada** — o perfil `dev` sobe um H2
em memória, aplica as migrations e cria usuários de teste:

```bash
git clone https://github.com/Clyvovet-Challenge/clyvovet-backend-java.git
cd clyvovet-backend-java
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

A aplicação sobe em `http://localhost:8080` e o Swagger fica em
`http://localhost:8080/swagger-ui.html`.

> ⚠️ O perfil ativo por padrão é `oracle`, não `dev`. Rodar `./mvnw spring-boot:run`
> sem argumento faz a aplicação tentar conectar no Oracle da FIAP e falhar se as
> variáveis de ambiente não estiverem definidas.

### A API exige autenticação

Todos os endpoints de domínio pedem um token JWT. O perfil `dev` semeia estes
usuários — as senhas valem apenas em desenvolvimento:

| E-mail | Senha | Perfil |
|---|---|---|
| `admin@clyvovet.com` | `admin12345` | ADMIN |
| `camila.ferreira@vetcare.com.br` | `vet12345` | VETERINARIO |
| `lucas.santos@email.com` | `tutor12345` | TUTOR |
| `maria.oliveira@email.com` | `tutor12345` | TUTOR |

Obtenha um token e use-o no cabeçalho `Authorization`:

```bash
# Devolve {"accessToken": "...", "refreshToken": "...", ...}
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@clyvovet.com","senha":"admin12345"}'

curl http://localhost:8080/api/v1/animais \
  -H 'Authorization: Bearer <accessToken>'
```

Sem o cabeçalho, a resposta é **401**. Com um token de perfil insuficiente, **403**.

### Outros perfis

| Perfil | Banco | Quando usar |
|---|---|---|
| `dev` | H2 em memória | desenvolvimento local — **recomendado** |
| `h2` | H2 em modo servidor | dentro do docker-compose (o host `clyvovet-db` só existe lá) |
| `oracle` | Oracle 19c FIAP | entrega e banco de testes |
| `mysql` | Azure Database for MySQL | alvo do deploy — [ainda não validado](docs/07-pendencias-e-divergencias.md) |

Os perfis `oracle` e `mysql` não têm credenciais no código: elas vêm do ambiente, e a
aplicação não sobe sem elas.

```bash
export DB_USERNAME=rm000000
export DB_PASSWORD=sua-senha
export JWT_SECRET=$(openssl rand -base64 32)

./mvnw spring-boot:run -Dspring-boot.run.profiles=oracle
```

O schema é criado e versionado pelo **Flyway** — não é preciso rodar SQL à mão. O
`src/main/resources/db/db-oracle.sql` é o DDL original e hoje serve só como
referência histórica. Detalhes em [`docs/04-configuracao.md`](docs/04-configuracao.md).

---

## Arquitetura

```
┌─────────────┐    HTTP + Bearer JWT    ┌────────────────────────────────┐
│  Front-end  │ ──────────────────────► │   Spring Boot API (8080)       │
│  / Mobile   │                         │                                │
└─────────────┘                         │   JwtAuthenticationFilter      │
                                        │   Controller                   │
                                        │   Service (cache + ownership)  │
                                        │   Repository (JPA)             │
                                        │   Entity                       │
                                        └───────────────┬────────────────┘
                                                        │  Flyway
                                        ┌───────────────▼────────────────┐
                                        │  Oracle 19c · MySQL · H2       │
                                        └────────────────────────────────┘
```

Cada requisição passa pelo filtro JWT antes de chegar ao controller. Além do perfil,
há uma regra de **ownership**: um tutor só enxerga os próprios pets — inclusive no
cache, cuja chave inclui o `tutorId`.

---

## Endpoints

### Autenticação — `/api/v1/auth`

| Método | Rota | Acesso |
|---|---|---|
| POST | `/api/v1/auth/login` | público |
| POST | `/api/v1/auth/refresh` | público |
| POST | `/api/v1/auth/logout` | público |
| POST | `/api/v1/auth/registrar` | público — o perfil é sempre TUTOR |
| POST | `/api/v1/auth/usuarios` | ADMIN — cria com perfil arbitrário |
| GET | `/api/v1/auth/me` | autenticado |

### Recursos de domínio

Todos os endpoints ficam sob **`/api/v1`**. Os seis recursos expõem o mesmo CRUD:

| Método | Rota | O que faz |
|---|---|---|
| GET | `/` | lista paginada, com filtros |
| GET | `/{id}` | busca por id |
| POST | `/` | cria — devolve **201** |
| PUT | `/{id}` | substitui o recurso inteiro |
| PATCH | `/{id}` | altera **só os campos enviados** |
| DELETE | `/{id}` | remove — devolve **204** |

| Recurso | Filtros na listagem |
|---|---|
| `/api/v1/tutores` | `nome`, `cidade` |
| `/api/v1/animais` | `nome`, `especie` |
| `/api/v1/clinicas` | `nome`, `cidade` |
| `/api/v1/veterinarios` | `nome`, `especialidade` |
| `/api/v1/eventos-clinicos` | `tipoEvento`, `animalNome` |
| `/api/v1/pagamentos` | `statusPagamento`, `formaPagamento` |

```
GET /api/v1/animais?page=0&size=5&sort=nome,asc
GET /api/v1/tutores?nome=Lucas&page=0&size=10
GET /api/v1/pagamentos?statusPagamento=PENDENTE
```

Toda listagem responde no mesmo formato:

```json
{
  "content": [ ... ],
  "page": { "size": 10, "number": 0, "totalElements": 42, "totalPages": 5 }
}
```

O PATCH aceita só os campos que mudam — o que não vier no corpo fica como está:

```bash
curl -X PATCH http://localhost:8080/api/v1/clinicas/{id} \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{"telefone":"1199998888"}'
```

Um campo omitido não é apagado: para limpar um campo opcional, use PUT.

### Quem pode o quê

| Operação | TUTOR | VETERINARIO | ADMIN |
|---|:---:|:---:|:---:|
| Ler animais | só os próprios | ✅ | ✅ |
| Criar e editar animais | só os próprios | ✅ | ✅ |
| Listar e criar tutores | — | ✅ | ✅ |
| Criar e editar eventos e pagamentos | — | ✅ | ✅ |
| Criar e editar clínicas e veterinários | — | — | ✅ |

Contratos completos, códigos de status e exemplos de payload estão em
[`docs/03-api-rest.md`](docs/03-api-rest.md). A matriz de autorização detalhada está
em [`docs/08-seguranca.md`](docs/08-seguranca.md).

### Valores de enum

| Campo | Valores |
|---|---|
| `tipoEvento` | `CONSULTA` `RETORNO` `VACINA` `EXAME` `CIRURGIA` `OUTRO` |
| `statusPagamento` | `PENDENTE` `PAGO` `CANCELADO` `REEMBOLSADO` |
| `formaPagamento` | `PIX` `CARTAO` `DINHEIRO` `BOLETO` |
| `sexo` (tutor, veterinário) | `MASCULINO` `FEMININO` `OUTRO` |
| `sexo` (animal) | `MACHO` `FEMEA` `DESCONHECIDO` |
| `porte` | `PEQUENO` `MEDIO` `GRANDE` |

---

## Documentação Técnica

| Documento | Conteúdo |
|---|---|
| [Funcionalidades](docs/00-funcionalidades.md) | **Comece aqui.** O que o sistema faz, fluxo de ponta a ponta, o que ainda não existe |
| [Arquitetura](docs/01-arquitetura.md) | Camadas, fluxo de requisição, cache, tratamento de erros |
| [Modelo de Dados](docs/02-modelo-de-dados.md) | Entidades, relacionamentos, enums, mapeamento objeto↔tabela |
| [API REST](docs/03-api-rest.md) | Endpoints em detalhe, filtros, contratos, erros |
| [Configuração](docs/04-configuracao.md) | Perfis, migrations, variáveis de ambiente |
| [Deploy](docs/05-deploy.md) | Docker, docker-compose, provisionamento Azure |
| [Guia de Desenvolvimento](docs/06-guia-de-desenvolvimento.md) | Convenções, como adicionar entidades, testes |
| [Pendências e Divergências](docs/07-pendencias-e-divergencias.md) | Inconsistências conhecidas e seu status |
| [Segurança](docs/08-seguranca.md) | JWT, perfis, ownership, bloqueio de conta |
| [Estado do Projeto](docs/09-estado-do-projeto.md) | Onde estamos, o que falta para atacar o absenteísmo e em que ordem |

Os requisitos do Challenge que originaram o projeto estão em
[`specs/`](specs/README.md).

---

## Deploy

```bash
# Local, com Docker
JWT_SECRET=$(openssl rand -base64 32) docker compose up --build

# VM Linux na Azure (requer Azure CLI autenticado)
bash deploy.sh
```

O `deploy.sh` provisiona a VM, instala Docker, clona o repositório de forma rasa e
esparsa e sobe o compose. É idempotente: num redeploy ele atualiza o clone em vez de
falhar. Ver [`docs/05-deploy.md`](docs/05-deploy.md).

---

## Testes

```bash
./mvnw test
```

São **205 testes** cobrindo CRUD e integração, mappers, JWT, bloqueio de conta,
ownership, autorização por recurso, os dois fluxos não-CRUD, o acesso ao
histórico em três níveis e as migrations do MySQL.

---

## Coleção da API

[`documentos/clyvovet-api.postman_collection.json`](documentos/clyvovet-api.postman_collection.json)
— **71 requisições em 12 pastas**, cobrindo todos os endpoints.

Formato Postman v2.1. O Insomnia importa esse formato sem perda, e o contrário
não é verdade — um arquivo só atende os dois.

**Como usar:**

1. Suba a aplicação com `./mvnw spring-boot:run` (perfil `dev`, H2 em memória).
2. Rode as três requisições de `0. Autenticação`. Elas guardam os tokens de
   admin, veterinária e tutor nas variáveis da coleção; o resto já usa o token
   do perfil certo em cada rota.
3. Os ids são os do seed da migration V2, então tudo funciona num banco
   recém-criado, sem precisar cadastrar nada antes.

As pastas **8, 9 e 10** são os fluxos de negócio — agendamento, retorno e
acesso ao histórico. É onde está a regra, e não o CRUD.

---

## Requisitos Técnicos Implementados

| Requisito | Status |
|---|---|
| Bean Validation nos Requests | ✅ todas as 6 entidades |
| HATEOAS — nível 3 de Richardson | ✅ links condicionais ao estado em animal e evento clínico |
| Coleção da API para import | ✅ `documentos/clyvovet-api.postman_collection.json` |
| Paginação de resultados | ✅ todas as 6 entidades |
| Ordenação de resultados | ✅ todas as 6 entidades |
| Busca com parâmetros | ✅ todas as 6 entidades |
| Cache para otimizar requisições | ✅ todas as 6 entidades |
| Tratamento de erros/exceções | ✅ `GlobalExceptionHandler` |
| DTOs (Request/Response) | ✅ todas as 6 entidades |
| Documentação com Swagger | ✅ `/swagger-ui.html` |
| Autenticação e autorização | ✅ JWT + perfis + ownership |
| Schema versionado | ✅ Flyway, um conjunto por banco |
| Versionamento da API | ✅ prefixo `/api/v1` |
| Atualização parcial | ✅ PATCH nos 6 recursos |

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
│   ├── clyvovet-api.postman_collection.json   # 71 requisições, todos os endpoints
│   ├── script_bd.sql                   # DDL completo, gerado das migrations
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
