# 08 — Cache

> **Pré-requisito:** [01 — O que é Spring](01-spring-boot-e-injecao-de-dependencia.md),
> principalmente a **armadilha do proxy**.

---

## O que é, e por que existe

Ir ao banco custa tempo: abrir conexão, mandar SQL, esperar, ler o resultado. Se a mesma
consulta é feita mil vezes por minuto e o resultado é sempre igual, mil idas ao banco são
desperdício.

**Cache** é guardar o resultado na memória e reusar.

```
1ª chamada:  aplicação ──▶ banco ──▶ resultado ──▶ guarda no cache ──▶ cliente
2ª chamada:  aplicação ──▶ cache ──────────────────────────────────▶ cliente
                                (o banco nem é consultado)
```

No Spring isso é **declarativo** — você anota o método:

```java
@Cacheable("animais")
public Page<AnimalResponse> listarTodos(...) { ... }
```

Na primeira chamada o método executa e o retorno é guardado. Nas seguintes, **com a mesma
chave**, o método **não roda**.

---

## Como funciona por baixo (e a armadilha)

O Spring embrulha o bean num **proxy** que consulta o cache antes de delegar:

```
quem chama ──▶ [ PROXY ] ──▶ seu método
                  │
          "já tenho essa chave?"
           sim → devolve, não chama
           não → chama, guarda, devolve
```

⚠️ **Consequência (a mesma de `@Transactional`): chamada interna não passa pelo proxy.**

```java
// ❌ o @Cacheable de metodoB é IGNORADO
public void metodoA() {
    this.metodoB();          // chamada direta
}

@Cacheable("x")
public Page<...> metodoB() { ... }
```

Sem erro, sem aviso — só não cacheia. Se algo "não está cacheando", esta é a primeira
hipótese.

---

## As anotações

| Anotação | O que faz |
|---|---|
| `@Cacheable` | consulta o cache; se não achar, executa e guarda |
| `@CacheEvict` | **remove** entradas |
| `@CachePut` | executa **sempre** e atualiza o cache |
| `@EnableCaching` | liga o mecanismo |

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

💡 **Conceito: por que invalidar TUDO (`allEntries = true`)**

Parece exagero apagar o cache inteiro só porque um animal foi criado. Mas pense no que está
guardado: **listagens paginadas e filtradas**, não animais individuais.

Um animal novo pode entrar em: `?nome=bo` página 0, `?especie=CAO` página 3, a listagem sem
filtro ordenada por nome, e assim por diante. **Qualquer combinação pode ter mudado**, e não
há como saber quais sem refazer todas.

A alternativa seria invalidar "a entrada daquele id" — que não existe, porque ninguém cacheou
animais por id.

Regra prática: **cacheou uma lista, invalide a lista inteira.** Invalidação seletiva só vale
quando o cache é por chave individual.

---

## Configuração

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

Duas configurações, cada uma resolvendo um problema:

| Configuração | Sem ela |
|---|---|
| `expireAfterWrite(10 min)` | entrada fica cacheada até a próxima escrita — dado velho por tempo indeterminado |
| `maximumSize(1_000)` | o mapa cresce sem limite; a cada nova combinação de filtro, mais uma entrada — **vazamento de memória** |

O `ConcurrentMapCacheManager` padrão do Spring **não tem nenhuma das duas**. Por isso a troca
por Caffeine.

E o limite virou necessidade quando o `tutorId` entrou na chave: o número de chaves possíveis
passou a crescer **junto com a base de usuários**.

⚠️ **Nome de cache precisa estar nessa lista.** Um `@Cacheable("relatorios")` sem declarar
`"relatorios"` ali não funciona.

---

## A chave é onde moram os bugs

Por padrão, a chave é a combinação dos parâmetros. Quando isso não basta, se declara em SpEL.
Esta chave tem **três decisões**:

```java
key = "#nome + '-' + #especie + '-' + @seguranca.tutorIdParaFiltro() + '-' + #pageable"
```

### 1. `@seguranca.tutorIdParaFiltro()` — isto é segurança, não performance

Sem isso:

```
tutor A: GET /animais           → consulta o banco, cacheia sob a chave "null-null-null-..."
tutor B: GET /animais           → mesma chave → recebe OS PETS DO TUTOR A
```

A autorização estava perfeita. O vazamento acontece **porque a segunda requisição nem chegou
a consultar** — o cache respondeu antes.

Coberto por `OwnershipTest.cacheNaoVazaEntreTutores`.

### 2. `#pageable` inteiro — um bug real

Era assim antes:

```java
// ❌ versão antiga
key = "#nome + '-' + #especie + '-' + #pageable.pageNumber + '-' + #pageable.pageSize"
```

Percebeu o que falta? O **sort**.

```
GET /animais?sort=nome,asc   → cacheia sob "null-null-0-10"
GET /animais?sort=nome,desc  → MESMA chave → devolve a lista na ordem errada
```

