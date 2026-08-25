# 03 — API REST (e o HTTP por baixo)

> **Pré-requisito:** [02 — Arquitetura em camadas](02-arquitetura-em-camadas.md).
> Esta é a parte que o usuário final "vê" da sua aplicação.

---

## Antes de REST: o que é HTTP

Se você nunca parou para olhar, um pedido HTTP é **texto**. Literalmente isto viaja pela
rede:

```http
GET /api/v1/animais/44444444-4444-4444-4444-000000000001 HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

E a resposta:

```http
HTTP/1.1 200 OK
Content-Type: application/json

{"id":"44444444-...","nome":"Bolinha","raca":"Golden Retriever"}
```

As peças:

| Peça | O que é | Exemplo |
|---|---|---|
| **Verbo** | a ação pretendida | `GET`, `POST`, `PUT`, `PATCH`, `DELETE` |
| **Caminho** | o que você quer | `/api/v1/animais/44444444-...` |
| **Query string** | filtros opcionais | `?nome=Rex&page=2` |
| **Headers** | metadados | `Authorization`, `Content-Type` |
| **Corpo** | os dados (só em POST/PUT/PATCH) | o JSON |
| **Status** | como terminou | `200`, `404`, `403` |

**API** = um programa conversando com outro por essa via, em vez de um humano com um site.
**JSON** é o formato de texto do corpo. **`Content-Type: application/json`** é o que avisa o
outro lado sobre isso.

Uma característica de HTTP que explica quase tudo o que vem depois: ele é **stateless**. O
servidor não lembra do pedido anterior. Cada requisição chega sozinha e precisa carregar tudo
o que é necessário — inclusive quem você é. Guarde isso; é o motivo de o token viajar em
**toda** chamada (documento [06](06-spring-security.md)).

---

## O que é REST

REST **não é um protocolo nem uma biblioteca**. É um estilo de organizar a API, com uma ideia
central:

> A URL identifica **um recurso** (um substantivo). O **verbo HTTP** diz o que fazer com ele.

```
❌ POST /criarAnimal            ❌ GET /buscarAnimalPorId?id=3
❌ POST /deletarAnimal          ❌ GET /listarTodosAnimais

✅ POST   /animais              ✅ GET    /animais/3
✅ DELETE /animais/3            ✅ GET    /animais
```

Repare o que aconteceu: **quatro URLs viraram duas**. `/animais` e `/animais/3` — e o verbo
diferencia a operação.

💡 **Conceito: por que substantivo e não verbo na URL**

Com verbo na URL, cada operação nova inventa um nome (`/criarAnimal`, `/cadastrarAnimal`,
`/novoAnimal`) e quem consome precisa **decorar** cada um.

Com recurso + verbo HTTP, quem já sabe HTTP **adivinha** a API: se `/animais` existe,
`GET /animais/{id}` provavelmente busca um; `DELETE /animais/{id}` provavelmente remove.

O ganho não é estético — é **previsibilidade**. Um desenvolvedor de mobile consegue usar sua
API sem ler documentação para cada rota.

---

## Os verbos e o que cada um promete

| Verbo | Ação | Idempotente? | Status aqui |
|---|---|---|---|
| `GET` | ler | sim | 200 |
| `POST` | criar | **não** | **201** |
| `PUT` | substituir por inteiro | sim | 200 |
| `PATCH` | alterar em parte | não necessariamente | 200 |
| `DELETE` | remover | sim | **204** |

**Idempotente** = repetir a mesma chamada dá o mesmo resultado final.

- `PUT /animais/3` duas vezes com o mesmo corpo → o animal fica igual. Idempotente.
- `POST /animais` duas vezes → **dois animais criados**. Não idempotente.

Por que isso é prático: se a rede cai e o cliente não sabe se a chamada chegou, ele pode
repetir com segurança um `PUT` ou `DELETE`. Um `POST`, não — pode duplicar.

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

`204 No Content` no DELETE porque **não há o que devolver**. Devolver `200` com um objeto
vazio seria descrever mal o que aconteceu.

---

## PUT × PATCH — a distinção que este projeto leva a sério

| | `PUT` | `PATCH` |
|---|---|---|
| Significa | "este é o recurso **inteiro** agora" | "mude **só** o que eu mandei" |
| Campo omitido | é **apagado** | é **preservado** |
| DTO | `AnimalRequest` — com obrigatoriedade | `AnimalPatchRequest` — sem |

Na prática:

```jsonc
// PUT /api/v1/animais/3  — se omitir "cor", a cor é APAGADA
{ "nome": "Bolinha", "raca": "Golden", "tutorId": "..." }

