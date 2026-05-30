# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
RUN mvn -B -DskipTests -pl api -am package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/api/target/codemate-review-api-*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-XX:+UseZGC","-jar","/app/app.jar"]