Sem erro, sem exceção — só a resposta errada. E valia para os 6 recursos. Item 8 de
[`../docs/07-pendencias-e-divergencias.md`](../docs/07-pendencias-e-divergencias.md).

Usar `#pageable` inteiro traz `page`, `size` **e** `sort` juntos.

### 3. O separador `'-'`

Detalhe que passa despercebido: concatenar sem separador cria colisão.

```
"ab" + "c"  = "abc"
"a"  + "bc" = "abc"     ← filtros diferentes, mesma chave
```

💡 **Conceito: a regra da chave de cache**

A chave precisa conter **tudo o que muda o resultado**. Tudo mesmo:

- os filtros (`nome`, `especie`);
- a paginação **e a ordenação**;
- **quem está perguntando**, se o resultado depende disso.

Faltou alguma coisa? Duas situações diferentes colidem na mesma chave, e a segunda recebe a
resposta da primeira. O sintoma nunca é um erro — é um dado errado, que ninguém percebe até
alguém reclamar.

---

## O que ainda está errado — item 9, em aberto

As respostas carregam campos **desnormalizados** da entidade associada (ver
[02](02-arquitetura-em-camadas.md)):

| Cache | Campo desnormalizado | Invalidado quando a origem muda? |
|---|---|---|
| `animais` | `tutorNome` | **não** |
| `veterinarios` | `clinicaNome` | **não** |
| `eventos` | `veterinarioNome`, `animalNome`, `clinicaNome` | **não** |

`@CacheEvict` é escopado à **própria** entidade. Renomear um tutor não limpa o cache de
`animais` — que continua servindo o nome antigo até alguma escrita em `Animal` acontecer, ou
até o TTL de 10 minutos expirar.

A correção seria declarar os caches afetados em quem é **referenciado**:

```java
// em TutorService
@CacheEvict(value = {"tutores", "animais"}, allEntries = true)
public TutorResponse atualizar(UUID id, TutorRequest request) { ... }
```

É um bom exemplo para a oral: **desnormalizar acelera a leitura e complica a invalidação**.
Nada é de graça.

---

## Limitação assumida: o cache é por processo

Caffeine vive na memória da JVM. Com mais de uma réplica da aplicação:

| Sintoma | Causa |
|---|---|
| Réplica A serve dado velho depois de escrita na B | o `@CacheEvict` da B não alcança a A |
| Rate limit vale por réplica, não no total | mesmo problema, no `RateLimitFilter` |
| Logout numa réplica não revoga nas outras | mesmo problema, no `RevogacaoTokenService` |

A saída seria **Redis** — um cache externo compartilhado. Está registrado como melhoria
sugerida em [`../docs/07`](../docs/07-pendencias-e-divergencias.md).

Não é defeito enquanto roda uma instância só. É uma **premissa**, e o valor de documentá-la é
que a decisão pode ser revista quando a premissa mudar.

---

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

Teste que compartilha cache com outro **passa por engano** — e um dia falha sem motivo
aparente, só porque a ordem de execução mudou. Estado compartilhado entre testes é uma das
piores fontes de teste instável.

---

## Quando cachear (e quando não)

| ✅ Cachear | ❌ Não cachear |
|---|---|
| leitura frequente, escrita rara | dado que muda a cada requisição |
| consulta cara (JOIN, agregação) | consulta trivial por chave primária |
| resultado igual para muitos | resultado por usuário **sem** o usuário na chave |

E a pergunta que resume tudo: **por quanto tempo um dado velho é aceitável aqui?** Se a
resposta for "nenhum", não cacheie. Cache é sempre uma troca entre velocidade e frescor.

---

## Consolidação

**Entender**
1. O que `@Cacheable` faz na primeira chamada e o que faz na segunda?
2. Por que o `ConcurrentMapCacheManager` padrão foi trocado por Caffeine?

**Aplicar**
3. Você criou um método cacheado que recebe `de`, `ate` e `veterinarioId`. Escreva a chave.
4. Onde você precisa declarar o nome de um cache novo?

**Analisar**
5. O que `@seguranca.tutorIdParaFiltro()` faz na chave? Descreva o vazamento que ocorre sem
   ele.
6. Por que `#pageable` inteiro e não `pageNumber` + `pageSize`? Qual era o sintoma do bug?
7. Se um tutor é renomeado, por quanto tempo `GET /animais` pode mostrar o nome antigo? Por
   quê?

**Avaliar**
8. Um colega quer cachear `GET /animais/{id}` por 1 hora. Que perguntas você faria antes de
   concordar?
9. A aplicação vai subir com 3 réplicas. O que quebra? Qual seria sua ordem de prioridade
   para corrigir?

---

## Se você levar só uma coisa daqui

**A chave precisa conter tudo o que muda o resultado — inclusive quem está perguntando.**
Faltou algo na chave, duas situações diferentes colidem, e o sintoma nunca é um erro: é um
dado errado.

---

**Anterior:** [07 — Tratamento de exceções](07-tratamento-de-excecoes.md) ·
**Próximo:** [09 — Flyway e migrations](09-flyway-e-migrations.md)
