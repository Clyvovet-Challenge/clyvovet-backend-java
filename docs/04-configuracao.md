# Configuração

## Variáveis de ambiente obrigatórias

| Variável | Onde | Observação |
|---|---|---|
| `JWT_SECRET` | todos os perfis | Base64 com ≥ 32 bytes. `dev` e `h2` têm default local; **`oracle` não** |
| `DB_USERNAME` | perfil `oracle` | sem default — a aplicação não sobe sem |
| `DB_PASSWORD` | perfil `oracle` | idem |
| `DB_URL` | perfil `oracle` | opcional; default é o Oracle da FIAP |

```bash
export JWT_SECRET=$(openssl rand -base64 48)
export DB_USERNAME=rm000000
export DB_PASSWORD=suasenha
```

Nenhuma credencial fica versionada. Além do risco em si, a disciplina de DevOps
desconta 20 pontos por "dados sensíveis expostos no código-fonte".

---

## Migrations (Flyway)

O schema é versionado em `src/main/resources/db/migration/`, em **dois conjuntos**
— um por banco:

```
db/migration/
├── oracle/   ← Oracle FIAP, e também H2 (que roda com MODE=Oracle)
└── mysql/    ← Azure Database for MySQL, alvo do deploy
```

Cada pasta tem a linha do tempo completa:

| Migration | Conteúdo |
|---|---|
| `V1__schema_inicial.sql` | As 6 tabelas de domínio, com FKs, uniques e checks |
| `V2__seed_inicial.sql` | Carga inicial, ≥ 5 registros por tabela |
| `V3__usuario_e_perfis.sql` | Tabela `usuario` |
| `V4__corrige_status_pagamento.sql` | Alinha o check a `REEMBOLSADO` |

Até agosto de 2026 havia um conjunto só, porque os dois bancos em uso eram Oracle e
H2 — e o H2 tem `MODE=Oracle`. Com a entrada do MySQL isso acabou: **MySQL não tem
modo de compatibilidade com Oracle**. As diferenças são poucas (três decisões em 205
linhas de SQL), mas não existe grafia comum que sirva às duas.

Os UUIDs do seed são literais fixos, o que dispensa PL/SQL e dá dados
determinísticos aos testes. O corpo da V2 é idêntico nas duas pastas.

> 📖 O raciocínio completo — por que dois conjuntos em vez de SQL portável, quais são
> as três diferenças e o que garante que as pastas não divirjam — está em
> [`db/migration/README.md`](../src/main/resources/db/migration/README.md).

### Onde cada perfil busca

O caminho é escrito por extenso em cada perfil, **não** com o coringa `{vendor}` do
Flyway: ele resolve pelo banco da conexão, e os perfis `dev`/`h2` conectam num H2 —
`{vendor}` viraria `h2` e não acharia pasta nenhuma, embora quem os sirva seja a
pasta `oracle/`.

| perfil | `spring.flyway.locations` |
|---|---|
| `oracle` | `classpath:db/migration/oracle` |
| `dev` | `classpath:db/migration/oracle` |
| `h2` | `classpath:db/migration/oracle` |
| `mysql` | `classpath:db/migration/mysql` |

### O que impede as pastas de divergirem

[`MigrationsMySqlTest`](../src/test/java/br/com/fiap/clyvovet/migration/MigrationsMySqlTest.java)
roda o conjunto `mysql/` do zero num H2 em `MODE=MySQL` e confere que as quatro
migrations aplicam e que o seed carrega as mesmas contagens. A build quebra se
alguém adicionar uma migration só de um lado.

Ele **não** substitui um MySQL real: H2 em `MODE=MySQL` valida sintaxe e semântica
principal, não o comportamento de tipos do servidor. Verde ali é "o SQL está
coerente", não "validado em produção".

### Bancos já provisionados

O perfil `oracle` usa `baseline-on-migrate=true` com **`baseline-version=2`**: bancos
criados pelo antigo `db-oracle.sql` entram com V1 e V2 marcadas como aplicadas, e a
migração segue da V3. Com `baseline-version=1` o seed rodaria de novo e duplicaria os
dados.

