-- ============================================================
-- V3 — Identidade e controle de acesso
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
-- ============================================================

CREATE TABLE usuario (
    id                VARCHAR2(36)  PRIMARY KEY,
    email             VARCHAR2(200) NOT NULL,
    senha             VARCHAR2(100) NOT NULL,
    perfil            VARCHAR2(20)  NOT NULL,
    ativo             NUMBER(1)     DEFAULT 1 NOT NULL,
    tentativas_falhas NUMBER(3)     DEFAULT 0 NOT NULL,
    bloqueado_ate     TIMESTAMP,
    tutor_id          VARCHAR2(36),
    veterinario_id    VARCHAR2(36),
    CONSTRAINT uk_usuario_email    UNIQUE (email),
    CONSTRAINT fk_usuario_tutor    FOREIGN KEY (tutor_id)       REFERENCES tutor(id),
    CONSTRAINT fk_usuario_vet      FOREIGN KEY (veterinario_id) REFERENCES veterinario(id),
    CONSTRAINT chk_usuario_perfil  CHECK (perfil IN ('TUTOR','VETERINARIO','ADMIN')),
    CONSTRAINT chk_usuario_ativo   CHECK (ativo IN (0,1))
);

CREATE INDEX idx_usuario_email ON usuario (email);
