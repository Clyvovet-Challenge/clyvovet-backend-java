# 05 — Bean Validation

> **Pré-requisito:** [03 — API REST](03-api-rest.md) (status 400 × 409) e a seção sobre
> **anotações** do [00](00-java-essencial.md).

---

## A regra que organiza tudo: nunca confie no cliente

Quem chama a sua API pode mandar qualquer coisa. Não porque é malicioso — muitas vezes é só
um bug no app, um campo que ficou vazio, um formulário mal preenchido.

```jsonc
POST /api/v1/animais
{ "nome": "", "tutorId": null }
```

Se a API aceitar, você grava lixo. Se estourar sem explicação, o cliente não sabe o que
corrigir. O certo é **recusar com clareza**:

```jsonc
400 Bad Request
[
  { "campo": "nome",    "mensagem": "Nome é obrigatório" },
  { "campo": "tutorId", "mensagem": "Tutor é obrigatório" }
]
```

---

## Sem e com Bean Validation

```java
// ❌ validação na mão — no controller
@PostMapping
public ResponseEntity<?> criar(@RequestBody AnimalRequest request) {
    if (request.getNome() == null || request.getNome().isBlank()) {
        return ResponseEntity.badRequest().body("Nome é obrigatório");
    }
    if (request.getTutorId() == null) {
        return ResponseEntity.badRequest().body("Tutor é obrigatório");
    }
    if (request.getObservacao() != null && request.getObservacao().length() > 1000) {
        return ResponseEntity.badRequest().body("Observação muito longa");
    }
    // ... e a regra do negócio ainda nem começou
}
```

Problemas: o controller engordou (violando o que vimos no
[02](02-arquitetura-em-camadas.md)), só o **primeiro** erro é reportado, e os outros 6
recursos vão repetir tudo.

```java
// ✅ com Bean Validation — a restrição mora no DTO
@NotBlank(message = "Nome é obrigatório")
private String nome;

@NotNull(message = "Tutor é obrigatório")
private UUID tutorId;

@Size(max = 1000)
private String observacao;
```

```java
// e o controller volta a ter uma linha
@PostMapping
public ResponseEntity<AnimalResponse> criar(@Valid @RequestBody AnimalRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(animalService.criar(request));
}
```

**A validação roda antes do corpo do método.** Se falhar, `animalService.criar` **nem é
chamado**.

---

## As duas peças (e o erro clássico)

```java
// 1. no controller: @Valid é o GATILHO
public ResponseEntity<AnimalResponse> criar(@Valid @RequestBody AnimalRequest request)

// 2. no DTO: as restrições
@NotBlank
private String nome;
```

⚠️ **Sem `@Valid`, as anotações do DTO são simplesmente ignoradas.** Nenhum erro, nenhum
aviso — os dados passam direto. É o bug mais comum de quem está começando, e é silencioso.

💡 **Conceito: declarativo precisa de alguém que leia**

Lembra do [00](00-java-essencial.md): anotação é um bilhete que **alguém** lê depois. `@NotBlank`
sozinha não faz nada.

Quem lê é o validador do Spring — e ele só é acionado quando vê `@Valid` no parâmetro. Sem
o gatilho, os bilhetes ficam colados no DTO sem ninguém para lê-los.

Sempre que uma anotação "não funcionou", a primeira pergunta é: **quem deveria ler isso, e
foi acionado?**

---

## As restrições usadas neste projeto

| Anotação | Verifica | Cuidado |
|---|---|---|
| `@NotNull` | não é `null` | **aceita `""` e `"   "`** |
| `@NotBlank` | texto não nulo, não vazio, não só espaços | só para `String` |
| `@NotEmpty` | coleção/texto não vazio | aceita `"   "` |
| `@Size(min, max)` | tamanho | alinhe com a coluna |
| `@Email` | formato de e-mail | permissivo por padrão |
| `@Positive` | número > 0 | |
| `@PastOrPresent` | data não futura | **ignora `null`** |
| `@Digits(integer, fraction)` | dígitos de um decimal | espelha `NUMBER(10,2)` |
| `@Pattern(regexp)` | expressão regular | último recurso, mas resolve formato |

### A confusão que custa caro: `@NotNull` × `@NotBlank`

