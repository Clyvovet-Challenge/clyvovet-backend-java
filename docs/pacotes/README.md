# Documentação por pacote

Um documento por pacote de `src/main/java/br/com/fiap/clyvovet`, explicando
**arquivo por arquivo** o que cada classe faz e por que ela existe separada das
outras.

Enquanto [01-arquitetura.md](../01-arquitetura.md) descreve as camadas em
abstrato, esta pasta é o mapa concreto: se você abriu um arquivo e não sabe o
que ele faz ali, comece pelo documento do pacote dele.

---

## Índice

| Pacote | Arquivos | O que vive ali |
|---|---:|---|
| [controller.md](controller.md) | 14 + 2 | A superfície HTTP. Recebe requisição, chama o service, devolve status |
| [service.md](service.md) | 18 | As regras de negócio, as transações e o cache |
| [repository.md](repository.md) | 14 | O acesso ao banco: interfaces Spring Data e as consultas JPQL |
| [mapper.md](mapper.md) | 12 | A cópia campo a campo entre DTO e entidade |
| [model.md](model.md) | 27 | As entidades JPA e os enums do domínio |
| [dto.md](dto.md) | 56 | Os contratos de entrada e saída da API, com as validações |
| [security.md](security.md) | 10 | JWT, ownership, rate limit, bloqueio de conta |
| [config.md](config.md) | 5 | Spring Security, cache, prefixo de versão, Swagger, seed de dev |
| [exception.md](exception.md) | 5 | As exceções de domínio e a tradução delas para status HTTP |

São **164 arquivos** em `src/main/java`, mais `ClyvovetApplication.java` na
raiz do pacote — a classe de boot, com `@SpringBootApplication` e
`@EnableCaching`, e nada além disso.

---

## O caminho de uma requisição

Todo pedido atravessa as mesmas camadas, sempre na mesma ordem:

```
HTTP
 │
 ├─ RateLimitFilter ............ o IP já pediu demais? → 429
 ├─ JwtAuthenticationFilter .... quem é você? → popula o SecurityContext
 ├─ @PreAuthorize .............. você pode ESTE registro? → 403
 │
 ├─ Controller ................. desempacota o DTO, devolve o status
 │    │
 │    └─ Service ............... a regra de negócio, a transação, o cache
 │         │
 │         ├─ Mapper ........... DTO → entidade → DTO
 │         └─ Repository ....... a consulta
 │              │
 │              └─ Model ....... a entidade JPA
 │
 └─ GlobalExceptionHandler ..... qualquer exceção vira JSON com status
```

Cada seta desta pilha é uma decisão que mora em **um** lugar só. Foi assim que o
projeto foi sendo arrumado: sempre que a mesma pergunta era respondida em dois
arquivos, um dos dois ficava desatualizado — e o modo como isso apareceu na
prática está registrado no documento de cada pacote.

## As três regras que valem em todo lugar

**1. Cada camada só conhece a de baixo.** O controller não sabe de JPA, o
service não sabe de HTTP, a entidade não sabe de JSON. Quando um `Controller`
precisa de um dado que só o repositório tem, quem busca é o service.

**2. Regra de negócio nunca fica no controller.** O controller escolhe o status
HTTP e mais nada. Se há um `if` decidindo se a operação é permitida, ele está no
service ou no `SegurancaService`.

**3. A entidade nunca sai pela API.** Toda resposta é um DTO. Isso não é
cerimônia: `Usuario` tem a senha, `Animal` tem o tutor inteiro, e serializar a
entidade direto expõe os dois — além de amarrar o contrato público ao mapeamento
do banco.

## Convenções de nome

| Sufixo | Onde | O que é |
|---|---|---|
| `...Controller` | `controller` | Um recurso ou um fluxo da API |
| `...Service` | `service` | Os casos de uso de um assunto |
| `...Repository` | `repository` | Acesso ao banco de uma entidade |
| `...Mapper` | `mapper` | Conversão entre DTO e entidade |
| `...Request` | `dto` | O que entra no POST e no PUT |
| `...PatchRequest` | `dto` | O que entra no PATCH — só os campos que mudam |
| `...Response` | `dto` | O que sai, sempre `record` |
| `...Exception` | `exception` | Erro de domínio, com status próprio |

O código é escrito em **português**, inclusive nomes de classe, método e enum. O
domínio é falado em português pelas pessoas que usam o sistema, e traduzir
"tutor" para "owner" só na camada de código criaria um dicionário para manter na
cabeça.

---

## Onde continuar

| Documento | Conteúdo |
|---|---|
| [../01-arquitetura.md](../01-arquitetura.md) | As camadas em abstrato, cache, tratamento de erros |
| [../02-modelo-de-dados.md](../02-modelo-de-dados.md) | O mapeamento objeto↔tabela e o DDL |
| [../03-api-rest.md](../03-api-rest.md) | Os contratos de cada endpoint |
| [../08-seguranca.md](../08-seguranca.md) | A matriz de autorização completa e as regras A/R/C/P |
