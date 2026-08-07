# Deploy

Três formas de executar, da mais simples à mais completa:

| Modo | Comando | Banco | Onde roda |
|---|---|---|---|
| Local | `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` | H2 em memória | máquina do dev |
| Container | `docker compose up --build` | H2 em container | Docker local |
| Nuvem | `bash deploy.sh` | H2 em container | VM Linux na Azure |

---

## Docker

### Dockerfile

[`Dockerfile`](../Dockerfile) — build multi-stage em duas etapas:

```dockerfile
# Etapa 1: build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B       # camada de cache das dependências
COPY src ./src
RUN mvn clean package -DskipTests -B

# Etapa 2: imagem final
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=h2", "-jar", "app.jar"]
```

Decisões relevantes:

| Decisão | Por quê |
|---|---|
| Multi-stage | A imagem final leva só o JRE e o JAR — sem Maven, sem código-fonte |
| `dependency:go-offline` antes de copiar `src` | Alterar código não invalida a camada de dependências; rebuild fica rápido |
| `-DskipTests` | O único teste é `@SpringBootTest`, que precisaria de banco disponível durante o build |
| `eclipse-temurin:17-jre-jammy` | JRE, não JDK — imagem menor |
| Usuário `appuser` sem privilégios | O processo não roda como root dentro do container |
| Perfil fixo no `ENTRYPOINT` | A imagem é sempre `h2`; o Oracle da FIAP não é acessível da nuvem |

### docker-compose.yml

[`docker-compose.yml`](../docker-compose.yml) — dois serviços:

| Serviço | Imagem | Portas | Função |
|---|---|---|---|
| `clyvovet-db` | `oscarfonts/h2` | `1521:1521`, `81:81` | H2 em modo servidor TCP + console web |
| `clyvovet-api` | build local | `8080:8080` | a aplicação |

```yaml
services:
  clyvovet-db:
    image: oscarfonts/h2
    container_name: clyvovet-db
    ports: ["1521:1521", "81:81"]
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
    ports: ["8080:8080"]
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

Pontos-chave:

- **`H2_OPTIONS=-ifNotExists`** — permite que o H2 crie o banco `clyvovet` na primeira
  conexão. Sem isso, a API falharia ao conectar num banco inexistente.
- **`depends_on` com `condition: service_healthy`** — a API só sobe depois que o
  healthcheck do H2 passa. Evita a corrida clássica de "app sobe antes do banco".
- **Volume nomeado** — os dados sobrevivem a `docker compose down`. Para zerar de
  fato: `docker compose down -v`.
- A API resolve o banco pelo nome do serviço (`clyvovet-db`), que a rede default do
  compose expõe como hostname.

### Comandos

```bash
docker compose up --build          # build + sobe tudo
docker compose up -d --build       # em background
docker compose logs -f clyvovet-api  # acompanha logs da API
docker compose down                # para, mantendo os dados
docker compose down -v             # para e apaga o volume
```

Depois de subir:

| Recurso | URL |
|---|---|
| API | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |
| Console H2 do container `clyvovet-db` | http://localhost:81 |
| Console H2 embarcado na API | http://localhost:8080/h2-console |

Para conectar no console H2, use a JDBC URL `jdbc:h2:tcp://clyvovet-db:1521/clyvovet`,
usuário `sa`, senha vazia.

---

## Azure

[`deploy.sh`](../deploy.sh) provisiona uma VM Linux do zero e sobe a aplicação nela.

### Parâmetros

| Variável | Valor |
|---|---|
| `RESOURCE_GROUP` | `clyvovet-rg` |
| `LOCATION` | `canadacentral` |
| `VM_NAME` | `clyvovet-vm` |
| `VM_IMAGE` | `Ubuntu2204` |
| `VM_SIZE` | `Standard_B2s_v2` |
| `ADMIN_USER` | `clyvovet` |
| `DNS_LABEL` | `clyvovet-api` |

### Etapas do script

| # | Comando | O que faz |
|---|---|---|
| 1 | `az group create` | Cria o resource group em `canadacentral` |
| 2 | `az vm create` | Provisiona a VM Ubuntu 22.04 com IP público Standard, DNS label e chave SSH gerada |
| 3 | `az vm open-port --port 8080` | Libera a porta da API (prioridade 1001) |
| 4 | `az vm open-port --port 80` | Libera HTTP (prioridade 1002) |
| 5 | `az vm run-command invoke` | Instala `git`, `curl`, `nano` e Docker; habilita o serviço; adiciona o usuário ao grupo `docker` |
| 6 | `az vm run-command invoke` | Clona o repositório e roda `docker compose up -d --build` |

### Execução

```bash
az login
bash deploy.sh
```

Ao final, a aplicação fica em:

```
http://clyvovet-api.canadacentral.cloudapp.azure.com:8080
http://clyvovet-api.canadacentral.cloudapp.azure.com:8080/swagger-ui.html
```

### Operação depois do deploy

```bash
# Acessar a VM
ssh clyvovet@clyvovet-api.canadacentral.cloudapp.azure.com

# Dentro da VM
cd ~/clyvovet-backend-java
docker compose ps
docker compose logs -f clyvovet-api

# Atualizar para a última versão do código
git pull && docker compose up -d --build

# Destruir tudo (da sua máquina)
az group delete --name clyvovet-rg --yes --no-wait
```

### Observações

- O script **clona o repositório público do GitHub**, não envia o código local. Um
  `git push` é pré-requisito para que o deploy reflita suas mudanças.
- O deploy é sempre no perfil `h2` — não há conectividade com o Oracle da FIAP a
  partir da Azure.
- Os dados vivem no volume Docker dentro da VM. Destruir o resource group apaga tudo.
- A porta 80 é aberta mas nada escuta nela: não há reverse proxy configurado. A API
  responde apenas na 8080.
- Não há HTTPS, autenticação nem restrição de IP. Adequado para demonstração
  acadêmica, não para dados reais.
- `--generate-ssh-keys` reaproveita `~/.ssh/id_rsa` se já existir, ou cria um novo par.
