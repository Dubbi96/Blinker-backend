# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS stage1
WORKDIR /opt/app
COPY pom.xml .
COPY ./src ./src
RUN mvn clean package

# Stage 2: Runtime
FROM eclipse-temurin:17-jdk
WORKDIR /opt/app

# 빌드된 JAR 파일 복사
COPY --from=stage1 /opt/app/target/Blinker-1.0.0.jar /opt/app/Blinker-1.0.0.jar

# 실행 스크립트 복사
COPY startup.sh /opt/app/startup.sh
RUN chmod +x /opt/app/startup.sh

EXPOSE 8080
CMD ["/bin/bash", "/opt/app/startup.sh"]