# 09 — Flyway e migrations

> Vale **20 pontos** da Sprint 3, e está entregue. O que segue é o porquê de cada decisão.

## O problema que resolve

Sem controle de versão de schema, o banco vira território de ninguém:

- alguém roda um `ALTER TABLE` na mão e esquece de avisar;
- o banco de dev tem uma coluna que o de produção não tem;
- ninguém sabe qual script já foi aplicado onde;
- `ddl-auto=update` "resolve" alterando o schema sozinho — e ninguém sabe o que ele fez.

**Flyway** trata o schema como código: cada mudança é um arquivo versionado, aplicado uma
vez, na ordem, e registrado numa tabela de controle (`flyway_schema_history`).

## Convenção de nome

```
V<versão>__<descrição>.sql
```

Dois underscores. As deste projeto:

```
src/main/resources/db/migration/
├── oracle/
│   ├── V1__schema_inicial.sql
│   ├── V2__seed_inicial.sql
│   ├── V3__usuario_e_perfis.sql
│   └── V4__corrige_status_pagamento.sql
└── mysql/
    └── (as mesmas quatro, traduzidas)
```

## A regra de ouro: migration aplicada é imutável

O Flyway guarda um **checksum** de cada arquivo. Editar um `.sql` já aplicado faz o próximo
boot falhar com *"Migration checksum mismatch"*.

Isso é proteção, não chatice: se você pudesse editar a V1, o banco de quem já a aplicou
ficaria diferente do banco de quem aplicar depois — e os dois achariam que estão na mesma
versão.

**Errou? Crie a próxima versão.** Foi exatamente o que aconteceu aqui:

```sql
-- src/main/resources/db/migration/oracle/V4__corrige_status_pagamento.sql
UPDATE pagamento SET status_pagamento = 'REEMBOLSADO' WHERE status_pagamento = 'ESTORNADO';

ALTER TABLE pagamento DROP CONSTRAINT chk_status_pagamento;

ALTER TABLE pagamento ADD CONSTRAINT chk_status_pagamento
    CHECK (status_pagamento IN ('PENDENTE','PAGO','CANCELADO','REEMBOLSADO'));
```

O comentário do arquivo conta o defeito: o enum Java dizia `REEMBOLSADO`, o check dizia
`ESTORNADO`. *"Na prática era impossível gravar um pagamento reembolsado: a requisição passava
na validação, chegava ao INSERT e estourava ORA-02290, devolvendo 500."*

Repare na ordem: **UPDATE antes do ALTER**. Trocar a constraint primeiro faria o novo check
rejeitar as linhas que ainda estavam como `ESTORNADO`.

## Flyway + `ddl-auto=validate`

```properties
# src/main/resources/application-oracle.properties
spring.flyway.enabled=true
spring.jpa.hibernate.ddl-auto=validate
```

A dupla que faz o sistema funcionar:

| Peça | Papel |
|---|---|
| Flyway | **cria e altera** o schema |
| Hibernate `validate` | **confere** se as entidades batem com o schema, e falha se não |

Consequência que precisa estar clara: **campo novo na entidade sem migration derruba o boot**.
E isso é desejável — o erro aparece no `mvn spring-boot:run`, não em produção às 3h.

O comentário do arquivo diz: *"validate garante que entidade e schema não voltem a divergir.
Este é o alvo real de entrega, então a checagem estrita fica aqui."*

## Baseline: adotar Flyway num banco que já existe

```properties
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=2
```

O cenário: o Oracle da FIAP já tinha sido provisionado à mão pelo `db-oracle.sql` **antes** de
o Flyway entrar. Se o Flyway rodasse do zero, tentaria criar tabelas que já existem.

`baseline-version=2` marca V1 (schema) e V2 (seed) como **já aplicadas** e roda só da V3 em
diante.

E o comentário registra a armadilha: *"com `baseline-version=1` o seed rodaria de novo e
duplicaria os dados."*

## Dois conjuntos de migrations, um por banco

```properties
spring.flyway.locations=classpath:db/migration/oracle
```

O caminho é escrito **por extenso**, e isso é deliberado. O Flyway oferece o coringa
`classpath:db/migration/{vendor}`, que parece o óbvio — mas o comentário explica por que não
serve:

> Ele resolve pelo banco da **conexão** — e os perfis `dev` e `h2` conectam num H2, então
> `{vendor}` viraria `"h2"` e o Flyway não acharia pasta nenhuma. Como o H2 roda com
> `MODE=Oracle`, quem serve a ele é a pasta `oracle/`, o que o coringa não tem como adivinhar.

As diferenças entre os dois conjuntos são quase todas mecânicas:

| Oracle | MySQL | Por quê |
|---|---|---|
| `VARCHAR2(n)` | `VARCHAR(n)` | MySQL não conhece `VARCHAR2` |
| `NUMBER(10,2)` | `DECIMAL(10,2)` | precisão exata; **nunca `DOUBLE`** para dinheiro |
| `NUMBER(1)` | `TINYINT` | booleano via `NumericBooleanConverter` |
| `NUMBER(3)` | `INT` | o `validate` compara o tipo JDBC: `int` espera `INTEGER` |
| `TIMESTAMP` | **`DATETIME`** | **esta é a importante** — ver abaixo |

