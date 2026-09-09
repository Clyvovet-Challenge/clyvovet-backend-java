#!/usr/bin/env bash
# Descobre o que a assinatura de estudante REALMENTE oferece, antes de decidir
# regiao e SKU. Nao cria nada, nao apaga nada -- so consulta.
#
# POR QUE ESTE SCRIPT EXISTE
# Assinatura de estudante tem catalogo restrito, e ele varia por regiao e por
# conta. Escolher SKU no chute e descobrir na hora do 'create' custa tempo que
# nao temos.
#
# O FALSO NEGATIVO, QUE E O RISCO REAL AQUI
# A versao anterior tratava "consulta vazia" como "nao disponivel". Sao coisas
# diferentes. A consulta tambem volta vazia quando:
#   - o namespace esta desligado na assinatura (Microsoft.Web, DBforMySQL);
#   - o endpoint de capabilities esta fora do ar (o do MySQL responde
#     InternalServerError com alguma frequencia, e POR REGIAO); ou
#   - a propria consulta esta errada -- e tres estavam, ver abaixo.
#
# Isso tem consequencia pratica. O repositorio da API .NET tem o resource group
# em brazilsouth e o MySQL em chilecentral, sem motivo documentado. A leitura
# antiga era "o Burstable nao existia em brazilsouth". Pode ter sido isso -- ou
# pode ter sido este falso negativo, e a infra ficou partida entre duas regioes
# por causa de uma consulta que falhou calada.
#
# TRES BUGS CORRIGIDOS, TODOS DO TIPO "MENTIA SEM AVISAR"
#   1. A checagem do App Service comparava 'brazilsouth' com o que a API devolve,
#      que e 'Brazil South'. Nunca casava: TODA regiao saia como indisponivel.
#   2. O JMESPath dos SKUs de MySQL filtrava antes de achatar a lista, e voltava
#      vazio ate numa regiao que responde. Confirmado contra eastus2, que devolve
#      nove SKUs Burstable com a query corrigida e nenhum com a antiga.
#   3. O filtro de runtimes procurava "JAVA:17", e o comando devolve "JAVA|17".
#      A conferencia de runtime nunca conferiu nada.
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
echo "2. NAMESPACES -- leia ANTES de acreditar nos itens 4, 5 e 6"
echo "   Assinatura nova vem com namespaces desligados. Desligado, ele faz a"
echo "   consulta falhar e voltar vazia -- e vazio PARECE 'indisponivel nesta"
echo "   regiao' sem ser."
echo
for NS in Microsoft.Web Microsoft.DBforMySQL; do
  ESTADO="$(az provider show -n "$NS" --query registrationState -o tsv 2>/dev/null)"
  echo "   $NS: ${ESTADO:-nao foi possivel consultar}"
done
echo
echo "   Se algum nao estiver 'Registered', o 01 liga os dois e espera. Avulso,"
echo "   se preferir (e gratuito e nao cria recurso nenhum):"
echo "       az provider register -n Microsoft.Web"
echo "       az provider register -n Microsoft.DBforMySQL"

linha
echo "3. REGIOES -- o catalogo E a policy"
echo "   Para valer, a regiao precisa passar nas duas. A policy costuma ser o"
echo "   filtro mais estreito, e e ela que recusa o create la na frente."
echo
echo "   Permitidas pela policy:"
# O `tr -d` do \r aparece em toda leitura multi-linha daqui para baixo: o Azure
# CLI no Windows termina cada linha com CRLF, e o $( ) do shell so remove o \r
# final. Os do meio sobram, quebram comparacao exata e sujam o que e exibido.
PERMITIDAS="$(az policy assignment list --query "[].parameters.listOfAllowedLocations.value[]" -o tsv 2>/dev/null | tr -d '\r' | sort -u | tr '\n' ' ')"
if [ -n "$PERMITIDAS" ]; then
  echo "       $PERMITIDAS"
else
  echo "       nenhuma policy de regiao encontrada (ou sem permissao de leitura)"
fi
echo
echo "   Existentes para a assinatura:"
az account list-locations --query "[].name" -o tsv 2>/dev/null | tr -d '\r' | sort | tr '\n' ' ' | fold -s -w 70 | sed 's/^/       /'
echo

