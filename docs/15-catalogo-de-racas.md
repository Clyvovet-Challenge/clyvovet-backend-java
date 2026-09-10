# 15 — Catálogo de raças

Por que `raca` deixou de ser texto livre, o que a mudança resolve e o que ela
deliberadamente **não** faz.

---

## O problema, medido

`t_clyvo_animal.raca` e `especie` eram `VARCHAR` livres. O app já oferecia lista
fechada para espécie, porte, sexo e castrado — **só a raça era campo de texto**.
Mas a API aceitava qualquer coisa de qualquer cliente, então o seed e as chamadas
diretas encheram a tabela.

Contado no MySQL, com 28 animais:

| O que foi digitado | Grafias |
|---|---|
| espécie "cachorro" | `Cachorro` · `CAO` · `CANINO` |
| espécie "gato" | `Gato` · `GATO` · `FELINO` |
| sem raça definida | `SRD` · `Vira` · `Vira-lata` — **9 animais** |
| variantes | `Poodle` / `Poodle Toy`, `Labrador misto` |
| sem acento | `Pastor Alemao`, `Bulldog Frances` |

Uma busca direta `IMAGENS[animal.raca]` acertaria **15 de 28** (54%).

## Isso já custava, e num lugar que pontua

O widget de saúde preditiva da **API .NET** casa raça para sugerir risco de
doença. Sem padronização, ele compara por substring — e o comentário no código
diz por quê:

```csharp
// Compara de forma tolerante (ex.: "Labrador" casa com "Labrador Retriever"
// cadastrado no Animal) — a raça do Animal vem de texto livre da API Java,
// sem padronização.
return a.Contains(b) || b.Contains(a);
```

Duas falhas, em direções opostas:

- **falso negativo:** `Siames` não casa com `Siamês` — `Contains` não tira acento
- **falso positivo:** uma predisposição cadastrada como `Terrier` casa com
  Yorkshire, Bull e Fox Terrier — raças com predisposições diferentes

Num recurso que fala de doença.

---

## A solução: `chave`

```sql
CREATE TABLE t_clyvo_raca (
  id            VARCHAR(36)  NOT NULL PRIMARY KEY,
  especie       VARCHAR(20)  NOT NULL,   -- CAO, GATO, ROEDOR, AVE, REPTIL
  nome          VARCHAR(100) NOT NULL,   -- 'Golden Retriever'   ← o tutor lê
  chave         VARCHAR(60)  NOT NULL,   -- 'golden-retriever'   ← o código usa
  porte_tipico  VARCHAR(20)  NULL,
  ativo         INT          NOT NULL DEFAULT 1,
  CONSTRAINT uk_raca_chave UNIQUE (chave)
);
```

**`chave` é o mesmo identificador em três lugares:** a arte em pixel do animal no
app, a predisposição de saúde na API .NET, e a busca no cliente. Ninguém
normaliza nada porque não há o que normalizar — quem escolhe do catálogo já
recebe a chave pronta. E `UNIQUE`, então duas linhas não disputam o mesmo
significado.

45 raças em cinco espécies. **Curto de propósito:** um seletor com 200 itens é
pior que um campo de texto, e cada linha daqui vai ganhar uma arte em pixel — o
catálogo não pode crescer mais rápido do que alguém consegue desenhar.

## Quatro decisões, e o motivo de cada uma

**A FK nasce anulável.** São 200 e poucas raças de cachorro; o catálogo lista as
comuns. "Outra raça" precisa continuar existindo, e nulo ali significa "o tutor
digitou algo que o catálogo não cobre".

**A coluna de texto `raca` fica.** É o que faz a `V14` não quebrar nada: todo
`SELECT` existente, o widget da .NET e o `AnimalResponse` continuam lendo `raca`
como sempre leram. A regra, dita uma vez: **com `raca_id` preenchido, `raca` é
cópia do catálogo; com ele nulo, é o que o tutor digitou.** `Labrador misto` diz
algo que `Labrador` não diz.

**Espécie continua `String`, mas passa a ser derivada.** Escolher a raça grava
`especie` a partir do catálogo. Uniformiza `CAO`/`CANINO`/`Cachorro` sem trocar o
tipo da coluna — o que quebraria o widget da .NET e o contrato que o app consome.

**Enum não serve para raça.** Este era o conserto proposto na pendência 12.
Acrescentar uma raça viraria mudança de código e redeploy; com tabela, é um
`INSERT`. Enum serve para *espécie*, que é fechada e pequena — e é o que
`EspecieAnimal` é.

