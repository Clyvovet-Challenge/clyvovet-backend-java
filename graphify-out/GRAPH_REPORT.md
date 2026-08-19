# Graph Report - clyvovet-backend-java  (2026-08-19)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 917 nodes · 2971 edges · 46 communities (37 shown, 9 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 425 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `d04f0d76`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Testes de CRUD e Integração
- RateLimitFilter
- Veterinários e Anotações Base
- lombok.RequiredArgsConstructor
- Eventos Clínicos
- DevDataSeeder
- VeterinarioResponse
- TutorResponse
- Pagamentos e Formas de Pagamento
- Identidade e Ownership
- Emissão e Leitura de JWT
- AuthController.java
- org.springframework.http.ResponseEntity
- Animal
- SecurityConfig
- Suporte de Testes de API
- API REST CRUD
- Repositórios Base e Recursos
- Perfis Spring (oracle / h2 / dev)
- Login e Perfis de Acesso
- Pendencias e Divergencias
- Seguranca da API
- Usuario
- Guia das Migrations
- Cache, Filtros e Suas Pendências
- Challenge FIAP 2026
- Maven Wrapper
- Testes de Mapper de Animal
- DDL Oracle Legado
- Teste das Migrations MySQL
- Bootstrap da Aplicação
- Schema Inicial (MySQL)
- Schema Inicial (Oracle)
- Workflow graphify
- Script de Deploy Azure
- Lombok na IDE
- Coordenadas Maven
- Tabela Usuario (MySQL)
- Tabela Usuario (Oracle)
- Tabela Pagamento na V4 (MySQL)
- Tabela Pagamento na V4 (Oracle)
- .toResponse

## God Nodes (most connected - your core abstractions)
1. `Usuario` - 43 edges
2. `TesteDeApi` - 34 edges
3. `Clinica` - 32 edges
4. `Tutor` - 32 edges
5. `Veterinario` - 31 edges
6. `EventoClinico` - 30 edges
7. `Animal` - 29 edges
8. `JwtService` - 23 edges
9. `Pagamento` - 23 edges
10. `Recurso` - 22 edges

## Surprising Connections (you probably didn't know these)
- `Diagrama de Classes UML` --semantically_similar_to--> `Modelo de Dominio CLYVO VET`  [INFERRED] [semantically similar]
  documentos/Diagrama_De_Classes.pdf → docs/02-modelo-de-dados.md
- `Cronograma de Desenvolvimento` --semantically_similar_to--> `Cronograma de Entregas`  [INFERRED] [semantically similar]
  documentos/Cronograma_CLYVOVET.pdf → specs/README.md
- `UUID como Chave Primaria` --references--> `Animal`  [INFERRED]
  docs/01-arquitetura.md → src/main/java/br/com/fiap/clyvovet/model/Animal.java
- `Claim 'tipo' Distingue Access de Refresh` --references--> `JwtAuthenticationFilter`  [EXTRACTED]
  docs/08-seguranca.md → src/main/java/br/com/fiap/clyvovet/security/JwtAuthenticationFilter.java
- `Escalacao de Privilegio no Auto-cadastro` --references--> `AuthService`  [EXTRACTED]
  docs/08-seguranca.md → src/main/java/br/com/fiap/clyvovet/service/AuthService.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Defesa em Profundidade do Login** — docs_08_seguranca_autenticacao_jwt, docs_08_seguranca_bloqueio_de_conta, docs_08_seguranca_rate_limit_por_ip, docs_08_seguranca_enumeracao_de_usuarios, docs_08_seguranca_requires_new_na_contagem [EXTRACTED 0.90]
