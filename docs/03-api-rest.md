# API REST

Base URL local: `http://localhost:8080` — não há context-path configurado.
Todos os endpoints consomem e produzem `application/json`.

**Todos os endpoints exigem autenticação**, exceto `/auth/login`, `/auth/refresh`,
`/auth/registrar` e o Swagger. Envie o access token no header:

```
Authorization: Bearer <access-token>
```

Obtenha-o em `POST /auth/login`. Os cinco endpoints de `/auth`, a matriz de
autorização por perfil e as regras de ownership estão em
[08-seguranca.md](08-seguranca.md).

---

## Índice de endpoints

Trinta rotas, cinco por entidade, todas no mesmo padrão CRUD.

| # | Verbo | Rota | Descrição | Status de sucesso |
|---|---|---|---|---|
| 1 | GET | `/tutores` | Lista tutores paginados | 200 |
| 2 | GET | `/tutores/{id}` | Busca tutor por UUID | 200 |
| 3 | POST | `/tutores` | Cadastra tutor | 201 |
| 4 | PUT | `/tutores/{id}` | Atualiza tutor | 200 |
| 5 | DELETE | `/tutores/{id}` | Remove tutor | 204 |
| 6 | GET | `/animais` | Lista animais paginados | 200 |
| 7 | GET | `/animais/{id}` | Busca animal por UUID | 200 |
| 8 | POST | `/animais` | Cadastra animal | 201 |
| 9 | PUT | `/animais/{id}` | Atualiza animal | 200 |
| 10 | DELETE | `/animais/{id}` | Remove animal | 204 |
| 11 | GET | `/clinicas` | Lista clínicas paginadas | 200 |
| 12 | GET | `/clinicas/{id}` | Busca clínica por UUID | 200 |
| 13 | POST | `/clinicas` | Cadastra clínica | 201 |
| 14 | PUT | `/clinicas/{id}` | Atualiza clínica | 200 |
| 15 | DELETE | `/clinicas/{id}` | Remove clínica | 204 |
| 16 | GET | `/veterinarios` | Lista veterinários paginados | 200 |
| 17 | GET | `/veterinarios/{id}` | Busca veterinário por UUID | 200 |
| 18 | POST | `/veterinarios` | Cadastra veterinário | 201 |
| 19 | PUT | `/veterinarios/{id}` | Atualiza veterinário | 200 |
| 20 | DELETE | `/veterinarios/{id}` | Remove veterinário | 204 |
| 21 | GET | `/eventos-clinicos` | Lista eventos paginados | 200 |
| 22 | GET | `/eventos-clinicos/{id}` | Busca evento por UUID | 200 |
| 23 | POST | `/eventos-clinicos` | Registra evento clínico | 201 |
| 24 | PUT | `/eventos-clinicos/{id}` | Atualiza evento | 200 |
| 25 | DELETE | `/eventos-clinicos/{id}` | Remove evento | 204 |
| 26 | GET | `/pagamentos` | Lista pagamentos paginados | 200 |
| 27 | GET | `/pagamentos/{id}` | Busca pagamento por UUID | 200 |
| 28 | POST | `/pagamentos` | Registra pagamento | 201 |
| 29 | PUT | `/pagamentos/{id}` | Atualiza pagamento | 200 |
| 30 | DELETE | `/pagamentos/{id}` | Remove pagamento | 204 |

### Endpoints de infraestrutura

| Rota | Descrição | Disponível em |
|---|---|---|
| `/swagger-ui.html` | Interface interativa do Swagger | todos os perfis |
| `/v3/api-docs` | Especificação OpenAPI 3 em JSON | todos os perfis |
| `/h2-console` | Console web do H2 | perfis `dev` e `h2` |

---

## Paginação, ordenação e filtros

Todos os GETs de listagem aceitam os parâmetros padrão do Spring Data:

| Parâmetro | Tipo | Default | Descrição |
|---|---|---|---|
| `page` | int | `0` | Página, base zero |
| `size` | int | `10` | Itens por página |
| `sort` | string | varia por recurso | `campo,asc` ou `campo,desc`; aceita múltiplos |

Além deles, cada recurso expõe dois filtros opcionais:

| Recurso | Filtro 1 | Filtro 2 | Tipo de comparação | Sort default |
|---|---|---|---|---|
| `/tutores` | `nome` | `cidade` | parcial, case-insensitive | `nome` |
| `/animais` | `nome` | `especie` | parcial, case-insensitive | `nome` |
| `/clinicas` | `nome` | `cidade` | parcial, case-insensitive | `nome` |
| `/veterinarios` | `nome` | `especialidade` | parcial, case-insensitive | `nome` |
| `/eventos-clinicos` | `tipoEvento` | `animalNome` | enum exato / parcial | `data` |
| `/pagamentos` | `statusPagamento` | `formaPagamento` | enum exato / enum exato | `dataPagamento` |

Regras de combinação:

- Filtro omitido é **ignorado** — não filtra nada
- Dois filtros preenchidos combinam com **AND**
- Filtros de texto usam `LIKE %valor%` com `LOWER()` nos dois lados
- `cidade` navega o endereço embutido (`endereco.cidade`)
- `animalNome` navega a associação (`animal.nome`)
- Filtro de enum com valor inválido devolve **400**

### Formato da resposta paginada

```json
{
  "content": [ /* array de Response */ ],
  "pageable": { "pageNumber": 0, "pageSize": 10, "sort": { "sorted": true } },
  "totalPages": 3,
  "totalElements": 27,
  "last": false,
  "first": true,
  "numberOfElements": 10,
  "size": 10,
  "number": 0,
  "empty": false
}
```

### Exemplos

```http
GET /animais?page=0&size=5&sort=nome,asc
GET /tutores?nome=Lucas&cidade=Sao Paulo
GET /veterinarios?especialidade=Cardiologia&sort=nome,desc
GET /eventos-clinicos?tipoEvento=VACINA&animalNome=Bolinha
GET /pagamentos?statusPagamento=PENDENTE
GET /pagamentos?formaPagamento=PIX&sort=dataPagamento,desc
```

---

## Ordem de criação

Cada nível depende do UUID do anterior. Tentar criar fora de ordem devolve 404.

| Passo | Recurso | Precisa de |
|---|---|---|
| 1 | `POST /tutores` · `POST /clinicas` | nada |
| 2 | `POST /animais` | `tutorId` |
| 2 | `POST /veterinarios` | `clinicaId` |
| 3 | `POST /eventos-clinicos` | `veterinarioId` + `animalId` + `clinicaId` |
| 4 | `POST /pagamentos` | `eventoClinicoId` |

---

## Tutores — `/tutores`

[`TutorController.java`](../src/main/java/br/com/fiap/clyvovet/controller/TutorController.java)

### Request

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| `nome` | string | sim | 3–100 caracteres |
| `cpf` | string | sim | exatamente 11 caracteres |
| `email` | string | sim | formato de e-mail |
| `telefone` | string | sim | 10–11 caracteres |
| `sexo` | enum | sim | `MASCULINO` `FEMININO` `OUTRO` |
| `dataNascimento` | date | sim | `yyyy-MM-dd` |
| `endereco` | objeto | sim | validado em cascata |

### Response

`id`, `nome`, `email`, `telefone`, `sexo` *(string)*, `dataNascimento`, `cpf`, `endereco`

### Exemplo

```http
POST /tutores
Content-Type: application/json

{
  "nome": "João Silva",
  "cpf": "12345678989",
  "email": "silva@email.com",
  "telefone": "11999999990",
  "sexo": "MASCULINO",
  "dataNascimento": "1990-01-15",
  "endereco": {
    "logradouro": "Av. Paulista",
    "numero": "1000",
    "bairro": "Bela Vista",
    "cidade": "São Paulo",
    "estado": "SP",
    "cep": "01310100"
  }
}
```

