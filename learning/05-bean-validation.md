# 05 — Bean Validation

## O que é

**Bean Validation** (Jakarta Validation) é a especificação que permite declarar restrições
com anotações no próprio DTO, em vez de espalhar `if` pelo controller.

```java
// ❌ sem Bean Validation
if (request.getNome() == null || request.getNome().isBlank()) {
    return ResponseEntity.badRequest().body("Nome é obrigatório");
}
if (request.getEmail() == null || !request.getEmail().contains("@")) { ... }
```

```java
// ✅ com Bean Validation
@NotBlank(message = "Nome é obrigatório")
private String nome;

@Email
private String email;
```

A validação roda **antes** do corpo do método do controller. Se falhar, o método nem começa.

## Como ligar

Duas peças:

```java
// 1. no controller — @Valid dispara a validação
@PostMapping
public ResponseEntity<AnimalResponse> criar(@Valid @RequestBody AnimalRequest request) {
```

```java
// 2. no DTO — as restrições
@NotNull
private UUID veterinarioId;
```

Sem `@Valid`, as anotações do DTO são simplesmente ignoradas — é um erro silencioso e comum.

## As restrições usadas aqui

| Anotação | Verifica | Cuidado |
|---|---|---|
| `@NotNull` | não é nulo | aceita `""` e `"   "` |
| `@NotBlank` | texto não nulo, não vazio, não só espaços | **só para String** |
| `@NotEmpty` | coleção/texto não vazio | aceita `"   "` |
| `@Size(min, max)` | tamanho | alinhe com a coluna do banco |
| `@Email` | formato de e-mail | permissivo por padrão |
| `@Positive` | número > 0 | |
| `@PastOrPresent` | data não futura | **ignora nulo** |
| `@Digits(integer, fraction)` | dígitos de um decimal | espelha `NUMBER(10,2)` |
| `@Pattern(regexp)` | expressão regular | último recurso, mas resolve formato |

Exemplo real, com as três restrições cobrindo coisas diferentes:

```java
// src/main/java/br/com/fiap/clyvovet/dto/pagamento/PagamentoRequest.java
@NotNull(message = "Valor é obrigatório")
@Positive(message = "Valor deve ser positivo")
@Digits(integer = 9, fraction = 2, message = "Valor inválido: máximo 9 dígitos inteiros e 2 decimais")
private BigDecimal valor;
```

## A resposta de erro

```java
// src/main/java/br/com/fiap/clyvovet/exception/GlobalExceptionHandler.java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<List<ErroValidacao>> handleValidationErrors(MethodArgumentNotValidException ex) {
    List<ErroValidacao> erros = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(erro -> new ErroValidacao(erro.getField(), erro.getDefaultMessage()))
            .toList();
    return ResponseEntity.badRequest().body(erros);
}
```

O cliente recebe **400** com a lista de todos os campos que falharam — não só o primeiro:

```json
[
  { "campo": "nome",  "mensagem": "Nome é obrigatório" },
  { "campo": "valor", "mensagem": "Valor deve ser positivo" }
]
```

## A regra que este projeto seguiu: validação espelha a coluna

O princípio: **o que a coluna não comporta, a validação deve recusar** — para o erro sair
como 400 com o campo indicado, e não como 500 vindo do banco.

```java
// src/main/java/br/com/fiap/clyvovet/dto/eventoClinico/EventoClinicoRequest.java
@Size(max = 1000)   // coluna VARCHAR2(1000)
private String descricao;
```

O comentário `// coluna VARCHAR2(1000)` é a prática que sustenta isso: quem mexer no DTO vê
de onde veio o número.

Esse alinhamento foi conquistado corrigindo defeitos reais, nas duas direções:

| Direção | Campo | O que acontecia |
|---|---|---|
| Validação **mais estreita** que a coluna | `crmv` era 4–6, a coluna é 30 | o CRMV real (`CRMV-SP 14320`, 13 chars) era **rejeitado** — o seed não podia ser recriado pela API |
| Validação **mais larga** que a coluna | `observacao` sem `@Size`, coluna `VARCHAR2(1000)` | texto grande passava e estourava no INSERT → **500** |

Itens 6 e 18 de [`../docs/07-pendencias-e-divergencias.md`](../docs/07-pendencias-e-divergencias.md).

## Validação × regra de negócio

Nem tudo cabe em anotação. A divisão usada aqui:

