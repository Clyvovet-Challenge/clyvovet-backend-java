# Plano de entrega — Sprint 3 (12/09/2026)

> Escrito em **07/09/2026**, faltando 5 dias. Consolida as decisões tomadas numa
> sessão de entrevista sobre a arquitetura, revisadas contra o **documento oficial
> do Challenge** (`2TDS Fevereiro - Challenge 2026 - 2º Semestre.pdf`).
>
> Complementa a [11-auditoria-de-arquitetura.md](11-auditoria-de-arquitetura.md),
> que lista o que o código precisa corrigir. Este documento diz **o que entra na
> entrega, em que ordem e por quê** — quando os dois discordam, este vale, porque
> ele considera a régua de avaliação.

---

## 1. O que o documento oficial corrigiu

Quatro afirmações que circulavam nas nossas specs internas estavam erradas.
Registradas aqui porque cada uma mudou uma prioridade.

| Afirmação anterior | O que o documento oficial diz |
|---|---|
| *"Pipeline CI/CD quebrado zera a nota de DevOps"* | **Falso para a Sprint 3.** Os requisitos dela (páginas 10–16) não mencionam pipeline. O zero por pipeline quebrado é da **Sprint 4** (p. 53), junto com Azure DevOps obrigatório |
| *"Nenhuma spec permite Azure SQL"* | **Falso.** A p. 11, item 3.2, lista explicitamente *"MySQL, Azure SQL ou PostgreSQL"*. Os dois são válidos — ficamos em MySQL por economia, não por regra |
| *"O deploy na nuvem não bloqueia a entrega"* | **Meia verdade.** Não bloqueia os 100 pontos de Mobile. Mas **"Entrega em LOCALHOST: zero de nota"** (p. 15) zera DevOps |
| *"O vídeo de DevOps é item secundário"* | **Falso.** Ele vale **80 dos 100 pontos** da disciplina (p. 13, item 9.2) |

---

## 2. A régua que dirige este plano

### DevOps Tools & Cloud Computing — 100 pontos

| Item | Pontos |
|---|---|
| Desenho da arquitetura com fluxos, recursos e explicação | até **20** |
| Vídeo demonstrativo da solução | até **80** |

**Opção escolhida: 2 — App Service + Banco PaaS.** *"Aqui nada pode ser
Containerizado"* (p. 10).

Penalidades que este repositório pode disparar:

| Penalidade | Valor |
|---|---|
| Entrega em LOCALHOST | **zero de nota** |
| Professor sem acesso ao repositório ou vídeo | **zero de nota** |
| Recursos não criados via Azure CLI | **−30** |
| Script faltando | **−10 por script** |
| App containerizado | **−40** |
| Banco containerizado | **−40** |
| Banco não permitido (H2 entra aqui) | **−40** |
| Sem README com orientações de deploy/teste | **−30** |
| Sem evidência clara de cada operação CRUD no banco | **−30** |
| Sem o PDF com nome completo, RM e links | **−30** |
| Dados sensíveis expostos no código-fonte | **−20** |
| Não incluir o DDL das tabelas | **−10** |
| Usar apenas uma tabela no CRUD | **−20** |
| Tabelas fora do CORE da solução | **−30** |
| Desenho de arquitetura parecido com Fluxo, Togaf ou UML | **−20** |

### Java Advanced — 100 pontos

| Item | Pontos | Estado |
|---|---|---|
| Camada de visualização (frontend) | 30 | **em aberto** — ver §6 |
| Flyway para controle de versões | 20 | ✅ V1 a V9 |
| Spring Security: 2+ tipos de usuário, rotas protegidas por perfil | 30 | ✅ três perfis, matriz de autorização |
| Funcionalidades completas: 2+ fluxos além de CRUD, com validação | 20 | ✅ agendamento e consentimento de histórico |

---

## 3. A infraestrutura decidida

Tudo novo, na assinatura **`2tdspw-rm562312-pedrooliveira`**, e não na do colega
que fez a entrega de .NET. Motivo: quem opera precisa ser dono da assinatura, e
depender de acesso de terceiro no item mais crítico da entrega é risco fora do
nosso controle.

