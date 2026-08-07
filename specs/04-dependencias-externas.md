# Dependências externas sobre o backend Java

As specs 01–03 cobrem o que a disciplina de Java Advanced cobra diretamente. Este
documento reúne o que **outras disciplinas exigem deste repositório** — são requisitos
que recaem sobre o backend mesmo não sendo avaliados pelo professor de Java.

Cada disciplina tem sua própria entrega e seus próprios critérios; aqui está apenas o
recorte que afeta o código Java.

---

## DevOps Tools & Cloud Computing

A disciplina permite escolher entre a solução de **Java Advanced** ou a de .NET. Se o
grupo escolher Java — como fez na Sprint 1 — o backend precisa atender:

### Sprint 3

| Exigência | Impacto no backend |
|---|---|
| Banco de dados em nuvem obrigatório | **H2 não será aceito** — o perfil `h2`, usado hoje no Docker e no deploy Azure, deixa de servir |
| Bancos aceitos | Oracle (container em nuvem ou o da FIAP), MySQL, Azure SQL, PostgreSQL |
| DDL em `script_bd.sql` | Arquivo separado, com estrutura e comentários |
| Tabelas do CORE | Nada de cadastro auxiliar — as tabelas do CRUD demonstrado devem ser centrais à solução |
| CRUD sobre ≥ 2 tabelas relacionadas | Atendido pelo domínio atual |
| Container do app sem privilégio de root | Já atendido (usuário `appuser` no Dockerfile) |
| Dados sensíveis expostos no código-fonte | **−20 pontos** — hoje há usuário e senha do Oracle em texto puro em `application-oracle.properties` |

Duas opções mutuamente exclusivas: **ACR + ACI** (app e banco containerizados) ou
**App Service + banco PaaS** (nada containerizado). Misturar: −40 pontos.

### Sprint 4

| Exigência | Impacto no backend |
|---|---|
| Pipeline CI no Azure DevOps | Build + **execução de testes** automatizada. Hoje o build usa `-DskipTests` e o único teste falha sem Oracle |
| Publicação de artefato no CI | O JAR gerado precisa ser publicado |
| Pipeline CD | Deploy automático em Azure Web App ou ACI |
| CI dispara a cada alteração na branch `master` | Branch atual do repositório é `main` |
| Variáveis de ambiente protegidas | Credenciais fora do código, em variáveis seguras da pipeline |

---

## Mobile Application Development

Na Sprint 3, o app React Native deve integrar-se a um backend HTTP, que pode ser a API
de Java ou de .NET.

| Exigência do mobile | O que o backend precisa oferecer |
|---|---|
| CRUD completo integrado à interface, em **duas funcionalidades distintas** | Endpoints de create, read, update e delete — já existem |
| Autenticação real via serviço externo **ou API de Java/.NET** | Se não usarem Firebase, o backend precisa expor login e emitir token |
| Dados exclusivamente da API, sem mock | API acessível pelo app (rede, CORS, URL pública) |
| Alterações refletidas sem reiniciar o app | Nada especial — mas o cache em memória do backend pode servir dados stale |

Se a autenticação do mobile vier deste backend, o Spring Security da Sprint 3 precisa
expor autenticação por token (JWT), não só login por formulário — os dois modos
coexistem na mesma aplicação, mas é uma decisão a tomar cedo.

---

## Mastering Relational and Non-Relational Database

### Sprint 3

| Exigência | Impacto |
|---|---|
| Modelagem no Oracle Data Modeler, DER (Barker) + MER, mínimo 3FN | As tabelas devem **coincidir com os objetos do back-end** |
| 2 procedures, 2 functions, 1 trigger de auditoria | Objetos criados no mesmo schema usado pelo backend |
| Mínimo 5 registros por tabela | O seed atual tem 2 tutores e 3 animais — **abaixo do mínimo** |

### Sprint 4

| Exigência | Impacto |
|---|---|
| Objetos empacotados (packages PL/SQL) | Organização no banco |
| **Procedures chamadas via aplicação**, demonstradas em vídeo | O backend Java precisa invocar procedures — hoje só usa JPQL. Requer `@Procedure` ou `StoredProcedureQuery` |
| Mesma base relacional usada pelo backend | Reforça o alinhamento schema ↔ entidades |

---

## Disruptive Architectures: IoT, IoB & Generative IA

| Sprint | Exigência | Impacto |
|---|---|---|
| 3 | Documentar o componente de IA e o fluxo de dados entre usuários, aplicação, banco e IA | Diagrama arquitetural incluindo esta API |
| 4 | **Implementar** a IA integrada à aplicação | Backend precisa expor/consumir o componente de IA e alimentá-lo com dados do histórico clínico |

Os dados citados no documento — perfil do pet, histórico clínico, vacinas, consultas,
medicamentos — mapeiam para as entidades `Animal`, `EventoClinico` e `TipoEvento` já
existentes.

---

## Resumo dos conflitos com o estado atual

| # | Conflito | Origem | Prazo |
|---|---|---|---|
| 1 | H2 não aceito em nuvem; perfil `h2` é a base do Docker e do `deploy.sh` | DevOps S3 | 12/09/2026 |
| 2 | Credenciais em texto puro no repositório (−20 pts) | DevOps S3 | 12/09/2026 |
| 3 | Build com `-DskipTests`; CI exige execução de testes | DevOps S4 | 04/11/2026 |
| 4 | Branch `main` vs. gatilho de CI em `master` | DevOps S4 | 04/11/2026 |
| 5 | Seed com menos de 5 registros em `tutor` e `animal` | Database S3 | 12/09/2026 |
| 6 | Backend não chama procedures | Database S4 | 04/11/2026 |
| 7 | Sem autenticação para o app mobile consumir | Mobile S3 | 12/09/2026 |
