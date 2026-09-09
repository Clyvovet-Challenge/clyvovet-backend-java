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
