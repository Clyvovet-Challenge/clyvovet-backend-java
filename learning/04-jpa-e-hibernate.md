# 04 — JPA e Hibernate

## O que é

**JPA** (*Jakarta Persistence API*) é a especificação de mapeamento objeto-relacional em
Java. **Hibernate** é a implementação usada aqui. **Spring Data JPA** é a camada por cima
que gera repositórios a partir de interfaces.

O que o ORM resolve: você trabalha com objetos Java, ele traduz para SQL.

```java
Animal animal = animalRepository.obterPorId(id);   // vira SELECT
animal.setNome("Bolinha");
animalRepository.save(animal);                     // vira UPDATE
```

## Entidade

```java
// src/main/java/br/com/fiap/clyvovet/model/Animal.java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity
public class Animal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String nome;
    private String raca;
    private String especie;
    private String porte;
    private String cor;
    @Enumerated(EnumType.STRING)
    @Column(name = "genero")
    private SexoAnimal sexo;
    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;
    @Column(name = "observacoes")
    private String observacao;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tutor_id")
    private Tutor tutor;
}
```

| Anotação | Função |
|---|---|
| `@Entity` | classe mapeada para tabela (nome = classe, se não disser outro) |
| `@Id` | chave primária |
| `@GeneratedValue` | como o id é gerado |
| `@Column(name = ...)` | ponte quando o nome Java difere da coluna |
| `@Enumerated(EnumType.STRING)` | grava o **nome** do enum, não o ordinal |
| `@ManyToOne` + `@JoinColumn` | associação N:1 e a coluna FK |

`@NoArgsConstructor` não é decoração: o Hibernate **precisa** de construtor sem argumentos
para instanciar a entidade ao ler do banco.

### `EnumType.STRING` — nunca use `ORDINAL`

`ORDINAL` grava a posição do enum (0, 1, 2). Se alguém inserir um valor no meio da
declaração, **todos os registros do banco passam a significar outra coisa**, em silêncio.
`STRING` grava `"MACHO"` — imune a reordenação e legível direto no banco.

## Chave primária: UUID em texto

Todas as entidades usam a mesma estratégia:

```java
@Id
@GeneratedValue(strategy = GenerationType.UUID)
private UUID id;
```

O UUID é gerado **pela aplicação**, antes do INSERT. Vantagem: o id existe antes de ir ao
banco, então não é preciso um round-trip para descobri-lo — e ids não são sequenciais e
adivinháveis, como seriam com `IDENTITY`.

A coluna é `VARCHAR2(36)` no Oracle e `VARCHAR(36)` no MySQL — **texto, não binário**. Isso
depende de duas propriedades:

```properties
# src/main/resources/application-oracle.properties
spring.jpa.properties.hibernate.id.uuid_jdbc_type=CHAR
spring.jpa.properties.hibernate.type.preferred_uuid_jdbc_type=CHAR
```

Sem elas o Hibernate grava `RAW(16)`/`BINARY(16)` e os ids não casam com os do seed, que
foram escritos como texto.

## Relacionamentos

Todos aqui são **`@ManyToOne` unidirecionais**:

```
Animal ──N:1──▶ Tutor
Veterinario ──N:1──▶ Clinica
EventoClinico ──N:1──▶ Veterinario, Animal, Clinica
Pagamento ──N:1──▶ EventoClinico
```

**Unidirecional** = `Animal` conhece seu `Tutor`, mas `Tutor` **não** tem
`List<Animal> animais`. É uma escolha:

| | Ganha | Perde |
|---|---|---|
| Unidirecional | entidades leves, sem recursão na serialização | não dá para navegar do "lado um" |

Sem a coleção, buscar os animais de um tutor é uma consulta no repositório — o que é
adequado, porque uma coleção mapeada carregaria **todos** eles sem paginação.

### `EAGER` × `LAZY`

| | Quando busca a associação |
|---|---|
| `EAGER` | junto com a entidade principal |
| `LAZY` | só quando o campo é acessado |

Aqui é `EAGER` em todos — explícito em cinco casos, implícito em `Pagamento` (é o default de
`@ManyToOne`). Isso combina com:

```properties
# src/main/resources/application.properties
spring.jpa.open-in-view=false
```

Com `open-in-view=false`, a sessão do Hibernate **fecha ao sair da camada de serviço**. Se
um relacionamento fosse `LAZY` e o mapper tentasse acessá-lo depois, viria
`LazyInitializationException`. O comentário do arquivo diz exatamente isso: *"nenhum
relacionamento do projeto é LAZY, então nada depende disso"*.

O preço do `EAGER`: uma consulta que devolve 100 eventos traz veterinário, animal, tutor e
clínica de cada um. Para CRUD é aceitável; para **agregação analítica** não é — por isso a
spec dos módulos novos exige *projections* nas consultas de painel.

## `@Embeddable` — objeto sem tabela própria

```java
// src/main/java/br/com/fiap/clyvovet/model/Endereco.java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Embeddable
public class Endereco {
    @Column(name = "rua")
    private String logradouro;
    private String numero;
    private String bairro;
    private String cidade;
    private String estado;
    private String cep;
    private String complemento;
}
```

```java
// em Tutor, Clinica e Veterinario
@Embedded
private Endereco endereco;
```

As colunas ficam **achatadas** na tabela do dono — `tutor` tem `rua`, `numero`, `bairro`…
Ganha-se reúso de código sem criar tabela nem JOIN.