```java
@NotNull  private String nome;   // "" passa ✅ (e não deveria)
@NotBlank private String nome;   // "" é recusado ✅
```

Para **texto obrigatório é sempre `@NotBlank`**. Este projeto aprendeu na prática: o campo
`hora` era `@NotNull`, então `""` passava, chegava ao banco e estourava lá.

### Três restrições cobrindo coisas diferentes

```java
// src/main/java/br/com/fiap/clyvovet/dto/pagamento/PagamentoRequest.java
@NotNull(message = "Valor é obrigatório")
@Positive(message = "Valor deve ser positivo")
@Digits(integer = 9, fraction = 2, message = "Valor inválido: máximo 9 dígitos inteiros e 2 decimais")
private BigDecimal valor;
```

`@NotNull` cuida da presença, `@Positive` do sinal, `@Digits` do formato. Cada uma responde
uma pergunta — e juntas descrevem exatamente o que a coluna `NUMBER(10,2)` aceita.

---

## Como o erro vira resposta

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

Lendo em português: *"pegue todos os erros de campo, transforme cada um num `ErroValidacao`
com o nome do campo e a mensagem, junte numa lista e devolva 400"*.

O `.stream()...map()...toList()` é o jeito idiomático de transformar uma lista em outra —
`map` aplica a função a cada item (revisar [00, seção 9](00-java-essencial.md)).

O cliente recebe **todos** os campos com problema de uma vez, não só o primeiro. Isso importa
para a experiência: um formulário que mostra um erro por vez é insuportável.

---

## A regra de ouro: validação espelha a coluna

**O que a coluna não comporta, a validação deve recusar.** Assim o erro vira 400 com o campo
indicado, e não 500 vindo do banco.

```java
// src/main/java/br/com/fiap/clyvovet/dto/eventoClinico/EventoClinicoRequest.java
@Size(max = 1000)   // coluna VARCHAR2(1000)
private String descricao;
```

O comentário `// coluna VARCHAR2(1000)` é a prática que sustenta isso — quem mexer depois vê
**de onde veio o número** e não o muda por engano.

Este alinhamento foi conquistado corrigindo defeitos reais, e eles falharam em **direções
opostas**:

| Direção | Campo | O que acontecia |
|---|---|---|
| Validação **mais estreita** que a coluna | `crmv` era 4–6; a coluna é 30 | o CRMV real (`CRMV-SP 14320`, 13 caracteres) era **recusado** — os dados do próprio seed não podiam ser recriados pela API |
| Validação **mais larga** que a coluna | `observacao` sem `@Size`; coluna `VARCHAR2(1000)` | texto grande passava e estourava no INSERT → **500** |

Itens 6 e 18 de
[`../docs/07-pendencias-e-divergencias.md`](../docs/07-pendencias-e-divergencias.md).

Repare que os dois erros são igualmente ruins, por motivos diferentes: um bloqueia uso
legítimo, o outro transforma erro do cliente em erro do servidor.

---

## Validação × regra de negócio

Nem tudo cabe em anotação. A divisão:

| | Bean Validation | Regra de negócio |
|---|---|---|
| Olha para | **um campo**, isolado | o objeto inteiro, o banco, o contexto |
| Onde mora | DTO | **Service** |
| Falha vira | **400** | **409** (`RegraDeNegocioException`) |
| Exemplo | "valor deve ser positivo" | "pagamento PAGO exige data de pagamento" |

O teste para decidir: **essa restrição depende de outro campo, de outro registro ou do banco?**
Se sim, é regra de negócio.

### O caso real, ainda em aberto

```java
// src/main/java/br/com/fiap/clyvovet/dto/pagamento/PagamentoRequest.java
@NotNull(message = "Data de pagamento é obrigatória")
@PastOrPresent(message = "Data de pagamento não pode ser futura")
private LocalDate dataPagamento;
```

O problema: um pagamento **PENDENTE** ainda não tem data — o próprio seed grava pendentes com
`data_pagamento` nula. Como está, **é impossível registrar um pendente pela API**; o usuário é
forçado a inventar uma data.

A obrigatoriedade **depende do status** — ou seja, de outro campo. Não cabe numa anotação de
campo:

```java
// no service, onde o contexto existe
if (request.getStatusPagamento() == StatusPagamento.PAGO && request.getDataPagamento() == null) {
    throw new RegraDeNegocioException("dataPagamento", "Pagamento com status PAGO exige data de pagamento");
}
```

O `@PastOrPresent` pode continuar: por especificação, ele **ignora nulos**.

É o item 7 de [`../docs/07`](../docs/07-pendencias-e-divergencias.md), aberto.

---

## Validação no PATCH

```java
// src/main/java/br/com/fiap/clyvovet/dto/eventoClinico/EventoClinicoPatchRequest.java
/**
 * Corpo do PATCH: so os campos que mudam.
 * Mantem as restricoes de FORMATO e abre mao das de PRESENCA.
 */
@Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d", message = "Hora deve estar no formato HH:mm")
private String hora;
```

**Formato fica, presença sai.** Se `hora` vier, precisa ser `HH:mm`; se não vier, o campo não
é tocado.

A alternativa seria reaproveitar o `Request` com **grupos de validação**:

```java
// ❌ o caminho não escolhido
@NotBlank(groups = Criacao.class)
@Pattern(regexp = "...", groups = {Criacao.class, Atualizacao.class})
private String hora;
```

Funciona, e o projeto não foi por aí: grupos espalham a regra em anotações condicionais
difíceis de ler, e os dois DTOs têm semânticas genuinamente diferentes. Duas classes simples
costumam vencer uma classe com dois modos.

---

## Armadilhas

### 1. Esquecer o `@Valid` (de novo)

Vale repetir porque é o erro mais comum, e falha em silêncio.

### 2. Validar `@RequestParam` é outra história

`@Valid` cobre o `@RequestBody`. Para parâmetros de query seria preciso `@Validated` na
classe. Aqui não aparece porque todos os `@RequestParam` são opcionais e sem restrição.

### 3. Validação **não** substitui constraint no banco

A unicidade de CPF, CNPJ e CRMV existe **só** como constraint no banco. E não dá para
resolver na validação:

> Duas requisições simultâneas com o mesmo CPF passam **as duas** pela validação — cada uma
> consulta antes de a outra gravar. Só o banco, que serializa as escritas, decide quem chega
> primeiro.

Isso se chama **condição de corrida**, e é por isso que existe a rede de segurança:

```java
@ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<ErroValidacao> handleIntegridade(DataIntegrityViolationException ex) {
    log.warn("Violacao de integridade ao gravar", ex);
    return respostaDe(HttpStatus.CONFLICT, "registro",
            "Registro duplicado ou em uso por outro cadastro.");
}
```

E note: a causa vai para o **log**, não para o cliente. A mensagem do Oracle carregaria o SQL
e o nome da constraint (`uk_tutor_cpf`), expondo a estrutura interna a quem estiver sondando.

**A validação melhora a mensagem; a constraint garante a regra.** As duas são necessárias.

---

## Consolidação

**Entender**
1. Qual a diferença entre `@NotNull`, `@NotEmpty` e `@NotBlank`? Qual usar para texto
   obrigatório?
2. O que acontece se você esquecer o `@Valid` no parâmetro do controller?

**Aplicar**
3. Escreva as anotações para um campo `peso` (`BigDecimal`), obrigatório, positivo, com no
   máximo 3 dígitos inteiros e 3 decimais.
4. `@Size(max = 1000)` em `descricao` — de onde veio esse número? Onde você conferiria?

**Analisar**
5. Por que a obrigatoriedade de `dataPagamento` **não** deveria ser `@NotNull`? Qual o teste
   para decidir isso?
6. Validação de campo dá 400 e regra de negócio dá 409. Por que a distinção importa para
   quem consome a API?

**Avaliar**
7. Se a validação já garante o formato, por que manter CHECK constraint no banco? Dê um
   cenário em que só a constraint salva.
8. Um colega quer validar "CPF já existe" com `@Unique` customizada no DTO. Isso resolve o
   problema? O que ele está deixando de considerar?

---

## Se você levar só uma coisa daqui

**Validação melhora a mensagem; constraint garante a regra.** Nenhuma das duas substitui a
outra — a validação evita o 500, a constraint evita o dado corrompido.

---

**Anterior:** [04 — JPA e Hibernate](04-jpa-e-hibernate.md) ·
**Próximo:** [06 — Spring Security](06-spring-security.md)
