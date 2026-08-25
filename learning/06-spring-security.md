# 06 — Spring Security

> Vale **30 pontos** da Sprint 3 e é o assunto que mais aparece na avaliação oral. A
> documentação do que está implementado está em
> [`../docs/08-seguranca.md`](../docs/08-seguranca.md); aqui o foco é **entender** cada peça.

## O que é

Spring Security é um **conjunto de filtros** que se coloca antes dos seus controllers. Toda
requisição passa por ele antes de chegar ao `@RestController` — e, se for barrada, o
controller nem é chamado.

Ele resolve duas perguntas diferentes, e confundi-las é o erro mais comum:

| Pergunta | Nome | Falha vira |
|---|---|---|
| **Quem é você?** | Autenticação (*authentication*) | **401** Unauthorized |
| **Você pode fazer isso?** | Autorização (*authorization*) | **403** Forbidden |

401 = *"não sei quem você é"*. 403 = *"sei quem você é, e você não pode"*.

## A cadeia de filtros

```
requisição
   │
   ▼
┌──────────────────┐  volume por IP; barra ANTES de custar um BCrypt
│ RateLimitFilter  │
└────────┬─────────┘
         ▼
┌──────────────────────────┐  lê o header, valida o JWT,
│ JwtAuthenticationFilter  │  popula o SecurityContext
└────────┬─────────────────┘
         ▼
┌──────────────────────────┐  regras de rota: perfil × verbo × caminho
│ AuthorizationFilter      │
└────────┬─────────────────┘
         ▼
   @PreAuthorize no controller  ← ownership: "este pet é seu?"
         ▼
     Controller
```

```java
// src/main/java/br/com/fiap/clyvovet/config/SecurityConfig.java
.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
.addFilterAfter(jwtAuthenticationFilter, RateLimitFilter.class);
```

A ordem é uma decisão: o rate limit vem **antes** da autenticação porque uma rajada de
tentativas de login precisa ser barrada antes de custar um hash BCrypt por requisição — que
é caro de propósito.

## Parte 1 — Autenticação

### Senhas: BCrypt

```java
// src/main/java/br/com/fiap/clyvovet/config/SecurityConfig.java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

Senha **nunca** é guardada em texto nem em MD5/SHA simples. BCrypt tem duas propriedades que
importam:

1. **Salt embutido** — a mesma senha gera hashes diferentes, então uma *rainbow table* não
   serve.
2. **Custo ajustável** — é lento de propósito. Lento o bastante para tornar força bruta cara,
   rápido o bastante para um login.

Por isso o hash não é comparado com `equals`, e sim com `matches`:

```java
if (!passwordEncoder.matches(request.getSenha(), usuario.getSenha())) { ... }
```

### JWT: o que é e por que aqui

Um **JWT** é um token assinado com três partes: `header.payload.signature`. O *payload*
carrega dados (as *claims*), e a assinatura garante que ninguém alterou.

Ponto central: o servidor **não guarda sessão**. O token se sustenta sozinho — o servidor só
confere a assinatura. É o que permite escalar horizontalmente e o que serve a um app mobile.

> ⚠️ O payload é **codificado em Base64, não criptografado**. Qualquer um lê o conteúdo em
> jwt.io. Nunca coloque senha ou dado sensível numa claim.

```java
// src/main/java/br/com/fiap/clyvovet/security/JwtService.java
private String gerar(Usuario usuario, String tipo, Duration validade) {
    Instant agora = Instant.now();
    return Jwts.builder()
            .id(UUID.randomUUID().toString())          // jti — identifica o token
            .subject(usuario.getId().toString())       // sub — quem é
            .claim(CLAIM_PERFIL, usuario.getPerfil().name())
            .claim(CLAIM_TIPO, tipo)                   // access ou refresh
            .issuedAt(Date.from(agora))
            .expiration(Date.from(agora.plus(validade)))
            .signWith(chave)
            .compact();
}
```

### Dois tipos de token — e por que

| Token | Validade | Serve para |
|---|---|---|
| `access` | **15 min** | autorizar as chamadas da API |
| `refresh` | **7 dias** | obter um novo access, e nada mais |

O raciocínio: um access token vazado vale no máximo 15 minutos. Sem a separação, ou o token
duraria 7 dias (janela enorme para um vazamento) ou o usuário refaria login a cada 15 minutos.

A claim `tipo` é o que impede o abuso óbvio — usar o refresh como se fosse access:

```java
// src/main/java/br/com/fiap/clyvovet/security/JwtAuthenticationFilter.java
// Refresh token nao autentica chamadas da API — so serve em /auth/refresh.
if (!jwtService.ehAccessToken(claims)) {
    return;
}
```

Sem essa checagem, um refresh vazado valeria como credencial por 7 dias.

### A chave não pode ser fraca

```java
this.chave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(segredo));
```

`Keys.hmacShaKeyFor` **recusa** segredos com menos de 256 bits. A aplicação não sobe com
chave fraca — falha no boot, que é o momento certo de descobrir. O segredo vem de
`JWT_SECRET`, variável de ambiente, sem valor padrão.

### O filtro que autentica

```java
// src/main/java/br/com/fiap/clyvovet/security/JwtAuthenticationFilter.java
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                FilterChain filterChain) throws ServletException, IOException {
    String token = extrairToken(request);
    if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        autenticar(token, request);
    }
    filterChain.doFilter(request, response);
}

