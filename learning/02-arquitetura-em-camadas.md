# 02 — Arquitetura em camadas

> **Pré-requisito:** [01 — O que é Spring](01-spring-boot-e-injecao-de-dependencia.md).
> Se `record`, `Optional` ou `::` ainda incomodam, revise o [00](00-java-essencial.md).

---

## O problema: a classe que faz tudo

Imagine cadastrar um animal, tudo numa classe só. Seria mais ou menos assim:

```java
// ❌ tudo junto — não é assim que o projeto faz
@RestController
public class AnimalController {

    @PostMapping("/animais")
    public String criar(@RequestBody String json) {
        // 1. converter o JSON em objeto
        // 2. validar: nome não vazio, tutor informado...
        // 3. abrir conexão com o banco
        // 4. conferir se o tutor existe
        // 5. montar o INSERT
        // 6. executar, tratar erro de constraint
        // 7. fechar conexão
        // 8. montar o JSON de resposta
        // 9. decidir o status HTTP
    }
}
```

Funciona. E é insustentável, por quatro motivos:

| Problema | Efeito prático |
|---|---|
| Impossível de testar em partes | testar a validação exige subir servidor **e** banco |
| Toda mudança toca este arquivo | duas pessoas mexendo = conflito garantido |
| A regra fica presa ao HTTP | não dá para reusar num job agendado |
| Repetição | os outros 6 recursos terão as mesmas 9 etapas |

---

## A solução: cada camada com um motivo de mudar

```
Controller ──▶ Service ──▶ Repository ──▶ Model (entidade)
    │             │
    │             └──▶ Mapper (converte entidade ↔ DTO)
    │
    └──▶ @PreAuthorize → SegurancaService
```

Leia a seta como **"conhece"**. `Controller` conhece `Service`. `Service` conhece
`Repository`. **A volta não existe** — `Repository` não sabe que existe um `Controller`.

💡 **Conceito: separação por motivo de mudança**

A pergunta que define uma boa camada não é *"o que ela faz?"*, e sim **"o que faria esta
classe precisar mudar?"**.

- `AnimalController` muda quando a **API** muda (rota nova, status diferente).
- `AnimalService` muda quando a **regra de negócio** muda.
- `AnimalRepository` muda quando a **consulta** muda.

Três motivos diferentes, três arquivos. Se estivessem juntos, uma mudança de rota e uma
mudança de regra tocariam o mesmo arquivo — e você nunca teria certeza de que não quebrou
a outra coisa.

Isso é o **S** do SOLID (Responsabilidade Única), e a rubrica da disciplina desconta
−10 por violação evidente.

---

## Camada por camada, no código real

### Controller — fino de propósito

```java
// src/main/java/br/com/fiap/clyvovet/controller/AnimalController.java
@RestController
@RequestMapping("/animais")
@RequiredArgsConstructor
@Tag(name = "Animais", description = "Gerenciamento de animais")
public class AnimalController {

    private final AnimalService animalService;

    @GetMapping
    public ResponseEntity<Page<AnimalResponse>> listarTodos(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String especie,
            @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(animalService.listarTodos(nome, especie, pageable));
    }
}
```

Traduzindo anotação por anotação:

| Anotação | Diz ao Spring |
|---|---|
| `@RestController` | "esta classe atende HTTP e devolve JSON" |
| `@RequestMapping("/animais")` | "todas as rotas daqui começam com `/animais`" |
| `@GetMapping` | "chame este método num GET" |
| `@RequestParam(required = false)` | "leia da query string: `?nome=Rex`" |
| `@PathVariable` | "leia da URL: `/animais/{id}`" |
| `@RequestBody` | "converta o JSON do corpo neste objeto" |
| `@PageableDefault` | "se não vier `page`/`size`/`sort`, use estes" |

**O corpo tem uma linha.** Essa é a métrica: se um controller ganha um `if`, uma regra
vazou para a camada errada.

### Service — onde a decisão mora

```java
// src/main/java/br/com/fiap/clyvovet/service/AnimalService.java
@Transactional
@CacheEvict(value = "animais", allEntries = true)
public AnimalResponse criar(AnimalRequest request) {
    Animal animal = animalMapper.toEntity(request, tutorRepository.obterPorId(request.getTutorId()));
    return animalMapper.toResponse(animalRepository.save(animal));
}
```

