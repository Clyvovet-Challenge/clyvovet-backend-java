# 06 — Spring Security

> **Pré-requisito:** [03 — API REST](03-api-rest.md), principalmente **HTTP é stateless** e
> **401 × 403**.
>
> Vale **30 pontos** da Sprint 3 e é o assunto que mais aparece na avaliação oral.
> O que está implementado: [`../docs/08-seguranca.md`](../docs/08-seguranca.md). Aqui o foco é
> **entender cada peça**.

---

## O problema, antes da solução

Até a Sprint 1/2, esta API respondia **tudo, para qualquer um**. Sem login. Na prática:

```bash
# qualquer pessoa na internet, sem credencial nenhuma
curl http://servidor/tutores
# → CPF, e-mail, telefone e endereço de todos os tutores

curl -X DELETE http://servidor/eventos-clinicos/55555555-...
# → histórico clínico apagado
```

Duas perguntas precisam ser respondidas em toda requisição, e elas são **diferentes**:

| Pergunta | Nome | Falha vira |
|---|---|---|
| **Quem é você?** | Autenticação (*authentication*) | **401** Unauthorized |
| **Você pode fazer isso?** | Autorização (*authorization*) | **403** Forbidden |

Analogia do prédio: a **portaria** confere seu documento — autenticação. O **crachá** define
quais andares abrem — autorização. Você pode estar perfeitamente identificado e ainda assim
não poder entrar no 12º.

- **401** = *"não sei quem você é"* → fazer login resolve.
- **403** = *"sei quem você é, e você não pode"* → login não resolve nada.

---

## Como o Spring Security funciona: uma fila de filtros

Spring Security é um **conjunto de filtros** colocado **antes** dos seus controllers. Toda
requisição passa por eles; se for barrada, o controller nem é chamado.

```
requisição chega
      │
      ▼
┌──────────────────┐  quantas requisições esse IP já fez neste minuto?
│ RateLimitFilter  │  → 429 se exagerou
└────────┬─────────┘
         ▼
┌──────────────────────────┐  tem token no header? é válido?
│ JwtAuthenticationFilter  │  → se sim, marca "este é o usuário X"
└────────┬─────────────────┘
         ▼
┌──────────────────────────┐  esse perfil pode acessar essa rota?
│ AuthorizationFilter      │  → 401 se anônimo, 403 se sem permissão
└────────┬─────────────────┘
         ▼
   @PreAuthorize no método  ← "este pet específico é seu?"
         ▼
     Controller             ← só chega aqui quem passou por tudo
```

```java
// src/main/java/br/com/fiap/clyvovet/config/SecurityConfig.java
.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
.addFilterAfter(jwtAuthenticationFilter, RateLimitFilter.class);
```

💡 **Conceito: por que a ordem dos filtros importa**

O rate limit vem **antes** da autenticação de propósito.

Verificar uma senha custa um **BCrypt**, que é lento por projeto (falaremos disso já já). Se
um atacante dispara 10.000 tentativas por minuto e cada uma custa 100 ms de CPU, o servidor
cai sem que nenhuma senha seja descoberta — é uma negação de serviço acidental.

Barrando por volume **antes**, a rajada é recusada com um contador em memória, que custa
microssegundos.

Regra geral: **coloque a verificação barata antes da cara.**

---

# Parte 1 — Autenticação: quem é você

## Senhas: nunca em texto puro

Se o banco guardar `senha = "joao123"` e alguém vazar a tabela, todas as contas caem — e
provavelmente as contas dessas pessoas em **outros sites**, porque as pessoas repetem senha.

A solução é **hash**: uma função de mão única.

```
"joao123"  ──hash──▶  "$2a$10$N9qo8uLOickgx2ZMRZoMye..."
                              ↑ e não existe caminho de volta
```

Para conferir o login, você **não** desfaz o hash — você faz o hash da senha digitada e
compara os dois resultados.

