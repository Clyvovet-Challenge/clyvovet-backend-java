# `security` — quem é você, e o que você alcança

`src/main/java/br/com/fiap/clyvovet/security` · 10 classes

Duas perguntas diferentes vivem aqui, e a distinção organiza o pacote:

- **Autenticação** — *quem* é você? Resolvida pelo token JWT, antes de qualquer
  controller.
- **Autorização** — você pode **este registro**? Resolvida por perfil (regra de
  rota, em [`SecurityConfig`](config.md)) e por *ownership* (`SegurancaService`).

Perfil sozinho não basta. `hasRole('TUTOR')` diz que você é um tutor; não diz que
*este* pet é seu.

---

## Os arquivos

### Autenticação

| Arquivo | O que faz |
|---|---|
| `JwtService.java` | Gera e valida os tokens. Dois tipos, diferenciados pela claim `tipo`: **access** (15 min, autoriza as chamadas) e **refresh** (7 dias, só serve para obter um novo access) |
| `JwtAuthenticationFilter.java` | Lê o header `Authorization`, valida o access token e popula o `SecurityContext` |
| `UsuarioDetailsService.java` | Carrega o usuário pelo id vindo do *subject* do token |
| `UsuarioAutenticado.java` | Adaptador entre a entidade `Usuario` e o contrato `UserDetails` do Spring Security |
| `RevogacaoTokenService.java` | *Deny-list* de refresh tokens revogados, guardada pelo claim `jti` |

A separação access/refresh limita a janela de uso de um access token vazado sem
obrigar o usuário a refazer login a cada 15 minutos.

O `JwtAuthenticationFilter` **não decide sobre autorização nem devolve erro**:
token ausente ou inválido simplesmente deixa o contexto vazio, e a cadeia do
Spring Security responde 401 ou 403 conforme a rota. É o que mantém uma única
fonte de decisão.

### Autorização

| Arquivo | O que faz |
|---|---|
| `SegurancaService.java` | As decisões de *ownership*. Registrado como bean `seguranca` para uso em SpEL |
| `RecorteDeAcesso.java` | Um `record (tutorId, clinicaId)`: o que este usuário enxerga nas listagens, resolvido de uma vez |

### Defesa

| Arquivo | O que faz |
|---|---|
| `RateLimitFilter.java` | Rate limiting **por IP**, com Bucket4j — o ataque volumétrico |
| `ControleTentativasLogin.java` | Bloqueio **por conta**: 5 falhas seguidas = 15 minutos travado |
| `RespostaErroSeguranca.java` | Traduz as falhas do Spring Security para o `ErroValidacao` do resto da API, em vez da página HTML padrão |

---

## `SegurancaService` — o bean `seguranca`

É consultado de dois lugares: pelas anotações `@PreAuthorize` (acesso por id) e
pelos services (o filtro das listagens).

```java
@PreAuthorize("@seguranca.podeAcessarAnimal(#id)")
```

| Método | Responde |
|---|---|
| `podeAcessarTutor(id)` · `podeAcessarAnimal(id)` · `podeAcessarEvento(id)` · `podeAcessarPagamento(id)` | "este registro é alcançável por quem está logado?" |
| `ehDonoOuAdministrador(animalId)` | Só o tutor dono e o ADMIN — **sem** o veterinário |
| `podeAtribuirTutor(id)` | Autoriza a atribuição de dono vinda de um PATCH |
| `podeGerenciarAgendaDe(veterinarioId)` | "esta é a sua própria agenda?" |
| `ehAdministradorDaPlataforma()` | O ADMIN |
| `recorte()` · `tutorIdParaFiltro()` · `clinicaParaFiltro()` | O recorte das listagens |
| `usuarioAutenticadoId()` · `clinicaDoUsuario()` | Identidade, para os registros de auditoria |

Duas notas de uso real que valem lembrar:

**`ehDonoOuAdministrador` não é `podeAcessarAnimal`.** A auditoria de acessos
(`GET /animais/{id}/acessos`) usa a primeira. A segunda passa por
`temVisaoAmpla` e liberaria **todo veterinário** a ler a auditoria de qualquer
animal, expondo o e-mail dos profissionais de outras clínicas e revelando quais
delas atenderam aquele paciente.

**No `POST /pagamentos` o dono está no corpo, não na rota.** Por isso a anotação
é `@seguranca.podeAcessarEvento(#request.eventoClinicoId)`.

---

## `RecorteDeAcesso` — a assimetria que quase vazou dados

```java
public record RecorteDeAcesso(UUID tutorId, UUID clinicaId) {
    static RecorteDeAcesso irrestrito();     // ADMIN
    static RecorteDeAcesso doTutor(UUID);    // TUTOR
    static RecorteDeAcesso daClinica(UUID);  // VETERINARIO
    String chaveDeCache();
}
```

Nulo significa "sem recorte nesta dimensão": o TUTOR não tem clínica, o
VETERINARIO não tem tutor, e o ADMIN não tem nenhum dos dois.

### Por que as fábricas não repassam nulo

`POST /auth/usuarios` aceitava `{"perfil":"VETERINARIO"}` **sem**
`veterinarioId` — a validação de vínculo recusava o cruzado (um TUTOR apontando
para veterinário, e vice-versa), nunca o ausente. O usuário nascia válido e sem
lastro.

