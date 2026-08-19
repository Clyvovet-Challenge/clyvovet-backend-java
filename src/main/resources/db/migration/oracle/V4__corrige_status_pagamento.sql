-- ============================================================
-- V4 — Alinha o check de status_pagamento ao enum StatusPagamento
--
-- O schema original aceitava 'ESTORNADO', mas o enum Java declara
-- 'REEMBOLSADO'. Na pratica era impossivel gravar um pagamento
-- reembolsado: a requisicao passava na validacao, chegava ao INSERT
-- e estourava ORA-02290 (check constraint violated), devolvendo 500.
--
-- Esta migration roda tambem nos bancos que entraram por baseline,
-- que e onde a divergencia realmente existe.
-- ============================================================

UPDATE pagamento SET status_pagamento = 'REEMBOLSADO' WHERE status_pagamento = 'ESTORNADO';

ALTER TABLE pagamento DROP CONSTRAINT chk_status_pagamento;

ALTER TABLE pagamento ADD CONSTRAINT chk_status_pagamento
    CHECK (status_pagamento IN ('PENDENTE','PAGO','CANCELADO','REEMBOLSADO'));
