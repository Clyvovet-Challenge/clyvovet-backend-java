-- ============================================================
-- V5 — Status do atendimento, retorno e peso aferido
--
-- POR QUE ESTA MIGRATION EXISTE
-- O evento_clinico registra hoje que um atendimento foi AGENDADO no
-- sistema, mas nao se ele aconteceu. Sem isso nao existe taxa de falta,
-- nao existe "pet que nao voltou" e nao existe continuidade de cuidado —
-- so uma lista de linhas soltas. As quatro colunas abaixo sao o minimo
-- para o fluxo de controle de retorno e para o Painel do Veterinario.
--
--   status_evento         o atendimento aconteceu, faltou ou foi cancelado
--   data_retorno_previsto quando o retorno DEVERIA acontecer
--   evento_origem_id      liga o RETORNO a consulta que o gerou
--   peso_kg               peso aferido no atendimento (serie por pet)
--
-- SOBRE O DEFAULT 'REALIZADO' — decisao com consequencia
-- Todo evento ja gravado passa a contar como comparecido, o que zera a
-- taxa de falta retroativa. A alternativa seria deixar o historico NULL
-- e exigir o status so em registro novo, mas ai TODA agregacao passa a
-- tratar nulo, e o primeiro relatorio errado nasce de um COUNT que
-- esqueceu disso. Escolhi o default explicito: o numero fica otimista
-- para o passado, mas honesto e uniforme para frente.
--   Fica em aberto QUEM marca o status e QUANDO — decisao 4 de
--   specs/07-backlog.md. Enquanto ela nao vier, o campo nasce correto
--   estruturalmente e povoado por default.
--
-- SOBRE evento_origem_id
-- FK auto-referente. O check chk_evento_origem_propria barra o caso
-- trivial de um evento apontar para si mesmo; ciclos mais longos
-- (A -> B -> A) o banco nao pega e ficam a cargo da aplicacao.
-- ============================================================

ALTER TABLE evento_clinico ADD status_evento VARCHAR2(20) DEFAULT 'REALIZADO' NOT NULL;
ALTER TABLE evento_clinico ADD data_retorno_previsto DATE;
ALTER TABLE evento_clinico ADD evento_origem_id VARCHAR2(36);
ALTER TABLE evento_clinico ADD peso_kg NUMBER(6,3);

ALTER TABLE evento_clinico ADD CONSTRAINT chk_evento_status
    CHECK (status_evento IN ('AGENDADO','REALIZADO','FALTOU','CANCELADO'));

-- peso_kg e opcional: o check so vale quando ha valor. Escrito com o
-- IS NULL explicito porque "peso_kg > 0" sozinho ja aceitaria nulo pela
-- logica de tres valores do SQL — e depender disso e pedir para alguem
-- ler errado depois.
ALTER TABLE evento_clinico ADD CONSTRAINT chk_evento_peso
    CHECK (peso_kg IS NULL OR peso_kg > 0);

ALTER TABLE evento_clinico ADD CONSTRAINT chk_evento_origem_propria
    CHECK (evento_origem_id IS NULL OR evento_origem_id <> id);

ALTER TABLE evento_clinico ADD CONSTRAINT fk_evento_origem
    FOREIGN KEY (evento_origem_id) REFERENCES evento_clinico(id);

-- Os tres indices sustentam as consultas do Painel e do controle de
-- retorno: recorte por veterinario no periodo, historico do pet em ordem
-- de data, e varredura de retornos vencidos.
CREATE INDEX idx_evento_vet_data    ON evento_clinico (veterinario_id, data_evento);
CREATE INDEX idx_evento_animal_data ON evento_clinico (animal_id, data_evento);
CREATE INDEX idx_evento_retorno     ON evento_clinico (data_retorno_previsto);
