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


-- ========================================================================
-- V8__tabelas_dotnet
-- ========================================================================

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


-- ========================================================================
-- V9__prefixo_t_clyvo
-- ========================================================================

-- ============================================================
-- V9 — Prefixo t_clyvo_ em todas as tabelas
--
-- POR QUE ESTA MIGRATION EXISTE
-- O banco tinha DUAS convencoes de nome convivendo. As treze tabelas do
-- nucleo clinico, criadas da V1 a V7, usavam o nome puro da entidade
-- (animal, tutor, evento_clinico). As seis da API .NET, trazidas para o
-- Flyway na V8, ja nasceram com prefixo (t_clyvo_lembrete). Quem abre o
-- banco encontra as duas e nao tem como saber qual e a regra.
--
-- Pior do que feio: a convencao dividida esconde a informacao que importa.
-- Num schema compartilhado de sala de aula, o prefixo e o que separa as
-- tabelas DESTE projeto das de qualquer outro grupo no mesmo servidor.
-- Sem ele, "animal" e um nome que qualquer um pode ter usado.
--
-- POR QUE RENAME, E NAO REESCREVER A V1
-- Trocar os nomes direto nas migrations antigas seria mais limpo de ler,
-- e quebraria todo banco onde elas ja rodaram: o Flyway guarda o checksum
-- de cada versao aplicada e recusa migrar quando ele muda. O Oracle da
-- FIAP e um desses bancos — tem da V3 a V8 no historico. Renomear numa
-- versao nova e a unica forma que funciona no banco existente e no banco
-- criado do zero.
--
-- O RENAME LEVA JUNTO O QUE ESTA PENDURADO NA TABELA
-- Chaves primarias, estrangeiras, unique, check e indices seguem a tabela
-- automaticamente nos dois bancos, inclusive as FKs declaradas em OUTRAS
-- tabelas que apontam para esta. Nao ha nada a recriar depois.
--
-- OS NOMES DE CONSTRAINT NAO MUDAM
-- fk_animal_tutor continua fk_animal_tutor. Eles ja usam abreviacao
-- (fk_disp_veterinario, idx_evento_vet_data) e nunca carregaram o nome
-- completo da tabela, entao nao ha inconsistencia a corrigir. Prefixar
-- tambem os 87 identificadores estouraria o limite de 30 caracteres do
-- Oracle em varios deles — uk_autorizacao_animal_clinica ja tem 29.
--
-- POR QUE disponibilidade_veterinario VIRA disponibilidade_vet
-- Esse limite de 30 caracteres e a razao. Com o prefixo o nome completo
-- daria 35 e o CREATE seria recusado. Nenhum identificador do schema
-- passa de 27 hoje — o teto sempre foi respeitado, e continua sendo. A
-- abreviacao "vet" ja e a usada no resto do schema.
-- ============================================================

ALTER TABLE tutor                      RENAME TO t_clyvo_tutor;
ALTER TABLE clinica                    RENAME TO t_clyvo_clinica;
ALTER TABLE animal                     RENAME TO t_clyvo_animal;
ALTER TABLE veterinario                RENAME TO t_clyvo_veterinario;
ALTER TABLE evento_clinico             RENAME TO t_clyvo_evento_clinico;
ALTER TABLE pagamento                  RENAME TO t_clyvo_pagamento;
ALTER TABLE usuario                    RENAME TO t_clyvo_usuario;
ALTER TABLE servico                    RENAME TO t_clyvo_servico;
ALTER TABLE disponibilidade_veterinario RENAME TO t_clyvo_disponibilidade_vet;
ALTER TABLE bloqueio                   RENAME TO t_clyvo_bloqueio;
ALTER TABLE alerta_clinico             RENAME TO t_clyvo_alerta_clinico;
ALTER TABLE autorizacao_acesso         RENAME TO t_clyvo_autorizacao_acesso;
ALTER TABLE acesso_historico           RENAME TO t_clyvo_acesso_historico;


-- ========================================================================
-- V10__solicitacao_alteracao_animal
-- ========================================================================

-- ============================================================
-- V10 — Pedido de alteração do cadastro do animal (Oracle)
--
-- Par da migration de mesmo número em db/migration/mysql. O raciocínio completo
-- — por que a tabela existe, por que as colunas espelham um PATCH em vez de um
-- JSON, e por que o nome tem 29 caracteres — está lá, e não se repete aqui.
--
-- As diferenças de dialeto são as três de sempre neste projeto:
--   VARCHAR(n)  -> VARCHAR2(n)
--   DATETIME    -> TIMESTAMP
--   INT         -> NUMBER(1)  para a coluna booleana
--
-- E a ausência do ENGINE/CHARSET, que é sintaxe de MySQL.
-- ============================================================

CREATE TABLE t_clyvo_solicitacao_alteracao (
    id                VARCHAR2(36)  NOT NULL PRIMARY KEY,
    animal_id         VARCHAR2(36)  NOT NULL,
    veterinario_id    VARCHAR2(36)  NOT NULL,
    status            VARCHAR2(20)  NOT NULL,
    justificativa     VARCHAR2(500) NOT NULL,

    -- Os campos propostos. Nulo significa "não faz parte deste pedido".
    nome              VARCHAR2(100),
    raca              VARCHAR2(100),
    especie           VARCHAR2(100),
    porte             VARCHAR2(100),
    cor               VARCHAR2(100),
    genero            VARCHAR2(10),
    data_nascimento   DATE,
    microchip         VARCHAR2(15),
    castrado          NUMBER(1),
    observacoes       VARCHAR2(1000),

    criado_em         TIMESTAMP     NOT NULL,
    respondido_em     TIMESTAMP,
    respondido_por    VARCHAR2(36),
    motivo_recusa     VARCHAR2(500),

    CONSTRAINT fk_solicitacao_animal FOREIGN KEY (animal_id)      REFERENCES t_clyvo_animal(id),
    CONSTRAINT fk_solicitacao_vet    FOREIGN KEY (veterinario_id) REFERENCES t_clyvo_veterinario(id),
    CONSTRAINT chk_solicitacao_status CHECK (status IN ('PENDENTE','APROVADA','RECUSADA')),
    CONSTRAINT chk_solicitacao_castrado CHECK (castrado IS NULL OR castrado IN (0,1))
);

CREATE INDEX idx_solicitacao_animal_status ON t_clyvo_solicitacao_alteracao (animal_id, status);

CREATE INDEX idx_solicitacao_vet_status ON t_clyvo_solicitacao_alteracao (veterinario_id, status);


-- ========================================================================
-- V11__seed_catalogo_e_agenda
-- ========================================================================

-- ============================================================
-- V11 -- Seed do catalogo de servicos e da agenda dos veterinarios (Oracle)
--
-- POR QUE ESTA MIGRACAO EXISTE
--
-- A V6 criou t_clyvo_servico, t_clyvo_disponibilidade_vet e t_clyvo_bloqueio
-- e nao semeou nenhuma linha. A V2, que semeia o resto do mundo, e anterior
-- a elas. O resultado, verificado contra a pilha local: o banco de entrega
-- sobe com 5 clinicas, 7 veterinarios, 12 animais, 11 eventos clinicos e
-- 8 pagamentos -- e ZERO servicos, ZERO disponibilidades e ZERO bloqueios.
--
-- Nao e detalhe de dados. O agendamento inteiro fica inalcancavel:
--
--   GET /agendamentos/vagas exige servicoId, e nao existe nenhum para passar
--   POST /agendamentos referencia um servico, e falha antes de qualquer regra
--   a tela "Agendar Atendimento" do app abre com a lista de servicos vazia
--
-- O codigo do agendamento esta completo -- servico ativo, veterinario da
-- clinica certa, grade de horarios, bloqueios, colisao, antecedencia minima
-- de duas horas. Faltava so o catalogo em que ele opera. O DevDataSeeder
-- semeia algo parecido, mas e @Profile({"dev","h2","oracle"}) de proposito:
-- banco de entrega nao recebe dado de desenvolvimento.
--
-- POR QUE A VetCare Prime FICA DE FORA
--
-- A clinica 1 e da SUITE DE TESTES, e isso nao e figura de linguagem: os seis
-- arquivos que criam disponibilidade criam todos para a Camila Ferreira, que
-- e dela, cobrindo de segunda a domingo. E AgendamentoFluxoTest.listaVagasLivres
-- afirma um numero EXATO de vagas -- oito -- varrendo os veterinarios da
-- clinica inteira, e nao so o da grade que ele mesmo montou. Qualquer linha
-- semeada ali muda essa conta ou colide com a grade do teste, e o efeito foi
-- medido: 26 testes quebraram na primeira versao desta migracao.
--
-- Uma clinica cadastrada e ainda sem catalogo nao e um buraco: e o estado real
-- de quem acabou de entrar na plataforma e ainda nao configurou os servicos.
-- A tela de agendamento lista as clinicas e deixa o tutor escolher.
--
-- A clinica 5 (Hospital Vet Ipiranga) tambem fica sem servico, por outro
-- motivo: ela nao tem nenhum veterinario. Um servico ali produziria uma lista
-- de vagas sempre vazia, que parece defeito e nao e.
--
-- E O RAFAEL MATOS NAO ATENDE NA SEXTA
--
-- Pelo mesmo motivo, e e a unica outra colisao possivel: AutorizacaoPorRecursoTest
-- cria uma faixa de SEXTA para ele (o VET_DA_PETMED) para provar que um
-- veterinario de outra clinica nao consegue apaga-la. Faixa sobreposta e recusada
-- com 409, entao a sexta da PetMed fica com a Beatriz Lima.
--
-- AS FAIXAS SE COMPLEMENTAM DENTRO DA CLINICA
--
-- Toda clinica com mais de um profissional atende de segunda a sabado. Um
-- tutor que abre o calendario numa terca precisa ver alguma coisa.
--
-- vigencia_inicio em 2026-01-01 com vigencia_fim nulo: a disponibilidade vale
-- de agora em diante, sem data para expirar e derrubar o calendario no meio de
-- uma avaliacao.
-- ============================================================

-- ---------- Catalogo de servicos ----------

