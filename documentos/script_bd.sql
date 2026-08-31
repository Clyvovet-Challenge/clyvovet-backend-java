-- ============================================================================
-- CLYVOVET -- Script completo do banco de dados (Oracle)
--
-- GERADO A PARTIR DAS MIGRATIONS, nao escrito a mao. A fonte da verdade e
-- src/main/resources/db/migration/oracle/, aplicada pelo Flyway; este arquivo
-- e a consolidacao delas num lugar so, para a entrega da disciplina de DevOps.
--
-- Um DDL mantido a mao em paralelo divergiria do banco no primeiro ALTER que
-- alguem esquecesse de replicar -- e ai o script serviria para enganar, nao
-- para documentar. Para regerar:
--
--     python scripts/gerar-script-bd.py
--
-- O EQUIVALENTE EM MYSQL esta em db/migration/mysql/, com as mesmas versoes.
-- Os dois conjuntos sao espelhos, e o MigrationsMySqlTest quebra se um deles
-- ficar para tras.
-- ============================================================================

-- ========================================================================
-- V1__schema_inicial
-- ========================================================================

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


-- ========================================================================
-- V2__seed_inicial
-- ========================================================================

-- ============================================================
-- V2 — Carga inicial
--
-- UUIDs fixos em vez da funcao fn_uuid do script original:
--   * o SQL fica portavel entre Oracle e H2 (sem PL/SQL);
--   * as FKs sao resolvidas por literal, sem blocos DECLARE;
--   * os testes tem dados deterministicos para asserir.
--
-- Datas em literal ANSI (DATE 'aaaa-mm-dd'), aceito pelos dois bancos.
-- Minimo de 5 registros por tabela, exigencia da disciplina de banco.
-- ============================================================

-- ------------------------------------------------------------
-- CLINICAS (5)
-- ------------------------------------------------------------
INSERT INTO clinica (id, nome, cnpj, telefone, email, rua, numero, bairro, cidade, estado, cep) VALUES
('11111111-1111-1111-1111-000000000001', 'VetCare Prime', '12345678000191', '1131000001', 'contato@vetcareprime.com.br', 'Av. Paulista', '1000', 'Bela Vista', 'Sao Paulo', 'SP', '01310100');
INSERT INTO clinica (id, nome, cnpj, telefone, email, rua, numero, bairro, cidade, estado, cep) VALUES
('11111111-1111-1111-1111-000000000002', 'PetMed Centro', '23456789000102', '1131000002', 'contato@petmed.com.br', 'R. Augusta', '420', 'Consolacao', 'Sao Paulo', 'SP', '01304000');
INSERT INTO clinica (id, nome, cnpj, telefone, email, rua, numero, bairro, cidade, estado, cep) VALUES
('11111111-1111-1111-1111-000000000003', 'AnimalSaude SP', '34567890000113', '1131000003', 'contato@animalsaude.com.br', 'R. Oscar Freire', '88', 'Jardins', 'Sao Paulo', 'SP', '01426001');
INSERT INTO clinica (id, nome, cnpj, telefone, email, rua, numero, bairro, cidade, estado, cep) VALUES
('11111111-1111-1111-1111-000000000004', 'CliniPet Jardins', '45678901000124', '1131000004', 'contato@clinipet.com.br', 'Al. Santos', '200', 'Jardim Paulista', 'Sao Paulo', 'SP', '01419001');
INSERT INTO clinica (id, nome, cnpj, telefone, email, rua, numero, bairro, cidade, estado, cep) VALUES
('11111111-1111-1111-1111-000000000005', 'Hospital Vet Ipiranga', '56789012000135', '1131000005', 'contato@hvipiranga.com.br', 'Av. Nazare', '1500', 'Ipiranga', 'Sao Paulo', 'SP', '04262001');