Três decisões acontecem aqui, e nenhuma é do controller:

1. **resolver a FK** — `tutorRepository.obterPorId(...)` já lança 404 se o tutor não existir;
2. **delegar a montagem** ao mapper;
3. **invalidar o cache** (`@CacheEvict`).

Note a leitura quase em português: *"monte o animal a partir do request e do tutor; salve;
converta para resposta"*.

### Repository — a consulta

```java
// src/main/java/br/com/fiap/clyvovet/repository/AnimalRepository.java
public interface AnimalRepository extends RepositorioBase<Animal> {

    @Query("SELECT a FROM Animal a WHERE " +
            "(:nome IS NULL OR LOWER(a.nome) LIKE LOWER(CONCAT('%', :nome, '%')) ESCAPE '\\') AND " +
            "(:especie IS NULL OR LOWER(a.especie) LIKE LOWER(CONCAT('%', :especie, '%')) ESCAPE '\\') AND " +
            "(:tutorId IS NULL OR a.tutor.id = :tutorId)")
    Page<Animal> buscarPorFiltros(...);
}
```

**É uma interface sem implementação** (revisar [00, seção 5](00-java-essencial.md)). O Spring
Data escreve a classe em tempo de execução. Detalhes no [04](04-jpa-e-hibernate.md).

---

## O padrão que este projeto extraiu: `RepositorioBase`

Vale acompanhar o raciocínio, porque é exatamente o tipo de refatoração que a disciplina
cobra.

### O sintoma

Esta linha estava repetida em **cerca de vinte lugares**:

```java
// ❌ em cada service, com a mensagem escrita de novo
findById(id).orElseThrow(() -> new EntityNotFoundException("Animal não encontrado: " + id));
```

Pior que a repetição: cada cópia escrevia a mensagem do próprio jeito. Uns diziam "não
encontrado", outros "não encontrada".

### A extração

```java
// src/main/java/br/com/fiap/clyvovet/repository/RepositorioBase.java
@NoRepositoryBean
public interface RepositorioBase<T> extends JpaRepository<T, UUID> {

    default T obterPorId(UUID id, Recurso recurso) {
        return findById(id).orElseThrow(RecursoNaoEncontradoException.naoEncontrado(recurso, id));
    }

    default void garantirQueExiste(UUID id, Recurso recurso) {
        if (!existsById(id)) {
            throw new RecursoNaoEncontradoException(recurso, id);
        }
    }
}
```

Cada repositório fixa o próprio recurso:

```java
// src/main/java/br/com/fiap/clyvovet/repository/AnimalRepository.java
default Animal obterPorId(UUID id) {
    return obterPorId(id, Recurso.ANIMAL);
}
```

E o service volta a dizer só a intenção:

```java
Animal animal = animalRepository.obterPorId(id);   // "obtenha este animal"
```

Duas decisões técnicas dentro dessas dez linhas:

- **`@NoRepositoryBean`** — impede o Spring de tentar criar um bean da interface base. Sem
  ela, ele tentaria implementar `RepositorioBase` (que não corresponde a entidade nenhuma) e
  **falharia no boot**.
- **`default`** — o Spring Data gera implementação a partir do **nome** de métodos
  abstratos; um método com corpo ele apenas respeita. É o que permite estender o repositório
  sem escrever classe nenhuma.

---

## DTO — por que não devolver a entidade direto

Um **DTO** (*Data Transfer Object*) é o objeto que entra e sai pela API. A **entidade** é o
objeto que representa a linha do banco.

A pergunta natural é: *por que duas classes parecidas?*

| Problema de devolver a entidade | Exemplo concreto aqui |
|---|---|
| **Vaza dado que não devia sair** | `Usuario.senha` é o hash BCrypt — nunca aparece em nenhum Response |
| **Amarra o contrato ao banco** | renomear a coluna `observacoes` quebraria todos os clientes |
| **Recursão na serialização** | entidades que se referenciam entram em laço infinito |
| **Cliente escolhe o que gravar** | um POST poderia setar campos que a regra não permite |

O primeiro item sozinho já justifica: sem DTO, um `GET /usuarios` distribuiria hashes de
senha.

