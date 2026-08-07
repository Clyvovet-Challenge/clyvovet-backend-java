# Sprint 1 e 2 — Java Advanced

**Entrega:** 24/05 · **Peso:** 100 pontos · **Status:** entregue

No 1º semestre há uma única entrega, apresentada nas duas sprints. Para receber nota
nas duas, é necessário entregar nas duas.

---

## Descrição geral (texto oficial)

> Desenvolva uma solução tecnológica proposta utilizando conceitos de Java e Spring Boot.
>
> A solução deve ser, no mínimo, capaz de persistir, gerenciar e consultar dados em um
> SGBD relacional (H2 ou Oracle). No entanto, é importante destacar que a implementação
> apenas de operações de CRUD **não será considerada suficiente** para resolver de forma
> eficaz o problema proposto neste Challenge.

---

## Solicitação

| # | Requisito | Status |
|---|---|---|
| 1 | Aplicação Java com Spring Boot que ajude a resolver um problema do contexto (usar criatividade) | Parcial — CRUD, sem fluxo de negócio |
| 2 | Respeitar fundamentos de POO; entidades relacionadas e mapeadas com JPA | ✅ 6 entidades + `@Embeddable`, 6 associações `@ManyToOne` |
| 3 | Código com coesão e desacoplamento | ✅ Controller → Service → Repository → Entity, DTOs e mappers |
| 4 | Uso adequado de padrões de projeto e validação funcional | ✅ DTO, Mapper, Repository, injeção por construtor |
| 5 | Respeitar os conceitos fundamentais de APIs REST (RESTful) | ✅ Recursos no plural, verbos corretos, status adequados |
| 6 | Utilizar Design Patterns com prudência | ✅ Sem over-engineering |
| 7 | Pode utilizar JPQL e/ou Spring JPA Query Methods | ✅ JPQL com filtros opcionais nos 6 repositories |
| 8 | Artefatos no GitHub público, professores com acesso | ✅ `leojp04/clyvovet-backend-java` |

---

## Requisitos técnicos obrigatórios

| Requisito | Status | Onde |
|---|---|---|
| Validação de campos com Bean Validation | ✅ | 7 Requests com `@NotBlank`, `@NotNull`, `@Size`, `@Email`, `@Positive`, `@PastOrPresent`, `@Digits` |
| Paginação de resultados | ✅ | `Pageable` + `@PageableDefault(size = 10)` nos 6 GETs de lista |
| Ordenação de resultados | ✅ | `sort` do Spring Data; default por recurso |
| Busca com parâmetros | ✅ | 2 filtros opcionais por recurso via JPQL |
| Uso de cache para otimizar requisições | ✅ | `@EnableCaching` + `@Cacheable`/`@CacheEvict` nos 6 services |
| Tratamento de erros/exceções | ✅ | `GlobalExceptionHandler`: 400 para validação, 404 para `EntityNotFoundException` |
| Utilização de DTOs | ✅ | 7 Request + 7 Response, entidades nunca expostas |
| Documentação com Swagger | ✅ | springdoc-openapi, `@Tag`/`@Operation` em português |
| Testes dos endpoints (Postman/Insomnia) exportados | ⚠️ | Coleção citada no README mas **ausente** de `documentos/` |

Detalhamento técnico de cada item em [`../docs/`](../docs/).

---

## Distribuição da pontuação

| Pontos | Critério | Status |
|---|---|---|
| até 5 | Cronograma de desenvolvimento, respeitando prazos. Documento dizendo **quem fará o quê e quando** | ✅ `documentos/Cronograma_CLYVOVET.pdf` |
| até 10 | Imagens explicativas da arquitetura, definição das classes de domínio e **Diagrama de Classes de Entidade**. DER e Diagrama de Classes devem ser **coerentes entre si**. Explicar relacionamentos e constraints | ⚠️ Diagrama existe; coerência com o DER da disciplina de Database precisa ser verificada |
| até 40 | Implementação das classes de Entidade necessárias para a solução | ✅ 6 entidades + `Endereco` + 5 enums |
| até 15 | Aplicação respeita REST/RESTful e está de acordo com o **modelo de maturidade** | ⚠️ Nível 2 de Richardson (recursos + verbos + status). Nível 3 exigiria HATEOAS |
| até 10 | Gestão de configuração: todos os artefatos no GitHub, professores com acesso | ✅ |
| até 10 | Envio do link do projeto público no GitHub | ✅ |
| até 10 | Preocupação em testar a aplicação, provada com documentos. Export do Postman/Insomnia **na pasta `documentos/`**, com persistência e recuperação perfeitas | ⚠️ Export ausente |

**Total: 100 pontos.**

---

## Pendências desta entrega

Itens que ainda custam nota e são baratos de resolver:

| # | Pendência | Impacto | Verificado em 07/08/2026 |
|---|---|---|---|
| 1 | Coleção Insomnia/Postman nunca foi commitada — o commit `2b2108d`, cuja mensagem diz "adiciona colecao Insomnia", adicionou apenas 6 PNGs. `git log --all -- '*insomnia*' '*postman*'` não retorna nada | até 10 pts | ✅ confirmado |
| 2 | Coerência entre o Diagrama de Classes de Entidade e o DER entregue em Database | até 10 pts | não verificável neste repo |
| 3 | Divergências entre código e schema que quebram a "perfeita persistência e recuperação" exigida | até 10 pts | ✅ confirmado |
| 4 | HATEOAS ausente — limita o modelo de maturidade ao nível 2. Nenhuma referência a `EntityModel`/`WebMvcLinkBuilder` no código | parte dos 15 pts | ✅ confirmado |

Sobre o item 3, o levantamento completo está em
[`../docs/07-pendencias-e-divergencias.md`](../docs/07-pendencias-e-divergencias.md).
Os que afetam diretamente a demonstração de persistência:

- `StatusPagamento.REEMBOLSADO` viola o check constraint do Oracle (que espera `ESTORNADO`)
- `@Size` em `crmv` (4–6) e `telefone` (10–11) rejeita o formato dos dados do próprio seed
- `dataPagamento` obrigatória impede registrar pagamento `PENDENTE`
- Unicidade de CPF/CNPJ/CRMV só existe no banco → duplicata retorna 500, não 409
