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

# JVM PARA MAQUINA PEQUENA
# No plano gratuito da Render o container tem 512 MB e uma fracao de CPU, e o
# servico dorme depois de 15 min parado. Com a JVM no padrao, acordar levou de
# 2 a mais de 5 minutos (medido em 31/10 e 03/11/2026) -- mais do que o app
# espera no login.
#   TieredStopAtLevel=1  so o compilador C1: o C2 gasta a pouca CPU no boot
#                        otimizando codigo que, com este trafego, nunca se paga
#   UseSerialGC          coletor sem threads paralelas, que aqui so disputariam CPU
#   MaxRAMPercentage=75  o heap pode usar 384 MB dos 512, e nao os 128 do padrao
#   Xss512k              metade da pilha por thread, sobra para o heap
# Em JAVA_TOOL_OPTIONS, e nao no ENTRYPOINT, para poder ser trocado no painel da
# Render sem rebuild.
ENV JAVA_TOOL_OPTIONS="-XX:TieredStopAtLevel=1 -XX:+UseSerialGC -XX:MaxRAMPercentage=75 -Xss512k"

# Sem perfil fixo no ENTRYPOINT. O -D de linha de comando tem precedencia sobre
# a variavel de ambiente, entao o "h2" que ficava aqui vencia qualquer
# SPRING_PROFILES_ACTIVE que o Azure definisse -- a imagem subiria em H2 em
# producao, e H2 nao e aceito como banco em nuvem pela disciplina de DevOps.
# Sem o -D, quem manda e SPRING_PROFILES_ACTIVE; sem ela, o default de
# application.properties, que e mysql.
ENTRYPOINT ["java", "-jar", "app.jar"]