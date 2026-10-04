# 001 — A interface avaliada é o app mobile

**Data:** 2026-10-03 · **Status:** substituído por [002](002-telas-thymeleaf-no-spring.md)

## Contexto

A Sprint 4 de Java Advanced avalia, dentro dos 40 pontos de demonstração técnica, se "a
interface apresenta boa UI e UX". Este repositório é só uma API REST: não tem Thymeleaf
nem arquivos estáticos.

Já existe uma interface completa consumindo esta API: o app em React Native / Expo,
desenvolvido em Mobile Application Development, que também roda como alvo web (foi para
ele que o CORS foi corrigido no commit `fa9e8d4`).

As alternativas eram construir telas web no próprio Spring (Thymeleaf) ou demonstrar só
pelo Swagger.

## Decisão

A interface mostrada no vídeo de Java Advanced é o **app mobile**, consumindo esta API
publicada na Azure. Não vamos construir telas no Spring.

## Consequências

- Nenhuma tela nova a manter neste repositório. O esforço da sprint vai para a API, o deploy
  e a documentação.
- A demonstração passa a provar também a integração entre disciplinas (critério 3), já que o
  app é de outra matéria.
- A nota de UI/UX passa a depender de um repositório que não é este. O app precisa rodar os
  fluxos principais contra a API **publicada**, e não contra o localhost, no dia da gravação.
- O vídeo precisa deixar claro que o backend mostrado é este, Java/Spring: por exemplo,
  alternando entre o app e o Swagger ou os logs da API durante os fluxos.
