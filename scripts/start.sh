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
  message="$2"
  next_step="$3"
  resolver="$4"

  echo "JASPERAI_ERROR_CODE=$code" >&2
  echo "JASPERAI_ERROR_MESSAGE=$message" >&2
  echo "JASPERAI_NEXT_STEP=$next_step" >&2
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
    "JasperAI завершился с ошибкой. Код выхода: $exit_code." \
    "Не печатайте лог приложения пользователю. Попросите пользователя передать файл логов '$LOG_FILE' разработчику Денису Володину." \
    "Сообщи пользователю, что JasperAI завершился с ошибкой запуска. Не печатай содержимое лога. Попроси пользователя передать файл '$LOG_FILE' разработчику Денису Володину и останови сценарий запуска."
fi
