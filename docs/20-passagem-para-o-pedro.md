# Passagem do projeto Java para o Pedro

Escrito em **25/10/2026**, na passagem de bastão do Leonardo para o Pedro. Diz o que está
pronto, onde está cada coisa, o que **não** está no git e o que falta até a entrega de
**04/11/2026**.

> **Este arquivo não tem segredo nenhum.** Senhas e chaves ficam nos painéis (seção 3). Nunca as
> coloque no git, no chat ou em e-mail.

---

## 1. Em um minuto

- O código da Sprint 4 em Java está **pronto e provado no ar**: 514 testes passando, site na
  Render com o commit `1f17c40`, GitHub em dia.
- O que falta **não é código**: concluir um atendimento (28/10), testar o app mobile contra a
  Render, ensaiar as 18 perguntas e gravar o vídeo.
- O que o git **não** passa sozinho: **os acessos** (Render, Aiven, Google Cloud, GitHub OAuth) e
  **os valores dos segredos**. Peça-os ao Leonardo (seção 2).

## 2. Acessos que você precisa pedir ao Leonardo

Marque cada um quando receber.

- [ ] **Render**: o workspace *Sprint 4 Front End* está no plano **Hobby (gratuito)**, e a Render
  **não deixa convidar membros** nesse plano (só no Pro, que é pago). Então você **não** entra no
  workspace: o Leonardo dispara os deploys e, quando você precisar ver um log ou uma variável, ele
  copia ou compartilha a tela. Se o grupo decidir subir para o Pro, o convite é em *Settings* →
  *Team members* → *+ Invite members*, com o papel **Developer**.
- [ ] **Aiven**: peça um **usuário próprio** no serviço MySQL (o Leonardo o cria na aba de usuários
  do serviço), em vez de usar o usuário principal. Assim o acesso se revoga sem trocar a senha de
  todo mundo. O período de teste **vence em 17/11/2026**.
- [ ] **Google Cloud**: convite de membro no projeto *ClyvoVet*. É em *Google Auth Platform* que
  ficam o cliente OAuth, a tela de consentimento e o status "Em produção".
- [ ] **OAuth App do GitHub** (login com GitHub): pergunte em qual conta ele foi criado.
- [ ] **Senha dos usuários de demonstração**, para testar como cada perfil (seção 6).

## 3. Onde estão os segredos

Só os **nomes** das variáveis estão no repositório ([`05-deploy.md`](05-deploy.md), seção
*Variáveis de ambiente*). Os valores ficam na Render, aba *Environment*:

`DB_PASSWORD`, `JWT_SECRET`, `GOOGLE_CLIENT_SECRET`, `GITHUB_CLIENT_SECRET`, `CLYVOVET_DEMO_SENHA`.

Como você não entra na Render (seção 2), o Leonardo passa os valores de que você precisar **por
canal privado** (mensagem direta ou gerenciador de senhas), **nunca** no git, no chat do grupo ou
em e-mail. **Variável nova na Render só vale depois de um novo deploy.**

**Você quase não precisa deles.** Sem nenhum segredo, o projeto roda no perfil `dev` (H2 em
memória): `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`. Só vai precisar de valores se
rodar contra o banco real (`DB_URL`, o usuário e a senha do **seu** usuário do Aiven; o
`JWT_SECRET` local pode ser qualquer texto longo) ou testar o login social. Guarde-os fora do
repositório, exportados no terminal ou na configuração de execução do IDE. Um arquivo `.env` está
no `.gitignore`, mas o Spring **não** o lê sozinho.

## 4. Onde está cada coisa

| O quê | Onde |
|---|---|
| Código Java | `https://github.com/Clyvovet-Challenge/clyvovet-backend-java`, branch **`sprint-4`** |
| Site no ar | `https://clyvovet-backend-java.onrender.com` (Swagger em `/swagger-ui.html`) |
| Política de privacidade | `/privacidade` (exigida pelo Google para publicar o login social) |
| Banco | MySQL no Aiven ([ADR 008](adr/008-java-na-render-com-mysql-no-aiven.md)) |
| API .NET | `https://github.com/Clyvovet-Challenge/ClyvoVet-api` |
| App mobile | repositório do Classroom, em `documentos/LINKS-ENTREGA.txt` |

## 5. Como trabalhar

- **Branch:** tudo na `sprint-4`. A `main` é a entrega da Sprint 3 e **fica congelada** até o
  Leonardo pedir o merge (via PR).
- **CI:** só roda na `main` e em PR para ela. Na `sprint-4` ele **não** roda, então rode a suíte
  antes de cada commit: `./mvnw test`.
- **Deploy é manual.** O `git push` **não** atualiza o site (o *autoDeploy* está desligado). Como
  você não entra na Render, **peça ao Leonardo** depois de cada push: no painel dele, serviço
  `clyvovet-backend-java` → *Manual Deploy* → *Deploy latest commit*. O build leva uns 6 minutos, e
  durante a troca o app antigo ainda responde. Só texto de documentação **não** precisa de deploy.
