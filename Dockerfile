# Etapa 1: Compilação com Maven e Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY index.html .
RUN mvn clean package -DskipTests

# Etapa 2: Execução leve
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/ControleDespesaPessoal-1.0-SNAPSHOT.jar app.jar
COPY --from=build /app/index.html index.html

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
