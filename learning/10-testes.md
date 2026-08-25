# 10 — Testes

## O que é

Teste automatizado é código que exercita o seu código e falha quando o comportamento muda.
O valor não está em "ter testes" — está em **poder mudar o código sem medo**, porque algo
avisa se você quebrou o que já funcionava.

Este projeto tem **126 testes**, e vale saber que nem sempre foi assim: até a Sprint 3 havia
**um**, o `contextLoads()` do Initializr — e ele falhava sem conexão com o Oracle da FIAP. Era
por isso que o `Dockerfile` usava `-DskipTests`.

## A pirâmide, e onde este projeto fica

| Tipo | Escopo | Velocidade | Aqui |
|---|---|---|---|
| Unitário | uma classe isolada | ms | mappers (`src/test/.../mapper/`) |
| Integração | várias camadas juntas | s | **a maioria** — `crud/`, `security/` |
| E2E | sistema inteiro | min | não há |

A escolha por integração é consciente: o que este projeto precisa provar — ownership,
constraint, filtro, status HTTP — só aparece com a cadeia real de filtros rodando.

## A base: `TesteDeApi`

```java
// src/test/java/br/com/fiap/clyvovet/support/TesteDeApi.java
@SpringBootTest
@AutoConfigureMockMvc
public abstract class TesteDeApi {

    protected static final String ADMIN = "admin@clyvovet.com";
    protected static final String VETERINARIA = "camila.ferreira@vetcare.com.br";
    protected static final String LUCAS = "lucas.santos@email.com";
    protected static final String MARIA = "maria.oliveira@email.com";

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
```

| Anotação | O que faz |
|---|---|
| `@SpringBootTest` | sobe o contexto inteiro da aplicação |
| `@AutoConfigureMockMvc` | injeta o `MockMvc` |

**`MockMvc`** simula requisições HTTP **sem subir servidor**: passa pela cadeia real de
filtros, controllers e services, mas sem socket. Rápido e realista ao mesmo tempo.

O comentário da classe explica por que ela existe: sem ela, *"o mesmo helper de token
copiado em cada arquivo, cada um com uma regra própria para descobrir a senha do usuário"*.

### Helpers

```java
protected String token(String email, String senha) throws Exception {
    String corpo = mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
    return objectMapper.readTree(corpo).get("accessToken").asText();
}

protected ResultActions buscar(String url, String token) throws Exception {
    return mockMvc.perform(get(url).header("Authorization", "Bearer " + token));
}
```

O login é **real**: emite JWT de verdade, que passa pelo filtro de verdade. Não há mock de
segurança — se a autorização quebrar, o teste acusa.

### O helper que esconde uma decisão de contrato

```java
/**
 * O caminho e "page.totalElements", e nao "totalElements" na raiz, desde
 * que o WebConfig passou a serializar as paginas via PagedModel. Concentrar
 * a leitura aqui e o que faz uma mudanca dessas custar uma linha em vez de
 * uma varredura pela suite.
 */
protected int totalDe(JsonNode corpo) {
    return corpo.get("page").get("totalElements").asInt();
}
```

Quando o envelope de paginação mudou, **uma linha** mudou na suíte inteira.

## Sem rollback — decisão contra-intuitiva

O padrão comum é `@Transactional` na classe de teste, que dá rollback ao fim. Aqui **não**:

```java
/**
 * Os testes gravam de verdade — nao ha transacao de teste com rollback,
 * porque ela adiaria os INSERTs para um commit que nunca acontece e
 * esconderia justamente o que se quer verificar: constraint de unicidade,
 * chave estrangeira, limite de coluna.
 */
protected void removerDepois(String url) {
    aRemover.push(url);
}

@AfterEach
void limparRecursosCriados() throws Exception {
    ...
    while (!aRemover.isEmpty()) {
        remover(aRemover.pop(), admin);   // ordem inversa: pagamento antes do evento
    }
}
```

O argumento é forte. Numa transação com rollback, o Hibernate adia os INSERTs até o commit —
que nunca vem. Um CPF duplicado ou um texto acima do limite da coluna **não estouraria**, e o
teste passaria dando falsa segurança sobre exatamente o que deveria provar.

O preço é a limpeza manual, feita numa pilha (`Deque`) — remoção em ordem inversa à do
cadastro, que é a ordem que as FKs permitem.

## Perfil de teste isolado

```properties
# src/test/resources/application.properties
spring.profiles.active=dev
```

Uma linha que resolveu um problema real: o perfil padrão é `oracle`, então `mvn test`
**falhava sem conectividade com a FIAP**. Com `dev`, roda em H2 na memória, sem rede — e o
`-DskipTests` do build deixa de ser necessário, o que é pré-requisito do CI da Sprint 4.

## Dados de teste com nome

```java
// src/test/java/br/com/fiap/clyvovet/support/SeedV2.java
public final class SeedV2 {
    public static final String TUTOR_LUCAS = "22222222-2222-2222-2222-000000000001";

    /** Lucas e dono do Bolinha; Maria, da Mimi e do Rex. */
    public static final String ANIMAL_BOLINHA_DO_LUCAS = "44444444-4444-4444-4444-000000000001";

    /** Nao existe em tabela nenhuma — serve para exercitar o 404. */
    public static final String ID_INEXISTENTE = "00000000-0000-0000-0000-000000000999";
}
```

