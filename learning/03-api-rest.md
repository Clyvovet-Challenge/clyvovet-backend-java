# 03 — API REST

## O que é

**REST** não é um protocolo, é um estilo de arquitetura. A ideia central: a API expõe
**recursos** (substantivos) identificados por URL, e a ação vem do **verbo HTTP**, não do
nome da rota.

```
❌ POST /criarAnimal          ❌ GET /buscarAnimalPorId?id=3
✅ POST /animais              ✅ GET /animais/{id}
```

O verbo já diz o que fazer. A URL só diz *sobre o quê*.

## Verbos e o que cada um promete

| Verbo | Ação | Idempotente? | Status de sucesso aqui |
|---|---|---|---|
| `GET` | ler | sim | 200 |
| `POST` | criar | **não** | **201** |
| `PUT` | substituir por inteiro | sim | 200 |
| `PATCH` | alterar em parte | não necessariamente | 200 |
| `DELETE` | remover | sim | **204** |

**Idempotente** = repetir a mesma chamada dá o mesmo resultado final. `PUT` duas vezes com o
mesmo corpo deixa o recurso igual; `POST` duas vezes cria **dois** registros.

```java
// src/main/java/br/com/fiap/clyvovet/controller/AnimalController.java
@PostMapping
public ResponseEntity<AnimalResponse> criar(@Valid @RequestBody AnimalRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(animalService.criar(request));
}

@DeleteMapping("/{id}")
public ResponseEntity<Void> deletar(@PathVariable UUID id) {
    animalService.deletar(id);
    return ResponseEntity.noContent().build();   // 204
}
```

`204 No Content` no DELETE porque não há corpo para devolver — devolver `200` com um objeto
vazio seria mentir sobre o que aconteceu.

## PUT × PATCH — a distinção que este projeto leva a sério

| | `PUT` | `PATCH` |
|---|---|---|
| Semântica | "este é o recurso inteiro agora" | "mude só o que eu mandei" |
| Campo omitido | é **apagado** | é **preservado** |
| DTO | `AnimalRequest` — com `@NotNull`/`@NotBlank` | `AnimalPatchRequest` — sem obrigatoriedade |

Os dois DTOs existem separados de propósito:

```java
// src/main/java/br/com/fiap/clyvovet/dto/eventoClinico/EventoClinicoRequest.java
@NotBlank
@Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d", message = "Hora deve estar no formato HH:mm")
private String hora;
```

```java
// src/main/java/br/com/fiap/clyvovet/dto/eventoClinico/EventoClinicoPatchRequest.java
/** A coluna e VARCHAR2(5): so cabe HH:mm. */
@Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d", message = "Hora deve estar no formato HH:mm")
private String hora;
```

O `@Pattern` (formato) fica nos dois; o `@NotBlank` (presença) só no `Request`. O comentário
do arquivo registra o raciocínio: **não** se reaproveita o `Request` com grupos de validação
porque, no PATCH, um campo ausente significa "não mexa" — e um campo **não pode ser apagado**
por PATCH nesta API.

Consequência prática dessa escolha: para limpar um campo, use `PUT`.

## Status codes que esta API usa

| Código | Quando | Onde é decidido |
|---|---|---|
| 200 | leitura, PUT, PATCH | controller |
| 201 | POST | controller |
| 204 | DELETE | controller |
| **400** | falha de Bean Validation | `GlobalExceptionHandler` |
| **401** | sem token, token inválido, credencial errada | Spring Security / `BadCredentialsException` |
| **403** | autenticado, mas sem permissão | Spring Security / `@PreAuthorize` |
| **404** | id inexistente | `RecursoNaoEncontradoException` |
| **409** | duplicidade, FK em uso, regra de negócio | `RegraDeNegocioException`, `DataIntegrityViolationException` |
| **429** | rate limit estourado | `RateLimitFilter` |

A diferença entre **401** e **403** costuma cair na oral: 401 é *"não sei quem você é"*;
403 é *"sei quem você é, e você não pode"*.

Formato único de erro:

```java
// src/main/java/br/com/fiap/clyvovet/dto/exception/ErroValidacao.java
public record ErroValidacao(String campo, String mensagem) {}
```

## Paginação, ordenação e filtros

Listagem nunca devolve tudo. O Spring Data resolve com `Pageable`:

```java
@GetMapping
public ResponseEntity<Page<AnimalResponse>> listarTodos(
        @RequestParam(required = false) String nome,
        @RequestParam(required = false) String especie,
        @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
```

| Parâmetro | Default | Exemplo |
|---|---|---|
| `page` | 0 | `?page=2` |
| `size` | 10 | `?size=25` |
| `sort` | varia por recurso | `?sort=nome,desc` |

Filtros opcionais usam o padrão `:param IS NULL OR ...` no JPQL — o filtro **desaparece**
quando não é informado, sem precisar de Criteria API.

### O envelope da resposta paginada

```json
{
  "content": [ ... ],
  "page": { "size": 10, "number": 0, "totalElements": 27, "totalPages": 3 }
}
```

Esse formato foi uma decisão explícita:

```java
// src/main/java/br/com/fiap/clyvovet/config/WebConfig.java
@EnableSpringDataWebSupport(
        pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
```

Sem `VIA_DTO`, o Spring serializa o `PageImpl` inteiro — mais de vinte campos internos do
framework — e avisa no boot que *"there is no guarantee about the stability of the resulting
JSON structure"*. Ou seja: um upgrade do Spring poderia mudar a resposta sem uma linha de
código mudar.

## Versionamento

