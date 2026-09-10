-- ============================================================
-- V15 -- A base de doencas para a IA de saude preditiva, e o conserto
--        do widget que nunca funcionou (MySQL)
--
-- DUAS COISAS AQUI, E ELAS SAO A MESMA HISTORIA
--
-- 1) O CONSERTO. A V8 converteu o schema Oracle da API .NET para ca e, na
--    t_clyvo_predisposicao_saude, DEIXOU UMA COLUNA PARA TRAS: o arquivo
--    original (ClyvoVet-api/schema/04) declara `criado_em TIMESTAMP DEFAULT
--    SYSTIMESTAMP`, o EF da .NET mapeia a coluna -- e ela nao existia aqui.
--    Todo GET /widget-saude-preditiva respondia 500 com "Unknown column
--    't.criado_em'". E o seed de 42 predisposicoes (schema/05) nunca foi
--    portado, entao mesmo sem o 500 a tabela responderia vazio. Ninguem pegou
--    porque os testes de integracao da .NET rodam em UseInMemoryDatabase, que
--    cria o schema a partir do modelo -- nunca do schema real.
--
-- 2) A BASE NOVA. A feature de IA (parecer de saude preditiva via OCI
--    Generative AI) precisa de fatos para citar, nao de opiniao do modelo.
--    t_clyvo_base_doencas guarda a agregacao de dois datasets publicos com DOI
--    (Dryad), preparados pelo time de dados: contagem de casos e controles por
--    especie, raca e doenca. O LLM redige em cima disso; quando ele esta fora
--    do ar, o mesmo dado alimenta o parecer deterministico. Uma fonte, dois
--    consumidores.
--
-- COMO A AGREGACAO FOI FEITA (e por que nao sao os CSVs crus)
--
-- - Caes vem SO do dataset Dryad 10.5061/dryad.266k4. O CSV multiespecies
--   amostra os mesmos registros de cao; usar os dois dobraria a contagem.
-- - Codigos com sufixo de raca (MCT_labradorRetrievers, GC_boxers_bulldogs)
--   sao o MESMO caso re-rotulado para o estudo por raca; foram normalizados
--   para o codigo generico e deduplicados por (animal, doenca).
-- - Medidas continuas (angulo de Norberg da displasia coxofemoral) NAO viram
--   caso: sao medicao, nao diagnostico, e nao inferimos doenca de medida.
-- - Linhas so de controles ficaram fora: controle nao afirma predisposicao.
-- - AVE sao passeriformes selvagens de Galapagos (so variola aviaria) e REPTIL
--   sao registros de resgate SEM diagnostico -- por isso raca_chave e NULL
--   nessas especies e o servico as trata como "base de referencia limitada".
--
-- `raca_chave` LIGA NA V14 DE PROPOSITO
--
-- E a mesma chave de t_clyvo_raca ('labrador-retriever', 'siames'...). O
-- servico da .NET casa por igualdade de chave -- exatamente o que a V14
-- construiu para aposentar o `Contains` de substring do widget.
--
-- t_clyvo_parecer_ia E CACHE COM ENDERECO
--
-- Um parecer por animal (UNIQUE), com validade. E o que transforma "chamar um
-- LLM na home" em uma chamada por animal por semana -- controle de custo dos
-- creditos OCI e de latencia da tela. `origem` distingue parecer da IA de
-- parecer das regras, porque o app mostra isso ao tutor.
-- ============================================================

-- ------------------------------------------------------------
-- 1) O conserto da V8
-- ------------------------------------------------------------

ALTER TABLE t_clyvo_predisposicao_saude
    ADD COLUMN criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- As 42 predisposicoes do arquivo original da .NET (schema/05), nunca portadas.
-- IDs fixos, como em toda migracao com seed deste repositorio.

INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000001', 'CACHORRO', 'Labrador', 6, 'Displasia de quadril', 'Agendar avaliacao ortopedica e considerar suplementacao articular preventiva.', 'VetCompass (RVC) - Labrador Retrievers under primary veterinary care in the UK', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000002', 'CACHORRO', 'Labrador', 7, 'Obesidade', 'Reavaliar dieta e nivel de atividade fisica; agendar checkup nutricional.', 'VetCompass (RVC) - Labrador Retrievers under primary veterinary care in the UK', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000003', 'CACHORRO', 'Dachshund', 3, 'Doenca de disco intervertebral (hernia)', 'Evitar impacto/escadas e agendar avaliacao neurologica se houver dor ou dificuldade de locomocao.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000004', 'CACHORRO', 'Bulldog Frances', 0, 'Sindrome respiratoria braquicefalica', 'Evitar exercicio intenso e calor; agendar avaliacao respiratoria com veterinario.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000005', 'CACHORRO', 'Bulldog Ingles', 0, 'Sindrome respiratoria braquicefalica', 'Evitar exercicio intenso e calor; agendar avaliacao respiratoria com veterinario.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000006', 'CACHORRO', 'Pastor Alemao', 7, 'Displasia de quadril', 'Agendar avaliacao ortopedica preventiva.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000007', 'CACHORRO', 'Pastor Alemao', 8, 'Mielopatia degenerativa', 'Monitorar fraqueza em membros posteriores; agendar avaliacao neurologica.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000008', 'CACHORRO', 'Yorkshire', 2, 'Luxacao de patela', 'Agendar avaliacao ortopedica se houver claudicacao intermitente.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000009', 'CACHORRO', 'Poodle', 4, 'Atrofia progressiva de retina', 'Agendar avaliacao oftalmologica preventiva anual.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000010', 'CACHORRO', 'Golden Retriever', 8, 'Predisposicao a neoplasias (linfoma, hemangiossarcoma)', 'Agendar checkup geral com exames de rotina a partir dessa idade.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000011', 'CACHORRO', 'Rottweiler', 5, 'Displasia de cotovelo', 'Agendar avaliacao ortopedica se houver claudicacao.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000012', 'CACHORRO', 'Chihuahua', 5, 'Colapso de traqueia', 'Evitar coleira (preferir peitoral) e agendar avaliacao respiratoria se houver tosse seca persistente.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000013', 'CACHORRO', 'Cavalier King Charles Spaniel', 4, 'Doenca valvar mitral', 'Agendar avaliacao cardiologica preventiva (ausculta/ecocardiograma).', 'VetCompass (RVC) - Disorders in Cavalier King Charles Spaniels attending primary-care practices in England', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000014', 'CACHORRO', 'Pug', 0, 'Sindrome respiratoria braquicefalica e dermatite de dobras cutaneas', 'Higienizar dobras de pele regularmente e evitar exercicio intenso em dias quentes.', 'VetCompass (RVC) - Health of Pug dogs in the UK: disorder predispositions and protections', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000015', 'GATO', 'Persa', 3, 'Doenca renal policistica (PKD)', 'Agendar ultrassonografia renal preventiva.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000016', 'GATO', 'Persa', 0, 'Sindrome respiratoria braquicefalica', 'Monitorar respiracao ruidosa; evitar calor excessivo.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000017', 'GATO', 'Siames', 2, 'Cardiomiopatia hipertrofica', 'Agendar avaliacao cardiologica preventiva.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000018', 'GATO', 'Maine Coon', 2, 'Cardiomiopatia hipertrofica', 'Agendar avaliacao cardiologica preventiva (raca com predisposicao genetica conhecida).', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000019', 'GATO', 'SRD', 7, 'Obesidade e diabetes mellitus', 'Reavaliar dieta e agendar exame de glicemia preventivo.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000020', 'PASSARO', 'Calopsita', 0, 'Deficiencia de calcio', 'Revisar dieta (suplementacao de calcio e exposicao a luz UV adequada).', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000021', 'PASSARO', 'Periquito', 0, 'Deficiencia de calcio', 'Revisar dieta (suplementacao de calcio e exposicao a luz UV adequada).', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000022', 'PASSARO', 'Papagaio', 0, 'Doenca respiratoria por ma ventilacao (aspergilose)', 'Melhorar ventilacao do ambiente e agendar avaliacao respiratoria se houver espirros/secrecao.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000023', 'PASSARO', 'Calopsita', 5, 'Tumores (lipoma, tumor renal)', 'Agendar checkup geral a partir dessa idade.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000024', 'REPTIL', 'Tartaruga', 0, 'Doenca ossea metabolica (deficit de UV/calcio)', 'Revisar exposicao a luz UVB e suplementacao de calcio.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000025', 'REPTIL', 'Iguana', 0, 'Doenca ossea metabolica (deficit de UV/calcio)', 'Revisar exposicao a luz UVB e suplementacao de calcio.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000026', 'REPTIL', 'Jabuti', 0, 'Infeccao respiratoria por temperatura inadequada', 'Revisar temperatura e umidade do terrario; agendar avaliacao se houver secrecao nasal.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000027', 'ROEDOR', 'Coelho', 0, 'Estase gastrointestinal', 'Revisar dieta rica em fibras (feno) e agendar avaliacao se houver reducao de apetite.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000028', 'ROEDOR', 'Coelho', 0, 'Ma oclusao dentaria', 'Agendar avaliacao odontologica se houver dificuldade para se alimentar.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000029', 'ROEDOR', 'Hamster', 1.5, 'Tumor adrenal', 'Agendar checkup geral a partir dessa idade.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000030', 'ROEDOR', 'Porquinho-da-india', 0, 'Ma oclusao dentaria', 'Agendar avaliacao odontologica se houver dificuldade para se alimentar.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000031', 'ROEDOR', 'Chinchila', 0, 'Golpe de calor (sensibilidade termica)', 'Manter ambiente fresco e ventilado, evitar exposicao a temperaturas acima de 25 graus.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000032', 'BOVINO', 'Holandesa', 0, 'Deslocamento de abomaso', 'Monitorar animais no pos-parto imediato; agendar avaliacao veterinaria se houver reducao brusca de apetite.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000033', 'BOVINO', 'Holandesa', 0, 'Cetose', 'Monitorar animais em inicio de lactacao; ajustar manejo nutricional.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000034', 'BOVINO', 'Holandesa', 5, 'Febre do leite (hipocalcemia)', 'Monitorar animais mais velhos ao redor do parto; considerar suplementacao preventiva de calcio.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000035', 'BOVINO', 'Nelore', 0, 'Verminose gastrointestinal', 'Manter protocolo de vermifugacao em animais jovens.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000036', 'BOVINO', NULL, 0, 'Mastite', 'Reforcar higiene da ordenha em femeas em lactacao.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000037', 'BOVINO', NULL, 0, 'Laminite', 'Revisar dieta com excesso de graos; agendar avaliacao podal.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000038', 'EQUINO', 'Mangalarga Marchador', 0, 'Colica', 'Manter rotina de alimentacao regular e acesso continuo a agua; agendar avaliacao imediata em caso de sinais de dor abdominal.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000039', 'EQUINO', 'Quarto de Milha', 0, 'Laminite', 'Revisar dieta rica em graos/pastagem e controlar peso corporal.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000040', 'EQUINO', NULL, 15, 'Sindrome de Cushing equino (PPID)', 'Agendar avaliacao hormonal preventiva em cavalos idosos.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000041', 'EQUINO', NULL, 15, 'Osteoartrite / doenca articular degenerativa', 'Agendar avaliacao ortopedica se houver claudicacao ou rigidez.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_predisposicao_saude (id, especie, raca, idade_minima_anos, doenca, recomendacao, fonte_referencia, criado_em) VALUES
('ade50000-0000-0000-0000-000000000042', 'EQUINO', NULL, 0, 'RAO / obstrucao recorrente das vias aereas ("asma equina")', 'Revisar qualidade do feno/estabulo (poeira e mofo) e ventilacao do ambiente.', 'Conhecimento veterinario consolidado', CURRENT_TIMESTAMP);