- **Evidencias de Teste dos POSTs** — documentos_post_animais_evidencia_post_animais, documentos_post_tutores_evidencia_post_tutores, documentos_post_clinicas_evidencia_post_clinicas, docs_03_api_rest_api_rest_crud [EXTRACTED 0.90]
- **Nucleo do Dominio Clinico** — docs_02_modelo_de_dados_modelo_de_dominio, src_main_java_br_com_fiap_clyvovet_model_animal_animal, src_main_java_br_com_fiap_clyvovet_model_veterinario_veterinario, src_main_java_br_com_fiap_clyvovet_model_clinica_clinica, src_main_java_br_com_fiap_clyvovet_model_eventoclinico_eventoclinico, src_main_java_br_com_fiap_clyvovet_model_pagamento_pagamento [EXTRACTED 0.95]
- **As Tres Frentes do Ownership** — docs_08_seguranca_ownership, docs_08_seguranca_ownership_por_id, docs_08_seguranca_ownership_em_listagens, docs_08_seguranca_ownership_no_corpo, docs_08_seguranca_vazamento_de_cache_entre_contas [EXTRACTED 0.95]
- **Lacunas Abertas para a Entrega Final** — specs_02_sprint_3_frontend_pendente, specs_02_sprint_3_fluxos_nao_crud, specs_04_dependencias_externas_devops_banco_em_nuvem, specs_04_dependencias_externas_pipeline_azure_devops, specs_03_sprint_4_deploy_online [INFERRED 0.85]

## Communities (46 total, 9 thin omitted)

### Community 0 - "Testes de CRUD e Integração"
Cohesion: 0.06
Nodes (21): com.fasterxml.jackson.databind.JsonNode, java.sql.Connection, org.junit.jupiter.api.AfterEach, org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable, org.junit.jupiter.api.DisplayName, org.junit.jupiter.api.Test, org.springframework.test.web.servlet.ResultActions, AtendimentoCrudTest (+13 more)

### Community 1 - "RateLimitFilter"
Cohesion: 0.07
Nodes (38): com.fasterxml.jackson.databind.ObjectMapper, com.github.benmanes.caffeine.cache.Cache, Tratamento Global de Erros, Constraints que so existem no Banco, Unicidade so Existe no Banco, Erros que nao Vazam, io.github.bucket4j.Bucket, io.github.bucket4j.ConsumptionProbe (+30 more)

### Community 2 - "Veterinários e Anotações Base"
Cohesion: 0.06
Nodes (45): Convencao Java vs Coluna, Dois Enums de Sexo, Endereco Embeddable, Enums Persistidos como STRING, Modelo de Dominio CLYVO VET, Relacionamentos Unidirecionais, Diagrama de Classes UML, jakarta.persistence.Embeddable (+37 more)

### Community 3 - "lombok.RequiredArgsConstructor"
Cohesion: 0.14
Nodes (18): io.swagger.v3.oas.annotations.tags.Tag, lombok.RequiredArgsConstructor, org.springframework.cache.annotation.CacheEvict, org.springframework.stereotype.Service, org.springframework.transaction.annotation.Transactional, ClinicaController, GetMapping, PostMapping (+10 more)

### Community 4 - "Eventos Clínicos"
Cohesion: 0.10
Nodes (19): EventoClinicoController, GetMapping, PostMapping, PutMapping, RequestMapping, RestController, EventoClinicoRequest, EventoClinicoResponse (+11 more)

### Community 5 - "DevDataSeeder"
Cohesion: 0.35
Nodes (5): Usuarios de Desenvolvimento, lombok.extern.slf4j.Slf4j, org.springframework.boot.ApplicationRunner, org.springframework.context.annotation.Profile, DevDataSeeder

### Community 6 - "VeterinarioResponse"
Cohesion: 0.18
Nodes (8): DeleteMapping, GetMapping, PostMapping, PutMapping, RequestMapping, RestController, VeterinarioController, VeterinarioResponse

### Community 7 - "TutorResponse"
Cohesion: 0.17
Nodes (9): DeleteMapping, GetMapping, PostMapping, PutMapping, RequestMapping, RestController, TutorController, TutorResponse (+1 more)

### Community 8 - "Pagamentos e Formas de Pagamento"
Cohesion: 0.07
Nodes (38): FetchType EAGER Explicito, lombok.Data, DeleteMapping, GetMapping, PostMapping, PutMapping, RequestMapping, RestController (+30 more)

### Community 9 - "Identidade e Ownership"
Cohesion: 0.13
Nodes (7): Ownership por ID, org.springframework.security.core.GrantedAuthority, org.springframework.security.core.userdetails.UserDetails, SegurancaService, Override, UsuarioAutenticado, Override

### Community 10 - "Emissão e Leitura de JWT"
Cohesion: 0.21
Nodes (6): io.jsonwebtoken.Claims, javax.crypto.SecretKey, RefreshRequest, JwtService, RevogacaoTokenService, AuthService

