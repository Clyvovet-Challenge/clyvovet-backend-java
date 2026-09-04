# Guia do frontend

Como um cliente desta API deve se comportar. Não é a lista de endpoints — essa está
em [03-api-rest.md](03-api-rest.md) —, nem a matriz de autorização, que está em
[08-seguranca.md](08-seguranca.md). Aqui ficam as decisões que o frontend precisa
tomar **por causa de como o backend se comporta**: o que ele pode assumir, o que
ele nunca deve assumir, e o que quebra se ele assumir errado.

Tudo abaixo foi conferido no código em 04/09/2026.

---

## 1. Três coisas que travam antes da primeira tela

### 1.1 PATCH não passa pelo CORS

`SecurityConfig.corsConfigurationSource()` permite `GET, POST, PUT, DELETE, OPTIONS`.
**PATCH não está na lista**, e a API tem seis rotas PATCH: `/tutores/{id}`,
`/animais/{id}`, `/clinicas/{id}`, `/veterinarios/{id}`, `/eventos-clinicos/{id}`
e `/pagamentos/{id}`.

O navegador manda um preflight `OPTIONS` com `Access-Control-Request-Method: PATCH`,
o Spring não reconhece o método e recusa. A requisição real nunca sai. **A falha
aparece só no navegador**: Postman, Insomnia e cliente HTTP nativo de mobile não
fazem preflight e funcionam normalmente — o que faz esse bug parecer "problema do
frontend" quando não é.

Enquanto não for corrigido no backend (uma linha), há duas saídas:

- usar `PUT` no lugar do `PATCH` — mas `PUT` exige o objeto **inteiro**, e omitir
  um campo apaga o valor dele;
- servir o frontend da mesma origem da API, o que dispensa CORS.

A correção definitiva é acrescentar `"PATCH"` a `setAllowedMethods`.

### 1.2 A origem do seu dev server precisa estar na lista

O padrão é `http://localhost:3000,http://localhost:8081` — React CRA e Expo. Quem
usa **Vite (5173)**, Angular (4200) ou outra porta precisa acrescentar a própria
origem:

```properties
clyvovet.cors.origens=http://localhost:5173,http://localhost:3000
```

Não existe `*`, e isso é proposital. Headers aceitos: só `Authorization` e
`Content-Type` — um header customizado qualquer derruba o preflight.

### 1.3 `X-Rate-Limit-Remaining` chega, mas o navegador não lê

O filtro de rate limit escreve `X-Rate-Limit-Remaining` em toda resposta bem
sucedida, mas o CORS só expõe `Retry-After`. Num frontend web, `fetch` devolve
`null` para o primeiro. Só dá para reagir **depois** do 429, não antes dele.

---

## 2. Autenticação

### 2.1 O login

`POST /api/v1/auth/login` com `{"email":"...","senha":"..."}` devolve:

```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "expiraEmSegundos": 900,
  "perfil": "TUTOR"
}
```

`perfil` vem no login **para que a primeira tela já saiba qual app desenhar**, sem
uma chamada extra. Ele é `TUTOR`, `VETERINARIO` ou `ADMIN`.

Em toda requisição seguinte:

```
Authorization: Bearer <accessToken>
```

`expiraEmSegundos` é o access token (15 min por padrão,
`clyvovet.jwt.access-token-minutos`). O refresh dura 7 dias
(`clyvovet.jwt.refresh-token-dias`).

### 2.2 Onde guardar

Não há cookie nenhum: a API é stateless e o CSRF está desligado **porque** não há
cookie para o navegador anexar sozinho. Isso significa que quem guarda o token é o
cliente, e a exposição é XSS, não CSRF.

O arranjo recomendado para um SPA:

- **access token em memória** (variável do módulo, contexto, store) — some ao
  recarregar a página, e 15 minutos de vida já limitam o estrago;
- **refresh token em `localStorage`** — é ele que sobrevive ao F5 e reconstrói a
  sessão no boot do app.

Guardar os dois em `localStorage` é o que a maioria faz e funciona; só entenda que
qualquer script injetado na página leva a sessão inteira junto.

### 2.3 Renovação

Ao receber **401 em qualquer rota**, chame `POST /auth/refresh` com
`{"refreshToken":"..."}` e repita a requisição original. Se o refresh também
devolver 401, aí sim é logout: limpe o estado e mande para a tela de login.

