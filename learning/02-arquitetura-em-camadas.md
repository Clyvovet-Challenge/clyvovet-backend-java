# 02 — Arquitetura em camadas

## O que é

Separar a aplicação em camadas com **uma responsabilidade cada**, onde cada uma só conhece
a de baixo. O objetivo não é burocracia: é que uma mudança tenha **um lugar só** para
acontecer.

```
Controller ──▶ Service ──▶ Repository ──▶ Model (entidade JPA)
    │             │
    │             └──▶ Mapper (Entity ↔ DTO)
    │
    └──▶ @PreAuthorize → SegurancaService
```

Neste projeto os pacotes são organizados **por camada técnica**, não por funcionalidade:

```
br.com.fiap.clyvovet
├── controller/   7 @RestController
├── service/      8 @Service
├── repository/   RepositorioBase<T> + 7 interfaces
├── mapper/       8 @Component + 3 classes de apoio
├── model/        7 @Entity + 1 @Embeddable + 6 enums
├── dto/          27 DTOs, um subpacote por recurso
├── exception/    tradução de exceção → HTTP
├── security/     JWT, ownership, rate limit
└── config/       Cache, Security, Web, OpenAPI
```

## Quem faz o quê

| Camada | Faz | **Não** faz |
|---|---|---|
| Controller | rota, verbo, `@Valid`, montar `Pageable`, status HTTP, documentação | regra de negócio, `try/catch` |
| Service | regra, resolver FKs, transação, cache | conhecer HTTP, copiar campo a campo |
| Mapper | converter DTO ↔ entidade | acessar repositório |
| Repository | consulta ao banco | conhecer DTO |
| Model | mapeamento JPA | lógica |

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
    @Operation(summary = "Listar animais com paginação e filtros por nome e espécie")
    public ResponseEntity<Page<AnimalResponse>> listarTodos(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String especie,
            @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(animalService.listarTodos(nome, especie, pageable));
    }
```

Uma linha de corpo. Se um controller começa a ter `if`, a regra escapou para a camada errada.

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

Três coisas acontecem aqui, e nenhuma delas é do controller: resolver a FK (com 404 se o
tutor não existir), delegar a montagem ao mapper e invalidar o cache.

### Repository — a consulta

```java
// src/main/java/br/com/fiap/clyvovet/repository/AnimalRepository.java
public interface AnimalRepository extends RepositorioBase<Animal> {

