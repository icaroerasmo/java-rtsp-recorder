FROM maven:3.8.8-amazoncorretto-21 AS build
WORKDIR /app
COPY src ./src/
COPY pom.xml .
RUN mvn clean package

FROM amazoncorretto:21-alpine
RUN mkdir -p /app/data/tmp /app/data/records
RUN apk add --no-cache rclone
RUN apk add --no-cache translate-shell
RUN apk add --no-cache ffmpeg
COPY --from=build /app/target/java-rtsp-recorder-*.jar /app/java-rtsp-recorder.jar
RUN ls -la /app
ENTRYPOINT [ "java", "-Dspring.config.additional-location=optional:/app/config/config.yaml", "-jar", "/app/java-rtsp-recorder.jar" ]
