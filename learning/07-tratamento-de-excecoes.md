# 07 — Tratamento de exceções

> **Pré-requisito:** [03 — API REST](03-api-rest.md) (status codes) e a seção sobre
> **exceções** do [00](00-java-essencial.md).

---

## O problema

Quando algo dá errado no meio do código, uma **exceção** é lançada e "sobe" até alguém
tratar. Se ninguém tratar, o Spring devolve **500** com uma página de erro genérica.

Sem organização, cada controller vira isto:

```java
// ❌ o que acontece sem um lugar central
@GetMapping("/{id}")
public ResponseEntity<?> buscarPorId(@PathVariable UUID id) {
    try {
        return ResponseEntity.ok(service.buscarPorId(id));
    } catch (EntityNotFoundException e) {
        return ResponseEntity.status(404).body(Map.of("erro", e.getMessage()));
    } catch (DataIntegrityViolationException e) {
        return ResponseEntity.status(409).body("conflito");
    } catch (Exception e) {
        return ResponseEntity.status(500).body("erro interno");
    }
}
```

Multiplique por 42 endpoints. Três consequências: o controller some sob `try/catch`, cada um
inventa um formato de erro diferente, e o cliente nunca sabe o que esperar.

---

## A solução: um lugar só

```java
// src/main/java/br/com/fiap/clyvovet/exception/GlobalExceptionHandler.java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroValidacao> handleRecursoNaoEncontrado(RecursoNaoEncontradoException ex) {
        return respostaDe(HttpStatus.NOT_FOUND, "id", ex.getMessage());
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroValidacao> handleRegraDeNegocio(RegraDeNegocioException ex) {
        return respostaDe(HttpStatus.CONFLICT, ex.getCampo(), ex.getMessage());
    }
}
```

E o controller volta a ter uma linha:

```java
@GetMapping("/{id}")
public ResponseEntity<AnimalResponse> buscarPorId(@PathVariable UUID id) {
    return ResponseEntity.ok(animalService.buscarPorId(id));
}
```

| Anotação | O que faz |
|---|---|
| `@RestControllerAdvice` | "eu trato exceções de **todos** os controllers, e devolvo JSON" |
| `@ExceptionHandler(X.class)` | "este método cuida da exceção X" |

💡 **Conceito: *cross-cutting concern***

Tratamento de erro é uma preocupação **transversal**: atravessa todas as camadas e todos os
recursos, mas não pertence a nenhum deles.

Colocar transversal dentro de cada classe é o que gera duplicação massiva. O padrão do Spring
é **extrair para um lugar e aplicar por fora** — mesma ideia por trás de `@Transactional`
(transação), `@Cacheable` (cache) e dos filtros de segurança.

Reconhecer uma preocupação transversal é o que faz você parar de copiar `try/catch`.

---

## O mapa completo deste projeto

| Exceção | Status | Vem de |
|---|---|---|
| `MethodArgumentNotValidException` | **400** | Bean Validation |
| `RecursoNaoEncontradoException` | **404** | domínio — id inexistente |
| `EntityNotFoundException` | **404** | rede de segurança do JPA |
| `RegraDeNegocioException` | **409** | domínio — regra violada |
| `DataIntegrityViolationException` | **409** | constraint do banco |
| `BadCredentialsException` | **401** | falha de login |

Formato único de saída:

```java
// src/main/java/br/com/fiap/clyvovet/dto/exception/ErroValidacao.java
public record ErroValidacao(String campo, String mensagem) {}
```

Erro de validação devolve uma **lista** (podem ser vários campos); os demais devolvem um
objeto.

---

## Exceção de domínio, não de infraestrutura

Este projeto criou a própria exceção de "não encontrado" em vez de deixar subir a do JPA:

```java
// src/main/java/br/com/fiap/clyvovet/exception/RecursoNaoEncontradoException.java
/**
 * E uma excecao de dominio, e nao a EntityNotFoundException do JPA: quem
 * decide que "buscar por um id inexistente e um erro" e a regra da aplicacao,
 * nao a camada de persistencia. Manter a excecao do JPA subindo pelos services
 * amarraria o dominio a uma escolha de infraestrutura.
 */
public class RecursoNaoEncontradoException extends RuntimeException {
```

