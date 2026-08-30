# Etapa 1: build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
# Os testes rodam no build: eles usam H2 em memoria (perfil dev fixado em
# src/test/resources) e nao dependem mais de conectividade com o Oracle.
RUN mvn clean package -B

# Etapa 2: imagem final
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Cria usuário sem privilégios
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Sem perfil fixo no ENTRYPOINT. O -D de linha de comando tem precedencia sobre
# a variavel de ambiente, entao o "h2" que ficava aqui vencia qualquer
# SPRING_PROFILES_ACTIVE que o Azure definisse -- a imagem subiria em H2 em
# producao, e H2 nao e aceito como banco em nuvem pela disciplina de DevOps.
# Sem o -D, quem manda e SPRING_PROFILES_ACTIVE; sem ela, o default de
# application.properties, que e mysql.
ENTRYPOINT ["java", "-jar", "app.jar"]