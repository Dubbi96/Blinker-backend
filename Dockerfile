# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS stage1
WORKDIR /opt/app
COPY pom.xml .
COPY ./src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jdk
WORKDIR /opt/app

# 빌드된 JAR 파일 복사
COPY --from=stage1 /opt/app/target/Blinker-1.0.0.jar /opt/app/Blinker-1.0.0.jar

# 환경 변수 설정
ENV GOOGLE_APPLICATION_CREDENTIALS=/opt/app/key/blinker-backend-key.json
ENV PORT=8080

# 컨테이너 실행 시 JAR 실행
EXPOSE 8080
CMD ["sh", "-c", "java -jar /opt/app/Blinker-1.0.0.jar --server.port=${PORT}"]
