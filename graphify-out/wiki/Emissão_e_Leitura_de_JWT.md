# Emissão e Leitura de JWT

> 22 nodes · cohesion 0.21

## Key Concepts

- **JwtService** (23 connections) — `src/main/java/br/com/fiap/clyvovet/security/JwtService.java`
- **JwtServiceTest** (10 connections) — `src/test/java/br/com/fiap/clyvovet/security/JwtServiceTest.java`
- **io.jsonwebtoken.Claims** (8 connections)
- **.lerClaims()** (8 connections) — `src/main/java/br/com/fiap/clyvovet/security/JwtService.java`
- **.accessTokenCarregaIdEPerfil()** (8 connections) — `src/test/java/br/com/fiap/clyvovet/security/JwtServiceTest.java`
- **.gerarAccessToken()** (7 connections) — `src/main/java/br/com/fiap/clyvovet/security/JwtService.java`
- **.refreshTokenEDistinguivel()** (7 connections) — `src/test/java/br/com/fiap/clyvovet/security/JwtServiceTest.java`
- **.ehRefreshToken()** (6 connections) — `src/main/java/br/com/fiap/clyvovet/security/JwtService.java`
- **.tokenDeOutraChaveERejeitado()** (6 connections) — `src/test/java/br/com/fiap/clyvovet/security/JwtServiceTest.java`
- **.tokenExpiradoERejeitado()** (6 connections) — `src/test/java/br/com/fiap/clyvovet/security/JwtServiceTest.java`
- **JwtService.java** (5 connections) — `src/main/java/br/com/fiap/clyvovet/security/JwtService.java`
- **.ehAccessToken()** (5 connections) — `src/main/java/br/com/fiap/clyvovet/security/JwtService.java`
- **.extrairUsuarioId()** (5 connections) — `src/main/java/br/com/fiap/clyvovet/security/JwtService.java`
- **.gerarRefreshToken()** (5 connections) — `src/main/java/br/com/fiap/clyvovet/security/JwtService.java`
- **.tokenValido()** (5 connections) — `src/main/java/br/com/fiap/clyvovet/security/JwtService.java`
- **.tokenAdulteradoERejeitado()** (5 connections) — `src/test/java/br/com/fiap/clyvovet/security/JwtServiceTest.java`
- **.extrairJti()** (4 connections) — `src/main/java/br/com/fiap/clyvovet/security/JwtService.java`
- **.gerar()** (4 connections) — `src/main/java/br/com/fiap/clyvovet/security/JwtService.java`
- **.segredoFracoERecusado()** (4 connections) — `src/test/java/br/com/fiap/clyvovet/security/JwtServiceTest.java`
- **.setUp()** (4 connections) — `src/test/java/br/com/fiap/clyvovet/security/JwtServiceTest.java`
- **javax.crypto.SecretKey** (2 connections)
- **.JwtService()** (1 connections) — `src/main/java/br/com/fiap/clyvovet/security/JwtService.java`

## Relationships

- [Testes de CRUD e Integração](Testes_de_CRUD_e_Integração.md) (12 shared connections)
- [Endpoints de Autenticação](Endpoints_de_Autenticação.md) (11 shared connections)
- [Entidade Usuário](Entidade_Usuário.md) (8 shared connections)
- [Identidade e Ownership](Identidade_e_Ownership.md) (3 shared connections)
- [Serviços com Cache e Repositórios](Serviços_com_Cache_e_Repositórios.md) (2 shared connections)
- [Suporte de Testes de API](Suporte_de_Testes_de_API.md) (2 shared connections)
- [Configuração de Segurança e Filtros](Configuração_de_Segurança_e_Filtros.md) (1 shared connections)
- [Hardening e Autenticação](Hardening_e_Autenticação.md) (1 shared connections)

## Source Files

- `src/main/java/br/com/fiap/clyvovet/security/JwtService.java`
- `src/test/java/br/com/fiap/clyvovet/security/JwtServiceTest.java`

## Audit Trail

- EXTRACTED: 63 (71%)
- INFERRED: 26 (29%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*