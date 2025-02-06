#!/bin/bash

echo "🔹 환경 변수에서 SSL 인증서 및 서비스 계정 키 복구 시작..."

# SSL Keystore 복구
echo $SSL_KEYSTORE_BASE64 | base64 --decode > /opt/app/config/client-keystore.p12
chmod 600 /opt/app/config/client-keystore.p12

# 서비스 계정 키 복구
echo $SERVICE_ACCOUNT_KEY_BASE64 | base64 --decode > /opt/app/config/service-account-key.json
chmod 600 /opt/app/config/service-account-key.json

# SSL Server CA 인증서 복구
echo $SSL_SERVER_CA_CERT | base64 --decode > /opt/app/config/server-ca-final.pem
chmod 600 /opt/app/config/server-ca-final.pem

echo "✅ SSL 인증서 및 서비스 계정 키 복구 완료!"

# 5초 대기 (SSL 안정화)
sleep 5

# 애플리케이션 실행
echo "🚀 애플리케이션 시작..."
java -Djavax.net.ssl.keyStore=/opt/app/config/client-keystore.p12 \
     -Djavax.net.ssl.keyStorePassword=certificate-keystore-password-123 \
     -jar /opt/app/Blinker-1.0.0.jar