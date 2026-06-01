---
name: jasper_ai
description: Локальный сервис для генерации печатных форм, чеков, HTML-макетов, JRXML-кода и JasperReports-шаблонов; используй этот скилл для запуска, остановки и подготовки jasper_ai, даже если пользователь не называет jasper_ai напрямую.
---

# jasper_ai

## Назначение

Скилл отвечает за локальное приложение jasper_ai: запуск, остановку и подготовку окружения для генерации печатных форм, чеков, JRXML-кода и JasperReports-шаблонов.

Используй скилл, если запрос связан с:

- печатными формами;
- печатками;
- чеками;
- HTML-макетами документов;
- JRXML;
- JasperReports;
- шаблонами отчетов;
- локальным редактором печатной формы.

Папка дистрибутива - это папка, в которой лежат `scripts/pre-start.sh`, `scripts/start.sh` и `scripts/stop.sh`.

```text
./jasper_ai.jar
./env
./logs/jasperai.log
```

Если JAR собран из исходников через Maven, файл `target/jasper_ai.jar` нужно скопировать в корень папки дистрибутива как `jasper_ai.jar`.

## Сценарий запуска

Перед командами перейди в папку дистрибутива jasper_ai.

1. Проверь порт `8080`:

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
```

2. Если порт `8080` уже слушает Java/jasper_ai, второй экземпляр не запускай. Сообщи пользователю, что приложение уже доступно на `http://localhost:8080/`.

3. Если порт `8080` занят другим процессом, сообщи пользователю, что порт занят, и покажи найденный процесс.

4. Если `lsof` ничего не выводит, сначала выполни детерминированные проверки:

```bash
./scripts/pre-start.sh
```

5. Если `pre-start.sh` вернул `JASPERAI_ERROR_CODE`, обработай структурированную ошибку сразу. `start.sh` не запускай и порт не жди.

6. Если `pre-start.sh` вернул `JASPERAI_PRE_START_STATUS=OK`, запускай Java-приложение:

```bash
mkdir -p ./logs
./scripts/start.sh > ./logs/start-command.log 2>&1 &
```

7. После запуска жди появления порта `8080` не дольше 15 секунд. Если `start.sh` уже вернул структурированную ошибку, прекращай ожидание:

```bash
for i in $(seq 1 15); do
  lsof -nP -iTCP:8080 -sTCP:LISTEN && break
  grep -q 'JASPERAI_ERROR_CODE=' ./logs/start-command.log && break
  sleep 1
done
```

8. Если порт `8080` слушает Java/jasper_ai, сообщи пользователю, что приложение запущено, и дай адрес `http://localhost:8080/`.

9. Если порт `8080` не появился, проверь вывод команды запуска и хвост лога приложения:

```bash
cat ./logs/start-command.log
tail -n 80 ./logs/jasperai.log
```

Не печатай лог целиком. Сообщи пользователю, что приложение не подтвердило запуск, и кратко перескажи релевантные строки из вывода/хвоста лога.

Не проверяй вручную `jasper_ai.jar`, `env`, Java или доступность логов. Это делает `scripts/pre-start.sh`.

## Сценарий остановки

Перед командами перейди в папку дистрибутива jasper_ai.

```bash
./scripts/stop.sh
```

`stop.sh` сам ищет процесс на порту `8080` и завершает его.

## Карта интентов

Запускать сценарий запуска, если пользователь просит:

- запустить jasper_ai;
- открыть jasper_ai;
- поднять jasper_ai;
- стартануть jasper_ai;
- запустить приложение в контексте jasper_ai;
- открыть приложение в контексте jasper_ai;
- сгенерировать печатную форму;
- сделать печатку;
- сгенерировать чек;
- получить JRXML;
- сделать JasperReports-шаблон.

Запускать сценарий остановки, если пользователь просит:

- остановить jasper_ai;
- закрыть jasper_ai;
- выключить jasper_ai;
- остановить приложение в контексте jasper_ai;
- закрыть приложение в контексте jasper_ai.

Если пользователь просит "запусти приложение", "открой приложение", "закрой приложение" или "останови приложение", а контекст jasper_ai неочевиден, спроси, какое приложение нужно обработать.

## Формат env

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

## Ошибки скриптов

Если `pre-start.sh`, `start.sh` или `stop.sh` завершился с ошибкой, найди в stdout/stderr структурированные строки:

```text
JASPERAI_ERROR_CODE=...
JASPERAI_ERROR_MESSAGE=...
JASPERAI_NEXT_STEP=...
JASPERAI_LOG_FILE=...
```

Действия:

- сообщи пользователю причину из `JASPERAI_ERROR_MESSAGE`;
- выполни `JASPERAI_NEXT_STEP`, если он не требует секрета или отдельного решения пользователя;
- если следующий шаг требует API-ключ или выбора провайдера, спроси пользователя;
- если есть `JASPERAI_LOG_FILE`, не печатай содержимое лога без отдельной просьбы;
- не заменяй структурированную ошибку общей фразой.

Если `pre-start.sh` вернул `ENV_FILE_MISSING`, `DEEPSEEK_API_KEY_MISSING`, `GIGACHAT_API_KEY_MISSING` или `SPRING_AI_MODEL_CHAT_UNSUPPORTED`, сообщи пользователю, что перед запуском нужно заполнить `./env`, затем спроси провайдера/API-ключ и создай или обнови `./env` без вывода ключа в чат.

Для DeepSeek:

```bash
cat > ./env <<'EOF'
SPRING_AI_MODEL_CHAT=deepseek
DEEPSEEK_API_KEY=PASTE_USER_KEY_HERE
EOF
```

Для GigaChat:

```bash
cat > ./env <<'EOF'
SPRING_AI_MODEL_CHAT=gigachat
GIGACHAT_API_KEY=PASTE_USER_KEY_HERE
EOF
```
