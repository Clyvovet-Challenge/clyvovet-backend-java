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

# B1 -- e nao o F1 gratuito, e nao B2.
#
# O F1 existe em brazilsouth, mas NAO TEM ALWAYS ON: o app dorme depois de ~20 min
# ocioso. O feedback das entregas e em 26/09, entao o professor abriria a URL e
# pegaria um cold start de Spring Boot num container de 1 GB. A regua trata
# "aplicativo nao funcional" e "depender de intervencao do professor" como zero de
# nota. Nao e economia -- e apostar a nota para nao gastar credito intocado.
#
# B1 e o SKU mais barato COM Always On, que e o risco real. Se o 09-verificar.sh
# mostrar que 1,75 GB apertam para as duas APIs, subir e UM comando e nao recria
# nada -- o plano e o unico recurso trocavel a quente:
#
#   az appservice plan update -g $RG -n $PLAN_NAME --sku B2
export PLAN_NAME="plan-clyvovet-sprint3"
export PLAN_SKU="B1"

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
