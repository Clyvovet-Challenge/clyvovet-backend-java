# Pendências e divergências

Levantamento feito a partir da leitura completa do código, do DDL e da documentação.
Nada aqui impede o build ou a execução — são descolamentos entre **código**,
**schema do banco** e **documentação**, além de lacunas conhecidas.

Cada item traz o efeito prático e uma sugestão de correção. A decisão sobre o que
corrigir é do time.

## Resumo

Os itens marcados ✅ foram resolvidos na Sprint 3 (Flyway + Spring Security).

| # | Item | Severidade | Área | Situação |
|---|---|---|---|---|
| 1 | `REEMBOLSADO` vs `ESTORNADO` no check constraint | Alta | Banco | ✅ migration `V4` |
| 2 | Credenciais do Oracle versionadas em texto puro | Alta | Segurança | ✅ `DB_USERNAME`/`DB_PASSWORD` |
| 3 | Unicidade só existe no banco → 500 em duplicidade | Média | Erros | ✅ 409 no handler |
| 4 | Exclusão com dependentes → 500 | Média | Erros | aberto |
| 5 | Perfil `h2` não roda fora do Docker | Média | Configuração | ✅ documentado; use `dev` |
| 6 | `@Size` em `crmv` e `telefone` rejeita o formato do seed | Média | Validação | aberto |
| 7 | `dataPagamento` obrigatória impede registrar pendente | Média | Validação | aberto |
| 8 | Chave de cache ignora a ordenação | Média | Cache | ✅ `#pageable` na chave |
| 9 | Cache não invalida entre entidades relacionadas | Média | Cache | aberto |
| 10 | NPE em `endereco` ou `sexo` nulos | Média | Mapper | aberto |
| 11 | Ausência de `@Transactional` nos services | Baixa | Consistência | parcial — só em `AuthService` |
| 12 | `especie` e `porte` como texto livre | Baixa | Modelo | aberto |
| 13 | Único teste, que depende do Oracle | Baixa | Testes | ✅ 27 testes, perfil fixo em `dev` |
| 14 | README desatualizado na seção de estrutura | Baixa | Documentação | ✅ corrigido |
| 15 | Inconsistências internas de padrão | Baixa | Código | aberto |

