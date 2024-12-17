FROM openjdk:17-jdk-slim

# Ustaw katalog roboczy
WORKDIR /app

# Skopiuj aplikację Spring Boot (plik JAR)
COPY target/shopapp.jar app.jar

# Uruchom aplikację
ENTRYPOINT ["java", "-jar", "app.jar"]
