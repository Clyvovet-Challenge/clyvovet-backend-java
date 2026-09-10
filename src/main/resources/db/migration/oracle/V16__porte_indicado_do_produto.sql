-- ============================================================================
-- V16 — o porte que o produto atende
-- ============================================================================
--
-- POR QUE ESTA COLUNA PRECISOU EXISTIR
--
-- A tela de produtos do app recorta o catalogo pelo animal escolhido, e ate
-- aqui o unico recorte possivel era a especie. So que especie nao resolve o
-- caso mais comum da categoria mais comum: racao de cachorro pequeno e racao de
-- cachorro grande sao produtos diferentes, com formulacao e granulometria
-- diferentes, e ambos apareciam para qualquer cachorro.
--
-- Coleira, casinha, transportadora e dosagem de antipulgas tem o mesmo
-- problema. Sem porte, "indicado para o seu pet" era uma promessa que o dado
-- nao sustentava.
--
-- POR QUE 'TODOS' E NAO NULL
--
-- NULL diria "nao sabemos", e o que se quer dizer e "serve a qualquer porte" --
-- uma consulta veterinaria, um shampoo neutro. Sao coisas diferentes, e so a
-- segunda pode aparecer com seguranca na lista de um animal especifico.
--
-- O default 'TODOS' tambem e o que torna esta migration segura para as linhas
-- que ja existem: nenhuma delas tinha porte, e nenhuma deve sumir da tela por
-- causa disso. Produto que nao declara porte continua servindo a todos, que era
-- exatamente o comportamento anterior.
--
-- O VALOR ESPELHA t_clyvo_animal.porte
--
-- 'PEQUENO', 'MEDIO', 'GRANDE' sao os mesmos tres valores do chk_animal_porte
-- da V1, e em maiuscula pelo mesmo motivo: e assim que o AnimalMapper grava o
-- porte do animal, e o filtro compara os dois lados. Aqui a caixa nao e
-- detalhe de estilo -- o Oracle e case-sensitive, e divergir faria o casamento
-- falhar em silencio.
-- ============================================================================

ALTER TABLE t_clyvo_produto ADD (porte_indicado VARCHAR2(30) DEFAULT 'TODOS' NOT NULL);

ALTER TABLE t_clyvo_produto
    ADD CONSTRAINT chk_produto_porte
    CHECK (porte_indicado IN ('PEQUENO', 'MEDIO', 'GRANDE', 'TODOS'));

-- Recorte por especie + porte e a consulta que a tela faz a cada abertura.
CREATE INDEX idx_produto_especie_porte
    ON t_clyvo_produto (especie_indicada, porte_indicado);

-- ----------------------------------------------------------------------------
-- Os produtos do seed que SAO de porte
--
-- A racao Golden Formula Adulto 15kg e de porte medio/grande, e a Whiskas sache
-- e de gato (porte nao se aplica, fica TODOS). Ajustar o seed aqui, e nao no
-- 02_seed_dotnet.sql, e o que garante que um banco ja criado tambem receba o
-- dado -- o seed so roda em banco novo.
-- ----------------------------------------------------------------------------

UPDATE t_clyvo_produto
   SET porte_indicado = 'GRANDE'
 WHERE nome LIKE 'Racao Golden Formula Adulto%';
