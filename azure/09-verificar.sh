#!/usr/bin/env bash
# Prova que o deploy funciona de ponta a ponta, e ensaia o que o video vai mostrar.
#
# POR QUE ESTE SCRIPT EXISTE
# O item 9.2 exige o deploy "seguindo exatamente os passos descritos no README",
# comecando pelo clone e SEM CORTES ao evidenciar os testes. Nao ha como consertar
# no meio da gravacao. Este script roda o caminho inteiro antes, e imprime no final
# o SQL exato que o item 9.3 pede -- para que na hora de gravar seja copiar e colar.
#
#     bash azure/09-verificar.sh
set -uo pipefail
cd "$(dirname "$0")"
source ./00-variaveis.sh

exigir DOTNET_API_KEY || exit 1

JAVA="https://${APP_JAVA}.azurewebsites.net"
DOTNET="https://${APP_DOTNET}.azurewebsites.net"
SELO="$(date +%s)"
EMAIL="verificacao.${SELO}@clyvovet.com"
SENHA='Verifica@12345'

falhou=0
checar() {
    local rotulo="$1" esperado="$2" obtido="$3"
    if [ "$obtido" = "$esperado" ]; then
        printf '  [ok]   %-46s %s\n' "$rotulo" "$obtido"
    else
        printf '  [FALHA] %-45s esperado %s, obtido %s\n' "$rotulo" "$esperado" "$obtido"
        falhou=$((falhou + 1))
    fi
}

echo
echo "=============================================================="
echo " 1. SAUDE DAS DUAS APIS"
echo "=============================================================="
checar "GET /actuator/health (Java)" "200" \
  "$(curl -s -o /dev/null -w '%{http_code}' -m 20 "$JAVA/actuator/health")"
checar "GET /health/live (.NET)" "200" \
  "$(curl -s -o /dev/null -w '%{http_code}' -m 20 "$DOTNET/health/live")"
checar "GET /health/ready (.NET, encosta no banco)" "200" \
  "$(curl -s -o /dev/null -w '%{http_code}' -m 30 "$DOTNET/health/ready")"

echo
echo "=============================================================="
echo " 2. AUTENTICACAO -- prova que o Flyway criou o schema"
echo "=============================================================="
CADASTRO="$(curl -s -m 30 -X POST "$JAVA/api/v1/auth/registrar" \
  -H 'Content-Type: application/json' \
  -d "{\"nome\":\"Verificacao ${SELO}\",\"email\":\"${EMAIL}\",\"senha\":\"${SENHA}\",\"cpf\":\"39053344705\",\"telefone\":\"11999990000\"}")"
TUTOR="$(echo "$CADASTRO" | python -c "import sys,json;print(json.load(sys.stdin).get('tutorId',''))" 2>/dev/null)"
[ -n "$TUTOR" ] && printf '  [ok]   %-46s %s\n' "POST /auth/registrar" "$TUTOR" \
  || { printf '  [FALHA] %-45s %s\n' "POST /auth/registrar" "$CADASTRO"; falhou=$((falhou+1)); }

TOKEN="$(curl -s -m 30 -X POST "$JAVA/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"${EMAIL}\",\"senha\":\"${SENHA}\"}" \
  | python -c "import sys,json;print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null)"
[ -n "$TOKEN" ] && printf '  [ok]   %-46s %d chars\n' "POST /auth/login" "${#TOKEN}" \
  || { printf '  [FALHA] %-45s sem token\n' "POST /auth/login"; falhou=$((falhou+1)); }

echo
echo "=============================================================="
echo " 2b. PARIDADE DO SEGREDO ENTRE AS DUAS APIS"
echo "=============================================================="
# A checagem mais barata do JWT compartilhado, e a que pega o erro mais caro.
#
# As duas APIs assinam com o MESMO valor de Jwt__Secret / JWT_SECRET, mas a chave
# HMAC sao os bytes do base64 DECODIFICADO. Se a .NET derivasse a chave dos bytes
# da string (o idioma comum em ASP.NET), o valor seria o mesmo e a chave seria
# outra: nenhuma assinatura conferiria, e o sintoma em producao e 401 em tudo,
# sem nada no log.
#
# Com Api__EscopoPorTutor=false o token e ignorado, entao 200 aqui nao prova a
# paridade -- prova apenas que o token nao ATRAPALHA. A prova completa exige o
# escopo ligado, e por isso o resultado abaixo e informativo e nao entra em
# "falhou": ele existe para ser lido antes de virar a flag.
COD_COM_TOKEN="$(curl -s -o /dev/null -w '%{http_code}' -m 30 \
  "$DOTNET/api/v1/lembretes?tamanho=1" \
  -H "X-Api-Key: $DOTNET_API_KEY" -H "Authorization: Bearer $TOKEN")"