---

## Três coisas que só apareceram rodando

Vale registrar, porque nenhuma estava no plano.

**1. `TINYINT` não serve.** O `NumericBooleanConverter` do Hibernate mapeia
boolean para `INTEGER`, e o perfil `mysql` roda com `ddl-auto=validate`: com
`TINYINT` a aplicação **não sobe**. A `V3` já tinha deixado anotado o mapeamento
`NUMBER(1) -> INT`.

**2. As duas pastas de migration rodam contra H2.** `spring.flyway.locations` dos
perfis `dev` e `h2` aponta para `db/migration/oracle`, e `dev` é o que a suíte de
testes usa; o `MigrationsMySqlTest` aponta para `db/migration/mysql`. A primeira
versão usava `MERGE` + `CONVERT(x,'US7ASCII')` no arquivo Oracle e
`UPDATE ... JOIN` no MySQL. Nenhum dos dois existe em H2, e **os 363 testes
caíram de uma vez** com `Syntax error ... expected "data type"` — erro que não
menciona acento nenhum. A reconciliação virou subconsulta correlacionada com
`REPLACE` encadeado: feio, e roda nos três bancos.

**3. O casamento por nome era ambíguo.** Duas linhas do catálogo se chamam
`Sem raça definida` — uma de cão, outra de gato — e a subconsulta devolveria duas
linhas. **Não estourou na primeira execução por acidente de ordem:** naquele
momento o texto ainda era `SRD`/`Vira` e não casava com aquele nome. Só que o
passo 4 reescreve o texto para `Sem raça definida`, e uma reaplicação encontraria
a ambiguidade. Recortar por espécie resolve — e está certo de qualquer forma:
nome de raça só significa algo dentro de uma espécie.

## Duas divergências antigas fechadas de quebra

**`porte` inválido era 500.** O `chk_animal_porte` existe desde a `V1` nos dois
dialetos, mas nada validava antes de chegar lá — o erro de integridade voltava
como 500, que o cliente lê como "a API quebrou". Agora é **400** com mensagem.

**O app mandava `Pequeno` e só o MySQL perdoava.** A colação da coluna é
`utf8mb4_0900_ai_ci` — *accent-insensitive, case-insensitive* —, então
`'Pequeno' IN ('PEQUENO',…)` é verdadeiro lá. No **Oracle não é**: o mesmo
cadastro que funciona hoje seria recusado. O mapper normaliza para maiúscula.

> Vale a generalização, porque ela explica o problema todo: **o banco já era
> tolerante a acento e caixa; quem não é são os comparadores em código** — o
> `Contains` da .NET e qualquer `IMAGENS[raca]` no app. Era lá que a
> padronização precisava existir.

---

## Verificação

Contra o MySQL real, com o volume recriado do zero:

```
14 migrations aplicadas, todas com sucesso
6 de 6 animais do seed casados com o catálogo
espécie: 5 grafias → 2
```

E os três casos de borda, pela API:

| Enviado | Resultado |
|---|---|
| `porte: "XXX"` | **400** — `porte deve ser PEQUENO, MEDIO ou GRANDE` |
| `porte: "Pequeno"` (como o app manda) | **201**, gravado `PEQUENO` |
| `racaId` de Dachshund + `raca: "vira lata qualquer"` + `especie: "CANINO"` | **201**, gravado `Dachshund` / `Cachorro` / `dachshund` |

`MigrationsMySqlTest` cobre a reconciliação contra H2: catálogo completo, chaves
únicas, nenhum animal sem ligação, e o acento reescrito. **363 testes verdes.**

---

## O que fica pendente

**A API .NET usar a `chave`.** A tabela de predisposição guarda `Raca` como
texto; trocar por `raca_chave` e o `Contains` por `==` elimina tanto o falso
negativo do acento quanto o falso positivo do `Terrier`. Enquanto isso não
acontece ela continua funcionando — e melhor do que antes, porque o texto que ela
lê agora é uniforme.

**A arte em pixel por raça.** É o consumidor que motivou tudo isto: cada `chave`
vira o nome de um arquivo, e o app faz `IMAGENS[animal.racaChave]` sem
normalizar nada. O fallback é por espécie, e por isso o catálogo tem uma linha
"sem raça definida" em cada uma — 9 dos 28 animais caíam ali.
