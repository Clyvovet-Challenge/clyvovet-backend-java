# 04 — JPA e Hibernate (o banco, sem escrever SQL)

> **Pré-requisito:** [00 — O Java que você precisa](00-java-essencial.md), seções sobre
> **interface**, **generics** e **anotações**.

---

## O mínimo de banco relacional

Um banco relacional guarda dados em **tabelas** — como planilhas, com regras.

```
tabela: animal
┌──────────────┬──────────┬───────────────────┬──────────────┐
│ id           │ nome     │ raca              │ tutor_id     │
├──────────────┼──────────┼───────────────────┼──────────────┤
│ 4444...0001  │ Bolinha  │ Golden Retriever  │ 2222...0001  │
│ 4444...0002  │ Mimi     │ Siames            │ 2222...0002  │
└──────────────┴──────────┴───────────────────┴──────────────┘
```

| Termo | O que é |
|---|---|
| **Coluna** | um campo (`nome`, `raca`) |
| **Linha** | um registro (um animal) |
| **PK** (chave primária) | a coluna que identifica a linha **unicamente** — aqui, `id` |
| **FK** (chave estrangeira) | coluna que **aponta** para a PK de outra tabela — `tutor_id` → `tutor.id` |
| **Constraint** | regra que o banco garante (`NOT NULL`, `UNIQUE`, `CHECK`) |

A FK é o que cria o relacionamento: o animal `Bolinha` pertence ao tutor `2222...0001`. E o
banco **recusa** um `tutor_id` que não exista — isso se chama integridade referencial.

E o SQL, a linguagem de consulta:

```sql
SELECT nome FROM animal WHERE raca = 'Golden Retriever';
INSERT INTO animal (id, nome, raca) VALUES ('444...', 'Bolinha', 'Golden Retriever');
UPDATE animal SET nome = 'Bolinha Silva' WHERE id = '444...';
DELETE FROM animal WHERE id = '444...';
```

---

## O problema: objeto e tabela não se parecem

Java tem **objetos**, com referências entre eles. O banco tem **tabelas**, com FKs. Traduzir
entre os dois na mão é assim:

```java
// ❌ como seria sem ORM — e isto é só um SELECT
String sql = "SELECT id, nome, raca, tutor_id FROM animal WHERE id = ?";
try (Connection conn = dataSource.getConnection();
     PreparedStatement stmt = conn.prepareStatement(sql)) {
    stmt.setString(1, id.toString());
    ResultSet rs = stmt.executeQuery();
    if (rs.next()) {
        Animal animal = new Animal();
        animal.setId(UUID.fromString(rs.getString("id")));
        animal.setNome(rs.getString("nome"));
        animal.setRaca(rs.getString("raca"));
        // e agora buscar o tutor, montar o objeto Tutor, setar...
    }
}
```

Vinte linhas para ler **uma** linha. Multiplique por 7 entidades × 5 operações.

**ORM** (*Object-Relational Mapping*) automatiza isso:

```java
Animal animal = animalRepository.obterPorId(id);   // pronto
```

| Nome | O que é |
|---|---|
| **JPA** | a **especificação** — o conjunto de anotações e regras |
| **Hibernate** | a **implementação** que faz o trabalho |
| **Spring Data JPA** | a camada que gera os repositórios automaticamente |

Analogia: JPA é a norma técnica da tomada; Hibernate é a tomada fabricada; Spring Data é o
adaptador que deixa tudo mais fácil de plugar.

---

## Mapeando uma entidade

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

Anotação por anotação:

| Anotação | Diz ao Hibernate |
|---|---|
| `@Entity` | "esta classe é uma tabela" (nome = nome da classe, se não disser outro) |
| `@Id` | "esta é a chave primária" |
| `@GeneratedValue` | "gere o valor assim" |
| `@Column(name = "genero")` | "o campo `sexo` é a coluna `genero`" |
| `@Enumerated(EnumType.STRING)` | "grave o **nome** do enum" |
| `@ManyToOne` | "muitos Animais para um Tutor" |
| `@JoinColumn(name = "tutor_id")` | "a FK é a coluna `tutor_id`" |

Campos **sem** `@Column` (como `nome`, `raca`) usam o nome do atributo. Só quando difere é
preciso dizer.

> ⚠️ `@NoArgsConstructor` não é decoração: o Hibernate **precisa** de um construtor sem
> argumentos para instanciar a entidade ao ler do banco.

