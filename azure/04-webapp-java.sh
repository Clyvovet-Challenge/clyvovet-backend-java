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

# --basic-auth Enabled NAO E OPCIONAL AQUI.
#
# A Azure passou a criar Web App com a publicacao por basic auth DESLIGADA por
# padrao (basicPublishingCredentialsPolicies/scm allow=false). Sem ela, o
# `az webapp deploy` do 07/08 nao usa a credencial de publicacao: cai para
# autenticacao AAD contra o endpoint SCM, e essa aquisicao de token pede sessao
# interativa. O deploy morre com uma mensagem que nao ajuda nada:
#
#     Raw Error : O conjunto de chaves nao existe.
#                 Status_InteractionRequired, Error code: 2148073494
#
# O erro fala de "conjunto de chaves" e o problema e uma politica de publicacao
# -- por isso vale deixar escrito. Foi assim que o 07 falhou no ensaio, com o
# jar de 77 MB ja construido e os cinco recursos de pe.
echo "==> Criando Web App $APP_JAVA (JAVA:17-java17)..."
az webapp create \
    --resource-group "$RG" \
    --plan "$PLAN_NAME" \
    --name "$APP_JAVA" \
    --runtime "JAVA:17-java17" \
    --basic-auth Enabled -o table

echo "==> Ligando o log para o portal (Log stream)..."
az webapp log config \
    --resource-group "$RG" --name "$APP_JAVA" \
    --application-logging filesystem --level information -o none

echo
echo "==> https://${APP_JAVA}.azurewebsites.net"
echo "==> Proximo: bash azure/05-webapp-dotnet.sh"
