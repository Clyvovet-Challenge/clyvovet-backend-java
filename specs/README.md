# Specs — Java Advanced · Challenge FIAP 2026

Requisitos da disciplina **JAVA ADVANCED** extraídos dos documentos oficiais do
Challenge 2026 — 2º ano ADS, turmas de Fevereiro.

Estas specs cobrem **apenas Java Advanced**. As demais disciplinas (DevOps, Mobile,
Database, Compliance/QA, Disruptive Architectures) aparecem só onde criam uma
dependência direta sobre o backend — ver [04-dependencias-externas.md](04-dependencias-externas.md).

---

## Índice

| Documento | Conteúdo |
|---|---|
| [01-sprint-1-2.md](01-sprint-1-2.md) | Requisitos da entrega do 1º semestre (Sprints 1 e 2) + status no repositório |
| [02-sprint-3.md](02-sprint-3.md) | Frontend, Flyway e Spring Security |
| [03-sprint-4.md](03-sprint-4.md) | Entrega final: consolidação, deploy e apresentação |
| [04-dependencias-externas.md](04-dependencias-externas.md) | O que outras disciplinas exigem deste backend |
| [05-plano-de-implementacao.md](05-plano-de-implementacao.md) | Backlog da Sprint 3 — **fotografia de 07/08/2026, histórico** *(não consta no PDF)* |
| [06-checklist-pre-sprint-3.md](06-checklist-pre-sprint-3.md) | Auditoria pré-Sprint 3 — **fotografia de 07/08/2026, histórico** *(não consta no PDF)* |
| **[07-backlog.md](07-backlog.md)** | **O que está implementado e o que falta — backlog consolidado e vigente** *(não consta no PDF)* |
| **[08-modelo-de-negocio.md](08-modelo-de-negocio.md)** | **O produto que queremos — visão do time confrontada com o código, e as regras a implementar** *(não consta no PDF)* |

> **Comece pelo [07-backlog.md](07-backlog.md).** Ele reconcilia as specs 01–04 com o
> estado real do código em 25/08/2026 e reúne, num lugar só, tudo o que falta — incluindo os
> dois módulos decididos na mentoria de 21/08. Os documentos 05 e 06 seguem no repositório
> como registro histórico, mas o status deles está vencido.
>
> O **[08-modelo-de-negocio.md](08-modelo-de-negocio.md)** é o par dele: onde o 07 diz de onde
> partimos, o 08 diz para onde vamos. Ele descreve o produto — tutor que agenda, consentimento
> do tutor para o veterinário ler o histórico, documentos externos, licença da clínica — e
> marca, item por item, o que o código já sustenta e o que falta.

Documentação técnica do que já existe: [`../docs/`](../docs/).
Spec técnica dos módulos novos: [`../SPEC_PAINEL_VETERINARIO_E_DADOS_IA.md`](../SPEC_PAINEL_VETERINARIO_E_DADOS_IA.md).

---

## Cronograma

| Data | Evento | Responsável |
|---|---|---|
| 16/04 | Abertura do Challenge (kickoff) | Clyvo |
| XX/05 | Mentoria online | Clyvo |
| **24/05** | **Entrega Sprints 1 e 2** | Aluno |
| até 07/06 | Feedback Sprints 1 e 2 | Professores |
| 21/08/2026 | Mentoria Clyvo — presencial | Clyvo |
| **12/09/2026** | **Entrega Sprint 3** | Aluno |
| 26/09/2026 | Feedback Sprint 3 | Professores |
| Setembro/2026* | Pré-banca de professores — seleção dos projetos | Professores |
| Setembro/2026* | Apresentação banca final | Clyvo |
| 24/10/2026 | NEXT | FIAP |
| **04/11/2026** | **Entrega Sprint 4** | Aluno |
| 11/11/2026 | Feedback Sprint 4 | Professores |

\* Datas a divulgar pelo Teams.

Entrega fora do prazo ou fora do portal na Sprint 4: **−100 pontos**.

---

## Status por sprint

Verificado no código em **25/08/2026**. Detalhamento e evidência em [07-backlog.md](07-backlog.md).

| Sprint | Entrega | Peso Java | Status |
|---|---|---|---|
| 1 e 2 | 24/05 | 100 pts | Entregue — API REST com 6 entidades. Até 20 pts ainda recuperáveis (coleção Insomnia, HATEOAS) |
| 3 | 12/09/2026 | 100 pts | **50/100** — ✅ Flyway (20) e Spring Security (30); ❌ frontend (30) e 2 fluxos não-CRUD (20) |
| 4 | 04/11/2026 | 100 pts | Não iniciado — sem CI/CD, sem deploy verificado, sem procedures chamadas pela aplicação |

Além da rubrica, dois módulos foram decididos na mentoria de 21/08/2026 —
**Painel do Veterinário** e **Dados + IA** — especificados em
[`../SPEC_PAINEL_VETERINARIO_E_DADOS_IA.md`](../SPEC_PAINEL_VETERINARIO_E_DADOS_IA.md) e
sequenciados em [07-backlog.md](07-backlog.md).

---

## Regra que atravessa todas as sprints

> "A implementação **apenas de operações de CRUD não será considerada suficiente**
> para resolver de forma eficaz o problema proposto neste Challenge."
> — Java Advanced, Sprint 1

Esse é o critério mais estruturante da disciplina e vale desde a primeira entrega.
O estado atual do repositório é CRUD puro sobre seis entidades: seis controllers
idênticos, nenhum fluxo de negócio. A Sprint 3 formaliza a cobrança ao exigir
"pelo menos dois fluxos completos do sistema (**exceto CRUD**)".

O tema do Challenge é **continuidade do cuidado e engajamento na jornada de saúde do
pet** — sair do modelo episódico/reativo para um contínuo, preventivo e integrado.
Fluxos que atendam a isso são o que diferencia a nota.

---

## Regras gerais do Challenge (aplicáveis ao Java)

| Regra | Detalhe |
|---|---|
| Times | Máximo 5 integrantes; desenvolvimento individual não é permitido |
| Repositório | GitHub **público**, professores com acesso |
| Histórico Git | Deve refletir construção real e evolução gradual |
| Avaliação oral | Individual, em sala, a partir da Sprint 3 |
| Uso de IA | Deve ser declarado na avaliação oral |
| Plágio | Nota zero |
