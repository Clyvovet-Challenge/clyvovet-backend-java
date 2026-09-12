# 01 — O que é Spring, afinal

> **Pré-requisito:** [00 — O Java que você precisa](00-java-essencial.md), principalmente as
> seções sobre **anotações**, **interface** e **`final`**.

---

## Comece pelo problema, não pela ferramenta

Esqueça Spring por um minuto. Você quer construir isto:

> Alguém digita um endereço no navegador ou abre o app. Do outro lado, um programa recebe o
> pedido, busca dados num banco e devolve a resposta.

Esse programa do outro lado é o **backend**. Para ele funcionar, alguém precisa:

1. **abrir uma porta de rede** e ficar escutando pedidos;
2. **entender HTTP** — o protocolo em que o pedido chega;
3. **descobrir qual código chamar** para `/animais/3` e qual para `/tutores`;
4. **converter JSON em objeto Java** na entrada, e o contrário na saída;
5. **abrir conexão com o banco**, montar SQL, ler o resultado, fechar a conexão;
6. **gerenciar transações** — desfazer tudo se algo falhar no meio;
7. **checar senha, token, permissão**;
8. **tratar erro** sem derrubar o servidor.

Escrever isso à mão dá meses de trabalho — e nada disso é o seu problema de negócio. Seu
problema é *"cadastrar um pet e registrar consultas"*.

**Spring é o pacote que já resolveu os oito itens.** Você escreve só o que é seu.

---

## Spring, Spring Boot, Spring Data: quem é quem

Os nomes confundem. A separação:

| Nome | O que é |
|---|---|
| **Spring Framework** | a base — o container de objetos e a injeção de dependência |
| **Spring Boot** | o Spring **com as decisões já tomadas**: servidor embutido, configuração automática, um `main` que sobe tudo |
| **Spring Data JPA** | o módulo que gera os repositórios de banco |
| **Spring Security** | o módulo de autenticação e autorização |
| **Spring MVC** | o módulo que trata requisições HTTP |

Analogia: Spring Framework é a caixa de peças de Lego. Spring Boot é o kit com o manual e as
peças certas separadas. Os outros são caixas temáticas que se encaixam no mesmo sistema.

**Neste projeto todos estão presentes** — é o que o `pom.xml` declara.

---

## O programa inteiro cabe em cinco linhas

```java
// src/main/java/br/com/fiap/clyvovet/ClyvovetApplication.java
@SpringBootApplication
public class ClyvovetApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClyvovetApplication.class, args);
    }
}
```

Rode isso e você tem um servidor HTTP no ar, na porta 8080, conectado ao banco, com Swagger
funcionando.

O que a linha `SpringApplication.run(...)` dispara, em ordem:

```
1. lê application.properties e descobre o perfil ativo
2. varre o pacote br.com.fiap.clyvovet procurando classes anotadas
3. cria um objeto de cada uma e guarda no container
4. injeta as dependências de cada objeto
5. conecta no banco e roda as migrations do Flyway
6. confere se as entidades batem com o schema (ddl-auto=validate)
7. sobe o Tomcat e começa a escutar a porta 8080
```

Se qualquer etapa falhar, a aplicação **não sobe**. Isso é bom: erro no boot é barato,
erro em produção não é.

### A anotação `@SpringBootApplication`

Ela é três em uma:

| Contém | O que faz |
|---|---|
| `@SpringBootConfiguration` | marca a classe como fonte de configuração |
| `@EnableAutoConfiguration` | configura automaticamente o que achar no classpath |
| `@ComponentScan` | **varre o pacote desta classe e os subpacotes** procurando anotações |

O `@ComponentScan` explica algo importante: `ClyvovetApplication` está em
`br.com.fiap.clyvovet`, então o Spring varre **daí para baixo**. Uma classe em
`br.com.outro.pacote` seria simplesmente ignorada — e a injeção falharia no boot com
"nenhum bean deste tipo encontrado".

