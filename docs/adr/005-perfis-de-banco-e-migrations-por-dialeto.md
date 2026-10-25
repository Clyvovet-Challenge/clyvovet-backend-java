# 005 — Perfis de banco e migrations em dois conjuntos (Oracle e MySQL)

**Data:** 2026-10-10 (registra uma decisão anterior à Sprint 4) · **Status:** aceito

## Contexto

O projeto precisa rodar em três lugares diferentes: no **Oracle da FIAP** (banco de teste e de
entrega da faculdade), no **MySQL** que serve de alvo de deploy na nuvem e num **H2 em memória**
para desenvolver sem instalar nada. Até agosto de 2026 havia um conjunto só de migrations, porque
os dois bancos em uso eram Oracle e H2, e o H2 tem `MODE=Oracle`. Com a entrada do MySQL isso
acabou: o MySQL não tem modo de compatibilidade com Oracle.

## Decisão

- **Dois conjuntos de migrations**, `db/migration/oracle/` e `db/migration/mysql/`, cada um com a
  linha do tempo completa (V1 a V21). O perfil `dev` e o `h2` usam a pasta `oracle/`.
- **Um perfil Spring por banco**: `dev` (H2, recomendado para desenvolver), `h2` (dentro do
  docker-compose), `oracle` e `mysql` (alvo do deploy). O caminho das migrations é escrito por
  extenso em cada perfil, e não com o coringa `{vendor}` do Flyway, porque `dev` e `h2` conectam
  num H2 e `{vendor}` não acharia pasta nenhuma.
- **`ddl-auto=validate` nos perfis `oracle` e `mysql`**: o Hibernate confere o schema, quem o cria é o Flyway.
- **`oracle` usa `baseline-version=2`** para bancos que já foram provisionados com V1 e V2;
  **`mysql` não usa baseline**, porque um MySQL novo começa vazio e roda a V1.
- Nenhuma credencial é versionada: `DB_USERNAME`, `DB_PASSWORD` e `JWT_SECRET` vêm do ambiente.

## Consequências

- Escrever SQL portável não era viável: são poucas diferenças (três decisões em 205 linhas de SQL
  na época da divisão), mas não existe grafia comum que sirva aos dois bancos. O custo é manter
  duas pastas em dia.
- O `MigrationsMySqlTest` roda o conjunto `mysql/` do zero num H2 em `MODE=MySQL` e faz a build
  quebrar se uma migration existir só de um lado.
- **Limite honesto:** verde nesse teste quer dizer "o SQL está coerente", e não "validado no
  servidor". O perfil `mysql` foi validado à mão contra um MySQL 8 local com V1 a V20; a V21 só
  rodou em H2, e o Azure Database for MySQL ainda não foi exercitado.
- O banco da FIAP é compartilhado pela sala, então a suíte contra o perfil `oracle` grava de verdade.
- Detalhes: [`../04-configuracao.md`](../04-configuracao.md) e
  [`db/migration/README.md`](../../src/main/resources/db/migration/README.md).
