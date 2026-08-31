-- ============================================================
-- V6 — Catalogo de servicos, agenda do veterinario e alertas clinicos
--
-- POR QUE ESTA MIGRATION EXISTE
-- O tutor agenda a propria consulta (spec 08, fluxo A), e para isso o
-- sistema precisa responder a duas perguntas que hoje nao tem contra o
-- que ser respondidas: "esta clinica oferece este servico?" e "este
-- veterinario tem horario livre?". Nao existia catalogo nem agenda.
--
-- Junto entram as pecas do nivel 1 do fluxo C — o resumo de seguranca
-- que qualquer veterinario autenticado alcanca pelo microchip, sem
-- consentimento previo, porque e o que decide um atendimento de urgencia.
--
--   servico                      o que a clinica oferece, por quanto, em
--                                quanto tempo
--   disponibilidade_veterinario  a grade de horarios de cada profissional
--   bloqueio                     ferias, folga, almoco — o furo na grade
--   alerta_clinico               alergia, condicao cronica, medicacao
--                                continua: o conteudo do nivel 1
--   animal.microchip             identificacao no balcao
--   animal.castrado              compoe o resumo de seguranca
--   evento_clinico.servico_id    liga o atendimento ao catalogo, e e DAQUI
--                                que sai o valor cobrado
--   evento_clinico.desfecho      resultado clinico, base da leitura por raca
--   evento_clinico.motivo_cancelamento  exigido quando a clinica cancela
--
-- SOBRE O MICROCHIP — o que ele e e o que ele NAO e
-- Ele identifica; ele nao autoriza. Esta impresso na carteira de
-- vacinacao e no contrato de adocao, qualquer leitor de pet shop o le, e
-- o padrao ISO 11784/11785 tem faixas previsiveis por pais e fabricante.
-- Como senha nao vale nada. Quem credencia a leitura do resumo e a
-- autenticacao do veterinario, nunca o numero em si.
-- A unicidade e parcial por natureza: animal sem chip fica NULL, e o
-- Oracle nao conta nulos no indice unico — varios animais sem chip
-- convivem, dois com o mesmo chip nao.
--
-- SOBRE A GRADE DE HORARIOS
-- dia_semana guarda o nome em portugues, e nao o numero ISO, para que a
-- linha seja legivel em consulta manual: quem abre a tabela no SQL
-- Developer as 2h da manha nao deveria ter de lembrar se domingo e 1 ou 7.
-- hora_inicio e hora_fim seguem o formato VARCHAR2(5) 'HH:mm' ja usado em
-- evento_clinico.hora_evento — divergir aqui obrigaria a duas conversoes
-- diferentes no mesmo fluxo de agendamento.
--
-- SOBRE bloqueio
-- Cobre dois casos com uma estrutura so. Ferias sao varios dias sem hora
-- (hora_inicio e hora_fim nulos = o dia inteiro); almoco e um dia com
-- hora. O check garante que ou as duas horas vem, ou nenhuma vem.
-- ============================================================

-- ---------- Catalogo ----------

CREATE TABLE servico (
    id               VARCHAR2(36)  PRIMARY KEY,
    clinica_id       VARCHAR2(36)  NOT NULL,
    nome             VARCHAR2(100) NOT NULL,
    tipo_evento      VARCHAR2(20)  NOT NULL,
    preco            NUMBER(10,2)  NOT NULL,
    duracao_minutos  NUMBER(4)     NOT NULL,
    ativo            NUMBER(1)     DEFAULT 1 NOT NULL,
    CONSTRAINT fk_servico_clinica  FOREIGN KEY (clinica_id) REFERENCES clinica(id),
    CONSTRAINT chk_servico_tipo    CHECK (tipo_evento IN ('CONSULTA','RETORNO','VACINA','EXAME','CIRURGIA','OUTRO')),
    CONSTRAINT chk_servico_preco   CHECK (preco >= 0),
    CONSTRAINT chk_servico_duracao CHECK (duracao_minutos BETWEEN 5 AND 480),
    CONSTRAINT chk_servico_ativo   CHECK (ativo IN (0,1)),
    CONSTRAINT uk_servico_clinica_nome UNIQUE (clinica_id, nome)
);

CREATE INDEX idx_servico_clinica ON servico (clinica_id, ativo);

-- ---------- Agenda ----------

