# Migrations

O schema é versionado pelo Flyway e existe em **dois conjuntos**, um por banco:

```
db/migration/
├── oracle/   ← Oracle FIAP, e também H2 (que roda com MODE=Oracle)
└── mysql/    ← Azure Database for MySQL, alvo do deploy
```

Cada pasta é uma linha do tempo completa e independente: V1 a V4 nas duas. Quem
abrir `mysql/` vê o schema inteiro, sem precisar cruzar com a outra pasta.

## Por que dois conjuntos

Até agosto de 2026 havia um conjunto só. Isso funcionava porque os dois bancos em
uso eram Oracle e H2, e o H2 tem `MODE=Oracle` — ele aceita `VARCHAR2`, `NUMBER` e a
semântica do Oracle sem reclamar.

Com a entrada do MySQL isso acabou: **MySQL não tem modo de compatibilidade com
Oracle**. Não existe um dialeto comum, e as opções eram duas:

- **um conjunto em SQL portável**, escrito no menor denominador comum;
- **um conjunto por banco**, que é o que está aqui.

A segunda foi escolhida por um motivo específico: o conjunto portável exigiria
editar a V3 e a V4, que já estão aplicadas no Oracle da FIAP e têm checksum
registrado no `flyway_schema_history`. Isso pediria um `flyway repair` num banco
compartilhado da faculdade, no meio de uma migração de infraestrutura. Trocar um
custo recorrente pequeno (escrever migration nova duas vezes) por um risco pontual
em banco alheio não compensava.

Se um dia o Oracle sair de cena, o conjunto `oracle/` pode ser apagado inteiro e o
problema desaparece.

## O custo disso, e o que segura o custo

O risco real de manter dois conjuntos é a **divergência silenciosa**: alguém cria a
V5 só em `mysql/`, ninguém percebe, e o erro aparece no deploy.

Quem guarda contra isso é o [`MigrationsMySqlTest`](../../../../test/java/br/com/fiap/clyvovet/migration/MigrationsMySqlTest.java).
Ele roda o conjunto `mysql/` do zero num H2 em `MODE=MySQL` e confere que as quatro
migrations aplicam e que o seed carrega as mesmas contagens. Se `mysql/` parar de
rodar, a build quebra.

**O que esse teste não prova:** H2 em `MODE=MySQL` não é MySQL. Ele valida sintaxe e
semântica principal, não o comportamento de tipos do servidor real. Verde ali
significa "o SQL está coerente", não "validado em produção" — para isso é preciso um
MySQL de verdade, via Testcontainers, quando houver Docker no ambiente de CI.

## As diferenças entre os dois conjuntos

São 205 linhas de SQL, e **três decisões**. O resto é renomeação mecânica.

| | `oracle/` | `mysql/` | por quê |
|---|---|---|---|
| texto | `VARCHAR2(n)` | `VARCHAR(n)` | MySQL não conhece `VARCHAR2`. 69 ocorrências, troca direta |
| dinheiro | `NUMBER(10,2)` | `DECIMAL(10,2)` | mesma precisão exata. Nunca `DOUBLE` para valor monetário |
| — | — | `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4` | são os defaults do MySQL 8, mas explícito evita MyISAM (ignora FK em silêncio) e charset sem acento |
| `usuario.ativo` | `NUMBER(1)` | `TINYINT` | o campo é `boolean` com `NumericBooleanConverter`: grava 0/1 num inteiro, não num BOOLEAN nativo |
| `usuario.tentativas_falhas` | `NUMBER(3)` | `INT` | o campo é `int` em Java, e o `validate` do Hibernate compara o tipo JDBC. `SMALLINT` caberia, mas economizar 2 bytes não paga uma falha de boot |
| `usuario.bloqueado_ate` | `TIMESTAMP` | `DATETIME` | **a importante** — ver abaixo |

### `TIMESTAMP` → `DATETIME`

No MySQL, `TIMESTAMP` converte o valor para UTC na escrita e de volta para o fuso da
sessão na leitura, e satura em 2038-01-19. `DATETIME` guarda o instante literal, que
é exatamente a semântica de `LocalDateTime`.

Com `TIMESTAMP`, um servidor de aplicação e um de banco em fusos diferentes fariam a
conta de `Usuario.estaBloqueado()` errar por horas. E o sintoma seria uma conta
bloqueada destravando cedo demais — sem exceção nenhuma no log.

### A V2 é idêntica nas duas pastas

O seed é só `INSERT` com literal ANSI (`DATE 'aaaa-mm-dd'`), que os dois bancos
aceitam. Ao alterá-lo, altere nos dois. Para conferir que não divergiram:

```bash
diff <(tail -n +12 oracle/V2__seed_inicial.sql) <(tail -n +17 mysql/V2__seed_inicial.sql)
```

## Onde cada perfil busca as migrations

O caminho é escrito por extenso em cada `application-<perfil>.properties`:

| perfil | `spring.flyway.locations` |
|---|---|
| `oracle` | `classpath:db/migration/oracle` |
| `dev` | `classpath:db/migration/oracle` (H2 com `MODE=Oracle`) |
| `h2` | `classpath:db/migration/oracle` (idem) |
| `mysql` | `classpath:db/migration/mysql` |

O Flyway oferece o coringa `classpath:db/migration/{vendor}`, que parece o óbvio
aqui — mas ele resolve pelo banco da **conexão**. Os perfis `dev` e `h2` conectam num
H2, então `{vendor}` viraria `h2` e o Flyway não acharia pasta nenhuma. Como esses
perfis rodam com `MODE=Oracle`, quem serve a eles é a pasta `oracle/`, e o coringa
não tem como adivinhar isso. Explícito não tem esse problema.

## Baseline: só no perfil `oracle`

O perfil `oracle` usa `baseline-on-migrate=true` com `baseline-version=2`, porque
aquele banco foi provisionado à mão pelo `db/db-oracle.sql` antes de o Flyway entrar
no projeto: V1 e V2 entram marcadas como aplicadas e a migração segue da V3.

O perfil `mysql` **não** tem baseline, de propósito. Um MySQL novo começa vazio, e a
V1 e a V2 precisam rodar de verdade. Ligar baseline ali pularia o schema e o seed, e
a aplicação subiria contra um banco sem tabela.

## Ao adicionar uma migration nova

1. Escreva em `oracle/` e em `mysql/`, com o mesmo número de versão.
2. Rode `./mvnw test -Dtest=MigrationsMySqlTest` — ele pega erro de sintaxe em `mysql/`.
3. Rode a suíte inteira — ela exercita `oracle/` via H2.
4. Se o SQL for portável, deixe os corpos idênticos e diga isso no cabeçalho, como a V2 faz.
