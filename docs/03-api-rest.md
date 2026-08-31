# API REST

Base URL local: `http://localhost:8080` — não há context-path configurado.
Todos os endpoints consomem e produzem `application/json`.

**Todos os endpoints exigem autenticação**, exceto `/api/v1/auth/login`, `/api/v1/auth/refresh`,
`/api/v1/auth/registrar` e o Swagger. Envie o access token no header:

```
Authorization: Bearer <access-token>
```

Obtenha-o em `POST /api/v1/auth/login`. Os cinco endpoints de `/api/v1/auth`, a matriz de
autorização por perfil e as regras de ownership estão em
[08-seguranca.md](08-seguranca.md).

---

## Índice de endpoints

Trinta e seis rotas, seis por entidade, todas no mesmo padrão CRUD. Todas ficam
sob o prefixo **`/api/v1`** — ver [Versionamento](#versionamento) no fim deste
documento. A numeração da coluna `#` segue a ordem original de cinco rotas; os
PATCH entraram depois e por isso aparecem sem número.

| # | Verbo | Rota | Descrição | Status de sucesso |
|---|---|---|---|---|
| 1 | GET | `/api/v1/tutores` | Lista tutores paginados | 200 |
| 2 | GET | `/api/v1/tutores/{id}` | Busca tutor por UUID | 200 |
| 3 | POST | `/api/v1/tutores` | Cadastra tutor | 201 |
| 4 | PUT | `/api/v1/tutores/{id}` | Atualiza tutor | 200 |
| — | PATCH | `/api/v1/tutores/{id}` | Atualiza parcialmente tutor | 200 |
| 5 | DELETE | `/api/v1/tutores/{id}` | Remove tutor | 204 |
| 6 | GET | `/api/v1/animais` | Lista animais paginados | 200 |
| 7 | GET | `/api/v1/animais/{id}` | Busca animal por UUID | 200 |
| 8 | POST | `/api/v1/animais` | Cadastra animal | 201 |
| 9 | PUT | `/api/v1/animais/{id}` | Atualiza animal | 200 |
| — | PATCH | `/api/v1/animais/{id}` | Atualiza parcialmente animal | 200 |
| 10 | DELETE | `/api/v1/animais/{id}` | Remove animal | 204 |
| 11 | GET | `/api/v1/clinicas` | Lista clínicas paginadas | 200 |
| 12 | GET | `/api/v1/clinicas/{id}` | Busca clínica por UUID | 200 |
| 13 | POST | `/api/v1/clinicas` | Cadastra clínica | 201 |
| 14 | PUT | `/api/v1/clinicas/{id}` | Atualiza clínica | 200 |
| — | PATCH | `/api/v1/clinicas/{id}` | Atualiza parcialmente clínica | 200 |
| 15 | DELETE | `/api/v1/clinicas/{id}` | Remove clínica | 204 |
| 16 | GET | `/api/v1/veterinarios` | Lista veterinários paginados | 200 |
| 17 | GET | `/api/v1/veterinarios/{id}` | Busca veterinário por UUID | 200 |
| 18 | POST | `/api/v1/veterinarios` | Cadastra veterinário | 201 |
| 19 | PUT | `/api/v1/veterinarios/{id}` | Atualiza veterinário | 200 |
| — | PATCH | `/api/v1/veterinarios/{id}` | Atualiza parcialmente veterinário | 200 |
| 20 | DELETE | `/api/v1/veterinarios/{id}` | Remove veterinário | 204 |
| 21 | GET | `/api/v1/eventos-clinicos` | Lista eventos paginados | 200 |
| 22 | GET | `/api/v1/eventos-clinicos/{id}` | Busca evento por UUID | 200 |
| 23 | POST | `/api/v1/eventos-clinicos` | Registra evento clínico | 201 |
| 24 | PUT | `/api/v1/eventos-clinicos/{id}` | Atualiza evento | 200 |
| — | PATCH | `/api/v1/eventos-clinicos/{id}` | Atualiza parcialmente evento | 200 |
| 25 | DELETE | `/api/v1/eventos-clinicos/{id}` | Remove evento | 204 |
| 26 | GET | `/api/v1/pagamentos` | Lista pagamentos paginados | 200 |
| 27 | GET | `/api/v1/pagamentos/{id}` | Busca pagamento por UUID | 200 |
| 28 | POST | `/api/v1/pagamentos` | Registra pagamento | 201 |
| 29 | PUT | `/api/v1/pagamentos/{id}` | Atualiza pagamento | 200 |
| — | PATCH | `/api/v1/pagamentos/{id}` | Atualiza parcialmente pagamento | 200 |
| 30 | DELETE | `/api/v1/pagamentos/{id}` | Remove pagamento | 204 |

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
| `/api/v1/tutores` | `nome` | `cidade` | parcial, case-insensitive | `nome` |
| `/api/v1/animais` | `nome` | `especie` | parcial, case-insensitive | `nome` |
| `/api/v1/clinicas` | `nome` | `cidade` | parcial, case-insensitive | `nome` |
| `/api/v1/veterinarios` | `nome` | `especialidade` | parcial, case-insensitive | `nome` |
| `/api/v1/eventos-clinicos` | `tipoEvento` | `animalNome` | enum exato / parcial | `data` |
| `/api/v1/pagamentos` | `statusPagamento` | `formaPagamento` | enum exato / enum exato | `dataPagamento` |

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
  "page": {
    "size": 10,
    "number": 0,
    "totalElements": 27,
    "totalPages": 3
  }
}
```

> ⚠️ **Este formato mudou.** Antes a resposta era o `PageImpl` do Spring serializado
> como estava: mais de vinte campos (`pageable`, `last`, `first`, `numberOfElements`,
> `empty`…) espalhados na raiz. O próprio Spring avisava no boot que *"there is no
> guarantee about the stability of the resulting JSON structure"* — ou seja, um
> upgrade do framework podia mudar o contrato sem uma linha de código mudar.
>
> A aplicação passou a usar o `PagedModel` (`@EnableSpringDataWebSupport(VIA_DTO)` em
> `WebConfig`), que é o formato estável e suportado. Quem lia `totalElements` na raiz
> agora lê em `page.totalElements`.

### Exemplos

```http
GET /api/v1/animais?page=0&size=5&sort=nome,asc
GET /api/v1/tutores?nome=Lucas&cidade=Sao Paulo
GET /api/v1/veterinarios?especialidade=Cardiologia&sort=nome,desc
GET /api/v1/eventos-clinicos?tipoEvento=VACINA&animalNome=Bolinha
GET /api/v1/pagamentos?statusPagamento=PENDENTE
GET /api/v1/pagamentos?formaPagamento=PIX&sort=dataPagamento,desc
```

---

## Ordem de criação

Cada nível depende do UUID do anterior. Tentar criar fora de ordem devolve 404.

| Passo | Recurso | Precisa de |
|---|---|---|
| 1 | `POST /api/v1/tutores` · `POST /api/v1/clinicas` | nada |
| 2 | `POST /api/v1/animais` | `tutorId` |
| 2 | `POST /api/v1/veterinarios` | `clinicaId` |
| 3 | `POST /api/v1/eventos-clinicos` | `veterinarioId` + `animalId` + `clinicaId` |
| 4 | `POST /api/v1/pagamentos` | `eventoClinicoId` |

---

## Tutores — `/api/v1/tutores`

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
POST /api/v1/tutores
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

## Animais — `/api/v1/animais`

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
POST /api/v1/animais
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

## Clínicas — `/api/v1/clinicas`

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
POST /api/v1/clinicas
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

## Veterinários — `/api/v1/veterinarios`

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
POST /api/v1/veterinarios
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

## Eventos Clínicos — `/api/v1/eventos-clinicos`

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
POST /api/v1/eventos-clinicos
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

## Pagamentos — `/api/v1/pagamentos`

[`PagamentoController.java`](../src/main/java/br/com/fiap/clyvovet/controller/PagamentoController.java)

É o recurso com validação mais rígida e o único cujas mensagens de erro são
customizadas em português.

### Request

| Campo | Tipo | Obrigatório | Validação | Mensagem de erro |
|---|---|---|---|---|
| `formaPagamento` | enum | sim | `PIX` `CARTAO` `DINHEIRO` `BOLETO` | "Forma de pagamento é obrigatória" |
| `valor` | decimal | sim | positivo, máx. 9 inteiros + 2 decimais | "Valor deve ser positivo" |
| `dataPagamento` | date | **não** | não pode ser futura | "Data de pagamento não pode ser futura" |
| `descricao` | string | não | máx. 255 caracteres | "Descrição deve ter no máximo 255 caracteres" |
| `observacao` | string | não | máx. 500 caracteres | "Observação deve ter no máximo 500 caracteres" |
| `eventoClinicoId` | UUID | sim | deve existir → senão 404 | "ID do evento clínico é obrigatório" |
| ~~`statusPagamento`~~ | — | — | **saiu do corpo** — ver a nota abaixo | — |

### Response

`id`, `formaPagamento`, `valor`, `dataPagamento`, `descricao`, `observacao`,
`eventoClinicoId`, `statusPagamento`

### Exemplo

```http
POST /api/v1/pagamentos
Content-Type: application/json

