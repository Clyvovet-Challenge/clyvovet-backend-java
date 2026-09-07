#!/usr/bin/env bash
# Apaga TUDO que os scripts 01 a 05 criaram, para parar de consumir credito.
#
# Rode so depois da entrega. Enquanto o professor puder corrigir, os recursos
# precisam estar de pe -- "Entrega em LOCALHOST" e zero de nota, e um recurso
# apagado equivale a isso.
#
#     bash azure/99-destruir.sh
set -euo pipefail
cd "$(dirname "$0")"
source ./00-variaveis.sh

az account set --subscription "$SUBSCRIPTION"

echo "Isso apaga o Resource Group '$RG' e TUDO dentro dele:"
echo "  - MySQL $MYSQL_SERVER  (o banco e os dados, sem volta)"
echo "  - App Service Plan $PLAN_NAME"
echo "  - Web Apps $APP_JAVA e $APP_DOTNET"
echo
read -r -p "Digite 'confirmo' para prosseguir: " RESPOSTA
if [ "$RESPOSTA" != "confirmo" ]; then
    echo "Cancelado."
    exit 0
fi

# Sem --no-wait de proposito: o comando so retorna quando terminou, e assim voce
# sabe que acabou em vez de supor.
echo "==> Apagando $RG (pode levar alguns minutos)..."
az group delete --name "$RG" --yes

echo "==> Feito. Confira com: az group list -o table"
