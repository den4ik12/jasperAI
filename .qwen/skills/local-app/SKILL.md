---
name: JasperAI
description: Локальный сервис для генерации печатных форм, чеков, HTML-макетов, JRXML-кода и JasperReports-шаблонов; используй этот скилл для запуска, остановки и подготовки JasperAI, даже если пользователь не называет JasperAI напрямую.
---

# JasperAI

## Назначение

Скилл отвечает за локальное приложение JasperAI: запуск, остановку и подготовку окружения для генерации печатных форм, чеков, JRXML-кода и JasperReports-шаблонов.

Используй скилл, если запрос связан с:

- печатными формами;
- печатками;
- чеками;
- HTML-макетами документов;
- JRXML;
- JasperReports;
- шаблонами отчетов;
- локальным редактором печатной формы.

Папка дистрибутива - это папка, в которой лежит `scripts/start.sh`.

```text
./jasper_ai.jar
./env
./logs/jasperai.log
```

Если JAR собран из исходников через Maven, файл `target/jasper_ai.jar` нужно скопировать в корень папки дистрибутива как `jasper_ai.jar`.

## Сценарий запуска

Перед командами перейди в папку дистрибутива JasperAI.

1. Проверь порт `8080`:

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
```

2. Если `lsof` ничего не выводит, запусти приложение:

```bash
./scripts/start.sh
```

3. Если порт `8080` слушает Java/JasperAI, второй экземпляр не запускай.

4. Если порт `8080` занят другим процессом, сообщи пользователю, что порт занят, и покажи найденный процесс.

Не проверяй вручную `jasper_ai.jar`, `env`, Java или логи. Это делает `scripts/start.sh`.

## Сценарий остановки

Перед командами перейди в папку дистрибутива JasperAI.

```bash
./scripts/stop.sh
```

`stop.sh` сам ищет процесс на порту `8080` и завершает его.

## Карта интентов

Запускать сценарий запуска, если пользователь просит:

- запустить JasperAI;
- открыть JasperAI;
- поднять JasperAI;
- стартануть JasperAI;
- запустить приложение в контексте JasperAI;
- открыть приложение в контексте JasperAI;
- сгенерировать печатную форму;
- сделать печатку;
- сгенерировать чек;
- получить JRXML;
- сделать JasperReports-шаблон.

Запускать сценарий остановки, если пользователь просит:

- остановить JasperAI;
- закрыть JasperAI;
- выключить JasperAI;
- остановить приложение в контексте JasperAI;
- закрыть приложение в контексте JasperAI.

Если пользователь просит "запусти приложение", "открой приложение", "закрой приложение" или "останови приложение", а контекст JasperAI неочевиден, спроси, какое приложение нужно обработать.

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

Если `start.sh` или `stop.sh` завершился с ошибкой, найди в stdout/stderr структурированные строки:

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

Если `start.sh` вернул `ENV_FILE_MISSING`, `DEEPSEEK_API_KEY_MISSING`, `GIGACHAT_API_KEY_MISSING` или `SPRING_AI_MODEL_CHAT_UNSUPPORTED`, спроси провайдера/API-ключ и создай или обнови `./env`.

Для DeepSeek:

```bash
cat > "./env" <<'EOF'
SPRING_AI_MODEL_CHAT=deepseek
DEEPSEEK_API_KEY=PASTE_USER_KEY_HERE
EOF
```

Для GigaChat:

```bash
cat > "./env" <<'EOF'
SPRING_AI_MODEL_CHAT=gigachat
GIGACHAT_API_KEY=PASTE_USER_KEY_HERE
EOF
```
