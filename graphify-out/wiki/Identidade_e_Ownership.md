# Identidade e Ownership

> 25 nodes · cohesion 0.14

## Key Concepts

- **UsuarioAutenticado** (18 connections) — `src/main/java/br/com/fiap/clyvovet/security/UsuarioAutenticado.java`
- **.autenticar()** (10 connections) — `src/main/java/br/com/fiap/clyvovet/security/JwtAuthenticationFilter.java`
- **.tutorIdParaFiltro()** (8 connections) — `src/main/java/br/com/fiap/clyvovet/security/SegurancaService.java`
- **UsuarioDetailsService** (8 connections) — `src/main/java/br/com/fiap/clyvovet/security/UsuarioDetailsService.java`
- **Override** (7 connections)
- **UsuarioDetailsService.java** (6 connections) — `src/main/java/br/com/fiap/clyvovet/security/UsuarioDetailsService.java`
- **.autenticado()** (5 connections) — `src/main/java/br/com/fiap/clyvovet/security/SegurancaService.java`
- **org.springframework.security.core.userdetails.UserDetails** (4 connections)
- **.temVisaoAmpla()** (4 connections) — `src/main/java/br/com/fiap/clyvovet/security/SegurancaService.java`
- **.tutorIdDoUsuario()** (4 connections) — `src/main/java/br/com/fiap/clyvovet/security/SegurancaService.java`
- **UsuarioAutenticado.java** (4 connections) — `src/main/java/br/com/fiap/clyvovet/security/UsuarioAutenticado.java`
- **.getAuthorities()** (4 connections) — `src/main/java/br/com/fiap/clyvovet/security/UsuarioAutenticado.java`
- **.getUsuario()** (4 connections) — `src/main/java/br/com/fiap/clyvovet/security/UsuarioAutenticado.java`
- **.isAccountNonLocked()** (4 connections) — `src/main/java/br/com/fiap/clyvovet/security/UsuarioAutenticado.java`
- **.loadUserByUsername()** (4 connections) — `src/main/java/br/com/fiap/clyvovet/security/UsuarioDetailsService.java`
- **.getTutorId()** (3 connections) — `src/main/java/br/com/fiap/clyvovet/security/UsuarioAutenticado.java`
- **.isEnabled()** (3 connections) — `src/main/java/br/com/fiap/clyvovet/security/UsuarioAutenticado.java`
- **.carregarPorId()** (3 connections) — `src/main/java/br/com/fiap/clyvovet/security/UsuarioDetailsService.java`
- **org.springframework.security.core.GrantedAuthority** (2 connections)
- **org.springframework.security.core.userdetails.UserDetailsService** (2 connections)
- **.getPassword()** (2 connections) — `src/main/java/br/com/fiap/clyvovet/security/UsuarioAutenticado.java`
- **.getUsername()** (2 connections) — `src/main/java/br/com/fiap/clyvovet/security/UsuarioAutenticado.java`
- **.isAccountNonExpired()** (2 connections) — `src/main/java/br/com/fiap/clyvovet/security/UsuarioAutenticado.java`
- **.isCredentialsNonExpired()** (2 connections) — `src/main/java/br/com/fiap/clyvovet/security/UsuarioAutenticado.java`
- **Override** (1 connections)

## Relationships

- [Serviços com Cache e Repositórios](Serviços_com_Cache_e_Repositórios.md) (8 shared connections)
- [Entidade Usuário](Entidade_Usuário.md) (7 shared connections)
- [Configuração de Segurança e Filtros](Configuração_de_Segurança_e_Filtros.md) (4 shared connections)
- [Emissão e Leitura de JWT](Emissão_e_Leitura_de_JWT.md) (3 shared connections)
- [Endpoints de Autenticação](Endpoints_de_Autenticação.md) (3 shared connections)
- [Paginação e Listagens](Paginação_e_Listagens.md) (2 shared connections)
- [Tutores](Tutores.md) (2 shared connections)
- [Eventos Clínicos](Eventos_Clínicos.md) (1 shared connections)
- [Cache, Filtros e Suas Pendências](Cache,_Filtros_e_Suas_Pendências.md) (1 shared connections)
- [Suporte de Testes de API](Suporte_de_Testes_de_API.md) (1 shared connections)

## Source Files

- `src/main/java/br/com/fiap/clyvovet/security/JwtAuthenticationFilter.java`
- `src/main/java/br/com/fiap/clyvovet/security/SegurancaService.java`
- `src/main/java/br/com/fiap/clyvovet/security/UsuarioAutenticado.java`
- `src/main/java/br/com/fiap/clyvovet/security/UsuarioDetailsService.java`

## Audit Trail

- EXTRACTED: 58 (78%)
- INFERRED: 16 (22%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*