### O caso `TIMESTAMP` × `DATETIME`

No MySQL, `TIMESTAMP` converte o valor para UTC na escrita e de volta para o fuso da sessão
na leitura, e satura em 2038. `DATETIME` guarda o instante literal — que é a semântica de
`LocalDateTime`.

O comentário da migration explica o efeito: *"com `TIMESTAMP`, um servidor de aplicação e um
de banco em fusos diferentes fariam a conta do `estaBloqueado()` errar por horas — e o erro
só apareceria como usuário destravando cedo demais, sem nenhuma exceção."*

Bug sem exceção, sem log, sem stack trace. Só comportamento errado.

## O que não versionar numa migration

```sql
-- src/main/resources/db/migration/oracle/V3__usuario_e_perfis.sql
-- Nao ha usuario semeado aqui de proposito: hash de senha nao
-- deve ser versionado. Os usuarios de desenvolvimento sao criados
-- por DevDataSeeder, ativo apenas nos perfis dev e h2.
```

Migration roda em **todo** ambiente, inclusive o de entrega. Um usuário `admin/admin12345`
numa migration existiria em produção. Por isso os usuários de desenvolvimento vêm de um bean
com `@Profile({"dev", "h2"})`.

## Detalhe do seed: UUID fixo em vez de função

```sql
-- src/main/resources/db/migration/oracle/V2__seed_inicial.sql
INSERT INTO clinica (id, nome, ...) VALUES
('11111111-1111-1111-1111-000000000001', 'VetCare Prime', ...);
```

O script original usava uma função `fn_uuid`. Os UUIDs viraram literais por três motivos, e o
terceiro é o que mais rende:

1. o SQL fica portável entre Oracle e H2 (sem PL/SQL);
2. as FKs são resolvidas por literal, sem blocos `DECLARE`;
3. **os testes têm dados determinísticos para asserir.**

É o que permite isto:

```java
// src/test/java/br/com/fiap/clyvovet/support/SeedV2.java
public static final String TUTOR_LUCAS = "22222222-2222-2222-2222-000000000001";
/** Lucas e dono do Bolinha; Maria, da Mimi e do Rex. */
public static final String ANIMAL_BOLINHA_DO_LUCAS = "44444444-4444-4444-4444-000000000001";
```

## Comandos úteis

| Comando | O que faz |
|---|---|
| `./mvnw flyway:info` | mostra o que foi aplicado e o que está pendente |
| `./mvnw flyway:validate` | confere os checksums |
| `./mvnw flyway:repair` | corrige a tabela de histórico após um erro |
| `SELECT * FROM flyway_schema_history` | o histórico, direto no banco |

## Armadilhas

### 1. Migration escrita para um banco só

Este projeto tem **dois conjuntos**. Esquecer o par MySQL não quebra o build nem os testes —
que rodam em H2 `MODE=Oracle` — só quebra o **deploy**. Existe um teste dedicado a isso,
`MigrationsMySqlTest`, que roda o conjunto `mysql/` num H2 em `MODE=MySQL`. Ele precisa ser
estendido a cada migration nova.

### 2. H2 em `MODE=Oracle` **imita**, não é Oracle

Foi assim que o bug do `ESCAPE ''` ficou escondido: o H2 reproduz a semântica de "string vazia
é nulo", então o defeito existia nos dois — mas nada garante que a imitação cubra tudo. Por
isso existe `EscapeNoOracleTest`, que roda por JDBC puro contra o Oracle real e fica **pulado**
enquanto `DB_USERNAME` não estiver no ambiente.

### 3. `DROP CONSTRAINT` e `CHECK` dependem da versão do MySQL

`DROP CONSTRAINT` existe a partir do MySQL 8.0.19. E `CHECK` só é **aplicado** a partir do
8.0.16 — abaixo disso o servidor aceita a sintaxe **em silêncio** e não valida nada. O Azure
Flexible Server é 8.0.21+, então passa; mas é o tipo de coisa que se confirma antes, não
depois.

## Perguntas de avaliação oral

1. Por que não se pode editar uma migration já aplicada? O que o Flyway faz para impedir?
2. Por que a V4 existe, em vez de corrigir a V1?
3. Na V4, por que o `UPDATE` vem antes do `ALTER TABLE`?
4. Por que `ddl-auto=validate` e não `update`?
5. O que `baseline-version=2` faz? O que aconteceria com `baseline-version=1`?
6. Por que `spring.flyway.locations` não usa o coringa `{vendor}`?
7. Por que `bloqueado_ate` é `DATETIME` no MySQL e não `TIMESTAMP`?
8. Por que não há usuário semeado na migration V3?
9. Por que os UUIDs do seed são literais fixos?

---

**Anterior:** [08 — Cache](08-cache.md) ·
**Próximo:** [10 — Testes](10-testes.md)
