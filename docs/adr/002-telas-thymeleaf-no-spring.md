# 002 — Telas Thymeleaf no próprio Spring, com login OAuth2

**Data:** 2026-10-04 · **Status:** aceito · **Substitui:** [001](001-ui-pelo-app-mobile.md)

## Contexto

O ADR 001, de 03/10, apostou que a interface avaliada seria o app mobile, sem telas no Spring.
No dia seguinte saiu a nota da Sprint 3: **34/100**. O professor chamou a API de "a mais madura
da turma", mas zerou a parte de aplicação web e escreveu como **melhoria obrigatória**:

- camada de visualização em **Thymeleaf**;
- página de login com **OAuth2 (Google/GitHub)**;
- os fluxos de negócio **na interface**, com formulários e validações.

A Sprint 4 cobra "boa UI e UX" dentro dos 40 pontos de demonstração técnica. Mostrar só o app
mobile repetiria, no critério que mais pesa, a falta que custou a Sprint 3.

## Decisão

A aplicação Spring passa a servir **telas Thymeleaf**, no mesmo artefato e no mesmo deploy da
API:

- **Duas cadeias de segurança.** A da API (`/api/**`) continua stateless, com JWT e CSRF
  desligado. A das telas usa sessão, **CSRF ligado**, login por formulário e OAuth2.
- **OAuth2 só autentica quem já tem conta.** Um e-mail desconhecido é recusado, pelo mesmo
  motivo pelo qual o cadastro não vincula conta por e-mail (regra X13 do `UsuarioService`).
- **As telas não têm regra própria.** Elas chamam os mesmos services e as mesmas expressões
  `@seguranca.*` da API. A sessão guarda o mesmo `UsuarioAutenticado` que o filtro JWT monta.
- Os fluxos levados à tela são os dois que não são CRUD: o **agendamento** (tutor) e a
  **conclusão com retorno** (veterinário).

## Consequências

- A API REST não muda. O app mobile continua consumindo-a e passa a entrar no vídeo como
  evidência de **integração multidisciplinar**, não como a interface avaliada.
- O repositório ganha `templates/`, `static/` e um pacote `web/`, ou seja, código novo para manter
  e para passar pela revisão de boas práticas e repetição.
- Com sessão, volta o risco de CSRF. Por isso ele é ligado na cadeia das telas, como o
  comentário do `SecurityConfig` já previa.
- O login social depende de apps OAuth criados no Google e no GitHub, com o redirect da URL
  da Azure, e precisam estar prontos antes do deploy.
- O roteiro do vídeo passa a navegar pelas telas Thymeleaf.