ESCOPO="$(az webapp config appsettings list -g "$RG" -n "$APP_DOTNET" \
  --query "[?name=='Api__EscopoPorTutor'].value | [0]" -o tsv 2>/dev/null)"

printf '  Api__EscopoPorTutor = %s\n' "${ESCOPO:-nao definida}"
if [ "$ESCOPO" = "true" ]; then
    checar "GET /lembretes com Bearer (escopo LIGADO)" "200" "$COD_COM_TOKEN"
    echo "         200 aqui prova que as duas APIs derivam a MESMA chave."
else
    printf '  [info] GET /lembretes com Bearer devolveu %s\n' "$COD_COM_TOKEN"
    echo "         Com o escopo desligado o token e ignorado; isto nao prova paridade."
    echo "         Para provar: ligue a flag e rode este script de novo."
fi

echo
echo "=============================================================="
echo " 3. CRUD NA API JAVA -- t_clyvo_animal"
echo "=============================================================="
ANIMAL="$(curl -s -m 30 -X POST "$JAVA/api/v1/animais" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{\"nome\":\"Verificado ${SELO}\",\"especie\":\"Cachorro\",\"raca\":\"Golden Retriever\",\"porte\":\"GRANDE\",\"sexo\":\"MACHO\",\"cor\":\"Dourado\",\"dataNascimento\":\"2022-05-10\",\"tutorId\":\"${TUTOR}\"}" \
  | python -c "import sys,json;print(json.load(sys.stdin).get('id',''))" 2>/dev/null)"
[ -n "$ANIMAL" ] && printf '  [ok]   %-46s %s\n' "POST /animais (CREATE)" "$ANIMAL" \
  || { printf '  [FALHA] %-45s sem id\n' "POST /animais"; falhou=$((falhou+1)); }

checar "GET /animais/{id} (READ)" "200" \
  "$(curl -s -o /dev/null -w '%{http_code}' -m 30 "$JAVA/api/v1/animais/$ANIMAL" -H "Authorization: Bearer $TOKEN")"
checar "PATCH /animais/{id} (UPDATE)" "200" \
  "$(curl -s -o /dev/null -w '%{http_code}' -m 30 -X PATCH "$JAVA/api/v1/animais/$ANIMAL" \
     -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
     -d '{"cor":"Dourado claro"}')"

echo
echo "=============================================================="
echo " 4. INTEGRACAO CRUZADA -- a prova de que o banco e compartilhado"
echo "=============================================================="
# O Bearer vai junto da chave DE PROPOSITO, mesmo com Api__EscopoPorTutor=false.
# Este script e o portao que decide se o deploy esta bom o bastante para gravar --
# se ele so mandasse a X-Api-Key, ligar o escopo transformaria a verificacao em
# falha garantida, e justamente no passo que prova que o banco e compartilhado.
# Mandando os dois headers, o script vale nas duas configuracoes.
LEMBRETE="$(curl -s -m 30 -X POST "$DOTNET/api/v1/lembretes" \
  -H "X-Api-Key: $DOTNET_API_KEY" -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"animalId\":\"${ANIMAL}\",\"titulo\":\"Vacina V10 - ${SELO}\",\"tipo\":0,\"agendadoEm\":\"2026-12-01T10:00:00\",\"recorrente\":false}")"
NOME="$(echo "$LEMBRETE" | python -c "import sys,json;print(json.load(sys.stdin).get('nomeAnimal',''))" 2>/dev/null)"
if [ "$NOME" = "Verificado ${SELO}" ]; then
    printf '  [ok]   %-46s %s\n' "POST /lembretes resolveu nomeAnimal" "$NOME"
    echo "         (a .NET leu t_clyvo_animal que a Java acabou de gravar)"
else
    printf '  [FALHA] %-45s %s\n' "POST /lembretes" "$LEMBRETE"; falhou=$((falhou+1))
fi

echo
echo "=============================================================="
echo " 5. DELETE -- fecha o CRUD"
echo "=============================================================="
ID_LEMBRETE="$(echo "$LEMBRETE" | python -c "import sys,json;print(json.load(sys.stdin).get('id',''))" 2>/dev/null)"
checar "DELETE /lembretes/{id} (.NET)" "204" \
  "$(curl -s -o /dev/null -w '%{http_code}' -m 30 -X DELETE "$DOTNET/api/v1/lembretes/$ID_LEMBRETE" \
     -H "X-Api-Key: $DOTNET_API_KEY" -H "Authorization: Bearer $TOKEN")"