---

## Duas decisões que valem entender

### `EnumType.STRING` — nunca use `ORDINAL`

```java
public enum SexoAnimal { MACHO, FEMEA, DESCONHECIDO }
```

| Estratégia | Grava no banco |
|---|---|
| `ORDINAL` | `0`, `1`, `2` — a **posição** |
| `STRING` | `'MACHO'`, `'FEMEA'` — o **nome** |

💡 **Conceito: por que `ORDINAL` é uma bomba-relógio**

Com `ORDINAL`, o banco guarda a posição. Se alguém inserir um valor **no meio** do enum:

```java
public enum SexoAnimal { MACHO, INDEFINIDO, FEMEA, DESCONHECIDO }
//                                ↑ novo
```

...todo registro que valia `1` (`FEMEA`) passa a significar `INDEFINIDO`. **Todos os dados
históricos mudam de sentido, em silêncio.** Nenhum erro, nenhum log.

Com `STRING`, `'FEMEA'` continua sendo `'FEMEA'` — e ainda dá para ler o banco direto e
entender.

O custo é alguns bytes por linha. É o melhor negócio da lista.

### UUID em texto, gerado pela aplicação

```java
@Id
@GeneratedValue(strategy = GenerationType.UUID)
private UUID id;
```

| Estratégia | Quem gera | Consequência |
|---|---|---|
| `IDENTITY` | o banco (auto-increment) | precisa ir ao banco para saber o id; ids sequenciais e adivinháveis |
| `UUID` | a **aplicação**, antes do INSERT | id existe antes de gravar; não é adivinhável |

"Adivinhável" importa: com id `1`, `2`, `3`, alguém tenta `/animais/2` e descobre quanto
você tem de base. Com UUID, não há sequência a percorrer.

A coluna é `VARCHAR2(36)` (Oracle) / `VARCHAR(36)` (MySQL) — **texto, não binário**. Isso
depende de duas propriedades:

```properties
spring.jpa.properties.hibernate.id.uuid_jdbc_type=CHAR
spring.jpa.properties.hibernate.type.preferred_uuid_jdbc_type=CHAR
```

Sem elas, o Hibernate grava `RAW(16)`/`BINARY(16)` e os ids **não casam** com os do seed, que
foram escritos como texto.

---

## Relacionamentos

Neste projeto, todos são `@ManyToOne`:

```
Animal ──N:1──▶ Tutor                    (muitos animais, um tutor)
Veterinario ──N:1──▶ Clinica
EventoClinico ──N:1──▶ Veterinario, Animal, Clinica
Pagamento ──N:1──▶ EventoClinico
```

O lado `@ManyToOne` é o que **tem a FK**. Faz sentido: a coluna `tutor_id` fica na tabela
`animal`, porque cada animal tem **um** tutor.

### Unidirecional — e por quê

`Animal` conhece seu `Tutor`. Mas `Tutor` **não** tem `List<Animal> animais`.

Se tivesse (`@OneToMany`), você poderia escrever `tutor.getAnimais()`. Parece conveniente, e
tem dois custos:

1. **Sem paginação.** `tutor.getAnimais()` carrega **todos** — 3 ou 3.000.
2. **Recursão na serialização.** `Tutor` tem `List<Animal>`, cada `Animal` tem `Tutor`, que
   tem `List<Animal>`… O Jackson entra em laço infinito.

Sem a coleção, buscar animais de um tutor é uma consulta no repositório — **que aceita
paginação e filtro**. É a escolha certa aqui.

### `EAGER` × `LAZY`

| | Quando busca a associação |
|---|---|
| `EAGER` | **junto** com a entidade principal |
| `LAZY` | só quando o campo é acessado |

Neste projeto tudo é `EAGER`. E isso combina com:

```properties
# src/main/resources/application.properties
spring.jpa.open-in-view=false
```

💡 **Conceito: `open-in-view` e o `LazyInitializationException`**

Uma associação `LAZY` só é carregada quando você a acessa — e para isso a **sessão** do
Hibernate precisa estar aberta.

`open-in-view=true` (o padrão do Boot) mantém a sessão aberta até a resposta ser serializada.
Parece bom, mas segura a conexão por mais tempo e permite que uma consulta dispare de dentro
da camada web, longe de onde deveria.

