# Use the official Maven image to build the application
FROM maven:3.8.4-eclipse-temurin-17 AS build

WORKDIR /app

# Download dependencies first (cached unless pom.xml changes); reduces network flakiness
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests -B

# Use the official OpenJDK image to run the application
FROM eclipse-temurin:17-jdk

# Set the working directory for running the application
WORKDIR /app

# Copy the JAR file from the build stage to the runtime image
COPY --from=build /app/target/srishna-manual-posts-1.0.0.jar .

# Set full permissions for the JAR file (optional, not always needed)
RUN chmod 755 srishna-manual-posts-1.0.0.jar

# Expose the application port
EXPOSE 8080

# Use writable path for SQLite (Cloud Run filesystem is read-only except /tmp)
ENV SQLITE_PATH=/tmp/srishna.db

# Command to run the application
ENTRYPOINT ["java", "-jar", "srishna-manual-posts-1.0.0.jar"]