O recorte desse usuário era `(null, null)`, que é exatamente o do ADMIN. O
cadastro incompleto não tirava acesso: **dava**. Um "veterinário" sem clínica
lia o atendimento de toda a plataforma e — porque a guarda de clínica própria
também compara com nulo — registrava atendimento em qualquer clínica.

Hoje a falta de vínculo vira um id que nenhuma linha carrega, e a mesma
cláusula `IS NULL OR` devolve página vazia. Falha fechada, e sem exceção: o
método também é chamado de dentro da chave de cache, em SpEL, onde uma exceção
viraria 500 em vez de 403.

### As duas metades são independentes

`UsuarioService.validarVinculo` passou a exigir o vínculo, e não só a recusar o
cruzado: TUTOR precisa de tutor, VETERINARIO precisa de veterinário, e o ADMIN
não precisa de nenhum dos dois (nele o vínculo é inerte — o recorte do ADMIN é
irrestrito de qualquer forma).

As quatro checagens não são intercambiáveis, e a ordem importa para quem lê o
teste: a do vínculo **ausente** roda antes, então um pedido que manda só o id
errado (`perfil=VETERINARIO` com `tutorId`) é recusado por falta de
veterinário, e não por cruzamento. Os dois casos devolvem 409, e só o `campo`
da resposta os separa — por isso `vinculoCruzadoRecusado` manda **os dois ids**
e confere o campo. Conferir só o status deixava a checagem do cruzado
descoberta: apagando as duas linhas do serviço, o teste continuava verde.

Mas a validação cobre só a porta da frente. A linha pode existir vinda de uma
migration, de uma correção manual no banco ou de um bug futuro, e é por isso que
o recorte continua contendo o caso por conta própria. Quem testa cada metade
sabe disso: `CicloDeSessaoTest` bate na rota, e `RecorteSemVinculoTest` grava a
linha **direto no repositório**, justamente porque a porta da frente já não a
produz.

Antes isto não era um objeto, e sim **três coisas escritas à mão** em cada
recurso: uma string SpEL na chave do cache, um par de argumentos na chamada do
repositório e um par de cláusulas `IS NULL OR` na consulta. Manter as três em
acordo era responsabilidade de quem lembrasse.

A parte importante é o `chaveDeCache()`, que inclui **sempre as duas dimensões**,
mesmo onde a consulta filtra por apenas uma. A assimetria é o motivo:

> Chave **mais larga** que o filtro só custa entradas a mais no cache.
> Chave **mais estreita** que o filtro **entrega a página de um usuário a outro**.

Foi assim que uma das falhas quase passou — o recorte existia na consulta e
faltava na chave. Com a chave saindo daqui, ela não tem como ficar mais estreita
que o recorte.

---

## Os dois limites são dois ataques diferentes

| | `RateLimitFilter` | `ControleTentativasLogin` |
|---|---|---|
| Conta o quê | Requisições por **IP** | Falhas por **conta** |
| Contra o quê | Rajada volumétrica | Força bruta direcionada |
| Onde vive | Filtro, antes da cadeia | Bean transacional |

A divisão importa. Um limite curto por IP no login (algo como 5 a cada 15 min)
parece mais seguro, mas não é: contra força bruta agrega pouco, já que o
bloqueio por conta barra o ataque, e **quebra uso legítimo** — uma clínica
inteira atrás de um mesmo IP público compartilha o balde e trava depois de
poucos logins.

### Por que `ControleTentativasLogin` é um bean próprio

O registro da falha precisa ser **commitado mesmo quando o login termina em
exceção**. Se o incremento acontecesse na mesma transação do login, o rollback
disparado pelo `BadCredentialsException` desfaria a contagem e o bloqueio nunca
chegaria a valer — o contador voltaria a zero a cada tentativa.

Daí o `Propagation.REQUIRES_NEW`. E como o Spring aplica `@Transactional` via
proxy, isso só funciona **a partir de outro bean**: se estes métodos fossem
privados do `AuthService`, a anotação seria ignorada em silêncio.

### Por que os buckets vivem num Caffeine com expiração

Sem expiração, cada IP visto criaria uma entrada permanente e a memória cresceria
sem limite — o próprio rate limiter viraria um vetor de DoS.

---

## Duas limitações conhecidas

`RateLimitFilter` e `RevogacaoTokenService` guardam estado **local ao processo**.
Com mais de uma réplica:

- cada instância teria sua própria contagem de rate limit;
- um logout numa instância não revogaria o token nas demais.

A correção nos dois casos é a mesma: mover o estado para um cache compartilhado
(Redis; para o rate limit, `bucket4j-redis`). Está registrado como pendência, não
como surpresa.

---

## As mensagens genéricas são de propósito

O `RespostaErroSeguranca` devolve texto deliberadamente vago. Detalhar qual regra
barrou a requisição ajudaria a mapear a superfície de autorização.

Pelo mesmo motivo, o `AuthService` usa **uma mensagem única** para credencial
errada, e-mail inexistente e conta bloqueada: mensagens distintas permitiriam
descobrir quais e-mails existem na base apenas observando a resposta —
enumeração de usuários.

---

## Onde continuar

| Assunto | Documento |
|---|---|
| A cadeia de filtros e as regras de rota | [config.md](config.md) |
| A matriz de autorização completa | [../08-seguranca.md](../08-seguranca.md) |
| Onde o recorte entra na consulta | [repository.md](repository.md) |
| A guarda que exige `@PreAuthorize` | [controller.md](controller.md) |
