# JasperAI

JasperAI - Spring Boot сервис для генерации JasperReports-шаблонов (`.jrxml`) из HTML-макетов с помощью LLM. Поддерживаются DeepSeek и GigaChat.

## Что делает

1. Принимает HTML-макет документа.
2. Извлекает структуру элементов через LLM: текст, поля данных, прямоугольники, изображения и группировки.
3. Обогащает элементы координатами из браузерного рендера.
4. Масштабирует координаты под целевой формат страницы.
5. Группирует связанные блоки во фреймы и настраивает динамическое растяжение текста.
6. Генерирует `.jrxml` через JasperReports API.
7. Компилирует результат и при ошибках пытается исправить JRXML через LLM.
8. Может сформировать PDF-превью и варианты JSON-данных для проверки отчета.

## Стек

| Компонент | Версия |
|-----------|--------|
| Java | 17+ |
| Spring Boot | 3.5.13 |
| Spring AI | 1.1.3 |
| JasperReports | 6.21.3 |
| GigaChat Spring AI Starter | 1.1.1 |
| Maven | Wrapper |

## Быстрый старт для разработки

### Требования

- JDK 17+.
- API-ключ DeepSeek или GigaChat.

### Переменные окружения

```bash
# Провайдер: deepseek или gigachat
export SPRING_AI_MODEL_CHAT=deepseek

# Для DeepSeek
export DEEPSEEK_API_KEY=YOUR_KEY

# Для GigaChat
export GIGACHAT_API_KEY=YOUR_KEY
```

По умолчанию в `application.properties` выбран `deepseek`.

### Сборка

```bash
./mvnw clean package
```

Проект не содержит тестового контура: директория `src/test` и тестовые зависимости отсутствуют.

### Запуск из исходников

```bash
./mvnw spring-boot:run
```

После старта откройте редактор: `http://localhost:8080/`.

## Локальный запуск дистрибутива

Папка дистрибутива - это папка, в которой лежит `scripts/start.sh`. Все служебные файлы запуска лежат в корне этой папки:

- `jasper_ai.jar` - исполняемый JAR;
- `env` - переменные окружения для выбранного LLM-провайдера;
- `logs/` - логи запуска, создается автоматически.

Если JAR собран из исходников командой `./mvnw clean package`, Maven создаст файл `target/jasper_ai.jar`. Скопируйте его в корень папки дистрибутива как `jasper_ai.jar`.

### Настройка env-файла

Для `./scripts/start.sh` нужен файл `env` в корне папки дистрибутива:

```bash
SPRING_AI_MODEL_CHAT=deepseek
DEEPSEEK_API_KEY=YOUR_KEY
```

Для GigaChat:

```bash
SPRING_AI_MODEL_CHAT=gigachat
GIGACHAT_API_KEY=YOUR_KEY
```

### Запуск

Последовательность запуска:

