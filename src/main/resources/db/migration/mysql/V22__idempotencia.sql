-- Chaves de idempotencia dos POST que criam cobranca e agendamento.
--
-- O app manda um Idempotency-Key (UUID gerado quando o formulario abre). Se o
-- pedido estourar o tempo no celular mas o servidor terminar de gravar, o
-- segundo toque traz a mesma chave e recebe a resposta guardada, em vez de
-- criar uma segunda cobranca. Ver IdempotenciaFilter.
--
-- A chave vale por usuario: o UNIQUE e (usuario_id, chave), e e ele -- e nao uma
-- consulta antes do INSERT -- que decide qual de dois pedidos simultaneos passa.
-- Nada aqui e dado pessoal: a chave e um UUID aleatorio do cliente.
CREATE TABLE t_clyvo_idempotencia (
    usuario_id    VARCHAR(36)  NOT NULL,
    chave         VARCHAR(36)  NOT NULL,
    rota          VARCHAR(120) NOT NULL,
    -- SHA-256 do corpo. A mesma chave com outro corpo e erro de quem chama, e
    -- devolver a resposta do primeiro esconderia isso.
    hash_corpo    VARCHAR(64)  NOT NULL,
    -- NULL enquanto o primeiro pedido ainda esta sendo processado.
    status_http   INT          NULL,
    corpo         MEDIUMTEXT   NULL,
    criado_em     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_idempotencia PRIMARY KEY (usuario_id, chave),
    CONSTRAINT fk_idempotencia_usuario FOREIGN KEY (usuario_id) REFERENCES t_clyvo_usuario (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
