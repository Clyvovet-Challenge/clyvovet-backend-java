# Tratamento de Erros HTTP

> 30 nodes · cohesion 0.14

## Key Concepts

- **org.springframework.http.ResponseEntity** (51 connections)
- **GlobalExceptionHandler** (14 connections) — `src/main/java/br/com/fiap/clyvovet/exception/GlobalExceptionHandler.java`
- **ErroValidacao** (13 connections) — `src/main/java/br/com/fiap/clyvovet/dto/exception/ErroValidacao.java`
- **GlobalExceptionHandler.java** (11 connections) — `src/main/java/br/com/fiap/clyvovet/exception/GlobalExceptionHandler.java`
- **.respostaDe()** (9 connections) — `src/main/java/br/com/fiap/clyvovet/exception/GlobalExceptionHandler.java`
- **org.springframework.web.bind.annotation.ExceptionHandler** (7 connections)
- **.handleRegraDeNegocio()** (7 connections) — `src/main/java/br/com/fiap/clyvovet/exception/GlobalExceptionHandler.java`
- **RegraDeNegocioException** (7 connections) — `src/main/java/br/com/fiap/clyvovet/exception/RegraDeNegocioException.java`
- **.handleCredenciais()** (6 connections) — `src/main/java/br/com/fiap/clyvovet/exception/GlobalExceptionHandler.java`
- **.handleIntegridade()** (6 connections) — `src/main/java/br/com/fiap/clyvovet/exception/GlobalExceptionHandler.java`
- **.handleNotFound()** (6 connections) — `src/main/java/br/com/fiap/clyvovet/exception/GlobalExceptionHandler.java`
- **.handleRecursoNaoEncontrado()** (6 connections) — `src/main/java/br/com/fiap/clyvovet/exception/GlobalExceptionHandler.java`
- **.deletar()** (5 connections) — `src/main/java/br/com/fiap/clyvovet/controller/ClinicaController.java`
- **.deletar()** (5 connections) — `src/main/java/br/com/fiap/clyvovet/controller/PagamentoController.java`
- **.handleValidationErrors()** (5 connections) — `src/main/java/br/com/fiap/clyvovet/exception/GlobalExceptionHandler.java`
- **Erros que nao Vazam** (4 connections) — `docs/08-seguranca.md`
- **lombok.extern.slf4j.Slf4j** (4 connections)
- **org.springframework.http.HttpStatus** (4 connections)
- **Tratamento Global de Erros** (3 connections) — `docs/01-arquitetura.md`
- **org.springframework.security.authentication.BadCredentialsException** (3 connections)
- **jakarta.persistence.EntityNotFoundException** (2 connections)
- **org.springframework.dao.DataIntegrityViolationException** (2 connections)
- **org.springframework.web.bind.annotation.RestControllerAdvice** (2 connections)
- **org.springframework.web.bind.MethodArgumentNotValidException** (2 connections)
- **.getCampo()** (2 connections) — `src/main/java/br/com/fiap/clyvovet/exception/RegraDeNegocioException.java`
- *... and 5 more nodes in this community*

## Relationships

- [Configuração de Segurança e Filtros](Configuração_de_Segurança_e_Filtros.md) (8 shared connections)
- [Veterinários e Anotações Base](Veterinários_e_Anotações_Base.md) (8 shared connections)
- [Endpoints de Autenticação](Endpoints_de_Autenticação.md) (7 shared connections)
- [Clínicas e Endereço](Clínicas_e_Endereço.md) (6 shared connections)
- [Eventos Clínicos](Eventos_Clínicos.md) (6 shared connections)
- [Controller de Pagamentos](Controller_de_Pagamentos.md) (6 shared connections)
- [Tutores](Tutores.md) (6 shared connections)
- [Animais e Autorização de Rota](Animais_e_Autorização_de_Rota.md) (5 shared connections)
- [Schema, Migrations e Divergências](Schema,_Migrations_e_Divergências.md) (3 shared connections)
- [Paginação e Listagens](Paginação_e_Listagens.md) (2 shared connections)
- [Serviços com Cache e Repositórios](Serviços_com_Cache_e_Repositórios.md) (2 shared connections)
- [DTOs de Autenticação e Perfis](DTOs_de_Autenticação_e_Perfis.md) (2 shared connections)

## Source Files

- `docs/01-arquitetura.md`
- `docs/08-seguranca.md`
- `src/main/java/br/com/fiap/clyvovet/controller/ClinicaController.java`
- `src/main/java/br/com/fiap/clyvovet/controller/PagamentoController.java`
- `src/main/java/br/com/fiap/clyvovet/dto/exception/ErroValidacao.java`
- `src/main/java/br/com/fiap/clyvovet/exception/GlobalExceptionHandler.java`
- `src/main/java/br/com/fiap/clyvovet/exception/RegraDeNegocioException.java`

## Audit Trail

- EXTRACTED: 125 (98%)
- INFERRED: 3 (2%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*