### Autoconfiguração: adivinhação com regra

`@EnableAutoConfiguration` olha as bibliotecas presentes e configura o que fizer sentido:

| Achou no `pom.xml` | Configura |
|---|---|
| `spring-boot-starter-web` | Tomcat na 8080, JSON via Jackson |
| `spring-boot-starter-data-jpa` + driver | conexão, pool, Hibernate |
| `spring-boot-starter-security` | cadeia de filtros de segurança |
| `flyway-core` | roda migrations no boot |

Não é mágica: são classes de configuração com condições do tipo *"se existe X no classpath
e o usuário não definiu o seu próprio, use este padrão"*.

E você pode **substituir** qualquer padrão declarando o seu:

```java
// src/main/java/br/com/fiap/clyvovet/config/CacheConfig.java
@Bean
public CacheManager cacheManager() {
    // este bean vence o CacheManager padrão do Boot
}
```

---

## Inversão de Controle: quem chama quem

Aqui está a ideia central do Spring, e ela merece calma.

### Como seria sem Spring

`AnimalService` precisa de um repositório, um mapper e o serviço de segurança. Sem Spring,
ele cria tudo:

```java
public class AnimalService {
    private AnimalRepository repo = new AnimalRepositoryImpl(/* precisa do DataSource... */);
    private AnimalMapper mapper = new AnimalMapper();
    private SegurancaService seguranca = new SegurancaService(/* que precisa de 3 repos... */);
}
```

Três problemas, e o terceiro é o pior:

1. `AnimalService` precisa saber **como construir** cada dependência — inclusive as
   dependências das dependências.
2. Trocar `AnimalRepositoryImpl` por outra implementação exige editar `AnimalService`.
3. **Testar fica inviável.** Não dá para entregar um repositório falso: o `new` está
   soldado dentro da classe.

### Como fica com Spring

A classe **declara** o que precisa e **recebe** pronto:

```java
// src/main/java/br/com/fiap/clyvovet/service/AnimalService.java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnimalService {

    private final AnimalRepository animalRepository;
    private final TutorRepository tutorRepository;
    private final AnimalMapper animalMapper;
    private final SegurancaService seguranca;
```

Nenhum `new`. Nenhuma menção a implementação concreta.

💡 **Conceito: Inversão de Controle (IoC)**

Normalmente **seu código** decide quando criar objetos. Com IoC, isso inverte: **o
framework** cria e entrega. Daí o nome — o controle foi invertido.

Aqui, o Spring lê `@Service`, vê que o construtor pede quatro coisas, procura cada uma no
container e monta `AnimalService` já pronto. Você nunca escreve `new AnimalService(...)`.

O ganho concreto: como a classe **recebe** em vez de **criar**, um teste pode entregar
qualquer implementação. A classe não sabe a diferença — e é exatamente por isso que ela é
testável.

**Injeção de Dependência** (DI) é o nome do mecanismo que entrega. IoC é o princípio; DI é
a técnica.

---

## Bean: o que o container guarda

**Bean** = objeto criado e gerenciado pelo Spring. As anotações que marcam um:

| Anotação | Intenção | Exemplo daqui |
|---|---|---|
| `@RestController` | camada web | `AnimalController` |
| `@Service` | regra de negócio | `AnimalService`, `AuthService` |
| `@Component` | apoio genérico | `AnimalMapper`, `JwtAuthenticationFilter` |
| `@Repository` | persistência | implícito — Spring Data implementa as interfaces |
| `@Configuration` + `@Bean` | objeto de terceiro que você monta | `CacheConfig` |

**Tecnicamente todas fazem a mesma coisa**: registram um bean. A diferença é **comunicar
intenção** para quem lê. `@Service` numa classe que só converte dados confundiria o próximo
leitor.

### Todo bean é singleton (e isso tem consequência)

