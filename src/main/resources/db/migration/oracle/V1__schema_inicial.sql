-- ============================================================
-- V1 — Schema inicial do CLYVO VET
-- Convertido de db/db-oracle.sql, que passa a ser apenas
-- referencia historica. O schema oficial vive nas migrations.
-- ============================================================

CREATE TABLE tutor (
    id              VARCHAR2(36)  PRIMARY KEY,
    cpf             VARCHAR2(11),
    nome            VARCHAR2(150) NOT NULL,
    data_nascimento DATE,
    genero          VARCHAR2(10),
    email           VARCHAR2(200),
    telefone        VARCHAR2(20),
    rua             VARCHAR2(300),
    numero          VARCHAR2(10),
    complemento     VARCHAR2(100),
    bairro          VARCHAR2(150),
    cidade          VARCHAR2(100),
    estado          VARCHAR2(50),
    cep             VARCHAR2(10),
    CONSTRAINT uk_tutor_cpf     UNIQUE (cpf),
    CONSTRAINT uk_tutor_email   UNIQUE (email),
    CONSTRAINT chk_tutor_genero CHECK (genero IN ('MASCULINO','FEMININO','OUTRO'))
);

CREATE TABLE clinica (
    id          VARCHAR2(36)  PRIMARY KEY,
    nome        VARCHAR2(200) NOT NULL,
    cnpj        VARCHAR2(14),
    telefone    VARCHAR2(20),
    email       VARCHAR2(200),
    rua         VARCHAR2(300),
    numero      VARCHAR2(10),
    complemento VARCHAR2(100),
    bairro      VARCHAR2(150),
    cidade      VARCHAR2(100),
    estado      VARCHAR2(50),
    cep         VARCHAR2(10),
    CONSTRAINT uk_clinica_cnpj UNIQUE (cnpj)
);

CREATE TABLE animal (
    id               VARCHAR2(36)  PRIMARY KEY,
    nome             VARCHAR2(100) NOT NULL,
    raca             VARCHAR2(100),
    especie          VARCHAR2(50),
    porte            VARCHAR2(20),
    cor              VARCHAR2(80),
    genero           VARCHAR2(10),
    data_nascimento  DATE,
    observacoes      VARCHAR2(1000),
    tutor_id         VARCHAR2(36),
    CONSTRAINT fk_animal_tutor   FOREIGN KEY (tutor_id) REFERENCES tutor(id),
    CONSTRAINT chk_animal_porte  CHECK (porte  IN ('PEQUENO','MEDIO','GRANDE')),
    CONSTRAINT chk_animal_genero CHECK (genero IN ('MACHO','FEMEA','DESCONHECIDO'))
);

CREATE TABLE veterinario (
    id               VARCHAR2(36)  PRIMARY KEY,
    cpf              VARCHAR2(11),
    nome             VARCHAR2(150) NOT NULL,
    data_nascimento  DATE,
    genero           VARCHAR2(10),
    email            VARCHAR2(200),
    telefone         VARCHAR2(20),
    especialidade    VARCHAR2(100),
    crmv             VARCHAR2(30),
    rua              VARCHAR2(300),
    numero           VARCHAR2(10),
    complemento      VARCHAR2(100),
    bairro           VARCHAR2(150),
    cidade           VARCHAR2(100),
    estado           VARCHAR2(50),
    cep              VARCHAR2(10),
    clinica_id       VARCHAR2(36),
    CONSTRAINT fk_vet_clinica  FOREIGN KEY (clinica_id) REFERENCES clinica(id),
    CONSTRAINT uk_vet_cpf      UNIQUE (cpf),
    CONSTRAINT uk_vet_crmv     UNIQUE (crmv),
    CONSTRAINT chk_vet_genero  CHECK (genero IN ('MASCULINO','FEMININO','OUTRO'))
);

CREATE TABLE evento_clinico (
    id              VARCHAR2(36)  PRIMARY KEY,
    data_evento     DATE,
    hora_evento     VARCHAR2(5),
    descricao       VARCHAR2(1000),
    tipo_evento     VARCHAR2(20),
    veterinario_id  VARCHAR2(36),
    animal_id       VARCHAR2(36),
    clinica_id      VARCHAR2(36),
    CONSTRAINT fk_evento_vet     FOREIGN KEY (veterinario_id) REFERENCES veterinario(id),
    CONSTRAINT fk_evento_animal  FOREIGN KEY (animal_id)      REFERENCES animal(id),
    CONSTRAINT fk_evento_clinica FOREIGN KEY (clinica_id)     REFERENCES clinica(id),
    CONSTRAINT chk_evento_tipo   CHECK (tipo_evento IN ('CONSULTA','RETORNO','VACINA','EXAME','CIRURGIA','OUTRO'))
);

-- NOTA: o check de status_pagamento reproduz aqui o schema original, com
-- 'ESTORNADO'. A correcao para 'REEMBOLSADO' esta na V4 — assim ela se aplica
-- tambem aos bancos ja provisionados, que entram via baseline.
CREATE TABLE pagamento (
    id                VARCHAR2(36)  PRIMARY KEY,
    metodo_pagamento  VARCHAR2(10),
    valor             NUMBER(10,2),
    data_pagamento    DATE,
    descricao         VARCHAR2(500),
    notas             VARCHAR2(1000),
    status_pagamento  VARCHAR2(15),
    evento_id         VARCHAR2(36),
    CONSTRAINT fk_pagamento_evento  FOREIGN KEY (evento_id) REFERENCES evento_clinico(id),
    CONSTRAINT chk_forma_pagamento  CHECK (metodo_pagamento IN ('PIX','CARTAO','DINHEIRO','BOLETO')),
    CONSTRAINT chk_status_pagamento CHECK (status_pagamento IN ('PENDENTE','PAGO','CANCELADO','ESTORNADO')),
    CONSTRAINT chk_pagamento_valor  CHECK (valor > 0)
);
