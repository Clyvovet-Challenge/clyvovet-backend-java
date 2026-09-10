-- ============================================================================
-- V17 — o arquivo do exame, e nao so o registro de que ele aconteceu
-- ============================================================================
--
-- O QUE FALTAVA
--
-- A tela de documentos do app dizia, em letra miuda, que o anexo "ainda nao e
-- guardado pelo sistema -- peca a copia na clinica que atendeu". Era verdade: o
-- prontuario tinha a LINHA ("Hemograma completo, 12/03, VetCare") e nao tinha o
-- PAPEL. O tutor que trocasse de clinica levava a lista do que foi feito e
-- deixava para tras o unico documento que o proximo veterinario ia querer ler.
--
-- Isso contradiz a tese do produto. Se o prontuario e do tutor, o exame tambem
-- e -- senao o que ele carrega e um indice de arquivos que continuam com quem
-- os produziu.
--
-- POR QUE O BYTE FICA NO BANCO, E NAO NUM BUCKET
--
-- Um object storage (Azure Blob, OCI) e a resposta de manual, e continua sendo
-- a resposta certa em volume. Aqui ele custaria mais do que resolve:
--
--   1. O controle de acesso do prontuario ja existe e e caro de duplicar --
--      nivel por perfil, consentimento vigente, quebra de vidro com motivo,
--      auditoria por (usuario, animal, dia). Uma URL assinada de bucket e uma
--      SEGUNDA porta para o mesmo dado, com regras proprias e um relogio
--      proprio. Quem tem o link tem o exame, e a auditoria nao ve a leitura.
--   2. Um bucket e mais uma credencial em mais um ambiente. A entrega ja depende
--      de credencial da OCI que ainda nao chegou.
--
-- Com o BLOB, ler o arquivo passa OBRIGATORIAMENTE pelo mesmo caminho que ler o
-- historico: mesma checagem, mesmo registro de acesso. E a troca e reversivel na
-- direcao barata -- migrar do banco para um bucket depois e copiar linhas; o
-- contrario e reescrever a autorizacao.
--
-- O limite de 8 MB por arquivo (imposto no Spring, ver comum.properties) e o
-- que mantem essa escolha honesta: laudo em PDF e foto de exame cabem; video de
-- ultrassom nao, e nao deve caber. LONGBLOB e nao BLOB porque o BLOB do MySQL
-- para em 64 KB -- guardaria um PDF truncado sem reclamar.
--
-- POR QUE O sha256 EXISTE
--
-- Um laudo e prova. Guardar o hash do que entrou permite responder "este e o
-- arquivo que a clinica enviou" sem depender da confianca no banco -- e detectar
-- o reenvio do mesmo documento sem comparar megabytes.
-- ============================================================================

CREATE TABLE t_clyvo_documento_clinico (
    id             VARCHAR(36)  NOT NULL,
    animal_id      VARCHAR(36)  NOT NULL,
    -- O atendimento que gerou o documento, quando ha um. NULL e o caso legitimo
    -- do tutor anexando o exame de uma clinica que nunca usou o ClyvoVet.
    evento_id      VARCHAR(36)  NULL,
    -- Quem enviou, e por qual clinica. A clinica e NULL quando quem envia e o
    -- proprio tutor -- e essa diferenca aparece na tela.
    enviado_por    VARCHAR(36)  NOT NULL,
    clinica_id     VARCHAR(36)  NULL,
    titulo         VARCHAR(150) NOT NULL,
    nome_arquivo   VARCHAR(255) NOT NULL,
    tipo_conteudo  VARCHAR(100) NOT NULL,
    tamanho_bytes  BIGINT       NOT NULL,
    sha256         VARCHAR(64)  NOT NULL,
    enviado_em     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    conteudo       LONGBLOB     NOT NULL,
    CONSTRAINT pk_documento_clinico PRIMARY KEY (id),
    CONSTRAINT fk_documento_animal   FOREIGN KEY (animal_id)   REFERENCES t_clyvo_animal (id),
    CONSTRAINT fk_documento_evento   FOREIGN KEY (evento_id)   REFERENCES t_clyvo_evento_clinico (id),
    CONSTRAINT fk_documento_usuario  FOREIGN KEY (enviado_por) REFERENCES t_clyvo_usuario (id),
    CONSTRAINT fk_documento_clinica  FOREIGN KEY (clinica_id)  REFERENCES t_clyvo_clinica (id),
    -- A lista fechada no banco e a rede de baixo: se um dia a validacao da
    -- aplicacao deixar passar um executavel renomeado, o INSERT ainda barra.
    CONSTRAINT ck_documento_tipo CHECK (tipo_conteudo IN ('application/pdf', 'image/jpeg', 'image/png')),
    CONSTRAINT ck_documento_tamanho CHECK (tamanho_bytes > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- A consulta da tela e sempre "os documentos deste animal, do mais recente para
-- o mais antigo". Sem o indice, ela vira full scan numa tabela de BLOBs.
CREATE INDEX ix_documento_animal ON t_clyvo_documento_clinico (animal_id, enviado_em);
