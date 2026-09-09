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
# O `if !` em volta do read existe por causa do EOF.
#
# Sem entrada interativa (rodando por pipe, por CI, ou com < /dev/null) o read
# devolve 1, e com `set -e` o script morria ALI -- sem apagar nada, o que esta
# certo, mas tambem sem imprimir "Cancelado" e saindo com codigo 1. Quem visse
# so o banner de aviso seguido de erro concluiria que a exclusao quebrou, e
# rodaria de novo. Agora cancelar e cancelar, e diz que foi.
if ! read -r -p "Digite 'confirmo' para prosseguir: " RESPOSTA; then
    echo
    echo "Cancelado: nao ha entrada interativa para confirmar."
    exit 0
fi
if [ "$RESPOSTA" != "confirmo" ]; then
    echo "Cancelado."
    exit 0
fi

# Sem --no-wait de proposito: o comando so retorna quando terminou.
#
# SO QUE O EXIT CODE DELE NAO E CONFIAVEL PARA DIZER SE APAGOU.
# Essa espera dura varios minutos (o MySQL demora), e da tempo do token do az
# expirar no meio. Quando expira, o CLI devolve erro DEPOIS de o ARM ja ter
# aceitado e concluido a exclusao -- e o script anunciava falha para uma
# exclusao que deu certo. Aconteceu no ensaio, e a leitura errada do exit code
# levou a afirmar que os recursos continuavam de pe, cobrando.
#
# Por isso o resultado e CONFERIDO no final, nunca deduzido do exit code.
echo "==> Apagando $RG (pode levar alguns minutos)..."
if ! az group delete --name "$RG" --yes; then
    echo
    echo "[aviso] o comando terminou com erro. Isso NAO quer dizer que a exclusao" >&2
    echo "        falhou -- o pedido pode ter sido aceito e o erro ter vindo da" >&2
    echo "        espera. Conferindo o estado real..." >&2
fi

# Tres desfechos, e eles sao diferentes: apagou, nao apagou, e nao consegui
# saber. O terceiro nao pode se disfarcar de um dos outros dois.
SAIDA="$(az group show -n "$RG" -o none 2>&1)" && AINDA_EXISTE=1 || AINDA_EXISTE=0
if [ "$AINDA_EXISTE" -eq 1 ]; then
    echo "[ERRO] o grupo $RG ainda existe. Nada foi apagado." >&2
    echo "       Rode de novo, ou apague pelo portal." >&2
    exit 1
elif echo "$SAIDA" | grep -qiE "ResourceGroupNotFound|could not be found"; then
    echo "==> Confirmado: o grupo $RG nao existe mais."
    echo "    Veja o que sobrou com: az group list -o table"
else
    echo "[ATENCAO] nao consegui confirmar se o grupo foi apagado." >&2
    echo "          A consulta falhou por outro motivo (sessao expirada ou rede):" >&2
    echo "          $SAIDA" >&2
    echo "          Rode 'az login' e confira: az group list -o table" >&2
    exit 1
fi
