# `exception` — os erros e o status que cada um vira

`src/main/java/br/com/fiap/clyvovet/exception` · 5 classes

Três exceções de domínio, um enum de apoio e o tradutor que transforma qualquer
uma delas — e as do framework também — em resposta HTTP.

É por causa deste pacote que **não existe `try/catch` em controller nenhum**:
cada controller cuida do caminho feliz, e o desvio vira status e corpo num lugar
só.

---

## Os arquivos

| Arquivo | O que é |
|---|---|
| `GlobalExceptionHandler.java` | O `@RestControllerAdvice` que traduz exceção em resposta |
| `RecursoNaoEncontradoException.java` | Recurso inexistente → **404** |
| `RegraDeNegocioException.java` | Violação de regra de negócio → **409** |
| `LimiteDeAcessoExcedidoException.java` | Teto de leitura do prontuário → **429** |
| `Recurso.java` | Enum com a frase de "não encontrado" de cada recurso |

---

## A tabela de tradução

| Exceção | Status | Quando |
|---|---:|---|
| `MethodArgumentNotValidException` | **400** | Bean Validation reprovou o corpo |
| `BadCredentialsException` | **401** | Login falhou |
| `RecursoNaoEncontradoException` | **404** | Id que não existe |
| `EntityNotFoundException` | **404** | Rede de segurança para o que vier do próprio JPA |
| `RegraDeNegocioException` | **409** | O estado atual não admite a operação |
| `DataIntegrityViolationException` | **409** | Constraint do banco (CPF, CNPJ, CRMV, e-mail duplicados) |
| `LimiteDeAcessoExcedidoException` | **429** | Teto de animais distintos por dia |

O `RespostaErroSeguranca`, que fica no pacote [`security`](security.md), cobre
os outros dois: **401** para não autenticado e **403** para autenticado sem
permissão.

Toda resposta sai no mesmo formato:

```json
{ "campo": "statusEvento", "mensagem": "Este atendimento já foi concluído" }
```

---

## Por que 429, e não 403

`LimiteDeAcessoExcedidoException` poderia ter virado 403 sem que nada quebrasse.
Não virou, e a distinção muda o que quem recebe faz a seguir:

> **403** diria "você não pode ver isto", o que é **falso** — o veterinário
> podia, e até agora vinha podendo. O que aconteceu foi um limite de **volume**.

Um front que recebe 403 esconde o botão; um que recebe 429 mostra "aguarde".

E ela é **distinta do rate limit por IP**. Aquele protege a infraestrutura contra
rajada e conta requisições; este protege os pacientes contra coleta e conta
**animais distintos por profissional por dia**. Um veterinário que abre o mesmo
prontuário quarenta vezes durante uma cirurgia não chega perto do teto; um que
consulta duzentos animais diferentes numa tarde chega, e não está atendendo
nenhum deles.

## Por que a exceção de domínio, e não a do JPA

`RecursoNaoEncontradoException` existe em vez de deixar a
`EntityNotFoundException` do JPA subir pelos services. Quem decide que "buscar
por um id inexistente é um erro" é a **regra da aplicação**, não a camada de
persistência — manter a exceção do JPA subindo amarraria o domínio a uma escolha
de infraestrutura.

O handler de `EntityNotFoundException` continua lá como rede de segurança, para
o que vier do próprio Hibernate (o acesso a um proxy de entidade já removida, por
exemplo). Sem ele, viraria 500.

## Por que a duplicata não ecoa o SQL

A unicidade de CPF, CNPJ, CRMV e e-mail existe apenas como constraint no banco.
Sem o handler de `DataIntegrityViolationException`, uma duplicata sobe como 500
carregando o SQL e o **nome da constraint** na resposta — o que expõe a estrutura
interna. Por isso a causa vai para o log, e não para o cliente.

---

## `Recurso` — as doze frases

```java
public enum Recurso { ALERTA_CLINICO, ANIMAL, AUTORIZACAO, BLOQUEIO, CLINICA,
                      EVENTO_CLINICO, PAGAMENTO, DISPONIBILIDADE, SERVICO,
                      TUTOR, USUARIO, VETERINARIO }
```

A mensagem de "não encontrado" estava repetida em cerca de vinte pontos, cada um
concatenando o próprio texto. Além da duplicação, a concordância variava ("não
encontrado" × "não encontrada") e nada garantia que continuasse coerente. Aqui
cada recurso declara a própria frase uma única vez, e o
[`RepositorioBase`](repository.md) a usa.

A exceção tem ainda duas formas além da padrão:

- **mensagem própria**, para quando a busca não foi por id. A frase padrão
  termina em "com ID: `<uuid>`", o que seria enganoso ao procurar por microchip:
  o número informado não é um id, e ecoá-lo como se fosse mandaria quem lê atrás
  da chave errada.
- **fornecedor**, para `Optional.orElseThrow`:

```java
findById(id).orElseThrow(naoEncontrado(Recurso.ANIMAL, id));
```

---

## Onde continuar

| Assunto | Documento |
|---|---|
| O formato `ErroValidacao` | [dto.md](dto.md) |
| Os erros 401 e 403 | [security.md](security.md) |
| Quem lança cada exceção | [service.md](service.md) |
| Os códigos por endpoint | [../03-api-rest.md](../03-api-rest.md) |