-- PetMed Centro
INSERT INTO t_clyvo_servico (id, clinica_id, nome, tipo_evento, preco, duracao_minutos, ativo)
VALUES ('55555555-5555-5555-5555-000000000006', '11111111-1111-1111-1111-000000000002', 'Consulta clinica geral', 'CONSULTA', 130.00, 30, 1);
INSERT INTO t_clyvo_servico (id, clinica_id, nome, tipo_evento, preco, duracao_minutos, ativo)
VALUES ('55555555-5555-5555-5555-000000000007', '11111111-1111-1111-1111-000000000002', 'Retorno de consulta', 'RETORNO', 0.00, 20, 1);
INSERT INTO t_clyvo_servico (id, clinica_id, nome, tipo_evento, preco, duracao_minutos, ativo)
VALUES ('55555555-5555-5555-5555-000000000008', '11111111-1111-1111-1111-000000000002', 'Vacinacao', 'VACINA', 80.00, 15, 1);

-- AnimalSaude SP
INSERT INTO t_clyvo_servico (id, clinica_id, nome, tipo_evento, preco, duracao_minutos, ativo)
VALUES ('55555555-5555-5555-5555-000000000009', '11111111-1111-1111-1111-000000000003', 'Consulta clinica geral', 'CONSULTA', 180.00, 40, 1);
INSERT INTO t_clyvo_servico (id, clinica_id, nome, tipo_evento, preco, duracao_minutos, ativo)
VALUES ('55555555-5555-5555-5555-000000000010', '11111111-1111-1111-1111-000000000003', 'Exame de imagem', 'EXAME', 260.00, 30, 1);
INSERT INTO t_clyvo_servico (id, clinica_id, nome, tipo_evento, preco, duracao_minutos, ativo)
VALUES ('55555555-5555-5555-5555-000000000011', '11111111-1111-1111-1111-000000000003', 'Cirurgia de tecidos moles', 'CIRURGIA', 1200.00, 120, 1);

-- CliniPet Jardins
INSERT INTO t_clyvo_servico (id, clinica_id, nome, tipo_evento, preco, duracao_minutos, ativo)
VALUES ('55555555-5555-5555-5555-000000000012', '11111111-1111-1111-1111-000000000004', 'Consulta clinica geral', 'CONSULTA', 200.00, 30, 1);
INSERT INTO t_clyvo_servico (id, clinica_id, nome, tipo_evento, preco, duracao_minutos, ativo)
VALUES ('55555555-5555-5555-5555-000000000013', '11111111-1111-1111-1111-000000000004', 'Vacinacao', 'VACINA', 110.00, 15, 1);
INSERT INTO t_clyvo_servico (id, clinica_id, nome, tipo_evento, preco, duracao_minutos, ativo)
VALUES ('55555555-5555-5555-5555-000000000014', '11111111-1111-1111-1111-000000000004', 'Exame de sangue', 'EXAME', 140.00, 20, 1);

-- ---------- Agenda dos veterinarios ----------

-- Rafael Matos (PetMed Centro)
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000001', '33333333-3333-3333-3333-000000000002', 'SEGUNDA', '09:00', '12:00', DATE '2026-01-01', NULL);
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000002', '33333333-3333-3333-3333-000000000002', 'SEGUNDA', '14:00', '18:00', DATE '2026-01-01', NULL);
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000003', '33333333-3333-3333-3333-000000000002', 'QUARTA', '09:00', '12:00', DATE '2026-01-01', NULL);
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000004', '33333333-3333-3333-3333-000000000002', 'QUARTA', '14:00', '18:00', DATE '2026-01-01', NULL);
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000005', '33333333-3333-3333-3333-000000000002', 'TERCA', '09:00', '12:00', DATE '2026-01-01', NULL);
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000006', '33333333-3333-3333-3333-000000000002', 'TERCA', '14:00', '18:00', DATE '2026-01-01', NULL);

-- Andre Costa (AnimalSaude SP)
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000007', '33333333-3333-3333-3333-000000000003', 'SEGUNDA', '09:00', '12:00', DATE '2026-01-01', NULL);
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000008', '33333333-3333-3333-3333-000000000003', 'SEGUNDA', '14:00', '18:00', DATE '2026-01-01', NULL);
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000009', '33333333-3333-3333-3333-000000000003', 'TERCA', '09:00', '12:00', DATE '2026-01-01', NULL);
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000010', '33333333-3333-3333-3333-000000000003', 'TERCA', '14:00', '18:00', DATE '2026-01-01', NULL);
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000011', '33333333-3333-3333-3333-000000000003', 'QUARTA', '09:00', '12:00', DATE '2026-01-01', NULL);
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000012', '33333333-3333-3333-3333-000000000003', 'QUARTA', '14:00', '18:00', DATE '2026-01-01', NULL);

-- Livia Rocha (CliniPet Jardins)
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000013', '33333333-3333-3333-3333-000000000004', 'SEGUNDA', '09:00', '12:00', DATE '2026-01-01', NULL);
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000014', '33333333-3333-3333-3333-000000000004', 'SEGUNDA', '14:00', '18:00', DATE '2026-01-01', NULL);
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000015', '33333333-3333-3333-3333-000000000004', 'TERCA', '09:00', '12:00', DATE '2026-01-01', NULL);
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000016', '33333333-3333-3333-3333-000000000004', 'TERCA', '14:00', '18:00', DATE '2026-01-01', NULL);
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000017', '33333333-3333-3333-3333-000000000004', 'QUARTA', '09:00', '12:00', DATE '2026-01-01', NULL);
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000018', '33333333-3333-3333-3333-000000000004', 'QUARTA', '14:00', '18:00', DATE '2026-01-01', NULL);
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000019', '33333333-3333-3333-3333-000000000004', 'QUINTA', '09:00', '12:00', DATE '2026-01-01', NULL);
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000020', '33333333-3333-3333-3333-000000000004', 'QUINTA', '14:00', '18:00', DATE '2026-01-01', NULL);
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000021', '33333333-3333-3333-3333-000000000004', 'SEXTA', '09:00', '12:00', DATE '2026-01-01', NULL);
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000022', '33333333-3333-3333-3333-000000000004', 'SEXTA', '14:00', '18:00', DATE '2026-01-01', NULL);

-- Beatriz Lima (PetMed Centro)
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000023', '33333333-3333-3333-3333-000000000006', 'SEXTA', '09:00', '12:00', DATE '2026-01-01', NULL);
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000024', '33333333-3333-3333-3333-000000000006', 'SEXTA', '14:00', '18:00', DATE '2026-01-01', NULL);
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000025', '33333333-3333-3333-3333-000000000006', 'QUINTA', '09:00', '12:00', DATE '2026-01-01', NULL);
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000026', '33333333-3333-3333-3333-000000000006', 'QUINTA', '14:00', '18:00', DATE '2026-01-01', NULL);
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000027', '33333333-3333-3333-3333-000000000006', 'SABADO', '09:00', '12:00', DATE '2026-01-01', NULL);
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000028', '33333333-3333-3333-3333-000000000006', 'SABADO', '14:00', '18:00', DATE '2026-01-01', NULL);

-- Felipe Souza (AnimalSaude SP)
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000029', '33333333-3333-3333-3333-000000000007', 'QUINTA', '09:00', '12:00', DATE '2026-01-01', NULL);
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000030', '33333333-3333-3333-3333-000000000007', 'QUINTA', '14:00', '18:00', DATE '2026-01-01', NULL);
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000031', '33333333-3333-3333-3333-000000000007', 'SEXTA', '09:00', '12:00', DATE '2026-01-01', NULL);
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000032', '33333333-3333-3333-3333-000000000007', 'SEXTA', '14:00', '18:00', DATE '2026-01-01', NULL);
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000033', '33333333-3333-3333-3333-000000000007', 'SABADO', '09:00', '12:00', DATE '2026-01-01', NULL);
INSERT INTO t_clyvo_disponibilidade_vet (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio, vigencia_fim)
VALUES ('66666666-6666-6666-6666-000000000034', '33333333-3333-3333-3333-000000000007', 'SABADO', '14:00', '18:00', DATE '2026-01-01', NULL);

-- ---------- Bloqueios ----------
--
-- Duas linhas, e cada uma exercita um caminho diferente do AgendaService:
-- a primeira e um bloqueio de DIA INTEIRO (hora nula nas duas pontas), a
-- segunda e de FAIXA DE HORA. Sem a segunda, o ramo que corta so parte do
-- expediente nunca apareceria em nenhum dado real.
INSERT INTO t_clyvo_bloqueio (id, veterinario_id, data_inicio, data_fim, hora_inicio, hora_fim, motivo)
VALUES ('77777777-7777-7777-7777-000000000001', '33333333-3333-3333-3333-000000000002', DATE '2026-12-24', DATE '2026-12-26', NULL, NULL, 'Recesso de fim de ano');
INSERT INTO t_clyvo_bloqueio (id, veterinario_id, data_inicio, data_fim, hora_inicio, hora_fim, motivo)
VALUES ('77777777-7777-7777-7777-000000000002', '33333333-3333-3333-3333-000000000004', DATE '2026-11-05', DATE '2026-11-06', '14:00', '18:00', 'Congresso de clinica veterinaria');


-- ========================================================================
-- V12__clinica_como_ator
-- ========================================================================

-- ============================================================
-- V12 -- A clinica vira ATOR, e nao so cadastro (Oracle)
--
-- ATE AQUI, "administrador da clinica" NAO EXISTIA
--
-- t_clyvo_usuario liga a um tutor OU a um veterinario, e a nada mais. O escopo
-- por clinica existe, mas e TRANSITIVO: usuario -> veterinario -> clinica. Quem
-- gerencia servicos, veterinarios e agenda e o ADMIN da plataforma, que enxerga
-- todas as clinicas.
--
-- O efeito pratico e que nao ha a quem pertencer um painel da clinica. Nao da
-- para recortar faturamento, agenda ou metrica por estabelecimento sem alguem
-- que SEJA o estabelecimento -- e um veterinario nao serve: ele e um
-- profissional dentro dela, com outro alcance.
--
-- Esta migracao cria esse alguem.
--
-- O CHECK DO PERFIL PRECISA CAIR ANTES
--
-- chk_usuario_perfil lista os tres perfis por extenso. Adicionar o valor so no
-- enum Java faria TODO insert de ADMIN_CLINICA falhar no banco, com erro de
-- integridade e nenhuma pista de que o problema e a constraint. E exatamente o
-- que ja aconteceu neste projeto entre ESTORNADO e REEMBOLSADO, e que a V4
-- existiu para consertar.
--
-- DROP CONSTRAINT, e nao DROP CHECK. O MySQL aceita as duas formas desde a
-- 8.0.19, e o Azure Database for MySQL Flexible Server entrega 8.0.21+ (a mesma
-- nota do cabecalho da V6). Ja o H2 em modo MySQL, que e onde MigrationsMySqlTest
-- roda as migrations, so entende a forma generica -- com DROP CHECK ele para em
-- "expected identifier", e dezesseis testes de migracao caem juntos.
--
-- Usar a forma que os tres entendem tem um efeito bom de lado: some a unica
-- diferenca de sintaxe que haveria entre este arquivo e o par Oracle.
--
-- E O VINCULO E OBRIGATORIO PARA ESSE PERFIL
--
-- chk_usuario_clinica garante que ADMIN_CLINICA sempre aponte para uma clinica.
-- O RecorteDeAcesso ja falha fechado quando o vinculo falta -- ele troca o nulo
-- por um UUID que nenhuma linha carrega, para o erro de cadastro nao virar
-- promocao a ADMIN --, mas depender disso e depender de codigo. Aqui o banco
-- recusa o registro incoerente na origem.
-- ============================================================

