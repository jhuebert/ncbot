# Pre-built JAR runtime image
# The JAR must be built externally first (e.g. via ./build.sh or ./gradlew build)
FROM eclipse-temurin:25.0.3_9-jre-alpine
WORKDIR /app
RUN mkdir -p /data && addgroup -S ncbot && adduser -S ncbot -G ncbot
COPY build/libs/ncbot-*.jar app.jar
EXPOSE 8080
USER ncbot
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.datasource.url=jdbc:sqlite:/data/ncbot.db"]
