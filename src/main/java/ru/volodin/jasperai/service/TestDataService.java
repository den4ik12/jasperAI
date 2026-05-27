package ru.volodin.jasperai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TestDataService {

    private static final String SYSTEM_PROMPT = """
            Ты генератор тестовых данных для JasperReports datasource (формат JsonDataSource).
            
            Входные данные: JRXML-файл отчёта.
            
            Задача:
            1. Найди все поля вида $F{fieldName} в JRXML.
            2. Сгенерируй ровно 5 различных вариантов тестовых данных в виде JSON-массива объектов.
            
            Варианты (строго в таком порядке):
            1. name="Реалистичные данные" — запись с правдоподобными значениями (ФИО, суммы, даты, адреса).
            2. name="Минимальные данные" — запись, только обязательные поля, остальные null.
            3. name="Длинный текст" — запись, текстовые поля содержат максимально длинные строки для проверки переполнения.
            
            Формат ответа — строго валидный JSON-массив:
            [
              {
                "name": "...",
                "description": "...",
                "records": [ { "fieldName": "value", ... }, ... ]
              }
            ]
            
            Требования:
            - Выведи ТОЛЬКО JSON, без пояснений, комментариев, markdown-оберток.
            - Начинай сразу с [.
            - records — массив объектов, где ключи — имена полей из JRXML (без $F{ }).
            - Типы значений: числовые поля — числа, строковые — строки, даты — строки формата dd.MM.yyyy.
            """;

    private final ChatClient chatClient;

    public TestDataService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public List<LlmVariant> generate(String jrxml) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .advisors(SimpleLoggerAdvisor.builder()
                                  .build())
                .user(jrxml)
                .call()
                .entity(new ParameterizedTypeReference<>() {});
    }
}
