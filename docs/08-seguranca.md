# Segurança

Autenticação, autorização e endurecimento da API. Implementado na Sprint 3
(Spring Security — 30 pontos).

Até a Sprint 1/2 os 30 endpoints respondiam sem autenticação, expondo CPF, e-mail
e telefone de tutores e veterinários e permitindo que qualquer um criasse ou
apagasse eventos clínicos e pagamentos.

---

## Visão geral

| Camada | Mecanismo |
|---|---|
| Autenticação | JWT stateless (HS384), access + refresh |
| Senhas | BCrypt |
| Autorização por perfil | Regras de rota no `SecurityFilterChain` |
| Autorização por dono | `@PreAuthorize` + filtro na query das listagens |
| Força bruta direcionada | Bloqueio de conta: 5 falhas → 15 min |
| Abuso volumétrico | Rate limit por IP (Bucket4j) |
| Enumeração de usuários | Resposta e tempo de login uniformes |
| Cabeçalhos | CSP, nosniff, frame-options, referrer-policy, HSTS |
| CORS | Allowlist explícita, sem `*` |

Não há sessão: cada requisição se sustenta pelo próprio token.

---

## Perfis

| Perfil | Quem é | Enxerga |
|---|---|---|
| `TUTOR` | Dono do pet | Apenas o próprio cadastro, pets, eventos e pagamentos |
| `VETERINARIO` | Profissional que atende | O cadastro de qualquer animal (nível 0, para atender quem chega pela primeira vez) e o **atendimento da própria clínica** — ou de fora, com consentimento do tutor |
| `ADMIN_CLINICA` | Quem responde pelo estabelecimento | O que aconteceu na própria clínica: agenda, catálogo, profissionais, faturamento e painel. **Não** o cadastro de tutores e animais da plataforma |
| `ADMIN` | Administração | Tudo, mais a gestão de clínicas, veterinários e usuários |

`Usuario` é uma entidade separada das de domínio, com FK opcional para `Tutor`,
`Veterinario` e `Clinica`. É esse vínculo que viabiliza o ownership. Detalhes do
mapeamento em [02-modelo-de-dados.md](02-modelo-de-dados.md).

### Por que o gestor da clínica não é um veterinário com mais poder

São alcances diferentes, e não graus do mesmo alcance. O veterinário alcança o
**cadastro de qualquer animal** porque precisa: ele atende um paciente que nunca viu,
e o nível 0 do histórico existe para isso. O gestor não atende ninguém — ele responde
pelo negócio. Dar a ele a mesma visão entregaria CPF e e-mail de todos os tutores da
plataforma a quem só precisa administrar uma casa.

Por isso `ADMIN_CLINICA` fica **fora** de `temVisaoAmpla()`, e o que é da clínica dele
chega por outro caminho: `recorte()` devolve `daClinica(...)`, que filtra as listagens,
e `podeAlcancarOAtendimento` deixa passar o que aconteceu na casa.

A clínica de um usuário tem **dois caminhos**, resolvidos num único lugar
(`Usuario.getClinicaEfetiva()`): o vínculo direto, do gestor que responde pela clínica,
e o transitivo (`veterinario.clinica`), do profissional que atende nela. O direto vem
primeiro — se alguém for as duas coisas, vale o alcance de quem responde pelo negócio.
O `GET /api/v1/auth/me` devolve `clinicaId` e `clinicaNome` justamente daí: sem esse
campo o perfil existia e era inútil no cliente, que não tinha como descobrir por qual
clínica ele responde.

---

## Endpoints de autenticação

| Rota | Acesso | Função |
|---|---|---|
| `POST /api/v1/auth/login` | público | e-mail + senha → access token + refresh token |
| `POST /api/v1/auth/refresh` | público | renova o access token |
| `POST /api/v1/auth/registrar` | público | auto-cadastro; **perfil sempre `TUTOR`** |
| `POST /api/v1/auth/usuarios` | `ADMIN` | cria usuário com perfil arbitrário |
| `GET /api/v1/auth/me` | autenticado | dados do usuário logado |

