# Pendências e divergências

Levantamento feito a partir da leitura completa do código, do DDL e da documentação.
Nada aqui impede o build ou a execução — são descolamentos entre **código**,
**schema do banco** e **documentação**, além de lacunas conhecidas.

Cada item traz o efeito prático e uma sugestão de correção. A decisão sobre o que
corrigir é do time.

## Resumo

Os itens marcados ✅ foram resolvidos na Sprint 3 (Flyway + Spring Security) ou na
revisão de código que veio depois dela — os itens 16 a 18 foram descobertos nessa
revisão e já entraram corrigidos e cobertos por teste.

| # | Item | Severidade | Área | Situação |
|---|---|---|---|---|
| 1 | `REEMBOLSADO` vs `ESTORNADO` no check constraint | Alta | Banco | ✅ migration `V4` |
| 2 | Credenciais do Oracle versionadas em texto puro | **Alta** | Segurança | parcial — código e docs limpos; **falta trocar a senha** |
| 3 | Unicidade só existe no banco → 500 em duplicidade | Média | Erros | ✅ 409 no handler |
| 4 | Exclusão com dependentes → 500 | Média | Erros | ✅ 409 no handler, coberto por `IntegridadeReferencialTest` |
| 5 | Perfil `h2` não roda fora do Docker | Média | Configuração | ✅ documentado; use `dev` |
| 6 | `@Size` em `crmv` e `telefone` rejeita o formato do seed | Média | Validação | ✅ `crmv` alinhado à coluna (30) |
| 7 | `dataPagamento` obrigatória impede registrar pendente | Média | Validação | aberto |
| 8 | Chave de cache ignora a ordenação | Média | Cache | ✅ `#pageable` na chave |
| 9 | Cache não invalida entre entidades relacionadas | Média | Cache | aberto |
| 10 | NPE em `endereco` ou `sexo` nulos | Média | Mapper | ✅ null-guard no `EnderecoMapper`; `sexo` virou enum |
| 11 | Ausência de `@Transactional` nos services | Baixa | Consistência | ✅ escritas e leituras anotadas |
| 12 | `especie` e `porte` como texto livre | Baixa | Modelo | aberto |
| 13 | Único teste, que depende do Oracle | Baixa | Testes | ✅ 98 testes, perfil fixo em `dev` |
| 14 | README desatualizado na seção de estrutura | Baixa | Documentação | ✅ corrigido |
| 15 | Inconsistências internas de padrão | Baixa | Código | ✅ parcial — resta `PagamentoRequest` com `@Data` |
| 16 | Filtros por texto nunca casavam (`LIKE ... ESCAPE ''`) | **Alta** | Consultas | ✅ `ESCAPE '\'` explícito — falta confirmar no Oracle |
| 17 | `tutorId` do corpo não passava por checagem de dono | **Alta** | Segurança | ✅ `@PreAuthorize` no POST e no PUT |
| 18 | Validação mais permissiva que a coluna → 500 | Média | Validação | ✅ limites alinhados ao schema |
| 19 | Perfil `mysql` nunca executado contra um MySQL real | Média | Configuração | aberto |