O argumento vale entender. Se o service lançasse `EntityNotFoundException` (uma classe do
JPA), então:

- a camada de **negócio** passaria a depender de uma escolha de **persistência**;
- trocar JPA por outra coisa um dia obrigaria a mexer em toda a camada de negócio.

Além disso, há um argumento conceitual: *"animal não encontrado"* é uma decisão da
**aplicação**, não do banco. Para o banco, um `SELECT` sem resultado é normal — quem chama
isso de erro é a sua regra.

### Por que `extends RuntimeException`

| Tipo | Obriga tratar? |
|---|---|
| *checked* (`extends Exception`) | sim — `try/catch` ou `throws` em toda a cadeia |
| *unchecked* (`extends RuntimeException`) | não |

Todas as exceções daqui são **unchecked**. O motivo: como existe um lugar único que captura
tudo (`@RestControllerAdvice`), obrigar `throws RecursoNaoEncontradoException` em cada método
da cadeia só poluiria assinaturas — sem ninguém no meio do caminho tendo o que fazer com a
informação.

### O truque do `Supplier`

```java
public static Supplier<RecursoNaoEncontradoException> naoEncontrado(Recurso recurso, UUID id) {
    return () -> new RecursoNaoEncontradoException(recurso, id);
}
```

```java
// src/main/java/br/com/fiap/clyvovet/repository/RepositorioBase.java
default T obterPorId(UUID id, Recurso recurso) {
    return findById(id).orElseThrow(RecursoNaoEncontradoException.naoEncontrado(recurso, id));
}
```

`Optional.orElseThrow` espera um **fornecedor** de exceção, não uma exceção pronta — assim a
exceção só é **construída** se o `Optional` estiver vazio. No caminho feliz, o objeto nem
chega a existir.

---

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

O comentário da classe conta o problema resolvido: a mensagem estava repetida em cerca de
vinte pontos, *"cada um concatenando o próprio texto. Além da duplicação, a concordância
variava ('não encontrado' × 'não encontrada')"*.

É DRY em escala pequena, e por isso mesmo é um bom exemplo: cada recurso declara a própria
frase **uma vez**, e o enum garante que não existe um oitavo recurso sem mensagem.

---

## A regra mais importante: não vazar detalhe interno

```java
@ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<ErroValidacao> handleIntegridade(DataIntegrityViolationException ex) {
    log.warn("Violacao de integridade ao gravar", ex);
    return respostaDe(HttpStatus.CONFLICT, "registro",
            "Registro duplicado ou em uso por outro cadastro.");
}
```

Repare na assimetria: a causa completa vai para o **log**; o cliente recebe uma mensagem
genérica.

Sem isso, uma duplicata de CPF subiria assim:

```
500 Internal Server Error
ORA-00001: unique constraint (RM550341.UK_TUTOR_CPF) violated
```

O que isso entrega de graça a quem estiver sondando: o banco é Oracle, o schema chama
`RM550341`, existe uma tabela `tutor`, existe uma constraint em `cpf`. Cada detalhe é um passo
a mais no mapa de quem quer atacar.

O mesmo princípio no login:

```java
@ExceptionHandler(BadCredentialsException.class)
public ResponseEntity<ErroValidacao> handleCredenciais(BadCredentialsException ex) {
    return respostaDe(HttpStatus.UNAUTHORIZED, "credenciais", ex.getMessage());
}
```

E a mensagem que chega aqui é sempre a genérica definida no `AuthService` — porque
*"distinguir 'senha errada' de 'e-mail inexistente' permitiria enumerar a base"*
(ver [06](06-spring-security.md)).

💡 **Conceito: a quem a mensagem de erro serve**

Existem **dois públicos** para um erro, e eles querem coisas opostas:

- **O usuário** precisa saber o que **ele** pode corrigir: *"o CPF já está cadastrado"*.
- **Você** precisa saber o que aconteceu tecnicamente: stack trace, SQL, constraint.

A regra: **detalhe técnico vai para o log; mensagem acionável vai para o cliente.** Um erro
que só diz "erro interno" frustra o usuário; um que devolve o stack trace ajuda o atacante.

---

## 401 e 403 não passam por aqui

Este é um ponto que confunde muita gente.

Erros de segurança acontecem **nos filtros**, antes de a requisição chegar ao Spring MVC. E
`@RestControllerAdvice` é um mecanismo do MVC — ele **não enxerga** o que acontece antes.

Por isso existe um componente próprio:

```java
// src/main/java/br/com/fiap/clyvovet/config/SecurityConfig.java
.exceptionHandling(ex -> ex
        .authenticationEntryPoint(respostaErroSeguranca)   // 401
        .accessDeniedHandler(respostaErroSeguranca))       // 403
```

`RespostaErroSeguranca` devolve o **mesmo formato JSON** dos demais erros. Sem ele, 401 e 403
sairiam como página HTML padrão do container — e qualquer cliente que espere JSON quebraria
ao tentar interpretar `<html>`.

Guarde a pergunta diagnóstica: **"meu handler global não pega o 403"** → é porque o erro
nasceu antes do MVC.

---

## Armadilhas

### 1. Capturar `Exception` genérica esconde bug

```java
// ❌ tentador, e ruim
@ExceptionHandler(Exception.class)
public ResponseEntity<ErroValidacao> handleTudo(Exception ex) {
    return respostaDe(HttpStatus.INTERNAL_SERVER_ERROR, "erro", "Erro interno");
}
```

Parece cuidadoso. Na prática, transforma todo `NullPointerException` num 500 bonitinho — e
o bug **some do radar**, porque ninguém percebe a diferença entre "erro esperado" e "bug que
ninguém previu".

Este projeto **não** tem esse handler. Falha inesperada sobe como 500 e aparece no log, onde
alguém pode consertá-la.

### 2. Ordem entre handlers

Se houvesse `@ExceptionHandler(RuntimeException.class)` **e**
`@ExceptionHandler(RecursoNaoEncontradoException.class)`, o Spring escolhe o **mais
específico**. Funciona — mas hierarquias profundas de exceção tornam difícil prever qual
handler pega o quê. Quanto mais rasa, melhor.

### 3. Exceção não é fluxo de controle

Exceção é para o **excepcional**. Usar `throw`/`catch` para decidir o caminho normal do
programa é lento (a JVM monta o stack trace) e ilegível.

---

## Consolidação

**Entender**
1. Por que os controllers deste projeto não têm `try/catch`?
2. O que é uma preocupação transversal (*cross-cutting concern*)? Cite outras duas neste
   projeto.

**Aplicar**
3. Você precisa devolver **422** para uma regra nova. O que criaria e onde?
4. Um `NullPointerException` acontece num service. Que status o cliente recebe hoje, e por
   quê?

**Analisar**
5. Por que criar `RecursoNaoEncontradoException` em vez de usar `EntityNotFoundException`?
6. Por que `naoEncontrado()` devolve um `Supplier` e não a exceção pronta?
7. Por que 401 e 403 **não** passam pelo `GlobalExceptionHandler`? Quem cuida deles?

**Avaliar**
8. Por que a `DataIntegrityViolationException` vira mensagem genérica? O que exatamente
   vazaria sem isso, e por que cada detalhe importa?
9. Um colega adiciona `@ExceptionHandler(Exception.class)` "para nunca mais ver 500 feio". O
   que você argumenta?

---

## Se você levar só uma coisa daqui

**Detalhe técnico vai para o log; mensagem acionável vai para o cliente.** Um erro serve a
dois públicos com necessidades opostas, e confundi-los ou frustra o usuário ou ajuda o
atacante.

---

**Anterior:** [06 — Spring Security](06-spring-security.md) ·
**Próximo:** [08 — Cache](08-cache.md)
