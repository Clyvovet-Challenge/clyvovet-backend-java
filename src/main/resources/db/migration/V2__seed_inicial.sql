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
