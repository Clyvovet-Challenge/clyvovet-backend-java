# CLYVO VET — Backend Java

API REST do Challenge FIAP 2026 (disciplina Java Advanced). Conecta tutores, veterinários
e clínicas em torno do histórico clínico do pet, que atravessa clínicas. A tese do
produto é combater o **absenteísmo** (retorno e falta). Visão de negócio em
[`docs/00-funcionalidades.md`](docs/00-funcionalidades.md); estado atual em
[`docs/09-estado-do-projeto.md`](docs/09-estado-do-projeto.md).

## Branch: a `main` está congelada

A `main` é a entrega da Sprint 3 (nota 34/100, saiu em 04/10/2026). Todo trabalho da
Sprint 4 vai para a branch **`sprint-4`**. Nada é commitado nem mergeado na `main` até o
Leonardo pedir o merge (via PR).

O CI (`.github/workflows/ci.yml`) só roda em push na `main` e em PR para a `main`. Push
na `sprint-4` **não** dispara CI: rode a suíte localmente antes de cada commit.

## Stack

Spring Boot 3.5 · Java 17 · Spring Data JPA · Flyway · Spring Security + JWT (jjwt) ·
Caffeine (cache) · HATEOAS · springdoc (Swagger) · Bucket4j (rate limit) · Actuator ·
Lombok · H2 / Oracle / MySQL.

## Comandos

```bash
./mvnw test                                              # suíte inteira, H2 em memória (perfil dev)
PERFIL_TESTE=oracle ./mvnw test                          # mesma suíte contra o Oracle da FIAP
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev    # sobe local em :8080, sem configurar nada
```

Swagger em `http://localhost:8080/swagger-ui.html`. O perfil `dev` cria usuários de
teste (ver `config/DevDataSeeder`).

## Perfis

| Perfil | Banco | Uso |
|---|---|---|
| `dev` | H2 em memória | desenvolvimento e testes |
| `h2` | H2 servidor | dentro do `docker-compose.yml` |
| `oracle` | Oracle FIAP | banco da disciplina de Database |
| `mysql` | Azure Database for MySQL | deploy na Azure |

O perfil padrão em `application.properties` é **`mysql`**: rodar `spring-boot:run` sem
`-Dspring-boot.run.profiles` tenta conectar na Azure. `oracle` e `mysql` leem as
credenciais do ambiente (`DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`) e não sobem sem elas.

## Mapa do código

`src/main/java/br/com/fiap/clyvovet/`

| Pacote | Papel |
|---|---|
| `controller` | rotas; o prefixo `/api/v1` é aplicado em `config/WebConfig` e **não** vai na anotação |
| `dto` | entrada e saída da API, um subpacote por recurso |
| `service` | regras de negócio e os fluxos A, R, C e P |
| `repository` / `model` | JPA |
| `mapper` | entidade ↔ DTO |
| `security` | JWT, autorização, ownership |
| `exception` | contrato de erro ([`docs/17-contrato-de-erro.md`](docs/17-contrato-de-erro.md)) |
| `config` | segurança, cache, CORS, seeder |

Migrations em `src/main/resources/db/migration/{oracle,mysql}`. Testes em
`src/test/java/br/com/fiap/clyvovet/{crud,fluxo,service,security,mapper,migration,support}`.
Explicação arquivo por arquivo em [`docs/pacotes/`](docs/pacotes/).

## Onde está cada coisa

| Pasta | O que tem |
|---|---|
| [`docs/`](docs/README.md) | documentação técnica numerada, com índice no README |
| [`specs/`](specs/README.md) | enunciados por sprint e backlog; Sprint 4 em [`specs/03-sprint-4.md`](specs/03-sprint-4.md) |
| [`learning/`](learning/README.md) | material de estudo sobre o código real |
| `documentos/` | artefatos de entrega: DDL, diagrama, Postman, `LINKS-ENTREGA.txt` |
| `scripts/` | `gerar-script-bd.py`, que gera `documentos/script_bd.sql` a partir das migrations |
| `azure/` | scripts de provisionamento |

## Sprint 4 (entrega final)

Prazo **04/11/2026**, entrega pelo portal (fora do prazo = −100). Régua completa e
penalidades em [`specs/03-sprint-4.md`](specs/03-sprint-4.md). A interface avaliada
("boa UI e UX") são as **telas Thymeleaf deste repositório**, com login OAuth2 (Google/GitHub),
exigidas pelo professor no feedback da Sprint 3 ([ADR-002](docs/adr/002-telas-thymeleaf-no-spring.md)).
O app mobile Expo continua consumindo a API e entra como integração multidisciplinar.

Penalidades que o código pode causar: violação evidente de boas práticas (−10 cada) e
código repetido que poderia ser extraído (−5 cada).

## Equipe

Fabricio Henrique Pereira (RM563237) · Henrique Sinkevicius Maran (RM562977) ·
Leonardo José Pereira (RM563065) · Miguel Henrique Oliveira Dias (RM565492) · Pedro Henrique de Oliveira (RM562312).

A ausência de colaboração custa −10 e é conferida pelo histórico do Git: os commits
precisam estar distribuídos entre os cinco.
