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
az vm run-command invoke \
  --resource-group $RESOURCE_GROUP \
  --name $VM_NAME \
  --command-id RunShellScript \
  --scripts "
    cd /home/$ADMIN_USER &&
    git clone https://github.com/leojp04/clyvovet-backend-java.git &&
    cd clyvovet-backend-java &&
    docker compose up -d --build
  "

echo ""
echo "==> Deploy concluído!"
echo "==> API disponível em: http://$DNS_LABEL.$LOCATION.cloudapp.azure.com:8080"
echo "==> Swagger: http://$DNS_LABEL.$LOCATION.cloudapp.azure.com:8080/swagger-ui.html"