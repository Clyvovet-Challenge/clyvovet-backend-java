-- ============================================================
-- V12 -- A clinica vira ATOR, e nao so cadastro (MySQL)
--
-- ATE AQUI, "administrador da clinica" NAO EXISTIA
--
-- t_clyvo_usuario liga a um tutor OU a um veterinario, e a nada mais. O escopo
-- por clinica existe, mas e TRANSITIVO: usuario -> veterinario -> clinica. Quem
-- gerencia servicos, veterinarios e agenda e o ADMIN da plataforma, que enxerga
-- todas as clinicas.
--
-- O efeito pratico e que nao ha a quem pertencer um painel da clinica. Nao da
-- para recortar faturamento, agenda ou metrica por estabelecimento sem alguem
-- que SEJA o estabelecimento -- e um veterinario nao serve: ele e um
-- profissional dentro dela, com outro alcance.
--
-- Esta migracao cria esse alguem.
--
-- O CHECK DO PERFIL PRECISA CAIR ANTES
--
-- chk_usuario_perfil lista os tres perfis por extenso. Adicionar o valor so no
-- enum Java faria TODO insert de ADMIN_CLINICA falhar no banco, com erro de
-- integridade e nenhuma pista de que o problema e a constraint. E exatamente o
-- que ja aconteceu neste projeto entre ESTORNADO e REEMBOLSADO, e que a V4
-- existiu para consertar.
--
-- DROP CONSTRAINT, e nao DROP CHECK. O MySQL aceita as duas formas desde a
-- 8.0.19, e o Azure Database for MySQL Flexible Server entrega 8.0.21+ (a mesma
-- nota do cabecalho da V6). Ja o H2 em modo MySQL, que e onde MigrationsMySqlTest
-- roda as migrations, so entende a forma generica -- com DROP CHECK ele para em
-- "expected identifier", e dezesseis testes de migracao caem juntos.
--
-- Usar a forma que os tres entendem tem um efeito bom de lado: some a unica
-- diferenca de sintaxe que haveria entre este arquivo e o par Oracle.
--
-- E O VINCULO E OBRIGATORIO PARA ESSE PERFIL
--
-- chk_usuario_clinica garante que ADMIN_CLINICA sempre aponte para uma clinica.
-- O RecorteDeAcesso ja falha fechado quando o vinculo falta -- ele troca o nulo
-- por um UUID que nenhuma linha carrega, para o erro de cadastro nao virar
-- promocao a ADMIN --, mas depender disso e depender de codigo. Aqui o banco
-- recusa o registro incoerente na origem.
-- ============================================================

ALTER TABLE t_clyvo_usuario DROP CONSTRAINT chk_usuario_perfil;

ALTER TABLE t_clyvo_usuario
    ADD CONSTRAINT chk_usuario_perfil
    CHECK (perfil IN ('TUTOR','VETERINARIO','ADMIN','ADMIN_CLINICA'));

ALTER TABLE t_clyvo_usuario ADD COLUMN clinica_id VARCHAR(36);

ALTER TABLE t_clyvo_usuario
    ADD CONSTRAINT fk_usuario_clinica FOREIGN KEY (clinica_id) REFERENCES t_clyvo_clinica(id);

ALTER TABLE t_clyvo_usuario
    ADD CONSTRAINT chk_usuario_clinica
    CHECK (perfil <> 'ADMIN_CLINICA' OR clinica_id IS NOT NULL);

CREATE INDEX idx_usuario_clinica ON t_clyvo_usuario (clinica_id);
