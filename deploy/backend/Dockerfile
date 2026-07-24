FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY aurora-server.jar app.jar

EXPOSE 8800

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