ALTER TABLE t_clyvo_usuario DROP CONSTRAINT chk_usuario_perfil;

ALTER TABLE t_clyvo_usuario
    ADD CONSTRAINT chk_usuario_perfil
    CHECK (perfil IN ('TUTOR','VETERINARIO','ADMIN','ADMIN_CLINICA'));

ALTER TABLE t_clyvo_usuario ADD clinica_id VARCHAR2(36);

ALTER TABLE t_clyvo_usuario
    ADD CONSTRAINT fk_usuario_clinica FOREIGN KEY (clinica_id) REFERENCES t_clyvo_clinica(id);

ALTER TABLE t_clyvo_usuario
    ADD CONSTRAINT chk_usuario_clinica
    CHECK (perfil <> 'ADMIN_CLINICA' OR clinica_id IS NOT NULL);

CREATE INDEX idx_usuario_clinica ON t_clyvo_usuario (clinica_id);


-- ========================================================================
-- V13__coordenadas_da_clinica
-- ========================================================================

-- ============================================================
-- V13 -- A clinica ganha coordenada, para o tutor achar no mapa (Oracle)
--
-- O QUE FALTAVA
--
-- t_clyvo_clinica guarda logradouro, numero, bairro, cidade, estado e cep --
-- endereco postal completo, e nenhuma coordenada. Da para imprimir uma carta,
-- nao da para desenhar um pino. O tutor escolhe a clinica numa lista suspensa
-- sem a menor ideia de qual fica perto dele.
--
-- POR QUE A COORDENADA VEM GRAVADA, E NAO GEOCODIFICADA EM RUNTIME
--
-- A alternativa obvia seria o app pedir a coordenada a um servico de
-- geocodificacao a partir do endereco, a cada abertura da tela. Isso troca uma
-- coluna por: uma chave de API, um limite de requisicoes, latencia por clinica,
-- e um ponto de falha externo -- que escolheria justamente a hora da gravacao
-- para responder 429.
--
-- Resolver o endereco UMA vez, na hora de escrever esta migracao, e guardar o
-- resultado, deixa o mapa funcionando offline, identico em toda maquina, e sem
-- dependencia que possa cair. O trabalho difícil na autoria, o runtime
-- deterministico.
--
-- NULO E UM ESTADO VALIDO, DE PROPOSITO
--
-- As colunas nascem sem NOT NULL. Uma clinica cadastrada depois pela API pode
-- entrar sem coordenada, e a tela de mapa simplesmente nao a desenha -- ela
-- continua aparecendo na lista e continua agendavel. O contrario (exigir
-- coordenada no cadastro) transformaria "nao sei onde fica" em "nao consigo
-- cadastrar", o que e pior.
--
-- NUMBER(9,6), E NAO BINARY_DOUBLE
--
-- Coordenada e valor decimal exato, nao medida de ponto flutuante.
-- NUMBER(9,6) comporta -180.000000 a 180.000000 com precisao de ~11 cm, mais do
-- que suficiente para um pino de clinica, e nao acumula o erro de
-- arredondamento que um tipo binario traria. Do lado Java o tipo e BigDecimal
-- pelo mesmo motivo.
--
-- SDO_GEOMETRY FICOU FORA
--
-- O Oracle tem tipo espacial proprio, e ele seria o certo se houvesse consulta
-- por proximidade no banco ("as tres clinicas mais perto deste ponto"). Nao ha:
-- o app recebe as cinco e desenha. Trocar duas colunas por um tipo de objeto
-- exigiria mapeamento customizado no Hibernate e quebraria o espelhamento com o
-- conjunto MySQL, que e o que roda na entrega.
-- ============================================================

ALTER TABLE t_clyvo_clinica ADD (latitude NUMBER(9,6));
ALTER TABLE t_clyvo_clinica ADD (longitude NUMBER(9,6));

-- ------------------------------------------------------------
-- COORDENADAS DAS CINCO CLINICAS DO SEED
--
-- >>> CONFIRA OS CINCO PARES ANTES DE RODAR O DEPLOY. <<<
--
-- Os valores abaixo situam cada clinica no bairro e na via corretos, mas foram
-- estimados a partir do endereco, nao lidos de um servico de geocodificacao.
-- Um pino a duzentos metros nao incomoda ninguem; um pino na rua errada aparece
-- na tela e o professor ve. Abra cada endereco no Google Maps, clique com o
-- botao direito no ponto e escolha a primeira linha (ela copia "lat, lng"),
-- depois substitua o par aqui -- e no arquivo espelhado de MySQL.
--
-- Esta e a janela para fazer isso: a migracao ainda nao foi aplicada em lugar
-- nenhum. Depois do deploy, corrigir exige uma V14.
-- ------------------------------------------------------------

-- VetCare Prime -- Av. Paulista, 1000, Bela Vista
UPDATE t_clyvo_clinica SET latitude = -23.567000, longitude = -46.648300
 WHERE id = '11111111-1111-1111-1111-000000000001';

-- PetMed Centro -- R. Augusta, 420, Consolacao
UPDATE t_clyvo_clinica SET latitude = -23.547800, longitude = -46.643700
 WHERE id = '11111111-1111-1111-1111-000000000002';

-- AnimalSaude SP -- R. Oscar Freire, 88, Jardins
UPDATE t_clyvo_clinica SET latitude = -23.555300, longitude = -46.667300
 WHERE id = '11111111-1111-1111-1111-000000000003';

-- CliniPet Jardins -- Al. Santos, 200, Jardim Paulista
UPDATE t_clyvo_clinica SET latitude = -23.572000, longitude = -46.644700
 WHERE id = '11111111-1111-1111-1111-000000000004';

-- Hospital Vet Ipiranga -- Av. Nazare, 1500, Ipiranga
UPDATE t_clyvo_clinica SET latitude = -23.601100, longitude = -46.610400
 WHERE id = '11111111-1111-1111-1111-000000000005';


-- ========================================================================
-- V14__catalogo_de_racas
-- ========================================================================

-- ============================================================
-- V14 -- Catalogo de racas: a raca deixa de ser texto que cada um escreve
--        do seu jeito e passa a ter identidade (Oracle)
--
-- A explicacao completa do porque esta na versao MySQL desta migracao. O resumo:
-- `raca` e `especie` eram VARCHAR livres, e o banco acumulou grafias
-- concorrentes (Cachorro/CAO/CANINO, SRD/Vira/Vira-lata, com e sem acento).
-- Isso ja estava errando no widget de saude preditiva da API .NET, que casa
-- raca por substring justamente porque nao havia padronizacao.
--
-- A coluna `chave` e a resposta: mesmo identificador na arte do app, na
-- predisposicao da .NET e na busca do cliente. Quem escolhe do catalogo recebe
-- a chave pronta, e nao sobra o que normalizar.
--
-- ONDE ESTE ARQUIVO DIVERGE DO MYSQL, E POR QUE
--
-- So no DDL: NUMBER(1) no lugar de TINYINT e VARCHAR2 no lugar de VARCHAR,
-- seguindo o resto do schema Oracle. A reconciliacao e IDENTICA nos dois
-- arquivos, e isso e proposital -- ver a nota longa la embaixo.
--
-- ESTE ARQUIVO TAMBEM RODA CONTRA H2
--
-- Nao e so o Oracle que le esta pasta: `spring.flyway.locations` dos perfis
-- `dev` e `h2` aponta para ca, e o perfil `dev` e o que a suite de testes usa.
-- Entao tudo aqui precisa ser entendido pelos DOIS.
--
-- Foi o que derrubou a primeira versao desta migracao: ela usava MERGE com
-- CONVERT(x, 'US7ASCII'), que e o idioma do Oracle para comparar sem acento. O
-- H2 nao tem a funcao, e os 300 e poucos testes cairam de uma vez com
-- "Syntax error ... expected data type" -- erro que nao menciona acento nenhum.
-- ============================================================

CREATE TABLE t_clyvo_raca (
    id            VARCHAR2(36)  NOT NULL,
    especie       VARCHAR2(20)  NOT NULL,
    nome          VARCHAR2(100) NOT NULL,
    chave         VARCHAR2(60)  NOT NULL,
    porte_tipico  VARCHAR2(20),
    ativo         NUMBER(1)     DEFAULT 1 NOT NULL,
    CONSTRAINT pk_raca PRIMARY KEY (id),
    CONSTRAINT uk_raca_chave UNIQUE (chave),
    CONSTRAINT ck_raca_especie CHECK (especie IN ('CAO','GATO','ROEDOR','AVE','REPTIL')),
    CONSTRAINT ck_raca_ativo CHECK (ativo IN (0,1))
);

CREATE INDEX ix_raca_especie ON t_clyvo_raca (especie, ativo);

-- ------------------------------------------------------------
-- O CATALOGO
--
-- Curto de proposito: um seletor com 200 itens e pior que um campo de texto, e
-- cada linha daqui vai ganhar uma arte em pixel -- o catalogo nao pode crescer
-- mais rapido do que alguem consegue desenhar.
--
-- A ultima linha de cada especie e o "sem raca definida", que nao e caso de
-- borda: sao 9 dos 28 animais deste banco.
-- ------------------------------------------------------------

INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000001','CAO','Golden Retriever','golden-retriever','GRANDE');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000002','CAO','Labrador Retriever','labrador-retriever','GRANDE');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000003','CAO','Pastor Alemão','pastor-alemao','GRANDE');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000004','CAO','Border Collie','border-collie','MEDIO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000005','CAO','Poodle','poodle','PEQUENO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000006','CAO','Bulldog Francês','bulldog-frances','PEQUENO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000007','CAO','Dachshund','dachshund','PEQUENO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000008','CAO','Shih Tzu','shih-tzu','PEQUENO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000009','CAO','Yorkshire Terrier','yorkshire-terrier','PEQUENO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000010','CAO','Pug','pug','PEQUENO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000011','CAO','Rottweiler','rottweiler','GRANDE');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000012','CAO','Beagle','beagle','MEDIO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000013','CAO','Pinscher','pinscher','PEQUENO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000014','CAO','Chihuahua','chihuahua','PEQUENO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000015','CAO','Husky Siberiano','husky-siberiano','GRANDE');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000016','CAO','Maltês','maltes','PEQUENO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000017','CAO','Cocker Spaniel','cocker-spaniel','MEDIO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000018','CAO','Boxer','boxer','GRANDE');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000019','CAO','Sem raça definida','srd-cao',NULL);

INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000101','GATO','Siamês','siames','PEQUENO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000102','GATO','Persa','persa','PEQUENO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000103','GATO','Maine Coon','maine-coon','GRANDE');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000104','GATO','Angorá','angora','PEQUENO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000105','GATO','Bengal','bengal','MEDIO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000106','GATO','Sphynx','sphynx','PEQUENO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000107','GATO','Ragdoll','ragdoll','MEDIO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000108','GATO','British Shorthair','british-shorthair','MEDIO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000109','GATO','Sem raça definida','srd-gato',NULL);

INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000201','ROEDOR','Hamster Sírio','hamster-sirio','PEQUENO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000202','ROEDOR','Hamster Anão Russo','hamster-anao-russo','PEQUENO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000203','ROEDOR','Porquinho-da-índia','porquinho-da-india','PEQUENO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000204','ROEDOR','Coelho','coelho','PEQUENO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000205','ROEDOR','Chinchila','chinchila','PEQUENO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000206','ROEDOR','Gerbil','gerbil','PEQUENO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000209','ROEDOR','Outro roedor','srd-roedor',NULL);

INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000301','AVE','Calopsita','calopsita','PEQUENO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000302','AVE','Periquito','periquito','PEQUENO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000303','AVE','Canário','canario','PEQUENO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000304','AVE','Papagaio','papagaio','MEDIO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000309','AVE','Outra ave','srd-ave',NULL);

INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000401','REPTIL','Jabuti','jabuti','PEQUENO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000402','REPTIL','Tartaruga-d''água','tartaruga-dagua','PEQUENO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000403','REPTIL','Iguana','iguana','MEDIO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000404','REPTIL','Gecko','gecko','PEQUENO');
INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES ('22222222-0000-0000-0000-000000000409','REPTIL','Outro réptil','srd-reptil',NULL);

-- ------------------------------------------------------------
-- A LIGACAO COM O ANIMAL
--
-- A FK nasce anulavel e a coluna de texto `raca` fica. Sao 200 e poucas racas
-- de cachorro: "outra raca" precisa continuar existindo. E manter o texto e o
-- que faz esta migracao nao quebrar nada -- todo SELECT existente, o widget da
-- .NET e o AnimalResponse continuam lendo `raca` como sempre leram.
--
-- A regra do modelo, dita uma vez: com `raca_id` preenchido, `raca` e copia do
-- catalogo; com ele nulo, `raca` e o que o tutor digitou.
-- ------------------------------------------------------------

ALTER TABLE t_clyvo_animal ADD raca_id VARCHAR2(36);

ALTER TABLE t_clyvo_animal
    ADD CONSTRAINT fk_animal_raca FOREIGN KEY (raca_id) REFERENCES t_clyvo_raca(id);

CREATE INDEX ix_animal_raca ON t_clyvo_animal (raca_id);

-- ------------------------------------------------------------
-- RECONCILIACAO DO QUE JA ESTAVA GRAVADO
--
-- Casa o texto legado com o catalogo ignorando acento e caixa -- que e
-- exatamente o que o texto livre produziu de diferente.
--
-- POR QUE ESTE ENCADEAMENTO DE REPLACE, E NAO ALGO LEGIVEL
--
-- Cada banco resolve acento de um jeito e nenhum dos tres concorda:
--   MySQL   a colacao ja e utf8mb4_0900_ai_ci (accent+case insensitive)
--   Oracle  teria CONVERT(x, 'US7ASCII')
--   H2      nao tem nenhum dos dois
--
-- E os tres precisam rodar isto: o perfil `dev` dos testes aplica a pasta
-- `oracle` contra H2, e o MigrationsMySqlTest aplica a pasta `mysql` tambem
-- contra H2. O feio aqui e o preco de uma migracao que roda em todo lugar.
--
-- Pelo mesmo motivo o UPDATE usa subconsulta correlacionada em vez de
-- `UPDATE ... JOIN` (so MySQL) ou `MERGE` (que o H2 escreve de outro jeito).
--
-- O que NAO casar fica com raca_id nulo e mantem o texto original. Nao e perda:
-- e a diferenca entre "este animal e um Golden" e "o tutor escreveu Labrador
-- misto", e a segunda frase tem informacao que o catalogo nao tem.
-- ------------------------------------------------------------

-- 1) casamento pelo nome, DENTRO DA ESPECIE
--
-- O recorte por especie nao e refinamento: e o que torna a subconsulta
-- deterministica. Duas linhas do catalogo se chamam "Sem raca definida" -- uma
-- de cao, outra de gato -- e sem o recorte a subconsulta devolveria duas linhas
-- e o UPDATE morreria com "Subquery returns more than 1 row".
--
-- Na primeira execucao isso passa despercebido, porque nesse momento o texto
-- ainda e "SRD"/"Vira"/"Vira-lata" e nenhum deles casa com aquele nome. So que
-- depois do passo 4 o texto VIRA "Sem raca definida", e uma segunda execucao
-- (banco restaurado, migracao reaplicada) encontraria a ambiguidade. Foi
-- exatamente o que aconteceu ao testar isto contra o MySQL local.
--
-- Alem disso o recorte esta semanticamente certo: nome de raca so significa
-- alguma coisa dentro de uma especie.
UPDATE t_clyvo_animal
   SET raca_id = (SELECT r.id FROM t_clyvo_raca r
                   WHERE REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(LOWER(TRIM(r.nome)),'á','a'),'â','a'),'ã','a'),'à','a'),'é','e'),'ê','e'),'í','i'),'ó','o'),'ô','o'),'õ','o'),'ú','u'),'ç','c') = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(LOWER(TRIM(t_clyvo_animal.raca)),'á','a'),'â','a'),'ã','a'),'à','a'),'é','e'),'ê','e'),'í','i'),'ó','o'),'ô','o'),'õ','o'),'ú','u'),'ç','c')
                     AND r.especie = CASE
                     WHEN REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(LOWER(TRIM(t_clyvo_animal.especie)),'á','a'),'â','a'),'ã','a'),'à','a'),'é','e'),'ê','e'),'í','i'),'ó','o'),'ô','o'),'õ','o'),'ú','u'),'ç','c') IN ('cachorro','cao','canino') THEN 'CAO'
                     WHEN REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(LOWER(TRIM(t_clyvo_animal.especie)),'á','a'),'â','a'),'ã','a'),'à','a'),'é','e'),'ê','e'),'í','i'),'ó','o'),'ô','o'),'õ','o'),'ú','u'),'ç','c') IN ('gato','felino')           THEN 'GATO'
                     WHEN REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(LOWER(TRIM(t_clyvo_animal.especie)),'á','a'),'â','a'),'ã','a'),'à','a'),'é','e'),'ê','e'),'í','i'),'ó','o'),'ô','o'),'õ','o'),'ú','u'),'ç','c') IN ('roedor')                  THEN 'ROEDOR'
                     WHEN REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(LOWER(TRIM(t_clyvo_animal.especie)),'á','a'),'â','a'),'ã','a'),'à','a'),'é','e'),'ê','e'),'í','i'),'ó','o'),'ô','o'),'õ','o'),'ú','u'),'ç','c') IN ('passaro','ave')           THEN 'AVE'
                     WHEN REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(LOWER(TRIM(t_clyvo_animal.especie)),'á','a'),'â','a'),'ã','a'),'à','a'),'é','e'),'ê','e'),'í','i'),'ó','o'),'ô','o'),'õ','o'),'ú','u'),'ç','c') IN ('reptil')                  THEN 'REPTIL'
                   END)
 WHERE raca_id IS NULL
   AND EXISTS (SELECT 1 FROM t_clyvo_raca r
                WHERE REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(LOWER(TRIM(r.nome)),'á','a'),'â','a'),'ã','a'),'à','a'),'é','e'),'ê','e'),'í','i'),'ó','o'),'ô','o'),'õ','o'),'ú','u'),'ç','c') = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(LOWER(TRIM(t_clyvo_animal.raca)),'á','a'),'â','a'),'ã','a'),'à','a'),'é','e'),'ê','e'),'í','i'),'ó','o'),'ô','o'),'õ','o'),'ú','u'),'ç','c')
                     AND r.especie = CASE
                     WHEN REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(LOWER(TRIM(t_clyvo_animal.especie)),'á','a'),'â','a'),'ã','a'),'à','a'),'é','e'),'ê','e'),'í','i'),'ó','o'),'ô','o'),'õ','o'),'ú','u'),'ç','c') IN ('cachorro','cao','canino') THEN 'CAO'
                     WHEN REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(LOWER(TRIM(t_clyvo_animal.especie)),'á','a'),'â','a'),'ã','a'),'à','a'),'é','e'),'ê','e'),'í','i'),'ó','o'),'ô','o'),'õ','o'),'ú','u'),'ç','c') IN ('gato','felino')           THEN 'GATO'
                     WHEN REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(LOWER(TRIM(t_clyvo_animal.especie)),'á','a'),'â','a'),'ã','a'),'à','a'),'é','e'),'ê','e'),'í','i'),'ó','o'),'ô','o'),'õ','o'),'ú','u'),'ç','c') IN ('roedor')                  THEN 'ROEDOR'
                     WHEN REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(LOWER(TRIM(t_clyvo_animal.especie)),'á','a'),'â','a'),'ã','a'),'à','a'),'é','e'),'ê','e'),'í','i'),'ó','o'),'ô','o'),'õ','o'),'ú','u'),'ç','c') IN ('passaro','ave')           THEN 'AVE'
                     WHEN REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(LOWER(TRIM(t_clyvo_animal.especie)),'á','a'),'â','a'),'ã','a'),'à','a'),'é','e'),'ê','e'),'í','i'),'ó','o'),'ô','o'),'õ','o'),'ú','u'),'ç','c') IN ('reptil')                  THEN 'REPTIL'
                   END);

