# `dto` — os contratos de entrada e saída

`src/main/java/br/com/fiap/clyvovet/dto` · 56 classes em 14 subpacotes

Um DTO é o que a API promete receber e o que ela promete devolver. **A entidade
nunca sai por aqui** — serializar `Usuario` direto expõe o hash da senha,
serializar `Animal` arrasta o tutor inteiro junto, e os dois amarram o contrato
público ao mapeamento do banco.

---

## As três formas

| Sufixo | Tipo Java | Papel |
|---|---|---|
| `...Request` | `class` com `@Getter` | O corpo do `POST` e do `PUT` — descreve o recurso **inteiro** |
| `...PatchRequest` | `class` com `@Getter` | O corpo do `PATCH` — descreve **só o que muda** |
| `...Response` | `record` | O que sai. Imutável: ninguém precisa alterar uma resposta depois de montada |

Os `Request` são classes e não records porque o Jackson precisa desserializar
com construtor sem argumentos e as anotações de validação ficam nos campos. Os
`Response` são records porque são dados de saída imutáveis.

---

## Os subpacotes

| Subpacote | Arquivos | O que carrega |
|---|---:|---|
| `tutor/` | 3 | `TutorRequest`, `TutorPatchRequest`, `TutorResponse` |
| `animal/` | 3 | Idem para animal — inclui microchip e o interruptor do resumo |
| `clinica/` | 3 | Idem para clínica |
| `veterinario/` | 3 | Idem para veterinário |
| `endereco/` | 2 | `EnderecoRequest` e `EnderecoResponse`, aninhados em tutor e clínica |
| `eventoClinico/` | 7 | CRUD do atendimento **mais** os corpos do fluxo R |
| `pagamento/` | 8 | CRUD do pagamento **mais** os corpos e as saídas do fluxo P |
| `agendamento/` | 3 | Fluxo A: marcar, cancelar, e a vaga livre |
| `agenda/` | 4 | Disponibilidade e bloqueio, entrada e saída |
| `historico/` | 10 | Fluxo C: os três níveis, alertas, auditoria |
| `servico/` | 2 | O catálogo |
| `auth/` | 6 | Login, refresh, registro, criação de usuário |
| `autorizacao/` | 1 | `AutorizacaoResponse` — o consentimento como o tutor o vê |
| `exception/` | 1 | `ErroValidacao` — o formato único de erro da API |

### `eventoClinico/`

| Arquivo | O que é |
|---|---|
| `EventoClinicoRequest.java` | Corpo do POST e do PUT |
| `EventoClinicoPatchRequest.java` | Corpo do PATCH |
| `EventoClinicoResponse.java` | A saída |
| `ConclusaoRequest.java` | O veterinário fechando o atendimento — peso, desfecho, retorno previsto |
| `ConclusaoResponse.java` | O atendimento concluído **mais o aviso clínico**, quando há um |
| `RetornoRequest.java` | A marcação do retorno |
| `RetornoVencidoResponse.java` | Uma linha da lista de pets que deviam ter voltado |

### `pagamento/`

| Arquivo | O que é |
|---|---|
| `PagamentoRequest.java` · `PagamentoPatchRequest.java` · `PagamentoResponse.java` | O CRUD |
| `ConfirmacaoRequest.java` | `PENDENTE → PAGO`, com a data |
| `EstornoRequest.java` | `PAGO → REEMBOLSADO`, com o motivo |
| `SaldoResponse.java` | Quanto custou, quanto entrou, quanto falta |
| `InadimplenciaResponse.java` | Uma linha da lista de devedores, **com o contato do tutor** |
| `ExtratoResponse.java` | Pago, pendente e estornado no período |

### `historico/`

| Arquivo | O que é |
|---|---|
| `ResumoDeSegurancaResponse.java` | **Nível 1** — o que qualquer veterinário autenticado alcança pelo microchip |
| `HistoricoResponse.java` | **Nível 2** — o histórico montado conforme o nível que o solicitante alcança |
| `LinhaDoTempoResponse.java` | Um atendimento da linha do tempo |
| `PesoResponse.java` | Um ponto da série de peso |
| `VacinaResponse.java` | Derivada dos eventos de tipo `VACINA`, nunca digitada à parte |
| `AlertaRequest.java` · `AlertaResponse.java` | Alergia, condição crônica, medicação contínua |
| `EmergenciaRequest.java` | A quebra de vidro — só o motivo |
| `AcessoResponse.java` | Uma linha da auditoria, como o tutor a vê |
| `ExcessoDeAcessoResponse.java` | Uma linha da revisão de tetos: um profissional, um dia, uma contagem |

