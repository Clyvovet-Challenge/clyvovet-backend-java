# Plano de entrega — Sprint 4 (04/11/2026)

> Aberto em **03/10/2026**, faltando 32 dias. Este documento diz **o que entra na entrega
> final, em que ordem e por quê**. O enunciado e a régua completa estão em
> [`specs/03-sprint-4.md`](../specs/03-sprint-4.md). Aqui fica o que decidimos fazer com eles.
>
> **Estado:** lacunas levantadas e cronograma proposto em 03/10/2026. Os números da seção 4
> foram medidos nesse dia, e não copiados do backlog de agosto.

---

## 1. Onde o trabalho acontece

A `main` é a entrega da Sprint 3, cuja nota saiu em 04/10/2026: 34/100 (ver L9).

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

## 3. Riscos já conhecidos

Levantados em 03/10/2026, ao abrir a sprint.

- **Colaboração.** Até o commit `2373015`, o histórico tem commits de `pedrinzz10` (127) e do
  Leonardo (`leojp04` 26 + `leop04` 5). Fabricio, Miguel e Henrique têm
  **zero** (Henrique foi incluído na lista da equipe em 03/10/2026). Os commits da Sprint 4
  precisam vir dos cinco, distribuídos no tempo, e não num push único no fim.
- ~~**A interface avaliada é de outro repositório.**~~ Revisto em 04/10/2026: a nota da Sprint 3
  (34/100) zerou a parte web, e o professor exigiu telas Thymeleaf com login OAuth2. A
  interface avaliada passa a ser este repositório (ver [ADR-002](adr/002-telas-thymeleaf-no-spring.md)).
  O app mobile segue como evidência de integração e precisa rodar contra a API **publicada**.
- **O README diverge do código.** Ele diz que o perfil padrão é `oracle` (o código diz `mysql`,
  em `application.properties`), que a suíte tem 205 testes (são 392) e se contradiz sobre o que
  `spring-boot:run` sem perfil faz. O README completo vale nota (critério 5).
- **O índice de `docs/` está defasado:** o `17-contrato-de-erro.md` não aparece em `docs/README.md`.
- **`LINKS-ENTREGA.txt` ainda é da Sprint 3.**
- **A API está fora do ar.** O Web App da Sprint 3 foi apagado:
  `app-clyvovet-java-rm562312.azurewebsites.net` não resolve no DNS (NXDOMAIN em 03/10/2026).
- **O perfil `mysql` nunca rodou num MySQL real.** Só foi validado em H2 `MODE=MySQL`
  (`MigrationsMySqlTest`). O primeiro boot real pode falhar no `ddl-auto=validate`.
- **Database fica fora do escopo deste repositório** até termos o enunciado oficial da Sprint 4
  de Database. O `specs/04-dependencias-externas.md` fala em procedures chamadas pelo Java, mas
  isso vem de um documento interno de agosto, não do enunciado.

## 4. O que este repositório precisa entregar

Escopo decidido em 03/10/2026: **consolidar** o que existe, sem módulo novo. **Revisto em
04/10/2026**, depois da nota da Sprint 3: entra um módulo novo, a camada web Thymeleaf com login
OAuth2 (L9 e Fase 1b). O deploy sai na
conta Azure do Pedro (os nomes em `azure/00-variaveis.sh` já são dela), mais perto da gravação.

### Lacunas

| # | Lacuna | Pesa em | Evidência |
|---|---|---|---|
| L1 | API fora do ar | 40 pts (demonstração) | NXDOMAIN, ver seção 3 |
| L2 | Perfil `mysql` nunca rodou num MySQL real | risco de o deploy falhar no boot | só H2 `MODE=MySQL` |
| L3 | README diverge do código | 10 pts (organização) | `application.properties:5` diz `mysql`; a suíte tem 392 testes |
| L4 | Índice de `docs/` sem o 17; `LINKS-ENTREGA.txt` da Sprint 3 | 10 pts (organização) | `docs/README.md` |
| L5 | Código não revisado contra as penalidades de boas práticas (−10) e repetição (−5) | penalidades | — |
| L6 | 3 de 5 integrantes sem commit | −10 | `git log` |
| L7 | Sem roteiro do vídeo nem seção de integração multidisciplinar com evidências | 20 + 20 + 10 pts | não existe |
| L8 | Pipeline sem deploy automático, com gatilho só na `main` | DevOps, se o grupo usar o Java lá | `azure-pipelines.yml` |
| L9 | Sem telas web: o professor exigiu Thymeleaf, login OAuth2 (Google/GitHub) e os fluxos na interface | 40 pts (demonstração, "boa UI e UX") | feedback da Sprint 3, nota 34/100 |

