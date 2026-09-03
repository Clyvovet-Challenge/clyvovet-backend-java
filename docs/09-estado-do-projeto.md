# Estado do projeto

Snapshot de **25/08/2026**: o que está construído, o quanto disso serve à tese do
produto, o que falta e em que ordem atacar.

Escrito para conversa — mentoria, alinhamento com as outras disciplinas, retomada
depois de uma pausa. Para a referência técnica de cada parte, siga os links.

> Este documento **envelhece**. Os números vêm da suíte e do código em 25/08/2026;
> confira antes de citá-los em outro contexto.

> **O que mudou desde o snapshot anterior (19/08):** o código não mudou — a revisão
> `/api/v1`, o PATCH e a estabilização da paginação já estavam entregues. O que mudou foi
> a **direção**: a mentoria presencial da Clyvo em **21/08/2026** acrescentou dois módulos
> ao escopo deste backend. Ver a [seção 5](#5-o-que-a-mentoria-de-2108-acrescentou).

---

## Em três linhas

A espinha dorsal está de pé: histórico clínico do pet que **atravessa clínicas**, com
acesso seguro e isolado do tutor, sobre uma API testada e pronta para deploy.

O que ainda não existe é justamente a parte que ataca o **absenteísmo** — o sistema
registra o que aconteceu, mas não sabe o que *deveria* ter acontecido e não aconteceu.

A distância entre uma coisa e outra é menor do que parece: três campos e um canal de
aviso.

---

## 1. O que está construído

### Domínio

```
Tutor ──< Animal ──< EventoClinico >── Veterinario ──> Clinica
                          │
                          └──< Pagamento
```

Seis entidades de domínio, mais `Usuario` (identidade) e `Endereco` (embutido).
Detalhe em [02-modelo-de-dados.md](02-modelo-de-dados.md).

### API

| | |
|---|---|
| Rotas | **74**, todas sob `/api/v1` — 36 de CRUD, 6 de autenticação e 32 dos fluxos |
| Operações por recurso | listar, buscar, criar, substituir (PUT), alterar parcialmente (PATCH), remover |
| Consulta | paginação, ordenação e filtros próprios de cada recurso |
| Contrato | versionado na URL; listagens com envelope estável (`content` + `page`) |

Inventário completo em [00-funcionalidades.md](00-funcionalidades.md); contratos em
[03-api-rest.md](03-api-rest.md).

### Segurança

| | |
|---|---|
| Autenticação | JWT — access de 15 min, refresh de 7 dias com revogação real no logout |
| Perfis | TUTOR · VETERINARIO · ADMIN |
| Ownership | um tutor só enxerga os próprios pets — verificado em 3 frentes |
| Força bruta | bloqueio de conta (5 falhas / 15 min) + rate limit por IP |
| Senhas | hash BCrypt, nunca devolvidas em resposta |

Detalhe em [08-seguranca.md](08-seguranca.md).

### Qualidade e infraestrutura

| | |
|---|---|
| Testes | **274**, cobrindo CRUD, filtros, PATCH, JWT, sessão, ownership, rate limit e migrations |
| Schema | versionado por Flyway, um conjunto de migrations por banco |
| Bancos | H2 (dev e container), Oracle 19c (entrega), MySQL (alvo do deploy) |
| Deploy | Docker + docker-compose; script Azure CLI idempotente |

---

## 2. O que disso já serve à tese

A aplicação existe para **reduzir o absenteísmo no cuidado com o pet**. Quatro coisas
já construídas jogam a favor disso — e as duas do meio são as menos óbvias.

**A linha do tempo do cuidado já existe.** O `EventoClinico` registra CONSULTA,
RETORNO, VACINA, EXAME e CIRURGIA com data. É a matéria-prima: sem histórico não há
como falar em falha de continuidade.

**O histórico segue o pet, não a clínica.** Clínica e veterinário são entidades
separadas do animal. Se o tutor trocar de clínica, o histórico continua íntegro — o
que é exatamente *continuidade do cuidado*, e o que um sistema de clínica isolada não
consegue oferecer.

**A identidade do tutor já está ligada ao domínio.** O `Usuario` aponta para o
`Tutor`, e o isolamento é garantido. Um app voltado ao tutor pode ser construído sobre
isso sem retrabalho de base.

**A base aguenta.** 274 testes, segurança real e schema versionado significam que a
lacuna do projeto é de **direção de produto**, não de capacidade de execução.

---

## 3. O que falta

Quatro lacunas, em ordem de dependência — cada uma destrava a seguinte.

| # | Falta | O que fica impossível hoje |
|---|---|---|
| 1 | `status` no `EventoClinico` | distinguir **agendado / realizado / faltou** |
| 2 | vínculo "este atendimento gera retorno em X" | saber o que **venceu** |
| 3 | filtro por intervalo de data nos eventos | perguntar "o que está atrasado" |
| 4 | canal de aviso ao tutor | **agir** sobre o atraso |

> O contraste que resume tudo: o `Pagamento` **tem** `statusPagamento`; o
> `EventoClinico` **não tem** status nenhum. O sistema sabe dizer se a clínica
> recebeu, não sabe dizer se o pet foi cuidado.

Confirmado no código em 19/08/2026: `EventoClinico` tem `data`, `hora`, `descricao`,
`tipoEvento` e os três vínculos — e nada mais. A busca de eventos filtra por
`tipoEvento`, `animalNome` e `tutorId`, sem recorte de data.

---

## 4. Caminho de evolução

### Etapa 1 — tornar o problema mensurável

Um enum `StatusEvento` (`AGENDADO` · `REALIZADO` · `FALTOU` · `CANCELADO`), uma
migration em cada banco, o campo no DTO e no filtro.

Trabalho pequeno e mecânico — mesma forma da mudança que introduziu o PATCH.
**Sem esta etapa, nada mais tem significado**, porque não existe número de partida.

*Pergunta que precisa ser respondida antes:* na rotina da clínica, **quem** marca esse
status, e em que momento?

### Etapa 2 — saber o que venceu

Filtro por intervalo de data na busca de eventos, mais o vínculo do retorno esperado.
Habilita a consulta de que o produto inteiro depende: *quais pets estão com cuidado
atrasado?*

*Pergunta:* o retorno é agendado **no ato** da consulta, ou o tutor liga depois?

### Etapa 3 — agir

Rotina agendada + canal de aviso ao tutor. **É aqui que o produto deixa de ser CRUD.**

É também a única etapa cuja decisão não é técnica: qual canal, com que frequência,
dizendo o quê. Depende de validação com quem conhece a operação.

### Etapa 4 — provar

Taxa de comparecimento por clínica e por período. Só faz sentido depois que as etapas
1 a 3 rodarem por um tempo e houver dado acumulado.

### Resumo

| Etapa | Natureza | Destrava |
|---|---|---|
| 1. Status do evento | mecânica, pequena | medir |
| 2. Data e retorno esperado | média | detectar atraso |
| 3. Aviso ao tutor | decisão de produto + integração externa | agir |
| 4. Métricas de adesão | consulta agregada | provar |

As etapas 1 e 2 são executáveis com o que já existe. A 3 é a que precisa ser
destravada por fora.

---

## 5. O que a mentoria de 21/08 acrescentou

Dois módulos, ambos deste lado Java. Especificação técnica completa — entidades, DDL nos
dois bancos, contratos de endpoint e regras de cálculo — em
[`../SPEC_PAINEL_VETERINARIO_E_DADOS_IA.md`](../SPEC_PAINEL_VETERINARIO_E_DADOS_IA.md).

### Painel do Veterinário

Dashboard analítico da própria clínica: volume por raça, desfecho por raça, gasto com
medicamento, benchmark regional, score de risco de abandono por pet, taxa de retorno e
"tempo de vida ganho".

### Dados + IA

Apoio à decisão clínica: casos semelhantes no histórico, cluster de patologias por
raça/idade e validação cruzada de medicação. **A IA sugere; o veterinário decide e assina.**

### O achado que reposiciona a seção 3

Os módulos foram descritos como analíticos — "cruzam dados que já existem". **O schema não
sustenta isso.** Das 12 necessidades de dado levantadas, **uma** é atendida hoje.

E as lacunas que os bloqueiam são **exatamente as quatro da seção 3**:

| Falta (seção 3) | Já bloqueava | Agora também bloqueia |
|---|---|---|
| `status` no `EventoClinico` | medir absenteísmo | taxa de retorno, score de risco |
| vínculo "gera retorno em X" | saber o que venceu | retenção, cobrança de retorno |
| filtro por intervalo de data | perguntar "o que está atrasado" | toda janela do painel |
| — | — | **óbito e desfecho do animal** (novo) |
| — | — | **prescrição e medicamento** (novo — não existe nenhuma tabela) |
| — | — | **diagnóstico estruturado** (novo — só há `descricao` texto livre) |

Isto é uma **confirmação**, não uma reviravolta: a Etapa 1 do caminho de evolução continua
sendo o primeiro passo, e agora dois módulos dependem dela em vez de um. O que muda é o
tamanho da rodada de captura de dado que vem antes da análise.

Também vale registrar o que a mentoria **não** mudou: `animal.raca` continua sendo texto
livre sem catálogo (item 12 de [07-pendencias](07-pendencias-e-divergencias.md)). Era uma
pendência de severidade baixa; com o painel agregando por raça, passa a ser estrutural.

### Onde isso entra no calendário

Os dois módulos são maiores que a janela da Sprint 3 (18 dias em 25/08). O sequenciamento
proposto — o que cabe antes de 12/09 e o que vai para a Sprint 4 — está em
[`../specs/07-backlog.md`](../specs/07-backlog.md).

---

## 6. Pendências abertas

Do [levantamento completo](07-pendencias-e-divergencias.md). O que falta por sprint e por
disciplina — incluindo os módulos novos — está consolidado em
[`../specs/07-backlog.md`](../specs/07-backlog.md):

| # | Item | Severidade | Situação |
|---|---|---|---|
| 2 | Senha do Oracle no histórico do Git | **Alta** | código e docs limpos; **falta trocar a senha no portal da FIAP** |
| 19 | Perfil `mysql` nunca rodou contra um MySQL real | Média | escrito e testado contra H2 em `MODE=MySQL` |
| 7 | `dataPagamento` obrigatória impede registrar pendente | Média | aberto |
| 9 | Cache não invalida entre entidades relacionadas | Média | aberto |
| 12 | `especie` e `porte` como texto livre | Baixa | aberto |
| 15 | `PagamentoResponse` ainda usa `@Data` | Baixa | parcial |

### Fora do código

- **Avisar Mobile e Frontend da mudança de contrato.** As rotas ganharam `/api/v1` e o
  total das listagens saiu da raiz para `page.totalElements`. Está registrado em
  [`specs/04-dependencias-externas.md`](../specs/04-dependencias-externas.md) como
  conflito 8 — mas o documento sozinho não avisa ninguém.
- **Coleção Postman/Insomnia** exportada em `documentos/`. Vale até 10 pontos na
  rubrica e nunca existiu no repositório.

---

## 7. Números

| | |
|---|---|
| Entidades de domínio | 6 (+ `Usuario` e `Endereco` embutido) |
| Rotas | 42 sob `/api/v1` |
| Perfis de acesso | 3 |
| Migrations | V1 a V4, em dois conjuntos (Oracle e MySQL) |
| Testes automatizados | 126 |
| Bancos suportados | 3 (H2, Oracle, MySQL) |
