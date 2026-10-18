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
| API | http://localhost:8080/api/v1 |
| Swagger | http://localhost:8080/swagger-ui.html |
| Console H2 do container `clyvovet-db` | http://localhost:81 |
| Console H2 embarcado na API | http://localhost:8080/h2-console |

Para conectar no console H2, use a JDBC URL `jdbc:h2:tcp://clyvovet-db:1521/clyvovet`,
usuário `sa`, senha vazia.

---
## Render + Aiven — onde o Java roda na Sprint 4

O edital de Java Advanced da Sprint 4 pede só que a aplicação esteja *rodando online*. Ela
roda na **Render** (container) com banco **MySQL no Aiven**, fora da Azure, que ficou para a
DevOps. O porquê, e o que foi descartado, está no
[ADR 008](adr/008-java-na-render-com-mysql-no-aiven.md).

**Endereço:** https://clyvovet-backend-java.onrender.com — entra direto na tela de login.

| O quê | Valor |
|---|---|
| Serviço | Web Service Docker, plano gratuito, região Ohio |
| Código | repositório `clyvovet-backend-java`, branch `sprint-4`, usando o `Dockerfile` da raiz |
| Deploy | manual (auto-deploy desligado) |
| Banco | Aiven MySQL 8.4, plano Developer (período de teste) |
| Perfil Spring | `mysql`, o padrão do `application.properties` |

### Variáveis de ambiente

Cadastradas no painel da Render, aba *Environment*. Só os **nomes** ficam no repositório.

| Variável | O que é |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `mysql` |
| `DB_URL` | `jdbc:mysql://<host>:<porta>/<banco>?sslMode=REQUIRED` — host, porta e banco vêm da página *Overview* do serviço no Aiven. O Aiven recusa conexão sem TLS |
| `DB_USERNAME` | usuário do Aiven (`avnadmin` por padrão) |
| `DB_PASSWORD` | senha do Aiven. **Segredo: só no painel da Render** |
| `JWT_SECRET` | texto aleatório longo, gerado para este deploy (`openssl rand -base64 48`). **Segredo** |
| `GOOGLE_CLIENT_ID` | ID do cliente OAuth criado no Google Cloud Console (termina em `.apps.googleusercontent.com`) |
| `GOOGLE_CLIENT_SECRET` | segredo desse cliente (começa com `GOCSPX-`). **Segredo** |
| `GITHUB_CLIENT_ID` | ID do OAuth App do GitHub (começa com `Ov23`, 20 caracteres) |
| `GITHUB_CLIENT_SECRET` | segredo desse app. **Segredo** |
| `CLYVOVET_DEMO_SENHA` | senha dos 4 usuários de demonstração (mínimo 12 caracteres). **Segredo**, e opcional: sem ela nenhum usuário é criado |

As quatro últimas ligam o login social. Sem elas o app sobe normal, mas os botões "Entrar com
Google" e "Entrar com GitHub" mandam o `client_id=nao-configurado` ao provedor e falham lá.
Variável de ambiente só vale depois de um **novo deploy**: gravar com *Save only* não reinicia o app.

**Usuários de demonstração.** O banco do Aiven nasce com clínicas, veterinários e tutores, mas sem
nenhum usuário (o `DevDataSeeder` não roda no perfil `mysql`, de propósito). Com
`CLYVOVET_DEMO_SENHA` definida, o `UsuariosDeDemonstracaoSeeder` cria um usuário por perfil, todos
com essa mesma senha, ligados a linhas que a V2 já semeia:

| E-mail | Perfil | Ligado a |
|---|---|---|
| `demo.admin@clyvovet.com` | ADMIN | (plataforma) |
| `demo.gestor@clyvovet.com` | ADMIN_CLINICA | clínica VetCare |
| `demo.veterinaria@clyvovet.com` | VETERINARIO | veterinária Camila |
| `demo.tutor@clyvovet.com` | TUTOR | tutor Lucas, que já tem pets |

A senha **não está no repositório**: ela entra só no painel da Render e é entregue ao professor por
fora. É idempotente (reiniciar não duplica nem troca a senha) e recusa senha com menos de 12
caracteres. Terminada a avaliação, remova a variável; os usuários já criados continuam, e para
apagá-los é preciso removê-los no banco. Variável nova só vale depois de um **novo deploy**.
Coberto por `UsuariosDeDemonstracaoSeederTest`; a leitura da variável foi provada subindo o app local
com ela e logando com os quatro perfis. **Ainda não foi feito na Render.**

Sem `JWT_SECRET` o app cai no boot, em `Could not resolve placeholder 'JWT_SECRET'`. Foi o
erro da primeira tentativa de deploy, e mostrou que o build e os testes já passavam.