| | Bean Validation | Regra de negócio |
|---|---|---|
| Olha para | **um campo**, isoladamente | o objeto inteiro, o banco, o contexto |
| Onde | DTO | Service |
| Falha vira | **400** | **409** (`RegraDeNegocioException`) |
| Exemplo | "valor deve ser positivo" | "pagamento PAGO exige data de pagamento" |

O caso do `dataPagamento` mostra a fronteira — e é uma **pendência ainda aberta**:

```java
// src/main/java/br/com/fiap/clyvovet/dto/pagamento/PagamentoRequest.java
@NotNull(message = "Data de pagamento é obrigatória")
@PastOrPresent(message = "Data de pagamento não pode ser futura")
private LocalDate dataPagamento;
```

O problema: um pagamento **PENDENTE** não tem data ainda — o próprio seed grava pendentes
com `data_pagamento` nula. Como está, é impossível registrar um pendente pela API; o usuário
é forçado a inventar uma data.

A correção é justamente mover a obrigatoriedade para onde o contexto existe:

```java
// no service — a regra depende de OUTRO campo, então não cabe numa anotação de campo
if (request.getStatusPagamento() == StatusPagamento.PAGO && request.getDataPagamento() == null) {
    throw new RegraDeNegocioException("dataPagamento", "Pagamento com status PAGO exige data de pagamento");
}
```

O `@PastOrPresent` pode ficar: ele **ignora nulos** por especificação.

## Validação no PATCH

O PATCH tem outro DTO justamente por causa da validação:

```java
// src/main/java/br/com/fiap/clyvovet/dto/eventoClinico/EventoClinicoPatchRequest.java
/**
 * Corpo do PATCH: so os campos que mudam.
 *
 * Mantem as restricoes de FORMATO e abre mao das de PRESENCA.
 */
@Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d", message = "Hora deve estar no formato HH:mm")
private String hora;
```

Regra: **formato fica, presença sai.** Se `hora` vier, precisa ser `HH:mm`; se não vier, não
se mexe no campo.

A alternativa seria reaproveitar o `Request` com **grupos de validação**
(`@NotBlank(groups = Criacao.class)`). O projeto não foi por aí porque grupos espalham a
regra em anotações condicionais difíceis de ler, e porque os dois DTOs têm semânticas
genuinamente diferentes.

## Armadilhas

### 1. `@NotNull` numa String aceita `""`

`@NotNull` só checa nulo. Para texto obrigatório é sempre **`@NotBlank`**. Foi exatamente
esse o bug do campo `hora`: `@NotNull` deixava passar `""`, que estourava no banco.

### 2. Validar `Pageable` ou `@RequestParam` é outra história

`@Valid` cobre o `@RequestBody`. Para parâmetros de query é preciso `@Validated` na classe.
Aqui os `@RequestParam` são todos opcionais e sem restrição, então não aparece.

### 3. Validação não substitui constraint no banco

Unicidade de CPF, CNPJ e CRMV existe **só** como constraint no banco. Duas requisições
simultâneas com o mesmo CPF passam as duas pela validação — só o banco decide quem chega
primeiro. Por isso existe a rede de segurança:

```java
@ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<ErroValidacao> handleIntegridade(DataIntegrityViolationException ex) {
    log.warn("Violacao de integridade ao gravar", ex);
    return respostaDe(HttpStatus.CONFLICT, "registro",
            "Registro duplicado ou em uso por outro cadastro.");
}
```

Repare que a causa vai para o **log**, não para o cliente — a mensagem do Oracle carregaria o
SQL e o nome da constraint, expondo a estrutura interna.

## Perguntas de avaliação oral

1. Qual a diferença entre `@NotNull`, `@NotEmpty` e `@NotBlank`? Qual usar para uma String
   obrigatória?
2. O que acontece se você esquecer o `@Valid` no parâmetro do controller?
3. Por que `@Size(max = 1000)` em `descricao`? De onde vem esse número?
4. Por que a obrigatoriedade de `dataPagamento` não deveria ser um `@NotNull`?
5. Validação de campo dá 400 e regra de negócio dá 409. Por que a distinção importa para
   quem consome a API?
6. Se a validação já garante o formato, por que ainda existe CHECK constraint no banco?

---

**Anterior:** [04 — JPA e Hibernate](04-jpa-e-hibernate.md) ·
**Próximo:** [06 — Spring Security](06-spring-security.md)
