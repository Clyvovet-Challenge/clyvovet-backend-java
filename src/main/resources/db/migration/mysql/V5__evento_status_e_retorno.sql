-- ============================================================
-- V5 -- Status do atendimento, retorno e peso aferido (MySQL)
--
-- Espelho de db/migration/oracle/V5. A justificativa completa de cada
-- coluna, e a decisao sobre o DEFAULT 'REALIZADO', estao no cabecalho de
-- la -- aqui ficam so as diferencas de dialeto, que sao quatro:
--
--   1. ADD COLUMN, uma coluna por comando. O Oracle escreve
--      "ALTER TABLE t ADD col tipo"; o MySQL aceita a palavra COLUMN e e
--      a forma idiomatica.
--   2. A ordem de DEFAULT e NOT NULL se inverte: no Oracle e
--      "DEFAULT 'X' NOT NULL", no MySQL e "NOT NULL DEFAULT 'X'".
--   3. NUMBER(6,3) vira DECIMAL(6,3) -- mesma precisao, sem ponto
--      flutuante binario. FLOAT aqui seria erro: peso entra em conta de
--      dose por quilo na validacao de medicacao.
--   4. DATE e DATE nos dois. Nao usei DATETIME nem TIMESTAMP de
--      proposito: TIMESTAMP no MySQL sofre conversao de fuso e ja custou
--      um item de divergencia neste projeto (ver o cabecalho da V3).
--
-- CHECK constraint exige MySQL 8.0.16+. O Azure Database for MySQL
-- Flexible Server e 8.0.21+, entao os checks abaixo sao aplicados de
-- verdade, e nao ignorados em silencio como aconteceria no 5.7.
-- ============================================================

ALTER TABLE evento_clinico ADD COLUMN status_evento VARCHAR(20) NOT NULL DEFAULT 'REALIZADO';
ALTER TABLE evento_clinico ADD COLUMN data_retorno_previsto DATE;
ALTER TABLE evento_clinico ADD COLUMN evento_origem_id VARCHAR(36);
ALTER TABLE evento_clinico ADD COLUMN peso_kg DECIMAL(6,3);

ALTER TABLE evento_clinico ADD CONSTRAINT chk_evento_status
    CHECK (status_evento IN ('AGENDADO','REALIZADO','FALTOU','CANCELADO'));

-- Ver a nota do arquivo de oracle/: o IS NULL e explicito de proposito.
ALTER TABLE evento_clinico ADD CONSTRAINT chk_evento_peso
    CHECK (peso_kg IS NULL OR peso_kg > 0);

ALTER TABLE evento_clinico ADD CONSTRAINT chk_evento_origem_propria
    CHECK (evento_origem_id IS NULL OR evento_origem_id <> id);

-- O MySQL cria sozinho o indice que a FK precisa, entao nao ha
-- CREATE INDEX para evento_origem_id aqui nem no conjunto de oracle.
ALTER TABLE evento_clinico ADD CONSTRAINT fk_evento_origem
    FOREIGN KEY (evento_origem_id) REFERENCES evento_clinico(id);

CREATE INDEX idx_evento_vet_data    ON evento_clinico (veterinario_id, data_evento);
CREATE INDEX idx_evento_animal_data ON evento_clinico (animal_id, data_evento);
CREATE INDEX idx_evento_retorno     ON evento_clinico (data_retorno_previsto);
