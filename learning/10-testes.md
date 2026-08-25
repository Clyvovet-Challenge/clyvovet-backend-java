# 10 — Testes

> **Pré-requisito:** ter passado pelos documentos [03](03-api-rest.md) e
> [06](06-spring-security.md) — os testes daqui exercitam exatamente aquilo.

---

## Para que serve um teste automatizado

A resposta comum é "para achar bugs". É incompleta.

O valor real aparece depois: **poder mudar o código sem medo**. Com uma suíte que roda em
segundos, você refatora, renomeia, extrai um método — e algo avisa se você quebrou o que já
funcionava. Sem ela, cada mudança é uma aposta, e o time para de mexer no que "está
funcionando" (que é como código apodrece).

Este projeto tem **126 testes**. E nem sempre foi assim: até a Sprint 3 havia **um**, o
`contextLoads()` gerado pelo Initializr — e ele **falhava sem conexão com o Oracle da FIAP**.
Era por isso que o `Dockerfile` usava `-DskipTests`.

---

## Os tipos de teste

| Tipo | Testa | Velocidade | Aqui |
|---|---|---|---|
| **Unitário** | uma classe isolada | milissegundos | mappers (`src/test/.../mapper/`) |
| **Integração** | várias camadas juntas | segundos | **a maioria** — `crud/`, `security/` |
| **E2E** | sistema inteiro, pelo navegador | minutos | não há |

A "pirâmide de testes" clássica manda ter muitos unitários e poucos de integração. **Este
projeto inverte** — e por um bom motivo.

💡 **Conceito: teste o que pode quebrar**

O que este projeto precisa provar é: *"um tutor consegue ver o pet de outro?"*, *"apagar um
tutor com animais devolve 409?"*, *"o filtro por nome funciona?"*.

Nada disso aparece numa classe isolada. Ownership envolve filtro + `@PreAuthorize` + cache +
JWT — **quatro peças que só falham juntas**. Um teste unitário do service com repositório
falso passaria feliz enquanto a API vaza dados.

A regra: **teste na altura onde o comportamento existe.** Se a regra nasce da interação
entre camadas, teste a interação.

---

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
| `@SpringBootTest` | sobe o contexto **inteiro** da aplicação |
| `@AutoConfigureMockMvc` | injeta o `MockMvc` |

**`MockMvc`** simula requisições HTTP **sem abrir porta de rede**. A requisição passa pela
cadeia real — filtros, controller, service, repositório, banco — mas sem socket, sem
serialização de rede. Realista e rápido ao mesmo tempo.

O comentário da classe explica por que ela existe: sem ela, *"o mesmo helper de token copiado
em cada arquivo, cada um com uma regra própria para descobrir a senha do usuário"*.

### Os helpers

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

⚠️ **O login é real.** Emite um JWT de verdade, que passa pelo filtro de verdade. **Não há
mock de segurança.** Se a autorização quebrar, os testes acusam — o que não aconteceria com
`@WithMockUser`, que pula a autenticação inteira.

### Um helper que esconde uma decisão de contrato

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

Quando o envelope de paginação mudou (documento [03](03-api-rest.md)), **uma linha** mudou na
suíte inteira. DRY vale em teste tanto quanto em produção.

---

## Sem rollback — a decisão contra-intuitiva

O padrão que se ensina é `@Transactional` na classe de teste: tudo o que o teste grava é
desfeito no fim. Aqui **não é assim**:

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

💡 **Conceito: por que o rollback esconderia bugs**

O Hibernate não manda o `INSERT` na hora — ele acumula e envia no **commit** (isso se chama
*flush*).

Num teste com rollback, o commit **nunca acontece**. Então:

- CPF duplicado? A constraint `UNIQUE` **nunca é verificada**.
- Texto de 1001 caracteres numa coluna de 1000? **Nunca chega ao banco**.
- FK apontando para id inexistente? **Nunca é validada**.

O teste passaria — dando falsa segurança sobre exatamente o que deveria provar.

Gravando de verdade, o banco reclama de verdade. O preço é a limpeza manual, feita numa
pilha (`Deque`), em ordem inversa à do cadastro — que é a ordem que as FKs permitem: o
pagamento sai antes do evento, o evento antes do animal.

---

## Perfil de teste isolado

```properties
# src/test/resources/application.properties
spring.profiles.active=dev
```

Uma linha que resolveu um problema real. O perfil padrão da aplicação é `oracle` — então
`mvn test` **falhava sem conectividade com a FIAP**, mesmo em testes que nada tinham a ver com
o Oracle.

Com `dev`, roda em H2 na memória, sem rede. E aí o `-DskipTests` do build deixa de ser
necessário — que é **pré-requisito do CI da Sprint 4**, onde a pipeline precisa executar os
testes.

---

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

Compare a legibilidade:

```java
buscar("/api/v1/animais/44444444-4444-4444-4444-000000000001", tokenTutor(MARIA))   // ❌
buscar("/api/v1/animais/" + ANIMAL_BOLINHA_DO_LUCAS, tokenTutor(MARIA))            // ✅
```

