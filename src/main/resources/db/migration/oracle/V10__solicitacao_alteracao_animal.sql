-- ============================================================
-- V10 — Pedido de alteração do cadastro do animal (Oracle)
--
-- Par da migration de mesmo número em db/migration/mysql. O raciocínio completo
-- — por que a tabela existe, por que as colunas espelham um PATCH em vez de um
-- JSON, e por que o nome tem 29 caracteres — está lá, e não se repete aqui.
--
-- As diferenças de dialeto são as três de sempre neste projeto:
--   VARCHAR(n)  -> VARCHAR2(n)
--   DATETIME    -> TIMESTAMP
--   INT         -> NUMBER(1)  para a coluna booleana
--
-- E a ausência do ENGINE/CHARSET, que é sintaxe de MySQL.
-- ============================================================

CREATE TABLE t_clyvo_solicitacao_alteracao (
    id                VARCHAR2(36)  NOT NULL PRIMARY KEY,
    animal_id         VARCHAR2(36)  NOT NULL,
    veterinario_id    VARCHAR2(36)  NOT NULL,
    status            VARCHAR2(20)  NOT NULL,
    justificativa     VARCHAR2(500) NOT NULL,

    -- Os campos propostos. Nulo significa "não faz parte deste pedido".
    nome              VARCHAR2(100),
    raca              VARCHAR2(100),
    especie           VARCHAR2(100),
    porte             VARCHAR2(100),
    cor               VARCHAR2(100),
    genero            VARCHAR2(10),
    data_nascimento   DATE,
    microchip         VARCHAR2(15),
    castrado          NUMBER(1),
    observacoes       VARCHAR2(1000),

    criado_em         TIMESTAMP     NOT NULL,
    respondido_em     TIMESTAMP,
    respondido_por    VARCHAR2(36),
    motivo_recusa     VARCHAR2(500),

    CONSTRAINT fk_solicitacao_animal FOREIGN KEY (animal_id)      REFERENCES t_clyvo_animal(id),
    CONSTRAINT fk_solicitacao_vet    FOREIGN KEY (veterinario_id) REFERENCES t_clyvo_veterinario(id),
    CONSTRAINT chk_solicitacao_status CHECK (status IN ('PENDENTE','APROVADA','RECUSADA')),
    CONSTRAINT chk_solicitacao_castrado CHECK (castrado IS NULL OR castrado IN (0,1))
);

CREATE INDEX idx_solicitacao_animal_status ON t_clyvo_solicitacao_alteracao (animal_id, status);

CREATE INDEX idx_solicitacao_vet_status ON t_clyvo_solicitacao_alteracao (veterinario_id, status);
