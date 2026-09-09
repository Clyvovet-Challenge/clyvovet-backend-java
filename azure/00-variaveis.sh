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
# As SEIS. As duas primeiras sao exigidas aqui; as quatro seguintes, pelo 06 --
# e descobrir isso no meio do 06, com os cinco recursos ja criados, e o tipo de
# surpresa que custa uma gravacao.
#
#     export MYSQL_PASSWORD='...'      # senha do admin do MySQL          [aqui]
#     export JWT_SECRET='...'          # base64, minimo 32 bytes          [aqui]
#     export DOTNET_API_KEY='...'      # chave da API .NET                [06]
#     export TELEGRAM_BOT_TOKEN='...'  # token do bot; formato valido basta [06]
#     export ADMIN_EMAIL='...'         # primeiro ADMIN da plataforma     [06]
#     export ADMIN_SENHA='...'         # anote: so aparece uma vez        [06]

export SUBSCRIPTION="2tdspw-rm562312-pedrooliveira"

# Uma regiao so para tudo. A infra anterior tinha o grupo em brazilsouth e o banco
# em chilecentral, e toda consulta atravessava regioes. Confirmado por CLI que
# brazilsouth oferece App Service Linux (B1 e B2) e MySQL Burstable.
#
# TUDO ABAIXO ACEITA OVERRIDE PELO AMBIENTE, e o padrao continua o mesmo.
# Serve para duas coisas reais:
#   - ensaiar o deploy inteiro sem queimar os nomes de producao (MYSQL_SERVER,
#     APP_JAVA e APP_DOTNET sao globais em toda a Azure, nao so na assinatura);
#   - subir noutra regiao quando esta estiver com problema, sem editar arquivo
#     no meio do deploy -- foi exatamente o caso do endpoint de capabilities do
#     MySQL respondendo 500 so em brazilsouth.
export LOCATION="${LOCATION:-brazilsouth}"
export RG="${RG:-rg-clyvovet-sprint3}"

export MYSQL_SERVER="${MYSQL_SERVER:-mysql-clyvovet-rm562312}"
export MYSQL_DB="${MYSQL_DB:-clyvovet}"
export MYSQL_ADMIN="${MYSQL_ADMIN:-clyvovetadmin}"
export MYSQL_SKU="${MYSQL_SKU:-Standard_B1ms}"
export MYSQL_TIER="${MYSQL_TIER:-Burstable}"
# 8.0.21, e NAO "8.0".
#
# O CLI valida este campo localmente contra a lista que a API devolve, e a lista
# traz a versao completa: {'5.7', '8.0.21', '8.4', '9.5'}. Com "8.0" ele recusa
# antes de chamar a Azure -- e a mensagem some no meio do log, porque logo antes
# aparece um InternalServerError transitorio do endpoint de capabilities que nao
# tem nada a ver com isso. Foi assim que este bug ficou escondido: o erro visivel
# nao era o erro real.
#
#     Incorrect value for --version. Allowed values : {'8.0.21', '9.5', '8.4', '5.7'}
#
# Continua sendo o MySQL 8.0 que a aplicacao espera -- 8.0.21 e a versao dessa
# linha que o Flexible Server publica.
export MYSQL_VERSION="${MYSQL_VERSION:-8.0.21}"

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
export PLAN_NAME="${PLAN_NAME:-plan-clyvovet-sprint3}"
export PLAN_SKU="${PLAN_SKU:-B1}"

export APP_JAVA="${APP_JAVA:-app-clyvovet-java-rm562312}"
export APP_DOTNET="${APP_DOTNET:-app-clyvovet-dotnet-rm562312}"

# Onde o repositorio da API .NET esta clonado, para os scripts 05 e 08.
export CAMINHO_DOTNET="${CAMINHO_DOTNET:-../../ClyvoVet-api}"

