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
  message="$2"
  next_step="$3"
  resolver="$4"

  echo "JASPERAI_ERROR_CODE=$code" >&2
  echo "JASPERAI_ERROR_MESSAGE=$message" >&2
  echo "JASPERAI_NEXT_STEP=$next_step" >&2
  echo "JASPERAI_ERROR_RESOLVER=$resolver" >&2
  if [ -n "${LOG_FILE:-}" ] && [ -f "$LOG_FILE" ]; then
    echo "JASPERAI_LOG_FILE=$LOG_FILE" >&2
  fi
  exit 1
}

if [ ! -f "$APP_JAR" ]; then
  fail \
    "APP_JAR_NOT_FOUND" \
    "jasper_ai.jar для JasperAI не найден: $APP_JAR" \
    "Поместите jasper_ai.jar в '$APP_JAR', затем снова выполните ./scripts/pre-start.sh." \
    "Сообщи пользователю, что в корне дистрибутива нет jasper_ai.jar. Попроси пользователя положить готовый jasper_ai.jar по указанному пути и останови сценарий запуска. Не собирай проект, не запускай Maven, не скачивай jar и не придумывай альтернативный путь."
fi

if [ ! -s "$APP_JAR" ]; then
  fail \
    "APP_JAR_EMPTY" \
    "jasper_ai.jar для JasperAI пустой или битый: $APP_JAR" \
    "Замените jasper_ai.jar корректным файлом в '$APP_JAR', затем снова выполните ./scripts/pre-start.sh." \
    "Сообщи пользователю, что jasper_ai.jar пустой или поврежден. Попроси заменить файл готовым корректным jar по указанному пути и останови сценарий запуска. Не собирай проект, не запускай Maven и не скачивай jar."
fi

if [ ! -f "$ENV_FILE" ]; then
  if ! mkdir -p "$(dirname "$ENV_FILE")"; then
    fail \
      "ENV_DIR_CREATE_FAILED" \
      "Не удалось создать папку для env-файла JasperAI: $(dirname "$ENV_FILE")" \
      "Проверьте права на папку дистрибутива: $(dirname "$ENV_FILE")" \
      "Сообщи пользователю, что не удалось подготовить место для env-файла. Попроси проверить права на папку дистрибутива и останови сценарий запуска."
  fi

  fail \
    "ENV_FILE_MISSING" \
    "Файл переменных окружения JasperAI не найден: $ENV_FILE" \
    "Создайте '$ENV_FILE' и заполните SPRING_AI_MODEL_CHAT и API-ключ выбранного провайдера, затем снова выполните ./scripts/pre-start.sh." \
    "Сообщи пользователю, что перед запуском нужно создать ./env. Спроси провайдера DeepSeek или GigaChat и API-ключ. После ответа создай ./env по шаблону из SKILL.md. Не запускай start.sh до повторного успешного ./scripts/pre-start.sh."
fi

set -a
# shellcheck disable=SC1090
if ! . "$ENV_FILE"; then
  set +a
  fail \
    "ENV_FILE_LOAD_FAILED" \
    "Не удалось загрузить env-файл JasperAI: $ENV_FILE" \
    "Проверьте, что '$ENV_FILE' содержит корректные shell-переменные, затем снова выполните ./scripts/pre-start.sh." \
    "Сообщи пользователю, что ./env содержит некорректный shell-синтаксис. Попроси исправить файл или разрешить пересоздать его по шаблону из SKILL.md. Не запускай start.sh."
fi
set +a

