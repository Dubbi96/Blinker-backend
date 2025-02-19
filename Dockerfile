# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS stage1
WORKDIR /opt/app
COPY pom.xml .
COPY ./src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jdk
WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=stage1 /opt/app/target/Blinker-1.0.0.jar /app/Blinker-1.0.0.jar

# 서비스 계정 키 파일 복사
#COPY blinker-backend-key.json /app/blinker-backend-key.json

# 환경 변수 설정
#ENV GOOGLE_APPLICATION_CREDENTIALS=/app/blinker-backend-key.json
COPY blinker-backend-key.json /app/blinker-backend-key.json
ENV PORT=8080

# Cloud Run에서 자동 할당된 포트 사용하도록 설정
CMD ["java", "-jar", "/app/Blinker-1.0.0.jar"]