-- 2) os sinonimos de "sem raca definida", que nenhum nome de catalogo alcanca
UPDATE t_clyvo_animal
   SET raca_id = '22222222-0000-0000-0000-000000000019'
 WHERE raca_id IS NULL
   AND LOWER(TRIM(raca)) IN ('srd','vira','vira-lata','viralata','sem raca definida','vira lata')
   AND REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(LOWER(TRIM(especie)),'á','a'),'â','a'),'ã','a'),'à','a'),'é','e'),'ê','e'),'í','i'),'ó','o'),'ô','o'),'õ','o'),'ú','u'),'ç','c') IN ('cachorro','cao','canino');

UPDATE t_clyvo_animal
   SET raca_id = '22222222-0000-0000-0000-000000000109'
 WHERE raca_id IS NULL
   AND LOWER(TRIM(raca)) IN ('srd','vira','vira-lata','viralata','sem raca definida','vira lata')
   AND REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(LOWER(TRIM(especie)),'á','a'),'â','a'),'ã','a'),'à','a'),'é','e'),'ê','e'),'í','i'),'ó','o'),'ô','o'),'õ','o'),'ú','u'),'ç','c') IN ('gato','felino');

-- 3) variantes que o catalogo cobre sob outro nome
UPDATE t_clyvo_animal SET raca_id = '22222222-0000-0000-0000-000000000005'
 WHERE raca_id IS NULL AND LOWER(TRIM(raca)) LIKE 'poodle%';
UPDATE t_clyvo_animal SET raca_id = '22222222-0000-0000-0000-000000000002'
 WHERE raca_id IS NULL AND LOWER(TRIM(raca)) LIKE 'labrador%';
UPDATE t_clyvo_animal SET raca_id = '22222222-0000-0000-0000-000000000001'
 WHERE raca_id IS NULL AND LOWER(TRIM(raca)) LIKE 'golden%';

-- 4) uniformiza especie e texto de quem passou a ter raca de catalogo.
--    E o que faz CAO / CANINO / Cachorro pararem de conviver -- nao por
--    validacao que recusa, mas por derivacao que uniformiza.
UPDATE t_clyvo_animal
   SET especie = (SELECT CASE r.especie
                             WHEN 'CAO'    THEN 'Cachorro'
                             WHEN 'GATO'   THEN 'Gato'
                             WHEN 'ROEDOR' THEN 'Roedor'
                             WHEN 'AVE'    THEN 'Passaro'
                             WHEN 'REPTIL' THEN 'Reptil'
                         END
                    FROM t_clyvo_raca r WHERE r.id = t_clyvo_animal.raca_id),
       raca    = (SELECT r.nome FROM t_clyvo_raca r WHERE r.id = t_clyvo_animal.raca_id)
 WHERE raca_id IS NOT NULL;


-- ========================================================================
-- V15__base_de_doencas_e_parecer_ia
-- ========================================================================

-- ============================================================
-- V15 -- A base de doencas para a IA de saude preditiva, e o conserto
--        do widget que nunca funcionou (Oracle)
--
-- O racional completo esta no arquivo irmao em db/migration/mysql -- as duas
-- pastas dizem a mesma coisa em dialetos diferentes, e o comentario longo em
-- dobro so envelheceria em dobro. O resumo:
--
--   1) A V8 esqueceu `criado_em` em t_clyvo_predisposicao_saude ao converter
--      o schema da .NET (o original declara a coluna e o EF a mapeia) e nunca
--      portou o seed de 42 predisposicoes. Resultado: 500 em todo
--      GET /widget-saude-preditiva. Aqui a coluna nasce e o seed entra.
--   2) t_clyvo_base_doencas: agregacao dos datasets Dryad (contagem de casos
--      por especie/raca/doenca), grounding do parecer da IA e do fallback.
--   3) t_clyvo_parecer_ia: um parecer por animal, com validade -- o cache que
--      limita a OCI a uma chamada por animal por semana.
--
-- Lembretes de dialeto: booleanos sao NUMBER(1) (nunca aqui), datas TIMESTAMP
-- com SYSTIMESTAMP, e TUDO precisa passar no H2 MODE=Oracle -- o perfil `dev`
-- dos testes roda ESTA pasta contra H2.
-- ============================================================

-- ------------------------------------------------------------
-- 1) O conserto da V8
-- ------------------------------------------------------------

ALTER TABLE t_clyvo_predisposicao_saude
    ADD (criado_em TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL);

-- As 42 predisposicoes do arquivo original da .NET (schema/05), nunca portadas.

INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000001', 'CACHORRO', 'Labrador', 6, 'Displasia de quadril', 'Agendar avaliacao ortopedica e considerar suplementacao articular preventiva.', 'VetCompass (RVC) - Labrador Retrievers under primary veterinary care in the UK', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000002', 'CACHORRO', 'Labrador', 7, 'Obesidade', 'Reavaliar dieta e nivel de atividade fisica; agendar checkup nutricional.', 'VetCompass (RVC) - Labrador Retrievers under primary veterinary care in the UK', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000003', 'CACHORRO', 'Dachshund', 3, 'Doenca de disco intervertebral (hernia)', 'Evitar impacto/escadas e agendar avaliacao neurologica se houver dor ou dificuldade de locomocao.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000004', 'CACHORRO', 'Bulldog Frances', 0, 'Sindrome respiratoria braquicefalica', 'Evitar exercicio intenso e calor; agendar avaliacao respiratoria com veterinario.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000005', 'CACHORRO', 'Bulldog Ingles', 0, 'Sindrome respiratoria braquicefalica', 'Evitar exercicio intenso e calor; agendar avaliacao respiratoria com veterinario.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000006', 'CACHORRO', 'Pastor Alemao', 7, 'Displasia de quadril', 'Agendar avaliacao ortopedica preventiva.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000007', 'CACHORRO', 'Pastor Alemao', 8, 'Mielopatia degenerativa', 'Monitorar fraqueza em membros posteriores; agendar avaliacao neurologica.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000008', 'CACHORRO', 'Yorkshire', 2, 'Luxacao de patela', 'Agendar avaliacao ortopedica se houver claudicacao intermitente.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000009', 'CACHORRO', 'Poodle', 4, 'Atrofia progressiva de retina', 'Agendar avaliacao oftalmologica preventiva anual.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000010', 'CACHORRO', 'Golden Retriever', 8, 'Predisposicao a neoplasias (linfoma, hemangiossarcoma)', 'Agendar checkup geral com exames de rotina a partir dessa idade.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000011', 'CACHORRO', 'Rottweiler', 5, 'Displasia de cotovelo', 'Agendar avaliacao ortopedica se houver claudicacao.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000012', 'CACHORRO', 'Chihuahua', 5, 'Colapso de traqueia', 'Evitar coleira (preferir peitoral) e agendar avaliacao respiratoria se houver tosse seca persistente.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000013', 'CACHORRO', 'Cavalier King Charles Spaniel', 4, 'Doenca valvar mitral', 'Agendar avaliacao cardiologica preventiva (ausculta/ecocardiograma).', 'VetCompass (RVC) - Disorders in Cavalier King Charles Spaniels attending primary-care practices in England', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000014', 'CACHORRO', 'Pug', 0, 'Sindrome respiratoria braquicefalica e dermatite de dobras cutaneas', 'Higienizar dobras de pele regularmente e evitar exercicio intenso em dias quentes.', 'VetCompass (RVC) - Health of Pug dogs in the UK: disorder predispositions and protections', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000015', 'GATO', 'Persa', 3, 'Doenca renal policistica (PKD)', 'Agendar ultrassonografia renal preventiva.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000016', 'GATO', 'Persa', 0, 'Sindrome respiratoria braquicefalica', 'Monitorar respiracao ruidosa; evitar calor excessivo.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000017', 'GATO', 'Siames', 2, 'Cardiomiopatia hipertrófica', 'Agendar avaliacao cardiologica preventiva.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000018', 'GATO', 'Maine Coon', 2, 'Cardiomiopatia hipertrófica', 'Agendar avaliacao cardiologica preventiva (raca com predisposicao genetica conhecida).', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000019', 'GATO', 'SRD', 7, 'Obesidade e diabetes mellitus', 'Reavaliar dieta e agendar exame de glicemia preventivo.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000020', 'PASSARO', 'Calopsita', 0, 'Deficiencia de calcio', 'Revisar dieta (suplementacao de calcio e exposicao a luz UV adequada).', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000021', 'PASSARO', 'Periquito', 0, 'Deficiencia de calcio', 'Revisar dieta (suplementacao de calcio e exposicao a luz UV adequada).', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000022', 'PASSARO', 'Papagaio', 0, 'Doenca respiratoria por ma ventilacao (aspergilose)', 'Melhorar ventilacao do ambiente e agendar avaliacao respiratoria se houver espirros/secrecao.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000023', 'PASSARO', 'Calopsita', 5, 'Tumores (lipoma, tumor renal)', 'Agendar checkup geral a partir dessa idade.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000024', 'REPTIL', 'Tartaruga', 0, 'Doenca ossea metabolica (deficit de UV/calcio)', 'Revisar exposicao a luz UVB e suplementacao de calcio.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000025', 'REPTIL', 'Iguana', 0, 'Doenca ossea metabolica (deficit de UV/calcio)', 'Revisar exposicao a luz UVB e suplementacao de calcio.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000026', 'REPTIL', 'Jabuti', 0, 'Infeccao respiratoria por temperatura inadequada', 'Revisar temperatura e umidade do terrario; agendar avaliacao se houver secrecao nasal.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000027', 'ROEDOR', 'Coelho', 0, 'Estase gastrointestinal', 'Revisar dieta rica em fibras (feno) e agendar avaliacao se houver reducao de apetite.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000028', 'ROEDOR', 'Coelho', 0, 'Ma oclusao dentaria', 'Agendar avaliacao odontologica se houver dificuldade para se alimentar.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000029', 'ROEDOR', 'Hamster', 1.5, 'Tumor adrenal', 'Agendar checkup geral a partir dessa idade.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000030', 'ROEDOR', 'Porquinho-da-india', 0, 'Ma oclusao dentaria', 'Agendar avaliacao odontologica se houver dificuldade para se alimentar.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000031', 'ROEDOR', 'Chinchila', 0, 'Golpe de calor (sensibilidade termica)', 'Manter ambiente fresco e ventilado, evitar exposicao a temperaturas acima de 25 graus.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000032', 'BOVINO', 'Holandesa', 0, 'Deslocamento de abomaso', 'Monitorar animais no pos-parto imediato; agendar avaliacao veterinaria se houver reducao brusca de apetite.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000033', 'BOVINO', 'Holandesa', 0, 'Cetose', 'Monitorar animais em inicio de lactacao; ajustar manejo nutricional.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000034', 'BOVINO', 'Holandesa', 5, 'Febre do leite (hipocalcemia)', 'Monitorar animais mais velhos ao redor do parto; considerar suplementacao preventiva de calcio.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000035', 'BOVINO', 'Nelore', 0, 'Verminose gastrointestinal', 'Manter protocolo de vermifugacao em animais jovens.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000036', 'BOVINO', NULL, 0, 'Mastite', 'Reforcar higiene da ordenha em femeas em lactacao.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000037', 'BOVINO', NULL, 0, 'Laminite', 'Revisar dieta com excesso de graos; agendar avaliacao podal.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000038', 'EQUINO', 'Mangalarga Marchador', 0, 'Colica', 'Manter rotina de alimentacao regular e acesso continuo a agua; agendar avaliacao imediata em caso de sinais de dor abdominal.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000039', 'EQUINO', 'Quarto de Milha', 0, 'Laminite', 'Revisar dieta rica em graos/pastagem e controlar peso corporal.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000040', 'EQUINO', NULL, 15, 'Sindrome de Cushing equino (PPID)', 'Agendar avaliacao hormonal preventiva em cavalos idosos.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000041', 'EQUINO', NULL, 15, 'Osteoartrite / doenca articular degenerativa', 'Agendar avaliacao ortopedica se houver claudicacao ou rigidez.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000042', 'EQUINO', NULL, 0, 'RAO / obstrucao recorrente das vias aereas ("asma equina")', 'Revisar qualidade do feno/estabulo (poeira e mofo) e ventilacao do ambiente.', 'Conhecimento veterinario consolidado', SYSTIMESTAMP);

