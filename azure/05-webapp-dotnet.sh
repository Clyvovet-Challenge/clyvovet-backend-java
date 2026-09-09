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
echo "==> Criando Web App $APP_DOTNET (DOTNETCORE:8.0)..."
az webapp create \
    --resource-group "$RG" \
    --plan "$PLAN_NAME" \
    --name "$APP_DOTNET" \
    --runtime "DOTNETCORE:8.0" \
    --basic-auth Enabled -o table

az webapp log config \
    --resource-group "$RG" --name "$APP_DOTNET" \
    --application-logging filesystem --level information -o none

echo
echo "==> https://${APP_DOTNET}.azurewebsites.net"
echo "==> Proximo: bash azure/06-configuracoes.sh"