-- ------------------------------------------------------------
-- 2) A base agregada de doencas (grounding da IA e do fallback)
-- ------------------------------------------------------------

CREATE TABLE t_clyvo_base_doencas (
    id             VARCHAR(36)  NOT NULL,
    especie        VARCHAR(20)  NOT NULL,
    -- O texto do dataset ('labrador_retriever', 'Geospiza fuliginosa').
    raca_texto     VARCHAR(100) NOT NULL,
    -- A chave do catalogo da V14, quando a raca do dataset existe la.
    raca_chave     VARCHAR(60)  NULL,
    doenca_codigo  VARCHAR(60)  NOT NULL,
    doenca_nome    VARCHAR(200) NOT NULL,
    categoria      VARCHAR(60)  NOT NULL,
    casos          INT          NOT NULL,
    controles      INT          NOT NULL,
    fonte          VARCHAR(300) NULL,
    doi            VARCHAR(100) NULL,
    criado_em      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_base_doencas PRIMARY KEY (id),
    CONSTRAINT ck_base_doencas_especie CHECK (especie IN ('CAO','GATO','ROEDOR','AVE','REPTIL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX ix_base_doencas_busca ON t_clyvo_base_doencas (especie, raca_chave);

INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000001','AVE','Certhidea fusca',NULL,'PoxT','Variola aviaria (avian pox)','INFECCIOSA',2,0,'Avian disease surveillance on the island of San Cristobal, Galapagos','10.5061/dryad.kwh70rz4z',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000002','AVE','Geospiza fortis',NULL,'PoxT','Variola aviaria (avian pox)','INFECCIOSA',13,0,'Avian disease surveillance on the island of San Cristobal, Galapagos','10.5061/dryad.kwh70rz4z',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000003','AVE','Geospiza fuliginosa',NULL,'PoxT','Variola aviaria (avian pox)','INFECCIOSA',49,0,'Avian disease surveillance on the island of San Cristobal, Galapagos','10.5061/dryad.kwh70rz4z',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000004','AVE','Myiarchus magnirostris',NULL,'PoxT','Variola aviaria (avian pox)','INFECCIOSA',3,0,'Avian disease surveillance on the island of San Cristobal, Galapagos','10.5061/dryad.kwh70rz4z',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000005','AVE','Setophaga petechia aureola',NULL,'PoxT','Variola aviaria (avian pox)','INFECCIOSA',3,0,'Avian disease surveillance on the island of San Cristobal, Galapagos','10.5061/dryad.kwh70rz4z',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000006','CAO','airedale_terrier',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',1,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000008','CAO','airedale_terrier',NULL,'MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000009','CAO','airedale_terrier',NULL,'lymphoma','Linfoma','ONCOLOGICA',5,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000011','CAO','alaskan_malamute',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000013','CAO','american_eskimo_dog',NULL,'MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000014','CAO','american_eskimo_dog',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000015','CAO','american_pit_bull_terrier',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000016','CAO','american_pit_bull_terrier',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000017','CAO','american_staffordshire_terrier',NULL,'MCT','Mastocitoma','ONCOLOGICA',3,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000018','CAO','american_staffordshire_terrier',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000021','CAO','australian_cattle_dog',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000024','CAO','australian_shepherd',NULL,'MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000025','CAO','australian_shepherd',NULL,'lymphoma','Linfoma','ONCOLOGICA',5,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000026','CAO','basset_hound',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000027','CAO','basset_hound',NULL,'ED','Displasia de cotovelo','ORTOPEDICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000028','CAO','basset_hound',NULL,'MCT','Mastocitoma','ONCOLOGICA',6,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000029','CAO','basset_hound',NULL,'lymphoma','Linfoma','ONCOLOGICA',9,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000030','CAO','beagle','beagle','MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000031','CAO','beagle','beagle','MVD','Displasia da valva mitral','CARDIACA',3,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000032','CAO','beagle','beagle','lymphoma','Linfoma','ONCOLOGICA',3,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000036','CAO','belgian_malinois',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000039','CAO','belgian_sheepdog',NULL,'MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000042','CAO','bernese_mountain_dog',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',3,3,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000043','CAO','bernese_mountain_dog',NULL,'ED','Displasia de cotovelo','ORTOPEDICA',2,5,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000044','CAO','bernese_mountain_dog',NULL,'MCT','Mastocitoma','ONCOLOGICA',3,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000045','CAO','bernese_mountain_dog',NULL,'lymphoma','Linfoma','ONCOLOGICA',2,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000046','CAO','bichon_frise',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000048','CAO','bichon_frise',NULL,'MVD','Displasia da valva mitral','CARDIACA',5,4,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000049','CAO','bichon_frise',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000052','CAO','bloodhound',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000053','CAO','boerboel',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000055','CAO','border_collie','border-collie','CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',1,16,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000056','CAO','border_collie','border-collie','ED','Displasia de cotovelo','ORTOPEDICA',1,17,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000057','CAO','border_collie','border-collie','lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000060','CAO','borzoi',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000063','CAO','boston_terrier',NULL,'MCT','Mastocitoma','ONCOLOGICA',5,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000064','CAO','boston_terrier',NULL,'MVD','Displasia da valva mitral','CARDIACA',9,2,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000065','CAO','boston_terrier',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000066','CAO','bouvier_des_flandres',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000068','CAO','bouvier_des_flandres',NULL,'MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000069','CAO','boxer','boxer','CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000071','CAO','boxer','boxer','GC','Colite granulomatosa','GASTROINTESTINAL',40,74,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000072','CAO','boxer','boxer','MCT','Mastocitoma','ONCOLOGICA',32,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000073','CAO','boxer','boxer','lymphoma','Linfoma','ONCOLOGICA',13,6,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000075','CAO','brittany',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000076','CAO','bull_terrier',NULL,'MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000077','CAO','bull_terrier',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000078','CAO','bulldog_english',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',5,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000080','CAO','bulldog_english',NULL,'GC','Colite granulomatosa','GASTROINTESTINAL',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000081','CAO','bulldog_english',NULL,'MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000082','CAO','bulldog_english',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000084','CAO','bulldog_french','bulldog-frances','GC','Colite granulomatosa','GASTROINTESTINAL',5,17,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000085','CAO','bullmastiff',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',2,3,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000086','CAO','bullmastiff',NULL,'ED','Displasia de cotovelo','ORTOPEDICA',3,3,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000087','CAO','bullmastiff',NULL,'MCT','Mastocitoma','ONCOLOGICA',3,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000088','CAO','cairn_terrier',NULL,'MVD','Displasia da valva mitral','CARDIACA',2,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000089','CAO','cairn_terrier',NULL,'PSVA','Anomalia vascular portossistemica','HEPATICA/VASCULAR',21,23,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000090','CAO','cairn_terrier',NULL,'lymphoma','Linfoma','ONCOLOGICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000091','CAO','cane_corso',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',5,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000092','CAO','cane_corso',NULL,'ED','Displasia de cotovelo','ORTOPEDICA',2,4,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000093','CAO','cardigan_welsh_corgi',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000095','CAO','cavalier_king_charles_spaniel',NULL,'MVD','Displasia da valva mitral','CARDIACA',36,2,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000096','CAO','chesapeake_bay_retriever',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',3,2,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000098','CAO','chesapeake_bay_retriever',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000099','CAO','chihuahua','chihuahua','MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000100','CAO','chihuahua','chihuahua','MVD','Displasia da valva mitral','CARDIACA',4,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000101','CAO','chinese_shar-pei',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000102','CAO','chinese_shar-pei',NULL,'MCT','Mastocitoma','ONCOLOGICA',7,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000104','CAO','chow_chow',NULL,'ED','Displasia de cotovelo','ORTOPEDICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000105','CAO','cocker_spaniel','cocker-spaniel','CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',2,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000107','CAO','cocker_spaniel','cocker-spaniel','MCT','Mastocitoma','ONCOLOGICA',4,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000108','CAO','cocker_spaniel','cocker-spaniel','MVD','Displasia da valva mitral','CARDIACA',22,11,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000109','CAO','cocker_spaniel','cocker-spaniel','lymphoma','Linfoma','ONCOLOGICA',7,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000112','CAO','collie',NULL,'lymphoma','Linfoma','ONCOLOGICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000113','CAO','coton_de_tulear',NULL,'MVD','Displasia da valva mitral','CARDIACA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000114','CAO','dachshund','dachshund','MVD','Displasia da valva mitral','CARDIACA',8,9,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000115','CAO','dachshund','dachshund','lymphoma','Linfoma','ONCOLOGICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000116','CAO','dachshund_miniature','dachshund','MVD','Displasia da valva mitral','CARDIACA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000117','CAO','dalmatian',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000119','CAO','dandie_dinmont_terrier',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000120','CAO','doberman_pinscher',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',4,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000121','CAO','doberman_pinscher',NULL,'ED','Displasia de cotovelo','ORTOPEDICA',1,4,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000122','CAO','doberman_pinscher',NULL,'lymphoma','Linfoma','ONCOLOGICA',5,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000123','CAO','dogue_de_bordeaux',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000124','CAO','dogue_de_bordeaux',NULL,'ED','Displasia de cotovelo','ORTOPEDICA',1,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000126','CAO','english_cocker_spaniel','cocker-spaniel','MVD','Displasia da valva mitral','CARDIACA',1,2,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000127','CAO','english_cocker_spaniel','cocker-spaniel','lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000129','CAO','english_setter',NULL,'ED','Displasia de cotovelo','ORTOPEDICA',27,52,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000130','CAO','english_setter',NULL,'MCT','Mastocitoma','ONCOLOGICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000131','CAO','english_springer_spaniel',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',1,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000133','CAO','english_springer_spaniel',NULL,'lymphoma','Linfoma','ONCOLOGICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000134','CAO','english_toy_spaniel',NULL,'MVD','Displasia da valva mitral','CARDIACA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000137','CAO','flat-coated_retriever',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000138','CAO','fox_terrier_wire',NULL,'MVD','Displasia da valva mitral','CARDIACA',1,3,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000139','CAO','german_shepherd_dog','pastor-alemao','CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',24,24,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000140','CAO','german_shepherd_dog','pastor-alemao','ED','Displasia de cotovelo','ORTOPEDICA',10,45,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000141','CAO','german_shepherd_dog','pastor-alemao','MCT','Mastocitoma','ONCOLOGICA',1,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000142','CAO','german_shepherd_dog','pastor-alemao','lymphoma','Linfoma','ONCOLOGICA',5,7,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000143','CAO','german_shorthaired_pointer',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000145','CAO','german_shorthaired_pointer',NULL,'MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000148','CAO','golden_retriever','golden-retriever','CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',28,32,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000149','CAO','golden_retriever','golden-retriever','ED','Displasia de cotovelo','ORTOPEDICA',8,49,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000150','CAO','golden_retriever','golden-retriever','MCT','Mastocitoma','ONCOLOGICA',31,9,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000151','CAO','golden_retriever','golden-retriever','lymphoma','Linfoma','ONCOLOGICA',43,48,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000153','CAO','great_dane',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',4,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000154','CAO','great_dane',NULL,'ED','Displasia de cotovelo','ORTOPEDICA',1,5,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000155','CAO','great_pyrenees',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',1,2,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000157','CAO','great_pyrenees',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000161','CAO','greyhound',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000163','CAO','havanese',NULL,'MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000164','CAO','havanese',NULL,'PSVA','Anomalia vascular portossistemica','HEPATICA/VASCULAR',17,15,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000167','CAO','irish_setter',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',1,3,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000168','CAO','irish_setter',NULL,'ED','Displasia de cotovelo','ORTOPEDICA',1,5,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000169','CAO','irish_wolfhound',NULL,'ED','Displasia de cotovelo','ORTOPEDICA',1,4,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000170','CAO','irish_wolfhound',NULL,'epilepsy','Epilepsia','NEUROLOGICA',34,168,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000171','CAO','irish_wolfhound',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000172','CAO','italian_greyhound',NULL,'MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000173','CAO','italian_greyhound',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000174','CAO','jack_russell_terrier',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000176','CAO','jack_russell_terrier',NULL,'MCT','Mastocitoma','ONCOLOGICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000177','CAO','jack_russell_terrier',NULL,'MVD','Displasia da valva mitral','CARDIACA',3,9,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000178','CAO','jack_russell_terrier',NULL,'lymphoma','Linfoma','ONCOLOGICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000179','CAO','keeshond',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000182','CAO','labrador_retriever','labrador-retriever','CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',114,173,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000183','CAO','labrador_retriever','labrador-retriever','ED','Displasia de cotovelo','ORTOPEDICA',30,180,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000184','CAO','labrador_retriever','labrador-retriever','MCT','Mastocitoma','ONCOLOGICA',160,107,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000185','CAO','labrador_retriever','labrador-retriever','lymphoma','Linfoma','ONCOLOGICA',22,62,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000190','CAO','maltese','maltes','MVD','Displasia da valva mitral','CARDIACA',12,7,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000191','CAO','maltese','maltes','PSVA','Anomalia vascular portossistemica','HEPATICA/VASCULAR',26,24,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000192','CAO','maltese','maltes','lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000193','CAO','manchester_terrier_toy',NULL,'MVD','Displasia da valva mitral','CARDIACA',1,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000194','CAO','manchester_terrier_toy',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000195','CAO','mastiff',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',1,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000197','CAO','miniature_pinscher','pinscher','MVD','Displasia da valva mitral','CARDIACA',5,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000198','CAO','miniature_schnauzer',NULL,'MVD','Displasia da valva mitral','CARDIACA',9,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000199','CAO','miniature_schnauzer',NULL,'PSVA','Anomalia vascular portossistemica','HEPATICA/VASCULAR',21,17,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000200','CAO','miniature_schnauzer',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000203','CAO','mix','srd-cao','PSVA','Anomalia vascular portossistemica','HEPATICA/VASCULAR',7,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000204','CAO','mix','srd-cao','lymphoma','Linfoma','ONCOLOGICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000205','CAO','newfoundland',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',7,15,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000206','CAO','newfoundland',NULL,'ED','Displasia de cotovelo','ORTOPEDICA',8,22,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000207','CAO','newfoundland',NULL,'MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000208','CAO','norfolk_terrier',NULL,'MVD','Displasia da valva mitral','CARDIACA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000209','CAO','norfolk_terrier',NULL,'PSVA','Anomalia vascular portossistemica','HEPATICA/VASCULAR',10,10,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000212','CAO','nova_scotia_duck_tolling_retriever',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000215','CAO','old_english_sheepdog',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000217','CAO','papillon',NULL,'PSVA','Anomalia vascular portossistemica','HEPATICA/VASCULAR',2,9,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000218','CAO','pekingese',NULL,'MVD','Displasia da valva mitral','CARDIACA',2,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000219','CAO','pembroke_welsh_corgi',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000221','CAO','pembroke_welsh_corgi',NULL,'MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000225','CAO','pomeranian',NULL,'MVD','Displasia da valva mitral','CARDIACA',6,2,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000226','CAO','pomeranian',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000227','CAO','poodle','poodle','CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',1,2,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000229','CAO','poodle','poodle','MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000230','CAO','poodle','poodle','lymphoma','Linfoma','ONCOLOGICA',3,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000231','CAO','poodle_miniature','poodle','MVD','Displasia da valva mitral','CARDIACA',1,2,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000232','CAO','poodle_toy','poodle','CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000233','CAO','poodle_toy','poodle','MCT','Mastocitoma','ONCOLOGICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000234','CAO','poodle_toy','poodle','MVD','Displasia da valva mitral','CARDIACA',3,6,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000238','CAO','pug','pug','MCT','Mastocitoma','ONCOLOGICA',8,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000241','CAO','rat_terrier',NULL,'MVD','Displasia da valva mitral','CARDIACA',1,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000243','CAO','rhodesian_ridgeback',NULL,'MCT','Mastocitoma','ONCOLOGICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000244','CAO','rottweiler','rottweiler','CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',26,11,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000245','CAO','rottweiler','rottweiler','ED','Displasia de cotovelo','ORTOPEDICA',11,30,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000246','CAO','rottweiler','rottweiler','MCT','Mastocitoma','ONCOLOGICA',3,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000247','CAO','rottweiler','rottweiler','lymphoma','Linfoma','ONCOLOGICA',11,2,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000248','CAO','saint_bernard',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',3,3,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000249','CAO','saint_bernard',NULL,'ED','Displasia de cotovelo','ORTOPEDICA',2,5,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000251','CAO','saint_bernard',NULL,'lymphoma','Linfoma','ONCOLOGICA',5,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000252','CAO','saluki',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000253','CAO','samoyed',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',2,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000255','CAO','scottish_terrier',NULL,'MVD','Displasia da valva mitral','CARDIACA',2,2,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000256','CAO','scottish_terrier',NULL,'lymphoma','Linfoma','ONCOLOGICA',3,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000258','CAO','shetland_sheepdog',NULL,'MCT','Mastocitoma','ONCOLOGICA',4,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000259','CAO','shetland_sheepdog',NULL,'lymphoma','Linfoma','ONCOLOGICA',4,2,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000261','CAO','shiba_inu',NULL,'MVD','Displasia da valva mitral','CARDIACA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000262','CAO','shih_tzu','shih-tzu','MVD','Displasia da valva mitral','CARDIACA',7,6,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000263','CAO','shih_tzu','shih-tzu','lymphoma','Linfoma','ONCOLOGICA',3,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000264','CAO','shiloh_shepherd',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000265','CAO','siberian_husky','husky-siberiano','CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000267','CAO','siberian_husky','husky-siberiano','MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000268','CAO','siberian_husky','husky-siberiano','lymphoma','Linfoma','ONCOLOGICA',2,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000269','CAO','soft_coated_wheaten_terrier',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',1,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000271','CAO','soft_coated_wheaten_terrier',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000272','CAO','spinone_italiano',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000276','CAO','staffordshire_bull_terrier',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',5,1,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000277','CAO','staffordshire_bull_terrier',NULL,'ED','Displasia de cotovelo','ORTOPEDICA',1,3,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000278','CAO','staffordshire_bull_terrier',NULL,'MCT','Mastocitoma','ONCOLOGICA',6,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000281','CAO','standard_schnauzer',NULL,'lymphoma','Linfoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000283','CAO','tibetan_spaniel',NULL,'PSVA','Anomalia vascular portossistemica','HEPATICA/VASCULAR',10,13,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000284','CAO','tibetan_terrier',NULL,'MCT','Mastocitoma','ONCOLOGICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000287','CAO','vizsla',NULL,'MCT','Mastocitoma','ONCOLOGICA',51,26,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000288','CAO','vizsla',NULL,'lymphoma','Linfoma','ONCOLOGICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000289','CAO','weimaraner',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',1,3,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000291','CAO','weimaraner',NULL,'MCT','Mastocitoma','ONCOLOGICA',1,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000292','CAO','west_highland_white_terrier',NULL,'CLLD','Doenca do ligamento cruzado cranial','ORTOPEDICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000293','CAO','west_highland_white_terrier',NULL,'MCT','Mastocitoma','ONCOLOGICA',3,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000294','CAO','west_highland_white_terrier',NULL,'MVD','Displasia da valva mitral','CARDIACA',3,5,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000295','CAO','west_highland_white_terrier',NULL,'lymphoma','Linfoma','ONCOLOGICA',3,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000300','CAO','yorkshire_terrier','yorkshire-terrier','MCT','Mastocitoma','ONCOLOGICA',2,0,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000301','CAO','yorkshire_terrier','yorkshire-terrier','MVD','Displasia da valva mitral','CARDIACA',2,7,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000302','CAO','yorkshire_terrier','yorkshire-terrier','PSVA','Anomalia vascular portossistemica','HEPATICA/VASCULAR',57,105,'Dryad - Complex disease and phenotype mapping in the domestic dog','10.5061/dryad.266k4',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000303','GATO','Abyssinian',NULL,'all_GI','Doenca gastrointestinal - grupo combinado','GASTROINTESTINAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000304','GATO','DLH','srd-gato','DM','Diabetes mellitus','ENDOCRINA',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000305','GATO','DLH','srd-gato','FEK','Ceratoconjuntivite eosinofilica felina','OFTALMOLOGICA',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000306','GATO','DLH','srd-gato','IBD','Doenca inflamatoria intestinal','GASTROINTESTINAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000307','GATO','DLH','srd-gato','chronic_enteropathy','Enteropatia cronica','GASTROINTESTINAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000308','GATO','DLH','srd-gato','hypercalcemia','Hipercalcemia','METABOLICA',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000309','GATO','DMH','srd-gato','FEK','Ceratoconjuntivite eosinofilica felina','OFTALMOLOGICA',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000310','GATO','DSH','srd-gato','DM','Diabetes mellitus','ENDOCRINA',5,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000311','GATO','DSH','srd-gato','FEK','Ceratoconjuntivite eosinofilica felina','OFTALMOLOGICA',3,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000312','GATO','DSH','srd-gato','HCM','Cardiomiopatia hipertrofica','CARDIACA',3,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000313','GATO','DSH','srd-gato','IBD','Doenca inflamatoria intestinal','GASTROINTESTINAL',2,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000314','GATO','DSH','srd-gato','SCAL','Linfoma alimentar de pequenas celulas','ONCOLOGICA/GASTROINTESTINAL',5,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000315','GATO','DSH','srd-gato','all_GI','Doenca gastrointestinal - grupo combinado','GASTROINTESTINAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000316','GATO','DSH','srd-gato','chronic_enteropathy','Enteropatia cronica','GASTROINTESTINAL',4,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000317','GATO','DSH','srd-gato','hypercalcemia','Hipercalcemia','METABOLICA',3,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000318','GATO','DSH','srd-gato','hyperthyroidism','Hipertireoidismo','ENDOCRINA',4,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000319','GATO','Himalayan',NULL,'CKD','Doenca renal cronica','RENAL',2,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000320','GATO','Himalayan',NULL,'IBD','Doenca inflamatoria intestinal','GASTROINTESTINAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000321','GATO','Himalayan',NULL,'all_GI','Doenca gastrointestinal - grupo combinado','GASTROINTESTINAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000322','GATO','Himalayan',NULL,'chronic_enteropathy','Enteropatia cronica','GASTROINTESTINAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000323','GATO','Maine_coon','maine-coon','CKD','Doenca renal cronica','RENAL',2,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000324','GATO','Maine_coon','maine-coon','FEK','Ceratoconjuntivite eosinofilica felina','OFTALMOLOGICA',2,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000325','GATO','Maine_coon','maine-coon','HCM','Cardiomiopatia hipertrofica','CARDIACA',2,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000326','GATO','Maine_coon','maine-coon','IBD','Doenca inflamatoria intestinal','GASTROINTESTINAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000327','GATO','Maine_coon','maine-coon','SCAL','Linfoma alimentar de pequenas celulas','ONCOLOGICA/GASTROINTESTINAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000328','GATO','Maine_coon','maine-coon','all_GI','Doenca gastrointestinal - grupo combinado','GASTROINTESTINAL',2,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000329','GATO','Manx',NULL,'CKD','Doenca renal cronica','RENAL',2,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000330','GATO','Manx',NULL,'SCAL','Linfoma alimentar de pequenas celulas','ONCOLOGICA/GASTROINTESTINAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000331','GATO','Manx',NULL,'all_GI','Doenca gastrointestinal - grupo combinado','GASTROINTESTINAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000332','GATO','Manx',NULL,'hypercalcemia','Hipercalcemia','METABOLICA',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000333','GATO','Manx',NULL,'hyperthyroidism','Hipertireoidismo','ENDOCRINA',2,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000334','GATO','Persian','persa','CKD','Doenca renal cronica','RENAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000335','GATO','Persian','persa','IBD','Doenca inflamatoria intestinal','GASTROINTESTINAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000336','GATO','Persian','persa','all_GI','Doenca gastrointestinal - grupo combinado','GASTROINTESTINAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000337','GATO','Persian','persa','hypercalcemia','Hipercalcemia','METABOLICA',2,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000338','GATO','Ragdoll','ragdoll','HCM','Cardiomiopatia hipertrofica','CARDIACA',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000339','GATO','Ragdoll','ragdoll','chronic_enteropathy','Enteropatia cronica','GASTROINTESTINAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000340','GATO','Siamese','siames','DM','Diabetes mellitus','ENDOCRINA',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000341','GATO','Siamese','siames','HCM','Cardiomiopatia hipertrofica','CARDIACA',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000342','GATO','Siamese','siames','IBD','Doenca inflamatoria intestinal','GASTROINTESTINAL',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000343','GATO','Siamese','siames','hyperthyroidism','Hipertireoidismo','ENDOCRINA',1,0,'Complex Feline Disease Mapping Using a Dense Genotyping Array','10.5061/dryad.f1vhhmgwp',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000344','REPTIL','Blotched Blue-tongue',NULL,'RESCUE_DISEASE','Doenca/condicao clinica nao especificada','CLINICA_NAO_ESPECIFICADA',2,0,'Interactions between reptiles and people: A perspective from wildlife rehabilitation records','10.5061/dryad.jh9w0vtmc',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000345','REPTIL','Carpet Python',NULL,'RESCUE_DISEASE','Doenca/condicao clinica nao especificada','CLINICA_NAO_ESPECIFICADA',8,0,'Interactions between reptiles and people: A perspective from wildlife rehabilitation records','10.5061/dryad.jh9w0vtmc',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000346','REPTIL','Eastern Bearded Dragon',NULL,'RESCUE_DISEASE','Doenca/condicao clinica nao especificada','CLINICA_NAO_ESPECIFICADA',5,0,'Interactions between reptiles and people: A perspective from wildlife rehabilitation records','10.5061/dryad.jh9w0vtmc',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000347','REPTIL','Eastern Blue-tongue',NULL,'RESCUE_DISEASE','Doenca/condicao clinica nao especificada','CLINICA_NAO_ESPECIFICADA',39,0,'Interactions between reptiles and people: A perspective from wildlife rehabilitation records','10.5061/dryad.jh9w0vtmc',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000348','REPTIL','Green Tree Snake',NULL,'RESCUE_DISEASE','Doenca/condicao clinica nao especificada','CLINICA_NAO_ESPECIFICADA',1,0,'Interactions between reptiles and people: A perspective from wildlife rehabilitation records','10.5061/dryad.jh9w0vtmc',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000349','REPTIL','Highland Copperhead',NULL,'RESCUE_DISEASE','Doenca/condicao clinica nao especificada','CLINICA_NAO_ESPECIFICADA',1,0,'Interactions between reptiles and people: A perspective from wildlife rehabilitation records','10.5061/dryad.jh9w0vtmc',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000350','REPTIL','Lace Monitor',NULL,'RESCUE_DISEASE','Doenca/condicao clinica nao especificada','CLINICA_NAO_ESPECIFICADA',3,0,'Interactions between reptiles and people: A perspective from wildlife rehabilitation records','10.5061/dryad.jh9w0vtmc',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000351','REPTIL','Land Mullet',NULL,'RESCUE_DISEASE','Doenca/condicao clinica nao especificada','CLINICA_NAO_ESPECIFICADA',4,0,'Interactions between reptiles and people: A perspective from wildlife rehabilitation records','10.5061/dryad.jh9w0vtmc',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000352','REPTIL','Pink-tongued Skink',NULL,'RESCUE_DISEASE','Doenca/condicao clinica nao especificada','CLINICA_NAO_ESPECIFICADA',1,0,'Interactions between reptiles and people: A perspective from wildlife rehabilitation records','10.5061/dryad.jh9w0vtmc',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000353','REPTIL','Shingleback',NULL,'RESCUE_DISEASE','Doenca/condicao clinica nao especificada','CLINICA_NAO_ESPECIFICADA',1,0,'Interactions between reptiles and people: A perspective from wildlife rehabilitation records','10.5061/dryad.jh9w0vtmc',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000354','REPTIL','Tiger Snake',NULL,'RESCUE_DISEASE','Doenca/condicao clinica nao especificada','CLINICA_NAO_ESPECIFICADA',1,0,'Interactions between reptiles and people: A perspective from wildlife rehabilitation records','10.5061/dryad.jh9w0vtmc',CURRENT_TIMESTAMP);
INSERT INTO t_clyvo_base_doencas (id, especie, raca_texto, raca_chave, doenca_codigo, doenca_nome, categoria, casos, controles, fonte, doi, criado_em) VALUES
('ab5e0000-0000-0000-0000-000000000355','REPTIL','Water Dragon',NULL,'RESCUE_DISEASE','Doenca/condicao clinica nao especificada','CLINICA_NAO_ESPECIFICADA',4,0,'Interactions between reptiles and people: A perspective from wildlife rehabilitation records','10.5061/dryad.jh9w0vtmc',CURRENT_TIMESTAMP);

-- ------------------------------------------------------------
-- 3) O cache do parecer da IA
-- ------------------------------------------------------------

CREATE TABLE t_clyvo_parecer_ia (
    id         VARCHAR(36)  NOT NULL,
    animal_id  VARCHAR(36)  NOT NULL,
    -- 'IA' quando veio da OCI; 'REGRAS' quando o fallback deterministico
    -- respondeu. O app mostra a diferenca ao tutor.
    origem     VARCHAR(20)  NOT NULL,
    -- Qual modelo redigiu (ex.: 'cohere.command-r-08-2024'); NULL nas regras.
    modelo     VARCHAR(120) NULL,
    -- O parecer estruturado, em JSON. TEXT porque 1000 caracteres nao dao para
    -- riscos + recomendacoes + disclaimer.
    conteudo   TEXT         NOT NULL,
    gerado_em  DATETIME     NOT NULL,
    valido_ate DATETIME     NOT NULL,
    CONSTRAINT pk_parecer_ia PRIMARY KEY (id),
    -- UM parecer por animal: regenerar e substituir, nao acumular. O historico
    -- de saude de verdade mora no prontuario, nao num cache de LLM.
    CONSTRAINT uk_parecer_ia_animal UNIQUE (animal_id),
    CONSTRAINT fk_parecer_ia_animal FOREIGN KEY (animal_id) REFERENCES t_clyvo_animal (id),
    CONSTRAINT ck_parecer_ia_origem CHECK (origem IN ('IA','REGRAS'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
