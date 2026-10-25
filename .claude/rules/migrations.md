---
paths:
  - "src/main/resources/db/migration/**"
---

# Regras para migrations (Flyway)

Contexto completo em `src/main/resources/db/migration/README.md`. Leia antes da primeira mudança.

- **Nunca edite uma migration já existente.** Elas estão aplicadas no Oracle da FIAP e no MySQL
  da Azure, com checksum no `flyway_schema_history`. Editar uma delas quebra o boot. Para
  corrigir um erro, crie uma migration nova.
- **Toda migration nova sai em par:** mesma versão e mesmo nome em `oracle/` e em `mysql/`.
  `mysql/` é produção, `oracle/` é o banco de teste (e o H2 da suíte, em `MODE=Oracle`).
- Próxima versão: olhe a maior `V` nas duas pastas (`ls | sort -V`) e some 1. As duas pastas
  andam juntas.
- Traduções entre os dois bancos: `VARCHAR2` → `VARCHAR`, `NUMBER(10,2)` → `DECIMAL(10,2)`,
  `TIMESTAMP` → `DATETIME`, tabelas MySQL com `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`.
  Valor monetário nunca é `DOUBLE`. Os casos que não são troca direta estão no README.
- Depois de mudar o schema, rode `python scripts/gerar-script-bd.py` para regenerar
  `documentos/script_bd.sql`. Nunca edite esse arquivo à mão: o `ScriptDoBancoTest` reprova
  o script desatualizado.
- Quem prova que `mysql/` aplica é o `MigrationsMySqlTest` (H2 em `MODE=MySQL`). Verde ali
  quer dizer "SQL coerente", não "validado no MySQL real".
