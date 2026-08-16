FROM maven:3.8.8-amazoncorretto-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src/
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
ENV DEBIAN_FRONTEND=noninteractive
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg \
    rclone \
    tzdata \
    && rm -rf /var/lib/apt/lists/*

ARG TZ=UTC
ENV TZ=${TZ}
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN mkdir -p /app/data/records /app/config

COPY --from=build /app/target/java-rtsp-recorder-*.jar /app/java-rtsp-recorder.jar

ENTRYPOINT [ "java", "-Dspring.config.additional-location=/app/config/config.yaml", "-jar", "/app/java-rtsp-recorder.jar" ]
