# 1단계: 빌드 단계 (Maven으로 JAR 빌드)
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# 2단계: 런타임 환경
FROM eclipse-temurin:17-jdk AS runtime
WORKDIR /app

# Cloud Storage에서 SSL 인증서 다운로드 (빌드 단계에서 수행)
RUN apt-get update && apt-get install -y curl unzip && \
    curl -O https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-cli-460.0.0-linux-x86_64.tar.gz && \
    tar -xvf google-cloud-cli-460.0.0-linux-x86_64.tar.gz && \
    ./google-cloud-sdk/install.sh --quiet && \
    ./google-cloud-sdk/bin/gcloud components install gsutil --quiet && \
    ./google-cloud-sdk/bin/gcloud storage cp gs://blinker-backend-ssl/client-keystore.p12 /app/config/client-keystore.p12

# JAR 파일 및 실행 스크립트 복사
COPY --from=builder /app/target/Blinker-1.0.0.jar /app/app.jar
COPY startup.sh /app/startup.sh
RUN chmod +x /app/startup.sh

EXPOSE 8080

# 애플리케이션 실행 (startup.sh 실행)
CMD ["/bin/bash", "/app/startup.sh"]