Detalhe que importa: quando **todas** as colunas do embeddable estão nulas, o Hibernate
devolve `null` no campo `endereco` — e não um objeto com campos nulos. Isso já causou NPE
nos mappers (item 10 de [`../docs/07`](../docs/07-pendencias-e-divergencias.md)), resolvido
com null-guard no `EnderecoMapper`.

## Consultas

### Derivadas do nome do método

```java
// src/main/java/br/com/fiap/clyvovet/repository/UsuarioRepository.java
Optional<Usuario> findByEmail(String email);
boolean existsByEmail(String email);
```

Sem corpo, sem `@Query`. O Spring Data lê o **nome** e gera o SQL.

### JPQL com `@Query`

```java
// src/main/java/br/com/fiap/clyvovet/repository/PagamentoRepository.java
@Query("SELECT p FROM Pagamento p WHERE " +
        "(:statusPagamento IS NULL OR p.statusPagamento = :statusPagamento) AND " +
        "(:formaPagamento IS NULL OR p.formaPagamento = :formaPagamento) AND " +
        "(:tutorId IS NULL OR p.eventoClinico.animal.tutor.id = :tutorId)")
Page<Pagamento> buscarPorFiltros(...);
```

**JPQL não é SQL.** Ele opera sobre **entidades e atributos Java**, não tabelas e colunas:
`p.eventoClinico.animal.tutor.id` navega quatro entidades e vira uma cadeia de JOINs.

Dois padrões desta linha valem guardar:

1. **`:param IS NULL OR ...`** — o filtro desaparece quando não informado. Uma query serve a
   todas as combinações, sem Criteria API.
2. **O recorte de segurança dentro da query.** `tutorId` não é filtro do cliente: é o
   isolamento por dono. Aplicá-lo **na** query, e não depois dela, é o que mantém a
   paginação correta — filtrar em memória deixaria a página 1 com 7 itens em vez de 10.

## `ddl-auto` — quem cria as tabelas

| Valor | Efeito |
|---|---|
| `create-drop` | recria tudo a cada boot — só dev |
| `update` | tenta ajustar o schema — **imprevisível** |
| **`validate`** | **não altera nada**; confere se entidade e schema batem, e falha se não |
| `none` | ignora |

```properties
spring.jpa.hibernate.ddl-auto=validate
```

`validate` é a escolha certa quando o schema é versionado por Flyway: o banco é responsabilidade
das migrations, e o Hibernate só **confere**. Consequência direta: **campo novo na entidade
sem migration correspondente derruba o boot** — e isso é bom, porque a alternativa é
descobrir em produção.

## Armadilhas reais deste projeto

### 1. Enum do Java × CHECK constraint do banco

```java
public enum StatusPagamento { PENDENTE, PAGO, CANCELADO, REEMBOLSADO }
```

```sql
CONSTRAINT chk_status_pagamento CHECK (status_pagamento IN ('PENDENTE','PAGO','CANCELADO','ESTORNADO'))
```

`REEMBOLSADO` passava na validação, chegava ao INSERT e estourava `ORA-02290` — **500** para
o cliente. O valor era literalmente impossível de gravar. Corrigido pela migration V4, que
alinhou o check ao enum.

Lição: `@Enumerated(STRING)` não valida nada sozinho. Enum e CHECK são **duas fontes de
verdade** que precisam ser mantidas em sincronia — e a única coisa que garante isso é o
`ddl-auto=validate` mais o cuidado de escrever a migration junto.

### 2. Apagar entidade com dependentes

`DELETE /tutores/{id}` de um tutor com animais estoura violação de FK. Hoje o
`GlobalExceptionHandler` traduz para **409**. Cascata automática seria pior: apagar um tutor
não deveria apagar o histórico clínico do pet.

### 3. Texto livre onde deveria haver enum

`especie` e `porte` são `String`. O banco restringe `porte` por CHECK, mas a aplicação aceita
`"grande"` minúsculo — que o Oracle rejeita. E `especie` não tem constraint nenhuma: o seed
grava `'CAO'`, o README exemplifica `"CACHORRO"`, os dois convivem, e o filtro `?especie=CAO`
não acha os registros gravados como `CACHORRO`.

Item 12 de [`../docs/07`](../docs/07-pendencias-e-divergencias.md). Estava classificado como
severidade **baixa** — e a spec do Painel do Veterinário mostra que, no momento em que a API
começa a **agregar por raça e espécie**, ele vira estrutural.

## Perguntas de avaliação oral

1. Por que `@Enumerated(EnumType.STRING)` e não `ORDINAL`?
2. Por que o UUID é gerado pela aplicação e não pelo banco?
3. Por que `Tutor` não tem `List<Animal>`? O que se ganha e o que se perde?
4. O que aconteceria se um `@ManyToOne` fosse `LAZY` neste projeto, dado
   `open-in-view=false`?
5. O que `ddl-auto=validate` faz? O que quebra se você adicionar um campo na entidade e
   esquecer a migration?
6. Em `p.eventoClinico.animal.tutor.id`, o que o Hibernate gera de SQL?
7. Por que o `tutorId` entra **dentro** da query em vez de a lista ser filtrada depois?

---

**Anterior:** [03 — API REST](03-api-rest.md) ·
**Próximo:** [05 — Bean Validation](05-bean-validation.md)
