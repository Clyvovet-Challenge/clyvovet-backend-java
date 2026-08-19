# Endpoints de Autenticação

> 25 nodes · cohesion 0.15

## Key Concepts

- **AuthService** (19 connections) — `src/main/java/br/com/fiap/clyvovet/service/AuthService.java`
- **AuthController.java** (18 connections) — `src/main/java/br/com/fiap/clyvovet/controller/AuthController.java`
- **AuthController** (13 connections) — `src/main/java/br/com/fiap/clyvovet/controller/AuthController.java`
- **.refresh()** (11 connections) — `src/main/java/br/com/fiap/clyvovet/service/AuthService.java`
- **RefreshRequest** (10 connections) — `src/main/java/br/com/fiap/clyvovet/dto/auth/RefreshRequest.java`
- **.login()** (9 connections) — `src/main/java/br/com/fiap/clyvovet/service/AuthService.java`
- **.montarResposta()** (9 connections) — `src/main/java/br/com/fiap/clyvovet/service/AuthService.java`
- **.me()** (8 connections) — `src/main/java/br/com/fiap/clyvovet/controller/AuthController.java`
- **RevogacaoTokenService** (8 connections) — `src/main/java/br/com/fiap/clyvovet/security/RevogacaoTokenService.java`
- **.criarUsuario()** (7 connections) — `src/main/java/br/com/fiap/clyvovet/controller/AuthController.java`
- **.login()** (7 connections) — `src/main/java/br/com/fiap/clyvovet/controller/AuthController.java`
- **.refresh()** (7 connections) — `src/main/java/br/com/fiap/clyvovet/controller/AuthController.java`
- **.registrar()** (7 connections) — `src/main/java/br/com/fiap/clyvovet/controller/AuthController.java`
- **.logout()** (7 connections) — `src/main/java/br/com/fiap/clyvovet/service/AuthService.java`
- **org.springframework.web.bind.annotation.PostMapping** (6 connections)
- **.logout()** (6 connections) — `src/main/java/br/com/fiap/clyvovet/controller/AuthController.java`
- **.lerClaimsDoRefresh()** (5 connections) — `src/main/java/br/com/fiap/clyvovet/service/AuthService.java`
- **org.springframework.web.bind.annotation.GetMapping** (2 connections)
- **org.springframework.web.bind.annotation.RequestMapping** (2 connections)
- **org.springframework.web.bind.annotation.RestController** (2 connections)
- **.getValidadeAccessSegundos()** (2 connections) — `src/main/java/br/com/fiap/clyvovet/security/JwtService.java`
- **.estaRevogado()** (2 connections) — `src/main/java/br/com/fiap/clyvovet/security/RevogacaoTokenService.java`
- **.revogar()** (2 connections) — `src/main/java/br/com/fiap/clyvovet/security/RevogacaoTokenService.java`
- **.getId()** (2 connections) — `src/main/java/br/com/fiap/clyvovet/security/UsuarioAutenticado.java`
- **.RevogacaoTokenService()** (1 connections) — `src/main/java/br/com/fiap/clyvovet/security/RevogacaoTokenService.java`

## Relationships

- [Veterinários e Anotações Base](Veterinários_e_Anotações_Base.md) (18 shared connections)
- [DTOs de Autenticação e Perfis](DTOs_de_Autenticação_e_Perfis.md) (16 shared connections)
- [Emissão e Leitura de JWT](Emissão_e_Leitura_de_JWT.md) (11 shared connections)
- [Entidade Usuário](Entidade_Usuário.md) (10 shared connections)
- [Tratamento de Erros HTTP](Tratamento_de_Erros_HTTP.md) (7 shared connections)
- [Tutores](Tutores.md) (5 shared connections)
- [Configuração de Segurança e Filtros](Configuração_de_Segurança_e_Filtros.md) (4 shared connections)
- [Identidade e Ownership](Identidade_e_Ownership.md) (3 shared connections)
- [Hardening e Autenticação](Hardening_e_Autenticação.md) (3 shared connections)
- [Serviços com Cache e Repositórios](Serviços_com_Cache_e_Repositórios.md) (2 shared connections)
- [Suporte de Testes de API](Suporte_de_Testes_de_API.md) (1 shared connections)

## Source Files

- `src/main/java/br/com/fiap/clyvovet/controller/AuthController.java`
- `src/main/java/br/com/fiap/clyvovet/dto/auth/RefreshRequest.java`
- `src/main/java/br/com/fiap/clyvovet/security/JwtService.java`
- `src/main/java/br/com/fiap/clyvovet/security/RevogacaoTokenService.java`
- `src/main/java/br/com/fiap/clyvovet/security/UsuarioAutenticado.java`
- `src/main/java/br/com/fiap/clyvovet/service/AuthService.java`

## Audit Trail

- EXTRACTED: 105 (83%)
- INFERRED: 21 (17%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*