CREATE TABLE disponibilidade_veterinario (
    id               VARCHAR2(36) PRIMARY KEY,
    veterinario_id   VARCHAR2(36) NOT NULL,
    dia_semana       VARCHAR2(10) NOT NULL,
    hora_inicio      VARCHAR2(5)  NOT NULL,
    hora_fim         VARCHAR2(5)  NOT NULL,
    vigencia_inicio  DATE         NOT NULL,
    vigencia_fim     DATE,
    CONSTRAINT fk_disp_veterinario FOREIGN KEY (veterinario_id) REFERENCES veterinario(id),
    CONSTRAINT chk_disp_dia        CHECK (dia_semana IN
        ('SEGUNDA','TERCA','QUARTA','QUINTA','SEXTA','SABADO','DOMINGO')),
    CONSTRAINT chk_disp_horas      CHECK (hora_fim > hora_inicio),
    CONSTRAINT chk_disp_vigencia   CHECK (vigencia_fim IS NULL OR vigencia_fim >= vigencia_inicio)
);

CREATE INDEX idx_disp_vet_dia ON disponibilidade_veterinario (veterinario_id, dia_semana);

CREATE TABLE bloqueio (
    id              VARCHAR2(36)  PRIMARY KEY,
    veterinario_id  VARCHAR2(36)  NOT NULL,
    data_inicio     DATE          NOT NULL,
    data_fim        DATE          NOT NULL,
    hora_inicio     VARCHAR2(5),
    hora_fim        VARCHAR2(5),
    motivo          VARCHAR2(200) NOT NULL,
    CONSTRAINT fk_bloqueio_veterinario FOREIGN KEY (veterinario_id) REFERENCES veterinario(id),
    CONSTRAINT chk_bloqueio_datas CHECK (data_fim >= data_inicio),
    CONSTRAINT chk_bloqueio_horas CHECK (
        (hora_inicio IS NULL AND hora_fim IS NULL)
     OR (hora_inicio IS NOT NULL AND hora_fim IS NOT NULL AND hora_fim > hora_inicio))
);

CREATE INDEX idx_bloqueio_vet_data ON bloqueio (veterinario_id, data_inicio, data_fim);

-- ---------- Nivel 1 do fluxo C ----------

CREATE TABLE alerta_clinico (
    id           VARCHAR2(36)  PRIMARY KEY,
    animal_id    VARCHAR2(36)  NOT NULL,
    tipo         VARCHAR2(20)  NOT NULL,
    descricao    VARCHAR2(500) NOT NULL,
    origem       VARCHAR2(15)  NOT NULL,
    registrado_em DATE         DEFAULT SYSDATE NOT NULL,
    ativo        NUMBER(1)     DEFAULT 1 NOT NULL,
    CONSTRAINT fk_alerta_animal FOREIGN KEY (animal_id) REFERENCES animal(id),
    CONSTRAINT chk_alerta_tipo  CHECK (tipo IN
        ('ALERGIA','CONDICAO_CRONICA','MEDICACAO_CONTINUA','CRITICO')),
    -- A origem nao e decoracao: "o tutor disse que tem alergia a dipirona"
    -- e "o veterinario registrou anafilaxia a dipirona" pesam diferente na
    -- decisao clinica, e quem le o resumo precisa saber qual dos dois e.
    CONSTRAINT chk_alerta_origem CHECK (origem IN ('TUTOR','VETERINARIO')),
    CONSTRAINT chk_alerta_ativo  CHECK (ativo IN (0,1))
);

CREATE INDEX idx_alerta_animal ON alerta_clinico (animal_id, ativo);

-- ---------- Colunas novas ----------

ALTER TABLE animal ADD microchip VARCHAR2(15);
ALTER TABLE animal ADD castrado NUMBER(1);

ALTER TABLE animal ADD CONSTRAINT uk_animal_microchip UNIQUE (microchip);
ALTER TABLE animal ADD CONSTRAINT chk_animal_castrado CHECK (castrado IS NULL OR castrado IN (0,1));

ALTER TABLE evento_clinico ADD servico_id VARCHAR2(36);
ALTER TABLE evento_clinico ADD desfecho VARCHAR2(20);
ALTER TABLE evento_clinico ADD motivo_cancelamento VARCHAR2(500);

ALTER TABLE evento_clinico ADD CONSTRAINT fk_evento_servico
    FOREIGN KEY (servico_id) REFERENCES servico(id);

-- Nulo e o estado legitimo de todo evento ja gravado e de todo
-- atendimento ainda em aberto: so faz sentido falar de desfecho depois
-- que o atendimento terminou. Por isso nao ha DEFAULT aqui — ao
-- contrario de status_evento na V5, onde o default existia justamente
-- para nao deixar a agregacao lidando com nulo.
ALTER TABLE evento_clinico ADD CONSTRAINT chk_evento_desfecho
    CHECK (desfecho IS NULL OR desfecho IN ('MELHORA','ESTAVEL','PIORA','OBITO','INDEFINIDO'));
