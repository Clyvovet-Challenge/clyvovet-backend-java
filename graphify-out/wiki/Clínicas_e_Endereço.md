# Clínicas e Endereço

> 50 nodes · cohesion 0.09

## Key Concepts

- **ClinicaRequest** (16 connections) — `src/main/java/br/com/fiap/clyvovet/dto/clinica/ClinicaRequest.java`
- **ClinicaResponse** (15 connections) — `src/main/java/br/com/fiap/clyvovet/dto/clinica/ClinicaResponse.java`
- **Endereco** (15 connections) — `src/main/java/br/com/fiap/clyvovet/model/Endereco.java`
- **ClinicaController** (13 connections) — `src/main/java/br/com/fiap/clyvovet/controller/ClinicaController.java`
- **ClinicaService** (13 connections) — `src/main/java/br/com/fiap/clyvovet/service/ClinicaService.java`
- **EnderecoResponse** (11 connections) — `src/main/java/br/com/fiap/clyvovet/dto/endereco/EnderecoResponse.java`
- **ClinicaController.java** (10 connections) — `src/main/java/br/com/fiap/clyvovet/controller/ClinicaController.java`
- **ClinicaMapper** (10 connections) — `src/main/java/br/com/fiap/clyvovet/mapper/ClinicaMapper.java`
- **.toResponse()** (9 connections) — `src/main/java/br/com/fiap/clyvovet/mapper/ClinicaMapper.java`
- **.toEntity()** (9 connections) — `src/main/java/br/com/fiap/clyvovet/mapper/EnderecoMapper.java`
- **.atualizar()** (9 connections) — `src/main/java/br/com/fiap/clyvovet/service/ClinicaService.java`
- **.listarTodos()** (8 connections) — `src/main/java/br/com/fiap/clyvovet/controller/ClinicaController.java`
- **.toEntity()** (8 connections) — `src/main/java/br/com/fiap/clyvovet/mapper/ClinicaMapper.java`
- **EnderecoMapper** (8 connections) — `src/main/java/br/com/fiap/clyvovet/mapper/EnderecoMapper.java`
- **.toResponse()** (8 connections) — `src/main/java/br/com/fiap/clyvovet/mapper/EnderecoMapper.java`
- **.criar()** (8 connections) — `src/main/java/br/com/fiap/clyvovet/service/ClinicaService.java`
- **ClinicaMapperTest.java** (8 connections) — `src/test/java/br/com/fiap/clyvovet/mapper/ClinicaMapperTest.java`
- **EnderecoMapperTest.java** (8 connections) — `src/test/java/br/com/fiap/clyvovet/mapper/EnderecoMapperTest.java`
- **.atualizar()** (7 connections) — `src/main/java/br/com/fiap/clyvovet/controller/ClinicaController.java`
- **.criar()** (7 connections) — `src/main/java/br/com/fiap/clyvovet/controller/ClinicaController.java`
- **.atualizar()** (7 connections) — `src/main/java/br/com/fiap/clyvovet/mapper/ClinicaMapper.java`
- **.obterPorId()** (7 connections) — `src/main/java/br/com/fiap/clyvovet/repository/ClinicaRepository.java`
- **.listarTodos()** (7 connections) — `src/main/java/br/com/fiap/clyvovet/service/ClinicaService.java`
- **ClinicaMapperTest** (7 connections) — `src/test/java/br/com/fiap/clyvovet/mapper/ClinicaMapperTest.java`
- **.idaEVoltaPreservaConteudo()** (7 connections) — `src/test/java/br/com/fiap/clyvovet/mapper/EnderecoMapperTest.java`
- *... and 25 more nodes in this community*

## Relationships

- [Veterinários e Anotações Base](Veterinários_e_Anotações_Base.md) (32 shared connections)
- [Testes de CRUD e Integração](Testes_de_CRUD_e_Integração.md) (18 shared connections)
- [Serviços com Cache e Repositórios](Serviços_com_Cache_e_Repositórios.md) (14 shared connections)
- [Tutores](Tutores.md) (10 shared connections)
- [Paginação e Listagens](Paginação_e_Listagens.md) (7 shared connections)
- [Tratamento de Erros HTTP](Tratamento_de_Erros_HTTP.md) (6 shared connections)
- [Entidade e Mapper de Tutor](Entidade_e_Mapper_de_Tutor.md) (4 shared connections)
- [Configuração de Segurança e Filtros](Configuração_de_Segurança_e_Filtros.md) (4 shared connections)
- [Evidências e Contrato da API](Evidências_e_Contrato_da_API.md) (2 shared connections)
- [Decisões de Arquitetura e Domínio](Decisões_de_Arquitetura_e_Domínio.md) (1 shared connections)
- [Eventos Clínicos](Eventos_Clínicos.md) (1 shared connections)

## Source Files

- `src/main/java/br/com/fiap/clyvovet/controller/ClinicaController.java`
- `src/main/java/br/com/fiap/clyvovet/dto/clinica/ClinicaRequest.java`
- `src/main/java/br/com/fiap/clyvovet/dto/clinica/ClinicaResponse.java`
- `src/main/java/br/com/fiap/clyvovet/dto/endereco/EnderecoResponse.java`
- `src/main/java/br/com/fiap/clyvovet/mapper/ClinicaMapper.java`
- `src/main/java/br/com/fiap/clyvovet/mapper/EnderecoMapper.java`
- `src/main/java/br/com/fiap/clyvovet/model/Endereco.java`
- `src/main/java/br/com/fiap/clyvovet/repository/ClinicaRepository.java`
- `src/main/java/br/com/fiap/clyvovet/service/ClinicaService.java`
- `src/test/java/br/com/fiap/clyvovet/mapper/ClinicaMapperTest.java`
- `src/test/java/br/com/fiap/clyvovet/mapper/EnderecoMapperTest.java`

## Audit Trail

- EXTRACTED: 178 (84%)
- INFERRED: 33 (16%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*