    @Query("SELECT a FROM Animal a WHERE " +
            "(:nome IS NULL OR LOWER(a.nome) LIKE LOWER(CONCAT('%', :nome, '%')) ESCAPE '\\') AND " +
            "(:especie IS NULL OR LOWER(a.especie) LIKE LOWER(CONCAT('%', :especie, '%')) ESCAPE '\\') AND " +
            "(:tutorId IS NULL OR a.tutor.id = :tutorId)")
    Page<Animal> buscarPorFiltros(...);
```

Interface sem implementação — o Spring Data gera a classe em tempo de execução.

## O padrão que este projeto extraiu: `RepositorioBase`

Antes, **cada service** repetia a mesma linha:

```java
// ❌ repetido em ~20 lugares, cada um com sua mensagem
findById(id).orElseThrow(() -> new EntityNotFoundException("Animal não encontrado: " + id));
```

A extração:

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

E cada repositório fixa o próprio recurso:

```java
// src/main/java/br/com/fiap/clyvovet/repository/AnimalRepository.java
default Animal obterPorId(UUID id) {
    return obterPorId(id, Recurso.ANIMAL);
}
```

Assim o service volta a dizer só a intenção: `animalRepository.obterPorId(id)`.

Dois detalhes que valem entender:

- **`@NoRepositoryBean`** impede o Spring de tentar criar um bean da interface base. Sem
  isso, ele tentaria implementar `RepositorioBase` e falharia no boot.
- **`default`** existe porque o Spring Data deriva consultas do **nome** de métodos
  abstratos. Um método com corpo ele apenas respeita — é o que permite estender o
  repositório sem infraestrutura extra.

## DTO: por que não devolver a entidade

Um **DTO** (*Data Transfer Object*) é o objeto do contrato da API. A entidade é o objeto do
banco. Misturar os dois causa quatro problemas concretos:

| Problema | Exemplo neste projeto |
|---|---|
| Vaza dado que não devia sair | `Usuario.senha` é o hash BCrypt — nunca aparece em Response |
| Acopla contrato a schema | renomear uma coluna quebraria os clientes |
| Recursão na serialização | entidades com referências mútuas entram em laço |
| Cliente decide o que gravar | um POST poderia setar campos que a regra não permite |

Convenção daqui:

| Tipo | Forma | Por quê |
|---|---|---|
| `Request` | **classe** Lombok `@NoArgsConstructor @AllArgsConstructor @Getter` | Jackson precisa construir e preencher; sem `@Setter`, não é alterado depois de validado |
| `PatchRequest` | classe separada | mantém restrições de **formato**, abandona as de **presença** |
| `Response` | **`record`** | dado de saída imutável, com `equals`/`hashCode`/`toString` de graça |

```java
// src/main/java/br/com/fiap/clyvovet/dto/eventoClinico/EventoClinicoResponse.java
public record EventoClinicoResponse(
        UUID id, LocalDate data, String hora, String descricao, TipoEvento tipoEvento,
        UUID veterinarioId, String veterinarioNome,
        UUID animalId, String animalNome,
        UUID clinicaId, String clinicaNome
) {}
```

Repare em `veterinarioNome`, `animalNome`, `clinicaNome`: são campos **desnormalizados** da
entidade associada, para o cliente não precisar de uma segunda chamada só para descobrir um
nome. Tem um custo, e ele está documentado — item 9 de
[`../docs/07-pendencias-e-divergencias.md`](../docs/07-pendencias-e-divergencias.md): se o
tutor é renomeado, o cache de `animais` continua servindo o nome antigo por até 10 minutos.

## Mapper: conversão em um lugar só

Aqui os mappers são **escritos à mão** (`@Component`), não MapStruct — apesar do nome do
pacote. Quatro métodos por mapper:

| Método | Uso |
|---|---|
| `toEntity(request, ...)` | POST — monta entidade nova |
| `atualizar(entidade, request, ...)` | PUT — sobrescreve **todos** os campos |
| `aplicarPatch(entidade, patch, ...)` | PATCH — sobrescreve **só os presentes** |
| `toResponse(entidade)` | saída |

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

`aplicarSePresente` é um utilitário de três linhas:

```java
// src/main/java/br/com/fiap/clyvovet/mapper/AtualizacaoParcial.java
static <T> void aplicarSePresente(T valor, Consumer<T> destino) {
    if (valor != null) {
        destino.accept(valor);
    }
}
```

Vale pelo que evita: `if (x != null)` repetido dezenas de vezes é exatamente onde passa
despercebido o campo que ninguém lembrou de copiar.

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

O `record` acima existe para encurtar assinaturas que chegavam a cinco parâmetros — quatro
deles do mesmo tipo e fáceis de trocar de ordem, sem o compilador acusar nada.

## Armadilhas reais deste projeto

### 1. A duplicação estrutural é cobrada em nota

Os 7 controllers/services/mappers são estruturalmente idênticos. A rubrica da Sprint 3
desconta **−5 por ocorrência** de violação de DRY, cumulativo. As três classes de apoio
acima (`AtualizacaoParcial`, `Referencias`, `RelacionamentosDoEvento`) e o `RepositorioBase`
existem para atacar exatamente isso.

### 2. Mapper não pode buscar no banco

Repare que `aplicarPatch` recebe `RelacionamentosDoEvento` **já resolvido**. Quem foi ao
banco foi o service:

```java
// src/main/java/br/com/fiap/clyvovet/service/EventoClinicoService.java
private RelacionamentosDoEvento resolverRelacionamentos(EventoClinicoPatchRequest patch) {
    return new RelacionamentosDoEvento(
            patch.getVeterinarioId() == null ? null : veterinarioRepository.obterPorId(patch.getVeterinarioId()),
            patch.getAnimalId()      == null ? null : animalRepository.obterPorId(patch.getAnimalId()),
            patch.getClinicaId()     == null ? null : clinicaRepository.obterPorId(patch.getClinicaId()));
}
```

O ternário não é preciosismo: buscar mesmo assim custaria três consultas à toa e
transformaria um id **omitido** num 404 — mudando o significado do PATCH.

### 3. Divergência entre nome Java e coluna é normal aqui

`sexo`→`genero`, `observacao`→`observacoes`, `data`→`data_evento`,
`formaPagamento`→`metodo_pagamento`. O schema veio antes do código; `@Column` faz a ponte.
Não é bug — mas é preciso saber explicar por que existe.

## Perguntas de avaliação oral

1. Por que `AnimalController.listarTodos` tem uma linha só? Onde estaria a regra se houvesse
   uma?
2. O que `RepositorioBase` resolve? Por que os métodos são `default` e não abstratos?
3. Por que `@NoRepositoryBean` em `RepositorioBase`? O que acontece sem ela?
4. Por que `Response` é `record` e `Request` é classe? O que muda na prática?
5. Por que o `EventoClinicoMapper` recebe `RelacionamentosDoEvento` em vez de buscar as
   entidades ele mesmo?
6. No PATCH, por que um `tutorId` ausente **não** pode virar 404?

---

**Anterior:** [01 — Spring Boot e injeção de dependência](01-spring-boot-e-injecao-de-dependencia.md) ·
**Próximo:** [03 — API REST](03-api-rest.md)
