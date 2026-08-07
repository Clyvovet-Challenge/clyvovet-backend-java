# Modelo de dados

## Diagrama de entidades

```
                       ┌─────────────┐
                       │   Tutor     │
                       │─────────────│
                       │ id (UUID)   │
                       │ cpf         │
                       │ nome        │
                       │ dataNasc.   │
                       │ sexo  ──────┼──► Sexo
                       │ email       │
                       │ telefone    │
                       │ endereco ───┼──► «Endereco»
                       └──────┬──────┘
                              │ 1
                              │
                              │ N
                       ┌──────▼──────┐          ┌─────────────┐
                       │   Animal    │          │  Clinica    │
                       │─────────────│          │─────────────│
                       │ id (UUID)   │          │ id (UUID)   │
                       │ nome        │          │ nome        │
                       │ raca        │          │ cnpj        │
                       │ especie     │          │ telefone    │
                       │ porte       │          │ email       │
                       │ cor         │          │ endereco ───┼──► «Endereco»
                       │ sexo ───────┼──►Sexo   └──────┬──────┘
                       │ dataNasc.   │  Animal         │ 1
                       │ observacao  │                 │
                       │ tutor       │                 │ N
                       └──────┬──────┘          ┌──────▼──────────┐
                              │ N               │  Veterinario    │
                              │                 │─────────────────│
                              │                 │ id (UUID)       │
                              │            1    │ cpf · nome      │
                              │   ┌─────────────┤ dataNascimento  │
                              │   │             │ sexo ───────────┼──► Sexo
                              ▼   ▼             │ email·telefone  │
                       ┌──────────────────┐  N  │ especialidade   │
                       │  EventoClinico   │◄────┤ crmv            │
                       │──────────────────│     │ endereco ───────┼──► «Endereco»
                       │ id (UUID)        │     │ clinica         │
                       │ data             │     └─────────────────┘
                       │ hora (String)    │
                       │ descricao        │            ▲
                       │ tipoEvento ──────┼──► TipoEvento
                       │ veterinario      │            │ N
                       │ animal           ├────────────┘
                       │ clinica          │
                       └────────┬─────────┘
                                │ 1
                                │
                                │ N
                       ┌────────▼──────────┐
                       │    Pagamento      │
                       │───────────────────│
                       │ id (UUID)         │
                       │ formaPagamento ───┼──► FormaPagamento
                       │ valor (BigDecimal)│
                       │ dataPagamento     │
                       │ descricao         │
                       │ observacao        │
                       │ statusPagamento ──┼──► StatusPagamento
                       │ eventoClinico     │
                       └───────────────────┘
```

## Relacionamentos

| Origem | Destino | Cardinalidade | Anotação | Fetch | Coluna FK |
|---|---|---|---|---|---|
| Animal | Tutor | N:1 | `@ManyToOne` | EAGER | `tutor_id` |
| Veterinario | Clinica | N:1 | `@ManyToOne` | EAGER | `clinica_id` |
| EventoClinico | Veterinario | N:1 | `@ManyToOne` | EAGER | `veterinario_id` |
| EventoClinico | Animal | N:1 | `@ManyToOne` | EAGER | `animal_id` |
| EventoClinico | Clinica | N:1 | `@ManyToOne` | EAGER | `clinica_id` |
| Pagamento | EventoClinico | N:1 | `@ManyToOne` | EAGER (default) | `evento_id` |

Todos os relacionamentos são **unidirecionais**: `Animal` conhece seu `Tutor`, mas
`Tutor` não tem uma coleção de animais. Isso mantém as entidades leves e evita
recursão na serialização, ao custo de não existir consulta navegando do "lado um".

O `FetchType.EAGER` é explícito em cinco dos seis casos — em `Pagamento` ele fica
implícito, já que EAGER é o default de `@ManyToOne`.

---

## Chaves primárias

Todas as entidades usam a mesma estratégia:

```java
@Id
@GeneratedValue(strategy = GenerationType.UUID)
private UUID id;
```

O UUID é gerado pelo Hibernate na aplicação, antes do INSERT. No Oracle a coluna é
`VARCHAR2(36)`, e a conversão é garantida por duas propriedades no perfil:

```properties
spring.jpa.properties.hibernate.id.uuid_jdbc_type=CHAR
spring.jpa.properties.hibernate.type.preferred_uuid_jdbc_type=CHAR
```

