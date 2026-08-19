# DTOs de Autenticação e Perfis

> 25 nodes · cohesion 0.16

## Key Concepts

- **Perfil** (21 connections) — `src/main/java/br/com/fiap/clyvovet/model/Perfil.java`
- **UsuarioService** (17 connections) — `src/main/java/br/com/fiap/clyvovet/service/UsuarioService.java`
- **UsuarioResponse** (13 connections) — `src/main/java/br/com/fiap/clyvovet/dto/auth/UsuarioResponse.java`
- **UsuarioMapperTest.java** (11 connections) — `src/test/java/br/com/fiap/clyvovet/mapper/UsuarioMapperTest.java`
- **LoginResponse** (10 connections) — `src/main/java/br/com/fiap/clyvovet/dto/auth/LoginResponse.java`
- **.toResponse()** (10 connections) — `src/main/java/br/com/fiap/clyvovet/mapper/UsuarioMapper.java`
- **.criar()** (10 connections) — `src/main/java/br/com/fiap/clyvovet/service/UsuarioService.java`
- **.registrar()** (8 connections) — `src/main/java/br/com/fiap/clyvovet/service/UsuarioService.java`
- **UsuarioMapperTest** (7 connections) — `src/test/java/br/com/fiap/clyvovet/mapper/UsuarioMapperTest.java`
- **UsuarioMapper** (6 connections) — `src/main/java/br/com/fiap/clyvovet/mapper/UsuarioMapper.java`
- **.novoUsuario()** (6 connections) — `src/main/java/br/com/fiap/clyvovet/service/UsuarioService.java`
- **.usuario()** (6 connections) — `src/test/java/br/com/fiap/clyvovet/mapper/UsuarioMapperTest.java`
- **.usuarioDeTutor()** (6 connections) — `src/test/java/br/com/fiap/clyvovet/mapper/UsuarioMapperTest.java`
- **.usuarioDeVeterinario()** (6 connections) — `src/test/java/br/com/fiap/clyvovet/mapper/UsuarioMapperTest.java`
- **.buscarPorId()** (5 connections) — `src/main/java/br/com/fiap/clyvovet/service/UsuarioService.java`
- **.adminSemVinculo()** (5 connections) — `src/test/java/br/com/fiap/clyvovet/mapper/UsuarioMapperTest.java`
- **.garantirEmailDisponivel()** (4 connections) — `src/main/java/br/com/fiap/clyvovet/service/UsuarioService.java`
- **.validarVinculo()** (4 connections) — `src/main/java/br/com/fiap/clyvovet/service/UsuarioService.java`
- **.de()** (3 connections) — `src/main/java/br/com/fiap/clyvovet/dto/auth/LoginResponse.java`
- **UsuarioResponse.java** (3 connections) — `src/main/java/br/com/fiap/clyvovet/dto/auth/UsuarioResponse.java`
- **.obterPorId()** (3 connections) — `src/main/java/br/com/fiap/clyvovet/repository/UsuarioRepository.java`
- **LoginResponse.java** (2 connections) — `src/main/java/br/com/fiap/clyvovet/dto/auth/LoginResponse.java`
- **ADMIN** (1 connections) — `src/main/java/br/com/fiap/clyvovet/model/Perfil.java`
- **TUTOR** (1 connections) — `src/main/java/br/com/fiap/clyvovet/model/Perfil.java`
- **VETERINARIO** (1 connections) — `src/main/java/br/com/fiap/clyvovet/model/Perfil.java`

## Relationships

- [Endpoints de Autenticação](Endpoints_de_Autenticação.md) (16 shared connections)
- [Veterinários e Anotações Base](Veterinários_e_Anotações_Base.md) (11 shared connections)
- [Entidade Usuário](Entidade_Usuário.md) (10 shared connections)
- [Testes de CRUD e Integração](Testes_de_CRUD_e_Integração.md) (9 shared connections)
- [Tutores](Tutores.md) (8 shared connections)
- [Configuração de Segurança e Filtros](Configuração_de_Segurança_e_Filtros.md) (5 shared connections)
- [Serviços com Cache e Repositórios](Serviços_com_Cache_e_Repositórios.md) (5 shared connections)
- [Suporte de Testes de API](Suporte_de_Testes_de_API.md) (3 shared connections)
- [Entidade e Mapper de Tutor](Entidade_e_Mapper_de_Tutor.md) (3 shared connections)
- [Tratamento de Erros HTTP](Tratamento_de_Erros_HTTP.md) (2 shared connections)
- [Eventos Clínicos](Eventos_Clínicos.md) (1 shared connections)

## Source Files

- `src/main/java/br/com/fiap/clyvovet/dto/auth/LoginResponse.java`
- `src/main/java/br/com/fiap/clyvovet/dto/auth/UsuarioResponse.java`
- `src/main/java/br/com/fiap/clyvovet/mapper/UsuarioMapper.java`
- `src/main/java/br/com/fiap/clyvovet/model/Perfil.java`
- `src/main/java/br/com/fiap/clyvovet/repository/UsuarioRepository.java`
- `src/main/java/br/com/fiap/clyvovet/service/UsuarioService.java`
- `src/test/java/br/com/fiap/clyvovet/mapper/UsuarioMapperTest.java`

## Audit Trail

- EXTRACTED: 105 (87%)
- INFERRED: 16 (13%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*