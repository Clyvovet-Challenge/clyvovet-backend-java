# Configuração de Segurança e Filtros

> 75 nodes · cohesion 0.06

## Key Concepts

- **org.springframework.stereotype.Component** (26 connections)
- **RateLimitFilter** (16 connections) — `src/main/java/br/com/fiap/clyvovet/security/RateLimitFilter.java`
- **DevDataSeeder** (15 connections) — `src/main/java/br/com/fiap/clyvovet/config/DevDataSeeder.java`
- **SecurityConfig** (15 connections) — `src/main/java/br/com/fiap/clyvovet/config/SecurityConfig.java`
- **RateLimitFilter.java** (15 connections) — `src/main/java/br/com/fiap/clyvovet/security/RateLimitFilter.java`
- **jakarta.servlet.http.HttpServletRequest** (13 connections)
- **DevDataSeeder.java** (13 connections) — `src/main/java/br/com/fiap/clyvovet/config/DevDataSeeder.java`
- **SecurityConfig.java** (13 connections) — `src/main/java/br/com/fiap/clyvovet/config/SecurityConfig.java`
- **JwtAuthenticationFilter** (12 connections) — `src/main/java/br/com/fiap/clyvovet/security/JwtAuthenticationFilter.java`
- **RespostaErroSeguranca.java** (12 connections) — `src/main/java/br/com/fiap/clyvovet/security/RespostaErroSeguranca.java`
- **RespostaErroSeguranca** (11 connections) — `src/main/java/br/com/fiap/clyvovet/security/RespostaErroSeguranca.java`
- **org.springframework.context.annotation.Bean** (10 connections)
- **org.springframework.security.crypto.password.PasswordEncoder** (10 connections)
- **jakarta.servlet.http.HttpServletResponse** (9 connections)
- **org.springframework.context.annotation.Configuration** (8 connections)
- **.salvarSeAusente()** (7 connections) — `src/main/java/br/com/fiap/clyvovet/config/DevDataSeeder.java`
- **JwtAuthenticationFilter.java** (7 connections) — `src/main/java/br/com/fiap/clyvovet/security/JwtAuthenticationFilter.java`
- **.doFilterInternal()** (7 connections) — `src/main/java/br/com/fiap/clyvovet/security/JwtAuthenticationFilter.java`
- **Faixa** (7 connections) — `src/main/java/br/com/fiap/clyvovet/security/RateLimitFilter.java`
- **.doFilterInternal()** (7 connections) — `src/main/java/br/com/fiap/clyvovet/security/RateLimitFilter.java`
- **com.fasterxml.jackson.databind.ObjectMapper** (6 connections)
- **.semearUsuariosDeDesenvolvimento()** (6 connections) — `src/main/java/br/com/fiap/clyvovet/config/DevDataSeeder.java`
- **.commence()** (6 connections) — `src/main/java/br/com/fiap/clyvovet/security/RespostaErroSeguranca.java`
- **.escrever()** (6 connections) — `src/main/java/br/com/fiap/clyvovet/security/RespostaErroSeguranca.java`
- **.handle()** (6 connections) — `src/main/java/br/com/fiap/clyvovet/security/RespostaErroSeguranca.java`
- *... and 50 more nodes in this community*

## Relationships

- [Tutores](Tutores.md) (15 shared connections)
- [Entidade Usuário](Entidade_Usuário.md) (8 shared connections)
- [Tratamento de Erros HTTP](Tratamento_de_Erros_HTTP.md) (8 shared connections)
- [Hardening e Autenticação](Hardening_e_Autenticação.md) (6 shared connections)
- [Veterinários e Anotações Base](Veterinários_e_Anotações_Base.md) (6 shared connections)
- [DTOs de Autenticação e Perfis](DTOs_de_Autenticação_e_Perfis.md) (5 shared connections)
- [Suporte de Testes de API](Suporte_de_Testes_de_API.md) (4 shared connections)
- [Endpoints de Autenticação](Endpoints_de_Autenticação.md) (4 shared connections)
- [Identidade e Ownership](Identidade_e_Ownership.md) (4 shared connections)
- [Clínicas e Endereço](Clínicas_e_Endereço.md) (4 shared connections)
- [Testes de CRUD e Integração](Testes_de_CRUD_e_Integração.md) (2 shared connections)
- [Animais e Autorização de Rota](Animais_e_Autorização_de_Rota.md) (2 shared connections)

## Source Files

- `docs/08-seguranca.md`
- `src/main/java/br/com/fiap/clyvovet/config/CacheConfig.java`
- `src/main/java/br/com/fiap/clyvovet/config/DevDataSeeder.java`
- `src/main/java/br/com/fiap/clyvovet/config/OpenApiConfig.java`
- `src/main/java/br/com/fiap/clyvovet/config/SecurityConfig.java`
- `src/main/java/br/com/fiap/clyvovet/repository/UsuarioRepository.java`
- `src/main/java/br/com/fiap/clyvovet/security/JwtAuthenticationFilter.java`
- `src/main/java/br/com/fiap/clyvovet/security/RateLimitFilter.java`
- `src/main/java/br/com/fiap/clyvovet/security/RespostaErroSeguranca.java`
- `src/main/java/br/com/fiap/clyvovet/security/RevogacaoTokenService.java`

## Audit Trail

- EXTRACTED: 233 (99%)
- INFERRED: 3 (1%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*