Sem elas, o Hibernate tentaria gravar o UUID como `RAW(16)` e a leitura do seed
(que grava UUID em texto via `fn_uuid`) quebraria.

---

## Entidades

### Tutor

[`Tutor.java`](../src/main/java/br/com/fiap/clyvovet/model/Tutor.java) — dono do pet.
Raiz do cadastro: não depende de nenhuma outra entidade.

| Campo Java | Tipo | Coluna | Observação |
|---|---|---|---|
| `id` | `UUID` | `id` | PK |
| `cpf` | `String` | `cpf` | UNIQUE no banco |
| `nome` | `String` | `nome` | NOT NULL |
| `dataNascimento` | `LocalDate` | `data_nascimento` | |
| `sexo` | `Sexo` | `genero` | `@Enumerated(STRING)` |
| `email` | `String` | `email` | UNIQUE no banco |
| `telefone` | `String` | `telefone` | |
| `endereco` | `Endereco` | *(achatado)* | `@Embedded` |

### Animal

[`Animal.java`](../src/main/java/br/com/fiap/clyvovet/model/Animal.java) — o pet.

| Campo Java | Tipo | Coluna | Observação |
|---|---|---|---|
| `id` | `UUID` | `id` | PK |
| `nome` | `String` | `nome` | NOT NULL |
| `raca` | `String` | `raca` | texto livre |
| `especie` | `String` | `especie` | texto livre, **não é enum** |
| `porte` | `String` | `porte` | texto livre no Java; CHECK no banco |
| `cor` | `String` | `cor` | |
| `sexo` | `SexoAnimal` | `genero` | `@Enumerated(STRING)` |
| `dataNascimento` | `LocalDate` | `data_nascimento` | |
| `observacao` | `String` | `observacoes` | singular no Java, plural na coluna |
| `tutor` | `Tutor` | `tutor_id` | `@ManyToOne` EAGER |

`porte` aceita qualquer string na aplicação, mas o Oracle restringe a
`PEQUENO`/`MEDIO`/`GRANDE` via check constraint.

### Clinica

[`Clinica.java`](../src/main/java/br/com/fiap/clyvovet/model/Clinica.java) — clínica
parceira. Também é raiz do cadastro.

| Campo Java | Tipo | Coluna | Observação |
|---|---|---|---|
| `id` | `UUID` | `id` | PK |
| `nome` | `String` | `nome` | NOT NULL |
| `cnpj` | `String` | `cnpj` | UNIQUE no banco |
| `telefone` | `String` | `telefone` | |
| `email` | `String` | `email` | |
| `endereco` | `Endereco` | *(achatado)* | `@Embedded` |

### Veterinario

[`Veterinario.java`](../src/main/java/br/com/fiap/clyvovet/model/Veterinario.java) —
profissional vinculado a uma clínica.

| Campo Java | Tipo | Coluna | Observação |
|---|---|---|---|
| `id` | `UUID` | `id` | PK |
| `cpf` | `String` | `cpf` | UNIQUE no banco |
| `nome` | `String` | `nome` | NOT NULL |
| `dataNascimento` | `LocalDate` | `data_nascimento` | sem `@Column`; resolvido pela naming strategy padrão |
| `sexo` | `Sexo` | `genero` | `@Enumerated(STRING)` |
| `email` | `String` | `email` | |
| `telefone` | `String` | `telefone` | |
| `endereco` | `Endereco` | *(achatado)* | `@Embedded` |
| `especialidade` | `String` | `especialidade` | texto livre |
| `crmv` | `String` | `crmv` | UNIQUE no banco |
| `clinica` | `Clinica` | `clinica_id` | `@ManyToOne` EAGER |

### EventoClinico

[`EventoClinico.java`](../src/main/java/br/com/fiap/clyvovet/model/EventoClinico.java)
— núcleo do domínio. Liga pet, profissional e local em uma data.

| Campo Java | Tipo | Coluna | Observação |
|---|---|---|---|
| `id` | `UUID` | `id` | PK |
| `data` | `LocalDate` | `data_evento` | |
| `hora` | `String` | `hora_evento` | **String**, não `LocalTime`; formato `"HH:mm"` |
| `descricao` | `String` | `descricao` | |
| `tipoEvento` | `TipoEvento` | `tipo_evento` | `@Enumerated(STRING)` |
| `veterinario` | `Veterinario` | `veterinario_id` | `@ManyToOne` EAGER |
| `animal` | `Animal` | `animal_id` | `@ManyToOne` EAGER |
| `clinica` | `Clinica` | `clinica_id` | `@ManyToOne` EAGER |

