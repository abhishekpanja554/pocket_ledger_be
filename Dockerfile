# Stage 1: build the jar
FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

# Stage 2: run it
FROM eclipse-temurin:26-jre-noble
WORKDIR /app
RUN useradd --system appuser
COPY --from=build /app/target/*.jar app.jar
COPY entrypoint.sh .
RUN chmod +x entrypoint.sh
USER appuser
EXPOSE 8080
ENTRYPOINT ["./entrypoint.sh"]