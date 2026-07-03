FROM gradle:8.14.3-jdk21 AS build
WORKDIR /workspace
COPY . .
ARG MODULE
RUN gradle ":${MODULE}:bootJar" --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
ARG MODULE
COPY --from=build /workspace/${MODULE}/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
