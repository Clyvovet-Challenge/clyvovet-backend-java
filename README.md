# CLYVO VET — Backend Java

API REST desenvolvida em Spring Boot para o sistema de saúde contínua de pets **CLYVO VET**, como parte do Challenge FIAP 2026 — 1º Semestre.

---

## Integrantes do Grupo

| Nome | RM |
|---|---|
| Fabrício Henrique Pereira | RM 563237 |
| Leonardo José Pereira | RM 563065 |
| Miguel Henrique Oliveira Dias | RM 565492 |
| Pedro Henrique de Oliveira | RM 562312 |

---

## Descrição do Projeto

O CLYVO VET é uma plataforma digital de saúde animal que conecta tutores de pets, veterinários e clínicas parceiras. A solução promove a continuidade do cuidado preventivo, centralizando o histórico clínico dos animais, o agendamento de eventos e o controle de pagamentos.

Este repositório contém o **backend Java/Spring Boot**, responsável por expor uma API REST completa com persistência em banco Oracle (FIAP) ou H2 (desenvolvimento local).

### Benefícios para o Negócio

- Centralização do histórico clínico do pet, acessível para tutores e clínicas
- Redução do gap de continuidade no cuidado preventivo
- Agendamento e controle de eventos clínicos com rastreabilidade completa
- Geração de dados longitudinais para decisões clínicas mais assertivas
- Escalável para múltiplas clínicas e hospitais parceiros

---

## Arquitetura Macro

```
┌─────────────┐     HTTP/REST      ┌──────────────────────────┐
│  Front-end  │ ────────────────►  │  Spring Boot API (8080)  │
│  / Mobile   │                    │                          │
└─────────────┘                    │  Controller              │
                                   │  Service (Cache)         │
                                   │  Repository (JPA)        │
                                   │  Entity (@Column map)    │
                                   └────────────┬─────────────┘
                                                │
                                   ┌────────────▼─────────────┐
                                   │   Oracle 19c (FIAP)      │
                                   │   ou H2 (dev local)      │
                                   └──────────────────────────┘
```

**Stack:** Java 17 · Spring Boot 3.5 · JPA/Hibernate 6.6 · Oracle 19c · H2 · Swagger/OpenAPI · Bean Validation · Spring Cache · Lombok

---

## Pré-requisitos

- Java 17+
- Maven 3.8+
- Acesso ao Oracle FIAP **ou** rodar localmente com H2

---

## Instalação e Execução

### 1. Clonar o repositório

```bash
git clone https://github.com/leojp04/clyvovet-backend-java.git
cd clyvovet-backend-java
```

### 2. Configurar o banco de dados

#### Opção A — Oracle FIAP (produção)

Antes de iniciar a aplicação, execute o script SQL no SQL Developer:

```
src/main/resources/db/db-oracle.sql
```

Este script cria as tabelas, índices, views, procedures e dados de seed. Execute com **Run Script (F5)** conectado ao Oracle da FIAP com suas credenciais.

Ative o perfil Oracle:

```properties
# application.properties
spring.profiles.active=oracle
```

Configure suas credenciais em `application-oracle.properties`:

```properties
spring.datasource.url=jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL
spring.datasource.username=SEU_RM
spring.datasource.password=SUA_SENHA
```

#### Opção B — H2 (desenvolvimento local, sem Oracle)

Ative o perfil H2:

```properties
# application.properties
spring.profiles.active=h2
```

### 3. Executar

```bash
mvn spring-boot:run
```

A aplicação sobe em `http://localhost:8080`

### 4. Acessar o Swagger

```
http://localhost:8080/swagger-ui.html
```

---

## Endpoints da API

### Tutores — `/tutores`

| Método | Rota | Descrição |
|---|---|---|
| GET | `/tutores` | Lista todos (paginado). Filtros: `?nome=` `?cidade=` |
| GET | `/tutores/{id}` | Busca por ID |
| POST | `/tutores` | Cadastra novo tutor |
| PUT | `/tutores/{id}` | Atualiza tutor |
| DELETE | `/tutores/{id}` | Remove tutor |

### Animais — `/animais`

| Método | Rota | Descrição |
|---|---|---|
| GET | `/animais` | Lista todos (paginado). Filtros: `?nome=` `?especie=` |
| GET | `/animais/{id}` | Busca por ID |
| POST | `/animais` | Cadastra novo animal |
| PUT | `/animais/{id}` | Atualiza animal |
| DELETE | `/animais/{id}` | Remove animal |

### Clínicas — `/clinicas`

