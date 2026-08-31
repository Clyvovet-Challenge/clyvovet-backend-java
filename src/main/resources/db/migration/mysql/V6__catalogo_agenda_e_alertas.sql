-- ============================================================
-- V6 -- Catalogo de servicos, agenda do veterinario e alertas (MySQL)
--
-- Espelho de db/migration/oracle/V6. A justificativa de cada tabela, a
-- decisao sobre o microchip e o desenho do bloqueio estao no cabecalho de
-- la -- aqui ficam so as diferencas de dialeto, que sao cinco:
--
--   1. VARCHAR2 vira VARCHAR; NUMBER(10,2) vira DECIMAL(10,2);
--      NUMBER(4) vira INT; NUMBER(1) vira TINYINT -- as mesmas
--      correspondencias ja usadas da V1 a V5.
--   2. A ordem de DEFAULT e NOT NULL se inverte.
--   3. SYSDATE vira CURRENT_DATE.
--   4. ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 em toda tabela nova, como
--      nas anteriores. InnoDB e o que sustenta a chave estrangeira;
--      utf8mb4 e o que aceita acento em descricao de alerta clinico.
--   5. ADD COLUMN, uma coluna por comando.
--
-- UMA DIFERENCA QUE NAO E COSMETICA -- o indice unico do microchip.
-- Oracle e MySQL concordam aqui, e vale registrar porque a intuicao diz o
-- contrario: nos dois, um indice UNIQUE ignora as linhas com NULL. Varios
-- animais sem chip convivem sem violar a restricao; dois com o mesmo chip
-- nao. E exatamente o comportamento desejado, e nao precisa de indice
-- parcial nem de valor sentinela.
--
-- CHECK constraint exige MySQL 8.0.16+. O Azure Database for MySQL
-- Flexible Server entrega 8.0.21+, entao os checks abaixo sao aplicados
-- de fato -- e nao silenciosamente ignorados, como aconteceria no 5.7.
-- ============================================================

-- ---------- Catalogo ----------

