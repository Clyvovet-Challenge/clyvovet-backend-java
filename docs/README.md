# Documentação — CLYVO VET Backend

Documentação técnica da API REST do CLYVO VET, plataforma de saúde contínua para pets
desenvolvida como Challenge FIAP 2026 — 1º Semestre.

Esta pasta descreve **o que o código faz hoje**. Onde a implementação diverge da
documentação de negócio ou do schema do banco, isso está registrado em
[07-pendencias-e-divergencias.md](07-pendencias-e-divergencias.md) em vez de ser
silenciosamente corrigido no texto.

---

## Índice

| Documento | Conteúdo |
|---|---|
| [00-funcionalidades.md](00-funcionalidades.md) | **Comece aqui.** O que o sistema faz: domínio, funcionalidades, fluxo de ponta a ponta e o que ainda não existe |
| [01-arquitetura.md](01-arquitetura.md) | Camadas, fluxo de uma requisição, responsabilidades, cache, tratamento de erros |
| [02-modelo-de-dados.md](02-modelo-de-dados.md) | Entidades JPA, relacionamentos, enums, mapeamento objeto↔tabela, DDL Oracle |
| [03-api-rest.md](03-api-rest.md) | Os 36 endpoints sob `/api/v1`, filtros, paginação, PATCH, contratos, códigos de erro |
| [04-configuracao.md](04-configuracao.md) | Perfis Spring (`oracle`, `h2`, `dev`), propriedades, como rodar localmente |
| [05-deploy.md](05-deploy.md) | Dockerfile, docker-compose, provisionamento Azure via `deploy.sh`, o que chega na VM |
| [06-guia-de-desenvolvimento.md](06-guia-de-desenvolvimento.md) | Convenções do código, como adicionar uma entidade nova, build, testes e o grafo do codebase |
| [07-pendencias-e-divergencias.md](07-pendencias-e-divergencias.md) | Inconsistências conhecidas entre código, banco e documentação |
| [08-seguranca.md](08-seguranca.md) | Autenticação JWT, perfis, ownership, matriz de autorização e hardening |

---

## Visão geral em 30 segundos

O CLYVO VET conecta **tutores de pets**, **veterinários** e **clínicas parceiras**,
centralizando o histórico clínico do animal e o controle financeiro dos atendimentos.

O backend é uma API REST em Spring Boot que expõe CRUD completo sobre seis entidades:

```
Tutor ──1:N──> Animal ──┐
                        ├──> EventoClinico ──1:N──> Pagamento
Clinica ─1:N─> Veterinario ──┘
```

- **Tutor** e **Clinica** são raízes: não dependem de ninguém.
- **Animal** pertence a um Tutor; **Veterinario** pertence a uma Clinica.
- **EventoClinico** é o núcleo do domínio — amarra animal + veterinário + clínica numa data.
- **Pagamento** registra a cobrança de um evento clínico.

### Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.5.14 |
| Persistência | Spring Data JPA / Hibernate 6.6 |
| Banco (produção) | Oracle 19c (FIAP) |
| Banco (dev/container) | H2 |
| Segurança | Spring Security + JWT (jjwt) · BCrypt · Bucket4j (rate limit) |
| Migrations | Flyway |
| Validação | Bean Validation (Jakarta) |
| Cache | Spring Cache — Caffeine, TTL de 10 min |
| Documentação | springdoc-openapi 2.8.16 (Swagger UI) |
| Boilerplate | Lombok |
| Build | Maven (wrapper incluso) |

### Números do projeto

| Item | Quantidade |
|---|---|
| Entidades JPA | 7 + 1 `@Embeddable` |
| Enums | 6 |
| Controllers | 7 |
| Endpoints REST | 35 |
| Services | 7 |
| Repositories | 7 |
| Mappers | 7 |
| DTOs | 19 |
| Migrations Flyway | 4 |
| Testes automatizados | 27 |

---

## Links rápidos

| Recurso | URL (execução local) |
|---|---|
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Console H2 | http://localhost:8080/h2-console (perfis `dev` e `h2`) |

## Material complementar

Fora desta pasta, o repositório traz:

| Arquivo | Conteúdo |
|---|---|
| [../README.md](../README.md) | Apresentação do projeto, integrantes do grupo, quick start |
| `../documentos/Diagrama_De_Classes.pdf` | Diagrama de classes UML das entidades |
| `../documentos/Cronograma_CLYVOVET.pdf` | Cronograma de desenvolvimento |
| `../documentos/Post_*.png` | Capturas de tela dos POSTs testados |
| [../src/main/resources/db/db-oracle.sql](../src/main/resources/db/db-oracle.sql) | DDL + seed para o Oracle da FIAP |
