# Learning — Spring Boot e API REST, do zero, sobre este projeto

Material de estudo escrito para quem **nunca viu Spring** e tem **pouco Java**.

Cada documento parte do **problema** (como seria sem a ferramenta), explica o **conceito**,
mostra o **código real deste repositório** com o caminho do arquivo, e termina com
**perguntas** que sobem de dificuldade.

Não é documentação do sistema (isso é [`../docs/`](../docs/)) nem requisito de entrega (isso é
[`../specs/`](../specs/)). É para **aprender e conseguir explicar**.

---

## Por onde começar

**Se você nunca viu Spring:** comece no [00](00-java-essencial.md) e siga a ordem. Cada
documento assume o anterior.

**Se você já programa em Java:** pule o 00, leia o [01](01-spring-boot-e-injecao-de-dependencia.md)
e o [02](02-arquitetura-em-camadas.md), depois vá para o que precisar.

**Se a prova é amanhã:** [06 (Security)](06-spring-security.md) e
[02 (Camadas)](02-arquitetura-em-camadas.md). Um vale 30 pontos da Sprint 3; o outro é o que o
professor usa para checar se você entende o que escreveu.

---

## A trilha

| # | Documento | O que você sai sabendo |
|---|---|---|
| **00** | [O Java que você precisa](00-java-essencial.md) | anotações, interface, generics, `Optional`, lambda, `record`, Lombok |
| **01** | [O que é Spring, afinal](01-spring-boot-e-injecao-de-dependencia.md) | o que o framework resolve, IoC, beans, injeção, perfis, **proxy** |
| **02** | [Arquitetura em camadas](02-arquitetura-em-camadas.md) | Controller → Service → Repository, DTO, Mapper, por que separar |
| **03** | [API REST (e o HTTP por baixo)](03-api-rest.md) | HTTP, verbos, status, PUT × PATCH, paginação, versionamento |
| **04** | [JPA e Hibernate](04-jpa-e-hibernate.md) | tabelas, FK, `@Entity`, relacionamentos, JPQL, `ddl-auto` |
| **05** | [Bean Validation](05-bean-validation.md) | `@Valid`, restrições, validação × regra de negócio |
| **06** | [**Spring Security**](06-spring-security.md) | autenticação × autorização, filtros, BCrypt, JWT, ownership |
| **07** | [Tratamento de exceções](07-tratamento-de-excecoes.md) | `@RestControllerAdvice`, exceção de domínio, o que não vazar |
| **08** | [Cache](08-cache.md) | `@Cacheable`, chave, TTL, invalidação, vazamento entre usuários |
| **09** | [Flyway e migrations](09-flyway-e-migrations.md) | schema versionado, imutabilidade, baseline, dois bancos |
| **10** | [Testes](10-testes.md) | `MockMvc`, integração × unitário, fronteira, regressão |

---

## Como cada documento é organizado

```
O problema          → como seria sem a ferramenta, e por que dói
O conceito          → a ideia, com analogia quando ajuda
💡 Conceito         → a decisão técnica menos óbvia daquele trecho
No código real      → o arquivo deste repositório, com o caminho
Armadilhas          → os bugs que ESTE projeto já teve
Consolidação        → perguntas em 4 níveis
Se você levar só... → a única frase para guardar
```

### As perguntas do fim

Não são para decorar. Elas sobem de nível:

| Nível | Tipo | Exemplo |
|---|---|---|
| **Entender** | "o que é / por quê" | *"O que uma anotação faz sozinha?"* |
| **Aplicar** | usar num caso novo | *"Escreva a chave de cache para este método"* |
| **Analisar** | comparar, ver nuance | *"Por que construtor e não campo?"* |
| **Avaliar** | julgar uma decisão | *"Um colega quer trocar BCrypt por SHA-256. O que você responde?"* |

Se travar numa pergunta de **Entender**, releia a seção. Se travar só nas de **Avaliar**, está
no caminho — essas são as que aparecem na banca.

---

## Por que estudar sobre o próprio código

A disciplina cobra uma **avaliação oral individual** a partir da Sprint 3, em que cada
integrante precisa:

- explicar trechos específicos do **próprio** código;
- justificar decisões de implementação;
- comentar dificuldades encontradas;
- declarar se e como usou IA no processo.

Conceito genérico não passa nessa conversa — o professor aponta uma linha e pergunta **por que
assim**. Por isso todas as perguntas daqui são sobre **este** repositório.

### Os bugs reais como material de aula

Este projeto mantém um registro honesto dos defeitos que já teve, em
[`../docs/07-pendencias-e-divergencias.md`](../docs/07-pendencias-e-divergencias.md). Vários
viraram seção de armadilha aqui, porque **bug real ensina mais que exemplo de tutorial**:

| Bug | Documento | O que ensina |
|---|---|---|
| `LIKE ... ESCAPE ''` — todo filtro por texto voltava vazio | [03](03-api-rest.md), [10](10-testes.md) | teste que só passa pelo caminho fácil não protege nada |
| `tutorId` do corpo sem checagem de dono | [06](06-spring-security.md) | proteger o id da URL não protege o corpo |
| `sort` fora da chave do cache | [08](08-cache.md) | a chave precisa conter tudo o que muda o resultado |
| `TIMESTAMP` × `DATETIME` no MySQL | [09](09-flyway-e-migrations.md) | nem todo bug grita |
| Enum `REEMBOLSADO` × CHECK `ESTORNADO` | [04](04-jpa-e-hibernate.md) | duas fontes de verdade divergem sozinhas |

---

## Convenções deste projeto, para não estranhar

O código de domínio é todo em **português** — classes, campos, métodos, rotas, mensagens. Só
anotações e tipos do framework ficam em inglês:

```java
public Page<AnimalResponse> listarTodos(String nome, String especie, Pageable pageable)
```

Não é o padrão mais comum no mercado, mas é consistente do começo ao fim. Consistência é o que
se cobra. As convenções completas estão em
[`../docs/06-guia-de-desenvolvimento.md`](../docs/06-guia-de-desenvolvimento.md).

---

## Como ler os trechos de código

Todo bloco traz o arquivo de origem na primeira linha:

```java
// src/main/java/br/com/fiap/clyvovet/service/AnimalService.java
```

**Abra o arquivo ao lado.** Os trechos aqui são recortes — o arquivo real costuma ter
comentários explicando a decisão, e eles valem tanto quanto o código.

---

## Para ir além

| Tema | Onde |
|---|---|
| O que o sistema faz | [`../docs/00-funcionalidades.md`](../docs/00-funcionalidades.md) |
| Arquitetura, com números | [`../docs/01-arquitetura.md`](../docs/01-arquitetura.md) |
| Contratos dos 42 endpoints | [`../docs/03-api-rest.md`](../docs/03-api-rest.md) |
| Segurança implementada | [`../docs/08-seguranca.md`](../docs/08-seguranca.md) |
| Defeitos conhecidos | [`../docs/07-pendencias-e-divergencias.md`](../docs/07-pendencias-e-divergencias.md) |
| O que falta fazer | [`../specs/07-backlog.md`](../specs/07-backlog.md) |
| Documentação oficial | https://docs.spring.io/spring-boot/index.html |
