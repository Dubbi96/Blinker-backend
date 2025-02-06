# 1단계: 빌드 단계
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# 2단계: 런타임 환경
FROM eclipse-temurin:17-jdk AS runtime
WORKDIR /app

# Cloud SDK 설치 (gsutil 포함)
RUN apt-get update && apt-get install -y curl unzip && \
    curl -O https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-cli-460.0.0-linux-x86_64.tar.gz && \
    tar -xvf google-cloud-cli-460.0.0-linux-x86_64.tar.gz && \
    ./google-cloud-sdk/install.sh --quiet && \
    ./google-cloud-sdk/bin/gcloud components install gsutil --quiet

# startup.sh 및 JAR 파일 복사
COPY --from=builder /app/target/Blinker-1.0.0.jar /app/app.jar
COPY startup.sh /app/startup.sh
RUN chmod +x /app/startup.sh

EXPOSE 8080

# 애플리케이션 실행 (startup.sh 실행)
CMD ["/bin/bash", "/app/startup.sh"]