### Tokens

| Token | Validade | Uso |
|---|---|---|
| access | 15 min | autoriza as chamadas da API |
| refresh | 7 dias | só obtém um novo access |

Os dois carregam uma claim `tipo`, e o `JwtAuthenticationFilter` recusa um refresh
token usado como bearer. Sem essa distinção, um refresh vazado valeria como
credencial de 7 dias.

O segredo vem de `JWT_SECRET`. `Keys.hmacShaKeyFor` recusa chaves com menos de
256 bits, então a aplicação não sobe com segredo fraco.

### Exemplo

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@clyvovet.com","senha":"admin12345"}' \
  | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

curl localhost:8080/api/v1/animais -H "Authorization: Bearer $TOKEN"
```

No Swagger, use o botão **Authorize** e cole apenas o token, sem o prefixo `Bearer`.

---

## Matriz de autorização

| Recurso | Operação | TUTOR | VETERINARIO | ADMIN_CLINICA | ADMIN |
|---|---|---|---|---|---|
| `/api/v1/auth/login`, `/refresh`, `/registrar` | — | público | público | público | público |
| `/api/v1/auth/usuarios` | POST | ✗ | ✗ | ✗ | ✓ |
| `/swagger-ui/**`, `/v3/api-docs/**` | GET | público | público | público | público |
| `/api/v1/tutores` | GET lista | ✗ | ✓ | ✗ | ✓ |
| `/api/v1/tutores/{id}` | GET, PUT, PATCH | só o próprio | ✓ | ✗ | ✓ |
| `/api/v1/tutores` | POST, DELETE | ✗ | ✓ | ✗ | ✓ |
| `/api/v1/animais` | GET lista | só os próprios | ✓ | ✗ | ✓ |
| `/api/v1/animais/{id}` | GET, PUT, PATCH, DELETE | só os próprios | ✓ | ✗ | ✓ |
| `/api/v1/animais` | POST | ✓ | ✓ | ✗ | ✓ |
| `/api/v1/clinicas`, `/api/v1/veterinarios` | GET | ✓ | ✓ | ✓ | ✓ |
| `/api/v1/clinicas` | POST, DELETE | ✗ | ✗ | ✗ | ✓ |
| `/api/v1/clinicas/{id}` | PUT, PATCH | ✗ | ✗ | só a própria | ✓ |
| `/api/v1/veterinarios` | POST, PUT, PATCH, DELETE | ✗ | ✗ | só os da casa | ✓ |
| `/api/v1/servicos` | POST, PUT, DELETE | ✗ | ✗ | só o próprio catálogo | ✓ |
| `/api/v1/disponibilidades`, `/bloqueios` | POST, DELETE | ✗ | só a própria grade | qualquer grade da casa | ✓ |
| `/api/v1/eventos-clinicos` | GET | só dos próprios pets | da própria clínica | da própria clínica | ✓ |
| `/api/v1/eventos-clinicos` | POST, PUT, PATCH, DELETE | ✗ | ✓ | ✗ | ✓ |
| `/api/v1/eventos-clinicos/{id}/concluir`, `/retorno`, `/marcar-faltas` | POST | ✗ | ✓ | ✗ | ✓ |
| `/api/v1/pagamentos` | GET | só dos próprios pets | da própria clínica | da própria clínica | ✓ |
| `/api/v1/pagamentos` | POST, PUT, PATCH, DELETE | ✗ | ✓ | ✗ | ✓ |
| `/api/v1/pagamentos/{id}/confirmar`, `/estornar` | POST | ✗ | ✓ | ✗ | ✓ |
| `/api/v1/pagamentos/inadimplencia` | GET | ✗ | ✓ | ✓ | ✓ |
| `/api/v1/clinicas/{id}/painel` | GET | ✗ | só a própria | só a própria | ✓ |
| `/api/v1/auditoria/**` | GET | ✗ | ✗ | ✗ | ✓ |

A cadeia termina em `anyRequest().authenticated()`: rota nova nasce protegida.

> **A regra de rota abre o VERBO; ela não sabe de qual clínica é o recurso.** Toda linha
> acima que diz "só a própria" ou "só os da casa" é decidida em duas partes: o
> `hasAnyRole(...)` deixa o perfil entrar, e um `@PreAuthorize("@seguranca....")` no
> controller decide **de quem** é o registro. Sem a segunda metade, o `ADMIN_CLINICA`
> seria um caminho para mexer no catálogo, na agenda e no cadastro da concorrente.

> **A listagem também é uma porta.** `GET /api/v1/animais` filtra apenas por `tutorId`,
> e `tutorId` nulo significa "sem recorte" — o que fazia o gestor da clínica **listar os
> 26 animais de todas as clínicas**, com nome e id do tutor de cada um, enquanto
> `GET /api/v1/animais/{id}` respondia 403 para ele. Medido contra a pilha local e
> fechado por rota. É o mesmo erro da ruptura B1, na outra ponta.

---

## Ownership

Regra de rota resolve *"qual perfil acessa qual rota"*, mas não *"este tutor pode ver
este pet"* — dois tutores têm o mesmo perfil e passam igualmente pela regra. A
verificação acontece em três frentes, e todas são necessárias.

### Acesso por ID

`@PreAuthorize` sobre o bean `SegurancaService`, registrado como `@Service("seguranca")`:

```java
@GetMapping("/{id}")
@PreAuthorize("@seguranca.podeAcessarAnimal(#id)")
public ResponseEntity<AnimalResponse> buscarPorId(@PathVariable UUID id) { ... }
```

O serviço percorre a cadeia até o dono — `Pagamento → EventoClinico → Animal → Tutor` —
e libera direto para `VETERINARIO` e `ADMIN`.

### Listagens

`@PreAuthorize` não filtra coleção. O `tutorId` é propagado até a query:

```sql
AND (:tutorId IS NULL OR a.tutor.id = :tutorId)
```

Mesmo padrão de filtro opcional já usado nos seis repositories. Os services passam
`null` para `VETERINARIO`/`ADMIN` e o id do tutor logado para `TUTOR`. Filtrar na
query, e não depois dela, mantém a paginação correta.

### Dono informado no corpo

As duas frentes acima olham a URL. O `tutorId` de `AnimalRequest` vem do **corpo**, e
por ele passava a mesma decisão sem nenhuma verificação: um tutor autenticado
cadastrava pet no nome de qualquer outro (`POST /api/v1/animais`) e podia transferir o
próprio pet para outro tutor (`PUT /api/v1/animais/{id}`). A escrita de animal checa as duas
perguntas separadamente:

**No PATCH a segunda checagem é condicional.** O `tutorId` pode não vir no corpo, e aí
não há troca de dono a autorizar. `SegurancaService.podeAtribuirTutor` trata `null`
como "não mexa no dono" — sempre permitido a quem já pode editar o animal. Sem isso,
`podeAcessarTutor(null)` devolveria `false` e um tutor não conseguiria alterar o
próprio pet sem reenviar o próprio id. Trocar o dono via PATCH segue barrado, coberto
por `AtualizacaoParcialTest.tutorNaoTransferePetViaPatch`.

```java
@PreAuthorize("@seguranca.podeAcessarAnimal(#id) and @seguranca.podeAcessarTutor(#request.tutorId)")
```

Coberto por `OwnershipTest.tutorNaoCadastraPetParaTerceiro` e
`OwnershipTest.tutorNaoTransfereOProprioPet`. Os demais recursos não têm o problema:
a escrita de evento e de pagamento já é restrita a `VETERINARIO`/`ADMIN` pela rota.

### ⚠️ Cache e vazamento entre contas

A chave do `@Cacheable` **precisa incluir o `tutorId`**:

```java
@Cacheable(value = "animais",
        key = "#nome + '-' + #especie + '-' + @seguranca.tutorIdParaFiltro() + '-' + #pageable")
```

Sem isso, a primeira listagem de um tutor seria servida a qualquer outro que usasse
os mesmos filtros e paginação. É o ponto de maior risco da autorização, e está
coberto por `OwnershipTest.cacheNaoVazaEntreTutores`.

O `#pageable` inteiro na chave, no lugar de `pageNumber + pageSize`, trouxe o `sort`
junto e corrigiu um bug anterior: `?sort=nome,asc` e `?sort=nome,desc` colidiam na
mesma chave e a segunda chamada recebia o resultado da primeira, na ordem errada.

---

## Proteção contra ataques

### Força bruta — duas defesas complementares

| Controle | Escopo | Limite | Onde |
|---|---|---|---|
| Bloqueio de conta | por conta | 5 falhas → 15 min | `ControleTentativasLogin` |
| Rate limit | por IP | 10 logins/min | `RateLimitFilter` |

A divisão importa. Um limite curto por IP no login parece mais seguro, mas contra
força bruta agrega pouco — o bloqueio por conta já barra o ataque — e quebra uso
legítimo: uma clínica inteira atrás de um mesmo IP público compartilha o balde.
Quem limita tentativa por conta é o lockout; o filtro por IP contém volume.

> **Armadilha de implementação.** O registro da falha roda em transação própria
> (`Propagation.REQUIRES_NEW`, em bean separado). Na primeira versão o incremento
> ficava dentro do `login()` anotado com `@Transactional`, e o rollback disparado
> pelo `BadCredentialsException` desfazia a contagem — o bloqueio nunca chegava a
> valer. Como o Spring aplica `@Transactional` via proxy, um método privado do
> próprio `AuthService` não resolveria: a anotação seria ignorada.
> Coberto por `BloqueioContaTest.contagemDeFalhasEPersistida`.

### Enumeração de usuários

Credencial errada, e-mail inexistente e conta bloqueada devolvem **a mesma resposta**:
`401` com `{"campo":"credenciais","mensagem":"Credenciais invalidas"}`. Quando o
e-mail não existe, o serviço ainda executa um BCrypt descartável, para que o tempo de
resposta também não denuncie a diferença.

### Rate limiting

| Escopo | Limite |
|---|---|
| `POST /api/v1/auth/login` | 10/min por IP |
| `/api/v1/auth/**` | 30/min por IP |
| Demais rotas | 100/min por IP |

Excedido: **429** com `Retry-After`. Swagger e console H2 ficam fora do limite.

Os buckets vivem num cache Caffeine com expiração — num `Map` comum, cada IP visto
criaria uma entrada permanente e o próprio rate limiter viraria vetor de DoS.

Desligável com `clyvovet.rate-limit.enabled=false` (usado nos testes).

**Limitação conhecida:** o estado é local ao processo. Com mais de uma réplica, cada
uma teria sua própria contagem; o correto nesse cenário seria Bucket4j sobre Redis.

### Escalação de privilégio

`RegistroRequest` **não tem campo `perfil`**. O auto-cadastro força `TUTOR` no
service. Aceitar o perfil vindo do corpo permitiria que qualquer um se cadastrasse
como `ADMIN` — mass assignment. Coberto por
`AutorizacaoTest.autoCadastroNaoPermiteEscolherPerfil`.

### Cabeçalhos

| Header | Valor |
|---|---|
| `Content-Security-Policy` | `default-src 'self'; frame-ancestors 'none'` |
| `X-Content-Type-Options` | `nosniff` |
| `X-Frame-Options` | `DENY` (`SAMEORIGIN` onde o console H2 está ativo) |
| `Referrer-Policy` | `strict-origin-when-cross-origin` |
| `Strict-Transport-Security` | configurado; **só emitido em HTTPS** |

HSTS ausente em `http://localhost` é o comportamento correto do Spring Security —
o header não tem significado fora de uma conexão segura.

### CORS

Allowlist explícita em `clyvovet.cors.origens`. Nunca `*` combinado com credenciais.

### Erros que não vazam

| Situação | Antes | Agora |
|---|---|---|
| Sem token | HTML padrão do Spring | 401 JSON |
| Sem permissão | HTML padrão | 403 JSON |
| CPF/CNPJ/CRMV duplicado | **500 com SQL e nome da constraint** | 409 genérico |
| Regra de negócio violada | — | 409 |

Todos no mesmo formato `ErroValidacao` do resto da API.

### Já coberto antes desta sprint

- **SQL injection** — as seis queries usam JPQL com parâmetros nomeados
- **Mass assignment** — DTOs de entrada sempre separados das entidades

---

## Comportamento a conhecer

**A validação do corpo roda antes da autorização.** Um `PUT` com JSON inválido em
recurso de terceiro devolve **400**, não 403 — o binding do `@RequestBody` acontece
antes do interceptor do `@PreAuthorize`. A garantia que importa se mantém: a escrita
nunca executa. Métodos sem corpo (`GET`, `DELETE`) devolvem 403 direto.

---

## Usuários de desenvolvimento

Criados por `DevDataSeeder`, ativo apenas nos perfis `dev` e `h2`. Os hashes são
gerados em tempo de execução — hash de senha não é versionado em migration.

| E-mail | Senha | Perfil | Vínculo |
|---|---|---|---|
| `admin@clyvovet.com` | `admin12345` | ADMIN | — |
| `camila.ferreira@vetcare.com.br` | `vet12345` | VETERINARIO | Clínica VetCare Prime |
| `lucas.santos@email.com` | `tutor12345` | TUTOR | dono do Bolinha |
| `maria.oliveira@email.com` | `tutor12345` | TUTOR | dona da Mimi e do Rex |

Os dois tutores têm pets distintos de propósito: é o que permite exercitar o
isolamento sem cadastrar nada à mão.

**Não há `ADMIN_CLINICA` no seed, e isso é deliberado:** o perfil exige um
`clinicaId`, e qual clínica depende do que se quer exercitar. Ele é criado pelo ADMIN,
e é o único perfil cujo cadastro o banco cobra — `chk_usuario_clinica` recusa
`ADMIN_CLINICA` sem clínica, e a API responde 409 antes disso:

```bash
curl -X POST http://localhost:8080/api/v1/auth/usuarios   -H "Authorization: Bearer $TOKEN_DO_ADMIN" -H 'Content-Type: application/json'   -d '{"email":"gestor@clinica.com","senha":"Gestor@12345",
       "perfil":"ADMIN_CLINICA","clinicaId":"11111111-1111-1111-1111-000000000001"}'
```

Depois, `GET /api/v1/auth/me` devolve o `clinicaId` dele — que é por onde começa toda
rota da clínica, o painel inclusive.

---

## Testes

98 testes, todos em `mvn test` — sem necessidade de banco externo. Os de segurança:

| Classe | Cobre |
|---|---|
| `JwtServiceTest` | claims, distinção access/refresh, assinatura inválida, token adulterado, expiração, segredo fraco |
| `AutorizacaoTest` | 401 sem token, 403 por perfil, Swagger público, refresh não autentica API, escalação de perfil, 409 em duplicata |
| `OwnershipTest` | acesso por id, **dono informado no corpo**, listagens isoladas, **vazamento de cache**, escopo do veterinário |
| `BloqueioContaTest` | bloqueio após 5 falhas, persistência da contagem, reset no sucesso, isolamento por conta |

Os demais cobrem CRUD, mapeamento, filtros e validação — ver
[06-guia-de-desenvolvimento](06-guia-de-desenvolvimento.md#testes).

```bash
mvn test
```

Antes da Sprint 3 o único teste existente falhava sem conectividade com o Oracle — por
isso o Dockerfile usava `-DskipTests`. `src/test/resources/application.properties` fixa
o perfil `dev`, e o build passou a rodar os testes de verdade.
