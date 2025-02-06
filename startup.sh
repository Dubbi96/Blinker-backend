#!/bin/bash

echo "Cloud Storage에서 SSL 인증서 다운로드"

# /app/config 디렉토리 생성
mkdir -p /app/config

# gsutil 설치 확인
if ! command -v gsutil &> /dev/null
then
    echo "gsutil이 설치되지 않음. 설치 진행..."
    apt-get update && apt-get install google-cloud-sdk -y
fi

# Cloud Storage에서 client-keystore.p12 다운로드
echo "gsutil을 사용하여 인증서 다운로드 중..."
gsutil cp gs://blinker-backend-ssl/client-keystore.p12 /app/config/client-keystore.p12

# 파일 권한 변경
chmod 600 /app/config/client-keystore.p12

echo "SSL 인증서 다운로드 완료!"

# SSL 인증서 검증
echo "SSL 인증서 확인..."
keytool -list -keystore /app/config/client-keystore.p12 -storepass certificate-keystore-password-123 -storetype PKCS12

# 애플리케이션 실행
echo "애플리케이션 시작..."
java -Djavax.net.ssl.keyStore=/app/config/client-keystore.p12 \
     -Djavax.net.ssl.keyStorePassword=certificate-keystore-password-123 \
     -jar /app/app.jar