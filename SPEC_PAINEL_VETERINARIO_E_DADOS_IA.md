# Spec — Painel do Veterinário e Dados + IA

**Data:** 25/08/2026 · **Status:** especificação, nada implementado · **Alvo:** backend Java (este repositório)

Documento derivado da leitura completa do código, do DDL das migrations e da documentação
em [`docs/`](docs/). Serve de base para gerar os prompts de implementação camada por camada.

> **O que este documento não é:** não é um plano de sprint, não decide prioridade e não
> escreve código. Onde o código atual não deixa um padrão claro, a suposição está
> declarada na [Parte VII](#parte-vii--suposições-declaradas) — nenhuma foi assumida em
> silêncio.

---

## Índice

| Parte | Conteúdo |
|---|---|
| [I](#parte-i--análise-do-código-existente) | Análise do código existente — arquitetura, convenções, segurança, testes |
| [II](#parte-ii--inventário-de-dados-o-que-existe-x-o-que-falta) | Inventário: qual dado cada feature exige e se ele existe hoje |
| [III](#parte-iii--módulo-1-painel-do-veterinário) | Módulo 1 — Painel do Veterinário |
| [IV](#parte-iv--módulo-2-dados--ia) | Módulo 2 — Dados + IA |
| [V](#parte-v--dependências-entre-os-módulos) | Dependências entre os dois módulos |
| [VI](#parte-vi--pontos-de-atenção-e-riscos-técnicos) | Pontos de atenção e riscos técnicos |
| [VII](#parte-vii--suposições-declaradas) | Suposições declaradas |
| [VIII](#parte-viii--perguntas-em-aberto) | Perguntas em aberto |
| [IX](#parte-ix--ordem-sugerida-de-implementação) | Ordem sugerida de implementação |

---

# Parte I — Análise do código existente

## 1. Padrão arquitetural

**MVC em camadas clássico. Não é DDD** — não há agregados, value objects (além do
`@Embeddable Endereco`), repositórios de domínio nem eventos de domínio. Os pacotes são
organizados **por camada técnica**, não por feature:

```
br.com.fiap.clyvovet
├── config/        CacheConfig · DevDataSeeder · OpenApiConfig · SecurityConfig · WebConfig
├── controller/    7 @RestController
├── dto/<recurso>/ Request · PatchRequest · Response, um subpacote por recurso
├── exception/     GlobalExceptionHandler · Recurso · RecursoNaoEncontradoException · RegraDeNegocioException
├── mapper/        @Component escritos à mão (NÃO MapStruct, apesar do nome do pacote)
├── model/         8 @Entity/@Embeddable + 6 enums
├── repository/    RepositorioBase<T> + 7 interfaces
├── security/      JWT, ownership, rate limit, lockout
└── service/       8 @Service
```

Fluxo de uma requisição, sempre o mesmo:

```
Controller ──▶ Service ──▶ Repository ──▶ Model
    │             │
    │             └──▶ Mapper (Entity ↔ DTO)
    │
    └──▶ @PreAuthorize("@seguranca.…")  → SegurancaService
```

Regras de comunicação observadas, sem exceção no código atual:

| Camada | Faz | Não faz |
|---|---|---|
| `Controller` | rota, verbo, `@Valid`, `@PageableDefault`, status HTTP, `@Tag`/`@Operation`, `@PreAuthorize` | nenhuma regra de negócio, nenhum try/catch |
| `Service` | resolve FKs, aplica regra, `@Transactional`, `@Cacheable`/`@CacheEvict` | não conhece HTTP, não monta DTO campo a campo |
| `Mapper` | `toEntity` · `atualizar` · `aplicarPatch` · `toResponse` | não acessa repositório — recebe as entidades já resolvidas |
| `Repository` | JPQL com filtro opcional, `obterPorId`/`garantirQueExiste` | não conhece DTO |
| `Model` | mapeamento JPA + Lombok | sem lógica |

**Os dois módulos novos devem respeitar essa divisão.** Uma agregação analítica é
"regra de negócio": mora no service; a query mora no repositório; o cálculo composto
(score, taxa) fica em classe de apoio no pacote do service, nunca no controller.

## 2. Convenções de código

| Item | Convenção observada |
|---|---|
| Idioma | **Português** em classes, métodos, variáveis, rotas e mensagens. Anotações e tipos do framework em inglês |
| Entidades | `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Entity` (Lombok) |
| PK | `@Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id` — **sempre UUID**, gerado na aplicação |
| Enums | `@Enumerated(EnumType.STRING)` + CHECK constraint espelhando os valores no banco |
| Request DTO | **classe** Lombok `@NoArgsConstructor @AllArgsConstructor @Getter` (sem `@Setter`, sem `@Data`) |
| Response DTO | **`record`** — imutável. A exceção histórica (`PagamentoResponse` com `@Data`) já foi corrigida |
| PatchRequest | classe separada; mantém restrições de **formato**, abandona as de **presença** |
| Mapper | `@Component` manual; `Referencias.de(...)` para null-guard de associação; `AtualizacaoParcial.aplicarSePresente(...)` no PATCH |
| Service | `@Service @RequiredArgsConstructor @Transactional(readOnly = true)` na classe; `@Transactional` nas escritas |
| Injeção | **por construtor**, via `@RequiredArgsConstructor` + campos `private final`. Nunca `@Autowired` em campo |
| Repositório | `interface XRepository extends RepositorioBase<X>` + `default obterPorId(UUID)` / `garantirQueExiste(UUID)` delegando ao enum `Recurso` |
| Filtro opcional | `(:param IS NULL OR ...)` no JPQL — sem Specification, sem Criteria API |
| `LIKE` | **sempre** com `ESCAPE` explícito (ver item 16 de [`docs/07`](docs/07-pendencias-e-divergencias.md)) |
| Erros | exceção → `GlobalExceptionHandler` → `ErroValidacao(campo, mensagem)`; 404 via `RecursoNaoEncontradoException`, 409 via `RegraDeNegocioException` |
| Comentários | densos e explicativos — dizem **por que**, não o quê; costumam citar o defeito que motivou a decisão |

## 3. Convenções de banco e migrations

| Item | Convenção |
|---|---|
| Nomenclatura | `snake_case`, tabelas no **singular** (`tutor`, `animal`, `evento_clinico`, `pagamento`, `usuario`) |
| PK | coluna `id`, `VARCHAR2(36)` no Oracle / `VARCHAR(36)` no MySQL — UUID **em texto**, nunca binário |
| FK | `<entidade>_id`. Exceção existente: `pagamento.evento_id` (e não `evento_clinico_id`) |
| Constraints | `uk_<tabela>_<coluna>`, `fk_<tabela>_<alvo>`, `chk_<tabela>_<coluna>`, `idx_<tabela>_<coluna>` |
| Enums | `VARCHAR2(n)` + `CHECK (col IN ('A','B',...))` |
| Dinheiro | `NUMBER(10,2)` / `DECIMAL(10,2)` — nunca `DOUBLE` |
| Datas | `DATE` (Oracle e MySQL) para `LocalDate`; `TIMESTAMP` (Oracle) / `DATETIME` (MySQL) para `LocalDateTime` |
| Booleano | `NUMBER(1)` / `TINYINT` + `NumericBooleanConverter` + `CHECK (col IN (0,1))` |
| Migrations | Flyway, **dois conjuntos espelhados**: `db/migration/oracle/` e `db/migration/mysql/`. Versões atuais: V1–V4. **A próxima livre é a V5** |
| DDL | `spring.jpa.hibernate.ddl-auto=validate` — entidade sem coluna correspondente **derruba o boot** |
| MySQL | `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4` explícito em toda tabela |

**Divergência entre nome Java e nome da coluna é aceita e resolvida com `@Column`.**
Casos existentes: `sexo`→`genero`, `observacao`→`observacoes`, `data`→`data_evento`,
`hora`→`hora_evento`, `formaPagamento`→`metodo_pagamento`, `eventoClinico`→`evento_id`.
Não é acidente a corrigir; é o padrão de convivência com um schema que veio antes do código.

## 4. Modelo de dados atual

```
Tutor ──< Animal ──< EventoClinico >── Veterinario ──> Clinica
                          │       └──────────────────> Clinica
                          └──< Pagamento
Usuario ──> Tutor | Veterinario   (identidade, opcional e mutuamente exclusiva)
```

Todos os relacionamentos são **`@ManyToOne`, unidirecionais e EAGER**. `Tutor` não tem
coleção de animais; `Animal` não tem coleção de eventos. `spring.jpa.open-in-view=false`.

Campos por tabela, com o que interessa aos módulos novos em destaque:

| Tabela | Campos hoje | Serve para |
|---|---|---|
| `tutor` | id, cpf, nome, data_nascimento, genero, email, telefone, endereço achatado | retenção, contato |
| `animal` | id, nome, **raca**, especie, porte, cor, genero, **data_nascimento**, observacoes, tutor_id | raça, idade, coorte |
| `veterinario` | id, cpf, nome, data_nascimento, genero, email, telefone, especialidade, crmv, endereço, clinica_id | escopo do painel |
| `clinica` | id, nome, cnpj, telefone, email, endereço (**cidade, estado, bairro, cep**) | recorte geográfico |
| `evento_clinico` | id, **data_evento**, hora_evento (VARCHAR2(5)), descricao (VARCHAR2(1000)), tipo_evento, veterinario_id, animal_id, clinica_id | volume de atendimento |
| `pagamento` | id, metodo_pagamento, **valor**, data_pagamento, descricao, notas, status_pagamento, evento_id | faturamento por evento |
| `usuario` | id, email, senha, perfil, ativo, tentativas_falhas, bloqueado_ate, tutor_id, **veterinario_id** | identidade do vet logado |

## 5. Segurança e escopo de visão

- JWT: access de 15 min, refresh de 7 dias, revogação real no logout.
- Perfis: `TUTOR` · `VETERINARIO` · `ADMIN`.
- Autorização em **duas frentes**: regra de rota em `SecurityConfig` (perfil × verbo × rota)
  e ownership por recurso via `@PreAuthorize("@seguranca.…")` no controller.
- `SecurityConfig.configurarRotas` termina em `.anyRequest().authenticated()` —
  **rota nova nasce protegida, mas apenas como "autenticado"**: sem regra explícita, um
  TUTOR logado alcança o painel do veterinário.
- `SegurancaService` (bean `"seguranca"`) expõe hoje: `tutorIdParaFiltro()`,
  `podeAcessarTutor/Animal/Evento/Pagamento`, `podeAtribuirTutor`.
- **Lacuna decisiva:** `UsuarioAutenticado` tem `getTutorId()`, **mas não
  `getVeterinarioId()`**, e `SegurancaService` não tem `veterinarioIdParaFiltro()`. A
  entidade `Usuario` já carrega o vínculo `veterinario` — o dado existe, o acessor não.
  Sem ele, o Painel não tem como se limitar ao vet logado.

## 6. Cache

`CacheConfig` monta um `CaffeineCacheManager` com **lista fixa de nomes**
(`tutores`, `animais`, `clinicas`, `veterinarios`, `eventos`, `pagamentos`), TTL de
10 minutos e teto de 1.000 entradas. Cache novo exige **declarar o nome ali**.

A chave sempre inclui o recorte de segurança:

```java
key = "#nome + '-' + #especie + '-' + @seguranca.tutorIdParaFiltro() + '-' + #pageable"
```

Esse `tutorIdParaFiltro()` na chave existe porque, sem ele, a listagem de um tutor era
servida a outro. **O mesmo cuidado vale integralmente para o painel**, trocando tutor
por veterinário.

## 7. Testes

| Item | Padrão |
|---|---|
| Quantidade | ≈127 métodos `@Test` |
| Base | `TesteDeApi` — `@SpringBootTest @AutoConfigureMockMvc`, login real, header `Authorization`, helpers `buscar/criar/atualizar/atualizarParcialmente/remover` |
| Perfil | `dev` fixo em `src/test/resources/application.properties` (H2 em memória, `MODE=Oracle`) |
| Dados | seed da migration V2 + `DevDataSeeder`; ids fixos nomeados em `SeedV2` |
| Transação | **sem rollback de teste, de propósito** — grava de verdade para exercitar constraint, FK e limite de coluna. Limpeza via `removerDepois(url)` em ordem inversa |
| Cache | limpo no `@AfterEach` para um teste não servir a página do outro |
| Estilo | AssertJ + `@DisplayName` em português descrevendo comportamento |
| Unitários | mappers testados isoladamente em `src/test/.../mapper/` |

## 8. Camada de relatórios/analytics ou IA existente

**Nenhuma.** Não há controller, service, repositório, DTO, tabela, dependência de cliente
HTTP externo, chave de API, retry/circuit breaker ou qualquer vestígio de agregação
analítica. Grep por `relatorio|dashboard|analytic|painel|openai|anthropic|llm` no código
retorna apenas um comentário em `FiltrosDeBuscaTest`.

Os dois módulos são **100% novos**. Os únicos precedentes reaproveitáveis são estruturais:
`RepositorioBase`, `Recurso`, `GlobalExceptionHandler`, `SegurancaService`, `CacheConfig`,
`TesteDeApi`.

O que **existe como intenção documentada** e converge com o Painel: a Etapa 4 do roadmap
em [`docs/09-estado-do-projeto.md`](docs/09-estado-do-projeto.md) ("provar — taxa de
comparecimento por clínica e por período") e a linha "Endpoint de histórico clínico por
animal e de totalizadores financeiros por período" nas melhorias sugeridas de
[`docs/07`](docs/07-pendencias-e-divergencias.md).

---

# Parte II — Inventário de dados: o que existe x o que falta

Esta é a parte mais importante do documento. Os dois módulos foram descritos como
"essencialmente analíticos, cruzam dados que já existem". **A análise do schema não
sustenta essa premissa.** Boa parte dos indicadores pedidos depende de dado que hoje não
é registrado em lugar nenhum.

| # | Feature pedida | Dado necessário | Existe hoje? | Onde / o que falta |
|---|---|---|---|---|
| 1 | Volume de atendimentos por raça | `animal.raca` + `evento_clinico.data_evento` + `veterinario_id` | ✅ **sim** | funciona hoje — ressalva de qualidade no item 3 |
| 2 | Taxa de óbito / sobrevida por raça | status vital do animal, data do óbito | ❌ **não existe** | `animal` não tem `status`, `vivo`, `data_obito` nem `causa`. Sem isso a métrica é impossível |
| 3 | Agrupar por raça de forma confiável | raça normalizada | ⚠️ **parcial** | `animal.raca` é `VARCHAR2(100)` **texto livre**, sem catálogo. "Golden Retriever", "golden retriever" e "Golden" viram três grupos |
| 4 | Gasto com medicamento por raça | prescrição, medicamento, valor por item | ❌ **não existe** | **não há nenhuma tabela de receita, prescrição, medicamento ou item.** `pagamento` guarda **um valor único por evento**, sem discriminação |
| 5 | Ranking geográfico / benchmark regional | localização da clínica | ⚠️ **parcial** | `clinica.cidade/estado/bairro/cep` existem; **não há latitude/longitude** — benchmark por raio em km é inviável |
| 6 | Retenção: pets que não retornaram | status do evento (compareceu?), retorno esperado | ❌ **não existe** | `evento_clinico` **não tem status nenhum**. É o item 1 do roadmap em `docs/09` |
| 7 | Taxa de retorno do tutor | mesmo que acima + vínculo retorno→consulta de origem | ❌ **não existe** | `TipoEvento.RETORNO` existe como rótulo, mas nada liga o retorno à consulta que o gerou |
| 8 | "Tempo de vida ganho" da raça X | expectativa de vida de referência por raça + idade ao óbito | ❌ **não existe** | nem tabela de referência, nem óbito registrado (item 2) |
| 9 | Casos semelhantes no histórico | diagnóstico/patologia estruturada | ❌ **não existe** | só `evento_clinico.descricao`, `VARCHAR2(1000)` **texto livre** |
| 10 | Cluster de patologias por raça/idade | patologia codificada + idade (derivável) | ❌ **parcial** | idade sai de `data_nascimento` ✅; patologia estruturada ❌ |
| 11 | Validação cruzada de medicação | medicamento identificado, dose, **peso do animal**, base normativa | ❌ **não existe** | sem prescrição, sem catálogo de medicamento, sem `peso`, sem base de normas |
| 12 | "A IA nunca decide sozinha" (rastreabilidade) | registro da sugestão e da decisão do vet | ❌ **não existe** | precisa de tabela de auditoria própria |

**Resumo:** das 12 necessidades, **1 é atendida hoje**, 3 são parciais e **8 exigem schema
novo**. Os módulos não são de leitura pura — eles exigem uma rodada de captura de dado
antes de ter o que analisar.

> ### Consequência prática
> A ordem natural não é "Painel depois IA". É: **(a)** fechar as lacunas de captura no
> domínio existente → **(b)** Painel sobre o que passou a existir → **(c)** IA sobre a
> mesma base. Um painel construído antes de (a) mostraria zeros ou números inventados.

---

# Parte III — Módulo 1: Painel do Veterinário

## III.1 Tabelas existentes, apenas lidas (read-only)

Nenhuma escrita. O módulo inteiro é `@Transactional(readOnly = true)`.

| Tabela | Uso no painel |
|---|---|
| `evento_clinico` | volume, cadência, comparecimento, denominadores |
| `animal` | raça, espécie, porte, idade, desfecho |
| `tutor` | retenção, contato |
| `veterinario` | escopo (o painel é sempre "de um vet") |
| `clinica` | recorte geográfico e benchmark |
| `pagamento` | ticket médio, faturamento, pendência financeira |

## III.2 Campos novos em tabelas existentes

Migrations **V5** e **V6** (espelhadas em `oracle/` e `mysql/`).

### V5 — status e continuidade do atendimento

| Tabela | Coluna | Tipo Oracle | Tipo MySQL | Nulo? | Regra |
|---|---|---|---|---|---|
| `evento_clinico` | `status_evento` | `VARCHAR2(20)` | `VARCHAR(20)` | NOT NULL, DEFAULT `'REALIZADO'` | `CHECK IN ('AGENDADO','REALIZADO','FALTOU','CANCELADO')` |
| `evento_clinico` | `data_retorno_previsto` | `DATE` | `DATE` | nulo | data em que o retorno deveria acontecer |
| `evento_clinico` | `evento_origem_id` | `VARCHAR2(36)` | `VARCHAR(36)` | nulo | FK auto-referente → `evento_clinico(id)`; liga o RETORNO à consulta que o gerou |
| `evento_clinico` | `peso_kg` | `NUMBER(6,3)` | `DECIMAL(6,3)` | nulo | peso aferido no atendimento; série temporal por pet |

Índices: `idx_evento_vet_data (veterinario_id, data_evento)`,
`idx_evento_animal_data (animal_id, data_evento)`,
`idx_evento_retorno (data_retorno_previsto)`.

> **O DEFAULT `'REALIZADO'` é uma decisão com consequência.** Todo evento histórico passa
> a contar como comparecido, o que zera a taxa de falta retroativa. A alternativa é
> `NULL` para o histórico e NOT NULL só para registros novos — mas aí toda agregação
> precisa tratar o nulo. **Ver [pergunta 2](#viii2--óbito-e-desfecho).**

### V6 — desfecho do animal

| Tabela | Coluna | Tipo Oracle | Tipo MySQL | Nulo? | Regra |
|---|---|---|---|---|---|
| `animal` | `status_vital` | `VARCHAR2(15)` | `VARCHAR(15)` | NOT NULL, DEFAULT `'VIVO'` | `CHECK IN ('VIVO','OBITO','INATIVO')` |
| `animal` | `data_obito` | `DATE` | `DATE` | nulo | obrigatória quando `status_vital = 'OBITO'` (regra na aplicação) |
| `animal` | `causa_obito` | `VARCHAR2(300)` | `VARCHAR(300)` | nulo | texto livre nesta versão |
| `animal` | `raca_referencia_id` | `VARCHAR2(36)` | `VARCHAR(36)` | nulo | FK → `raca_referencia(id)`; normalização opcional |

`INATIVO` cobre o caso real de "o pet saiu da base sem ter morrido" (mudança de cidade,
tutor sumiu) — sem ele, esses casos poluiriam a taxa de óbito.

## III.3 Tabelas novas

### `raca_referencia` — catálogo de raças (V6)

Resolve simultaneamente o item 3 (agrupamento confiável) e o item 8 (expectativa de vida)
da Parte II.

| Coluna | Tipo Oracle | Tipo MySQL | Restrição |
|---|---|---|---|
| `id` | `VARCHAR2(36)` | `VARCHAR(36)` | PK (UUID textual) |
| `nome` | `VARCHAR2(100)` | `VARCHAR(100)` | NOT NULL |
| `nome_normalizado` | `VARCHAR2(100)` | `VARCHAR(100)` | NOT NULL, `uk_raca_normalizado` UNIQUE — maiúsculas, sem acento, sem espaço duplo |
| `especie` | `VARCHAR2(50)` | `VARCHAR(50)` | NOT NULL |
| `porte_padrao` | `VARCHAR2(20)` | `VARCHAR(20)` | `CHECK IN ('PEQUENO','MEDIO','GRANDE')` |
| `expectativa_vida_meses` | `NUMBER(4)` | `SMALLINT` | nulo permitido |
| `expectativa_vida_fonte` | `VARCHAR2(300)` | `VARCHAR(300)` | de onde veio o número — **obrigatório se a expectativa estiver preenchida** |
| `ativo` | `NUMBER(1)` | `TINYINT` | DEFAULT 1, `CHECK IN (0,1)` |

Entidade `RacaReferencia`, repositório `RacaReferenciaRepository extends RepositorioBase<RacaReferencia>`,
novo valor `RACA_REFERENCIA("Raça de referência não encontrada")` no enum `Recurso`.

> **Sem catálogo, o painel mente.** Enquanto `animal.raca_referencia_id` não estiver
> preenchido, todas as agregações por raça devem usar
> `UPPER(TRIM(a.raca))` como chave — paliativo explícito, que resolve caixa e espaço, mas
> não acento nem sinônimo.

### Materialização de métricas — **não recomendada nesta fase**

Considerei uma tabela `painel_metrica_diaria` (snapshot pré-agregado). **Recomendo não
criar agora:** o volume de dados do projeto é de ordem de centenas de linhas, a agregação
sai em milissegundos, e um snapshot introduz um job agendado, defasagem e um segundo lugar
onde o número pode divergir. O caminho é agregação sob demanda + cache Caffeine, deixando
materialização como evolução caso o volume real justifique.

## III.4 Enums novos

```java
public enum StatusEvento { AGENDADO, REALIZADO, FALTOU, CANCELADO }
public enum StatusVital  { VIVO, OBITO, INATIVO }
public enum FaixaRisco   { BAIXO, MEDIO, ALTO }
public enum FaixaEtaria  { FILHOTE, ADULTO, SENIOR }
```

`StatusEvento` e `StatusVital` são persistidos (`@Enumerated(STRING)` + CHECK).
`FaixaRisco` e `FaixaEtaria` são **derivados**, existem só no DTO de saída.

## III.5 Endpoints

Pacote `controller/PainelVeterinarioController`, `@RequestMapping("/painel")`
(o prefixo `/api/v1` é aplicado pelo `WebConfig`).

Todos os endpoints:

- são **GET**, retornam 200;
- aceitam `de` e `ate` (`LocalDate`, ISO). Default: últimos 12 meses;
- aceitam `clinicaId` opcional (vet que atende em mais de uma clínica);
- exigem `@PreAuthorize("@seguranca.podeAcessarPainelDoVeterinario(#veterinarioId)")`;
- respondem 404 (`Recurso.VETERINARIO`) se o vet não existir e 403 se for de outro vet.

| # | Verbo | Rota | Query params | Resposta |
|---|---|---|---|---|
| 1 | GET | `/api/v1/painel/veterinarios/{veterinarioId}/resumo` | `de`, `ate`, `clinicaId` | `ResumoPainelResponse` (KPIs de cabeçalho) |
| 2 | GET | `/api/v1/painel/veterinarios/{veterinarioId}/atendimentos-por-raca` | `de`, `ate`, `clinicaId`, `especie`, `page`, `size`, `sort` | `Page<AtendimentoPorRacaResponse>` |
| 3 | GET | `/api/v1/painel/veterinarios/{veterinarioId}/desfecho-por-raca` | `de`, `ate`, `clinicaId`, `especie`, `minimoCasos` | `Page<DesfechoPorRacaResponse>` |
| 4 | GET | `/api/v1/painel/veterinarios/{veterinarioId}/gasto-medicamento-por-raca` | `de`, `ate`, `clinicaId`, `page`, `size` | `Page<GastoMedicamentoPorRacaResponse>` |
| 5 | GET | `/api/v1/painel/veterinarios/{veterinarioId}/retencao` | `de`, `ate`, `clinicaId` | `RetencaoResponse` |
| 6 | GET | `/api/v1/painel/veterinarios/{veterinarioId}/risco-abandono` | `faixaRisco`, `clinicaId`, `page`, `size`, `sort` | `Page<RiscoPetResponse>` |
| 7 | GET | `/api/v1/painel/veterinarios/{veterinarioId}/impacto-vida` | `especie`, `minimoCasos` | `Page<ImpactoVidaResponse>` |
| 8 | GET | `/api/v1/painel/veterinarios/{veterinarioId}/benchmark` | `de`, `ate`, `abrangencia` (`CIDADE`\|`ESTADO`) | `BenchmarkResponse` |

**Sort default por endpoint:** 2 → `atendimentos,desc`; 3 → `taxaObito,desc`;
4 → `gastoTotal,desc`; 6 → `score,desc`; 7 → `mesesGanhos,desc`.

**Alternativa considerada e descartada:** rotas `/painel/me/...` lendo o vet do token.
Descartada porque o projeto sempre põe o id do recurso no path e resolve a permissão com
`@seguranca` — `/painel/me` criaria um segundo padrão e impediria o ADMIN de inspecionar
o painel de um vet específico. **Ver [pergunta 8](#viii8--forma-dos-endpoints).**

### DTOs de resposta (records, como todo Response do projeto)

```java
// dto/painel/ResumoPainelResponse.java
public record ResumoPainelResponse(
        UUID veterinarioId, String veterinarioNome,
        LocalDate de, LocalDate ate,
        long atendimentosRealizados, long atendimentosAgendados, long faltas,
        BigDecimal taxaComparecimento,     // 0.0000–1.0000, 4 casas
        long petsAtendidos, long tutoresAtendidos,
        BigDecimal ticketMedio, BigDecimal faturamentoTotal,
        long petsEmRiscoAlto) {}

// dto/painel/AtendimentoPorRacaResponse.java
public record AtendimentoPorRacaResponse(
        String raca, String especie,
        long atendimentos, long petsDistintos,
        BigDecimal participacao,           // fração do total do vet na janela
        BigDecimal ticketMedio) {}

// dto/painel/DesfechoPorRacaResponse.java
public record DesfechoPorRacaResponse(
        String raca, String especie,
        long petsAcompanhados, long obitos,
        BigDecimal taxaObito, BigDecimal taxaSobrevida,
        boolean amostraSuficiente) {}      // false quando petsAcompanhados < minimoCasos

// dto/painel/GastoMedicamentoPorRacaResponse.java
public record GastoMedicamentoPorRacaResponse(
        String raca, String especie,
        BigDecimal gastoTotal, BigDecimal gastoMedioPorPet,
        long petsComPrescricao, long itensPrescritos,
        String classeTerapeuticaPredominante) {}

// dto/painel/RetencaoResponse.java
public record RetencaoResponse(
        LocalDate de, LocalDate ate,
        long tutoresAtendidos, long tutoresRecorrentes,
        BigDecimal taxaRecorrencia,
        long retornosPrevistos, long retornosCumpridos, long retornosEmAtraso,
        BigDecimal taxaRetornoCumprido,
        Integer medianaDiasEntreAtendimentos) {}

// dto/painel/RiscoPetResponse.java
public record RiscoPetResponse(
        UUID animalId, String animalNome, String raca, String especie,
        UUID tutorId, String tutorNome, String tutorTelefone,
        LocalDate ultimoAtendimento, Integer diasDesdeUltimoAtendimento,
        LocalDate retornoPrevisto, Integer diasEmAtraso,
        int score, FaixaRisco faixaRisco,
        List<String> motivos) {}           // rótulos legíveis do que puxou o score

// dto/painel/ImpactoVidaResponse.java
public record ImpactoVidaResponse(
        String raca, String especie,
        Integer expectativaReferenciaMeses, String fonteReferencia,
        BigDecimal idadeMediaObitoMeses,
        BigDecimal mesesGanhos,            // negativo é possível e deve ser exibido
        long obitosConsiderados, boolean amostraSuficiente) {}

// dto/painel/BenchmarkResponse.java
public record BenchmarkResponse(
        String abrangencia, String regiao,
        int veterinariosNaRegiao, boolean anonimatoPreservado,
        BigDecimal meuTicketMedio, BigDecimal medianaRegiaoTicketMedio,
        BigDecimal minhaTaxaComparecimento, BigDecimal medianaRegiaoComparecimento,
        BigDecimal minhaTaxaRecorrencia, BigDecimal medianaRegiaoRecorrencia,
        Integer meuPercentilAtendimentos) {}
```

## III.6 Regras de negócio, feature a feature

Toda janela é **fechada nos dois extremos**: `data_evento BETWEEN :de AND :ate`.
Salvo indicação contrária, apenas eventos com `status_evento = 'REALIZADO'` entram nos
numeradores clínicos — `AGENDADO` no futuro não é atendimento e `CANCELADO` não aconteceu.

### R1 — Volume de atendimentos por raça (endpoint 2)

```
atendimentos    = COUNT(e) WHERE e.veterinario_id = :vet
                          AND e.status_evento = 'REALIZADO'
                          AND e.data_evento BETWEEN :de AND :ate
                  GROUP BY chave_raca
petsDistintos   = COUNT(DISTINCT e.animal_id) no mesmo grupo
participacao    = atendimentos / total_de_atendimentos_do_vet_na_janela
ticketMedio     = AVG(p.valor) dos pagamentos PAGO ligados a esses eventos
```

`chave_raca` = `a.raca_referencia.nome` quando o vínculo existir, senão `UPPER(TRIM(a.raca))`.
Raça nula ou vazia agrupa como `"NAO INFORMADA"` e **é exibida**, não descartada — esconder
o não informado faz os percentuais mentirem.

### R2 — Taxa de óbito / sobrevida por raça (endpoint 3)

```
petsAcompanhados = COUNT(DISTINCT a.id) tal que exista evento REALIZADO
                   com esse vet e esse animal na janela
obitos           = subconjunto com a.status_vital = 'OBITO'
                   AND a.data_obito BETWEEN :de AND :ate + 90 dias
taxaObito        = obitos / petsAcompanhados
taxaSobrevida    = 1 - taxaObito
```

Regras de exibição, **não negociáveis**:

1. `minimoCasos` default **5**. Abaixo disso, `amostraSuficiente = false` e o front deve
   suprimir o percentual, mostrando apenas o par bruto `obitos/petsAcompanhados`.
2. O denominador **sempre acompanha** o percentual na resposta. "50% de óbito" com n=2 é
   ruído, e o número sozinho é enganoso.
3. A janela de +90 dias após `ate` existe para capturar o óbito de um pet cujo último
   atendimento foi no fim da janela. É arbitrária — **ver [pergunta 2](#viii2--óbito-e-desfecho)**.

> **Alerta de interpretação, a levar para o produto:** esta métrica **não mede qualidade
> do veterinário**. Um oncologista terá taxa de óbito estruturalmente alta porque recebe os
> casos graves; um vet de rotina vacinal terá taxa próxima de zero. Comparar os dois é
> comparar populações diferentes, não competências. A tela precisa carregar essa ressalva
> junto do número, e o benchmark (R7) **não deve** ranquear vets por essa taxa.

### R3 — Gasto com medicamento por raça (endpoint 4)

Depende inteiramente das tabelas do Módulo 2 (`prescricao`, `item_prescricao`,
`medicamento`). **Enquanto elas não existirem, este endpoint não deve ser implementado** —
nem devolvendo zeros, o que seria indistinguível de "nenhum medicamento prescrito".

```
gastoTotal       = SUM(ip.valor_total) dos itens de prescrições
                   de eventos REALIZADOS do vet na janela, agrupado por chave_raca
gastoMedioPorPet = gastoTotal / COUNT(DISTINCT animal_id do grupo)
classeTerapeuticaPredominante = classe com maior SUM(valor_total) no grupo
```

**Decisão pendente e relevante:** `valor_total` é o que a clínica **cobrou** ou o preço de
tabela do produto? Muda o significado do indicador de "custo do tratamento para o tutor"
para "receita de medicamento da clínica" — e é a diferença entre um indicador clínico e um
indicador comercial. **Ver [pergunta 5](#viii5--medicamento-e-gasto).**

### R4 — Retenção e taxa de retorno (endpoint 5)

Duas métricas distintas, deliberadamente separadas — o pedido original ("taxa de retorno
dos tutores") mistura as duas, e elas respondem perguntas diferentes:

**(a) Retorno prescrito cumprido** — mede adesão ao plano clínico:

```
retornosPrevistos = COUNT(e) WHERE e.veterinario_id = :vet
                             AND e.data_retorno_previsto BETWEEN :de AND :ate
retornoCumprido   = existe evento posterior REALIZADO para o MESMO animal
                    com data_evento <= e.data_retorno_previsto + TOLERANCIA (30 dias)
                    -- preferencialmente com evento_origem_id = e.id;
                    -- na ausência do vínculo, qualquer atendimento do mesmo animal conta
retornosEmAtraso  = previstos, não cumpridos, com data_retorno_previsto + TOLERANCIA < hoje
taxaRetornoCumprido = retornosCumpridos / retornosPrevistos
```

**(b) Recorrência do tutor** — mede fidelidade à clínica:

```
tutoresAtendidos   = COUNT(DISTINCT a.tutor_id) com evento REALIZADO na janela
tutoresRecorrentes = subconjunto com >= 2 atendimentos REALIZADOS
                     separados por >= 30 dias (evita contar consulta + retorno da mesma
                     ocorrência como fidelidade)
taxaRecorrencia    = tutoresRecorrentes / tutoresAtendidos
medianaDiasEntreAtendimentos = mediana dos intervalos entre atendimentos consecutivos
                               do mesmo animal, na janela
```

`TOLERANCIA` (30 dias) e a separação mínima (30 dias) são **parâmetros configuráveis**,
não constantes soltas: `clyvovet.painel.retorno.tolerancia-dias` e
`clyvovet.painel.retencao.separacao-minima-dias`, com esses defaults.

### R5 — Score de risco por pet (endpoint 6)

Score de **0 a 100**, quanto maior pior. Soma ponderada de quatro componentes, cada um
normalizado para 0–1 antes de receber o peso:

| # | Componente | Cálculo do fator (0–1) | Peso |
|---|---|---|---|
| 1 | Atraso do retorno previsto | `min(dias_em_atraso / 90, 1)`; 0 se não há retorno previsto | **40** |
| 2 | Silêncio desde o último atendimento | `min(meses_desde_ultimo / cadencia_esperada - 1, 1)`, piso 0 | **30** |
| 3 | Histórico de faltas | `faltas / (faltas + realizados)` no histórico completo do pet | **20** |
| 4 | Pendência financeira | 1 se existe pagamento `PENDENTE` com evento há mais de 60 dias, senão 0 | **10** |

`cadencia_esperada`, por faixa etária derivada de `animal.data_nascimento`:

| Faixa | Critério | Cadência esperada |
|---|---|---|
| `FILHOTE` | < 12 meses | 4 meses |
| `ADULTO` | 12 meses a 70% da expectativa da raça (default 84 meses) | 12 meses |
| `SENIOR` | acima disso | 6 meses |

Faixas de saída: `BAIXO` 0–39 · `MEDIO` 40–69 · `ALTO` 70–100.

Regras de corte:

- **Excluir** pets com `status_vital` em (`OBITO`, `INATIVO`). Cobrar retorno de um pet
  morto é o pior erro possível deste módulo.
- **Excluir** pets sem nenhum atendimento `REALIZADO` com este vet — não são carteira dele.
- `motivos` traz um rótulo por componente que contribuiu com ≥ 10 pontos
  (ex.: `"Retorno em atraso há 47 dias"`, `"Sem atendimento há 19 meses"`).

Pesos e limiares em `application.properties` sob `clyvovet.painel.risco.*`, injetados com
`@Value` no service — o modelo será calibrado com uso real, e recompilar para mudar um peso
não é aceitável.

### R6 — Impacto: "tempo de vida ganho" (endpoint 7)

```
idadeMediaObitoMeses = AVG(meses entre a.data_nascimento e a.data_obito)
                       para animais da raça, com status_vital = 'OBITO',
                       que tiveram >= 1 evento REALIZADO com este vet
mesesGanhos          = idadeMediaObitoMeses - rr.expectativa_vida_meses
```

**Esta é a métrica metodologicamente mais frágil do módulo, e a spec registra isso.**
Quatro problemas, todos reais:

1. **Censura à direita.** Só entram pets que morreram. Os vivos — inclusive os que já
   passaram da expectativa — ficam de fora, o que **puxa a média para baixo** e faz um vet
   excelente parecer mediano. A correção estatística correta seria Kaplan-Meier, fora do
   escopo razoável aqui.
2. **Atribuição.** O vet pode ter atendido o pet uma vez, para uma vacina, dez anos antes
   do óbito. Nada disso é causalidade.
3. **Amostra.** Óbitos por raça, por vet, são poucos. `minimoCasos` default **5**, e
   `amostraSuficiente = false` abaixo disso.
4. **Referência.** Sem `expectativa_vida_meses` preenchido e com fonte, o cálculo não sai —
   e não existe base pública canônica em português para isso.

**Recomendação:** rotular a tela como *indicativa/experimental*, exibir sempre
`obitosConsiderados` e `fonteReferencia`, e nunca usar esta métrica em benchmark
comparativo entre profissionais.

### R7 — Benchmark regional (endpoint 8)

Região = `clinica.cidade` (abrangência `CIDADE`) ou `clinica.estado` (`ESTADO`), obtida
pela clínica do veterinário. **Não há geolocalização** — raio em km está fora de alcance
sem adicionar latitude/longitude e um serviço de geocodificação.

```
grupoComparacao = veterinários cuja clínica está na mesma região,
                  com >= 10 atendimentos REALIZADOS na janela (evita cadastro inativo
                  puxando a mediana para baixo), excluindo o próprio vet
Para cada métrica: mediana do grupo (não a média — resistente a outlier)
meuPercentilAtendimentos = posição do vet na distribuição de atendimentos do grupo
```

**Regra de privacidade, obrigatória:** se `veterinariosNaRegiao < 5`, devolver
`anonimatoPreservado = false` e **suprimir todas as medianas** (nulas). Com dois ou três
vets na cidade, "mediana da região" identifica o colega. A resposta **nunca** nomeia outro
veterinário ou clínica, e não expõe valores individuais de terceiros — só agregados.

## III.7 Camadas a criar (Módulo 1)

| Camada | Artefatos |
|---|---|
| `model/` | `StatusEvento`, `StatusVital`, `RacaReferencia` + campos novos em `EventoClinico` e `Animal` |
| `repository/` | `RacaReferenciaRepository`; `PainelRepository` (interface `@Repository` com JPQL de agregação devolvendo **projections**, não entidades) |
| `dto/painel/` | os 8 records da seção III.5 + `FaixaRisco`, `FaixaEtaria` |
| `service/` | `PainelVeterinarioService` (orquestra) + `CalculadoraDeRisco` e `CalculadoraDeRetencao` como `@Component` de apoio, testáveis isoladamente |
| `mapper/` | `PainelMapper` — converte projection → Response |
| `controller/` | `PainelVeterinarioController` |
| `security/` | `SegurancaService.veterinarioIdParaFiltro()` e `podeAcessarPainelDoVeterinario(UUID)`; `UsuarioAutenticado.getVeterinarioId()` |
| `config/` | novos caches em `CacheConfig`: `painel-resumo`, `painel-agregados`, `painel-risco` |
| `exception/` | novo valor `RACA_REFERENCIA` no enum `Recurso` |

**Sobre as queries de agregação:** usar **projections** (interface ou `SELECT new`), nunca
carregar entidades. Como todos os `@ManyToOne` do projeto são EAGER, uma query que devolve
`List<EventoClinico>` traz veterinário, animal, tutor e clínica de cada linha — inaceitável
para agregação. É um desvio consciente do padrão de repositório atual (que devolve
`Page<Entidade>`) e deve ser comentado no código, como o projeto costuma fazer.

---

# Parte IV — Módulo 2: Dados + IA

> **Princípio que atravessa o módulo inteiro:** a IA **sugere**, o veterinário **decide e
> assina**. Isso não é só um texto de tela — tem consequência de schema (registro da
> sugestão e da decisão), de contrato (nenhum endpoint altera prontuário) e de resposta
> (toda sugestão vem acompanhada das evidências que a geraram).

## IV.1 Tabelas existentes, apenas lidas

`animal`, `evento_clinico`, `veterinario`, `clinica`, `tutor` (apenas para escopo — **dado
pessoal do tutor nunca é enviado ao provedor de IA**, ver R11).

## IV.2 Tabelas novas

Migrations **V7** (dados clínicos estruturados) e **V8** (camada de IA), espelhadas nos
dois bancos.

### V7 — prontuário estruturado

#### `patologia` — catálogo

| Coluna | Tipo Oracle | Tipo MySQL | Restrição |
|---|---|---|---|
| `id` | `VARCHAR2(36)` | `VARCHAR(36)` | PK |
| `codigo` | `VARCHAR2(20)` | `VARCHAR(20)` | `uk_patologia_codigo` UNIQUE |
| `nome` | `VARCHAR2(200)` | `VARCHAR(200)` | NOT NULL |
| `sistema` | `VARCHAR2(50)` | `VARCHAR(50)` | aparelho acometido (DERMATOLOGICO, CARDIACO, …) |
| `especie_alvo` | `VARCHAR2(50)` | `VARCHAR(50)` | nulo = qualquer espécie |
| `ativo` | `NUMBER(1)` | `TINYINT` | DEFAULT 1, `CHECK IN (0,1)` |

#### `diagnostico` — patologia observada em um atendimento

| Coluna | Tipo Oracle | Tipo MySQL | Restrição |
|---|---|---|---|
| `id` | `VARCHAR2(36)` | `VARCHAR(36)` | PK |
| `evento_id` | `VARCHAR2(36)` | `VARCHAR(36)` | `fk_diagnostico_evento` → `evento_clinico(id)`, NOT NULL |
| `patologia_id` | `VARCHAR2(36)` | `VARCHAR(36)` | `fk_diagnostico_patologia` → `patologia(id)`, NOT NULL |
| `principal` | `NUMBER(1)` | `TINYINT` | DEFAULT 0 — no máximo um principal por evento (regra na aplicação) |
| `status_diagnostico` | `VARCHAR2(20)` | `VARCHAR(20)` | `CHECK IN ('SUSPEITA','CONFIRMADO','DESCARTADO')` |
| `observacao` | `VARCHAR2(1000)` | `VARCHAR(1000)` | |

Índices: `idx_diagnostico_evento (evento_id)`, `idx_diagnostico_patologia (patologia_id)`.

#### `medicamento` — catálogo

| Coluna | Tipo Oracle | Tipo MySQL | Restrição |
|---|---|---|---|
| `id` | `VARCHAR2(36)` | `VARCHAR(36)` | PK |
| `nome` | `VARCHAR2(200)` | `VARCHAR(200)` | NOT NULL |
| `principio_ativo` | `VARCHAR2(200)` | `VARCHAR(200)` | |
| `classe_terapeutica` | `VARCHAR2(100)` | `VARCHAR(100)` | agrupador do painel (R3) |
| `tarja` | `VARCHAR2(20)` | `VARCHAR(20)` | `CHECK IN ('LIVRE','AMARELA','VERMELHA','PRETA')` |
| `controlado` | `NUMBER(1)` | `TINYINT` | DEFAULT 0, `CHECK IN (0,1)` |
| `registro_mapa` | `VARCHAR2(50)` | `VARCHAR(50)` | registro no MAPA |
| `ativo` | `NUMBER(1)` | `TINYINT` | DEFAULT 1, `CHECK IN (0,1)` |

> ⚠️ **Verificar antes de criar:** o enunciado diz que a API .NET, no mesmo banco Oracle,
> já trata "produtos" e "sugestões de produtos". Se `produto` já existir com medicamentos,
> criar `medicamento` duplica catálogo e as duas APIs divergem em silêncio.
> **É a [pergunta 1](#viii1--o-schema-compartilhado-com-a-api-net) e deve ser respondida
> antes de qualquer linha de migration.**

#### `prescricao` e `item_prescricao`

| `prescricao` | Tipo Oracle | Tipo MySQL | Restrição |
|---|---|---|---|
| `id` | `VARCHAR2(36)` | `VARCHAR(36)` | PK |
| `evento_id` | `VARCHAR2(36)` | `VARCHAR(36)` | `fk_prescricao_evento` → `evento_clinico(id)`, NOT NULL |
| `veterinario_id` | `VARCHAR2(36)` | `VARCHAR(36)` | `fk_prescricao_vet` → `veterinario(id)`, NOT NULL — **quem assina** |
| `data_prescricao` | `DATE` | `DATE` | NOT NULL |
| `status_prescricao` | `VARCHAR2(20)` | `VARCHAR(20)` | `CHECK IN ('ATIVA','CONCLUIDA','SUSPENSA','CANCELADA')` |
| `observacao` | `VARCHAR2(1000)` | `VARCHAR(1000)` | |

| `item_prescricao` | Tipo Oracle | Tipo MySQL | Restrição |
|---|---|---|---|
| `id` | `VARCHAR2(36)` | `VARCHAR(36)` | PK |
| `prescricao_id` | `VARCHAR2(36)` | `VARCHAR(36)` | `fk_item_prescricao` → `prescricao(id)`, NOT NULL |
| `medicamento_id` | `VARCHAR2(36)` | `VARCHAR(36)` | `fk_item_medicamento` → `medicamento(id)` |
| `dose` | `VARCHAR2(50)` | `VARCHAR(50)` | texto, ex.: `"10 mg/kg"` |
| `via_administracao` | `VARCHAR2(30)` | `VARCHAR(30)` | `CHECK IN ('ORAL','TOPICA','INJETAVEL','OFTALMICA','OTOLOGICA','OUTRA')` |
| `frequencia_horas` | `NUMBER(3)` | `SMALLINT` | intervalo entre doses |
| `duracao_dias` | `NUMBER(4)` | `SMALLINT` | |
| `quantidade` | `NUMBER(8,3)` | `DECIMAL(8,3)` | |
| `valor_unitario` | `NUMBER(10,2)` | `DECIMAL(10,2)` | |
| `valor_total` | `NUMBER(10,2)` | `DECIMAL(10,2)` | **alimenta R3 do Painel** |

Nome da coluna `via_administracao`, e não `via`: evita risco de palavra reservada e é mais
legível.

#### `restricao_medicamento` — base determinística da validação

| Coluna | Tipo Oracle | Tipo MySQL | Restrição |
|---|---|---|---|
| `id` | `VARCHAR2(36)` | `VARCHAR(36)` | PK |
| `medicamento_id` | `VARCHAR2(36)` | `VARCHAR(36)` | `fk_restricao_medicamento` → `medicamento(id)`, NOT NULL |
| `tipo_restricao` | `VARCHAR2(30)` | `VARCHAR(30)` | `CHECK IN ('ESPECIE_PROIBIDA','DOSE_MAXIMA','IDADE_MINIMA','GESTACAO','INTERACAO','CONTRAINDICACAO_PATOLOGIA')` |
| `especie` | `VARCHAR2(50)` | `VARCHAR(50)` | nulo = todas |
| `medicamento_interacao_id` | `VARCHAR2(36)` | `VARCHAR(36)` | FK → `medicamento(id)`, para `INTERACAO` |
| `patologia_id` | `VARCHAR2(36)` | `VARCHAR(36)` | FK → `patologia(id)`, para `CONTRAINDICACAO_PATOLOGIA` |
| `valor_limite` | `NUMBER(10,3)` | `DECIMAL(10,3)` | para `DOSE_MAXIMA` (mg/kg) e `IDADE_MINIMA` (meses) |
| `severidade` | `VARCHAR2(15)` | `VARCHAR(15)` | `CHECK IN ('INFO','ATENCAO','CRITICO')` |
| `descricao` | `VARCHAR2(1000)` | `VARCHAR(1000)` | NOT NULL — o texto mostrado ao vet |
| `fonte` | `VARCHAR2(300)` | `VARCHAR(300)` | NOT NULL — norma, bula ou publicação |
| `vigencia_inicio` | `DATE` | `DATE` | |
| `vigencia_fim` | `DATE` | `DATE` | nulo = vigente |

### V8 — rastreabilidade da IA

#### `sugestao_ia`

| Coluna | Tipo Oracle | Tipo MySQL | Restrição |
|---|---|---|---|
| `id` | `VARCHAR2(36)` | `VARCHAR(36)` | PK |
| `tipo_sugestao` | `VARCHAR2(30)` | `VARCHAR(30)` | `CHECK IN ('CASO_SEMELHANTE','CLUSTER_PATOLOGIA','VALIDACAO_MEDICACAO')` |
| `veterinario_id` | `VARCHAR2(36)` | `VARCHAR(36)` | `fk_sugestao_vet` → `veterinario(id)`, NOT NULL — quem pediu |
| `animal_id` | `VARCHAR2(36)` | `VARCHAR(36)` | FK → `animal(id)`, nulo permitido |
| `evento_id` | `VARCHAR2(36)` | `VARCHAR(36)` | FK → `evento_clinico(id)`, nulo permitido |
| `entrada` | `CLOB` | `LONGTEXT` | JSON do que foi enviado, **já anonimizado** |
| `saida` | `CLOB` | `LONGTEXT` | JSON da resposta |
| `modelo` | `VARCHAR2(100)` | `VARCHAR(100)` | identificador do modelo |
| `versao_prompt` | `VARCHAR2(20)` | `VARCHAR(20)` | versiona o template usado |
| `origem` | `VARCHAR2(20)` | `VARCHAR(20)` | `CHECK IN ('DETERMINISTICA','LLM','HIBRIDA')` |
| `latencia_ms` | `NUMBER(8)` | `INT` | |
| `criado_em` | `TIMESTAMP` | `DATETIME` | NOT NULL — `DATETIME` no MySQL pelo mesmo motivo de `bloqueado_ate` (ver comentário da V3) |

#### `sugestao_ia_decisao`

| Coluna | Tipo Oracle | Tipo MySQL | Restrição |
|---|---|---|---|
| `id` | `VARCHAR2(36)` | `VARCHAR(36)` | PK |
| `sugestao_id` | `VARCHAR2(36)` | `VARCHAR(36)` | `fk_decisao_sugestao` → `sugestao_ia(id)`, NOT NULL, `uk_decisao_sugestao` UNIQUE |
| `veterinario_id` | `VARCHAR2(36)` | `VARCHAR(36)` | `fk_decisao_vet` → `veterinario(id)`, NOT NULL |
| `decisao` | `VARCHAR2(20)` | `VARCHAR(20)` | `CHECK IN ('ACEITA','ACEITA_COM_RESSALVA','REJEITADA','IGNORADA')` |
| `justificativa` | `VARCHAR2(1000)` | `VARCHAR(1000)` | obrigatória para `REJEITADA` e `ACEITA_COM_RESSALVA` (regra na aplicação) |
| `decidido_em` | `TIMESTAMP` | `DATETIME` | NOT NULL |

> **Por que a decisão é tabela separada, e não coluna em `sugestao_ia`:** a sugestão é
> gerada pela máquina, a decisão é ato do profissional. Separar deixa a autoria de cada
> lado inequívoca e permite exigir a decisão sem tornar a sugestão mutável depois de
> emitida — que é justamente a garantia que "a IA não decide sozinha" precisa ter.

## IV.3 Endpoints

Pacote `controller/AssistenteClinicoController`, `@RequestMapping("/ia")`.
Todos exigem perfil `VETERINARIO` ou `ADMIN` — **regra explícita em `SecurityConfig`**.

| # | Verbo | Rota | Corpo / params | Resposta |
|---|---|---|---|---|
| 1 | POST | `/api/v1/ia/casos-semelhantes` | `CasosSemelhantesRequest` | `200` `CasosSemelhantesResponse` |
| 2 | GET | `/api/v1/ia/clusters-patologia` | `raca`, `especie`, `faixaEtaria`, `minimoSuporte`, `page`, `size` | `200` `Page<ClusterPatologiaResponse>` |
| 3 | POST | `/api/v1/ia/validacao-medicacao` | `ValidacaoMedicacaoRequest` | `200` `ValidacaoMedicacaoResponse` |
| 4 | POST | `/api/v1/ia/sugestoes/{id}/decisao` | `DecisaoSugestaoRequest` | `201` `DecisaoSugestaoResponse` |
| 5 | GET | `/api/v1/ia/sugestoes` | `animalId`, `veterinarioId`, `tipoSugestao`, `de`, `ate`, `page`, `size` | `200` `Page<SugestaoResumoResponse>` |
| 6 | GET | `/api/v1/ia/sugestoes/{id}` | — | `200` `SugestaoDetalheResponse` |

**Sobre os POST em um módulo descrito como "de leitura":** os endpoints 1 e 3 são POST por
duas razões, não por descuido. Primeira, o corpo é estruturado e pode ser longo (lista de
sintomas, lista de itens prescritos) — não cabe em query string. Segunda, e mais importante,
**eles gravam**: toda consulta à IA registra uma linha em `sugestao_ia`. Sem esse registro
não há rastreabilidade, e sem rastreabilidade a promessa de "a IA não decide sozinha" não é
verificável. O endpoint 4 é a escrita da decisão do profissional.

### Contratos

```java
// dto/ia/CasosSemelhantesRequest.java  (classe Lombok, como todo Request)
@NoArgsConstructor @AllArgsConstructor @Getter
public class CasosSemelhantesRequest {
    @NotNull  private UUID animalId;
              private UUID eventoId;
    @Size(max = 10) private List<UUID> patologiaIds;
    @Size(max = 1000) private String quadroClinico;   // texto livre do vet
    @Min(1) @Max(20) private Integer limite;          // default 5
}

// dto/ia/CasosSemelhantesResponse.java
public record CasosSemelhantesResponse(
        UUID sugestaoId,
        List<CasoSemelhante> casos,
        String sugestaoTextual,       // redigida pelo LLM sobre os casos; null se indisponível
        String modelo, String versaoPrompt,
        String aviso) {}              // texto fixo: apoio à decisão, não substitui o vet

public record CasoSemelhante(
        UUID eventoId, UUID animalId, String animalNome,
        String raca, String especie, Integer idadeMesesNaEpoca,
        LocalDate dataEvento, TipoEvento tipoEvento,
        List<String> patologias, List<String> medicamentos,
        StatusVital desfechoAnimal,
        BigDecimal scoreSimilaridade,     // 0.0000–1.0000
        List<String> fatoresDeSimilaridade) {}

// dto/ia/ClusterPatologiaResponse.java
public record ClusterPatologiaResponse(
        String raca, String especie, FaixaEtaria faixaEtaria,
        List<String> patologias,          // as que co-ocorrem
        long casosObservados,
        BigDecimal suporte, BigDecimal confianca, BigDecimal lift,
        boolean amostraSuficiente) {}

// dto/ia/ValidacaoMedicacaoRequest.java
@NoArgsConstructor @AllArgsConstructor @Getter
public class ValidacaoMedicacaoRequest {
    @NotNull private UUID animalId;
             private UUID eventoId;
    @NotEmpty @Size(max = 20) @Valid private List<ItemValidacao> itens;

    @NoArgsConstructor @AllArgsConstructor @Getter
    public static class ItemValidacao {
        @NotNull private UUID medicamentoId;
        @Size(max = 50) private String dose;
        private ViaAdministracao viaAdministracao;
        @Min(1) @Max(365) private Integer duracaoDias;
    }
}

// dto/ia/ValidacaoMedicacaoResponse.java
public record ValidacaoMedicacaoResponse(
        UUID sugestaoId,
        SeveridadeAlerta severidadeMaxima,
        List<AlertaMedicacao> alertas,
        boolean exigeConfirmacaoExplicita,   // true se houver algum CRITICO
        String parecerTextual,               // camada LLM; null se indisponível
        String aviso) {}

public record AlertaMedicacao(
        UUID medicamentoId, String medicamentoNome,
        TipoRestricao tipo, SeveridadeAlerta severidade,
        String descricao, String fonte,
        OrigemAlerta origem) {}              // DETERMINISTICA | LLM

// dto/ia/DecisaoSugestaoRequest.java
@NoArgsConstructor @AllArgsConstructor @Getter
public class DecisaoSugestaoRequest {
    @NotNull private DecisaoSugestao decisao;
    @Size(max = 1000) private String justificativa;
}
```

Enums novos: `SeveridadeAlerta { INFO, ATENCAO, CRITICO }`,
`TipoRestricao`, `OrigemAlerta { DETERMINISTICA, LLM }`,
`DecisaoSugestao { ACEITA, ACEITA_COM_RESSALVA, REJEITADA, IGNORADA }`,
`TipoSugestao`, `StatusPrescricao`, `StatusDiagnostico`, `ViaAdministracao`, `Tarja`.

## IV.4 Regras de negócio, feature a feature

### R8 — Histórico com memória / casos semelhantes (endpoint 1)

**Arquitetura em dois estágios: recuperação determinística primeiro, redação por LLM
depois.** Isso não é preciosismo — é o que permite ao vet auditar de onde veio a sugestão,
e é o que mantém o sistema útil quando o provedor de IA estiver fora do ar.

**Estágio 1 — recuperação (SQL puro, sem IA).** Candidatos: eventos `REALIZADO` de animais
que não o próprio, com pelo menos um destes:

- mesma espécie **(obrigatório — filtro duro)**;
- mesma raça normalizada, ou mesmo porte quando a raça não bate;
- idade no evento dentro de ±24 meses da idade atual do animal-alvo;
- ao menos uma patologia em comum com `patologiaIds`;
- janela: últimos 5 anos.

Escopo de visibilidade: eventos da **mesma clínica** do vet solicitante por padrão.
Ampliar para a base inteira é decisão de produto — **ver [pergunta 7](#viii7--escopo-de-visibilidade-da-ia)**.

**Score de similaridade** (0–1), soma ponderada:

| Fator | Cálculo | Peso |
|---|---|---|
| Patologias em comum | Jaccard entre os conjuntos de patologias | 0,45 |
| Raça | 1 mesma raça · 0,5 mesmo porte e espécie · 0 caso contrário | 0,25 |
| Proximidade etária | `1 - min(|Δmeses| / 24, 1)` | 0,20 |
| Recência | `1 - min(anos_desde_o_evento / 5, 1)` | 0,10 |

Ordena por score, corta em `limite` (default 5, teto 20). Descarta score < 0,30 —
melhor devolver dois casos bons que cinco irrelevantes.

**Estágio 2 — redação (LLM).** Recebe **apenas** os K casos já selecionados, anonimizados,
e produz um parágrafo do tipo *"em casos semelhantes você tratou com X; considerou Y?"*.
Regras rígidas:

- o LLM **não escolhe os casos** — só redige sobre os que o SQL selecionou. Isso elimina a
  classe inteira de alucinação "citou um caso que não existe";
- se o provedor falhar ou estourar o timeout, `sugestaoTextual` volta `null` e os casos são
  entregues mesmo assim. **Degradação graciosa, não erro 5xx**;
- toda resposta carrega o `aviso` fixo e o `sugestaoId`.

> **Por que não embeddings/busca vetorial:** o banco é Oracle 19c (ou MySQL 8) compartilhado
> com outra aplicação, sem extensão vetorial disponível, e o volume não justifica um índice
> vetorial externo. A recuperação estruturada acima é determinística, auditável, roda no
> banco que já existe — e, com patologia codificada, é provavelmente mais precisa aqui do
> que similaridade semântica sobre texto livre.

### R9 — Cluster de patologias (endpoint 2)

**Sem IA. Regra de associação em SQL puro** — o dado responde sozinho, e um LLM aqui só
adicionaria custo e incerteza a uma contagem.

Para cada par (ou trio) de patologias que co-ocorrem em animais do mesmo estrato
(raça normalizada × faixa etária × espécie):

```
suporte   = casos_com_A_e_B / total_de_casos_do_estrato
confianca = casos_com_A_e_B / casos_com_A
lift      = confianca / (casos_com_B / total_do_estrato)
```

Cortes default, configuráveis em `clyvovet.ia.cluster.*`:

| Parâmetro | Default | Por quê |
|---|---|---|
| `minimo-casos` | 5 | abaixo disso é anedota |
| `minimo-suporte` | 0,05 | |
| `minimo-confianca` | 0,30 | |
| `minimo-lift` | 1,2 | lift ≤ 1 significa "co-ocorrem por acaso" — não é achado |

`amostraSuficiente = false` quando `casosObservados < minimo-casos`; nesse caso as métricas
percentuais são suprimidas, como em R2.

### R10 — Validação cruzada de medicação (endpoint 3)

**Duas camadas, nesta ordem, e a primeira nunca depende da segunda.**

**Camada 1 — determinística**, sobre `restricao_medicamento` (só linhas vigentes na data
de hoje):

| Verificação | Regra | Severidade típica |
|---|---|---|
| Espécie proibida | `tipo = ESPECIE_PROIBIDA` e `especie` = espécie do animal | `CRITICO` |
| Dose máxima | dose em mg/kg × peso > `valor_limite` | `CRITICO` |
| Idade mínima | idade do animal em meses < `valor_limite` | `ATENCAO` |
| Interação | outro item da mesma requisição casa `medicamento_interacao_id` | `ATENCAO`/`CRITICO` |
| Contraindicação por patologia | animal tem diagnóstico `CONFIRMADO` da `patologia_id` | `CRITICO` |
| Controlado | `medicamento.controlado = 1` | `INFO` — lembrete de receituário |

**Peso do animal:** vem do `peso_kg` do evento mais recente (campo novo da V5). Se não
houver peso registrado, a verificação de dose **não roda** e a resposta traz um alerta
`INFO` explícito — `"dose não verificada: peso do animal não registrado"`. Silenciar isso
seria pior que não verificar: o vet acharia que passou na checagem.

**Camada 2 — LLM.** Recebe o parecer determinístico e a ficha anonimizada, e verifica
consistência com bula e normas vigentes, devolvendo `parecerTextual` **com citação de
fonte**. Regras:

- é **aditiva, nunca subtrativa**: o LLM pode acrescentar um alerta, **jamais remover** ou
  rebaixar um alerta da camada 1;
- alerta originado no LLM vem sempre com `origem = LLM`, e o front deve distingui-lo
  visualmente do determinístico;
- se o provedor falhar, `parecerTextual = null` e a camada 1 responde sozinha.

**A API nunca bloqueia a prescrição.** Ela emite alertas; havendo `CRITICO`,
`exigeConfirmacaoExplicita = true` e o cliente deve exigir confirmação do vet — que fica
registrada em `sugestao_ia_decisao`.

> **Limite honesto que precisa estar na tela:** "checar a legislação vigente" só é confiável
> na medida em que `restricao_medicamento` esteja preenchida e atualizada. Um LLM sem base
> documental **inventa** número de norma com fluência. A camada 2 é auxiliar; a fonte da
> verdade é a tabela. Quem mantém essa tabela é a
> [pergunta 6](#viii6--base-normativa-de-medicação).

### R11 — Privacidade no envio à IA (transversal, obrigatória)

Antes de qualquer chamada externa, o payload passa por um `AnonimizadorClinico`:

| Nunca sai da aplicação | Pode sair |
|---|---|
| nome, CPF, e-mail, telefone e endereço do tutor | espécie, raça, porte, sexo |
| nome do animal | idade em meses, peso |
| nome, CPF, CRMV, e-mail do veterinário | patologias (código e nome) |
| nome, CNPJ e endereço da clínica | medicamentos, doses, vias |
| qualquer UUID de entidade do domínio | datas relativas (ex.: "há 8 meses"), desfecho |

Ids são substituídos por rótulos locais (`caso-1`, `caso-2`) resolvidos de volta na
aplicação. `sugestao_ia.entrada` guarda o payload **já anonimizado** — é o que foi
efetivamente enviado, e é isso que uma auditoria precisa ver.

### R12 — Resiliência da chamada externa (transversal)

| Aspecto | Decisão proposta |
|---|---|
| Timeout | 20 s de leitura, 5 s de conexão |
| Retry | 1 tentativa, só para erro de rede/5xx; nunca para 4xx |
| Falha | degradação graciosa — camada determinística responde; **nunca 5xx por causa da IA** |
| Cliente | `RestClient` do Spring Framework 6 (já disponível via `spring-boot-starter-web`) — não adicionar WebFlux só para isso |
| Chave | variável de ambiente, **nunca versionada** (precedente: item 2 de `docs/07`) |
| Rate limit | faixa própria no `RateLimitFilter` (`Faixa.IA`), bem abaixo de `GERAL(100/min)` — sugestão: 10/min. Sem isso, um cliente em laço gera custo real |
| Interruptor | `clyvovet.ia.habilitada=false` desliga a camada 2 inteira sem redeploy |

## IV.5 Camadas a criar (Módulo 2)

| Camada | Artefatos |
|---|---|
| `model/` | `Patologia`, `Diagnostico`, `Medicamento`, `Prescricao`, `ItemPrescricao`, `RestricaoMedicamento`, `SugestaoIa`, `SugestaoIaDecisao` + 9 enums |
| `repository/` | um por entidade nova (`extends RepositorioBase<T>`) + `CasosSemelhantesRepository` e `ClusterPatologiaRepository` com JPQL/nativa de agregação |
| `dto/ia/`, `dto/prescricao/`, `dto/diagnostico/` | Request (classe) / Response (record) |
| `service/` | `AssistenteClinicoService`, `ValidacaoMedicacaoService`, `ClusterPatologiaService`, `SugestaoIaService` (registro e auditoria), `AnonimizadorClinico` |
| `integration/` (**pacote novo**) | `ClienteIa` (interface) + implementação HTTP + `ClienteIaDesabilitado` como no-op |
| `controller/` | `AssistenteClinicoController` + CRUDs de `Prescricao`, `Diagnostico`, `Medicamento`, `Patologia` (no padrão dos 6 existentes) |
| `config/` | propriedades `clyvovet.ia.*`; nova faixa no `RateLimitFilter` |
| `exception/` | novos valores no enum `Recurso`; possivelmente `IaIndisponivelException` → 503 no handler |

**O pacote `integration/` é o único desvio estrutural do projeto** (hoje só há camadas
técnicas do fluxo HTTP→banco). Justificativa: um cliente de serviço externo não é service
de domínio nem repositório, e enfiá-lo em `service/` embaralharia "regra de negócio" com
"detalhe de transporte". Deve vir comentado, no estilo do projeto.

---

# Parte V — Dependências entre os módulos

```
┌──────────────────────────────────────────────────────────────┐
│  Base (captura) — V5, V6, V7                                 │
│  status_evento · data_retorno_previsto · evento_origem_id     │
│  peso_kg · status_vital · data_obito · raca_referencia        │
│  patologia · diagnostico · medicamento · prescricao · item    │
└───────────────┬──────────────────────────────┬───────────────┘
                │                              │
                ▼                              ▼
   ┌────────────────────────┐      ┌───────────────────────────┐
   │  Módulo 1 — Painel     │      │  Módulo 2 — Dados + IA    │
   │  (leitura + agregação) │      │  (recuperação + LLM)      │
   └────────────┬───────────┘      └───────────┬───────────────┘
                │                              │
                │   sugestao_ia_decisao ───────┘
                ▼   (KPI de adoção da IA)
        ┌───────────────────┐
        │  Tela do vet      │
        └───────────────────┘
```

**Direção real da dependência — e ela é o contrário do que o enunciado sugere.** A hipótese
era "Dados + IA alimenta o Painel". Na prática:

| Direção | Existe? | Detalhe |
|---|---|---|
| Tabelas do Módulo 2 → Painel | ✅ **forte** | `prescricao`/`item_prescricao`/`medicamento` são **pré-requisito** de R3 (gasto com medicamento). `diagnostico`/`patologia` habilitam recortes por patologia no painel |
| Painel → Módulo 2 | ⚠️ fraca | `CalculadoraDeRisco` e as faixas etárias podem ser reaproveitadas pela IA; nada mais |
| **LLM → Painel** | ❌ **nenhuma** | **O Painel não deve depender do provedor de IA em nenhum ponto.** Se a IA cair, o painel continua inteiro |
| Módulo 2 → Painel (métrica) | ✅ opcional | `sugestao_ia_decisao` permite um KPI de adoção ("quantas sugestões foram aceitas") — é enfeite, não fundação |

**Consequência para o sequenciamento:** as tabelas clínicas da V7 pertencem logicamente ao
Módulo 2, mas o Módulo 1 depende delas. Ou o Painel entrega sem R3 numa primeira volta, ou
a V7 é implementada antes dos dois módulos. **A segunda opção é a recomendada** — ver
[Parte IX](#parte-ix--ordem-sugerida-de-implementação).

---

# Parte VI — Pontos de atenção e riscos técnicos

Ordenados por impacto sobre a implementação.

### 1. Banco compartilhado com a API .NET — risco de colisão · **Alto**

O Oracle é compartilhado e o lado .NET trata "produtos, sugestões de produtos, lembretes e
eventos". Criar `medicamento`, `produto` ou qualquer tabela de catálogo sem combinar antes
pode colidir com objeto existente, ou duplicar em silêncio um catálogo que já existe. Pior:
como as duas APIs se integram **só pelo banco**, uma divergência de catálogo aparece como
dado inconsistente, não como erro. **Bloqueante para a V7.**

### 2. `ddl-auto=validate` — entidade sem migration derruba o boot · **Alto**

Toda entidade ou campo novo exige migration **nos dois conjuntos** (`oracle/` e `mysql/`).
Esquecer o par MySQL não quebra o build nem os testes (que rodam em H2 `MODE=Oracle`) — só
quebra o deploy. `MigrationsMySqlTest` existe justamente para pegar isso e **precisa ser
estendido** a cada migration nova.

### 3. `animal.raca` é texto livre — a agregação por raça é o módulo inteiro · **Alto**

O eixo central do Painel (raça) não tem catálogo, normalização nem constraint. Sem
`raca_referencia` + normalização, "atendimentos por raça" produz grupos duplicados e
percentuais errados. O paliativo `UPPER(TRIM(...))` resolve caixa e espaço, **não** acento
nem sinônimo ("Pastor Alemão" vs "Pastor Alemao" vs "German Shepherd"). Relacionado: item
12 de `docs/07` (`especie` e `porte` como texto livre, com `CAO`/`CACHORRO` convivendo na
base).

### 4. Escopo do veterinário não existe na camada de segurança · **Alto**

`UsuarioAutenticado` não expõe `getVeterinarioId()` e `SegurancaService` não tem
`veterinarioIdParaFiltro()`. **Sem isso, o painel do vet A serve dados do vet B** — é
exatamente o bug de vazamento entre tutores que o projeto já corrigiu uma vez, na mesma
forma. Precisa vir **antes** de qualquer endpoint de painel, e com teste de regressão no
padrão de `OwnershipTest`.

### 5. Cache pode vazar dado entre veterinários · **Alto**

Duas armadilhas: (a) nome de cache não declarado em `CacheConfig` não funciona; (b) chave
sem o id do veterinário serve o painel de um vet para outro. A chave precisa incluir
`@seguranca.veterinarioIdParaFiltro()`, **e também a janela de datas** — `de`/`ate`
diferentes com a mesma chave devolvem o período errado, repetindo o defeito do item 8 de
`docs/07` (sort fora da chave).

### 6. Todos os `@ManyToOne` são EAGER — agregação carregando o grafo · **Médio**

Uma query analítica que devolva entidades traz veterinário, animal, tutor e clínica por
linha. **Usar projections** (interface ou `SELECT new`) em todas as consultas de agregação.
É desvio consciente do padrão de repositório atual e deve ser comentado.

### 7. Índices ausentes para o padrão de acesso analítico · **Médio**

Só existe `idx_usuario_email`. As agregações filtram por `veterinario_id` + `data_evento` e
por `animal_id` + `data_evento`, e navegam `pagamento.evento_id`. Sem índice, cada carga do
painel varre `evento_clinico` inteira — em banco de conta de aula, compartilhado, isso é
visível. Criar junto das migrations V5/V7.

### 8. Volume de dados insuficiente para agregação com sentido · **Médio**

O seed V2 tem 5 tutores, 6 animais, 11 eventos e 8 pagamentos. Nenhuma agregação por raça,
cluster ou benchmark produz número interpretável nessa escala — e testes de agregação
precisam de dados determinísticos. Será necessário um **seed analítico** (migration
adicional aplicada só em `dev`/`h2`, ou fixtures criadas pelo próprio teste). Nota
relacionada: Database exige mínimo de 5 registros por tabela, o que também vale para as
tabelas novas.

### 9. Chamada a LLM dentro de request HTTP síncrono · **Médio**

Custo por chamada, latência de segundos, indisponibilidade do provedor, e um cliente em
laço gerando conta. Mitigações em R12 — timeout, degradação graciosa, faixa própria de rate
limit, interruptor de desligamento. **A degradação graciosa é a mais importante**: a IA
indisponível não pode derrubar o atendimento.

### 10. Dado clínico saindo para provedor externo · **Médio (compliance)**

Prontuário é dado sensível; o tutor é pessoa identificável. Sem o `AnonimizadorClinico`
(R11), a implementação mais direta manda nome do tutor e do pet para fora. Isso é assunto
da disciplina de Compliance/QA, não só técnico.

### 11. `CLOB`/`LONGTEXT` com `ddl-auto=validate` · **Médio**

`@Lob String` em Oracle costuma exigir ajuste (`@JdbcTypeCode(SqlTypes.LONGVARCHAR)`) para
o `validate` não acusar divergência de tipo, e o comportamento difere entre Oracle e MySQL.
**Verificar cedo, com uma entidade mínima**, antes de construir sobre isso. Alternativa
conservadora: `VARCHAR2(4000)` com truncamento explícito do payload.

### 12. `evento_clinico.hora` é `VARCHAR2(5)` e `data_evento` é `DATE` · **Baixo**

Não há timestamp real do atendimento. Agregação por hora do dia ou ordenação precisa dentro
do mesmo dia são inviáveis sem concatenar strings. **Todas as métricas propostas são
diárias ou maiores** — o ponto só importa se alguém pedir "horário de pico".

### 13. Semântica de `DEFAULT 'REALIZADO'` no histórico · **Baixo, mas visível**

Marcar todo evento antigo como realizado torna a taxa de comparecimento histórica igual a
100%, o que é falso e some com o problema que o produto quer medir. Assumir e comunicar:
métricas de comparecimento só valem **a partir da data da migration**. A alternativa (nulo
no histórico) complica toda agregação — a escolha é entre dois males, e este é o menor.

### 14. `CorsConfiguration` não lista `PATCH` · **Informativo**

`SecurityConfig.corsConfigurationSource` permite `GET, POST, PUT, DELETE, OPTIONS`. Os
endpoints novos são GET e POST, então **não afeta estes módulos** — mas explica um PATCH
que falhe pelo navegador em outro contexto. Registrado aqui para não se perder.

---

# Parte VII — Suposições declaradas

Onde o código não decidiu por mim, assumi o seguinte. **Cada uma é revogável** e vira
pergunta correspondente na Parte VIII.

| # | Suposição | Base | Se estiver errada |
|---|---|---|---|
| 1 | Nenhuma tabela de receita, prescrição, medicamento, diagnóstico ou óbito existe no schema deste repositório | grep no DDL das 4 migrations e nas 8 entidades | se a API .NET já tiver equivalentes no Oracle compartilhado, **reusar em vez de criar** — muda a V7 inteira |
| 2 | Alvo primário do schema é Oracle, com MySQL espelhado | `spring.profiles.active=oracle`; dois conjuntos de migrations | se o MySQL virar o único alvo, o conjunto `oracle/` para de crescer |
| 3 | Os módulos são de leitura, exceto: registro de sugestão/decisão de IA e o CRUD das tabelas clínicas novas | enunciado + necessidade de rastreabilidade | se escrita for proibida, a IA fica sem auditoria e a promessa "não decide sozinha" não é verificável |
| 4 | O painel é sempre "de um veterinário"; ADMIN pode ver o de qualquer um; TUTOR não acessa | padrão de ownership do `SegurancaService` | painel por clínica (gestor) é outro recorte, com outro conjunto de queries |
| 5 | "Região" do benchmark = cidade ou estado da clínica | `Endereco` tem cidade/estado, não tem lat/long | raio em km exige geocodificação — mudança maior |
| 6 | Óbito é registrado no cadastro do animal, não como tipo de evento clínico | `TipoEvento` não tem `OBITO`; é atributo de estado, não de agenda | se virar evento, `status_vital` sai e a métrica muda de forma |
| 7 | Peso é medido **por atendimento** (`evento_clinico.peso_kg`), não atributo do animal | peso varia; validação de dose precisa do valor da época | se for atributo do animal, perde-se a série temporal |
| 8 | `Response` é `record`, `Request` é classe Lombok `@Getter` | padrão uniforme nos 6 recursos | — |
| 9 | Novos endpoints ficam sob `/api/v1` sem versão própria | `WebConfig.PREFIXO_API` aplicado por pacote | — |
| 10 | Paginação nas listas que podem crescer (ranking, risco, clusters); objeto único nos KPIs de cabeçalho | `Page` é o padrão do projeto; um resumo não é lista | — |
| 11 | Expectativa de vida por raça vem de tabela de referência **carregada manualmente**, com fonte obrigatória | não existe base pública canônica em pt-BR | se a Clyvo tiver base própria, muda a carga, não o schema |
| 12 | Cluster de patologias é **estatística determinística, sem LLM** | é contagem; LLM só adicionaria custo e incerteza | se exigirem IA no cluster, a arquitetura de R9 muda |
| 13 | Provedor de IA é externo via HTTP, com chave em variável de ambiente | não há nenhuma integração hoje; precedente de segredo por ambiente | modelo local mudaria `integration/`, não os contratos |
| 14 | Testes seguem `TesteDeApi` (integração real, sem rollback) + unitários puros para as calculadoras | padrão da suíte | — |

---

# Parte VIII — Perguntas em aberto

Ordenadas por quanto bloqueiam. As três primeiras precisam de resposta **antes** da primeira
linha de migration.

### VIII.1 — O schema compartilhado com a API .NET

1. **Quais tabelas a API .NET já mantém no Oracle compartilhado?** Existe DDL ou
   documentação desse lado? *(Bloqueante para toda a V7.)*
2. A tabela de **produtos** do lado .NET já contém medicamentos, com princípio ativo e
   classe terapêutica? Se sim, `medicamento` deve ser **descartada** e substituída por
   leitura da tabela existente.
3. Há convenção de **prefixo ou schema** acordada entre as duas APIs para evitar colisão de
   nome de tabela?
4. Quem tem autoridade para criar objeto no schema compartilhado — as duas APIs criam
   livremente, ou há um dono?

### VIII.2 — Óbito e desfecho

5. **Quem registra o óbito, e quando?** O vet no atendimento, a recepção, o tutor pelo app?
6. Como tratar óbito **fora da clínica**, que ninguém comunica? Hoje esse pet vira "sumido"
   e entra na lista de risco de abandono — cobrando retorno de um animal morto.
7. A janela de +90 dias após o fim do período, em R2, faz sentido para a operação, ou o
   correto é atribuir o óbito ao vet do **último** atendimento, independente da janela?
8. `causa_obito` deve ser texto livre ou apontar para `patologia`?

### VIII.3 — Status do evento e retorno

9. **Quem marca `status_evento`, e em que momento da rotina da clínica?** *(Já registrada em
   `docs/09` e ainda sem resposta. Sem ela, o campo nasce despovoado e todas as métricas de
   comparecimento valem zero.)*
10. **O retorno é agendado no ato da consulta, ou o tutor liga depois?** Determina se
    `data_retorno_previsto` é preenchido de forma confiável ou fica quase sempre nulo.
11. Existe tolerância padrão para "retorno cumprido" na prática da clínica? Assumi 30 dias.
12. Evento com `status_evento = AGENDADO` cuja data já passou: vira `FALTOU`
    automaticamente por rotina, ou depende de alguém marcar?

### VIII.4 — Painel: definições de negócio

13. "Taxa de retorno do tutor" significa **retorno prescrito cumprido** ou **recorrência do
    tutor**? Especifiquei as duas (R4) porque respondem perguntas diferentes — confirmar qual
    aparece na tela, ou se ambas.
14. Os **pesos do score de risco** (40/30/20/10) fazem sentido para quem conhece a operação?
    São chute fundamentado, não calibração.
15. Qual **n mínimo** para exibir taxa de óbito e tempo de vida ganho? Assumi 5.
16. Qual **n mínimo de veterinários** para liberar o benchmark regional sem identificar
    colega? Assumi 5.
17. O vet pode se comparar com colegas da **própria clínica**, ou o benchmark deve excluí-los?
18. Um pet atendido por vários vets da mesma clínica: o desfecho é atribuído a quem — ao
    último, ao que mais atendeu, ou a todos?

### VIII.5 — Medicamento e gasto

19. `item_prescricao.valor_total` é **o que a clínica cobrou** ou preço de tabela do produto?
    Muda o indicador de "custo do tratamento" para "receita de medicamento".
20. A clínica **vende** o medicamento ou apenas prescreve? Se apenas prescreve, o valor pode
    simplesmente não existir — e R3 (gasto por raça) precisa de outra fonte.
21. O objetivo declarado é "indicar parceria com farmácia". Isso exige **volume por
    princípio ativo**, não por raça. Confirmar se o corte por raça é mesmo o desejado, ou se
    é volume por medicamento com recorte de raça.

### VIII.6 — Base normativa de medicação

22. **Qual é a fonte normativa** da validação: MAPA, CFMV, bula do fabricante, literatura?
23. **Quem popula e mantém `restricao_medicamento`?** Sem manutenção, a validação envelhece
    e passa a dar falsa segurança — pior que não existir.
24. Há base de dados licenciável de interações medicamentosas veterinárias disponível, ou
    tudo será carga manual?
25. Aceita-se que o LLM cite normas **sem base documental local**? Recomendo fortemente que
    **não** — é onde a alucinação tem maior custo clínico.

### VIII.7 — Escopo de visibilidade da IA

26. "Casos semelhantes" busca em qual universo: **só a clínica do vet**, a rede toda, ou a
    base inteira? Assumi a clínica. Ampliar aumenta muito a utilidade e levanta questão de
    concorrência entre clínicas e de consentimento do tutor.
27. O tutor precisa **consentir** que o histórico do seu pet seja usado como caso de
    referência para outro atendimento, mesmo anonimizado?
28. Por quanto tempo guardar `sugestao_ia.entrada`/`saida`? Há política de retenção?

### VIII.8 — Forma dos endpoints

29. `/painel/veterinarios/{id}/...` (proposto) ou `/painel/me/...`? A primeira segue o padrão
    do projeto e permite auditoria por ADMIN.
30. O TUTOR terá alguma visão analítica do próprio pet (ex.: "seu pet está atrasado")? Não
    foi pedido; se sim, é um terceiro conjunto de endpoints, com outro recorte de segurança.
31. O front consome esses dados em tempo real ou aceita cache de 10 minutos, como as demais
    listagens?

### VIII.9 — Infraestrutura de IA

32. **Qual provedor de LLM, e quem paga?** Há teto de custo mensal?
33. Existe limite de latência aceitável na tela do vet? Assumi timeout de 20 s com
    degradação graciosa.
34. A camada de IA precisa funcionar **offline/local** em alguma demonstração — por exemplo,
    apresentação de banca sem internet confiável?

---

# Parte IX — Ordem sugerida de implementação

Cada bloco entrega algo verificável e não depende do seguinte. As perguntas bloqueantes
estão marcadas.

| # | Bloco | Entrega | Bloqueado por |
|---|---|---|---|
| 0 | **Escopo do veterinário na segurança** | `UsuarioAutenticado.getVeterinarioId()`, `SegurancaService.veterinarioIdParaFiltro()` e `podeAcessarPainelDoVeterinario`, com teste no padrão `OwnershipTest` | — |
| 1 | **V5 — status e continuidade** | `StatusEvento`, `data_retorno_previsto`, `evento_origem_id`, `peso_kg` + DTOs, filtros por data e índices | pergunta 9, 10 |
| 2 | **V6 — desfecho e catálogo de raça** | `StatusVital`, `data_obito`, `raca_referencia` + CRUD e normalização | pergunta 5, 6 |
| 3 | **Painel — fatia vertical** | endpoints 1, 2 e 5 (resumo, atendimentos por raça, retenção) ponta a ponta, com cache e teste | blocos 0–2 |
| 4 | **Painel — desfecho e risco** | endpoints 3, 6, 7 + `CalculadoraDeRisco` com teste unitário e parametrizado | bloco 2; perguntas 14–15 |
| 5 | **Painel — benchmark** | endpoint 8 com regra de k-anonimato | perguntas 16, 17 |
| 6 | **V7 — prontuário estruturado** | `patologia`, `diagnostico`, `medicamento`, `prescricao`, `item_prescricao`, `restricao_medicamento` + CRUDs | **perguntas 1–4** (colisão com .NET) |
| 7 | **Painel — gasto com medicamento** | endpoint 4, fechando o Módulo 1 | bloco 6; perguntas 19–21 |
| 8 | **IA determinística** | clusters (R9) e camada 1 da validação (R10) — **sem nenhum LLM** | bloco 6; perguntas 22–24 |
| 9 | **V8 + integração LLM** | `sugestao_ia`, `sugestao_ia_decisao`, `AnonimizadorClinico`, `ClienteIa`, casos semelhantes (R8) e camada 2 (R10) | bloco 8; perguntas 26, 32 |
| 10 | **Seed analítico e testes de agregação** | dados determinísticos em `dev`/`h2` + testes das métricas | pode andar em paralelo a partir do bloco 3 |

> **A ordem tem uma lógica que vale explicitar:** os blocos 8 e 9 estão separados de
> propósito. Toda a parte da IA que é **estatística** roda sem provedor externo, sem custo e
> sem risco de alucinação — e é a que sustenta as demonstrações. O LLM entra por último,
> como camada aditiva, sobre uma base que já funciona sozinha. Se ele falhar, atrasar ou
> ficar caro demais, **nada do que foi entregue antes deixa de funcionar**.

---

## Referências no repositório

| Assunto | Arquivo |
|---|---|
| Arquitetura e responsabilidades por camada | [`docs/01-arquitetura.md`](docs/01-arquitetura.md) |
| Modelo de dados atual | [`docs/02-modelo-de-dados.md`](docs/02-modelo-de-dados.md) |
| Convenções REST, paginação e filtros | [`docs/03-api-rest.md`](docs/03-api-rest.md) |
| Pendências abertas do projeto | [`docs/07-pendencias-e-divergencias.md`](docs/07-pendencias-e-divergencias.md) |
| Segurança e ownership | [`docs/08-seguranca.md`](docs/08-seguranca.md) |
| Estado do projeto e roadmap de produto | [`docs/09-estado-do-projeto.md`](docs/09-estado-do-projeto.md) |
| Exigências das outras disciplinas sobre este backend | [`specs/04-dependencias-externas.md`](specs/04-dependencias-externas.md) |
