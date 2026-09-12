# Arquitetura

## Visão macro

```
┌──────────────┐    HTTP/JSON    ┌────────────────────────────────────┐
│  Front-end   │ ─────────────►  │      Spring Boot API :8080         │
│   / Mobile   │ ◄─────────────  │                                    │
└──────────────┘                 │  ┌──────────────────────────────┐  │
                                 │  │ Controller   @RestController │  │
                                 │  │  • rotas, status HTTP        │  │
                                 │  │  • @Valid, @PageableDefault  │  │
                                 │  └───────────┬──────────────────┘  │
                                 │              ▼                     │
                                 │  ┌──────────────────────────────┐  │
                                 │  │ Service      @Service        │  │
                                 │  │  • regra de negócio          │  │
                                 │  │  • resolve FKs               │  │
                                 │  │  • @Cacheable / @CacheEvict  │  │
                                 │  └───────────┬──────────────────┘  │
                                 │              ▼                     │
                                 │  ┌──────────────┐  ┌────────────┐  │
                                 │  │ Repository   │  │  Mapper    │  │
                                 │  │ JpaRepository│  │ @Component │  │
                                 │  │  + JPQL      │  │ Entity↔DTO │  │
                                 │  └───────┬──────┘  └────────────┘  │
                                 │          ▼                         │
                                 │  ┌──────────────────────────────┐  │
                                 │  │ Model        @Entity         │  │
                                 │  └───────────┬──────────────────┘  │
                                 └──────────────┼─────────────────────┘
                                                ▼ JDBC
                                 ┌────────────────────────────────────┐
                                 │  Oracle 19c (FIAP)  ou  H2 (dev)   │
                                 └────────────────────────────────────┘
```

Arquitetura em camadas clássica, sem desvios. Não há camada de segurança,
autenticação, mensageria ou integração externa.

---

## Estrutura de pacotes

Raiz: `br.com.fiap.clyvovet`

| Pacote | Papel | Arquivos |
|---|---|---|
| *(raiz)* | Bootstrap da aplicação | `ClyvovetApplication` |
| `controller` | Expõe rotas REST, traduz HTTP ↔ DTO | 6 classes |
| `service` | Orquestra regra de negócio e cache | 6 classes |
| `repository` | Acesso a dados via Spring Data JPA | 6 interfaces |
| `model` | Entidades JPA e enums de domínio | 7 entidades + 5 enums |
| `dto` | Contratos de entrada e saída da API | 7 subpacotes |
| `mapper` | Conversão Entity ↔ DTO | 7 componentes |
| `exception` | Tratamento global de erros | `GlobalExceptionHandler` |
| `security` | JWT, filtros, ownership | `JwtService` · `SegurancaService` · filtros |
| `config` | Convenções transversais | `WebConfig` · `SecurityConfig` · `CacheConfig` · `OpenApiConfig` |

```
src/main/java/br/com/fiap/clyvovet/
├── ClyvovetApplication.java
├── controller/    AnimalController · ClinicaController · EventoClinicoController
│                  PagamentoController · TutorController · VeterinarioController
├── service/       (mesmos 6 nomes, sufixo Service) · AuthService · UsuarioService
├── repository/    (mesmos 6 nomes, sufixo Repository) · UsuarioRepository
│                  RepositorioBase (obterPorId · garantirQueExiste)
├── model/         Animal · Clinica · Endereco · EventoClinico · Pagamento
│                  Tutor · Veterinario
│                  FormaPagamento · Sexo · SexoAnimal · StatusPagamento · TipoEvento
├── dto/
│   ├── animal/         AnimalRequest · AnimalResponse
│   ├── clinica/        ClinicaRequest · ClinicaResponse
│   ├── endereco/       EnderecoRequest · EnderecoResponse
│   ├── eventoClinico/  EventoClinicoRequest · EventoClinicoResponse
│   ├── exception/      ErroValidacao
│   ├── pagamento/      PagamentoRequest · PagamentoResponse
│   ├── tutor/          TutorRequest · TutorResponse
│   └── veterinario/    VeterinarioRequest · VeterinarioResponse
├── mapper/        AnimalMapper · ClinicaMapper · EnderecoMapper · EventoClinicoMapper
│                  PagamentoMapper · TutorMapper · UsuarioMapper · VeterinarioMapper
│                  Referencias · RelacionamentosDoEvento
└── exception/     GlobalExceptionHandler · RegraDeNegocioException
                   RecursoNaoEncontradoException · Recurso
```

---

## Bootstrap

[`ClyvovetApplication`](../src/main/java/br/com/fiap/clyvovet/ClyvovetApplication.java)
carrega apenas duas anotações:

```java
@SpringBootApplication
@EnableCaching
public class ClyvovetApplication { ... }
```

