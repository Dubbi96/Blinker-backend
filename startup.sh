#!/bin/bash

echo "Cloud Storage에서 SSL 인증서 다운로드"

# Cloud Storage에서 client-keystore.p12 다운로드
# 디렉토리 생성
mkdir -p /app/config
gsutil cp gs://blinker-backend-ssl/client-keystore.p12 /app/config/client-keystore.p12

# 파일 권한 변경 (읽기 가능하도록)
chmod 600 /app/config/client-keystore.p12

echo "SSL 인증서 다운로드 완료!"

# 애플리케이션 실행
echo "애플리케이션 시작..."
java -Djavax.net.ssl.keyStore=/app/config/client-keystore.p12 \
     -Djavax.net.ssl.keyStorePassword=certificate-keystore-password-123 \
     -jar /app/app.jar