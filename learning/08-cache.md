# 08 — Cache

## O que é

Guardar o resultado de uma operação cara para não repeti-la. No Spring, isso é declarativo:
você anota o método e o framework intercepta a chamada.

```java
@Cacheable("animais")
public Page<AnimalResponse> listarTodos(...) { ... }
```

Na primeira chamada o método executa e o retorno é guardado. Nas seguintes, com a mesma
chave, o método **não roda** — o valor sai do cache.

Como funciona por baixo: o Spring embrulha o bean num **proxy** que consulta o cache antes de
delegar. Consequência prática (a mesma de `@Transactional`): **chamada interna não passa pelo
proxy**. Se `metodoA()` chama `this.metodoB()` da mesma classe, o `@Cacheable` de `metodoB`
é ignorado.

## As anotações

| Anotação | O que faz |
|---|---|
| `@Cacheable` | consulta o cache; se não achar, executa e guarda |
| `@CacheEvict` | remove entradas |
| `@CachePut` | executa **sempre** e atualiza o cache |
| `@EnableCaching` | liga o mecanismo |

## Neste projeto

### Leitura

```java
// src/main/java/br/com/fiap/clyvovet/service/AnimalService.java
@Cacheable(value = "animais",
        key = "#nome + '-' + #especie + '-' + @seguranca.tutorIdParaFiltro() + '-' + #pageable")
public Page<AnimalResponse> listarTodos(String nome, String especie, Pageable pageable) {
    return animalRepository.buscarPorFiltros(nome, especie, seguranca.tutorIdParaFiltro(), pageable)
            .map(animalMapper::toResponse);
}
```

### Escrita

```java
@Transactional
@CacheEvict(value = "animais", allEntries = true)
public AnimalResponse criar(AnimalRequest request) { ... }
```

`allEntries = true` limpa o cache inteiro daquele nome. É grosseiro — mas é o correto aqui:
uma inserção pode afetar **qualquer** página de **qualquer** combinação de filtros. Invalidar
só a entrada do id criado não adiantaria nada, porque o que está cacheado são listagens.

### Configuração

```java
// src/main/java/br/com/fiap/clyvovet/config/CacheConfig.java
@Bean
public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "tutores", "animais", "clinicas", "veterinarios", "eventos", "pagamentos");
    cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(1_000));
    return cacheManager;
}
```

O `ConcurrentMapCacheManager` padrão do Spring foi trocado por **Caffeine** por um motivo
concreto, registrado no comentário da classe: o default *"não tem TTL nem limite de tamanho:
uma entrada cacheada permanecia até a próxima escrita da entidade, e o mapa crescia
indefinidamente conforme apareciam novas combinações de filtro e paginação"*.

E o limite virou necessidade, não luxo, quando o `tutorId` entrou na chave: o número de
chaves possíveis passou a crescer junto com a base de usuários.

## A chave é o assunto mais delicado

Por padrão, a chave é a combinação dos parâmetros do método. Quando isso não basta, se
declara em SpEL — e é aí que moram os bugs.

Esta chave tem três decisões dentro:

```java
key = "#nome + '-' + #especie + '-' + @seguranca.tutorIdParaFiltro() + '-' + #pageable"
```

### 1. `@seguranca.tutorIdParaFiltro()` — segurança

Sem isso, a listagem do tutor A seria servida ao tutor B que usasse os mesmos filtros.
**Vazamento de dados entre contas** — não um problema de performance.

Um cache que serve resultado filtrado por permissão **precisa** ter o escopo do usuário na
chave. Coberto por `OwnershipTest.cacheNaoVazaEntreTutores`.

### 2. `#pageable` inteiro — não só `pageNumber` e `pageSize`

Era assim antes:

```java
// ❌ versão antiga — bug real
key = "#nome + '-' + #especie + '-' + #pageable.pageNumber + '-' + #pageable.pageSize"
```

`?sort=nome,asc` e `?sort=nome,desc` colidiam na mesma chave. A segunda chamada recebia o
resultado da primeira, **na ordem errada** — sem erro nenhum, só a resposta errada. Valia
para os 6 recursos.

Usar `#pageable` inteiro traz o `sort` junto. Item 8 de
[`../docs/07-pendencias-e-divergencias.md`](../docs/07-pendencias-e-divergencias.md).

