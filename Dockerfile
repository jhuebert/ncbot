# Pre-built JAR runtime image
# The JAR must be built externally first (e.g. via ./build.sh or ./gradlew build)
FROM eclipse-temurin:25.0.3_9-jre-alpine
WORKDIR /app
RUN mkdir -p /data && addgroup -S ncbot && adduser -S ncbot -G ncbot && chown -R ncbot:ncbot /data
COPY build/libs/ncbot.jar app.jar
EXPOSE 8080
USER ncbot
# --enable-native-access=ALL-UNNAMED silences sqlite-jdbc's restricted-method
# warning (System::load) and is required for it to keep working in future JVMs
# where restricted native access is blocked by default.
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "app.jar", "--spring.datasource.url=jdbc:sqlite:/data/ncbot.db"]
