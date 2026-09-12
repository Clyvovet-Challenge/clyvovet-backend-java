# Arquitetura na Azure

> Este documento é o par escrito do desenho. A régua de DevOps pede *"desenho da
> arquitetura com fluxos, recursos e explicação"* (até **20 pontos**), e penaliza
> **−20** um desenho *"parecido com Fluxo, Togaf ou UML"* — então o desenho mostra
> **os recursos que existem na Azure** e **o tráfego entre eles**, não etapas de um
> processo nem classes.

![Arquitetura na Azure](arquitetura-azure.svg)

---

## 1. Os recursos, e por que cada um é o que é

Cinco recursos, todos criados por **Azure CLI** — recurso criado fora da CLI vale
**−30**, e script faltando vale **−10 cada**, então há um script por recurso.

| # | Recurso | Nome | Configuração | Script |
|---|---|---|---|---|
| 1 | Resource Group | `rg-PetTrack-sprint3` | `brazilsouth` | [`azure/01-resource-group.sh`](../azure/01-resource-group.sh) |
| 2 | Azure Database for MySQL Flexible Server | `mysql-PetTrack-rm562312` | `Standard_B1ms`, Burstable, MySQL 8.0, banco `PetTrack` | [`azure/02-banco-mysql.sh`](../azure/02-banco-mysql.sh) |
| 3 | App Service Plan | `plan-PetTrack-sprint3` | **B1** Linux, 1 core, 1,75 GB, 1 instância | [`azure/03-plano-app-service.sh`](../azure/03-plano-app-service.sh) |
| 4 | Web App (Java) | `app-clyvovet-java-rm562312` | runtime `JAVA:17-java17`, nativo | [`azure/04-webapp-java.sh`](../azure/04-webapp-java.sh) |
| 5 | Web App (.NET) | `app-clyvovet-dotnet-rm562312` | runtime `DOTNETCORE:8.0`, nativo | [`azure/05-webapp-dotnet.sh`](../azure/05-webapp-dotnet.sh) |

As app settings das duas entram em [`azure/06-configuracoes.sh`](../azure/06-configuracoes.sh);
o `publish` de cada uma em `07` e `08`; a verificação de ponta a ponta em `09`; e a
destruição em `99`.

### Uma região só

A infraestrutura anterior tinha o grupo em `brazilsouth` e o banco em
`chilecentral`, e **toda consulta atravessava região**. Aqui é uma região para
tudo. Confirmado por CLI antes de decidir, e não presumido:

```bash
az appservice list-locations --linux-workers-enabled --sku B1 -o tsv   # traz Brazil South
az mysql flexible-server list-skus -l brazilsouth -o table            # traz Burstable / Standard_B1ms
```

### B1, e não o F1 gratuito

O F1 existe em `brazilsouth`. O problema dele não é memória, é **não ter Always
On**: o app dorme depois de ~20 minutos ocioso. O feedback das entregas é em 26/09,
então quem abrisse a URL depois disso pegaria um *cold start* de Spring Boot. A
régua trata *"aplicativo não funcional"* e *"depender de intervenção do professor"*
como zero — não é economia, é apostar a nota para não gastar crédito intocado.

B1 é o SKU mais barato **com** Always On. Se 1,75 GB apertarem para as duas APIs,
subir para B2 é um comando e não recria nada:

```bash
az appservice plan update -g rg-PetTrack-sprint3 -n plan-PetTrack-sprint3 --sku B2
```

### Um plano para as duas APIs

O app móvel precisa das duas para funcionar de verdade. Manter uma na nuvem e a
outra local criaria configuração dupla de URL no app — o tipo de coisa que falha
durante a gravação. Como as duas cabem num plano, elas dividem um.

---

## 2. Os fluxos

### ① App → API Java · HTTPS 443, `Authorization: Bearer`

Sessão de usuário de verdade: login em `/api/v1/auth/login`, access token de 15
minutos, refresh de 7 dias. Todo endpoint REST vive sob `/api/v1`, aplicado
centralmente pelo `WebConfig.PREFIXO_API`.

### ② App → API .NET · HTTPS 443, `X-Api-Key`

Autenticação **de aplicação**, não de usuário: uma chave fixa que o app carrega. É
a razão pela qual o cliente `.NET` do app não tem lógica de refresh, e traduz 401
para erro de configuração em vez de sessão expirada — um 401 dali significa chave
errada, nunca token vencido.

