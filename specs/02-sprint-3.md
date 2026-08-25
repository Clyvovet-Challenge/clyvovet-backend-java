# Sprint 3 — Java Advanced

**Entrega:** 12/09/2026 · **Peso:** 100 pontos · **Status:** 50/100 implementados

| Requisito | Pontos | Situação |
|---|---|---|
| Frontend | 30 | não iniciado — adiado por decisão do time |
| Flyway | 20 | ✅ V1–V4 em dois conjuntos: `oracle/` (serve também ao H2, que roda em `MODE=Oracle`) e `mysql/` |
| Spring Security | 30 | ✅ JWT, 3 perfis, ownership, hardening |
| Funcionalidades (2 fluxos não-CRUD) | 20 | não iniciado — fluxos ainda não escolhidos |

Detalhamento do que foi entregue em [../docs/08-seguranca.md](../docs/08-seguranca.md).

---

## Escopo (texto oficial)

> Você deverá desenvolver uma **aplicação web completa** utilizando o framework Spring
> Boot para dar suporte à solução proposta. Esta aplicação deve ter o foco nos
> seguintes tópicos:
>
> 1. Camada de visualização (frontend);
> 2. Flyway para controle de versões do banco de dados;
> 3. Spring Security para autenticação e controle de acesso.

A palavra-chave é **aplicação web**, não API. O projeto atual é uma API REST headless;
a Sprint 3 pede uma camada de visualização servida pela própria aplicação Spring.

---

## Requisitos e pontuação

| # | Requisito | Pontos |
|---|---|---|
| 1 | **Frontend** | 30 |
| 2 | **Flyway** | 20 |
| 3 | **Spring Security** | 30 |
| 4 | **Funcionalidades completas** | 20 |

### 1. Frontend — 30 pontos

Camada de visualização. O documento não prescreve tecnologia; a leitura natural, dado
que o requisito está na disciplina de Java e o item 4 fala em "validações básicas nos
**formulários**", é server-side rendering com Thymeleaf.

### 2. Flyway — 20 pontos

Controle de versões do banco de dados. Implica migrar do modelo atual
(`ddl-auto` + `db-oracle.sql` executado à mão) para migrations versionadas.

### 3. Spring Security — 30 pontos

| Sub-requisito | Detalhe |
|---|---|
| Autenticação | Login funcional |
| Perfis | **Pelo menos dois tipos de usuário, com permissões diferentes** |
| Autorização | **Proteção de rotas com base no perfil do usuário** |

O domínio já sugere os perfis: **tutor** (vê o próprio pet e histórico) e
**veterinário/clínica** (registra eventos, vê a base de pacientes).

### 4. Funcionalidades completas — 20 pontos

| Sub-requisito | Detalhe |
|---|---|
| Fluxos | **Pelo menos dois fluxos completos do sistema — exceto CRUD** |
| Validações | Básicas nos formulários e nos dados |

Este é o item que materializa a regra do 1º semestre ("CRUD não é suficiente"). Um
fluxo completo tem entrada → processamento com regra de negócio → resultado, e
atravessa mais de uma entidade. Cadastrar um animal não conta; agendar um retorno
verificando disponibilidade e disparando lembrete conta.

---

## Penalidades

Aplicadas **independentemente dos requisitos técnicos** — dizem respeito à qualidade
do código e ao funcionamento geral do sistema.

| Problema | Desconto |
|---|---|
| Violação evidente de princípios SOLID (métodos gigantes, responsabilidades múltiplas) | −10 por ocorrência |
| Código com repetições desnecessárias (violação de DRY) | −5 por ocorrência |
| Código com problemas de legibilidade (violação de Clean Code) | −5 por ocorrência |
| Comentários no lugar de refatorações claras | −3 |
| Funcionalidade com comportamento inesperado ou erro evidente | −5 por funcionalidade |
| Página que não carrega ou link quebrado | −5 por página/link |
| Apresentação incompleta ou incoerente com a proposta do challenge | −15 |

São **por ocorrência** e cumulativas. Os seis controllers/services/mappers atuais são
estruturalmente idênticos — se a duplicação crescer para as telas, o desconto por DRY
escala junto.

---

## Entrega

Artefatos no portal, até o prazo. Não são aceitas entregas após o prazo ou por outros meios.

| Artefato | Detalhe |
|---|---|
| Repositório | Link público do GitHub com o código-fonte da aplicação Spring completa |
| README | Todas as instruções de **instalação, execução e acesso** da aplicação |
| Vídeo | Demonstração da aplicação funcionando, com as principais funcionalidades da aplicação web — **máx. 10 min** |

---

## Avaliação oral

Após a entrega, **cada aluno** participa de uma avaliação oral individual em sala. O
objetivo declarado é verificar a compreensão do código entregue e promover o uso
consciente de IA. Durante a conversa, o aluno deverá:

- Explicar trechos específicos do próprio código
- Justificar decisões de implementação
- Comentar eventuais dificuldades encontradas
- Descrever se e como utilizou ferramentas de IA no processo

Consequência prática: cada integrante precisa conseguir defender oralmente a parte que
escreveu. Vale distribuir o trabalho por fluxo, não por camada.

---

## Impacto no código atual

| Área | Situação | Sprint 3 exige |
|---|---|---|
| Camada web | API REST pura, sem views | Frontend com formulários — **falta** |
| Schema | ✅ Flyway com 4 migrations versionadas | atendido |
| Segurança | ✅ JWT, 3 perfis, rotas protegidas, ownership | atendido, acima do mínimo (pedia 2 perfis) |
| Domínio | CRUD sobre 7 entidades | 2 fluxos de negócio não-CRUD — **falta** |
| Usuário | ✅ `Usuario` + `Perfil`, vinculado a Tutor/Veterinário | atendido |

Backlog derivado em [05-plano-de-implementacao.md](05-plano-de-implementacao.md).