Por isso a correção do `status_pagamento` está na V4, e não na V1 — assim ela alcança
também os bancos que entraram por baseline.

O perfil `mysql` **não** tem baseline, de propósito: um MySQL novo começa vazio e a
V1 e a V2 precisam rodar de verdade. Ligar baseline ali pularia o schema e o seed, e
a aplicação subiria contra um banco sem tabela.

---

## Arquivos de propriedades

| Arquivo | Papel |
|---|---|
| [`application.properties`](../src/main/resources/application.properties) | Nome da aplicação e escolha do perfil ativo |
| [`application-oracle.properties`](../src/main/resources/application-oracle.properties) | Oracle FIAP — perfil de entrega/produção |
| [`application-h2.properties`](../src/main/resources/application-h2.properties) | H2 em modo servidor — usado dentro do Docker |
| [`application-dev.properties`](../src/main/resources/application-dev.properties) | H2 em memória — desenvolvimento local |
| [`application-mysql.properties`](../src/main/resources/application-mysql.properties) | Azure Database for MySQL — alvo do deploy. **Ainda não executado contra um MySQL real** |

O arquivo raiz tem apenas duas linhas:

```properties
spring.application.name=clyvovet
spring.profiles.active=oracle
```

**O perfil `oracle` é o ativo por padrão.** Rodar `mvn spring-boot:run` sem
sobrescrever nada faz a aplicação tentar conectar no Oracle da FIAP.

---

## Comparativo dos perfis

| Aspecto | `oracle` | `mysql` | `h2` | `dev` |
|---|---|---|---|---|
| Banco | Oracle 19c FIAP | Azure MySQL Flexible Server | H2 em modo TCP | H2 em memória |
| Migrations | `db/migration/oracle` | `db/migration/mysql` | `db/migration/oracle` | `db/migration/oracle` |
| Driver | `oracle.jdbc.OracleDriver` | `com.mysql.cj.jdbc.Driver` | `org.h2.Driver` | `org.h2.Driver` |
| `ddl-auto` | `validate` | `validate` | `none` | `none` |
| Baseline do Flyway | `baseline-version=2` | **nenhum** | nenhum | nenhum |
| Dados persistem? | sim | sim | sim (volume Docker) | **não** |
| Seed inicial | 42 registros (V2) | idem | idem | idem |
| Console H2 | — | — | `/h2-console` | `/h2-console` |
| Roda fora do Docker? | sim | sim | **não** | sim |
| Uso pretendido | banco de testes / entrega | **deploy** | container | desenvolvimento local |
| Já rodou de verdade? | sim | **não** | sim | sim |

> ⚠️ O perfil `h2` aponta para o host `clyvovet-db`, que só existe na rede do
> docker-compose. Para rodar localmente sem Docker, use o perfil **`dev`**.

---

## Perfil `oracle`

```properties
server.port=8080
spring.jackson.serialization.write-dates-as-timestamps=false

# Credenciais NUNCA versionadas — vêm do ambiente.
spring.datasource.url=${DB_URL:jdbc:oracle:thin:@oracle.fiap.com.br:1521:orcl}
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

spring.flyway.locations=classpath:db/migration/oracle
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=2

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.OracleDialect
spring.jpa.properties.hibernate.id.uuid_jdbc_type=CHAR
spring.jpa.properties.hibernate.type.preferred_uuid_jdbc_type=CHAR

clyvovet.jwt.secret=${JWT_SECRET}
```

Pontos de atenção:

- **`ddl-auto=none`** — o schema precisa existir antes. Execute
  [`db-oracle.sql`](../src/main/resources/db/db-oracle.sql) no SQL Developer com
  **Run Script (F5)** antes do primeiro boot.
- As duas propriedades `uuid_jdbc_type=CHAR` são o que faz o UUID gravar como texto em
  `VARCHAR2(36)`. Sem elas o Hibernate usaria `RAW(16)` e não leria o seed.