CREATE TABLE servico (
    id               VARCHAR(36)   PRIMARY KEY,
    clinica_id       VARCHAR(36)   NOT NULL,
    nome             VARCHAR(100)  NOT NULL,
    tipo_evento      VARCHAR(20)   NOT NULL,
    preco            DECIMAL(10,2) NOT NULL,
    duracao_minutos  INT           NOT NULL,
    ativo            TINYINT       NOT NULL DEFAULT 1,
    CONSTRAINT fk_servico_clinica  FOREIGN KEY (clinica_id) REFERENCES clinica(id),
    CONSTRAINT chk_servico_tipo    CHECK (tipo_evento IN ('CONSULTA','RETORNO','VACINA','EXAME','CIRURGIA','OUTRO')),
    CONSTRAINT chk_servico_preco   CHECK (preco >= 0),
    CONSTRAINT chk_servico_duracao CHECK (duracao_minutos BETWEEN 5 AND 480),
    CONSTRAINT chk_servico_ativo   CHECK (ativo IN (0,1)),
    CONSTRAINT uk_servico_clinica_nome UNIQUE (clinica_id, nome)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_servico_clinica ON servico (clinica_id, ativo);

-- ---------- Agenda ----------

CREATE TABLE disponibilidade_veterinario (
    id               VARCHAR(36) PRIMARY KEY,
    veterinario_id   VARCHAR(36) NOT NULL,
    dia_semana       VARCHAR(10) NOT NULL,
    hora_inicio      VARCHAR(5)  NOT NULL,
    hora_fim         VARCHAR(5)  NOT NULL,
    vigencia_inicio  DATE        NOT NULL,
    vigencia_fim     DATE,
    CONSTRAINT fk_disp_veterinario FOREIGN KEY (veterinario_id) REFERENCES veterinario(id),
    CONSTRAINT chk_disp_dia        CHECK (dia_semana IN
        ('SEGUNDA','TERCA','QUARTA','QUINTA','SEXTA','SABADO','DOMINGO')),
    CONSTRAINT chk_disp_horas      CHECK (hora_fim > hora_inicio),
    CONSTRAINT chk_disp_vigencia   CHECK (vigencia_fim IS NULL OR vigencia_fim >= vigencia_inicio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_disp_vet_dia ON disponibilidade_veterinario (veterinario_id, dia_semana);

CREATE TABLE bloqueio (
    id              VARCHAR(36)  PRIMARY KEY,
    veterinario_id  VARCHAR(36)  NOT NULL,
    data_inicio     DATE         NOT NULL,
    data_fim        DATE         NOT NULL,
    hora_inicio     VARCHAR(5),
    hora_fim        VARCHAR(5),
    motivo          VARCHAR(200) NOT NULL,
    CONSTRAINT fk_bloqueio_veterinario FOREIGN KEY (veterinario_id) REFERENCES veterinario(id),
    CONSTRAINT chk_bloqueio_datas CHECK (data_fim >= data_inicio),
    CONSTRAINT chk_bloqueio_horas CHECK (
        (hora_inicio IS NULL AND hora_fim IS NULL)
     OR (hora_inicio IS NOT NULL AND hora_fim IS NOT NULL AND hora_fim > hora_inicio))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_bloqueio_vet_data ON bloqueio (veterinario_id, data_inicio, data_fim);

-- ---------- Nivel 1 do fluxo C ----------

CREATE TABLE alerta_clinico (
    id            VARCHAR(36)  PRIMARY KEY,
    animal_id     VARCHAR(36)  NOT NULL,
    tipo          VARCHAR(20)  NOT NULL,
    descricao     VARCHAR(500) NOT NULL,
    origem        VARCHAR(15)  NOT NULL,
    registrado_em DATE         NOT NULL DEFAULT (CURRENT_DATE),
    ativo         TINYINT      NOT NULL DEFAULT 1,
    -- ON DELETE CASCADE: estas linhas nao tem vida propria sem o animal.
    -- Sem isso, DELETE /animais/{id} passaria a falhar em todo animal que
    -- ja tivesse alerta, autorizacao ou acesso registrado -- e o erro
    -- chegaria como 409 generico de integridade, sem dizer o que travou.
    CONSTRAINT fk_alerta_animal FOREIGN KEY (animal_id)
        REFERENCES animal(id) ON DELETE CASCADE,
    CONSTRAINT chk_alerta_tipo  CHECK (tipo IN
        ('ALERGIA','CONDICAO_CRONICA','MEDICACAO_CONTINUA','CRITICO')),
    CONSTRAINT chk_alerta_origem CHECK (origem IN ('TUTOR','VETERINARIO')),
    CONSTRAINT chk_alerta_ativo  CHECK (ativo IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_alerta_animal ON alerta_clinico (animal_id, ativo);

-- ---------- Colunas novas ----------

ALTER TABLE animal ADD COLUMN microchip VARCHAR(15);
ALTER TABLE animal ADD COLUMN castrado TINYINT;

ALTER TABLE animal ADD CONSTRAINT uk_animal_microchip UNIQUE (microchip);
ALTER TABLE animal ADD CONSTRAINT chk_animal_castrado CHECK (castrado IS NULL OR castrado IN (0,1));

ALTER TABLE evento_clinico ADD COLUMN servico_id VARCHAR(36);
ALTER TABLE evento_clinico ADD COLUMN desfecho VARCHAR(20);
ALTER TABLE evento_clinico ADD COLUMN motivo_cancelamento VARCHAR(500);

ALTER TABLE evento_clinico ADD CONSTRAINT fk_evento_servico
    FOREIGN KEY (servico_id) REFERENCES servico(id);

ALTER TABLE evento_clinico ADD CONSTRAINT chk_evento_desfecho
    CHECK (desfecho IS NULL OR desfecho IN ('MELHORA','ESTAVEL','PIORA','OBITO','INDEFINIDO'));
