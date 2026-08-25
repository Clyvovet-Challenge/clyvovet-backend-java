# 00 — O Java que você precisa antes do Spring

> **Comece por aqui se:** você já viu Java na faculdade, mas trava quando aparece `<T>`,
> `Optional`, `::` ou uma anotação `@Alguma`.
>
> Este documento **não ensina Java do zero**. Ele cobre exatamente as construções que
> aparecem neste projeto e que costumam ser o obstáculo real — cada uma com o trecho do
> repositório onde ela aparece.

Se algo aqui parecer óbvio, pule. Se algo parecer estranho, **marque** — vai reaparecer nos
próximos documentos.

---

## 1. Classe e objeto — a base de tudo

Uma **classe** é uma planta. Um **objeto** é a casa construída a partir dela.

```java
// A planta: descreve o que todo Animal tem
public class Animal {
    private String nome;    // atributo (o dado)
    private String raca;

    public String getNome() {   // método (o comportamento)
        return nome;
    }
}
```

```java
// A casa: um Animal concreto, na memória
Animal bolinha = new Animal();
bolinha.setNome("Bolinha");
```

| Palavra | Significa |
|---|---|
| `public` | qualquer código pode usar |
| `private` | só a própria classe pode usar |
| `class` | estou definindo uma planta |
| `new` | construa um objeto a partir da planta |

💡 **Conceito: encapsulamento**

Repare que `nome` é `private` mas `getNome()` é `public`. Isso se chama
**encapsulamento**: o dado fica protegido, e o acesso passa por um método.

Por que importa aqui: se amanhã `nome` precisar ser sempre em maiúsculas, você muda **um**
método — e nenhum dos cem lugares que chamam `getNome()` precisa saber. Se `nome` fosse
`public`, cada lugar acessaria direto e a mudança seria impossível de fazer com segurança.

É a diferença entre uma torneira e um cano furado: os dois dão água, mas só um deixa você
controlar a vazão.

---

## 2. `final` — "isto não muda mais"

```java
private final AnimalRepository animalRepository;
```

`final` num atributo significa: **depois que receber um valor, nunca troca**. O compilador
recusa qualquer tentativa de reatribuir.

Isso aparece em **todo** service deste projeto, e não é decoração — é o que garante que a
dependência de um objeto não é trocada no meio da execução. Voltaremos a isso no documento
[01](01-spring-boot-e-injecao-de-dependencia.md).

---

## 3. Tipos que aparecem neste projeto

Java é **tipado**: toda variável declara o que guarda.

| Tipo | Guarda | Exemplo daqui |
|---|---|---|
| `String` | texto | `private String nome;` |
| `int` | número inteiro | `private int tentativasFalhas;` |
| `boolean` | verdadeiro/falso | `private boolean ativo;` |
| `UUID` | identificador único de 36 caracteres | `private UUID id;` |
| `LocalDate` | data sem hora | `private LocalDate dataNascimento;` |
| `LocalDateTime` | data com hora | `private LocalDateTime bloqueadoAte;` |
| `BigDecimal` | número decimal **exato** | `private BigDecimal valor;` |

### Por que `BigDecimal` e não `double` para dinheiro

Este é um dos erros mais caros da programação, e vale ver acontecendo:

```java
System.out.println(0.1 + 0.2);   // imprime 0.30000000000000004
```

`double` guarda números em binário, e há decimais que **não existem** exatamente em binário
— como 1/3 não existe exatamente em decimal. Some mil vezes e o erro vira centavos; some um
milhão e vira reais.

`BigDecimal` guarda o número como texto e faz a conta dígito a dígito. É mais lento, e é o
que se usa para dinheiro, sempre.

```java
// src/main/java/br/com/fiap/clyvovet/model/Pagamento.java
private BigDecimal valor;
```

E no banco, pelo mesmo motivo: `NUMBER(10,2)` no Oracle, `DECIMAL(10,2)` no MySQL — nunca
`DOUBLE`.

---

## 4. `null` — a ausência de objeto

`null` significa "esta variável não aponta para objeto nenhum".

```java
Animal animal = null;
animal.getNome();   // 💥 NullPointerException
```