### A convenção deste projeto

| Tipo | Forma | Por quê |
|---|---|---|
| `Request` | **classe** Lombok `@NoArgsConstructor @AllArgsConstructor @Getter` | o Jackson precisa construir e preencher; sem `@Setter`, ninguém altera depois de validado |
| `PatchRequest` | classe separada | mantém restrições de **formato**, abandona as de **presença** |
| `Response` | **`record`** | dado de saída pronto — imutável por natureza |

```java
// src/main/java/br/com/fiap/clyvovet/dto/eventoClinico/EventoClinicoResponse.java
public record EventoClinicoResponse(
        UUID id, LocalDate data, String hora, String descricao, TipoEvento tipoEvento,
        UUID veterinarioId, String veterinarioNome,
        UUID animalId, String animalNome,
        UUID clinicaId, String clinicaNome
) {}
```

💡 **Conceito: desnormalização no DTO (e o preço dela)**

Repare em `veterinarioNome`, `animalNome`, `clinicaNome`. No banco, o evento guarda só os
**ids**. O nome vem junto na resposta para o cliente não precisar de três chamadas extras só
para montar uma tela.

Isso se chama **desnormalizar**: repetir um dado onde ele é lido, em vez de só onde ele
mora.

O preço aparece no cache: se o tutor é renomeado, o cache de `animais` continua servindo o
nome antigo por até 10 minutos, porque `@CacheEvict` é escopado à própria entidade. Está
registrado como item 9 de
[`../docs/07-pendencias-e-divergencias.md`](../docs/07-pendencias-e-divergencias.md).

**Toda otimização de leitura cobra na escrita.** Aqui o preço foi aceito conscientemente —
e documentado, que é o que separa uma decisão de um descuido.

---

## Mapper — a conversão num lugar só

O mapper traduz entre DTO e entidade. Aqui são **escritos à mão** (`@Component`), não
gerados por MapStruct — apesar do nome do pacote.

Quatro métodos por mapper:

| Método | Usado em | O que faz |
|---|---|---|
| `toEntity(request, ...)` | POST | monta entidade nova |
| `atualizar(entidade, request, ...)` | PUT | sobrescreve **todos** os campos |
| `aplicarPatch(entidade, patch, ...)` | PATCH | sobrescreve **só os presentes** |
| `toResponse(entidade)` | saída | converte para DTO |

```java
// src/main/java/br/com/fiap/clyvovet/mapper/EventoClinicoMapper.java
public void aplicarPatch(EventoClinico evento, EventoClinicoPatchRequest patch,
                         RelacionamentosDoEvento relacionamentos) {
    aplicarSePresente(patch.getData(), evento::setData);
    aplicarSePresente(patch.getHora(), evento::setHora);
    aplicarSePresente(patch.getDescricao(), evento::setDescricao);
    aplicarSePresente(patch.getTipoEvento(), evento::setTipoEvento);
    aplicarSePresente(relacionamentos.veterinario(), evento::setVeterinario);
    aplicarSePresente(relacionamentos.animal(), evento::setAnimal);
    aplicarSePresente(relacionamentos.clinica(), evento::setClinica);
}
```

E `aplicarSePresente` tem três linhas:

```java
// src/main/java/br/com/fiap/clyvovet/mapper/AtualizacaoParcial.java
static <T> void aplicarSePresente(T valor, Consumer<T> destino) {
    if (valor != null) {
        destino.accept(valor);
    }
}
```

Sem essa extração, o método acima seria:

```java
// ❌ 21 linhas em vez de 7
if (patch.getData() != null)      evento.setData(patch.getData());
if (patch.getHora() != null)      evento.setHora(patch.getHora());
if (patch.getDescricao() != null) evento.setDescricao(patch.getDescricao());
// ... e assim por diante
```

Não é só volume. Com `if` repetido, **o campo esquecido é invisível** — todas as linhas
parecem iguais. Na versão de uma linha por campo, uma linha faltando salta aos olhos.

> 💡 Se `evento::setData` ainda parece estranho, volte à
> [seção 9 do documento 00](00-java-essencial.md): é o método `setData` **passado como
> valor**, ainda não executado.

Duas outras extrações do mesmo espírito:

```java
// src/main/java/br/com/fiap/clyvovet/mapper/Referencias.java
static <O, V> V de(O origem, Function<O, V> extrator) {
    return origem == null ? null : extrator.apply(origem);
}
```

```java
// src/main/java/br/com/fiap/clyvovet/mapper/RelacionamentosDoEvento.java
public record RelacionamentosDoEvento(Veterinario veterinario, Animal animal, Clinica clinica) {}
```

O `record` acima resolve um risco real. Antes, o método recebia cinco parâmetros:

```java
// ❌ e se você trocar animal com clinica na chamada?
public void atualizar(EventoClinico evento, EventoClinicoRequest request,
                      Veterinario veterinario, Animal animal, Clinica clinica)
```

Como os três últimos são de tipos diferentes, o compilador ainda pegaria a troca — mas
agrupá-los deixa a assinatura legível e a intenção explícita.

---

## A regra que faz as camadas valerem: mapper não vai ao banco

Repare que `aplicarPatch` recebe `RelacionamentosDoEvento` **já resolvido**. Quem consultou
o banco foi o service:

```java
// src/main/java/br/com/fiap/clyvovet/service/EventoClinicoService.java
private RelacionamentosDoEvento resolverRelacionamentos(EventoClinicoPatchRequest patch) {
    return new RelacionamentosDoEvento(
            patch.getVeterinarioId() == null ? null : veterinarioRepository.obterPorId(patch.getVeterinarioId()),
            patch.getAnimalId()      == null ? null : animalRepository.obterPorId(patch.getAnimalId()),
            patch.getClinicaId()     == null ? null : clinicaRepository.obterPorId(patch.getClinicaId()));
}
```

O ternário não é preciosismo. Ele codifica uma regra de negócio: **num PATCH, id ausente
significa "não mexa"**.

Se buscasse mesmo assim:

- gastaria três consultas à toa;
- e, pior, `obterPorId(null)` transformaria um campo **omitido** num **404** — mudando o
  significado do PATCH.

---

## Armadilhas reais deste projeto

### 1. A duplicação estrutural custa nota

Os 7 controllers/services/mappers são estruturalmente idênticos. A rubrica desconta **−5 por
ocorrência** de violação de DRY, cumulativo. `RepositorioBase`, `AtualizacaoParcial`,
`Referencias` e `RelacionamentosDoEvento` existem para atacar exatamente isso — e é o tipo
de coisa que se aponta na avaliação oral.

### 2. Nome do campo Java ≠ nome da coluna

`sexo`→`genero`, `observacao`→`observacoes`, `data`→`data_evento`,
`formaPagamento`→`metodo_pagamento`. O schema veio antes do código, e `@Column` faz a ponte.

Não é bug — mas é preciso saber explicar por que existe, em vez de "achar que está errado".

---

## Consolidação

**Entender**
1. Por que `AnimalController.listarTodos` tem uma linha só? O que estaria errado se tivesse
   dez?
2. O que é um DTO, e qual o problema concreto de devolver a entidade `Usuario` direto?

**Aplicar**
3. Você vai criar o recurso "vacina". Quais arquivos precisa criar, em quais pacotes?
4. Escreva (em pseudocódigo) o que `AnimalService.deletar(id)` deveria fazer, seguindo o
   padrão da casa.

**Analisar**
5. `RepositorioBase.obterPorId` é `default`, não abstrato. O que aconteceria se fosse
   abstrato?
6. Por que `Response` é `record` e `Request` é classe? Cite um risco concreto que o `record`
   elimina.

**Avaliar**
7. Um colega quer que o `AnimalMapper` busque o tutor no banco, "para simplificar o service".
   Que argumentos você usaria? Existe algum caso em que ele teria razão?
8. Num PATCH sem `tutorId`, por que a resposta **não pode** ser 404? Que princípio isso
   protege?

---

## Se você levar só uma coisa daqui

**Cada camada tem um motivo de mudar.** Se você não consegue dizer qual é o motivo de mudar
de uma classe, provavelmente ela está fazendo mais de uma coisa.

---

**Anterior:** [01 — O que é Spring](01-spring-boot-e-injecao-de-dependencia.md) ·
**Próximo:** [03 — API REST](03-api-rest.md)