### Passo a passo

1. No Aiven, crie um serviço **MySQL** e copie host, porta, banco e usuário da *Overview*.
2. Na Render, crie um **Web Service** a partir do repositório, ambiente **Docker**, branch
   `sprint-4`, plano gratuito.
3. Cadastre as variáveis acima e dispare o deploy. As do login social dependem dos passos da
   seção seguinte.
4. O build roda `mvn package` **com os testes** dentro do Docker e leva alguns minutos.
5. No primeiro boot o Flyway cria as 21 migrations no banco vazio. A Render detecta a porta
   8080 e **reinicia o deploy uma vez**: é normal no primeiro deploy.

### O que foi conferido em 18/10/2026

- O Flyway conectou no MySQL 8.4 do Aiven e validou as 21 migrations (schema na versão 21).
- O app subiu com `ddl-auto=validate`, o que prova que as tabelas batem com as entidades.
- `GET /actuator/health` respondeu `UP` e `/` redirecionou para `/login` com HTTP 200.

Conferido depois, no app online, ainda em 18/10/2026:

- Cadastro de tutor (`POST /api/v1/auth/registrar`, 201) e login com e-mail e senha: gravam e
  leem no Aiven.
- `GET /api/v1/racas` com token devolveu 200 e 44 raças das cinco espécies. A migração V14 tem
  45 linhas de inserção; a diferença de uma não foi explicada (a API só lista raças ativas).
- Login com GitHub e com Google: o redirecionamento sai com o `client_id` real e a
  `redirect_uri` da Render, e o login com uma conta de tutor já cadastrada entrou no sistema.

**Não foi conferido:** perfis de veterinário e de administrador, as telas de agendamento com
dados, o refresh e o logout do token, e o limite de tentativas atrás do proxy da Render.

### Login social (Google e GitHub)

O app só aceita e-mail que já exista como usuário ([ADR 002](adr/002-telas-thymeleaf-no-spring.md)).
Sem conta com aquele e-mail, o login volta para `/login` com "Nenhuma conta usa este e-mail".

1. **GitHub:** *Settings → Developer settings → OAuth Apps → New OAuth App*. Homepage
   `https://clyvovet-backend-java.onrender.com` e URL de retorno
   `https://clyvovet-backend-java.onrender.com/login/oauth2/code/github`. O GitHub mostra o
   segredo **uma vez só**.
2. **Google:** no Google Cloud Console, crie um projeto, abra *Google Auth Platform*, monte a
   tela de consentimento (público **Externo**) e crie um cliente do tipo *Aplicativo da Web* com
   a URL de retorno `https://clyvovet-backend-java.onrender.com/login/oauth2/code/google`.
   Enquanto o app estiver em modo **Teste**, só entra quem estiver em *Público-alvo → Usuários
   de teste*, e o Google mostra o aviso de "app não verificado". A configuração pode levar de
   cinco minutos a algumas horas para valer.
3. Cadastre as quatro variáveis na Render e faça **Save, rebuild, and deploy**.
4. Crie um tutor com o e-mail da conta social (`POST /api/v1/auth/registrar`, pelo Swagger).

Cuidado com a troca entre ID e segredo: o ID vai na URL pública do botão; se o segredo for
colado no campo do ID, ele aparece para qualquer pessoa. Se isso ocorrer, gere um segredo novo
e apague o antigo no provedor.

### Limites que quem for avaliar precisa saber

- **Subida a frio.** No plano gratuito a Render desliga o app depois de uns 15 minutos sem
  uso, e a primeira requisição depois disso espera o Spring subir. A Render avisa "50
  segundos ou mais"; a subida do Spring levou 256 s na primeira medição.
- **Banco com prazo.** O período de teste do Aiven é de **30 dias a partir de 18/10/2026**.
  Depois dele o serviço é desligado, a menos que se cadastre um cartão (US$ 5 por mês). O
  plano Free do Aiven não vence, mas desliga por inatividade e só o dono da conta religa.

### Rodar sem nuvem

O perfil `dev` sobe um H2 em memória, sem configurar nada
(`./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`). É o caminho para quem não alcançar o
endereço acima.

---

## Azure — App Service com banco PaaS

> ℹ️ Este capítulo descreve o deploy da **Sprint 3** (disciplina de DevOps). Na Sprint 4 o Java
> roda na Render, como descrito acima; o App Service, o plano B1 e os scripts de `azure/`
> continuam documentados porque a API .NET e a DevOps ainda os usam.

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
as 24 tabelas, incluindo as seis `t_clyvo_*` que a API .NET consome. Se a .NET subir
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
