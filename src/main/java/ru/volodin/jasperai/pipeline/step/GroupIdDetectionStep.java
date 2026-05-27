package ru.volodin.jasperai.pipeline.step;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;
import ru.volodin.jasperai.domain.LlmElement;
import ru.volodin.jasperai.domain.LlmGroupLink;
import ru.volodin.jasperai.domain.LlmGroupLinksResponse;
import ru.volodin.jasperai.domain.LlmTemplateData;
import ru.volodin.jasperai.pipeline.PipelineContext;
import ru.volodin.jasperai.pipeline.PipelineStep;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class GroupIdDetectionStep implements PipelineStep {

    private static final PromptTemplate USER_MESSAGE_TEMPLATE = new PromptTemplate("""
            WHITELIST допустимых elementId (используй ТОЛЬКО значения из этого массива):
            {allowedIds}

            HTML-макет:
            {html}
            """);

    private static final String SYSTEM_PROMPT = """
            Ты эксперт по анализу семантики HTML-макетов банковских документов.
            Твоя единственная задача — определить смысловые пары «подпись → значение» и вернуть для них groupId.
            Ничего не извлекаешь, не описываешь стили, не классифицируешь элементы. Только связи.

            ════════════════════════════════════════════
            ЧТО ТАКОЕ groupId
            ════════════════════════════════════════════

            groupId объединяет элементы, которые при растяжении отчёта ДОЛЖНЫ оставаться вместе.
            Критерий — смысловая зависимость, а не визуальная близость.

            ЗНАЧЕНИЕ groupId = id HTML-элемента подписи (текстовой метки) из этой пары.
            И у подписи, и у значения groupId одинаковый — он равен id самой подписи.

            ════════════════════════════════════════════
            КОГДА СТАВИТЬ ОДИНАКОВЫЙ groupId
            ════════════════════════════════════════════

            • Подпись и её значение: «Сумма» ↔ «30 ₽», «БИК» ↔ «044525225», «ФИО получателя» ↔ «Иван Иванович».
            • Иконка и подпись к ней.
            • Заголовок раздела и непосредственно следующее за ним значение одной логической строки.
            • Многострочная подпись или многострочное значение — каждый <p> пары получает один и тот же groupId (= id первого <p> подписи).

            ════════════════════════════════════════════
            КОГДА НЕ СТАВИТЬ groupId
            ════════════════════════════════════════════

            • Несколько самостоятельных подписей рядом, но без общего значения.
            • Заголовок всего документа и дата — это разные смысловые единицы.
            • Два независимых поля данных без общей подписи.
            • Свободный текст (абзацы договора, пояснения, примечания) — не пара.
            • Элемент без пары — не включай его в ответ вовсе.

            ════════════════════════════════════════════
            ОСОБЫЙ СЛУЧАЙ: ПАРАЛЛЕЛЬНЫЕ КОЛОНКИ
            ════════════════════════════════════════════

            Когда подписи и значения находятся в разных соседних div-ах (левая колонка — подписи,
            правая колонка — значения):
            • Сопоставляй по порядку: 1-я подпись ↔ 1-е значение, 2-я ↔ 2-е и т.д.
            • Проверяй соответствие по смыслу: «Сумма» ↔ элемент с денежной суммой, «БИК» ↔ 9 цифр.
            • Количество подписей и количество значений обычно совпадает — если нет, выравнивай по смыслу.
            • Каждой паре назначай уникальный groupId = id элемента-подписи из этой пары.

            ════════════════════════════════════════════
            РЕАЛЬНЫЕ ПРИМЕРЫ ИЗ ПРОЕКТА
            ════════════════════════════════════════════

            ПРИМЕР 1 — Параллельные колонки (подписи слева, значения справа).
            HTML:
              <div id="22229_4243">
                <p id="22229_4244">На карту</p>
                <p id="22229_4245">ФИО получателя</p>
                <p id="22229_4246">Банк получателя</p>
                <p id="22229_4247">БИК</p>
                <p id="22229_4248">Кор. счёт</p>
              </div>
              <div id="22229_4266">
                <p id="22229_4267">Полностью ?</p>
                <p id="22229_4268">Ирина Анатольевна Б.</p>
                <p id="22229_4269">ПАО Сбербанк Г. Москва</p>
                <p id="22229_4270">044525225</p>
                <p id="22229_4271">30101 810 4 0000 0000225</p>
              </div>
            Ответ:
              { "elementId": "22229_4244", "groupId": "22229_4244" }
              { "elementId": "22229_4267", "groupId": "22229_4244" }
              { "elementId": "22229_4245", "groupId": "22229_4245" }
              { "elementId": "22229_4268", "groupId": "22229_4245" }
              { "elementId": "22229_4246", "groupId": "22229_4246" }
              { "elementId": "22229_4269", "groupId": "22229_4246" }
              { "elementId": "22229_4247", "groupId": "22229_4247" }
              { "elementId": "22229_4270", "groupId": "22229_4247" }
              { "elementId": "22229_4248", "groupId": "22229_4248" }
              { "elementId": "22229_4271", "groupId": "22229_4248" }

            ПРИМЕР 2 — Заголовок и дата стоят рядом, но НЕ являются парой.
            HTML:
              <p id="22229_4249">Чек по операции</p>
              <p id="22229_4250">14 июля 2023</p>
            Ответ: в ответ эти элементы НЕ попадают. Ни одному из них groupId не назначается.

            ПРИМЕР 3 — Самостоятельные подписи без значений в том же макете.
            HTML:
              <div id="22229_4240">
                <p id="22229_4241">Офис банка</p>
                <p id="22229_4242">Операция</p>
              </div>
            Ответ: если для этих подписей нет соответствующих значений — в ответ НЕ включай.

            ПРИМЕР 4 — Большая колонка значений напротив колонки подписей (частый паттерн чеков).
            HTML:
              <div id="22229_4251">
                <p id="22229_4252">С карты</p>
                <p id="22229_4253">Банк плательщика</p>
                <p id="22229_4254">БИК</p>
                <p id="22229_4255">Кор. счёт</p>
                <p id="22229_4256">ФИО плательщика</p>
                <p id="22229_4257">Сумма</p>
                <p id="22229_4258">Комиссия</p>
                <p id="22229_4259">Цель перевода</p>
                <p id="22229_4260">Назначение</p>
                <p id="22229_4261">Номер документа</p>
              </div>
              <div id="22229_4272">
                <p id="22229_4273">Полностью ?</p>
                <p id="22229_4274">ПАО Сбербанк Г. Москва</p>
                <p id="22229_4275">044525225</p>
                <p id="22229_4276">30101 810 4 0000 0000225</p>
                <p id="22229_4277">Иван Иванович Иванов</p>
                <p id="22229_4278">30 ₽</p>
                <p id="22229_4279">30 ₽</p>
                <p id="22229_4280">Другое</p>
                <p id="22229_4281">Не обязательно</p>
                <p id="22229_4282">65677109</p>
              </div>
            Ответ:
              { "elementId": "22229_4252", "groupId": "22229_4252" }
              { "elementId": "22229_4273", "groupId": "22229_4252" }
              { "elementId": "22229_4253", "groupId": "22229_4253" }
              { "elementId": "22229_4274", "groupId": "22229_4253" }
              { "elementId": "22229_4254", "groupId": "22229_4254" }
              { "elementId": "22229_4275", "groupId": "22229_4254" }
              { "elementId": "22229_4255", "groupId": "22229_4255" }
              { "elementId": "22229_4276", "groupId": "22229_4255" }
              { "elementId": "22229_4256", "groupId": "22229_4256" }
              { "elementId": "22229_4277", "groupId": "22229_4256" }
              { "elementId": "22229_4257", "groupId": "22229_4257" }
              { "elementId": "22229_4278", "groupId": "22229_4257" }
              { "elementId": "22229_4258", "groupId": "22229_4258" }
              { "elementId": "22229_4279", "groupId": "22229_4258" }
              { "elementId": "22229_4259", "groupId": "22229_4259" }
              { "elementId": "22229_4280", "groupId": "22229_4259" }
              { "elementId": "22229_4260", "groupId": "22229_4260" }
              { "elementId": "22229_4281", "groupId": "22229_4260" }
              { "elementId": "22229_4261", "groupId": "22229_4261" }
              { "elementId": "22229_4282", "groupId": "22229_4261" }

            ПРИМЕР 5 — Многострочная подпись.
            HTML:
              <div id="22229_4262">
                <p id="22229_4262_0"><span id="22229_4262_0_1">Дополнительная</span></p>
                <p id="22229_4262_1"><span id="22229_4262_1_1">информация</span></p>
              </div>
            Две строки образуют одну подпись «Дополнительная информация». groupId пары равен id
            первой строки подписи (22229_4262_0). Обе строки подписи получают groupId = 22229_4262_0.

            ════════════════════════════════════════════
            ОГРАНИЧЕНИЯ НА elementId В ОТВЕТЕ
            ════════════════════════════════════════════

            • elementId в ответе — ТОЛЬКО значение атрибута id HTML-элемента (никогда class).
            • В ответ включай ТОЛЬКО те elementId, которые присутствуют в переданном whitelist.
            • Whitelist передаётся в user-message как JSON-массив. Любые другие id игнорируй.
            • Не придумывай новые id и не берите id родительских div, если они не в whitelist.

            ════════════════════════════════════════════
            ФОРМАТ ОТВЕТА
            ════════════════════════════════════════════

            Верни валидный JSON, соответствующий схеме LlmGroupLinksResponse:
            {
              "links": [
                { "elementId": "...", "groupId": "..." },
                ...
              ]
            }
            Без пояснений, комментариев, markdown. Начинай сразу с {.
            Элементы без пары НЕ включай в links.
            """;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public GroupIdDetectionStep(ChatClient.Builder builder, ObjectMapper objectMapper) {
        this.chatClient = builder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    public void execute(PipelineContext context) throws JsonProcessingException {
        LlmTemplateData data = context.getLlmTemplateData();
        if (data == null || data.getElements() == null || data.getElements().isEmpty()) {
            return;
        }

        Map<String, LlmElement> elementsById = indexElementsById(data.getElements());
        String userMessage = buildUserMessage(context.getInputHtml(), elementsById.keySet());

        LlmGroupLinksResponse response = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .advisors(SimpleLoggerAdvisor.builder().build())
                .user(userMessage)
                .call()
                .entity(LlmGroupLinksResponse.class);

        applyLinks(response, elementsById);
    }

    private Map<String, LlmElement> indexElementsById(List<LlmElement> elements) {
        return elements.stream()
                .filter(e -> e.getElementId() != null)
                .collect(Collectors.toMap(LlmElement::getElementId, Function.identity()));
    }

    private String buildUserMessage(String html, Set<String> allowedIds) throws JsonProcessingException {
        String allowedIdsJson = objectMapper.writeValueAsString(allowedIds);
        return USER_MESSAGE_TEMPLATE.render(Map.of("allowedIds", allowedIdsJson, "html", html));
    }

    private void applyLinks(LlmGroupLinksResponse response, Map<String, LlmElement> elementsById) {
        if (response == null || response.getLinks() == null) {
            return;
        }
        for (LlmGroupLink link : response.getLinks()) {
            applyLink(link, elementsById);
        }
    }

    private void applyLink(LlmGroupLink link, Map<String, LlmElement> elementsById) {
        LlmElement element = elementsById.get(link.getElementId());
        if (element == null || !elementsById.containsKey(link.getGroupId())) {
            return;
        }
        element.setGroupId(link.getGroupId());
    }
}
