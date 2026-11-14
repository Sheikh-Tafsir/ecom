FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Copy all subproject build files for dependency caching
COPY contracts/build.gradle contracts/build.gradle
COPY common/build.gradle common/build.gradle
COPY modules/order/build.gradle modules/order/build.gradle
COPY apps/order-app/build.gradle apps/order-app/build.gradle

RUN sed -i 's/\r$//' gradlew
RUN chmod +x gradlew

RUN ./gradlew dependencies --no-daemon \
    -Dorg.gradle.internal.http.connectionTimeout=120000 \
    -Dorg.gradle.internal.http.socketTimeout=120000

# Copy all source
COPY contracts/src contracts/src
COPY common/src common/src
COPY modules/order/src modules/order/src
COPY apps/order-app/src apps/order-app/src

RUN ./gradlew :apps:order-app:bootJar -x test --no-daemon


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache ca-certificates \
    && update-ca-certificates

RUN addgroup -S spring \
    && adduser -S spring -G spring

RUN mkdir -p /app/logs/server \
    && chown -R spring:spring /app/logs

USER spring:spring

COPY --from=build /app/apps/order-app/build/libs/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-Xmx320m", "-XX:MaxMetaspaceSize=128m", "-XX:+UseContainerSupport", "-XX:+UseG1GC", "-jar", "app.jar"]
