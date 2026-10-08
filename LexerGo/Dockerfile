# ---------- Stage 1: сборка ----------

FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

# ---------- Stage 2: запуск ----------

FROM eclipse-temurin:11-jre
WORKDIR /app

COPY --from=build /build/target/go-lexer-1.0.jar /app/go-lexer.jar
COPY test1.go test2.go test3.go /app/

ENTRYPOINT ["java", "-jar", "/app/go-lexer.jar"]
CMD ["test1.go", "test2.go", "test3.go"]