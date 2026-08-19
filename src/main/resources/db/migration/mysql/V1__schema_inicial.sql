-- ============================================================
-- V1 -- Schema inicial do CLYVO VET (MySQL)
--
-- Traducao da V1 de db/migration/oracle/. As unicas diferencas
-- em relacao a ela sao de tipo, e estao explicadas aqui:
--
--   VARCHAR2(n)  -> VARCHAR(n)
--       O MySQL nao conhece VARCHAR2. Semantica equivalente.
--
--   NUMBER(10,2) -> DECIMAL(10,2)
--       Mesma precisao exata. Nao usar DOUBLE: valor monetario
--       em ponto flutuante acumula erro de arredondamento.
--
--   ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
--       Explicito de proposito. Sao os defaults do MySQL 8, mas
--       um servidor com configuracao propria poderia criar tabela
--       MyISAM (que ignora FK em silencio) ou com charset que nao
--       aceita acento em nome e endereco.
--
-- DATE fica DATE: todas as entidades usam LocalDate, entao a
-- ausencia de componente de hora no DATE do MySQL nao perde nada.
--
-- Os CHECK sao aplicados de verdade a partir do MySQL 8.0.16.
-- Abaixo disso o servidor os aceita e ignora. O Azure Database
-- for MySQL Flexible Server e 8.0.21+, entao valem.
-- ============================================================

CREATE TABLE tutor (
    id              VARCHAR(36)  PRIMARY KEY,
    cpf             VARCHAR(11),
    nome            VARCHAR(150) NOT NULL,
    data_nascimento DATE,
    genero          VARCHAR(10),
    email           VARCHAR(200),
    telefone        VARCHAR(20),
    rua             VARCHAR(300),
    numero          VARCHAR(10),
    complemento     VARCHAR(100),
    bairro          VARCHAR(150),
    cidade          VARCHAR(100),
    estado          VARCHAR(50),
    cep             VARCHAR(10),
    CONSTRAINT uk_tutor_cpf     UNIQUE (cpf),
    CONSTRAINT uk_tutor_email   UNIQUE (email),
    CONSTRAINT chk_tutor_genero CHECK (genero IN ('MASCULINO','FEMININO','OUTRO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE clinica (
    id          VARCHAR(36)  PRIMARY KEY,
    nome        VARCHAR(200) NOT NULL,
    cnpj        VARCHAR(14),
    telefone    VARCHAR(20),
    email       VARCHAR(200),
    rua         VARCHAR(300),
    numero      VARCHAR(10),
    complemento VARCHAR(100),
    bairro      VARCHAR(150),
    cidade      VARCHAR(100),
    estado      VARCHAR(50),
    cep         VARCHAR(10),
    CONSTRAINT uk_clinica_cnpj UNIQUE (cnpj)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE animal (
    id               VARCHAR(36)  PRIMARY KEY,
    nome             VARCHAR(100) NOT NULL,
    raca             VARCHAR(100),
    especie          VARCHAR(50),
    porte            VARCHAR(20),
    cor              VARCHAR(80),
    genero           VARCHAR(10),
    data_nascimento  DATE,
    observacoes      VARCHAR(1000),
    tutor_id         VARCHAR(36),
    CONSTRAINT fk_animal_tutor   FOREIGN KEY (tutor_id) REFERENCES tutor(id),
    CONSTRAINT chk_animal_porte  CHECK (porte  IN ('PEQUENO','MEDIO','GRANDE')),
    CONSTRAINT chk_animal_genero CHECK (genero IN ('MACHO','FEMEA','DESCONHECIDO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE veterinario (
    id               VARCHAR(36)  PRIMARY KEY,
    cpf              VARCHAR(11),
    nome             VARCHAR(150) NOT NULL,
    data_nascimento  DATE,
    genero           VARCHAR(10),
    email            VARCHAR(200),
    telefone         VARCHAR(20),
    especialidade    VARCHAR(100),
    crmv             VARCHAR(30),
    rua              VARCHAR(300),
    numero           VARCHAR(10),
    complemento      VARCHAR(100),
    bairro           VARCHAR(150),
    cidade           VARCHAR(100),
    estado           VARCHAR(50),
    cep              VARCHAR(10),
    clinica_id       VARCHAR(36),
    CONSTRAINT fk_vet_clinica  FOREIGN KEY (clinica_id) REFERENCES clinica(id),
    CONSTRAINT uk_vet_cpf      UNIQUE (cpf),
    CONSTRAINT uk_vet_crmv     UNIQUE (crmv),
    CONSTRAINT chk_vet_genero  CHECK (genero IN ('MASCULINO','FEMININO','OUTRO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE evento_clinico (
    id              VARCHAR(36)  PRIMARY KEY,
    data_evento     DATE,
    hora_evento     VARCHAR(5),
    descricao       VARCHAR(1000),
    tipo_evento     VARCHAR(20),
    veterinario_id  VARCHAR(36),
    animal_id       VARCHAR(36),
    clinica_id      VARCHAR(36),
    CONSTRAINT fk_evento_vet     FOREIGN KEY (veterinario_id) REFERENCES veterinario(id),
    CONSTRAINT fk_evento_animal  FOREIGN KEY (animal_id)      REFERENCES animal(id),
    CONSTRAINT fk_evento_clinica FOREIGN KEY (clinica_id)     REFERENCES clinica(id),
    CONSTRAINT chk_evento_tipo   CHECK (tipo_evento IN ('CONSULTA','RETORNO','VACINA','EXAME','CIRURGIA','OUTRO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- NOTA: o check de status_pagamento reproduz aqui o schema original, com
-- 'ESTORNADO'. A correcao para 'REEMBOLSADO' esta na V4 — assim ela se aplica
-- tambem aos bancos ja provisionados, que entram via baseline.
CREATE TABLE pagamento (
    id                VARCHAR(36)  PRIMARY KEY,
    metodo_pagamento  VARCHAR(10),
    valor             DECIMAL(10,2),
    data_pagamento    DATE,
    descricao         VARCHAR(500),
    notas             VARCHAR(1000),
    status_pagamento  VARCHAR(15),
    evento_id         VARCHAR(36),
    CONSTRAINT fk_pagamento_evento  FOREIGN KEY (evento_id) REFERENCES evento_clinico(id),
    CONSTRAINT chk_forma_pagamento  CHECK (metodo_pagamento IN ('PIX','CARTAO','DINHEIRO','BOLETO')),
    CONSTRAINT chk_status_pagamento CHECK (status_pagamento IN ('PENDENTE','PAGO','CANCELADO','ESTORNADO')),
    CONSTRAINT chk_pagamento_valor  CHECK (valor > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
