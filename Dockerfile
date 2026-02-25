# Etapa 1: Build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copia pom.xml y descarga dependencias para caching
COPY pom.xml .
RUN apt-get update -qq && apt-get install -y -qq maven && mvn dependency:go-offline

# Copia código fuente y compila
COPY src ./src
RUN mvn package -DskipTests

# Etapa 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
