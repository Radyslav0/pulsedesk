# — Build stage —
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
RUN mvn dependency:go-offline -q
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests -q

# — Runtime stage —
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/pulsedesk-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]