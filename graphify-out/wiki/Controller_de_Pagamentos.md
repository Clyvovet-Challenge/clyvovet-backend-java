# Controller de Pagamentos

> 17 nodes · cohesion 0.21

## Key Concepts

- **PagamentoResponse** (16 connections) — `src/main/java/br/com/fiap/clyvovet/dto/pagamento/PagamentoResponse.java`
- **PagamentoController.java** (13 connections) — `src/main/java/br/com/fiap/clyvovet/controller/PagamentoController.java`
- **PagamentoController** (13 connections) — `src/main/java/br/com/fiap/clyvovet/controller/PagamentoController.java`
- **.listarTodos()** (10 connections) — `src/main/java/br/com/fiap/clyvovet/controller/PagamentoController.java`
- **.atualizar()** (10 connections) — `src/main/java/br/com/fiap/clyvovet/service/PagamentoService.java`
- **.toResponse()** (9 connections) — `src/main/java/br/com/fiap/clyvovet/mapper/PagamentoMapper.java`
- **.criar()** (9 connections) — `src/main/java/br/com/fiap/clyvovet/service/PagamentoService.java`
- **.atualizar()** (7 connections) — `src/main/java/br/com/fiap/clyvovet/controller/PagamentoController.java`
- **.buscarPorId()** (7 connections) — `src/main/java/br/com/fiap/clyvovet/controller/PagamentoController.java`
- **.criar()** (7 connections) — `src/main/java/br/com/fiap/clyvovet/controller/PagamentoController.java`
- **.buscarPorId()** (5 connections) — `src/main/java/br/com/fiap/clyvovet/service/PagamentoService.java`
- **.obterPorId()** (4 connections) — `src/main/java/br/com/fiap/clyvovet/repository/PagamentoRepository.java`
- **GetMapping** (2 connections)
- **PostMapping** (1 connections)
- **PutMapping** (1 connections)
- **RequestMapping** (1 connections)
- **RestController** (1 connections)

## Relationships

- [Pagamentos e Formas de Pagamento](Pagamentos_e_Formas_de_Pagamento.md) (16 shared connections)
- [Serviços com Cache e Repositórios](Serviços_com_Cache_e_Repositórios.md) (11 shared connections)
- [Paginação e Listagens](Paginação_e_Listagens.md) (6 shared connections)
- [Tratamento de Erros HTTP](Tratamento_de_Erros_HTTP.md) (6 shared connections)
- [Veterinários e Anotações Base](Veterinários_e_Anotações_Base.md) (5 shared connections)
- [Mapeamento de Evento e Pagamento](Mapeamento_de_Evento_e_Pagamento.md) (5 shared connections)
- [Tutores](Tutores.md) (4 shared connections)
- [Eventos Clínicos](Eventos_Clínicos.md) (3 shared connections)
- [Animais e Autorização de Rota](Animais_e_Autorização_de_Rota.md) (2 shared connections)
- [Evidências e Contrato da API](Evidências_e_Contrato_da_API.md) (2 shared connections)

## Source Files

- `src/main/java/br/com/fiap/clyvovet/controller/PagamentoController.java`
- `src/main/java/br/com/fiap/clyvovet/dto/pagamento/PagamentoResponse.java`
- `src/main/java/br/com/fiap/clyvovet/mapper/PagamentoMapper.java`
- `src/main/java/br/com/fiap/clyvovet/repository/PagamentoRepository.java`
- `src/main/java/br/com/fiap/clyvovet/service/PagamentoService.java`

## Audit Trail

- EXTRACTED: 73 (83%)
- INFERRED: 15 (17%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*