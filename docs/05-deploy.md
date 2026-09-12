# Deploy

Três formas de executar, da mais simples à mais completa:

| Modo | Comando | Banco | Onde roda |
|---|---|---|---|
| Local | `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` | H2 em memória | máquina do dev |
| Container | `docker compose up --build` | H2 em container | Docker local |
| Integração | `docker compose up` em `local/` (repo do app) | MySQL 8 em container | Docker local, com as duas APIs |
| **Nuvem** | `bash azure/01..08` | **MySQL Flexible Server (PaaS)** | **App Service Linux** |

> O passo a passo da nuvem vive no [README da raiz](../README.md#deploy-na-azure--passo-a-passo),
> e não aqui, por um motivo concreto: o vídeo de entrega da disciplina de DevOps
> exige o deploy *"seguindo exatamente os passos descritos no README.md"*. Ter dois
> roteiros é ter um deles errado. **Este documento explica o porquê; o README diz
> como.**

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
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Decisões relevantes:

| Decisão | Por quê |
|---|---|
| Multi-stage | A imagem final leva só o JRE e o JAR — sem Maven, sem código-fonte |
| `dependency:go-offline` antes de copiar `src` | Alterar código não invalida a camada de dependências; rebuild fica rápido |
| Testes rodam no build | Usam H2 em memória (perfil `dev` fixado em `src/test/resources`), sem depender do Oracle — um build que passa é um JAR testado |
| `eclipse-temurin:17-jre-jammy` | JRE, não JDK — imagem menor |
| Usuário `appuser` sem privilégios | O processo não roda como root dentro do container |
| Perfil fixo no `ENTRYPOINT` | A imagem é sempre `h2`; o Oracle da FIAP não é acessível da nuvem |

### .dockerignore

[`.dockerignore`](../.dockerignore) — antes de copiar qualquer coisa, o Docker
envia o diretório inteiro ao daemon como *build context*. Sem esse arquivo eram
89 MB a cada `docker compose up --build` — 72 MB de `target/`, 4,7 MB de
`documentos/` — para o Dockerfile usar menos de 1 MB.

Nada disso chegava à imagem: o Dockerfile copia só `pom.xml` e `src/`. O
desperdício estava no envio, não no resultado.

### docker-compose.yml

[`docker-compose.yml`](../docker-compose.yml) — dois serviços:

| Serviço | Imagem | Portas | Função |
|---|---|---|---|
| `PetTrack-db` | `oscarfonts/h2` | `1521:1521`, `81:81` | H2 em modo servidor TCP + console web |
| `PetTrack-api` | build local | `8080:8080` | a aplicação |

```yaml
services:
  PetTrack-db:
    image: oscarfonts/h2
    container_name: PetTrack-db
    ports: ["1521:1521", "81:81"]
    environment:
      - H2_OPTIONS=-ifNotExists
    volumes:
      - PetTrack-h2-data:/opt/h2-data
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "-q", "-O", "-", "http://localhost:81"]
      interval: 5s
      timeout: 5s
      retries: 10
      start_period: 20s

  PetTrack-api:
    build: .
    container_name: PetTrack-api
    ports: ["8080:8080"]
    environment:
      - SPRING_PROFILES_ACTIVE=h2
    depends_on:
      PetTrack-db:
        condition: service_healthy
    restart: unless-stopped

volumes:
  PetTrack-h2-data:
    name: PetTrack-h2-data
```

Pontos-chave:

- **`H2_OPTIONS=-ifNotExists`** — permite que o H2 crie o banco `PetTrack` na primeira
  conexão. Sem isso, a API falharia ao conectar num banco inexistente.
- **`depends_on` com `condition: service_healthy`** — a API só sobe depois que o
  healthcheck do H2 passa. Evita a corrida clássica de "app sobe antes do banco".
- **Volume nomeado** — os dados sobrevivem a `docker compose down`. Para zerar de
  fato: `docker compose down -v`.
- A API resolve o banco pelo nome do serviço (`PetTrack-db`), que a rede default do
  compose expõe como hostname.

### Comandos

```bash
docker compose up --build          # build + sobe tudo
docker compose up -d --build       # em background
docker compose logs -f PetTrack-api  # acompanha logs da API
docker compose down                # para, mantendo os dados
docker compose down -v             # para e apaga o volume
```

Depois de subir:

| Recurso | URL |
|---|---|
| API | http://localhost:8080/api/v1 |
| Swagger | http://localhost:8080/swagger-ui.html |
| Console H2 do container `PetTrack-db` | http://localhost:81 |
| Console H2 embarcado na API | http://localhost:8080/h2-console |

Para conectar no console H2, use a JDBC URL `jdbc:h2:tcp://PetTrack-db:1521/PetTrack`,
usuário `sa`, senha vazia.

---
## Azure — App Service com banco PaaS

### Por que não é mais uma VM

Havia aqui um `deploy.sh` que provisionava VM Ubuntu, instalava Docker e subia o
compose com H2. Ele foi removido, e o motivo é a régua da disciplina, não gosto:

| O que aquele desenho fazia | Penalidade |
|---|---|
| App rodando em container | **−40** |
| Banco em container | **−40** |
| H2 na nuvem — banco não permitido | **−40** |

A disciplina exige escolher **uma** opção e não misturar. A escolhida é a **Opção 2
— Serviço de Aplicativo com banco PaaS**, onde *"nada pode ser containerizado"*.

### Os recursos

| # | Recurso | Script | Detalhe |
|---|---|---|---|
| 1 | Resource Group | `01-resource-group.sh` | região `brazilsouth` |
| 2 | MySQL Flexible Server | `02-banco-mysql.sh` | `Standard_B1ms`, Burstable, TLS obrigatório, nasce **vazio** |
| 3 | App Service Plan | `03-plano-app-service.sh` | **B1** Linux, uma instância — o SKU mais barato **com** Always On |
| 4 | Web App Java | `04-webapp-java.sh` | runtime `JAVA:17-java17` |
| 5 | Web App .NET | `05-webapp-dotnet.sh` | runtime `DOTNETCORE:8.0`, mesmo plano |
| — | App settings das duas | `06-configuracoes.sh` | nenhum segredo no arquivo |

Um script por recurso não é organização — é a régua: *"Entregue todos os scripts dos
recursos criados na Azure"*, com **−10 por script faltando** e **−30** se algum
recurso não vier da CLI.

### O Dockerfile continua aqui, e isso não é contradição

O que a disciplina proíbe é o **artefato publicado** ser um container. O `Dockerfile`
segue servindo ao desenvolvimento local, ao `docker-compose` de integração e ao CI.
O que vai para a Azure é o **jar** do `mvnw package`, entregue por
`az webapp deploy --type jar` ao runtime Java nativo do App Service.

Pelo mesmo motivo, o estágio `Imagem` do `azure-pipelines.yml` foi removido: um
pipeline que publica imagem é o caminho mais curto para o artefato errado subir.

### Dois detalhes de plataforma que decidem se a aplicação sobe

**`SERVER_PORT=80` na Java.** O App Service Linux encaminha para a porta 80 dentro do
container, e o Spring Boot sobe em 8080. Sem essa app setting a aplicação inicia
normalmente, o health check externo nunca responde, e o App Service reinicia em loop
— **sem mensagem óbvia no log**. Funciona porque o *relaxed binding* do Spring mapeia
a variável `SERVER_PORT` para a propriedade `server.port`, sem tocar em nenhum
`.properties`.

A API .NET não precisa do equivalente: a imagem `DOTNETCORE:8.0` resolve o
`ASPNETCORE_URLS` sozinha.

**TLS obrigatório.** O Flexible Server recusa conexão sem TLS. Daí `sslMode=REQUIRED`
na URL JDBC e `SslMode=Required` na connection string da .NET.

### Ordem de subida

A **Java sobe primeiro**, e isso é dependência, não preferência: o Flyway dela cria
as 19 tabelas, incluindo as seis `t_clyvo_*` que a API .NET consome. Se a .NET subir
antes, ela **sobe normalmente** — o EF Core não valida schema no boot — e falha só na
primeira consulta, dizendo que a tabela não existe.

No ambiente local isso está resolvido por `depends_on: service_healthy`. Na Azure, é
a ordem dos scripts 07 e 08.

### Uma instância, autoscale desligado

Cinco componentes das duas APIs guardam estado no processo: revogação de token, rate
limit e cache aqui; os dois `BackgroundService` na .NET. Com uma instância, nenhum é
problema. Com duas, o logout deixa de funcionar e a notificação duplica — sem erro no
log. O `06-configuracoes.sh` fixa `--number-of-workers 1` por isso.

Ver [12-plano-de-entrega-sprint3.md](12-plano-de-entrega-sprint3.md), seção 7.

### Verificação e encerramento

`09-verificar.sh` exercita o caminho inteiro — saúde, cadastro, login, CRUD de animal
e o fluxo cruzado com a .NET — e imprime o SQL da demonstração de CRUD no banco. Sai
com código diferente de zero se algo falhou.

`99-destruir.sh` apaga o Resource Group inteiro, e só deve ser rodado **depois da
correção**: recurso apagado equivale a entrega em localhost, que é zero de nota.