checar "DELETE /animais/{id} (Java)" "204" \
  "$(curl -s -o /dev/null -w '%{http_code}' -m 30 -X DELETE "$JAVA/api/v1/animais/$ANIMAL" -H "Authorization: Bearer $TOKEN")"

echo
echo "=============================================================="
if [ "$falhou" -eq 0 ]; then
    echo " TUDO VERDE. O caminho do video esta provado."
else
    echo " $falhou VERIFICACAO(OES) FALHOU. NAO grave ainda."
fi
echo "=============================================================="

echo
echo "=============================================================="
echo " 6. ORCAMENTO DE CONEXOES -- medido, nao presumido"
echo "=============================================================="
# As duas APIs somam 25 conexoes potenciais com uma instancia de cada: 10 da Java
# (spring.datasource.hikari.maximum-pool-size) e 15 da .NET (Database__MaxPoolSize).
# O teto de um Standard_B1ms depende do tier, e a Azure ja mudou esses numeros --
# entao o certo e ler o valor real. Se 25 nao couber com folga, o lugar de
# descobrir e aqui, e nao durante a gravacao do video.
if command -v mysql >/dev/null 2>&1 && [ -n "${MYSQL_PASSWORD:-}" ]; then
    TETO="$(mysql -h "${MYSQL_SERVER}.mysql.database.azure.com" \
                  -u "$MYSQL_ADMIN" -p"$MYSQL_PASSWORD" --ssl-mode=REQUIRED \
                  -N -B -e "SHOW VARIABLES LIKE 'max_connections';" 2>/dev/null | awk '{print $2}')"
    if [ -n "$TETO" ]; then
        printf '  max_connections do servidor: %s\n' "$TETO"
        printf '  orcamento das duas APIs:     25 (Java 10 + .NET 15)\n'
        if [ "$TETO" -gt 40 ] 2>/dev/null; then
            echo "  [ok]   folga confortavel"
        else
            echo "  [ATENCAO] teto apertado. Baixe os dois pools antes de gravar:"
            echo "            spring.datasource.hikari.maximum-pool-size (repo Java)"
            echo "            Database__MaxPoolSize (app setting da .NET)"
            falhou=$((falhou + 1))
        fi
    else
        echo "  [pulado] nao consegui consultar o servidor"
    fi
else
    echo "  [pulado] cliente mysql ausente ou MYSQL_PASSWORD nao exportada"
fi

cat <<SQL

--------------------------------------------------------------------
 SQL PARA O ITEM 9.3 DO VIDEO
 "Demonstracao detalhada e individual de todas as operacoes do CRUD
  diretamente no Banco de Dados por SELECT"

 Conecte com:
   mysql -h ${MYSQL_SERVER}.mysql.database.azure.com \\
         -u ${MYSQL_ADMIN} -p ${MYSQL_DB} --ssl-mode=REQUIRED

 Duas tabelas relacionadas e do CORE, como o item 4 exige
 (t_clyvo_animal referencia t_clyvo_tutor por FK):
--------------------------------------------------------------------

-- INSERCAO -> exibir no banco (rode depois do POST /animais no app)
SELECT a.id, a.nome, a.raca, a.cor, t.nome AS tutor, t.email
  FROM t_clyvo_animal a
  JOIN t_clyvo_tutor  t ON t.id = a.tutor_id
 ORDER BY a.id DESC LIMIT 5;

-- ATUALIZACAO -> exibir no banco (rode depois do PATCH, mesma consulta)
SELECT a.id, a.nome, a.cor, t.nome AS tutor
  FROM t_clyvo_animal a
  JOIN t_clyvo_tutor  t ON t.id = a.tutor_id
 WHERE a.nome LIKE 'Verificado%';

-- EXCLUSAO -> exibir no banco (a contagem cai, e a linha desaparece)
SELECT COUNT(*) AS animais_restantes FROM t_clyvo_animal;

-- CONSULTA de registros, evidenciando o relacionamento
SELECT t.nome AS tutor, COUNT(a.id) AS qtd_animais
  FROM t_clyvo_tutor t
  LEFT JOIN t_clyvo_animal a ON a.tutor_id = t.id
 GROUP BY t.id, t.nome
 ORDER BY qtd_animais DESC;

SQL

exit "$falhou"
