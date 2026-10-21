FROM maven:3.9.16-eclipse-temurin-25 AS build
WORKDIR /build

# Dependencies change less often than the code, so they get their own layer.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Unit and web tests run here; the Testcontainers tests skip themselves without Docker.
RUN mvn -B package

FROM eclipse-temurin:25-jre

RUN groupadd --system --gid 10001 archmaxim \
    && useradd --system --uid 10001 --gid archmaxim --home-dir /app --shell /usr/sbin/nologin archmaxim \
    && mkdir -p /data/storage \
    && chown archmaxim:archmaxim /data/storage

WORKDIR /app
COPY --from=build /build/target/archmaxim-*.jar app.jar

USER archmaxim
ENV ARCHMAXIM_STORAGE_LOCATION=/data/storage
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