O `NullPointerException` (NPE) é o erro mais comum do Java. Ele acontece quando você chama
um método em cima do nada.

Este projeto teve um NPE real, e ele é instrutivo:

```java
// ❌ antes — estourava quando o endereço vinha nulo do banco
enderecoMapper.toResponse(tutor.getEndereco())
```

Quando **todas** as colunas de endereço estão nulas, o Hibernate devolve `null` no campo
inteiro — não um objeto com campos vazios. O mapper não esperava isso.

A defesa, que você vai ver bastante:

```java
// ✅ null-guard
String tutorNome = animal.getTutor() != null ? animal.getTutor().getNome() : null;
```

O `? :` é o **operador ternário** — um `if/else` em uma linha:

```java
condição ? valorSeVerdadeiro : valorSeFalso
```

---

## 5. Interface — o contrato sem a implementação

Uma **interface** diz *o que* pode ser feito, sem dizer *como*.

```java
public interface Veiculo {
    void acelerar();     // sem corpo — só a assinatura
}

public class Carro implements Veiculo {
    public void acelerar() {          // aqui vem o como
        System.out.println("vruum");
    }
}
```

Por que isso importa neste projeto: os repositórios são **interfaces sem implementação
nenhuma**, e mesmo assim funcionam.

```java
// src/main/java/br/com/fiap/clyvovet/repository/UsuarioRepository.java
public interface UsuarioRepository extends RepositorioBase<Usuario> {
    Optional<Usuario> findByEmail(String email);
}
```

Não existe `UsuarioRepositoryImpl` em lugar nenhum. Quem escreve a implementação é o
**Spring Data**, em tempo de execução, lendo o **nome** do método. Isso vai ser destrinchado
no documento [04](04-jpa-e-hibernate.md) — por ora, só registre que é possível.

💡 **Conceito: programar contra interface**

Quando `AnimalService` declara que depende de `AnimalRepository` (a interface), ele não
sabe nem se importa com quem implementa.

Por que importa: num teste, você pode entregar uma implementação falsa que devolve dados
fixos, sem banco nenhum. O service não percebe a diferença — ele só conhece o contrato.
Se ele dependesse de uma classe concreta, testar exigiria um banco de verdade.

### `default` em interface

Desde o Java 8, uma interface pode ter método **com corpo**:

```java
// src/main/java/br/com/fiap/clyvovet/repository/RepositorioBase.java
default T obterPorId(UUID id, Recurso recurso) {
    return findById(id).orElseThrow(RecursoNaoEncontradoException.naoEncontrado(recurso, id));
}
```

Isso é usado aqui de um jeito esperto: o Spring Data gera implementação para métodos
**abstratos** (sem corpo), mas um método `default` ele apenas respeita. É o que permite
acrescentar comportamento ao repositório sem escrever classe nenhuma.

---

## 6. `enum` — uma lista fechada de valores

```java
// src/main/java/br/com/fiap/clyvovet/model/TipoEvento.java
public enum TipoEvento {
    CONSULTA, RETORNO, VACINA, EXAME, CIRURGIA, OUTRO
}
```

Um `enum` é um tipo cujos valores possíveis são **conhecidos e finitos**.

```java
evento.setTipoEvento(TipoEvento.CONSULTA);   // ✅
evento.setTipoEvento("consulta");            // ❌ nem compila
```

Compare com o que acontece quando **não** se usa enum. Neste projeto, `especie` é `String`
livre — e o resultado está documentado como defeito: o banco tem `'CAO'` em uns registros e
`'CACHORRO'` em outros, e o filtro `?especie=CAO` não encontra os segundos.

**A regra prática:** se o conjunto de valores é fechado (status, tipo, categoria), use enum.
O compilador vira seu revisor.

---

## 7. Generics — o `<T>` que assusta

`<T>` significa "um tipo que eu decido depois".

Sem generics, uma lista guardaria qualquer coisa e você teria que torcer:

```java
List nomes = new ArrayList();
nomes.add("Bolinha");
nomes.add(42);                        // compila! e quebra depois
String n = (String) nomes.get(1);     // 💥 em tempo de execução
```

