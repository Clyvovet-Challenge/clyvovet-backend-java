#!/usr/bin/env bash
# Descobre o que a assinatura de estudante REALMENTE oferece, antes de decidir
# regiao e SKU. Nao cria nada, nao apaga nada -- so consulta.
#
# POR QUE ESTE SCRIPT EXISTE
# Assinatura de estudante tem catalogo restrito, e ele varia por regiao e por
# conta. Escolher SKU no chute e descobrir na hora do 'create' custa tempo que
# nao temos. Um sinal disso ja esta no repositorio da API .NET: o resource group
# de la esta em brazilsouth e o MySQL em chilecentral. O motivo nao esta
# documentado, mas o padrao sugere que o Burstable nao estava disponivel na
# primeira regiao.
#
#   bash azure/00-descobrir-recursos.sh
#
# Rode com a SUA conta logada:  az login

set -uo pipefail

REGIOES="${REGIOES:-brazilsouth eastus2 chilecentral}"

linha() { printf '\n%s\n' "------------------------------------------------------------"; }

linha
echo "1. QUAL CONTA E QUAL ASSINATURA"
az account show --query "{assinatura:name, id:id, estado:state, usuario:user.name}" -o yaml 2>/dev/null \
  || { echo "  Nao ha sessao ativa. Rode: az login"; exit 1; }

echo
echo "  Todas as assinaturas visiveis:"
az account list --query "[].{nome:name, estado:state, padrao:isDefault}" -o table

linha
echo "2. REGIOES HABILITADAS PARA ESTA ASSINATURA"
echo "   (se brazilsouth nao aparecer aqui, a decisao ja esta tomada)"
az account list-locations --query "[].name" -o tsv 2>/dev/null | sort | tr '\n' ' ' | fold -s -w 76
echo

linha
echo "3. APP SERVICE PLAN -- SKUs Linux por regiao"
echo "   Precisamos de B1, B2 ou B3. As duas APIs vao dividir UM plano."
for R in $REGIOES; do
  echo
  echo "   >>> $R"
  RESULTADO=$(az appservice list-locations --linux-workers-enabled \
                --query "[?name=='$R' || contains(displayName, '$R')].displayName" -o tsv 2>/dev/null)
  if [ -n "$RESULTADO" ]; then
    echo "       Linux workers: DISPONIVEL"
  else
    echo "       Linux workers: nao listado -- provavelmente indisponivel"
  fi
done

linha
echo "4. MYSQL FLEXIBLE SERVER -- SKUs Burstable por regiao"
echo "   Queremos Standard_B1ms ou Standard_B2s."
for R in $REGIOES; do
  echo
  echo "   >>> $R"
  az mysql flexible-server list-skus --location "$R" \
     --query "[].supportedFlexibleServerEditions[?name=='Burstable'].supportedServerVersions[].supportedSkus[].name" \
     -o tsv 2>/dev/null | sort -u | tr '\n' ' ' | fold -s -w 72 | sed 's/^/       /'
  echo
done

linha
echo "5. RUNTIMES DISPONIVEIS -- Java 17 e .NET 8 precisam aparecer"
az webapp list-runtimes --os linux -o tsv 2>/dev/null | grep -iE "^JAVA:17|^DOTNETCORE:8" | sed 's/^/   /' \
  || echo "   nao foi possivel listar"

linha
echo "6. O QUE JA EXISTE NESTA ASSINATURA"
echo "   (para nao criar em cima de nada, e para saber o que consome credito)"
az group list --query "[].{grupo:name, regiao:location}" -o table 2>/dev/null | sed 's/^/   /'

linha
echo "RESUMO DO QUE PRECISO DE VOLTA"
echo "  - a regiao onde App Service Linux E MySQL Burstable aparecem JUNTOS"
echo "  - se B2 aparece, ou se so B1 esta disponivel"
echo "  - se ja existe algum resource group seu na lista do item 6"
echo