`hora` como `String` casa com a coluna `VARCHAR2(5)` do banco e evita conversão de
fuso, mas abre mão de qualquer validação de formato.

### Pagamento

[`Pagamento.java`](../src/main/java/br/com/fiap/clyvovet/model/Pagamento.java) —
cobrança de um evento clínico.

| Campo Java | Tipo | Coluna | Observação |
|---|---|---|---|
| `id` | `UUID` | `id` | PK |
| `formaPagamento` | `FormaPagamento` | `metodo_pagamento` | `@Enumerated(STRING)` |
| `valor` | `BigDecimal` | `valor` | `NUMBER(10,2)`; CHECK > 0 |
| `dataPagamento` | `LocalDate` | `data_pagamento` | |
| `descricao` | `String` | `descricao` | |
| `observacao` | `String` | `notas` | |
| `statusPagamento` | `StatusPagamento` | `status_pagamento` | `@Enumerated(STRING)` |
| `eventoClinico` | `EventoClinico` | `evento_id` | `@ManyToOne` |

### Endereco (`@Embeddable`)

[`Endereco.java`](../src/main/java/br/com/fiap/clyvovet/model/Endereco.java) — não é
tabela. Seus campos são achatados dentro de `tutor`, `clinica` e `veterinario`.

| Campo Java | Coluna |
|---|---|
| `logradouro` | `rua` |
| `numero` | `numero` |
| `bairro` | `bairro` |
| `cidade` | `cidade` |
| `estado` | `estado` |
| `cep` | `cep` |
| `complemento` | `complemento` |

Reutilizar o embeddable é o que permite filtrar tutores e clínicas por cidade com a
mesma sintaxe JPQL (`t.endereco.cidade`).

---

## Enums

Todos persistidos como texto via `@Enumerated(EnumType.STRING)` — legível no banco e
imune a reordenação, ao contrário de `ORDINAL`.

| Enum | Valores | Usado em |
|---|---|---|
| [`Sexo`](../src/main/java/br/com/fiap/clyvovet/model/Sexo.java) | `MASCULINO` `FEMININO` `OUTRO` | Tutor, Veterinario |
| [`SexoAnimal`](../src/main/java/br/com/fiap/clyvovet/model/SexoAnimal.java) | `MACHO` `FEMEA` `DESCONHECIDO` | Animal |
| [`TipoEvento`](../src/main/java/br/com/fiap/clyvovet/model/TipoEvento.java) | `CONSULTA` `RETORNO` `VACINA` `EXAME` `CIRURGIA` `OUTRO` | EventoClinico |
| [`FormaPagamento`](../src/main/java/br/com/fiap/clyvovet/model/FormaPagamento.java) | `PIX` `CARTAO` `DINHEIRO` `BOLETO` | Pagamento |
| [`StatusPagamento`](../src/main/java/br/com/fiap/clyvovet/model/StatusPagamento.java) | `PENDENTE` `PAGO` `CANCELADO` `REEMBOLSADO` | Pagamento |

Existem **dois** enums de sexo porque o vocabulário difere: pessoas usam
masculino/feminino/outro, animais usam macho/fêmea/desconhecido. Ambos gravam na
coluna `genero` das suas respectivas tabelas.

> ⚠️ `StatusPagamento.REEMBOLSADO` não é aceito pelo check constraint do Oracle, que
> espera `ESTORNADO`. Ver [07-pendencias-e-divergencias.md](07-pendencias-e-divergencias.md).

---

## Convenção de nomes: Java vs. banco

O projeto mantém deliberadamente nomes de domínio no Java diferentes dos nomes de
coluna, para casar com o schema do "projeto completo" sem renomear as entidades:

| Entidade | Campo Java | Coluna | Motivo |
|---|---|---|---|
| Tutor, Veterinario | `sexo` | `genero` | vocabulário do schema legado |
| Animal | `sexo` | `genero` | idem |
| Animal | `observacao` | `observacoes` | plural no schema |
| Pagamento | `observacao` | `notas` | nome diferente no schema |
| Pagamento | `formaPagamento` | `metodo_pagamento` | idem |
| Pagamento | `eventoClinico` | `evento_id` | idem |
| EventoClinico | `data` | `data_evento` | `data` é palavra ambígua em SQL |
| EventoClinico | `hora` | `hora_evento` | idem |
| Endereco | `logradouro` | `rua` | vocabulário do schema legado |

