-- ============================================================
-- CLYVO VET — BANCO DE DADOS ORACLE
--
-- ⚠️ SCRIPT HISTORICO — NAO EXECUTE MAIS MANUALMENTE.
--
-- O schema passou a ser versionado pelo Flyway em db/migration/.
-- Este arquivo fica no repositorio apenas como referencia da
-- entrega da Sprint 1/2 e como origem do baseline: bancos criados
-- por ele entram no Flyway com baseline-version=2, ou seja, V1 e V2
-- sao consideradas ja aplicadas e a migracao segue da V3.
--
-- Para provisionar um banco novo, basta subir a aplicacao.
-- ============================================================

-- ------------------------------------------------------------
-- PASSO 1: FUNCAO UUID (necessaria para o seed)
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_uuid RETURN VARCHAR2 IS
BEGIN
RETURN LOWER(REGEXP_REPLACE(
        RAWTOHEX(SYS_GUID()),
        '([A-F0-9]{8})([A-F0-9]{4})([A-F0-9]{4})([A-F0-9]{4})([A-F0-9]{12})',
        '\1-\2-\3-\4-\5'
             ));
END fn_uuid;
/

-- ------------------------------------------------------------
-- PASSO 2: TABELAS
-- NOTA: PKs sem DEFAULT fn_uuid() — o Java gera os UUIDs
-- Nomes de colunas alinhados com o DB do projeto completo
-- ------------------------------------------------------------

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

-- ------------------------------------------------------------
-- PASSO 3: SEED DATA
-- ------------------------------------------------------------

-- CLINICAS
INSERT INTO clinica (id, nome, cnpj, telefone, email, rua, numero, bairro, cidade, estado, cep)
VALUES (fn_uuid(), 'VetCare Prime', '12345678000191', '(11) 3100-0001', 'contato@vetcareprime.com.br', 'Av. Paulista', '1000', 'Bela Vista', 'Sao Paulo', 'SP', '01310100');
INSERT INTO clinica (id, nome, cnpj, telefone, email, rua, numero, bairro, cidade, estado, cep)
VALUES (fn_uuid(), 'PetMed Centro', '23456789000102', '(11) 3100-0002', 'contato@petmed.com.br', 'R. Augusta', '420', 'Consolacao', 'Sao Paulo', 'SP', '01304000');
INSERT INTO clinica (id, nome, cnpj, telefone, email, rua, numero, bairro, cidade, estado, cep)
VALUES (fn_uuid(), 'AnimalSaude SP', '34567890000113', '(11) 3100-0003', 'contato@animalsaude.com.br', 'R. Oscar Freire', '88', 'Jardins', 'Sao Paulo', 'SP', '01426001');
INSERT INTO clinica (id, nome, cnpj, telefone, email, rua, numero, bairro, cidade, estado, cep)
VALUES (fn_uuid(), 'CliniPet Jardins', '45678901000124', '(11) 3100-0004', 'contato@clinipet.com.br', 'Al. Santos', '200', 'Jardim Paulista', 'Sao Paulo', 'SP', '01419001');
COMMIT;

-- TUTORES
INSERT INTO tutor (id, nome, cpf, telefone, data_nascimento, genero, rua, numero, bairro, cidade, estado, cep, email)
VALUES (fn_uuid(), 'Lucas M. Santos', '11100011100', '(11) 98000-0001', TO_DATE('10/05/1990','DD/MM/YYYY'), 'MASCULINO', 'R. Haddock Lobo', '595', 'Cerqueira Cesar', 'Sao Paulo', 'SP', '01414002', 'lucas.santos@email.com');
INSERT INTO tutor (id, nome, cpf, telefone, data_nascimento, genero, rua, numero, bairro, cidade, estado, cep, email)
VALUES (fn_uuid(), 'Maria Oliveira', '22200022200', '(11) 97000-0002', TO_DATE('22/08/1985','DD/MM/YYYY'), 'FEMININO', 'R. Estados Unidos', '1000', 'Jardins', 'Sao Paulo', 'SP', '01427002', 'maria.oliveira@email.com');
COMMIT;

