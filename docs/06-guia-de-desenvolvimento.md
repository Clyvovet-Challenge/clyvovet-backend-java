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
| Service | `listarTodos`, `buscarPorId`, `criar`, `atualizar`, `deletar` |
| Repository | `buscarPorFiltros`, `obterPorId`, `garantirQueExiste` + herdados de `JpaRepository` |
| Mapper | `toEntity`, `atualizar`, `toResponse` |

O verbo do service é o mesmo do controller (`criar`), e o mapper usa `toResponse`
em todas as entidades — antes cada mapper tinha o próprio nome
(`animalToResponse`, `requestToTutor`) e só `PagamentoService` dizia `criar`.

`obterPorId` e `garantirQueExiste` vêm de
[`RepositorioBase`](../src/main/java/br/com/fiap/clyvovet/repository/RepositorioBase.java)
e já lançam `RecursoNaoEncontradoException` (404) quando o id não existe — é o que
mantém os services livres de `orElseThrow` repetido.

### Injeção de dependências

Sempre por construtor, via `@RequiredArgsConstructor` do Lombok sobre campos `final`.
Não há `@Autowired` em campo em lugar nenhum do projeto.

```java
@Service
@RequiredArgsConstructor
public class AnimalService {
    private final AnimalRepository animalRepository;
    private final TutorRepository tutorRepository;
    private final AnimalMapper animalMapper;
    private final SegurancaService seguranca;
}
```

### DTOs

| Tipo | Estilo | Motivo |
|---|---|---|
| Request | classe com `@NoArgsConstructor @AllArgsConstructor @Getter` | Jackson precisa do construtor vazio para desserializar |
| Response | `record` | imutável, conciso |

Uma exceção: `PagamentoRequest` usa `@Data`. Todos os Response são `record`.

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

São 98 testes, e `./mvnw test` roda todos sem banco externo:
`src/test/resources/application.properties` fixa o perfil `dev` (H2 em memória) e
desliga o rate limit, que barraria a rajada de chamadas dos próprios testes.

| Pacote | Classe | Cobre |
|---|---|---|
| `mapper` | um `…MapperTest` por mapper | cópia campo a campo, id preservado no `atualizar`, associação nula |
| `crud` | `CadastroCrudTest` | ciclo completo de tutor, clínica e veterinário; 409 em documento repetido; 404 por id inexistente |
| `crud` | `AtendimentoCrudTest` | ciclo de animal → evento → pagamento, com os nomes desnormalizados na resposta |
| `crud` | `FiltrosDeBuscaTest` | filtros por texto: o que trazem e o que deixam de fora |
| `crud` | `ValidacaoDeEntradaTest` | limites que precisam bater com a coluna do banco |
| `crud` | `IntegridadeReferencialTest` | remoção com dependentes responde 409, e não erro de servidor |
| `crud` | `EscapeNoOracleTest` | semântica do `LIKE ... ESCAPE` no Oracle real — **pulado** sem `DB_USERNAME` |
| `security` | ver [08-seguranca](08-seguranca.md#testes) | token, perfil, ownership, bloqueio de conta |

### Como escrever um novo

Os testes de API estendem
[`TesteDeApi`](../src/test/java/br/com/fiap/clyvovet/support/TesteDeApi.java), que
resolve login, header `Authorization`, leitura do JSON e limpeza:

```java
class MeuRecursoCrudTest extends TesteDeApi {

    @Test
    void criaERemove() throws Exception {
        String id = corpoDe(criar("/recursos", tokenAdmin(), CORPO)
                .andExpect(status().isCreated())).get("id").asText();
        removerDepois("/recursos/" + id);   // sai no fim, mesmo se o teste falhar
    }
}
```

Duas regras que evitam teste instável:

- **Grave de verdade, sem `@Transactional` na classe de teste.** A transação de teste
  adia os INSERTs para um commit que nunca acontece, e o que se queria verificar —
  unicidade, chave estrangeira, limite de coluna — deixa de acontecer.
- **Não reaproveite CPF, CNPJ ou CRMV do seed.** Eles têm constraint de unicidade e o
  seed segue uma sequência previsível; documentos de teste começam com `9`.

Ids do seed ficam em
[`SeedV2`](../src/test/java/br/com/fiap/clyvovet/support/SeedV2.java), com nome em vez
de UUID solto.

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

## Grafo do codebase (graphify)

`graphify-out/` guarda um mapa do projeto gerado pelo [graphify](https://pypi.org/project/graphifyy/)
e versionado junto com o codigo, de modo que qualquer clone ja venha com ele. O
hook `post-commit` reconstroi o grafo em segundo plano sempre que um commit toca
arquivos fora de `graphify-out/`.

O que fica de fora esta em `.graphifyignore`: as instrucoes das skills de IA, a
memoria de outros assistentes e o proprio `scripts/`. Sao arquivos que falam de
ferramentas, nao da aplicacao -- sem eles, um `graphify update .` completo
reproduz exatamente o grafo versionado.

### Nomes das comunidades

O graphify agrupa os nos em comunidades e nomeia cada uma pelo no mais conectado
do grupo, o que produz nomes como `org.junit.jupiter.api.Test`. Os nomes deste
projeto foram escritos a mao ("Testes de CRUD e Integracao") e ficam em
`graphify-out/.graphify_labels.json`.

Eles valem enquanto a comunidade tiver exatamente os mesmos membros -- e isso que
`graphify-out/.graphify_labels.json.sig` registra, e por isso os dois arquivos
sao versionados juntos. Quando o reagrupamento muda de verdade, as comunidades
afetadas voltam ao nome padrao. Para trazer os nomes de volta:

```bash
python scripts/label-communities.py    # devolve os nomes curados
graphify cluster-only .                # regenera GRAPH_REPORT.md e graph.html
```

O script nao confia no numero da comunidade, que nao e estavel entre rebuilds:
`scripts/community-labels.json` guarda quais nos formavam cada grupo e o nome vai
para a comunidade que herdou a maior parte deles. Depois de renomear comunidades
a mao, grave a nova referencia com `python scripts/label-communities.py --snapshot`.

O aviso pode ser automatico:

```bash
cp scripts/pre-commit .git/hooks/pre-commit
```

Esse hook roda `label-communities.py --check` antes de cada commit e avisa, sem
bloquear, quando algum nome saiu do lugar. Ele fica no `pre-commit` porque o
rebuild do `post-commit` e assincrono: quando o rebuild termina, o hook daquele
commit ja saiu ha muito tempo.

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
