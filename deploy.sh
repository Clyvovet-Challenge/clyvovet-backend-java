#!/bin/bash
# ============================================================
# CLYVO VET — Script Azure CLI
# Provisiona VM Linux, instala Docker e Git, sobe a aplicação
# ============================================================

# Variáveis
RESOURCE_GROUP="clyvovet-rg"
LOCATION="canadacentral"
VM_NAME="clyvovet-vm"
VM_IMAGE="Ubuntu2204"
VM_SIZE="Standard_B2s_v2"
ADMIN_USER="clyvovet"
DNS_LABEL="clyvovet-api"
REPO_URL="https://github.com/Clyvovet-Challenge/clyvovet-backend-java.git"
REPO_DIR="clyvovet-backend-java"

echo "==> Criando Resource Group..."
az group create \
  --name $RESOURCE_GROUP \
  --location $LOCATION

echo "==> Criando VM Linux..."
az vm create \
  --resource-group $RESOURCE_GROUP \
  --name $VM_NAME \
  --image $VM_IMAGE \
  --size $VM_SIZE \
  --admin-username $ADMIN_USER \
  --generate-ssh-keys \
  --public-ip-sku Standard \
  --public-ip-address-dns-name $DNS_LABEL

echo "==> Abrindo porta 8080 (API)..."
az vm open-port \
  --resource-group $RESOURCE_GROUP \
  --name $VM_NAME \
  --port 8080 \
  --priority 1001

echo "==> Abrindo porta 80 (HTTP)..."
az vm open-port \
  --resource-group $RESOURCE_GROUP \
  --name $VM_NAME \
  --port 80 \
  --priority 1002

echo "==> Instalando Docker, Git e ferramentas na VM..."
az vm run-command invoke \
  --resource-group $RESOURCE_GROUP \
  --name $VM_NAME \
  --command-id RunShellScript \
  --scripts "
    apt-get update -y &&
    apt-get install -y git curl nano &&
    curl -fsSL https://get.docker.com | sh &&
    systemctl enable docker &&
    systemctl start docker &&
    usermod -aG docker $ADMIN_USER
  "

echo "==> Clonando repositório e subindo aplicação com Docker Compose..."
# O clone e raso e esparso: a VM so precisa dos arquivos da raiz (Dockerfile,
# docker-compose.yml, pom.xml) e de src/. Assim os docs e os documentos de
# entrega nem sao baixados -- "--filter=blob:none" evita puxar
# o conteudo deles e "sparse-checkout set src" os mantem fora do disco.
#
# JWT_SECRET e gerado uma unica vez na VM e persistido em ~/.clyvovet.env:
# redeploys reaproveitam o mesmo valor (senao cada "docker compose up" novo
# derrubaria as sessoes ativas ao trocar a chave de assinatura por baixo).
#
# O bloco distingue primeiro deploy de redeploy. Antes ele so clonava, e num
# redeploy o diretorio ja existia: o "git clone" falhava, a cadeia de "&&"
# abortava e o "docker compose up" nem chegava a rodar -- o script terminava
# sem erro visivel e sem atualizar nada.
#
# No redeploy vai "fetch + reset --hard" em vez de "pull" de proposito: a VM e
# alvo de deploy, nao copia de trabalho. O reset garante que ela fique
# identica a origin/main mesmo que alguem tenha editado algo la dentro, e
# nao trava num conflito de merge como o pull travaria.
az vm run-command invoke \
  --resource-group $RESOURCE_GROUP \
  --name $VM_NAME \
  --command-id RunShellScript \
  --scripts "
    cd /home/$ADMIN_USER &&
    [ -f .clyvovet.env ] || echo JWT_SECRET=\$(openssl rand -base64 32) > .clyvovet.env &&
    chmod 600 .clyvovet.env &&
    if [ -d $REPO_DIR/.git ]; then
      cd $REPO_DIR &&
      git fetch --depth 1 origin main &&
      git reset --hard FETCH_HEAD
    else
      git clone --depth 1 --filter=blob:none --sparse $REPO_URL $REPO_DIR &&
      cd $REPO_DIR
    fi &&
    git sparse-checkout set src &&
    set -a && . ../.clyvovet.env && set +a &&
    docker compose up -d --build
  "

echo ""
echo "==> Deploy concluído!"
echo "==> API disponível em: http://$DNS_LABEL.$LOCATION.cloudapp.azure.com:8080"
echo "==> Swagger: http://$DNS_LABEL.$LOCATION.cloudapp.azure.com:8080/swagger-ui.html"