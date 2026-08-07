# Verificação e checklist pré-Sprint 3

Auditoria do repositório contra as specs, feita em **07/08/2026**, 36 dias antes da
entrega da Sprint 3.

**Método:** leitura do código, `git log`, contagem no seed e compilação do projeto.
Cada linha abaixo tem evidência — nada foi marcado por presunção.

```
mvn -o compile   → BUILD SUCCESS
```

`mvn clean` falha offline por dependência não cacheada do `maven-clean-plugin`; não é
problema do projeto.

---

## Requisitos técnicos da Sprint 1/2 — todos verificados

| Requisito | Situação | Evidência |
|---|---|---|
| Bean Validation | ✅ completo | 7 Requests anotados; `@Valid` em todos os POST/PUT |
| Paginação | ✅ completo | `@PageableDefault(size = 10)` nos 6 GETs de lista |
| Ordenação | ✅ completo | `sort` do Spring Data, default por recurso |
| Busca com parâmetros | ✅ completo | 6 `buscarPorFiltros` em JPQL, 2 filtros cada |
| Cache | ✅ completo | `@EnableCaching` + `@Cacheable`/`@CacheEvict` nos 6 services |
| Tratamento de erros | ✅ completo | `GlobalExceptionHandler` com 400 e 404 |
| DTOs | ✅ completo | 14 DTOs; entidades nunca expostas |
| Swagger | ✅ completo | springdoc + `@Tag`/`@Operation` em 100% dos endpoints |
| Export Postman/Insomnia | ❌ **ausente** | nunca existiu em nenhum commit ou branch |

Oito de nove. O único não atendido vale até 10 pontos e é o mais barato de resolver.

---

## Verificações que mudaram o diagnóstico

### 1. A coleção Insomnia nunca foi commitada

O commit `2b2108d` tem a mensagem *"docs: adiciona colecao Insomnia com testes dos
endpoints"*, mas seu diff contém apenas seis PNGs e uma alteração em
`AnimalResponse.java`. Uma busca no histórico completo não encontra o arquivo:

```
git log --all -- '*insomnia*' '*postman*'   → vazio
```

O README afirma em dois lugares que a coleção está em `documentos/`. O requisito pede
o export **na pasta `documentos/`** e vale até 10 pontos.

### 2. Todo o histórico tem um único autor

```
24 commits  leojp04 <leojp04@gmail.com>
 5 commits  leop04  <leojp04@gmail.com>
```

Mesmo e-mail, duas grafias de nome — é a mesma pessoa. Nenhum dos outros três
integrantes aparece no histórico, em 29 commits ao longo de 21 dias.

A Sprint 4 desconta **−10 pontos por "ausência de evidência de colaboração entre
membros"**, e a avaliação oral da Sprint 3 é individual: cada aluno precisa explicar
trechos do próprio código. Um histórico com autor único não sustenta nenhum dos dois.

Esse é o achado mais sério da auditoria, porque não é corrigível retroativamente — só
muda com o padrão de trabalho da Sprint 3 em diante.

### 3. O seed está abaixo do mínimo da disciplina de Database

| Tabela | Registros | Mínimo exigido |
|---|---|---|
| `clinica` | 4 | 5 |
| `tutor` | **2** | 5 |
| `veterinario` | 7 | 5 ✅ |
| `animal` | **3** | 5 |
| `evento_clinico` | 11 | 5 ✅ |
| `pagamento` | 8 | 5 ✅ |

Três tabelas abaixo do mínimo. A penalidade é da disciplina de Database, mas o seed
vive neste repositório e vai virar migration Flyway na Sprint 3 — corrigir antes evita
refazer.

### 4. Nada da Sprint 3 está preparado

```
grep -in "security|flyway|thymeleaf" pom.xml   → nenhum resultado
```

As três dependências centrais da Sprint 3 não existem. Também não há `@Transactional`
em lugar nenhum, o que passa a importar quando os fluxos não-CRUD gravarem mais de uma
entidade por operação.

---

## O que melhorar antes de começar

Ordenado por relação custo/benefício. Os quatro primeiros cabem em um dia de trabalho
e destravam o resto.

