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
SUBIU=0
for i in $(seq 1 40); do
    CODIGO="$(curl -s -o /dev/null -w '%{http_code}' -m 10 \
              "https://${APP_JAVA}.azurewebsites.net/actuator/health" || true)"
    if [ "$CODIGO" = "200" ]; then
        echo "    health respondeu 200 apos ${i} tentativas"
        SUBIU=1
        break
    fi
    sleep 15
done

curl -s "https://${APP_JAVA}.azurewebsites.net/actuator/health"; echo

# O LACO ACIMA NAO PODE TERMINAR EM SILENCIO.
#
# Antes, esgotar as 40 tentativas caia direto no "Proximo: 08" e o script saia 0.
# Um deploy que nunca subiu ficava indistinguivel de um que subiu: quem seguisse
# o README passava para o 08 achando que estava tudo certo, e so descobriria no
# 09 -- ou, pior, gravando.
if [ "$SUBIU" -ne 1 ]; then
    echo >&2
    echo "[ERRO] a API Java nao respondeu 200 em /actuator/health depois de 40" >&2
    echo "       tentativas (10 minutos). NAO siga para o 08." >&2
    echo >&2
    echo "       O log do container diz o motivo:" >&2
    echo "           az webapp log tail -g $RG -n $APP_JAVA" >&2
    echo >&2
    echo "       Suspeitos, em ordem de frequencia:" >&2
    echo "         - SERVER_PORT ausente: o Spring sobe na 8080 e o App Service" >&2
    echo "           encaminha para a 80, entao o health externo nunca responde;" >&2
    echo "         - credencial ou host do banco errados no 06;" >&2
    echo "         - o Flyway parando numa migration." >&2
    exit 1
fi
echo "==> Proximo: bash azure/08-deploy-dotnet.sh"
