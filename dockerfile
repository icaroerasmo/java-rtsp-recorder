# syntax=docker/dockerfile:1
FROM --platform=$BUILDPLATFORM maven:3.9.9-eclipse-temurin-21 AS build

ARG TARGETPLATFORM
ARG BUILDPLATFORM
WORKDIR /app
COPY src ./src/
COPY pom.xml .
RUN mvn clean package -DskipTests

FROM archlinux:latest
WORKDIR /app
RUN pacman -Sy --noconfirm --needed \
    ffmpeg \
    jre21-openjdk-headless \
    rclone \
    translate-shell \
    tzdata \
    && pacman -Scc --noconfirm
RUN mkdir -p /app/config /app/data/tmp /app/data/records

ARG TZ
ENV TZ $TZ

COPY --from=build /app/target/java-rtsp-recorder-*.jar /app/java-rtsp-recorder.jar
ENTRYPOINT [ "java", "-Dspring.config.additional-location=optional:/app/config/config.yaml", "-jar", "/app/java-rtsp-recorder.jar" ]
