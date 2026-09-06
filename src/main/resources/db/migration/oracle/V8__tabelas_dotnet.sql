-- ============================================================
-- V8 — Tabelas da API .NET no schema compartilhado
--
-- Espelho de db/migration/mysql/V8__tabelas_dotnet.sql. O motivo de a migration
-- existir esta no cabecalho de la; aqui ficam so as diferencas de dialeto:
--
--   1. VARCHAR vira VARCHAR2; DECIMAL(p,s) vira NUMBER(p,s); BIGINT vira
--      NUMBER(19); DATETIME vira TIMESTAMP.
--   2. TINYINT vira NUMBER(1) -- a mesma correspondencia de boolean usada da
--      V1 a V7 neste conjunto. Note que no conjunto mysql estas colunas ficam
--      TINYINT, e nao INT como as tabelas do Java: quem le e escreve aqui e o
--      EF Core com Pomelo, que mapeia bool para tinyint(1). A regra de INT vale
--      so para as tabelas que o Hibernate valida.
--   3. A ordem de DEFAULT e NOT NULL se inverte.
--   4. CURRENT_DATE vira SYSDATE; CURRENT_TIMESTAMP vira SYSTIMESTAMP.
--   5. Sem ENGINE nem CHARSET -- nao existem no Oracle.
--
-- ESTE CONJUNTO CONTINUA LOAD-BEARING
-- A suite de testes roda em H2 com MODE=Oracle e aplica estas migrations, e o
-- gerar-script-bd.py monta o DDL de entrega a partir daqui. Deixar a V8 so no
-- conjunto mysql quebraria a paridade que o db/migration/README.md exige.
-- ============================================================

CREATE TABLE t_clyvo_produto (
    id                VARCHAR2(36)  NOT NULL PRIMARY KEY,
    nome              VARCHAR2(200) NOT NULL,
    descricao         VARCHAR2(1000),
    categoria         VARCHAR2(30),
    preco             NUMBER(10,2),
    especie_indicada  VARCHAR2(30),
    ativo             NUMBER(1)     DEFAULT 1 NOT NULL,
    criado_em         TIMESTAMP     NOT NULL,
    CONSTRAINT chk_produto_ativo CHECK (ativo IN (0,1))
);

CREATE TABLE t_clyvo_sugestao_produto (
    id             VARCHAR2(36)  NOT NULL PRIMARY KEY,
    animal_id      VARCHAR2(36)  NOT NULL,
    produto_id     VARCHAR2(36)  NOT NULL,
    justificativa  VARCHAR2(500),
    data_sugestao  DATE          NOT NULL,
    ativo          NUMBER(1)     DEFAULT 1 NOT NULL,
    criado_em      TIMESTAMP     NOT NULL,
    CONSTRAINT fk_sugestao_animal  FOREIGN KEY (animal_id)  REFERENCES animal(id),
    CONSTRAINT fk_sugestao_produto FOREIGN KEY (produto_id) REFERENCES t_clyvo_produto(id),
    CONSTRAINT chk_sugestao_ativo  CHECK (ativo IN (0,1))
);

CREATE INDEX idx_sugestao_animal  ON t_clyvo_sugestao_produto(animal_id);
CREATE INDEX idx_sugestao_produto ON t_clyvo_sugestao_produto(produto_id);

CREATE TABLE t_clyvo_lembrete (
    id           VARCHAR2(36)  NOT NULL PRIMARY KEY,
    animal_id    VARCHAR2(36)  NOT NULL,
    titulo       VARCHAR2(200) NOT NULL,
    descricao    VARCHAR2(1000),
    -- tipo e status sao enums na .NET, gravados como texto maiusculo via
    -- HasConversion. Na resposta JSON eles saem como numero, porque o
    -- System.Text.Json serializa enum como inteiro por padrao.
    tipo         VARCHAR2(30)  NOT NULL,
    agendado_em  TIMESTAMP     NOT NULL,
    recorrente   NUMBER(1)     DEFAULT 0 NOT NULL,
    status       VARCHAR2(30)  NOT NULL,
    criado_em    TIMESTAMP     NOT NULL,
    CONSTRAINT fk_lembrete_animal FOREIGN KEY (animal_id) REFERENCES animal(id),
    CONSTRAINT chk_lembrete_recorrente CHECK (recorrente IN (0,1))
);

CREATE INDEX idx_lembrete_animal ON t_clyvo_lembrete(animal_id);

CREATE TABLE t_clyvo_evento_pet (
    id              VARCHAR2(36)  NOT NULL PRIMARY KEY,
    titulo          VARCHAR2(200) NOT NULL,
    descricao       VARCHAR2(1000),
    tipo            VARCHAR2(30)  NOT NULL,
    rua             VARCHAR2(300),
    numero          VARCHAR2(10),
    bairro          VARCHAR2(150),
    cidade          VARCHAR2(100),
    estado          VARCHAR2(10),
    cep             VARCHAR2(10),
    data_inicio     DATE          NOT NULL,
    data_fim        DATE,
    especie_alvo    VARCHAR2(30)  NOT NULL,
    organizador     VARCHAR2(200),
    gratuito        NUMBER(1)     DEFAULT 1 NOT NULL,
    link_inscricao  VARCHAR2(500),
    ativo           NUMBER(1)     DEFAULT 1 NOT NULL,
    criado_em       TIMESTAMP     NOT NULL,
    CONSTRAINT chk_evento_pet_gratuito CHECK (gratuito IN (0,1)),
    CONSTRAINT chk_evento_pet_ativo    CHECK (ativo IN (0,1))
);

