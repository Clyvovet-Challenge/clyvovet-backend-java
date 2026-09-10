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
