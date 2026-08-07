# Guia de desenvolvimento

## Ambiente

| Requisito | Versão |
|---|---|
| JDK | 17+ |
| Maven | 3.8+ (ou use o wrapper `./mvnw`) |
| Docker | opcional, só para `docker compose` |
| Oracle FIAP | opcional, só para o perfil `oracle` |

Lombok exige *annotation processing* habilitado na IDE:

- **IntelliJ IDEA** — instalar o plugin Lombok e marcar
  *Settings → Build → Compiler → Annotation Processors → Enable annotation processing*
- **VS Code** — extensão *Lombok Annotations Support for VS Code*
- **Eclipse/STS** — rodar o instalador do `lombok.jar`

Sem isso, a IDE acusa "cannot find symbol getNome()" mesmo com o build do Maven
passando.

---

## Comandos

| Comando | O que faz |
|---|---|
| `./mvnw clean package` | Compila e gera `target/clyvovet-0.0.1-SNAPSHOT.jar` |
| `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` | Sobe com H2 em memória |
| `./mvnw test` | Roda os testes |
| `./mvnw clean package -DskipTests` | Build sem testes |
| `./mvnw dependency:tree` | Árvore de dependências |
| `docker compose up --build` | Sobe API + H2 em containers |

No Windows use `mvnw.cmd`; o `.gitattributes` garante `LF` no `mvnw` e `CRLF` no
`.cmd`.

---

## Convenções do código

### Idioma

Todo o código de domínio é em **português**: nomes de classes, campos, métodos, rotas,
mensagens de erro e comentários. Anotações e tipos do framework permanecem em inglês.

```java
public Page<AnimalResponse> listarTodos(String nome, String especie, Pageable pageable)
```

### Nomenclatura por camada

| Camada | Padrão | Exemplo |
|---|---|---|
| Entidade | substantivo singular | `Animal` |
| Controller | `{Entidade}Controller` | `AnimalController` |
| Service | `{Entidade}Service` | `AnimalService` |
| Repository | `{Entidade}Repository` | `AnimalRepository` |
| Mapper | `{Entidade}Mapper` | `AnimalMapper` |
| DTO entrada | `{Entidade}Request` | `AnimalRequest` |
| DTO saída | `{Entidade}Response` | `AnimalResponse` |
| Rota | plural em português | `/animais` |

### Métodos padrão

| Camada | Métodos |
|---|---|
| Controller | `listarTodos`, `buscarPorId`, `criar`, `atualizar`, `deletar` |
| Service | `listarTodos`, `buscarPorId`, `salvar`, `atualizar`, `deletar` |
| Repository | `buscarPorFiltros` + herdados de `JpaRepository` |
| Mapper | `toEntity`, `atualizar`, `{entidade}ToResponse` |

Exceção conhecida: `PagamentoService` usa `criar` em vez de `salvar`.

### Injeção de dependências

Sempre por construtor, via `@RequiredArgsConstructor` do Lombok sobre campos `final`.
Não há `@Autowired` em campo em lugar nenhum do projeto.

```java
@Service
@RequiredArgsConstructor
public class AnimalService {
    private final AnimalRepository animalRepository;
    private final AnimalMapper animalMapper;
    private final TutorRepository tutorRepository;
}
```

### DTOs

| Tipo | Estilo | Motivo |
|---|---|---|
| Request | classe com `@NoArgsConstructor @AllArgsConstructor @Getter` | Jackson precisa do construtor vazio para desserializar |
| Response | `record` | imutável, conciso |

Duas exceções: `PagamentoRequest` usa `@Data` e `PagamentoResponse` é `@Data` com
setters em vez de `record`.

Requests **não** têm setters — são preenchidos por reflection pelo Jackson.

### Entidades

Sempre o mesmo conjunto de anotações Lombok:

```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity
public class Animal { ... }
```

Não use `@Data` em entidade JPA: ele gera `equals`/`hashCode` sobre todos os campos,
o que quebra com associações e proxies do Hibernate.

---

## Adicionando uma entidade nova

Siga a ordem abaixo para manter a simetria com as seis existentes. Exemplo: `Vacina`.

### 1. Entidade — `model/Vacina.java`

```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity
public class Vacina {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String nome;
    @Column(name = "data_aplicacao")
    private LocalDate dataAplicacao;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "animal_id")
    private Animal animal;
}
```

Se o nome da coluna diferir do campo, anote com `@Column`. Enums levam
`@Enumerated(EnumType.STRING)`.

### 2. DTOs — `dto/vacina/`

`VacinaRequest` com Bean Validation, `VacinaResponse` como `record`. Achate as
associações em `{assoc}Id` + `{assoc}Nome`.

### 3. Mapper — `mapper/VacinaMapper.java`