- `write-dates-as-timestamps=false` faz o Jackson serializar `LocalDate` como
  `"2025-05-20"` em vez de array numérico.
- **As credenciais estão versionadas em texto puro.** Ver
  [07-pendencias-e-divergencias.md](07-pendencias-e-divergencias.md).

Para usar credenciais próprias sem editar o arquivo:

```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--spring.datasource.username=SEU_RM --spring.datasource.password=SUA_SENHA"
```

---

## Perfil `mysql`

Alvo do deploy no **Azure Container Apps + Azure Database for MySQL Flexible
Server**.

> ⚠️ **Este perfil ainda não foi executado contra um MySQL real.** Ele foi escrito
> junto com as migrations de `db/migration/mysql/` enquanto a infraestrutura era
> definida com o time de devops, e validado apenas por
> `MigrationsMySqlTest` (H2 em `MODE=MySQL`). Trate-o como ponto de partida
> revisado, não como configuração validada em produção.

```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/clyvovet?sslMode=REQUIRED}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.flyway.locations=classpath:db/migration/mysql
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.properties.hibernate.id.uuid_jdbc_type=CHAR
spring.jpa.properties.hibernate.type.preferred_uuid_jdbc_type=CHAR
```

Três pontos que não são óbvios:

**`sslMode=REQUIRED` não é enfeite.** O Azure Flexible Server recusa conexão sem TLS
por padrão. Sem isso a aplicação simplesmente não conecta.

**`serverTimezone` não é definido de propósito.** As colunas temporais são `DATETIME`,
que não sofre conversão de fuso — foi justamente para evitar essa conversão que o
`bloqueado_ate` não é `TIMESTAMP` no MySQL. Definir `serverTimezone` reintroduziria a
classe de bug que a escolha do tipo eliminou.

**As duas linhas de `uuid_jdbc_type` são obrigatórias.** Os ids são `UUID` gravados
como `VARCHAR(36)`, igual ao Oracle. Sem elas o Hibernate grava UUID como
`BINARY(16)` no MySQL, e os ids deixariam de casar com o seed da V2 e com qualquer
dado migrado do Oracle.

### Segredos

`DB_USERNAME`, `DB_PASSWORD` e `JWT_SECRET` não têm default — a aplicação não sobe
sem eles. No Azure Container Apps eles entram como **secrets do container** ou
referência ao Key Vault, nunca como variável de ambiente em texto no manifesto.

---

## Perfil `h2`

```properties
server.port=8080
spring.application.name=clyvovet
spring.jackson.serialization.write-dates-as-timestamps=false

spring.datasource.url=jdbc:h2:tcp://clyvovet-db:1521/clyvovet
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.h2.console.enabled=true
spring.h2.console.settings.web-allow-others=true
```

É o perfil do container. O host `clyvovet-db` é o nome do serviço H2 no
[`docker-compose.yml`](../docker-compose.yml), e a porta 1521 é a porta TCP do H2
(coincide com a do Oracle por escolha do compose, não por acaso técnico).

Com `ddl-auto=update`, o Hibernate cria as tabelas no primeiro boot e as preserva
entre reinícios — os dados sobrevivem no volume `clyvovet-h2-data`.

`web-allow-others=true` libera o console H2 para conexões externas ao container.

---

## Perfil `dev`

```properties
spring.datasource.url=jdbc:h2:mem:clyvovetdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
```

Banco em memória, recriado do zero a cada boot e descartado ao encerrar. É o perfil
mais prático para desenvolver: não precisa de Oracle, não precisa de Docker, não
precisa rodar SQL nenhum.

Não define `server.port`, então usa o default 8080.

---

## Como rodar

### Localmente com H2 em memória (recomendado para desenvolvimento)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Banco vazio em `http://localhost:8080`. Comece criando um tutor e uma clínica.

### Localmente contra o Oracle FIAP

Pré-requisito: ter executado o `db-oracle.sql` no seu schema.

