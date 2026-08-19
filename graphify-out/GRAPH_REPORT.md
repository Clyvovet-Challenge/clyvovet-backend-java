# Graph Report - clyvovet-backend-java  (2026-08-19)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 886 nodes · 2937 edges · 41 communities (35 shown, 6 thin omitted)
- Extraction: 85% EXTRACTED · 15% INFERRED · 0% AMBIGUOUS · INFERRED: 427 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `0915be34`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Testes de CRUD e Integração
- Configuração de Segurança e Filtros
- Veterinários e Anotações Base
- Clínicas e Endereço
- Eventos Clínicos
- Tratamento de Erros HTTP
- Serviços com Cache e Repositórios
- Tutores
- Pagamentos e Formas de Pagamento
- Identidade e Ownership
- Endpoints de Autenticação
- DTOs de Autenticação e Perfis
- Paginação e Listagens
- Animais e Autorização de Rota
- Emissão e Leitura de JWT
- Suporte de Testes de API
- Evidências e Contrato da API
- Repositórios Base e Recursos
- Configuração de Ambiente e Deploy
- Mapeamento de Evento e Pagamento
- Controller de Pagamentos
- Schema, Migrations e Divergências
- Hardening e Autenticação
- Entidade e Mapper de Tutor
- Entidade Usuário
- Decisões de Arquitetura e Domínio
- Cache, Filtros e Suas Pendências
- Entidade Animal
- Maven Wrapper
- Testes de Mapper de Animal
- DDL Oracle Legado
- Migration V1 Schema Inicial
- Bootstrap da Aplicação
- Sprint 4 e Entrega Final
- Migration V3 Usuário e Perfis
- Workflow graphify
- Script de Deploy Azure
- Lombok na IDE
- Coordenadas Maven
- Tabela Pagamento no Seed

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
- `Perfis Spring (oracle / h2 / dev)` --references--> `CacheConfig`  [INFERRED]
  docs/04-configuracao.md → src/main/java/br/com/fiap/clyvovet/config/CacheConfig.java
- `Cabecalhos de Seguranca` --references--> `SecurityConfig`  [EXTRACTED]
  docs/08-seguranca.md → src/main/java/br/com/fiap/clyvovet/config/SecurityConfig.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Defesa em Profundidade do Login** — docs_08_seguranca_autenticacao_jwt, docs_08_seguranca_bloqueio_de_conta, docs_08_seguranca_rate_limit_por_ip, docs_08_seguranca_enumeracao_de_usuarios, docs_08_seguranca_requires_new_na_contagem [EXTRACTED 0.90]
- **Evidencias de Teste dos POSTs** — documentos_post_animais_evidencia_post_animais, documentos_post_tutores_evidencia_post_tutores, documentos_post_clinicas_evidencia_post_clinicas, docs_03_api_rest_api_rest_crud [EXTRACTED 0.90]
- **Nucleo do Dominio Clinico** — docs_02_modelo_de_dados_modelo_de_dominio, src_main_java_br_com_fiap_clyvovet_model_animal_animal, src_main_java_br_com_fiap_clyvovet_model_veterinario_veterinario, src_main_java_br_com_fiap_clyvovet_model_clinica_clinica, src_main_java_br_com_fiap_clyvovet_model_eventoclinico_eventoclinico, src_main_java_br_com_fiap_clyvovet_model_pagamento_pagamento [EXTRACTED 0.95]
- **As Tres Frentes do Ownership** — docs_08_seguranca_ownership, docs_08_seguranca_ownership_por_id, docs_08_seguranca_ownership_em_listagens, docs_08_seguranca_ownership_no_corpo, docs_08_seguranca_vazamento_de_cache_entre_contas [EXTRACTED 0.95]
- **Lacunas Abertas para a Entrega Final** — specs_02_sprint_3_frontend_pendente, specs_02_sprint_3_fluxos_nao_crud, specs_04_dependencias_externas_devops_banco_em_nuvem, specs_04_dependencias_externas_pipeline_azure_devops, specs_03_sprint_4_deploy_online [INFERRED 0.85]

## Communities (41 total, 6 thin omitted)

