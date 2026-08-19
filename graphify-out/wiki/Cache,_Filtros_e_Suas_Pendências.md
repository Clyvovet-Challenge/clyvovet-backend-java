# Cache, Filtros e Suas Pendências

> 13 nodes · cohesion 0.17

## Key Concepts

- **Cache Caffeine** (7 connections) — `docs/01-arquitetura.md`
- **Filtro Opcional JPQL** (5 connections) — `docs/01-arquitetura.md`
- **Ownership** (5 connections) — `docs/08-seguranca.md`
- **Vazamento de Cache entre Contas** (4 connections) — `docs/08-seguranca.md`
- **Chave de Cache das Listagens** (3 connections) — `docs/01-arquitetura.md`
- **Filtros LIKE sem ESCAPE** (3 connections) — `docs/07-pendencias-e-divergencias.md`
- **Ownership em Listagens** (3 connections) — `docs/08-seguranca.md`
- **Ownership do Dono Informado no Corpo** (3 connections) — `docs/08-seguranca.md`
- **Invalidacao allEntries** (2 connections) — `docs/01-arquitetura.md`
- **Filtros Opcionais por Recurso** (2 connections) — `docs/03-api-rest.md`
- **Paginacao e Ordenacao** (2 connections) — `docs/03-api-rest.md`
- **Cache nao Invalida entre Entidades (aberto)** (2 connections) — `docs/07-pendencias-e-divergencias.md`
- **tutorId do Corpo sem Checagem** (2 connections) — `docs/07-pendencias-e-divergencias.md`

## Relationships

- [Evidências e Contrato da API](Evidências_e_Contrato_da_API.md) (3 shared connections)
- [Schema, Migrations e Divergências](Schema,_Migrations_e_Divergências.md) (3 shared connections)
- [Decisões de Arquitetura e Domínio](Decisões_de_Arquitetura_e_Domínio.md) (2 shared connections)
- [Hardening e Autenticação](Hardening_e_Autenticação.md) (2 shared connections)
- [Paginação e Listagens](Paginação_e_Listagens.md) (2 shared connections)
- [Configuração de Segurança e Filtros](Configuração_de_Segurança_e_Filtros.md) (1 shared connections)
- [Serviços com Cache e Repositórios](Serviços_com_Cache_e_Repositórios.md) (1 shared connections)
- [Identidade e Ownership](Identidade_e_Ownership.md) (1 shared connections)
- [Animais e Autorização de Rota](Animais_e_Autorização_de_Rota.md) (1 shared connections)
- [Testes de CRUD e Integração](Testes_de_CRUD_e_Integração.md) (1 shared connections)

## Source Files

- `docs/01-arquitetura.md`
- `docs/03-api-rest.md`
- `docs/07-pendencias-e-divergencias.md`
- `docs/08-seguranca.md`

## Audit Trail

- EXTRACTED: 26 (87%)
- INFERRED: 4 (13%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*