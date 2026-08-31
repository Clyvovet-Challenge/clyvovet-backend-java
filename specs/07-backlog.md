# Backlog consolidado — o que está implementado e o que falta

**Verificado em:** 25/08/2026, contra o código da branch `main` (commit `735e49a`).
**Método:** leitura do código, `git shortlog`, `grep` no `pom.xml` e no schema, contagem no seed.
Nada aqui foi marcado por presunção — cada linha tem evidência ou está declarada como não verificável.

> **Este documento não consta nos PDFs do Challenge.** É o backlog derivado das specs 01–04,
> reconciliado com o estado real do repositório e acrescido dos dois módulos decididos na
> mentoria presencial da Clyvo em **21/08/2026**.
>
> **Ele substitui o status** de [05-plano-de-implementacao.md](05-plano-de-implementacao.md)
> e [06-checklist-pre-sprint-3.md](06-checklist-pre-sprint-3.md), que são fotografias de
> 07/08/2026 e hoje contradizem o código em vários pontos. Os dois seguem no repositório
> como registro histórico.

---

## Onde estamos

| | |
|---|---|
| **Hoje** | 25/08/2026 |
| **Entrega Sprint 3** | 12/09/2026 — **18 dias** |
| **Entrega Sprint 4** | 04/11/2026 — **71 dias** |
| Pontos em aberto na Sprint 3 | **50 de 100** (frontend 30 + fluxos não-CRUD 20) |
| Pontos em aberto na Sprint 1/2 | até **20** (coleção Insomnia + maturidade REST) |

