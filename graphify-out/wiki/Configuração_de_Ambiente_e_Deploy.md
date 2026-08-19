# Configuração de Ambiente e Deploy

> 17 nodes · cohesion 0.15

## Key Concepts

- **Perfis Spring (oracle / h2 / dev)** (9 connections) — `docs/04-configuracao.md`
- **Dockerfile Multi-stage** (6 connections) — `docs/05-deploy.md`
- **Docker Compose (API + H2)** (4 connections) — `docs/05-deploy.md`
- **Dependencias Externas sobre o Backend** (4 connections) — `specs/04-dependencias-externas.md`
- **DevOps Exige Banco em Nuvem** (4 connections) — `specs/04-dependencias-externas.md`
- **Perfil h2 so roda no Docker** (3 connections) — `docs/04-configuracao.md`
- **Deploy Azure via deploy.sh** (3 connections) — `docs/05-deploy.md`
- **Credenciais Oracle Versionadas** (3 connections) — `docs/07-pendencias-e-divergencias.md`
- **Testes de Seguranca** (3 connections) — `docs/08-seguranca.md`
- **Pipeline CI/CD Azure DevOps** (3 connections) — `specs/04-dependencias-externas.md`
- **UUID como Chave Primaria** (2 connections) — `docs/01-arquitetura.md`
- **UUID gravado como CHAR no Oracle** (2 connections) — `docs/02-modelo-de-dados.md`
- **Variaveis de Ambiente Obrigatorias** (2 connections) — `docs/04-configuracao.md`
- **Perfil Fixo no ENTRYPOINT** (2 connections) — `docs/05-deploy.md`
- **Servico clyvovet-db** (1 connections) — `docker-compose.yml`
- **Precedencia de Configuracao** (1 connections) — `docs/04-configuracao.md`
- **Container sem Privilegio de Root** (1 connections) — `docs/05-deploy.md`

## Relationships

- [Schema, Migrations e Divergências](Schema,_Migrations_e_Divergências.md) (2 shared connections)
- [Evidências e Contrato da API](Evidências_e_Contrato_da_API.md) (2 shared connections)
- [Decisões de Arquitetura e Domínio](Decisões_de_Arquitetura_e_Domínio.md) (2 shared connections)
- [Hardening e Autenticação](Hardening_e_Autenticação.md) (2 shared connections)
- [Entidade Animal](Entidade_Animal.md) (1 shared connections)
- [Configuração de Segurança e Filtros](Configuração_de_Segurança_e_Filtros.md) (1 shared connections)
- [Sprint 4 e Entrega Final](Sprint_4_e_Entrega_Final.md) (1 shared connections)

## Source Files

- `docker-compose.yml`
- `docs/01-arquitetura.md`
- `docs/02-modelo-de-dados.md`
- `docs/04-configuracao.md`
- `docs/05-deploy.md`
- `docs/07-pendencias-e-divergencias.md`
- `docs/08-seguranca.md`
- `specs/04-dependencias-externas.md`

## Audit Trail

- EXTRACTED: 27 (84%)
- INFERRED: 5 (16%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*