---

## Animais — `/animais`

[`AnimalController.java`](../src/main/java/br/com/fiap/clyvovet/controller/AnimalController.java)

### Request

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| `nome` | string | sim | 3–100 caracteres |
| `raca` | string | sim | 3–100 caracteres |
| `especie` | string | sim | 3–100 caracteres, texto livre |
| `porte` | string | sim | 3–100 caracteres |
| `cor` | string | sim | 3–100 caracteres |
| `sexo` | enum | sim | `MACHO` `FEMEA` `DESCONHECIDO` |
| `dataNascimento` | date | sim | `yyyy-MM-dd` |
| `observacao` | string | não | sem limite declarado |
| `tutorId` | UUID | sim | deve existir → senão 404 |

### Response

`id`, `nome`, `raca`, `especie`, `porte`, `cor`, `sexo`, `dataNascimento`,
`observacao`, `tutorId`, `tutorNome`

O `tutorNome` vem desnormalizado para poupar uma segunda chamada.

### Exemplo

```http
POST /animais
Content-Type: application/json

{
  "nome": "Thor",
  "raca": "Golden Retriever",
  "especie": "CACHORRO",
  "porte": "GRANDE",
  "cor": "Dourado",
  "sexo": "MACHO",
  "dataNascimento": "2021-03-10",
  "observacao": "Animal saudável",
  "tutorId": "3f2504e0-4f89-11d3-9a0c-0305e82c3301"
}
```

---

## Clínicas — `/clinicas`

[`ClinicaController.java`](../src/main/java/br/com/fiap/clyvovet/controller/ClinicaController.java)

### Request

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| `nome` | string | sim | 3–100 caracteres |
| `cnpj` | string | sim | exatamente 14 caracteres |
| `telefone` | string | sim | 10–11 caracteres |
| `email` | string | sim | formato de e-mail, 10–100 caracteres |
| `endereco` | objeto | sim | validado em cascata |

### Response

`id`, `nome`, `cnpj`, `telefone`, `email`, `endereco`

### Exemplo

```http
POST /clinicas
Content-Type: application/json

{
  "nome": "VetCare Prime",
  "cnpj": "12345678000191",
  "telefone": "1131000001",
  "email": "contato@vetcareprime.com.br",
  "endereco": {
    "logradouro": "Av. Paulista",
    "numero": "1000",
    "bairro": "Bela Vista",
    "cidade": "São Paulo",
    "estado": "SP",
    "cep": "01310100"
  }
}
```

---

## Veterinários — `/veterinarios`

[`VeterinarioController.java`](../src/main/java/br/com/fiap/clyvovet/controller/VeterinarioController.java)

### Request

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| `cpf` | string | sim | exatamente 11 caracteres |
| `nome` | string | sim | 3–100 caracteres |
| `dataNascimento` | date | sim | `yyyy-MM-dd` |
| `sexo` | enum | sim | `MASCULINO` `FEMININO` `OUTRO` |
| `email` | string | sim | formato de e-mail, 10–100 caracteres |
| `telefone` | string | sim | 10–11 caracteres |
| `endereco` | objeto | sim | validado em cascata |
| `especialidade` | string | sim | 3–100 caracteres |
| `crmv` | string | sim | 4–6 caracteres |
| `clinicaId` | UUID | sim | deve existir → senão 404 |

### Response

`id`, `nome`, `cpf`, `telefone`, `email`, `crmv`, `especialidade`, `dataNascimento`,
`sexo`, `endereco`, `clinicaId`, `clinicaNome`

### Exemplo

```http
POST /veterinarios
Content-Type: application/json

{
  "cpf": "11122233344",
  "nome": "Camila Ferreira",
  "dataNascimento": "1985-03-15",
  "sexo": "FEMININO",
  "email": "camila.ferreira@vetcare.com.br",
  "telefone": "11990010001",
  "especialidade": "Clinica Geral",
  "crmv": "14320",
  "clinicaId": "3f2504e0-4f89-11d3-9a0c-0305e82c3301",
  "endereco": {
    "logradouro": "Av. Paulista",
    "numero": "1500",
    "bairro": "Bela Vista",
    "cidade": "São Paulo",
    "estado": "SP",
    "cep": "01310200"
  }
}
```