Com generics, o tipo é travado:

```java
List<String> nomes = new ArrayList<>();
nomes.add("Bolinha");
nomes.add(42);        // ❌ nem compila
```

Neste projeto, `<T>` aparece assim:

```java
// src/main/java/br/com/fiap/clyvovet/repository/RepositorioBase.java
public interface RepositorioBase<T> extends JpaRepository<T, UUID> {
    default T obterPorId(UUID id, Recurso recurso) { ... }
}
```

Leia como: *"um repositório de **algum** tipo `T`, cujo id é `UUID`; o método `obterPorId`
devolve um objeto **desse mesmo** tipo"*.

E na hora de usar, `T` vira algo concreto:

```java
public interface AnimalRepository extends RepositorioBase<Animal> { }
//                                                        ↑ T = Animal
```

A partir daí, `animalRepository.obterPorId(id)` devolve um `Animal` — não um `Object` que
você precisa converter. **O compilador sabe.**

💡 **Conceito: generics são para o compilador, não para o programa**

`<T>` desaparece quando o código roda (chama-se *type erasure*). Ele existe para que
**erros de tipo apareçam ao compilar**, e não com o sistema no ar.

É a mesma ideia do enum: mover a descoberta do erro para o momento mais barato possível.
Erro que o compilador pega custa segundos. O mesmo erro em produção custa uma madrugada.

---

## 8. `Optional` — "pode ser que não tenha"

O problema: um método que busca algo pode não achar. Devolver `null` funciona, mas nada
lembra quem chama de verificar — e aí vem o NPE.

`Optional<T>` é uma caixa que ou tem um valor, ou está vazia. E o tipo **obriga** você a
encarar a possibilidade.

```java
// src/main/java/br/com/fiap/clyvovet/repository/UsuarioRepository.java
Optional<Usuario> findByEmail(String email);
```

Os métodos que aparecem neste projeto:

```java
optional.isEmpty()              // está vazia?
optional.get()                  // pega o valor (só depois de checar!)
optional.map(Animal::getTutor)  // se tem valor, transforma; se vazia, continua vazia
optional.filter(x -> ...)       // se tem valor e passa no teste, mantém
optional.orElseThrow(...)       // se vazia, lança exceção
optional.ifPresent(x -> ...)    // se tem valor, executa
```

Uso real, encadeado:

```java
// src/main/java/br/com/fiap/clyvovet/security/SegurancaService.java
animalRepository.findById(animalId)
        .map(Animal::getTutor)     // do animal, pega o tutor
        .map(Tutor::getId);        // do tutor, pega o id
```

Leia como: *"ache o animal; se achou, pegue o tutor; se tem tutor, pegue o id"*. Se **qualquer
etapa** vier vazia, o resultado é vazio — e **nenhum NPE acontece**.

Sem `Optional`, o mesmo código seria:

```java
Animal animal = animalRepository.findById(animalId);
if (animal != null) {
    Tutor tutor = animal.getTutor();
    if (tutor != null) {
        return tutor.getId();
    }
}
return null;
```

Seis linhas, dois `if` aninhados, e o risco de esquecer um deles.

---

## 9. Lambda e `::` — passar comportamento como valor

Este é o conceito que mais trava quem vem de Java básico. Vamos por partes.

### O problema

Às vezes você quer passar **uma ação** para um método, não um dado.

### Lambda: uma função sem nome

```java
x -> x * 2                    // recebe x, devolve x*2
(a, b) -> a + b               // recebe dois, devolve a soma
() -> System.out.println("oi")  // não recebe nada, faz algo
```

A seta separa **o que entra** do **que sai**.

### Method reference: o atalho `::`

Quando a lambda só chama um método existente, dá para encurtar:

```java
animal -> animal.getNome()    // lambda
Animal::getNome               // method reference — mesma coisa
```

```java
resposta -> mapper.toResponse(resposta)   // lambda
mapper::toResponse                        // method reference
```

Uso real:

```java
// src/main/java/br/com/fiap/clyvovet/service/AnimalService.java
return animalRepository.buscarPorFiltros(nome, especie, tutorId, pageable)
        .map(animalMapper::toResponse);
```

