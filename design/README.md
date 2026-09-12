# design/

Maquetes das telas da API, no design system do app **Clyvo Vet**.

Os tokens saem de `clyvo-rn/src/theme/tokens.ts` do projeto de design — teal
`#008060`, orange `#d97a3d`, DM Sans, raios 8/12/16/20/999, sombras em
`rgba(0,80,64,·)`. Nenhum valor foi arredondado.

| Arquivo | O que é |
|---|---|
| `Main.dc.html` | App do tutor: início, agendar com consentimento, cartão do pet, acessos |
| `Veterinario.dc.html` | Estação do veterinário: agenda, retornos vencidos, microchip, grade |
| `canvas.json` | Layout dos artboards no canvas |

O lado do veterinário **não existe** no projeto de design original — foi
estendido a partir do mesmo vocabulário, só mudando a densidade.

## Regerar o canvas

O `clyvovet-telas.html` publicado não está versionado: são 2,2 MB de editor
embutido, e ele sai destes três arquivos com um comando (`/design` no Claude
Code, ou o `seed-canvas.mjs` do skill).

## Ressalvas

- Os números de métrica são de exemplo — faturamento, taxa de retorno, taxa
  de falta.
- Os ícones são placeholders geométricos. O projeto de design tem um
  `Icon.tsx` com set próprio que não foi portado.
- Isto **não consome a API**. Serve para decidir layout, não é o frontend.