**Novo — introduzido e corrigido na Sprint 3.** A chave de cache das listagens passou
a precisar do `tutorId`: sem ele, a listagem de um tutor seria servida a outro. Está
em [08-seguranca.md](08-seguranca.md#ownership) e coberta por
`OwnershipTest.cacheNaoVazaEntreTutores`.

Restam **4 itens abertos** (7, 9, 12 e 19) e duas pendências parciais (2 e 15).

> ⚠️ O item **2 é o único de severidade alta ainda em aberto**: o código e a
> documentação já não têm a senha, mas ela continua no histórico do Git de um
> repositório público. Só trocar a senha no portal da FIAP fecha isso.

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
spring.datasource.username=<RM do aluno>
spring.datasource.password=<senha em texto puro>
```

O [`.gitignore`](../.gitignore) protege `application-prod.properties`, mas esse arquivo
não existe — a proteção não cobre o perfil que está em uso.

**Efeito:** as credenciais estão no histórico do Git de um repositório público. Como
são de conta de aula da FIAP, o risco é limitado, mas o padrão é ruim e a senha
provavelmente é reutilizada pelo aluno em outros contextos.

### O que já foi feito

O perfil passou a ler do ambiente, e a aplicação não sobe sem as variáveis:

```properties
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

A senha também saiu deste documento e do [04-configuracao.md](04-configuracao.md),
onde ainda aparecia em texto puro dentro de um bloco de exemplo.

### O que continua aberto

**A senha permanece no histórico do Git.** Ela aparece em 3 commits, tendo passado
por 5 arquivos ao longo do tempo:

| Arquivo | Situação hoje |
|---|---|
| `application-oracle.properties` | limpo — lê do ambiente |
| `application.properties` | limpo |
| `application-prod.properties` | não existe mais |
| `docs/04-configuracao.md` | limpo |
| `docs/07-pendencias-e-divergencias.md` | limpo (este arquivo) |

Qualquer pessoa com acesso ao repositório público consegue recuperá-la com
`git log -S`. Reescrever o histórico (`git filter-repo`, BFG) exigiria force push e
invalidaria os clones de todo mundo — desproporcional para uma conta de aula.

**A correção real é trocar a senha no portal da FIAP.** Enquanto isso não acontecer,
o item continua aberto, por mais limpo que o código esteja.

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

**✅ Resolvido pelo caminho do item 3.** O handler devolve 409 com mensagem genérica, e
`IntegridadeReferencialTest` fixa o comportamento nos três elos (tutor→animal,
animal→evento, clínica→veterinário), inclusive verificando que o nome da constraint
não vaza na resposta.

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

**✅ Resolvido para `crmv`**, agora 4–30, igual à coluna;
`CadastroCrudTest.aceitaCrmvNoFormatoReal` garante que o formato do conselho passa.
O `telefone` não precisou de mudança: o seed grava só dígitos (`11990010001`), dentro
do 10–11 já exigido — a máscara citada acima não existe nos dados.

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

**✅ Resolvido.** `EnderecoMapper.toResponse` e `toEntity` devolvem `null` para entrada
nula, e `TutorResponse.sexo` passou a ser o enum — sem `toString()`, não há o que
estourar.

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

**✅ Aplicado.** Os seis services de CRUD e o `UsuarioService` levam
`@Transactional(readOnly = true)` na classe e `@Transactional` nas escritas.
`AuthService.login` segue **sem** transação de propósito — ver a nota na própria
classe e o item de bloqueio de conta em [08-seguranca](08-seguranca.md).

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

**✅ Resolvido em parte.** `PagamentoService.salvar` virou `criar`, `PagamentoResponse`
virou `record` e `TutorResponse.sexo` passou a ser o enum `Sexo` (o JSON não muda — o
Jackson já serializava o enum pelo nome). Os mappers uniformizaram
`toEntity`/`atualizar`/`toResponse`. Continuam abertos: `EventoClinico.hora` como
`String` (agora ao menos com `@NotBlank`), `PagamentoRequest` com `@Data`,
`AnimalRequest.observacao` sem `@Size` e a ordem de campos de `Endereco`.

---

## 16. Filtros por texto nunca casavam

**Severidade: alta** — descoberto na revisão de código pós-Sprint 3.

Toda listagem filtrada por texto devolvia lista vazia: `GET /veterinarios?nome=Camila`
retornava `totalElements: 0` com a Camila cadastrada. Só o filtro vazio funcionava,
por cair no ramo `:nome IS NULL`.

A causa está no SQL que o Hibernate gera para o `LIKE` do JPQL:

```sql
lower(v1_0.nome) like lower(('%'||?||'%')) escape ''
```

O `escape ''` é emitido pelo Hibernate. Sob a semântica do Oracle — que o H2 imita
com `MODE=Oracle`, usado nos perfis `dev` e `h2` — **string vazia é NULL**. O predicado
vira `ESCAPE NULL`, avalia como desconhecido e nunca é verdadeiro. O efeito não aparecia
nos testes porque nenhum deles exercitava os filtros: as listagens são testadas com o
recorte por tutor, em que o parâmetro de texto vai nulo.

**Correção aplicada:** declarar o escape explicitamente nas cinco queries que usam
`LIKE`, em `Animal`, `Clinica`, `Tutor`, `Veterinario` e `EventoClinico`:

```java
"(:nome IS NULL OR LOWER(v.nome) LIKE LOWER(CONCAT('%', :nome, '%')) ESCAPE '\\')"
```

Verificado no perfil `dev`: `?nome=Camila`, `?nome=camila` (a busca é case-insensitive),
`?especialidade=Cardio`, `?cidade=Sao` e `?animalNome=Bolinha` passaram a retornar os
registros esperados. Cobertura de regressão em `FiltrosDeBuscaTest` — sem o `ESCAPE`,
três testes de lá falham.

### ⏳ Falta confirmar no Oracle

O H2 com `MODE=Oracle` **imita** a semântica de string vazia, mas o alvo de entrega é o
Oracle da FIAP, e lá isso ainda não foi conferido. Há duas saídas possíveis, e ambas
confirmam o diagnóstico: ou o `LIKE` não casa nada (mesmo comportamento do H2), ou o
banco recusa a consulta com `ORA-01425: escape character must be string of length 1`.

[`EscapeNoOracleTest`](../src/test/java/br/com/fiap/clyvovet/crud/EscapeNoOracleTest.java)
existe para isso. Roda por JDBC puro sobre `dual` — não lê nem grava nenhuma tabela do
projeto, não sobe o contexto e portanto não dispara Flyway nem `ddl-auto=validate` num
banco compartilhado. Fica **pulado** enquanto `DB_USERNAME` não estiver no ambiente,
então não interfere no `mvn test` de ninguém:

```bash
DB_USERNAME=seu_rm DB_PASSWORD=sua_senha ./mvnw test -Dtest=EscapeNoOracleTest
```

Quatro checagens: `ESCAPE '\'` casa; `ESCAPE ''` não casa (a causa); sem cláusula
`ESCAPE` casa (isola a causa no escape vazio, e não no `LIKE`); e o escape continua
escapando — `%` literal não vira curinga.

**Nota sobre rodar a suíte inteira no perfil `oracle`:** não é o caminho para esta
verificação. Ela grava cadastros de teste num banco compartilhado e depende do
`DevDataSeeder`, que só existe nos perfis `dev` e `h2` — sem os usuários dele não há
login, e quase tudo falharia com 401 por um motivo alheio ao `ESCAPE`.

---

## 17. `tutorId` do corpo não passava por checagem de dono

**Severidade: alta** — descoberto na revisão de código pós-Sprint 3.

O ownership de animal era verificado pelo id da URL, e só por ele:

```java
@PreAuthorize("@seguranca.podeAcessarAnimal(#id)")
```

Só que quem define o dono do pet é o campo `tutorId`, que vem no **corpo**. Nenhuma
regra olhava esse campo, e `POST /animais` não tinha regra nenhuma além de "estar
autenticado". Um tutor logado, portanto:

- cadastrava pet no nome de outro tutor — o dono legítimo passava a ver na própria
  listagem um animal que nunca cadastrou;
- transferia o próprio pet para outro tutor num `PUT`, perdendo o acesso a ele e
  empurrando o registro para a conta alheia.

**Correção aplicada:** a escrita passou a fazer as duas perguntas, e não só a primeira.

```java
@PostMapping
@PreAuthorize("@seguranca.podeAcessarTutor(#request.tutorId)")

@PutMapping("/{id}")
@PreAuthorize("@seguranca.podeAcessarAnimal(#id) and @seguranca.podeAcessarTutor(#request.tutorId)")
```

`podeAcessarTutor` já liberava `VETERINARIO` e `ADMIN`, então a clínica continua
cadastrando pet para qualquer tutor. Coberto por
`OwnershipTest.tutorNaoCadastraPetParaTerceiro`,
`OwnershipTest.tutorNaoTransfereOProprioPet` e, do lado positivo,
`AtendimentoCrudTest.tutorCadastraOProprioPet`.

Os outros recursos não tinham a mesma brecha: evento e pagamento só podem ser
escritos por `VETERINARIO`/`ADMIN`, pela regra de rota.

---

## 18. Validação mais permissiva que a coluna

**Severidade: média** — descoberto na revisão de código pós-Sprint 3.

Mesma família do item 6, na direção contrária: onde o DTO aceitava mais do que a
coluna comporta, o valor passava pelo Bean Validation e só falhava no INSERT. O
cliente recebia erro de servidor por um dado que ele mesmo poderia corrigir.

| Campo | Coluna | Validação antes | Agora |
|---|---|---|---|
| `EventoClinico.hora` | `VARCHAR2(5)` | `@NotNull` (aceitava `""` e `"14:30:00"`) | `@NotBlank` + `@Pattern` HH:mm |
| `Animal.observacao` | `VARCHAR2(1000)` | sem limite | `@Size(max = 1000)` |
| `Endereco.numero` | `VARCHAR2(10)` | sem limite | `@Size(max = 10)` |

**Correção aplicada:** os três passam a responder 400 com o campo indicado, em vez de
500. `ValidacaoDeEntradaTest` cobre o limite exato (1000 caracteres passa, 1001 não) e
os horários de borda (`00:00` e `23:59` passam; `25:00`, `9:00`, `14:30:00` e vazio
não).

---

## 19. Perfil `mysql` nunca executado contra um MySQL real

**Severidade:** Média · **Área:** Configuração · **Situação:** aberto

Em agosto de 2026 a infraestrutura mudou de rumo: o Oracle da FIAP passa a ser
banco de testes e o deploy vai para **Azure Container Apps + Azure Database for
MySQL**. Ainda em discussão com o time de devops.

O que já está no repositório, feito enquanto a decisão amadurecia:

- migrations separadas por banco em `db/migration/oracle/` e `db/migration/mysql/`;
- perfil `mysql` em `application-mysql.properties`;
- `mysql-connector-j` e `flyway-mysql` no `pom.xml`;
- `MigrationsMySqlTest`, que roda o conjunto `mysql/` num H2 em `MODE=MySQL`.

**O que ainda não foi verificado.** Nenhuma linha disso tocou um MySQL de verdade —
não havia Docker disponível na máquina onde foi escrito. H2 em `MODE=MySQL` valida
sintaxe e semântica principal, mas não o comportamento de tipos do servidor real. Os
pontos que só um MySQL responde:

| O que conferir | Por que pode falhar |
|---|---|
| `ddl-auto=validate` sobe | O validate compara o tipo JDBC de cada coluna com o do atributo. `TINYINT` para `ativo` e `INT` para `tentativas_falhas` foram escolhidos com isso em mente, mas não confirmados |
| `DROP CONSTRAINT` na V4 | Existe a partir do MySQL 8.0.19. O Flexible Server é 8.0.21+, então deve passar — confirmar a versão exata do servidor provisionado |
| Os `CHECK` são aplicados | Só valem do MySQL 8.0.16 em diante. Abaixo disso o servidor os aceita **em silêncio** e não valida nada |
| UUID como `VARCHAR(36)` | Sem as duas linhas de `uuid_jdbc_type` no perfil, o Hibernate grava `BINARY(16)` e os ids não casam com o seed |

**Como fechar:** subir um MySQL 8 local (`docker run` ou Testcontainers), rodar a
aplicação com `SPRING_PROFILES_ACTIVE=mysql` e confirmar que o boot passa do
`validate`. Depois disso, trocar o `MigrationsMySqlTest` por um teste com
Testcontainers no CI, que é o único jeito de manter a garantia viva.

---

## Melhorias sugeridas (não são defeitos)

Itens fora do escopo do Challenge, registrados para quem for evoluir o projeto:

| Tema | Sugestão |
|---|---|
| Cache | Redis no lugar do Caffeine, se houver mais de uma instância — hoje o cache é por processo |
| Rate limit | Mesma observação: `bucket4j-redis` para o limite valer no conjunto, e não por réplica |
| Observabilidade | `spring-boot-starter-actuator` para health check e métricas |
| Consultas | Endpoint de histórico clínico por animal e de totalizadores financeiros por período |
| Auditoria | `@CreatedDate`/`@LastModifiedDate` via `@EnableJpaAuditing` |
| Deploy | A VM Azure está sendo substituída por Container Apps (item 19). Enquanto ela existir: reverse proxy com HTTPS — a porta 80 é aberta mas ninguém escuta nela |
| CI | GitHub Actions rodando `mvn verify` a cada push |
