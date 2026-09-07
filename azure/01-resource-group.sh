#!/usr/bin/env bash
# Recurso 1 de 5: o Resource Group que contem todo o resto.
#
#     bash azure/01-resource-group.sh
set -euo pipefail
cd "$(dirname "$0")"
source ./00-variaveis.sh

az account set --subscription "$SUBSCRIPTION"

echo "==> Criando Resource Group $RG em $LOCATION..."
az group create --name "$RG" --location "$LOCATION" -o table

echo "==> Pronto. Proximo: bash azure/02-banco-mysql.sh"