-- VETERINARIOS
DECLARE
v_c1 VARCHAR2(36); v_c2 VARCHAR2(36); v_c3 VARCHAR2(36); v_c4 VARCHAR2(36);
BEGIN
SELECT id INTO v_c1 FROM clinica WHERE cnpj = '12345678000191';
SELECT id INTO v_c2 FROM clinica WHERE cnpj = '23456789000102';
SELECT id INTO v_c3 FROM clinica WHERE cnpj = '34567890000113';
SELECT id INTO v_c4 FROM clinica WHERE cnpj = '45678901000124';

INSERT INTO veterinario (id, nome, crmv, especialidade, email, cpf, telefone, genero, data_nascimento, clinica_id, rua, numero, bairro, cidade, estado, cep)
VALUES (fn_uuid(), 'Dra. Camila Ferreira', 'CRMV-SP 14320', 'Clinica Geral', 'camila.ferreira@vetcare.com.br', '11122233344', '(11) 99001-0001', 'FEMININO', TO_DATE('15/03/1985','DD/MM/YYYY'), v_c1, 'Av. Paulista', '1500', 'Bela Vista', 'Sao Paulo', 'SP', '01310200');
INSERT INTO veterinario (id, nome, crmv, especialidade, email, cpf, telefone, genero, data_nascimento, clinica_id, rua, numero, bairro, cidade, estado, cep)
VALUES (fn_uuid(), 'Dr. Rafael Matos', 'CRMV-SP 18741', 'Cardiologia', 'rafael.matos@petmed.com.br', '22233344455', '(11) 99001-0002', 'MASCULINO', TO_DATE('22/07/1980','DD/MM/YYYY'), v_c2, 'R. Augusta', '500', 'Consolacao', 'Sao Paulo', 'SP', '01305000');
INSERT INTO veterinario (id, nome, crmv, especialidade, email, cpf, telefone, genero, data_nascimento, clinica_id, rua, numero, bairro, cidade, estado, cep)
VALUES (fn_uuid(), 'Dr. Andre Costa', 'CRMV-SP 9812', 'Ortopedia', 'andre.costa@animalsaude.com.br', '33344455566', '(11) 99001-0003', 'MASCULINO', TO_DATE('05/11/1978','DD/MM/YYYY'), v_c3, 'R. Oscar Freire', '90', 'Jardins', 'Sao Paulo', 'SP', '01426002');
INSERT INTO veterinario (id, nome, crmv, especialidade, email, cpf, telefone, genero, data_nascimento, clinica_id, rua, numero, bairro, cidade, estado, cep)
VALUES (fn_uuid(), 'Dra. Livia Rocha', 'CRMV-SP 16540', 'Dermatologia', 'livia.rocha@clinipet.com.br', '44455566677', '(11) 99001-0004', 'FEMININO', TO_DATE('18/09/1990','DD/MM/YYYY'), v_c4, 'Al. Santos', '300', 'Jardim Paulista', 'Sao Paulo', 'SP', '01419002');
INSERT INTO veterinario (id, nome, crmv, especialidade, email, cpf, telefone, genero, data_nascimento, clinica_id, rua, numero, bairro, cidade, estado, cep)
VALUES (fn_uuid(), 'Dr. Tomas Oliveira', 'CRMV-SP 11204', 'Clinica Geral', 'tomas.oliveira@vetcare.com.br', '55566677788', '(11) 99001-0005', 'MASCULINO', TO_DATE('30/01/1982','DD/MM/YYYY'), v_c1, 'Av. Paulista', '1200', 'Bela Vista', 'Sao Paulo', 'SP', '01310300');
INSERT INTO veterinario (id, nome, crmv, especialidade, email, cpf, telefone, genero, data_nascimento, clinica_id, rua, numero, bairro, cidade, estado, cep)
VALUES (fn_uuid(), 'Dra. Beatriz Lima', 'CRMV-SP 20333', 'Oncologia', 'beatriz.lima@petmed.com.br', '66677788899', '(11) 99001-0006', 'FEMININO', TO_DATE('14/06/1992','DD/MM/YYYY'), v_c2, 'R. Augusta', '600', 'Consolacao', 'Sao Paulo', 'SP', '01305100');
INSERT INTO veterinario (id, nome, crmv, especialidade, email, cpf, telefone, genero, data_nascimento, clinica_id, rua, numero, bairro, cidade, estado, cep)
VALUES (fn_uuid(), 'Dr. Felipe Souza', 'CRMV-SP 25101', 'Nutricao Animal', 'felipe.souza@animalsaude.com.br', '77788899900', '(11) 99001-0007', 'MASCULINO', TO_DATE('09/04/1995','DD/MM/YYYY'), v_c3, 'R. Oscar Freire', '100', 'Jardins', 'Sao Paulo', 'SP', '01426003');
COMMIT;
END;
/