Por padrão existe **uma instância só** de cada bean, compartilhada por todas as requisições
— inclusive simultâneas.

Consequência prática: **bean não pode guardar estado de uma requisição**.

```java
// ❌ NUNCA faça isso
@Service
public class AnimalService {
    private Animal animalAtual;   // requisições concorrentes se atropelam
}
```

```java
// ✅ estado vive nos parâmetros e no retorno
public AnimalResponse buscarPorId(UUID id) {
    return animalMapper.toResponse(animalRepository.obterPorId(id));
}
```

Repare que **todos** os campos dos services deste projeto são `final` — ou seja, dependências,
não estado. Não é coincidência.

---

## Por que injeção por construtor

Existem três formas. Este projeto usa uma, e vale saber por quê.

```java
// ❌ por campo
@Autowired
private AnimalRepository animalRepository;

// ❌ por setter
@Autowired
public void setAnimalRepository(AnimalRepository r) { this.animalRepository = r; }

// ✅ por construtor — o que este projeto usa
private final AnimalRepository animalRepository;

public AnimalService(AnimalRepository animalRepository) {
    this.animalRepository = animalRepository;
}
```

E o construtor nem aparece no código, porque o Lombok gera:

```java
@RequiredArgsConstructor   // gera construtor com TODOS os campos final
```

Comparação:

| Critério | Por campo | Por construtor |
|---|---|---|
| Campo pode ser `final` | ❌ | ✅ imutável |
| Objeto nasce completo | ❌ existe um instante com `null` | ✅ |
| Testar sem Spring | precisa de reflexão | ✅ `new AnimalService(repo, mapper, ...)` |
| Excesso de dependências | fica escondido | ✅ **aparece** |

O último item é o mais valioso e o menos óbvio. Um construtor com oito parâmetros
**incomoda visualmente** — e deve incomodar, porque é sinal de que a classe faz coisas
demais. Com `@Autowired` em campo, você pode acumular vinte dependências sem que nada
pareça errado.

💡 **Conceito: quando o Spring dispensa `@Autowired`**

Desde o Spring 4.3, se a classe tem **um único construtor**, o Spring usa aquele
automaticamente — sem anotação.

É por isso que nenhuma classe deste projeto tem `@Autowired`: `@RequiredArgsConstructor`
gera exatamente um construtor, e o Spring o encontra sozinho. Menos ruído, mesmo resultado.

---

## Configuração externa e perfis

Valor que muda por ambiente **não** fica no código.

```java
// src/main/java/br/com/fiap/clyvovet/security/ControleTentativasLogin.java
@Value("${clyvovet.seguranca.max-tentativas-login:5}")
private int maxTentativas;
```

Sintaxe: `${chave:valorPadrão}`. Sem a propriedade definida, usa 5.

### Perfil = um conjunto de configurações por vez

| Perfil | Arquivo | Banco |
|---|---|---|
| `oracle` (padrão) | `application-oracle.properties` | Oracle da FIAP |
| `mysql` | `application-mysql.properties` | MySQL (alvo do deploy) |
| `dev` | `application-dev.properties` | H2 em memória |
| `h2` | `application-h2.properties` | H2 em container |

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Um bean também pode existir **só** em certos perfis:

```java
// src/main/java/br/com/fiap/clyvovet/config/DevDataSeeder.java
@Configuration
@Profile({"dev", "h2"})
public class DevDataSeeder {
```

Isto aqui é **decisão de segurança**, não conveniência: o seeder cria usuários com senha
conhecida (`admin12345`). Ele **não pode** existir no ambiente de entrega. Sem o `@Profile`,
existiria.

---

## Armadilhas reais deste projeto

### 1. Segredo com valor padrão é segredo vazado