### Community 0 - "Testes de CRUD e Integração"
Cohesion: 0.08
Nodes (17): com.fasterxml.jackson.databind.JsonNode, java.sql.Connection, org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable, org.junit.jupiter.api.DisplayName, org.junit.jupiter.api.Test, org.springframework.test.web.servlet.ResultActions, AtendimentoCrudTest, CadastroCrudTest (+9 more)

### Community 1 - "Configuração de Segurança e Filtros"
Cohesion: 0.06
Nodes (44): AuthorizationManagerRequestMatcherRegistry, com.fasterxml.jackson.databind.ObjectMapper, com.github.benmanes.caffeine.cache.Cache, Usuarios de Desenvolvimento, io.github.bucket4j.Bucket, io.github.bucket4j.ConsumptionProbe, io.swagger.v3.oas.models.OpenAPI, jakarta.servlet.FilterChain (+36 more)

### Community 2 - "Veterinários e Anotações Base"
Cohesion: 0.08
Nodes (31): Diagrama de Classes UML, io.swagger.v3.oas.annotations.Operation, lombok.AllArgsConstructor, lombok.Getter, lombok.NoArgsConstructor, lombok.Setter, DeleteMapping, GetMapping (+23 more)

### Community 3 - "Clínicas e Endereço"
Cohesion: 0.09
Nodes (20): jakarta.persistence.Embeddable, ClinicaController, GetMapping, PostMapping, PutMapping, RequestMapping, RestController, ClinicaRequest (+12 more)

### Community 4 - "Eventos Clínicos"
Cohesion: 0.09
Nodes (21): EventoClinicoController, DeleteMapping, GetMapping, PostMapping, PutMapping, RequestMapping, RestController, EventoClinicoRequest (+13 more)

### Community 5 - "Tratamento de Erros HTTP"
Cohesion: 0.14
Nodes (16): Tratamento Global de Erros, Erros que nao Vazam, jakarta.persistence.EntityNotFoundException, lombok.extern.slf4j.Slf4j, org.springframework.dao.DataIntegrityViolationException, org.springframework.http.HttpStatus, org.springframework.http.ResponseEntity, org.springframework.security.authentication.BadCredentialsException (+8 more)

### Community 6 - "Serviços com Cache e Repositórios"
Cohesion: 0.17
Nodes (12): Ownership por ID, org.springframework.cache.annotation.Cacheable, org.springframework.cache.annotation.CacheEvict, org.springframework.stereotype.Service, org.springframework.transaction.annotation.Transactional, AnimalRepository, ClinicaRepository, EventoClinicoRepository (+4 more)

### Community 7 - "Tutores"
Cohesion: 0.16
Nodes (13): io.swagger.v3.oas.annotations.tags.Tag, lombok.RequiredArgsConstructor, DeleteMapping, PostMapping, PutMapping, RequestMapping, RestController, TutorController (+5 more)

### Community 8 - "Pagamentos e Formas de Pagamento"
Cohesion: 0.12
Nodes (18): lombok.Data, PagamentoRequest, FormaPagamento, BOLETO, CARTAO, DINHEIRO, PIX, AllArgsConstructor (+10 more)

### Community 9 - "Identidade e Ownership"
Cohesion: 0.14
Nodes (7): org.springframework.security.core.GrantedAuthority, org.springframework.security.core.userdetails.UserDetails, org.springframework.security.core.userdetails.UserDetailsService, Override, UsuarioAutenticado, Override, UsuarioDetailsService

### Community 10 - "Endpoints de Autenticação"
Cohesion: 0.15
Nodes (8): org.springframework.web.bind.annotation.GetMapping, org.springframework.web.bind.annotation.PostMapping, org.springframework.web.bind.annotation.RequestMapping, org.springframework.web.bind.annotation.RestController, AuthController, RefreshRequest, RevogacaoTokenService, AuthService

### Community 11 - "DTOs de Autenticação e Perfis"
Cohesion: 0.16
Nodes (9): LoginResponse, UsuarioResponse, UsuarioMapper, Perfil, ADMIN, TUTOR, VETERINARIO, UsuarioService (+1 more)

