#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
LOG_DIR="$APP_DIR/logs"
PORT="${JASPERAI_PORT:-8080}"

fail() {
  code="$1"
  resolver="$2"

  echo "JASPERAI_ERROR_CODE=$code" >&2
  echo "JASPERAI_ERROR_RESOLVER=$resolver" >&2
  exit 1
}

clear_logs() {
  if [ ! -d "$LOG_DIR" ]; then
    echo "JASPERAI_LOG_CLEANUP_STATUS=SKIPPED"
    echo "JASPERAI_LOG_CLEANUP_MESSAGE=Папка логов не найдена: $LOG_DIR"
    return 0
  fi

  for log_file in "$LOG_DIR/jasperai.log" "$LOG_DIR/start-command.log"; do
    if [ -f "$log_file" ] && ! : > "$log_file"; then
      fail \
        "LOG_CLEANUP_FAILED" \
        "Выведи на экран пользователю: не удалось очистить файл логов $log_file. Нужно проверить права на файл и папку logs."
    fi
  done

  echo "JASPERAI_LOG_CLEANUP_STATUS=CLEARED"
  echo "JASPERAI_LOG_CLEANUP_MESSAGE=Логи JasperAI очищены."
}

if ! command -v lsof >/dev/null 2>&1; then
  fail \
    "LSOF_NOT_FOUND" \
    "Выведи на экран пользователю: команда lsof недоступна, поэтому автоматическая остановка JasperAI невозможна. Нужно установить lsof или вручную завершить процесс на порту $PORT. Не придумывай альтернативные команды остановки."
fi

pids="$(lsof -tiTCP:"$PORT" -sTCP:LISTEN || true)"

if [ -z "$pids" ]; then
  clear_logs
  echo "JASPERAI_STOP_STATUS=NOT_RUNNING"
  echo "JASPERAI_STOP_MESSAGE=Порт $PORT свободен. JasperAI не запущен."
  exit 0
fi

echo "JASPERAI_STOP_STATUS=STOPPING"
echo "JASPERAI_STOP_MESSAGE=Завершаю процесс на порту $PORT."
lsof -nP -iTCP:"$PORT" -sTCP:LISTEN || true

for pid in $pids; do
  if ! kill "$pid"; then
    fail \
      "PROCESS_KILL_FAILED" \
      "Выведи на экран пользователю: процесс $pid на порту $PORT не удалось завершить. Нужно проверить права пользователя или завершить процесс вручную. Не повторяй kill бесконечно."
  fi
done

sleep 2

remaining_pids="$(lsof -tiTCP:"$PORT" -sTCP:LISTEN || true)"
if [ -n "$remaining_pids" ]; then
  fail \
    "PROCESS_STILL_RUNNING" \
    "Выведи на экран пользователю: после остановки порт $PORT все еще занят процессом $remaining_pids. Нужно проверить процесс вручную. Останови сценарий."
fi

clear_logs
echo "JASPERAI_STOP_STATUS=STOPPED"
echo "JASPERAI_STOP_MESSAGE=Порт $PORT свободен. JasperAI остановлен."
