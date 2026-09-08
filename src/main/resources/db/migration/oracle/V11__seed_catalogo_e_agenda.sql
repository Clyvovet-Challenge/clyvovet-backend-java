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
