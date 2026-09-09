#!/usr/bin/env bash
# Abre uma sessao SQL no banco da Azure para gravar o item 9.3 do video.
#
# POR QUE ESTE SCRIPT EXISTE
# O item 9.3 pede "demonstracao detalhada e individual de todas as operacoes do
# CRUD diretamente no Banco de Dados por SELECT". O 09-verificar.sh ja imprime o
# SQL exato -- o que faltava era como RODAR: o Git Bash nao traz cliente mysql, e
# no ensaio na Azure o deploy inteiro ficou provado com a gravacao ainda
# impossivel, porque nao havia com o que abrir o banco.
#
# Instalar o MySQL Community na maquina de quem grava e uma dependencia a mais
# para falhar ao vivo. Este script usa o cliente local se existir e, se nao,
# roda o mesmo cliente dentro de um container que some depois.
#
#     bash azure/10-sql-do-video.sh              # sessao interativa (para gravar)
#     bash azure/10-sql-do-video.sh --rodar      # roda os SELECTs e mostra o resultado
#
# ANTES DE GRAVAR, DUAS COISAS
#   1. O 02 liberou o SEU IP no firewall do banco. Se a sua internet trocou de IP
#      desde entao (reiniciar o roteador basta), libere o novo:
#        az mysql flexible-server firewall-rule create -g $RG -n $MYSQL_SERVER \
#           --rule-name minha-maquina-2 --start-ip-address <IP> --end-ip-address <IP>
#   2. Rode o app e faca as operacoes ANTES de abrir esta sessao, para os SELECTs
#      terem o que mostrar. A ordem que a regua quer e: operacao no app, depois
#      SELECT no banco provando que aconteceu.
set -uo pipefail
cd "$(dirname "$0")"
source ./00-variaveis.sh

HOST="$(mysql_host)"

# Um SELECT trivial vale mais que `command -v`: prova credencial, TLS e firewall
# de uma vez. Descobrir que o IP mudou depois de comecar a gravar e o tipo de
# surpresa que este projeto ja pagou uma vez.
echo "==> Testando o acesso a $HOST..."
if ! PROVA="$(mysql_do_azure 'SELECT 1;' 2>&1)"; then
    CODIGO=$?
    echo >&2
    if [ "$CODIGO" -eq 127 ]; then
        echo "[ERRO] nao ha cliente mysql instalado NEM docker rodando." >&2
        echo "       Suba o Docker Desktop (nao precisa instalar mais nada) ou" >&2
        echo "       instale um cliente MySQL." >&2
    else
        echo "[ERRO] o banco recusou a conexao:" >&2
        echo "       $PROVA" >&2
        echo >&2
        echo "       Suspeitos, em ordem:" >&2
        echo "         - o seu IP mudou e nao esta no firewall (ver cabecalho);" >&2
        echo "         - MYSQL_PASSWORD nao e a senha deste servidor;" >&2
        echo "         - o servidor nao existe (o 99 ja apagou o grupo?)." >&2
    fi
    exit 1
fi
echo "    ok -- credencial, TLS e firewall respondendo"

SQL_DO_VIDEO="$(cat <<'FIM'
-- 1. INSERCAO: a linha que o app acabou de criar
SELECT id, nome, especie, raca, tutor_id
  FROM t_clyvo_animal
 ORDER BY id DESC
 LIMIT 5;

-- 2. ATUALIZACAO: o campo alterado, com o tutor ao lado (prova a FK)
SELECT a.nome AS animal, a.cor, t.nome AS tutor
  FROM t_clyvo_animal a
  JOIN t_clyvo_tutor t ON t.id = a.tutor_id
 ORDER BY a.id DESC
 LIMIT 5;

-- 3. EXCLUSAO: a contagem cai e a linha desaparece
SELECT COUNT(*) AS animais_cadastrados FROM t_clyvo_animal;

-- 4. CONSULTA evidenciando o relacionamento entre as duas tabelas do core
SELECT t.nome AS tutor, COUNT(a.id) AS qtd_animais
  FROM t_clyvo_tutor t
  LEFT JOIN t_clyvo_animal a ON a.tutor_id = t.id
 GROUP BY t.id, t.nome
 ORDER BY qtd_animais DESC
 LIMIT 10;
FIM
)"

if [ "${1:-}" = "--rodar" ]; then
    echo
    echo "==> Rodando os SELECTs do item 9.3:"
    echo
    # -t desliga o modo batch e devolve a tabela desenhada, que e o que fica
    # legivel em video. O -N -B do mysql_do_azure serve para script, nao para tela.
    if command -v mysql >/dev/null 2>&1; then
        MYSQL_PWD="$MYSQL_PASSWORD" mysql -h "$HOST" -u "$MYSQL_ADMIN" --ssl-mode=REQUIRED -t "$MYSQL_DB" <<<"$SQL_DO_VIDEO"
    else
        docker run --rm -i -e MYSQL_PWD="$MYSQL_PASSWORD" "$MYSQL_IMAGEM" mysql -h "$HOST" -u "$MYSQL_ADMIN" --ssl-mode=REQUIRED -t "$MYSQL_DB" <<<"$SQL_DO_VIDEO"
    fi
    exit $?
fi

echo
echo "=============================================================="
echo " COLE ESTES SELECTs DURANTE A GRAVACAO (um por vez)"
echo "=============================================================="
echo "$SQL_DO_VIDEO"
echo "=============================================================="
echo
echo "==> Abrindo a sessao. Saia com \\q"
echo

if command -v mysql >/dev/null 2>&1; then
    MYSQL_PWD="$MYSQL_PASSWORD" mysql -h "$HOST" -u "$MYSQL_ADMIN" --ssl-mode=REQUIRED "$MYSQL_DB"
else
    # -it precisa de terminal. No Git Bash o docker as vezes exige winpty para
    # entregar um TTY; sem ele a sessao abre e fecha sozinha, sem erro nenhum.
    echo "    (usando $MYSQL_IMAGEM em container -- nao ha cliente local)"
    # Sessao interativa exige terminal. Rodado por pipe, por CI ou dentro de um
    # editor sem console, o `docker run -it` falha com "the input device is not a
    # TTY" -- mensagem que nao diz o que fazer. Melhor recusar explicando.
    if [ ! -t 0 ]; then
        echo >&2
        echo "[ERRO] a sessao interativa precisa de um terminal, e a entrada atual" >&2
        echo "       nao e um. Abra o Git Bash e rode direto:" >&2
        echo "           bash azure/10-sql-do-video.sh" >&2
        echo "       Se voce so quer VER os resultados, use o modo nao interativo:" >&2
        echo "           bash azure/10-sql-do-video.sh --rodar" >&2
        exit 1
    fi
    if command -v winpty >/dev/null 2>&1; then
        winpty docker run --rm -it -e MYSQL_PWD="$MYSQL_PASSWORD" "$MYSQL_IMAGEM" mysql -h "$HOST" -u "$MYSQL_ADMIN" --ssl-mode=REQUIRED "$MYSQL_DB"
    else
        docker run --rm -it -e MYSQL_PWD="$MYSQL_PASSWORD" "$MYSQL_IMAGEM" mysql -h "$HOST" -u "$MYSQL_ADMIN" --ssl-mode=REQUIRED "$MYSQL_DB"
    fi
fi
