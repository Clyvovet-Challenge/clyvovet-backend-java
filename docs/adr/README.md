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
| [001](001-ui-pelo-app-mobile.md) | A interface avaliada é o app mobile, e não telas no Spring | substituído por 002 |
| [002](002-telas-thymeleaf-no-spring.md) | Telas Thymeleaf no próprio Spring, com login OAuth2 | aceito |
| [003](003-prontuario-pertence-ao-tutor.md) | O prontuário pertence ao tutor: três níveis de acesso e quebra de vidro | aceito |
| [004](004-banco-compartilhado-java-dona-do-schema.md) | Banco compartilhado entre as duas APIs, com a Java dona do schema | aceito |
| [005](005-perfis-de-banco-e-migrations-por-dialeto.md) | Perfis de banco e migrations em dois conjuntos (Oracle e MySQL) | aceito |
| [006](006-catalogo-de-racas-com-chave.md) | Catálogo de raças com `chave`, em vez de texto livre ou enum | aceito |
| [007](007-contrato-de-erro-com-referencia.md) | Contrato de erro das três pontas, com referência só na falha de servidor | aceito |
| [008](008-java-na-render-com-mysql-no-aiven.md) | Java na Render e banco MySQL no Aiven, fora da Azure | aceito |

Os ADRs 003 a 007 foram escritos em 10/10/2026 para decisões **anteriores** à Sprint 4. A
data de cada um diz quando foi registrado, e o texto aponta o documento onde a decisão nasceu.
O ADR 008 é de 18/10/2026 e registra uma decisão tomada durante a Sprint 4.
