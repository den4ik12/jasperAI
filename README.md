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

Укажите API-ключ в `src/main/resources/application.properties`:

```properties
# Выбор провайдера: deepseek или gigachat
spring.ai.model.chat=gigachat

# DeepSeek
spring.ai.deepseek.api-key=YOUR_KEY

# GigaChat
spring.ai.gigachat.auth.bearer.api-key=YOUR_KEY
```

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

2. Откройте `editor.html` в браузере (двойной клик по файлу или через File -> Open).

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