### Community 11 - "AuthController.java"
Cohesion: 0.19
Nodes (7): org.springframework.web.bind.annotation.GetMapping, org.springframework.web.bind.annotation.PostMapping, org.springframework.web.bind.annotation.RequestMapping, org.springframework.web.bind.annotation.RestController, AuthController, UsuarioResponse, UsuarioService

### Community 12 - "org.springframework.http.ResponseEntity"
Cohesion: 0.17
Nodes (8): io.swagger.v3.oas.annotations.Operation, org.springframework.cache.annotation.Cacheable, org.springframework.data.domain.Page, org.springframework.data.domain.Pageable, org.springframework.data.jpa.repository.Query, org.springframework.http.ResponseEntity, DeleteMapping, DeleteMapping

### Community 13 - "Animal"
Cohesion: 0.10
Nodes (23): org.springframework.security.access.prepost.PreAuthorize, AnimalController, DeleteMapping, GetMapping, PostMapping, PutMapping, RequestMapping, RestController (+15 more)

### Community 14 - "SecurityConfig"
Cohesion: 0.13
Nodes (17): AuthorizationManagerRequestMatcherRegistry, Cabecalhos de Seguranca, Matriz de Autorizacao, io.swagger.v3.oas.models.OpenAPI, OpenAPI, org.springframework.cache.CacheManager, org.springframework.context.annotation.Bean, org.springframework.context.annotation.Configuration (+9 more)

### Community 15 - "Suporte de Testes de API"
Cohesion: 0.19
Nodes (6): org.junit.jupiter.api.BeforeEach, org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc, org.springframework.boot.test.context.SpringBootTest, org.springframework.test.web.servlet.MockMvc, ClyvovetApplicationTests, BloqueioContaTest

### Community 16 - "API REST CRUD"
Cohesion: 0.17
Nodes (19): Arquitetura em Camadas, Desnormalizacao do Nome na Resposta, DTOs em vez de Entidades, Fluxo de Requisicao, Mappers Manuais, API REST CRUD, Swagger / OpenAPI, Convencoes de Codigo (+11 more)

### Community 17 - "Repositórios Base e Recursos"
Cohesion: 0.16
Nodes (12): org.springframework.data.jpa.repository.JpaRepository, org.springframework.data.repository.NoRepositoryBean, Recurso, ANIMAL, CLINICA, EVENTO_CLINICO, PAGAMENTO, TUTOR (+4 more)

### Community 18 - "Perfis Spring (oracle / h2 / dev)"
Cohesion: 0.15
Nodes (17): Servico clyvovet-db, UUID como Chave Primaria, UUID gravado como CHAR no Oracle, Perfil h2 so roda no Docker, Perfis Spring (oracle / h2 / dev), Precedencia de Configuracao, Variaveis de Ambiente Obrigatorias, Deploy Azure via deploy.sh (+9 more)

### Community 19 - "Login e Perfis de Acesso"
Cohesion: 0.23
Nodes (6): LoginRequest, LoginResponse, Perfil, ADMIN, TUTOR, VETERINARIO

### Community 21 - "Pendencias e Divergencias"
Cohesion: 0.12
Nodes (20): Filtro Opcional JPQL, Schema Oracle (db-oracle.sql), Seed Data, Filtros Opcionais por Recurso, Baseline Version 2, Migrations Flyway, dataPagamento Obrigatoria (aberto), Filtros LIKE sem ESCAPE (+12 more)

### Community 22 - "Seguranca da API"
Cohesion: 0.18
Nodes (14): Autenticacao Bearer nos Endpoints, Access e Refresh Token, Autenticacao JWT Stateless, Bloqueio de Conta, Buckets em Caffeine com Expiracao, Claim 'tipo' Distingue Access de Refresh, CORS Allowlist, Defesa contra Enumeracao de Usuarios (+6 more)

### Community 24 - "Usuario"
Cohesion: 0.14
Nodes (10): org.springframework.security.core.userdetails.UserDetailsService, AllArgsConstructor, Entity, Getter, NoArgsConstructor, Setter, Usuario, UsuarioRepository (+2 more)