| Recurso | Valor | Por quê |
|---|---|---|
| Região | **`brazilsouth`** | verificado por CLI: App Service Linux (B1 e B2) **e** MySQL Burstable disponíveis na mesma região. A infra anterior tinha o grupo em `brazilsouth` e o banco em `chilecentral`, atravessando regiões em cada consulta |
| Resource Group | um só, em `brazilsouth` | |
| App Service Plan | **B1** Linux, compartilhado | O F1 gratuito existe em `brazilsouth`, mas **não tem Always On**: o app dorme após ~20 min ocioso, e o feedback das entregas é em 26/09. B1 é o SKU mais barato **com** Always On. Se 1,75 GB apertarem para as duas APIs, subir para B2 é um comando e não recria nada |
| Web App Java | runtime `JAVA:17-java17` | |
| Web App .NET | runtime `DOTNETCORE:8.0` | as duas em prod: o app precisa das duas, e URL local numa e pública na outra é o tipo de configuração que falha ao vivo |
| MySQL | Flexible Server `Standard_B1ms`, Burstable, 8.0 | PaaS, o que evita o −40 de banco containerizado |
| Instâncias | **uma por API. Autoscale DESLIGADO** | ver §7 |
| Segredos | Key Vault para a chave JWT compartilhada | |

**O banco nasce vazio.** O Flyway cria as 19 tabelas no primeiro boot, da V1 à V9.
É o único caminho em que o DDL entregue e o banco real não podem divergir — e essa
divergência já mordeu duas vezes. Como o vídeo ainda não foi gravado, mudar o
processo agora não custa nada.

**Os recursos da assinatura do colega só são apagados depois** da infra nova estar
de pé e verificada, e a execução é dele. Nunca antes: ficar sem nenhuma infra
enquanto a nova sobe é risco desnecessário a 5 dias do prazo.

---

## 4. O que este repositório precisa entregar

Em ordem de execução. O que está acima destrava o que está abaixo.

1. **Scripts `az`, um por recurso** — resource group, MySQL, plano, os dois web
   apps, e as configurações. Recurso fora da CLI é −30; script faltando é −10
   cada. O repositório da .NET tem `azure/00-04.sh` servindo de modelo.
2. **README com o passo a passo de deploy** — ver §5, porque ele é mais crítico do
   que parece.
3. **Deploy das duas APIs e verificação** de que o fluxo cruzado responde.
4. **Varredura de segredo exposto no código-fonte** — vale −20 direto e leva
   minutos.
5. **Roteiro do vídeo de DevOps** com o seed pensado para a narrativa: o item 9.3
   exige CRUD em **duas tabelas relacionadas**, com `SELECT` evidenciando cada
   operação. `t_clyvo_animal` e `t_clyvo_tutor` servem — são o CORE e têm FK entre
   si, o que atende também o *"tabelas significativas para a solução"*.
6. **PDF de entrega** — nome completo e RM de todos, link do GitHub, link do
   YouTube. *"Não pode ter mais nada no PDF"* (p. 12).
7. ✅ **Removido o que disparava −40** — o `deploy.sh` provisionava VM com Docker
   Compose e H2 (três penalidades de −40 no mesmo arquivo: app containerizado,
   banco containerizado e banco não permitido), e o `azure-pipelines.yml` tinha um
   estágio `Imagem` que construía imagem Docker. O `Dockerfile` ficou: o proibido é
   o **artefato publicado** sair dele.
8. **Se sobrar tempo:** JWT compartilhado com a .NET, na ordem decidida — só
   depois do deploy estar verificado, para que uma quebra tenha causa óbvia.

---

## 5. O README é o roteiro do vídeo, não a documentação depois do fato

O item 9.2 exige, textualmente:

> *"Deploy da aplicação seguindo **exatamente** os passos descritos no README.md"*
> e *"Criação, configuração e testes do App e do Banco de Dados na nuvem seguindo
> **exatamente** os passos descritos no README.md"*

E o vídeo precisa **começar pelo clone do repositório** — *"obrigatório começar
assim os testes da solução"* —, **sem cortes** ao evidenciar testes e persistência.

A consequência prática inverte a ordem natural de trabalho: **o README precisa
estar correto e testado antes de gravar**, porque a gravação o segue linha por
linha e não há corte para consertar no meio. Um README que "está quase certo" custa
uma regravação inteira.

Por isso a meta é o caminho completo — clone, scripts, deploy, teste — rodado do
zero pelo menos **uma vez antes de gravar**.

---

## 6. Os 30 pontos de frontend: em aberto com o professor

A p. 20 exige *"Camada de visualização (frontend)"* valendo **30 pontos**, e a
p. 22 pede *"vídeo da aplicação **web**"*.

**Estado verificado do repositório:** não há camada de visualização. Sem
`templates/`, sem `static/`, nenhum `@Controller` — apenas `@RestController`. É uma
API REST pura.

**Decisão tomada:** o app Expo é a camada de visualização, e a pergunta vai ao
professor. Não vamos construir um frontend Thymeleaf preventivamente.

**Se a resposta for que o app não conta**, o caminho está mapeado e o custo é
conhecido:

- Entra `spring-boot-starter-thymeleaf`; o `spring-boot-starter-web` já está no
  `pom.xml`.
