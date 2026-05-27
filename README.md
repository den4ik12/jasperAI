# JasperAI

Сервис автоматической генерации JasperReports-шаблонов (`.jrxml`) из HTML-макетов с помощью LLM (DeepSeek / GigaChat).

## Что делает

1. Принимает HTML-макет документа
2. Извлекает структуру элементов (текст, поля, фигуры, изображения) через LLM
3. Обогащает элементы координатами из браузерного рендера
4. Масштабирует координаты под целевой формат страницы (A4, A3, Letter и др.)
5. Генерирует валидный `.jrxml` через JasperReports API
6. Автоматически исправляет ошибки компиляции с помощью LLM (до 5 попыток)

## Стек технологий

| Компонент | Версия |
|-----------|--------|
| Java | 17 |
| Spring Boot | 3.5.13 |
| Spring AI | 1.1.3 |
| JasperReports | 6.21.3 |
| GigaChat Spring AI Starter | 1.1.1 |
| Lombok | — |
| Maven | Wrapper |

## Быстрый старт

### Предварительные требования

- JDK 17+
- API-ключ DeepSeek или GigaChat

### Настройка

Укажите API-ключи через переменные окружения:

```bash
# Выбор провайдера: deepseek или gigachat
export SPRING_AI_MODEL_CHAT=gigachat

# DeepSeek
export DEEPSEEK_API_KEY=YOUR_KEY

# GigaChat
export GIGACHAT_API_KEY=YOUR_KEY
```

Для запуска через `./scripts/start.sh` можно положить ключи в локальный файл `~/.jasperai/env`:

```bash
SPRING_AI_MODEL_CHAT=gigachat
DEEPSEEK_API_KEY=YOUR_KEY
GIGACHAT_API_KEY=YOUR_KEY
```

Если файла `~/.jasperai/env` нет, `./scripts/start.sh` не спрашивает ввод интерактивно. Скрипт возвращает структурированную ошибку `ENV_FILE_MISSING`, а агент должен спросить у пользователя провайдера/API-ключ и создать env-файл.

### Сборка и запуск

```bash
./mvnw clean install -Dmaven.test.skip=true
./mvnw spring-boot:run
```

> **Примечание:** тесты в текущей версии падают, поэтому сборка выполняется с флагом `-DskipTests`.

### Тесты

```bash
./mvnw test
```

## Локальный запуск через Qwen CLI

### Требования

- Java 17+.
- Qwen CLI с доступом к этому репозиторию/папке проекта.

### Команды

Обновление:

```bash
./scripts/update.sh
```

Запуск:

```bash
./scripts/start.sh
```

### Qwen CLI skill

Skill лежит в `.qwen/skills/local-app/SKILL.md`.
Skill относится только к JasperAI: если команда "запусти приложение" или "обнови приложение" неоднозначна, Qwen CLI должен уточнить, какое приложение имеется в виду.

- "запусти приложение" -> `./scripts/start.sh`
- "обнови приложение" -> `./scripts/update.sh`
- "обнови и запусти приложение" -> `./scripts/update.sh`, потом `./scripts/start.sh`

### Поведение

- `update` скачивает последнюю версию `app.jar` из GitHub Releases в `~/.jasperai/app.jar`.
- `start` проверяет наличие `app.jar`, Java 17+, экспортирует переменные из `~/.jasperai/env` и запускает локальный `app.jar`.
- Qwen CLI skill перед запуском должен сначала проверить `~/.jasperai/app.jar`; если JAR отсутствует или пустой, сначала выполнить `./scripts/update.sh`, и только после успешного обновления запускать `./scripts/start.sh`.
- если `~/.jasperai/env` отсутствует, `start` возвращает `ENV_FILE_MISSING`; env-файл должен создать агент после запроса провайдера и API-ключа у пользователя.
- логи Java-процесса пишутся в `~/.jasperai/logs/jasperai.log`.
- при ошибках скрипты выводят `JASPERAI_ERROR_CODE`, `JASPERAI_ERROR_MESSAGE` и `JASPERAI_NEXT_STEP`, чтобы CLI/AI мог понять причину и следующий шаг.
- если Java-процесс завершился с ошибкой приложения, `start` выводит путь к лог-файлу и просит передать его разработчику Денису Володину, не печатая лог пользователю.
- Запуск не обновляет приложение автоматически.
- Обновление не запускает приложение автоматически.

## API

