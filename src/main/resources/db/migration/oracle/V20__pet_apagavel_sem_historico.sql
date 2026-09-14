-- ============================================================================
-- V20 — o pet sem histórico volta a ser apagável
-- ============================================================================
--
-- O SINTOMA
--
-- Excluir um animal recém-cadastrado respondia 409, e o app traduzia isso como
-- "Este pet tem histórico clínico registrado e não pode ser excluído" — numa
-- ficha com ZERO atendimentos. A mensagem estava errada porque o 409 não vinha
-- de regra de negócio nenhuma: `AnimalService.deletar()` chama `deleteById`
-- direto, sem validação. Quem recusava era o banco, por chave estrangeira.
--
-- A CAUSA
--
-- Quatro tabelas apontam para t_clyvo_animal com NO ACTION, e TRÊS delas são
-- escritas pela API .NET — que compartilha este banco mas não compartilha o
-- código. A API Java apaga o animal sem saber que essas linhas existem.
--
-- A pior é t_clyvo_parecer_ia: o widget de saúde preditiva roda na home do
-- tutor, sobre o pet em destaque, e grava um parecer. Ou seja, bastava o tutor
-- ABRIR o aplicativo para o pet ficar indelével para sempre. Não havia como
-- chegar ao botão de excluir sem antes cruzar a tela que criava o impedimento.
--
-- A DECISÃO
--
-- Estas quatro são dado DERIVADO: só existem porque o animal existe, e não
-- significam nada sem ele. Um parecer de IA sobre um animal apagado, um
-- lembrete de vacina para quem não está mais lá, uma sugestão de ração, um
-- pedido de correção de cadastro — todos viram lixo no mesmo instante. Seguem
-- o animal, como t_clyvo_acesso_historico, t_clyvo_alerta_clinico e
-- t_clyvo_autorizacao_acesso já seguiam desde que foram criadas.
--
-- O QUE CONTINUA BLOQUEANDO, DE PROPÓSITO
--
-- t_clyvo_evento_clinico e t_clyvo_documento_clinico ficam em NO ACTION. Esses
-- são o prontuário — a razão de o produto existir. Recusar a exclusão de um
-- animal com atendimento ou laudo registrado é a regra correta, e é o caso em
-- que a mensagem do app passa a ser verdadeira em vez de genérica.
--
-- DROP CONSTRAINT, e não DROP FOREIGN KEY: a primeira funciona nos três bancos
-- (Oracle, MySQL 8.0.19+ e H2), como a V4 já documentou. Os testes rodam em H2
-- no MODE=Oracle, então sintaxe exclusiva de MySQL aqui derrubaria a suíte.
-- ============================================================================

-- Parecer da saúde preditiva (escrito pela API .NET).
ALTER TABLE t_clyvo_parecer_ia DROP CONSTRAINT fk_parecer_ia_animal;
ALTER TABLE t_clyvo_parecer_ia
    ADD CONSTRAINT fk_parecer_ia_animal FOREIGN KEY (animal_id)
    REFERENCES t_clyvo_animal (id) ON DELETE CASCADE;

-- Lembretes de vacina, medicação e higiene (escritos pela API .NET).
ALTER TABLE t_clyvo_lembrete DROP CONSTRAINT fk_lembrete_animal;
ALTER TABLE t_clyvo_lembrete
    ADD CONSTRAINT fk_lembrete_animal FOREIGN KEY (animal_id)
    REFERENCES t_clyvo_animal (id) ON DELETE CASCADE;

-- Indicações de produto para aquele pet (escritas pela API .NET).
ALTER TABLE t_clyvo_sugestao_produto DROP CONSTRAINT fk_sugestao_animal;
ALTER TABLE t_clyvo_sugestao_produto
    ADD CONSTRAINT fk_sugestao_animal FOREIGN KEY (animal_id)
    REFERENCES t_clyvo_animal (id) ON DELETE CASCADE;

-- Pedidos do veterinário para o tutor corrigir um dado do cadastro.
ALTER TABLE t_clyvo_solicitacao_alteracao DROP CONSTRAINT fk_solicitacao_animal;
ALTER TABLE t_clyvo_solicitacao_alteracao
    ADD CONSTRAINT fk_solicitacao_animal FOREIGN KEY (animal_id)
    REFERENCES t_clyvo_animal (id) ON DELETE CASCADE;