> ⚠️ **O aperto real:** faltam 18 dias para a Sprint 3 com 50 pontos parados em 0%, e os dois
> módulos novos da mentoria são maiores que essa janela. A [Parte 6](#6--sequenciamento-proposto)
> propõe o que cabe antes de 12/09 e o que vai para a Sprint 4 — a alternativa é chegar na
> entrega com os dois módulos pela metade e os 50 pontos ainda abertos.

---

## 1 — O que JÁ está implementado

Consolidado para não se repetir adiante. Tudo verificado no código em 25/08/2026.

### Sprint 1 e 2 — entregue

| Requisito | Evidência |
|---|---|
| 6 entidades JPA + `Endereco` `@Embeddable` + 6 enums | `model/` |
| Bean Validation | 7 Requests anotados, `@Valid` em todo POST/PUT/PATCH |
| Paginação e ordenação | `@PageableDefault` nos 6 GETs de lista; envelope `content` + `page` |
| Busca com parâmetros | 6 `buscarPorFiltros` em JPQL, 2 filtros cada |
| Cache | Caffeine com TTL e teto, `@Cacheable`/`@CacheEvict` nos 6 services |
| Tratamento de erros | `GlobalExceptionHandler` — 400, 404, 409, 401 |
| DTOs | Request (classe Lombok) + Response (`record`), entidade nunca exposta |
| Swagger | springdoc, `@Tag`/`@Operation` em 100% dos endpoints |

### Sprint 3 — 50 de 100 pontos

| Requisito | Pts | Situação |
|---|---|---|
| **Flyway** | 20 | ✅ **completo** — V1–V4 em **dois conjuntos** (`oracle/` e `mysql/`), `ddl-auto=validate` |
| **Spring Security** | 30 | ✅ **completo e acima do mínimo** — JWT (access 15 min + refresh 7 dias com revogação), 3 perfis, ownership em 3 frentes, lockout de conta, rate limit por IP, headers de hardening |
| Frontend | 30 | ❌ **0%** |
| 2 fluxos não-CRUD | 20 | ❌ **0%** |

### Ganhos fora da rubrica, obtidos depois da auditoria de 07/08

| Item | Situação em 07/08 | Hoje |
|---|---|---|
| Versionamento da API | rotas na raiz | ✅ `/api/v1` via `WebConfig`, com PATCH em todos os recursos |
| Testes | 1 teste, que falhava sem Oracle | ✅ **≈127 `@Test`**, perfil `dev` fixo, base `TesteDeApi` |
| Seed mínimo da disciplina de Database | 3 tabelas abaixo de 5 registros | ✅ **todas ≥ 5** (clinica 5, tutor 5, vet 7, animal 6, evento 11, pagamento 8) |
| Credenciais no código | usuário e senha em texto puro | ✅ lidas do ambiente (**mas ver item E1**) |
| Filtros por texto | nunca casavam (`ESCAPE ''`) | ✅ corrigido, com regressão em `FiltrosDeBuscaTest` |
| Duplicidade de CPF/CNPJ/CRMV | 500 | ✅ 409 no handler |
| `@Transactional` | ausente | ✅ nos 7 services |
| Colaboração no histórico | **1 autor** em 29 commits | ⚠️ **2 autores** em 58 commits (pedrinzz10 29 · leojp04 29) — melhor, ainda **2 de 4** |
| Repositório | `leojp04/clyvovet-backend-java` | ✅ organização `Clyvovet-Challenge/clyvovet-backend-java` |

---

## 2 — O que FALTA, por origem

Cada item traz o que é, quanto vale, e a evidência de que está aberto.

### A — Sprint 1/2: pontos ainda recuperáveis

| # | Item | Vale | Evidência de que está aberto |
|---|---|---|---|
| ~~A1~~ | **Coleção Insomnia/Postman em `documentos/`** | até 10 pts | ✅ **fechado em 31/08/2026** — `documentos/clyvovet-api.postman_collection.json`, 71 requisições em 12 pastas. Formato Postman v2.1, que o Insomnia importa sem perda |
| ~~A2~~ | **HATEOAS** — nível 3 de maturidade de Richardson | parte de 15 pts | ✅ **fechado em 31/08/2026** — links **condicionais ao estado** em animal e evento clínico. Um evento `AGENDADO` traz `cancelar` e `concluir`; `CANCELADO` não traz nenhum dos dois |
| A3 | Coerência entre Diagrama de Classes e o DER da disciplina de Database | até 10 pts | não verificável neste repositório — depende do artefato entregue em Database |

> ~~A1 é o melhor custo/benefício aberto no projeto inteiro.~~ **A1 e A2 foram fechados em
> 31/08/2026.** Resta A3, que não é verificável neste repositório.
>
> Sobre A2, vale registrar o que foi feito: nível 3 de Richardson não é "adicionar um link
> `self`". O que entrou foram **links que mudam com o estado do recurso** — o cliente descobre
> pela própria resposta o que pode fazer agora, em vez de carregar por fora uma cópia da
> máquina de estados que envelhece em silêncio quando a regra do servidor muda.

### B — Sprint 3: os 50 pontos que faltam

| # | Item | Vale | Situação |
|---|---|---|---|
| B1 | **Frontend / camada de visualização** | 30 pts | Nenhuma dependência de Thymeleaf no `pom.xml`, sem `templates/`, sem `static/`. A spec 02 registra "adiado por decisão do time" — **a decisão precisa ser revista ou o item é perda certa de 30 pontos** |
| B2 | **Dois fluxos completos, exceto CRUD** | 20 pts | Nenhum fluxo de negócio existe. Os 7 services são CRUD estrutural idêntico. Os fluxos **nem foram escolhidos** — é a decisão 2 da spec 05, aberta desde 07/08 |
| B3 | Validações básicas nos formulários | parte de B1 | depende de B1 |
| B4 | Vídeo de demonstração ≤ 10 min | requisito de entrega | — |
| B5 | README com instalação, execução e acesso | requisito de entrega | o README atual é bom; precisa refletir o estado final |

> **B1 e B2 são a Sprint 3 inteira do que falta.** Tudo o mais listado neste documento é
> valioso, mas não substitui esses 50 pontos.

### C — Sprint 4

| # | Item | Vale | Situação |
|---|---|---|---|
| C1 | **Aplicação rodando online** por URL pública no dia da avaliação | parte de 40 pts | `deploy.sh` existe (Azure CLI); nada publicado e verificado hoje |
| C2 | **Pipeline CI no Azure DevOps** — build + **execução de testes** + publicação do artefato | DevOps S4 | `ls .github azure-pipelines.yml` → não existe. O `Dockerfile` ainda usa `-DskipTests` |
| C3 | **Pipeline CD** — deploy automático em Azure Web App ou ACI | DevOps S4 | idem |
| C4 | Gatilho do CI na branch — a spec cita `master`, o repositório usa `main` | DevOps S4 | confirmar qual o Azure DevOps espera |
| C5 | **Procedures PL/SQL chamadas pela aplicação**, demonstradas em vídeo | Database S4 | `grep -r "@Procedure\|StoredProcedureQuery"` → ausente. Hoje só JPQL |
| C6 | **IA integrada à aplicação** | Disruptive S4 | coberto pelo **Módulo 2** (Parte 4) |
| C7 | Narrativa da solução — decisões e justificativas | 20 pts | a base documental em `docs/` já sustenta isso |
| C8 | Integração multidisciplinar com evidências | 20 pts | reunir artefatos de Mobile, Database, DevOps, Disruptive |
| C9 | Vídeo ≤ 15 min **com os 4 integrantes** | 10 pts | — |
| C10 | Boa UI e UX | parte de 40 pts | depende de B1 |

### D — Dependências externas (specs 04)

| # | Item | Origem | Situação |
|---|---|---|---|
| D1 | **`script_bd.sql`** — DDL em arquivo separado, com estrutura e comentários | DevOps S3 | `find . -name "script_bd*"` → **ausente**. As migrations existem, mas o requisito pede este arquivo |
| D2 | Banco em nuvem (H2 não é aceito) | DevOps S3 | decidido: **MySQL no Azure**. Perfil `mysql` pronto — **nunca executado contra um MySQL real** (item E5) |
| D3 | **Avisar Mobile e Frontend da mudança de contrato** (`/api/v1` + `page.totalElements`) | Backend, 19/08 | registrado na spec 04 como conflito 8; **o documento sozinho não avisa ninguém** |
| D4 | Autenticação do app mobile por esta API | Mobile S3 | ✅ JWT pronto e documentado — falta o mobile consumir |
| D5 | Container sem privilégio de root | DevOps S3 | ✅ usuário `appuser` no `Dockerfile` |

### E — Pendências técnicas abertas (docs/07)

| # | Item | Severidade | Situação |
|---|---|---|---|
| E1 | **Senha do Oracle no histórico do Git** | **Alta** | código e docs limpos; **falta trocar a senha no portal da FIAP**. Único jeito de fechar |
| E2 | `dataPagamento` obrigatória impede registrar pagamento `PENDENTE` | Média | confirmado aberto: `@NotNull` ainda em `PagamentoRequest:30`. O próprio seed grava pendentes com data nula |
| E3 | Cache não invalida entre entidades relacionadas | Média | renomear um tutor deixa `GET /animais` devolvendo o nome antigo por até 10 min |
| E4 | `especie` e `porte` como texto livre | Baixa | `CAO` e `CACHORRO` convivem; quebra o filtro. **Vira alta com o Painel** — ver risco R3 da spec dos módulos |
| E5 | Perfil `mysql` nunca rodou contra um MySQL real | Média | validado só em H2 `MODE=MySQL`; `ddl-auto=validate` no servidor real é o teste que importa |
| E6 | `EventoClinico.hora` como `String`; ordem de campos de `Endereco` divergente | Baixa | resíduo do item 15 |
| E7 | Verificar `ESCAPE '\'` no Oracle real | Baixa | `EscapeNoOracleTest` existe e fica pulado sem `DB_USERNAME` no ambiente |

### F — Processo e colaboração

| # | Item | Situação |
|---|---|---|
| F1 | **Commits distribuídos entre os 4 integrantes** | 2 de 4 aparecem no histórico (29 + 29 em 58 commits). −10 pts na S4 por ausência de evidência de colaboração; **não é corrigível retroativamente** |
| F2 | Avaliação oral individual (a partir da S3) | cada integrante precisa defender o que escreveu — reforça dividir por fluxo, não por camada |
| F3 | Declarar uso de IA na avaliação oral | exigência explícita da disciplina |

---

## 3 — Módulo novo: Painel do Veterinário

Decidido na mentoria Clyvo de **21/08/2026**. Especificação técnica completa em
[`../SPEC_PAINEL_VETERINARIO_E_DADOS_IA.md`](../SPEC_PAINEL_VETERINARIO_E_DADOS_IA.md) —
entidades, DDL nos dois bancos, contratos de endpoint e regras de cálculo.

Dashboard analítico do veterinário sobre a própria clínica: volume por raça, desfecho por
raça, gasto com medicamento, benchmark regional, score de risco de abandono por pet, taxa
de retorno e "tempo de vida ganho".

### O achado que muda o planejamento

O módulo foi descrito como analítico — "cruza dados que já existem". **O schema não sustenta
isso.** Das 12 necessidades de dado levantadas, **1 é atendida hoje**:

| Falta no schema | Impede |
|---|---|
| status de óbito e `data_obito` no `animal` | taxa de óbito, sobrevida, tempo de vida ganho |
| status no `evento_clinico` (agendado/realizado/faltou) | taxa de retorno, retenção, score de risco |
| vínculo retorno → consulta de origem | saber o que venceu |
| **qualquer tabela de receita, prescrição ou medicamento** | gasto com medicamento por raça |
| diagnóstico estruturado (só existe `descricao` texto livre) | recorte por patologia |
| expectativa de vida por raça | tempo de vida ganho |
| peso do animal | validação de dose (Módulo 2) |

Além disso, `animal.raca` é texto livre sem catálogo — o eixo central do painel agrupa
"Golden Retriever" e "golden retriever" separadamente (é o item E4 acima, que deixa de ser
"baixo" no momento em que o painel existir).

**Consequência:** antes do painel existe uma rodada de **captura de dado**. Um painel
construído antes disso mostraria zeros ou números inventados.

### Tarefas

| # | Tarefa | Depende de |
|---|---|---|
| P0 | `UsuarioAutenticado.getVeterinarioId()` + `SegurancaService.veterinarioIdParaFiltro()` e `podeAcessarPainelDoVeterinario`, com teste no padrão `OwnershipTest` | — |
| P1 | **Migration V5** — `status_evento`, `data_retorno_previsto`, `evento_origem_id`, `peso_kg` + índices, nos **dois** conjuntos | decisões de negócio (quem marca o status) |
| P2 | **Migration V6** — `status_vital`, `data_obito`, `causa_obito` no animal + tabela `raca_referencia` com CRUD | decisão sobre quem registra óbito |
| P3 | Painel — fatia vertical: resumo, atendimentos por raça, retenção (endpoints 1, 2, 5) | P0–P2 |
| P4 | Painel — desfecho por raça, score de risco, impacto de vida (endpoints 3, 6, 7) + `CalculadoraDeRisco` com teste unitário | P2 |
| P5 | Painel — benchmark regional com k-anonimato (endpoint 8) | P3 |
| P6 | Painel — gasto com medicamento por raça (endpoint 4) | **Módulo 2 / I1** |
| P7 | Seed analítico determinístico para `dev`/`h2` + testes das agregações | P1–P2 |

> **P0 é bloqueante e não negociável.** `UsuarioAutenticado` expõe `getTutorId()` mas **não**
> `getVeterinarioId()`. Sem isso, o painel do vet A serve dados do vet B — exatamente o
> vazamento entre contas que o projeto já corrigiu uma vez para tutores.

---

## 4 — Módulo novo: Dados + IA

Também decidido em 21/08/2026. Camada de apoio à decisão clínica: casos semelhantes no
histórico, cluster de patologias por raça/idade e validação cruzada de medicação.
**A IA nunca decide — sugere, e o veterinário assina.**

Atende diretamente **C6** (Disruptive Architectures S4: "implementar a IA integrada à
aplicação").

### Tarefas

| # | Tarefa | Depende de |
|---|---|---|
| I1 | **Migration V7** — `patologia`, `diagnostico`, `medicamento`, `prescricao`, `item_prescricao`, `restricao_medicamento` + CRUDs no padrão do projeto | **pergunta bloqueante abaixo** |
| I2 | Cluster de patologias (regra de associação: suporte/confiança/lift) — **SQL puro, sem LLM** | I1 |
| I3 | Validação de medicação, **camada 1 determinística** sobre `restricao_medicamento` | I1 + `peso_kg` (P1) |
| I4 | **Migration V8** — `sugestao_ia` e `sugestao_ia_decisao` (rastreabilidade) | I1 |
| I5 | `AnonimizadorClinico` — nenhum dado pessoal de tutor, pet, vet ou clínica sai da aplicação | — |
| I6 | `ClienteIa` no pacote novo `integration/` — timeout, retry, interruptor `clyvovet.ia.habilitada`, faixa própria no `RateLimitFilter` | I4, I5 |
| I7 | Casos semelhantes — recuperação SQL determinística + LLM só para redigir | I1, I6 |
| I8 | Validação de medicação, **camada 2 (LLM)** — aditiva, nunca remove alerta da camada 1 | I3, I6 |

> **Pergunta que bloqueia I1:** o enunciado diz que a API .NET, **no mesmo banco Oracle**,
> já trata "produtos e sugestões de produtos". Se `produto` já contém medicamentos, criar
> `medicamento` duplica catálogo — e como as duas APIs se integram **só pelo banco**, a
> divergência apareceria como dado inconsistente, não como erro. **Precisa ser respondida
> antes da primeira linha de migration.**

> **Por que I2 e I3 vêm antes de I6–I8:** toda a parte estatística roda sem provedor externo,
> sem custo e sem risco de alucinação — e é a que sustenta a demonstração. O LLM entra por
> último, como camada aditiva sobre uma base que já funciona sozinha. Se ele falhar, atrasar
> ou ficar caro, nada do que foi entregue antes deixa de funcionar.

---

## 5 — A conexão que resolve os 20 pontos parados

**B2 (dois fluxos completos, exceto CRUD) está em 0% desde 07/08 porque os fluxos nunca
foram escolhidos.** Os módulos novos oferecem candidatos naturais — mas com uma ressalva
que vale registrar antes de apostar neles.

| Candidato | Serve como "fluxo completo"? | Leitura |
|---|---|---|
| **Validação cruzada de medicação** (I3) | ✅ **sim, sem ambiguidade** | entrada (prescrição) → processamento com regra real (espécie, dose por peso, interação, contraindicação) → saída (parecer com severidade e decisão registrada). Atravessa 5 entidades |
| **Controle de retorno** (habilitado por P1) | ✅ **sim** | evento concluído → calcula retorno previsto, detecta vencidos, produz lista de pets em atraso → ação. É o "agendamento de retorno" já sugerido na spec 05, agora com o schema que ele precisava |
| **Painel do Veterinário** (P3–P5) | ⚠️ **parcial** | tem regra de negócio real (score, taxas, k-anonimato), mas um avaliador pode lê-lo como **relatório**, não como fluxo |
| Cluster de patologias (I2) | ⚠️ parcial | mesma ressalva do painel |

**Recomendação:** apostar os 20 pontos em **controle de retorno** + **validação de medicação**.
São inequivocamente fluxos, atendem ao tema do Challenge (continuidade do cuidado, saindo do
modelo episódico) e cada um cabe em um integrante ponta a ponta — o que ataca F1 e F2 de
quebra. O Painel entra como a **prova** desses fluxos, não como um deles.

---

## 6 — Sequenciamento proposto

### Até 12/09 — Sprint 3 (18 dias)

Prioridade absoluta nos 50 pontos abertos. Os módulos novos entram **só na medida em que
sustentam B2**.

| Ordem | O quê | Por quê agora |
|---|---|---|
| 1 | **A1** — exportar e commitar a coleção Insomnia | 10 pts por 30 min de trabalho |
| 2 | **Decidir B1** (frontend) e **B2** (os dois fluxos) | 50 pontos travados nessa decisão desde 07/08 |
| 3 | **P0** + **P1** (V5: status, retorno previsto, peso) | destrava o fluxo de controle de retorno e o painel inteiro |
| 4 | **B2, fluxo 1** — controle de retorno | 20 pts, sobre a V5 |
| 5 | **I1 parcial** + **I3** — medicamento, prescrição, restrição e validação determinística | **B2, fluxo 2** — sem depender de LLM nenhum |
| 6 | **B1** — frontend das telas dos dois fluxos + login + painéis básicos | 30 pts |
| 7 | **D1** (`script_bd.sql`) e **E2** (`dataPagamento`) | baratos, cobram nota em outra disciplina |
| 8 | **B4/B5** — vídeo ≤ 10 min e README | requisito de entrega |

### Até 04/11 — Sprint 4 (71 dias)

| Ordem | O quê |
|---|---|
| 1 | **C2/C3/C4** — pipeline CI/CD no Azure DevOps, com testes rodando (tirar o `-DskipTests`) |
| 2 | **E5** — subir o perfil `mysql` contra um MySQL 8 real e confirmar o `validate` |
| 3 | **C1** — aplicação online e estável, URL pública verificada |
| 4 | **P2 → P7** — Painel do Veterinário completo |
| 5 | **I4 → I8** — rastreabilidade e camada LLM (**C6**, Disruptive S4) |
| 6 | **C5** — procedures PL/SQL chamadas pela aplicação (Database S4) |
| 7 | **A2** — HATEOAS, se sobrar janela |
| 8 | **C7/C8/C9** — narrativa, evidências multidisciplinares, vídeo com os 4 integrantes |
| 9 | **E1** — trocar a senha do Oracle no portal da FIAP |

### Fora de sequência, contínuo

| Item | Por quê |
|---|---|
| **F1** — commits distribuídos entre os 4 integrantes | vale −10 pts na S4 e **não é recuperável depois**. Se dividir por fluxo (item 5 da Parte 5), resolve junto com F2 |
| **D3** — avisar Mobile e Frontend da mudança de contrato | quanto mais tarde, mais retrabalho do outro lado |

---

## 7 — Decisões que travam trabalho

Herdadas da spec 05, atualizadas. As três primeiras bloqueiam código.

| # | Decisão | Situação | Trava |
|---|---|---|---|
| 1 | **Quais 2 fluxos não-CRUD** | **aberta desde 07/08** | 20 pts + telas + testes |
| 2 | **Frontend: fazer ou abrir mão dos 30 pts** | "adiado por decisão do time" | 30 pts. Se for abrir mão, é melhor saber agora e realocar o esforço |
| 3 | **Tabelas do lado .NET no Oracle compartilhado** | não respondida | migration V7 inteira (I1) |
| 4 | Quem marca `status_evento`, e quando | não respondida | P1 — o campo nasce despovoado sem isso |
| 5 | Quem registra o óbito do animal | não respondida | P2 |
| 6 | Provedor de LLM, e quem paga | não respondida | I6 |
| 7 | Tecnologia do frontend (Thymeleaf × SPA) | depende da decisão 2 | B1 |
| ~~8~~ | ~~Banco em nuvem~~ | ✅ resolvida — MySQL no Azure | — |
| ~~9~~ | ~~Autenticação do mobile~~ | ✅ resolvida — JWT desta API | — |
| ~~10~~ | ~~Modelo de usuário~~ | ✅ resolvida — entidade `Usuario` + `Perfil` | — |

A lista completa de perguntas em aberto dos dois módulos novos (34 delas, agrupadas por
tema) está na Parte VIII de
[`../SPEC_PAINEL_VETERINARIO_E_DADOS_IA.md`](../SPEC_PAINEL_VETERINARIO_E_DADOS_IA.md).

---

## 8 — Riscos

| Risco | Efeito | Mitigação |
|---|---|---|
| **Módulos novos consumirem a janela da Sprint 3** | 50 pts perdidos por trabalho que só é cobrado na S4 | Sequenciamento da Parte 6: na S3, dos módulos entra só o que sustenta B2 |
| **Decisão 1 continuar aberta** | Fluxos, telas e testes comprimidos na última semana | Fechar nesta semana — já custou 18 dias |
| Frontend seguir adiado sem decisão explícita | 30 pts perdidos por omissão, não por escolha | Decidir e registrar; se for abrir mão, realocar para B2 e C |
| Colisão de tabelas com a API .NET | Migration refeita ou catálogo duplicado em silêncio | Responder a decisão 3 antes de I1 |
| `raca` texto livre | Painel exibe grupos duplicados e percentuais errados | `raca_referencia` na P2; paliativo `UPPER(TRIM())` enquanto isso |
| Trabalho concentrado em 2 dos 4 integrantes | −10 pts na S4 + avaliação oral individual fraca | Dividir por fluxo, um dono ponta a ponta |
| LLM indisponível na demonstração | Fluxo quebra na banca | Camada determinística responde sozinha (I2/I3); LLM é aditivo |
| MySQL real só na reta final | Deploy quebra no `validate` | E5 no início da Sprint 4, não no fim |

---

## Referências

| Assunto | Documento |
|---|---|
| Requisitos oficiais por sprint | [01](01-sprint-1-2.md) · [02](02-sprint-3.md) · [03](03-sprint-4.md) |
| Exigências das outras disciplinas | [04-dependencias-externas.md](04-dependencias-externas.md) |
| Backlog original da Sprint 3 (**histórico**, 07/08/2026) | [05-plano-de-implementacao.md](05-plano-de-implementacao.md) |
| Auditoria pré-Sprint 3 (**histórico**, 07/08/2026) | [06-checklist-pre-sprint-3.md](06-checklist-pre-sprint-3.md) |
| Spec técnica dos dois módulos novos | [`../SPEC_PAINEL_VETERINARIO_E_DADOS_IA.md`](../SPEC_PAINEL_VETERINARIO_E_DADOS_IA.md) |
| Pendências técnicas detalhadas | [`../docs/07-pendencias-e-divergencias.md`](../docs/07-pendencias-e-divergencias.md) |
| Estado do projeto e roadmap de produto | [`../docs/09-estado-do-projeto.md`](../docs/09-estado-do-projeto.md) |
