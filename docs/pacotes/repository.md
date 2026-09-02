# `repository` — o acesso ao banco

`src/main/java/br/com/fiap/clyvovet/repository` · 13 interfaces + 1 base

São interfaces do Spring Data JPA. Ninguém escreve implementação: o framework
gera a classe em tempo de execução, derivando a consulta do **nome do método**
ou usando o JPQL declarado em `@Query`.

---

## `RepositorioBase` — a base de todos

Toda interface daqui estende `RepositorioBase<T>`, não `JpaRepository<T, UUID>`
direto. Ela acrescenta as duas buscas que sempre vinham acompanhadas de um
"senão, 404":

```java
T obterPorId(UUID id, Recurso recurso);      // devolve, ou lança 404
void garantirQueExiste(UUID id, Recurso recurso);  // só verifica
```

Antes, cada service repetia
`findById(id).orElseThrow(() -> new EntityNotFoundException("... " + id))` — o
mesmo trecho em cerca de vinte lugares, cada um livre para escrever a mensagem
do seu jeito. Com a decisão aqui, o service volta a expressar só a intenção:

```java
Animal animal = animalRepository.obterPorId(id);
```

Os métodos são `default` **de propósito**. O Spring Data deriva consultas a
partir do nome de métodos *abstratos*; um método com corpo ele simplesmente
respeita. É o que permite estender o repositório sem infraestrutura extra.

Cada interface concreta então expõe o atalho já com o `Recurso` preenchido:

```java
default Animal obterPorId(UUID id) {
    return obterPorId(id, Recurso.ANIMAL);
}
```

---

## Os arquivos

### Os seis recursos de CRUD

| Arquivo | Consultas próprias |
|---|---|
| `TutorRepository.java` | Busca paginada com filtro por `nome` e `cidade`; `existsByEmail`, que sustenta a recusa do auto-cadastro quando a clínica já cadastrou a pessoa |
| `AnimalRepository.java` | Busca paginada por `nome` e `especie`, **com o recorte por tutor**; `findByMicrochip` para a identificação no balcão |
| `ClinicaRepository.java` | Busca paginada por `nome` e `cidade` |
| `VeterinarioRepository.java` | Busca paginada por `nome` e `especialidade`; `daClinica`, que sustenta a busca de vagas |
| `EventoClinicoRepository.java` | O maior do pacote — oito consultas que sustentam agenda, retorno e inadimplência (abaixo) |
| `PagamentoRepository.java` | Busca paginada por status e forma; a soma por evento e o extrato do tutor, ambos recortados por clínica |

### O fluxo clínico e a agenda

| Arquivo | O que responde |
|---|---|
| `DisponibilidadeVeterinarioRepository.java` | As faixas do veterinário naquele dia da semana **que estão vigentes na data**. O filtro de vigência entra na consulta, e não em memória, porque a grade acumula versões |
| `BloqueioRepository.java` | Os bloqueios que *alcançam* a data — o intervalo cobre, não precisa começar nela |
| `ServicoRepository.java` | `ativosDaClinica`: o catálogo visível ao tutor, só o que a clínica realmente oferece hoje |
| `AlertaClinicoRepository.java` | Os alertas ativos de um animal, ordenados por tipo — o que entra no resumo de segurança |
| `AutorizacaoAcessoRepository.java` | A autorização de uma clínica sobre um animal, **independente do status**; e a lista que o tutor vê |
| `AcessoHistoricoRepository.java` | O registro de auditoria e as contagens que sustentam os tetos de leitura |
| `UsuarioRepository.java` | `findByEmail` e `existsByEmail` |

---

## As consultas que carregam regra

### `EventoClinicoRepository.retornosVencidos` — o `NOT EXISTS` é o coração

```sql
... AND e.dataRetornoPrevisto < :hoje
    AND NOT EXISTS (SELECT r FROM EventoClinico r WHERE r.eventoOrigem = e ...)
```

Não basta a data ter passado: é preciso que **não exista** um retorno apontando
para este evento. Sem essa parte, o pet que voltou continuaria na lista de
atrasados, e a lista deixaria de servir para o que existe — virar ligação.

### `ocupandoAAgenda` — cancelado e faltou ficam de fora

O horário de quem cancelou volta a estar livre. Se `CANCELADO` e `FALTOU`
entrassem na consulta de ocupação, um único cancelamento bloquearia aquele slot
para sempre.

### `countByUsuarioIdAndDiaAndEmergencial` — conta linhas, não somas

A tabela de auditoria guarda **uma linha por (usuário, animal, dia)** com um
contador. A contagem do teto olha as linhas, e é essa escolha que faz o limite
medir a coisa certa:

- o veterinário que reabre o mesmo prontuário quarenta vezes durante uma
  cirurgia conta **1**;
- o que consulta duzentos animais diferentes numa tarde conta **200** — e não
  está atendendo nenhum deles.

### O recorte de acesso mora na consulta

Vários métodos recebem `tutorId` e `clinicaId`. **Não são filtros de busca
expostos ao cliente** — são o recorte de segurança, preenchido pelo service com
o que o [`RecorteDeAcesso`](security.md) do usuário logado disser.

```sql
AND (:clinicaId IS NULL OR p.eventoClinico.clinica.id = :clinicaId)
```

Nulo significa "sem recorte nesta dimensão", e é o que o ADMIN recebe.

Aplicar o recorte **na query, e não depois dela**, é o que mantém a paginação
correta: filtrar em memória depois de paginar devolveria páginas de tamanhos
irregulares e um `totalElements` mentiroso.

Foi a ausência desse recorte em `doTutorNoPeriodo` e `realizadosAte` que entregou
o extrato financeiro de um tutor em clínicas concorrentes e a carteira de
inadimplentes da plataforma inteira, com nome e telefone.

---

## A armadilha do `ESCAPE`

Em todas as buscas por texto:

```java
@Query("SELECT a FROM Animal a WHERE " +
       "(:nome IS NULL OR LOWER(a.nome) LIKE LOWER(CONCAT('%', :nome, '%')) ESCAPE '\\') ...")
```

**Não remova o `ESCAPE '\'`.** Sem ele o Hibernate emite `LIKE ... ESCAPE ''`, e
sob a semântica do Oracle (que o H2 imita com `MODE=Oracle`) string vazia *é*
nulo. O predicado vira `ESCAPE NULL`, avalia como desconhecido e **nunca casa** —
todos os filtros por texto da API devolviam lista vazia por causa disso.

---

## Onde continuar

| Assunto | Documento |
|---|---|
| As entidades que estas consultas carregam | [model.md](model.md) |
| Quem preenche `tutorId` e `clinicaId` | [security.md](security.md) |
| O `Recurso` das mensagens de 404 | [exception.md](exception.md) |
| O DDL e os índices | [../02-modelo-de-dados.md](../02-modelo-de-dados.md) |
