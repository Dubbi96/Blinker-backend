# 1단계: 빌드 단계
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# 2단계: 런타임 환경
FROM eclipse-temurin:17-jdk AS runtime
WORKDIR /app

# JAR 파일 및 인증서 복사
COPY --from=builder /app/target/Blinker-1.0.0.jar /app/app.jar
COPY src/main/resources/key/client-keystore.p12 /app/config/client-keystore.p12

# 실행 권한 부여
RUN chmod 600 /app/config/client-keystore.p12

# startup.sh 유지 (기존 방식과 호환)
COPY startup.sh /app/startup.sh
RUN chmod +x /app/startup.sh

EXPOSE 8080

# startup.sh 실행 (기존 방식 유지)
CMD ["/bin/bash", "/app/startup.sh"]