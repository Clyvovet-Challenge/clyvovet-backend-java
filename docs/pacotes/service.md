# `service` — as regras de negócio

`src/main/java/br/com/fiap/clyvovet/service` · 18 classes

É onde o sistema decide. Um service orquestra repositório, mapeamento e cache;
o que ele **não** faz é conversar com HTTP (isso é do controller), copiar campo
a campo entre DTO e entidade (é do [mapper](mapper.md)) nem decidir "quem
enxerga o quê" (é do [`SegurancaService`](security.md)).

Todo service da aplicação é `@Transactional(readOnly = true)` na classe, e cada
método que escreve reabre com `@Transactional` próprio. O padrão é ao contrário
do usual de propósito: leitura é o caso comum, e um método de escrita que
esqueça a anotação falha na hora, em vez de gravar sem transação.

---

## Os arquivos, por assunto

### CRUD dos seis recursos

Mesmo esqueleto nos seis: `listarTodos` paginado com filtros, `buscarPorId`,
`criar`, `atualizar`, `atualizarParcialmente`, `deletar`.

| Arquivo | O que faz |
|---|---|
| `TutorService.java` | CRUD de tutor |
| `AnimalService.java` | CRUD de animal. **A listagem é recortada**: o tutor logado só enxerga os próprios |
| `ClinicaService.java` | CRUD de clínica |
| `VeterinarioService.java` | CRUD de veterinário |
| `EventoClinicoService.java` | CRUD de atendimento, recortado por tutor e por clínica |
| `PagamentoService.java` | CRUD de pagamento, recortado do mesmo jeito |

### Os quatro fluxos do domínio

| Arquivo | Regras | O que faz |
|---|---|---|
| `AgendamentoService.java` | A1–A15 | Marcar, cancelar e "meus agendamentos". As validações são chamadas **em ordem de custo, as mais baratas primeiro**: serviço ativo, veterinário da clínica, antecedência mínima e só então a colisão de agenda, que é a consulta cara |
| `RetornoService.java` | R1–R21 | Concluir o atendimento, marcar retorno, listar vencidos e a varredura de faltas. É o fluxo que registra que a consulta *devia ter tido sequência e não teve* |
| `HistoricoService.java` | C1–C22 | Os três níveis de acesso ao prontuário, os tetos de leitura e a quebra de vidro |
| `CobrancaService.java` | P1–P14 | Confirmar, estornar, saldo, inadimplência e extrato |

### Apoio dos fluxos

| Arquivo | O que faz |
|---|---|
| `AgendaService.java` | Responde **"este veterinário está livre neste intervalo?"** cruzando grade semanal × bloqueios × atendimentos já marcados. Classe separada porque a mesma pergunta vem de dois lugares — o agendamento, sobre *um* horário, e a busca de vagas, sobre todos os de um período. Juntos, um dos dois reimplementaria a regra |
| `AgendaCadastroService.java` | Onde a grade é **escrita**: faixas de disponibilidade e bloqueios. Separado do leitor acima, que é chamado a cada agendamento e a cada busca de vagas, para mantê-lo livre de dependências de escrita |
| `AutorizacaoService.java` | O ciclo de vida do consentimento. **Não há endpoint de concessão** — a autorização nasce dentro do agendamento; o que sobra aqui é estender, revogar e listar |
| `AlertaService.java` | Alergias, condições crônicas e medicação contínua — o conteúdo do nível 1. Tanto o tutor quanto o veterinário registram; o que muda é a **origem**, derivada do perfil de quem grava e nunca aceita do corpo |
| `ServicoService.java` | O catálogo da clínica: o que ela oferece, por quanto e em quanto tempo |
| `AuditoriaService.java` | A leitura dos tetos, para revisão do ADMIN |

### Identidade

| Arquivo | O que faz |
|---|---|
| `AuthService.java` | Provar quem é o usuário e emitir tokens: login, refresh, logout |
| `UsuarioService.java` | Cadastrar: o auto-cadastro público e a criação com perfil arbitrário pelo ADMIN |

Estavam na mesma classe por compartilharem o prefixo `/auth` na URL. São
responsabilidades com motivos de mudança diferentes — uma muda quando a política
de credencial muda, a outra quando o cadastro ganha campo ou regra de vínculo.

---

## Cinco decisões que valem a pena entender

### 1. A chave do cache inclui quem está perguntando

```java
@Cacheable(value = "animais",
        key = "#nome + '-' + #especie + '-' + @seguranca.recorte().chaveDeCache() + '-' + #pageable")
public Page<AnimalResponse> listarTodos(String nome, String especie, Pageable pageable) { ... }
```

Sem o recorte na chave, a primeira listagem de um tutor seria servida a qualquer
outro que usasse os mesmos filtros e paginação — **vazamento de dados entre
contas**.

E `#pageable` inteiro, não `pageNumber` e `pageSize`: assim o *sort* entra
junto. Antes, `?sort=nome,asc` e `?sort=nome,desc` colidiam na mesma chave, e a
segunda chamada recebia o resultado da primeira, na ordem errada.

A chave e o filtro da consulta saem hoje da **mesma fonte**, o
`RecorteDeAcesso`. Enquanto cada service montava os dois à mão, era possível — e
aconteceu — a consulta recortar por clínica e a chave não.

### 2. `AgendaService.Janela` é onde texto vira hora

As horas são gravadas como `VARCHAR2(5)`. Comparar como texto funciona por
acidente do formato de largura fixa e quebra no dia em que alguém gravar `9:00`.

A conversão mora nas fábricas de `Janela`, e em nenhum outro lugar:

```java
Janela.de("08:00", "12:00")            // um intervalo [início, fim)
Janela.deDuracao("14:30", 30)          // início + duração do serviço
Janela.hora("09:00")                   // o único LocalTime.parse de src/main
```

Enquanto a conversão estava espalhada, `LocalTime.parse` aparecia em dezessete
pontos e a regra de colisão chegou a ser reescrita à mão numa segunda
implementação. Hoje, mudar o formato da hora toca uma classe.

### 3. `concluir()` é a única porta para `REALIZADO`

Se o status fosse editável por PATCH, bastaria um `{"statusEvento":"REALIZADO"}`
para marcar como realizado um atendimento futuro — e as regras R1 a R21
existiriam só no papel. Quem responde se a transição é permitida é o enum
[`StatusEvento`](model.md), não um `if` local.

### 4. O aviso de variação de peso avisa, não bloqueia

Um filhote que sai de 2 kg para 3 kg variou 50% e está saudável; um gato adulto
que perde 25% pode estar com doença renal. A regra não distingue os dois casos —
o veterinário distingue.

E o aviso volta na **resposta**, não só no log: alerta clínico que o veterinário
não vê não é alerta.

### 5. `marcarFaltas()` é endpoint, não `@Scheduled`

Sem a varredura, a taxa de falta nunca sai de zero — ninguém volta ao sistema
para registrar que o pet não apareceu. E agendador em aplicação com mais de uma
instância dispara em todas ao mesmo tempo, o que numa varredura de escrita
significa contenção e faltas marcadas em duplicidade.

---

## Onde as coisas *não* estão

| Se você procura | Vá para |
|---|---|
| A cópia campo a campo DTO ↔ entidade | [`mapper`](mapper.md) |
| "Este tutor pode ver este pet?" | [`SegurancaService`](security.md) |
| A consulta SQL/JPQL | [`repository`](repository.md) |
| O status HTTP da resposta | [`controller`](controller.md) e [`GlobalExceptionHandler`](exception.md) |
| O texto completo das regras A/R/C/P | [../08-seguranca.md](../08-seguranca.md) |
