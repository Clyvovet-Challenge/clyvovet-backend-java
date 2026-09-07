#!/usr/bin/env bash
# Recurso 5 de 5: Web App da API .NET, no MESMO plano da Java.
#
# POR QUE A .NET TAMBEM VAI PARA A NUVEM
# A disciplina de DevOps exige o deploy de UMA API, e a escolhida e a Java. Mas o
# app movel precisa das duas. Manter uma publica e outra em localhost obrigaria a
# trocar URL na hora de gravar -- exatamente o tipo de configuracao que falha ao
# vivo. Custa um webapp create a mais no plano que ja existe.
#
#     bash azure/05-webapp-dotnet.sh
set -euo pipefail
cd "$(dirname "$0")"
source ./00-variaveis.sh

az account set --subscription "$SUBSCRIPTION"

echo "==> Criando Web App $APP_DOTNET (DOTNETCORE:8.0)..."
az webapp create \
    --resource-group "$RG" \
    --plan "$PLAN_NAME" \
    --name "$APP_DOTNET" \
    --runtime "DOTNETCORE:8.0" -o table

az webapp log config \
    --resource-group "$RG" --name "$APP_DOTNET" \
    --application-logging filesystem --level information -o none

echo
echo "==> https://${APP_DOTNET}.azurewebsites.net"
echo "==> Proximo: bash azure/06-configuracoes.sh"