```java
// src/main/java/br/com/fiap/clyvovet/config/SecurityConfig.java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

```java
// e é por isso que a comparação NÃO é com equals
if (!passwordEncoder.matches(request.getSenha(), usuario.getSenha())) { ... }
```

### Por que BCrypt e não MD5 ou SHA-256

| | MD5 / SHA-256 | BCrypt |
|---|---|---|
| Velocidade | **rapidíssimo** | **lento de propósito** |
| Salt | não tem | embutido |

Os dois pontos são o mesmo argumento invertido:

**Salt** é um valor aleatório misturado à senha antes do hash. Sem ele, a mesma senha gera
sempre o mesmo hash — então um atacante pré-calcula os hashes das senhas comuns uma vez
(*rainbow table*) e consulta a tabela. Com salt, `"joao123"` gera hash diferente para cada
usuário, e a tabela pré-calculada não serve.

**Lentidão** é feature. Um hash rápido permite testar bilhões de senhas por segundo numa
GPU. O BCrypt custa ~100 ms por tentativa — imperceptível num login, proibitivo em força
bruta.

---

## JWT: como o servidor lembra quem você é sem lembrar de nada

Aqui está o ponto que confunde quem está começando.

Lembre do [03](03-api-rest.md): **HTTP é stateless**. O servidor não guarda memória entre
requisições. Então, depois que você faz login, como a próxima chamada sabe que é você?

**Opção clássica: sessão.** O servidor guarda uma tabela `sessão → usuário` na memória e
manda um cookie. Funciona, e tem dois custos: consome memória por usuário logado, e com
várias réplicas cada uma tem sua própria tabela.

**Opção usada aqui: JWT.** O servidor não guarda nada. Ele entrega um **crachá assinado**, e
o cliente reapresenta a cada chamada. O servidor só confere a assinatura.

### Anatomia de um token

Um JWT tem três partes separadas por ponto:

```
eyJhbGciOiJIUzI1NiJ9  .  eyJzdWIiOiI0NDQ0Li4uIiwicGVyZmlsIjoiVFVUT1IifQ  .  dBjftJeZ4CVP...
      cabeçalho                          conteúdo (claims)                      assinatura
```

⚠️ **O conteúdo é Base64, não é criptografia.** Qualquer pessoa cola o token em jwt.io e lê
tudo. **Nunca coloque senha ou dado sensível numa claim.**

Então de que adianta? A **assinatura**. Ela é gerada com uma chave secreta que só o servidor
tem. Se alguém editar o conteúdo — trocar `"perfil":"TUTOR"` por `"perfil":"ADMIN"` — a
assinatura deixa de bater e o token é recusado.

> **Resumo:** JWT protege contra **adulteração**, não contra **leitura**.

```java
// src/main/java/br/com/fiap/clyvovet/security/JwtService.java
private String gerar(Usuario usuario, String tipo, Duration validade) {
    Instant agora = Instant.now();
    return Jwts.builder()
            .id(UUID.randomUUID().toString())          // jti — identifica ESTE token
            .subject(usuario.getId().toString())       // sub — de quem é
            .claim(CLAIM_PERFIL, usuario.getPerfil().name())
            .claim(CLAIM_TIPO, tipo)                   // access ou refresh
            .issuedAt(Date.from(agora))
            .expiration(Date.from(agora.plus(validade)))
            .signWith(chave)                           // ← a assinatura
            .compact();
}
```

### Dois tipos de token, e o raciocínio por trás

| Token | Validade | Serve para |
|---|---|---|
| `access` | **15 min** | autorizar as chamadas da API |
| `refresh` | **7 dias** | obter um novo access, e **nada mais** |

O dilema que isso resolve:

- Token de **7 dias** para tudo → se vazar, o atacante tem uma semana de acesso.
- Token de **15 min** para tudo → o usuário refaz login a cada 15 minutos. Insuportável.

Com os dois: o access circula em toda chamada (mais exposto) e **vale 15 minutos**; o refresh
circula raramente, só para renovar.

E a claim `tipo` impede o abuso óbvio — usar o refresh como se fosse access:

```java
// src/main/java/br/com/fiap/clyvovet/security/JwtAuthenticationFilter.java
// Refresh token nao autentica chamadas da API — so serve em /auth/refresh.
if (!jwtService.ehAccessToken(claims)) {
    return;
}
```

Sem essa checagem, um refresh vazado valeria como credencial por 7 dias — e a separação toda
perderia o sentido.

### A chave não pode ser fraca

```java
this.chave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(segredo));
```

`Keys.hmacShaKeyFor` **recusa** segredos com menos de 256 bits. A aplicação **não sobe** com
chave fraca — falha no boot, que é onde se quer descobrir. O segredo vem de `JWT_SECRET`,
variável de ambiente, **sem valor padrão**.

---

## O filtro que autentica

```java
// src/main/java/br/com/fiap/clyvovet/security/JwtAuthenticationFilter.java
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                FilterChain filterChain) throws ServletException, IOException {
    String token = extrairToken(request);
    if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        autenticar(token, request);
    }
    filterChain.doFilter(request, response);   // ← passa a bola para o próximo filtro
}

