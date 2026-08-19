# Serviços com Cache e Repositórios

> 30 nodes · cohesion 0.17

## Key Concepts

- **org.springframework.transaction.annotation.Transactional** (39 connections)
- **org.springframework.cache.annotation.CacheEvict** (24 connections)
- **org.springframework.stereotype.Service** (22 connections)
- **SegurancaService** (22 connections) — `src/main/java/br/com/fiap/clyvovet/security/SegurancaService.java`
- **EventoClinicoService.java** (20 connections) — `src/main/java/br/com/fiap/clyvovet/service/EventoClinicoService.java`
- **PagamentoService.java** (18 connections) — `src/main/java/br/com/fiap/clyvovet/service/PagamentoService.java`
- **SegurancaService.java** (16 connections) — `src/main/java/br/com/fiap/clyvovet/security/SegurancaService.java`
- **AnimalService.java** (16 connections) — `src/main/java/br/com/fiap/clyvovet/service/AnimalService.java`
- **PagamentoService** (15 connections) — `src/main/java/br/com/fiap/clyvovet/service/PagamentoService.java`
- **VeterinarioService.java** (15 connections) — `src/main/java/br/com/fiap/clyvovet/service/VeterinarioService.java`
- **ClinicaService.java** (14 connections) — `src/main/java/br/com/fiap/clyvovet/service/ClinicaService.java`
- **org.springframework.cache.annotation.Cacheable** (12 connections)
- **AnimalRepository** (12 connections) — `src/main/java/br/com/fiap/clyvovet/repository/AnimalRepository.java`
- **ClinicaRepository** (12 connections) — `src/main/java/br/com/fiap/clyvovet/repository/ClinicaRepository.java`
- **EventoClinicoRepository** (12 connections) — `src/main/java/br/com/fiap/clyvovet/repository/EventoClinicoRepository.java`
- **RepositorioBase** (12 connections) — `src/main/java/br/com/fiap/clyvovet/repository/RepositorioBase.java`
- **PagamentoRepository** (10 connections) — `src/main/java/br/com/fiap/clyvovet/repository/PagamentoRepository.java`
- **.podeAcessar()** (7 connections) — `src/main/java/br/com/fiap/clyvovet/security/SegurancaService.java`
- **.deletar()** (5 connections) — `src/main/java/br/com/fiap/clyvovet/service/AnimalService.java`
- **.deletar()** (5 connections) — `src/main/java/br/com/fiap/clyvovet/service/ClinicaService.java`
- **.deletar()** (5 connections) — `src/main/java/br/com/fiap/clyvovet/service/PagamentoService.java`
- **EventoClinico.java** (4 connections) — `src/main/java/br/com/fiap/clyvovet/model/EventoClinico.java`
- **Ownership por ID** (3 connections) — `docs/08-seguranca.md`
- **.podeAcessarAnimal()** (3 connections) — `src/main/java/br/com/fiap/clyvovet/security/SegurancaService.java`
- **.garantirQueExiste()** (2 connections) — `src/main/java/br/com/fiap/clyvovet/repository/AnimalRepository.java`
- *... and 5 more nodes in this community*

## Relationships

- [Veterinários e Anotações Base](Veterinários_e_Anotações_Base.md) (25 shared connections)
- [Tutores](Tutores.md) (24 shared connections)
- [Paginação e Listagens](Paginação_e_Listagens.md) (22 shared connections)
- [Eventos Clínicos](Eventos_Clínicos.md) (21 shared connections)
- [Animais e Autorização de Rota](Animais_e_Autorização_de_Rota.md) (15 shared connections)
- [Clínicas e Endereço](Clínicas_e_Endereço.md) (14 shared connections)
- [Controller de Pagamentos](Controller_de_Pagamentos.md) (11 shared connections)
- [Identidade e Ownership](Identidade_e_Ownership.md) (8 shared connections)
- [Pagamentos e Formas de Pagamento](Pagamentos_e_Formas_de_Pagamento.md) (8 shared connections)
- [Entidade Usuário](Entidade_Usuário.md) (6 shared connections)
- [Mapeamento de Evento e Pagamento](Mapeamento_de_Evento_e_Pagamento.md) (6 shared connections)
- [DTOs de Autenticação e Perfis](DTOs_de_Autenticação_e_Perfis.md) (5 shared connections)

## Source Files

- `docs/08-seguranca.md`
- `src/main/java/br/com/fiap/clyvovet/model/EventoClinico.java`
- `src/main/java/br/com/fiap/clyvovet/repository/AnimalRepository.java`
- `src/main/java/br/com/fiap/clyvovet/repository/ClinicaRepository.java`
- `src/main/java/br/com/fiap/clyvovet/repository/EventoClinicoRepository.java`
- `src/main/java/br/com/fiap/clyvovet/repository/PagamentoRepository.java`
- `src/main/java/br/com/fiap/clyvovet/repository/RepositorioBase.java`
- `src/main/java/br/com/fiap/clyvovet/security/SegurancaService.java`
- `src/main/java/br/com/fiap/clyvovet/service/AnimalService.java`
- `src/main/java/br/com/fiap/clyvovet/service/ClinicaService.java`
- `src/main/java/br/com/fiap/clyvovet/service/EventoClinicoService.java`
- `src/main/java/br/com/fiap/clyvovet/service/PagamentoService.java`
- `src/main/java/br/com/fiap/clyvovet/service/VeterinarioService.java`

## Audit Trail

- EXTRACTED: 253 (98%)
- INFERRED: 6 (2%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*