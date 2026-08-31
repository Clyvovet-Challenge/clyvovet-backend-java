# 08 — Modelo de negócio e regras a implementar

**Escrito em:** 30/08/2026, a partir da visão de produto descrita pelo time.
**Verificado contra:** o código da branch `main`, commit `7e618c1`.
**Método:** cada linha da visão foi confrontada com o código. O que já existe está marcado
com a evidência; o que falta está especificado para implementação.
**Última decisão incorporada:** 30/08/2026 — o consentimento de acesso ao histórico passa a
ser concedido no agendamento (Partes 5 e 6).

> **Este documento não consta nos PDFs do Challenge.** Ele descreve **o produto que
> queremos**, e não a rubrica da disciplina. Onde os dois se encontram está indicado.
>
> Ele **não substitui** o [07-backlog.md](07-backlog.md), que continua sendo o status
> vigente do repositório. Este aqui é o alvo; aquele é o mapa de onde estamos.

---

## Sumário

| Parte | Conteúdo |
|---|---|
| [1](#1--a-visão) | A visão, como foi descrita |
| [2](#2--o-que-já-está-de-acordo) | O que o código já sustenta |
| [3](#3--o-que-não-está-de-acordo) | As divergências, com evidência |
| [4](#4--as-peças-novas) | As 12 entidades e colunas que faltam |
| [5](#5--fluxo-a--agendamento-pelo-tutor) | Fluxo A — Agendamento |
| [6](#6--fluxo-c--acesso-ao-histórico-clínico) | Fluxo C — Acesso ao histórico, em três níveis |
| [7](#7--fluxo-d--documentos-e-histórico-clínico) | Fluxo D — Documentos |
| [8](#8--fluxo-r--retorno-e-falta) | Fluxo R — Retorno e falta |
| [9](#9--fluxo-p--cobrança) | Fluxo P — Cobrança |
| [10](#10--fluxo-n--clínica-planos-e-painel) | Fluxo N — Clínica, planos e painel |
| [11](#11--o-que-quebra-no-que-já-existe) | Mudanças de ruptura |
| [12](#12--sequenciamento) | Sequenciamento por dependência |
| [13](#13--decisões) | O que já foi decidido e o que falta |

---

## 1 — A visão

Transcrita do que o time descreveu, sem edição de conteúdo.

**Tutor.** Cria o próprio usuário. Adiciona seus animais e as informações deles. Gerencia
seus animais e **marca consultas, exames, retornos** — desde que a clínica ofereça o serviço
e haja horário disponível com o veterinário. Anexa documentos ao histórico clínico do animal
— prontuários, vacinas, cirurgias — **inclusive de outras clínicas e consultas externas**,
alimentando a informação que o veterinário usa para o melhor diagnóstico possível.

**Animal.** Gerenciado pelo tutor. Pode ter os dados alterados pelo tutor ou pelo
veterinário, **desde que o veterinário receba autorização do tutor**.

**Clínica.** Estabelecimento registrado na plataforma, com um **administrador principal**
que gerencia serviços oferecidos, raças atendidas e o corpo de veterinários. É o carro-chefe
da monetização: paga uma **licença** para estar na plataforma e, conforme o plano, acessa
informação estratégica. Tem um painel com consultas marcadas e atendidas, faturamento,
gestão de veterinários e leitura competitiva — quais raças mais atende, onde tem mais
sucesso e mais fracasso.

**Veterinário.** Associado a uma clínica. Registra os horários disponíveis do seu serviço.
Gerencia os dados clínicos dos animais — atualiza e consulta o histórico clínico para
entender melhor o paciente. Vê as próprias métricas de atendimento.

**Histórico clínico.** Contém as informações do animal e os documentos anexados. Tutor e
veterinário têm acesso — **mas o veterinário precisa da autorização do tutor para ver.**

> **Refinamento de 30/08/2026 — o acesso virou três níveis.** A regra acima vale na íntegra
> para o **histórico completo**: o veterinário não o vê sem consentimento, e esse consentimento
> é concedido no ato do agendamento, sem pedido separado.
>
> Acima dela ficou um **resumo de segurança** — alergias, condições crônicas, medicação
> contínua, vacinas, último peso — acessível a qualquer veterinário autenticado pelo número do
> microchip, sem consentimento prévio, porque é o que decide um atendimento de emergência. Esse
> resumo notifica o tutor a cada leitura e pode ser desligado por ele. Ver
> [Parte 6](#6--fluxo-c--acesso-ao-histórico-clínico).

---

## 2 — O que já está de acordo

Sete pontos da visão já estão de pé no código. Nada aqui precisa ser construído.

| Item da visão | Onde está |
|---|---|
| Tutor cria o próprio usuário | `POST /api/v1/auth/registrar` — perfil fixado em `TUTOR`, nunca vem da requisição (`UsuarioService.java:38`) |
| Tutor gerencia os próprios animais | `SegurancaService.podeAcessarAnimal()` + `@PreAuthorize` em `AnimalController` |
| Tutor só enxerga o que é dele | `tutorIdParaFiltro()` recorta as listagens; ownership atravessa animal → evento → pagamento |
| Animal pertence a um tutor | `Animal.tutor` — `@ManyToOne` |
| Veterinário associado a uma clínica | `Veterinario.clinica` — `@ManyToOne` |
| Clínica é um estabelecimento registrado | entidade `Clinica` com CNPJ único (`uk_clinica_cnpj`) |
| Atendimento registrado com animal, vet e clínica | `EventoClinico` com as três FKs |

A base de segurança também já existe e é reaproveitada inteira: JWT com refresh e revogação,
lockout de conta, rate limit por IP, três perfis, e o `SegurancaService` como ponto único de
decisão de acesso. **O modelo de consentimento da Parte 6 se encaixa nele sem reescrever a
autenticação.**

---

## 3 — O que NÃO está de acordo

Onze divergências. Três são inversões — o código faz hoje o contrário do que a visão pede.

### 3.1 — Inversões

| # | A visão diz | O código faz | Evidência |
|---|---|---|---|
| **X1** | O tutor marca consultas, exames e retornos | O tutor é **o único que não pode** criar atendimento | `SecurityConfig.java:114` — `POST /eventos-clinicos` é `hasAnyRole(VETERINARIO, ADMIN)` |
| **X2** | O veterinário precisa de autorização para ver o histórico | O veterinário **vê tudo, de todos os animais, sem pedir nada** | `SegurancaService.java:111` — `temVisaoAmpla()` devolve `true` para `VETERINARIO` |
| **X3** | O veterinário precisa de autorização para alterar dados do animal | O veterinário **altera qualquer animal livremente** | mesma origem: `podeAcessarAnimal()` passa direto por `temVisaoAmpla()` |

X2 é a mais estruturante do documento. Ela não é um campo novo: é a inversão do padrão de
acesso de todo o sistema, e atravessa toda consulta que hoje devolve "a base inteira" para o
perfil `VETERINARIO`.

### 3.2 — Ausências

| # | A visão pede | Situação | Consequência |
|---|---|---|---|
| **X4** | Catálogo de serviços por clínica | não existe | "só se a clínica oferece o serviço" não tem contra o quê validar |
| **X5** | Horários disponíveis do veterinário | não existe | "só se houver horário disponível" não tem contra o quê validar |
| **X6** | Documentos anexados ao histórico | não existe entidade nem storage | o insumo do diagnóstico não tem onde morar |
| **X7** | Administrador principal da clínica | não há vínculo `Usuario` ↔ `Clinica` | `ADMIN` hoje é global da plataforma, não da clínica |
| **X8** | Raças atendidas pela clínica | não existe | |
| **X9** | Licença e planos | não existe nada de assinatura | a monetização não tem modelo |
| **X10** | Métricas do veterinário | não existe — e está bloqueado | `UsuarioAutenticado` expõe `getTutorId()` e **não** `getVeterinarioId()`; a identidade do vet logado nunca é lida |
| **X11** | Sucesso e fracasso por raça | não existe **desfecho clínico** em lugar nenhum | sem desfecho, "onde a clínica tem mais sucesso" é uma pergunta sem dado |

### 3.3 — Dois defeitos encontrados no caminho

Não fazem parte da visão, mas foram achados ao confrontá-la com o código e afetam o Fluxo A.

| # | Defeito | Evidência | Efeito |
|---|---|---|---|
| **X12** | O auto-cadastro não cria o registro `Tutor` | `UsuarioService.registrar()` só grava `Usuario` | O tutor recém-registrado fica com `tutor_id` nulo → `podeAcessar()` devolve `false` em tudo → **não consegue cadastrar o próprio animal** |
| **X13** | O auto-cadastro aceita `tutorId` do corpo, em rota **pública** | `UsuarioService.java:40` | Quem souber o UUID de um tutor existente se registra apontando para ele e assume o acesso aos animais, atendimentos e pagamentos daquela pessoa |

X13 expõe dado de saúde e CPF de terceiro. Os UUIDs não são enumeráveis pela API — `GET
/tutores` exige `VETERINARIO` ou `ADMIN` — então o risco depende de um ID vazar. **Corrigir
independente do resto deste documento.**

---

## 4 — As peças novas

Doze entidades. As três primeiras destravam a maior parte da visão.

| # | Entidade | Para quê | Campos essenciais |
|---|---|---|---|
| **E1** | `Servico` | catálogo da clínica | `clinica`, `tipoEvento`, `nome`, `preco`, `duracaoMinutos`, `ativo` |
| **E2** | `DisponibilidadeVeterinario` | grade de horários | `veterinario`, `diaSemana`, `horaInicio`, `horaFim`, `vigenciaInicio`, `vigenciaFim` |
| **E3** | `AutorizacaoAcesso` | o consentimento do tutor (nível 2) | `animal`, `clinica`, `status`, `concedidaEm`, `validoAte`, `origemAgendamentoId`, `revogadaEm` |
| **E4** | `Documento` | anexo do histórico | `animal`, `tipo`, `origem`, `dataDocumento`, `arquivoUri`, `enviadoPor`, `eventoClinico` (opcional) |
| **E5** | `Plano` | níveis de licença | `nome`, `precoMensal`, `recursos` |
| **E6** | `Assinatura` | a licença da clínica | `clinica`, `plano`, `inicio`, `fim`, `status` |
| **E7** | `RacaAtendida` | recorte de atendimento | `clinica`, `especie`, `raca` — ou uma lista dentro de `Servico` |
| **E8** | `Bloqueio` | férias, folga, almoço | `veterinario`, `inicio`, `fim`, `motivo` |
| **E9** | — | vínculo `Usuario.clinica` | não é entidade: é uma coluna e um escopo novo de `ADMIN` |
| **E10** | `AcessoHistorico` | a auditoria de C18 | `animal`, `usuario`, `clinica`, `dia`, `nivel`, `vezes`, `emergencial`, `motivo` |
| **E11** | `CorrecaoCadastral` | o vet propõe, o tutor decide (C17) | `animal`, `campo`, `valorSugerido`, `solicitadoPor`, `status` |
| **E12** | `AlertaClinico` | o conteúdo do nível 1 | `animal`, `tipo` (`ALERGIA`, `CONDICAO_CRONICA`, `MEDICACAO_CONTINUA`, `CRITICO`), `descricao`, `origem` (tutor ou vet), `ativo` |

Além delas, **três colunas** em tabelas existentes:

| Coluna | Tabela | Para quê |
|---|---|---|
| `servico_id` | `evento_clinico` | liga o atendimento ao catálogo — **e é daqui que sai o valor cobrado** |
| `desfecho` | `evento_clinico` | `MELHORA`, `ESTAVEL`, `PIORA`, `OBITO`, `INDEFINIDO` — resolve X11 |
| `clinica_id` | `usuario` | resolve X7 |
| `microchip` | `animal` | identificação no balcão (C1). **Não existe hoje** — nem em `Animal.java`, nem no schema |
| `castrado` | `animal` | compõe o resumo de segurança do nível 1 |

> **Convergência que vale registrar.** `Servico.preco` fecha um furo que ficou aberto no
> mapeamento do fluxo de cobrança: não existia valor no `evento_clinico` para comparar com o
> pagamento. O catálogo resolve os dois problemas de uma vez.
>
> E o status **`AGENDADO`**, criado pela V5 sem dono definido, encontra aqui o seu: é o
> estado em que o evento nasce quando o **tutor** marca. A [decisão 4 do backlog](07-backlog.md#7--decisões-que-travam-trabalho)
> fica respondida.

---

## 5 — Fluxo A — Agendamento pelo tutor

Resolve X1, X4, X5. **Entrada** (tutor escolhe clínica, serviço e horário) → **processamento**
(serviço oferecido? vet atende a espécie? horário livre?) → **resultado** (evento `AGENDADO`).

Atravessa `Tutor`, `Animal`, `Clinica`, `Servico`, `Veterinario`, `Disponibilidade` e
`EventoClinico` — sete entidades.

### Regras

| # | Regra | Erro |
|---|---|---|
| **A1** | O tutor só agenda para animal **dele** | 403 |
| **A2** | O serviço tem de pertencer à clínica escolhida e estar `ativo` | 409 |
| **A3** | O veterinário tem de pertencer à clínica escolhida | 409 |
| **A4** | A clínica tem de atender a espécie/raça do animal (E7) | 409 |
| **A5** | O horário tem de cair dentro da `DisponibilidadeVeterinario` vigente | 409 |
| **A6** | O horário não pode colidir com outro evento `AGENDADO` do mesmo veterinário | 409 |
| **A7** | O horário não pode cair em `Bloqueio` | 409 |
| **A8** | A duração vem de `Servico.duracaoMinutos` — a colisão de A6 considera o intervalo, não só o instante | — |
| **A9** | Não se agenda no passado | 400 |
| **A10** | Não se agenda com menos de **N horas** de antecedência (`N` configurável, sugestão: 2) | 409 |
| **A11** | O evento nasce `AGENDADO`, com `servico_id` preenchido | — |
| **A12** | O tutor cancela o próprio agendamento até **M horas** antes (sugestão: 24) | 409 |
| **A13** | Cancelamento depois de M horas: permitido, mas marcado para a política de no-show | — |
| **A14** | A clínica ou o veterinário cancelam a qualquer momento, com motivo obrigatório | — |
| **A15** | **Agendar é consentir.** O agendamento libera o histórico clínico do animal para a clínica escolhida — ver Fluxo C | — |

A15 é a decisão que funde os dois fluxos. O tutor escolhe a clínica, marca a consulta e, no
mesmo ato, libera o histórico para quem vai atender. Não há pedido, espera nem aprovação: o
consentimento é o próprio agendamento.

### Endpoints

```
GET  /api/v1/clinicas/{id}/servicos
GET  /api/v1/clinicas/{id}/disponibilidade   ?servicoId=&de=&ate=
POST /api/v1/agendamentos                    { animalId, servicoId, veterinarioId, data, hora,
                                               consentimentoHistorico }
POST /api/v1/agendamentos/{id}/cancelar      { motivo }
GET  /api/v1/agendamentos/meus
```

`GET /disponibilidade` é o que devolve os slots livres já descontando A5–A8. É a consulta que
o frontend consome para desenhar o calendário.

---

## 6 — Fluxo C — Acesso ao histórico clínico

Resolve X2 e X3. É a inversão do modelo de acesso atual e o item mais distintivo do produto.

> **Decisão de 30/08/2026 — acesso em três níveis.**
> Duas ideias foram avaliadas: consentimento no agendamento com vigência longa, e histórico
> aberto consultável por microchip. Nenhuma das duas sozinha serve — a primeira trava o
> atendimento de emergência em clínica sem relação prévia; a segunda transforma dado de saúde
> e dado pessoal do tutor em informação pública.
>
> **O que separa as duas é que nem todo o histórico tem o mesmo peso.** O que salva a vida no
> primeiro minuto — alergia, condição crônica, medicação contínua, antirrábica, último peso —
> é pouco dado e expõe pouco. O que expõe muito — quais clínicas o animal frequentou, laudos,
> diagnósticos, CPF e endereço do tutor — não é o que resolve a emergência.
>
> Daí os três níveis abaixo. **O microchip identifica; ele nunca autoriza.** Quem autoriza o
> nível 1 é o CRMV do veterinário; o nível 2, o tutor.

### Os três níveis

| Nível | Quem alcança | O que vê | Base |
|---|---|---|---|
| **0 — Operacional** | qualquer um da clínica com agendamento | nome, espécie, raça, porte, idade | execução do atendimento |
| **1 — Resumo de segurança** | **qualquer veterinário autenticado**, sempre | alergias, condições crônicas, medicação contínua, vacinas, último peso, castrado, contato de emergência | proteção da vida do animal |
| **2 — Histórico completo** | só com consentimento do tutor | linha do tempo de eventos, documentos, laudos, desfechos, dados completos do tutor | consentimento |

### As duas exceções permanentes

Valem em qualquer nível e não dependem de consentimento.

| # | Exceção | Justificativa |
|---|---|---|
| **C0a** | O veterinário vê os atendimentos que ele mesmo conduziu, **enquanto vinculado à clínica onde aconteceram** | O prontuário é obrigação de guarda — mas do **estabelecimento**, não do profissional. Vet que troca de emprego não leva a carteira de pacientes |
| **C0b** | A clínica **sempre** vê os eventos realizados nela | É o registro do próprio atendimento, e a guarda é dela |

---

### Nível 1 — Resumo de segurança

| # | Regra |
|---|---|
| **C1** | `Animal` ganha `microchip` (15 dígitos, ISO 11784/11785), único quando preenchido |
| **C2** | `GET /animais/resumo?microchip=` devolve o nível 1 a **qualquer veterinário autenticado**, sem consentimento e sem vínculo prévio |
| **C3** | O conteúdo é **derivado**, nunca digitado à parte: alertas ativos, vacinas dos eventos, último `peso_kg`, castração. Resumo mantido à mão envelhece e mata |
| **C4** | **Todo acesso de nível 1 notifica o tutor** no momento em que acontece, com o nome do veterinário e da clínica |
| **C5** | O tutor pode desligar o nível 1, com aviso explícito do que perde. Nasce **ligado** |
| **C6** | Teto de consultas por veterinário e por dia. Quem passa do teto é sinalizado ao admin da plataforma — leitura em massa é coleta, não atendimento |
| **C7** | O nível 1 **não** expõe CPF, endereço, histórico de eventos, documentos nem valores. Só telefone de emergência |

> **Por que o microchip não é chave.** Ele está impresso na carteira de vacinação e no contrato
> de adoção, qualquer leitor de pet shop ou canil o lê, e o padrão ISO tem faixas previsíveis
> por país e fabricante. Como senha ele não vale nada. Como identificador é excelente — e é
> exatamente para isso que entra aqui. O que credencia o acesso é a autenticação do veterinário.

---

### Nível 2 — Histórico completo

| # | Regra |
|---|---|
| **C8** | O agendamento (`POST /agendamentos`) cria a autorização. Não há endpoint de solicitação |
| **C9** | O corpo carrega `consentimentoHistorico` **explícito**, e a tela mostra o que está sendo liberado, para quem e por quanto tempo |
| **C10** | O campo chega `false` por padrão. **Consentimento pré-marcado não é consentimento** — não é escolha do frontend, é regra da API |
| **C11** | Recusar é permitido: o agendamento acontece do mesmo jeito, com os níveis 0 e 1 |
| **C12** | A autorização é **por animal, para a clínica** — não para o veterinário. Quem atende no dia pode não ser quem estava agendado |
| **C13** | **Vigência: até 2 anos após o último atendimento naquela clínica.** A autorização vive enquanto a relação viver — continua indo, ela se mantém; parou de ir, ela expira sozinha |
| **C14** | Cancelar agendamento só revoga se **nunca** houve atendimento ali. Cancelar uma consulta depois de três anos de relação não revoga nada |
| **C15** | O tutor revoga a qualquer momento, sem justificar |
| **C16** | Revogar **não** apaga o que o veterinário escreveu — registro clínico não se desfaz, e C0a/C0b continuam valendo |
| **C17** | Alterar dados cadastrais do animal (X3) não vem junto: o veterinário propõe a correção e o tutor confirma |

C13 resolve sozinha o conflito com **R21**: um retorno de castração, de oncologia ou de exame
de imagem passa dos 30 dias com frequência, e uma vigência curta faria o veterinário perder o
acesso no meio do cuidado — o oposto do tema do Challenge.

---

### O que `GET /animais/{id}/historico` devolve

O objeto central do fluxo, definido para não sair diferente em cada tela.

| Bloco | Nível | Conteúdo |
|---|---|---|
| Identificação | 0 | nome, espécie, raça, porte, sexo, nascimento, idade calculada, microchip |
| Alertas | 1 | alergias, condições crônicas, medicação contínua — com a origem de cada um |
| Vitais | 1 | série de `peso_kg` ao longo do tempo, castração |
| Imunização | 1 | vacinas aplicadas, a partir dos eventos de tipo `VACINA` |
| Linha do tempo | 2 | eventos em ordem, **marcando quais são desta clínica e quais de outras** |
| Documentos | 2 | anexos, com origem interna ou externa |
| Desfechos | 2 | resultado clínico de cada atendimento |
| Tutor | 2 | dados completos. No nível 1, só o telefone de emergência |

A marcação "desta clínica × de outras" é o que dá sentido a C0b: sem consentimento o
veterinário vê só a fatia dele; com consentimento, a linha inteira.

---

### Auditoria e transparência

| # | Regra |
|---|---|
| **C18** | Todo acesso de nível 1 ou 2 é registrado — **uma linha por (veterinário, animal, dia)**, com contador |
| **C19** | O tutor consulta a qualquer momento quais clínicas têm acesso e quem leu o quê |
| **C20** | Lembrete anual ao tutor: "estas clínicas têm acesso aos seus animais" — mantém a decisão viva sem pedir de novo |

C18 é decisão de implementação com efeito real: uma linha por requisição viraria 40 registros
por atendimento, e a auditoria ficaria maior que o resto do banco e ilegível para o tutor. O que
importa é "a Dra. Camila leu o histórico do Thor em 12/09", não quantas vezes rolou a página.

---

### Quebra de vidro

| # | Regra |
|---|---|
| **C21** | Emergência sem agendamento e sem consentimento acessa o **nível 2** com: motivo obrigatório, janela de **12 horas**, vínculo com a clínica do veterinário, notificação imediata ao tutor e marcação destacada na auditoria |
| **C22** | Teto por veterinário e por período. Acionar com frequência é sinalizado ao admin da plataforma |

Com os três níveis, a quebra de vidro cobre um vão bem menor — o nível 1 já resolve a maior
parte da urgência clínica. Por isso ela pode ser apertada sem travar o atendimento.

Sem C21 e C22 nesse formato, "qualquer veterinário com um campo de texto livre" seria uma porta
aberta com livro de visitas, e anularia C8–C17.

---

### Por que este desenho fecha com Compliance

Cada nível tem base legal distinta e declarada: execução do atendimento (0), proteção da vida
do animal (1), consentimento informado (2). Todos são auditados, o tutor enxerga tudo o que
acontece e revoga o que é revogável. É o desenho que a LGPD pede para dado sensível — e o
argumento fica de pé na avaliação oral, que "o histórico é público" não ficaria.

### Endpoints

```
GET   /api/v1/animais/resumo                       ?microchip=          (nível 1)
GET   /api/v1/animais/{id}/historico                                    (níveis 0-2)
GET   /api/v1/animais/{id}/acessos
GET   /api/v1/autorizacoes/minhas
POST  /api/v1/autorizacoes/{id}/revogar
POST  /api/v1/animais/{id}/alertas                 { tipo, descricao }
POST  /api/v1/animais/{id}/acesso-emergencial      { motivo }           (C21)
POST  /api/v1/animais/{id}/correcoes               { campo, valorSugerido }
PATCH /api/v1/correcoes/{id}                       { decisao }
```

`POST /agendamentos` carrega `consentimentoHistorico` e é o que cria a autorização de nível 2.

### Onde isso é implementado

Em **um lugar só**: `SegurancaService`. O método `temVisaoAmpla()` (`SegurancaService.java:111`)
deixa de devolver `true` para `VETERINARIO` e passa a resolver o nível de acesso — C0a/C0b,
depois autorização vigente. Toda a estrutura de `@PreAuthorize` já existente continua valendo
sem alteração nos controllers.

Isso é o que torna a mudança viável apesar do tamanho — mas ver a Parte 11, porque **quebra
testes**.

---

## 7 — Fluxo D — Documentos e histórico clínico

Resolve X6. É o insumo do Fluxo C: sem documento, a autorização não protege nada relevante.

Documento é sempre **nível 2** — nunca entra no resumo de segurança. Um laudo tem nome de
clínica, data, diagnóstico e às vezes o nome do tutor; nada disso cabe numa consulta por
microchip. O que o nível 1 aproveita dos documentos é **zero**: os alertas vêm de `AlertaClinico`
(E12), que é estruturado justamente para isso.

| # | Regra |
|---|---|
| **DOC1** | O tutor anexa documento a animal **dele** |
| **DOC2** | Tipos: `PRONTUARIO`, `VACINA`, `EXAME`, `CIRURGIA`, `RECEITA`, `LAUDO`, `OUTRO` |
| **DOC3** | Origem: `INTERNA` (gerada na plataforma) ou `EXTERNA` (outra clínica) |
| **DOC4** | Documento externo carrega `dataDocumento` e, opcionalmente, o nome da clínica de origem — texto livre, porque ela não está na plataforma |
| **DOC5** | O veterinário anexa documento a atendimento que ele conduziu (C0a) |
| **DOC6** | Documento é **nível 2**: ler exige autorização vigente, salvo C0a e C0b |
| **DOC7** | O tutor remove documento que **ele** anexou. Documento gerado em atendimento não se remove — arquiva |
| **DOC8** | Formatos: PDF, JPG, PNG. Teto de tamanho por arquivo |
| **DOC9** | O arquivo **não** vai para o banco. Vai para o Azure Blob Storage; a entidade guarda a URI |
| **DOC10** | A URI nunca é pública — o download passa pela API, que reavalia DOC6 a cada requisição |

DOC10 é o detalhe que decide se o consentimento vale alguma coisa: uma URL de blob assinada e
vazada contorna toda a Parte 6.

### Endpoints

```
POST   /api/v1/animais/{id}/documentos     multipart
GET    /api/v1/animais/{id}/documentos
GET    /api/v1/documentos/{id}/arquivo
DELETE /api/v1/documentos/{id}
```

---

## 8 — Fluxo R — Retorno e falta

Já mapeado; **o schema existe desde a V5** (`status_evento`, `data_retorno_previsto`,
`evento_origem_id`, `peso_kg`). Falta o mapeamento na entidade e as regras.

| # | Regra |
|---|---|
| **R1** | Data futura nasce `AGENDADO`; data de hoje ou passada nasce `REALIZADO` |
| **R2** | `REALIZADO` exige `data <= hoje` |
| **R3** | `FALTOU` exige `data < hoje` |
| **R4** | `CANCELADO` é terminal |
| **R5** | Nada volta para `AGENDADO` |
| **R6** | `peso_kg` só em evento `REALIZADO` |
| **R7** | Variação de peso > 20% vs. a última aferição → **aviso**, não bloqueio |
| **R8** | Colisão de agenda do veterinário → 409 *(unificada com A6)* |
| **R9** | `RETORNO` exige `evento_origem_id` |
| **R10** | A origem tem de ser do mesmo animal |
| **R11** | A origem tem de estar `REALIZADA` |
| **R12** | A data do retorno é posterior à da origem |
| **R13** | Sem ciclos `A → B → A` — o banco só barra a auto-referência (`chk_evento_origem_propria`) |
| **R14** | Um retorno em aberto por evento de origem |
| **R15** | `data_retorno_previsto` posterior à `data` do evento |
| **R16** | `data_retorno_previsto` só em evento `REALIZADO` |
| **R17** | **Retorno vencido** = `data_retorno_previsto < hoje` **e** não há `RETORNO` realizado apontando para ele |
| **R18** | Varredura: `AGENDADO` com `data < hoje` vira `FALTOU` |
| **R19** | Evento com pagamento `PAGO` não é cancelado nem removido |
| **R20** | `REALIZADO` → `FALTOU` só dentro de 24h, como correção |
| **R21** | O retorno previsto pelo vet aparece para o tutor como sugestão de agendamento no Fluxo A |

R21 é o que fecha o ciclo do tema do Challenge: o vet prevê, o sistema lembra, o tutor marca.

```
POST /api/v1/eventos-clinicos/{id}/concluir     { pesoKg, descricao, desfecho, dataRetornoPrevisto }
POST /api/v1/eventos-clinicos/{id}/retorno      { data, hora, veterinarioId }
GET  /api/v1/eventos-clinicos/retornos-vencidos
POST /api/v1/eventos-clinicos/marcar-faltas
```

---

## 9 — Fluxo P — Cobrança

O valor vem de `Servico.preco`, gravado em `evento_clinico.servico_id` no agendamento.

| # | Regra |
|---|---|
| **P1** | `dataPagamento` obrigatória **só** em `PAGO` — hoje é `@NotNull` sempre, e um `PENDENTE` precisa inventar uma data |
| **P2** | `dataPagamento >= data` do evento |
| **P3** | `dataPagamento` não futura — ✅ já existe (`@PastOrPresent`) |
| **P4** | Evento `CANCELADO` não recebe pagamento |
| **P5** | Evento `FALTOU` pode gerar taxa de no-show *(decisão DEC3)* |
| **P6** | Evento `AGENDADO` aceita pré-pagamento *(decisão DEC4)* |
| **P7** | Σ dos `PAGO` ≤ `Servico.preco` do evento |
| **P8** | Pagamento parcial permitido *(decisão DEC5)* |
| **P9** | `PAGO` não é removido — é estornado |
| **P10** | `REEMBOLSADO` exige justificativa em `observacao` |
| **P11** | Estorno só a partir de `PAGO` |
| **P12** | Cancelar evento com pagamento `PAGO` exige estorno antes — é R19 pelo outro lado |
| **P13** | **Inadimplência** = evento `REALIZADO` + Σ `PAGO` < preço + N dias corridos |
| **P14** | `statusPagamento` **sai** do PUT e do PATCH |

Transições: `PENDENTE` → `PAGO` ou `CANCELADO`; `PAGO` → `REEMBOLSADO`. `CANCELADO` e
`REEMBOLSADO` são terminais. Nada volta para `PENDENTE`.

> **P14 não é detalhe.** `PagamentoPatchRequest` expõe `statusPagamento` hoje
> (`PagamentoPatchRequest.java:47`). Enquanto ele estiver lá, P1–P13 são decorativas: basta um
> `PATCH {"statusPagamento":"PAGO"}` para contornar todas.

```
POST /api/v1/pagamentos/{id}/confirmar   { formaPagamento, dataPagamento }
POST /api/v1/pagamentos/{id}/estornar    { motivo }
GET  /api/v1/eventos-clinicos/{id}/saldo
GET  /api/v1/tutores/{id}/extrato
GET  /api/v1/pagamentos/inadimplencia    ?diasMinimos=30
```

---

## 10 — Fluxo N — Clínica, planos e painel

Resolve X7, X8, X9, X10, X11. É a monetização — e o que menos cabe até 12/09.

### Administração da clínica (X7)

| # | Regra |
|---|---|
| **N1** | `Usuario` ganha vínculo opcional com `Clinica` |
| **N2** | O perfil `ADMIN` se divide: `ADMIN_PLATAFORMA` (hoje) e `ADMIN_CLINICA` (novo) |
| **N3** | `ADMIN_CLINICA` gerencia serviços, raças, veterinários e assinatura — **só da própria clínica** |
| **N4** | Toda clínica tem exatamente um administrador principal; ele pode delegar |
| **N5** | Criar clínica continua sendo de `ADMIN_PLATAFORMA` — é ele quem vende a licença |

### Licença e planos (X9)

| # | Regra |
|---|---|
| **N6** | Clínica sem assinatura `ATIVA` fica em modo leitura: não recebe agendamento novo |
| **N7** | O plano determina quais recursos do painel respondem; sem plano, 402 ou recurso oculto |
| **N8** | Assinatura vencida entra em carência antes de bloquear |

### Painel (X10, X11)

| # | Regra |
|---|---|
| **N9** | `UsuarioAutenticado` ganha `getVeterinarioId()`, espelhando `getTutorId()` — **destrava X10** |
| **N10** | O evento nasce atribuído a quem o registrou; atribuir a outro vet exige perfil de administração |
| **N11** | Métricas do vet: atendimentos no período, taxa de falta, taxa de retorno cumprido, desfechos |
| **N12** | Métricas da clínica: marcadas × realizadas × faltadas, faturamento, ticket médio, ocupação da agenda |
| **N13** | Leitura competitiva por raça e por serviço, usando `desfecho` |
| **N14** | Recorte com menos de **k** indivíduos não é exibido (k-anonimato, sugestão k=5) |

N14 evita que "sucesso e fracasso por raça" vire identificação indireta de um paciente único.

---

## 11 — O que quebra no que já existe

Honestidade sobre o custo. Estas mudanças não são aditivas.

| # | Ruptura | Impacto |
|---|---|---|
| **B1** | `temVisaoAmpla()` deixa de valer para `VETERINARIO` e vira resolução de nível (0, 1 ou 2) | **Toda listagem muda de comportamento.** Os testes que hoje passam justamente porque o vet vê tudo vão falhar — e é o comportamento *correto* falhando contra a expectativa *antiga* |
| **B2** | `POST /eventos-clinicos` passa a aceitar `TUTOR` | Regra de rota em `SecurityConfig` + validação nova |
| **B3** | `statusPagamento` sai dos DTOs de PUT/PATCH | Quebra contrato de API — quem já usa vai receber 400 |
| **B4** | `ADMIN` se divide em dois perfis | Migration com `UPDATE` em `usuario.perfil` e mudança no `chk_usuario_perfil` |
| **B5** | `dataPagamento` deixa de ser obrigatória sempre | Muda validação e possivelmente o schema |
| **B6** | O auto-cadastro passa a criar o `Tutor` junto | Corrige X12; muda o corpo de `RegistroRequest` |

> **B1 é o risco técnico central do documento.** São **130 testes verdes** hoje, e vários
> passam justamente porque o veterinário enxerga tudo. Inverter isso derruba uma parte deles, e
> separar "quebrou porque a regra mudou" de "quebrou porque errei" é trabalho de leitura, não de
> digitação.
>
> É por isso que B1 está isolado na **onda 3** do sequenciamento, com as duas mitigações da
> [Parte 12](#12--sequenciamento): reescrever os testes de acesso **antes** de tocar no
> `SegurancaService`, e só entrar na onda 3 com a onda 2 commitada e verde.

---

## 12 — Sequenciamento

**Faltam 13 dias para 12/09** e há **50 pontos** da Sprint 3 em aberto — 30 de frontend, 20
dos dois fluxos não-CRUD. O time avaliou o prazo em 30/08 e decidiu ir com o escopo completo.
A ordem abaixo não corta escopo: ela ordena por **dependência**, para que nada seja começado
antes do que ele precisa.

### Onda 1 — a base (nada depende de decisão pendente)

| Item | Por quê primeiro |
|---|---|
| **X12 e X13** — os defeitos do auto-cadastro | X13 expõe dado de terceiro; X12 impede o tutor de cadastrar o próprio animal. O Fluxo A não funciona sem os dois |
| **N9** — `getVeterinarioId()` em `UsuarioAutenticado` | Uma linha. Destrava A6, R8, C0a e o painel inteiro |
| **E1 `Servico`** + **E2 `Disponibilidade`** + **E8 `Bloqueio`** — migration **V6** | Base do Fluxo A. Traz de brinde o valor do atendimento, que o Fluxo P precisa |
| `microchip` e `castrado` em `animal` — mesma **V6** | Coluna barata e sem dependência. O nível 1 do Fluxo C precisa dela, e a V6 já vai abrir a tabela |

### Onda 2 — os dois fluxos da rubrica

| Item | Por quê aqui |
|---|---|
| **Fluxo A — agendamento** | 1º fluxo não-CRUD. Depende inteiramente da onda 1 |
| **Fluxo R — retorno e falta** | 2º fluxo não-CRUD. Schema pronto desde a V5; só falta mapear a entidade e escrever as regras. É o mais barato dos cinco |

Fechados esses dois, **os 20 pontos da Sprint 3 estão garantidos** — o resto é ganho.

### Onda 3 — o consentimento

| Item | Por quê depois de A |
|---|---|
| **E12 `AlertaClinico`** — pode ir junto na **V6** | É o conteúdo do nível 1, e o nível 1 não depende de consentimento nenhum: pode entrar antes do resto do Fluxo C |
| **E3 `AutorizacaoAcesso`** + **E10 `AcessoHistorico`** — migration **V7** | |
| **Fluxo C — consentimento** | O agendamento é o que cria a autorização (C1). Sem o Fluxo A de pé, não há por onde consentir |
| **B1** — inverter `temVisaoAmpla()` | **É aqui que os testes quebram.** Ver Parte 11 |

### Onda 4 — o histórico completo

| Item | Depende de |
|---|---|
| **E4 `Documento`** + storage no Azure Blob — migration **V8** | infraestrutura |
| **Fluxo D — documentos** | do Fluxo C, para saber quem pode ler |
| **`desfecho`** em `evento_clinico` (X11) | nada — pode ser antecipado para a V6 se sobrar espaço |

### Onda 5 — a monetização

| Item | Depende de |
|---|---|
| **E5 `Plano`** + **E6 `Assinatura`** + **E9 `Usuario.clinica`** — migration **V9** | |
| **Fluxo N — admin da clínica, planos e painel** | de C e D para ter o que exibir, e de `desfecho` para a leitura competitiva |

### Onde está o risco

Não é volume — é a **onda 3**. Inverter `temVisaoAmpla()` derruba parte dos 130 testes verdes,
e separar "quebrou porque a regra mudou" de "quebrou porque errei" é trabalho de leitura, não
de digitação. Duas mitigações valem a pena:

1. **Reescrever os testes de acesso do veterinário antes de mexer no `SegurancaService`**, com
   as expectativas novas. Eles nascem vermelhos e ficam verdes quando a regra entra — em vez
   de uma suíte vermelha de origem ambígua.
2. **Fazer a onda 3 depois que a onda 2 estiver commitada e verde.** Assim há um ponto de
   retorno conhecido a qualquer momento.

### Fora de escopo declarado

`RacaAtendida` (E7) pode virar uma lista dentro de `Servico` e não precisa de entidade
própria. Fica registrado para não ser reinventado depois.

---

## 13 — Decisões

### Fechadas em 30/08/2026

| # | Decisão | Onde entrou |
|---|---|---|
| ~~DEC1~~ | **O consentimento é o agendamento** — sem ciclo de pedido e aprovação | A15, C1–C3 |
| ~~DEC2~~ | Autorização por **clínica**, não por veterinário individual | C4 |
| ~~DEC7~~ | Prazo do nível 2: até **2 anos após o último atendimento** naquela clínica | C13 |
| ~~DEC8~~ | **Quebra de vidro entra** — obrigatória, com janela de 12h e teto | C21, C22 |
| ~~DEC12~~ | O tutor **pode** agendar recusando o consentimento — fica com os níveis 0 e 1 | C11 |
| ~~DEC13~~ | Quebra de vidro: qualquer veterinário, sem aprovação prévia, mas com janela curta, vínculo de clínica, notificação e teto | C21, C22 |
| ~~DEC14~~ | **Acesso em três níveis** — o microchip identifica, o CRMV credencia o nível 1, o tutor autoriza o nível 2 | Parte 6 inteira |
| — | Quem marca `status_evento` e quando *(decisão 4 do backlog)* | R1, A11 |
| — | Quem registra o óbito *(decisão 5 do backlog)* | `desfecho` em `POST /concluir` |

### Ainda abertas

| # | Decisão | Trava | Recomendação |
|---|---|---|---|
| **DEC3** | Evento `FALTOU` gera cobrança? | P5 | Sim, com taxa definida pela clínica |
| **DEC4** | Pré-pagamento em evento `AGENDADO`? | P6 | Sim — dá sentido a A10 e reduz no-show |
| **DEC5** | Pagamento parcial? | P7, P8 | Sim — parcelamento é comum em cirurgia |
| **DEC6** | Antecedência mínima para agendar (`N`) e para cancelar (`M`) | A10, A12 | 2h e 24h |
| **DEC10** | `ADMIN` vira dois perfis ou ganha escopo? | N2, B4 | Dois perfis — escopo implícito é o que produz vazamento silencioso |
| **DEC11** | Storage dos documentos | Fluxo D | Azure Blob, alinhado à migração de infra já decidida |
| **DEC15** | Teto de consultas de nível 1 por veterinário/dia | C6 | 30 — acima disso deixa de parecer atendimento |
| **DEC16** | O tutor pode desligar o nível 1? | C5 | Sim, com aviso do que perde. Forçar seria paternalista; o dado é dele |

Nenhuma das abertas bloqueia a **onda 1** nem a **onda 2** do sequenciamento. DEC15 e DEC16
precisam estar fechadas antes da onda 3.

---

## Referências

| Documento | Relação |
|---|---|
| [07-backlog.md](07-backlog.md) | Status vigente do repositório — o "de onde partimos" |
| [02-sprint-3.md](02-sprint-3.md) | Origem da exigência dos dois fluxos não-CRUD |
| [04-dependencias-externas.md](04-dependencias-externas.md) | O que Database, DevOps e Compliance esperam deste backend |
| [`../docs/`](../docs/) | Documentação técnica do que já existe |
