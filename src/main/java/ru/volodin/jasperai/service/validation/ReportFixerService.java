package ru.volodin.jasperai.service.validation;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ReportFixerService {

    private static final String SYSTEM_PROMPT = """
            У тебя есть jrxml с ошибками и описание ошибки. Исправь все ошибки в нём и верни ТОЛЬКО исправленный JRXML.

            Требования:
            - Не добавляй никаких комментариев, пояснений, описаний.
            - Не меняй структуру, атрибуты и содержимое, кроме необходимых исправлений.
            - Не экранируй строки: сохраняй <![CDATA[...]]> секции без изменений, не заменяй их на &amp;, &lt;, &gt;.
            - Не экранируй символы: выводи сырой текст без \\", \\n, \\t и других escape-последовательностей.
            - Теги <text> и <textFieldExpression> пиши строго в одну строку без переносов: <text><![CDATA[...]]></text>.
            - Выведи только чистый JRXML (начиная с <?xml ...> и заканчивая </jasperReport>).
            """;

    private final ChatClient chatClient;

    public ReportFixerService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String fixReport(String jrxml, String errorDescription) {
        Prompt fixedPrompt = PromptTemplate.builder()
                .template("Ошибочный jrxml: {jrxml} \n Описание ошибки: {error_description}")
                .variables(Map.of("jrxml",
                                  jrxml,
                                  "error_description",
                                  errorDescription))
                .build()
                .create();

        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(fixedPrompt.getContents())
                .call()
                .content();
    }
}
