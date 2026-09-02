# `model` — as entidades e os enums do domínio

`src/main/java/br/com/fiap/clyvovet/model` · 13 entidades + 1 `@Embeddable` + 13 enums

É o vocabulário do sistema. Uma classe aqui é uma tabela; um enum aqui é uma
coluna com valores fechados, gravada **sempre como texto**
(`@Enumerated(EnumType.STRING)`), nunca como ordinal — o padrão do JPA tornaria a
tabela ilegível em consulta manual e quebraria no dia em que alguém reordenasse
as constantes.

Todas as entidades usam `@GeneratedValue(strategy = GenerationType.UUID)`.
Nenhuma delas sai pela API: a resposta é sempre um DTO.

---

## As entidades

### O núcleo

```
Tutor ──1:N──> Animal ──┐
                        ├──> EventoClinico ──1:N──> Pagamento
Clinica ─1:N─> Veterinario ──┘
```

| Arquivo | O que representa |
|---|---|
| `Tutor.java` | A pessoa dona do animal. Raiz: não depende de ninguém |
| `Animal.java` | O paciente. Carrega o **microchip**, o peso e o interruptor do resumo de segurança |
| `Clinica.java` | A clínica parceira. Raiz |
| `Veterinario.java` | O profissional, vinculado a uma clínica |
| `EventoClinico.java` | **O centro do domínio.** Amarra animal + veterinário + clínica numa data |
| `Pagamento.java` | A cobrança de um atendimento |
| `Endereco.java` | `@Embeddable` — as colunas de endereço, compartilhadas por tutor e clínica |

### Agenda e catálogo

| Arquivo | O que representa |
|---|---|
| `Servico.java` | Um item do catálogo da clínica: nome, **preço** e duração |
| `DisponibilidadeVeterinario.java` | Uma faixa recorrente da grade: "toda terça, das 08:00 às 12:00" |
| `Bloqueio.java` | Um furo na grade: férias, folga, congresso, almoço |

### Prontuário e consentimento

| Arquivo | O que representa |
|---|---|
| `AlertaClinico.java` | Alergia, condição crônica, medicação contínua ou aviso crítico |
| `AutorizacaoAcesso.java` | O consentimento do tutor para uma clínica ler o histórico |
| `AcessoHistorico.java` | O registro de que alguém leu o prontuário de um animal |

### Identidade

| Arquivo | O que representa |
|---|---|
| `Usuario.java` | Quem faz login. Separado das entidades de domínio |

---

## Seis decisões de modelagem que explicam o resto do sistema

### 1. `EventoClinico.agendado(...)` — a única forma de nascer

```java
public static EventoClinico agendado(Animal animal, Clinica clinica, Servico servico,
                                     Veterinario veterinario, TipoEvento tipoEvento,
                                     LocalDate data, String hora, String descricao)
```

Antes, agendar e marcar retorno montavam o agregado à mão, cada um com sua
sequência de setters: os mesmos oito campos, escritos duas vezes. Nada acusava o
campo esquecido — foi assim que o serviço sumiu do PATCH e o preço do
atendimento deixou de acompanhar a correção.

Com a construção aqui, campo obrigatório novo vira **erro de compilação** nos
dois pontos de chamada, e não uma omissão silenciosa num deles.

O que **não** entra: `eventoOrigem`, `pesoKg`, `desfecho` e `dataRetornoPrevisto`.
São do ciclo de vida posterior, não do nascimento — quem os define é `concluir()`
e `agendarRetorno()`.

### 2. `StatusEvento` carrega a tabela de transições

```java
//         concluir  cancelar  retorno   impedimento para concluir
AGENDADO ( true,     true,     false,    null),
REALIZADO( false,    false,    true,     "Este atendimento já foi concluído"),
FALTOU   ( false,    false,    false,    "Um atendimento marcado como falta não pode ser concluído"),
CANCELADO( false,    false,    false,    "Um atendimento cancelado não pode ser concluído");
```

A mesma pergunta é feita em três lugares: a conclusão (`RetornoService`), o
cancelamento (`AgendamentoService`) e os links HATEOAS (`LinksDoEvento`).
Enquanto cada um decidia por conta própria, `FALTOU` aceitava conclusão no
service enquanto o HATEOAS já o tratava como estado terminal — e **concluir uma
falta registrava no histórico clínico uma vacina que não foi aplicada**.

Estado novo entra como uma linha, e a linha não compila sem responder às três
colunas. É esse o ponto: a célula esquecida deixa de ser possível.

### 3. O microchip identifica, nunca autoriza

Ele está impresso na carteira de vacinação e no contrato de adoção, e qualquer
leitor de pet shop ou canil o lê — como senha não valeria nada. O que credencia
a leitura do resumo de segurança é a **autenticação do veterinário**; o chip só
diz de qual animal se trata.

### 4. Alerta é tabela, não coluna

