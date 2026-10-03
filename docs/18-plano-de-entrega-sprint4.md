# Plano de entrega — Sprint 4 (04/11/2026)

> Aberto em **03/10/2026**, faltando 32 dias. Este documento diz **o que entra na entrega
> final, em que ordem e por quê**. O enunciado e a régua completa estão em
> [`specs/03-sprint-4.md`](../specs/03-sprint-4.md). Aqui fica o que decidimos fazer com eles.
>
> **Estado:** esqueleto. As seções 4 e 6 ficam vazias até o levantamento de lacunas.

---

## 1. Onde o trabalho acontece

A nota da Sprint 3 ainda não saiu, então a `main` continua sendo a entrega avaliada.

- Todo trabalho da Sprint 4 vai para a branch **`sprint-4`**.
- Merge `sprint-4` → `main` só depois da nota sair, via PR, sem rebase nem force-push.
- O CI só roda em push na `main` e em PR para a `main`. Até o PR, a suíte roda localmente
  antes de cada commit.

## 2. A régua que dirige este plano

| Critério | Pontos | O que decide a nota |
|---|---|---|
| Demonstração técnica | 40 | App **no ar**, fluxos principais navegáveis, conceitos da disciplina no contexto do app, boa UI/UX |
| Narrativa da solução | 20 | Proposta clara, decisões **justificadas**, originalidade |
| Integração multidisciplinar | 20 | Como as outras disciplinas entram, **com evidência** (docs, canvas, protótipos, SQL) |
| Apresentação oral | 10 | Os quatro no vídeo, com clareza e domínio |
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
  Leonardo (`leojp04` 26 + `leop04` 5). Fabricio e Miguel têm **zero**. Os commits da Sprint 4
  precisam vir dos quatro, distribuídos no tempo, e não num push único no fim.
- **A interface avaliada é de outro repositório.** A UI/UX vem do app mobile (ver
  [ADR-001](adr/001-ui-pelo-app-mobile.md)). O app precisa rodar os fluxos principais contra
  a API **publicada**, e não contra o localhost.
- **O README diverge do código.** Ele diz que o perfil padrão é `oracle` (o código diz `mysql`,
  em `application.properties`), que a suíte tem 205 testes (são 392) e se contradiz sobre o que
  `spring-boot:run` sem perfil faz. O README completo vale nota (critério 5).
- **O índice de `docs/` está defasado:** o `17-contrato-de-erro.md` não aparece em `docs/README.md`.
- **`LINKS-ENTREGA.txt` ainda é da Sprint 3.**

## 4. O que este repositório precisa entregar

*A preencher no levantamento de lacunas: deploy na Azure, fluxos que o app usa, README final,
evidências de cada disciplina, roteiro do vídeo de 15 minutos.*

## 5. Decisões

Cada decisão com motivo não óbvio vira um ADR em [`adr/`](adr/README.md). Os ADRs são o
material bruto da narrativa (critério 2).

| ADR | Decisão |
|---|---|
| [001](adr/001-ui-pelo-app-mobile.md) | A interface avaliada é o app mobile, e não telas no Spring |

## 6. Cronograma

*A definir depois da seção 4. Datas fixas até aqui: entrega em **04/11/2026**.*
