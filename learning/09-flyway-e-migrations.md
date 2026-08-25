# 09 — Flyway e migrations

> **Pré-requisito:** [04 — JPA e Hibernate](04-jpa-e-hibernate.md), principalmente a seção
> sobre `ddl-auto`.
>
> Vale **20 pontos** da Sprint 3, e está entregue.

---

## O problema

Seu código está no Git: dá para ver quem mudou o quê, voltar atrás, saber em que versão cada
ambiente está.

**O banco não.** Sem controle, ele vira território de ninguém:

- alguém roda um `ALTER TABLE` na mão e esquece de avisar;
- o banco do dev tem uma coluna que o de produção não tem;
- ninguém sabe qual script já foi aplicado onde;
- o colega novo não consegue montar o banco local, porque "tem que rodar uns scripts, acho
  que nessa ordem".

E `ddl-auto=update` (do documento [04](04-jpa-e-hibernate.md)) parece resolver — o Hibernate
ajusta o schema sozinho. Só que ele **não remove** coluna, **não renomeia**, e **ninguém sabe
o que ele fez**. Você troca um problema conhecido por um invisível.

---

## A solução: schema como código

**Flyway** trata cada mudança de banco como um arquivo versionado:

- aplicado **uma vez**;
- na **ordem**;
- registrado numa tabela de controle (`flyway_schema_history`);
- **igual em todos os ambientes**.

O resultado prático: clonar o repositório e rodar a aplicação monta o banco do zero, na versão
certa, sem ninguém explicar nada.

### Convenção de nome

```
V<versão>__<descrição>.sql
   ↑        ↑
   │        └── DOIS underscores
   └── número, define a ordem
```

As deste projeto:

```
src/main/resources/db/migration/
├── oracle/
│   ├── V1__schema_inicial.sql          ← CREATE TABLE
│   ├── V2__seed_inicial.sql            ← INSERT dos dados iniciais
│   ├── V3__usuario_e_perfis.sql        ← a tabela usuario (Sprint 3)
│   └── V4__corrige_status_pagamento.sql
└── mysql/
    └── (as mesmas quatro, traduzidas)
```

No boot, o Flyway compara os arquivos com a tabela de histórico e aplica só o que falta.

---

## A regra de ouro: migration aplicada é imutável

O Flyway guarda um **checksum** (uma impressão digital) de cada arquivo. Se você editar um
`.sql` já aplicado, o próximo boot falha:

```
Migration checksum mismatch for migration version 1
```

💡 **Conceito: por que isso é proteção, e não chatice**

Imagine que a V1 já rodou no seu banco e no do colega. Você edita a V1 acrescentando uma
coluna.

- No **seu** banco: nada acontece — a V1 já consta como aplicada.
- Num banco **novo**: a V1 roda inteira, **com** a coluna nova.

Resultado: dois bancos diferentes, os dois dizendo "estou na versão 1". E ninguém consegue
descobrir isso olhando o código.

O checksum torna essa divergência **impossível de acontecer em silêncio**.

**Errou? Crie a próxima versão.** Foi exatamente o que aconteceu aqui.

---

## Estudo de caso: a migration V4

O defeito, registrado no comentário do arquivo: o enum Java dizia `REEMBOLSADO`, o CHECK do
banco dizia `ESTORNADO`. *"Na prática era impossível gravar um pagamento reembolsado: a
requisição passava na validação, chegava ao INSERT e estourava ORA-02290, devolvendo 500."*

Como a V1 já estava aplicada, a correção virou arquivo novo:

```sql
-- src/main/resources/db/migration/oracle/V4__corrige_status_pagamento.sql
UPDATE pagamento SET status_pagamento = 'REEMBOLSADO' WHERE status_pagamento = 'ESTORNADO';

ALTER TABLE pagamento DROP CONSTRAINT chk_status_pagamento;

ALTER TABLE pagamento ADD CONSTRAINT chk_status_pagamento
    CHECK (status_pagamento IN ('PENDENTE','PAGO','CANCELADO','REEMBOLSADO'));
```

⚠️ **Repare na ordem: `UPDATE` antes do `ALTER`.**

Se a constraint fosse trocada primeiro, o novo CHECK (que não aceita `ESTORNADO`) encontraria
linhas com `ESTORNADO` já gravadas — e o `ALTER TABLE` falharia.

**Migration é código que roda sobre dados que já existem.** A pergunta a fazer sempre é: *"e
se a tabela já tiver mil linhas?"*