CREATE TABLE t_clyvo_predisposicao_saude (
    id                 VARCHAR2(36)   NOT NULL PRIMARY KEY,
    especie            VARCHAR2(30)   NOT NULL,
    raca               VARCHAR2(100),
    idade_minima_anos  NUMBER(4,1),
    doenca             VARCHAR2(200)  NOT NULL,
    recomendacao       VARCHAR2(1000) NOT NULL,
    fonte_referencia   VARCHAR2(300)
);

CREATE TABLE t_clyvo_tutor_telegram (
    id         VARCHAR2(36) NOT NULL PRIMARY KEY,
    tutor_id   VARCHAR2(36) NOT NULL,
    chat_id    NUMBER(19)   NOT NULL,
    criado_em  TIMESTAMP    NOT NULL,
    -- Sem FK para tutor de proposito: o vinculo e validado pela API da .NET no
    -- /vincular, e uma constraint aqui obrigaria o bot do Telegram a conhecer
    -- o ciclo de vida do tutor.
    CONSTRAINT uk_tutor_telegram_tutor UNIQUE (tutor_id)
);

-- ------------------------------------------------------------
-- Seed
--
-- UUIDs fixos e identicos aos do conjunto mysql. Prefixos 7 e 8 -- de 1 a 6 ja
-- estao ocupados pelo seed da V2, cujos animais sao referenciados aqui.
-- ------------------------------------------------------------

INSERT INTO t_clyvo_produto (id, nome, descricao, categoria, preco, especie_indicada, ativo, criado_em) VALUES
('77777777-7777-7777-7777-000000000001', 'Racao Golden Formula Adulto 15kg', 'Racao premium para caes adultos de medio e grande porte.', 'RACAO', 189.90, 'CACHORRO', 1, SYSTIMESTAMP);
INSERT INTO t_clyvo_produto (id, nome, descricao, categoria, preco, especie_indicada, ativo, criado_em) VALUES
('77777777-7777-7777-7777-000000000002', 'Racao Whiskas Sache Carne 85g', 'Racao umida completa para gatos adultos.', 'RACAO', 4.50, 'GATO', 1, SYSTIMESTAMP);
INSERT INTO t_clyvo_produto (id, nome, descricao, categoria, preco, especie_indicada, ativo, criado_em) VALUES
('77777777-7777-7777-7777-000000000003', 'Frontline Plus Antipulgas', 'Antiparasitario topico de amplo espectro, aplicacao mensal.', 'MEDICAMENTO', 68.00, 'CACHORRO', 1, SYSTIMESTAMP);
INSERT INTO t_clyvo_produto (id, nome, descricao, categoria, preco, especie_indicada, ativo, criado_em) VALUES
('77777777-7777-7777-7777-000000000004', 'Consulta de Rotina Veterinaria', 'Check-up clinico geral com veterinario credenciado.', 'SERVICO', 150.00, 'TODOS', 1, SYSTIMESTAMP);
INSERT INTO t_clyvo_produto (id, nome, descricao, categoria, preco, especie_indicada, ativo, criado_em) VALUES
('77777777-7777-7777-7777-000000000005', 'Coleira Antipulgas Seresto', 'Protecao continua contra pulgas e carrapatos por ate 8 meses.', 'ACESSORIO', 120.00, 'GATO', 1, SYSTIMESTAMP);

INSERT INTO t_clyvo_sugestao_produto (id, animal_id, produto_id, justificativa, data_sugestao, ativo, criado_em) VALUES
('88888888-8888-8888-8888-000000000001', '44444444-4444-4444-4444-000000000001', '77777777-7777-7777-7777-000000000001', 'Racao indicada para o porte e a fase de vida do animal.', SYSDATE, 1, SYSTIMESTAMP);
INSERT INTO t_clyvo_sugestao_produto (id, animal_id, produto_id, justificativa, data_sugestao, ativo, criado_em) VALUES
('88888888-8888-8888-8888-000000000002', '44444444-4444-4444-4444-000000000001', '77777777-7777-7777-7777-000000000003', 'Antipulgas recomendado conforme sazonalidade e historico clinico.', SYSDATE, 1, SYSTIMESTAMP);
INSERT INTO t_clyvo_sugestao_produto (id, animal_id, produto_id, justificativa, data_sugestao, ativo, criado_em) VALUES
('88888888-8888-8888-8888-000000000003', '44444444-4444-4444-4444-000000000002', '77777777-7777-7777-7777-000000000002', 'Sache umido indicado para hidratacao e palatabilidade em gatos.', SYSDATE, 1, SYSTIMESTAMP);