### Community 12 - "Paginação e Listagens"
Cohesion: 0.22
Nodes (5): org.springframework.data.domain.Page, org.springframework.data.domain.Pageable, org.springframework.data.jpa.repository.Query, GetMapping, GetMapping

### Community 13 - "Animais e Autorização de Rota"
Cohesion: 0.19
Nodes (11): org.springframework.security.access.prepost.PreAuthorize, AnimalController, DeleteMapping, PostMapping, PutMapping, RequestMapping, RestController, AnimalRequest (+3 more)

### Community 14 - "Emissão e Leitura de JWT"
Cohesion: 0.21
Nodes (4): io.jsonwebtoken.Claims, javax.crypto.SecretKey, JwtService, JwtServiceTest

### Community 15 - "Suporte de Testes de API"
Cohesion: 0.16
Nodes (7): org.junit.jupiter.api.AfterEach, org.junit.jupiter.api.BeforeEach, org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc, org.springframework.boot.test.context.SpringBootTest, org.springframework.test.web.servlet.MockMvc, ClyvovetApplicationTests, BloqueioContaTest

### Community 16 - "Evidências e Contrato da API"
Cohesion: 0.17
Nodes (18): Desnormalizacao do Nome na Resposta, API REST CRUD, Swagger / OpenAPI, Cronograma de Desenvolvimento, Divisao de Responsabilidades, Evidencia POST /animais, Evidencia POST /clinicas, Evidencia POST /eventos-clinicos (+10 more)

### Community 17 - "Repositórios Base e Recursos"
Cohesion: 0.16
Nodes (11): org.springframework.data.jpa.repository.JpaRepository, org.springframework.data.repository.NoRepositoryBean, Recurso, ANIMAL, CLINICA, EVENTO_CLINICO, PAGAMENTO, TUTOR (+3 more)

### Community 18 - "Configuração de Ambiente e Deploy"
Cohesion: 0.15
Nodes (17): Servico clyvovet-db, UUID como Chave Primaria, UUID gravado como CHAR no Oracle, Perfil h2 so roda no Docker, Perfis Spring (oracle / h2 / dev), Precedencia de Configuracao, Variaveis de Ambiente Obrigatorias, Deploy Azure via deploy.sh (+9 more)

### Community 19 - "Mapeamento de Evento e Pagamento"
Cohesion: 0.23
Nodes (9): FetchType EAGER Explicito, PagamentoMapper, EventoClinico, AllArgsConstructor, Entity, Getter, NoArgsConstructor, Setter (+1 more)

### Community 20 - "Controller de Pagamentos"
Cohesion: 0.21
Nodes (7): GetMapping, PostMapping, PutMapping, RequestMapping, RestController, PagamentoController, PagamentoResponse

### Community 21 - "Schema, Migrations e Divergências"
Cohesion: 0.17
Nodes (15): Constraints que so existem no Banco, Schema Oracle (db-oracle.sql), Seed Data, Baseline Version 2, Migrations Flyway, dataPagamento Obrigatoria (aberto), especie e porte como Texto Livre (aberto), Exclusao com Dependentes (+7 more)

### Community 22 - "Hardening e Autenticação"
Cohesion: 0.15
Nodes (16): Autenticacao Bearer nos Endpoints, Access e Refresh Token, Autenticacao JWT Stateless, Bloqueio de Conta, Buckets em Caffeine com Expiracao, Cabecalhos de Seguranca, Claim 'tipo' Distingue Access de Refresh, CORS Allowlist (+8 more)

### Community 23 - "Entidade e Mapper de Tutor"
Cohesion: 0.20
Nodes (7): AllArgsConstructor, Entity, Getter, NoArgsConstructor, Setter, Tutor, TutorMapperTest

### Community 24 - "Entidade Usuário"
Cohesion: 0.19
Nodes (8): AllArgsConstructor, Entity, Getter, NoArgsConstructor, Setter, Usuario, UsuarioRepository, ControleTentativasLogin

### Community 25 - "Decisões de Arquitetura e Domínio"
Cohesion: 0.16
Nodes (15): Arquitetura em Camadas, DTOs em vez de Entidades, Fluxo de Requisicao, Mappers Manuais, Convencao Java vs Coluna, Dois Enums de Sexo, Endereco Embeddable, Enums Persistidos como STRING (+7 more)

