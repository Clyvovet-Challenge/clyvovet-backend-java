#!/usr/bin/env bash
# Deploy da API .NET: dotnet publish e publicacao por --type zip.
#
# SEM CONTAINER. O repositorio da .NET tem Dockerfile, mas publicar a imagem
# custaria -40 ("App Containerizado") na Opcao 2. O artefato aqui sai do
# dotnet publish e vai como zip para o runtime DOTNETCORE:8.0 nativo.
#
#     bash azure/08-deploy-dotnet.sh
set -euo pipefail
cd "$(dirname "$0")"
source ./00-variaveis.sh

az account set --subscription "$SUBSCRIPTION"

PROJETO="${CAMINHO_DOTNET}/ClyvoVet.Api/ClyvoVet.Api.csproj"
if [ ! -f "$PROJETO" ]; then
    echo "[ERRO] nao achei $PROJETO" >&2
    echo "       ajuste CAMINHO_DOTNET em 00-variaveis.sh" >&2
    exit 1
fi

# O `zip` NAO vem no Git Bash.
#
# Na maquina em que isto foi ensaiado ele existe por acidente: o Oracle XE poe o
# proprio zip no PATH. Em outra maquina do grupo -- a de quem for gravar, por
# exemplo -- o script morreria aqui com "zip: command not found", depois do
# publish, com os cinco recursos ja criados. Falhar antes, dizendo o que fazer,
# custa tres linhas.
if ! command -v zip >/dev/null 2>&1; then
    echo "[ERRO] 'zip' nao encontrado no PATH." >&2
    echo "       Instale (Git Bash nao traz) ou gere o pacote pelo PowerShell:" >&2
    echo "         dotnet publish <csproj> -c Release -o publish" >&2
    echo "         Compress-Archive -Path publish\* -DestinationPath api.zip" >&2
    echo "         az webapp deploy -g \$RG -n \$APP_DOTNET --type zip --src-path api.zip" >&2
    exit 1
fi

SAIDA="$(mktemp -d)"
echo "==> dotnet publish -c Release..."
dotnet publish "$PROJETO" -c Release -o "$SAIDA/publish" --nologo -v q

echo "==> Empacotando..."
( cd "$SAIDA/publish" && zip -qr ../api.zip . )
echo "    $(du -h "$SAIDA/api.zip" | cut -f1)"

echo "==> Publicando em $APP_DOTNET..."
az webapp deploy \
    --resource-group "$RG" \
    --name "$APP_DOTNET" \
    --type zip \
    --src-path "$SAIDA/api.zip" \
    --async false -o table

echo "==> Aguardando o health check..."
for i in $(seq 1 30); do
    CODIGO="$(curl -s -o /dev/null -w '%{http_code}' -m 10 \
              "https://${APP_DOTNET}.azurewebsites.net/health/live" || true)"
    if [ "$CODIGO" = "200" ]; then
        echo "    live respondeu 200 apos ${i} tentativas"
        break
    fi
    sleep 10
done

curl -s "https://${APP_DOTNET}.azurewebsites.net/health"; echo
rm -rf "$SAIDA"
echo "==> Proximo: bash azure/09-verificar.sh"
