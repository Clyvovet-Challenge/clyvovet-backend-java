-- ============================================================================
-- V18 — o lembrete que de fato se repete
-- ============================================================================
--
-- O QUE ESTAVA QUEBRADO
--
-- A tabela tinha `recorrente NUMBER(1)`, a API o gravava, o app o oferecia num
-- seletor "Uma vez / Recorrente" -- e NADA NO SISTEMA AGIA SOBRE ELE. O
-- LembreteNotificationService da .NET notificava e marcava o lembrete como
-- Enviado, ponto. Um lembrete marcado recorrente disparava UMA VEZ e morria.
--
-- Ou seja: a interface prometia repeticao, o banco guardava a intencao, e a
-- repeticao nunca acontecia. Pior que feature faltando -- e feature que mente.
--
-- Verificado no codigo antes de escrever esta migration: `Recorrente` aparecia
-- em oito lugares da .NET, todos de leitura, gravacao ou mapeamento. Nenhum
-- decidia nada com ele.
--
-- O QUE ENTRA
--
-- `intervalo_dias`: de quantos em quantos dias o lembrete volta. NULO significa
-- "nao repete", e e o default -- entao toda linha que ja existe continua com o
-- comportamento de antes, sem UPDATE nenhum.
--
-- `repetir_ate`: o fim da serie. NULO significa "sem fim previsto", que e o caso
-- do antipulgas mensal. Preenchido, e o caso do antibiotico de dez dias -- e e
-- exatamente o "de x dia ate y dia" que faltava: `agendado_em` e o comeco,
-- `repetir_ate` e o fim.
--
-- POR QUE INTERVALO EM DIAS, E NAO UMA UNIDADE (DIA/SEMANA/MES)
--
-- Mes nao tem duracao fixa, e a pergunta "a cada 1 mes a partir de 31/01" nao
-- tem resposta unica. Em dose de medicamento e em antipulgas, o que o
-- veterinario quer dizer e "a cada 30 dias" mesmo -- o intervalo, nao o dia do
-- calendario. Um inteiro em dias nao tem ambiguidade, e a tela converte a
-- escolha do usuario ("a cada 1 mes") para 30 na hora de enviar.
--
-- POR QUE `recorrente` FICA
--
-- Ela continua na tabela e passa a ser DERIVADA: a .NET grava
-- `recorrente = intervalo_dias IS NOT NULL`. Remover a coluna quebraria o
-- LembreteResponse, que o app le hoje. Deixar as duas divergirem seria pior --
-- daí a derivacao, num unico ponto do service.
--
-- COMO A SERIE ANDA (decisao que evitou uma tabela)
--
-- Ao notificar um lembrete com intervalo, a .NET NAO cria uma linha nova: ela
-- empurra `agendado_em` para a frente e mantem o status Pendente. Quando a
-- proxima data passa de `repetir_ate`, ai sim marca Enviado -- a serie
-- terminou.
--
-- Clonar a cada disparo daria historico, e custaria uma linha por ocorrencia
-- (36 por ano num lembrete mensal), uma coluna de origem para agrupar a serie, e
-- uma lista que o tutor abriria para ver trinta e seis vezes o mesmo antipulgas.
-- Um lembrete e "a proxima coisa a fazer", nao um diario -- o historico clinico
-- mora no prontuario.
-- ============================================================================

ALTER TABLE t_clyvo_lembrete ADD (
    intervalo_dias NUMBER(5)  NULL,
    repetir_ate    TIMESTAMP  NULL
);

-- Intervalo zero ou negativo faria a serie andar para tras ou nao andar --
-- e o laco que avanca a data nunca sairia do lugar.
ALTER TABLE t_clyvo_lembrete
    ADD CONSTRAINT chk_lembrete_intervalo
    CHECK (intervalo_dias IS NULL OR intervalo_dias > 0);

-- A varredura do notificador pergunta "pendentes vencendo ate X". Com serie, ela
-- passa a reordenar a mesma linha muitas vezes ao longo do tempo; o indice por
-- (status, agendado_em) e o que mantem essa consulta barata quando a tabela
-- cresce.
CREATE INDEX idx_lembrete_varredura ON t_clyvo_lembrete (status, agendado_em);
