# Roteiro do vídeo — Java Advanced, Sprint 4

> **Limite do edital: 15 minutos, com os cinco integrantes falando.** O roteiro soma **14 min 30 s**,
> deixando 30 s de folga. Este é o roteiro do vídeo de **Java**. O da DevOps é outro vídeo e
> fica no repositório da API .NET. O [`14-roteiro-do-video.md`](14-roteiro-do-video.md) é o da
> DevOps da Sprint 3 e não vale aqui.

O professor disse que a nota é "praticamente a apresentação" e que vai avaliar **se todos sabem
explicar o sistema**. Por isso o roteiro tem duas partes: o que mostrar (seção 2) e o que cada um
precisa saber responder (seção 4).

---

## 1. Antes de apertar REC

Nada disto aparece no vídeo. Tudo precisa ser verdade antes.

- [ ] **Aplicação no ar numa URL pública** (o edital: "rodando online"). *Depende do deploy, que ainda não existe.*
- [ ] **Login Google e GitHub funcionando nessa URL.** Os apps OAuth precisam ter
      `{URL}/login/oauth2/code/google` e `/github` como redirecionamento, e o e-mail usado precisa estar cadastrado.
- [ ] **Usuários de demonstração existem no ambiente do vídeo.** O perfil `mysql` não semeia usuários
      (README, "A API exige autenticação"). Confirmar como eles entram nesse ambiente. *Não verificado.*
- [ ] **A clínica da veterinária tem agenda.** Sem o perfil `demo`, a VetCare não tem serviço e o tutor não
      consegue marcar com a Camila (README, "Limites conhecidos").
- [ ] **O app mobile aponta para a API publicada** (`EXPO_PUBLIC_JAVA_BASE_URL`) e a API .NET também está no ar.
- [ ] **Ensaio completo do começo ao fim, cronometrado.** Se estourar 14:30, corte o que a seção 2 manda, nunca a demonstração.
- [ ] **Sem erro visual ou de fluxo na tela.** O edital desconta 5 pontos por isso. Ensaiar também a troca de perfil
      (sair da sessão entre o tutor e a veterinária).
- [ ] **Cada um sabe responder a sua folha** (seção 4).
- [ ] Nenhum segredo real aparece na tela. As senhas de desenvolvimento do README podem aparecer.

---

## 2. A ordem do vídeo

Preencha a coluna "Quem" antes do ensaio. Sugestão: quem fala cada bloco é quem melhor o domina,
mas **todos aparecem e respondem** (seção 4).

| # | Tempo | Bloco | Quem | O que mostrar e dizer |
|---|---|---|---|---|
| 1 | 0:00 – 1:00 | **Abertura e problema** | ___ | O histórico do pet fica espalhado entre clínicas. Quem atende não sabe da alergia. Apresentar os cinco, o nome do projeto e a ordem do vídeo. |
| 2 | 1:00 – 2:30 | **A ideia: o prontuário é do tutor** | ___ | Três níveis de acesso, consentimento que nasce no agendamento, quebra de vidro. Slide ou o README (ver [ADR 003](adr/003-prontuario-pertence-ao-tutor.md)). Dizer o **porquê**: negar alergia em emergência é pior que expor. |
| 3 | 2:30 – 5:00 | **Demo: o tutor** | ___ | Navegador na URL pública. Login `lucas.santos@email.com`. Agendar: escolher pet, clínica e serviço, ver os horários livres, confirmar **com** o consentimento. Ver a consulta em "meus agendamentos". Cancelar uma, mostrando o motivo obrigatório. |
| 4 | 5:00 – 7:00 | **Demo: a veterinária** | ___ | Sair e entrar como `camila.ferreira@vetcare.com.br`. Lista de atendimentos, **Concluir** (peso, desfecho, retorno previsto), **marcar o retorno**, lista de retornos vencidos. Explicar que a conclusão é o único caminho para `REALIZADO`. |
| 5 | 7:00 – 8:30 | **Segurança** | ___ | Login social: o primeiro acesso cria uma conta de tutor, só com e-mail verificado pelo provedor, e nunca vincula a um cadastro da clínica ([ADR 009](adr/009-login-social-cria-conta-de-tutor.md)). A veterinária abrindo `/tutor` recebe "Acesso negado". Maria não vê os pets do Lucas. Duas cadeias: JWT na API, sessão e CSRF nas telas. |
| 6 | 8:30 – 10:00 | **A API** | ___ | Swagger: login, token, uma chamada com token e outra sem (401). **Quebra de vidro** com motivo, e `GET /animais/{id}/acessos` mostrando quem leu o prontuário. |
| 7 | 10:00 – 12:00 | **Integração com as outras disciplinas** | ___ | App mobile usando a **mesma** API. Mostrar, se o ensaio confirmar, um agendamento feito no celular aparecendo na tela web (mesma API, mas **não testado ainda**). API .NET: widget de saúde preditiva no mesmo banco ([ADR 004](adr/004-banco-compartilhado-java-dona-do-schema.md)). Mostrar a tabela do README, "Integração multidisciplinar". |
| 8 | 12:00 – 13:30 | **Banco e decisões** | ___ | 21 migrations em dois conjuntos ([ADR 005](adr/005-perfis-de-banco-e-migrations-por-dialeto.md)), catálogo de raças com `chave` ([ADR 006](adr/006-catalogo-de-racas-com-chave.md)), contrato de erro ([ADR 007](adr/007-contrato-de-erro-com-referencia.md)). Mostrar um trecho do `script_bd.sql` ou o diagrama de classes. |
| 9 | 13:30 – 14:30 | **Fecho** | todos | Cada um, em uma frase: o que mais aprendeu. Dizer onde estão o repositório e o README. |

