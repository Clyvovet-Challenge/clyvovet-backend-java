#!/usr/bin/env bash
# Recurso 4 de 5: Web App da API Java.
#
# SEM CONTAINER, DE PROPOSITO
# O runtime e o JAVA:17-java17 nativo do App Service. A Opcao 2 da disciplina
# desconta -40 por "App Containerizado". O Dockerfile continua no repositorio
# servindo ao desenvolvimento local -- o proibido e o ARTEFATO PUBLICADO sair dele.
#
#     bash azure/04-webapp-java.sh
set -euo pipefail
cd "$(dirname "$0")"
source ./00-variaveis.sh

az account set --subscription "$SUBSCRIPTION"

echo "==> Runtimes Java disponiveis (conferencia):"
az webapp list-runtimes --os linux -o tsv | grep -i "^JAVA" | head -5 || true

echo "==> Criando Web App $APP_JAVA (JAVA:17-java17)..."
az webapp create \
    --resource-group "$RG" \
    --plan "$PLAN_NAME" \
    --name "$APP_JAVA" \
    --runtime "JAVA:17-java17" -o table

echo "==> Ligando o log para o portal (Log stream)..."
az webapp log config \
    --resource-group "$RG" --name "$APP_JAVA" \
    --application-logging filesystem --level information -o none

echo
echo "==> https://${APP_JAVA}.azurewebsites.net"
echo "==> Proximo: bash azure/05-webapp-dotnet.sh"
