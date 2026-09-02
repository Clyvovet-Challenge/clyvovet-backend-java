# `controller` — a superfície HTTP

`src/main/java/br/com/fiap/clyvovet/controller` · 14 classes + 2 em `hateoas/`

Esta camada faz três coisas e mais nada: **recebe** a requisição já
desserializada e validada, **chama** o service e **escolhe o status HTTP** da
resposta. Não há `try/catch` em controller nenhum — quem traduz exceção em
resposta é o [`GlobalExceptionHandler`](exception.md).

Também não há `/api/v1` em nenhum `@RequestMapping`. O prefixo é aplicado por
[`WebConfig`](config.md) a todo o pacote, de uma vez; o controller declara só o
próprio recurso (`/tutores`), e a versão da API vive numa constante só.

---

## Os dois tipos de controller

O pacote tem duas famílias, e a divisão é deliberada.

**Controllers de recurso** expõem as seis operações de CRUD sobre uma entidade:
`GET` na lista, `GET` por id, `POST`, `PUT`, `PATCH`, `DELETE`. São seis:
tutor, animal, clínica, veterinário, evento clínico e pagamento.

**Controllers de fluxo** expõem *ações* do domínio — marcar, cancelar, concluir,
confirmar, estornar. Cada uma tem regra própria e recusa própria.

A separação existe porque misturar as duas famílias faz a ação parecer mais um
verbo de CRUD, e é isso que abre o buraco: **nenhuma ação aceita o status no
corpo**. Com `statusEvento` editável por PATCH, um `{"statusEvento":"REALIZADO"}`
contornaria as vinte e uma regras do fluxo de retorno de uma vez.

```
EventoClinicoController   CRUD sobre a entidade evento
AgendamentoController     marcar · cancelar · minha agenda      (fluxo A)
RetornoController         concluir · retorno · faltas           (fluxo R)
HistoricoController       os três níveis de acesso              (fluxo C)
CobrancaController        confirmar · estornar · saldo          (fluxo P)
```

---

## Os arquivos

### Recursos (CRUD)

| Arquivo | Rota base | O que faz |
|---|---|---|
| `TutorController.java` | `/tutores` | CRUD de tutor. Listagem com filtro por `nome` e `cidade` |
| `AnimalController.java` | `/animais` | CRUD de animal, filtro por `nome` e `especie`. O `GET /{id}` e o `POST` devolvem **HATEOAS** |
| `ClinicaController.java` | `/clinicas` | CRUD de clínica. Leitura aberta a qualquer autenticado, escrita só para ADMIN |
| `VeterinarioController.java` | `/veterinarios` | CRUD de veterinário, filtro por `nome` e `especialidade` |
| `EventoClinicoController.java` | `/eventos-clinicos` | CRUD de atendimento. O `GET /{id}` devolve os **links das ações possíveis no estado atual** |
| `PagamentoController.java` | `/pagamentos` | CRUD de pagamento. No `POST`, quem é verificado é o dono do **atendimento citado no corpo** — a rota não tem id |

O `POST /pagamentos` merece a nota. O `@PreAuthorize` dos outros métodos olha o
`{id}` da rota; aqui não existe id, o dono está no corpo. Por isso a anotação é
`@seguranca.podeAcessarEvento(#request.eventoClinicoId)` — sem ela, um
veterinário lançava cobrança no atendimento de outra clínica.

### Fluxos