Todas as rotas ficam sob `/api/v1`, mas **nenhum controller escreve isso**:

```java
// src/main/java/br/com/fiap/clyvovet/config/WebConfig.java
public static final String PREFIXO_API = "/api/v1";

@Override
public void configurePathMatch(PathMatchConfigurer configurer) {
    configurer.addPathPrefix(PREFIXO_API,
            HandlerTypePredicate.forBasePackage("br.com.fiap.clyvovet.controller"));
}
```

O prefixo vive num lugar só; ir para `/api/v2` um dia é mudar esta constante.

Dois detalhes que o comentário do arquivo registra e que são ótimos exemplos de decisão
técnica com justificativa:

1. **Por que não `server.servlet.context-path`?** Porque ele moveria o Swagger e o console
   do H2 junto.
2. **Por que o predicado é por pacote, e não por `@RestController`?** Porque o springdoc
   também anota as classes dele com `@RestController` — filtrar pela anotação levava
   `/v3/api-docs` para `/api/v1/v3/api-docs` e o Swagger parava de abrir.

## Maturidade de Richardson — onde esta API está

| Nível | O que tem | Aqui? |
|---|---|---|
| 0 | um endpoint, tudo por POST | — |
| 1 | recursos separados por URL | ✅ |
| 2 | verbos e status codes corretos | ✅ **é onde estamos** |
| 3 | HATEOAS — links de navegação na resposta | ❌ |

**Nível 3** significaria a resposta carregar os próximos passos possíveis:

```json
{
  "id": "...", "nome": "Bolinha",
  "_links": {
    "self":    { "href": "/api/v1/animais/44444444-..." },
    "tutor":   { "href": "/api/v1/tutores/22222222-..." },
    "eventos": { "href": "/api/v1/eventos-clinicos?animalNome=Bolinha" }
  }
}
```

Isso está registrado como pendência: a rubrica da Sprint 1/2 dá até 15 pontos para
"maturidade REST", e o item A2 de [`../specs/07-backlog.md`](../specs/07-backlog.md)
confirma que não há nenhuma referência a `EntityModel`/`WebMvcLinkBuilder` no código.

## Documentação: OpenAPI/Swagger

```java
@Tag(name = "Animais", description = "Gerenciamento de animais")
...
@Operation(summary = "Listar animais com paginação e filtros por nome e espécie")
```

O springdoc lê as anotações e o próprio código (tipos, `@Valid`, `@RequestParam`) e gera a
especificação. Disponível em `/swagger-ui.html` e `/v3/api-docs`.

## Armadilhas reais deste projeto

### 1. Filtro por texto que nunca casava — o bug mais instrutivo daqui

Toda listagem filtrada por texto devolvia lista vazia. `?nome=Camila` retornava
`totalElements: 0` com a Camila cadastrada.

Causa: o Hibernate emite `LIKE ... ESCAPE ''`. Na semântica do Oracle — que o H2 imita com
`MODE=Oracle` — **string vazia é NULL**. O predicado virava `ESCAPE NULL`, avaliava como
desconhecido e nunca era verdadeiro.

```java
// A correção, em cinco repositórios
"(:nome IS NULL OR LOWER(a.nome) LIKE LOWER(CONCAT('%', :nome, '%')) ESCAPE '\\')"
```

A lição real não é o `ESCAPE`. É **por que ninguém viu**: os testes de listagem só
exercitavam o recorte por tutor, com o parâmetro de texto indo `null` — caindo sempre no ramo
`:nome IS NULL`. Um teste que só passa pelo caminho fácil não protege nada. Hoje há
`FiltrosDeBuscaTest`, que confere cada filtro **nos dois sentidos**: o que ele traz e o que
ele deixa de fora.

História completa: item 16 de
[`../docs/07-pendencias-e-divergencias.md`](../docs/07-pendencias-e-divergencias.md).

### 2. Mudar o contrato quebra quem consome

A introdução do `/api/v1` e a troca do envelope de paginação quebraram os clientes: o total
saiu da raiz (`totalElements`) para `page.totalElements`. Está registrado como conflito 8 em
[`../specs/04-dependencias-externas.md`](../specs/04-dependencias-externas.md) — e o aviso
ao time de Mobile **ainda é um item aberto** no backlog. Versionar a URL é metade do
trabalho; comunicar é a outra.

### 3. Validação mais permissiva que a coluna vira 500

`hora` era `@NotNull` numa coluna `VARCHAR2(5)`. `""` e `"14:30:00"` passavam na validação e
estouravam no INSERT — erro de servidor por um dado que o cliente poderia corrigir. Hoje é
`@NotBlank` + `@Pattern`, que responde **400 com o campo indicado**. Item 18 do mesmo
documento.

## Perguntas de avaliação oral

1. Por que o DELETE devolve 204 e o POST devolve 201?
2. Qual a diferença entre 401 e 403? Dê um exemplo de cada nesta API.
3. Por que existem `AnimalRequest` e `AnimalPatchRequest` separados? Por que não usar grupos
   de validação?
4. Como um cliente apaga o campo `descricao` de um evento clínico nesta API?
5. Onde está escrito `/api/v1`? Por que não em cada `@RequestMapping`?
6. Em que nível de maturidade de Richardson esta API está, e o que faltaria para o próximo?
7. Por que `?nome=Camila` retornava vazio? Por que os testes não pegaram isso?

---

**Anterior:** [02 — Arquitetura em camadas](02-arquitetura-em-camadas.md) ·
**Próximo:** [04 — JPA e Hibernate](04-jpa-e-hibernate.md)
