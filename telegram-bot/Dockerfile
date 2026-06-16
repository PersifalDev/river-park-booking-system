# syntax=docker/dockerfile:1.7
FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /app

COPY pom.xml ./
RUN mkdir -p common-libs catalog-service booking-service payment-service user-service telegram-bot notification-service
COPY common-libs/pom.xml common-libs/pom.xml
COPY catalog-service/pom.xml catalog-service/pom.xml
COPY booking-service/pom.xml booking-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY telegram-bot/pom.xml telegram-bot/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml

COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -pl telegram-bot -am package -Dmaven.test.skip=true

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/telegram-bot/target/*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
