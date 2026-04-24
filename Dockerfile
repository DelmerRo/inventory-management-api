# =============================================
# Etapa 1: Build
# =============================================
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Variables de entorno para Maven
ENV MAVEN_OPTS="-Dmaven.repo.local=/root/.m2/repository"

# Instalar Maven y configurar
RUN apt-get update -qq && \
    apt-get install -y -qq maven && \
    rm -rf /var/lib/apt/lists/*

# Copiar pom.xml primero (para cachear dependencias)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fuente y compilar
COPY src ./src
RUN mvn package -DskipTests -B

# =============================================
# Etapa 2: Runtime
# =============================================
FROM eclipse-temurin:21-jre
WORKDIR /app

# ✅ Configurar zona horaria y UTF-8
ENV TZ=America/Argentina/Buenos_Aires
ENV LANG=es_AR.UTF-8
ENV LANGUAGE=es_AR:es
ENV LC_ALL=es_AR.UTF-8

# ✅ Instalar tzdata para zona horaria
RUN apt-get update -qq && \
    apt-get install -y -qq tzdata ca-certificates && \
    ln -fs /usr/share/zoneinfo/$TZ /etc/localtime && \
    dpkg-reconfigure -f noninteractive tzdata && \
    rm -rf /var/lib/apt/lists/*

# Copiar el JAR construido
COPY --from=build /app/target/*.jar app.jar

# Exponer puerto
EXPOSE 9092

# ✅ Healthcheck para monitoreo
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:9092/actuator/health || exit 1

# ✅ Comando de entrada con opciones JVM optimizadas
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Duser.timezone=America/Argentina/Buenos_Aires", \
    "-Dfile.encoding=UTF-8", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]