Leia como: *"pegue a página de `Animal` e converta **cada item** usando `toResponse`"*.

### As três interfaces funcionais que aparecem aqui

| Interface | Recebe | Devolve | Lê-se |
|---|---|---|---|
| `Supplier<T>` | nada | `T` | "fornecedor de T" |
| `Consumer<T>` | `T` | nada | "consumidor de T" |
| `Function<A,B>` | `A` | `B` | "função de A para B" |

```java
// src/main/java/br/com/fiap/clyvovet/mapper/AtualizacaoParcial.java
static <T> void aplicarSePresente(T valor, Consumer<T> destino) {
    if (valor != null) {
        destino.accept(valor);
    }
}
```

E quem chama passa **o setter como valor**:

```java
aplicarSePresente(patch.getData(), evento::setData);
//                                 ↑ "o método setData do evento", ainda não executado
```

💡 **Conceito: avaliação preguiçosa (*lazy*)**

Repare no `Supplier` deste trecho:

```java
// src/main/java/br/com/fiap/clyvovet/security/SegurancaService.java
private boolean podeAcessar(Supplier<Optional<UUID>> tutorDonoDoRecurso) {
    if (temVisaoAmpla()) {
        return true;                        // sai antes de chamar o Supplier
    }
    ...
    return tutorDonoDoRecurso.get()...      // só AQUI vai ao banco
}
```

O dono do recurso chega como **função**, não como valor pronto. Se o usuário é ADMIN, o
método retorna `true` na primeira linha e **a consulta ao banco nunca acontece**.

Se o parâmetro fosse `Optional<UUID>` (valor pronto), quem chama teria que consultar o banco
**antes**, mesmo quando o resultado seria descartado. Uma consulta desperdiçada por
requisição.

---

## 10. `record` — classe de dados em uma linha

```java
// src/main/java/br/com/fiap/clyvovet/dto/exception/ErroValidacao.java
public record ErroValidacao(String campo, String mensagem) {}
```

Essa linha equivale a umas 30 linhas de classe tradicional: dois atributos `private final`,
um construtor, dois getters, `equals`, `hashCode` e `toString`.

Diferenças em relação a uma classe comum:

| | classe | `record` |
|---|---|---|
| Campos | mutáveis se houver setter | **sempre imutáveis** |
| Getter | `getCampo()` | `campo()` — sem `get` |
| Construtor | você escreve | gerado |

Neste projeto, **todo DTO de resposta é `record`**. O motivo é semântico: uma resposta é um
dado de saída pronto — ninguém deveria alterá-la depois de montada.

---

## 11. Anotações — o conceito mais importante daqui

Uma **anotação** (`@Alguma`) é um bilhete colado no código. Ela **não faz nada sozinha**.

```java
@Entity
public class Animal { ... }
```

`@Entity` não cria tabela nenhuma. Ela apenas **marca** a classe. Alguém — no caso, o
Hibernate — vai ler essa marca depois e decidir o que fazer.

É a chave para entender Spring inteiro: **o framework lê as anotações e age**. Você declara
a intenção; ele executa.

```java
@Service              // "Spring, gerencie esta classe"
@Transactional        // "Spring, abra transação nos métodos daqui"
@GetMapping("/{id}")  // "Spring, chame este método num GET nessa URL"
@NotBlank             // "Validador, recuse se vier vazio"
@Column(name = "genero")  // "Hibernate, este campo é a coluna genero"
```

💡 **Conceito: programação declarativa**

Existem duas formas de dizer o que quer:

- **Imperativa** — você escreve o passo a passo: *"abra a transação, execute, se der erro
  desfaça, senão confirme"*.
- **Declarativa** — você declara o resultado desejado: `@Transactional`. Alguém sabe fazer.

Anotação é declarativa. O ganho é enorme em volume de código; o custo é que **o
comportamento fica invisível**. Quando algo não funciona, não há linha para depurar — é
preciso saber quem lê aquela anotação e sob quais condições ela age.