### ③ API Java → MySQL · 3306, TLS obrigatório

**É a Java quem cria o schema**, pelo Flyway, no primeiro boot: migrations V1 a V9
em `db/migration/mysql`. O banco é provisionado **vazio** de propósito.

`sslMode=REQUIRED` não é enfeite — o Flexible Server recusa a conexão no
*handshake* sem TLS.

### ④ API .NET → MySQL · 3306, TLS obrigatório

A .NET **lê** `t_clyvo_animal` e `t_clyvo_tutor` para validar FKs e enriquecer
respostas, e **nunca escreve** nelas. É o ADR-001 daquele repositório, e é o que
mantém um banco compartilhado sem conflito de escrita.

Ela **não tem migrations**, por decisão registrada no ADR-002: migrations EF aqui
seriam uma terceira fonte de verdade para o mesmo schema.

### Ordem de subida, e ela é obrigatória

A **Java sobe antes da .NET**. As seis tabelas que a .NET consome nascem do Flyway
no primeiro boot da Java; se a .NET subir primeiro, encontra um banco vazio. No
ambiente local isso está resolvido por `depends_on: service_healthy`; na Azure é
ordem de execução dos scripts — `07-deploy-java.sh` antes de `08-deploy-dotnet.sh`.

---

## 3. Uma instância, autoscale desligado — e por que está escrito

Não é economia. **Cinco componentes guardam estado no processo**, três aqui e dois
na .NET:

| Componente | Repositório | O que quebra com mais de uma instância |
|---|---|---|
| `security/RevogacaoTokenService` | Java | logout numa instância não revoga nas outras — o token segue válido |
| `security/RateLimitFilter` | Java | o limite efetivo multiplica pelo número de réplicas |
| `config/CacheConfig` | Java | `@CacheEvict` limpa só a instância que atendeu |
| `LembreteNotificationService` | .NET | o tutor recebe a mesma notificação uma vez por réplica |
| `TelegramLinkListenerService` | .NET | `getUpdates` concorrentes; o Telegram entrega cada update a um consumidor só |

Com uma instância, nenhum é problema. Ligar autoscale por engano quebra segurança e
produto ao mesmo tempo, e **sem erro no log** — daí o registro explícito. A correção
definitiva é Redis, e ela entra no mesmo dia que o autoscale, nunca antes.

O `06-configuracoes.sh` fixa `--number-of-workers 1` justamente para que ninguém
precise lembrar disso.

---

## 4. Orçamento de conexões

| Origem | Limite | Onde se configura |
|---|---:|---|
| API Java (HikariCP) | 10 | `spring.datasource.hikari.maximum-pool-size` em `application-mysql.properties` |
| API .NET (MySqlConnector) | 15 | app setting `Database__MaxPoolSize` |
| **Total com uma instância de cada** | **25** | |

O teto real de um `Standard_B1ms` depende do tier, e a Azure já mudou esses
números — então o `09-verificar.sh` roda `SHOW VARIABLES LIKE 'max_connections'`,
compara com os 25 e avisa se estiver apertado. O lugar de descobrir isso é a
verificação, não a gravação do vídeo.

---

## 5. Segredos

Nenhum valor sensível vive no código-fonte — a régua desconta **−20** por *"deixar
dados sensíveis expostos (usuário, senha e tokens) no código fonte"*.

Os scripts leem tudo do ambiente e **recusam rodar sem**, em vez de assumir um
padrão que acabaria commitado:

```bash
export MYSQL_PASSWORD='...'      JWT_SECRET='...'
export DOTNET_API_KEY='...'      TELEGRAM_BOT_TOKEN='...'
```

Na Azure eles chegam como **App Settings**, não como arquivo. E nos perfis de banco
real (`mysql`, `oracle`, `h2`) a propriedade `PetTrack.jwt.secret` é `${JWT_SECRET}`
**sem valor padrão**, de propósito: a ausência da variável derruba o boot em vez de
subir com uma chave conhecida.

---

## 5.1 O JWT compartilhado, e como desligá-lo sem redeploy

A API .NET autentica por `X-Api-Key` — uma chave **de aplicação**, não de usuário.
Ela prova que a chamada veio do app, e nada mais: quem extrair a chave do
aplicativo lê os lembretes de qualquer tutor. Fechar isso exige que a .NET saiba
*quem* está chamando, e a única fonte dessa informação é o token que a Java emite.