-- ------------------------------------------------------------
-- 2) A base agregada de doencas
-- ------------------------------------------------------------

CREATE TABLE t_clyvo_base_doencas (
    id             VARCHAR2(36)  NOT NULL,
    especie        VARCHAR2(20)  NOT NULL,
    raca_texto     VARCHAR2(100) NOT NULL,
    raca_chave     VARCHAR2(60)  NULL,
    doenca_codigo  VARCHAR2(60)  NOT NULL,
    doenca_nome    VARCHAR2(200) NOT NULL,
    categoria      VARCHAR2(60)  NOT NULL,
    casos          NUMBER(10)    NOT NULL,
    controles      NUMBER(10)    NOT NULL,
    fonte          VARCHAR2(300) NULL,
    doi            VARCHAR2(100) NULL,
    criado_em      TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_base_doencas PRIMARY KEY (id),
    CONSTRAINT ck_base_doencas_especie CHECK (especie IN ('CAO','GATO','ROEDOR','AVE','REPTIL'))
);

CREATE INDEX ix_base_doencas_busca ON t_clyvo_base_doencas (especie, raca_chave);

INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000001','AVE','Certhidea fusca',NULL,'PoxT','Varíola aviária','INFECCIOSA',2,0,'Avian disease surveillance on the island of San Cristobal, Galapagos','10.5061/dryad.kwh70rz4z',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000002','AVE','Geospiza fortis',NULL,'PoxT','Varíola aviária','INFECCIOSA',13,0,'Avian disease surveillance on the island of San Cristobal, Galapagos','10.5061/dryad.kwh70rz4z',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000003','AVE','Geospiza fuliginosa',NULL,'PoxT','Varíola aviária','INFECCIOSA',49,0,'Avian disease surveillance on the island of San Cristobal, Galapagos','10.5061/dryad.kwh70rz4z',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000004','AVE','Myiarchus magnirostris',NULL,'PoxT','Varíola aviária','INFECCIOSA',3,0,'Avian disease surveillance on the island of San Cristobal, Galapagos','10.5061/dryad.kwh70rz4z',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000005','AVE','Setophaga petechia aureola',NULL,'PoxT','Varíola aviária','INFECCIOSA',3,0,'Avian disease surveillance on the island of San Cristobal, Galapagos','10.5061/dryad.kwh70rz4z',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000006','CAO','airedale_terrier',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',1,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000008','CAO','airedale_terrier',NULL,'MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000009','CAO','airedale_terrier',NULL,'lymphoma','Linfoma','ONCOLOGICA',5,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000011','CAO','alaskan_malamute',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000013','CAO','american_eskimo_dog',NULL,'MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000014','CAO','american_eskimo_dog',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000015','CAO','american_pit_bull_terrier',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000016','CAO','american_pit_bull_terrier',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000017','CAO','american_staffordshire_terrier',NULL,'MCT','Mastocitoma','ONCOLOGICA',3,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000018','CAO','american_staffordshire_terrier',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000021','CAO','australian_cattle_dog',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000024','CAO','australian_shepherd',NULL,'MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000025','CAO','australian_shepherd',NULL,'lymphoma','Linfoma','ONCOLOGICA',5,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000026','CAO','basset_hound',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000027','CAO','basset_hound',NULL,'ED','Displasia de cotovelo','ORTOPEDICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000028','CAO','basset_hound',NULL,'MCT','Mastocitoma','ONCOLOGICA',6,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000029','CAO','basset_hound',NULL,'lymphoma','Linfoma','ONCOLOGICA',9,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000030','CAO','beagle','beagle','MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000031','CAO','beagle','beagle','MVD','Displasia da valva mitral','CARDIACA',3,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000032','CAO','beagle','beagle','lymphoma','Linfoma','ONCOLOGICA',3,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000036','CAO','belgian_malinois',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000039','CAO','belgian_sheepdog',NULL,'MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000042','CAO','bernese_mountain_dog',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',3,3,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000043','CAO','bernese_mountain_dog',NULL,'ED','Displasia de cotovelo','ORTOPEDICA',2,5,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000044','CAO','bernese_mountain_dog',NULL,'MCT','Mastocitoma','ONCOLOGICA',3,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000045','CAO','bernese_mountain_dog',NULL,'lymphoma','Linfoma','ONCOLOGICA',2,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000046','CAO','bichon_frise',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000048','CAO','bichon_frise',NULL,'MVD','Displasia da valva mitral','CARDIACA',5,4,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000049','CAO','bichon_frise',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000052','CAO','bloodhound',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000053','CAO','boerboel',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000055','CAO','border_collie','border-collie','CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',1,16,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000056','CAO','border_collie','border-collie','ED','Displasia de cotovelo','ORTOPEDICA',1,17,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000057','CAO','border_collie','border-collie','lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000060','CAO','borzoi',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000063','CAO','boston_terrier',NULL,'MCT','Mastocitoma','ONCOLOGICA',5,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000064','CAO','boston_terrier',NULL,'MVD','Displasia da valva mitral','CARDIACA',9,2,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000065','CAO','boston_terrier',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000066','CAO','bouvier_des_flandres',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000068','CAO','bouvier_des_flandres',NULL,'MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000069','CAO','boxer','boxer','CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000071','CAO','boxer','boxer','GC','Colite granulomatosa','GASTROINTESTINAL',40,74,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000072','CAO','boxer','boxer','MCT','Mastocitoma','ONCOLOGICA',32,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000073','CAO','boxer','boxer','lymphoma','Linfoma','ONCOLOGICA',13,6,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000075','CAO','brittany',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000076','CAO','bull_terrier',NULL,'MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000077','CAO','bull_terrier',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000078','CAO','bulldog_english',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',5,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000080','CAO','bulldog_english',NULL,'GC','Colite granulomatosa','GASTROINTESTINAL',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000081','CAO','bulldog_english',NULL,'MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000082','CAO','bulldog_english',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000084','CAO','bulldog_french','bulldog-frances','GC','Colite granulomatosa','GASTROINTESTINAL',5,17,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000085','CAO','bullmastiff',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',2,3,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000086','CAO','bullmastiff',NULL,'ED','Displasia de cotovelo','ORTOPEDICA',3,3,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000087','CAO','bullmastiff',NULL,'MCT','Mastocitoma','ONCOLOGICA',3,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000088','CAO','cairn_terrier',NULL,'MVD','Displasia da valva mitral','CARDIACA',2,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000089','CAO','cairn_terrier',NULL,'PSVA','Anomalia vascular portossistêmica','HEPATICA/VASCULAR',21,23,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000090','CAO','cairn_terrier',NULL,'lymphoma','Linfoma','ONCOLOGICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000091','CAO','cane_corso',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',5,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000092','CAO','cane_corso',NULL,'ED','Displasia de cotovelo','ORTOPEDICA',2,4,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000093','CAO','cardigan_welsh_corgi',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000095','CAO','cavalier_king_charles_spaniel',NULL,'MVD','Displasia da valva mitral','CARDIACA',36,2,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000096','CAO','chesapeake_bay_retriever',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',3,2,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000098','CAO','chesapeake_bay_retriever',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000099','CAO','chihuahua','chihuahua','MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000100','CAO','chihuahua','chihuahua','MVD','Displasia da valva mitral','CARDIACA',4,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000101','CAO','chinese_shar-pei',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000102','CAO','chinese_shar-pei',NULL,'MCT','Mastocitoma','ONCOLOGICA',7,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000104','CAO','chow_chow',NULL,'ED','Displasia de cotovelo','ORTOPEDICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000105','CAO','cocker_spaniel','cocker-spaniel','CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',2,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000107','CAO','cocker_spaniel','cocker-spaniel','MCT','Mastocitoma','ONCOLOGICA',4,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000108','CAO','cocker_spaniel','cocker-spaniel','MVD','Displasia da valva mitral','CARDIACA',22,11,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000109','CAO','cocker_spaniel','cocker-spaniel','lymphoma','Linfoma','ONCOLOGICA',7,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000112','CAO','collie',NULL,'lymphoma','Linfoma','ONCOLOGICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000113','CAO','coton_de_tulear',NULL,'MVD','Displasia da valva mitral','CARDIACA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000114','CAO','dachshund','dachshund','MVD','Displasia da valva mitral','CARDIACA',8,9,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000115','CAO','dachshund','dachshund','lymphoma','Linfoma','ONCOLOGICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000116','CAO','dachshund_miniature','dachshund','MVD','Displasia da valva mitral','CARDIACA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000117','CAO','dalmatian',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000119','CAO','dandie_dinmont_terrier',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000120','CAO','doberman_pinscher',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',4,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000121','CAO','doberman_pinscher',NULL,'ED','Displasia de cotovelo','ORTOPEDICA',1,4,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000122','CAO','doberman_pinscher',NULL,'lymphoma','Linfoma','ONCOLOGICA',5,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000123','CAO','dogue_de_bordeaux',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000124','CAO','dogue_de_bordeaux',NULL,'ED','Displasia de cotovelo','ORTOPEDICA',1,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000126','CAO','english_cocker_spaniel','cocker-spaniel','MVD','Displasia da valva mitral','CARDIACA',1,2,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000127','CAO','english_cocker_spaniel','cocker-spaniel','lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000129','CAO','english_setter',NULL,'ED','Displasia de cotovelo','ORTOPEDICA',27,52,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000130','CAO','english_setter',NULL,'MCT','Mastocitoma','ONCOLOGICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000131','CAO','english_springer_spaniel',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',1,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000133','CAO','english_springer_spaniel',NULL,'lymphoma','Linfoma','ONCOLOGICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000134','CAO','english_toy_spaniel',NULL,'MVD','Displasia da valva mitral','CARDIACA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000137','CAO','flat-coated_retriever',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000138','CAO','fox_terrier_wire',NULL,'MVD','Displasia da valva mitral','CARDIACA',1,3,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000139','CAO','german_shepherd_dog','pastor-alemao','CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',24,24,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000140','CAO','german_shepherd_dog','pastor-alemao','ED','Displasia de cotovelo','ORTOPEDICA',10,45,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000141','CAO','german_shepherd_dog','pastor-alemao','MCT','Mastocitoma','ONCOLOGICA',1,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000142','CAO','german_shepherd_dog','pastor-alemao','lymphoma','Linfoma','ONCOLOGICA',5,7,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000143','CAO','german_shorthaired_pointer',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000145','CAO','german_shorthaired_pointer',NULL,'MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000148','CAO','golden_retriever','golden-retriever','CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',28,32,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000149','CAO','golden_retriever','golden-retriever','ED','Displasia de cotovelo','ORTOPEDICA',8,49,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000150','CAO','golden_retriever','golden-retriever','MCT','Mastocitoma','ONCOLOGICA',31,9,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000151','CAO','golden_retriever','golden-retriever','lymphoma','Linfoma','ONCOLOGICA',43,48,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000153','CAO','great_dane',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',4,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000154','CAO','great_dane',NULL,'ED','Displasia de cotovelo','ORTOPEDICA',1,5,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000155','CAO','great_pyrenees',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',1,2,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000157','CAO','great_pyrenees',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000161','CAO','greyhound',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000163','CAO','havanese',NULL,'MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000164','CAO','havanese',NULL,'PSVA','Anomalia vascular portossistêmica','HEPATICA/VASCULAR',17,15,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000167','CAO','irish_setter',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',1,3,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000168','CAO','irish_setter',NULL,'ED','Displasia de cotovelo','ORTOPEDICA',1,5,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000169','CAO','irish_wolfhound',NULL,'ED','Displasia de cotovelo','ORTOPEDICA',1,4,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000170','CAO','irish_wolfhound',NULL,'epilepsy','Epilepsia','NEUROLOGICA',34,168,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000171','CAO','irish_wolfhound',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000172','CAO','italian_greyhound',NULL,'MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000173','CAO','italian_greyhound',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000174','CAO','jack_russell_terrier',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000176','CAO','jack_russell_terrier',NULL,'MCT','Mastocitoma','ONCOLOGICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000177','CAO','jack_russell_terrier',NULL,'MVD','Displasia da valva mitral','CARDIACA',3,9,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000178','CAO','jack_russell_terrier',NULL,'lymphoma','Linfoma','ONCOLOGICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000179','CAO','keeshond',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000182','CAO','labrador_retriever','labrador-retriever','CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',114,173,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000183','CAO','labrador_retriever','labrador-retriever','ED','Displasia de cotovelo','ORTOPEDICA',30,180,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000184','CAO','labrador_retriever','labrador-retriever','MCT','Mastocitoma','ONCOLOGICA',160,107,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000185','CAO','labrador_retriever','labrador-retriever','lymphoma','Linfoma','ONCOLOGICA',22,62,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000190','CAO','maltese','maltes','MVD','Displasia da valva mitral','CARDIACA',12,7,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000191','CAO','maltese','maltes','PSVA','Anomalia vascular portossistêmica','HEPATICA/VASCULAR',26,24,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000192','CAO','maltese','maltes','lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000193','CAO','manchester_terrier_toy',NULL,'MVD','Displasia da valva mitral','CARDIACA',1,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000194','CAO','manchester_terrier_toy',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000195','CAO','mastiff',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',1,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000197','CAO','miniature_pinscher','pinscher','MVD','Displasia da valva mitral','CARDIACA',5,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000198','CAO','miniature_schnauzer',NULL,'MVD','Displasia da valva mitral','CARDIACA',9,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000199','CAO','miniature_schnauzer',NULL,'PSVA','Anomalia vascular portossistêmica','HEPATICA/VASCULAR',21,17,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000200','CAO','miniature_schnauzer',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000203','CAO','mix','srd-cao','PSVA','Anomalia vascular portossistêmica','HEPATICA/VASCULAR',7,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000204','CAO','mix','srd-cao','lymphoma','Linfoma','ONCOLOGICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000205','CAO','newfoundland',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',7,15,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000206','CAO','newfoundland',NULL,'ED','Displasia de cotovelo','ORTOPEDICA',8,22,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000207','CAO','newfoundland',NULL,'MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000208','CAO','norfolk_terrier',NULL,'MVD','Displasia da valva mitral','CARDIACA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000209','CAO','norfolk_terrier',NULL,'PSVA','Anomalia vascular portossistêmica','HEPATICA/VASCULAR',10,10,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000212','CAO','nova_scotia_duck_tolling_retriever',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000215','CAO','old_english_sheepdog',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000217','CAO','papillon',NULL,'PSVA','Anomalia vascular portossistêmica','HEPATICA/VASCULAR',2,9,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000218','CAO','pekingese',NULL,'MVD','Displasia da valva mitral','CARDIACA',2,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000219','CAO','pembroke_welsh_corgi',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000221','CAO','pembroke_welsh_corgi',NULL,'MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000225','CAO','pomeranian',NULL,'MVD','Displasia da valva mitral','CARDIACA',6,2,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000226','CAO','pomeranian',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000227','CAO','poodle','poodle','CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',1,2,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000229','CAO','poodle','poodle','MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000230','CAO','poodle','poodle','lymphoma','Linfoma','ONCOLOGICA',3,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000231','CAO','poodle_miniature','poodle','MVD','Displasia da valva mitral','CARDIACA',1,2,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000232','CAO','poodle_toy','poodle','CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000233','CAO','poodle_toy','poodle','MCT','Mastocitoma','ONCOLOGICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000234','CAO','poodle_toy','poodle','MVD','Displasia da valva mitral','CARDIACA',3,6,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000238','CAO','pug','pug','MCT','Mastocitoma','ONCOLOGICA',8,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000241','CAO','rat_terrier',NULL,'MVD','Displasia da valva mitral','CARDIACA',1,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000243','CAO','rhodesian_ridgeback',NULL,'MCT','Mastocitoma','ONCOLOGICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000244','CAO','rottweiler','rottweiler','CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',26,11,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000245','CAO','rottweiler','rottweiler','ED','Displasia de cotovelo','ORTOPEDICA',11,30,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000246','CAO','rottweiler','rottweiler','MCT','Mastocitoma','ONCOLOGICA',3,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000247','CAO','rottweiler','rottweiler','lymphoma','Linfoma','ONCOLOGICA',11,2,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000248','CAO','saint_bernard',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',3,3,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000249','CAO','saint_bernard',NULL,'ED','Displasia de cotovelo','ORTOPEDICA',2,5,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000251','CAO','saint_bernard',NULL,'lymphoma','Linfoma','ONCOLOGICA',5,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000252','CAO','saluki',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000253','CAO','samoyed',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',2,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000255','CAO','scottish_terrier',NULL,'MVD','Displasia da valva mitral','CARDIACA',2,2,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000256','CAO','scottish_terrier',NULL,'lymphoma','Linfoma','ONCOLOGICA',3,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000258','CAO','shetland_sheepdog',NULL,'MCT','Mastocitoma','ONCOLOGICA',4,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000259','CAO','shetland_sheepdog',NULL,'lymphoma','Linfoma','ONCOLOGICA',4,2,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000261','CAO','shiba_inu',NULL,'MVD','Displasia da valva mitral','CARDIACA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000262','CAO','shih_tzu','shih-tzu','MVD','Displasia da valva mitral','CARDIACA',7,6,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000263','CAO','shih_tzu','shih-tzu','lymphoma','Linfoma','ONCOLOGICA',3,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000264','CAO','shiloh_shepherd',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000265','CAO','siberian_husky','husky-siberiano','CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000267','CAO','siberian_husky','husky-siberiano','MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000268','CAO','siberian_husky','husky-siberiano','lymphoma','Linfoma','ONCOLOGICA',2,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000269','CAO','soft_coated_wheaten_terrier',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',1,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000271','CAO','soft_coated_wheaten_terrier',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000272','CAO','spinone_italiano',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000276','CAO','staffordshire_bull_terrier',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',5,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000277','CAO','staffordshire_bull_terrier',NULL,'ED','Displasia de cotovelo','ORTOPEDICA',1,3,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000278','CAO','staffordshire_bull_terrier',NULL,'MCT','Mastocitoma','ONCOLOGICA',6,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000281','CAO','standard_schnauzer',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000283','CAO','tibetan_spaniel',NULL,'PSVA','Anomalia vascular portossistêmica','HEPATICA/VASCULAR',10,13,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000284','CAO','tibetan_terrier',NULL,'MCT','Mastocitoma','ONCOLOGICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000287','CAO','vizsla',NULL,'MCT','Mastocitoma','ONCOLOGICA',51,26,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000288','CAO','vizsla',NULL,'lymphoma','Linfoma','ONCOLOGICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000289','CAO','weimaraner',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',1,3,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000291','CAO','weimaraner',NULL,'MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000292','CAO','west_highland_white_terrier',NULL,'CLLD','Doença do ligamento cruzado cranial','ORTOPEDICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000293','CAO','west_highland_white_terrier',NULL,'MCT','Mastocitoma','ONCOLOGICA',3,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000294','CAO','west_highland_white_terrier',NULL,'MVD','Displasia da valva mitral','CARDIACA',3,5,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000295','CAO','west_highland_white_terrier',NULL,'lymphoma','Linfoma','ONCOLOGICA',3,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000300','CAO','yorkshire_terrier','yorkshire-terrier','MCT','Mastocitoma','ONCOLOGICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000301','CAO','yorkshire_terrier','yorkshire-terrier','MVD','Displasia da valva mitral','CARDIACA',2,7,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000302','CAO','yorkshire_terrier','yorkshire-terrier','PSVA','Anomalia vascular portossistêmica','HEPATICA/VASCULAR',57,105,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000303','GATO','Abyssinian',NULL,'all_GI','Doença gastrointestinal (grupo combinado)','GASTROINTESTINAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000304','GATO','DLH','srd-gato','DM','Diabetes mellitus','ENDOCRINA',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000305','GATO','DLH','srd-gato','FEK','Ceratoconjuntivite eosinofílica felina','OFTALMOLOGICA',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000306','GATO','DLH','srd-gato','IBD','Doença inflamatória intestinal','GASTROINTESTINAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000307','GATO','DLH','srd-gato','chronic_enteropathy','Enteropatia crônica','GASTROINTESTINAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000308','GATO','DLH','srd-gato','hypercalcemia','Hipercalcemia','METABOLICA',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000309','GATO','DMH','srd-gato','FEK','Ceratoconjuntivite eosinofílica felina','OFTALMOLOGICA',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000310','GATO','DSH','srd-gato','DM','Diabetes mellitus','ENDOCRINA',5,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000311','GATO','DSH','srd-gato','FEK','Ceratoconjuntivite eosinofílica felina','OFTALMOLOGICA',3,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000312','GATO','DSH','srd-gato','HCM','Cardiomiopatia hipertrófica','CARDIACA',3,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000313','GATO','DSH','srd-gato','IBD','Doença inflamatória intestinal','GASTROINTESTINAL',2,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000314','GATO','DSH','srd-gato','SCAL','Linfoma alimentar de pequenas células','ONCOLOGICA/GASTROINTESTINAL',5,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000315','GATO','DSH','srd-gato','all_GI','Doença gastrointestinal (grupo combinado)','GASTROINTESTINAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000316','GATO','DSH','srd-gato','chronic_enteropathy','Enteropatia crônica','GASTROINTESTINAL',4,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000317','GATO','DSH','srd-gato','hypercalcemia','Hipercalcemia','METABOLICA',3,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000318','GATO','DSH','srd-gato','hyperthyroidism','Hipertireoidismo','ENDOCRINA',4,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000319','GATO','Himalayan',NULL,'CKD','Doença renal crônica','RENAL',2,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000320','GATO','Himalayan',NULL,'IBD','Doença inflamatória intestinal','GASTROINTESTINAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000321','GATO','Himalayan',NULL,'all_GI','Doença gastrointestinal (grupo combinado)','GASTROINTESTINAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000322','GATO','Himalayan',NULL,'chronic_enteropathy','Enteropatia crônica','GASTROINTESTINAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000323','GATO','Maine_coon','maine-coon','CKD','Doença renal crônica','RENAL',2,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000324','GATO','Maine_coon','maine-coon','FEK','Ceratoconjuntivite eosinofílica felina','OFTALMOLOGICA',2,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000325','GATO','Maine_coon','maine-coon','HCM','Cardiomiopatia hipertrófica','CARDIACA',2,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000326','GATO','Maine_coon','maine-coon','IBD','Doença inflamatória intestinal','GASTROINTESTINAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000327','GATO','Maine_coon','maine-coon','SCAL','Linfoma alimentar de pequenas células','ONCOLOGICA/GASTROINTESTINAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000328','GATO','Maine_coon','maine-coon','all_GI','Doença gastrointestinal (grupo combinado)','GASTROINTESTINAL',2,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000329','GATO','Manx',NULL,'CKD','Doença renal crônica','RENAL',2,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000330','GATO','Manx',NULL,'SCAL','Linfoma alimentar de pequenas células','ONCOLOGICA/GASTROINTESTINAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000331','GATO','Manx',NULL,'all_GI','Doença gastrointestinal (grupo combinado)','GASTROINTESTINAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000332','GATO','Manx',NULL,'hypercalcemia','Hipercalcemia','METABOLICA',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000333','GATO','Manx',NULL,'hyperthyroidism','Hipertireoidismo','ENDOCRINA',2,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000334','GATO','Persian','persa','CKD','Doença renal crônica','RENAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000335','GATO','Persian','persa','IBD','Doença inflamatória intestinal','GASTROINTESTINAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000336','GATO','Persian','persa','all_GI','Doença gastrointestinal (grupo combinado)','GASTROINTESTINAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000337','GATO','Persian','persa','hypercalcemia','Hipercalcemia','METABOLICA',2,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000338','GATO','Ragdoll','ragdoll','HCM','Cardiomiopatia hipertrófica','CARDIACA',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000339','GATO','Ragdoll','ragdoll','chronic_enteropathy','Enteropatia crônica','GASTROINTESTINAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000340','GATO','Siamese','siames','DM','Diabetes mellitus','ENDOCRINA',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000341','GATO','Siamese','siames','HCM','Cardiomiopatia hipertrófica','CARDIACA',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000342','GATO','Siamese','siames','IBD','Doença inflamatória intestinal','GASTROINTESTINAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000343','GATO','Siamese','siames','hyperthyroidism','Hipertireoidismo','ENDOCRINA',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000344','REPTIL','Blotched Blue-tongue',NULL,'RESCUE_DISEASE','Doença ou condição clínica não especificada','CLINICA_NAO_ESPECIFICADA',2,0,'Interactions between reptiles and people: A perspective from wildlife rehabilitation records','10.5061/dryad.jh9w0vtmc',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000345','REPTIL','Carpet Python',NULL,'RESCUE_DISEASE','Doença ou condição clínica não especificada','CLINICA_NAO_ESPECIFICADA',8,0,'Interactions between reptiles and people: A perspective from wildlife rehabilitation records','10.5061/dryad.jh9w0vtmc',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000346','REPTIL','Eastern Bearded Dragon',NULL,'RESCUE_DISEASE','Doença ou condição clínica não especificada','CLINICA_NAO_ESPECIFICADA',5,0,'Interactions between reptiles and people: A perspective from wildlife rehabilitation records','10.5061/dryad.jh9w0vtmc',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000347','REPTIL','Eastern Blue-tongue',NULL,'RESCUE_DISEASE','Doença ou condição clínica não especificada','CLINICA_NAO_ESPECIFICADA',39,0,'Interactions between reptiles and people: A perspective from wildlife rehabilitation records','10.5061/dryad.jh9w0vtmc',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000348','REPTIL','Green Tree Snake',NULL,'RESCUE_DISEASE','Doença ou condição clínica não especificada','CLINICA_NAO_ESPECIFICADA',1,0,'Interactions between reptiles and people: A perspective from wildlife rehabilitation records','10.5061/dryad.jh9w0vtmc',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000349','REPTIL','Highland Copperhead',NULL,'RESCUE_DISEASE','Doença ou condição clínica não especificada','CLINICA_NAO_ESPECIFICADA',1,0,'Interactions between reptiles and people: A perspective from wildlife rehabilitation records','10.5061/dryad.jh9w0vtmc',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000350','REPTIL','Lace Monitor',NULL,'RESCUE_DISEASE','Doença ou condição clínica não especificada','CLINICA_NAO_ESPECIFICADA',3,0,'Interactions between reptiles and people: A perspective from wildlife rehabilitation records','10.5061/dryad.jh9w0vtmc',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000351','REPTIL','Land Mullet',NULL,'RESCUE_DISEASE','Doença ou condição clínica não especificada','CLINICA_NAO_ESPECIFICADA',4,0,'Interactions between reptiles and people: A perspective from wildlife rehabilitation records','10.5061/dryad.jh9w0vtmc',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000352','REPTIL','Pink-tongued Skink',NULL,'RESCUE_DISEASE','Doença ou condição clínica não especificada','CLINICA_NAO_ESPECIFICADA',1,0,'Interactions between reptiles and people: A perspective from wildlife rehabilitation records','10.5061/dryad.jh9w0vtmc',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000353','REPTIL','Shingleback',NULL,'RESCUE_DISEASE','Doença ou condição clínica não especificada','CLINICA_NAO_ESPECIFICADA',1,0,'Interactions between reptiles and people: A perspective from wildlife rehabilitation records','10.5061/dryad.jh9w0vtmc',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000354','REPTIL','Tiger Snake',NULL,'RESCUE_DISEASE','Doença ou condição clínica não especificada','CLINICA_NAO_ESPECIFICADA',1,0,'Interactions between reptiles and people: A perspective from wildlife rehabilitation records','10.5061/dryad.jh9w0vtmc',SYSTIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000355','REPTIL','Water Dragon',NULL,'RESCUE_DISEASE','Doença ou condição clínica não especificada','CLINICA_NAO_ESPECIFICADA',4,0,'Interactions between reptiles and people: A perspective from wildlife rehabilitation records','10.5061/dryad.jh9w0vtmc',SYSTIMESTAMP);

-- ------------------------------------------------------------
-- 3) O cache do parecer da IA
-- ------------------------------------------------------------

CREATE TABLE t_clyvo_parecer_ia (
    id         VARCHAR2(36)  NOT NULL,
    animal_id  VARCHAR2(36)  NOT NULL,
    origem     VARCHAR2(20)  NOT NULL,
    modelo     VARCHAR2(120) NULL,
    conteudo   CLOB          NOT NULL,
    gerado_em  TIMESTAMP     NOT NULL,
    valido_ate TIMESTAMP     NOT NULL,
    CONSTRAINT pk_parecer_ia PRIMARY KEY (id),
    CONSTRAINT uk_parecer_ia_animal UNIQUE (animal_id),
    CONSTRAINT fk_parecer_ia_animal FOREIGN KEY (animal_id) REFERENCES t_clyvo_animal (id),
    CONSTRAINT ck_parecer_ia_origem CHECK (origem IN ('IA','REGRAS'))
);
