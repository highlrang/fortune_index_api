FROM gradle:8.10.2-jdk17-alpine AS builder

WORKDIR /workspace

COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradlew .
COPY gradle ./gradle
COPY libs ./libs

RUN chmod +x ./gradlew
RUN ./gradlew --no-daemon dependencies || true

COPY src ./src

RUN ./gradlew clean bootJar -x test --no-daemon
RUN cp build/libs/*.jar app.jar

FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime \
    && echo "Asia/Seoul" > /etc/timezone

RUN addgroup -S spring && adduser -S spring -G spring
RUN mkdir -p /data/logs/archive && chown -R spring:spring /data/logs
USER spring:spring

EXPOSE 8080

ENV TZ=Asia/Seoul
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8
ENV JAVA_OPTS="-Duser.timezone=Asia/Seoul -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=25"
ENV LOG_PATH=/data/logs
ENV SPRING_PROFILES_ACTIVE=dev

COPY --from=builder /workspace/app.jar /app/app.jar

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