private void autenticar(String token, HttpServletRequest request) {
    try {
        Claims claims = jwtService.lerClaims(token);
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

Três coisas para notar:

1. **`OncePerRequestFilter`** garante execução única por requisição, mesmo com `forward`
   interno.
2. **O filtro não devolve erro.** Token ausente ou inválido apenas deixa o contexto vazio, e
   a cadeia do Spring Security responde 401 ou 403 conforme a rota. Uma fonte de decisão só.
3. **`SecurityContextHolder`** é um `ThreadLocal`: quem está autenticado *nesta* thread.

### `UserDetails` — a ponte com o seu domínio

O Spring Security não conhece a sua entidade `Usuario`. Ele fala `UserDetails`:

```java
// src/main/java/br/com/fiap/clyvovet/security/UsuarioAutenticado.java
public class UsuarioAutenticado implements UserDetails {

    private final transient Usuario usuario;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // O prefixo ROLE_ e o que faz hasRole("ADMIN") funcionar nas regras.
        return List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name()));
    }

    @Override public String getPassword() { return usuario.getSenha(); }
    @Override public String getUsername() { return usuario.getEmail(); }
    @Override public boolean isAccountNonLocked() { return !usuario.estaBloqueado(); }
    @Override public boolean isEnabled() { return usuario.isAtivo(); }

    /** Id do tutor vinculado, ou null se o usuario nao for um tutor. */
    public UUID getTutorId() {
        return usuario.getTutor() != null ? usuario.getTutor().getId() : null;
    }
}
```

**A pegadinha do `ROLE_`:** `hasRole("ADMIN")` procura a authority `ROLE_ADMIN` — o prefixo é
adicionado por baixo dos panos. Já `hasAuthority("ADMIN")` procura exatamente `ADMIN`.
Misturar os dois é fonte clássica de "por que meu 403 não passa".

### Login

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

Duas defesas contra **enumeração de usuários** — descobrir quais e-mails existem só
observando as respostas:

1. **Mensagem única.** `CREDENCIAIS_INVALIDAS` vale para senha errada, e-mail inexistente e
   conta bloqueada. Mensagens distintas entregariam a informação.
2. **Tempo uniforme.** Se o e-mail não existe, o código **mesmo assim** gasta um BCrypt. Sem
   isso, a resposta voltaria em 2 ms para e-mail inexistente e em 100 ms para existente — e
   o atacante leria a diferença.

Repare também no que o método **não** tem:

```java
/**
 * Sem @Transactional de proposito: o login so le, e o registro da tentativa
 * e commitado a parte pelo ControleTentativasLogin. Abrir uma transacao aqui
 * faria o rollback do BadCredentialsException apagar a contagem de falhas.
 */
```

### Bloqueio de conta

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

`REQUIRES_NEW` abre uma transação separada, que commita **independentemente** do destino da
transação que a chamou. É o que faz a contagem sobreviver à exceção do login.

Duas defesas complementares, que respondem a ataques diferentes:

| Mecanismo | Contra | Escopo |
|---|---|---|
| `ControleTentativasLogin` | força bruta **direcionada** a uma conta | por **conta** |
| `RateLimitFilter` (Bucket4j) | abuso **volumétrico** | por **IP** |

O comentário do `RateLimitFilter` explica por que não se trocou um pelo outro: um limite
curto por IP no login *"quebra uso legítimo — uma clínica inteira atrás de um mesmo IP
público compartilha o balde e trava depois de poucos logins"*.

### Logout com token stateless

Como o servidor não guarda sessão, "deslogar" exige uma deny-list:

```java
// src/main/java/br/com/fiap/clyvovet/security/RevogacaoTokenService.java
public void revogar(String jti) { revogados.put(jti, Boolean.TRUE); }
public boolean estaRevogado(String jti) { return revogados.getIfPresent(jti) != null; }
```

Guardada em memória (Caffeine) com expiração igual à do refresh: cada entrada some sozinha
quando o token que ela bloqueia já teria expirado.

O logout revoga o **refresh**; o access emitido junto continua valendo até 15 min. É a mesma
janela curta que já limita o estrago de um access vazado — e é uma limitação conhecida e
documentada, não um descuido.

## Parte 2 — Autorização

### Nível 1: regras de rota

```java
// src/main/java/br/com/fiap/clyvovet/config/SecurityConfig.java
rotas
    // --- Publico ---
    .requestMatchers(api("/auth/login", "/auth/refresh", "/auth/logout", "/auth/registrar")).permitAll()
    .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

    // --- Somente ADMIN ---
    .requestMatchers(api("/auth/usuarios")).hasRole(ADMIN)
    .requestMatchers(HttpMethod.POST,   api("/clinicas", "/veterinarios")).hasRole(ADMIN)

    // --- Corpo clinico ---
    .requestMatchers(HttpMethod.POST,   api("/eventos-clinicos", "/pagamentos")).hasAnyRole(VETERINARIO, ADMIN)

    // Listar tutores expoe CPF e e-mail de terceiros: nao e para tutor.
    .requestMatchers(HttpMethod.GET,    api("/tutores")).hasAnyRole(VETERINARIO, ADMIN)

    // Fecha por padrao: rota nova nasce protegida, nao aberta.
    .anyRequest().authenticated();
```

**A ordem importa:** o Spring aplica a **primeira** regra que casa. Uma regra genérica no
topo anula as específicas abaixo dela.

E `.anyRequest().authenticated()` no fim é o *fail-safe* mais importante do arquivo: uma rota
nova nasce protegida. Se a lista terminasse em `permitAll()`, todo endpoint novo nasceria
aberto — e ninguém notaria.

### Nível 2: ownership com `@PreAuthorize`

Regra de rota resolve *"qual perfil acessa qual rota"*. Ela **não** resolve *"este tutor pode
ver este pet"* — qualquer tutor autenticado passaria.

```java
// src/main/java/br/com/fiap/clyvovet/controller/AnimalController.java
@GetMapping("/{id}")
@PreAuthorize("@seguranca.podeAcessarAnimal(#id)")
public ResponseEntity<AnimalResponse> buscarPorId(@PathVariable UUID id) {
```

A sintaxe `@seguranca.metodo(#parametro)` é **SpEL**: `@seguranca` referencia o bean pelo
nome, `#id` referencia o parâmetro do método.

```java
// src/main/java/br/com/fiap/clyvovet/security/SegurancaService.java
@Service("seguranca")
@RequiredArgsConstructor
public class SegurancaService {

    public boolean podeAcessarAnimal(UUID animalId) {
        return podeAcessar(() -> animalRepository.findById(animalId)
                .map(Animal::getTutor)
                .map(Tutor::getId));
    }

    private boolean podeAcessar(Supplier<Optional<UUID>> tutorDonoDoRecurso) {
        if (temVisaoAmpla()) {
            return true;
        }
        UUID meuTutorId = tutorIdDoUsuario();
        return meuTutorId != null && tutorDonoDoRecurso.get().filter(meuTutorId::equals).isPresent();
    }
}
```

O `Supplier` não é enfeite: o dono chega como **função**, não como valor pronto, para que a
consulta ao banco **não aconteça** quando o perfil já tem visão ampla (VETERINARIO e ADMIN).

### Nível 3: o recorte nas listagens

`@PreAuthorize` protege acesso por id. Uma **listagem** precisa de outra coisa — não adianta
verificar depois, porque a paginação já teria sido calculada errado.

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

E, na query, o `tutorId` entra como mais um filtro opcional — `null` significa "sem recorte".

## O ataque real que passou despercebido

Este é o melhor exemplo do projeto inteiro para a avaliação oral. O ownership era verificado
pelo id da **URL**, e só por ele:

```java
@PreAuthorize("@seguranca.podeAcessarAnimal(#id)")
```

Só que quem define o dono do pet é o campo `tutorId`, que vem no **corpo**. Nada olhava esse
campo. Consequência: um tutor logado **cadastrava pet no nome de outro tutor** e
**transferia o próprio pet** para outro tutor num PUT.

A correção passou a fazer as duas perguntas:

```java
// src/main/java/br/com/fiap/clyvovet/controller/AnimalController.java
@PostMapping
@PreAuthorize("@seguranca.podeAcessarTutor(#request.tutorId)")

@PutMapping("/{id}")
@PreAuthorize("@seguranca.podeAcessarAnimal(#id) and @seguranca.podeAcessarTutor(#request.tutorId)")
```

E o PATCH precisou de um terceiro método, porque ali `tutorId` ausente significa "não mexa no
dono":

```java
public boolean podeAtribuirTutor(UUID tutorId) {
    return tutorId == null || podeAcessarTutor(tutorId);
}
```

Sem isso, `podeAcessarTutor(null)` devolveria `false` e um tutor ficaria impedido de corrigir
o nome do próprio pet.

**A lição:** proteger o identificador da URL não basta. Todo campo do corpo que decide
propriedade ou vínculo precisa da mesma checagem. História completa no item 17 de
[`../docs/07-pendencias-e-divergencias.md`](../docs/07-pendencias-e-divergencias.md).

## O segundo vazamento: cache sem escopo

Com o filtro por tutor na consulta, a chave do cache passou a importar:

```java
// src/main/java/br/com/fiap/clyvovet/service/AnimalService.java
@Cacheable(value = "animais",
        key = "#nome + '-' + #especie + '-' + @seguranca.tutorIdParaFiltro() + '-' + #pageable")
```

Sem `tutorIdParaFiltro()` na chave, a primeira listagem de um tutor seria servida a **qualquer
outro** que usasse os mesmos filtros. Coberto por `OwnershipTest.cacheNaoVazaEntreTutores`.

## Hardening

```java
// src/main/java/br/com/fiap/clyvovet/config/SecurityConfig.java
headers
    .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(UM_ANO_EM_SEGUNDOS))
    .contentTypeOptions(Customizer.withDefaults())
    .referrerPolicy(r -> r.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
    .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; frame-ancestors 'none'"));
```

### CSRF desabilitado — decisão, não esquecimento

```java
.csrf(AbstractHttpConfigurer::disable)
```

O comentário do arquivo é o argumento completo, e é o que se espera numa banca:

> O ataque CSRF depende do navegador anexar credenciais **automaticamente** a uma requisição
> cross-site, o que acontece com cookie de sessão. Esta API é stateless e autentica por token
> no header `Authorization`, que só é enviado se o cliente o colocar explicitamente — um site
> malicioso não consegue fazê-lo.
>
> **GATILHO PARA REATIVAR:** se a Sprint 3 adicionar form login com sessão (frontend
> Thymeleaf), o vetor passa a existir e o CSRF deve ser habilitado.

Esse "gatilho para reativar" é a parte que transforma uma decisão de risco em decisão
gerenciada. E é diretamente relevante: o frontend da Sprint 3 ainda está por fazer.

### CORS com allowlist

```java
config.setAllowedOrigins(Arrays.asList(origensPermitidas));   // nunca "*"
config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
```

`*` combinado com credenciais é recusado pelos navegadores — e seria errado de qualquer jeito.

## Perguntas de avaliação oral

1. Qual a diferença entre 401 e 403? Dê um exemplo de cada nesta API.
2. Por que existem dois tokens (access e refresh)? O que a claim `tipo` impede?
3. Por que `hasRole("ADMIN")` funciona se a authority guardada é `ROLE_ADMIN`?
4. Por que o `AuthService` chama `passwordEncoder.encode()` quando o e-mail **não** existe?
5. Por que `ControleTentativasLogin` usa `Propagation.REQUIRES_NEW`? O que quebra sem isso?
6. `@PreAuthorize("@seguranca.podeAcessarAnimal(#id)")` — o que é `@seguranca` e o que é `#id`?
7. Por que proteger só o `#id` da URL era insuficiente no POST de animais?
8. Por que o `tutorId` do usuário logado entra na **chave do cache**?
9. Por que o CSRF está desabilitado? Em que cenário ele precisaria voltar?
10. Por que o `RateLimitFilter` vem **antes** do filtro JWT?
11. Como se faz logout numa API stateless? Qual a limitação da solução usada aqui?

---

**Anterior:** [05 — Bean Validation](05-bean-validation.md) ·
**Próximo:** [07 — Tratamento de exceções](07-tratamento-de-excecoes.md)
