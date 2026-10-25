# 009 — O login social cria a conta de tutor no primeiro acesso

**Data:** 2026-10-18 · **Status:** aceito

Substitui **uma** das decisões do [ADR 002](002-telas-thymeleaf-no-spring.md): a de que o OAuth2
só autentica quem já tem conta. O resto do 002 continua valendo.

## Contexto

O ADR 002 decidiu que o login com Google ou GitHub só abre uma conta que **já existe**: um e-mail
desconhecido era recusado, com a mensagem "peça à sua clínica para cadastrá-lo". A razão era a
regra X13 do `UsuarioService`, que proíbe vincular conta a cadastro por e-mail sem prova de posse.

Na Sprint 4 isso virou um problema de uso. O professor avalia o sistema **no ar**, e para testar
precisa conseguir criar a própria conta e entrar com o Gmail ou o GitHub dele. Com o 002, ele via
"Nenhuma conta usa este e-mail" e não tinha o que fazer: o cadastro só existia pela API, e as
contas de veterinário e administrador nascem pela gestão.

O 002 tinha uma lacuna que esse caso expôs: ele tratava "o provedor prova o e-mail" e "a conta já
existe" como a mesma coisa. Não são. Prova de posse do e-mail é justamente o que faltava para o
cadastro ser seguro, e o provedor entrega essa prova.

## Decisão

No primeiro acesso por Google ou GitHub, **se não existe conta com aquele e-mail, nasce uma conta
de TUTOR**, com o nome que o provedor informa. Valem estas travas:

- **Só com e-mail verificado pelo provedor.** O Google entrega `email_verified`; o GitHub, o e-mail
  principal e verificado de `/user/emails`. Sem isso, ninguém entra e nada é criado (já era assim).
- **O perfil é sempre TUTOR**, o de menor poder. Veterinário, administrador de clínica e
  administrador da plataforma continuam sendo criados só pela gestão.
- **Nunca vincula a um cadastro existente.** Se o e-mail já é de um tutor que a clínica cadastrou
  (sem usuário), a conta **não** é criada nem ligada a ele, e a pessoa vê a mesma recusa de sempre.
  Vincular a um cadastro preexistente continua exigindo o fluxo próprio da regra X13.
- **E-mail que já tem conta continua entrando na conta dele**, como antes. Conta inativa ou
  bloqueada continua recusada: o bloqueio por senha errada não é contornado por um clique.
- **A conta criada não tem senha que alguém conheça.** A coluna é obrigatória, então grava-se o
  hash de 32 bytes aleatórios que nunca são mostrados. Ela entra só pelo provedor.

O nome do tutor vem do provedor; sem nome, do `login` (GitHub); sem ele, da parte do e-mail antes
do arroba. É o único dado que a tabela exige, e os mesmos limites do cadastro por formulário (3 a
100 caracteres) valem aqui.

Alternativas descartadas:

- **Manter o 002 e dar usuários prontos ao professor.** É o que o `UsuariosDeDemonstracaoSeeder`
  já faz, e continua existindo como plano B. Mas não permite que ele crie a própria conta nem teste
  o login social de verdade, que é o que a disciplina quer ver funcionando.
- **Criar a conta com qualquer e-mail que o provedor devolva.** Um e-mail não verificado é só um
  texto que alguém digitou no provedor. Aceitá-lo deixaria qualquer pessoa cadastrar o e-mail de
  outra no GitHub e entrar aqui como ela.
- **Vincular ao tutor já cadastrado quando o e-mail confere.** Parece gentil, mas é o vetor do X13
  por uma porta mais larga, e a decisão sobre ele merece o seu próprio registro.

## Consequências

**O que fica mais fácil.** Qualquer pessoa cria a conta e entra com Google, GitHub ou formulário
(a tela `/cadastro` é do mesmo dia). O professor consegue testar o sistema sem depender de nós.

**O que fica mais difícil, ou passa a ser obrigatório.**

- **O Google precisa sair do modo Teste.** Enquanto o app OAuth do Google estiver em *Teste*, só
  entra quem está em *Usuários de teste* no Google Cloud Console. Publicar o app é um passo do
  dono do projeto, fora do código. O GitHub não tem essa restrição.
- **A recusa mudou de sentido.** Ela agora acontece quando o provedor não entregou e-mail
  verificado, a conta está inativa ou bloqueada, ou o e-mail é de um tutor cadastrado pela
  clínica. A tela explica as três saídas em uma mensagem só, sem distinguir os casos: a pessoa já
  provou o e-mail ao provedor, e separar os motivos não ajuda ninguém a adivinhar contas.
- **Surge uma classe de conta sem senha.** Ela não tem como entrar pelo formulário, e não há
  "esqueci minha senha" no sistema. Quem criou a conta pelo Google entra sempre pelo Google.
- **Há uma corrida conhecida, não tratada.** Dois primeiros acessos simultâneos com o mesmo e-mail
  podem bater na restrição de unicidade do banco e devolver erro em um deles; a segunda tentativa
  entra normalmente. É raro, e o custo de tratá-la não se justifica agora.
- **O limite de requisições não cobre o login social.** O filtro de rate limit só vale na cadeia da
  API. Isso já era assim, e o 009 não piora, mas passa a importar mais porque agora o login cria
  linhas no banco.
- **Três testes mudaram de propósito:** "e-mail sem conta é recusado" virou "cria um TUTOR e
  entra", e foram acrescentados os casos de nome, de não duplicar e do tutor pré-cadastrado. O
  `passwordEncoder` virou `static` no `SecurityConfig`, porque o login social passou a depender do
  `UsuarioService` e isso fechava um ciclo de dependência.
