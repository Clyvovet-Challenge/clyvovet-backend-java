# 004 — Banco compartilhado entre as duas APIs, com a Java dona do schema

**Data:** 2026-09-06 (auditoria de arquitetura), registrada aqui em 2026-10-10 · **Status:** aceito

## Contexto

A plataforma tem duas APIs: esta (Java), que cobre identidade, núcleo clínico, agenda e cobrança, e
uma API .NET de engajamento (lembretes, sugestão de produtos, widget de saúde preditiva). A .NET
precisa ler dados que nascem aqui, como animal e tutor. As alternativas eram:

- a .NET consumir a Java por HTTP;
- bancos separados com mensageria entre eles;
- um banco só, compartilhado.

## Decisão

**Um banco compartilhado, com a API Java dona do schema.**

A condição que torna isso seguro é que **cada tabela tem exatamente um escritor**. A .NET lê
`t_clyvo_animal` e `t_clyvo_tutor` e nunca escreve nelas (a auditoria de 06/09 conferiu no
`AnimalRepository.cs`, que expõe só `GetByIdAsync` e `GetByTutorIdAsync`).

- O **Flyway desta API é a fonte única do schema**, inclusive das seis tabelas que só a .NET usa
  (trazidas na `V8`). A .NET continua dona do *conteúdo* delas; só a *definição* mora aqui.
- A emissão e a validação de **JWT** ficam aqui. A .NET não tem noção de usuário.

## Consequências

- Não há HTTP nem fila entre as APIs para dados de leitura, ou seja, menos componentes para operar.
  As alternativas resolviam disputa de escrita entre serviços, e essa disputa não existe aqui.
- Uma migration nova pode quebrar a .NET sem que o build desta API perceba. Foi o que a `V9`
  (prefixo `t_clyvo_` nas tabelas) exigiu da .NET, registrado em
  [`../11-auditoria-de-arquitetura.md`](../11-auditoria-de-arquitetura.md) §3.1.
- Em ambiente novo, a API Java sobe antes da .NET: as tabelas que a .NET consome nascem nas
  migrations daqui.
- A regra "um escritor por tabela" passa a ser uma invariante a preservar. Se a .NET um dia
  escrever numa tabela da Java, esta decisão precisa ser revista.