### `POST /api/report/extract`

Извлечение структуры элементов из HTML-макета.

**Content-Type:** `application/json`
**Ответ:** `application/json`

```json
{
  "html": "<div>...</div>"
}
```

Возвращает `LlmTemplateData` — плоский список элементов с типами, стилями и группировками.

### `POST /api/report/generate`

Генерация JRXML из HTML с координатами элементов.

**Content-Type:** `application/json`
**Ответ:** `application/xml`

```json
{
  "html": "<div>...</div>",
  "coordinates": [
    { "id": "element_1", "x": 10, "y": 20, "width": 200, "height": 30 }
  ],
  "llmTemplateData": { "elements": [...] },
  "targetFormat": "A4",
  "pageWidth": 800,
  "pageHeight": 1130
}
```

Возвращает готовый `.jrxml`, прошедший валидацию компилятором JasperReports.

## Использование через UI

В корне проекта находится `editor.html` — визуальный редактор для генерации отчётов.

### Запуск

1. Запустите бэкенд:

```bash
./mvnw spring-boot:run
```

2. Откройте редактор в браузере: `http://localhost:8080/`.

### Рабочий процесс

1. **Выбор шаблона** — в правой панели выберите HTML-макет из выпадающего списка (c2c, credit-1..5, reclamation). Макет отобразится в iframe слева.

2. **Выбор формата** — укажите целевой формат страницы (A4, A3, A5, Letter, Legal).

3. **Извлечение элементов** — нажмите кнопку **"Извлечь"**. LLM проанализирует HTML и определит все элементы макета. После извлечения на макете появятся цветные рамки:
   - синие — поля данных (`TEXT_FIELD`)
   - жёлтые — статичный текст (`STATIC_TEXT`)

4. **Редактирование** — кликните на любой выделенный элемент, чтобы:
   - переключить тип (поле данных / статичный текст)
   - изменить содержимое (имя поля или текст)

5. **Генерация** — нажмите **"Сгенерировать"**. Сервис соберёт JRXML, провалидирует его и автоматически скачает файл `report.jrxml`.

### Добавление собственного шаблона

Положите HTML-файл в `archive/source/` и добавьте `<option>` в селектор `#templateSelect` в `editor.html`:

```html
<option value="archive/source/my-template.html">my-template</option>
```

## Архитектура

### Pipeline

Генерация построена на последовательном пайплайне шагов (`PipelineStep`):

```
HTML ──► Extract Pipeline
         └─ HtmlStructuredExtractionStep    — LLM парсит HTML → LlmTemplateData

HTML + Coordinates + LlmTemplateData ──► Generate Pipeline
         ├─ EmptyStaticTextFilterStep       — удаление пустых текстовых элементов
         ├─ CoordinateEnrichmentStep        — привязка координат к элементам
         ├─ CoordinateScalingStep           — масштабирование под целевой формат
         ├─ FrameGroupingStep              — группировка элементов во фреймы
         ├─ DynamicStretchStep             — настройка растяжения текста
         ├─ HideEmptyRowsStep             — скрытие пустых строк
         ├─ JrxmlGenerationStep           — сборка JasperDesign → JRXML
         └─ JrxmlValidationStep           — компиляция + авто-исправление через LLM
```

### Типы элементов

| Тип | Описание |
|-----|----------|
| `STATIC_TEXT` | Фиксированный текст (заголовки, подписи) |
| `TEXT_FIELD` | Динамическое поле (параметры отчёта) |
| `RECTANGLE` | Фоновые блоки, рамки, разделители |
| `IMAGE` | Изображения (img, background-image) |

### Форматы страниц

`A3`, `A4`, `A5`, `LETTER`, `LEGAL`

## Структура проекта

```
src/main/java/ru/volodin/jasperai/
├── controller/             — REST API
│   └── dto/                — DTO запросов (GenerateRequest, Coordinate)
├── domain/                 — доменные модели (LlmElement, JrxmlElement, PageFormat)
├── jrxml/
│   ├── converter/          — конвертеры элементов в JasperReports-объекты
│   │   └── impl/           — StaticText, TextField, Rectangle, Image
│   └── style/              — генерация стилей (StyleKey)
├── pipeline/               — движок пайплайна (Pipeline, PipelineStep, PipelineContext)
│   └── step/               — шаги пайплайна
└── service/                — бизнес-логика
    └── validation/         — компиляция и авто-исправление JRXML
```
