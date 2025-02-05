# 1단계: 빌드 단계
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# 2단계: 런타임 환경
FROM eclipse-temurin:17-jdk AS runtime
WORKDIR /app

# startup.sh 및 JAR 파일 복사
COPY --from=builder /app/target/Blinker-1.0.0.jar /app/app.jar
COPY startup.sh /app/startup.sh
RUN chmod +x /app/startup.sh

EXPOSE 8080

# 애플리케이션 실행 (startup.sh 실행)
CMD ["/bin/bash", "/app/startup.sh"]