| Método | Rota | Descrição |
|---|---|---|
| GET | `/clinicas` | Lista todas (paginado). Filtros: `?nome=` `?cidade=` |
| GET | `/clinicas/{id}` | Busca por ID |
| POST | `/clinicas` | Cadastra nova clínica |
| PUT | `/clinicas/{id}` | Atualiza clínica |
| DELETE | `/clinicas/{id}` | Remove clínica |

### Veterinários — `/veterinarios`

| Método | Rota | Descrição |
|---|---|---|
| GET | `/veterinarios` | Lista todos (paginado). Filtros: `?nome=` `?especialidade=` |
| GET | `/veterinarios/{id}` | Busca por ID |
| POST | `/veterinarios` | Cadastra novo veterinário |
| PUT | `/veterinarios/{id}` | Atualiza veterinário |
| DELETE | `/veterinarios/{id}` | Remove veterinário |

### Eventos Clínicos — `/eventos-clinicos`

| Método | Rota | Descrição |
|---|---|---|
| GET | `/eventos-clinicos` | Lista todos (paginado). Filtros: `?tipoEvento=` `?animalNome=` |
| GET | `/eventos-clinicos/{id}` | Busca por ID |
| POST | `/eventos-clinicos` | Cadastra novo evento |
| PUT | `/eventos-clinicos/{id}` | Atualiza evento |
| DELETE | `/eventos-clinicos/{id}` | Remove evento |

Valores válidos para `tipoEvento`: `CONSULTA` `RETORNO` `VACINA` `EXAME` `CIRURGIA` `OUTRO`

### Pagamentos — `/pagamentos`

| Método | Rota | Descrição |
|---|---|---|
| GET | `/pagamentos` | Lista todos (paginado). Filtros: `?statusPagamento=` `?formaPagamento=` |
| GET | `/pagamentos/{id}` | Busca por ID |
| POST | `/pagamentos` | Registra novo pagamento |
| PUT | `/pagamentos/{id}` | Atualiza pagamento |
| DELETE | `/pagamentos/{id}` | Remove pagamento |

Valores válidos para `statusPagamento`: `PENDENTE` `PAGO` `CANCELADO` `REEMBOLSADO`

Valores válidos para `formaPagamento`: `PIX` `CARTAO` `DINHEIRO` `BOLETO`

---

## Exemplos de Uso

### Paginação, ordenação e filtros

```
GET /animais?page=0&size=5&sort=nome,asc
GET /tutores?nome=Lucas&page=0&size=10
GET /veterinarios?especialidade=Cardiologia
GET /eventos-clinicos?tipoEvento=VACINA
GET /pagamentos?statusPagamento=PENDENTE
GET /pagamentos?formaPagamento=PIX
```

### Exemplo de POST — Criar Tutor

```json
POST /tutores
{
  "nome": "João Silva",
  "cpf": "12345678989",
  "email": "silva@email.com",
  "telefone": "11999999990",
  "sexo": "MASCULINO",
  "dataNascimento": "1990-01-15",
  "endereco": {
    "logradouro": "Av. Paulista",
    "numero": "1000",
    "bairro": "Bela Vista",
    "cidade": "São Paulo",
    "estado": "SP",
    "cep": "01310100"
  }
}
```

### Exemplo de POST — Criar Animal

```json
POST /animais
{
  "nome": "Thor",
  "raca": "Golden Retriever",
  "especie": "CACHORRO",
  "porte": "GRANDE",
  "cor": "Dourado",
  "sexo": "MACHO",
  "dataNascimento": "2021-03-10",
  "observacao": "Animal saudável",
  "tutorId": "uuid-do-tutor"
}
```

Valores válidos para `sexo` (animal): `MACHO` `FEMEA` `DESCONHECIDO`

### Exemplo de POST — Criar Evento Clínico

```json
POST /eventos-clinicos
{
  "data": "2025-05-20",
  "hora": "10:00",
  "descricao": "Consulta de rotina",
  "tipoEvento": "CONSULTA",
  "veterinarioId": "uuid-do-veterinario",
  "animalId": "uuid-do-animal",
  "clinicaId": "uuid-da-clinica"
}
```

### Exemplo de POST — Criar Pagamento

```json
POST /pagamentos
{
  "formaPagamento": "PIX",
  "valor": 150.00,
  "dataPagamento": "2025-05-20",
  "descricao": "Pagamento consulta de rotina",
  "statusPagamento": "PAGO",
  "eventoClinicoId": "uuid-do-evento"
}
```

---

## Requisitos Técnicos Implementados

