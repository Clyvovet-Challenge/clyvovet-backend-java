-- ============================================================================
-- V21 — os dois eventos do seed que estao no futuro voltam a ser AGENDADOS
-- ============================================================================
--
-- O SINTOMA
--
-- Dois atendimentos que ainda nao aconteceram constavam como REALIZADOS:
--
--   ...0006  15/12/2026  consulta do Bolinha com a Camila (VetCare)
--   ...0009  22/12/2026  exame do animal 2 com o Rafael Matos (PetMed)
--
-- O primeiro foi visto nas capturas de tela: a lista de agendamentos da Camila
-- comecava vazia. O segundo apareceu no teste da regra geral desta migration.
--
-- A CAUSA
--
-- A V2 semeou os eventos antes de existir status. A V5 criou status_evento com
-- DEFAULT 'REALIZADO' para todo evento ja gravado -- decisao certa para o
-- passado, que ela documenta, mas que pegou tambem os dois eventos no futuro.
--
-- A CORRECAO
--
-- So estes dois, pelo id. O "AND status_evento = 'REALIZADO'" deixa a
-- migration inofensiva se alguem ja tiver mudado o status a mao: ela so
-- desfaz o default da V5, nunca uma decisao de alguem.
--
-- O SQL e o mesmo nos dois conjuntos (oracle/ e mysql/), porque o erro
-- estava nos dois.
-- ============================================================================

UPDATE t_clyvo_evento_clinico
   SET status_evento = 'AGENDADO'
 WHERE id IN ('55555555-5555-5555-5555-000000000006',
              '55555555-5555-5555-5555-000000000009')
   AND status_evento = 'REALIZADO';