Campos sem `@Column` seguem a naming strategy padrão do Spring Boot
(`CamelCaseToUnderscoresNamingStrategy`): `dataNascimento` → `data_nascimento`.

---

## Schema Oracle

O DDL completo está em
[`db-oracle.sql`](../src/main/resources/db/db-oracle.sql) (290 linhas), pensado para
rodar com **Run Script (F5)** no SQL Developer conectado ao Oracle da FIAP.

### Estrutura do script

| Passo | Conteúdo |
|---|---|
| 1 | `CREATE OR REPLACE FUNCTION fn_uuid` — gera UUID em texto a partir de `SYS_GUID()` |
| 2 | `CREATE TABLE` das 6 tabelas, com FKs, uniques e checks |
| 3 | Seed data em blocos `DECLARE/BEGIN/END` que resolvem FKs por chave natural |

A função `fn_uuid` existe **só para o seed** — as PKs não têm `DEFAULT`, porque quem
gera os UUIDs em produção é o Hibernate.

### Tabelas

| Tabela | PK | FKs | Uniques | Checks |
|---|---|---|---|---|
| `tutor` | `id VARCHAR2(36)` | — | `cpf`, `email` | `genero IN (MASCULINO, FEMININO, OUTRO)` |
| `clinica` | `id VARCHAR2(36)` | — | `cnpj` | — |
| `animal` | `id VARCHAR2(36)` | `tutor_id` | — | `porte IN (PEQUENO, MEDIO, GRANDE)`, `genero IN (MACHO, FEMEA, DESCONHECIDO)` |
| `veterinario` | `id VARCHAR2(36)` | `clinica_id` | `cpf`, `crmv` | `genero IN (MASCULINO, FEMININO, OUTRO)` |
| `evento_clinico` | `id VARCHAR2(36)` | `veterinario_id`, `animal_id`, `clinica_id` | — | `tipo_evento IN (CONSULTA, RETORNO, VACINA, EXAME, CIRURGIA, OUTRO)` |
| `pagamento` | `id VARCHAR2(36)` | `evento_id` | — | `metodo_pagamento IN (PIX, CARTAO, DINHEIRO, BOLETO)`, `status_pagamento IN (PENDENTE, PAGO, CANCELADO, ESTORNADO)`, `valor > 0` |

Note que várias constraints existem **só no banco**: unicidade de CPF, CNPJ, e-mail e
CRMV não é verificada pela aplicação. Uma tentativa de cadastrar CPF duplicado passa
pela validação, chega ao INSERT e estoura ORA-00001, que hoje vira 500.

### Seed data

| Tabela | Registros | Destaques |
|---|---|---|
| `clinica` | 4 | VetCare Prime, PetMed Centro, AnimalSaude SP, CliniPet Jardins |
| `tutor` | 2 | Lucas M. Santos, Maria Oliveira |
| `veterinario` | 7 | especialidades variadas, distribuídos entre as 4 clínicas |
| `animal` | 3 | Bolinha (CAO), Mimi (GATO), Rex (CAO) |
| `evento_clinico` | 11 | histórico de 2024 + 2 eventos futuros via `TRUNC(SYSDATE) + n` |
| `pagamento` | 8 | mistura de PAGO, PENDENTE e CANCELADO |

Os blocos PL/SQL resolvem as FKs consultando por chave natural (`WHERE cnpj = ...`,
`WHERE crmv = ...`, `WHERE nome = ...`), o que torna o script re-executável em ordem
sem depender de IDs fixos.

Dois detalhes do seed que a API não reproduz: pagamentos `PENDENTE` têm
`data_pagamento` **nula**, e `especie` usa `'CAO'`/`'GATO'`. Ambos estão registrados
em [07-pendencias-e-divergencias.md](07-pendencias-e-divergencias.md).

### Geração automática de schema

Só o perfil `oracle` usa o DDL manual:

| Perfil | `ddl-auto` | Origem do schema |
|---|---|---|
| `oracle` | `none` | `db-oracle.sql`, executado à mão |
| `h2` | `update` | gerado pelo Hibernate, preservado entre reinícios |
| `dev` | `create-drop` | gerado pelo Hibernate, descartado ao encerrar |

Nos perfis H2 não há seed: o banco sobe vazio.