- **O app dorme** no plano gratuito. Abra o site 1 a 2 minutos antes de gravar ou testar.
- **Teste primeiro** (TDD), e bug corrigido leva teste de regressão. O `CLAUDE.md` e o
  `.claude/rules/` do projeto valem para quem usar Claude Code.

## 6. Usuários de demonstração

Um por perfil, todos com a **mesma senha** (a variável `CLYVOVET_DEMO_SENHA`):

| E-mail | Perfil | O que vê nas telas |
|---|---|---|
| `demo.tutor@clyvovet.com` | Tutor (Lucas) | Meus pets, Meus agendamentos |
| `demo.veterinaria@clyvovet.com` | Veterinária (Camila) | Atendimentos, Retornos vencidos |
| `demo.gestor@clyvovet.com` | Gestor da clínica | só "Gestão da clínica" (o resto é no app) |
| `demo.admin@clyvovet.com` | Administrador | cai em `/veterinario` |

Cinco senhas erradas bloqueiam a conta por 15 minutos, e o limite é de 10 tentativas por minuto.

## 7. O que já está provado e o que não está

**Provado no ar** (detalhes no [`18-plano-de-entrega-sprint4.md`](18-plano-de-entrega-sprint4.md)):
login com Google com o app em produção; política de privacidade; CSRF em cookie com aviso de
página expirada; rate limit sem escapar por cabeçalho `X-Forwarded-For` forjado; gestor e admin
entrando e sendo barrados no que não podem.

**Não verificado:**
- Concluir um atendimento (grava prontuário).
- App mobile contra a API publicada.
- Login com GitHub, refeito online (só o Google foi refeito na Sprint 4).
- Dois IPs reais com baldes de rate limit separados ([`08-seguranca.md`](08-seguranca.md)).

## 8. O que falta, com data

| O quê | Quando | Observação |
|---|---|---|
| **Concluir um atendimento** | **28/10** | A consulta do Bolinha com a Camila é às 17:30. O botão *Concluir* só aparece para consulta de hoje ou anterior |
| App mobile contra a Render | antes do vídeo | `EXPO_PUBLIC_JAVA_BASE_URL=https://clyvovet-backend-java.onrender.com` |
| Ensaio das 18 perguntas | até 31/10 | [`19-roteiro-do-video-sprint4.md`](19-roteiro-do-video-sprint4.md). Todos precisam explicar o projeto inteiro |
| Vídeo (até 15 min) | até 04/11 | Navegando pelas telas, com os cinco falando |
| Entrega no portal | **04/11** | Blocos prontos em `documentos/LINKS-ENTREGA.txt` |

## 9. Riscos e decisões em aberto

1. **O Aiven (teste) vence em 17/11/2026.** Se a correção passar dessa data, o site cai, a menos
   que o grupo cadastre um cartão (decisão do grupo).
2. **A senha do usuário principal do Aiven não foi trocada.** Por isso o Pedro recebe um **usuário
   próprio**, que se revoga sem trocar a senha de ninguém. Trocar a do usuário principal continua
   sendo escolha do Leonardo.
3. **O contato da política de privacidade é o e-mail do Leonardo** (`leojp04@gmail.com`), assim
   como o e-mail de suporte no Google. Troque quando assumir: o texto está em
   `src/main/resources/templates/privacidade.html`, e o do Google, em *Branding*.
4. **Rate limit atrás da Render:** a chave do balde é o último valor do `X-Forwarded-For`
   (`clyvovet.rate-limit.proxies-confiaveis=1`). Se a Render puser outro balanceador na frente,
   todos dividirão um balde só: suba para `2`, sem mudar código.

## 10. Armadilhas que já custaram tempo

- **Teste de limite com o app dormindo engana:** cada pedido demora tanto que o balde se repõe e
  nunca dá 429. Aqueça o site antes de medir.
- **Abrir um template `.html` direto do disco** mostra a página sem CSS. Para ver como o usuário
  vê, suba o app (`./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`).
- **`live` na Render não é "versão nova atendendo":** há uns minutos com a antiga ainda
  respondendo. Teste a rota nova até dar 200.
- **O Google só deixa publicar o login** com página inicial e política de privacidade no domínio
  autorizado. Por isso existe a `/privacidade`.

## 11. Leia nesta ordem

1. [`18-plano-de-entrega-sprint4.md`](18-plano-de-entrega-sprint4.md): a verdade sobre o que está
   feito.
2. [`05-deploy.md`](05-deploy.md): Render, Aiven e variáveis.
3. [`19-roteiro-do-video-sprint4.md`](19-roteiro-do-video-sprint4.md): vídeo e perguntas.
4. [`08-seguranca.md`](08-seguranca.md) e os [ADRs](adr/README.md): o porquê das decisões.

> **Ignore o [`09-estado-do-projeto.md`](09-estado-do-projeto.md):** é um retrato de 25/08/2026 e
> está velho. Vale o `18`.

## 12. O que você não herda

As preferências pessoais do Leonardo no Claude Code (modo Plan/Auto, resumo no fim de cada
tarefa, commit só com aprovação da mensagem) ficam na máquina dele, fora do git. O que vale para
o projeto está no `CLAUDE.md`.