> O limite de 4–6 caracteres em `crmv` impede o formato `CRMV-SP 14320` usado no seed
> do banco. Ver [07-pendencias-e-divergencias.md](07-pendencias-e-divergencias.md).

---

## Eventos Clínicos — `/eventos-clinicos`

[`EventoClinicoController.java`](../src/main/java/br/com/fiap/clyvovet/controller/EventoClinicoController.java)

### Request

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| `data` | date | sim | `yyyy-MM-dd` |
| `hora` | string | sim | texto livre; convenção `"HH:mm"` |
| `descricao` | string | não | — |
| `tipoEvento` | enum | sim | `CONSULTA` `RETORNO` `VACINA` `EXAME` `CIRURGIA` `OUTRO` |
| `veterinarioId` | UUID | sim | deve existir → senão 404 |
| `animalId` | UUID | sim | deve existir → senão 404 |
| `clinicaId` | UUID | sim | deve existir → senão 404 |

As três FKs são resolvidas **antes** do save, cada uma com sua mensagem de 404.

### Response

`id`, `data`, `hora`, `descricao`, `tipoEvento`, `veterinarioId`, `veterinarioNome`,
`animalId`, `animalNome`, `clinicaId`, `clinicaNome`

### Exemplo

```http
POST /eventos-clinicos
Content-Type: application/json

{
  "data": "2025-05-20",
  "hora": "10:00",
  "descricao": "Consulta de rotina",
  "tipoEvento": "CONSULTA",
  "veterinarioId": "3f2504e0-4f89-11d3-9a0c-0305e82c3301",
  "animalId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "clinicaId": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

## Pagamentos — `/pagamentos`

[`PagamentoController.java`](../src/main/java/br/com/fiap/clyvovet/controller/PagamentoController.java)

É o recurso com validação mais rígida e o único cujas mensagens de erro são
customizadas em português.

### Request

| Campo | Tipo | Obrigatório | Validação | Mensagem de erro |
|---|---|---|---|---|
| `formaPagamento` | enum | sim | `PIX` `CARTAO` `DINHEIRO` `BOLETO` | "Forma de pagamento é obrigatória" |
| `valor` | decimal | sim | positivo, máx. 9 inteiros + 2 decimais | "Valor deve ser positivo" |
| `dataPagamento` | date | sim | não pode ser futura | "Data de pagamento não pode ser futura" |
| `descricao` | string | não | máx. 255 caracteres | "Descrição deve ter no máximo 255 caracteres" |
| `observacao` | string | não | máx. 500 caracteres | "Observação deve ter no máximo 500 caracteres" |
| `eventoClinicoId` | UUID | sim | deve existir → senão 404 | "ID do evento clínico é obrigatório" |
| `statusPagamento` | enum | sim | `PENDENTE` `PAGO` `CANCELADO` `REEMBOLSADO` | "Status de pagamento é obrigatório" |

### Response

`id`, `formaPagamento`, `valor`, `dataPagamento`, `descricao`, `observacao`,
`eventoClinicoId`, `statusPagamento`

### Exemplo

```http
POST /pagamentos
Content-Type: application/json