private void autenticar(String token, HttpServletRequest request) {
    try {
        Claims claims = jwtService.lerClaims(token);        // valida assinatura e expiração
        if (!jwtService.ehAccessToken(claims)) return;

        UsuarioAutenticado usuario = usuarioDetailsService.carregarPorId(jwtService.extrairUsuarioId(claims));
        if (!usuario.isEnabled() || !usuario.isAccountNonLocked()) return;

        var authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

    } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
        SecurityContextHolder.clearContext();
    }
}
```

Três coisas para entender:

**1. `OncePerRequestFilter`** garante que o filtro roda **uma vez** por requisição, mesmo com
`forward` interno.

**2. O filtro não devolve erro.** Repare: token inválido → `return`, e a requisição **segue**.
Isso parece errado, e não é. O filtro apenas *não marca ninguém como autenticado*; quem
decide o status é o `AuthorizationFilter`, mais adiante, que sabe se aquela rota exigia
autenticação. Uma fonte de decisão só, em vez de duas discordando.

**3. `SecurityContextHolder`** guarda "quem está autenticado" num `ThreadLocal` — uma variável
visível só para a thread atual. Como cada requisição roda na própria thread, cada uma enxerga
o seu usuário. É o que permite, lá no fundo do service, perguntar "quem está logado?" sem
passar o usuário de parâmetro em parâmetro.

---

## `UserDetails` — a ponte com o seu domínio

O Spring Security não conhece a sua classe `Usuario`. Ele fala uma interface própria,
`UserDetails`. O adaptador:

```java
// src/main/java/br/com/fiap/clyvovet/security/UsuarioAutenticado.java
public class UsuarioAutenticado implements UserDetails {

    private final transient Usuario usuario;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // O prefixo ROLE_ e o que faz hasRole("ADMIN") funcionar nas regras.
        return List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name()));
    }

    @Override public String getPassword()          { return usuario.getSenha(); }
    @Override public String getUsername()          { return usuario.getEmail(); }
    @Override public boolean isAccountNonLocked()  { return !usuario.estaBloqueado(); }
    @Override public boolean isEnabled()           { return usuario.isAtivo(); }

    /** Id do tutor vinculado, ou null se o usuario nao for um tutor. */
    public UUID getTutorId() {
        return usuario.getTutor() != null ? usuario.getTutor().getId() : null;
    }
}
```

Este é o **padrão Adapter**: uma classe que traduz entre dois contratos que não se conhecem.
Seu domínio continua limpo (a entidade `Usuario` não implementa nada do Spring), e o
framework recebe o que espera.

⚠️ **A pegadinha do `ROLE_`:** `hasRole("ADMIN")` procura a authority `ROLE_ADMIN` — o prefixo
é adicionado por baixo dos panos. Já `hasAuthority("ADMIN")` procura exatamente `ADMIN`.
Misturar os dois é a origem clássica do "por que meu 403 não passa".

---

## Login: e as duas defesas que não são óbvias

```java
// src/main/java/br/com/fiap/clyvovet/service/AuthService.java
public LoginResponse login(LoginRequest request) {
    Optional<Usuario> encontrado = usuarioRepository.findByEmail(request.getEmail());

    if (encontrado.isEmpty()) {
        // Gasta o mesmo tempo de um BCrypt real para nao vazar, pelo tempo de
        // resposta, se o e-mail existe ou nao.
        passwordEncoder.encode(request.getSenha());
        throw new BadCredentialsException(CREDENCIAIS_INVALIDAS);
    }
    ...
}
```

💡 **Conceito: enumeração de usuários**

Um atacante quer descobrir **quais e-mails existem** na sua base — é o primeiro passo antes
de tentar senhas. E ele consegue isso sem acertar nenhuma senha, se a API o deixar.

**Vazamento 1 — pela mensagem.** Se "e-mail não cadastrado" e "senha incorreta" são
mensagens diferentes, basta comparar. Por isso `CREDENCIAIS_INVALIDAS` é a mesma para senha
errada, e-mail inexistente **e conta bloqueada**.

**Vazamento 2 — pelo tempo.** Este é o sutil. Se o e-mail não existe, o código normal
retornaria na hora (2 ms). Se existe, ele calcula o BCrypt (100 ms). O atacante cronometra:
resposta lenta = e-mail existe.

Por isso a linha acima: quando o e-mail **não** existe, o código gasta um BCrypt **à toa**,
só para o tempo ficar igual. É o que se chama de defesa contra *timing attack*.

Repare também no que o método **não** tem:

```java
/**
 * Sem @Transactional de proposito: o login so le, e o registro da tentativa
 * e commitado a parte pelo ControleTentativasLogin. Abrir uma transacao aqui
 * faria o rollback do BadCredentialsException apagar a contagem de falhas.
 */