Dois detalhes do comportamento real:

- **O refresh antigo continua valendo.** Cada `/refresh` emite um par novo, mas
  não revoga o anterior — ele vale até expirar ou até um `logout` explícito.
  Guarde o novo, e não trate o antigo como inválido.
- **Enfileire as renovações.** Com várias chamadas em paralelo, todas tomam 401 ao
  mesmo tempo e cada uma dispara seu próprio refresh. Use um único refresh em voo
  e faça as demais esperarem por ele — é o padrão de interceptor com fila.

Um access token **não** renova a si mesmo: mandar o access em `/refresh` devolve
401 com `"Token informado nao e um refresh token"`.

### 2.4 Logout

`POST /auth/logout` com o refresh token no corpo. Ele revoga **só o refresh**; o
access emitido junto continua válido até expirar (até 15 min). Por isso o logout
do cliente tem que apagar o token da memória — não basta chamar a rota.

A revogação é local ao processo. Com mais de uma réplica, o logout vale só na
instância que atendeu; é uma limitação conhecida, registrada em
[07-pendencias-e-divergencias.md](07-pendencias-e-divergencias.md).

### 2.5 Conta bloqueada: o que a UI pode dizer

Cinco senhas erradas seguidas bloqueiam a conta por 15 minutos. Mas a resposta é
**a mesma** de senha errada e de e-mail inexistente:

```json
{"campo":"credenciais","mensagem":"Credenciais invalidas"}
```

Isso é deliberado — mensagens distintas deixariam descobrir quais e-mails existem
na base. **Não invente "sua conta está bloqueada"**: o frontend não tem como saber.
O texto honesto é o genérico, e cabe sugerir "aguarde alguns minutos ou recupere a
senha" depois de algumas falhas seguidas contadas no cliente.

---

## 3. O recorte de acesso: a lista já vem filtrada

Esta é a parte que mais gera bug de frontend, porque ela é invisível na resposta.

Toda listagem é recortada no servidor pelo vínculo de quem chamou:

| Perfil | O que a listagem devolve |
|---|---|
| `TUTOR` | só o que pertence ao próprio tutor |
| `VETERINARIO` | só o que pertence à própria clínica |
| `ADMIN` | tudo |

Três consequências práticas:

**Não filtre de novo no cliente.** Não existe "todos os animais" para um tutor; o
`GET /animais` dele já é a lista dele. Filtrar por `clinicaId` no frontend é
trabalho duplicado que se torna errado no dia em que a regra do servidor mudar.

**Lista vazia não é base vazia.** Pode ser recorte. Um veterinário sem clínica
vinculada recebe `content: []` em tudo — porque falta de vínculo virou "não alcança
nada", e não "alcança tudo". A tela de estado vazio deve dizer algo como "nada por
aqui", nunca "o sistema não tem registros".

**Parâmetro de query não amplia recorte.** `GET /eventos-clinicos/retornos-vencidos`
aceita `clinicaId`, mas para quem não é ADMIN ele é **ignorado**: o servidor usa a
clínica do token. Mandar o id de outra clínica não devolve erro — devolve a lista
da sua. Não construa UI que dependa de trocar de clínica por esse parâmetro; para
não-admin ele não é um seletor.

---

## 4. Forma das respostas

### 4.1 Paginação — o ponto que quebra silenciosamente

O contrato **não** é o `PageImpl` cru do Spring. `WebConfig` serializa via DTO, e o
resultado é:

```json
{
  "content": [ { "id": "...", "nome": "..." } ],
  "page": { "size": 10, "number": 0, "totalElements": 42, "totalPages": 5 }
}
```

`totalElements` está **dentro de `page`**, não na raiz. Código escrito contra
exemplos genéricos de Spring lê `resposta.totalElements` e recebe `undefined` — o
que costuma virar "0 resultados" numa tela que na verdade tem 42.

Parâmetros: `?page=0&size=10&sort=campo,asc`. `page` é base zero. O `size` padrão é
10 em toda listagem; o `sort` padrão varia (`nome` em animais, `data` em eventos).

### 4.2 Erros

Formato único em toda a API — inclusive nos 401 e 403 do Spring Security, que foram
traduzidos para o mesmo shape em vez da página HTML padrão:

```json
{"campo": "email", "mensagem": "Ja existe usuario com o e-mail informado"}
```

A exceção é o **400 de validação**, que é um **array** — um item por campo inválido:

```json
[
  {"campo": "email", "mensagem": "must be a well-formed email address"},
  {"campo": "senha", "mensagem": "A senha deve ter entre 8 e 72 caracteres"}
]
```

Trate os dois formatos: `Array.isArray(corpo)` decide se é erro de formulário
(marcar campo a campo) ou erro único (toast). O `campo` do objeto único costuma
apontar um campo real do formulário — `tutorId`, `veterinarioId`, `email` — e vale
usá-lo para destacar o input certo.

### 4.3 Os status e o que fazer com cada um

| Status | Significa | O que o frontend faz |
|---|---|---|
| 400 | validação de campo | marca os campos do array |
| 401 | sem token, token expirado ou inválido | tenta `/refresh`; se falhar, login |
| 403 | autenticado, mas o perfil não alcança | não mostre a tela — esconda a ação antes |
| 404 | não existe **ou não é seu** | trate como "não encontrado", sem sugerir que existe |
| 409 | regra de negócio | mensagem do servidor, direto ao usuário |
| 429 | volume: rate limit ou teto de histórico | leia `Retry-After` e espere |

Sobre o **404**: quando o recurso não existe, a autorização deixa passar de
propósito e quem responde é o service. Isso evita que 403 e 404 juntos revelem quais
ids existem. Do lado do cliente, isso significa que **404 não distingue** "apagado"
de "não é seu" — e a UI não deveria tentar distinguir.

