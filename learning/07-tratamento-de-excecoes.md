# 07 — Tratamento de exceções

## O que é

Sem um ponto central, cada controller acaba com `try/catch` traduzindo exceção em resposta —
e cada um inventa o próprio formato de erro. O `@RestControllerAdvice` resolve: **um lugar
só** converte exceção em status HTTP + corpo.

```java
// ❌ o que acontece sem um handler global
@GetMapping("/{id}")
public ResponseEntity<?> buscarPorId(@PathVariable UUID id) {
    try {
        return ResponseEntity.ok(service.buscarPorId(id));
    } catch (EntityNotFoundException e) {
        return ResponseEntity.status(404).body(Map.of("erro", e.getMessage()));
    } catch (Exception e) {
        return ResponseEntity.status(500).body("erro interno");
    }
}
```

```java
// ✅ com handler global — o controller só cuida do caminho feliz
@GetMapping("/{id}")
public ResponseEntity<AnimalResponse> buscarPorId(@PathVariable UUID id) {
    return ResponseEntity.ok(animalService.buscarPorId(id));
}
```

## O handler deste projeto

```java
// src/main/java/br/com/fiap/clyvovet/exception/GlobalExceptionHandler.java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<ErroValidacao>> handleValidationErrors(MethodArgumentNotValidException ex) { ... }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroValidacao> handleRecursoNaoEncontrado(RecursoNaoEncontradoException ex) {
        return respostaDe(HttpStatus.NOT_FOUND, "id", ex.getMessage());
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroValidacao> handleRegraDeNegocio(RegraDeNegocioException ex) {
        return respostaDe(HttpStatus.CONFLICT, ex.getCampo(), ex.getMessage());
    }
    ...
}
```

`@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody`: vale para todos os
controllers e o retorno vira JSON.

### O mapa completo

| Exceção | Status | Origem |
|---|---|---|
| `MethodArgumentNotValidException` | **400** | Bean Validation |
| `RecursoNaoEncontradoException` | **404** | domínio — id inexistente |
| `EntityNotFoundException` | **404** | rede de segurança do JPA |
| `RegraDeNegocioException` | **409** | domínio — regra violada |
| `DataIntegrityViolationException` | **409** | constraint do banco |
| `BadCredentialsException` | **401** | falha de login |

## Exceção de domínio, não de infraestrutura

Este projeto criou a própria exceção de "não encontrado" em vez de deixar subir a do JPA:

```java
// src/main/java/br/com/fiap/clyvovet/exception/RecursoNaoEncontradoException.java
/**
 * Recurso inexistente — mapeada para 404 pelo GlobalExceptionHandler.
 *
 * E uma excecao de dominio, e nao a EntityNotFoundException do JPA: quem
 * decide que "buscar por um id inexistente e um erro" e a regra da aplicacao,
 * nao a camada de persistencia. Manter a excecao do JPA subindo pelos services
 * amarraria o dominio a uma escolha de infraestrutura.
 */
public class RecursoNaoEncontradoException extends RuntimeException {
```

O argumento vale entender: se o service lançasse `EntityNotFoundException`, trocar JPA por
outra coisa um dia obrigaria a mexer em toda a camada de negócio. A exceção de domínio isola
essa decisão.

Repare que ela **estende `RuntimeException`** (unchecked). É a escolha padrão no Spring:
checked exception obrigaria `throws` em toda a cadeia, poluindo assinaturas sem ganho — e o
`@RestControllerAdvice` captura as duas do mesmo jeito.

### O truque do `Supplier`

```java
public static Supplier<RecursoNaoEncontradoException> naoEncontrado(Recurso recurso, UUID id) {
    return () -> new RecursoNaoEncontradoException(recurso, id);
}
```

Existe porque `Optional.orElseThrow` espera um **fornecedor**, não uma exceção pronta:

```java
// src/main/java/br/com/fiap/clyvovet/repository/RepositorioBase.java
default T obterPorId(UUID id, Recurso recurso) {
    return findById(id).orElseThrow(RecursoNaoEncontradoException.naoEncontrado(recurso, id));
}
```

A exceção só é **construída** se o `Optional` estiver vazio.

## Mensagem no enum, não espalhada

```java
// src/main/java/br/com/fiap/clyvovet/exception/Recurso.java
public enum Recurso {

    ANIMAL("Animal não encontrado"),
    CLINICA("Clínica não encontrada"),
    EVENTO_CLINICO("Evento clínico não encontrado"),
    PAGAMENTO("Pagamento não encontrado"),
    TUTOR("Tutor não encontrado"),
    USUARIO("Usuário não encontrado"),
    VETERINARIO("Veterinário não encontrado");

    public String mensagemDeAusencia(UUID id) {
        return ausencia + " com ID: " + id;
    }
}
```

