-- ============================================================
-- V9 — Prefixo t_clyvo_ em todas as tabelas
--
-- POR QUE ESTA MIGRATION EXISTE
-- O banco tinha DUAS convencoes de nome convivendo. As treze tabelas do
-- nucleo clinico, criadas da V1 a V7, usavam o nome puro da entidade
-- (animal, tutor, evento_clinico). As seis da API .NET, trazidas para o
-- Flyway na V8, ja nasceram com prefixo (t_clyvo_lembrete). Quem abre o
-- banco encontra as duas e nao tem como saber qual e a regra.
--
-- Pior do que feio: a convencao dividida esconde a informacao que importa.
-- Num schema compartilhado, o prefixo e o que separa as tabelas DESTE
-- projeto das de qualquer outro. Sem ele, "animal" e um nome que qualquer
-- um pode ter usado.
--
-- POR QUE RENAME, E NAO REESCREVER A V1
-- Trocar os nomes direto nas migrations antigas seria mais limpo de ler,
-- e quebraria todo banco onde elas ja rodaram: o Flyway guarda o checksum
-- de cada versao aplicada e recusa migrar quando ele muda. Renomear numa
-- versao nova e a unica forma que funciona no banco existente e no banco
-- criado do zero.
--
-- O RENAME LEVA JUNTO O QUE ESTA PENDURADO NA TABELA
-- No InnoDB, chaves primarias, estrangeiras, unique, check e indices
-- seguem a tabela automaticamente — inclusive as FKs declaradas em OUTRAS
-- tabelas que apontam para esta. As seis tabelas t_clyvo_* da V8
-- referenciam animal(id); depois deste rename elas referenciam
-- t_clyvo_animal(id) sem nenhum comando adicional.
--
-- CONSEQUENCIA FORA DESTE REPOSITORIO
-- A API .NET le animal e tutor por nome, em AnimalConfiguration.cs e
-- TutorConfiguration.cs. O ToTable dos dois precisa acompanhar esta
-- migration, ou a primeira consulta dela falha dizendo que a tabela nao
-- existe. O EF nao valida schema no boot, entao a API sobe normalmente e
-- so quebra na chamada.
--
-- OS NOMES DE CONSTRAINT NAO MUDAM
-- fk_animal_tutor continua fk_animal_tutor. Eles ja usam abreviacao
-- (fk_disp_veterinario, idx_evento_vet_data) e nunca carregaram o nome
-- completo da tabela, entao nao ha inconsistencia a corrigir. Prefixar
-- tambem os 87 identificadores estouraria o limite de 30 caracteres do
-- Oracle em varios deles — uk_autorizacao_animal_clinica ja tem 29 — e os
-- dois conjuntos de migration precisam continuar espelhados.
--
-- POR QUE disponibilidade_veterinario VIRA disponibilidade_vet
-- Esse limite de 30 caracteres e a razao. O MySQL aceitaria os 35 do nome
-- completo, mas o conjunto oracle nao, e um nome diferente em cada
-- dialeto e exatamente o tipo de divergencia que o MigrationsMySqlTest
-- existe para impedir. A abreviacao "vet" ja e a usada no resto do schema.
-- ============================================================

ALTER TABLE tutor                      RENAME TO t_clyvo_tutor;
ALTER TABLE clinica                    RENAME TO t_clyvo_clinica;
ALTER TABLE animal                     RENAME TO t_clyvo_animal;
ALTER TABLE veterinario                RENAME TO t_clyvo_veterinario;
ALTER TABLE evento_clinico             RENAME TO t_clyvo_evento_clinico;
ALTER TABLE pagamento                  RENAME TO t_clyvo_pagamento;
ALTER TABLE usuario                    RENAME TO t_clyvo_usuario;
ALTER TABLE servico                    RENAME TO t_clyvo_servico;
ALTER TABLE disponibilidade_veterinario RENAME TO t_clyvo_disponibilidade_vet;
ALTER TABLE bloqueio                   RENAME TO t_clyvo_bloqueio;
ALTER TABLE alerta_clinico             RENAME TO t_clyvo_alerta_clinico;
ALTER TABLE autorizacao_acesso         RENAME TO t_clyvo_autorizacao_acesso;
ALTER TABLE acesso_historico           RENAME TO t_clyvo_acesso_historico;
