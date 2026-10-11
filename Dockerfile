# Step 1: Build stage using Maven and Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Step 2: Lightweight runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/advanced-chat-app-backend-1.0-SNAPSHOT.jar app.jar
ENV PORT=5000
EXPOSE 5000
CMD ["java", "-Xmx384m", "-jar", "app.jar"]
