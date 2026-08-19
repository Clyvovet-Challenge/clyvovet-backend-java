-- ============================================================
-- V3 -- Identidade e controle de acesso (MySQL)
--
-- A tabela usuario separa IDENTIDADE (quem faz login) de DOMINIO
-- (tutor, veterinario). O vinculo e opcional dos dois lados:
--   * perfil TUTOR       -> tutor_id preenchido
--   * perfil VETERINARIO -> veterinario_id preenchido
--   * perfil ADMIN       -> nenhum dos dois
--
-- E o vinculo com tutor que viabiliza a regra de ownership:
-- um tutor so enxerga os proprios pets.
--
-- Nao ha usuario semeado aqui de proposito: hash de senha nao
-- deve ser versionado. Os usuarios de desenvolvimento sao criados
-- por DevDataSeeder, ativo apenas nos perfis dev e h2.
--
-- Diferencas em relacao a oracle/V3__usuario_e_perfis.sql -- estas
-- tres sao as unicas do projeto inteiro que nao sao renomeacao
-- mecanica de tipo, entao vale saber por que cada uma e assim:
--
--   ativo NUMBER(1) -> TINYINT
--       Usuario.ativo e boolean anotado com NumericBooleanConverter,
--       ou seja, o Hibernate grava 0/1 num inteiro -- nao um BOOLEAN
--       nativo. TINYINT e o inteiro de 1 byte do MySQL e o BOOLEAN do
--       MySQL e apelido dele, entao os dois lados falam a mesma coisa.
--
--   tentativas_falhas NUMBER(3) -> INT
--       O campo e int em Java. SMALLINT caberia de sobra (o limite de
--       tentativas e 5), mas o validate do Hibernate compara o tipo
--       JDBC da coluna com o do atributo, e int espera INTEGER.
--       Economizar 2 bytes por linha nao paga uma falha de boot.
--
--   bloqueado_ate TIMESTAMP -> DATETIME
--       ESTA E A IMPORTANTE. No MySQL, TIMESTAMP converte o valor para
--       UTC na escrita e de volta para o fuso da sessao na leitura, e
--       satura em 2038-01-19. DATETIME guarda o instante literal, que e
--       a semantica de LocalDateTime. Com TIMESTAMP, um servidor de
--       aplicacao e um de banco em fusos diferentes fariam a conta do
--       estaBloqueado() errar por horas -- e o erro so apareceria como
--       usuario destravando cedo demais, sem nenhuma excecao.
-- ============================================================

CREATE TABLE usuario (
    id                VARCHAR(36)  PRIMARY KEY,
    email             VARCHAR(200) NOT NULL,
    senha             VARCHAR(100) NOT NULL,
    perfil            VARCHAR(20)  NOT NULL,
    ativo             TINYINT      DEFAULT 1 NOT NULL,
    tentativas_falhas INT          DEFAULT 0 NOT NULL,
    bloqueado_ate     DATETIME,
    tutor_id          VARCHAR(36),
    veterinario_id    VARCHAR(36),
    CONSTRAINT uk_usuario_email    UNIQUE (email),
    CONSTRAINT fk_usuario_tutor    FOREIGN KEY (tutor_id)       REFERENCES tutor(id),
    CONSTRAINT fk_usuario_vet      FOREIGN KEY (veterinario_id) REFERENCES veterinario(id),
    CONSTRAINT chk_usuario_perfil  CHECK (perfil IN ('TUTOR','VETERINARIO','ADMIN')),
    CONSTRAINT chk_usuario_ativo   CHECK (ativo IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_usuario_email ON usuario (email);
