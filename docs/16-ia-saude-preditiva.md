# 16 — A base de doenças da IA de saúde preditiva (V15)

> Companheiro de migração da `V15__base_de_doencas_e_parecer_ia.sql`, no mesmo
> espírito do `15-catalogo-de-racas.md`: o racional que não cabe no SQL.

## Por que esta migração mora aqui, e não no repo .NET

Pela mesma razão da V8: **o Flyway deste repositório é quem provisiona o schema
compartilhado**, inclusive as tabelas que só a API .NET consome. A alternativa —
cada API criar as suas — já provou onde termina: foi a conversão manual da V8
que esqueceu a coluna `criado_em` de `t_clyvo_predisposicao_saude` e deixou o
widget de saúde preditiva respondendo 500 desde que nasceu. A V15 conserta esse
esquecimento (coluna + o seed de 42 predisposições que nunca fora portado) no
mesmo commit em que cria a base nova — é a mesma história.

## As três coisas que a V15 faz

1. **Conserta o widget**: `criado_em` em `t_clyvo_predisposicao_saude` + as 42
   predisposições do arquivo original da .NET (`schema/05`).
2. **`t_clyvo_base_doencas`**: a agregação dos datasets de doenças por
   espécie/raça — o *grounding* do parecer da IA e do fallback determinístico.
3. **`t_clyvo_parecer_ia`**: o cache do parecer, UM por animal
   (`UNIQUE(animal_id)`), com validade. Cache, não histórico: o prontuário é
   quem guarda saúde de verdade.

## Como a agregação foi feita (e por que não os CSVs crus)

Os dados vieram de dois CSVs preparados pelo integrante de banco de dados da
equipe, derivados de datasets [Dryad](https://datadryad.org):

- **Cães** — `Complex disease and phenotype mapping in the domestic dog`
  (DOI 10.5061/dryad.266k4), registros caso/controle por raça.
- **Multiespécie** — gatos (`Complex Feline Disease Mapping`,
  10.5061/dryad.f1vhhmgwp), aves (vigilância em Galápagos,
  10.5061/dryad.kwh70rz4z) e répteis (registros de reabilitação,
  10.5061/dryad.jh9w0vtmc).

Regras aplicadas na agregação (script de autoria; o resultado auditável são os
INSERTs da própria migração):

| Regra | Motivo |
|---|---|
| Cães só do Dryad canino | o CSV multiespécie amostra os mesmos registros; somar os dois dobraria a contagem |
| Códigos com sufixo de raça normalizados (`MCT_labradorRetrievers` → `MCT`) e deduplicados por (animal, doença) | são o MESMO caso re-rotulado para o estudo por raça |
| Medida contínua (ângulo de Norberg) não vira caso | é medição, não diagnóstico — não inferimos doença de medida |
| Linha só de controles fica fora | controle não afirma predisposição |
| `raca_chave` ligada ao catálogo da V14 | é o que permite a .NET casar por igualdade, aposentando o `Contains` de substring |
| Aves/répteis entram com `raca_chave NULL` | são fauna selvagem/resgate sem diagnóstico; o serviço marca `baseLimitada` e o app avisa o tutor |

Resultado: 260 linhas (202 cão, 41 gato, 5 ave, 12 réptil), 60 linhas de cão e
31 de gato ligadas ao catálogo.

## O consumidor

A API .NET (`SaudePreditivaService`) monta o parecer assim: fatos da base → OCI
Generative AI redige (quando `Oci__*` está no ambiente) → cache de 7 dias →
fallback determinístico com as mesmas linhas quando a IA falta. O canal de
mensagem é só o Telegram — o WhatsApp saiu do escopo nesta sprint, e a
marcação de consultas por mensagem junto com ele.

As variáveis OCI do deploy entram por `azure/06-configuracoes.sh` (todas
opcionais; sem elas o parecer sai com `origem = REGRAS`).

## O teste que prova o conteúdo

`MigrationsMySqlTest.a_v15_conserta_o_widget_e_semeia_a_base_de_doencas` — no
padrão da casa, prova conteúdo e não só schema: as 42 predisposições existem,
as 4 espécies estão na base, **nenhuma `raca_chave` aponta para fora do
catálogo**, nenhuma linha é só-controles, e o segundo parecer do mesmo animal é
recusado pelo banco.

## Pendência conhecida

A divergência de vocabulário de espécie segue existindo e é herdada, não criada
aqui: `t_clyvo_predisposicao_saude` fala `CACHORRO/PASSARO` (enum da .NET),
`t_clyvo_base_doencas` fala `CAO/AVE` (catálogo da V14 e datasets). O serviço
.NET traduz na borda (`CodigoDoCatalogo`). Unificar exigiria migrar o enum da
.NET — candidata natural à Sprint 4, registrada aqui para não virar surpresa.