| Requisito | Status |
|---|---|
| Bean Validation nos Requests | ✅ todas as 6 entidades |
| Paginação de resultados | ✅ todas as 6 entidades |
| Ordenação de resultados | ✅ todas as 6 entidades |
| Busca com parâmetros | ✅ todas as 6 entidades |
| Cache para otimizar requisições | ✅ todas as 6 entidades |
| Tratamento de erros/exceções | ✅ GlobalExceptionHandler + EntityNotFoundException |
| DTOs (Request/Response) | ✅ todas as 6 entidades |
| Documentação com Swagger | ✅ `/swagger-ui.html` |

---

## Estrutura do Projeto

```
clyvovet-backend-java/
├── documentos/
│   ├── Cronograma_CLYVOVET.pdf         # Cronograma de desenvolvimento
│   ├── DiagramaClasses_CLYVOVET.pdf    # Diagrama de classes UML das entidades
│   └── clyvovet_insomnia.json          # Coleção Insomnia para testes
├── src/
│   └── main/
│       ├── java/br/com/fiap/clyvovet/
│       │   ├── controller/             # Controllers REST (6 entidades)
│       │   ├── service/                # Regras de negócio + cache
│       │   ├── repository/             # JPA Repositories com queries JPQL
│       │   ├── model/                  # Entidades JPA + Enums
│       │   ├── dto/                    # DTOs de Request e Response
│       │   ├── mapper/                 # Conversão Entity ↔ DTO
│       │   ├── exception/              # GlobalExceptionHandler
│       │   └── ClyvovetApplication.java
│       └── resources/
│           ├── db/
│           │   └── db-oracle.sql       # DDL + seed para Oracle FIAP
│           ├── application.properties
│           ├── application-oracle.properties
│           └── application-h2.properties
├── Dockerfile
├── docker-compose.yml
└── deploy.sh                           # Script Azure CLI para deploy em VM Linux
```

---

## Testando os Endpoints

A coleção completa de requisições está disponível na pasta `documentos/` do repositório (exportação do Insomnia).

Você também pode testar diretamente pelo Swagger em:

```
http://localhost:8080/swagger-ui.html
```

---

## Deploy em Nuvem (DevOps)

O projeto inclui suporte completo para containerização e deploy na Azure:

```bash
# Build e execução local com Docker
docker-compose up --build

# Deploy em VM Linux na Azure (Azure CLI necessário)
bash deploy.sh
```

O `deploy.sh` provisiona automaticamente uma VM Linux na Azure, instala Docker e Git, e sobe a aplicação via `docker-compose`.

## Docker Compose

```yaml
services:
  clyvovet-db:
    image: oscarfonts/h2
    container_name: clyvovet-db
    ports:
      - "1521:1521"
      - "81:81"
    environment:
      - H2_OPTIONS=-ifNotExists
    volumes:
      - clyvovet-h2-data:/opt/h2-data
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "-q", "-O", "-", "http://localhost:81"]
      interval: 5s
      timeout: 5s
      retries: 10
      start_period: 20s

  clyvovet-api:
    build: .
    container_name: clyvovet-api
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=h2
    depends_on:
      clyvovet-db:
        condition: service_healthy
    restart: unless-stopped

volumes:
  clyvovet-h2-data:
    name: clyvovet-h2-data
```

## Script Azure CLI — deploy.sh

Script completo para provisionar a infraestrutura na Azure e fazer o deploy automaticamente:

```bash
#!/bin/bash
# ============================================================
# CLYVO VET — Script Azure CLI
# Provisiona VM Linux, instala Docker e Git, sobe a aplicação
# ============================================================

RESOURCE_GROUP="clyvovet-rg"
LOCATION="canadacentral"
VM_NAME="clyvovet-vm"
VM_IMAGE="Ubuntu2204"
VM_SIZE="Standard_B2s_v2"
ADMIN_USER="clyvovet"
DNS_LABEL="clyvovet-api"

az group create --name $RESOURCE_GROUP --location $LOCATION

az vm create \
  --resource-group $RESOURCE_GROUP \
  --name $VM_NAME \
  --image $VM_IMAGE \
  --size $VM_SIZE \
  --admin-username $ADMIN_USER \
  --generate-ssh-keys \
  --public-ip-sku Standard \
  --public-ip-address-dns-name $DNS_LABEL

az vm open-port --resource-group $RESOURCE_GROUP --name $VM_NAME --port 8080 --priority 1001
az vm open-port --resource-group $RESOURCE_GROUP --name $VM_NAME --port 80 --priority 1002

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
```liv