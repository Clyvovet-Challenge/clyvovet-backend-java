# Plano de implementação — Sprint 3

> **Este documento não consta nos PDFs do Challenge.** É um backlog derivado dos
> requisitos das specs 01–04, mapeado ao código existente. Serve como proposta de
> execução, não como requisito oficial.

**Prazo:** 12/09/2026 · **Hoje:** 07/08/2026 · **Janela:** ~5 semanas
**Mentoria presencial Clyvo:** 21/08/2026 — boa data para validar os fluxos escolhidos

---

## Decisões em aberto

Cinco escolhas que travam o resto do trabalho e valem ser fechadas pelo grupo antes de
codar:

| # | Decisão | Opções | Observação |
|---|---|---|---|
| 1 | Tecnologia do frontend | Thymeleaf (server-side, dentro do Spring) · SPA separada | O requisito está na disciplina de Java e cita "formulários"; Thymeleaf é a leitura mais direta e evita segundo repositório |
| 2 | Quais 2 fluxos não-CRUD | ver sugestões abaixo | Item de 20 pts + o que diferencia a nota |
| 3 | Banco em nuvem | Oracle FIAP · PostgreSQL · Azure SQL · MySQL | H2 deixa de ser aceito em nuvem (DevOps S3) |
| 4 | Autenticação do mobile | Firebase (mobile resolve sozinho) · JWT emitido por esta API | Se for JWT, o Spring Security precisa dos dois modos |
| 5 | Modelo de usuário | Entidade `Usuario` nova · reaproveitar `Tutor`/`Veterinario` | Afeta as migrations Flyway |

---

## Fluxos candidatos (requisito 4, 20 pts)

Precisam ser **não-CRUD** e coerentes com o tema — continuidade do cuidado, do modelo
episódico para o preventivo. Cada um atravessa mais de uma entidade e tem regra de
negócio real.

| Fluxo | Entrada | Processamento | Saída |
|---|---|---|---|
| **Agendamento de retorno** | Evento clínico concluído | Calcula data sugerida por `TipoEvento`, valida agenda do veterinário e da clínica, impede conflito de horário | Novo `EventoClinico` agendado + confirmação ao tutor |
| **Carteira de vacinação e alertas** | Histórico de eventos `VACINA` do animal | Compara com calendário vacinal por espécie/idade, identifica vacinas vencidas e próximas | Painel de pendências + alerta ao tutor |
| **Fechamento financeiro do atendimento** | Evento clínico + itens | Calcula valor, aplica regra de status, impede pagamento duplicado, concilia pendências | `Pagamento` gerado + extrato da clínica |
| **Linha do tempo clínica do pet** | `animalId` | Agrega eventos, pagamentos e vacinas em ordem cronológica, deriva indicadores | Histórico longitudinal unificado |

O agendamento de retorno e a carteira de vacinação são os que mais respondem
diretamente ao problema macro descrito no desafio ("esquecimento", "dificuldade de
continuidade", "perda de recorrência").

---

## Backlog

### Bloco A — Fundação (semana 1)

| # | Tarefa | Arquivos |
|---|---|---|
| A1 | Adicionar dependências: `flyway-core`, `spring-boot-starter-security`, `spring-boot-starter-thymeleaf` | [`pom.xml`](../pom.xml) |
| A2 | Converter o schema atual em `V1__schema_inicial.sql` | novo `resources/db/migration/` |
| A3 | Converter o seed em `V2__seed_inicial.sql`, com ≥ 5 registros por tabela (exigência de Database) | idem |
| A4 | Trocar `ddl-auto` por `flyway.enabled=true` nos perfis | [`application-*.properties`](../src/main/resources/) |
| A5 | Externalizar credenciais para variáveis de ambiente (−20 pts em DevOps se ficarem no código) | [`application-oracle.properties`](../src/main/resources/application-oracle.properties) |
| A6 | Corrigir divergências que quebram persistência (`REEMBOLSADO`, `@Size` de `crmv`/`telefone`, `dataPagamento`) | ver [`../docs/07-pendencias-e-divergencias.md`](../docs/07-pendencias-e-divergencias.md) |

Vale fazer A6 antes das migrations: cada divergência corrigida depois vira uma
migration extra.

### Bloco B — Segurança (semana 2) — 30 pts

| # | Tarefa | Arquivos |
|---|---|---|
| B1 | Entidade `Usuario` + enum `Perfil` (`TUTOR`, `VETERINARIO`) e migration correspondente | novo em `model/` |
| B2 | `UserDetailsService` + `PasswordEncoder` (BCrypt) | novo pacote `security/` |
| B3 | `SecurityFilterChain` com regras por perfil e login por formulário | novo `config/SecurityConfig.java` |
| B4 | Vincular `Usuario` a `Tutor`/`Veterinario` | migration + entidades |
| B5 | *(condicional à decisão 4)* Filtro JWT para o app mobile | `security/` |

Dois perfis com permissões diferentes e proteção de rotas por perfil são explicitamente
cobrados — não basta ter login.

### Bloco C — Fluxos de negócio (semanas 2–3) — 20 pts

| # | Tarefa |
|---|---|
| C1 | Implementar o fluxo 1 escolhido: service com a regra, validações e exceções de negócio |
| C2 | Implementar o fluxo 2 escolhido |
| C3 | Exceção `RegraDeNegocioException` + handler no `GlobalExceptionHandler` (hoje só trata validação e 404) |
| C4 | Testes unitários das regras — sustentam a avaliação oral e o CI da Sprint 4 |

### Bloco D — Frontend (semanas 3–4) — 30 pts

| # | Tarefa |
|---|---|
| D1 | Layout base Thymeleaf com fragments (evita o desconto por DRY) |
| D2 | Tela de login |
| D3 | Painel do tutor: meus pets, histórico, pendências |
| D4 | Painel do veterinário/clínica: agenda, pacientes, registro de evento |
| D5 | Telas dos dois fluxos não-CRUD |
| D6 | Validação de formulário integrada ao Bean Validation, com mensagens de erro na tela |

Cada página que não carrega ou link quebrado custa −5. Vale um passe final clicando em
tudo.

### Bloco E — Entrega (semana 5)

| # | Tarefa |
|---|---|
| E1 | README com instalação, execução e acesso (exigência explícita) |
| E2 | Vídeo de demonstração ≤ 10 min |
| E3 | Revisão de código contra as penalidades: SOLID, DRY, Clean Code, comentários no lugar de refatoração |
| E4 | Preparar a avaliação oral — cada integrante domina os trechos que escreveu |
| E5 | Atualizar [`../docs/`](../docs/) com as novas camadas |

---

## Riscos

| Risco | Efeito | Mitigação |
|---|---|---|
| Fluxos escolhidos tarde demais | Bloco C e D comprimidos; 20 pts em risco | Fechar a decisão 2 antes da mentoria de 21/08 |
| Duplicação nas telas | Penalidade DRY cumulativa, −5 por ocorrência | Fragments Thymeleaf desde a primeira tela (D1) |
| Frontend consumindo tempo do fluxo | Fluxos viram CRUD com tela | Blocos C e D em paralelo, por integrante |
| Trabalho concentrado em 1–2 pessoas | Avaliação oral individual; −10 por falta de colaboração na S4 | Dividir por fluxo/tela, commits distribuídos |
| Migração de banco na reta final | Quebra o deploy da DevOps | Bloco A na semana 1 |
