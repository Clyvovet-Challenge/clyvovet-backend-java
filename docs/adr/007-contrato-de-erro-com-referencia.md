# 007 — Contrato de erro das três pontas, com referência só na falha de servidor

**Data:** 2026-10-10 (registra uma decisão anterior à Sprint 4) · **Status:** aceito

## Contexto

Uma ação do tutor atravessa duas APIs: marcar uma consulta grava na Java e notifica pela .NET.
Quando algo falha, o tutor vê uma frase, e alguém precisa conseguir descobrir o que aconteceu a
partir dela. As duas APIs nasceram em disciplinas diferentes e respondem erro em formatos
diferentes:

```json
{ "campo": "dataNascimento", "mensagem": "A data não pode estar no futuro." }
```

```json
{ "error": "Produto com id abc não encontrado." }
```

## Decisão

- **Não unificar os formatos.** O app já normaliza os dois em `src/services/http.ts`, e unificá-los
  agora quebraria telas sem ganho real.
- **Acrescentar um campo `referencia`, com o mesmo nome nas duas APIs, apenas na falha de
  servidor** (o corpo do `500`). Ela liga a frase que o tutor vê à linha de log do servidor.
- **A referência nasce no aplicativo**, não no servidor: é o id de correlação enviado no header
  `X-Correlation-Id`, que as duas APIs aceitam e devolvem. Assim uma ação que toca as duas aparece
  com o mesmo id nos dois logs. Sem o header (`curl`, Swagger), cada API gera o seu.

Nos demais erros o usuário tem o que corrigir (campo em branco, data no passado, cadastro em uso)
e a frase já diz o quê; um código ao lado transformaria uma mensagem acionável em ruído. Na falha
de servidor não há nada que ele possa fazer, e a única coisa útil é poder dizer *qual* falha foi.

## Consequências

- Quem investiga pede a referência e procura nos logs das duas APIs. A pilha completa fica só no
  log: na tela ela entregaria classe, tabela e SQL a quem estiver sondando.
- O valor que vem de fora é saneado (só letras, dígitos, hífen e underline), porque um header com
  quebra de linha forjaria uma linha de log.
- Os dois formatos continuam diferentes, e o app precisa conhecer ambos.
- O `400` de validação é o único que responde uma lista de objetos; qualquer cliente novo precisa tratar isso.
- Origem da referência, onde aparece e como usar: [`../17-contrato-de-erro.md`](../17-contrato-de-erro.md).
