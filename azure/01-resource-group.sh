#!/usr/bin/env bash
# Recurso 1 de 5: o Resource Group que contem todo o resto.
#
#     bash azure/01-resource-group.sh
set -euo pipefail
cd "$(dirname "$0")"
source ./00-variaveis.sh

az account set --subscription "$SUBSCRIPTION"

# ANTES DO GRUPO: LIGAR OS NAMESPACES
#
# Registrar provider nao cria recurso e nao custa nada -- e so habilitar o
# namespace na assinatura. Mora aqui, e nao no 02/03, por causa do relogio:
# leva de 1 a 3 minutos por provider, e disparado agora ele termina enquanto o
# grupo e criado, em vez de segurar o deploy la na frente.
#
# A alternativa era confiar no registro automatico do primeiro create. O Azure
# as vezes faz isso sozinho, mas nao sempre -- e quando nao faz, o erro chega
# com o grupo ja criado e o passo pela metade.
echo "==> Habilitando os namespaces que o deploy usa..."
for NS in $PROVIDERS_NECESSARIOS; do
    ESTADO="$(estado_do_provider "$NS")"
    if [ "$ESTADO" = "Registered" ]; then
        echo "    $NS: ja registrado"
    else
        echo "    $NS: ${ESTADO:-desconhecido} -> registrando"
        az provider register -n "$NS" -o none
    fi
done

echo "==> Criando Resource Group $RG em $LOCATION..."
az group create --name "$RG" --location "$LOCATION" -o table

# Confirma antes de declarar sucesso: um "Registering" que nunca vira
# "Registered" derrubaria o 02 e o 03, e o 01 teria dito que estava tudo bem.
echo "==> Conferindo os namespaces (ate 5 min)..."
for NS in $PROVIDERS_NECESSARIOS; do
    for _ in $(seq 1 30); do
        if [ "$(estado_do_provider "$NS")" = "Registered" ]; then
            break
        fi
        sleep 10
    done
    ESTADO="$(estado_do_provider "$NS")"
    echo "    $NS: ${ESTADO:-desconhecido}"
    if [ "$ESTADO" != "Registered" ]; then
        echo "[ERRO] $NS nao ficou pronto a tempo. Rode o 01 de novo -- ele e" >&2
        echo "       idempotente, o grupo ja criado nao atrapalha." >&2
        exit 1
    fi
done

echo "==> Pronto. Proximo: bash azure/02-banco-mysql.sh"