{
  "formaPagamento": "PIX",
  "valor": 150.00,
  "descricao": "Pagamento consulta de rotina",
  "eventoClinicoId": "550e8400-e29b-41d4-a716-446655440000"
}
```

> **O status não vem do corpo** (regra P14 da
> [spec 08](../specs/08-modelo-de-negocio.md)). Todo pagamento nasce `PENDENTE`;
> as transições são ações próprias, e é nelas que as regras são verificadas:
>
> ```http
> POST /api/v1/pagamentos/{id}/confirmar   { formaPagamento, dataPagamento }
> POST /api/v1/pagamentos/{id}/estornar    { motivo }
> ```
>
> `PENDENTE → PAGO → REEMBOLSADO`. `CANCELADO` e `REEMBOLSADO` são terminais e
> nada volta para `PENDENTE`. Enquanto o campo esteve no corpo, um
> `{"statusPagamento":"PAGO"}` no POST ou no PUT contornava todas elas de uma
> vez — inclusive o teto da soma contra o preço do serviço.
>
> `dataPagamento` também deixou de ser obrigatória: ela entra na confirmação,
> porque um pagamento pendente não tem data de pagamento.

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

---

## Versionamento

Todos os endpoints da aplicação ficam sob **`/api/v1`**. O prefixo não aparece nos
`@RequestMapping` dos controllers — cada um declara só o próprio recurso
(`/tutores`) e o prefixo é aplicado de uma vez em
[`WebConfig`](../src/main/java/br/com/fiap/clyvovet/config/WebConfig.java):

```java
public static final String PREFIXO_API = "/api/v1";