O comentário da classe conta o problema que isso resolveu: a mensagem estava repetida em
cerca de vinte pontos, *"cada um concatenando o próprio texto. Além da duplicação, a
concordância variava ('não encontrado' × 'não encontrada')"*.

É um exemplo pequeno e ótimo de DRY: cada recurso declara a própria frase **uma vez**.

## Não vazar detalhe interno

```java
@ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<ErroValidacao> handleIntegridade(DataIntegrityViolationException ex) {
    log.warn("Violacao de integridade ao gravar", ex);
    return respostaDe(HttpStatus.CONFLICT, "registro",
            "Registro duplicado ou em uso por outro cadastro.");
}
```

A causa vai para o **log**; o cliente recebe mensagem genérica. Sem isso, uma duplicata de
CPF subiria como 500 carregando o SQL e o nome da constraint (`uk_tutor_cpf`) na resposta —
o que entrega a estrutura interna do banco a quem estiver sondando.

O mesmo princípio no login:

```java
@ExceptionHandler(BadCredentialsException.class)
public ResponseEntity<ErroValidacao> handleCredenciais(BadCredentialsException ex) {
    return respostaDe(HttpStatus.UNAUTHORIZED, "credenciais", ex.getMessage());
}
```

E a mensagem que chega aqui é sempre a genérica definida no `AuthService` — *"distinguir
'senha errada' de 'e-mail inexistente' permitiria enumerar a base"*.

**A regra geral:** mensagem de erro é para o usuário resolver o problema dele, não para o
atacante mapear o seu sistema. Detalhe técnico vai para o log.

## 401 e 403 fora do `@RestControllerAdvice`

Erros de segurança acontecem **nos filtros**, antes de chegar ao Spring MVC — então o
`@RestControllerAdvice` não os enxerga. Por isso existe um componente próprio:

```java
// src/main/java/br/com/fiap/clyvovet/config/SecurityConfig.java
.exceptionHandling(ex -> ex
        .authenticationEntryPoint(respostaErroSeguranca)   // 401
        .accessDeniedHandler(respostaErroSeguranca))       // 403
```

`RespostaErroSeguranca` devolve o mesmo formato JSON dos demais erros. Sem ele, 401 e 403
sairiam como página HTML padrão do container — quebrando qualquer cliente que espere JSON.

Vale saber isso: é uma pegadinha comum ("por que meu handler global não pega o 403?").

## Formato único

```java
// src/main/java/br/com/fiap/clyvovet/dto/exception/ErroValidacao.java
public record ErroValidacao(String campo, String mensagem) {}
```

Um formato só, em todos os erros. Erro de validação devolve uma **lista** (podem ser vários
campos); os demais devolvem um objeto.

## Armadilhas

### 1. Capturar `Exception` genérica esconde bug

Um `@ExceptionHandler(Exception.class)` devolvendo 500 formatado parece cuidadoso, mas
transforma `NullPointerException` em "erro interno" silencioso. Este projeto **não** tem esse
handler — falha inesperada sobe como 500 e aparece no log.

### 2. O handler não pega o que acontece nos filtros

Vale para JWT, CORS e rate limit. Ver a seção acima.

### 3. Ordem entre handlers

Se houvesse `@ExceptionHandler(RuntimeException.class)` e
`@ExceptionHandler(RecursoNaoEncontradoException.class)`, o Spring escolhe o **mais
específico**. Mas é fácil errar ao criar hierarquias de exceção — quanto mais rasa, melhor.

## Perguntas de avaliação oral

1. Por que os controllers não têm `try/catch`?
2. Por que criar `RecursoNaoEncontradoException` em vez de usar `EntityNotFoundException`?
3. Por que `naoEncontrado()` devolve um `Supplier` e não a exceção pronta?
4. Por que a mensagem de "não encontrado" fica no enum `Recurso`?
5. Por que a `DataIntegrityViolationException` vira mensagem genérica, com a causa só no log?
6. Por que 401 e 403 **não** passam pelo `GlobalExceptionHandler`? Quem cuida deles?
7. As exceções deste projeto são checked ou unchecked? Por quê?

---

**Anterior:** [06 — Spring Security](06-spring-security.md) ·
**Próximo:** [08 — Cache](08-cache.md)
