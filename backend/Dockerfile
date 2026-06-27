# ---- build faza ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# prvo zavisnosti (bolji cache); go-offline je best-effort pa ne ruši build
COPY pom.xml .
RUN mvn -B dependency:go-offline -DskipTests || true
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- run faza ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
