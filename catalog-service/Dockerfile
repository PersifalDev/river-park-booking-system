FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY . .
RUN chmod +x mvnw || true
RUN ./mvnw -pl catalog-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/catalog-service/target/*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "/app/app.jar"]