```

---

## Bloqueio de conta, e o detalhe transacional

```java
// src/main/java/br/com/fiap/clyvovet/security/ControleTentativasLogin.java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void registrarFalha(Usuario usuario) {
    usuarioRepository.findById(usuario.getId()).ifPresent(atual -> {
        atual.setTentativasFalhas(atual.getTentativasFalhas() + 1);
        if (atual.getTentativasFalhas() >= maxTentativas) {
            atual.setBloqueadoAte(LocalDateTime.now().plusMinutes(bloqueioMinutos));
            atual.setTentativasFalhas(0);
        }
        usuarioRepository.save(atual);
    });
}
```

💡 **Conceito: transação e `REQUIRES_NEW`**

Uma **transação** agrupa operações no banco: ou todas valem, ou nenhuma. Se algo lança
exceção no meio, o Spring desfaz tudo (*rollback*).

Aqui isso seria um desastre. Sequência do login com senha errada:

```
1. registrarFalha() → tentativasFalhas = 3
2. throw BadCredentialsException
3. rollback → tentativasFalhas volta para 2 😱
```

O contador **nunca chegaria a 5**, e o bloqueio jamais aconteceria — uma falha de segurança
silenciosa, sem erro nenhum no log.

`REQUIRES_NEW` abre uma transação **separada**, que commita independentemente do destino da
que a chamou. A contagem sobrevive à exceção.

E, ligando com o [01](01-spring-boot-e-injecao-de-dependencia.md): isso só funciona porque
`ControleTentativasLogin` é **outro bean**. Se fosse método privado do `AuthService`, a
chamada não passaria pelo proxy e a anotação seria ignorada — em silêncio.

### Duas defesas, dois ataques diferentes

| Mecanismo | Contra | Escopo |
|---|---|---|
| `ControleTentativasLogin` | força bruta **direcionada** a uma conta | por **conta** |
| `RateLimitFilter` (Bucket4j) | abuso **volumétrico** | por **IP** |

Por que não um limite curto por IP no login, que pareceria mais simples? O comentário do
`RateLimitFilter` responde: *"contra força bruta ele agrega pouco, já que o bloqueio por conta
já barra o ataque, e quebra uso legítimo — uma clínica inteira atrás de um mesmo IP público
compartilha o balde e trava depois de poucos logins"*.

**Cada mecanismo defende o que sabe defender.** Empilhar defesas mal escolhidas atrapalha
usuário legítimo sem atrapalhar atacante.

---

## Logout num sistema sem sessão

Se o servidor não guarda nada, "deslogar" é um problema: o token continua válido até expirar.

A solução é uma **deny-list** — uma lista de tokens revogados:

```java
// src/main/java/br/com/fiap/clyvovet/security/RevogacaoTokenService.java
public void revogar(String jti)          { revogados.put(jti, Boolean.TRUE); }
public boolean estaRevogado(String jti)  { return revogados.getIfPresent(jti) != null; }
```

Guardada em memória (Caffeine), com expiração igual à do refresh — cada entrada some sozinha
quando o token que ela bloqueia já teria expirado. Guardar no banco custaria escrita e leitura
a cada refresh sem ganhar nada, já que a entrada não precisa sobreviver a um restart.

**Limitação assumida:** o logout revoga o **refresh**; o access emitido junto continua valendo
até 15 min. É a mesma janela curta que já limita o estrago de um access vazado — decisão
documentada, não descuido.

---

# Parte 2 — Autorização: você pode?

Três níveis, do mais amplo ao mais específico.

## Nível 1 — regras de rota

"Qual perfil pode chamar qual rota?"

```java
// src/main/java/br/com/fiap/clyvovet/config/SecurityConfig.java
rotas
    // --- Publico ---
    .requestMatchers(api("/auth/login", "/auth/refresh", "/auth/logout", "/auth/registrar")).permitAll()
    .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

    // --- Somente ADMIN ---
    .requestMatchers(api("/auth/usuarios")).hasRole(ADMIN)
    .requestMatchers(HttpMethod.POST, api("/clinicas", "/veterinarios")).hasRole(ADMIN)

    // --- Corpo clinico ---
    .requestMatchers(HttpMethod.POST, api("/eventos-clinicos", "/pagamentos")).hasAnyRole(VETERINARIO, ADMIN)

    // Listar tutores expoe CPF e e-mail de terceiros: nao e para tutor.
    .requestMatchers(HttpMethod.GET, api("/tutores")).hasAnyRole(VETERINARIO, ADMIN)

    // Fecha por padrao: rota nova nasce protegida, nao aberta.
    .anyRequest().authenticated();