exigir() {
    local nome="$1"
    if [ -z "${!nome:-}" ]; then
        echo "[ERRO] defina $nome no ambiente antes de rodar. Ex: export $nome='...'" >&2
        return 1
    fi
}

# ---------------------------------------------------------------------------
# Providers -- o que barrou este deploy na primeira tentativa
# ---------------------------------------------------------------------------
#
# Assinatura nova nao vem com todos os namespaces habilitados. Nesta, tanto o
# Microsoft.Web quanto o Microsoft.DBforMySQL estavam NotRegistered, e sem eles
# o 02 e o 03 falham com "subscription is not registered to use namespace ...".
# O detalhe que doi: a falha so aparece DEPOIS do 01, com o grupo ja criado.
#
# Quem registra e espera e o 01. Estas funcoes existem para que o 02 e o 03,
# quando rodados avulsos, recusem cedo e digam o que fazer -- em vez de morrer
# no meio com uma mensagem que nao explica nada.

export PROVIDERS_NECESSARIOS="Microsoft.Web Microsoft.DBforMySQL"

estado_do_provider() {
    az provider show -n "$1" --query registrationState -o tsv 2>/dev/null
}

exigir_provider() {
    local ns="$1"
    local estado
    estado="$(estado_do_provider "$ns")"
    if [ "$estado" != "Registered" ]; then
        echo "[ERRO] o provider $ns esta '${estado:-desconhecido}' nesta assinatura." >&2
        echo "       Este passo falharia com 'subscription is not registered to use" >&2
        echo "       namespace $ns'. Registre (e espere) rodando:" >&2
        echo "           bash azure/01-resource-group.sh" >&2
        return 1
    fi
}

# ---------------------------------------------------------------------------
# Cliente MySQL -- sem exigir instalacao na maquina de quem grava
# ---------------------------------------------------------------------------
#
# O item 9.3 do video pede "demonstracao detalhada e individual de todas as
# operacoes do CRUD diretamente no Banco de Dados por SELECT". Isso precisa de um
# cliente mysql, e o Git Bash nao traz nenhum. Descoberto no ensaio na Azure, com
# o deploy inteiro no ar e a secao 6 do 09 saindo "[pulado]": o deploy estava
# provado e a gravacao, mesmo assim, impossivel.
#
# Instalar o MySQL Community na maquina de quem for gravar e uma dependencia a
# mais para dar errado na hora. O container faz o mesmo com o que a maquina ja
# tem, e some sozinho depois (--rm).
#
# A senha vai por MYSQL_PWD, e NAO em -p na linha de comando: argv aparece em
# `docker ps` e na lista de processos do host.
#
# Retorna 127 quando nao ha cliente NEM docker -- assim o chamador consegue dizer
# "nao consegui medir" em vez de "esta tudo bem", que sao coisas diferentes.
export MYSQL_IMAGEM="${MYSQL_IMAGEM:-mysql:8.0}"

mysql_host() { echo "${MYSQL_SERVER}.mysql.database.azure.com"; }

mysql_do_azure() {
    local sql="$1"
    if command -v mysql >/dev/null 2>&1; then
        MYSQL_PWD="$MYSQL_PASSWORD" mysql -h "$(mysql_host)" -u "$MYSQL_ADMIN" --ssl-mode=REQUIRED -N -B -e "$sql" "$MYSQL_DB"
    elif command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
        docker run --rm -e MYSQL_PWD="$MYSQL_PASSWORD" "$MYSQL_IMAGEM" mysql -h "$(mysql_host)" -u "$MYSQL_ADMIN" --ssl-mode=REQUIRED -N -B -e "$sql" "$MYSQL_DB"
    else
        return 127
    fi
}

exigir MYSQL_PASSWORD || return 1 2>/dev/null || exit 1
exigir JWT_SECRET     || return 1 2>/dev/null || exit 1

echo "==> assinatura: $SUBSCRIPTION"
echo "==> regiao:     $LOCATION"
echo "==> grupo:      $RG"