**Se o tempo apertar**, a ordem do que cortar é: bloco 8, depois metade do bloco 7. A demonstração
(blocos 3 e 4) vale 40 pontos e não se corta.

---

## 3. Regras de gravação

- **Dizer o porquê, não só o quê.** A narrativa vale 20 pontos e pede "decisões e justificativas": cada ADR é uma
  resposta pronta para "por que vocês fizeram assim?".
- **Mostrar, não descrever.** O que está no README e funciona na tela aparece na tela.
- **Não esconder a lacuna.** Se algo não está pronto, dizer. Uma lacuna dita custa menos que uma demonstração que quebra ao vivo.
- **Todos no vídeo.** Quem não aparecer é motivo de "ausência de evidência de colaboração" (−10).

---

## 4. Folha de perguntas: cada um precisa saber responder

O professor pode perguntar a qualquer integrante, sobre qualquer parte. Estas perguntas cobrem o
sistema inteiro; cada uma aponta para onde está a resposta. **Ninguém pode tratar uma parte como "do outro".**

| # | Pergunta | Onde está a resposta |
|---|---|---|
| 1 | Qual problema o CLYVO VET resolve e para quem? | README, "Descrição da Solução" |
| 2 | Por que o prontuário é do tutor e não da clínica? | [ADR 003](adr/003-prontuario-pertence-ao-tutor.md) |
| 3 | O que é a quebra de vidro e por que ela nunca é bloqueada? | ADR 003 |
| 4 | Por que o nível 1 (alergias) é visível sem consentimento? | ADR 003 |
| 5 | Por que telas Thymeleaf se já existia o app mobile? | [ADR 002](adr/002-telas-thymeleaf-no-spring.md) |
| 6 | Por que duas cadeias de segurança, e o que muda entre elas? | ADR 002; README, "Duas cadeias de segurança" |
| 7 | O que o login social faz quando o e-mail não tem conta, e por que só com e-mail verificado? | [ADR 009](adr/009-login-social-cria-conta-de-tutor.md); README, "Login com Google e GitHub" |
| 8 | O que impede um tutor de ver o pet de outro? | README, "Arquitetura" (ownership e chave de cache); [Segurança](08-seguranca.md) |
| 9 | Por que as duas APIs compartilham o banco, e por que isso é seguro? | [ADR 004](adr/004-banco-compartilhado-java-dona-do-schema.md) |
| 10 | Por que há duas pastas de migrations e o que garante que não divergem? | [ADR 005](adr/005-perfis-de-banco-e-migrations-por-dialeto.md) |
| 11 | O que o `ddl-auto=validate` faz e por que o Flyway cria o schema? | ADR 005 |
| 12 | Por que a raça virou catálogo e não enum? | [ADR 006](adr/006-catalogo-de-racas-com-chave.md) |
| 13 | O que é a `referencia` que aparece num erro 500? | [ADR 007](adr/007-contrato-de-erro-com-referencia.md) |
| 14 | O que acontece quando o tutor agenda? Que regras são checadas? | README, "Fluxo A" |
| 15 | Por que a conclusão é o único caminho para `REALIZADO`? | README, "Fluxo R" |
| 16 | Como o app mobile e a API .NET entram no sistema? | README, "Integração multidisciplinar" |
| 17 | O que ainda não está pronto ou não foi testado? | README, "Limites conhecidos"; [ADR 005](adr/005-perfis-de-banco-e-migrations-por-dialeto.md), "Consequências" |
| 18 | Quando alguém cria uma conta, como o sistema decide se ela é de tutor ou de veterinário? | Quem se cadastra sozinho (tela `/cadastro`, `POST /auth/registrar` ou login social) **sempre vira TUTOR**: `UsuarioService.registrar` e `registrarPorProvedor` fixam `Perfil.TUTOR`, e a tela nem tem campo de perfil. Veterinário e administradores só nascem em `POST /auth/usuarios`, que a regra de rota fecha para `ADMIN` (`SecurityConfig`, `hasRole(ADMIN)`) e que exige o vínculo certo (`veterinarioId`, `clinicaId`). Motivo: o perfil de menor poder é o padrão, e o que vem de formulário pode ser forjado. Na demonstração, o veterinário vem do `UsuariosDeDemonstracaoSeeder`. ADR 009 |

**Ensaio das perguntas:** cada integrante sorteia cinco números e responde em voz alta, sem olhar, em 30 segundos cada.

---

## 5. O que este roteiro não resolve

- O Java já está no ar na Render, com o banco MySQL no Aiven e fora da Azure
  ([ADR 008](adr/008-java-na-render-com-mysql-no-aiven.md)), em
  `https://clyvovet-backend-java.onrender.com`, com os apps OAuth do Google e do GitHub apontando
  para ele. O que ainda pode derrubar a demonstração, e o roteiro não resolve:
  - **Subida a frio:** o plano gratuito derruba o app depois de uns 15 minutos parado, e a primeira
    requisição leva mais de 4 minutos. Aquecer o app antes de gravar e antes da apresentação.
  - **Google em modo *Teste*:** só entra quem está na lista de usuários de teste, e aparece o aviso
    de "app não verificado".
  - **Aiven:** o período de teste vale até 17/11/2026. Passada a data, o banco desliga, a menos que
    o grupo cadastre um cartão.

  O que falta provar online está em [`18-plano-de-entrega-sprint4.md`](18-plano-de-entrega-sprint4.md).
- O vídeo da DevOps (pipeline, alteração de código, deploy) é outro roteiro, no repositório .NET.
- Quem fala em cada bloco é decisão do grupo.