Este projeto desligou. Aí surge a regra: com a sessão fechada ao sair do service, acessar um
campo `LAZY` no mapper estouraria `LazyInitializationException`. Por isso o comentário do
arquivo diz: *"nenhum relacionamento do projeto é LAZY, então nada depende disso"*.

**As duas decisões se sustentam mutuamente** — mudar uma sem a outra quebra o sistema.

O preço do `EAGER`: uma consulta que traz 100 eventos traz também veterinário, animal, tutor
e clínica de cada um. Para CRUD é aceitável. Para **agregação analítica** não é — e é por
isso que a spec do Painel do Veterinário exige *projections* nas consultas de relatório.

---

## `@Embeddable` — objeto sem tabela própria

Tutor, Clínica e Veterinário têm endereço. Criar uma tabela `endereco` daria JOIN em toda
consulta. A alternativa:

```java
// src/main/java/br/com/fiap/clyvovet/model/Endereco.java
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
Você reusa o código sem criar tabela nem JOIN.

⚠️ **Detalhe que causou um bug real:** quando **todas** as colunas do embeddable estão nulas,
o Hibernate devolve `null` no campo `endereco` inteiro — não um objeto de campos vazios. Os
mappers não esperavam isso e estouravam NPE. Item 10 de
[`../docs/07-pendencias-e-divergencias.md`](../docs/07-pendencias-e-divergencias.md).

---

## Consultas

### Nível 1 — derivada do nome do método

```java
// src/main/java/br/com/fiap/clyvovet/repository/UsuarioRepository.java
Optional<Usuario> findByEmail(String email);
boolean existsByEmail(String email);
```

Sem corpo. Sem SQL. O Spring Data **lê o nome** e gera a consulta:

| Nome do método | Vira |
|---|---|
| `findByEmail` | `WHERE email = ?` |
| `existsByEmail` | `SELECT COUNT(*) > 0 ... WHERE email = ?` |
| `findByNomeAndEspecie` | `WHERE nome = ? AND especie = ?` |
| `findByNomeContainingIgnoreCase` | `WHERE LOWER(nome) LIKE LOWER('%?%')` |

⚠️ Se você digitar `findByEmial`, a aplicação **não sobe** — o Spring não acha o atributo
`emial` na entidade e falha no boot. Erro no lugar certo.

### Nível 2 — JPQL com `@Query`

Quando o nome ficaria absurdo, escreva a consulta:

```java
// src/main/java/br/com/fiap/clyvovet/repository/PagamentoRepository.java
@Query("SELECT p FROM Pagamento p WHERE " +
        "(:statusPagamento IS NULL OR p.statusPagamento = :statusPagamento) AND " +
        "(:formaPagamento IS NULL OR p.formaPagamento = :formaPagamento) AND " +
        "(:tutorId IS NULL OR p.eventoClinico.animal.tutor.id = :tutorId)")