-- ------------------------------------------------------------
-- TUTORES (5)
-- ------------------------------------------------------------
INSERT INTO tutor (id, nome, cpf, telefone, data_nascimento, genero, rua, numero, bairro, cidade, estado, cep, email) VALUES
('22222222-2222-2222-2222-000000000001', 'Lucas M. Santos', '11100011100', '11980000001', DATE '1990-05-10', 'MASCULINO', 'R. Haddock Lobo', '595', 'Cerqueira Cesar', 'Sao Paulo', 'SP', '01414002', 'lucas.santos@email.com');
INSERT INTO tutor (id, nome, cpf, telefone, data_nascimento, genero, rua, numero, bairro, cidade, estado, cep, email) VALUES
('22222222-2222-2222-2222-000000000002', 'Maria Oliveira', '22200022200', '11970000002', DATE '1985-08-22', 'FEMININO', 'R. Estados Unidos', '1000', 'Jardins', 'Sao Paulo', 'SP', '01427002', 'maria.oliveira@email.com');
INSERT INTO tutor (id, nome, cpf, telefone, data_nascimento, genero, rua, numero, bairro, cidade, estado, cep, email) VALUES
('22222222-2222-2222-2222-000000000003', 'Carlos Eduardo Lima', '33300033300', '11960000003', DATE '1978-02-14', 'MASCULINO', 'R. Vergueiro', '2200', 'Vila Mariana', 'Sao Paulo', 'SP', '04101000', 'carlos.lima@email.com');
INSERT INTO tutor (id, nome, cpf, telefone, data_nascimento, genero, rua, numero, bairro, cidade, estado, cep, email) VALUES
('22222222-2222-2222-2222-000000000004', 'Ana Paula Ribeiro', '44400044400', '11950000004', DATE '1995-11-30', 'FEMININO', 'Av. Ibirapuera', '300', 'Moema', 'Sao Paulo', 'SP', '04029000', 'ana.ribeiro@email.com');
INSERT INTO tutor (id, nome, cpf, telefone, data_nascimento, genero, rua, numero, bairro, cidade, estado, cep, email) VALUES
('22222222-2222-2222-2222-000000000005', 'Fernanda Souza', '55500055500', '11940000005', DATE '1992-07-05', 'FEMININO', 'R. Domingos de Morais', '900', 'Vila Mariana', 'Sao Paulo', 'SP', '04010100', 'fernanda.souza@email.com');

