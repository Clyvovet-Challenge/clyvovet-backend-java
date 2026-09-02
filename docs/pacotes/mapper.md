# `mapper` — a conversão entre DTO e entidade

`src/main/java/br/com/fiap/clyvovet/mapper` · 9 mappers + 3 classes de apoio

A cópia campo a campo mora aqui, e em nenhum outro lugar. É trabalho chato e
repetitivo — e é exatamente por isso que ele merece um pacote: espalhado pelos
services, o campo esquecido não aparece em lugar nenhum.

A conversão é **manual**, sem MapStruct e sem ModelMapper. Num projeto deste
tamanho, o reflection de um mapeador automático troca um erro de compilação por
um campo que some em silêncio em tempo de execução.

---

## Os quatro métodos de um mapper

| Método | Quando é chamado | O que faz |
|---|---|---|
| `toEntity(request, ...)` | `POST` | Monta a entidade nova a partir do corpo |
| `atualizar(entidade, request, ...)` | `PUT` | Sobrescreve **todos** os campos |
| `aplicarPatch(entidade, patch, ...)` | `PATCH` | Sobrescreve **só os que vieram** |
| `toResponse(entidade)` | sempre | Monta o DTO de saída |

`toEntity` delega para `atualizar` sempre que os dois copiam os mesmos campos:

```java
public Animal toEntity(AnimalRequest request, Tutor tutor) {
    Animal animal = new Animal();
    atualizar(animal, request, tutor);
    return animal;
}
```

Assim a lista de campos existe **uma vez**. Antes, um campo novo precisava ser
lembrado em dois métodos, e no service também.

---

## Os arquivos

### Mappers

| Arquivo | Nota |
|---|---|
| `TutorMapper.java` | O endereço é delegado ao `EnderecoMapper` |
| `AnimalMapper.java` | Recebe o `Tutor` já resolvido — o mapper não vai ao banco |
| `ClinicaMapper.java` | Mesmo desenho do tutor |
| `VeterinarioMapper.java` | Recebe a `Clinica` já resolvida |
| `EventoClinicoMapper.java` | O mais denso: oito campos e cinco relacionamentos, agrupados num `RelacionamentosDoEvento` |
| `PagamentoMapper.java` | Recebe o `EventoClinico` já resolvido |
| `ServicoMapper.java` | O `atualizar` **não mexe em `clinica` nem em `ativo`**: serviço não muda de dono, e desativar é outra operação |
| `EnderecoMapper.java` | Converte o `@Embeddable`. Tem uma guarda contra nulo (abaixo) |
| `UsuarioMapper.java` | Só `toResponse`. A senha não aparece aqui — nem o hash |

O `UsuarioMapper` morava dentro do `AuthService`, único caso em que a conversão
não estava neste pacote. Fora do lugar, ninguém o encontrava para reaproveitar.
E um DTO de resposta que **nunca conhece** o campo senha é mais confiável do que
lembrar de omiti-lo caso a caso.

### Apoio

| Arquivo | O que é |
|---|---|
| `AtualizacaoParcial.java` | O `aplicarSePresente` usado por todo `aplicarPatch` |
| `Referencias.java` | Leitura de campo de entidade associada que pode ser nula |
| `RelacionamentosDoEvento.java` | Um `record` com as entidades que um evento referencia |

---

## As três classes de apoio, e o problema de cada uma

### `AtualizacaoParcial` — o PATCH em uma linha por campo

```java
aplicarSePresente(patch.getNome(),  tutor::setNome);
aplicarSePresente(patch.getEmail(), tutor::setEmail);
```

no lugar de um `if (x != null)` repetido dezenas de vezes — que é onde costuma
passar despercebido o campo que ninguém lembrou de copiar.

E passou. O `EventoClinicoMapper.aplicarPatch` não copiava `servico`: quem
corrigia um atendimento por PATCH via o serviço desaparecer da linha, e com ele
o preço — o atendimento saía da conta de inadimplência sem que nada acusasse. A
correção foi uma linha; o teste que a acompanha percorre os oito campos.

### `Referencias` — o relacionamento nulo

As respostas expõem o id e o nome do relacionamento (o tutor do animal, a
clínica do veterinário), e o relacionamento é opcional no banco. Sem este apoio,
cada campo desses vira um ternário repetido — eram seis linhas só no
`EventoClinicoMapper`.

### `RelacionamentosDoEvento` — a assinatura de cinco parâmetros

```java
public record RelacionamentosDoEvento(
        Veterinario veterinario, Animal animal, Clinica clinica, Servico servico) { }
```

As assinaturas de mapeamento chegavam a cinco parâmetros do mesmo "assunto",
fáceis de trocar de ordem na chamada — e o compilador não acusa nada quando os
tipos batem.

O serviço é opcional: eventos anteriores ao catálogo não têm, e o registro
direto pelo veterinário também pode não ter.

---

## A guarda do `EnderecoMapper`

```java
public EnderecoResponse toResponse(Endereco endereco) {
    if (endereco == null) return null;
    ...
}
```

`Endereco` é um `@Embedded` opcional: quando todas as colunas estão nulas, o
Hibernate devolve `null` no lugar do objeto. Sem esta guarda, um cadastro antigo
sem endereço derrubaria a **listagem inteira** com `NullPointerException`.

---

## O limite conhecido de todo `aplicarPatch`

Campo ausente e campo enviado como `null` chegam iguais ao DTO. Não há como
**apagar** um campo opcional via PATCH — use PUT para isso. Distinguir os dois
exigiria `Optional` em cada atributo ou JSON Merge Patch (RFC 7386),
complexidade que nenhum caso de uso deste projeto pede.

O raciocínio completo está em `TutorPatchRequest` — ver [dto.md](dto.md).

---

## Onde continuar

| Assunto | Documento |
|---|---|
| Quem chama o mapper e resolve os relacionamentos | [service.md](service.md) |
| Os DTOs de entrada e saída | [dto.md](dto.md) |
| As entidades de destino | [model.md](model.md) |