linha
echo "4. APP SERVICE PLAN -- Linux por regiao"
echo "   Precisamos de B1, B2 ou B3. As duas APIs vao dividir UM plano."
for R in $REGIOES; do
  echo
  echo "   >>> $R"
  ERRO="$(mktemp)"
  if SAIDA="$(az appservice list-locations --linux-workers-enabled --sku B1 --query "[].name" -o tsv 2>"$ERRO")"; then
    # A API devolve "Brazil South"; a variavel tem "brazilsouth". Normaliza os
    # dois lados antes de comparar -- era aqui que a checagem antiga mentia.
    if echo "$SAIDA" | tr -d '\r' | tr 'A-Z' 'a-z' | tr -d ' ' | grep -qx "$R"; then
      echo "       B1 Linux: DISPONIVEL"
    else
      echo "       B1 Linux: nao aparece no catalogo desta assinatura"
    fi
  else
    echo "       NAO FOI POSSIVEL CONSULTAR -- veja o item 2 (Microsoft.Web)."
    echo "       Isto NAO quer dizer que a regiao nao serve."
  fi
  rm -f "$ERRO"
done

linha
echo "5. MYSQL FLEXIBLE SERVER -- SKUs Burstable por regiao"
echo "   Queremos Standard_B1ms ou Standard_B2s."
for R in $REGIOES; do
  echo
  echo "   >>> $R"
  # O stderr vai para arquivo, e nao para dentro de SAIDA: este comando emite um
  # WARNING de preco mesmo quando da certo, e com 2>&1 o aviso era capturado
  # como se fosse a lista de SKUs -- a regiao aparecia "disponivel" exibindo um
  # link de precos no lugar dos SKUs.
  ERRO="$(mktemp)"
  if SAIDA="$(az mysql flexible-server list-skus --location "$R" \
       --query "[].supportedFlexibleServerEditions[] | [?name=='Burstable'].supportedServerVersions[].supportedSkus[].name" \
       -o tsv 2>"$ERRO")"; then
    if [ -n "$SAIDA" ]; then
      echo "$SAIDA" | tr -d '\r' | sort -u | tr '\n' ' ' | fold -s -w 66 | sed 's/^/       /'
      echo
    else
      echo "       nenhum SKU Burstable listado nesta regiao"
    fi
  else
    echo "       NAO FOI POSSIVEL CONSULTAR."
    if grep -qi "InternalServerError" "$ERRO"; then
      echo "       Motivo: InternalServerError no endpoint de capabilities."
      echo "       E do lado da Azure e vale POR REGIAO: ja foi observado"
      echo "       quebrado em brazilsouth com eastus2 respondendo normal, no"
      echo "       mesmo minuto. Espere e tente de novo, ou escolha outra regiao"
      echo "       da lista do item 3."
    else
      echo "       Veja o item 2: Microsoft.DBforMySQL pode estar desligado."
    fi
    echo "       Em nenhum dos casos isso significa que a regiao nao serve."
  fi
  rm -f "$ERRO"
done

linha
echo "6. RUNTIMES DISPONIVEIS -- Java 17 e .NET 8 precisam aparecer"
echo "   Atencao ao separador: a LISTAGEM imprime 'JAVA|17-java17', mas o que o"
echo "   'az webapp create --runtime' aceita e 'JAVA:17-java17' (Framework:Versao)."
echo "   Sao formas diferentes da mesma coisa -- o 04 e o 05 usam a com ':', e"
echo "   estao certos. Nao troque para '|' achando que esta corrigindo."
ERRO="$(mktemp)"
if SAIDA="$(az webapp list-runtimes --os linux -o tsv 2>"$ERRO")"; then
  # O separador e '|', nao ':'. O padrao antigo ("^JAVA:17") nunca casou.
  echo "$SAIDA" | tr -d '\r' | grep -iE "^JAVA.17|^DOTNETCORE.8[.]0" | sed 's/^/   /' \
    || echo "   nenhum dos dois apareceu na lista"
else
  echo "   NAO FOI POSSIVEL CONSULTAR -- veja o item 2 (Microsoft.Web)."
fi
rm -f "$ERRO"

linha
echo "7. O QUE JA EXISTE NESTA ASSINATURA"
echo "   (para nao criar em cima de nada, e para saber o que consome credito)"
az group list --query "[].{grupo:name, regiao:location}" -o table 2>/dev/null | sed 's/^/   /'

linha
echo "RESUMO DO QUE PRECISO DE VOLTA"
echo "  - os dois namespaces do item 2 estao 'Registered'?"
echo "  - a regiao escolhida aparece na lista da policy (item 3)?"
echo "  - App Service Linux e MySQL Burstable aparecem JUNTOS nessa regiao?"
echo "  - onde saiu 'NAO FOI POSSIVEL CONSULTAR', a resposta e 'nao sei', e"
echo "    nunca 'nao tem'. Resolva o item 2 e rode este script de novo."
echo "  - ja existe algum resource group seu no item 7?"
echo
