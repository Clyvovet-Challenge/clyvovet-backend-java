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
# O JWT COMPARTILHADO, E O INTERRUPTOR DELE
#
#   Jwt__Secret na .NET recebe O MESMO VALOR de JWT_SECRET que vai para a Java.
#   Nao e um segredo novo: as duas assinam o mesmo token. O valor e base64, e as
#   duas o DECODIFICAM antes de usar como chave HMAC -- a .NET com
#   Convert.FromBase64String, nunca Encoding.UTF8.GetBytes, porque com o mesmo
#   valor os dois caminhos produzem chaves diferentes e nenhuma assinatura confere.
#
#   Api__EscopoPorTutor sai em "false" DE PROPOSITO, mesmo sendo o padrao do
#   codigo. A app setting precisa EXISTIR para poder ser virada sem redeploy:
#
#       az webapp config appsettings set -g $RG -n $APP_DOTNET \
#          --settings Api__EscopoPorTutor=true
#
#   A ORDEM DE DESLIGAR IMPORTA, E ERRAR NELA E PIOR QUE NAO DESLIGAR.
#   Para reverter, sempre:
#
#       1o  Api__EscopoPorTutor=false     -> volta ao comportamento de antes
#       2o  so entao mexer no Jwt__Secret
#
#   O caminho inverso -- tirar o segredo primeiro, com o escopo ainda ligado --
#   deixa a API sem conseguir identificar ninguem enquanto ainda exige identidade,
#   e o resultado e 401 em toda rota protegida. E o pior estado possivel, e ele so
#   existe nessa ordem.
#
#   Nao existe Jwt:Secret no appsettings.json, e isso e deliberado: este projeto
#   versiona placeholder para todo segredo ("SUA_API_KEY"), e um placeholder aqui
#   faria a validacao deixar de ser inerte por padrao -- destruindo o interruptor.
#
#     bash azure/06-configuracoes.sh
set -euo pipefail
cd "$(dirname "$0")"
source ./00-variaveis.sh

exigir DOTNET_API_KEY     || exit 1
exigir TELEGRAM_BOT_TOKEN || exit 1

# O PRIMEIRO ADMIN DA PLATAFORMA
#
# O banco e provisionado vazio e as migrations criam clinicas, veterinarios e
# tutores -- mas NENHUM usuario. E correto: banco de entrega nao deve receber
# usuario de desenvolvimento com senha conhecida. So que POST /auth/usuarios exige
# perfil ADMIN, e POST /auth/registrar so cria TUTOR: sem um ADMIN inicial, ninguem
# consegue criar o primeiro, e os fluxos de veterinario e de administracao ficam
# inalcancaveis -- inclusive na hora de gravar o video.
#
# A aplicacao cria este usuario UMA vez, no boot, e so se ainda nao houver nenhum
# ADMIN. Depois do primeiro acesso, troque a senha e remova as duas app settings:
#
#   az webapp config appsettings delete -g $RG -n $APP_JAVA \
#      --setting-names CLYVOVET_ADMIN_EMAIL CLYVOVET_ADMIN_SENHA
exigir ADMIN_EMAIL        || exit 1
exigir ADMIN_SENHA        || exit 1

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
      CLYVOVET_ADMIN_EMAIL="$ADMIN_EMAIL" \
      CLYVOVET_ADMIN_SENHA="$ADMIN_SENHA" \
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
      Jwt__Secret="$JWT_SECRET" \
      Api__EscopoPorTutor="false" \
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