`@EnableCaching` é o que ativa o processamento de `@Cacheable`/`@CacheEvict` nos
services. Sem ela, as anotações seriam ignoradas silenciosamente.

Não existe nenhuma classe `@Configuration` no projeto — toda a configuração vem de
auto-configuração do Spring Boot mais os arquivos `.properties`.

---

## Fluxo de uma requisição

### Leitura — `GET /api/v1/animais?nome=Thor&page=0&size=10`

| # | Camada | O que acontece |
|---|---|---|
| 1 | Controller | Spring liga `nome`/`especie` aos `@RequestParam` e monta o `Pageable` a partir de `page`/`size`/`sort`, com defaults de `@PageableDefault(size = 10, sort = "nome")` |
| 2 | Service | `@Cacheable` calcula a chave `"Thor-null-0-10"`. Se houver hit, retorna sem tocar no banco |
| 3 | Repository | Em caso de miss, executa a JPQL `buscarPorFiltros`, que ignora filtros nulos |
| 4 | Mapper | `Page.map(...)` converte cada entidade em `AnimalResponse`, achatando as associações |
| 5 | Controller | `ResponseEntity.ok(...)` → 200 com o JSON de `Page` |

### Escrita — `POST /api/v1/animais`

| # | Camada | O que acontece |
|---|---|---|
| 1 | Controller | `@Valid` dispara o Bean Validation. Se falhar, o `GlobalExceptionHandler` intercepta e devolve 400 — o service nunca é chamado |
| 2 | Service | Busca o `Tutor` por `tutorId`; se não existir, lança `EntityNotFoundException` → 404 |
| 3 | Mapper | `toEntity(request, tutor)` monta a entidade já com a associação resolvida |
| 4 | Repository | `save(...)` — o Hibernate gera o UUID e faz o INSERT |
| 5 | Service | `@CacheEvict(allEntries = true)` limpa o cache `animais` |
| 6 | Controller | 201 com o `AnimalResponse` da entidade persistida |

---

## Responsabilidades por camada

### Controller

Cada um dos 6 controllers tem exatamente a mesma forma:

```java
@RestController
@RequestMapping("/animais")
@RequiredArgsConstructor          // injeção por construtor via Lombok
@Tag(name = "Animais", description = "Gerenciamento de animais")
public class AnimalController {
    private final AnimalService animalService;
    // 5 métodos: listarTodos, buscarPorId, criar, atualizar, deletar
}
```

Responsabilidades: mapear rota e verbo, aplicar `@Valid`, montar o `Pageable`,
definir o status HTTP e documentar via `@Tag`/`@Operation`. **Nenhuma regra de
negócio.**

### Service

Onde mora a lógica. Cada operação de escrita segue o mesmo roteiro:

1. Resolver as FKs pelos repositórios correspondentes, lançando
   `EntityNotFoundException` com mensagem específica para cada uma
2. Delegar a montagem/atualização da entidade ao mapper
3. Persistir
4. Converter o resultado em Response

Não há `@Transactional` explícito em nenhum service — cada `save`/`delete` roda na
transação implícita do próprio Spring Data. Ver
[07-pendencias-e-divergencias.md](07-pendencias-e-divergencias.md).

### Repository

Interfaces `JpaRepository<Entidade, UUID>`, cada uma com um único método de busca
declarado. O padrão de filtro opcional é sempre o mesmo:

```java
@Query("SELECT a FROM Animal a WHERE " +
       "(:nome IS NULL OR LOWER(a.nome) LIKE LOWER(CONCAT('%', :nome, '%'))) AND " +
       "(:especie IS NULL OR LOWER(a.especie) LIKE LOWER(CONCAT('%', :especie, '%')))")
Page<Animal> buscarPorFiltros(@Param("nome") String nome,
                              @Param("especie") String especie,
                              Pageable pageable);
```

A cláusula `:param IS NULL OR ...` faz o filtro desaparecer quando não informado, o
que evita precisar de Specifications ou Criteria API.

### Mapper

Componentes Spring escritos à mão — **não** MapStruct, apesar do nome do pacote.
Quatro métodos por mapper, quando aplicável:

| Método | Uso |
|---|---|
| `toEntity(request, ...)` | Criação: monta entidade nova |
| `atualizar(entidade, request, ...)` | PUT: sobrescreve todos os campos in-place |
| `aplicarPatch(entidade, patch, ...)` | PATCH: sobrescreve **só os campos presentes** |
| `toResponse(entidade)` | Saída: converte para DTO |

O `aplicarPatch` usa o `aplicarSePresente` de `AtualizacaoParcial`, que aplica o valor
só quando ele veio na requisição — uma linha por campo, em vez de um `if (x != null)`
repetido dezenas de vezes, que é onde costuma passar despercebido o campo que ninguém
lembrou de copiar.