// PATCH /api/v1/animais/3 — só o nome muda; o resto fica como está
{ "nome": "Bolinha Silva" }
```

Por isso existem dois DTOs:

```java
// EventoClinicoRequest.java — usado no POST e no PUT
@NotBlank
@Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d", message = "Hora deve estar no formato HH:mm")
private String hora;
```

```java
// EventoClinicoPatchRequest.java — usado no PATCH
@Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d", message = "Hora deve estar no formato HH:mm")
private String hora;
```

A regra: **formato fica nos dois, presença só no `Request`**. Se `hora` vier no PATCH,
precisa ser `HH:mm`; se não vier, não se mexe no campo.

Consequência prática dessa escolha, que vale saber responder: **nesta API não se apaga um
campo via PATCH** — porque `null` significa "não mencionei". Para limpar um campo, use `PUT`.

---

## Status codes

Os grupos:

| Faixa | Significa | Quem errou |
|---|---|---|
| **2xx** | deu certo | — |
| **3xx** | redirecionamento | — |
| **4xx** | **o cliente** errou | quem chamou |
| **5xx** | **o servidor** errou | você |

Essa divisão é a que mais importa: **todo 5xx é um bug seu**. Se um dado inválido do cliente
produz 500, o problema não é o dado — é a sua API, que deveria ter respondido 400 dizendo
qual campo está errado.

Os usados aqui:

| Código | Quando | Decidido em |
|---|---|---|
| 200 OK | leitura, PUT, PATCH | controller |
| 201 Created | POST | controller |
| 204 No Content | DELETE | controller |
| **400** Bad Request | falha de validação | `GlobalExceptionHandler` |
| **401** Unauthorized | sem token, token inválido, senha errada | Spring Security |
| **403** Forbidden | autenticado, mas sem permissão | `@PreAuthorize` |
| **404** Not Found | id inexistente | `RecursoNaoEncontradoException` |
| **409** Conflict | duplicidade, FK em uso, regra violada | `RegraDeNegocioException` |
| **429** Too Many Requests | rate limit | `RateLimitFilter` |

**401 × 403 cai muito na avaliação oral:**

- **401** = *"não sei quem você é"* → faça login.
- **403** = *"sei quem você é, e você não pode"* → login não resolve.

Formato único de erro:

```java
// src/main/java/br/com/fiap/clyvovet/dto/exception/ErroValidacao.java
public record ErroValidacao(String campo, String mensagem) {}
```

---

## Paginação: por que nunca devolver tudo

Se `GET /animais` devolvesse os 50.000 animais da base, três coisas quebrariam: a memória do
servidor, a rede e a tela do cliente.

```java
@GetMapping
public ResponseEntity<Page<AnimalResponse>> listarTodos(
        @RequestParam(required = false) String nome,
        @RequestParam(required = false) String especie,
        @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
```

`Pageable` é montado pelo Spring a partir da query string:

| Parâmetro | Default | Exemplo |
|---|---|---|
| `page` | 0 (a primeira é zero) | `?page=2` |
| `size` | 10 | `?size=25` |
| `sort` | varia por recurso | `?sort=nome,desc` |

```
GET /api/v1/animais?nome=re&page=0&size=5&sort=nome,asc
```

O filtro vira SQL assim:

```java
"(:nome IS NULL OR LOWER(a.nome) LIKE LOWER(CONCAT('%', :nome, '%')) ESCAPE '\\')"
```

Leia como: *"se `nome` não foi informado, ignore este pedaço; senão, filtre por nome
contendo o texto, sem diferenciar maiúsculas"*. É o truque que faz **uma** query servir a
todas as combinações de filtro.

### O envelope da resposta

```json
{
  "content": [ /* os 10 animais desta página */ ],
  "page": { "size": 10, "number": 0, "totalElements": 27, "totalPages": 3 }
}
```

Esse formato foi escolhido de propósito:

```java
// src/main/java/br/com/fiap/clyvovet/config/WebConfig.java
@EnableSpringDataWebSupport(
        pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
```

💡 **Conceito: contrato estável**

Sem `VIA_DTO`, o Spring serializava o objeto `PageImpl` **inteiro** — mais de vinte campos
internos do framework — e avisava no boot: *"there is no guarantee about the stability of the
resulting JSON structure"*.

Traduzindo o risco: um upgrade do Spring poderia mudar a resposta da sua API **sem uma linha
do seu código mudar**, e o app mobile quebraria sem explicação.

`VIA_DTO` fixa o contrato em `content` + `page`. A regra geral: **nunca deixe uma estrutura
interna de framework vazar para o contrato público**.

---

## Versionamento

Todas as rotas ficam sob `/api/v1` — mas **nenhum controller escreve isso**:

```java
// src/main/java/br/com/fiap/clyvovet/config/WebConfig.java
public static final String PREFIXO_API = "/api/v1";

@Override
public void configurePathMatch(PathMatchConfigurer configurer) {
    configurer.addPathPrefix(PREFIXO_API,
            HandlerTypePredicate.forBasePackage("br.com.fiap.clyvovet.controller"));
}
```

Por que versionar: quando a API mudar de forma incompatível, `/api/v2` nasce **ao lado** de
`/api/v1`. Os clientes antigos continuam funcionando enquanto migram. Sem versão, toda
mudança é uma quebra imediata.

Duas decisões registradas no arquivo, que são bons exemplos de "por que não o caminho óbvio":

1. **Por que não `server.servlet.context-path`?** Porque ele moveria o Swagger e o console do
   H2 junto.
2. **Por que o filtro é por pacote e não por `@RestController`?** Porque o springdoc também
   anota as classes dele com `@RestController` — filtrar pela anotação levava `/v3/api-docs`
   para `/api/v1/v3/api-docs` e **o Swagger parava de abrir**.

---

## Maturidade de Richardson — onde esta API está

Uma régua para medir o quão REST uma API é:

| Nível | O que tem | Aqui? |
|---|---|---|
| 0 | um endpoint só, tudo por POST | — |
| 1 | recursos separados por URL | ✅ |
| 2 | verbos e status codes corretos | ✅ **é onde estamos** |
| 3 | HATEOAS — links de navegação na resposta | ❌ |

Nível 3 seria a resposta carregar os próximos passos possíveis:

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

A ideia: o cliente **navega** pela API como se navega num site, seguindo links, sem montar
URLs na mão.

Está registrado como pendência — vale até parte de 15 pontos na rubrica da Sprint 1/2, e o
item A2 de [`../specs/07-backlog.md`](../specs/07-backlog.md) confirma que não há nenhuma
referência a `EntityModel`/`WebMvcLinkBuilder` no código.

---

## Documentação automática: Swagger

```java
@Tag(name = "Animais", description = "Gerenciamento de animais")
...
@Operation(summary = "Listar animais com paginação e filtros por nome e espécie")
```

O springdoc lê as anotações **e o próprio código** (tipos, `@Valid`, `@RequestParam`) e gera
uma página onde dá para testar cada endpoint pelo navegador:

- `/swagger-ui.html` — a interface
- `/v3/api-docs` — a especificação em JSON

Para chamar rota protegida no Swagger: botão **Authorize**, cole **só o token**, sem o
prefixo `Bearer`.

---

## Armadilhas reais deste projeto

### 1. O filtro que nunca casava — o bug mais instrutivo daqui

**Sintoma:** toda listagem filtrada por texto devolvia lista vazia.
`GET /veterinarios?nome=Camila` retornava `totalElements: 0` com a Camila cadastrada. Só a
listagem sem filtro funcionava.

**Causa:** o Hibernate gera `LIKE ... ESCAPE ''`. Na semântica do Oracle — que o H2 imita com
`MODE=Oracle` — **string vazia é NULL**. O predicado virava `ESCAPE NULL`, avaliava como
"desconhecido" e **nunca era verdadeiro**.

**Correção**, nos cinco repositórios que usam `LIKE`:

```java
"(:nome IS NULL OR LOWER(a.nome) LIKE LOWER(CONCAT('%', :nome, '%')) ESCAPE '\\')"
```

Mas a lição de verdade não é o `ESCAPE`. É **por que ninguém viu por meses**:

> Os testes de listagem que existiam passavam porque só exercitavam o recorte por tutor, com
> o parâmetro de texto indo `null` — caindo sempre no ramo `:nome IS NULL`.

**Um teste que percorre só o caminho fácil não protege nada.** Hoje existe
`FiltrosDeBuscaTest`, que confere cada filtro **nos dois sentidos**: o que ele traz e o que
ele deixa de fora. História completa no item 16 de
[`../docs/07-pendencias-e-divergencias.md`](../docs/07-pendencias-e-divergencias.md).

### 2. Validação mais frouxa que a coluna vira 500

`hora` era só `@NotNull` numa coluna `VARCHAR2(5)`. `""` e `"14:30:00"` passavam na validação
e estouravam no INSERT — **500** por um dado que o cliente poderia corrigir sozinho. Hoje é
`@NotBlank` + `@Pattern`, e responde **400 com o campo indicado**.

Lembre da regra dos status: 5xx é bug seu. Item 18 do mesmo documento.

### 3. Mudar o contrato quebra quem consome

O `/api/v1` e o novo envelope de paginação quebraram os clientes — o total saiu de
`totalElements` na raiz para `page.totalElements`. Está registrado como conflito 8 em
[`../specs/04-dependencias-externas.md`](../specs/04-dependencias-externas.md), e **o aviso
ao time de mobile ainda é um item aberto**.

Versionar a URL é metade do trabalho. Comunicar é a outra metade.

---

## Consolidação

**Entender**
1. Por que a URL é um substantivo (`/animais`) e não um verbo (`/criarAnimal`)?
2. O que significa dizer que HTTP é *stateless*? Que consequência isso tem para autenticação?

**Aplicar**
3. Escreva as chamadas HTTP (verbo + caminho) para: listar a 2ª página de tutores ordenados
   por nome decrescente; buscar um pagamento por id; apagar um evento clínico.
4. Um cliente quer apagar o campo `descricao` de um evento. Qual verbo ele usa nesta API, e
   por quê?

**Analisar**
5. Por que DELETE devolve 204 e POST devolve 201? O que cada escolha comunica?
6. Qual a diferença entre 401 e 403? Dê um exemplo de cada nesta API.
7. Por que `VIA_DTO` na serialização da página? Que risco concreto ele elimina?

**Avaliar**
8. Um colega sugere criar `POST /animais/buscar` com os filtros no corpo, "porque a query
   string fica grande". Quais são os prós e contras? Você aceitaria?
9. A API está no nível 2 de Richardson. Vale investir em HATEOAS neste projeto? Justifique
   considerando prazo e pontuação.

---

## Se você levar só uma coisa daqui

**Todo 5xx é um bug seu.** Dado inválido do cliente deve virar 400 com o campo indicado —
se virou 500, foi a sua API que deixou passar.

---

**Anterior:** [02 — Arquitetura em camadas](02-arquitetura-em-camadas.md) ·
**Próximo:** [04 — JPA e Hibernate](04-jpa-e-hibernate.md)
