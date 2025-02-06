#!/bin/bash

echo "🔹 환경 변수에서 SSL 인증서 복구 시작..."

# SSL 인증서 복구 (환경 변수에서 Base64 디코딩)
echo $SSL_SERVER_CA_CERT | base64 --decode > /app/config/server-ca-final.pem
chmod 600 /app/config/server-ca-final.pem

echo "✅ SSL 인증서 복구 완료!"

# 5초 대기 (SSL 안정화)
sleep 5

# 애플리케이션 실행
echo "🚀 애플리케이션 시작..."
java -Djavax.net.ssl.keyStore=/app/config/client-keystore.p12 \
     -Djavax.net.ssl.keyStorePassword=certificate-keystore-password-123 \
     -jar /opt/app/Blinker-1.0.0.jar