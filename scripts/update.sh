#!/usr/bin/env bash
set -euo pipefail

RELEASE_JAR_URL="https://github.com/den4ik12/jasperAI/releases/latest/download/app.jar"

APP_DIR="$HOME/.jasperai"
APP_JAR="$APP_DIR/app.jar"

fail() {
  code="$1"
  message="$2"
  next_step="$3"

  echo "JASPERAI_ERROR_CODE=$code" >&2
  echo "JASPERAI_ERROR_MESSAGE=$message" >&2
  echo "JASPERAI_NEXT_STEP=$next_step" >&2
  exit 1
}

if ! command -v curl >/dev/null 2>&1; then
  fail \
    "CURL_NOT_FOUND" \
    "curl не установлен или недоступен в PATH." \
    "Установите curl, затем снова выполните ./scripts/update.sh."
fi

if ! mkdir -p "$APP_DIR"; then
  fail \
    "APP_DIR_CREATE_FAILED" \
    "Не удалось создать папку JasperAI: $APP_DIR" \
    "Проверьте права на HOME или создайте папку вручную: mkdir -p '$APP_DIR'"
fi

if [ ! -w "$APP_DIR" ]; then
  fail \
    "APP_DIR_NOT_WRITABLE" \
    "Папка JasperAI недоступна для записи: $APP_DIR" \
    "Исправьте права на папку, затем снова выполните ./scripts/update.sh."
fi

if ! curl -fL "$RELEASE_JAR_URL" -o "$APP_JAR"; then
  fail \
    "APP_JAR_DOWNLOAD_FAILED" \
    "Не удалось скачать app.jar из GitHub Releases: $RELEASE_JAR_URL" \
    "Проверьте интернет и наличие app.jar в последнем GitHub Release, затем снова выполните ./scripts/update.sh."
fi

if [ ! -s "$APP_JAR" ]; then
  fail \
    "APP_JAR_EMPTY" \
    "Скачанный app.jar отсутствует или пустой: $APP_JAR" \
    "Удалите битый файл и снова выполните ./scripts/update.sh."
fi

echo "JasperAI успешно обновлен: $APP_JAR"