A mudança entra em **quatro camadas**, cada uma removível sozinha:

| Camada | Onde | Estado | Como se desliga |
|---|---|---|---|
| 0 — claim `tutorId` no access token | Java | ligada | aditiva; ninguém a exige |
| 1 — validar o token e identificar | .NET | **inerte** sem segredo | `Jwt__Secret` ausente |
| 2 — recortar por dono | .NET | **desligada** | `Api__EscopoPorTutor=false` |
| 3 — o app manda o `Bearer` | app | — | a .NET ignora se a 1 estiver inerte |

Os dois interruptores são **app settings**: viram sem recompilar, sem republicar.

```bash
# ligar o recorte
az webapp config appsettings set -g rg-PetTrack-sprint3    -n app-clyvovet-dotnet-rm562312 --settings Api__EscopoPorTutor=true
```

### A ordem de desligar importa, e errar nela é pior que não desligar

```
1º  Api__EscopoPorTutor=false     → volta ao comportamento anterior
2º  só então mexer no Jwt__Secret
```

O caminho inverso — tirar o segredo primeiro, com o escopo ainda ligado — deixa a
API **sem conseguir identificar ninguém enquanto ainda exige identidade**, e o
resultado é 401 em toda rota protegida. É o pior estado possível, e ele só existe
nessa ordem.

### O detalhe que decide se funciona: base64, não UTF-8

As duas APIs recebem **o mesmo valor** e assinam o mesmo token. Mas a chave HMAC
são os bytes do valor **decodificado de base64** — a Java faz
`Keys.hmacShaKeyFor(Decoders.BASE64.decode(segredo))`, e a .NET tem de fazer
`Convert.FromBase64String`.

O idioma padrão de todo tutorial ASP.NET é `Encoding.UTF8.GetBytes(segredo)`, que
com o **mesmo valor de configuração** produz uma **chave diferente**. Não há erro
de compilação, não há aviso, não há nada no log: só 401 em cem por cento das
chamadas. Os dois lados travam esse contrato por teste — `JwtServiceTest` no Java e
`ValidadorDeTokenJwtTests` na .NET asseveram os mesmos bytes.

### A camada 1 não consegue derrubar o boot

Hoje nenhuma configuração da API .NET impede `app.Run()`, e a validação do token
não podia ser a primeira. A chave é montada dentro de `try`: segredo ausente, valor
que não é base64 ou chave curta demais deixam o validador **inerte**, com log de
erro — nunca exceção. Com o recorte desligado, um segredo errado não tem
consequência nenhuma; uma aplicação que não sobe tem.

Pelo mesmo motivo `Jwt:Secret` **não existe** no `appsettings.json`. Este projeto
versiona placeholder para todo segredo (`"SUA_API_KEY"`), e um placeholder aqui
faria a camada deixar de ser inerte por padrão — destruindo o interruptor.

---

## 6. O que **não** está no desenho, e é deliberado

| Ausente | Por quê |
|---|---|
| Container de aplicação | **−40**. O `Dockerfile` continua no repositório para desenvolvimento local, mas o artefato publicado é o `.jar` |
| Container de banco | **−40** |
| H2 | **−40** (banco não permitido). Só existe no perfil de teste |
| Máquina virtual | A Opção 2 é PaaS. O `deploy.sh` antigo provisionava VM com Docker Compose e H2 — três penalidades de −40 no mesmo arquivo, e foi removido |
| Pipeline de CI/CD | É requisito da **Sprint 4**, não desta |
| Azure Key Vault | Não é exigido, e App Settings já mantém o segredo fora do código. Entra junto com o JWT compartilhado, se houver tempo |
| Autoscale / múltiplas instâncias | Seção 3 |
| Application Gateway, Front Door, Redis | Não há requisito que os justifique, e cada recurso a mais é um recurso a explicar na avaliação oral |

---

## 7. Onde está o resto

| Assunto | Documento |
|---|---|
| Passo a passo do deploy | [`README.md`](../README.md), seção *Deploy na Azure* |
| Decisões de deploy e o porquê de cada uma | [`docs/05-deploy.md`](05-deploy.md) |
| Plano da entrega, régua e cronograma | [`docs/12-plano-de-entrega-sprint3.md`](12-plano-de-entrega-sprint3.md) |
| Recorte da API .NET | `ClyvoVet-api/docs/plano-de-entrega-sprint3.md` |
