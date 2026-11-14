FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Copy all subproject build files for dependency caching
COPY contracts/build.gradle contracts/build.gradle
COPY common/build.gradle common/build.gradle
COPY modules/module-user/build.gradle modules/module-user/build.gradle
COPY modules/module-product/build.gradle modules/module-product/build.gradle
COPY modules/module-order/build.gradle modules/module-order/build.gradle
COPY modules/module-payment/build.gradle modules/module-payment/build.gradle
COPY modules/module-cms/build.gradle modules/module-cms/build.gradle
COPY modules/module-report/build.gradle modules/module-report/build.gradle
COPY apps/app-monolith/build.gradle apps/app-monolith/build.gradle

RUN sed -i 's/\r$//' gradlew
RUN chmod +x gradlew

RUN ./gradlew dependencies --no-daemon \
    -Dorg.gradle.internal.http.connectionTimeout=120000 \
    -Dorg.gradle.internal.http.socketTimeout=120000

# Copy all source
COPY contracts/src contracts/src
COPY common/src common/src
COPY modules/ modules/
COPY apps/app-monolith/src apps/app-monolith/src

RUN ./gradlew :apps:app-monolith:bootJar -x test --no-daemon


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache ca-certificates \
    && update-ca-certificates

RUN addgroup -S spring \
    && adduser -S spring -G spring

RUN mkdir -p /app/logs/server \
    && chown -R spring:spring /app/logs

USER spring:spring

COPY --from=build /app/apps/app-monolith/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Xmx448m", "-XX:MaxMetaspaceSize=192m", "-XX:+UseContainerSupport", "-XX:+UseG1GC", "-jar", "app.jar"]