provider="${SPRING_AI_MODEL_CHAT:-deepseek}"
case "$provider" in
  deepseek)
    if [ -z "${DEEPSEEK_API_KEY:-}" ]; then
      fail \
        "DEEPSEEK_API_KEY_MISSING" \
        "DEEPSEEK_API_KEY отсутствует в $ENV_FILE." \
        "Заполните DEEPSEEK_API_KEY в '$ENV_FILE', затем снова выполните ./scripts/pre-start.sh." \
        "Сообщи пользователю, что для DeepSeek не заполнен DEEPSEEK_API_KEY. Спроси API-ключ и обнови ./env по шаблону DeepSeek из SKILL.md. Не запускай start.sh до повторного успешного ./scripts/pre-start.sh."
    fi
    ;;
  gigachat)
    if [ -z "${GIGACHAT_API_KEY:-}" ]; then
      fail \
        "GIGACHAT_API_KEY_MISSING" \
        "GIGACHAT_API_KEY отсутствует в $ENV_FILE." \
        "Заполните GIGACHAT_API_KEY в '$ENV_FILE', затем снова выполните ./scripts/pre-start.sh." \
        "Сообщи пользователю, что для GigaChat не заполнен GIGACHAT_API_KEY. Спроси API-ключ и обнови ./env по шаблону GigaChat из SKILL.md. Не запускай start.sh до повторного успешного ./scripts/pre-start.sh."
    fi
    ;;
  *)
    fail \
      "SPRING_AI_MODEL_CHAT_UNSUPPORTED" \
      "Неподдерживаемый SPRING_AI_MODEL_CHAT в $ENV_FILE: $provider" \
      "Укажите SPRING_AI_MODEL_CHAT='deepseek' или 'gigachat' в '$ENV_FILE', затем снова выполните ./scripts/pre-start.sh." \
      "Сообщи пользователю, что выбран неподдерживаемый провайдер. Спроси, использовать DeepSeek или GigaChat, затем обнови ./env по соответствующему шаблону из SKILL.md. Не запускай start.sh до повторного успешного ./scripts/pre-start.sh."
    ;;
esac

if ! mkdir -p "$LOG_DIR"; then
  fail \
    "LOG_DIR_CREATE_FAILED" \
    "Не удалось создать папку логов JasperAI: $LOG_DIR" \
    "Проверьте права на папку дистрибутива или создайте папку вручную: mkdir -p '$LOG_DIR'" \
    "Сообщи пользователю, что папку logs нельзя создать. Попроси проверить, не лежит ли файл по пути ./logs, и права на папку дистрибутива. Не запускай start.sh."
fi

if ! touch "$LOG_FILE"; then
  fail \
    "LOG_FILE_WRITE_FAILED" \
    "Не удалось записать лог-файл JasperAI: $LOG_FILE" \
    "Проверьте права на файл, затем снова выполните ./scripts/pre-start.sh." \
    "Сообщи пользователю, что лог-файл недоступен для записи. Попроси проверить права на ./logs и ./logs/jasperai.log. Не запускай start.sh."
fi

if ! command -v java >/dev/null 2>&1; then
  fail \
    "JAVA_NOT_FOUND" \
    "Java не установлена или недоступна в PATH." \
    "Установите Java 17+ и убедитесь, что команда 'java' доступна в PATH, затем снова выполните ./scripts/pre-start.sh." \
    "Сообщи пользователю, что Java не найдена в PATH. Попроси пользователя скачать и установить Java 17+ из User Software, затем повторить запуск. Не скачивай и не устанавливай Java самостоятельно. Не запускай start.sh."
fi

java_version="$(java -version 2>&1 | awk -F '"' '/version/ {print $2; exit}')"
java_major="$(printf '%s' "$java_version" | awk -F. '{ if ($1 == "1") print $2; else print $1 }')"
if [ -z "$java_major" ] || [ "$java_major" -lt 17 ]; then
  fail \
    "JAVA_VERSION_UNSUPPORTED" \
    "Требуется Java 17+. Текущая версия Java: ${java_version:-unknown}." \
    "Установите или выберите Java 17+, затем снова выполните ./scripts/pre-start.sh." \
    "Сообщи пользователю текущую версию Java и что нужна Java 17+. Попроси пользователя скачать и установить Java 17+ из User Software, затем повторить запуск. Не скачивай и не устанавливай Java самостоятельно. Не запускай start.sh."
fi

echo "JASPERAI_PRE_START_STATUS=OK"
echo "JASPERAI_PRE_START_MESSAGE=Проверки перед запуском JasperAI успешно пройдены."
