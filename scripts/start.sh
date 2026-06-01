#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
APP_JAR="$APP_DIR/jasper_ai.jar"
ENV_FILE="$APP_DIR/env"
LOG_DIR="$APP_DIR/logs"
LOG_FILE="$LOG_DIR/jasperai.log"

fail_with_log_file() {
  code="$1"
  resolver="$2"

  echo "JASPERAI_ERROR_CODE=$code" >&2
  echo "JASPERAI_ERROR_RESOLVER=$resolver" >&2
  echo "JASPERAI_LOG_FILE=$LOG_FILE" >&2
  exit 1
}

set -a
# shellcheck disable=SC1090
. "$ENV_FILE"
set +a

provider="${SPRING_AI_MODEL_CHAT:-deepseek}"

mkdir -p "$LOG_DIR"
touch "$LOG_FILE"

echo "Запускаю JasperAI. Логи: $LOG_FILE"
set +e
{
  echo
  echo "===== JasperAI start: $(date -u '+%Y-%m-%dT%H:%M:%SZ') ====="
  echo "APP_JAR=$APP_JAR"
  echo "SPRING_AI_MODEL_CHAT=$provider"
  java -jar "$APP_JAR"
} >> "$LOG_FILE" 2>&1
exit_code=$?
set -e

if [ "$exit_code" -ne 0 ]; then
  fail_with_log_file \
    "APP_START_FAILED" \
    "Выведи на экран пользователю: JasperAI завершился с ошибкой запуска. Не печатай содержимое лога. Нужно передать файл '$LOG_FILE' разработчику Денису Володину. Останови сценарий запуска."
fi
