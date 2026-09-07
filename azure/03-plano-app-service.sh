#!/usr/bin/env bash
# Recurso 3 de 5: o App Service Plan que hospeda as duas APIs.
#
# POR QUE B2 E NAO B1
# As duas APIs dividem este plano. B1 e 1 core e 1,75 GB para um Spring Boot com
# Hibernate MAIS um ASP.NET Core rodando dois BackgroundService em loop continuo.
# B2 dobra os dois. A diferenca de custo e pequena; a de risco, nao -- B1 e onde
# "sem gargalo" deixa de valer, e voce descobre durante a gravacao do video.
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