-- ANIMAIS
DECLARE
v_tutor1 VARCHAR2(36); v_tutor2 VARCHAR2(36);
BEGIN
SELECT id INTO v_tutor1 FROM tutor WHERE cpf = '11100011100';
SELECT id INTO v_tutor2 FROM tutor WHERE cpf = '22200022200';

INSERT INTO animal (id, nome, especie, raca, porte, cor, genero, data_nascimento, observacoes, tutor_id)
VALUES (fn_uuid(), 'Bolinha', 'CAO', 'Golden Retriever', 'GRANDE', 'Dourado', 'MACHO', TO_DATE('12/03/2022','DD/MM/YYYY'), 'Cachorro brincalhao e afetivo', v_tutor1);
INSERT INTO animal (id, nome, especie, raca, porte, cor, genero, data_nascimento, observacoes, tutor_id)
VALUES (fn_uuid(), 'Mimi', 'GATO', 'Siames', 'PEQUENO', 'Bege e marrom', 'FEMEA', TO_DATE('05/07/2021','DD/MM/YYYY'), 'Gata independente', v_tutor2);
INSERT INTO animal (id, nome, especie, raca, porte, cor, genero, data_nascimento, observacoes, tutor_id)
VALUES (fn_uuid(), 'Rex', 'CAO', 'Pastor Alemao', 'GRANDE', 'Preto e marrom', 'MACHO', TO_DATE('18/01/2020','DD/MM/YYYY'), 'Cao de guarda, obediente', v_tutor2);
COMMIT;
END;
/

-- EVENTOS CLINICOS
DECLARE
v_vet1 VARCHAR2(36); v_vet2 VARCHAR2(36); v_vet3 VARCHAR2(36);
    v_vet4 VARCHAR2(36); v_vet5 VARCHAR2(36);
    v_c1   VARCHAR2(36); v_c2   VARCHAR2(36); v_c3   VARCHAR2(36); v_c4 VARCHAR2(36);
    v_a1   VARCHAR2(36); v_a2   VARCHAR2(36); v_a3   VARCHAR2(36);
BEGIN
SELECT id INTO v_vet1 FROM veterinario WHERE crmv = 'CRMV-SP 14320';
SELECT id INTO v_vet2 FROM veterinario WHERE crmv = 'CRMV-SP 18741';
SELECT id INTO v_vet3 FROM veterinario WHERE crmv = 'CRMV-SP 9812';
SELECT id INTO v_vet4 FROM veterinario WHERE crmv = 'CRMV-SP 16540';
SELECT id INTO v_vet5 FROM veterinario WHERE crmv = 'CRMV-SP 11204';
SELECT id INTO v_c1   FROM clinica     WHERE cnpj = '12345678000191';
SELECT id INTO v_c2   FROM clinica     WHERE cnpj = '23456789000102';
SELECT id INTO v_c3   FROM clinica     WHERE cnpj = '34567890000113';
SELECT id INTO v_c4   FROM clinica     WHERE cnpj = '45678901000124';
SELECT id INTO v_a1   FROM animal      WHERE nome = 'Bolinha';
SELECT id INTO v_a2   FROM animal      WHERE nome = 'Mimi';
SELECT id INTO v_a3   FROM animal      WHERE nome = 'Rex';

