# Testes de CRUD e Integração

> 113 nodes · cohesion 0.08

## Key Concepts

- **org.junit.jupiter.api.Test** (121 connections)
- **org.junit.jupiter.api.DisplayName** (119 connections)
- **TesteDeApi** (34 connections) — `src/test/java/br/com/fiap/clyvovet/support/TesteDeApi.java`
- **.tokenAdmin()** (34 connections) — `src/test/java/br/com/fiap/clyvovet/support/TesteDeApi.java`
- **.buscar()** (33 connections) — `src/test/java/br/com/fiap/clyvovet/support/TesteDeApi.java`
- **.criar()** (30 connections) — `src/test/java/br/com/fiap/clyvovet/support/TesteDeApi.java`
- **.corpoDe()** (25 connections) — `src/test/java/br/com/fiap/clyvovet/support/TesteDeApi.java`
- **.removerDepois()** (16 connections) — `src/test/java/br/com/fiap/clyvovet/support/TesteDeApi.java`
- **.tokenVeterinaria()** (16 connections) — `src/test/java/br/com/fiap/clyvovet/support/TesteDeApi.java`
- **FiltrosDeBuscaTest** (15 connections) — `src/test/java/br/com/fiap/clyvovet/crud/FiltrosDeBuscaTest.java`
- **.cicloDeVidaDoPagamento()** (13 connections) — `src/test/java/br/com/fiap/clyvovet/crud/AtendimentoCrudTest.java`
- **.remover()** (13 connections) — `src/test/java/br/com/fiap/clyvovet/support/TesteDeApi.java`
- **AtendimentoCrudTest** (12 connections) — `src/test/java/br/com/fiap/clyvovet/crud/AtendimentoCrudTest.java`
- **.cicloDeVidaDoEventoClinico()** (12 connections) — `src/test/java/br/com/fiap/clyvovet/crud/AtendimentoCrudTest.java`
- **OwnershipTest** (12 connections) — `src/test/java/br/com/fiap/clyvovet/security/OwnershipTest.java`
- **.tokenTutor()** (12 connections) — `src/test/java/br/com/fiap/clyvovet/support/TesteDeApi.java`
- **.pagamentoAceitaStatusReembolsado()** (11 connections) — `src/test/java/br/com/fiap/clyvovet/crud/AtendimentoCrudTest.java`
- **.nomesEm()** (11 connections) — `src/test/java/br/com/fiap/clyvovet/crud/FiltrosDeBuscaTest.java`
- **ValidacaoDeEntradaTest** (11 connections) — `src/test/java/br/com/fiap/clyvovet/crud/ValidacaoDeEntradaTest.java`
- **AutorizacaoTest** (11 connections) — `src/test/java/br/com/fiap/clyvovet/security/AutorizacaoTest.java`
- **.atualizar()** (11 connections) — `src/test/java/br/com/fiap/clyvovet/support/TesteDeApi.java`
- **.totalDe()** (11 connections) — `src/test/java/br/com/fiap/clyvovet/support/TesteDeApi.java`
- **.cicloDeVidaDoAnimal()** (10 connections) — `src/test/java/br/com/fiap/clyvovet/crud/AtendimentoCrudTest.java`
- **CadastroCrudTest** (10 connections) — `src/test/java/br/com/fiap/clyvovet/crud/CadastroCrudTest.java`
- **.cicloDeVidaDaClinica()** (10 connections) — `src/test/java/br/com/fiap/clyvovet/crud/CadastroCrudTest.java`
- *... and 88 more nodes in this community*

## Relationships

- [Suporte de Testes de API](Suporte_de_Testes_de_API.md) (22 shared connections)
- [Clínicas e Endereço](Clínicas_e_Endereço.md) (18 shared connections)
- [Veterinários e Anotações Base](Veterinários_e_Anotações_Base.md) (13 shared connections)
- [Emissão e Leitura de JWT](Emissão_e_Leitura_de_JWT.md) (12 shared connections)
- [Entidade e Mapper de Tutor](Entidade_e_Mapper_de_Tutor.md) (10 shared connections)
- [DTOs de Autenticação e Perfis](DTOs_de_Autenticação_e_Perfis.md) (9 shared connections)
- [Testes de Mapper de Animal](Testes_de_Mapper_de_Animal.md) (8 shared connections)
- [Eventos Clínicos](Eventos_Clínicos.md) (8 shared connections)
- [Mapeamento de Evento e Pagamento](Mapeamento_de_Evento_e_Pagamento.md) (8 shared connections)
- [Entidade Animal](Entidade_Animal.md) (2 shared connections)
- [Pagamentos e Formas de Pagamento](Pagamentos_e_Formas_de_Pagamento.md) (2 shared connections)
- [Configuração de Segurança e Filtros](Configuração_de_Segurança_e_Filtros.md) (2 shared connections)

## Source Files

- `src/test/java/br/com/fiap/clyvovet/crud/AtendimentoCrudTest.java`
- `src/test/java/br/com/fiap/clyvovet/crud/CadastroCrudTest.java`
- `src/test/java/br/com/fiap/clyvovet/crud/EscapeNoOracleTest.java`
- `src/test/java/br/com/fiap/clyvovet/crud/FiltrosDeBuscaTest.java`
- `src/test/java/br/com/fiap/clyvovet/crud/IntegridadeReferencialTest.java`
- `src/test/java/br/com/fiap/clyvovet/crud/ValidacaoDeEntradaTest.java`
- `src/test/java/br/com/fiap/clyvovet/mapper/ClinicaMapperTest.java`
- `src/test/java/br/com/fiap/clyvovet/mapper/TutorMapperTest.java`
- `src/test/java/br/com/fiap/clyvovet/mapper/UsuarioMapperTest.java`
- `src/test/java/br/com/fiap/clyvovet/security/AutorizacaoTest.java`
- `src/test/java/br/com/fiap/clyvovet/security/OwnershipTest.java`
- `src/test/java/br/com/fiap/clyvovet/support/SeedV2.java`
- `src/test/java/br/com/fiap/clyvovet/support/TesteDeApi.java`

## Audit Trail

- EXTRACTED: 425 (70%)
- INFERRED: 180 (30%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*