Guarde essa frase, porque ela explica quase todo bug estranho de Spring: *se a anotação não
funcionou, quase sempre é porque quem deveria lê-la não foi acionado.*

---

## 12. Lombok — as anotações que escrevem código

```java
// src/main/java/br/com/fiap/clyvovet/model/Animal.java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity
public class Animal {
    private String nome;
    private String raca;
}
```

O Lombok gera código **na hora de compilar**:

| Anotação | Gera |
|---|---|
| `@Getter` | `getNome()`, `getRaca()`… |
| `@Setter` | `setNome(...)`, `setRaca(...)`… |
| `@NoArgsConstructor` | `new Animal()` |
| `@AllArgsConstructor` | `new Animal(nome, raca, ...)` |
| `@RequiredArgsConstructor` | construtor com os campos `final` |
| `@Slf4j` | um objeto `log` para escrever no log |

Sem Lombok, `Animal` teria umas 80 linhas de getter e setter — código que ninguém lê e todo
mundo tem que manter.

⚠️ **Armadilha de ambiente:** o Lombok exige *annotation processing* habilitado na IDE. Sem
isso, a IDE acusa `cannot find symbol getNome()` **mesmo com o `mvn` compilando sem erro**.
Como habilitar está em
[`../docs/06-guia-de-desenvolvimento.md`](../docs/06-guia-de-desenvolvimento.md).

---

## 13. Pacotes e imports

```java
package br.com.fiap.clyvovet.service;      // onde ESTA classe mora

import br.com.fiap.clyvovet.model.Animal;  // classe de outro pacote que vou usar
```

O pacote é o endereço da classe, e espelha as pastas:

```
src/main/java/br/com/fiap/clyvovet/service/AnimalService.java
                └────────── pacote ─────────┘
```

Isso vai importar no próximo documento: o Spring só encontra suas classes se elas estiverem
**dentro** do pacote da aplicação (`br.com.fiap.clyvovet`) ou em subpacotes dele.

---

## 14. Exceções

Uma exceção interrompe a execução e "sobe" até alguém tratar.

```java
throw new RecursoNaoEncontradoException(Recurso.ANIMAL, id);
```

```java
try {
    Claims claims = jwtService.lerClaims(token);
} catch (JwtException e) {
    // token inválido: segue sem autenticar
}
```

| Tipo | Obriga tratar? | Exemplo |
|---|---|---|
| *checked* (`extends Exception`) | sim — `try/catch` ou `throws` | `IOException` |
| *unchecked* (`extends RuntimeException`) | não | `NullPointerException` |

As exceções deste projeto são todas **unchecked**:

```java
// src/main/java/br/com/fiap/clyvovet/exception/RegraDeNegocioException.java
public class RegraDeNegocioException extends RuntimeException {
```

O motivo aparece no documento [07](07-tratamento-de-excecoes.md): há um lugar único que
captura tudo, então obrigar `throws` em toda a cadeia só poluiria as assinaturas.

---

## Consolidação

Responda antes de seguir. As perguntas sobem de nível — se travar numa, volte à seção
correspondente.

**Entender**
1. O que uma anotação como `@Entity` faz **sozinha**?
2. Qual a diferença entre `Optional<Usuario>` e `Usuario` como retorno de um método?

**Aplicar**
3. Traduza `Animal::getNome` para uma lambda escrita por extenso.
4. Em `RepositorioBase<T>`, o que `T` vira quando `AnimalRepository` a estende?

**Analisar**
5. Por que `BigDecimal` para dinheiro e não `double`? Dê um exemplo do erro.
6. Por que `enum` para `TipoEvento` e `String` livre para `especie` gerou um problema real?

**Avaliar**
7. `SegurancaService.podeAcessar` recebe `Supplier<Optional<UUID>>` em vez de
   `Optional<UUID>`. Qual o ganho concreto? O que aconteceria se fosse o valor pronto?

---

## Se você levar só uma coisa daqui

**Anotação é um bilhete que alguém lê depois.** Todo o resto do Spring é descobrir *quem*
lê cada bilhete e *quando*.

---

**Próximo:** [01 — O que é Spring, afinal](01-spring-boot-e-injecao-de-dependencia.md)