### Prioridade 1 — fazer antes de escrever qualquer código da Sprint 3

| # | Ação | Por quê | Esforço |
|---|---|---|---|
| 1 | **Distribuir o trabalho entre os 4 integrantes, com commits próprios** | −10 pts na S4 e avaliação oral individual na S3. Não é recuperável depois | contínuo |
| 2 | **Exportar e commitar a coleção Insomnia em `documentos/`** | até 10 pts da S1/S2 ainda em aberto; README já promete o arquivo | 30 min |
| 3 | **Corrigir `REEMBOLSADO` → `ESTORNADO`** | Enum viola o check constraint do Oracle; impede gravar o status. Corrigir antes de virar migration | 15 min |
| 4 | **Tirar as credenciais do `application-oracle.properties`** | −20 pts em DevOps S3 por dados sensíveis no código-fonte | 30 min |

### Prioridade 2 — higiene que evita retrabalho nas migrations

| # | Ação | Por quê | Esforço |
|---|---|---|---|
| 5 | Ampliar `@Size` de `crmv` (4–6) e `telefone` (10–11) | Rejeitam o formato dos dados do próprio seed; travam PUT de registros existentes | 15 min |
| 6 | Tornar `dataPagamento` condicional ao status | Impossível registrar pagamento `PENDENTE` hoje | 1 h |
| 7 | Completar o seed para ≥ 5 registros em `clinica`, `tutor` e `animal` | Mínimo da disciplina de Database; o seed vira `V2__seed_inicial.sql` | 1 h |
| 8 | Null-guard em `endereco` e `sexo` nos mappers | 500 ao ler registro inserido direto no SQL Developer | 30 min |
| 9 | Tratar `DataIntegrityViolationException` → 409 | CPF/CNPJ/CRMV duplicado retorna 500 hoje | 1 h |

### Prioridade 3 — preparação estrutural

| # | Ação | Por quê | Esforço |
|---|---|---|---|
| 10 | Fixar perfil de teste (`src/test/resources/application.properties` com `dev`) | O único teste falha sem Oracle; o CI da S4 exige testes rodando | 15 min |
| 11 | Incluir o `sort` na chave dos `@Cacheable` | Bug real: ordenações diferentes colidem na mesma chave | 30 min |
| 12 | Adicionar `@Transactional` nos services | Necessário quando os fluxos não-CRUD gravarem múltiplas entidades | 30 min |
| 13 | Escolher os 2 fluxos não-CRUD | Trava segurança, telas e testes. Validar na mentoria de 21/08 | decisão |
| 14 | Decidir o banco em nuvem | H2 deixa de ser aceito; define as migrations Flyway | decisão |

---

## Prontidão por requisito da Sprint 3

| Requisito | Pontos | Prontidão | Falta |
|---|---|---|---|
| Frontend | 30 | 0% | Dependência, layout base, telas dos 2 perfis |
| Flyway | 20 | ~40% | Dependência e migrations — mas o DDL já existe e é convertível |
| Spring Security | 30 | 0% | Dependência, entidade `Usuario`, `SecurityFilterChain`, perfis |
| Funcionalidades (2 fluxos não-CRUD) | 20 | 0% | Fluxos nem escolhidos |

O Flyway é o único item com alguma base pronta. Os outros 80 pontos partem do zero.

---

## Recomendação

Duas coisas se destacam do resto.

A primeira é **a distribuição de trabalho entre os integrantes**. Todos os outros itens
desta lista são corrigíveis em qualquer momento; o histórico de colaboração, não. Ele
se constrói ao longo da Sprint 3 ou não existe na entrega da Sprint 4. Dividir por
fluxo — cada integrante dono de um fluxo ponta a ponta, do service à tela — resolve ao
mesmo tempo o desconto de −10 e a avaliação oral individual.

A segunda é **fechar as decisões 13 e 14 antes da mentoria de 21/08**. São duas
semanas de folga para chegar na mentoria com os fluxos escolhidos e receber validação
da Clyvo, em vez de sair de lá ainda decidindo.

Os itens 1 a 4 valem cerca de 30 pontos somados entre disciplinas e custam menos de um
dia. Vale limpar antes de abrir a primeira migration.
