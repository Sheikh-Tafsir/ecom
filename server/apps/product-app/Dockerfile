FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Copy all subproject build files for dependency caching
COPY contracts/build.gradle contracts/build.gradle
COPY common/build.gradle common/build.gradle
COPY modules/product/build.gradle modules/product/build.gradle
COPY apps/product-app/build.gradle apps/product-app/build.gradle

RUN sed -i 's/\r$//' gradlew
RUN chmod +x gradlew

RUN ./gradlew dependencies --no-daemon \
    -Dorg.gradle.internal.http.connectionTimeout=120000 \
    -Dorg.gradle.internal.http.socketTimeout=120000

# Copy all source
COPY contracts/src contracts/src
COPY common/src common/src
COPY modules/product/src modules/product/src
COPY apps/product-app/src apps/product-app/src

RUN ./gradlew :apps:product-app:bootJar -x test --no-daemon


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache ca-certificates \
    && update-ca-certificates

RUN addgroup -S spring \
    && adduser -S spring -G spring

RUN mkdir -p /app/logs/server \
    && chown -R spring:spring /app/logs

USER spring:spring

COPY --from=build /app/apps/product-app/build/libs/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-Xmx320m", "-XX:MaxMetaspaceSize=128m", "-XX:+UseContainerSupport", "-XX:+UseG1GC", "-jar", "app.jar"]