configurer.addPathPrefix(PREFIXO_API,
        HandlerTypePredicate.forBasePackage("br.com.fiap.clyvovet.controller"));
```

Assim a versão vive num lugar só, e migrar para `/api/v2` é trocar uma constante.

**Por que por pacote e não por `@RestController`.** O springdoc também anota suas
classes com `@RestController`. Filtrando pela anotação, o `/v3/api-docs` ia junto
para `/api/v1/v3/api-docs` e o Swagger parava de abrir. Restrito ao pacote de
controllers da aplicação, as ferramentas seguem onde se espera:

| Rota | Prefixada? |
|---|---|
| `/api/v1/**` — endpoints da aplicação | sim |
| `/swagger-ui.html`, `/v3/api-docs` | não |
| `/h2-console` | não |

`SecurityConfig` monta seus matchers a partir da mesma constante, por um helper
`api(...)`. Se os dois lados divergissem, uma rota ficaria aberta em silêncio.

---

## Atualização parcial (PATCH)

`PUT` substitui o recurso inteiro: o corpo precisa trazer todos os campos
obrigatórios, e o que não vier é sobrescrito. `PATCH` altera **apenas os campos
enviados**.

```bash
# Muda só o telefone. Nome, CNPJ, e-mail e endereço ficam como estavam.
curl -X PATCH http://localhost:8080/api/v1/clinicas/{id} \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{"telefone":"1199998888"}'
```

### O que muda na validação

Cada recurso tem um `XxxPatchRequest` separado do `XxxRequest`. Ele mantém as
restrições de **formato** e abre mão das de **presença**:

| Restrição | `XxxRequest` (POST/PUT) | `XxxPatchRequest` (PATCH) |
|---|---|---|
| `@NotNull`, `@NotBlank` | sim | **não** |
| `@Size`, `@Email`, `@Pattern`, `@Positive` | sim | sim |

Ou seja: um campo enviado continua validado — e-mail inválido é **400**, não um erro
de servidor lá no banco — e um campo omitido simplesmente não entra na conta.

DTOs separados em vez de grupos de validação sobre o `Request` existente: com grupos,
seria preciso anotar campo a campo dos DTOs já em uso, e um grupo esquecido
enfraqueceria em silêncio a validação do POST — o erro mais caro dos dois.

### Limite conhecido

Campo ausente e campo enviado como `null` chegam iguais à aplicação, então **não há
como apagar um campo opcional via PATCH** — use `PUT` para isso. Distinguir os dois
exigiria `Optional` em cada atributo ou JSON Merge Patch (RFC 7386), complexidade que
nenhum caso de uso deste projeto pede.

### Ownership no PATCH

Num `PUT /api/v1/animais/{id}` o `tutorId` é obrigatório, e o `@PreAuthorize` checa
duas coisas: o pet é meu **e** o dono que estou gravando continua sendo eu. Num PATCH
o `tutorId` pode não vir, e aí a segunda pergunta não se aplica.

`SegurancaService.podeAtribuirTutor` cobre isso: `null` significa "não mexa no dono" e
é sempre permitido a quem já pode editar o animal. Sem essa distinção,
`podeAcessarTutor(null)` devolveria `false` e um tutor não conseguiria corrigir a cor
do próprio pet sem reenviar o próprio id no corpo.

Trocar o dono via PATCH continua barrado para tutor — coberto por
`AtualizacaoParcialTest.tutorNaoTransferePetViaPatch`.
