-- ============================================================
-- V8 -- Separa "o pet apareceu?" de "o prontuario foi fechado?" (MySQL)
--
-- Espelho de db/migration/oracle/V8. A justificativa completa esta no
-- cabecalho de la -- aqui ficam so as diferencas de dialeto, que sao
-- duas:
--
--   1. ADD COLUMN, a forma idiomatica do MySQL.
--   2. O CAST do backfill: o Oracle exige CAST(data AS TIMESTAMP), o
--      MySQL promove DATE a DATETIME sozinho na atribuicao.
--
-- DATETIME e nao TIMESTAMP: o TIMESTAMP do MySQL converte para UTC na
-- gravacao e de volta na leitura, usando o fuso da SESSAO. Duas
-- instancias da aplicacao com fusos diferentes leriam valores
-- diferentes da mesma linha. Ja custou um item de divergencia neste
-- projeto -- ver o cabecalho da V3.
-- ============================================================

ALTER TABLE evento_clinico ADD COLUMN concluido_em DATETIME;

UPDATE evento_clinico
   SET concluido_em = data_evento
 WHERE status_evento = 'REALIZADO'
   AND concluido_em IS NULL;

ALTER TABLE evento_clinico ADD CONSTRAINT chk_evento_conclusao
    CHECK (concluido_em IS NULL OR status_evento = 'REALIZADO');