1. Убедитесь, что в корне папки дистрибутива есть `jasper_ai.jar`.
2. Создайте `env` в корне папки дистрибутива.
3. Проверьте, не занят ли порт `8080` уже запущенным приложением:

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
```

Если команда ничего не выводит, порт свободен и можно запускать приложение.
Если команда выводит процесс Java/JasperAI, приложение уже запущено.
Если порт занят другим процессом, освободите порт или измените порт запуска.

4. Запустите приложение:

```bash
./scripts/start.sh
```

`start` проверяет наличие и размер `jasper_ai.jar` рядом с папкой `scripts`, версию Java, env-файл и нужный API-ключ. Логи приложения пишутся в `logs/jasperai.log` в той же папке дистрибутива.

При ошибках скрипты выводят структурированные поля:

- `JASPERAI_ERROR_CODE`
- `JASPERAI_ERROR_MESSAGE`
- `JASPERAI_NEXT_STEP`
- `JASPERAI_LOG_FILE`, если есть лог приложения

Запуск не обновляет приложение автоматически.

### Остановка

Команда остановки ищет процесс, который слушает порт `8080`, и завершает его через `kill`:

```bash
./scripts/stop.sh
```

Если порт свободен, скрипт сообщит, что JasperAI не запущен. Если порт занят, перед завершением скрипт покажет найденный процесс.

## API

Базовый путь: `/api/report`.

### `POST /extract`

Извлекает структуру элементов из HTML-макета.

Запрос:

```json
{
  "html": "<div>...</div>"
}
```

Ответ: `application/json`, объект `LlmTemplateData`.

### `POST /generate`

Генерирует и валидирует JRXML.

Запрос:

```json
{
  "html": "<div>...</div>",
  "coordinates": [
    { "id": "element_1", "x": 10, "y": 20, "width": 200, "height": 30 }
  ],
  "llmTemplateData": { "elements": [] },
  "targetFormat": "A4",
  "pageWidth": 800,
  "pageHeight": 1130,
  "printArea": null
}
```

Ответ: `application/xml`, готовый `.jrxml`.

### `POST /preview/pdf`

Генерирует PDF-превью по тому же запросу, что и `/generate`.

Ответ: `application/pdf`.

### `POST /test-data`

Генерирует 5 вариантов JSON-данных для JasperReports `JsonDataSource`.

Запрос:

```json
{
  "jrxml": "<?xml version=\"1.0\" encoding=\"UTF-8\"?>..."
}
```

Ответ: `application/json`, список вариантов с данными.

## UI

Главный интерфейс - `editor.html`, доступен по адресу `http://localhost:8080/`.

Перед открытием UI приложение должно быть запущено, а порт `8080` должен слушать процесс JasperAI.

Рабочий процесс:

1. Нажмите `Выбрать HTML-файл` и выберите HTML-макет с компьютера.
2. Выберите формат страницы: `A3`, `A4`, `A5`, `LETTER` или `LEGAL`.
3. Нажмите `Извлечь`, чтобы LLM построила структуру элементов.
4. При необходимости отредактируйте найденные элементы в интерфейсе.
5. Нажмите `Сгенерировать`, чтобы получить JRXML.
6. Используйте PDF-превью для быстрой визуальной проверки результата.

Отдельная страница `test-data.html` помогает сгенерировать варианты JSON-данных по готовому JRXML.

### Добавление HTML-макета

Дополнительная настройка больше не нужна: выберите нужный `.html` или `.htm` файл через кнопку `Выбрать HTML-файл` в правой панели редактора.

## Архитектура

Генерация построена на последовательном пайплайне шагов (`PipelineStep`):

```text
HTML -> Extract Pipeline
        HtmlStructuredExtractionStep -> LlmTemplateData

HTML + Coordinates + LlmTemplateData -> Generate Pipeline
        EmptyStaticTextFilterStep
        CoordinateEnrichmentStep
        CoordinateScalingStep
        FrameGroupingStep
        DynamicStretchStep
        HideEmptyRowsStep
        JrxmlGenerationStep
        JrxmlValidationStep
```

Основные типы элементов:

| Тип | Описание |
|-----|----------|
| `STATIC_TEXT` | Фиксированный текст: заголовки, подписи, служебные строки |
| `TEXT_FIELD` | Динамическое поле отчета |
| `RECTANGLE` | Фон, рамка, разделитель или декоративный блок |
| `IMAGE` | Изображение из `img` или CSS-фона |

## Структура проекта

```text
src/main/java/ru/volodin/jasperai/
├── controller/             REST API и маршрутизация UI
│   └── dto/                DTO запросов
├── domain/                 Доменные модели LLM/JRXML/страниц
├── jrxml/
│   ├── converter/          Конвертация элементов в JasperReports-объекты
│   └── style/              Ключи и переиспользование стилей
├── pipeline/               Движок пайплайна
│   └── step/               Шаги извлечения, подготовки и генерации
└── service/                Бизнес-логика, preview, тестовые данные, validation
```
