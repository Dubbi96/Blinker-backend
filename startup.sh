#!/bin/bash

echo "Cloud Storage에서 SSL 인증서 다운로드 시작"

# /app/config 디렉토리 생성
mkdir -p /app/config

echo "SSL 인증서 다운로드 완료!"

# 🛠️ SSL 인증서 검증 (문제가 발생하면 로그 출력)
echo "SSL 인증서 확인..."
keytool -list -keystore /app/config/client-keystore.p12 -storepass certificate-keystore-password-123 -storetype PKCS12 || {
    echo "⚠️ SSL 인증서 검증 실패! 경로 및 파일 확인 필요."
    exit 1
}

# 🚀 애플리케이션 실행
echo "애플리케이션 시작..."
java -Djavax.net.ssl.keyStore=/app/config/client-keystore.p12 \
     -Djavax.net.ssl.keyStorePassword=certificate-keystore-password-123 \
     -jar /app/app.jar