{
  "formaPagamento": "PIX",
  "valor": 150.00,
  "dataPagamento": "2025-05-20",
  "descricao": "Pagamento consulta de rotina",
  "statusPagamento": "PAGO",
  "eventoClinicoId": "550e8400-e29b-41d4-a716-446655440000"
}
```

> Duas ressalvas registradas em
> [07-pendencias-e-divergencias.md](07-pendencias-e-divergencias.md):
> `dataPagamento` é obrigatória mesmo para status `PENDENTE`, e `REEMBOLSADO` é
> rejeitado pelo check constraint do Oracle.

---

## Objeto Endereco

Usado dentro de Tutor, Clínica e Veterinário. Validado em cascata via `@Valid`.

### Request

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| `logradouro` | string | sim | 3–100 caracteres |
| `numero` | string | sim | não vazio |
| `bairro` | string | sim | 3–100 caracteres |
| `cidade` | string | sim | 3–100 caracteres |
| `estado` | string | sim | exatamente 2 caracteres |
| `cep` | string | sim | exatamente 8 caracteres |
| `complemento` | string | não | — |

### Response

`logradouro`, `numero`, `complemento`, `bairro`, `cidade`, `estado`, `cep`

Note que a ordem dos campos difere entre Request e Response: `complemento` aparece em
terceiro no Response e por último no Request.

---

## Semântica das operações

| Operação | Comportamento |
|---|---|
| `POST` | Cria sempre um registro novo. Não é idempotente |
| `PUT` | **Substituição total**, não patch. Campos omitidos do body viram nulos |
| `DELETE` | Verifica existência com `existsById` antes de apagar; devolve 404 se não achar |
| `GET /{id}` | Vai sempre ao banco — não é cacheado |
| `GET` lista | Cacheado em memória; ver [01-arquitetura.md](01-arquitetura.md#cache) |

Não existe `PATCH` em nenhum recurso. Também não há operações em lote, soft delete ou
verificação de dependências antes de excluir — apagar um Tutor que tem Animais
vinculados falha por violação de FK no banco, resultando em 500.

---

## Códigos de erro

| Status | Quando ocorre | Formato do corpo |
|---|---|---|
| **400** | Bean Validation falhou | `[{"campo": "...", "mensagem": "..."}]` |
| **400** | Enum inválido em query param ou body | formato padrão do Spring Boot |
| **400** | UUID malformado no path | formato padrão do Spring Boot |
| **404** | ID do próprio recurso não existe | `{"campo": "id", "mensagem": "..."}` |
| **404** | FK referenciada não existe | `{"campo": "id", "mensagem": "..."}` |
| **401** | Sem token, token inválido ou expirado | `{"campo": "autenticacao", "mensagem": "..."}` |
| **401** | Credenciais inválidas no login | `{"campo": "credenciais", "mensagem": "Credenciais invalidas"}` |
| **403** | Autenticado, mas sem permissão para o recurso | `{"campo": "autorizacao", "mensagem": "..."}` |
| **409** | CPF, CNPJ, CRMV ou e-mail duplicado | `{"campo": "registro", "mensagem": "..."}` |
| **409** | Regra de negócio violada | `{"campo": "<campo>", "mensagem": "..."}` |
| **429** | Rate limit excedido | `{"campo": "requisicoes", "mensagem": "..."}` + `Retry-After` |
| **500** | Violação de constraint não prevista (FK em uso) | formato padrão do Spring Boot |

### Erro de validação — 400

```json
[
  { "campo": "nome",  "mensagem": "tamanho deve ser entre 3 e 100" },
  { "campo": "email", "mensagem": "deve ser um endereço de e-mail bem formado" }
]
```

### Recurso não encontrado — 404

```json
{ "campo": "id", "mensagem": "Animal não encontrado com ID: 3f2504e0-4f89-11d3-9a0c-0305e82c3301" }
```

### Mensagens de 404 por entidade

| Entidade | Mensagem |
|---|---|
| Tutor | `Tutor não encontrado com ID: {id}` |
| Animal | `Animal não encontrado com ID: {id}` |
| Clinica | `Clínica não encontrada com ID: {id}` |
| Veterinario | `Veterinário não encontrado com ID: {id}` |
| EventoClinico | `Evento clínico não encontrado com ID: {id}` |
| Pagamento | `Pagamento não encontrado com ID: {id}` |
| EventoClinico *(como FK em Pagamento)* | `EventoClinico não encontrado com ID: {id}` |
