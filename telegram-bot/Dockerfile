FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY . .
RUN chmod +x mvnw || true
RUN ./mvnw -pl telegram-bot -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/telegram-bot/target/*.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "/app/app.jar"]