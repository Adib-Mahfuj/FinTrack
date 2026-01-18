FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/fintrack-0.0.1-SNAPSHOT.jar fintrack-v1.0.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "fintrack-v1.0.jar"]
