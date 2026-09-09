#!/usr/bin/env bash
# Deploy da API Java: build local do jar e publicacao por --type jar.
#
# A JAVA SOBE PRIMEIRO, E ISSO E ORDEM, NAO PREFERENCIA
# O Flyway desta aplicacao cria as 20 tabelas, incluindo as seis t_clyvo_* que a
# API .NET consome. Se a .NET subir antes, ela nao encontra as tabelas dela --
# e o EF Core nao valida schema no boot, entao ela sobe normalmente e falha so na
# primeira consulta, com "Table doesn't exist".
#
#     bash azure/07-deploy-java.sh
set -euo pipefail
cd "$(dirname "$0")"
source ./00-variaveis.sh

az account set --subscription "$SUBSCRIPTION"

echo "==> Build do jar (sem testes -- eles rodam no CI e no 09-verificar)..."
( cd .. && ./mvnw -q clean package -DskipTests )

JAR="$(ls ../target/*.jar | grep -v 'original' | head -1)"
echo "==> Artefato: $JAR ($(du -h "$JAR" | cut -f1))"

echo "==> Publicando em $APP_JAVA..."
az webapp deploy \
    --resource-group "$RG" \
    --name "$APP_JAVA" \
    --type jar \
    --src-path "$JAR" \
    --async false -o table

echo "==> Aguardando o Flyway aplicar V1 a V12 e o contexto subir..."
for i in $(seq 1 40); do
    CODIGO="$(curl -s -o /dev/null -w '%{http_code}' -m 10 \
              "https://${APP_JAVA}.azurewebsites.net/actuator/health" || true)"
    if [ "$CODIGO" = "200" ]; then
        echo "    health respondeu 200 apos ${i} tentativas"
        break
    fi
    sleep 15
done

curl -s "https://${APP_JAVA}.azurewebsites.net/actuator/health"; echo
echo "==> Proximo: bash azure/08-deploy-dotnet.sh"
