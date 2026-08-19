# Animais e Autorização de Rota

> 23 nodes · cohesion 0.19

## Key Concepts

- **AnimalRequest** (16 connections) — `src/main/java/br/com/fiap/clyvovet/dto/animal/AnimalRequest.java`
- **AnimalController** (15 connections) — `src/main/java/br/com/fiap/clyvovet/controller/AnimalController.java`
- **AnimalResponse** (15 connections) — `src/main/java/br/com/fiap/clyvovet/dto/animal/AnimalResponse.java`
- **AnimalService** (15 connections) — `src/main/java/br/com/fiap/clyvovet/service/AnimalService.java`
- **org.springframework.security.access.prepost.PreAuthorize** (12 connections)
- **AnimalController.java** (11 connections) — `src/main/java/br/com/fiap/clyvovet/controller/AnimalController.java`
- **.atualizar()** (10 connections) — `src/main/java/br/com/fiap/clyvovet/service/AnimalService.java`
- **.toResponse()** (9 connections) — `src/main/java/br/com/fiap/clyvovet/mapper/AnimalMapper.java`
- **.criar()** (9 connections) — `src/main/java/br/com/fiap/clyvovet/service/AnimalService.java`
- **.atualizar()** (8 connections) — `src/main/java/br/com/fiap/clyvovet/controller/AnimalController.java`
- **.criar()** (8 connections) — `src/main/java/br/com/fiap/clyvovet/controller/AnimalController.java`
- **AnimalMapper** (8 connections) — `src/main/java/br/com/fiap/clyvovet/mapper/AnimalMapper.java`
- **.buscarPorId()** (7 connections) — `src/main/java/br/com/fiap/clyvovet/controller/AnimalController.java`
- **.atualizar()** (7 connections) — `src/main/java/br/com/fiap/clyvovet/mapper/AnimalMapper.java`
- **.deletar()** (6 connections) — `src/main/java/br/com/fiap/clyvovet/controller/AnimalController.java`
- **AnimalMapper.java** (6 connections) — `src/main/java/br/com/fiap/clyvovet/mapper/AnimalMapper.java`
- **.obterPorId()** (5 connections) — `src/main/java/br/com/fiap/clyvovet/repository/AnimalRepository.java`
- **.buscarPorId()** (5 connections) — `src/main/java/br/com/fiap/clyvovet/service/AnimalService.java`
- **DeleteMapping** (1 connections)
- **PostMapping** (1 connections)
- **PutMapping** (1 connections)
- **RequestMapping** (1 connections)
- **RestController** (1 connections)

## Relationships

- [Serviços com Cache e Repositórios](Serviços_com_Cache_e_Repositórios.md) (15 shared connections)
- [Tutores](Tutores.md) (11 shared connections)
- [Veterinários e Anotações Base](Veterinários_e_Anotações_Base.md) (9 shared connections)
- [Testes de Mapper de Animal](Testes_de_Mapper_de_Animal.md) (9 shared connections)
- [Entidade Animal](Entidade_Animal.md) (9 shared connections)
- [Paginação e Listagens](Paginação_e_Listagens.md) (7 shared connections)
- [Tratamento de Erros HTTP](Tratamento_de_Erros_HTTP.md) (5 shared connections)
- [Eventos Clínicos](Eventos_Clínicos.md) (4 shared connections)
- [Controller de Pagamentos](Controller_de_Pagamentos.md) (2 shared connections)
- [Evidências e Contrato da API](Evidências_e_Contrato_da_API.md) (2 shared connections)
- [Entidade e Mapper de Tutor](Entidade_e_Mapper_de_Tutor.md) (2 shared connections)
- [Configuração de Segurança e Filtros](Configuração_de_Segurança_e_Filtros.md) (2 shared connections)

## Source Files

- `src/main/java/br/com/fiap/clyvovet/controller/AnimalController.java`
- `src/main/java/br/com/fiap/clyvovet/dto/animal/AnimalRequest.java`
- `src/main/java/br/com/fiap/clyvovet/dto/animal/AnimalResponse.java`
- `src/main/java/br/com/fiap/clyvovet/mapper/AnimalMapper.java`
- `src/main/java/br/com/fiap/clyvovet/repository/AnimalRepository.java`
- `src/main/java/br/com/fiap/clyvovet/service/AnimalService.java`

## Audit Trail

- EXTRACTED: 111 (87%)
- INFERRED: 17 (13%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*