```properties
# src/main/resources/application-oracle.properties
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

Repare: **não há valor após os dois-pontos**. É proposital — sem a variável de ambiente, a
aplicação **não sobe**.

Se estivesse `${DB_PASSWORD:senha123}`, ela subiria com senha fraca e ninguém notaria. Falha
alta e visível é melhor que sucesso silencioso e errado.

Este projeto aprendeu isso do jeito difícil: a senha do Oracle ficou versionada em texto puro
por vários commits e **continua no histórico do Git**. Externalizar depois não apaga o que já
foi commitado — só trocar a senha resolve. Item 2 de
[`../docs/07-pendencias-e-divergencias.md`](../docs/07-pendencias-e-divergencias.md).

### 2. A armadilha do proxy — guarde esta

Esta é a causa da maioria dos "por que a anotação não funcionou" em Spring.

O Spring não entrega o seu objeto direto. Ele entrega um **proxy**: um objeto que embrulha o
seu e intercepta as chamadas para aplicar `@Transactional`, `@Cacheable`, `@PreAuthorize`.

```
quem chama ──▶ [ PROXY ] ──▶ seu objeto
                  ↑
        aqui a anotação é aplicada
```

O problema: **chamada interna não passa pelo proxy**.

```java
// ❌ o @Transactional de metodoB é IGNORADO
@Service
public class MeuService {
    public void metodoA() {
        this.metodoB();      // chamada direta, sem passar pelo proxy
    }

    @Transactional
    public void metodoB() { ... }
}
```

E não há erro, nem aviso, nem log. Só não funciona.

É exatamente por isso que o controle de tentativas de login vive em **bean separado**:

```java
// src/main/java/br/com/fiap/clyvovet/security/ControleTentativasLogin.java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void registrarFalha(Usuario usuario) { ... }
```

O comentário da classe diz: *"como o Spring aplica `@Transactional` via proxy, isso só
funciona a partir de outro bean; se estes métodos fossem privados do `AuthService`, a
anotação seria ignorada"*.

Consequência se fosse ignorada: o rollback do `BadCredentialsException` apagaria a contagem
de falhas, e o bloqueio de conta **nunca aconteceria** — o contador voltaria a zero a cada
tentativa. Uma falha de segurança silenciosa.

### 3. Lombok e a IDE

`cannot find symbol getNome()` na IDE com o `mvn` compilando = *annotation processing*
desligado. Ver [`../docs/06-guia-de-desenvolvimento.md`](../docs/06-guia-de-desenvolvimento.md).

---

## Consolidação

**Entender**
1. Explique com suas palavras: o que significa "inversão de controle"? O que foi invertido?
2. O que `@ComponentScan` faz, e por que todas as classes deste projeto ficam sob
   `br.com.fiap.clyvovet`?

**Aplicar**
3. Você criou `RelatorioService` em `br.com.fiap.clyvovet.service` e anotou com `@Service`.
   Precisa registrá-lo em algum lugar? Por quê?
4. Como você injetaria `AnimalRepository` numa classe nova, seguindo o padrão daqui?

**Analisar**
5. Compare injeção por campo e por construtor. Cite uma vantagem que **só** aparece quando a
   classe cresce demais.
6. `spring.datasource.password=${DB_PASSWORD}` não tem valor padrão, mas
   `max-tentativas-login:5` tem. Por que a diferença?

**Avaliar**
7. Um colega escreveu um `@Service` com um campo `private Animal animalEmProcessamento`.
   Qual o risco? Como você explicaria a ele?
8. Um `@Transactional` "não está funcionando". Quais duas hipóteses você levantaria primeiro,
   e como testaria cada uma?

---

## Se você levar só uma coisa daqui

**O Spring entrega um proxy, não o seu objeto.** Quase toda anotação de comportamento
(`@Transactional`, `@Cacheable`, `@PreAuthorize`) depende de a chamada passar por esse
proxy — e chamada interna não passa.

---

**Anterior:** [00 — O Java que você precisa](00-java-essencial.md) ·
**Próximo:** [02 — Arquitetura em camadas](02-arquitetura-em-camadas.md)
