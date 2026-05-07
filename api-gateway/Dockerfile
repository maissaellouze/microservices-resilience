FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copier le JAR construit par Maven
COPY target/*.jar app.jar

# JVM optimisée pour les conteneurs
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]

EXPOSE 8080
