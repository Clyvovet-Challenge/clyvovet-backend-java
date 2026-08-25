# Learning — Spring Boot e API REST, sobre o código deste projeto

Material de estudo dos conceitos que sustentam esta API. Cada documento explica **o que é**,
**por que existe**, **como se usa** — e mostra **o trecho real deste repositório** onde o
conceito aparece, com o caminho do arquivo.

Não é documentação do sistema (isso é [`../docs/`](../docs/)) nem requisito de entrega
(isso é [`../specs/`](../specs/)). É para **aprender e conseguir explicar**.

---

## Por que estudar sobre o próprio código

A disciplina cobra uma **avaliação oral individual** a partir da Sprint 3, em que cada
integrante precisa:

- explicar trechos específicos do próprio código;
- justificar decisões de implementação;
- comentar dificuldades encontradas;
- declarar se e como usou IA no processo.

Decorar conceito genérico não passa nessa conversa — o professor aponta uma linha e
pergunta *por que assim*. Por isso cada documento aqui termina com **perguntas de
avaliação oral** sobre o código deste repositório.

E há um segundo motivo, mais interessante: este projeto tem um registro honesto dos
**defeitos reais** que já apareceram nele, em
[`../docs/07-pendencias-e-divergencias.md`](../docs/07-pendencias-e-divergencias.md). Bug
real ensina mais que exemplo de tutorial, porque mostra *como a coisa falha na prática*.
Vários deles aparecem aqui como armadilhas.

---

## Trilha sugerida

Na ordem, cada um apoiando o seguinte:

| # | Documento | Conceito central |
|---|---|---|
| 01 | [Spring Boot e injeção de dependência](01-spring-boot-e-injecao-de-dependencia.md) | IoC, container, beans, `@Service`, injeção por construtor, perfis |
| 02 | [Arquitetura em camadas](02-arquitetura-em-camadas.md) | Controller → Service → Repository, DTO, Mapper, por que separar |
| 03 | [API REST](03-api-rest.md) | Recursos, verbos, status, PUT × PATCH, paginação, versionamento, maturidade |
| 04 | [JPA e Hibernate](04-jpa-e-hibernate.md) | `@Entity`, chaves, relacionamentos, fetch, JPQL, `ddl-auto` |
| 05 | [Bean Validation](05-bean-validation.md) | `@Valid`, restrições, validação × regra de negócio |
| 06 | [**Spring Security**](06-spring-security.md) | Autenticação × autorização, filter chain, JWT, BCrypt, `@PreAuthorize`, ownership |
| 07 | [Tratamento de exceções](07-tratamento-de-excecoes.md) | `@RestControllerAdvice`, exceção de domínio, mapa exceção → status |
| 08 | [Cache](08-cache.md) | `@Cacheable`, `@CacheEvict`, chave, TTL, invalidação |
| 09 | [Flyway e migrations](09-flyway-e-migrations.md) | Versionamento de schema, baseline, `validate` |
| 10 | [Testes](10-testes.md) | `@SpringBootTest`, MockMvc, AssertJ, o que testar |

Se o tempo for curto, **06 (Security) e 02 (camadas)** são os que mais aparecem na
avaliação oral: um porque vale 30 pontos da Sprint 3, o outro porque é o que o professor
usa para checar se você entende o que escreveu.

---

## Como ler os trechos de código

Todo bloco de código traz o arquivo de origem em comentário na primeira linha:

```java
// src/main/java/br/com/fiap/clyvovet/service/AnimalService.java
```

Abra o arquivo ao lado. Os trechos aqui são **recortes** — o arquivo real costuma ter
comentários explicando a decisão, que valem tanto quanto o código.

---

## Convenção deste projeto, para não estranhar

O código de domínio é todo em **português**: classes, campos, métodos, rotas e mensagens.
Só as anotações e os tipos do framework ficam em inglês.

```java
public Page<AnimalResponse> listarTodos(String nome, String especie, Pageable pageable)
```

Não é o padrão mais comum no mercado, mas é consistente do começo ao fim — e consistência
é o que se cobra. As convenções completas estão em
[`../docs/06-guia-de-desenvolvimento.md`](../docs/06-guia-de-desenvolvimento.md).

---

## Para ir além

| Tema | Onde |
|---|---|
| O que o sistema faz | [`../docs/00-funcionalidades.md`](../docs/00-funcionalidades.md) |
| Arquitetura de verdade, com números | [`../docs/01-arquitetura.md`](../docs/01-arquitetura.md) |
| Contratos de todos os 42 endpoints | [`../docs/03-api-rest.md`](../docs/03-api-rest.md) |
| Segurança implementada | [`../docs/08-seguranca.md`](../docs/08-seguranca.md) |
| Defeitos reais já encontrados | [`../docs/07-pendencias-e-divergencias.md`](../docs/07-pendencias-e-divergencias.md) |
| Documentação oficial do Spring | https://docs.spring.io/spring-boot/index.html |
