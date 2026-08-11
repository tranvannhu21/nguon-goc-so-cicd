# syntax=docker/dockerfile:1

# ----------------------------------------------------------------------------
# Stage 1: Build - Maven 3.9.16 (khop maven-wrapper.properties) + JDK 21
# ----------------------------------------------------------------------------
FROM maven:3.9.16-eclipse-temurin-21 AS build

WORKDIR /workspace

# Copy pom.xml truoc, chay go-offline de cache Maven dependencies
# (tang nay chi thay doi khi pom.xml thay doi)
COPY pom.xml .
RUN mvn -B --no-transfer-progress dependency:go-offline

# Copy source roi build JAR (bo qua test - test chay o buoc CI rieng voi DB service)
COPY src ./src
RUN mvn -B --no-transfer-progress -DskipTests clean package \
    && set -- target/*.jar \
    && [ "$#" -eq 1 ] \
    && cp "$1" /workspace/app.jar

# ----------------------------------------------------------------------------
# Stage 2: Runtime - JRE 21 (Ubuntu jammy), chay bang non-root user
# ----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy

# mysql-client: cung cap binary `mysql` (restore) va `mysqldump` (backup)
# ma BackupServiceImpl/RestoreServiceImpl goi truc tiep qua ProcessBuilder
RUN apt-get update \
    && apt-get install -y --no-install-recommends mysql-client \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --system app \
    && useradd --system --gid app --home-dir /app --shell /usr/sbin/nologin app

WORKDIR /app

# Cac thu muc ung dung can quyen ghi (xem application.properties):
#   app.upload.base-dir=./uploads
#   qr.image.storage.path=./files/qr
#   app.backup.local-dir=./backups
RUN mkdir -p /app/uploads /app/files/qr /app/backups \
    && chown -R app:app /app

# Copy dung executable Spring Boot JAR, khong dung wildcard
COPY --from=build --chown=app:app /workspace/app.jar /app/app.jar

USER app

EXPOSE 8080

# JVM duoc cau hinh qua JAVA_TOOL_OPTIONS (JVM tu dong doc tu environment,
# vi du: -e JAVA_TOOL_OPTIONS="-Xmx512m -XX:MaxRAMPercentage=75.0")
# Toan bo cau hinh ung dung (DB_*, JWT_SECRET, MAIL_*, MYSQLDUMP_PATH, ...)
# nhan qua environment variables - khong hard-code secret nao trong image.

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD bash -c '</dev/tcp/127.0.0.1/8080'

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
