# Plano de entrega — Sprint 4 (04/11/2026)

> Aberto em **03/10/2026**, faltando 32 dias. Este documento diz **o que entra na entrega
> final, em que ordem e por quê**. O enunciado e a régua completa estão em
> [`specs/03-sprint-4.md`](../specs/03-sprint-4.md). Aqui fica o que decidimos fazer com eles.
>
> **Estado:** lacunas levantadas e cronograma proposto em 03/10/2026. Os números da seção 4
> foram medidos nesse dia, e não copiados do backlog de agosto.
>
> **Revisto em 18/10/2026:** o Java **não vai para a Azure**. Roda na Render, com o banco MySQL
> no Aiven ([ADR 008](adr/008-java-na-render-com-mysql-no-aiven.md)). A Azure fica com a
> disciplina de DevOps, que publica a API .NET. As fases 3 e 4 abaixo já refletem isso.

---

## 1. Onde o trabalho acontece

A `main` é a entrega da Sprint 3, cuja nota saiu em 04/10/2026: 34/100. Em 07/10 o professor
informou que ela sobe para 44 (ver L9 e a [seção 2.1](#21-a-nota-da-sprint-3-por-critério)).

- Todo trabalho da Sprint 4 vai para a branch **`sprint-4`**.
- Merge `sprint-4` → `main` só quando o Leonardo pedir, via PR, sem rebase nem force-push.
- O CI só roda em push na `main` e em PR para a `main`. Até o PR, a suíte roda localmente
  antes de cada commit.

## 2. A régua que dirige este plano

| Critério | Pontos | O que decide a nota |
|---|---|---|
| Demonstração técnica | 40 | App **no ar**, fluxos principais navegáveis, conceitos da disciplina no contexto do app, boa UI/UX |
| Narrativa da solução | 20 | Proposta clara, decisões **justificadas**, originalidade |
| Integração multidisciplinar | 20 | Como as outras disciplinas entram, **com evidência** (docs, canvas, protótipos, SQL) |
| Apresentação oral | 10 | Os cinco no vídeo, com clareza e domínio |
| Organização da entrega | 10 | Repositório, README e documentação |

Penalidades que mais nos ameaçam (lista completa no spec):

| Penalidade | Desconto | Por que nos ameaça |
|---|---|---|
| Entrega fora do prazo ou fora do portal | −100 | Cada disciplina tem um formato. Ver `documentos/LINKS-ENTREGA.txt` |
| Ausência de evidência de colaboração | −10 | Ver seção 3 |
| Violação evidente de boas práticas | −10 cada | Revisão do código antes da entrega |
| Código repetido que poderia ser extraído | −5 cada | Idem |
| Erro visual ou de fluxo durante a apresentação | −5 | A demo depende do app e da API no ar ao mesmo tempo |

### 2.1 A nota da Sprint 3 por critério

Detalhada pelo professor em 07/10/2026, a pedido do grupo:

| Critério da Sprint 3 | Nota | O que faltou |
|---|---|---|
| Frontend | 0/50 | A aplicação web: telas, formulários e validações |
| Spring Security | 20/30 | Com um front, esperava-se OAuth2 e a tela de login configurada, não só JWT protegendo a API |
| Funcionalidades completas | 14/20 | Os fluxos de negócio expostos na interface |
| **Total** | **34 + 10 = 44** | |

Os 10 pontos a mais foram um ajuste do professor: as migrations não entraram na nota, porque esse
requisito saiu da sprint e os pontos foram para o frontend.

**O que isso muda aqui:** o que faltou em Security (OAuth2 e tela de login) é a base da Fase 1b,
feita em `a8f1e96`. O que faltou em Frontend e Funcionalidades são as telas dos fluxos, nas
etapas seguintes da Fase 1b. É a mesma lacuna L9.

**Como a Sprint 4 será avaliada, nas palavras do professor:** a nota será "praticamente sobre a
apresentação", e ele vai avaliar **se todos sabem explicar o sistema**. Essa é a evidência de
colaboração que conta: os cinco precisam dominar o projeto inteiro, não só a parte que fizeram.

## 3. Riscos já conhecidos

Levantados em 03/10/2026, ao abrir a sprint.

- **Colaboração.** Até o commit `2373015`, o histórico tem commits de `pedrinzz10` (127) e do
  Leonardo (`leojp04` 26 + `leop04` 5). Fabricio, Miguel e Henrique têm
  **zero** (Henrique foi incluído na lista da equipe em 03/10/2026). Os commits da Sprint 4
  precisam vir dos cinco, distribuídos no tempo, e não num push único no fim.
  **Atualizado em 07/10/2026:** o professor vai medir a colaboração na apresentação, perguntando
  sobre o sistema a cada integrante (ver seção 2.1). Commits de todos continuam ajudando, mas o
  risco maior passou a ser alguém não saber explicar o que o sistema faz e por quê.
- ~~**A interface avaliada é de outro repositório.**~~ Revisto em 04/10/2026: a nota da Sprint 3
  (34/100) zerou a parte web, e o professor exigiu telas Thymeleaf com login OAuth2. A
  interface avaliada passa a ser este repositório (ver [ADR-002](adr/002-telas-thymeleaf-no-spring.md)).
  O app mobile segue como evidência de integração e precisa rodar contra a API **publicada**.
- **O README diverge do código.** Ele diz que o perfil padrão é `oracle` (o código diz `mysql`,
  em `application.properties`), que a suíte tem 205 testes (são 392) e se contradiz sobre o que
  `spring-boot:run` sem perfil faz. O README completo vale nota (critério 5).
- **O índice de `docs/` está defasado:** o `17-contrato-de-erro.md` não aparece em `docs/README.md`.
- **`LINKS-ENTREGA.txt` ainda é da Sprint 3.**
- ~~**A API está fora do ar.**~~ O Web App da Sprint 3 foi apagado
  (`app-clyvovet-java-rm562312.azurewebsites.net` não resolvia no DNS em 03/10/2026). Resolvido em
  18/10/2026 com a API na Render, no endereço `https://clyvovet-backend-java.onrender.com`.
- ~~**O perfil `mysql` nunca rodou num MySQL real.**~~ Engano corrigido em 06/10/2026: ele
  já tinha rodado em 06/09 (V1 a V7), e a prova foi repetida com V1 a V20 num MySQL 8 local,
  passando no `ddl-auto=validate`. Falta só o servidor da Azure.
- **Database fica fora do escopo deste repositório** até termos o enunciado oficial da Sprint 4
  de Database. O `specs/04-dependencias-externas.md` fala em procedures chamadas pelo Java, mas
  isso vem de um documento interno de agosto, não do enunciado.

## 4. O que este repositório precisa entregar

Escopo decidido em 03/10/2026: **consolidar** o que existe, sem módulo novo. **Revisto em
04/10/2026**, depois da nota da Sprint 3: entra um módulo novo, a camada web Thymeleaf com login
OAuth2 (L9 e Fase 1b). **Revisto em 18/10/2026:** o deploy sai na
Render, com banco no Aiven, e não mais na conta Azure do Pedro (ver ADR 008). Os scripts de
`azure/` continuam no repositório como evidência da disciplina de Cloud, mas não são o caminho
da entrega.

### Lacunas

| # | Lacuna | Pesa em | Evidência |
|---|---|---|---|
| ~~L1~~ | ~~API fora do ar~~ | 40 pts (demonstração) | ✅ no ar na Render desde 18/10/2026; falta publicar os commits novos |
| ~~L2~~ | ~~Perfil `mysql` nunca rodou num MySQL real~~ | risco de o deploy falhar no boot | ✅ MySQL 8 local, V1 a V20 (06/10/2026); o Azure fica para a Fase 3 |
| ~~L3~~ | ~~README diverge do código~~ ✅ | 10 pts (organização) | `application.properties:5` diz `mysql`; a suíte tem 392 testes |
| ~~L4~~ | ~~Índice de `docs/` sem o 17; `LINKS-ENTREGA.txt` da Sprint 3~~ ✅ | 10 pts (organização) | `docs/README.md` |
| L5 | Código não revisado contra as penalidades de boas práticas (−10) e repetição (−5) | penalidades | — |
| L6 | 3 de 5 integrantes sem commit | −10 | `git log` |
| ~~L7~~ | ~~Sem roteiro do vídeo nem seção de integração multidisciplinar~~ | 20 + 20 + 10 pts | ✅ ADRs 003 a 009, seção no README e `19-roteiro-do-video-sprint4.md`; o vídeo em si ainda não foi gravado |
| ~~L8~~ | ~~Pipeline sem deploy automático~~ | DevOps | Descartada: o DevOps da Sprint 4 usa a API .NET, e o Java é publicado manualmente na Render |
| ~~L9~~ | ~~Sem telas web~~ | 40 pts (demonstração, "boa UI e UX") | ✅ Fase 1b fechada: Thymeleaf, OAuth2 Google/GitHub, fluxos do tutor e do veterinário, Meus pets |

### Fases

**Fase 1, não depende de ninguém** — feita (L3, L4 e a revisão de código; os achados 2 e 3 ficam abertos)
- L3 e L4: corrigir o README e o índice de `docs/`.
- L5: revisão de código (skill `/code-review`). Cada achado é corrigido com teste antes.
  Os achados restantes (2 e 3) ficam para depois da Fase 1b, que também passa pela revisão.

**Fase 1b, camada web (L9)** — feita; ver [ADR-002](adr/002-telas-thymeleaf-no-spring.md)
- Base: duas cadeias de segurança (API stateless com JWT; telas com sessão e CSRF), login por
  formulário, layout e login OAuth2 (Google e GitHub). OAuth2 só autentica e-mail que já tem conta.
- Fluxo do tutor: agendar consulta e cancelar, com formulário e validação.
- Fluxo do veterinário: concluir atendimento e marcar retorno; lista de retornos vencidos.
- README: seção "Aplicação web".
- **Em aberto:** quem implementa cada fluxo. O Leonardo vai perguntar ao professor que evidência
  de colaboração ele espera (ver L6); os fluxos foram desenhados como tarefas independentes.

**Fase 2, local e sem crédito** — feita em 06/10/2026
- L2: subir o perfil `mysql` contra um MySQL 8 no Docker e confirmar o `validate`.
- Se algo falhar, a correção é migration nova (ver `.claude/rules/migrations.md`).

**Fase 3, Render e Aiven** ([ADR 008](adr/008-java-na-render-com-mysql-no-aiven.md))
- L1: Web Service Docker na Render (branch `sprint-4`, deploy manual) e MySQL no Aiven. Provado em
  18/10/2026: Flyway validou as 21 migrations, `/actuator/health` respondeu `UP`, cadastro, login,
  raças e login social (Google e GitHub) funcionaram online.
- **Feito em 18 a 21/10/2026:**
  1. ~~`git push` da `sprint-4`~~ e ~~2. deploy manual na Render~~ (`dcd7aa0` no ar).
  3. ~~`CLYVOVET_DEMO_SENHA` na Render~~: o log do app em execução mostrou os 4 usuários criados, e o
     login do tutor pela tela funcionou. Os outros três perfis ainda **não foram logados** online.
  - ~~Botão **Excluir pet** em Meus pets~~, provado online: o pet sem histórico foi excluído e o
    Bolinha (com atendimentos) foi recusado pelo banco do Aiven, com a explicação na tela.
  - ~~5. Conferir na Render o 429 do login~~, provado em 21/10/2026 com e-mail inexistente: pela API,
    10 tentativas devolveram 401 e a 11ª em diante 429 (`Retry-After`), e o balde voltou a aceitar
    depois de 1 minuto. Pela tela (com token CSRF e cookie) o 429 veio na 10ª tentativa, porque o balde
    é **compartilhado** com a API e uma já tinha sido gasta. Sem token CSRF a tela responde 403 antes do
    limite. **Não testado:** se um cabeçalho `X-Forwarded-For` forjado escapa do limite (depende de o
    proxy da Render sobrescrevê-lo).
- **Falta, por ordem:**
  4. Tirar o app do Google do modo *Teste*, ou listar os `Usuários de teste` que vão aparecer no vídeo.
  6. Smoke test dos fluxos principais pelas telas e pelo app, apontando `EXPO_PUBLIC_JAVA_BASE_URL`
     para `https://clyvovet-backend-java.onrender.com`. Já provados online: login do tutor, Meus pets,
     cadastrar e excluir pet. Faltam agendar e cancelar consulta e a área do veterinário.
- **Riscos que o plano aceita:** a subida a frio leva mais de 4 minutos (aquecer o app antes de
  gravar e antes da apresentação); o teste do Aiven vale 30 dias a partir de 18/10/2026, ou seja,
  até **17/11/2026**. Se a correção da entrega passar dessa data, o serviço é desligado, a menos
  que o grupo cadastre um cartão (decisão do grupo, US$ 5 por mês).
- A API .NET pode apontar para o mesmo banco do Aiven no vídeo de DevOps. Ainda **não foi testado**.

**Fase 4, narrativa e vídeo**
- ~~L7~~: ADRs 003 a 009 (a narrativa, 20 pts), seção "Integração multidisciplinar" no README
  (20 pts), `docs/19-roteiro-do-video-sprint4.md` e `LINKS-ENTREGA.txt` da Sprint 4. ✅
- **Falta:** gravar o vídeo de até 15 minutos, com os cinco falando e navegando pelas telas
  Thymeleaf, e colocar o link no `LINKS-ENTREGA.txt`.
- **Falta:** ensaiar com os cinco. O professor vai perguntar a cada um como o sistema funciona
  (seção 2.1), então todos precisam explicar o projeto inteiro.

**Contínuo**
- L6: dividir as tarefas por pessoa, para que os cinco commitem na `sprint-4` ao longo do mês.

## 5. Decisões

Cada decisão com motivo não óbvio vira um ADR em [`adr/`](adr/README.md). Os ADRs são o
material bruto da narrativa (critério 2).

| ADR | Decisão |
|---|---|
| [001](adr/001-ui-pelo-app-mobile.md) | ~~A interface avaliada é o app mobile, e não telas no Spring~~ (substituído por 002) |
| [002](adr/002-telas-thymeleaf-no-spring.md) | Telas Thymeleaf no próprio Spring, com login OAuth2 |
| [003](adr/003-prontuario-pertence-ao-tutor.md) | O prontuário pertence ao tutor: três níveis de acesso e quebra de vidro |
| [004](adr/004-banco-compartilhado-java-dona-do-schema.md) | Banco compartilhado entre as duas APIs, com a Java dona do schema |
| [005](adr/005-perfis-de-banco-e-migrations-por-dialeto.md) | Perfis de banco e migrations em dois conjuntos (Oracle e MySQL) |
| [006](adr/006-catalogo-de-racas-com-chave.md) | Catálogo de raças com `chave`, em vez de texto livre ou enum |
| [007](adr/007-contrato-de-erro-com-referencia.md) | Contrato de erro das três pontas, com referência só na falha de servidor |
| [008](adr/008-java-na-render-com-mysql-no-aiven.md) | Java na Render e banco MySQL no Aiven, fora da Azure |
| [009](adr/009-login-social-cria-conta-de-tutor.md) | Login social cria conta de tutor no primeiro acesso |

## 6. Cronograma

Revisto em 18/10/2026 para refletir Render e Aiven. A data de entrega não muda.

| Até | Meta |
|---|---|
| ~~07/10~~ | Fase 1b, base: duas cadeias de segurança, login por formulário e OAuth2 — ✅ |
| ~~12/10~~ | Fase 1: README corrigido e primeira revisão de código — ✅ |
| ~~14/10~~ | Fase 1b, fluxos: telas do tutor e do veterinário — ✅ |
| ~~19/10~~ | Fase 2: perfil `mysql` validado num MySQL real local — ✅ feito em 06/10 |
| ~~26/10~~ | Fase 3: API e telas no ar — ✅ no ar desde 18/10 na Render + Aiven |
| **20/10** | Push da `sprint-4`, deploy na Render com os 4 commits novos e os passos 3 a 5 da Fase 3 |
| 26/10 | Smoke test completo pelas telas e pelo app; Google fora do modo *Teste* |
| 31/10 | Roteiro ensaiado e vídeo gravado, navegando pelas telas Thymeleaf |
| 01–03/11 | Margem para regravar e conferir a entrega |
| **04/11** | Entrega no portal |

Se na véspera da gravação a Render não responder, todo o resto para até ela subir. Sem o app no
ar, a demonstração de 40 pontos não existe, e nenhum outro item compensa essa perda. Por isso
o app é aquecido antes de gravar e o banco do Aiven é conferido antes de cada ensaio.
