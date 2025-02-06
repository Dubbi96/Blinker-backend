# 베이스 이미지 설정
FROM openjdk:17-jdk

# Google Cloud SDK 설치 (필요 시)
RUN apt-get update && apt-get install -y curl unzip && \
    curl -O https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-cli-460.0.0-linux-x86_64.tar.gz && \
    tar -xvf google-cloud-cli-460.0.0-linux-x86_64.tar.gz && \
    ./google-cloud-sdk/install.sh --quiet && \
    ./google-cloud-sdk/bin/gcloud components install gsutil --quiet

# 애플리케이션 디렉터리 생성
WORKDIR /app

# Cloud Storage에서 SSL 인증서 다운로드 (빌드 타임에 실행)
RUN ./google-cloud-sdk/bin/gcloud storage cp gs://blinker-backend-ssl/client-keystore.p12 /app/config/client-keystore.p12

# JAR 파일 복사
COPY build/libs/blinker-backend.jar /app/app.jar

# 실행 권한 부여
RUN chmod +x /app/app.jar

# 컨테이너 실행 명령
CMD ["java", "-Djavax.net.ssl.keyStore=/app/config/client-keystore.p12", "-Djavax.net.ssl.keyStorePassword=certificate-keystore-password-123", "-jar", "/app/app.jar"]