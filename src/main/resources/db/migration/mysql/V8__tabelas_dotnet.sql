-- ============================================================
-- V8 — Tabelas da API .NET no schema compartilhado
--
-- POR QUE ESTA MIGRATION EXISTE
-- As duas APIs do projeto gravam no MESMO banco, mas ate aqui so metade do
-- schema era versionada. As tabelas t_clyvo_* nasciam de um SQL avulso no
-- repositorio da .NET (schema/script_bd.sql), fora do Flyway e fora de
-- qualquer historico. Duas consequencias praticas:
--
--   1. No deploy so o Flyway roda. As tabelas t_clyvo_* simplesmente nao
--      existiriam, e a .NET falharia na primeira chamada dizendo que a tabela
--      t_clyvo_lembrete nao existe -- subindo normalmente antes disso, porque
--      ela nao valida schema nem tem migrations proprias.
--   2. Aquele arquivo carregava uma copia MANUAL do schema deste repositorio,
--      que divergia em silencio a cada mudanca feita aqui.
--
-- A partir desta migration o Flyway e a fonte unica do schema inteiro.
--
-- QUEM USA O QUE
-- Estas seis tabelas sao lidas e escritas pela API .NET (EF Core + Pomelo).
-- NENHUMA entidade JPA deste repositorio as mapeia, e o ddl-auto=validate
-- portanto nao as inspeciona.
--
-- Por isso os booleanos aqui sao TINYINT, e nao INT como nas tabelas do Java:
-- o Pomelo mapeia bool para tinyint(1) nativamente. Nas tabelas do Java o INT
-- e obrigatorio porque o NumericBooleanConverter entrega Integer ao JDBC e o
-- validate reprova TINYINT contra INTEGER. Os dois lados estao certos pelo
-- mesmo motivo: cada um segue o que o SEU ORM espera.
--
-- AS FKs APONTAM PARA animal(id), NAO PARA UMA COPIA
-- O schema antigo da .NET criava t_clyvo_animal e t_clyvo_tutor proprias, e
-- era isso que fazia o animalId devolvido pelo POST /animais nao existir para
-- a .NET. Aqui as FKs referenciam animal(id) da V1 -- e o que torna o
-- compartilhamento real em vez de nominal.
--
-- SEM ON DELETE CASCADE, AO CONTRARIO DA V7
-- Lembrete e sugestao sao dados de outra aplicacao. Apagar um animal e apagar
-- em silencio o que a .NET gravou sobre ele seria decisao dela, nao desta
-- migration. O DELETE /animais/{id} passa a falhar com 409 enquanto houver
-- lembrete -- que e o comportamento correto ate as duas equipes decidirem
-- outra coisa.
-- ============================================================

