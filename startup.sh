#!/bin/bash

echo "환경 변수에서 서비스 계정 키 & DB 인증서 복구 시작..."

# ✅ 서비스 계정 키 복구
echo $SERVICE_ACCOUNT_KEY_BASE64 | base64 --decode > /opt/app/config/service-account-key.json
chmod 600 /opt/app/config/service-account-key.json

# ✅ Cloud SQL SSL 인증서 복구
echo $SERVER_CA_BASE64 | base64 --decode > /opt/app/config/server-ca-final.pem
chmod 600 /opt/app/config/server-ca-final.pem

echo "✅ 모든 인증서 복구 완료!"

# 🚀 Cloud SQL Auth Proxy 실행
/cloud_sql_proxy -instances=blinker-db:asia-northeast3:blinker-atom=tcp:5432 -credential_file=/opt/app/config/service-account-key.json &

# 5초 대기 (Cloud SQL Auth Proxy 안정화)
sleep 5

# 애플리케이션 실행
echo "애플리케이션 시작..."
java -Djavax.net.ssl.keyStorePassword=certificate-keystore-password-123 \
     -Dspring.datasource.hikari.sslrootcert=/opt/app/config/server-ca-final.pem \
     -jar /opt/app/Blinker-1.0.0.jar