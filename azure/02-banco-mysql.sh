#!/usr/bin/env bash
# Recurso 2 de 5: Azure Database for MySQL Flexible Server (PaaS).
#
# POR QUE PaaS E NAO CONTAINER
# A disciplina escolheu a Opcao 2 (App Service + Banco PaaS) e desconta -40 por
# banco containerizado. Tambem desconta -40 por banco nao permitido -- H2 entra
# nessa lista, e era o que o deploy.sh antigo provisionava.
#
# O BANCO NASCE VAZIO, DE PROPOSITO
# O Flyway da aplicacao cria as 20 tabelas no primeiro boot, da V1 a V12. E o unico
# caminho em que o DDL entregue (documentos/script_bd.sql) e o banco real nao podem
# divergir. Provisionar por script avulso e depois ligar o Flyway exigiria baseline,
# e o historico nasceria inconsistente.
#
#     bash azure/02-banco-mysql.sh
set -euo pipefail
cd "$(dirname "$0")"
source ./00-variaveis.sh

az account set --subscription "$SUBSCRIPTION"

# DUAS GUARDAS ANTES DE TOCAR EM QUALQUER COISA
#
# (a) O namespace precisa estar ligado. Sem ele o create devolve "subscription
#     is not registered to use namespace Microsoft.DBforMySQL" -- mensagem que
#     nao diz o que fazer, num ponto em que o grupo ja existe.
exigir_provider Microsoft.DBforMySQL

# (b) O RESOURCE GROUP PRECISA EXISTIR, E ISSO NAO E OBVIO.
#     Descoberto na marra: apontar o `flexible-server create` para um grupo
#     inexistente NAO da erro -- o CLI cria o grupo sozinho, sem perguntar, e
#     so entao segue. Rodar o 02 antes do 01, ou com o RG errado no ambiente,
#     espalha grupos orfaos pela assinatura. Melhor recusar aqui.
if ! az group show -n "$RG" -o none 2>/dev/null; then
    echo "[ERRO] o resource group $RG nao existe." >&2
    echo "       Nao siga em frente: o 'az mysql flexible-server create' criaria" >&2
    echo "       o grupo sozinho, sem avisar. Rode primeiro:" >&2
    echo "           bash azure/01-resource-group.sh" >&2
    exit 1
fi

# CONFERENCIA, E NAO UM PORTAO
#
# Esta linha e um echo decorativo, mas e um pipeline -- e com `set -o pipefail`
# o exit code dela vira o exit code do script inteiro. O endpoint de
# capabilities do MySQL responde InternalServerError de tempos em tempos
# (respondeu durante a preparacao deste deploy, em TODAS as api-versions e em
# duas regioes), e o grep tambem sai 1 quando nao acha nada. Nos dois casos o
# `set -e` matava o script ANTES do create: o deploy morria por causa do
# enfeite, nao do banco.
echo "==> SKUs Burstable disponiveis em $LOCATION (conferencia):"
az mysql flexible-server list-skus --location "$LOCATION" -o json 2>/dev/null \
  | grep -o '"Standard_B[0-9a-z]*"' | tr -d '"' | sort -u | tr '\n' ' ' \
  || echo -n "(lista indisponivel -- quem decide e o create abaixo)"
echo

# IDEMPOTENTE, PORQUE FALHA NO MEIO ACONTECE
#
# Sem esta checagem, qualquer erro depois do create -- e teve um, o -o table
# abaixo -- deixava o servidor criado e cobrando com o script dizendo que
# falhou. Rodar de novo batia em "server already exists", e nao sobrava saida
# que nao fosse apagar o grupo inteiro e comecar do zero.
if az mysql flexible-server show -g "$RG" -n "$MYSQL_SERVER" -o none 2>/dev/null; then
    echo "==> $MYSQL_SERVER ja existe -- pulando a criacao."
else
echo "==> Criando MySQL Flexible Server $MYSQL_SERVER..."
# -o none, e NAO -o table.
#
# Com -o table este comando morria em "Table output unavailable": a resposta do
# create nao tem forma tabular, o formatador falha DEPOIS do servidor estar
# criado, e o exit code nao-zero derrubava o script. O recurso existia e o
# deploy dizia que nao. Nenhuma saida de create aqui e util o bastante para
# valer esse risco.
if ! az mysql flexible-server create \
    --resource-group "$RG" \
    --name "$MYSQL_SERVER" \
    --location "$LOCATION" \
    --admin-user "$MYSQL_ADMIN" \
    --admin-password "$MYSQL_PASSWORD" \
    --sku-name "$MYSQL_SKU" \
    --tier "$MYSQL_TIER" \
    --version "$MYSQL_VERSION" \
    --storage-size 20 \
    --public-access None \
    --yes -o none; then
    echo >&2
    echo "[ERRO] o create do MySQL falhou. LEIA A MENSAGEM ACIMA INTEIRA antes" >&2
    echo "       de mexer em qualquer variavel -- ela costuma ter duas partes." >&2
    echo >&2
    echo "       Um InternalServerError do endpoint de capabilities aparece com" >&2
    echo "       alguma frequencia e E TRANSITORIO: o proprio CLI repete a" >&2
    echo "       chamada e normalmente passa na segunda. Quando ele aparece, a" >&2
    echo "       causa REAL do fim costuma estar na ULTIMA linha, nao na" >&2
    echo "       primeira. Ja aconteceu de um 500 barulhento esconder um" >&2
    echo "       'Incorrect value for --version'." >&2
    echo >&2
    echo "       O que fazer, nesta ordem:" >&2
    echo "         1. rode de novo -- se era o 500, passa;" >&2
    echo "         2. se repetir igual, rode o mesmo comando com --debug e" >&2
    echo "            procure a linha 'ERROR: cli...azclierror', que traz o" >&2
    echo "            motivo verdadeiro;" >&2
    echo "         3. so entao considere trocar de regiao, e ANTES do 01:" >&2
    echo "            regiao trocada no meio deixa o grupo numa e o banco noutra." >&2
    exit 1
fi
fi   # fecha o "ja existe / criar"

echo "==> Criando o banco $MYSQL_DB (vazio -- o Flyway preenche)..."
az mysql flexible-server db create \
    --resource-group "$RG" \
    --server-name "$MYSQL_SERVER" \
    --database-name "$MYSQL_DB" -o table

# 0.0.0.0-0.0.0.0 e a regra especial do Azure para "servicos do Azure", e nao a
# internet inteira. E o que permite os dois App Services alcancarem o banco.
echo "==> Liberando acesso dos servicos do Azure..."
az mysql flexible-server firewall-rule create \
    --resource-group "$RG" \
    --name "$MYSQL_SERVER" \
    --rule-name permitir-servicos-azure \
    --start-ip-address 0.0.0.0 --end-ip-address 0.0.0.0 -o table

# Necessario para rodar o SELECT do video e conferir o schema da sua maquina.
MEU_IP="$(curl -4 -s https://api.ipify.org)"
echo "==> Liberando o seu IP ($MEU_IP) para inspecao manual..."
az mysql flexible-server firewall-rule create \
    --resource-group "$RG" \
    --name "$MYSQL_SERVER" \
    --rule-name minha-maquina \
    --start-ip-address "$MEU_IP" --end-ip-address "$MEU_IP" -o table

echo
echo "==> Servidor: ${MYSQL_SERVER}.mysql.database.azure.com"
echo "==> Proximo: bash azure/03-plano-app-service.sh"