-- ------------------------------------------------------------
-- VETERINARIOS (7)
-- ------------------------------------------------------------
INSERT INTO veterinario (id, nome, crmv, especialidade, email, cpf, telefone, genero, data_nascimento, clinica_id, rua, numero, bairro, cidade, estado, cep) VALUES
('33333333-3333-3333-3333-000000000001', 'Camila Ferreira', 'CRMV-SP 14320', 'Clinica Geral', 'camila.ferreira@vetcare.com.br', '11122233344', '11990010001', 'FEMININO', DATE '1985-03-15', '11111111-1111-1111-1111-000000000001', 'Av. Paulista', '1500', 'Bela Vista', 'Sao Paulo', 'SP', '01310200');
INSERT INTO veterinario (id, nome, crmv, especialidade, email, cpf, telefone, genero, data_nascimento, clinica_id, rua, numero, bairro, cidade, estado, cep) VALUES
('33333333-3333-3333-3333-000000000002', 'Rafael Matos', 'CRMV-SP 18741', 'Cardiologia', 'rafael.matos@petmed.com.br', '22233344455', '11990010002', 'MASCULINO', DATE '1980-07-22', '11111111-1111-1111-1111-000000000002', 'R. Augusta', '500', 'Consolacao', 'Sao Paulo', 'SP', '01305000');
INSERT INTO veterinario (id, nome, crmv, especialidade, email, cpf, telefone, genero, data_nascimento, clinica_id, rua, numero, bairro, cidade, estado, cep) VALUES
('33333333-3333-3333-3333-000000000003', 'Andre Costa', 'CRMV-SP 9812', 'Ortopedia', 'andre.costa@animalsaude.com.br', '33344455566', '11990010003', 'MASCULINO', DATE '1978-11-05', '11111111-1111-1111-1111-000000000003', 'R. Oscar Freire', '90', 'Jardins', 'Sao Paulo', 'SP', '01426002');
INSERT INTO veterinario (id, nome, crmv, especialidade, email, cpf, telefone, genero, data_nascimento, clinica_id, rua, numero, bairro, cidade, estado, cep) VALUES
('33333333-3333-3333-3333-000000000004', 'Livia Rocha', 'CRMV-SP 16540', 'Dermatologia', 'livia.rocha@clinipet.com.br', '44455566677', '11990010004', 'FEMININO', DATE '1990-09-18', '11111111-1111-1111-1111-000000000004', 'Al. Santos', '300', 'Jardim Paulista', 'Sao Paulo', 'SP', '01419002');
INSERT INTO veterinario (id, nome, crmv, especialidade, email, cpf, telefone, genero, data_nascimento, clinica_id, rua, numero, bairro, cidade, estado, cep) VALUES
('33333333-3333-3333-3333-000000000005', 'Tomas Oliveira', 'CRMV-SP 11204', 'Clinica Geral', 'tomas.oliveira@vetcare.com.br', '55566677788', '11990010005', 'MASCULINO', DATE '1982-01-30', '11111111-1111-1111-1111-000000000001', 'Av. Paulista', '1200', 'Bela Vista', 'Sao Paulo', 'SP', '01310300');
INSERT INTO veterinario (id, nome, crmv, especialidade, email, cpf, telefone, genero, data_nascimento, clinica_id, rua, numero, bairro, cidade, estado, cep) VALUES
('33333333-3333-3333-3333-000000000006', 'Beatriz Lima', 'CRMV-SP 20333', 'Oncologia', 'beatriz.lima@petmed.com.br', '66677788899', '11990010006', 'FEMININO', DATE '1992-06-14', '11111111-1111-1111-1111-000000000002', 'R. Augusta', '600', 'Consolacao', 'Sao Paulo', 'SP', '01305100');
INSERT INTO veterinario (id, nome, crmv, especialidade, email, cpf, telefone, genero, data_nascimento, clinica_id, rua, numero, bairro, cidade, estado, cep) VALUES
('33333333-3333-3333-3333-000000000007', 'Felipe Souza', 'CRMV-SP 25101', 'Nutricao Animal', 'felipe.souza@animalsaude.com.br', '77788899900', '11990010007', 'MASCULINO', DATE '1995-04-09', '11111111-1111-1111-1111-000000000003', 'R. Oscar Freire', '100', 'Jardins', 'Sao Paulo', 'SP', '01426003');

-- ------------------------------------------------------------
-- ANIMAIS (6) — distribuidos entre tutores distintos, o que
-- permite testar o isolamento por tutor (ownership).
-- ------------------------------------------------------------
INSERT INTO animal (id, nome, especie, raca, porte, cor, genero, data_nascimento, observacoes, tutor_id) VALUES
('44444444-4444-4444-4444-000000000001', 'Bolinha', 'CAO', 'Golden Retriever', 'GRANDE', 'Dourado', 'MACHO', DATE '2022-03-12', 'Cachorro brincalhao e afetivo', '22222222-2222-2222-2222-000000000001');
INSERT INTO animal (id, nome, especie, raca, porte, cor, genero, data_nascimento, observacoes, tutor_id) VALUES
('44444444-4444-4444-4444-000000000002', 'Mimi', 'GATO', 'Siames', 'PEQUENO', 'Bege e marrom', 'FEMEA', DATE '2021-07-05', 'Gata independente', '22222222-2222-2222-2222-000000000002');
INSERT INTO animal (id, nome, especie, raca, porte, cor, genero, data_nascimento, observacoes, tutor_id) VALUES
('44444444-4444-4444-4444-000000000003', 'Rex', 'CAO', 'Pastor Alemao', 'GRANDE', 'Preto e marrom', 'MACHO', DATE '2020-01-18', 'Cao de guarda, obediente', '22222222-2222-2222-2222-000000000002');
INSERT INTO animal (id, nome, especie, raca, porte, cor, genero, data_nascimento, observacoes, tutor_id) VALUES
('44444444-4444-4444-4444-000000000004', 'Nina', 'GATO', 'Persa', 'PEQUENO', 'Branco', 'FEMEA', DATE '2023-04-02', 'Precisa de escovacao frequente', '22222222-2222-2222-2222-000000000003');
INSERT INTO animal (id, nome, especie, raca, porte, cor, genero, data_nascimento, observacoes, tutor_id) VALUES
('44444444-4444-4444-4444-000000000005', 'Thor', 'CAO', 'Bulldog Frances', 'MEDIO', 'Cinza', 'MACHO', DATE '2021-10-25', 'Historico de dermatite', '22222222-2222-2222-2222-000000000004');
INSERT INTO animal (id, nome, especie, raca, porte, cor, genero, data_nascimento, observacoes, tutor_id) VALUES
('44444444-4444-4444-4444-000000000006', 'Luna', 'CAO', 'Border Collie', 'MEDIO', 'Preto e branco', 'FEMEA', DATE '2022-09-08', 'Muito ativa, precisa de exercicio diario', '22222222-2222-2222-2222-000000000005');

