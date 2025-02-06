#!/bin/bash

echo "Cloud Storage에서 SSL 인증서 다운로드 시작"

# /app/config 디렉토리 생성
mkdir -p /app/config

# 🛠️ Google Cloud SDK 설치 및 gsutil 설정
if ! command -v gsutil &> /dev/null
then
    echo "gsutil이 설치되지 않음. 설치 진행..."

    # Google Cloud SDK 설치 (Debian/Ubuntu 기반)
    apt-get update && apt-get install -y curl apt-transport-https ca-certificates gnupg

    # Google Cloud SDK 공식 GPG 키 추가
    curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key add -

    # Google Cloud SDK 저장소 추가
    echo "deb https://packages.cloud.google.com/apt cloud-sdk main" | tee -a /etc/apt/sources.list.d/google-cloud-sdk.list

    # Google Cloud SDK 설치
    apt-get update && apt-get install -y google-cloud-sdk

    echo "gsutil 설치 완료!"
fi

# ✅ Cloud Storage에서 client-keystore.p12 다운로드
echo "gsutil을 사용하여 인증서 다운로드 중..."
gsutil cp gs://blinker-backend-ssl/client-keystore.p12 /app/config/client-keystore.p12

# 파일 권한 변경
chmod 600 /app/config/client-keystore.p12

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