### Fases

**Fase 1, não depende de ninguém**
- L3 e L4: corrigir o README e o índice de `docs/`.
- L5: revisão de código (skill `/code-review`). Cada achado é corrigido com teste antes.
  Os achados restantes (2 e 3) ficam para depois da Fase 1b, que também passa pela revisão.

**Fase 1b, camada web (L9)** — ver [ADR-002](adr/002-telas-thymeleaf-no-spring.md)
- Base: duas cadeias de segurança (API stateless com JWT; telas com sessão e CSRF), login por
  formulário, layout e login OAuth2 (Google e GitHub). OAuth2 só autentica e-mail que já tem conta.
- Fluxo do tutor: agendar consulta e cancelar, com formulário e validação.
- Fluxo do veterinário: concluir atendimento e marcar retorno; lista de retornos vencidos.
- README: seção "Aplicação web".
- **Em aberto:** quem implementa cada fluxo. O Leonardo vai perguntar ao professor que evidência
  de colaboração ele espera (ver L6); os fluxos foram desenhados como tarefas independentes.

**Fase 2, local e sem crédito**
- L2: subir o perfil `mysql` contra um MySQL 8 no Docker e confirmar o `validate`.
- Se algo falhar, a correção é migration nova (ver `.claude/rules/migrations.md`).

**Fase 3, na conta do Pedro**
- L1: recriar os recursos com os scripts de `azure/`.
- Apontar o app para a URL pública (`EXPO_PUBLIC_JAVA_BASE_URL`).
- Cadastrar a URL pública como redirect nos apps OAuth do Google e do GitHub.
- Rodar um smoke test dos fluxos principais, pelas telas e pelo app.
- L8: CD no Azure DevOps, se o grupo usar o Java em DevOps.

**Fase 4, narrativa e vídeo**
- L7: ADRs das decisões principais, que viram a narrativa (20 pts).
- Seção "Integração multidisciplinar" no README, com link para cada evidência (20 pts).
- `docs/19-roteiro-do-video-sprint4.md`: até 15 minutos, com os cinco falando.
- `LINKS-ENTREGA.txt` da Sprint 4.

**Contínuo**
- L6: dividir as tarefas por pessoa, para que os cinco commitem na `sprint-4` ao longo do mês.

## 5. Decisões

Cada decisão com motivo não óbvio vira um ADR em [`adr/`](adr/README.md). Os ADRs são o
material bruto da narrativa (critério 2).

| ADR | Decisão |
|---|---|
| [001](adr/001-ui-pelo-app-mobile.md) | ~~A interface avaliada é o app mobile, e não telas no Spring~~ (substituído por 002) |
| [002](adr/002-telas-thymeleaf-no-spring.md) | Telas Thymeleaf no próprio Spring, com login OAuth2 |

## 6. Cronograma

Revisto em 04/10/2026 para incluir a Fase 1b. As datas de 19/10 em diante não mudam.

| Até | Meta |
|---|---|
| 07/10 | Fase 1b, base: duas cadeias de segurança, login por formulário e OAuth2 |
| 12/10 | Fase 1: README corrigido (feito em 04/10) e primeira revisão de código |
| 14/10 | Fase 1b, fluxos: telas do tutor e do veterinário |
| 19/10 | Fase 2: perfil `mysql` validado num MySQL real local |
| **26/10** | **Fase 3: API e telas no ar na conta do Pedro, com o app apontando para ela** |
| 31/10 | Fase 4: roteiro pronto e vídeo gravado, navegando pelas telas Thymeleaf |
| 01–03/11 | Margem para regravar e conferir a entrega |
| **04/11** | Entrega no portal |

Se em 26/10 a API não estiver no ar, todo o resto para até ela subir. Sem deploy, a demonstração
de 40 pontos não existe, e nenhum outro item compensa essa perda.