INSERT INTO evento_clinico (id, data_evento, hora_evento, tipo_evento, descricao, veterinario_id, animal_id, clinica_id)
VALUES (fn_uuid(), TO_DATE('10/01/2024','DD/MM/YYYY'), '09:00', 'CONSULTA', 'Check-up anual de rotina', v_vet1, v_a1, v_c1);
INSERT INTO evento_clinico (id, data_evento, hora_evento, tipo_evento, descricao, veterinario_id, animal_id, clinica_id)
VALUES (fn_uuid(), TO_DATE('15/02/2024','DD/MM/YYYY'), '10:00', 'VACINA', 'V10 - Vacina polivalente anual', v_vet1, v_a1, v_c1);
INSERT INTO evento_clinico (id, data_evento, hora_evento, tipo_evento, descricao, veterinario_id, animal_id, clinica_id)
VALUES (fn_uuid(), TO_DATE('20/03/2024','DD/MM/YYYY'), '14:00', 'EXAME', 'Hemograma completo e bioquimica', v_vet5, v_a1, v_c1);
INSERT INTO evento_clinico (id, data_evento, hora_evento, tipo_evento, descricao, veterinario_id, animal_id, clinica_id)
VALUES (fn_uuid(), TO_DATE('05/06/2024','DD/MM/YYYY'), '11:00', 'RETORNO', 'Retorno pos-exame, resultados normais', v_vet1, v_a1, v_c1);
INSERT INTO evento_clinico (id, data_evento, hora_evento, tipo_evento, descricao, veterinario_id, animal_id, clinica_id)
VALUES (fn_uuid(), TO_DATE('10/09/2024','DD/MM/YYYY'), '09:30', 'VACINA', 'Antirabica anual', v_vet5, v_a1, v_c1);
INSERT INTO evento_clinico (id, data_evento, hora_evento, tipo_evento, descricao, veterinario_id, animal_id, clinica_id)
VALUES (fn_uuid(), TRUNC(SYSDATE) + 7, '10:00', 'CONSULTA', 'Check-up e vermifugacao', v_vet1, v_a1, v_c1);
INSERT INTO evento_clinico (id, data_evento, hora_evento, tipo_evento, descricao, veterinario_id, animal_id, clinica_id)
VALUES (fn_uuid(), TO_DATE('20/02/2024','DD/MM/YYYY'), '15:00', 'CONSULTA', 'Consulta de rotina', v_vet4, v_a2, v_c4);
INSERT INTO evento_clinico (id, data_evento, hora_evento, tipo_evento, descricao, veterinario_id, animal_id, clinica_id)
VALUES (fn_uuid(), TO_DATE('15/04/2024','DD/MM/YYYY'), '16:00', 'VACINA', 'Vacina triplice felina', v_vet4, v_a2, v_c4);
INSERT INTO evento_clinico (id, data_evento, hora_evento, tipo_evento, descricao, veterinario_id, animal_id, clinica_id)
VALUES (fn_uuid(), TRUNC(SYSDATE) + 14, '14:00', 'EXAME', 'Exame de urina e sangue', v_vet2, v_a2, v_c2);
INSERT INTO evento_clinico (id, data_evento, hora_evento, tipo_evento, descricao, veterinario_id, animal_id, clinica_id)
VALUES (fn_uuid(), TO_DATE('08/03/2024','DD/MM/YYYY'), '08:00', 'CIRURGIA', 'Cirurgia de castracao', v_vet3, v_a3, v_c3);
INSERT INTO evento_clinico (id, data_evento, hora_evento, tipo_evento, descricao, veterinario_id, animal_id, clinica_id)
VALUES (fn_uuid(), TO_DATE('25/03/2024','DD/MM/YYYY'), '09:00', 'RETORNO', 'Retorno pos-cirurgico', v_vet3, v_a3, v_c3);
COMMIT;
END;
/

-- PAGAMENTOS
DECLARE
v_ev VARCHAR2(36);
    v_a1 VARCHAR2(36); v_a2 VARCHAR2(36); v_a3 VARCHAR2(36);
