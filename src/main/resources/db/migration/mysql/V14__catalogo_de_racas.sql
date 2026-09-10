-- ============================================================
-- V14 -- Catalogo de racas: a raca deixa de ser texto que cada um escreve
--        do seu jeito e passa a ter identidade (MySQL)
--
-- O QUE ESTAVA ERRADO
--
-- `t_clyvo_animal.raca` e `especie` sao VARCHAR livres. O app oferece lista
-- fechada para especie, porte e sexo, mas a API aceita qualquer coisa -- entao
-- o seed e as chamadas diretas encheram a tabela de grafias concorrentes. Medido
-- neste banco:
--
--   especie "cachorro"  ->  Cachorro | CAO | CANINO        (3 grafias)
--   especie "gato"      ->  Gato | GATO | FELINO           (3 grafias)
--   sem raca definida   ->  SRD | Vira | Vira-lata         (3 grafias, 9 animais)
--   sem acento          ->  Pastor Alemao, Bulldog Frances
--
-- ISSO JA ESTAVA QUEBRANDO COISA, NAO E SO ESTETICA
--
-- O widget de saude preditiva da API .NET casa raca para sugerir risco de
-- doenca. Como o texto nao e padronizado, ele compara por substring:
--
--     return a.Contains(b) || b.Contains(a);
--
-- e o proprio comentario dele explica: "a raca do Animal vem de texto livre da
-- API Java, sem padronizacao". Isso erra dos dois lados -- "Siames" nao casa com
-- "Siames" acentuado, e uma predisposicao cadastrada como "Terrier" casa com
-- Yorkshire, Bull e Fox Terrier, que tem predisposicoes diferentes. Num recurso
-- que fala de doenca.
--
-- A COLUNA `chave` E O CORACAO DESTA MIGRACAO
--
-- Ela e o mesmo identificador em tres lugares: a arte do animal no app, a
-- predisposicao na .NET e a busca no cliente. Ninguem precisa normalizar nada,
-- porque nao ha o que normalizar: quem escolhe do catalogo ja recebe a chave
-- pronta. E `UNIQUE`, entao duas linhas nao podem disputar o mesmo significado.
--
-- POR QUE A FK NASCE ANULAVEL E `raca` (TEXTO) FICA
--
-- Sao 200 e poucas racas de cachorro; o catalogo lista as comuns e "outra raca"
-- precisa continuar existindo. Alem disso, manter a coluna de texto e o que faz
-- esta migracao NAO QUEBRAR NADA: todo SELECT existente, o widget da .NET e o
-- `AnimalResponse` continuam lendo `raca` como sempre leram. O service passa a
-- preencher esse texto a partir do catalogo, entao ele vira uma copia sempre
-- coerente em vez de uma segunda fonte de verdade.
--
-- A regra, dita uma vez: quando `raca_id` esta preenchido, `raca` e copia do
-- catalogo. Quando esta nulo, `raca` e o que o tutor digitou.
-- ============================================================

CREATE TABLE t_clyvo_raca (
    id            VARCHAR(36)  NOT NULL,
    -- Nao e FK para uma tabela de especie: especie e lista fechada e pequena
    -- (cinco valores), entao vale mais um CHECK aqui do que uma tabela a mais.
    especie       VARCHAR(20)  NOT NULL,
    -- O que o tutor le na tela.
    nome          VARCHAR(100) NOT NULL,
    -- O que o codigo usa. Minusculo, sem acento, hifenizado.
    chave         VARCHAR(60)  NOT NULL,
    -- Pre-preenche o porte no cadastro. Anulavel porque nem toda raca tem porte
    -- previsivel -- e SRD, por definicao, nao tem.
    porte_tipico  VARCHAR(20)  NULL,
    -- Raca desativada some do seletor mas continua valendo para quem ja a
    -- escolheu. Mesma razao de `t_clyvo_servico.ativo`: apagar a linha levaria
    -- junto o significado do que ja foi cadastrado.
    --
    -- INT, e nao TINYINT. O NumericBooleanConverter do Hibernate mapeia boolean
    -- para INTEGER, e o perfil `mysql` roda com ddl-auto=validate -- com TINYINT
    -- a aplicacao NAO SOBE, com "wrong column type ... found [tinyint], but
    -- expecting [integer]". Todas as outras colunas booleanas do schema seguem
    -- isto; a V3 ate deixou anotado o mapeamento "NUMBER(1) -> INT".
    ativo         INT          NOT NULL DEFAULT 1,
    CONSTRAINT pk_raca PRIMARY KEY (id),
    CONSTRAINT uk_raca_chave UNIQUE (chave),
    CONSTRAINT ck_raca_especie CHECK (especie IN ('CAO','GATO','ROEDOR','AVE','REPTIL'))
);

CREATE INDEX ix_raca_especie ON t_clyvo_raca (especie, ativo);

-- ------------------------------------------------------------
-- O CATALOGO
--
-- Deliberadamente curto. Duas razoes, e as duas apontam para o mesmo tamanho:
--
--   1. um seletor com 200 itens e pior que um campo de texto;
--   2. cada linha daqui vai ganhar uma arte em pixel, e o catalogo nao pode
--      crescer mais rapido do que alguem consegue desenhar.
--
-- A ultima linha de cada especie e o "sem raca definida". Ela nao e um caso de
-- borda: sao 9 dos 28 animais deste banco.
-- ------------------------------------------------------------

INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES
 ('22222222-0000-0000-0000-000000000001','CAO','Golden Retriever','golden-retriever','GRANDE'),
 ('22222222-0000-0000-0000-000000000002','CAO','Labrador Retriever','labrador-retriever','GRANDE'),
 ('22222222-0000-0000-0000-000000000003','CAO','Pastor Alemão','pastor-alemao','GRANDE'),
 ('22222222-0000-0000-0000-000000000004','CAO','Border Collie','border-collie','MEDIO'),
 ('22222222-0000-0000-0000-000000000005','CAO','Poodle','poodle','PEQUENO'),
 ('22222222-0000-0000-0000-000000000006','CAO','Bulldog Francês','bulldog-frances','PEQUENO'),
 ('22222222-0000-0000-0000-000000000007','CAO','Dachshund','dachshund','PEQUENO'),
 ('22222222-0000-0000-0000-000000000008','CAO','Shih Tzu','shih-tzu','PEQUENO'),
 ('22222222-0000-0000-0000-000000000009','CAO','Yorkshire Terrier','yorkshire-terrier','PEQUENO'),
 ('22222222-0000-0000-0000-000000000010','CAO','Pug','pug','PEQUENO'),
 ('22222222-0000-0000-0000-000000000011','CAO','Rottweiler','rottweiler','GRANDE'),
 ('22222222-0000-0000-0000-000000000012','CAO','Beagle','beagle','MEDIO'),
 ('22222222-0000-0000-0000-000000000013','CAO','Pinscher','pinscher','PEQUENO'),
 ('22222222-0000-0000-0000-000000000014','CAO','Chihuahua','chihuahua','PEQUENO'),
 ('22222222-0000-0000-0000-000000000015','CAO','Husky Siberiano','husky-siberiano','GRANDE'),
 ('22222222-0000-0000-0000-000000000016','CAO','Maltês','maltes','PEQUENO'),
 ('22222222-0000-0000-0000-000000000017','CAO','Cocker Spaniel','cocker-spaniel','MEDIO'),
 ('22222222-0000-0000-0000-000000000018','CAO','Boxer','boxer','GRANDE'),
 ('22222222-0000-0000-0000-000000000019','CAO','Sem raça definida','srd-cao',NULL);

INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES
 ('22222222-0000-0000-0000-000000000101','GATO','Siamês','siames','PEQUENO'),
 ('22222222-0000-0000-0000-000000000102','GATO','Persa','persa','PEQUENO'),
 ('22222222-0000-0000-0000-000000000103','GATO','Maine Coon','maine-coon','GRANDE'),
 ('22222222-0000-0000-0000-000000000104','GATO','Angorá','angora','PEQUENO'),
 ('22222222-0000-0000-0000-000000000105','GATO','Bengal','bengal','MEDIO'),
 ('22222222-0000-0000-0000-000000000106','GATO','Sphynx','sphynx','PEQUENO'),
 ('22222222-0000-0000-0000-000000000107','GATO','Ragdoll','ragdoll','MEDIO'),
 ('22222222-0000-0000-0000-000000000108','GATO','British Shorthair','british-shorthair','MEDIO'),
 ('22222222-0000-0000-0000-000000000109','GATO','Sem raça definida','srd-gato',NULL);

INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES
 ('22222222-0000-0000-0000-000000000201','ROEDOR','Hamster Sírio','hamster-sirio','PEQUENO'),
 ('22222222-0000-0000-0000-000000000202','ROEDOR','Hamster Anão Russo','hamster-anao-russo','PEQUENO'),
 ('22222222-0000-0000-0000-000000000203','ROEDOR','Porquinho-da-índia','porquinho-da-india','PEQUENO'),
 ('22222222-0000-0000-0000-000000000204','ROEDOR','Coelho','coelho','PEQUENO'),
 ('22222222-0000-0000-0000-000000000205','ROEDOR','Chinchila','chinchila','PEQUENO'),
 ('22222222-0000-0000-0000-000000000206','ROEDOR','Gerbil','gerbil','PEQUENO'),
 ('22222222-0000-0000-0000-000000000209','ROEDOR','Outro roedor','srd-roedor',NULL);

INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES
 ('22222222-0000-0000-0000-000000000301','AVE','Calopsita','calopsita','PEQUENO'),
 ('22222222-0000-0000-0000-000000000302','AVE','Periquito','periquito','PEQUENO'),
 ('22222222-0000-0000-0000-000000000303','AVE','Canário','canario','PEQUENO'),
 ('22222222-0000-0000-0000-000000000304','AVE','Papagaio','papagaio','MEDIO'),
 ('22222222-0000-0000-0000-000000000309','AVE','Outra ave','srd-ave',NULL);

INSERT INTO t_clyvo_raca (id, especie, nome, chave, porte_tipico) VALUES
 ('22222222-0000-0000-0000-000000000401','REPTIL','Jabuti','jabuti','PEQUENO'),
 ('22222222-0000-0000-0000-000000000402','REPTIL','Tartaruga-d''água','tartaruga-dagua','PEQUENO'),
 ('22222222-0000-0000-0000-000000000403','REPTIL','Iguana','iguana','MEDIO'),
 ('22222222-0000-0000-0000-000000000404','REPTIL','Gecko','gecko','PEQUENO'),
 ('22222222-0000-0000-0000-000000000409','REPTIL','Outro réptil','srd-reptil',NULL);

-- ------------------------------------------------------------
-- A LIGACAO COM O ANIMAL
-- ------------------------------------------------------------

ALTER TABLE t_clyvo_animal ADD COLUMN raca_id VARCHAR(36) NULL;

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
