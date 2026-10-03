---
paths:
  - "src/test/**"
---

# Regras para testes

- **TDD:** escreva o teste, rode e veja ele falhar pelo motivo certo, e só então escreva o código.
  Bug corrigido vem com teste de regressão.
- Escolha a pasta pelo que o teste prova: `crud` (CRUD de um recurso), `fluxo` (fluxos de
  negócio A, R, C e P, e o painel), `security` (autenticação, autorização, ownership), `mapper`
  (unitário, sem Spring), `service`, `migration` (migrations e `script_bd.sql`).
- Teste que passa pela API **estende `support/TesteDeApi`**. Ele já faz login (`tokenAdmin()`,
  `tokenVeterinaria()`, `tokenTutor(email)`, `tokenAdminDaClinica(...)`), monta as requisições
  (`buscar`, `criar`, `atualizar`, `atualizarParcialmente`, `remover`), lê o JSON (`corpoDe`) e
  limpa o que o teste criou (`removerDepois(url)`). Não reescreva nenhum desses helpers na classe
  de teste: repetição custa −5 na Sprint 4.
- Ids do seed vêm de `support/SeedV2` (`CLINICA_VETCARE`, `TUTOR_LUCAS`, ...), nunca de UUID literal.
- Nomes em português, descrevendo o comportamento: método `filtraPorSituacao()` e
  `@DisplayName("filtra pela situacao do atendimento")`. Asserções com AssertJ (`assertThat`).
- A suíte roda no perfil `dev` (H2 em `MODE=Oracle`): `./mvnw test`. Antes de dizer que algo está
  pronto, rode a suíte inteira e mostre o resultado.
