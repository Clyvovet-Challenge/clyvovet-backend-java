#!/usr/bin/env bash
# Configuracao compartilhada pelos scripts 01 a 09. Nao cria nada.
#
# Use com source, nunca execute direto:
#     source azure/00-variaveis.sh
#
# NENHUM SEGREDO AQUI, DE PROPOSITO
# A regua de DevOps desconta -20 por "deixar dados sensiveis expostos (usuario,
# senha e tokens) no codigo fonte". Senha e chave vem do ambiente e o script
# recusa rodar sem elas, em vez de assumir um valor padrao que acabaria commitado.
#
#     export MYSQL_PASSWORD='...'      # senha do admin do MySQL
#     export JWT_SECRET='...'          # base64, minimo 32 bytes
#     export DOTNET_API_KEY='...'      # chave da API .NET
#     export TELEGRAM_BOT_TOKEN='...'  # token do bot; formato valido basta

export SUBSCRIPTION="2tdspw-rm562312-pedrooliveira"

# Uma regiao so para tudo. A infra anterior tinha o grupo em brazilsouth e o banco
# em chilecentral, e toda consulta atravessava regioes. Confirmado por CLI que
# brazilsouth oferece App Service Linux (B1 e B2) e MySQL Burstable.
export LOCATION="brazilsouth"
export RG="rg-clyvovet-sprint3"

export MYSQL_SERVER="mysql-clyvovet-rm562312"
export MYSQL_DB="clyvovet"
export MYSQL_ADMIN="clyvovetadmin"
export MYSQL_SKU="Standard_B1ms"
export MYSQL_TIER="Burstable"
export MYSQL_VERSION="8.0"

# B2 e nao B1: as duas APIs dividem o plano, e uma delas roda dois
# BackgroundService em loop continuo. B1 e 1 core e 1,75 GB para os dois.
export PLAN_NAME="plan-clyvovet-sprint3"
export PLAN_SKU="B2"

export APP_JAVA="app-clyvovet-java-rm562312"
export APP_DOTNET="app-clyvovet-dotnet-rm562312"

# Onde o repositorio da API .NET esta clonado, para os scripts 05 e 08.
export CAMINHO_DOTNET="${CAMINHO_DOTNET:-../../ClyvoVet-api}"

exigir() {
    local nome="$1"
    if [ -z "${!nome:-}" ]; then
        echo "[ERRO] defina $nome no ambiente antes de rodar. Ex: export $nome='...'" >&2
        return 1
    fi
}

exigir MYSQL_PASSWORD || return 1 2>/dev/null || exit 1
exigir JWT_SECRET     || return 1 2>/dev/null || exit 1

echo "==> assinatura: $SUBSCRIPTION"
echo "==> regiao:     $LOCATION"
echo "==> grupo:      $RG"
