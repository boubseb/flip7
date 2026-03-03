# ----- Stage 1: Build -----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy Maven wrapper and pom.xml (for dependency caching)
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN ./mvnw package -DskipTests -B

# ----- Stage 2: Runtime -----
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Default Spring Boot port
EXPOSE 3200

# Use environment variables to override DB config at runtime
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/flip7
ENV SPRING_DATASOURCE_USERNAME=flip7_admin
ENV SPRING_DATASOURCE_PASSWORD=Testflip7!
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "app.jar"]