CREATE TABLE t_clyvo_produto (
    id                VARCHAR(36)   NOT NULL PRIMARY KEY,
    nome              VARCHAR(200)  NOT NULL,
    descricao         VARCHAR(1000),
    categoria         VARCHAR(30),
    preco             DECIMAL(10,2),
    especie_indicada  VARCHAR(30),
    ativo             TINYINT       NOT NULL DEFAULT 1,
    criado_em         DATETIME      NOT NULL,
    CONSTRAINT chk_produto_ativo CHECK (ativo IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_clyvo_sugestao_produto (
    id             VARCHAR(36)  NOT NULL PRIMARY KEY,
    animal_id      VARCHAR(36)  NOT NULL,
    produto_id     VARCHAR(36)  NOT NULL,
    justificativa  VARCHAR(500),
    data_sugestao  DATE         NOT NULL,
    ativo          TINYINT      NOT NULL DEFAULT 1,
    criado_em      DATETIME     NOT NULL,
    CONSTRAINT fk_sugestao_animal  FOREIGN KEY (animal_id)  REFERENCES animal(id),
    CONSTRAINT fk_sugestao_produto FOREIGN KEY (produto_id) REFERENCES t_clyvo_produto(id),
    CONSTRAINT chk_sugestao_ativo  CHECK (ativo IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_sugestao_animal  ON t_clyvo_sugestao_produto(animal_id);
CREATE INDEX idx_sugestao_produto ON t_clyvo_sugestao_produto(produto_id);

CREATE TABLE t_clyvo_lembrete (
    id           VARCHAR(36)   NOT NULL PRIMARY KEY,
    animal_id    VARCHAR(36)   NOT NULL,
    titulo       VARCHAR(200)  NOT NULL,
    descricao    VARCHAR(1000),
    -- tipo e status sao enums na .NET, gravados como texto maiusculo via
    -- HasConversion. Na resposta JSON eles saem como numero, porque o
    -- System.Text.Json serializa enum como inteiro por padrao -- e e assim que
    -- o app espera. Coluna texto e contrato numerico convivem de proposito.
    tipo         VARCHAR(30)   NOT NULL,
    agendado_em  DATETIME      NOT NULL,
    recorrente   TINYINT       NOT NULL DEFAULT 0,
    status       VARCHAR(30)   NOT NULL,
    criado_em    DATETIME      NOT NULL,
    CONSTRAINT fk_lembrete_animal FOREIGN KEY (animal_id) REFERENCES animal(id),
    CONSTRAINT chk_lembrete_recorrente CHECK (recorrente IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_lembrete_animal ON t_clyvo_lembrete(animal_id);

CREATE TABLE t_clyvo_evento_pet (
    id              VARCHAR(36)   NOT NULL PRIMARY KEY,
    titulo          VARCHAR(200)  NOT NULL,
    descricao       VARCHAR(1000),
    tipo            VARCHAR(30)   NOT NULL,
    rua             VARCHAR(300),
    numero          VARCHAR(10),
    bairro          VARCHAR(150),
    cidade          VARCHAR(100),
    estado          VARCHAR(10),
    cep             VARCHAR(10),
    data_inicio     DATE          NOT NULL,
    data_fim        DATE,
    especie_alvo    VARCHAR(30)   NOT NULL,
    organizador     VARCHAR(200),
    gratuito        TINYINT       NOT NULL DEFAULT 1,
    link_inscricao  VARCHAR(500),
    ativo           TINYINT       NOT NULL DEFAULT 1,
    criado_em       DATETIME      NOT NULL,
    CONSTRAINT chk_evento_pet_gratuito CHECK (gratuito IN (0,1)),
    CONSTRAINT chk_evento_pet_ativo    CHECK (ativo IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_clyvo_predisposicao_saude (
    id                 VARCHAR(36)   NOT NULL PRIMARY KEY,
    especie            VARCHAR(30)   NOT NULL,
    raca               VARCHAR(100),
    idade_minima_anos  DECIMAL(4,1),
    doenca             VARCHAR(200)  NOT NULL,
    recomendacao       VARCHAR(1000) NOT NULL,
    fonte_referencia   VARCHAR(300)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_clyvo_tutor_telegram (
    id         VARCHAR(36) NOT NULL PRIMARY KEY,
    tutor_id   VARCHAR(36) NOT NULL,
    chat_id    BIGINT      NOT NULL,
    criado_em  DATETIME    NOT NULL,
    -- Sem FK para tutor de proposito: o vinculo e validado pela API da .NET no
    -- /vincular, e uma constraint aqui obrigaria o bot do Telegram a conhecer
    -- o ciclo de vida do tutor.
    CONSTRAINT uk_tutor_telegram_tutor UNIQUE (tutor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Seed
--
-- UUIDs fixos, e nao UUID(): o seed precisa ser identico nos dois conjuntos de
-- migration, e o Oracle nao tem a mesma funcao. Prefixos 7 e 8 -- de 1 a 6 ja
-- estao ocupados pelo seed da V2.
--
-- animal_id referencia os animais fixos da V2 em vez de inventar animais
-- proprios. predisposicao_saude e evento_pet ficam sem seed: nao alimentam
-- nenhuma tela do app.
-- ------------------------------------------------------------

INSERT INTO t_clyvo_produto (id, nome, descricao, categoria, preco, especie_indicada, ativo, criado_em) VALUES
('77777777-7777-7777-7777-000000000001', 'Racao Golden Formula Adulto 15kg', 'Racao premium para caes adultos de medio e grande porte.',      'RACAO',       189.90, 'CACHORRO', 1, CURRENT_TIMESTAMP),
('77777777-7777-7777-7777-000000000002', 'Racao Whiskas Sache Carne 85g',    'Racao umida completa para gatos adultos.',                      'RACAO',         4.50, 'GATO',     1, CURRENT_TIMESTAMP),
('77777777-7777-7777-7777-000000000003', 'Frontline Plus Antipulgas',        'Antiparasitario topico de amplo espectro, aplicacao mensal.',   'MEDICAMENTO',  68.00, 'CACHORRO', 1, CURRENT_TIMESTAMP),
('77777777-7777-7777-7777-000000000004', 'Consulta de Rotina Veterinaria',   'Check-up clinico geral com veterinario credenciado.',           'SERVICO',     150.00, 'TODOS',    1, CURRENT_TIMESTAMP),
('77777777-7777-7777-7777-000000000005', 'Coleira Antipulgas Seresto',       'Protecao continua contra pulgas e carrapatos por ate 8 meses.', 'ACESSORIO',   120.00, 'GATO',     1, CURRENT_TIMESTAMP);

INSERT INTO t_clyvo_sugestao_produto (id, animal_id, produto_id, justificativa, data_sugestao, ativo, criado_em) VALUES
('88888888-8888-8888-8888-000000000001', '44444444-4444-4444-4444-000000000001', '77777777-7777-7777-7777-000000000001', 'Racao indicada para o porte e a fase de vida do animal.',           CURRENT_DATE, 1, CURRENT_TIMESTAMP),
('88888888-8888-8888-8888-000000000002', '44444444-4444-4444-4444-000000000001', '77777777-7777-7777-7777-000000000003', 'Antipulgas recomendado conforme sazonalidade e historico clinico.', CURRENT_DATE, 1, CURRENT_TIMESTAMP),
('88888888-8888-8888-8888-000000000003', '44444444-4444-4444-4444-000000000002', '77777777-7777-7777-7777-000000000002', 'Sache umido indicado para hidratacao e palatabilidade em gatos.',   CURRENT_DATE, 1, CURRENT_TIMESTAMP);