`EnderecoMapper` é compartilhado por Tutor, Clinica e Veterinario, já que os três
embutem o mesmo `@Embeddable`.

Os mappers de entidades com associação fazem *null-guard* antes de achatar:

```java
UUID tutorId = animal.getTutor() != null ? animal.getTutor().getId() : null;
String tutorNome = animal.getTutor() != null ? animal.getTutor().getNome() : null;
```

Isso desnormaliza o nome da entidade relacionada dentro da resposta, poupando o
cliente de uma segunda chamada.

---

## Cache

Configuração: `spring-boot-starter-cache` com Caffeine, montado em
[`CacheConfig`](../src/main/java/br/com/fiap/clyvovet/config/CacheConfig.java) —
**expiração de 10 minutos e teto de 1.000 entradas**, em memória e por instância.

O `ConcurrentMapCacheManager` padrão foi trocado justamente por não ter nenhum dos
dois limites: com o `tutorId` na chave, o número de combinações possíveis passou a
crescer junto com a base de usuários.

| Cache | Alimentado por | Invalidado por |
|---|---|---|
| `tutores` | `TutorService.listarTodos` | criar, atualizar, deletar de Tutor |
| `animais` | `AnimalService.listarTodos` | criar, atualizar, deletar de Animal |
| `clinicas` | `ClinicaService.listarTodos` | criar, atualizar, deletar de Clinica |
| `veterinarios` | `VeterinarioService.listarTodos` | criar, atualizar, deletar de Veterinario |
| `eventos` | `EventoClinicoService.listarTodos` | criar, atualizar, deletar de EventoClinico |
| `pagamentos` | `PagamentoService.listarTodos` | criar, atualizar, deletar de Pagamento |

**Só as listagens são cacheadas.** `buscarPorId` vai ao banco toda vez.

Formato da chave — sempre os dois filtros mais a paginação:

```java
@Cacheable(value = "animais",
           key = "#nome + '-' + #especie + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
```

A invalidação é sempre `allEntries = true`: qualquer escrita descarta o cache inteiro
daquela entidade. É a estratégia mais simples e mais segura contra entradas órfãs.

Duas limitações conhecidas dessa chave estão documentadas em
[07-pendencias-e-divergencias.md](07-pendencias-e-divergencias.md): ela ignora o
`sort`, e a invalidação não cruza entidades relacionadas.

---

## Tratamento de erros

[`GlobalExceptionHandler`](../src/main/java/br/com/fiap/clyvovet/exception/GlobalExceptionHandler.java)
é um `@ControllerAdvice` com dois handlers:

| Exceção | Status | Corpo | Origem |
|---|---|---|---|
| `MethodArgumentNotValidException` | 400 | `List<ErroValidacao>` | `@Valid` no controller |
| `EntityNotFoundException` | 404 | `ErroValidacao` com `campo = "id"` | services, ao resolver id ou FK |

`ErroValidacao` é um par simples:

```java
public class ErroValidacao {
    private String campo;
    private String mensagem;
}
```

O handler de 404 existe porque, sem ele, o Spring devolveria 500 para
`EntityNotFoundException` — o comentário no próprio código registra essa motivação.

Exceções fora dessas duas (enum inválido em query param, JSON malformado, violação de
constraint no banco) caem no tratamento padrão do Spring Boot, que responde com o
formato `{"timestamp", "status", "error", "path"}`.

---

## Documentação da API

`springdoc-openapi-starter-webmvc-ui` gera a especificação automaticamente a partir
dos controllers. Cada classe tem `@Tag` e cada método tem `@Operation(summary = ...)`
em português.

| Rota | Conteúdo |
|---|---|
| `/swagger-ui.html` | Interface interativa |
| `/v3/api-docs` | Especificação OpenAPI 3 em JSON |

Não há `@ApiResponse`, `@Schema` ou exemplos customizados — a documentação é a
inferida das assinaturas mais os `summary`.

---

## Decisões de projeto observáveis no código

| Decisão | Efeito |
|---|---|
| UUID como PK em vez de sequence | IDs gerados na aplicação, não no banco; independente de fornecedor |
| Nome de campo Java ≠ nome de coluna | Permite casar com o schema legado do "projeto completo" sem renomear o domínio |
| Mappers manuais em vez de MapStruct | Zero geração de código; mais verboso, mas explícito |
| `record` para a maioria dos Responses | Imutáveis por construção |
| `FetchType.EAGER` explícito nos `@ManyToOne` | Evita `LazyInitializationException` na serialização, ao custo de joins sempre presentes |
| DTOs em vez de expor entidades | O cliente nunca vê o grafo JPA; sem risco de recursão infinita no JSON |
| Filtro opcional via `:param IS NULL OR ...` | Uma query cobre as 4 combinações de filtros |