```java
@Component
@RequiredArgsConstructor
public class VacinaMapper {
    public Vacina toEntity(VacinaRequest request, Animal animal) { ... }
    public void atualizar(Vacina vacina, VacinaRequest request, Animal animal) { ... }
    public VacinaResponse vacinaToResponse(Vacina vacina) { ... }  // com null-guard
}
```

### 4. Repository — `repository/VacinaRepository.java`

```java
public interface VacinaRepository extends JpaRepository<Vacina, UUID> {
    @Query("SELECT v FROM Vacina v WHERE " +
           "(:nome IS NULL OR LOWER(v.nome) LIKE LOWER(CONCAT('%', :nome, '%'))) AND " +
           "(:animalNome IS NULL OR LOWER(v.animal.nome) LIKE LOWER(CONCAT('%', :animalNome, '%')))")
    Page<Vacina> buscarPorFiltros(@Param("nome") String nome,
                                  @Param("animalNome") String animalNome,
                                  Pageable pageable);
}
```

### 5. Service — `service/VacinaService.java`

Cinco métodos, com `@Cacheable` na listagem e `@CacheEvict(allEntries = true)` nas
escritas. Resolva as FKs lançando `EntityNotFoundException` com mensagem específica.

### 6. Controller — `controller/VacinaController.java`

```java
@RestController
@RequestMapping("/vacinas")
@RequiredArgsConstructor
@Tag(name = "Vacinas", description = "Gerenciamento de vacinas")
public class VacinaController { ... }
```

Cinco métodos com `@Operation(summary = ...)`, `@Valid` nos bodies e
`@PageableDefault(size = 10, sort = "nome")` na listagem.

### 7. DDL — `resources/db/db-oracle.sql`

Adicione o `CREATE TABLE` com PK `VARCHAR2(36)`, FKs, uniques e checks alinhados com
os enums. Se houver seed, use `fn_uuid()` e resolva FKs por chave natural em bloco
`DECLARE/BEGIN/END`.

### 8. Documentação

Atualize [02-modelo-de-dados.md](02-modelo-de-dados.md),
[03-api-rest.md](03-api-rest.md) e o [README](../README.md) da raiz.

---

## Checklist de revisão

Antes de abrir PR, confira:

- [ ] O `@Column` bate exatamente com o nome no `db-oracle.sql`
- [ ] Enums têm `@Enumerated(EnumType.STRING)` e os valores batem com o CHECK do banco
- [ ] Request tem Bean Validation em todos os campos obrigatórios
- [ ] Limites de `@Size` cabem nos tamanhos de coluna do Oracle
- [ ] Mapper faz null-guard antes de acessar associação
- [ ] Service lança `EntityNotFoundException` para cada FK que resolve
- [ ] `@Cacheable` na listagem e `@CacheEvict` nas três escritas
- [ ] Controller devolve 201 no POST e 204 no DELETE
- [ ] `@Operation(summary = ...)` em cada método, em português
- [ ] Rota no plural, em português

---

## Testes

O projeto tem **um** teste:
[`ClyvovetApplicationTests`](../src/test/java/br/com/fiap/clyvovet/ClyvovetApplicationTests.java),
com o `contextLoads()` gerado pelo Spring Initializr.

Ele é um `@SpringBootTest`, o que significa que sobe o contexto inteiro — incluindo o
DataSource. **Com o perfil `oracle` ativo (o default), o teste falha se não houver
conectividade com o Oracle da FIAP.** Para rodar isolado:

```bash
./mvnw test -Dspring.profiles.active=dev
```

### Onde começar a cobrir

Sugestão de ordem, da maior relação custo/benefício para a menor:

| Alvo | Tipo | Foco |
|---|---|---|
| Services | unitário com Mockito | resolução de FK, `EntityNotFoundException`, chamadas ao mapper |
| Mappers | unitário puro | null-guard das associações, achatamento correto |
| Controllers | `@WebMvcTest` | status HTTP, corpo do 400, binding dos filtros |
| Repositories | `@DataJpaTest` | as JPQL com filtros nulos e combinados |

O padrão `:param IS NULL OR ...` das JPQL é o candidato natural a `@DataJpaTest`, já
que precisa funcionar nas quatro combinações de filtros.

---

## Build e artefatos

```
target/
├── classes/                       # .class compilados + resources
├── generated-sources/annotations/ # saída do Lombok
├── test-classes/
└── clyvovet-0.0.1-SNAPSHOT.jar    # JAR executável (após package)
```

`target/` está no `.gitignore`, assim como `.idea/`, `.vscode/`, `.settings/` e
`application-prod.properties`.

---

## Fluxo de trabalho

Branch principal: `main`. Os commits do projeto seguem
[Conventional Commits](https://www.conventionalcommits.org/):

```
feat: adiciona endpoint de vacinas
fix: corrige mapeamento da coluna observacoes
docs: atualiza README com filtros de pagamento
refactor: extrai resolução de FK para método privado
```
