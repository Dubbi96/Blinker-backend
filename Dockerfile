# 1단계: 빌드 단계
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# 2단계: 런타임 환경
FROM eclipse-temurin:17-jdk AS runtime
WORKDIR /app

# Cloud SQL Auth Proxy 다운로드
ADD https://dl.google.com/cloudsql/cloud_sql_proxy.linux.amd64 /cloud_sql_proxy
RUN chmod +x /cloud_sql_proxy

# 서비스 계정 키 복사 (절대 경로 주의!)
COPY src/main/resources/key/blinker-backend-key.json /app/service-account-key.json
RUN chmod 600 /app/service-account-key.json

# JAR 파일 및 인증서 복사
COPY --from=builder /app/target/Blinker-1.0.0.jar /app/app.jar
COPY key/client-keystore.p12 /app/config/client-keystore.p12

# 실행 권한 부여
RUN chmod 600 /app/config/client-keystore.p12

# startup.sh 유지 (기존 방식과 호환)
COPY startup.sh /app/startup.sh
RUN chmod +x /app/startup.sh

EXPOSE 8080

# startup.sh 실행 (기존 방식 유지)
CMD ["java", "-Dspring.profiles.active=prod", "-jar", "/app/app.jar"]