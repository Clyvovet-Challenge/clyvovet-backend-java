# Suporte de Testes de API

> 22 nodes · cohesion 0.16

## Key Concepts

- **BloqueioContaTest.java** (13 connections) — `src/test/java/br/com/fiap/clyvovet/security/BloqueioContaTest.java`
- **BloqueioContaTest** (12 connections) — `src/test/java/br/com/fiap/clyvovet/security/BloqueioContaTest.java`
- **TesteDeApi.java** (9 connections) — `src/test/java/br/com/fiap/clyvovet/support/TesteDeApi.java`
- **.findByEmail()** (8 connections) — `src/main/java/br/com/fiap/clyvovet/repository/UsuarioRepository.java`
- **JwtServiceTest.java** (7 connections) — `src/test/java/br/com/fiap/clyvovet/security/JwtServiceTest.java`
- **org.springframework.boot.test.context.SpringBootTest** (6 connections)
- **.contaBloqueiaApos5Falhas()** (5 connections) — `src/test/java/br/com/fiap/clyvovet/security/BloqueioContaTest.java`
- **.contagemDeFalhasEPersistida()** (5 connections) — `src/test/java/br/com/fiap/clyvovet/security/BloqueioContaTest.java`
- **.loginBemSucedidoZeraContagem()** (5 connections) — `src/test/java/br/com/fiap/clyvovet/security/BloqueioContaTest.java`
- **.tentarLogin()** (5 connections) — `src/test/java/br/com/fiap/clyvovet/security/BloqueioContaTest.java`
- **org.junit.jupiter.api.BeforeEach** (4 connections)
- **org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc** (4 connections)
- **org.springframework.test.web.servlet.MockMvc** (4 connections)
- **Perfil.java** (4 connections) — `src/main/java/br/com/fiap/clyvovet/model/Perfil.java`
- **.bloqueioNaoAfetaOutrosUsuarios()** (4 connections) — `src/test/java/br/com/fiap/clyvovet/security/BloqueioContaTest.java`
- **.criarUsuario()** (4 connections) — `src/test/java/br/com/fiap/clyvovet/security/BloqueioContaTest.java`
- **org.junit.jupiter.api.AfterEach** (3 connections)
- **ClyvovetApplicationTests.java** (3 connections) — `src/test/java/br/com/fiap/clyvovet/ClyvovetApplicationTests.java`
- **ClyvovetApplicationTests** (3 connections) — `src/test/java/br/com/fiap/clyvovet/ClyvovetApplicationTests.java`
- **Usuario.java** (2 connections) — `src/main/java/br/com/fiap/clyvovet/model/Usuario.java`
- **.contextLoads()** (2 connections) — `src/test/java/br/com/fiap/clyvovet/ClyvovetApplicationTests.java`
- **.limparCaches()** (2 connections) — `src/test/java/br/com/fiap/clyvovet/support/TesteDeApi.java`

## Relationships

- [Testes de CRUD e Integração](Testes_de_CRUD_e_Integração.md) (22 shared connections)
- [Entidade Usuário](Entidade_Usuário.md) (8 shared connections)
- [Configuração de Segurança e Filtros](Configuração_de_Segurança_e_Filtros.md) (4 shared connections)
- [DTOs de Autenticação e Perfis](DTOs_de_Autenticação_e_Perfis.md) (3 shared connections)
- [Emissão e Leitura de JWT](Emissão_e_Leitura_de_JWT.md) (2 shared connections)
- [Serviços com Cache e Repositórios](Serviços_com_Cache_e_Repositórios.md) (1 shared connections)
- [Identidade e Ownership](Identidade_e_Ownership.md) (1 shared connections)
- [Endpoints de Autenticação](Endpoints_de_Autenticação.md) (1 shared connections)

## Source Files

- `src/main/java/br/com/fiap/clyvovet/model/Perfil.java`
- `src/main/java/br/com/fiap/clyvovet/model/Usuario.java`
- `src/main/java/br/com/fiap/clyvovet/repository/UsuarioRepository.java`
- `src/test/java/br/com/fiap/clyvovet/ClyvovetApplicationTests.java`
- `src/test/java/br/com/fiap/clyvovet/security/BloqueioContaTest.java`
- `src/test/java/br/com/fiap/clyvovet/security/JwtServiceTest.java`
- `src/test/java/br/com/fiap/clyvovet/support/TesteDeApi.java`

## Audit Trail

- EXTRACTED: 71 (91%)
- INFERRED: 7 (9%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*