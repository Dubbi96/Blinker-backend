#!/bin/bash

echo "환경 변수에서 SSL Keystore & 서비스 계정 키 복구 시작..."

# 🔹 환경 변수 값 확인 (디버깅용)
echo "SSL_KEYSTORE_BASE64 길이: ${#SSL_KEYSTORE_BASE64}"
echo "SERVICE_ACCOUNT_KEY_BASE64 길이: ${#SERVICE_ACCOUNT_KEY_BASE64}"

# 🔹 /opt/app/config 폴더가 없으면 생성
mkdir -p /opt/app/config

# 🔹 SSL Keystore 복구
if [[ -n "$SSL_KEYSTORE_BASE64" ]]; then
  echo "$SSL_KEYSTORE_BASE64" | base64 --decode > /opt/app/config/client-keystore.p12
  chmod 600 /opt/app/config/client-keystore.p12
  echo "✅ SSL Keystore 복구 완료!"
else
  echo "⚠️ SSL_KEYSTORE_BASE64 값이 존재하지 않습니다. 환경 변수 확인 필요."
fi

# 🔹 서비스 계정 키 복구
if [[ -n "$SERVICE_ACCOUNT_KEY_BASE64" ]]; then
  echo "$SERVICE_ACCOUNT_KEY_BASE64" | base64 --decode > /opt/app/config/service-account-key.json
  chmod 600 /opt/app/config/service-account-key.json
  echo "✅ 서비스 계정 키 복구 완료!"
else
  echo "⚠️ SERVICE_ACCOUNT_KEY_BASE64 값이 존재하지 않습니다. 환경 변수 확인 필요."
fi

# 🔹 Cloud SQL Auth Proxy 다운로드 (없을 경우)
if [[ ! -f "/opt/app/cloud_sql_proxy" ]]; then
  echo "🚀 Cloud SQL Auth Proxy 다운로드 중..."
  curl -o /opt/app/cloud_sql_proxy \
    https://storage.googleapis.com/cloudsql-proxy/v1.33.3/cloud_sql_proxy.linux.amd64
  chmod +x /opt/app/cloud_sql_proxy
fi

# 🔹 Cloud SQL Auth Proxy 실행
/opt/app/cloud_sql_proxy -instances=blinker-db:asia-northeast3:blinker-atom=tcp:5432 \
  -credential_file=/opt/app/config/service-account-key.json &

# 5초 대기 (Cloud SQL Auth Proxy 안정화)
sleep 5

# 🚀 애플리케이션 실행
echo "🚀 애플리케이션 시작..."
java -Djavax.net.ssl.keyStore=/opt/app/config/client-keystore.p12 \
     -Djavax.net.ssl.keyStorePassword=certificate-keystore-password-123 \
     -jar /opt/app/Blinker-1.0.0.jar