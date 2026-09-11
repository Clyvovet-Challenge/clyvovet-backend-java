# O contrato de erro das três pontas

Uma ação do tutor atravessa duas APIs. Marcar uma consulta grava na **Java** e
notifica pela **.NET**. Quando alguma coisa falha, o tutor vê uma frase — e
alguém precisa conseguir descobrir o que aconteceu a partir dela.

Este documento descreve o formato que as três pontas combinaram.

---

## O formato

### API Java

```json
{ "campo": "dataNascimento", "mensagem": "A data não pode estar no futuro." }
```

O `400` de validação é o único que responde uma **lista** desses objetos — um por
campo recusado. Os demais status respondem um objeto só.

### API .NET

```json
{ "error": "Produto com id abc não encontrado." }
```

As chaves são diferentes por história, não por escolha: as duas APIs nasceram em
disciplinas diferentes e o aplicativo já normaliza os dois formatos em
`src/services/http.ts`. Unificá-las agora quebraria telas sem ganho real.

---

## A referência

Falha de servidor — e **só** ela — ganha um campo a mais, com o mesmo nome nas
duas APIs:

```json
{ "campo": "servidor",
  "mensagem": "Erro inesperado no servidor. Tente novamente em instantes.",
  "referencia": "app-m4k2p1-8f3a9c21" }
```

```json
{ "error": "Erro interno no servidor.",
  "referencia": "app-m4k2p1-8f3a9c21" }
```

### Por que só na falha de servidor

Nos outros erros o usuário tem o que corrigir: o campo está em branco, a data
está no passado, o cadastro está em uso. A frase já diz o que fazer, e um código
ao lado dela transformaria uma mensagem acionável em ruído.

Na falha de servidor não há nada que ele possa fazer. A única coisa útil que
sobra é poder dizer **qual** falha aconteceu — e é isso que a referência é.

### De onde ela vem

O identificador nasce no **aplicativo**, não no servidor:

```
app-m4k2p1-8f3a9c21
└─ src/services/http.ts, novaCorrelacao()
```

Ele viaja no header `X-Correlation-Id`, que as duas APIs aceitam e devolvem.
Gerado em cada servidor, uma ação que toca as duas produziria dois rastros sem
ligação entre si. Vindo do aplicativo, a mesma ação aparece com o **mesmo id**
nos dois logs.

Quando o aplicativo não manda (um `curl`, o Swagger, um webhook), cada API gera o
seu — um `UUID` na Java, o `TraceIdentifier` na .NET.

### Onde ela aparece

| Ponta | Onde |
|---|---|
| Java | header em toda resposta, e no corpo do `500` |
| .NET | header em toda resposta, e no corpo do `500` |
| Aplicativo | rodapé do `ErroBox`, e no `Alert` de `avisarErro` |
| Log Java | `[app-m4k2p1-8f3a9c21]` em toda linha, via MDC |
| Log .NET | `(app-m4k2p1-8f3a9c21)` em toda linha, via `LogContext` |

O valor vindo de fora é **saneado** nas duas APIs — só letras, dígitos, hífen e
underline. Um header com `\n` escreveria uma linha de log inteira por conta
própria, e log forjado é pior do que log nenhum: engana quem investiga.

---

## Como usar

Quando alguém relata um erro, peça a referência e procure por ela:

```bash
docker logs clyvovet-java  2>&1 | grep app-m4k2p1-8f3a9c21
docker logs clyvovet-dotnet 2>&1 | grep app-m4k2p1-8f3a9c21
```

A primeira devolve a pilha completa — ela fica no log de propósito, porque não
pode ir para a tela: a mensagem de uma exceção cita classe, tabela e às vezes o
SQL, o que não ajuda quem lê e entrega o desenho interno a quem estiver sondando.

---

## Onde está o código

| O quê | Onde |
|---|---|
| Filtro de correlação (Java) | `config/CorrelacaoFilter.java` |
| Rede de segurança (Java) | `exception/GlobalExceptionHandler.handleInesperado` |
| Formato de saída (Java) | `dto/exception/ErroValidacao.java` |
| Middleware de correlação (.NET) | `Middleware/CorrelationIdMiddleware.cs` |
| Decisão de status/mensagem/referência (.NET) | `Errors/MapaDeErro.cs` |
| Normalização dos formatos (app) | `src/services/http.ts` |
| Tradução para o usuário (app) | `src/utils/erros.ts` |

Testes: `RastreabilidadeDeErroTest` (Java) e `MapaDeErroTests` (.NET).