-- ------------------------------------------------------------
-- EVENTOS CLINICOS (11)
-- ------------------------------------------------------------
INSERT INTO evento_clinico (id, data_evento, hora_evento, tipo_evento, descricao, veterinario_id, animal_id, clinica_id) VALUES
('55555555-5555-5555-5555-000000000001', DATE '2024-01-10', '09:00', 'CONSULTA', 'Check-up anual de rotina', '33333333-3333-3333-3333-000000000001', '44444444-4444-4444-4444-000000000001', '11111111-1111-1111-1111-000000000001');
INSERT INTO evento_clinico (id, data_evento, hora_evento, tipo_evento, descricao, veterinario_id, animal_id, clinica_id) VALUES
('55555555-5555-5555-5555-000000000002', DATE '2024-02-15', '10:00', 'VACINA', 'V10 - Vacina polivalente anual', '33333333-3333-3333-3333-000000000001', '44444444-4444-4444-4444-000000000001', '11111111-1111-1111-1111-000000000001');
INSERT INTO evento_clinico (id, data_evento, hora_evento, tipo_evento, descricao, veterinario_id, animal_id, clinica_id) VALUES
('55555555-5555-5555-5555-000000000003', DATE '2024-03-20', '14:00', 'EXAME', 'Hemograma completo e bioquimica', '33333333-3333-3333-3333-000000000005', '44444444-4444-4444-4444-000000000001', '11111111-1111-1111-1111-000000000001');
INSERT INTO evento_clinico (id, data_evento, hora_evento, tipo_evento, descricao, veterinario_id, animal_id, clinica_id) VALUES
('55555555-5555-5555-5555-000000000004', DATE '2024-06-05', '11:00', 'RETORNO', 'Retorno pos-exame, resultados normais', '33333333-3333-3333-3333-000000000001', '44444444-4444-4444-4444-000000000001', '11111111-1111-1111-1111-000000000001');
INSERT INTO evento_clinico (id, data_evento, hora_evento, tipo_evento, descricao, veterinario_id, animal_id, clinica_id) VALUES
('55555555-5555-5555-5555-000000000005', DATE '2024-09-10', '09:30', 'VACINA', 'Antirabica anual', '33333333-3333-3333-3333-000000000005', '44444444-4444-4444-4444-000000000001', '11111111-1111-1111-1111-000000000001');
INSERT INTO evento_clinico (id, data_evento, hora_evento, tipo_evento, descricao, veterinario_id, animal_id, clinica_id) VALUES
('55555555-5555-5555-5555-000000000006', DATE '2026-12-15', '10:00', 'CONSULTA', 'Check-up e vermifugacao', '33333333-3333-3333-3333-000000000001', '44444444-4444-4444-4444-000000000001', '11111111-1111-1111-1111-000000000001');
INSERT INTO evento_clinico (id, data_evento, hora_evento, tipo_evento, descricao, veterinario_id, animal_id, clinica_id) VALUES
('55555555-5555-5555-5555-000000000007', DATE '2024-02-20', '15:00', 'CONSULTA', 'Consulta de rotina', '33333333-3333-3333-3333-000000000004', '44444444-4444-4444-4444-000000000002', '11111111-1111-1111-1111-000000000004');
INSERT INTO evento_clinico (id, data_evento, hora_evento, tipo_evento, descricao, veterinario_id, animal_id, clinica_id) VALUES
('55555555-5555-5555-5555-000000000008', DATE '2024-04-15', '16:00', 'VACINA', 'Vacina triplice felina', '33333333-3333-3333-3333-000000000004', '44444444-4444-4444-4444-000000000002', '11111111-1111-1111-1111-000000000004');
INSERT INTO evento_clinico (id, data_evento, hora_evento, tipo_evento, descricao, veterinario_id, animal_id, clinica_id) VALUES
('55555555-5555-5555-5555-000000000009', DATE '2026-12-22', '14:00', 'EXAME', 'Exame de urina e sangue', '33333333-3333-3333-3333-000000000002', '44444444-4444-4444-4444-000000000002', '11111111-1111-1111-1111-000000000002');
INSERT INTO evento_clinico (id, data_evento, hora_evento, tipo_evento, descricao, veterinario_id, animal_id, clinica_id) VALUES
('55555555-5555-5555-5555-000000000010', DATE '2024-03-08', '08:00', 'CIRURGIA', 'Cirurgia de castracao', '33333333-3333-3333-3333-000000000003', '44444444-4444-4444-4444-000000000003', '11111111-1111-1111-1111-000000000003');
INSERT INTO evento_clinico (id, data_evento, hora_evento, tipo_evento, descricao, veterinario_id, animal_id, clinica_id) VALUES
('55555555-5555-5555-5555-000000000011', DATE '2024-03-25', '09:00', 'RETORNO', 'Retorno pos-cirurgico', '33333333-3333-3333-3333-000000000003', '44444444-4444-4444-4444-000000000003', '11111111-1111-1111-1111-000000000003');

