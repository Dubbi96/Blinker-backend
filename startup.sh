#!/bin/bash

echo "환경 변수에서 SSL Keystore & 서비스 계정 키 복구 시작..."

# SSL Keystore 복구
echo $SSL_KEYSTORE_BASE64 | base64 --decode > /opt/app/config/client-keystore.p12
chmod 600 /opt/app/config/client-keystore.p12

# 서비스 계정 키 복구
echo $SERVICE_ACCOUNT_KEY_BASE64 | base64 --decode > /opt/app/config/service-account-key.json
chmod 600 /opt/app/config/service-account-key.json

echo "✅ SSL Keystore & 서비스 계정 키 복구 완료!"

# Cloud SQL Auth Proxy 실행
/cloud_sql_proxy -instances=blinker-db:asia-northeast3:blinker-atom=tcp:5432 -credential_file=/opt/app/config/service-account-key.json &

# 5초 대기 (Cloud SQL Auth Proxy 안정화)
sleep 5

# 애플리케이션 실행
echo "애플리케이션 시작..."
java -Djavax.net.ssl.keyStore=/opt/app/config/client-keystore.p12 \
     -Djavax.net.ssl.keyStorePassword=certificate-keystore-password-123 \
     -jar /opt/app/ATS-API-1.0.0.jar