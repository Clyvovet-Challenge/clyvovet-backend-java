-- ============================================================
-- V10 — Pedido de alteração do cadastro do animal
--
-- POR QUE ESTA TABELA EXISTE
-- A regra do produto é que o veterinário só altera os dados do animal "desde que
-- tenha uma confirmação e autorização do dono". Esse consentimento nunca existiu.
--
-- O que existia era o t_clyvo_autorizacao_acesso, e ele responde outra pergunta:
-- autoriza uma CLÍNICA a LER o histórico clínico, e nasce dentro do agendamento.
-- Nada a ver com mutação de cadastro. O próprio StatusAutorizacao documenta que
-- não há estado PENDENTE nem fila de aprovação.
--
-- Sem mecanismo, a escrita ficou fechada: PUT/PATCH/DELETE de animal exigem ser o
-- dono ou o ADMIN da plataforma. Esta tabela é o caminho que faltava para o
-- veterinário voltar a poder alterar — pedindo.
--
-- O PEDIDO É UM PATCH GUARDADO, E ISSO É DELIBERADO
-- As colunas abaixo espelham os campos editáveis do animal, todas anuláveis. Só as
-- preenchidas fazem parte do pedido, exatamente como o corpo de um PATCH.
--
-- A alternativa seria guardar um JSON com os campos. Foi descartada por dois
-- motivos: JSON não é portável entre MySQL e Oracle da mesma forma (no Oracle 19c
-- é CLOB com constraint IS JSON), e um blob de texto não aceita Bean Validation —
-- o pedido entraria sem validação de formato e só falharia na hora de aplicar,
-- depois de o tutor já ter aprovado.
--
-- O TUTOR APROVA O CONTEÚDO, NÃO A PERMISSÃO
-- Não há janela de tempo em que o veterinário "pode editar". Ele propõe valores
-- concretos, o tutor vê exatamente o que mudaria, e aprova ou recusa. A diferença
-- importa: uma permissão temporária precisa ser vigiada, um valor aprovado é
-- auditável para sempre — fica registrado quem pediu, o que pediu, quem respondeu
-- e quando.
--
-- NOME DA TABELA: 29 CARACTERES, DE PROPÓSITO
-- "t_clyvo_solicitacao_alteracao_animal" teria 36, e o Oracle anterior ao 12.2
-- corta identificadores em 30 — foi o que obrigou a V9 a abreviar
-- disponibilidade_veterinario. O sufixo "_animal" saiu; o FK já diz a qual
-- entidade o pedido se refere.
-- ============================================================

CREATE TABLE t_clyvo_solicitacao_alteracao (
    id                VARCHAR(36)  NOT NULL PRIMARY KEY,
    animal_id         VARCHAR(36)  NOT NULL,
    veterinario_id    VARCHAR(36)  NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    justificativa     VARCHAR(500) NOT NULL,

    -- Os campos propostos. Nulo significa "não faz parte deste pedido".
    nome              VARCHAR(100),
    raca              VARCHAR(100),
    especie           VARCHAR(100),
    porte             VARCHAR(100),
    cor               VARCHAR(100),
    genero            VARCHAR(10),
    data_nascimento   DATE,
    microchip         VARCHAR(15),
    -- INT, e não TINYINT: o NumericBooleanConverter entrega Integer ao JDBC, e o
    -- ddl-auto=validate reprova TINYINT contra INTEGER. Mesma regra das outras
    -- sete colunas booleanas deste schema.
    castrado          INT,
    observacoes       VARCHAR(1000),

    criado_em         DATETIME     NOT NULL,
    respondido_em     DATETIME,
    -- Quem respondeu. É sempre o dono no fluxo normal, mas o ADMIN da plataforma
    -- também pode — e aí é preciso saber qual dos dois foi.
    respondido_por    VARCHAR(36),
    motivo_recusa     VARCHAR(500),

    CONSTRAINT fk_solicitacao_animal FOREIGN KEY (animal_id)      REFERENCES t_clyvo_animal(id),
    CONSTRAINT fk_solicitacao_vet    FOREIGN KEY (veterinario_id) REFERENCES t_clyvo_veterinario(id),
    CONSTRAINT chk_solicitacao_status CHECK (status IN ('PENDENTE','APROVADA','RECUSADA')),
    CONSTRAINT chk_solicitacao_castrado CHECK (castrado IS NULL OR castrado IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- A consulta quente é "os pedidos pendentes dos meus pets", que o app faz a cada
-- abertura da caixa de pedidos do tutor.
CREATE INDEX idx_solicitacao_animal_status ON t_clyvo_solicitacao_alteracao (animal_id, status);

-- E a do outro lado: "o que eu pedi e ainda não foi respondido".
CREATE INDEX idx_solicitacao_vet_status ON t_clyvo_solicitacao_alteracao (veterinario_id, status);
