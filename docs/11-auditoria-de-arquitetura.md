# Auditoria de arquitetura — o que este repositório precisa corrigir

> Levantado em **06/09/2026** sobre o código, migrations, Dockerfile e pipelines —
> não sobre documentação. Relatório completo dos dois backends, com comparação de
> arquiteturas e matriz de riscos:
> [claude.ai/code/artifact/79da68b6-2d7d-41b1-82c2-aac5fc68652a](https://claude.ai/code/artifact/79da68b6-2d7d-41b1-82c2-aac5fc68652a)
>
> Este arquivo é o recorte do que **esta API** é dona. O recorte da API .NET está
> em `ClyvoVet-api/docs/auditoria-de-arquitetura.md`; o do app, em
> `2tdspw-challenge-clyvovet-challenge/spec/12-auditoria-de-arquitetura.md`.

---

## 1. A decisão de arquitetura

**Banco compartilhado entre as duas APIs, com esta aqui dona do schema.**

A decisão não foi tomada por conveniência de prazo. Foi tomada porque o código já
satisfaz a condição que torna banco compartilhado seguro:

> **Cada tabela tem exatamente um escritor.**
> A API .NET lê `t_clyvo_animal` e `t_clyvo_tutor` e nunca escreve nelas — verificado em
> `AnimalRepository.cs`, que expõe apenas `GetByIdAsync` e `GetByTutorIdAsync`, e
> por busca em todo o projeto dela por escrita nesses `DbSet`.

A objeção clássica ao banco compartilhado é disputa de escrita entre serviços.
Aqui ela não existe. As alternativas avaliadas — a .NET consumir esta API por
HTTP, ou bancos separados com mensageria — resolvem um problema que este sistema
não tem, e cobram por isso em componentes que o time teria de operar.

### O que esta API é dona

| | |
|---|---|
| **Dados** | `t_clyvo_usuario`, `t_clyvo_tutor`, `t_clyvo_animal`, `t_clyvo_clinica`, `t_clyvo_veterinario`, `t_clyvo_servico`, `t_clyvo_evento_clinico`, `t_clyvo_pagamento`, `t_clyvo_disponibilidade_vet`, `t_clyvo_bloqueio`, `t_clyvo_alerta_clinico`, `t_clyvo_autorizacao_acesso`, `t_clyvo_acesso_historico` |
| **Schema** | **Inteiro**, incluindo as seis tabelas `t_clyvo_*` que só a .NET consome (V8). O Flyway daqui é a fonte única |
| **Identidade** | Emissão e validação de JWT. A .NET não tem noção de usuário hoje |

### O que não muda

Nenhuma tabela troca de dono. A .NET continua dona do **conteúdo** das
`t_clyvo_*` — só a **definição** delas passou a viver aqui, para que o
provisionamento tenha um caminho só.

---

## 2. Achados desta API

Ordenados por gravidade. Cada um cita o arquivo que justifica a conclusão.

### 2.1 🔴 Estado de sessão em memória local

Três componentes guardam estado no processo:

| Componente | O que guarda | O que quebra com mais de uma instância |
|---|---|---|
| `security/RevogacaoTokenService.java` | deny-list de `jti` revogado | logout numa instância **não** revoga nas demais — o token segue válido em 2 de 3 |
| `security/RateLimitFilter.java` | buckets do bucket4j | o limite efetivo multiplica pelo número de réplicas |
| `config/CacheConfig.java` | caches `pagamentos`, `eventos`, `tutores`, `animais`, `clinicas`, `veterinarios` | `@CacheEvict` limpa só a instância que atendeu; as outras servem dado velho |

**Isto já estava documentado no próprio código.** O comentário de
`RevogacaoTokenService` diz, textualmente, que o estado é local ao processo e que
a correção seria um cache compartilhado (Redis). A auditoria confirma o
diagnóstico, não o descobre.

**Correção:** Redis para os três. **Não é necessário para a entrega** — uma
instância de App Service elimina os três de uma vez, e é a decisão certa para o
prazo. Vira obrigatório no dia em que houver autoscaling.

### 2.2 🔴 Não existe script de deploy para esta API

O repositório da .NET tem `azure/00-04.sh` provisionando Resource Group, MySQL,
App Service Plan e um Web App `DOTNETCORE:8.0`. **Não há equivalente aqui.**

Consequência: hoje só a .NET vai ao ar. O app móvel depende das duas — a .NET
cobre apenas lembretes; autenticação, pets, agenda e histórico são desta API.

O `deploy.sh` que existe provisiona VM Ubuntu com Docker Compose e H2, que é o
desenho antigo e não serve: a opção escolhida é App Service + banco PaaS.

**Correção:** script `az` cobrindo `appservice plan create --is-linux`,
`webapp create --runtime JAVA:17-java17`, `webapp config appsettings set` e
`webapp deploy --type jar`. O plano já existe e é compartilhável com a .NET.

### 2.3 🟡 Connection pool no padrão

Nenhuma propriedade `spring.datasource.hikari.*` nos `.properties`. Vale o padrão
do HikariCP: **10 conexões por instância**.

O problema não é esse número, é o desequilíbrio: a API .NET opera no padrão do
MySqlConnector, **100 por instância** — dez vezes mais, sendo a API com menos
endpoints e menos tráfego. Contra um `Standard_B1ms` são 110 conexões potenciais
com uma instância de cada.

**✅ Corrigido, mas com número diferente do sugerido acima: 10, não 15.**

O lado .NET foi fechado primeiro, em 15 (`Database__MaxPoolSize`, via
`MySqlConnectionStringBuilder`). Com isso o desequilíbrio de 10 contra 100 deixou de
existir — e o argumento para subir a Java caiu junto com ele.

O que restou decidir foi o número, e aí manda o hardware: o App Service é **B1, um
core**, compartilhado com a API .NET. Conexão ociosa não processa requisição. Num
core só, passar de 10 gasta memória da JVM (que roda com `-Xmx512m`) para engordar
uma fila que a CPU não drena mais rápido. O total fica em **10 + 15 = 25** conexões
potenciais com uma instância de cada.

O pool agora está **declarado**, e não herdado. O padrão do HikariCP é 10 — por
acaso o número certo aqui —, mas "por acaso o padrão serve" e "escolhemos 10" são
coisas diferentes na hora de justificar, e a segunda é a que sobrevive a uma troca
de versão. Junto entraram `minimum-idle=2` (o padrão do Hikari é não encolher
nunca, o que num plano compartilhado é o comportamento errado), `max-lifetime` e
`idle-timeout` abaixo do `wait_timeout` do servidor (quem fecha a conexão tem de
ser o pool, não o gateway do MySQL — se o servidor derruba primeiro, o Hikari só
descobre ao tentar usar, e o sintoma é erro de rede numa requisição de usuário) e
`connection-timeout=10000`.

**O teto continua sendo medido, não presumido** — e agora automaticamente. O
`azure/09-verificar.sh` roda `SHOW VARIABLES LIKE 'max_connections'`, compara com o
orçamento de 25 e avisa se estiver apertado. O lugar de descobrir isso é a
verificação, não a gravação do vídeo.

> **Não coberto por teste:** essas propriedades vivem no perfil `mysql`, e a suíte
> roda em `dev`/H2. Elas só valem quando o perfil `mysql` sobe de verdade — ou
> seja, no deploy. O `09-verificar.sh` é a rede aqui.

### 2.4 🟡 Sem correlation ID e sem tracing

Não há `MDC`, header de correlação, nem OpenTelemetry. O actuator expõe só
`health` — decisão de segurança correta, registrada em `comum.properties`, com o
custo de não haver métricas.

Rastrear uma requisição nesta API, que tem **74 endpoints**, depende hoje de
timestamp. A API .NET tem `CorrelationIdMiddleware` com `X-Correlation-Id` e
OpenTelemetry — a assimetria é grande.

**Correção:** filtro que aceita ou gera `X-Correlation-Id`, coloca no `MDC` e
devolve no header. O app propaga o mesmo id para as duas APIs, e aí passa a
existir rastro ponta a ponta mesmo sem as APIs se chamarem.

### 2.5 🟡 Nenhum controle de concorrência

Não existe `@Version`, `@Lock` ou `LockModeType` em nenhuma entidade. Nível de
isolamento não configurado — vale o padrão do MySQL (`REPEATABLE READ`).

Cenário concreto: dois `PATCH /animais/{id}` concorrentes se sobrescrevem em
silêncio. O ciclo do JPA é ler-modificar-gravar; o segundo commit vence e a
alteração do primeiro se perde **sem erro**.

**Correção:** `@Version` em `Animal`, `Tutor` e `EventoClinico` — as entidades que
o app edita. O Hibernate passa a lançar `OptimisticLockException`, que vira 409.
Exige migration nova (uma coluna por tabela).

### 2.6 🟢 Race condition conhecida em `marcarFaltas()`

`service/RetornoService.java:137` lê os agendamentos vencidos e grava todos como
`FALTOU`. Duas chamadas concorrentes processam o mesmo conjunto.

É **idempotente no resultado** — gravar `FALTOU` duas vezes dá `FALTOU` —, então
o impacto é escrita e log duplicados, não corrupção.

Vale o registro de que o comentário ali documenta a decisão de ser endpoint e não
`@Scheduled`, *"porque agendador em aplicação com mais de uma instância dispara em
todas ao mesmo tempo"*. **Não há nenhum `@Scheduled` no projeto.** É maturidade
real: o problema foi antecipado.

### 2.7 🟢 Estágio Docker no pipeline conflita com a opção de entrega

O `azure-pipelines.yml` tem um estágio `Imagem` que constrói imagem Docker. Se a
entrega de DevOps for App Service + banco PaaS, o artefato publicado não pode sair
dele.

**Correção:** remover ou desabilitar o estágio. O `Dockerfile` continua no
repositório servindo ao desenvolvimento e ao CI.

### 2.8 🟡 O DDL da API .NET é cópia manual do schema daqui

`ClyvoVet-api/schema/script_bd.sql` é o entregável de DDL da disciplina de DevOps
do outro repositório, e a PARTE 1 dele é **cópia à mão** das migrations deste. Já
defasou duas vezes: primeiro nas sete colunas booleanas que viraram `INT`, depois
nas treze tabelas que ganharam prefixo na V9.

O custo é assimétrico e cai deste lado. Aquela API não valida schema e sobe contra
um banco errado sem reclamar; **esta roda com `ddl-auto=validate` e não sobe**. Um
erro naquele arquivo não aparece lá — aparece aqui, no deploy.

**Correção durável:** o `scripts/gerar-script-bd.py` já gera
`documentos/script_bd.sql` a partir de `db/migration/oracle/`. Falta a variante
MySQL, gerada de `db/migration/mysql/`, para o outro repositório consumir em vez
de copiar.

Duas coisas a resolver antes de trocar o arquivo de lá:

1. O script deles começa com um bloco `DROP TABLE IF EXISTS` que o torna
   re-executável. Uma concatenação de migrations não tem isso, e o vídeo de
   entrega depende de rodar o arquivo mais de uma vez. O gerador precisaria emitir
   o preâmbulo de limpeza.
2. Os dialetos não são intercambiáveis. O `documentos/script_bd.sql` atual é DDL
   **Oracle** — `VARCHAR2`, sem `ENGINE=InnoDB`. Entregar esse arquivo para um
   banco MySQL produz um script que não roda.

### 2.9 🟢 Não há verificação de dependência vulnerável

Os outros dois repositórios têm o comando pronto: a API .NET responde a
`dotnet list package --vulnerable --include-transitive`, e o app a `npm audit` —
e os dois **acusaram** pacotes vulneráveis quando rodados. Este projeto não tem
equivalente configurado: o Maven não audita nada por padrão.

Isso não quer dizer que esteja limpo. Quer dizer que **ninguém sabe**, e que os
outros dois só souberam porque a ferramenta existia.

**✅ Verificado, e não estava limpo.** Quatro pacotes com aviso, **três deles
CRÍTICOS no mesmo artefato** — e é o pior artefato possível para isso:

| Pacote | Versão | Avisos | Corrigido em |
|---|---|---|---|
| `org.apache.tomcat.embed:tomcat-embed-core` | 10.1.55 | **3 CRÍTICOS**: `GHSA-9xv2-5v5q-p794` (bypass de autenticação por captura-replay no DIGEST), `GHSA-gcx9-497g-6cp6` (controle de acesso e autorização incorretos), `GHSA-h3x4-894j-xpx5` (autorização incorreta na autenticação FORM) | 10.1.58 |
| `com.fasterxml.jackson.core:jackson-databind` | 2.21.4 | 3 moderados: `@JsonView` e `@JsonIgnoreProperties` contornáveis na desserialização | 2.21.5 |
| `org.apache.logging.log4j:log4j-api` | 2.24.3 | `GHSA-qv9r-c865-cp47`, moderado | 2.25.5 |
| `org.apache.commons:commons-lang3` | 3.17.0 | `GHSA-j288-q9x7-2f5v`, moderado — recursão sem controle | 3.18.0 |

O `tomcat-embed-core` **é** a camada HTTP da aplicação, e é ela que fica exposta na
internet no App Service. Não é dependência de canto.

**A ferramenta proposta acima não serviu.** O `org.owasp:dependency-check-maven`
exige chave da API da NVD e falha sem ela:

```
UpdateException: Error updating the NVD Data
  caused by NvdApiException: Invalid API Key, length of 0 too short
```

**O que serviu:** a API pública do **OSV** (`api.osv.dev/v1/querybatch`), que agrega
a GitHub Advisory Database, não pede autenticação e aceita consulta em lote. O
caminho é: `mvn dependency:list -DincludeScope=runtime` para obter as 110
dependências reais, e uma consulta em lote ao OSV com ecossistema `Maven`. Roda em
segundos e não depende de cadastro.

**Como foi corrigido:** quatro propriedades no `<properties>` do `pom.xml`
sobrepondo o que o `spring-boot-dependencies` fixa. **Não havia patch do Boot para
subir** — a 3.5.16 é a última publicada, e é ela que fixa as versões com aviso.

O critério da versão escolhida foi a **menor que corrige**, não a mais recente: o
BOM do Spring Boot é um conjunto curado e testado junto, e quanto menor o desvio
dele, menor a chance de trocar uma vulnerabilidade por uma incompatibilidade.

Duas observações que só aparecem fazendo:

- **A 10.1.58 do Tomcat nunca foi publicada no Maven Central** — o projeto pula
  versões. A seguinte é a 10.1.59, e ela contém as correções.
- **Ficar na linha 10.1 é obrigatório**, não conservadorismo. O Tomcat 11 é
  Servlet 6.1 / Jakarta EE 11 e o Spring Boot 3.5 exige Servlet 6.0 / EE 10: subir
  para 11.x não seria atualizar, seria trocar de plataforma.

Depois da mudança: **277 testes passando** e o OSV devolvendo *"nenhuma
vulnerabilidade conhecida nas dependências de runtime"*.

---

## 3. O que já foi corrigido

Registrado aqui para que a auditoria não seja lida como lista de pendências que
inclui coisas resolvidas.

| Correção | Commit |
|---|---|
| `ddl-auto=validate` reprovava em **54 das 133 colunas** contra MySQL real — 33 de UUID, 14 de enum, 7 booleanas. Corrigido com duas propriedades de tipo JDBC e sete colunas em `INT` | `fix(mysql): faz o ddl-auto=validate passar contra um MySQL real` |
| Convenção `NUMBER(1) → TINYINT` em quatro documentos reintroduziria a falha na próxima tabela | `docs: corrige a convencao de tipo booleano no MySQL` |
| Metade do schema não era versionada — as `t_clyvo_*` nasciam de SQL avulso no repo .NET e não existiriam na nuvem | `feat(schema): V8 traz as tabelas da API .NET para o Flyway` |
| Duas convenções de nome conviviam: treze tabelas sem prefixo (V1–V7) e seis com (V8). Num schema de sala de aula, `animal` não identifica dono | `feat(schema): V9 padroniza os nomes de tabela com o prefixo t_clyvo` |

---

## 3.1 O que a V9 exigiu da API .NET — resolvido

O rename das tabelas é o único ponto onde uma mudança daqui **quebra a outra API**,
e por isso fica registrado em vez de ficar implícito.

A .NET lê `animal` e `tutor` por nome, em `AnimalConfiguration.cs` e
`TutorConfiguration.cs`. O EF Core não valida schema no boot: ela **sobe
normalmente** e falha só na primeira consulta, dizendo que a tabela não existe.
Enquanto os dois `ToTable` não acompanharem, os endpoints de lembrete que resolvem
`nomeAnimal` retornam erro.

Há um segundo efeito, menos óbvio. O `AppDbContext` da .NET declara
`DbSet<Veterinario>` mapeado para `t_clyvo_veterinario` — uma entidade morta,
apontando para uma tabela que até então **não existia**. Depois da V9 esse nome
passou a existir, com o formato do veterinário do núcleo clínico, que não é o do
modelo dela. O achado §2.7 da spec da .NET tratava isso como faxina; virou
correção necessária.

| O quê | Onde | Estado |
|---|---|---|
| `ToTable("t_clyvo_animal")` | `ClyvoVet.Api/Data/Configurations/AnimalConfiguration.cs` | ✅ |
| `ToTable("t_clyvo_tutor")` | `ClyvoVet.Api/Data/Configurations/TutorConfiguration.cs` | ✅ |
| Remover `Veterinario` e `Consulta` (entidade, configuration e `DbSet`) | `ClyvoVet.Api/Data/` e `Models/` | ✅ |
| Renomear as treze tabelas na PARTE 1 do DDL | `ClyvoVet-api/schema/script_bd.sql` | ✅ |

Verificado contra o MySQL 8 do ambiente local depois das quatro: `POST /animais`
aqui seguido de `POST /lembretes` lá devolve **201 com o `nomeAnimal` resolvido**,
que é o fluxo cruzado que a spec 11 do app chama de critério de pronto.

---

## 4. Ordem sugerida

| # | O quê | Bloqueia a entrega? |
|---|---|---|
| 1 | Script `az` de deploy desta API (§2.2) | ✅ feito — 12 scripts em `azure/`, um por recurso |
| 2 | Pool declarado (§2.3) | ✅ feito — em **10**, não 15, e o motivo mudou junto: ver §2.3 |
| 3 | Correlation ID (§2.4) | Não |
| 4 | Compartilhar a chave JWT com a .NET via Key Vault | Não — ver spec da .NET, §2.1 |
| 5 | Gerar a variante MySQL do `script_bd.sql` (§2.8) | Não, mas encerra uma classe de defeito que já mordeu duas vezes |
| 6 | Remover estágio `Imagem` do pipeline (§2.7) | ✅ feito — a régua confirmou: app containerizado é −40 |
| 7 | Auditoria de dependência (§2.9) | ✅ feito — e achou **3 CRÍTICOS** no `tomcat-embed-core`. Ver §2.9 |
| 8 | `@Version` (§2.5) | Não — **muda comportamento**, deixar para depois da entrega |
| 9 | Redis (§2.1) | Só se escalarem |

---

## 5. O que **não** fazer

Registrado porque são caminhos plausíveis que a auditoria descartou com base no
código, e alguém pode propô-los de novo:

- **Não** introduzir chamadas HTTP desta API para a .NET, nem o contrário. Hoje
  não existe nenhuma, e a integração pelo banco tem integridade referencial
  garantida que uma chamada HTTP perderia.
- **Não** separar os bancos. Exigiria replicar `animal` e `tutor` para o lado
  .NET — dados que ela não é dona — e trocar consistência forte por eventual.
- **Não** mover as `t_clyvo_*` de volta para fora do Flyway. Foi exatamente o que
  fez metade do schema não existir no caminho de deploy.