O resumo de segurança precisa ser derivado de dados **estruturados**, nunca de
um campo "observações" digitado à parte: texto livre mantido à mão envelhece, e
um resumo de alergias desatualizado é pior que nenhum.

### 5. Grade e furo são coisas separadas

`DisponibilidadeVeterinario` é semanal e recorrente; `Bloqueio` é o furo
pontual. A alternativa — exceções dentro da linha da grade — obrigaria a
recriar a grade inteira toda vez que alguém tirasse um dia.

E a grade tem **vigência**: quando o veterinário troca de horário, a linha
antiga ganha `vigencia_fim` e uma nova começa. Sem isso, remarcar a grade
invalidaria retroativamente agendamentos que eram legítimos quando foram feitos.

Um `Bloqueio` com as duas horas nulas vale **dias inteiros** (férias); com as
duas preenchidas, vale **aquela faixa de cada dia** do intervalo (almoço).

### 6. Auditoria é uma linha por (usuário, animal, dia)

Com contador — e não uma por requisição. O veterinário abre a tela várias vezes
durante a consulta e o front repagina; auditar cada `GET` encheria a tabela de
ruído e a tornaria ilegível justamente para quem ela existe: **o tutor**. O que
interessa a ele é "a Dra. Camila leu o histórico do Thor em 12/09", não quantas
vezes rolou a página.

---

## Os enums

| Enum | Valores | Nota |
|---|---|---|
| `Perfil` | `TUTOR` `VETERINARIO` `ADMIN` | Vira `ROLE_*` no Spring Security |
| `StatusEvento` | `AGENDADO` `REALIZADO` `FALTOU` `CANCELADO` | Carrega a tabela de transições |
| `TipoEvento` | `CONSULTA` `RETORNO` `VACINA` `EXAME` `CIRURGIA` `OUTRO` | `VACINA` é o que alimenta a lista de vacinas do resumo |
| `Desfecho` | `MELHORA` `ESTAVEL` `PIORA` `OBITO` `INDEFINIDO` | **Nulo ≠ `INDEFINIDO`**: nulo é "não concluído"; `INDEFINIDO` é "concluído, e o veterinário não soube classificar" |
| `StatusPagamento` | `PENDENTE` `PAGO` `CANCELADO` `REEMBOLSADO` | Os dois últimos são terminais |
| `FormaPagamento` | `PIX` `CARTAO` `DINHEIRO` `BOLETO` | |
| `NivelAcesso` | `OPERACIONAL` `RESUMO_DE_SEGURANCA` `COMPLETO` | Cada nível tem base legal própria |
| `TipoAlerta` | `ALERGIA` `CONDICAO_CRONICA` `MEDICACAO_CONTINUA` `CRITICO` | `CRITICO` é a saída para o que não cabe nas outras três e ainda assim não pode passar despercebido |
| `OrigemAlerta` | `TUTOR` `VETERINARIO` | Não é metadado decorativo — ver abaixo |
| `StatusAutorizacao` | `VIGENTE` `REVOGADA` `EXPIRADA` | **Não existe `PENDENTE`** — ver abaixo |
| `DiaSemana` | `SEGUNDA` … `DOMINGO` | Existe em vez de `DayOfWeek` — ver abaixo |
| `Sexo` | `MASCULINO` `FEMININO` `OUTRO` | Da pessoa |
| `SexoAnimal` | `MACHO` `FEMEA` `DESCONHECIDO` | Do animal |

### Por que `OrigemAlerta` importa

"O tutor disse que tem alergia a dipirona" e "o veterinário registrou anafilaxia
a dipirona" pesam diferente na decisão clínica. Quem lê o resumo de segurança
precisa dessa distinção para saber o quanto confiar no que está lendo.

Por isso a origem é **derivada do perfil de quem grava** e nunca aceita do corpo
da requisição.

### Por que não existe `PENDENTE` em `StatusAutorizacao`

O consentimento é concedido no próprio ato do agendamento. Não há pedido do
veterinário nem fila de aprovação, então não há estado intermediário a
representar: a autorização nasce `VIGENTE` ou não nasce.

### Por que `DiaSemana` existe em vez de `java.time.DayOfWeek`

O domínio inteiro fala português (`Perfil`, `TipoEvento`, `StatusPagamento`), e
uma linha gravada como `'MONDAY'` no meio de `'SEGUNDA'` seria a única em inglês.
A conversão de e para `DayOfWeek` fica dentro do próprio enum, num lugar só.

---

## Onde continuar

| Assunto | Documento |
|---|---|
| O mapeamento objeto↔tabela e o DDL | [../02-modelo-de-dados.md](../02-modelo-de-dados.md) |
| As consultas sobre estas entidades | [repository.md](repository.md) |
| Como elas viram JSON | [mapper.md](mapper.md) e [dto.md](dto.md) |