BEGIN
SELECT id INTO v_a1 FROM animal WHERE nome = 'Bolinha';
SELECT id INTO v_a2 FROM animal WHERE nome = 'Mimi';
SELECT id INTO v_a3 FROM animal WHERE nome = 'Rex';

SELECT id INTO v_ev FROM evento_clinico WHERE animal_id = v_a1 AND data_evento = TO_DATE('10/01/2024','DD/MM/YYYY');
INSERT INTO pagamento (id, metodo_pagamento, valor, status_pagamento, data_pagamento, descricao, evento_id)
VALUES (fn_uuid(), 'PIX', 150.00, 'PAGO', TO_DATE('10/01/2024','DD/MM/YYYY'), 'Consulta de rotina', v_ev);

SELECT id INTO v_ev FROM evento_clinico WHERE animal_id = v_a1 AND data_evento = TO_DATE('15/02/2024','DD/MM/YYYY');
INSERT INTO pagamento (id, metodo_pagamento, valor, status_pagamento, data_pagamento, descricao, evento_id)
VALUES (fn_uuid(), 'CARTAO', 80.00, 'PAGO', TO_DATE('15/02/2024','DD/MM/YYYY'), 'Vacina V10', v_ev);

SELECT id INTO v_ev FROM evento_clinico WHERE animal_id = v_a1 AND data_evento = TO_DATE('20/03/2024','DD/MM/YYYY');
INSERT INTO pagamento (id, metodo_pagamento, valor, status_pagamento, data_pagamento, descricao, evento_id)
VALUES (fn_uuid(), 'DINHEIRO', 200.00, 'PAGO', TO_DATE('20/03/2024','DD/MM/YYYY'), 'Hemograma e bioquimica', v_ev);

SELECT id INTO v_ev FROM evento_clinico WHERE animal_id = v_a1 AND data_evento = TO_DATE('05/06/2024','DD/MM/YYYY');
INSERT INTO pagamento (id, metodo_pagamento, valor, status_pagamento, data_pagamento, descricao, evento_id)
VALUES (fn_uuid(), 'PIX', 120.00, 'PENDENTE', NULL, 'Retorno Bolinha', v_ev);

SELECT id INTO v_ev FROM evento_clinico WHERE animal_id = v_a2 AND data_evento = TO_DATE('20/02/2024','DD/MM/YYYY');
INSERT INTO pagamento (id, metodo_pagamento, valor, status_pagamento, data_pagamento, descricao, evento_id)
VALUES (fn_uuid(), 'CARTAO', 100.00, 'PAGO', TO_DATE('20/02/2024','DD/MM/YYYY'), 'Consulta Mimi', v_ev);

SELECT id INTO v_ev FROM evento_clinico WHERE animal_id = v_a2 AND data_evento = TO_DATE('15/04/2024','DD/MM/YYYY');
INSERT INTO pagamento (id, metodo_pagamento, valor, status_pagamento, data_pagamento, descricao, evento_id)
VALUES (fn_uuid(), 'PIX', 90.00, 'PENDENTE', NULL, 'Vacina felina Mimi', v_ev);

SELECT id INTO v_ev FROM evento_clinico WHERE animal_id = v_a3 AND data_evento = TO_DATE('08/03/2024','DD/MM/YYYY');
INSERT INTO pagamento (id, metodo_pagamento, valor, status_pagamento, data_pagamento, descricao, evento_id)
VALUES (fn_uuid(), 'BOLETO', 800.00, 'PAGO', TO_DATE('08/03/2024','DD/MM/YYYY'), 'Cirurgia castracao Rex', v_ev);

SELECT id INTO v_ev FROM evento_clinico WHERE animal_id = v_a3 AND data_evento = TO_DATE('25/03/2024','DD/MM/YYYY');
INSERT INTO pagamento (id, metodo_pagamento, valor, status_pagamento, data_pagamento, descricao, evento_id)
VALUES (fn_uuid(), 'PIX', 150.00, 'CANCELADO', NULL, 'Retorno cancelado', v_ev);

COMMIT;
END;
/

-- ============================================================
-- FIM DO SCRIPT
-- ============================================================