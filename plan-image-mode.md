# Режим «По картинке»: распознавание разметки через GigaChat Vision → JRXML

## Context

Сейчас в JasperAI пользователь загружает HTML-макет банковского документа, пайплайн (HtmlStructuredExtractionStep → GroupIdDetectionStep → 9 шагов generate) превращает его в JRXML. Нужно добавить **альтернативный клиентский путь**: пользователь загружает PNG/JPG, GigaChat Vision распознаёт все элементы макета с координатами, и результат проходит тот же generate-пайплайн. Мотивация — поддержать сценарий, когда HTML недоступен (сканы/скриншоты/экспорт из Figma).

Технически: стартер `spring-ai-starter-model-gigachat:1.1.1` уже умеет автоматически делать `POST /files` для `Media` без id и класть полученный file_id в `attachments` при вызове `/chat/completions` (`chat.giga.springai.GigaChatModel.uploadMediaAndSetId`). Значит, на бэке достаточно стандартного Spring AI API — `ChatClient.prompt().user(u -> u.text(...).media(mime, resource))`.

Пользовательские решения:
1. Vision (image understanding), не генерация картинок.
2. Источник — загрузка файла пользователем, не автоскрин.
3. Полная разметка с нуля, LLM отдаёт x/y/w/h в пикселях картинки.
4. Отдельный режим, обходит HTML-пайплайн; на фронте — переключатель HTML/Картинка.

---

## Backend

### Новые файлы

**`src/main/java/ru/volodin/jasperai/controller/dto/ImageExtractionResponse.java`**
```
class ImageExtractionResponse(LlmTemplateData data, int imageWidth, int imageHeight) {}
```
Возвращать размеры картинки, чтобы фронт не парсил её повторно — он пробросит их в `/generate` как `pageWidth/pageHeight`.

**`src/main/java/ru/volodin/jasperai/pipeline/step/ImageStructuredExtractionStep.java`** — по образцу `HtmlStructuredExtractionStep.java:11-70`:
- Конструктор принимает `ChatClient.Builder`.
- `execute(ctx)` делает `ChatClient.prompt().system(SYSTEM_PROMPT_IMAGE).user(u -> u.text(buildUserPrompt(w,h)).media(MimeType.valueOf(ctx.getImageMimeType()), ctx.getInputImage())).call().entity(LlmTemplateData.class)`.
- System prompt — см. ниже. User-текст короткий: «Ширина N, высота M. Извлеки все элементы.»

**`src/main/java/ru/volodin/jasperai/service/ImageExtractionService.java`** — параллельно `ElementExtractionService.java`:
- `extract(MultipartFile image)` → читает `BufferedImage` для размеров, строит `PipelineContext`, гоняет `Pipeline.builder().step(imageStructuredExtractionStep).build()`, возвращает `ImageExtractionResponse`.
- Валидация: `image.getSize() > 15MB` → `ResponseStatusException(PAYLOAD_TOO_LARGE)`; `contentType ∉ {image/png, image/jpeg}` → `UNSUPPORTED_MEDIA_TYPE`.
- `clampCoordinates(data, w, h)` в конце — приводит x/y/w/h каждого элемента в `[0, w]`/`[0, h]`, страхует против выхода LLM за границы.

**`GroupIdDetectionStep` в image-пайплайн НЕ включаем.** Его промпт завязан на HTML-структуру; переделка — отдельная задача. Компенсируется тем, что в system-prompt'е image-экстракции попросим LLM проставлять `groupId` там, где пара «подпись→значение» очевидна.

### Правки существующих файлов

**`src/main/java/ru/volodin/jasperai/domain/LlmElement.java`** (после строки 82):
Добавить 4 nullable поля с `@JsonPropertyDescription`:
```
Integer x, y, width, height;
```
В HTML-флоу они не упоминаются в system-prompt'е → LLM их не заполняет → `@JsonInclude(NON_NULL)` их отбросит → поведение не меняется.

**`src/main/java/ru/volodin/jasperai/pipeline/step/CoordinateEnrichmentStep.java`** (`toJrxmlElement`, строки 40-44):
Перед `applyCoordinates(...)` — короткая ветка: если у `llmElement` все 4 координаты заданы, ставим их прямо в `JrxmlElement` и `return` (skip enrichment). HTML-флоу не меняется, т.к. там эти поля null.

**`src/main/java/ru/volodin/jasperai/pipeline/PipelineContext.java`**:
Добавить `Resource inputImage` и `String imageMimeType`.