| Arquivo | O que faz |
|---|---|
| `AgendamentoController.java` | **Fluxo A.** `GET /agendamentos/vagas` (o que o calendário do front consome), `POST /agendamentos` (marcar), `POST /{id}/cancelar`, `GET /meus`. O *ownership* é verificado por **animal**, não por perfil: o tutor só marca para os próprios pets, e a recepção da clínica marca para qualquer um |
| `RetornoController.java` | **Fluxo R.** `POST /{id}/concluir` (única porta para `REALIZADO`), `POST /{id}/retorno`, `GET /retornos-vencidos`, `POST /marcar-faltas`. Vive sob `/eventos-clinicos` porque age sobre o evento, mas são ações, não edição |
| `HistoricoController.java` | **Fluxo C.** `GET /animais/resumo?microchip=` (nível 1), `GET /animais/{id}/historico` (nível 2), `POST /acesso-emergencial` (quebra de vidro), `GET /acessos` (a auditoria, que é **do tutor**), alertas e autorizações. As rotas não dizem o nível — ele é resolvido por quem pergunta, sobre qual animal, com qual consentimento |
| `CobrancaController.java` | **Fluxo P.** `POST /pagamentos/{id}/confirmar`, `POST /{id}/estornar`, `GET /eventos-clinicos/{id}/saldo`, `GET /pagamentos/inadimplencia`, `GET /tutores/{id}/extrato` |
| `AgendaController.java` | A grade do veterinário: `GET /veterinarios/{id}/disponibilidades` e o cadastro de faixas e bloqueios. A **leitura é aberta a qualquer autenticado de propósito** — o tutor precisa ver quando o profissional atende para escolher horário, e o que a grade expõe é disponibilidade profissional, não dado pessoal |
| `ServicoController.java` | O catálogo: `GET /clinicas/{id}/servicos` (aninhado na clínica porque serviço não existe fora de uma) e a manutenção pelo ADMIN. O `DELETE` **desativa**, não apaga |
| `AuthController.java` | `/auth`: login, refresh, logout, auto-cadastro, criação por ADMIN e `/me`. Conversa com **dois** services — as rotas se agrupam por URL, mas autenticar e cadastrar são responsabilidades diferentes, e a divisão está na camada de baixo |
| `AuditoriaController.java` | `GET /auditoria/excessos` e `/quebras-de-vidro`. Sem estas consultas os tetos de leitura só produziriam linhas de log |

### `hateoas/` — os links da resposta

| Arquivo | O que faz |
|---|---|
| `LinksDoEvento.java` | Monta os `_links` de um atendimento **condicionais ao estado**. Quem responde "esta ação é possível?" é o próprio [`StatusEvento`](model.md), o mesmo enum que os services consultam: link oferecido aqui e chamada aceita lá são, por construção, a mesma regra |
| `LinksDoAnimal.java` | Links de um animal. `acessos` só aparece para quem pode segui-lo — um link que responderia 403 desenha um botão que só falha depois do clique |

É o que leva a API do nível 2 ao **nível 3 de Richardson**: o cliente descobre o
que pode fazer pela própria resposta, em vez de carregar por fora uma cópia da
máquina de estados que envelhece em silêncio quando a regra do servidor muda.

O `EntityModel` serializa o conteúdo inline e acrescenta `_links`, então o
contrato antigo continua valendo: quem lia `$.id` continua lendo `$.id`.

---

## O que um controller sempre tem

```java
@RestController
@RequestMapping("/tutores")          // sem /api/v1 — quem prefixa é o WebConfig
@RequiredArgsConstructor             // injeção por construtor, sem @Autowired
@Tag(name = "Tutores", ...)          // aparece no Swagger
public class TutorController {

    private final TutorService tutorService;   // final: a dependência não muda

    @PostMapping
    @PreAuthorize("hasAnyRole('VETERINARIO','ADMIN')")
    @Operation(summary = "...")
    public ResponseEntity<TutorResponse> criar(@Valid @RequestBody TutorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tutorService.criar(request));
    }
}
```

- **`@Valid`** dispara o Bean Validation do DTO. Sem ele, as anotações do
  `Request` são decoração.
- **`@PreAuthorize`** é obrigatório em todo `POST`, `PUT`, `PATCH` e `DELETE`
  deste pacote — ver abaixo.
- **`ResponseEntity`** para escolher o status: `201` no POST, `204` no DELETE,
  `200` no resto.

## A guarda que quebra o build

`CoberturaDeAutorizacaoTest` varre o `RequestMappingHandlerMapping` do Spring e
exige que **todo endpoint de escrita deste pacote** carregue `@PreAuthorize` ou
apareça numa lista de exceções com o motivo escrito por extenso.

Não é zelo abstrato. A guarda foi escrita depois de uma revisão manual de
segurança e achou **três falhas que a revisão tinha deixado passar** — entre
elas o `POST /{id}/concluir`, onde um veterinário gravava `desfecho: OBITO` no
prontuário de outra clínica.

Endpoint de escrita novo sem dono declarado não passa no `mvn test`.

---

## Para onde ir

| Assunto | Documento |
|---|---|
| A regra de negócio que o controller chama | [service.md](service.md) |
| O contrato de entrada e saída | [dto.md](dto.md) |
| Quem pode chamar cada rota | [../08-seguranca.md](../08-seguranca.md) |
| Payloads e códigos de erro | [../03-api-rest.md](../03-api-rest.md) |
