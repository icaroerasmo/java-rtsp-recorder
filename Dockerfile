FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY src ./src/
COPY pom.xml .
RUN mvn clean package -DskipTests

FROM archlinux:latest
WORKDIR /app

RUN pacman -Syu --noconfirm && \
    pacman -S --noconfirm --needed \
    jre21-openjdk-headless \
    ffmpeg \
    rclone \
    tzdata && \
    pacman -Scc --noconfirm

RUN mkdir -p /app/data/records /app/data/tmp /app/config

COPY --from=build /app/target/java-rtsp-recorder-*.jar /app/java-rtsp-recorder.jar

ENTRYPOINT [ "java", "-Dspring.config.additional-location=/app/config/config.yaml", "-jar", "/app/java-rtsp-recorder.jar" ]