---

## Flyway + `ddl-auto=validate`: a dupla

```properties
spring.flyway.enabled=true
spring.jpa.hibernate.ddl-auto=validate
```

| Peça | Papel |
|---|---|
| **Flyway** | **cria e altera** o schema |
| **Hibernate `validate`** | **confere** se as entidades batem, e derruba o boot se não |

Cada um faz uma coisa, e juntos fecham o ciclo:

```
1. você adiciona um campo na entidade
2. esquece a migration
3. o boot falha: "missing column [peso_kg] in table [animal]"
4. você escreve a migration
5. sobe
```

O erro aparece no `mvn spring-boot:run` — não em produção, às 3h da manhã. O comentário do
arquivo resume: *"validate garante que entidade e schema não voltem a divergir"*.

---

## Baseline: adotar Flyway num banco que já existe

```properties
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=2
```

O cenário real: o Oracle da FIAP já tinha sido montado à mão pelo `db-oracle.sql` **antes** de
o Flyway entrar no projeto. Se ele rodasse do zero, tentaria `CREATE TABLE tutor` numa tabela
que já existe — e falharia.

`baseline-version=2` diz: *"considere V1 (schema) e V2 (seed) como **já aplicadas**; comece a
valer da V3 em diante"*.

E o comentário registra a armadilha vizinha: *"com `baseline-version=1` o seed rodaria de novo
e duplicaria os dados"*.

---

## Dois conjuntos de migrations, um por banco

```properties
spring.flyway.locations=classpath:db/migration/oracle
```

O caminho é escrito **por extenso**, e isso é deliberado. O Flyway oferece o coringa
`classpath:db/migration/{vendor}`, que parece o óbvio — e o comentário explica por que não
serve:

> Ele resolve pelo banco da **conexão** — e os perfis `dev` e `h2` conectam num H2, então
> `{vendor}` viraria `"h2"` e o Flyway não acharia pasta nenhuma. Como o H2 roda com
> `MODE=Oracle`, quem serve a ele é a pasta `oracle/`, o que o coringa não tem como adivinhar.

As diferenças entre os dois conjuntos são quase todas mecânicas:

| Oracle | MySQL | Por quê |
|---|---|---|
| `VARCHAR2(n)` | `VARCHAR(n)` | MySQL não conhece `VARCHAR2` |
| `NUMBER(10,2)` | `DECIMAL(10,2)` | precisão exata; **nunca `DOUBLE`** para dinheiro |
| `NUMBER(1)` | `TINYINT` | booleano gravado como 0/1 |
| `NUMBER(3)` | `INT` | o `validate` compara o tipo JDBC: `int` espera `INTEGER` |
| `TIMESTAMP` | **`DATETIME`** | ver abaixo |

### O caso `TIMESTAMP` × `DATETIME` — o bug que não lançaria exceção

No MySQL:

| Tipo | Comportamento |
|---|---|
| `TIMESTAMP` | converte para UTC ao gravar e de volta ao ler; satura em 2038 |
| `DATETIME` | guarda o instante **literal** |

`LocalDateTime` em Java é um instante literal, sem fuso. Então `DATETIME` é o equivalente
correto.

O comentário da migration explica o efeito de errar:

> Com `TIMESTAMP`, um servidor de aplicação e um de banco em fusos diferentes fariam a conta
> do `estaBloqueado()` errar por horas — e o erro só apareceria como usuário destravando cedo
> demais, sem nenhuma exceção.

Pare um segundo nisso. O campo em questão é `bloqueado_ate`, do bloqueio de conta por
tentativas de login. O bug seria: **contas bloqueadas destravando antes da hora**. Sem
exceção, sem log, sem stack trace. Só uma proteção de segurança que não protege.

**Nem todo bug grita.** Os que mais custam costumam ser silenciosos.

---

## O que NÃO versionar numa migration

```sql
-- src/main/resources/db/migration/oracle/V3__usuario_e_perfis.sql
-- Nao ha usuario semeado aqui de proposito: hash de senha nao
-- deve ser versionado. Os usuarios de desenvolvimento sao criados
-- por DevDataSeeder, ativo apenas nos perfis dev e h2.
```

Migration roda em **todo** ambiente, inclusive o de entrega. Um usuário
`admin@clyvovet.com / admin12345` numa migration existiria **em produção**, com senha pública
no GitHub.