Antes eram literais soltos em cada classe, *"o que obrigava a decorar qual UUID era de quem"*.
`ANIMAL_BOLINHA_DO_LUCAS` diz de quem é o pet sem sair do arquivo de teste.

## Um teste de verdade

```java
// src/test/java/br/com/fiap/clyvovet/crud/FiltrosDeBuscaTest.java
@Test
@DisplayName("veterinarios: os dois filtros se somam, nao se substituem")
void filtrosSeSomam() throws Exception {
    String admin = tokenAdmin();

    assertThat(nomesEm("/api/v1/veterinarios?nome=Camila&especialidade=Clinica", admin))
            .containsExactly("Camila Ferreira");
    // Camila existe e Cardiologia existe, mas nao na mesma pessoa.
    assertThat(nomesEm("/api/v1/veterinarios?nome=Camila&especialidade=Cardiologia", admin)).isEmpty();
}
```

Três coisas a copiar daqui:

1. **`@DisplayName` descreve comportamento**, não implementação. Numa falha, o relatório diz
   o que parou de funcionar.
2. **AssertJ** (`assertThat(...).containsExactly(...)`) — encadeável e com mensagem de erro
   legível.
3. **Verifica os dois sentidos**: o que o filtro traz **e** o que ele deixa de fora.

Esse último ponto é a lição central desta classe, e ela nasceu de um bug: todo filtro por
texto devolvia lista vazia por meses. Os testes existentes passavam porque *"só exercitavam o
recorte por tutor, com o texto indo nulo"* — sempre pelo ramo `:nome IS NULL`. Um teste que
percorre só o caminho fácil não protege nada.

## O que a suíte cobre

| Classe | Prova |
|---|---|
| `CadastroCrudTest`, `AtendimentoCrudTest` | CRUD ponta a ponta |
| `FiltrosDeBuscaTest` | filtros trazem e excluem o que devem |
| `AtualizacaoParcialTest` | PATCH altera só o enviado |
| `ValidacaoDeEntradaTest` | limites exatos (1000 passa, 1001 não; `00:00` e `23:59` sim, `25:00` não) |
| `IntegridadeReferencialTest` | apagar com dependentes dá 409, sem vazar o nome da constraint |
| `OwnershipTest` | tutor não vê nem cadastra pet alheio; cache não vaza entre tutores |
| `AutorizacaoTest` | matriz de perfil × rota |
| `CicloDeSessaoTest`, `JwtServiceTest` | login, refresh, logout, expiração |
| `BloqueioContaTest`, `RateLimitTest` | 5 falhas bloqueiam; volume por IP responde 429 |
| `MigrationsMySqlTest` | o conjunto `mysql/` aplica sem erro |
| `EscapeNoOracleTest` | o `ESCAPE '\'` contra o Oracle real — **pulado** sem `DB_USERNAME` |

## Teste que só roda quando pode

```bash
DB_USERNAME=seu_rm DB_PASSWORD=sua_senha ./mvnw test -Dtest=EscapeNoOracleTest
```

`EscapeNoOracleTest` roda por JDBC puro sobre `dual` — não lê nem grava tabela do projeto, não
sobe o contexto, e portanto não dispara Flyway nem `validate` num banco compartilhado. Fica
**pulado** enquanto a variável não existir, então não atrapalha o `mvn test` de ninguém.

É o padrão certo para teste que depende de recurso externo: **skip condicional**, não teste
comentado nem quebrado.

## Boas práticas que aparecem aqui

| Prática | Por quê |
|---|---|
| Um comportamento por teste | a falha aponta uma causa só |
| `@DisplayName` em português | o relatório vira documentação |
| Sem dependência de ordem | `@AfterEach` limpa cache e recursos criados |
| Testar o caminho triste | 404, 403, 400, 409 — não só o caminho feliz |
| Testar o **limite** | 1000 passa, 1001 não; é na borda que o bug mora |
| Teste de regressão para bug corrigido | sem o `ESCAPE`, três testes de `FiltrosDeBuscaTest` falham |

## Rodando

```bash
./mvnw test                              # tudo
./mvnw test -Dtest=OwnershipTest         # uma classe
./mvnw test -Dtest=OwnershipTest#cacheNaoVazaEntreTutores   # um método
```

## Perguntas de avaliação oral

1. Por que os testes **não** usam `@Transactional` com rollback? O que isso esconderia?
2. O que `MockMvc` faz? Por que não subir o servidor de verdade?
3. Por que existe `src/test/resources/application.properties` com `spring.profiles.active=dev`?
4. Por que `TesteDeApi` faz login de verdade em vez de mockar a segurança?
5. Por que `FiltrosDeBuscaTest` verifica também o que o filtro **não** traz?
6. Por que `EscapeNoOracleTest` é pulado por padrão?
7. Se você adicionasse um campo novo numa entidade, quais testes esperaria ver quebrar?

---

**Anterior:** [09 — Flyway e migrations](09-flyway-e-migrations.md) ·
**Voltar ao índice:** [README](README.md)
