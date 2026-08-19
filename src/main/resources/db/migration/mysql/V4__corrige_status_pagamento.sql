-- ============================================================
-- V4 -- Alinha o check de status_pagamento ao enum StatusPagamento (MySQL)
--
-- O schema original aceitava 'ESTORNADO', mas o enum Java declara
-- 'REEMBOLSADO'. Na pratica era impossivel gravar um pagamento
-- reembolsado: a requisicao passava na validacao, chegava ao INSERT
-- e estourava violacao de check, devolvendo 500.
--
-- Este arquivo e igual ao de oracle/ no SQL. A diferenca esta so no
-- comentario, e vale registrar por que:
--
--   Escrevi DROP CHECK primeiro, que e a forma idiomatica do MySQL e
--   diz explicitamente o que se remove. Mas o MigrationsMySqlTest roda
--   estas migrations num H2 em MODE=MySQL, e o H2 nao entende DROP
--   CHECK -- o teste quebrava na V4.
--
--   DROP CONSTRAINT funciona nos tres (Oracle, MySQL 8.0.19+ e H2), e o
--   Azure Flexible Server e 8.0.21+. Trocar uma grafia que eu achava
--   mais legivel por uma cobertura de teste automatica e bom negocio:
--   sem o teste, a divergencia entre os dois conjuntos so apareceria no
--   deploy.
--
-- O UPDATE roda antes do check novo de proposito: se um registro
-- 'ESTORNADO' sobrasse, o ALTER falharia ao validar as linhas
-- existentes e a migration inteira ficaria pendente.
-- ============================================================

UPDATE pagamento SET status_pagamento = 'REEMBOLSADO' WHERE status_pagamento = 'ESTORNADO';

ALTER TABLE pagamento DROP CONSTRAINT chk_status_pagamento;

ALTER TABLE pagamento ADD CONSTRAINT chk_status_pagamento
    CHECK (status_pagamento IN ('PENDENTE','PAGO','CANCELADO','REEMBOLSADO'));