### Community 26 - "Cache, Filtros e Suas Pendências"
Cohesion: 0.17
Nodes (13): Cache Caffeine, Chave de Cache das Listagens, Filtro Opcional JPQL, Invalidacao allEntries, Filtros Opcionais por Recurso, Paginacao e Ordenacao, Cache nao Invalida entre Entidades (aberto), Filtros LIKE sem ESCAPE (+5 more)

### Community 27 - "Entidade Animal"
Cohesion: 0.19
Nodes (10): Animal, AllArgsConstructor, Entity, Getter, NoArgsConstructor, Setter, SexoAnimal, DESCONHECIDO (+2 more)

### Community 28 - "Maven Wrapper"
Cohesion: 0.33
Nodes (6): mvnw script, clean(), die(), exec_maven(), set_java_home(), verbose()

### Community 30 - "DDL Oracle Legado"
Cohesion: 0.46
Nodes (6): animal, clinica, evento_clinico, pagamento, tutor, veterinario

### Community 31 - "Migration V1 Schema Inicial"
Cohesion: 0.57
Nodes (6): animal, clinica, evento_clinico, pagamento, tutor, veterinario

### Community 32 - "Bootstrap da Aplicação"
Cohesion: 0.60
Nodes (3): org.springframework.boot.autoconfigure.SpringBootApplication, org.springframework.cache.annotation.EnableCaching, ClyvovetApplication

### Community 33 - "Sprint 4 e Entrega Final"
Cohesion: 0.50
Nodes (4): Frontend (30 pts, nao iniciado), Deploy Online Obrigatorio, Integracao Multidisciplinar, Sprint 4

### Community 34 - "Migration V3 Usuário e Perfis"
Cohesion: 0.50
Nodes (3): tutor, veterinario, usuario

## Knowledge Gaps
- **44 isolated node(s):** `AUTH`, `GERAL`, `LOGIN`, `ADMIN`, `TUTOR` (+39 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **6 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Usuario` connect `Entidade Usuário` to `Configuração de Segurança e Filtros`, `Veterinários e Anotações Base`, `Tutores`, `Identidade e Ownership`, `Endpoints de Autenticação`, `DTOs de Autenticação e Perfis`, `Emissão e Leitura de JWT`, `Suporte de Testes de API`, `Hardening e Autenticação`, `Entidade e Mapper de Tutor`?**
  _High betweenness centrality (0.075) - this node is a cross-community bridge._
- **Why does `Modelo de Dominio CLYVO VET` connect `Decisões de Arquitetura e Domínio` to `Veterinários e Anotações Base`, `Pagamentos e Formas de Pagamento`, `Evidências e Contrato da API`, `Mapeamento de Evento e Pagamento`, `Entidade e Mapper de Tutor`, `Entidade Animal`?**
  _High betweenness centrality (0.062) - this node is a cross-community bridge._
- **Why does `Tutor` connect `Entidade e Mapper de Tutor` to `Testes de CRUD e Integração`, `Veterinários e Anotações Base`, `Clínicas e Endereço`, `Serviços com Cache e Repositórios`, `Tutores`, `DTOs de Autenticação e Perfis`, `Paginação e Listagens`, `Animais e Autorização de Rota`, `Entidade Usuário`, `Decisões de Arquitetura e Domínio`, `Entidade Animal`, `Testes de Mapper de Animal`?**
  _High betweenness centrality (0.049) - this node is a cross-community bridge._
- **What connects `AUTH`, `GERAL`, `LOGIN` to the rest of the system?**
  _44 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Testes de CRUD e Integração` be split into smaller, more focused modules?**
  _Cohesion score 0.077275600505689 - nodes in this community are weakly interconnected._
- **Should `Configuração de Segurança e Filtros` be split into smaller, more focused modules?**
  _Cohesion score 0.05837837837837838 - nodes in this community are weakly interconnected._
- **Should `Veterinários e Anotações Base` be split into smaller, more focused modules?**
  _Cohesion score 0.0777489818585709 - nodes in this community are weakly interconnected._