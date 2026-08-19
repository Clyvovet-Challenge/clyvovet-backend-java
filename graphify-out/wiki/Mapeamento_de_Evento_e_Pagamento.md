# Mapeamento de Evento e Pagamento

> 17 nodes · cohesion 0.23

## Key Concepts

- **EventoClinico** (30 connections) — `src/main/java/br/com/fiap/clyvovet/model/EventoClinico.java`
- **.toEntity()** (10 connections) — `src/main/java/br/com/fiap/clyvovet/mapper/PagamentoMapper.java`
- **PagamentoMapper** (8 connections) — `src/main/java/br/com/fiap/clyvovet/mapper/PagamentoMapper.java`
- **PagamentoMapperTest** (8 connections) — `src/test/java/br/com/fiap/clyvovet/mapper/PagamentoMapperTest.java`
- **.request()** (8 connections) — `src/test/java/br/com/fiap/clyvovet/mapper/PagamentoMapperTest.java`
- **.atualizar()** (7 connections) — `src/main/java/br/com/fiap/clyvovet/mapper/PagamentoMapper.java`
- **.atualizarPreservaId()** (7 connections) — `src/test/java/br/com/fiap/clyvovet/mapper/PagamentoMapperTest.java`
- **.respostaTrazIdDoEvento()** (7 connections) — `src/test/java/br/com/fiap/clyvovet/mapper/PagamentoMapperTest.java`
- **.pagamentoSemEvento()** (6 connections) — `src/test/java/br/com/fiap/clyvovet/mapper/PagamentoMapperTest.java`
- **.toEntityCopiaCampos()** (6 connections) — `src/test/java/br/com/fiap/clyvovet/mapper/PagamentoMapperTest.java`
- **.evento()** (5 connections) — `src/test/java/br/com/fiap/clyvovet/mapper/PagamentoMapperTest.java`
- **FetchType EAGER Explicito** (1 connections) — `docs/01-arquitetura.md`
- **AllArgsConstructor** (1 connections)
- **Entity** (1 connections)
- **Getter** (1 connections)
- **NoArgsConstructor** (1 connections)
- **Setter** (1 connections)

## Relationships

- [Pagamentos e Formas de Pagamento](Pagamentos_e_Formas_de_Pagamento.md) (12 shared connections)
- [Testes de CRUD e Integração](Testes_de_CRUD_e_Integração.md) (8 shared connections)
- [Serviços com Cache e Repositórios](Serviços_com_Cache_e_Repositórios.md) (6 shared connections)
- [Controller de Pagamentos](Controller_de_Pagamentos.md) (5 shared connections)
- [Eventos Clínicos](Eventos_Clínicos.md) (5 shared connections)
- [Veterinários e Anotações Base](Veterinários_e_Anotações_Base.md) (5 shared connections)
- [Paginação e Listagens](Paginação_e_Listagens.md) (2 shared connections)
- [Configuração de Segurança e Filtros](Configuração_de_Segurança_e_Filtros.md) (1 shared connections)
- [Entidade Animal](Entidade_Animal.md) (1 shared connections)
- [Decisões de Arquitetura e Domínio](Decisões_de_Arquitetura_e_Domínio.md) (1 shared connections)

## Source Files

- `docs/01-arquitetura.md`
- `src/main/java/br/com/fiap/clyvovet/mapper/PagamentoMapper.java`
- `src/main/java/br/com/fiap/clyvovet/model/EventoClinico.java`
- `src/test/java/br/com/fiap/clyvovet/mapper/PagamentoMapperTest.java`

## Audit Trail

- EXTRACTED: 67 (87%)
- INFERRED: 10 (13%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*