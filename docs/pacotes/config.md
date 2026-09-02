# `config` — as convenções da aplicação

`src/main/java/br/com/fiap/clyvovet/config` · 5 classes

Cinco classes `@Configuration`, cada uma respondendo por uma decisão que vale
para o sistema inteiro. Nenhuma tem regra de negócio: são o contorno dentro do
qual o resto do código roda.

---

## Os arquivos

| Arquivo | O que decide |
|---|---|
| `SecurityConfig.java` | A cadeia de filtros, as rotas públicas e as regras de acesso por perfil |
| `WebConfig.java` | O prefixo `/api/v1` e o formato das respostas paginadas |
| `CacheConfig.java` | Troca o cache padrão do Spring por Caffeine, com TTL e limite de tamanho |
| `OpenApiConfig.java` | Declara o esquema *bearer* para o Swagger |
| `DevDataSeeder.java` | Cria os usuários de desenvolvimento |

---

## `WebConfig` — prefixo e paginação

### O prefixo

Aplicado aqui, e não em cada `@RequestMapping`, para que os controllers
continuem declarando só o próprio recurso (`/tutores`) e a versão viva num lugar
único. Trocar para `/api/v2` um dia é mudar uma constante.

A alternativa seria `server.servlet.context-path`, mas ela **move o Swagger e o
console do H2 junto**.

O predicado é **por pacote**, e não por `@RestController`: o springdoc também
anota suas classes com `@RestController`, então filtrar pela anotação levava
`/v3/api-docs` para `/api/v1/v3/api-docs` e o Swagger parava de abrir.
Restrito ao pacote de controllers da aplicação, `/swagger-ui.html`,
`/v3/api-docs` e `/h2-console` seguem na raiz, onde as ferramentas esperam
encontrá-los.

### A paginação

Sem `VIA_DTO` o Spring serializa o `PageImpl` inteiro e avisa no boot que *"there
is no guarantee about the stability of the resulting JSON structure"* — a
resposta carregava mais de vinte campos internos do framework e podia mudar de
forma num upgrade, sem nada no código mudar.

Com `VIA_DTO` o contrato passa a ser:

```json
{
  "content": [ ... ],
  "page": { "size": 10, "number": 0, "totalElements": 42, "totalPages": 5 }
}
```

---

## `SecurityConfig` — a cadeia

```java
.csrf(AbstractHttpConfigurer::disable)      // API stateless, sem cookie de sessão
.cors(Customizer.withDefaults())
.sessionManagement(... STATELESS)
.headers(this::configurarHeaders)
.exceptionHandling(... respostaErroSeguranca)   // 401 e 403 em JSON
.authorizeHttpRequests(this::configurarRotas)
.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
.addFilterAfter(jwtAuthenticationFilter, RateLimitFilter.class);
```

**O rate limit vem antes da autenticação**, e a ordem é o ponto: uma rajada de
tentativas de login precisa ser barrada antes de custar um BCrypt por
requisição — que é justamente o que torna o endpoint caro de atacar e caro de
defender.

O CSRF fica desabilitado porque a autenticação é pelo header `Authorization`, que
só é enviado se o cliente o colocar explicitamente; um site malicioso não
consegue fazê-lo. O comentário no arquivo registra o **gatilho para reativar**:
se um dia entrar form login com sessão, o vetor passa a existir.

Duas notas:

**As origens de CORS nunca são `*` combinado com credenciais.** A combinação é
recusada pelo próprio navegador, e onde ela "funciona" é porque as credenciais
não estão indo.

**O console do H2 só é liberado onde ele existe** — perfis `dev` e `h2`. Em
produção a rota nem chega a ser declarada.

E os matchers enxergam o caminho **já prefixado** com `/api/v1`, porque é o
`WebConfig` que prefixa os controllers. A constante é compartilhada entre os
dois de propósito: se divergissem, uma rota ficaria aberta em silêncio.

---

## `CacheConfig` — por que não o default

O `ConcurrentMapCacheManager` padrão do Spring Boot **não tem TTL nem limite de
tamanho**: uma entrada cacheada permanecia até a próxima escrita da entidade, e
o mapa crescia indefinidamente conforme apareciam novas combinações de filtro e
paginação.

Com o filtro por tutor introduzido na autorização, o número de chaves possíveis
passou a crescer junto com a base de usuários — o que torna o limite de tamanho
**necessário**, não apenas desejável.

Hoje: Caffeine, TTL de 10 minutos, tamanho máximo declarado.

---

## `DevDataSeeder` — e por que ele não é uma migration

Os hashes de senha são gerados **em tempo de execução**. Hash de credencial não
deve ser versionado, e uma migration com senha fixa acabaria aplicada também no
banco de entrega.

Ativo nos perfis `dev`, `h2` e `oracle`. Idempotente — nada é recriado se já
existe.

O perfil `oracle` entrou na lista quando o Oracle da FIAP virou o banco de teste
do projeto: sem estes usuários a suíte não consegue fazer login, e quase todo
teste falharia com 401 por um motivo que não é o dele.

**Produção (perfil `mysql`) continua fora da lista**, e esse é o ponto: o banco
de entrega não deve receber usuário de desenvolvimento.

---

## `OpenApiConfig`

Declara o esquema *bearer*, o que habilita o botão **Authorize** do Swagger. Sem
isso a UI não envia o header `Authorization` e todas as chamadas voltam 401 — o
que costuma ser lido como "a API está quebrada".

---

## Onde continuar

| Assunto | Documento |
|---|---|
| Os filtros e o `SegurancaService` | [security.md](security.md) |
| Perfis Spring, propriedades e como rodar | [../04-configuracao.md](../04-configuracao.md) |
| Docker, compose e Azure | [../05-deploy.md](../05-deploy.md) |