```bash
./mvnw spring-boot:run
```

O perfil `oracle` já é o default. Se as credenciais no arquivo não forem as suas,
sobrescreva pela linha de comando (ver acima).

### Via Docker Compose

```bash
docker compose up --build
```

Sobe H2 em container + API no perfil `h2`. Detalhes em [05-deploy.md](05-deploy.md).

### Gerando o JAR

```bash
./mvnw clean package
java -jar target/clyvovet-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

---

## Precedência de configuração

Do mais forte para o mais fraco:

| Prioridade | Origem | Exemplo |
|---|---|---|
| 1 | Argumento de linha de comando | `--spring.profiles.active=dev` |
| 2 | Propriedade de sistema (`-D`) | `-Dspring.profiles.active=h2` |
| 3 | Variável de ambiente | `SPRING_PROFILES_ACTIVE=h2` |
| 4 | `application-{perfil}.properties` | perfil ativo |
| 5 | `application.properties` | base |

No container, o `ENTRYPOINT` do Dockerfile passa `-Dspring.profiles.active=h2` **e** o
compose define `SPRING_PROFILES_ACTIVE=h2`. Como os dois valores coincidem, não há
conflito — mas vale saber que o `-D` venceria.

---

## Dependências Maven

Declaradas em [`pom.xml`](../pom.xml). Parent: `spring-boot-starter-parent:3.5.14`,
`java.version=17`.

| Dependência | Escopo | Para quê |
|---|---|---|
| `spring-boot-starter-web` | compile | MVC, Tomcat embarcado, Jackson |
| `spring-boot-starter-data-jpa` | compile | Hibernate, repositórios, HikariCP |
| `spring-boot-starter-validation` | compile | Bean Validation |
| `spring-boot-starter-cache` | compile | `@Cacheable`/`@CacheEvict` |
| `springdoc-openapi-starter-webmvc-ui:2.8.16` | compile | Swagger UI + OpenAPI 3 |
| `com.h2database:h2` | runtime | banco de dev e container |
| `com.oracle.database.jdbc:ojdbc11` | runtime | driver Oracle |
| `com.mysql:mysql-connector-j` | runtime | driver MySQL |
| `org.flywaydb:flyway-database-oracle` | compile | suporte do Flyway ao Oracle |
| `org.flywaydb:flyway-mysql` | compile | suporte do Flyway ao MySQL |
| `org.projectlombok:lombok` | optional | getters, setters, construtores |
| `spring-boot-starter-test` | test | JUnit 5, Mockito, AssertJ |

O Flyway 10+ separa o suporte a cada banco em um módulo próprio, por isso os dois
`flyway-*` aparecem juntos: o Oracle continua servindo de banco de testes e o MySQL é
o alvo do deploy. Qual conjunto de migrations roda é decidido pelo perfil, em
`spring.flyway.locations`. Nenhum dos dois declara versão — todas vêm do BOM do
Spring Boot.

O provider de cache é o **Caffeine**, configurado em `CacheConfig` com expiração de
10 minutos e limite de 1.000 entradas. Continua em memória e não compartilhado entre
instâncias — com mais de uma réplica, o caminho seria Redis.

### Plugins de build

| Plugin | Configuração |
|---|---|
| `spring-boot-maven-plugin` | exclui o Lombok do JAR final (só é necessário em compilação) |
| `maven-compiler-plugin` | registra o Lombok como annotation processor em `compile` e `test-compile` |

---

## Variáveis sensíveis

O [`.gitignore`](../.gitignore) exclui `application-prod.properties` — o padrão
previsto para credenciais reais. Os perfis versionados (`oracle`, `h2`, `dev`) contêm
apenas credenciais de aula.

Para sobrescrever qualquer propriedade sem tocar em arquivo versionado, use variável
de ambiente com o nome em maiúsculas e underscores:

```bash
export SPRING_DATASOURCE_USERNAME=rm999999
export SPRING_DATASOURCE_PASSWORD=minhasenha
./mvnw spring-boot:run
```
