-- ============================================================
-- V7 — Consentimento de acesso ao historico e auditoria de leitura
--
-- POR QUE ESTA MIGRATION EXISTE
-- Ate aqui o veterinario enxergava o historico clinico de TODOS os
-- animais, sem pedir autorizacao a ninguem: SegurancaService.temVisaoAmpla()
-- devolvia true para o perfil inteiro. A visao de produto exige o
-- contrario — o tutor decide quem ve o historico do animal dele.
--
-- O ACESSO PASSA A TER TRES NIVEIS (spec 08, parte 6)
--
--   0  operacional        quem tem agendamento: nome, especie, raca, porte
--   1  resumo de seguranca  qualquer veterinario autenticado, sempre:
--                         alergia, condicao cronica, medicacao continua,
--                         vacina, ultimo peso, castracao, contato
--   2  historico completo  so com consentimento: linha do tempo, laudos,
--                         desfechos, dados completos do tutor
--
-- O nivel 1 nao tem tabela propria: ele e DERIVADO de alerta_clinico
-- (V6), dos eventos de vacina e do peso ja gravado. Resumo mantido a mao
-- envelhece, e resumo de alergia desatualizado e pior que nenhum.
--
-- Esta migration cria o que sustenta o nivel 2.
--
-- SOBRE autorizacao_acesso — por que por CLINICA, e nao por veterinario
-- O tutor escolhe onde atender, nao quem o atende: quem esta de plantao
-- no dia pode nao ser quem estava agendado, e uma autorizacao nominal
-- deixaria o substituto sem acesso justamente no atendimento. A guarda do
-- prontuario tambem e do estabelecimento, nao do profissional.
--
-- SOBRE A VIGENCIA
-- valido_ate nasce em 2 anos apos o atendimento e e estendido a cada novo
-- atendimento na mesma clinica. A autorizacao vive enquanto a relacao
-- viver: quem continua indo mantem, quem parou de ir expira sozinho. Nao
-- ha renovacao a pedir, e nao ha acesso perpetuo por esquecimento.
--
-- SOBRE A UNICIDADE
-- uk_autorizacao_animal_clinica garante UMA linha por par. Um novo
-- agendamento ESTENDE a existente em vez de criar outra — sem isso, tres
-- anos de consultas deixariam trinta autorizacoes empilhadas e o tutor
-- teria de revogar uma a uma.
--
-- SOBRE acesso_historico — por que uma linha por DIA, e nao por leitura
-- "Todo acesso e registrado" ao pe da letra significa uma linha por GET.
-- O veterinario abre a tela tres vezes na consulta, o front repagina, e
-- viram dezenas de linhas por atendimento: a auditoria fica maior que o
-- resto do banco e ilegivel para o tutor, que e quem deveria le-la. O que
-- importa a ele e "a Dra. Camila leu o historico do Thor em 12/09", nao
-- quantas vezes rolou a pagina. Dai a chave (usuario, animal, dia) e o
-- contador vezes.
-- ============================================================

CREATE TABLE autorizacao_acesso (
    id             VARCHAR2(36) PRIMARY KEY,
    animal_id      VARCHAR2(36) NOT NULL,
    clinica_id     VARCHAR2(36) NOT NULL,
    status         VARCHAR2(15) NOT NULL,
    concedida_em   DATE         DEFAULT SYSDATE NOT NULL,
    valido_ate     DATE         NOT NULL,
    revogada_em    DATE,
    origem_evento_id VARCHAR2(36),
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
    -- Revogada sem data de revogacao seria um registro que afirma um fato
    -- sem dizer quando ele aconteceu — e a data e o que sustenta a
    -- pergunta "ele leu antes ou depois de eu revogar?".
    CONSTRAINT chk_autorizacao_revogacao CHECK (
        (status = 'REVOGADA' AND revogada_em IS NOT NULL)
     OR (status <> 'REVOGADA' AND revogada_em IS NULL)),
    CONSTRAINT uk_autorizacao_animal_clinica UNIQUE (animal_id, clinica_id)
);

CREATE INDEX idx_autorizacao_animal ON autorizacao_acesso (animal_id, status);

CREATE TABLE acesso_historico (
    id          VARCHAR2(36) PRIMARY KEY,
    animal_id   VARCHAR2(36) NOT NULL,
    usuario_id  VARCHAR2(36) NOT NULL,
    clinica_id  VARCHAR2(36),
    dia         DATE         NOT NULL,
    nivel       NUMBER(1)    NOT NULL,
    vezes       NUMBER(6)    DEFAULT 1 NOT NULL,
    emergencial NUMBER(1)    DEFAULT 0 NOT NULL,
    motivo      VARCHAR2(500),
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
    -- Quebra de vidro sem motivo seria o mesmo que nao ter quebra de
    -- vidro: o motivo obrigatorio e o unico custo que o acesso sem
    -- consentimento impoe a quem o aciona.
    CONSTRAINT chk_acesso_motivo CHECK (emergencial = 0 OR motivo IS NOT NULL),
    CONSTRAINT uk_acesso_dia UNIQUE (animal_id, usuario_id, dia, emergencial)
);

CREATE INDEX idx_acesso_animal ON acesso_historico (animal_id, dia);
CREATE INDEX idx_acesso_usuario ON acesso_historico (usuario_id, dia);

-- O interruptor do nivel 1, na mao do tutor.
-- Nasce ligado (DEFAULT 1) porque o valor do resumo esta em estar
-- disponivel na emergencia: opt-in silencioso significaria que quase
-- ninguem o teria quando precisasse. Desligar continua sendo escolha do
-- tutor, com aviso do que se perde.
ALTER TABLE animal ADD resumo_seguranca_ativo NUMBER(1) DEFAULT 1 NOT NULL;
ALTER TABLE animal ADD CONSTRAINT chk_animal_resumo CHECK (resumo_seguranca_ativo IN (0,1));
