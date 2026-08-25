# 01 — Spring Boot e injeção de dependência

## O que é

**Spring** é um container de objetos. Em vez de o seu código criar as próprias dependências
com `new`, ele **declara** o que precisa e o container entrega pronto. Isso se chama
**Inversão de Controle** (IoC): quem controla a criação não é a classe, é o framework.

**Spring Boot** é o Spring com as decisões já tomadas: servidor embutido, configuração
automática a partir das dependências que existem no classpath, e um `main` que sobe tudo.

Sem Spring, um controller precisaria montar a cadeia inteira à mão:

```java
// Como seria SEM injeção de dependência
var repository = new AnimalRepositoryImpl(dataSource);
var mapper     = new AnimalMapper();
var service    = new AnimalService(repository, mapper, seguranca);
var controller = new AnimalController(service);
```

Cada classe passaria a conhecer a construção de todas as outras. Trocar o repositório por
outra implementação, ou passar um dublê em teste, exigiria mexer em quem chama.

## O ponto de entrada

```java
// src/main/java/br/com/fiap/clyvovet/ClyvovetApplication.java
@SpringBootApplication
public class ClyvovetApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClyvovetApplication.class, args);
    }
}
```

`@SpringBootApplication` junta três anotações:

| Anotação | O que faz |
|---|---|
| `@SpringBootConfiguration` | marca a classe como fonte de configuração |
| `@EnableAutoConfiguration` | liga a autoconfiguração a partir do classpath |
| `@ComponentScan` | varre o **pacote da classe e os subpacotes** procurando beans |

O `@ComponentScan` explica por que tudo vive sob `br.com.fiap.clyvovet`: uma classe fora
desse pacote não seria encontrada, e a injeção falharia no boot.

## Beans: o que o container gerencia

Um **bean** é um objeto que o Spring cria, guarda e injeta em quem pedir. As anotações
mudam só a intenção — o efeito é o mesmo:

| Anotação | Uso neste projeto | Exemplo |
|---|---|---|
| `@RestController` | camada web | `AnimalController` |
| `@Service` | regra de negócio | `AnimalService`, `AuthService` |
| `@Component` | apoio genérico | `AnimalMapper`, `JwtAuthenticationFilter` |
| `@Repository` | persistência | implícito — o Spring Data implementa as interfaces |
| `@Configuration` + `@Bean` | objeto de terceiro que você monta | `CacheConfig`, `SecurityConfig` |

Por padrão todo bean é **singleton**: existe uma instância só, compartilhada. Por isso beans
não devem guardar estado por requisição.

## Injeção por construtor — o padrão aqui

Todo bean deste projeto recebe as dependências pelo construtor, gerado pelo Lombok:

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

`@RequiredArgsConstructor` gera um construtor com **todos os campos `final`**. O Spring vê
um único construtor e injeta por ele — sem precisar de `@Autowired`.

### Por que construtor e não campo

```java
// ❌ o que este projeto NÃO faz
@Autowired
private AnimalRepository animalRepository;
```

| Critério | Por campo | Por construtor |
|---|---|---|
| Campo pode ser `final` | não | **sim** — imutável depois de construído |
| Objeto nasce completo | não — fica um instante com `null` | **sim** |
| Testar sem Spring | precisa de reflexão | **`new AnimalService(...)`** |
| Excesso de dependências | fica escondido | **aparece** — construtor com 8 parâmetros incomoda, e deve incomodar |

Esse último ponto é o mais valioso: a injeção por construtor **denuncia** classes que estão
fazendo demais.

## Configuração externa e perfis

Valores que mudam por ambiente não ficam no código. Vêm de `application.properties` e podem
ser sobrescritos por variável de ambiente.

```java
// src/main/java/br/com/fiap/clyvovet/security/ControleTentativasLogin.java
@Value("${clyvovet.seguranca.max-tentativas-login:5}")
private int maxTentativas;

@Value("${clyvovet.seguranca.bloqueio-minutos:15}")
private int bloqueioMinutos;
```

A sintaxe é `${chave:valorPadrão}` — sem a propriedade definida, usa o default.

Um **perfil** é um conjunto de propriedades ativado por vez:

| Perfil | Arquivo | Banco |
|---|---|---|
| `oracle` (ativo por padrão) | `application-oracle.properties` | Oracle da FIAP |
| `mysql` | `application-mysql.properties` | MySQL (alvo do deploy) |
| `dev` | `application-dev.properties` | H2 em memória |
| `h2` | `application-h2.properties` | H2 em container |

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Um bean também pode existir só em certos perfis:

```java
// src/main/java/br/com/fiap/clyvovet/config/DevDataSeeder.java
@Configuration
@Profile({"dev", "h2"})
@RequiredArgsConstructor
public class DevDataSeeder {
```

Aqui isso é uma decisão de segurança, não de conveniência: o seeder cria usuários com senha
conhecida (`admin12345`), e ele **não pode** existir no perfil de entrega.

## Armadilhas reais deste projeto

### 1. Segredo com default é segredo vazado

```properties
# src/main/resources/application-oracle.properties
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

Repare que **não há default**. É proposital: sem a variável, a aplicação não sobe. Um
`${DB_PASSWORD:senha123}` faria ela subir com senha fraca e ninguém perceberia.

O item 2 de [`../docs/07-pendencias-e-divergencias.md`](../docs/07-pendencias-e-divergencias.md)
conta o resto da história — a senha ficou versionada em texto puro por vários commits e
continua no histórico do Git. Externalizar depois **não apaga** o que já foi commitado.

### 2. `@Transactional` em método privado não funciona

O Spring aplica `@Transactional` (e `@Cacheable`, e `@PreAuthorize`) por **proxy**: ele
embrulha o bean num objeto que intercepta as chamadas. Chamada interna — um método da classe
chamando outro da mesma classe — **não passa pelo proxy**, e a anotação é ignorada em
silêncio.

É por isso que o controle de tentativas de login vive em bean separado:

```java
// src/main/java/br/com/fiap/clyvovet/security/ControleTentativasLogin.java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void registrarFalha(Usuario usuario) { ... }
```

O comentário da própria classe explica: *"como o Spring aplica `@Transactional` via proxy,
isso só funciona a partir de outro bean; se estes métodos fossem privados do `AuthService`,
a anotação seria ignorada"*. Sem transação separada, o rollback do `BadCredentialsException`
apagaria a contagem de falhas e o bloqueio de conta nunca valeria.

### 3. Lombok exige annotation processing na IDE

Sem isso a IDE acusa `cannot find symbol getNome()` mesmo com o `mvn` compilando. Como
habilitar em cada IDE está em
[`../docs/06-guia-de-desenvolvimento.md`](../docs/06-guia-de-desenvolvimento.md).

## Perguntas de avaliação oral

1. Por que `AnimalService` recebe as dependências no construtor em vez de usar `@Autowired`
   no campo? Cite uma vantagem concreta.
2. O que aconteceria se você movesse uma classe `@Service` para o pacote `br.com.fiap.outro`?
3. Por que `DevDataSeeder` tem `@Profile({"dev", "h2"})`? O que daria errado sem isso?
4. Por que `ControleTentativasLogin` é um bean separado do `AuthService`, e não um método
   privado dele?
5. O que significa `${clyvovet.jwt.access-token-minutos:15}`? E por que
   `spring.datasource.password=${DB_PASSWORD}` **não** tem valor após os dois-pontos?

---

**Próximo:** [02 — Arquitetura em camadas](02-arquitetura-em-camadas.md)
