-- ============================================================
-- V8 — Separa "o pet apareceu?" de "o prontuário foi fechado?"
--
-- POR QUE ESTA MIGRATION EXISTE
-- A regra R1 da spec 08 diz que um evento com data de hoje ou passada
-- nasce REALIZADO: chamar de AGENDADO um atendimento cuja data já passou
-- é afirmar o que não aconteceu, e a varredura de faltas (R18) marcava
-- FALTOU um pet que tinha sido atendido.
--
-- Só que POST /eventos-clinicos/{id}/concluir — o único lugar que grava
-- peso, desfecho e retorno previsto — recusava evento REALIZADO, para
-- impedir a segunda conclusão. Aplicar R1 sozinha, portanto, tirava do
-- veterinário a única forma de registrar os dados clínicos de um
-- atendimento lançado retroativamente. Uma regra do documento apagava
-- uma capacidade do produto.
--
-- O conflito existia porque UMA coluna respondia DUAS perguntas:
--
--   status_evento  o pet apareceu?          AGENDADO | REALIZADO | FALTOU | CANCELADO
--   concluido_em   o prontuário foi fechado? NULL enquanto não foi
--
-- São independentes. Um atendimento pode ser REALIZADO desde o primeiro
-- instante (o vet registra o que fez ontem) e só ter o prontuário
-- fechado depois. Com as duas separadas, R1 vale e "não conclui duas
-- vezes" fica mais firme do que estava: antes um /concluir com corpo
-- vazio não deixava marca nenhuma, e a segunda chamada era barrada só
-- pelo status.
--
-- SOBRE O BACKFILL
-- Todo evento REALIZADO já gravado recebe concluido_em = data_evento.
-- Deixá-los NULL abriria a porta para reconcluir todo o histórico e
-- sobrescrever desfecho já registrado. A data do atendimento é a melhor
-- aproximação disponível: não existe registro de quando o prontuário foi
-- fechado, porque essa informação nunca foi guardada.
--
-- TIMESTAMP e não DATE: aqui o instante importa, ao contrário de
-- data_evento. Ver o cabeçalho da V3 sobre fuso — este campo é gravado e
-- lido pela aplicação, nunca comparado com data do usuário.
-- ============================================================

ALTER TABLE evento_clinico ADD concluido_em TIMESTAMP;

UPDATE evento_clinico
   SET concluido_em = CAST(data_evento AS TIMESTAMP)
 WHERE status_evento = 'REALIZADO'
   AND concluido_em IS NULL;

-- Um atendimento que não aconteceu não tem prontuário para fechar.
ALTER TABLE evento_clinico ADD CONSTRAINT chk_evento_conclusao
    CHECK (concluido_em IS NULL OR status_evento = 'REALIZADO');