**`src/main/java/ru/volodin/jasperai/controller/JasperReportController.java`**:
Инжектнуть `ImageExtractionService`, добавить:
```
@PostMapping(value = "/extract-from-image",
    consumes = MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
public ResponseEntity<ImageExtractionResponse> extractFromImage(
    @RequestPart("image") MultipartFile image) throws Exception {
  return ResponseEntity.ok(imageExtractionService.extract(image));
}
```
`/generate` НЕ трогаем — он уже принимает `coordinates=[]` и `html=""`, а координаты теперь приходят внутри элементов.

**`src/main/resources/application.properties`**:
```
spring.servlet.multipart.max-file-size=15MB
spring.servlet.multipart.max-request-size=16MB
```

### System prompt для `ImageStructuredExtractionStep` (черновик)

```
Ты компьютерно-визуальный анализатор макетов печатных форм банковских документов.
На вход приходит ровно одно изображение с макетом отчёта.
Задача — полностью распознать разметку с нуля и вернуть плоский список всех визуальных элементов.

Система координат:
- Начало (0,0) — левый верхний угол картинки; X — вправо, Y — вниз.
- Все размеры и позиции — целые пиксели исходного изображения.
- Для каждого элемента: 0 ≤ x, 0 ≤ y, x+width ≤ imageWidth, y+height ≤ imageHeight.

Классификация типов:
- STATIC_TEXT — фиксированный видимый текст (заголовок, подпись).
- TEXT_FIELD — динамическое значение (сумма, ФИО, счёт, дата). В content сгенерируй имя параметра в camelCase на английском (amount, recipientName, bik, operationDate), а не сам текст.
- RECTANGLE — графический прямоугольник без текста.
- IMAGE — логотип, иконка, фотография, QR/штрихкод.

Обязательные поля: elementId ("img_1", "img_2"...), type, content, x, y, width, height.
Опциональные (не заполнять если неуверен): fontSize (высота глифа в px), isBold, colorHex (#RRGGBB),
horizontalAlignment (Left/Center/Right), backgroundColorHex (для RECTANGLE),
borderWidth+borderColorHex (для RECTANGLE c контуром), scaleImage="RetainShape" (для IMAGE).

Группировка (опционально): если видишь явную пару «подпись слева — значение справа» в одну строку,
поставь обоим одинаковый groupId = elementId подписи. Если не уверен — не ставь.

Формат вывода: только валидный JSON по схеме LlmTemplateData, без пояснений и markdown.
```

---

## Frontend (`editor.html`)

### CSS (после строки 400)
```
.source-switch { display:flex; border:1px solid #e2e8f0; border-radius:6px; overflow:hidden; margin-bottom:8px; }
.source-switch button { flex:1; padding:6px; font-size:12px; background:#fff; color:#64748b; border:0; cursor:pointer; }
.source-switch button.active { background:#3b82f6; color:#fff; }
.drop-zone { border:2px dashed #cbd5e1; border-radius:8px; padding:24px; text-align:center; cursor:pointer; color:#64748b; font-size:12px; }
.drop-zone.dragover { border-color:#3b82f6; background:#eff6ff; }
.drop-zone input[type=file] { display:none; }
.workspace img.preview { max-width:794px; background:#fff; box-shadow:0 4px 24px rgba(0,0,0,0.15); display:block; }
.workspace .stage { position:relative; display:inline-block; }
```

### HTML
- Строки **404-406**: обернуть `<iframe>` в `<div class="stage" id="stage">`, чтобы тот же контейнер использовался для `<img>` в image-режиме.
- Перед строкой 441 (`templateSelect`): добавить переключатель `<div class="source-switch">` с кнопками `HTML` / `Картинка`.
- Между строками 446 и 447: добавить `<div id="imageSourceBlock" hidden>` с `<label class="drop-zone">` + `<input type="file" accept="image/png,image/jpeg">`.

