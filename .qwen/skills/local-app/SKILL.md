---
name: local-app
description: Генерация печатных форм, печаток, чеков, JRXML-кода и JasperReports-шаблонов через JasperAI; запуск и обновление локального приложения JasperAI.
---

# Локальное приложение JasperAI

## Назначение

Этот skill предназначен для Qwen CLI.

JasperAI — это локальное Spring Boot приложение для генерации печатных форм: печаток, чеков, JRXML-кода и JasperReports-шаблонов из HTML-макетов документов с помощью DeepSeek или GigaChat.

Приложение распространяется как один исполняемый JAR-файл:

```text
~/.jasperai/app.jar
```

JAR скачивается из GitHub Releases командой обновления. Запуск выполняется только после проверки, что JAR уже скачан и не пустой.

## Когда использовать этот skill

Используй этот skill только для JasperAI.

Skill подходит, если пользователь просит что-то из этого:

- запустить JasperAI;
- открыть JasperAI;
- поднять JasperAI;
- стартануть JasperAI;
- обновить JasperAI;
- скачать новую версию JasperAI;
- запустить свежую версию JasperAI;
- сгенерировать печатную форму;
- сделать печатку;
- сгенерировать чек;
- получить JRXML;
- сделать JasperReports-шаблон;
- работать с этим локальным приложением в папке проекта JasperAI.

Если пользователь говорит просто “запусти приложение”, “обнови приложение”, “открой приложение”, и при этом неясно, что речь именно про JasperAI, обязательно спроси, какое приложение имеется в виду. Не запускай команды молча.

## Главное правило запуска

Никогда не запускай `./scripts/start.sh` первой командой по интенту “запусти JasperAI”.

Даже если пользователь прямо сказал “запусти JasperAI”, сначала обязательно проверь, существует ли локальный JAR:

```bash
test -s "$HOME/.jasperai/app.jar"
```

Если JAR отсутствует или пустой, запуск невозможен. В этом случае сначала нужно скачать JAR через update.

Для пользовательского интента “запусти JasperAI” единственная правильная команда — весь этот блок целиком:

```bash
if test -s "$HOME/.jasperai/app.jar"; then
  ./scripts/start.sh
else
  ./scripts/update.sh && ./scripts/start.sh
fi
```

Этот блок запрещено упрощать до `./scripts/start.sh`.

## Команды

Проверить, скачан ли локальный JAR:

```bash
test -s "$HOME/.jasperai/app.jar"
```

Скачать последнюю версию `app.jar` из GitHub Releases:

```bash
./scripts/update.sh
```

Запустить уже скачанный `app.jar`:

```bash
./scripts/start.sh
```

Запустить JasperAI с обязательной проверкой JAR:

```bash
if test -s "$HOME/.jasperai/app.jar"; then
  ./scripts/start.sh
else
  ./scripts/update.sh && ./scripts/start.sh
fi
```

Обновить и запустить свежую версию:

```bash
./scripts/update.sh && ./scripts/start.sh
```

## Карта интентов

### Пользователь просит запустить JasperAI

Фразы:

- “запусти JasperAI”
- “открой JasperAI”
- “подними JasperAI”
- “стартани JasperAI”
- “запусти приложение” в явном контексте JasperAI
- “открой это приложение” в папке проекта JasperAI

Действие:

```bash
if test -s "$HOME/.jasperai/app.jar"; then
  ./scripts/start.sh
else
  ./scripts/update.sh && ./scripts/start.sh
fi
```

Не выполняй `./scripts/start.sh` напрямую для этих фраз.

### Пользователь просит обновить JasperAI

Фразы:

- “обнови JasperAI”
- “скачай новую версию JasperAI”
- “переустанови JasperAI”
- “обнови приложение” в явном контексте JasperAI

Действие:

```bash
./scripts/update.sh
```

После обновления не запускай приложение, если пользователь не просил запуск.

### Пользователь просит обновить и запустить JasperAI

Фразы:

- “обнови и запусти JasperAI”
- “запусти свежую версию JasperAI”
- “скачай новую версию и открой приложение”

Действие:

```bash
./scripts/update.sh && ./scripts/start.sh
```

### Пользователь просит создать печатную форму, печатку, чек, JRXML или JasperReports-шаблон

Фразы:

- “сгенерируй печатную форму”
- “сделай печатку”
- “сгенерируй чек”
- “сделай JRXML”
- “сделай JasperReports шаблон”

Действие:

1. Если JasperAI еще не запущен, запусти его через обязательный preflight-блок:

```bash
if test -s "$HOME/.jasperai/app.jar"; then
  ./scripts/start.sh
else
  ./scripts/update.sh && ./scripts/start.sh
fi
```

2. Не запускай `./scripts/start.sh` напрямую.

## Что делает start.sh

`./scripts/start.sh` предназначен только для запуска уже скачанного JAR.

Он выполняет проверки:

- `~/.jasperai/app.jar` существует и не пустой;
- установлена Java 17+;
- есть файл переменных окружения `~/.jasperai/env`;
- в env-файле указан провайдер `SPRING_AI_MODEL_CHAT`;
- для выбранного провайдера указан ключ `DEEPSEEK_API_KEY` или `GIGACHAT_API_KEY`;
- stdout/stderr Java-процесса пишутся в `~/.jasperai/logs/jasperai.log`.

`start.sh` никогда не спрашивает пользователя интерактивно и не ждет ввод. Его запускает агент, часто в фоне.

Если `~/.jasperai/env` отсутствует, `start.sh` создает папку `~/.jasperai`, возвращает ошибку `ENV_FILE_MISSING` и сообщает агенту, что нужно сделать дальше. Env-файл должен создать агент на более верхнем уровне после того, как спросит пользователя провайдера и API-ключ.

## Формат env-файла

DeepSeek:

```bash
SPRING_AI_MODEL_CHAT=deepseek
DEEPSEEK_API_KEY=YOUR_KEY
```

GigaChat:

```bash
SPRING_AI_MODEL_CHAT=gigachat
GIGACHAT_API_KEY=YOUR_KEY
```

Никогда не записывай API-ключи в:

- `src/main/resources/application.properties`;
- README;
- документацию;
- сообщения пользователю;
- историю команд.

## Формат ошибок скриптов

Если скрипт завершился ошибкой, он печатает машинно-читаемые строки:

```text
JASPERAI_ERROR_CODE=...
JASPERAI_ERROR_MESSAGE=...
JASPERAI_NEXT_STEP=...
JASPERAI_LOG_FILE=...
```

Правило:

- прочитай `JASPERAI_ERROR_CODE`;
- прочитай `JASPERAI_ERROR_MESSAGE`;
- выполни или предложи действие из `JASPERAI_NEXT_STEP`;
- если есть `JASPERAI_LOG_FILE`, не печатай содержимое лога пользователю без отдельной просьбы.

## Обработка ошибок

Если `test -s "$HOME/.jasperai/app.jar"` неуспешен, не вызывай `./scripts/start.sh`. Выполни:

```bash
./scripts/update.sh && ./scripts/start.sh
```

Если `update.sh` вернул `APP_JAR_DOWNLOAD_FAILED`, скажи пользователю, что обновление не удалось. Попроси проверить интернет и наличие `app.jar` в последнем GitHub Release.

Если `start.sh` вернул `JAVA_NOT_FOUND` или `JAVA_VERSION_UNSUPPORTED`, скажи пользователю установить Java 17+.

Если `start.sh` вернул ошибку env-файла или ключа, спроси у пользователя провайдера и API-ключ, затем создай или обнови `~/.jasperai/env`.

Если `start.sh` вернул `ENV_FILE_MISSING`, сделай так:

1. Спроси пользователя, какого провайдера использовать: `deepseek` или `gigachat`.
2. Спроси соответствующий API-ключ.
3. Создай файл `~/.jasperai/env`.
4. Запиши в него только нужные переменные.
5. Выполни `chmod 600 "$HOME/.jasperai/env"`.
6. Повтори запуск через обязательный preflight-блок.

Для DeepSeek:

```bash
mkdir -p "$HOME/.jasperai"
cat > "$HOME/.jasperai/env" <<'EOF'
SPRING_AI_MODEL_CHAT=deepseek
DEEPSEEK_API_KEY=PASTE_USER_KEY_HERE
EOF
chmod 600 "$HOME/.jasperai/env"
```

Для GigaChat:

```bash
mkdir -p "$HOME/.jasperai"
cat > "$HOME/.jasperai/env" <<'EOF'
SPRING_AI_MODEL_CHAT=gigachat
GIGACHAT_API_KEY=PASTE_USER_KEY_HERE
EOF
chmod 600 "$HOME/.jasperai/env"
```

Если `start.sh` вернул `APP_START_FAILED`, не печатай лог пользователю. Скажи:

```text
JasperAI не запустился. Передайте файл логов разработчику Денису Володину:
<путь из JASPERAI_LOG_FILE>
```

Если запрос неоднозначный и непонятно, что речь про JasperAI, спроси уточнение. Не запускай команды.

## Примеры

Пользователь:

```text
запусти JasperAI
```

Правильное действие:

```bash
if test -s "$HOME/.jasperai/app.jar"; then
  ./scripts/start.sh
else
  ./scripts/update.sh && ./scripts/start.sh
fi
```

Неправильное действие:

```bash
./scripts/start.sh
```

Пользователь:

```text
обнови JasperAI
```

Правильное действие:

```bash
./scripts/update.sh
```

Пользователь:

```text
обнови и запусти JasperAI
```

Правильное действие:

```bash
./scripts/update.sh && ./scripts/start.sh
```

Пользователь:

```text
запусти приложение
```

Если текущий контекст явно JasperAI, используй preflight-блок запуска.

Если текущий контекст неясен, спроси:

```text
Какое приложение нужно запустить: JasperAI или другое?
```