-- ------------------------------------------------------------
-- PAGAMENTOS (8)
-- ------------------------------------------------------------
INSERT INTO pagamento (id, metodo_pagamento, valor, status_pagamento, data_pagamento, descricao, evento_id) VALUES
('66666666-6666-6666-6666-000000000001', 'PIX', 150.00, 'PAGO', DATE '2024-01-10', 'Consulta de rotina', '55555555-5555-5555-5555-000000000001');
INSERT INTO pagamento (id, metodo_pagamento, valor, status_pagamento, data_pagamento, descricao, evento_id) VALUES
('66666666-6666-6666-6666-000000000002', 'CARTAO', 80.00, 'PAGO', DATE '2024-02-15', 'Vacina V10', '55555555-5555-5555-5555-000000000002');
INSERT INTO pagamento (id, metodo_pagamento, valor, status_pagamento, data_pagamento, descricao, evento_id) VALUES
('66666666-6666-6666-6666-000000000003', 'DINHEIRO', 200.00, 'PAGO', DATE '2024-03-20', 'Hemograma e bioquimica', '55555555-5555-5555-5555-000000000003');
INSERT INTO pagamento (id, metodo_pagamento, valor, status_pagamento, data_pagamento, descricao, evento_id) VALUES
('66666666-6666-6666-6666-000000000004', 'PIX', 120.00, 'PENDENTE', NULL, 'Retorno Bolinha', '55555555-5555-5555-5555-000000000004');
INSERT INTO pagamento (id, metodo_pagamento, valor, status_pagamento, data_pagamento, descricao, evento_id) VALUES
('66666666-6666-6666-6666-000000000005', 'CARTAO', 100.00, 'PAGO', DATE '2024-02-20', 'Consulta Mimi', '55555555-5555-5555-5555-000000000007');
INSERT INTO pagamento (id, metodo_pagamento, valor, status_pagamento, data_pagamento, descricao, evento_id) VALUES
('66666666-6666-6666-6666-000000000006', 'PIX', 90.00, 'PENDENTE', NULL, 'Vacina felina Mimi', '55555555-5555-5555-5555-000000000008');
INSERT INTO pagamento (id, metodo_pagamento, valor, status_pagamento, data_pagamento, descricao, evento_id) VALUES
('66666666-6666-6666-6666-000000000007', 'BOLETO', 800.00, 'PAGO', DATE '2024-03-08', 'Cirurgia castracao Rex', '55555555-5555-5555-5555-000000000010');
INSERT INTO pagamento (id, metodo_pagamento, valor, status_pagamento, data_pagamento, descricao, evento_id) VALUES
('66666666-6666-6666-6666-000000000008', 'PIX', 150.00, 'CANCELADO', NULL, 'Retorno cancelado', '55555555-5555-5555-5555-000000000011');