A segunda linha **conta a história do teste**: a Maria tentando acessar o pet do Lucas. A
primeira é um enigma.

---

## Um teste de verdade, comentado

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

Três coisas a copiar:

**1. `@DisplayName` descreve comportamento**, não implementação. Quando o teste falhar, o
relatório diz *"os dois filtros se somam, não se substituem"* — e você sabe o que parou de
funcionar sem abrir o código.

**2. AssertJ** — `assertThat(...).containsExactly(...)`. Encadeável e com mensagem de erro
legível:

```
Expecting actual: ["Camila Ferreira", "Rafael Matos"]
to contain exactly: ["Camila Ferreira"]
```

**3. Verifica os dois sentidos** — o que o filtro traz **e** o que ele deixa de fora. É a
lição central desta classe, e ela nasceu de um bug.

### Por que "os dois sentidos" virou regra aqui

Todo filtro por texto devolvia lista vazia — **por meses** (o bug do `ESCAPE`, documento
[03](03-api-rest.md)). E havia testes de listagem, que passavam.

> Os testes que existiam passavam porque só exercitavam o recorte por tutor, com o parâmetro
> de texto indo `null` — caindo sempre no ramo `:nome IS NULL`.

**Um teste que percorre só o caminho fácil não protege nada.** Ele dá a sensação de cobertura
sem a cobertura. Hoje, sem o `ESCAPE`, três testes de `FiltrosDeBuscaTest` falham — é isso que
um teste de regressão faz.

---

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

Repare no `ValidacaoDeEntradaTest`: **1000 passa, 1001 não**. Isso se chama **teste de
fronteira**, e é onde o bug mora. Testar com 500 e 2000 caracteres não prova nada sobre onde
está o limite — só testar exatamente na borda prova.

---

## Teste que só roda quando pode

```bash
DB_USERNAME=seu_rm DB_PASSWORD=sua_senha ./mvnw test -Dtest=EscapeNoOracleTest
```

`EscapeNoOracleTest` depende do Oracle real. Ele roda por JDBC puro sobre `dual` — não lê nem
grava tabela do projeto, não sobe o contexto, e portanto não dispara Flyway nem `validate` num
banco compartilhado. E fica **pulado** enquanto a variável não existir.

Esse é o padrão certo para teste que depende de recurso externo: **skip condicional**. As
alternativas são piores — teste comentado (que ninguém lembra de descomentar) ou teste que
falha para todo mundo (que ensina o time a ignorar falha vermelha).

---

## Boas práticas que aparecem aqui

| Prática | Por quê |
|---|---|
| Um comportamento por teste | quando falha, a causa é uma só |
| `@DisplayName` em português | o relatório vira documentação |
| Sem dependência de ordem | `@AfterEach` limpa cache e recursos criados |
| Testar o caminho triste | 404, 403, 400, 409 — não só o feliz |
| Testar a **fronteira** | 1000 passa, 1001 não |
| Teste de regressão para cada bug corrigido | garante que ele não volta |

Essa última é a mais valiosa e a mais pulada. **Todo bug corrigido merece um teste** — não
para provar que você consertou, mas para garantir que ninguém desfaz sem perceber, seis meses
depois.

---

## Rodando

```bash
./mvnw test                                                # tudo
./mvnw test -Dtest=OwnershipTest                           # uma classe
./mvnw test -Dtest=OwnershipTest#cacheNaoVazaEntreTutores  # um método
```

---

## Consolidação

**Entender**
1. O que `MockMvc` faz? Por que não subir um servidor de verdade?
2. Por que `@DisplayName` em vez de confiar no nome do método?

**Aplicar**
3. Escreva o `@DisplayName` de um teste que prova que um tutor recebe 403 ao buscar o pet de
   outro.
4. Você corrigiu um bug onde `?especie=CAO` não achava nada. Que teste escreveria?

**Analisar**
5. Por que os testes **não** usam `@Transactional` com rollback? Cite três coisas que ficariam
   sem verificação.
6. Por que `TesteDeApi` faz login de verdade em vez de usar `@WithMockUser`?
7. Por que `FiltrosDeBuscaTest` verifica também o que o filtro **não** traz?

**Avaliar**
8. A suíte tem mais testes de integração que unitários, invertendo a "pirâmide". Isso é um
   problema neste projeto? Justifique.
9. Você vai implementar o `CalculadoraDeRisco` do Painel do Veterinário (uma classe de cálculo
   puro). Que tipo de teste usaria, e por que seria diferente do padrão daqui?
10. Se você adicionasse um campo novo numa entidade, quais testes esperaria ver quebrar
    primeiro?

---

## Se você levar só uma coisa daqui

**Um teste que percorre só o caminho fácil dá a sensação de cobertura sem a cobertura.** Os
filtros desta API ficaram quebrados por meses com testes verdes — porque nenhum deles
exercitava um filtro de verdade.

---

**Anterior:** [09 — Flyway e migrations](09-flyway-e-migrations.md) ·
**Voltar ao índice:** [README](README.md)
