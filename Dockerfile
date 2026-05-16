FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy only what we need for a build to leverage Docker cache
COPY pom.xml mvnw ./
COPY src src

RUN mvn -DskipTests -B package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
