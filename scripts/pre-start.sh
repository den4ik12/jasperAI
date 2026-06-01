#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
APP_JAR="$APP_DIR/jasper_ai.jar"
ENV_FILE="$APP_DIR/env"
LOG_DIR="$APP_DIR/logs"
LOG_FILE="$LOG_DIR/jasperai.log"

fail() {
  code="$1"
  resolver="$2"

  echo "JASPERAI_ERROR_CODE=$code" >&2
  echo "JASPERAI_ERROR_RESOLVER=$resolver" >&2
  if [ -n "${LOG_FILE:-}" ] && [ -f "$LOG_FILE" ]; then
    echo "JASPERAI_LOG_FILE=$LOG_FILE" >&2
  fi
  exit 1
}

if [ ! -f "$APP_JAR" ]; then
  fail \
    "APP_JAR_NOT_FOUND" \
    "Выведи на экран пользователю: в корне дистрибутива нет jasper_ai.jar. Нужно положить готовый jasper_ai.jar по указанному пути. Останови сценарий запуска. Не собирай проект, не запускай Maven, не скачивай jar и не придумывай альтернативный путь."
fi

if [ ! -s "$APP_JAR" ]; then
  fail \
    "APP_JAR_EMPTY" \
    "Выведи на экран пользователю: jasper_ai.jar пустой или поврежден. Нужно заменить файл готовым корректным jar по указанному пути. Останови сценарий запуска. Не собирай проект, не запускай Maven и не скачивай jar."
fi

if [ ! -f "$ENV_FILE" ]; then
  if ! mkdir -p "$(dirname "$ENV_FILE")"; then
    fail \
      "ENV_DIR_CREATE_FAILED" \
      "Выведи на экран пользователю: не удалось подготовить место для env-файла. Нужно проверить права на папку дистрибутива. Останови сценарий запуска."
  fi

  fail \
    "ENV_FILE_MISSING" \
    "Выведи на экран пользователю: перед запуском нужно создать ./env. Спроси провайдера DeepSeek или GigaChat и API-ключ. После ответа создай ./env по шаблону из SKILL.md. Не запускай start.sh до повторного успешного ./scripts/pre-start.sh."
fi

set -a
# shellcheck disable=SC1090
if ! . "$ENV_FILE"; then
  set +a
  fail \
    "ENV_FILE_LOAD_FAILED" \
    "Выведи на экран пользователю: ./env содержит некорректный shell-синтаксис. Нужно исправить файл или разрешить пересоздать его по шаблону из SKILL.md. Не запускай start.sh."
fi
set +a

provider="${SPRING_AI_MODEL_CHAT:-deepseek}"
case "$provider" in
  deepseek)
    if [ -z "${DEEPSEEK_API_KEY:-}" ]; then
      fail \
        "DEEPSEEK_API_KEY_MISSING" \
        "Выведи на экран пользователю: для DeepSeek не заполнен DEEPSEEK_API_KEY. Спроси API-ключ и обнови ./env по шаблону DeepSeek из SKILL.md. Не запускай start.sh до повторного успешного ./scripts/pre-start.sh."
    fi
    ;;
  gigachat)
    if [ -z "${GIGACHAT_API_KEY:-}" ]; then
      fail \
        "GIGACHAT_API_KEY_MISSING" \
        "Выведи на экран пользователю: для GigaChat не заполнен GIGACHAT_API_KEY. Спроси API-ключ и обнови ./env по шаблону GigaChat из SKILL.md. Не запускай start.sh до повторного успешного ./scripts/pre-start.sh."
    fi
    ;;
  *)
    fail \
      "SPRING_AI_MODEL_CHAT_UNSUPPORTED" \
      "Выведи на экран пользователю: выбран неподдерживаемый провайдер. Спроси, использовать DeepSeek или GigaChat, затем обнови ./env по соответствующему шаблону из SKILL.md. Не запускай start.sh до повторного успешного ./scripts/pre-start.sh."
    ;;
esac

if ! mkdir -p "$LOG_DIR"; then
  fail \
    "LOG_DIR_CREATE_FAILED" \
    "Выведи на экран пользователю: папку logs нельзя создать. Нужно проверить, не лежит ли файл по пути ./logs, и права на папку дистрибутива. Не запускай start.sh."
fi

if ! touch "$LOG_FILE"; then
  fail \
    "LOG_FILE_WRITE_FAILED" \
    "Выведи на экран пользователю: лог-файл недоступен для записи. Нужно проверить права на ./logs и ./logs/jasperai.log. Не запускай start.sh."
fi

if ! command -v java >/dev/null 2>&1; then
  fail \
    "JAVA_NOT_FOUND" \
    "Выведи на экран пользователю: Java не найдена в PATH. Нужно скачать и установить Java 17+ из User Software, затем повторить запуск. Не скачивай и не устанавливай Java самостоятельно. Не запускай start.sh."
fi

java_version="$(java -version 2>&1 | awk -F '"' '/version/ {print $2; exit}')"
java_major="$(printf '%s' "$java_version" | awk -F. '{ if ($1 == "1") print $2; else print $1 }')"
if [ -z "$java_major" ] || [ "$java_major" -lt 17 ]; then
  fail \
    "JAVA_VERSION_UNSUPPORTED" \
    "Выведи на экран пользователю: текущая версия Java не подходит, нужна Java 17+. Нужно скачать и установить Java 17+ из User Software, затем повторить запуск. Не скачивай и не устанавливай Java самостоятельно. Не запускай start.sh."
fi

echo "JASPERAI_PRE_START_STATUS=OK"
echo "JASPERAI_PRE_START_MESSAGE=Проверки перед запуском JasperAI успешно пройдены."
