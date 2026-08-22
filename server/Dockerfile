FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN sed -i 's/\r$//' gradlew
RUN chmod +x gradlew

RUN ./gradlew dependencies --no-daemon \
    -Dorg.gradle.internal.http.connectionTimeout=120000 \
    -Dorg.gradle.internal.http.socketTimeout=120000

COPY src src

RUN ./gradlew bootJar --no-daemon


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache ca-certificates \
    && update-ca-certificates

RUN addgroup -S spring \
    && adduser -S spring -G spring

RUN mkdir -p /app/logs/server \
    && chown -R spring:spring /app/logs

USER spring:spring

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
