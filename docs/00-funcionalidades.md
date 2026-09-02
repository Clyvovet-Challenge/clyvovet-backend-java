# Funcionalidades

O que o backend do CLYVO VET faz hoje, em termos de capacidade — não de
implementação. Para *como* cada coisa é construída, siga os links para os
documentos técnicos.

Este documento descreve **o que está no código e coberto por teste**. O que ainda
não existe está na seção [O que ainda não faz](#o-que-ainda-não-faz), e as
divergências conhecidas em [07-pendencias-e-divergencias.md](07-pendencias-e-divergencias.md).

---

## Em uma frase

Uma API REST que centraliza o histórico de saúde de pets: quem são os animais, quem
cuida deles, o que aconteceu em cada atendimento e quanto foi cobrado — com controle
de quem enxerga o quê.

---

## O domínio

Seis entidades, ligadas assim:

```
Tutor ──< Animal ──< EventoClinico >── Veterinario ──> Clinica
                          │
                          └──< Pagamento
```

| Entidade | O que representa | Quem é dono da informação |
|---|---|---|
| **Tutor** | a pessoa responsável pelo animal | ela mesma |
| **Animal** | o pet: espécie, raça, porte, nascimento | o tutor |
| **Clinica** | o estabelecimento veterinário | a rede |
| **Veterinario** | o profissional, vinculado a uma clínica | a clínica |
| **EventoClinico** | um atendimento: consulta, vacina, exame, cirurgia… | a clínica que atendeu |
| **Pagamento** | a cobrança de um atendimento | a clínica |

O modelo completo — colunas, tipos, constraints e o mapeamento objeto↔tabela — está
em [02-modelo-de-dados.md](02-modelo-de-dados.md).

---

## Funcionalidades

### 1. Cadastro e consulta dos seis recursos

Cada entidade expõe o mesmo conjunto de operações sob `/api/v1`:

| Operação | Verbo | Resposta |
|---|---|---|
| Listar (paginado, com filtros) | `GET /recurso` | 200 |
| Buscar por id | `GET /recurso/{id}` | 200 |
| Criar | `POST /recurso` | **201** |
| Substituir por inteiro | `PUT /recurso/{id}` | 200 |
| Alterar só alguns campos | `PATCH /recurso/{id}` | 200 |
| Remover | `DELETE /recurso/{id}` | **204** |

São **36 rotas de domínio** nos seis recursos de CRUD — seis operações cada —
mais os quatro fluxos de ação, que somam **74 rotas** no total. Contratos,
payloads e códigos de erro em [03-api-rest.md](03-api-rest.md).

### 2. Busca, paginação e ordenação

Toda listagem aceita `page`, `size` e `sort`, mais filtros próprios do recurso:

| Recurso | Filtros |
|---|---|
| `/api/v1/tutores` | `nome`, `cidade` |
| `/api/v1/animais` | `nome`, `especie` |
| `/api/v1/clinicas` | `nome`, `cidade` |
| `/api/v1/veterinarios` | `nome`, `especialidade` |
| `/api/v1/eventos-clinicos` | `tipoEvento`, `animalNome` |
| `/api/v1/pagamentos` | `statusPagamento`, `formaPagamento` |

Os filtros de texto são *case-insensitive* e por trecho (`LIKE %termo%`). Filtro
ausente não restringe nada, então eles se combinam livremente.

A resposta é sempre a mesma forma:

```json
{
  "content": [ ... ],
  "page": { "size": 10, "number": 0, "totalElements": 42, "totalPages": 5 }
}
```

### 3. Atualização parcial

`PUT` exige o recurso inteiro; `PATCH` aceita só o que muda:

```bash
curl -X PATCH http://localhost:8080/api/v1/clinicas/{id} \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{"telefone":"1199998888"}'
```

O que não vier no corpo fica como está. Campo enviado continua validado; campo
omitido não entra na conta. **Não dá para apagar um campo opcional via PATCH** — para
isso, use `PUT`. O porquê está em
[03-api-rest.md](03-api-rest.md#atualização-parcial-patch).

### 4. Autenticação

Login por e-mail e senha, com JWT. A senha é guardada como hash BCrypt — nunca em
texto, nunca devolvida em resposta alguma.

| Rota | O que faz | Acesso |
|---|---|---|
| `POST /api/v1/auth/login` | troca credenciais por tokens | público |
| `POST /api/v1/auth/refresh` | renova o access token | público |
| `POST /api/v1/auth/logout` | revoga o refresh token (**204**) | público |
| `POST /api/v1/auth/registrar` | auto-cadastro — o perfil é sempre TUTOR (**201**) | público |
| `POST /api/v1/auth/usuarios` | cria usuário de qualquer perfil (**201**) | **ADMIN** |
| `GET /api/v1/auth/me` | dados do usuário autenticado | autenticado |

Dois tokens, com propósitos diferentes:

| Token | Validade | Serve para |
|---|---|---|
| `accessToken` | 15 minutos | autorizar cada requisição |
| `refreshToken` | 7 dias | obter um novo access sem refazer login |

O access é curto de propósito: se vazar, a janela de uso é pequena. O refresh é longo
para não obrigar o usuário a digitar a senha a cada 15 minutos. Ambos configuráveis
por `clyvovet.jwt.access-token-minutos` e `clyvovet.jwt.refresh-token-dias`.

**O logout revoga de verdade.** O refresh token entra numa lista de revogados que vive
enquanto ele viveria, então reusá-lo depois do logout responde 401 — não basta o
cliente descartar o token.

### 5. Perfis de acesso

Três perfis, com alcance crescente:

| Perfil | Enxerga | Escreve |
|---|---|---|
| **TUTOR** | só os próprios pets e o próprio cadastro | os próprios pets |
| **VETERINARIO** | toda a base | animais, tutores, eventos e pagamentos |
| **ADMIN** | toda a base | tudo, inclusive clínicas, veterinários e usuários |

Listar tutores é restrito a VETERINARIO e ADMIN — a listagem expõe CPF e e-mail de
terceiros.

### 6. Ownership: um tutor só enxerga os próprios pets

Regra de perfil não resolve isto: dois tutores têm o mesmo perfil e passariam
igualmente por ela. A verificação acontece em três frentes, porque há três maneiras
de contornar uma só:

| Frente | O que impede |
|---|---|
| Acesso por id | buscar `/api/v1/animais/{id}` de um pet alheio |
| Filtro na listagem | ver pets alheios em `GET /api/v1/animais` |
| Dono informado no **corpo** | transferir o próprio pet para outro tutor |

A chave do cache inclui o `tutorId`. Sem isso, a listagem de um tutor seria servida ao
seguinte que pedisse a mesma página. Detalhes em
[08-seguranca.md](08-seguranca.md).

### 7. Proteção contra força bruta

Duas camadas, que respondem a ataques diferentes:

| Camada | Alvo | Comportamento |
|---|---|---|
| **Bloqueio de conta** | uma conta específica | 5 falhas bloqueiam por 15 minutos |
| **Rate limit por IP** | volume, venha de onde vier | 10 logins/min · 30 em `/auth`/min · 100 geral/min |

O bloqueio protege a conta de quem está sendo atacado; o rate limit protege o
servidor de quem tenta muitas contas. Uma camada não substitui a outra.

Estourar o limite responde **429**. Os dois são configuráveis
(`clyvovet.seguranca.max-tentativas-login`, `clyvovet.seguranca.bloqueio-minutos`).

> Estado local ao processo. Com mais de uma réplica, cada uma teria a própria
> contagem — o caminho seria Bucket4j sobre Redis.

### 8. Erros previsíveis

A API não devolve 500 para situação esperada. Cada caso tem seu código:

| Situação | Código |
|---|---|
| Campo inválido ou fora do formato | **400** |
| Sem token, ou token inválido/expirado | **401** |
| Token válido, mas perfil insuficiente | **403** |
| Recurso não existe | **404** |
| CPF, CNPJ, e-mail ou CRMV já cadastrado | **409** |
| Excluir registro com dependentes | **409** |
| Limite de requisições estourado | **429** |

O corpo é sempre `{ "campo": ..., "mensagem": ... }`, apontando o que corrigir.

### 9. Cache de leitura

As listagens passam por cache Caffeine — 10 minutos, no máximo 1.000 entradas. Toda
escrita no recurso invalida o cache dele por inteiro.

A chave inclui os filtros, a paginação, a ordenação **e** o `tutorId` de quem pediu.
Faltando qualquer um deles, uma consulta devolveria o resultado de outra.

> Cache em memória, por processo. Com mais de uma instância, o caminho seria Redis.

### 10. Documentação interativa

Swagger UI em `/swagger-ui.html` e OpenAPI 3 em `/v3/api-docs`, com todos os
endpoints, schemas e códigos de resposta. Ficam **fora** do prefixo `/api/v1`, na
raiz, onde as ferramentas esperam encontrá-los.

### 11. Cabeçalhos de segurança e CORS

HSTS com `includeSubDomains`, `X-Content-Type-Options: nosniff`, `Referrer-Policy`,
CSP `default-src 'self'; frame-ancestors 'none'` e `X-Frame-Options: SAMEORIGIN`.

CORS liberado por padrão para `http://localhost:3000` e `http://localhost:8081` — o
frontend web e o app mobile em desenvolvimento. Configurável por
`clyvovet.cors.origens`.

---

## Como as peças se encaixam

Um atendimento completo, do cadastro à cobrança:

```
1. POST /api/v1/auth/login              → tutor obtém accessToken
2. POST /api/v1/tutores                 → veterinário cadastra o tutor
3. POST /api/v1/animais                 → cadastra o pet, vinculado ao tutor
4. POST /api/v1/eventos-clinicos        → registra a consulta
5. POST /api/v1/pagamentos              → cobra o atendimento
6. GET  /api/v1/eventos-clinicos?animalNome=Bolinha
                                        → histórico clínico do pet
```

A ordem importa: pagamento exige evento, evento exige animal, veterinário e clínica,
e animal exige tutor. Tentar fora de ordem devolve **404** apontando a FK que falta.

---

## Onde os dados vivem

| Perfil | Banco | Uso |
|---|---|---|
| `dev` | H2 em memória | desenvolvimento local — não precisa de configuração |
| `h2` | H2 em modo servidor | dentro do docker-compose |
| `oracle` | Oracle 19c FIAP | entrega e banco de testes |
| `mysql` | Azure Database for MySQL | alvo do deploy — [ainda não validado](07-pendencias-e-divergencias.md) |

O schema é versionado pelo Flyway, com um conjunto de migrations por banco. Ver
[04-configuracao.md](04-configuracao.md).

---

## O que ainda não faz

Registrado para não parecer omissão:

| Não existe | Observação |
|---|---|
| HATEOAS | a API está no nível 2 de Richardson: recursos + verbos + status |
| Upload de imagens ou anexos | sem foto de pet, sem laudo em PDF |
| Notificações | nada de e-mail, push ou lembrete de vacina |
| Relatórios e totalizadores | não há endpoint de faturamento por período |
| Auditoria | sem `createdAt`/`updatedAt` nem histórico de alterações |
| Soft delete | `DELETE` remove a linha de verdade |
| Recuperação de senha | não há fluxo de "esqueci minha senha" |
| Cache e rate limit distribuídos | ambos por processo; com réplicas, precisariam de Redis |
| Coleção Postman/Insomnia exportada | pendente, e vale 10 pontos na rubrica |

### Coberto por teste

Os 265 testes cobrem o CRUD dos seis recursos, filtros, atualização parcial, mappers,
JWT, ciclo de sessão, bloqueio de conta, ownership, rate limit e as migrations do
MySQL.

`CicloDeSessaoTest` fecha a lacuna que existia em `/auth/me`, `/auth/refresh`,
`/auth/logout` e `/auth/usuarios` — os quatro endpoints que ficaram sem cobertura até
19/08/2026. Além do caminho feliz, ele pina duas propriedades que só apareceriam em
produção se quebrassem:

- **um access token não é aceito como refresh.** Os dois são JWT assinados pela mesma
  chave; o que os separa é a claim `tipo`. Se o refresh aceitasse um access, um access
  vazado viraria sessão renovável por sete dias.
- **logout repetido responde 204.** Um retry ou uma aba duplicada não deve tomar erro:
  o efeito desejado — token revogado — já aconteceu.

---

## Números

| | |
|---|---|
| Entidades de domínio | 13 (+ `Endereco` embutido) |
| Rotas | 74, todas sob `/api/v1` |
| Perfis de acesso | 3 |
| Migrations | V1 a V7, em dois conjuntos (Oracle e MySQL) |
| Testes automatizados | 265 |
