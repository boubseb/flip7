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

# Install wget for the docker-compose healthcheck (not present by default in the JRE image)
RUN apt-get update && apt-get install -y --no-install-recommends wget && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar

# Run as a non-root user
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

EXPOSE 3200

# -XX:MaxRAMPercentage=75.0 lets the JVM use up to 75% of container memory
# All runtime config (DB URL, password, profile) is injected via docker-compose env vars
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
