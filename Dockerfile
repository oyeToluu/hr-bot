# Build stage: compile and package the Spring Boot jar.
# Tests are skipped here because they need a MongoDB; run them locally.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

# Run stage: a small Java runtime with just the jar.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/hr-bot-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
# Keep memory use inside free-tier limits (512 MB).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
