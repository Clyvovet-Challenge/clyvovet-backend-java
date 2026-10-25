# 006 — Catálogo de raças com `chave`, em vez de texto livre ou enum

**Data:** 2026-10-10 (registra uma decisão anterior à Sprint 4; migration `V14`) · **Status:** aceito

## Contexto

`t_clyvo_animal.raca` e `especie` eram `VARCHAR` livres. O app já oferecia lista fechada para
espécie, porte, sexo e castrado; só a raça era campo de texto, e a API aceitava qualquer coisa de
qualquer cliente. Contados no MySQL com 28 animais, a espécie "cachorro" aparecia como `Cachorro`,
`CAO` e `CANINO`, e "sem raça definida" como `SRD`, `Vira` e `Vira-lata` (9 animais). Uma busca
direta pela raça acertaria 15 dos 28 (54%).

Isso já custava num ponto que pontua: o widget de saúde preditiva da API .NET casa raça para
sugerir risco de doença e, sem padronização, comparava por substring.

## Decisão

Uma tabela `t_clyvo_raca` (45 raças em cinco espécies) com uma coluna **`chave`**
(`golden-retriever`), única, que é o mesmo identificador na arte do animal no app, na
predisposição de saúde da API .NET e na busca no cliente. Quatro escolhas dentro da decisão:

- **A FK `raca_id` nasce anulável.** Existem 200 e poucas raças de cachorro; "outra raça" precisa
  continuar possível.
- **A coluna de texto `raca` fica**, para a `V14` não quebrar nenhum `SELECT`, a .NET nem o
  `AnimalResponse`. Com `raca_id` preenchido, `raca` é cópia do catálogo; com ele nulo, é o que o
  tutor digitou.
- **A espécie continua `String`, mas passa a ser derivada** da raça escolhida, o que uniformiza
  `CAO`/`CANINO`/`Cachorro` sem trocar o tipo da coluna.
- **Enum não serve para raça**: acrescentar uma raça viraria mudança de código e redeploy, e com
  tabela é um `INSERT`. Enum serve para espécie, que é fechada e pequena (`EspecieAnimal`).

## Consequências

- Quem escolhe do catálogo já recebe a chave pronta, então ninguém normaliza texto.
- O catálogo é curto de propósito (um seletor com 200 itens é pior que um campo de texto, e cada
  linha ganha uma arte em pixel).
- A `V14` liga os animais já cadastrados à raça do catálogo quando o texto casa. O que não casa
  fica com `raca_id` nulo e mantém o texto original, sem perda.
- Resultados da verificação e o que ficou pendente: [`../15-catalogo-de-racas.md`](../15-catalogo-de-racas.md).
