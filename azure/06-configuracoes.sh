#!/usr/bin/env bash
# "Configuracoes adicionais" do item 8.6: as app settings das duas APIs.
#
# NENHUM SEGREDO NO ARQUIVO
# Todos os valores sensiveis vem do ambiente. A regua desconta -20 por dado
# sensivel exposto no codigo fonte, e este script e codigo fonte.
#
# DOIS DETALHES DE PLATAFORMA QUE DECIDEM SE A APLICACAO SOBE
#
#   1. SERVER_PORT=80 na Java. O App Service Linux encaminha para a porta 80 dentro
#      do container, e o Spring Boot sobe em 8080 por padrao. Sem isso a aplicacao
#      inicia, o health check externo nunca responde, e o App Service reinicia em
#      loop -- sem mensagem obvia no log. Funciona porque o relaxed binding do
#      Spring mapeia a variavel SERVER_PORT para a propriedade server.port.
#      A API .NET nao precisa do equivalente: a imagem DOTNETCORE:8.0 ja resolve
#      o ASPNETCORE_URLS sozinha.
#
#   2. sslMode=REQUIRED e SslMode=Required. O MySQL Flexible Server exige TLS. Sem
#      isso a conexao e recusada no handshake.
#
#     bash azure/06-configuracoes.sh
set -euo pipefail
cd "$(dirname "$0")"
source ./00-variaveis.sh

exigir DOTNET_API_KEY     || exit 1
exigir TELEGRAM_BOT_TOKEN || exit 1

az account set --subscription "$SUBSCRIPTION"

HOST="${MYSQL_SERVER}.mysql.database.azure.com"
URL_JAVA="https://${APP_JAVA}.azurewebsites.net"
URL_DOTNET="https://${APP_DOTNET}.azurewebsites.net"

echo "==> Configurando a API Java..."
az webapp config appsettings set \
    --resource-group "$RG" --name "$APP_JAVA" \
    --settings \
      SPRING_PROFILES_ACTIVE="mysql" \
      SERVER_PORT="80" \
      JAVA_OPTS="-Xmx512m -XX:+UseSerialGC" \
      DB_URL="jdbc:mysql://${HOST}:3306/${MYSQL_DB}?sslMode=REQUIRED&serverTimezone=UTC" \
      DB_USERNAME="$MYSQL_ADMIN" \
      DB_PASSWORD="$MYSQL_PASSWORD" \
      JWT_SECRET="$JWT_SECRET" \
      CLYVOVET_CORS_ORIGENS="${URL_JAVA},${URL_DOTNET},http://localhost:8081" \
    -o none
echo "    ok"

echo "==> Configurando a API .NET..."
az webapp config appsettings set \
    --resource-group "$RG" --name "$APP_DOTNET" \
    --settings \
      ASPNETCORE_ENVIRONMENT="Production" \
      ConnectionStrings__DefaultConnection="Server=${HOST};Port=3306;Database=${MYSQL_DB};Uid=${MYSQL_ADMIN};Pwd=${MYSQL_PASSWORD};SslMode=Required;" \
      Api__ApiKey="$DOTNET_API_KEY" \
      Telegram__BotToken="$TELEGRAM_BOT_TOKEN" \
      Cors__Origens="${URL_JAVA},${URL_DOTNET},http://localhost:8081" \
      Database__MaxPoolSize="15" \
    -o none
echo "    ok"

# O plano e compartilhado e cinco componentes das duas APIs guardam estado no
# processo: revogacao de token, rate limit e cache na Java; dois BackgroundService
# na .NET. Com uma instancia, nenhum e problema. Com duas, o logout para de
# funcionar e a notificacao duplica -- sem erro no log.
echo "==> Fixando UMA instancia em cada (ver docs/12, secao 7)..."
az appservice plan update --resource-group "$RG" --name "$PLAN_NAME" \
    --number-of-workers 1 -o none
echo "    ok"

echo
echo "==> Java:  $URL_JAVA"
echo "==> .NET:  $URL_DOTNET"
echo "==> Proximo: bash azure/07-deploy-java.sh"
