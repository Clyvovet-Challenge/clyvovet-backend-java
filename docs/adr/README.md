# Registros de decisão (ADR)

Um ADR (*Architecture Decision Record*) registra **uma** decisão cujo motivo não é óbvio
lendo o código: o que estava em jogo, o que escolhemos e o que isso custa. Serve para quem
chega depois e para a narrativa da entrega final, que pede "decisões de design e justificativas".

Um ADR não se edita depois de aceito. Se a decisão mudar, escreva um novo que o substitua e
marque o antigo como *substituído por NNN*.

## Formato

```markdown
# NNN — Título curto da decisão

**Data:** AAAA-MM-DD · **Status:** proposto | aceito | substituído por NNN

## Contexto
O problema e as forças em jogo, em poucas linhas.

## Decisão
O que foi escolhido.

## Consequências
O que fica mais fácil, o que fica mais difícil e o que passa a ser obrigatório.
```

## Índice

| ADR | Decisão | Status |
|---|---|---|
| [001](001-ui-pelo-app-mobile.md) | A interface avaliada é o app mobile, e não telas no Spring | aceito |
