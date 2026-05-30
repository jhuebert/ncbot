# ---- Builder stage ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew build -x test --no-daemon

# ---- Runtime stage ----
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN mkdir -p /data && addgroup -S ncbot && adduser -S ncbot -G ncbot
COPY --from=build /app/build/libs/ncbot-*.jar app.jar
EXPOSE 8080
USER ncbot
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.datasource.url=jdbc:sqlite:/data/ncbot.db"]