### Community 25 - "Guia das Migrations"
Cohesion: 0.20
Nodes (9): A V2 é idêntica nas duas pastas, Ao adicionar uma migration nova, As diferenças entre os dois conjuntos, Baseline: só no perfil `oracle`, Migrations, O custo disso, e o que segura o custo, Onde cada perfil busca as migrations, Por que dois conjuntos (+1 more)

### Community 26 - "Cache, Filtros e Suas Pendências"
Cohesion: 0.40
Nodes (6): Cache Caffeine, Chave de Cache das Listagens, Invalidacao allEntries, Paginacao e Ordenacao, Cache nao Invalida entre Entidades (aberto), Vazamento de Cache entre Contas

### Community 27 - "Challenge FIAP 2026"
Cohesion: 0.18
Nodes (12): Cronograma de Desenvolvimento, Divisao de Responsabilidades, CLYVO VET, Integrantes do Grupo, Stack Tecnologica, Frontend (30 pts, nao iniciado), Deploy Online Obrigatorio, Integracao Multidisciplinar (+4 more)

### Community 28 - "Maven Wrapper"
Cohesion: 0.33
Nodes (6): mvnw script, clean(), die(), exec_maven(), set_java_home(), verbose()

### Community 30 - "DDL Oracle Legado"
Cohesion: 0.46
Nodes (6): animal, clinica, evento_clinico, pagamento, tutor, veterinario

### Community 32 - "Bootstrap da Aplicação"
Cohesion: 0.60
Nodes (3): org.springframework.boot.autoconfigure.SpringBootApplication, org.springframework.cache.annotation.EnableCaching, ClyvovetApplication

### Community 33 - "Schema Inicial (MySQL)"
Cohesion: 0.57
Nodes (6): animal, clinica, evento_clinico, pagamento, tutor, veterinario

### Community 34 - "Schema Inicial (Oracle)"
Cohesion: 0.57
Nodes (6): animal, clinica, evento_clinico, pagamento, tutor, veterinario

### Community 40 - "Tabela Usuario (MySQL)"
Cohesion: 0.50
Nodes (3): tutor, veterinario, usuario

### Community 41 - "Tabela Usuario (Oracle)"
Cohesion: 0.50
Nodes (3): tutor, veterinario, usuario

## Knowledge Gaps
- **51 isolated node(s):** `AUTH`, `GERAL`, `LOGIN`, `DESCONHECIDO`, `FEMEA` (+46 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **9 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Usuario` connect `Usuario` to `Testes de CRUD e Integração`, `Veterinários e Anotações Base`, `lombok.RequiredArgsConstructor`, `DevDataSeeder`, `Identidade e Ownership`, `Emissão e Leitura de JWT`, `AuthController.java`, `Suporte de Testes de API`, `Repositórios Base e Recursos`, `.toResponse`, `Login e Perfis de Acesso`, `Seguranca da API`?**
  _High betweenness centrality (0.070) - this node is a cross-community bridge._
- **Why does `Modelo de Dominio CLYVO VET` connect `Veterinários e Anotações Base` to `API REST CRUD`, `Pagamentos e Formas de Pagamento`, `Challenge FIAP 2026`, `Animal`?**
  _High betweenness centrality (0.053) - this node is a cross-community bridge._
- **Why does `Tutor` connect `Veterinários e Anotações Base` to `Testes de CRUD e Integração`, `lombok.RequiredArgsConstructor`, `TutorResponse`, `Pagamentos e Formas de Pagamento`, `org.springframework.http.ResponseEntity`, `Animal`, `.toResponse`, `Usuario`, `Testes de Mapper de Animal`?**
  _High betweenness centrality (0.045) - this node is a cross-community bridge._
- **What connects `AUTH`, `GERAL`, `LOGIN` to the rest of the system?**
  _51 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Testes de CRUD e Integração` be split into smaller, more focused modules?**
  _Cohesion score 0.05936024044654358 - nodes in this community are weakly interconnected._
- **Should `RateLimitFilter` be split into smaller, more focused modules?**
  _Cohesion score 0.06944444444444445 - nodes in this community are weakly interconnected._
- **Should `Veterinários e Anotações Base` be split into smaller, more focused modules?**
  _Cohesion score 0.05845464725643897 - nodes in this community are weakly interconnected._