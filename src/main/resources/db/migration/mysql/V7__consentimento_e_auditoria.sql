-- ============================================================
-- V7 -- Consentimento de acesso ao historico e auditoria (MySQL)
--
-- Espelho de db/migration/oracle/V7. Os tres niveis de acesso, a razao de
-- a autorizacao ser por clinica e nao por veterinario, a vigencia de 2
-- anos e a decisao de auditar por dia em vez de por leitura estao no
-- cabecalho de la. Aqui, so as diferencas de dialeto:
--
--   1. VARCHAR2 vira VARCHAR; NUMBER(1) vira TINYINT; NUMBER(6) vira INT.
--   2. A ordem de DEFAULT e NOT NULL se inverte.
--   3. SYSDATE vira CURRENT_DATE, entre parenteses: o MySQL exige
--      parenteses em DEFAULT de expressao (8.0.13+).
--   4. ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 nas duas tabelas.
--
-- UMA DIFERENCA QUE NAO E COSMETICA -- uk_acesso_dia
-- A chave inclui emergencial, e no Oracle e no MySQL um indice UNIQUE
-- ignora linhas com NULL. Aqui isso NAO se aplica: emergencial e NOT NULL
-- com default 0, entao a unicidade vale sempre. E deliberado -- um acesso
-- normal e uma quebra de vidro no mesmo dia, pelo mesmo veterinario, no
-- mesmo animal, sao dois fatos distintos e precisam de duas linhas.
-- ============================================================

CREATE TABLE autorizacao_acesso (
    id               VARCHAR(36) PRIMARY KEY,
    animal_id        VARCHAR(36) NOT NULL,
    clinica_id       VARCHAR(36) NOT NULL,
    status           VARCHAR(15) NOT NULL,
    concedida_em     DATE        NOT NULL DEFAULT (CURRENT_DATE),
    valido_ate       DATE        NOT NULL,
    revogada_em      DATE,
    origem_evento_id VARCHAR(36),
    CONSTRAINT fk_autorizacao_animal  FOREIGN KEY (animal_id)
        REFERENCES animal(id) ON DELETE CASCADE,
    CONSTRAINT fk_autorizacao_clinica FOREIGN KEY (clinica_id) REFERENCES clinica(id),
    -- ON DELETE SET NULL: origem_evento_id e RASTRO de onde o
    -- consentimento veio, nao dependencia dele. Sem isso, um evento
    -- nao poderia mais ser removido enquanto houvesse autorizacao
    -- apontando para ele -- e o consentimento morreria junto com o
    -- agendamento que o originou, o que e o oposto do desenho: ele
    -- sobrevive ao atendimento e vale por dois anos.
    CONSTRAINT fk_autorizacao_evento  FOREIGN KEY (origem_evento_id)
        REFERENCES evento_clinico(id) ON DELETE SET NULL,
    CONSTRAINT chk_autorizacao_status CHECK (status IN ('VIGENTE','REVOGADA','EXPIRADA')),
    CONSTRAINT chk_autorizacao_datas  CHECK (valido_ate >= concedida_em),
    CONSTRAINT chk_autorizacao_revogacao CHECK (
        (status = 'REVOGADA' AND revogada_em IS NOT NULL)
     OR (status <> 'REVOGADA' AND revogada_em IS NULL)),
    CONSTRAINT uk_autorizacao_animal_clinica UNIQUE (animal_id, clinica_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_autorizacao_animal ON autorizacao_acesso (animal_id, status);

CREATE TABLE acesso_historico (
    id          VARCHAR(36) PRIMARY KEY,
    animal_id   VARCHAR(36) NOT NULL,
    usuario_id  VARCHAR(36) NOT NULL,
    clinica_id  VARCHAR(36),
    dia         DATE        NOT NULL,
    nivel       TINYINT     NOT NULL,
    vezes       INT         NOT NULL DEFAULT 1,
    emergencial TINYINT     NOT NULL DEFAULT 0,
    motivo      VARCHAR(500),
    -- ON DELETE CASCADE: estas linhas nao tem vida propria sem o animal.
    -- Sem isso, DELETE /animais/{id} passaria a falhar em todo animal que
    -- ja tivesse alerta, autorizacao ou acesso registrado -- e o erro
    -- chegaria como 409 generico de integridade, sem dizer o que travou.
    CONSTRAINT fk_acesso_animal  FOREIGN KEY (animal_id)
        REFERENCES animal(id) ON DELETE CASCADE,
    CONSTRAINT fk_acesso_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_acesso_clinica FOREIGN KEY (clinica_id) REFERENCES clinica(id),
    CONSTRAINT chk_acesso_nivel  CHECK (nivel IN (1,2)),
    CONSTRAINT chk_acesso_emerg  CHECK (emergencial IN (0,1)),
    CONSTRAINT chk_acesso_motivo CHECK (emergencial = 0 OR motivo IS NOT NULL),
    CONSTRAINT uk_acesso_dia UNIQUE (animal_id, usuario_id, dia, emergencial)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_acesso_animal ON acesso_historico (animal_id, dia);
CREATE INDEX idx_acesso_usuario ON acesso_historico (usuario_id, dia);

-- Ver o cabecalho do conjunto oracle para a razao do DEFAULT 1.
ALTER TABLE animal ADD COLUMN resumo_seguranca_ativo TINYINT NOT NULL DEFAULT 1;
ALTER TABLE animal ADD CONSTRAINT chk_animal_resumo CHECK (resumo_seguranca_ativo IN (0,1));