### 3. O separador `'-'`

Detalhe que passa despercebido: concatenar valores sem separador cria colisão. `"ab" + "c"` e
`"a" + "bc"` dão a mesma string — filtros diferentes, mesma chave.

## O que ainda está errado — item 9, em aberto

As respostas carregam campos **desnormalizados** da entidade associada:

| Cache | Campo desnormalizado | Invalidado quando a origem muda? |
|---|---|---|
| `animais` | `tutorNome` | **não** |
| `veterinarios` | `clinicaNome` | **não** |
| `eventos` | `veterinarioNome`, `animalNome`, `clinicaNome` | **não** |

`@CacheEvict` é sempre escopado à própria entidade. Renomear um tutor deixa
`GET /api/v1/animais` devolvendo o nome antigo até que alguma escrita em `Animal` limpe o
cache — ou até o TTL de 10 minutos expirar.

A correção seria listar os caches afetados em quem é referenciado:

```java
// em TutorService
@CacheEvict(value = {"tutores", "animais"}, allEntries = true)
public TutorResponse atualizar(UUID id, TutorRequest request) { ... }
```

Está aberto e documentado. É um bom exemplo para a oral: **desnormalizar tem custo**, e o
custo é invalidação de cache.

## Limitação conhecida: o cache é por processo

Caffeine vive na memória da JVM. Com mais de uma réplica, cada uma tem o próprio cache:

| Sintoma | Por quê |
|---|---|
| Réplica A serve dado velho depois de escrita na B | o `@CacheEvict` da B não alcança a A |
| Rate limit vale por réplica | mesmo problema, no `RateLimitFilter` |
| Logout não revoga em todas | mesmo problema, no `RevogacaoTokenService` |

A saída seria **Redis** (`spring-boot-starter-data-redis`, `bucket4j-redis`). Está listado
como melhoria sugerida em [`../docs/07`](../docs/07-pendencias-e-divergencias.md) — não é
defeito enquanto rodar uma instância só, mas é preciso saber que a decisão está tomada com
essa premissa.

## Nos testes

```java
// src/test/java/br/com/fiap/clyvovet/support/TesteDeApi.java
/**
 * As listagens sao cacheadas por 10 minutos. Sem limpar entre os testes, a
 * pagina montada por um deles seria servida a outro que caisse na mesma
 * chave, e o segundo passaria (ou falharia) por um motivo que nao e o dele.
 */
@AfterEach
void limparCaches() {
    for (String nome : cacheManager.getCacheNames()) {
        Cache cache = cacheManager.getCache(nome);
        if (cache != null) {
            cache.clear();
        }
    }
}
```

Teste que compartilha cache com outro teste é teste que **passa por engano** — e um dia falha
sem motivo aparente, dependendo da ordem de execução.

## Quando cachear (e quando não)

| Cachear | Não cachear |
|---|---|
| leitura frequente, escrita rara | dado que muda a cada requisição |
| consulta cara (JOIN, agregação) | consulta trivial por PK |
| resultado igual para muitos | resultado único por usuário **sem** escopo na chave |

O último ponto é o resumo do que se aprende aqui: cache e permissão se misturam mal. Se o
resultado depende de **quem** pergunta, o "quem" tem que estar na chave — ou não se cacheia.

## Perguntas de avaliação oral

1. Por que trocar o `ConcurrentMapCacheManager` padrão por Caffeine?
2. O que `@seguranca.tutorIdParaFiltro()` faz na chave do cache? O que acontecia sem ele?
3. Por que a chave usa `#pageable` inteiro e não `pageNumber` + `pageSize`?
4. Por que `@CacheEvict(allEntries = true)` e não invalidar só a entrada alterada?
5. Se um tutor é renomeado, por quanto tempo `GET /animais` pode mostrar o nome antigo? Por quê?
6. O que muda no cache se a aplicação subir com duas réplicas?
7. Por que os testes limpam o cache no `@AfterEach`?

---

**Anterior:** [07 — Tratamento de exceções](07-tratamento-de-excecoes.md) ·
**Próximo:** [09 — Flyway e migrations](09-flyway-e-migrations.md)
