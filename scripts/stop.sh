#!/usr/bin/env bash
set -euo pipefail

PORT="${JASPERAI_PORT:-8080}"

fail() {
  code="$1"
  message="$2"
  next_step="$3"

  echo "JASPERAI_ERROR_CODE=$code" >&2
  echo "JASPERAI_ERROR_MESSAGE=$message" >&2
  echo "JASPERAI_NEXT_STEP=$next_step" >&2
  exit 1
}

if ! command -v lsof >/dev/null 2>&1; then
  fail \
    "LSOF_NOT_FOUND" \
    "lsof не установлен или недоступен в PATH." \
    "Установите lsof или завершите процесс на порту $PORT вручную."
fi

pids="$(lsof -tiTCP:"$PORT" -sTCP:LISTEN || true)"

if [ -z "$pids" ]; then
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
      "Не удалось завершить процесс $pid на порту $PORT." \
      "Проверьте права пользователя или завершите процесс вручную: kill $pid"
  fi
done

sleep 2

remaining_pids="$(lsof -tiTCP:"$PORT" -sTCP:LISTEN || true)"
if [ -n "$remaining_pids" ]; then
  fail \
    "PROCESS_STILL_RUNNING" \
    "После команды kill порт $PORT все еще занят процессом: $remaining_pids" \
    "Проверьте процесс вручную: lsof -nP -iTCP:$PORT -sTCP:LISTEN"
fi

echo "JASPERAI_STOP_STATUS=STOPPED"
echo "JASPERAI_STOP_MESSAGE=Порт $PORT свободен. JasperAI остановлен."