```

⚠️ **A ordem importa muito.** O Spring aplica a **primeira** regra que casar. Uma regra
genérica no topo anula todas as específicas abaixo.

💡 **Conceito: *fail-safe default***

A última linha é a mais importante do arquivo:

```java
.anyRequest().authenticated();
```

Ela define o que acontece com **rotas que ninguém pensou em configurar** — inclusive as que
você vai criar no mês que vem.

Terminando em `authenticated()`, uma rota nova nasce **protegida**. Se terminasse em
`permitAll()`, nasceria **aberta** — e ninguém notaria, porque funcionaria perfeitamente
bem em todos os testes.

A regra geral de segurança: **quando esquecer de decidir, o padrão deve ser o seguro.**

## Nível 2 — ownership com `@PreAuthorize`

Regra de rota resolve "qual perfil". Ela **não** resolve *"este tutor pode ver **este** pet?"*
— qualquer tutor autenticado passaria pela regra de rota.

```java
// src/main/java/br/com/fiap/clyvovet/controller/AnimalController.java
@GetMapping("/{id}")
@PreAuthorize("@seguranca.podeAcessarAnimal(#id)")
public ResponseEntity<AnimalResponse> buscarPorId(@PathVariable UUID id) {
```

A string dentro do `@PreAuthorize` é **SpEL** (Spring Expression Language):

| Símbolo | Significa |
|---|---|
| `@seguranca` | o bean registrado com esse nome |
| `#id` | o parâmetro `id` **deste método** |

E o bean:

```java
// src/main/java/br/com/fiap/clyvovet/security/SegurancaService.java
@Service("seguranca")                    // ← o nome usado no SpEL
@RequiredArgsConstructor
public class SegurancaService {

    public boolean podeAcessarAnimal(UUID animalId) {
        return podeAcessar(() -> animalRepository.findById(animalId)
                .map(Animal::getTutor)
                .map(Tutor::getId));
    }

    private boolean podeAcessar(Supplier<Optional<UUID>> tutorDonoDoRecurso) {
        if (temVisaoAmpla()) {
            return true;                 // ADMIN e VETERINARIO passam sem consultar o banco
        }
        UUID meuTutorId = tutorIdDoUsuario();
        return meuTutorId != null && tutorDonoDoRecurso.get().filter(meuTutorId::equals).isPresent();
    }
}
```

Repare no `Supplier` (revisar [00, seção 9](00-java-essencial.md)): o dono chega como
**função**, não como valor pronto. Se o usuário é ADMIN, o método sai na primeira linha e **a
consulta ao banco nunca acontece**.

Se o parâmetro fosse `Optional<UUID>`, quem chama teria que consultar **antes**, mesmo quando
o resultado seria descartado — uma consulta desperdiçada por requisição.

## Nível 3 — o recorte nas listagens

`@PreAuthorize` protege acesso **por id**. Uma **listagem** precisa de outra coisa.

```java
// src/main/java/br/com/fiap/clyvovet/security/SegurancaService.java
public UUID tutorIdParaFiltro() {
    UsuarioAutenticado usuario = autenticado();
    if (usuario == null || usuario.getUsuario().getPerfil() != Perfil.TUTOR) {
        return null;   // VETERINARIO e ADMIN enxergam tudo
    }
    return usuario.getTutorId();
}
```

```java
// src/main/java/br/com/fiap/clyvovet/service/AnimalService.java
return animalRepository.buscarPorFiltros(nome, especie, seguranca.tutorIdParaFiltro(), pageable)
```

E na query, o `tutorId` entra como filtro opcional — `null` significa "sem recorte".

**Por que não filtrar a lista depois?** Porque a paginação já teria sido calculada errado: uma
"página de 10" voltaria com 3 itens, e `totalElements` mentiria. É o mesmo princípio do
[04](04-jpa-e-hibernate.md): **filtro que afeta a contagem tem que estar na query**.

---

## O ataque real que passou despercebido

Este é o melhor exemplo do projeto inteiro. Vale ler devagar.

O ownership era verificado pelo id da **URL**, e só por ele:

```java
@PostMapping
// nenhuma regra além de "estar autenticado"
public ResponseEntity<AnimalResponse> criar(@Valid @RequestBody AnimalRequest request)

@PutMapping("/{id}")
@PreAuthorize("@seguranca.podeAcessarAnimal(#id)")
public ResponseEntity<AnimalResponse> atualizar(@PathVariable UUID id, @RequestBody AnimalRequest request)
```

Parece protegido. **Não está.** Quem define o dono do pet é o campo `tutorId`, que vem no
**corpo** — e nada olhava esse campo.

Um tutor logado, portanto:

```jsonc
// POST /api/v1/animais — cadastrando pet no nome de OUTRO tutor
{ "nome": "Rex", "tutorId": "id-de-outra-pessoa" }
// → o dono legítimo passa a ver na listagem dele um animal que nunca cadastrou
```

```jsonc
// PUT /api/v1/animais/meu-pet — transferindo o próprio pet para outro
{ "nome": "Bolinha", "tutorId": "id-de-outra-pessoa" }
// → perde acesso ao próprio pet, e o registro vai para a conta alheia
```

A correção passou a fazer **as duas perguntas**:

```java
// src/main/java/br/com/fiap/clyvovet/controller/AnimalController.java
@PostMapping
@PreAuthorize("@seguranca.podeAcessarTutor(#request.tutorId)")

@PutMapping("/{id}")
@PreAuthorize("@seguranca.podeAcessarAnimal(#id) and @seguranca.podeAcessarTutor(#request.tutorId)")
```

Ou seja: *"o pet é meu (`#id`)?"* **e** *"o dono que estou gravando continua sendo eu
(`#request.tutorId`)?"*.

E o PATCH precisou de um terceiro método, porque ali `tutorId` ausente significa "não mexa no
dono":

```java
public boolean podeAtribuirTutor(UUID tutorId) {
    return tutorId == null || podeAcessarTutor(tutorId);
}
```

Sem isso, `podeAcessarTutor(null)` devolveria `false` e um tutor ficaria impedido de corrigir
o nome do próprio pet sem reenviar o próprio id no corpo.

> **A lição, e ela generaliza:** proteger o identificador da URL não basta. **Todo campo do
> corpo que decide propriedade ou vínculo precisa da mesma checagem.** Item 17 de
> [`../docs/07-pendencias-e-divergencias.md`](../docs/07-pendencias-e-divergencias.md).

---

## O segundo vazamento: cache sem escopo

Com o filtro por tutor na consulta, a chave do cache passou a importar:

```java
// src/main/java/br/com/fiap/clyvovet/service/AnimalService.java
@Cacheable(value = "animais",
        key = "#nome + '-' + #especie + '-' + @seguranca.tutorIdParaFiltro() + '-' + #pageable")
```

Sem `tutorIdParaFiltro()` na chave: o tutor A lista seus pets, o resultado é cacheado; o tutor
B chama com os mesmos filtros, cai na mesma chave e **recebe os pets do tutor A**.

Autorização perfeita, e vazamento mesmo assim — porque a resposta nem chegou a passar pela
autorização na segunda vez. Coberto por `OwnershipTest.cacheNaoVazaEntreTutores`.

**Cache e permissão se misturam mal.** Se o resultado depende de *quem* pergunta, o "quem"
precisa estar na chave.

---

## Hardening: as defesas de borda

```java
headers
    .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(UM_ANO_EM_SEGUNDOS))
    .contentTypeOptions(Customizer.withDefaults())
    .referrerPolicy(r -> r.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
    .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; frame-ancestors 'none'"));
```

| Header | Protege contra |
|---|---|
| HSTS | downgrade para HTTP sem TLS |
| `X-Content-Type-Options` | navegador "adivinhar" o tipo do conteúdo |
| Referrer-Policy | vazar a URL completa para sites externos |
| CSP `frame-ancestors 'none'` | *clickjacking* — seu site dentro de um iframe alheio |

### CSRF desabilitado — decisão, não esquecimento

```java
.csrf(AbstractHttpConfigurer::disable)
```

**CSRF** (*Cross-Site Request Forgery*): um site malicioso faz o navegador da vítima disparar
uma requisição para a sua API, aproveitando que o navegador **anexa cookies automaticamente**.

O comentário do arquivo é o argumento completo, e é o que se espera numa banca:

> O ataque CSRF depende do navegador anexar credenciais **automaticamente** a uma requisição
> cross-site, o que acontece com cookie de sessão. Esta API é stateless e autentica por token
> no header `Authorization`, que só é enviado se o cliente o colocar explicitamente — um site
> malicioso não consegue fazê-lo.
>
> **GATILHO PARA REATIVAR:** se a Sprint 3 adicionar form login com sessão (frontend
> Thymeleaf), o vetor passa a existir e o CSRF deve ser habilitado.

Esse "gatilho para reativar" é o que transforma um risco em decisão **gerenciada** — e é
diretamente relevante, porque o frontend da Sprint 3 ainda está por fazer.

### CORS com allowlist

```java
config.setAllowedOrigins(Arrays.asList(origensPermitidas));   // nunca "*"
config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
```

**CORS** é a regra do navegador que impede uma página em `site-a.com` de chamar `site-b.com`
sem permissão explícita. O servidor declara quem pode — e `*` combinado com credenciais é
recusado pelos próprios navegadores.

---

## Consolidação

**Entender**
1. Qual a diferença entre autenticação e autorização? Que status HTTP cada falha produz?
2. Por que o conteúdo de um JWT **não** é secreto? O que a assinatura garante, então?

**Aplicar**
3. Você criou `GET /api/v1/relatorios` e não configurou regra nenhuma. Quem consegue acessar?
   Por quê?
4. Em `@PreAuthorize("@seguranca.podeAcessarAnimal(#id)")`, o que é `@seguranca` e o que é
   `#id`?

**Analisar**
5. Por que existem access e refresh token? Que problema cada um resolve sozinho, e qual é
   resolvido só pela combinação?
6. Por que o `AuthService` chama `passwordEncoder.encode()` quando o e-mail **não** existe?
7. Por que `ControleTentativasLogin` precisa de `REQUIRES_NEW` **e** de ser um bean separado?
   São dois motivos distintos — cite os dois.

**Avaliar**
8. Proteger só o `#id` da URL era insuficiente no POST de animais. Explique o ataque e a
   correção. Como você procuraria essa mesma falha em outro endpoint?
9. Um colega quer trocar BCrypt por SHA-256 "porque é mais rápido". O que você responde?
10. O CSRF está desabilitado. Em que cenário exato ele precisaria voltar, e por quê?

---

## Se você levar só uma coisa daqui

**Proteger o id da URL não protege o corpo.** Todo campo que decide propriedade ou vínculo
precisa da mesma checagem de dono — foi assim que um tutor conseguia cadastrar pet no nome
de outro.

---

**Anterior:** [05 — Bean Validation](05-bean-validation.md) ·
**Próximo:** [07 — Tratamento de exceções](07-tratamento-de-excecoes.md)
