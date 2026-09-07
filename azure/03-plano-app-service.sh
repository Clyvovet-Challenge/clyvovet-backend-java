#!/usr/bin/env bash
# Recurso 3 de 5: o App Service Plan que hospeda as duas APIs.
#
# POR QUE B1, E NAO O F1 GRATUITO
# O F1 existe em brazilsouth, mas nao tem Always On: o app dorme apos ~20 min
# ocioso, e quem abrir a URL depois disso espera um cold start. O B1 e o SKU mais
# barato COM Always On -- o risco aqui nao e memoria, e o app estar dormindo na
# hora da correcao.
#
# Se apertar, a troca para B2 e um comando e nao recria nada. Ver 00-variaveis.sh.
#
# --is-linux e obrigatorio: os runtimes JAVA:17-java17 e DOTNETCORE:8.0 sao Linux.
#
#     bash azure/03-plano-app-service.sh
set -euo pipefail
cd "$(dirname "$0")"
source ./00-variaveis.sh

az account set --subscription "$SUBSCRIPTION"

echo "==> Criando App Service Plan $PLAN_NAME ($PLAN_SKU, Linux)..."
az appservice plan create \
    --resource-group "$RG" \
    --name "$PLAN_NAME" \
    --location "$LOCATION" \
    --is-linux \
    --sku "$PLAN_SKU" \
    --number-of-workers 1 -o table

echo "==> Proximo: bash azure/04-webapp-java.sh"