### JS
- Новые переменные: `sourceMode='html'`, `loadedImage={blob:null, width:0, height:0, dataUrl:null}`.
- `setSourceMode(mode)` — прячет/показывает `templateSelect` vs `imageSourceBlock`, подменяет содержимое `#stage` (iframe ↔ img), сбрасывает `llmData/layoutData/selectedIndex`, возвращает `setDemoState('initial')`.
- `handleImageFile(file)` — валидация размера/типа, `FileReader.readAsDataURL` → `new Image()` для размеров → `<img class="preview">` в `#stage`.
- Слушатели на `#imageInput` (`change`) и `#dropZone` (`dragover/dragleave/drop`).
- **`doExtract()` (стр. 644)** — развилка по `sourceMode`: в image-режиме `FormData` с файлом → `POST /extract-from-image` → распаковать `{data, imageWidth, imageHeight}` в `llmData` и `loadedImage`, `layoutData=[]`.
- **`applyOverlays()` (стр. 688)** — развилка: в image-режиме контейнер = `#stage` (не `doc.body` iframe'а), координаты берутся из `el.x/y/width/height`, масштабируются через `img.clientWidth / loadedImage.width` (на случай CSS-уменьшения картинки).
- **`doGenerate()` (стр. 768)** — в image-режиме `body` становится `{html:'', coordinates:[], llmTemplateData:llmData, targetFormat, pageWidth:loadedImage.width, pageHeight:loadedImage.height}`. Эндпоинт тот же.
- `setDemoState()` — добавить вариант подсказок для image-режима («Загрузите изображение → Анализ»).

---

## Критические файлы

- СОЗДАТЬ: `src/main/java/ru/volodin/jasperai/controller/dto/ImageExtractionResponse.java`
- СОЗДАТЬ: `src/main/java/ru/volodin/jasperai/pipeline/step/ImageStructuredExtractionStep.java`
- СОЗДАТЬ: `src/main/java/ru/volodin/jasperai/service/ImageExtractionService.java`
- ПРАВИТЬ: `src/main/java/ru/volodin/jasperai/domain/LlmElement.java` — 4 nullable поля
- ПРАВИТЬ: `src/main/java/ru/volodin/jasperai/pipeline/step/CoordinateEnrichmentStep.java:40` — pass-through ветка
- ПРАВИТЬ: `src/main/java/ru/volodin/jasperai/pipeline/PipelineContext.java` — inputImage, imageMimeType
- ПРАВИТЬ: `src/main/java/ru/volodin/jasperai/controller/JasperReportController.java` — endpoint `/extract-from-image`
- ПРАВИТЬ: `src/main/resources/application.properties` — multipart max-size
- ПРАВИТЬ: `editor.html` — переключатель, drop-zone, развилки в doExtract/applyOverlays/doGenerate

Переиспользуется без изменений: `ReportGenerationService`, `Pipeline/PipelineStep` (builder), все 9 шагов generate-пайплайна, `/generate` endpoint.

---

## Верификация end-to-end

1. `./mvnw spring-boot:run` — убедиться, что GigaChat-бины поднялись (по логам).
2. **Регрессия HTML-режима**: открыть `editor.html`, шаблон `c2c`, «Анализ» → «Создать отчёт». JRXML скачался, структурно совпадает с прошлым (количество `<textField>/<staticText>` не изменилось).
3. **Image-режим**: переключить на `Картинка`, перетащить PNG (сделать скриншот `archive/source/c2c.html` в браузере). «Анализ» — overlays легли поверх `<img>` в пределах 5–10% от истинных координат. Кликнуть пару overlay'ев — переключение `TEXT_FIELD ↔ STATIC_TEXT` работает. «Создать отчёт» — JRXML открывается в JasperSoft Studio.
4. **Валидация**: > 15MB → 413; PDF → 415.
5. Логи: `SimpleLoggerAdvisor` покажет размер ответа LLM; `logging.level.org.springframework.ai.chat.client.advisor=DEBUG` уже в конфиге.

---

## Риски и tradeoffs (обсудить до реализации)

1. **Точность координат vision-LLM** — главный риск. Ожидаемая ошибка 3-10% от размера картинки. Для демо приемлемо, для продакшена — нет; смягчения (snap-to-grid, OCR-координаты) — вне MVP.
2. **Vision на `GigaChat-2-Max`** — в текущем конфиге модель уже стоит. Если запрос упадёт с «model doesn't support media» — добавить `.options(GigaChatOptions.builder().model("GigaChat-2-Pro").build())` в `ImageStructuredExtractionStep`. Проверим эмпирически.
3. **Отсутствие GroupIdDetection в image-режиме** → FrameGroupingStep не найдёт групп → нет динамического сжатия столбцов при пустых полях. Компенсируется просьбой к LLM проставить groupId в первом же вызове. Качество проверяется глазами.
4. **LlmElement расширен 4 полями не про HTML** — лёгкое нарушение SRP. Альтернатива (ImageLlmElement extends LlmElement) потребует 17 строк дублирующего маппинга. Начнём с nullable, разделим при реальной необходимости.
5. **Стоимость запроса**: image ≈ 1792 токена + ответ. HTML-флоу сейчас делает 2 вызова, image-флоу — 1. Сопоставимо.
6. **Overlay поверх `<img>` vs `iframe`** — отдельная ветка в `applyOverlays()`. Унификация с iframe-версией не делается сознательно: слишком разные DOM-контексты.