- Os 74 endpoints, DTOs, services, repositories e mappers **não são tocados**.
- O `SecurityConfig` **é** tocado, e é o único ponto de risco. Ele tem hoje uma
  `SecurityFilterChain` única, com `csrf` desabilitado (linha 67) e
  `SessionCreationPolicy.STATELESS` (linha 72), valendo para tudo. Um frontend web
  precisa do contrário — sessão, formulário e CSRF ligado — então a cadeia precisa
  ser partida em duas:

```
@Order(1)  securityMatcher("/api/v1/**")  → stateless, csrf off, filtro JWT  (a de hoje)
@Order(2)  todo o resto                    → sessão, formLogin, csrf on       (a nova)
```

O que torna isso viável é o `PREFIXO_API` do `WebConfig`: **todo endpoint REST vive
sob `/api/v1`**, aplicado centralmente. A separação fica sem ambiguidade.

O risco real é que esse é o arquivo que protege os 74 endpoints do app, e errar ali
abre uma rota em silêncio. A rede de proteção já existe: `CoberturaDeAutorizacaoTest`,
`AutorizacaoPorRecursoTest`, `AcessoCruzadoTest` e `OwnershipTest` quebram se
alguma rota mudar de proteção.

---

## 7. Instância única, autoscale desligado — e isso precisa estar escrito

Não é economia. É que cinco componentes guardam estado no processo, e **três estão
neste repositório**:

| Componente | O que quebra com mais de uma instância |
|---|---|
| `security/RevogacaoTokenService` | logout numa instância não revoga nas outras — o token segue válido |
| `security/RateLimitFilter` | o limite efetivo multiplica pelo número de réplicas |
| `config/CacheConfig` | `@CacheEvict` limpa só a instância que atendeu |

Os outros dois estão na .NET: os dois `BackgroundService`, que duplicariam
notificação.

Com uma instância, nenhum dos cinco é problema. Ligar autoscale por engano quebra
segurança e produto ao mesmo tempo, e **sem erro no log** — daí o registro
explícito. A correção definitiva é Redis, e ela entra no mesmo dia que o autoscale,
nunca antes.

---

## 8. A avaliação oral, e o que ela implica no modo de trabalho

A p. 22 registra que, depois da entrega, **cada aluno faz uma avaliação oral
individual** em sala, onde deve:

- explicar trechos específicos do próprio código;
- justificar decisões de implementação;
- descrever **se e como utilizou ferramentas de IA** no processo.

Isso não é penalidade — é parte da avaliação, e muda o que "pronto" significa. Um
script `az` que funciona mas que ninguém do grupo sabe explicar é uma entrega
incompleta na prática.

Por isso as decisões deste plano estão registradas **com o motivo**, e não só com o
resultado: o "por que B1 e não o F1 gratuito", o "por que banco vazio e não `script_bd.sql`",
o "por que HS256 e não RS256". São exatamente as perguntas de uma banca.

---

## 9. O que **não** fazer

- **Não** publicar artefato saído do `Dockerfile`. App containerizado na Opção 2 é
  −40, e é o que o estágio `Imagem` do pipeline produz hoje.
- **Não** usar H2 na nuvem. É −40 explícito, e é o que o `deploy.sh` atual
  provisiona.
- **Não** montar pipeline no Azure DevOps agora. É requisito da **Sprint 4**;
  gastar tempo nele agora é tirar tempo dos 80 pontos do vídeo.
- **Não** migrar para Azure SQL nesta sprint. É permitido, mas custaria ~830 linhas
  de T-SQL, quebraria a V9 (`RENAME TO` não existe em T-SQL) e refaria a convenção
  booleana inteira.
- **Não** provisionar o banco pelo `script_bd.sql` e depois ligar o Flyway com
  baseline. O histórico nasce inconsistente e depende de alguém lembrar do
  parâmetro.
- **Não** apagar os recursos da outra assinatura antes de a infra nova estar
  verificada.
- **Não** deixar o redesenho visual do app entrar antes do deploy. Aparência não é
  critério pontuado em nenhuma das duas disciplinas deste repositório.

---

## 10. Cronograma

| Data | Meta |
|---|---|
| **08/09** | Scripts `az` escritos e README de deploy pronto |
| **09/09** | **Deploy completo rodado do zero e verificado.** É a data que decide se dá tempo de gravar com folga |
| 10/09 | Gravação do vídeo de DevOps, seguindo o README |
| 11/09 | Vídeo de Mobile, PDF, revisão. Margem para regravar |
| **12/09** | Entrega. PR **mergeado** no repositório do Classroom, não apenas aberto |

Se em 09/09 o deploy não estiver de pé, a ordem de sacrifício é: **1º** o redesenho
visual do app, **2º** o JWT compartilhado, **3º** a .NET fica local e só a Java vai
para a nuvem. Nessa ordem, porque é a ordem inversa do valor em pontos.
