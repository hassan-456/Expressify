# # Step 1: Build the app
# FROM maven:3.9.9-eclipse-temurin-17 AS build

# WORKDIR /app

# COPY pom.xml .
# COPY src ./src

# RUN mvn clean package -DskipTests

# # Step 2: Run the app
# FROM eclipse-temurin:17-jdk-jammy

# WORKDIR /app

# COPY --from=build /app/target/*.jar app.jar

# EXPOSE 8080

# CMD ["java", "-jar", "app.jar"]

# ===============================
# BUILD STAGE
# ===============================
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy only pom first (better caching)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source
COPY src ./src

# Build jar
RUN mvn clean package -DskipTests

# ===============================
# RUN STAGE
# ===============================
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Copy built jar
COPY --from=build /app/target/*.jar app.jar

# Render uses dynamic PORT
EXPOSE 8081

# Run app
ENTRYPOINT ["java", "-jar", "app.jar"]