---

## Cinco campos que **não** existem, e por quê

O que um DTO de entrada recusa a aceitar é decisão de segurança, não
esquecimento. Cinco ausências deste pacote fecham vetores reais:

| DTO | Campo ausente | O que ele permitiria |
|---|---|---|
| `RegistroRequest` | `perfil` | Qualquer um se cadastrar como **ADMIN** — escalação de privilégio por *mass assignment* |
| `RegistroRequest` | `tutorId` | Quem descobrisse o UUID de um tutor se registrava apontando para ele e passava a ver os animais, o histórico, os pagamentos, o CPF e o endereço daquela pessoa. A rota é **pública e não autenticada**; nenhuma verificação de identidade acontecia no caminho |
| `AgendamentoRequest` | `statusEvento` | Um atendimento que nasce `REALIZADO` sem nunca ter acontecido |
| `AlertaRequest` | `origem` | Um tutor gravar um alerta como se fosse do veterinário — e a origem é justamente o que diz ao próximo profissional o quanto confiar naquela informação |
| `RetornoRequest` | `animalId` | Marcar o retorno de um pet ligado à consulta de outro. A FK `evento_origem_id` não sabe nada sobre qual animal é qual, então nada no banco reclamaria |

Nos cinco casos o valor é **derivado no service**, não aceito do corpo.

E um que existe, mas é `Boolean` e não `boolean`:

```java
/** Consentimento de acesso ao histórico (fluxo C). */
private Boolean consentimentoHistorico;
```

O campo ausente chega `null` e conta como **recusa**. Consentimento pré-marcado
não é consentimento.

---

## Por que o PATCH tem DTO próprio

O raciocínio completo está escrito em `TutorPatchRequest`, e os outros quatro
`PatchRequest` apontam para lá. Em resumo:

No `POST` e no `PUT` o corpo descreve o recurso inteiro, então `nome` é
obrigatório. No `PATCH` o corpo descreve só o que muda, e a ausência de `nome`
significa "não mexa nele" — um `@NotBlank` ali rejeitaria toda requisição que
não reenviasse o objeto completo, que é justamente o que o PATCH evita.

Por isso o DTO de PATCH mantém as restrições de **formato** (`@Size`, `@Email`,
`@Pattern`) e abre mão das de **presença** (`@NotNull`, `@NotBlank`).

A alternativa seria reaproveitar o `Request` com **grupos de validação**. Foi
descartada porque exigiria anotar campo a campo dos DTOs já existentes, e um
grupo esquecido enfraqueceria em silêncio a validação do POST — o erro mais caro
dos dois.

**Limite conhecido, válido para todos os PATCH desta API:** campo ausente e
campo enviado como `null` chegam iguais, então não há como *apagar* um campo
opcional via PATCH. Use PUT.

---

## Duas validações que espelham o banco

### A hora, `@Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d")`

A coluna é `VARCHAR2(5)`: só cabe `HH:mm`. Antes o campo tinha apenas
`@NotNull`, então `""` era aceito e `"14:30:00"` chegava ao banco para estourar
lá, como erro de servidor. O `@Pattern` recusa os dois na **validação**, que é
onde o cliente consegue entender o que errou.

Na grade de disponibilidade o formato de largura fixa não é cosmético: o check
`chk_disp_horas` compara `hora_fim` com `hora_inicio` como **texto**, e `'9:00'`
é maior que `'18:00'` lexicograficamente. Sem o `@Pattern`, o zero à esquerda
faltante passaria pela API e desligaria a proteção do banco em silêncio.

### O motivo da emergência, `@Size(min = 10)`

Não é burocracia: é o único custo que o acesso sem consentimento impõe a quem o
aciona. Um campo que aceitasse `"x"` transformaria a exceção no caminho mais
curto, e o consentimento viraria enfeite.

---

## `ErroValidacao` — o formato único de erro

```java
public record ErroValidacao(String campo, String mensagem) { }
```

Toda falha da API sai no mesmo formato, montado pelo
[`GlobalExceptionHandler`](exception.md) — inclusive as do Spring Security, que
por padrão devolveriam uma página HTML. Um cliente que sabe ler um erro sabe ler
todos.

---

## Onde continuar

| Assunto | Documento |
|---|---|
| Os payloads completos, campo a campo | [../03-api-rest.md](../03-api-rest.md) |
| Quem copia o DTO para a entidade | [mapper.md](mapper.md) |
| As regras A/R/C/P por extenso | [../08-seguranca.md](../08-seguranca.md) |