Sobre o **403**: a mensagem é genérica por design ("Seu perfil nao tem permissao
para acessar este recurso"). Não espere explicação; use o `perfil` do login para não
desenhar o botão que vai falhar.

---

## 5. Desenhe o botão pelo link, não pelo estado

`GET /animais/{id}`, `GET /eventos-clinicos/{id}` e o POST que cria cada um
devolvem `_links` junto do objeto:

```json
{
  "id": "…", "statusEvento": "AGENDADO",
  "_links": {
    "self":      { "href": "/api/v1/eventos-clinicos/…" },
    "animal":    { "href": "/api/v1/animais/…" },
    "historico": { "href": "/api/v1/animais/…/historico" },
    "cancelar":  { "href": "/api/v1/agendamentos/…/cancelar" },
    "concluir":  { "href": "/api/v1/eventos-clinicos/…/concluir" }
  }
}
```

**Se o link não veio, a ação não existe.** Um evento `AGENDADO` traz `cancelar` e
`concluir`; o mesmo evento depois de `CANCELADO` não traz nenhum dos dois. Um
`REALIZADO` traz `marcar-retorno` — mas só se houver retorno previsto.

Habilitar botão por `if (status === 'AGENDADO')` funciona hoje e envelhece mal: é
uma cópia da máquina de estados do servidor mantida à mão, que passa a mentir na
primeira mudança de regra. `if (evento._links.concluir)` não envelhece.

A tabela real, para referência — mas prefira o link:

| Estado | concluir | cancelar | gerar retorno |
|---|---|---|---|
| `AGENDADO` | sim | sim | não |
| `REALIZADO` | não | não | sim |
| `FALTOU` | não | não | não |
| `CANCELADO` | não | não | não |

O mesmo vale para os `_links` do animal: `acessos` (a auditoria de quem leu o
histórico) **só aparece para quem pode segui-lo**. Um link que devolveria 403 é pior
que link nenhum — desenha um botão que só falha depois do clique.

---

## 6. Rate limit

Por IP, em janela de 1 minuto, com baldes separados:

| Faixa | Limite/min | Alcance |
|---|---|---|
| Login | 10 | `POST /auth/login` |
| Auth | 30 | resto de `/auth` |
| Geral | 100 | todo o restante |

Estourar o login não derruba o resto da API — são baldes diferentes. No 429, o
header `Retry-After` traz os segundos de espera, e é o único header de limite que o
navegador consegue ler.

Cuidado com o padrão "recarrega a lista a cada N segundos": 100 requisições por
minuto acabam rápido numa tela com vários widgets em polling. Se o app tiver
dashboard com atualização automática, use intervalo folgado e agrupe as chamadas.

---

## 7. Os três aplicativos

O mesmo backend atende três clientes, e o `perfil` do login diz qual montar.

### Tutor

Auto-cadastro público em `POST /auth/registrar` — `email`, `senha` e **`nome`**.
Não existe campo `perfil` (seria escalação de privilégio) nem `tutorId`: o registro
**cria** o tutor da pessoa. Só o nome é exigido; CPF, telefone e endereço ficam para
um `PATCH /tutores/{id}` depois, para não empurrar formulário longo antes de a
pessoa ver valor no produto.

Telas mínimas: meus pets (`/animais`), agendar (`/agendamentos/vagas` → `POST
/agendamentos`), meus agendamentos (`/agendamentos/meus`), extrato
(`/tutores/{id}/extrato`), e quem tem acesso ao histórico dos meus animais
(`/autorizacoes/minhas` e `POST /autorizacoes/{id}/revogar`).

O tutor **não** lista tutores (expõe CPF de terceiros) e **não** conclui
atendimento — ele marca e cancela; quem registra o que aconteceu é o veterinário.

### Veterinário

Hoje é também o app da clínica: o perfil `ADMIN_CLINICA` não existe (é N2 na spec
08). Tudo que o veterinário vê já vem recortado pela clínica dele.

Agenda (`/veterinarios/{id}/disponibilidades`, `/disponibilidades`, `/bloqueios`),
atendimento (`POST /eventos-clinicos`, `/eventos-clinicos/{id}/concluir`),
retornos vencidos, cobrança (`/pagamentos/{id}/confirmar`, `/estornar`,
`/pagamentos/inadimplencia`) e o histórico clínico.

Fora do alcance dele, e portanto fora da UI: criar clínica ou veterinário, editar o
catálogo de serviços (preço e duração são decisão da plataforma até o N2 existir) e
a auditoria.

### Admin da plataforma

Tudo acima sem recorte, mais `/auth/usuarios`, o CRUD de clínicas e veterinários, o
catálogo de serviços e `/auditoria/**` — quem leu muito prontuário e quem aciona
quebra de vidro com frequência.

---

## 8. Histórico clínico: leia o `nivelDeAcesso`

`GET /animais/{id}/historico` devolve **níveis diferentes do mesmo recurso**, e a
resposta carrega qual deles você recebeu:

| `nivelDeAcesso` | Conteúdo |
|---|---|
| `OPERACIONAL` | nome, espécie, raça, porte, idade |
| `RESUMO_DE_SEGURANCA` | + alergias, condições crônicas, medicação, vacinas, peso |
| `COMPLETO` | + linha do tempo, desfechos e dados do tutor |

**A UI precisa mostrar esse nível.** Sem ele, uma linha do tempo curta é
indistinguível de um animal com pouco histórico — e um veterinário tiraria conclusão
clínica de uma ausência que é de permissão, não de fato. Um rótulo do tipo "Você
está vendo o resumo de segurança; o histórico completo depende de autorização do
tutor" resolve.

Há ainda `GET /animais/resumo?microchip=…`, que qualquer veterinário autenticado
alcança sem vínculo prévio — o caso do animal que chega numa clínica que nunca o
atendeu.

E há o **teto**: 150 animais distintos por dia, por usuário. Ao estourar, a API
responde **429**, e não 403, de propósito — o profissional podia ver, e o que
aconteceu foi um limite de volume. A mensagem manda procurar o administrador, e é
isso que a tela deve dizer; não é erro de permissão nem de rede, e não adianta
repetir a chamada.

A quebra de vidro (`POST /animais/{id}/acesso-emergencial`) exige motivo escrito e
avisa o tutor. Se a UI oferecer o botão, ele precisa deixar isso explícito **antes**
do clique — é um acesso sem consentimento que fica registrado com nome e motivo.

---

## 9. Checklist

- [ ] Origem do dev server em `clyvovet.cors.origens`
- [ ] PATCH liberado no CORS do backend, ou `PUT` com objeto completo
- [ ] Interceptor: `Authorization` em toda chamada, refresh com fila no 401
- [ ] Access token em memória, refresh em `localStorage`
- [ ] Logout limpa o estado local além de chamar a rota
- [ ] Paginação lê `page.totalElements`, não `totalElements`
- [ ] Erro trata array (400) e objeto (o resto)
- [ ] Nenhum filtro de clínica ou tutor no cliente
- [ ] Estado vazio não afirma que a base está vazia
- [ ] Botões de ação vêm de `_links`
- [ ] `nivelDeAcesso` visível na tela de histórico
- [ ] Polling folgado, dentro das 100 req/min
