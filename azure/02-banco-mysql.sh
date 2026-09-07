#!/usr/bin/env bash
# Recurso 2 de 5: Azure Database for MySQL Flexible Server (PaaS).
#
# POR QUE PaaS E NAO CONTAINER
# A disciplina escolheu a Opcao 2 (App Service + Banco PaaS) e desconta -40 por
# banco containerizado. Tambem desconta -40 por banco nao permitido -- H2 entra
# nessa lista, e era o que o deploy.sh antigo provisionava.
#
# O BANCO NASCE VAZIO, DE PROPOSITO
# O Flyway da aplicacao cria as 19 tabelas no primeiro boot, da V1 a V9. E o unico
# caminho em que o DDL entregue (documentos/script_bd.sql) e o banco real nao podem
# divergir. Provisionar por script avulso e depois ligar o Flyway exigiria baseline,
# e o historico nasceria inconsistente.
#
#     bash azure/02-banco-mysql.sh
set -euo pipefail
cd "$(dirname "$0")"
source ./00-variaveis.sh

az account set --subscription "$SUBSCRIPTION"

echo "==> SKUs Burstable disponiveis em $LOCATION (conferencia):"
az mysql flexible-server list-skus --location "$LOCATION" -o json 2>/dev/null \
  | grep -o '"Standard_B[0-9a-z]*"' | tr -d '"' | sort -u | tr '\n' ' '
echo

echo "==> Criando MySQL Flexible Server $MYSQL_SERVER..."
az mysql flexible-server create \
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
    --yes -o table

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
