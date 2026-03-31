package ru.volodin.jasperai.pipeline.step;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.stereotype.Component;
import ru.volodin.jasperai.domain.LlmTemplateData;
import ru.volodin.jasperai.pipeline.PipelineContext;
import ru.volodin.jasperai.pipeline.PipelineStep;

@Component
public class HtmlStructuredExtractionStep implements PipelineStep {

    private static final String SYSTEM_PROMPT = """
            Ты парсер HTML/CSS-макетов. Твоя задача — извлечь структурированное описание макета.

            Источник данных:
            - Загруженный HTML-файл и связанные CSS-стили элементов.
            - Используй только информацию из спецификации.
            - Никаких вольных интерпретаций, только то, что в спецификации.

            Задача:
            - Составь плоский список всех визуальных элементов.

            Классификация типов элементов:
            - STATIC_TEXT: элемент содержит фиксированный текст (подпись, заголовок, описание).
            - TEXT_FIELD: элемент содержит динамическое значение (поле данных). Для content сгенерируй имя параметра в camelCase на английском языке.
            - RECTANGLE: блок без текста, используемый как фон, рамка или разделитель.
            - IMAGE: элемент с тегом <img> или CSS background-image.

            Правило заполнения полей:
            - Заполняй ТОЛЬКО поля, явно применимые к типу элемента и явно заданные в CSS самого элемента.
            - Не используй унаследованные, браузерные или дефолтные значения.
            - Если поле не применимо к типу или не задано явно в CSS — не включай его в JSON совсем (без null, "", 0, false).

            Поля по типам:
            - RECTANGLE: type, backgroundColorHex (только если есть background-color), borderWidth и borderColorHex (только если есть border).
            - STATIC_TEXT и TEXT_FIELD: type, content, fontFamily (если задан), fontSize (только если задан явно в px), isBold (только если font-weight >= 700), colorHex (если задан), horizontalAlignment (всегда: Left/Center/Right), verticalAlignment (всегда: Top/Middle), textAdjustStretch (только если true), lineHeight (только если задан явно).
            - IMAGE: type, imageUrl, scaleImage.
            - Все типы: groupId — Идентификатор логической группы, в которую входит этот элемент. Группа — это набор элементов, которые визуально составляют единый блок

            Идентификация элементов:
            - Для каждого элемента добавь поле elementId равное значению атрибута id соответствующего HTML-элемента. Если у элемента нет атрибута id — поднимись вверх по DOM-дереву и возьми id ближайшего предка у которого он есть. class={} НИКОГДА не используй как elementId!
            - Для текстовых строк используй id самого тега-носителя текста (<p>, <span> и т.п.), а не id родительского контейнера.
            - Если многострочный текст разбит на несколько тегов <p> (у каждого свой id), создай отдельный элемент для каждого тега <p> с его собственным id.
            - Никогда не используй id родительского div в качестве elementId текстового элемента, ЕСЛИ у самого текстового тега есть СВОЙ id.
            - class={} НИКОГДА не используй как elementId!

            Формат вывода:
            - Выведи только валидный JSON-объект, соответствующий схеме LlmTemplateData.
            - Без пояснений, комментариев, markdown.
            - Начинай сразу с {.
            """;

    private final ChatClient chatClient;

    public HtmlStructuredExtractionStep(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public void execute(PipelineContext context) {
        LlmTemplateData data = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .advisors(SimpleLoggerAdvisor.builder()
                                  .build())
                .user(context.getInputHtml())
                .call()
                .entity(LlmTemplateData.class);
        context.setLlmTemplateData(data);
    }
}
