# 008 — Java na Render e banco MySQL no Aiven, fora da Azure

**Data:** 2026-10-18 · **Status:** aceito

## Contexto

O edital de Java Advanced da Sprint 4 pede uma coisa sobre deploy: *"A aplicação deve estar
rodando online"*. Não diz onde, nem em que banco. A Azure ficou reservada à disciplina de DevOps,
que na Sprint 4 publica a API .NET, e o grupo combinou que o crédito dela não seria gasto com o
Java.

O Java precisava então de duas coisas fora da Azure: um lugar para rodar o container e um banco
que esse lugar alcance. O projeto já tem perfil, migrations e validação para **MySQL**
([ADR 005](005-perfis-de-banco-e-migrations-por-dialeto.md)), e o banco é compartilhado com a API
.NET ([ADR 004](004-banco-compartilhado-java-dona-do-schema.md)).

## Decisão

- **Aplicação na Render**, Web Service em Docker, usando o `Dockerfile` do repositório, plano
  gratuito, região Ohio, branch `sprint-4`, com auto-deploy desligado (cada deploy é manual).
- **Banco no Aiven MySQL**, plano *Developer* durante o período de teste. O perfil `mysql` é o
  padrão do `application.properties`, então nada no código muda: só quatro variáveis de ambiente
  (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`).

Alternativas descartadas:

- **Postgres da própria Render.** A Render não oferece MySQL. Usar o Postgres dela exigiria um
  perfil novo e reescrever as 21 migrations (o SQL do MySQL não roda igual), e deixaria Java e
  .NET com bancos diferentes.
- **Oracle da FIAP.** Seria sem custo e sem prazo, mas ninguém provou que a Render alcança a porta
  1521, e a .NET continuaria precisando de um MySQL com o schema criado pela Java.
- **Aiven Free.** Não vence, mas **desliga por inatividade** e só quem é dono da conta religa.
  Quem corrige a entrega não teria como ligar o banco.
- **H2 em memória no próprio app.** Sobe sem depender de nada, mas os dados somem a cada
  reinício, e a Render reinicia o app depois de ele dormir.

## Consequências

- **Prova feita em 18/10/2026:** o Flyway conectou no MySQL 8.4 do Aiven com TLS, validou as 21
  migrations (schema na versão 21), o app subiu com `ddl-auto=validate`, `GET /actuator/health`
  respondeu `UP` e `/` redirecionou para a tela de login (HTTP 200). Nessa primeira rodada, login
  e leitura de dados **não** foram exercitados; foram no mesmo dia, em uma segunda rodada (ver
  "Login social" abaixo e `docs/05-deploy.md`).
- **O banco do Aiven é o MySQL 8.4**, e a validação anterior era em 8.0. O Flyway avisa que a 8.4
  é mais nova que a versão que ele testou; nada falhou por isso.
- **O período de teste do Aiven é de 30 dias** a partir de 18/10/2026. Depois dele o serviço é
  desligado, a menos que se cadastre um cartão (US$ 5 por mês no plano Developer). É um custo e
  um prazo reais, e quem decide pagar é o grupo.
- **A subida a frio é lenta.** No plano gratuito a Render derruba a instância depois de uns 15
  minutos sem uso, e a subida seguinte levou mais de 4 minutos na primeira medição (256 s). A
  primeira requisição depois de parado espera por isso.
- **Os segredos ficam só no painel da Render.** O repositório documenta o nome das variáveis,
  nunca o valor, e não registra o endereço do banco.
- **Login social (Google e GitHub) está configurado e foi testado.** O professor pediu esse
  login. Foram necessários um OAuth App no GitHub, um cliente OAuth no Google Cloud, as quatro
  variáveis `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GITHUB_CLIENT_ID` e
  `GITHUB_CLIENT_SECRET` na Render, e a URL de retorno de cada um apontando para o endereço da
  Render. No Google, o app está em modo **Teste**: só entra quem está em *Usuários de teste*, e
  aparece o aviso de "app não verificado". O e-mail da conta social precisa existir como usuário.
- **Uma troca de ID por segredo quase expôs o segredo.** O segredo do GitHub foi colado no campo
  `GITHUB_CLIENT_ID`, e o ID vai na URL pública do botão. Foi percebido no teste de
  redirecionamento, o segredo foi gerado de novo e o antigo, apagado.
- A API .NET, no vídeo de DevOps, pode apontar para o mesmo banco do Aiven e usar a Azure só para
  hospedar o app. Isso ainda **não foi testado**.