-- ========================================================================
-- V3__usuario_e_perfis
-- ========================================================================

-- ============================================================
-- V3 — Identidade e controle de acesso
--
-- A tabela usuario separa IDENTIDADE (quem faz login) de DOMINIO
-- (tutor, veterinario). O vinculo e opcional dos dois lados:
--   * perfil TUTOR       -> tutor_id preenchido
--   * perfil VETERINARIO -> veterinario_id preenchido
--   * perfil ADMIN       -> nenhum dos dois
--
-- E o vinculo com tutor que viabiliza a regra de ownership:
-- um tutor so enxerga os proprios pets.
--
-- Nao ha usuario semeado aqui de proposito: hash de senha nao
-- deve ser versionado. Os usuarios de desenvolvimento sao criados
-- por DevDataSeeder, ativo apenas nos perfis dev e h2.
-- ============================================================

CREATE TABLE usuario (
    id                VARCHAR2(36)  PRIMARY KEY,
    email             VARCHAR2(200) NOT NULL,
    senha             VARCHAR2(100) NOT NULL,
    perfil            VARCHAR2(20)  NOT NULL,
    ativo             NUMBER(1)     DEFAULT 1 NOT NULL,
    tentativas_falhas NUMBER(3)     DEFAULT 0 NOT NULL,
    bloqueado_ate     TIMESTAMP,
    tutor_id          VARCHAR2(36),
    veterinario_id    VARCHAR2(36),
    CONSTRAINT uk_usuario_email    UNIQUE (email),
    CONSTRAINT fk_usuario_tutor    FOREIGN KEY (tutor_id)       REFERENCES tutor(id),
    CONSTRAINT fk_usuario_vet      FOREIGN KEY (veterinario_id) REFERENCES veterinario(id),
    CONSTRAINT chk_usuario_perfil  CHECK (perfil IN ('TUTOR','VETERINARIO','ADMIN')),
    CONSTRAINT chk_usuario_ativo   CHECK (ativo IN (0,1))
);

CREATE INDEX idx_usuario_email ON usuario (email);


-- ========================================================================
-- V4__corrige_status_pagamento
-- ========================================================================

-- ============================================================
-- V4 — Alinha o check de status_pagamento ao enum StatusPagamento
--
-- O schema original aceitava 'ESTORNADO', mas o enum Java declara
-- 'REEMBOLSADO'. Na pratica era impossivel gravar um pagamento
-- reembolsado: a requisicao passava na validacao, chegava ao INSERT
-- e estourava ORA-02290 (check constraint violated), devolvendo 500.
--
-- Esta migration roda tambem nos bancos que entraram por baseline,
-- que e onde a divergencia realmente existe.
-- ============================================================

UPDATE pagamento SET status_pagamento = 'REEMBOLSADO' WHERE status_pagamento = 'ESTORNADO';

ALTER TABLE pagamento DROP CONSTRAINT chk_status_pagamento;

ALTER TABLE pagamento ADD CONSTRAINT chk_status_pagamento
    CHECK (status_pagamento IN ('PENDENTE','PAGO','CANCELADO','REEMBOLSADO'));


-- ========================================================================
-- V5__evento_status_e_retorno
-- ========================================================================

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


-- ========================================================================
-- V6__catalogo_agenda_e_alertas
-- ========================================================================

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
    -- ON DELETE CASCADE: estas linhas nao tem vida propria sem o animal.
    -- Sem isso, DELETE /animais/{id} passaria a falhar em todo animal que
    -- ja tivesse alerta, autorizacao ou acesso registrado -- e o erro
    -- chegaria como 409 generico de integridade, sem dizer o que travou.
    CONSTRAINT fk_alerta_animal FOREIGN KEY (animal_id)
        REFERENCES animal(id) ON DELETE CASCADE,
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


-- ========================================================================
-- V7__consentimento_e_auditoria
-- ========================================================================

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