**Novo — introduzido e corrigido na Sprint 3.** A chave de cache das listagens passou
a precisar do `tutorId`: sem ele, a listagem de um tutor seria servida a outro. Está
em [08-seguranca.md](08-seguranca.md#ownership) e coberta por
`OwnershipTest.cacheNaoVazaEntreTutores`.

Restam **8 itens abertos**, todos de severidade média ou baixa.

---

## 1. `REEMBOLSADO` vs `ESTORNADO`

**Severidade: alta**

O enum [`StatusPagamento`](../src/main/java/br/com/fiap/clyvovet/model/StatusPagamento.java)
declara `REEMBOLSADO`. O check constraint em
[`db-oracle.sql`](../src/main/resources/db/db-oracle.sql) aceita `ESTORNADO`:

```sql
CONSTRAINT chk_status_pagamento CHECK (status_pagamento IN ('PENDENTE','PAGO','CANCELADO','ESTORNADO'))
```

**Efeito:** `POST /pagamentos` com `"statusPagamento": "REEMBOLSADO"` passa pela
validação, chega ao INSERT e estoura `ORA-02290: check constraint violated` — que hoje
vira 500. O valor é impossível de gravar no perfil `oracle`.

**Correção:** alinhar os dois lados. Renomear o valor do enum para `ESTORNADO` é a
opção de menor impacto, já que ninguém consegue ter gravado `REEMBOLSADO` no banco.
Se preferir manter o nome no Java, altere o constraint e o README.

---

## 2. Credenciais do Oracle versionadas

**Severidade: alta**

[`application-oracle.properties`](../src/main/resources/application-oracle.properties)
tem usuário e senha em texto puro, e esse é o perfil ativo por padrão:

```properties
spring.datasource.username=rm563065
spring.datasource.password=191298
```

O [`.gitignore`](../.gitignore) protege `application-prod.properties`, mas esse arquivo
não existe — a proteção não cobre o perfil que está em uso.

**Efeito:** as credenciais estão no histórico do Git de um repositório público. Como
são de conta de aula da FIAP, o risco é limitado, mas o padrão é ruim e a senha
provavelmente é reutilizada pelo aluno em outros contextos.

**Correção:**

```properties
spring.datasource.username=${DB_USERNAME:}
spring.datasource.password=${DB_PASSWORD:}
```

e passar por variável de ambiente. Trocar a senha no Oracle, já que ela permanece no
histórico mesmo após a remoção do arquivo.

---

## 3. Unicidade só existe no banco

**Severidade: média**

O DDL declara uniques que a aplicação desconhece:

| Tabela | Coluna única | Validação na API? |
|---|---|---|
| `tutor` | `cpf` | não |
| `tutor` | `email` | não |
| `clinica` | `cnpj` | não |
| `veterinario` | `cpf` | não |
| `veterinario` | `crmv` | não |

**Efeito:** cadastrar um CPF já existente passa pelo Bean Validation, chega ao INSERT e
estoura `ORA-00001: unique constraint violated`. O `GlobalExceptionHandler` não trata
`DataIntegrityViolationException`, então o cliente recebe **500** em vez de 409 ou 400
com mensagem legível.

**Correção:** duas frentes, complementares.

Adicionar checagem prévia no service:

```java
if (tutorRepository.existsByCpf(request.getCpf())) {
    throw new RegraDeNegocioException("Já existe tutor com o CPF informado");
}
```

E, como rede de segurança contra corrida, tratar a exceção no handler global:

```java
@ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<ErroValidacao> handleConflito(DataIntegrityViolationException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErroValidacao("registro", "Registro duplicado ou em uso"));
}
```

---

## 4. Exclusão com dependentes

**Severidade: média**

`DELETE /tutores/{id}` só verifica se o tutor existe. Se ele tiver animais vinculados,
o `deleteById` estoura violação de FK.

Aplica-se a: Tutor→Animal, Clinica→Veterinario/EventoClinico,
Animal/Veterinario/Clinica→EventoClinico, EventoClinico→Pagamento.

**Efeito:** 500 em vez de uma resposta clara.

**Correção:** verificar dependentes antes de apagar e devolver 409 com mensagem, ou
tratar `DataIntegrityViolationException` como no item 3. Cascata automática não é
recomendada aqui — apagar um tutor não deveria apagar o histórico clínico do pet.

---

## 5. Perfil `h2` não roda fora do Docker

**Severidade: média**

O [README](../README.md) instrui usar `spring.profiles.active=h2` para desenvolvimento
local sem Oracle. Mas o perfil aponta para um host que só existe na rede do compose:

```properties
spring.datasource.url=jdbc:h2:tcp://clyvovet-db:1521/clyvovet
```

**Efeito:** quem segue o README recebe erro de conexão. O perfil que de fato serve para
local é o `dev` (H2 em memória), que o README não menciona.

**Correção:** atualizar o README para indicar `dev` no desenvolvimento local e
documentar `h2` como perfil de container. Alternativamente, renomear os perfis para
`local` e `docker`, que descrevem melhor o uso.

---

## 6. `@Size` rejeita o formato usado no seed

**Severidade: média**

Dois campos têm limites mais estreitos que os dados reais:

| Campo | Validação na API | Formato no seed | Coluna |
|---|---|---|---|
| `crmv` | 4–6 caracteres | `CRMV-SP 14320` (13) | `VARCHAR2(30)` |
| `telefone` | 10–11 caracteres | `(11) 99001-0001` (15) | `VARCHAR2(20)` |

**Efeito:** os registros do seed são válidos no banco mas **não podem ser recriados
pela API**. Um cliente que leia um veterinário e tente reenviá-lo num `PUT` recebe 400.

**Correção:** decidir o formato canônico. Se for só dígitos, ampliar `crmv` para algo
como 4–20 e normalizar o seed. Se for com máscara, ampliar `telefone` para até 20 e
`crmv` para até 30. O importante é que API e seed concordem.

---

## 7. `dataPagamento` obrigatória impede registrar pendente

**Severidade: média**

[`PagamentoRequest`](../src/main/java/br/com/fiap/clyvovet/dto/pagamento/PagamentoRequest.java):

```java
@NotNull(message = "Data de pagamento é obrigatória")
@PastOrPresent(message = "Data de pagamento não pode ser futura")
private LocalDate dataPagamento;
```

O próprio seed grava pagamentos `PENDENTE` e `CANCELADO` com `data_pagamento` **nula** —
o que faz sentido: um pagamento pendente ainda não tem data.

**Efeito:** não é possível registrar um pagamento pendente pela API. O usuário é
forçado a inventar uma data.

**Correção:** tornar `dataPagamento` condicional ao status. O caminho mais simples é
remover o `@NotNull` e validar na regra de negócio:

```java
if (request.getStatusPagamento() == StatusPagamento.PAGO && request.getDataPagamento() == null) {
    throw new RegraDeNegocioException("Pagamento com status PAGO exige data de pagamento");
}
```

O `@PastOrPresent` pode ficar — ele já ignora valores nulos.

---

## 8. Chave de cache ignora a ordenação

**Severidade: média**

Todas as chaves `@Cacheable` usam apenas os filtros e a paginação:

```java
key = "#nome + '-' + #especie + '-' + #pageable.pageNumber + '-' + #pageable.pageSize"
```

**Efeito:** `GET /animais?sort=nome,asc` e `GET /animais?sort=nome,desc` colidem na
mesma chave. A segunda chamada devolve o resultado cacheado da primeira, com a ordem
errada. Vale para os 6 recursos.

**Correção:** incluir o sort na chave:

```java
key = "#nome + '-' + #especie + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort"
```

---

## 9. Cache não invalida entre entidades relacionadas

**Severidade: média**

`@CacheEvict` é sempre escopado à própria entidade. Mas os Responses carregam campos
desnormalizados da entidade relacionada:

| Cache | Campo desnormalizado | Invalidado quando a origem muda? |
|---|---|---|
| `animais` | `tutorNome` | não |
| `veterinarios` | `clinicaNome` | não |
| `eventos` | `veterinarioNome`, `animalNome`, `clinicaNome` | não |

**Efeito:** renomear um tutor deixa `GET /animais` devolvendo o nome antigo até que
alguma escrita em Animal limpe o cache.

**Correção:** listar os caches afetados na anotação de quem é referenciado:

```java
@CacheEvict(value = {"tutores", "animais"}, allEntries = true)
public TutorResponse atualizar(UUID id, TutorRequest request) { ... }
```

Para `EventoClinico`, os writes de Animal, Veterinario e Clinica deveriam também
evictar `eventos`, e os de EventoClinico deveriam evictar `pagamentos` se este passar a
expor dados do evento.

---

## 10. NPE em `endereco` ou `sexo` nulos

**Severidade: média**

Os mappers de Tutor, Clinica e Veterinario chamam
`enderecoMapper.enderecoToResponse(entidade.getEndereco())` sem verificar nulo. Quando
**todas** as colunas do `@Embeddable` estão nulas, o Hibernate devolve `null` no campo
`endereco` — e o mapper estoura `NullPointerException`.

[`TutorMapper`](../src/main/java/br/com/fiap/clyvovet/mapper/TutorMapper.java) tem um
segundo caso:

```java
tutor.getSexo().toString()   // NPE se genero estiver nulo no banco
```

**Efeito:** 500 ao listar ou buscar um registro cujo endereço ou gênero venha nulo do
banco. Não acontece com dados criados pela API (os campos são `@NotNull`), mas
acontece com registros inseridos direto no SQL Developer.

**Correção:** aplicar o mesmo null-guard já usado para as associações:

```java
EnderecoResponse endereco = tutor.getEndereco() != null
        ? enderecoMapper.enderecoToResponse(tutor.getEndereco())
        : null;
String sexo = tutor.getSexo() != null ? tutor.getSexo().name() : null;
```

---

## 11. Ausência de `@Transactional`

**Severidade: baixa**

Nenhum service declara `@Transactional`. Cada `save`/`delete` roda na transação
implícita aberta pelo próprio Spring Data, o que hoje basta porque todas as operações
persistem uma única entidade.

**Efeito:** nenhum, no estado atual. Vira problema assim que uma operação precisar
gravar duas entidades atomicamente — por exemplo, criar um evento clínico já com o
pagamento associado.

**Correção:** anotar as escritas com `@Transactional` e as leituras com
`@Transactional(readOnly = true)`. O `readOnly` também sinaliza otimização ao
Hibernate, que dispensa dirty checking.

---

## 12. `especie` e `porte` como texto livre

**Severidade: baixa**

Ambos são `String` na entidade [`Animal`](../src/main/java/br/com/fiap/clyvovet/model/Animal.java),
validados apenas por tamanho (3–100).

O banco restringe `porte` a `PEQUENO`/`MEDIO`/`GRANDE` por check constraint — mas a
aplicação aceita `"grande"` minúsculo, que o Oracle rejeita.

Para `especie` não há constraint nenhuma, e as fontes divergem: o seed grava
`'CAO'`/`'GATO'`, o README exemplifica `"CACHORRO"`. Ambos convivem no banco, o que
quebra o filtro `?especie=CAO`, que não encontra os registros gravados como
`CACHORRO`.

**Correção:** transformar os dois em enums, à semelhança de `TipoEvento`. Isso alinha
API, banco e documentação de uma vez e faz o filtro passar a ser confiável.

---

## 13. Único teste, que depende do Oracle

**Severidade: baixa**

[`ClyvovetApplicationTests`](../src/test/java/br/com/fiap/clyvovet/ClyvovetApplicationTests.java)
tem só o `contextLoads()` do Initializr. Sendo `@SpringBootTest`, ele sobe o contexto
inteiro — e como o perfil default é `oracle`, **falha sem conectividade com a FIAP**.

Por isso o [Dockerfile](../Dockerfile) usa `-DskipTests`: sem isso o build da imagem
não passaria.

**Correção mínima:** fixar o perfil de teste criando
`src/test/resources/application.properties` com `spring.profiles.active=dev`. O teste
passa a rodar isolado, com H2 em memória, e o `-DskipTests` deixa de ser necessário.

Depois disso, cobrir services e mappers com testes unitários — ver
[06-guia-de-desenvolvimento.md](06-guia-de-desenvolvimento.md#testes).

---

## 14. README desatualizado

**Severidade: baixa**

A seção "Estrutura do Projeto" do [README](../README.md) cita arquivos que não existem
com esses nomes:

| README diz | Realidade |
|---|---|
| `documentos/DiagramaClasses_CLYVOVET.pdf` | `documentos/Diagrama_De_Classes.pdf` |
| `documentos/clyvovet_insomnia.json` | não existe |

A seção "Testando os Endpoints" também afirma que a coleção do Insomnia está em
`documentos/`. O commit `2b2108d` menciona tê-la adicionado, mas o arquivo não está na
árvore atual.

**Correção:** corrigir o nome do PDF e ou recommitar a coleção do Insomnia, ou remover
as duas menções a ela.

---

## 15. Inconsistências internas de padrão

**Severidade: baixa**

Divergências pequenas em relação ao padrão que o resto do projeto segue:

| Onde | Padrão do projeto | O que está diferente |
|---|---|---|
| `PagamentoService` | método `salvar` | chama-se `criar` |
| `PagamentoResponse` | `record` | classe `@Data` com setters |
| `PagamentoRequest` | `@NoArgsConstructor @AllArgsConstructor @Getter` | `@Data` |
| `TutorResponse.sexo` | tipo do enum | `String` (via `.toString()`) |
| `EnderecoRequest` vs `EnderecoResponse` | mesma ordem de campos | `complemento` em posições diferentes |
| `EventoClinico.hora` | tipo temporal | `String` sem validação de formato |
| `AnimalRequest.observacao` | `@Size` como nos demais textos | sem limite, mas coluna é `VARCHAR2(1000)` |

Nenhuma quebra nada. `PagamentoRequest`/`Response` com `@Data` expõem setters
desnecessários, e `TutorResponse.sexo` como `String` faz o mesmo campo ter tipos
diferentes em `/tutores` e `/veterinarios` no contrato da API — o que é visível para o
cliente. `AnimalRequest.observacao` sem `@Size(max = 1000)` deixa passar textos que o
Oracle vai truncar ou rejeitar.

---

## Melhorias sugeridas (não são defeitos)

Itens fora do escopo do Challenge, registrados para quem for evoluir o projeto:

| Tema | Sugestão |
|---|---|
| Segurança | Spring Security com JWT; hoje a API é totalmente aberta |
| Cache | Trocar o `ConcurrentMapCacheManager` por Caffeine com TTL, ou Redis se houver mais de uma instância |
| Observabilidade | `spring-boot-starter-actuator` para health check e métricas |
| Migrations | Flyway ou Liquibase no lugar do SQL manual + `ddl-auto` |
| Consultas | Endpoint de histórico clínico por animal e de totalizadores financeiros por período |
| Auditoria | `@CreatedDate`/`@LastModifiedDate` via `@EnableJpaAuditing` |
| Deploy | Reverse proxy com HTTPS na VM Azure; a porta 80 é aberta mas ninguém escuta nela |
| CI | GitHub Actions rodando `mvn verify` a cada push |