Page<Pagamento> buscarPorFiltros(...);
```

**JPQL não é SQL.** Compare:

```sql
-- SQL: fala de TABELAS e COLUNAS
SELECT * FROM pagamento p
JOIN evento_clinico e ON p.evento_id = e.id
JOIN animal a ON e.animal_id = a.id
WHERE a.tutor_id = ?
```

```java
// JPQL: fala de ENTIDADES e ATRIBUTOS
"SELECT p FROM Pagamento p WHERE p.eventoClinico.animal.tutor.id = :tutorId"
```

`p.eventoClinico.animal.tutor.id` navega **quatro entidades** com um ponto — e o Hibernate
gera os JOINs. Você escreve na linguagem do seu modelo, não na do banco.

### Dois padrões desta query

**1. `:param IS NULL OR ...` — o filtro que desaparece**

```java
(:nome IS NULL OR LOWER(a.nome) LIKE ...)
```

Se `nome` vier `null`, a primeira metade é verdadeira e o `OR` já resolve — o filtro não
restringe nada. **Uma query serve a todas as combinações**, sem Criteria API nem
Specification.

**2. O recorte de segurança dentro da query**

```java
(:tutorId IS NULL OR a.tutor.id = :tutorId)
```

💡 **Conceito: filtrar no banco, não na memória**

`tutorId` não é filtro do cliente — é o isolamento por dono: um tutor só vê os próprios pets.

Por que **dentro** da query e não depois? Imagine 10 animais na página, 3 do tutor logado.
Filtrar depois entregaria uma "página de 10" com 3 itens, e `totalElements` mentiria.

Regra geral: **filtro que afeta a contagem tem que estar na query**. Vale para segurança,
vale para qualquer filtro em listagem paginada.

---

## `ddl-auto` — quem cria as tabelas

| Valor | O que faz | Quando |
|---|---|---|
| `create-drop` | apaga e recria tudo a cada boot | protótipo |
| `update` | tenta ajustar sozinho | **nunca em produção** — imprevisível, não remove coluna |
| **`validate`** | **não altera nada**; confere e falha se divergir | ✅ aqui |
| `none` | ignora | quando outra coisa cuida |

```properties
spring.jpa.hibernate.ddl-auto=validate
```

A divisão de trabalho:

| Peça | Papel |
|---|---|
| **Flyway** | **cria e altera** o schema (documento [09](09-flyway-e-migrations.md)) |
| **Hibernate `validate`** | **confere** se as entidades batem, e derruba o boot se não |

Consequência: **campo novo na entidade sem migration = aplicação não sobe**. E isso é bom —
o erro aparece no `mvn spring-boot:run`, não em produção.

---

## Armadilhas reais deste projeto

### 1. Enum do Java × CHECK do banco — duas fontes de verdade

```java
public enum StatusPagamento { PENDENTE, PAGO, CANCELADO, REEMBOLSADO }
```

```sql
CHECK (status_pagamento IN ('PENDENTE','PAGO','CANCELADO','ESTORNADO'))
```

Percebeu? O Java diz `REEMBOLSADO`, o banco diz `ESTORNADO`. O valor passava na validação,
chegava ao INSERT e estourava `ORA-02290` — **500** para o cliente. Era literalmente
impossível gravar um pagamento reembolsado.

**A lição:** `@Enumerated(STRING)` não valida nada sozinho. O enum e o CHECK são duas
declarações da mesma regra, em lugares diferentes, e nada as mantém sincronizadas
automaticamente. Corrigido pela migration V4.

### 2. Apagar entidade com dependentes

`DELETE /tutores/{id}` de um tutor com animais viola a FK. Hoje vira **409**.

E note a decisão de **não** usar cascata automática: apagar um tutor não deveria apagar o
histórico clínico do pet. Cascata é conveniente e perigosa.

### 3. Texto livre onde deveria haver enum

`especie` e `porte` são `String`. O banco restringe `porte` por CHECK, mas a aplicação aceita
`"grande"` minúsculo — que o Oracle rejeita. E `especie` não tem constraint nenhuma: o seed
grava `'CAO'`, o README exemplifica `"CACHORRO"`, os dois convivem, e `?especie=CAO` não acha
os segundos.

Item 12 de [`../docs/07`](../docs/07-pendencias-e-divergencias.md). Estava como severidade
**baixa** — e a spec do Painel do Veterinário mostra que, no momento em que a API passa a
**agregar por espécie e raça**, vira estrutural. Dívida técnica costuma envelhecer assim: sem
custo até a primeira vez que alguém precisa daquilo.

---

## Consolidação

**Entender**
1. O que é uma FK? Qual FK liga `animal` a `tutor`?
2. Qual a diferença entre JPA, Hibernate e Spring Data JPA?

**Aplicar**
3. Escreva a assinatura de um método derivado que busque veterinários por `crmv`.
4. Traduza para JPQL: "todos os eventos clínicos de animais de um tutor específico".

**Analisar**
5. Por que `EnumType.STRING` e não `ORDINAL`? Dê um cenário concreto de dado corrompido.
6. Por que `Tutor` **não** tem `List<Animal>`? Cite os dois custos que isso evita.
7. Por que `open-in-view=false` e `EAGER` são decisões que se sustentam mutuamente?

**Avaliar**
8. Você vai criar a entidade `Prescricao` com um campo novo. Quais passos precisa dar para a
   aplicação subir, dado `ddl-auto=validate`?
9. `especie` como `String` livre foi classificado como severidade "baixa" e virou problema
   estrutural. O que isso ensina sobre priorizar dívida técnica?

---

## Se você levar só uma coisa daqui

**Filtro que afeta a contagem tem que estar na query, não depois dela.** Vale para
segurança, paginação e qualquer listagem — filtrar em memória faz o `totalElements` mentir.

---

**Anterior:** [03 — API REST](03-api-rest.md) ·
**Próximo:** [05 — Bean Validation](05-bean-validation.md)
