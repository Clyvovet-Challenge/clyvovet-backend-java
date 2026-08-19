# Veterinários e Anotações Base

> 74 nodes · cohesion 0.08

## Key Concepts

- **io.swagger.v3.oas.annotations.Operation** (43 connections)
- **Clinica** (32 connections) — `src/main/java/br/com/fiap/clyvovet/model/Clinica.java`
- **Veterinario** (31 connections) — `src/main/java/br/com/fiap/clyvovet/model/Veterinario.java`
- **lombok.AllArgsConstructor** (24 connections)
- **lombok.Getter** (24 connections)
- **lombok.NoArgsConstructor** (24 connections)
- **VeterinarioRequest** (17 connections) — `src/main/java/br/com/fiap/clyvovet/dto/veterinario/VeterinarioRequest.java`
- **Sexo** (17 connections) — `src/main/java/br/com/fiap/clyvovet/model/Sexo.java`
- **VeterinarioResponse** (16 connections) — `src/main/java/br/com/fiap/clyvovet/dto/veterinario/VeterinarioResponse.java`
- **EventoClinicoMapperTest.java** (15 connections) — `src/test/java/br/com/fiap/clyvovet/mapper/EventoClinicoMapperTest.java`
- **EnderecoRequest** (14 connections) — `src/main/java/br/com/fiap/clyvovet/dto/endereco/EnderecoRequest.java`
- **VeterinarioRepository** (14 connections) — `src/main/java/br/com/fiap/clyvovet/repository/VeterinarioRepository.java`
- **VeterinarioService** (14 connections) — `src/main/java/br/com/fiap/clyvovet/service/VeterinarioService.java`
- **VeterinarioController** (13 connections) — `src/main/java/br/com/fiap/clyvovet/controller/VeterinarioController.java`
- **EventoClinicoMapper.java** (11 connections) — `src/main/java/br/com/fiap/clyvovet/mapper/EventoClinicoMapper.java`
- **Veterinario.java** (11 connections) — `src/main/java/br/com/fiap/clyvovet/model/Veterinario.java`
- **VeterinarioMapperTest.java** (11 connections) — `src/test/java/br/com/fiap/clyvovet/mapper/VeterinarioMapperTest.java`
- **VeterinarioController.java** (10 connections) — `src/main/java/br/com/fiap/clyvovet/controller/VeterinarioController.java`
- **VeterinarioMapper** (10 connections) — `src/main/java/br/com/fiap/clyvovet/mapper/VeterinarioMapper.java`
- **.toEntity()** (10 connections) — `src/main/java/br/com/fiap/clyvovet/mapper/VeterinarioMapper.java`
- **.toResponse()** (10 connections) — `src/main/java/br/com/fiap/clyvovet/mapper/VeterinarioMapper.java`
- **.atualizar()** (10 connections) — `src/main/java/br/com/fiap/clyvovet/service/VeterinarioService.java`
- **UsuarioRequest** (9 connections) — `src/main/java/br/com/fiap/clyvovet/dto/auth/UsuarioRequest.java`
- **Clinica.java** (9 connections) — `src/main/java/br/com/fiap/clyvovet/model/Clinica.java`
- **.criar()** (9 connections) — `src/main/java/br/com/fiap/clyvovet/service/VeterinarioService.java`
- *... and 49 more nodes in this community*

## Relationships

- [Clínicas e Endereço](Clínicas_e_Endereço.md) (32 shared connections)
- [Eventos Clínicos](Eventos_Clínicos.md) (27 shared connections)
- [Serviços com Cache e Repositórios](Serviços_com_Cache_e_Repositórios.md) (25 shared connections)
- [Tutores](Tutores.md) (22 shared connections)
- [Endpoints de Autenticação](Endpoints_de_Autenticação.md) (18 shared connections)
- [Paginação e Listagens](Paginação_e_Listagens.md) (15 shared connections)
- [Testes de CRUD e Integração](Testes_de_CRUD_e_Integração.md) (13 shared connections)
- [DTOs de Autenticação e Perfis](DTOs_de_Autenticação_e_Perfis.md) (11 shared connections)
- [Animais e Autorização de Rota](Animais_e_Autorização_de_Rota.md) (9 shared connections)
- [Tratamento de Erros HTTP](Tratamento_de_Erros_HTTP.md) (8 shared connections)
- [Entidade Animal](Entidade_Animal.md) (7 shared connections)
- [Entidade e Mapper de Tutor](Entidade_e_Mapper_de_Tutor.md) (7 shared connections)

## Source Files

- `documentos/Diagrama_De_Classes.pdf`
- `src/main/java/br/com/fiap/clyvovet/controller/VeterinarioController.java`
- `src/main/java/br/com/fiap/clyvovet/dto/animal/AnimalRequest.java`
- `src/main/java/br/com/fiap/clyvovet/dto/auth/LoginRequest.java`
- `src/main/java/br/com/fiap/clyvovet/dto/auth/RefreshRequest.java`
- `src/main/java/br/com/fiap/clyvovet/dto/auth/RegistroRequest.java`
- `src/main/java/br/com/fiap/clyvovet/dto/auth/UsuarioRequest.java`
- `src/main/java/br/com/fiap/clyvovet/dto/clinica/ClinicaRequest.java`
- `src/main/java/br/com/fiap/clyvovet/dto/endereco/EnderecoRequest.java`
- `src/main/java/br/com/fiap/clyvovet/dto/eventoClinico/EventoClinicoRequest.java`
- `src/main/java/br/com/fiap/clyvovet/dto/tutor/TutorRequest.java`
- `src/main/java/br/com/fiap/clyvovet/dto/veterinario/VeterinarioRequest.java`
- `src/main/java/br/com/fiap/clyvovet/dto/veterinario/VeterinarioResponse.java`
- `src/main/java/br/com/fiap/clyvovet/mapper/EventoClinicoMapper.java`
- `src/main/java/br/com/fiap/clyvovet/mapper/RelacionamentosDoEvento.java`
- `src/main/java/br/com/fiap/clyvovet/mapper/UsuarioMapper.java`
- `src/main/java/br/com/fiap/clyvovet/mapper/VeterinarioMapper.java`
- `src/main/java/br/com/fiap/clyvovet/model/Animal.java`
- `src/main/java/br/com/fiap/clyvovet/model/Clinica.java`
- `src/main/java/br/com/fiap/clyvovet/model/Sexo.java`

## Audit Trail

- EXTRACTED: 397 (93%)
- INFERRED: 32 (7%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*