Por isso os usuários de desenvolvimento vêm de um bean com `@Profile({"dev", "h2"})` — que
simplesmente **não existe** nos outros perfis (ver
[01](01-spring-boot-e-injecao-de-dependencia.md)).

**Regra:** migration é para **estrutura** e para **dado de referência**. Credencial, nunca.

---

## Detalhe do seed: UUID fixo em vez de função

```sql
-- src/main/resources/db/migration/oracle/V2__seed_inicial.sql
INSERT INTO clinica (id, nome, ...) VALUES
('11111111-1111-1111-1111-000000000001', 'VetCare Prime', ...);
```

O script original usava uma função `fn_uuid()`. Os UUIDs viraram literais por três motivos, e
o terceiro é o que mais rende:

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

Com UUID aleatório, nenhum teste poderia dizer "o Bolinha é do Lucas" — teria que descobrir
os ids em tempo de execução, e a suíte ficaria bem mais frágil.

---

## Comandos úteis

| Comando | O que faz |
|---|---|
| `./mvnw flyway:info` | mostra o que foi aplicado e o que está pendente |
| `./mvnw flyway:validate` | confere os checksums |
| `./mvnw flyway:repair` | corrige a tabela de histórico após um erro |
| `SELECT * FROM flyway_schema_history` | o histórico, direto no banco |

---

## Armadilhas

### 1. Migration escrita para um banco só

Este projeto tem **dois conjuntos**. Esquecer o par MySQL não quebra o build nem os testes —
que rodam em H2 `MODE=Oracle` — **só quebra o deploy**.

Existe um teste dedicado a isso, `MigrationsMySqlTest`, que roda o conjunto `mysql/` num H2 em
`MODE=MySQL`. **Ele precisa ser estendido a cada migration nova**, ou a proteção morre em
silêncio.

### 2. H2 em `MODE=Oracle` **imita**, não é Oracle

Foi assim que o bug do `ESCAPE ''` (documento [03](03-api-rest.md)) ficou escondido: o H2
reproduz a semântica de "string vazia é nulo", então o defeito existia nos dois ambientes.

Mas nada garante que a imitação cubra tudo. Por isso existe `EscapeNoOracleTest`, que roda por
JDBC puro contra o Oracle real e fica **pulado** enquanto `DB_USERNAME` não estiver no
ambiente.

**Ambiente parecido não é ambiente igual.** É a razão de o item 19 (perfil `mysql` nunca
executado contra um MySQL real) continuar aberto no backlog.

### 3. Versão do MySQL importa

`DROP CONSTRAINT` existe a partir do MySQL 8.0.19. E `CHECK` só é **aplicado** a partir do
8.0.16 — abaixo disso o servidor **aceita a sintaxe em silêncio e não valida nada**.

O Azure Flexible Server é 8.0.21+, então passa. Mas repare no tipo de risco: um CHECK que
existe no arquivo, é aceito pelo banco e **não faz nada**. Você acharia que os dados estão
protegidos.

---

## Consolidação

**Entender**
1. O que o Flyway resolve que `ddl-auto=update` não resolve?
2. O que acontece se você editar uma migration já aplicada?

**Aplicar**
3. Você precisa adicionar a coluna `peso_kg` em `evento_clinico`. Quais arquivos cria, com
   quais nomes?
4. Sua migration adiciona uma coluna `NOT NULL` numa tabela com dados. O que precisa fazer
   antes?

**Analisar**
5. Na V4, por que o `UPDATE` vem antes do `ALTER TABLE`?
6. Por que `baseline-version=2` e não `1`? O que aconteceria com `1`?
7. Por que `spring.flyway.locations` não usa o coringa `{vendor}`?

**Avaliar**
8. Por que `bloqueado_ate` é `DATETIME` no MySQL e não `TIMESTAMP`? Qual seria o sintoma do
   erro, e por que ele é especialmente perigoso?
9. Por que não há usuário semeado na V3? Que princípio isso protege?
10. Você vai criar as migrations V5 a V8 dos módulos novos. Que cuidado deste documento é o
    mais fácil de esquecer, e como você se protegeria?

---

## Se você levar só uma coisa daqui

**Migration é código que roda sobre dados que já existem.** Antes de escrever qualquer
`ALTER`, pergunte: "e se a tabela já tiver mil linhas?"

---

**Anterior:** [08 — Cache](08-cache.md) ·
**Próximo:** [10 — Testes](10-testes.md)
