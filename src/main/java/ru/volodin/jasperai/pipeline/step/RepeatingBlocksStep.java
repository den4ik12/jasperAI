package ru.volodin.jasperai.pipeline.step;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import ru.volodin.jasperai.pipeline.PipelineContext;
import ru.volodin.jasperai.pipeline.PipelineStep;


/// пока не используется требуется доработка
@Component
public class RepeatingBlocksStep implements PipelineStep {

    private static final String SYSTEM_PROMPT = """
            Prompt 5 - Повторяющиеся блоки (jr:list).
            
            Источник данных:
            - Последний загруженный JRXML с моими исправлениями.
            - Это единственный source of truth.
            - Игнорируй все предыдущие версии и любые предположения о структуре.
            
            Задача:
            - Для данной печатной формы 3 блока внизу ("Исходящее", "Входящее", "Исходящее") являются повторяющимися и заполняются из массива данных.
            - Замени эти 3 блока одним jr:list, который визуально повторяет один блок.
            - Для этого используй инструменты Jasper Report из примеров внизу.
            - Размести subDataset в соответствии с требуемым порядком в XSD: между style и field.
            - Поля <field> в subDataset сформируй по $F{...} полям, используемым внутри одного исходного блока.
            - Удали <field>, вместо которых будет использоваться subDataset. Удаляй только те <field>, которые используются только внутри повторяющегося блока и больше нигде в отчёте.
            - Остальные field не переставляй.
            - Добавь xmlns:jr и components schemaLocation только если они отсутствуют. Не дублируй и не изменяй существующие.
            - Не добавляй атрибут uuid в новые элементы JRXML. Уже имеющиеся атрибуты uuid оставь без изменения.
            - Разрешены только изменения, необходимые для блока list, заполняемого из массива. Для остальных элементов координаты и размеры не менять.
            
            Примеры:
            1) subDataset для определения полей
            ```
            <subDataset name="subDatasetName">
              <field .../>
              ...
            </subDataset>
            ```
            2) Повторяющийся блок создается следующим образом:
            ```
            <jasperReport ...
                xmlns:jr="http://jasperreports.sourceforge.net/jasperreports/components"
                xsi:schemaLocation="
                    http://jasperreports.sourceforge.net/jasperreports http://jasperreports.sourceforge.net/xsd/jasperreport.xsd
                    http://jasperreports.sourceforge.net/jasperreports/components http://jasperreports.sourceforge.net/xsd/components.xsd"
            ...>
            ...
            <subDataset name="subDatasetName">
              <field name="field1" class="java.lang.String"/>
              <field name="field2" class="java.lang.String"/>
              ...
            </subDataset>
            ...
            <band height="HEIGHT" splitType="Stretch">
              <componentElement>
                <reportElement x="X" y="Y" width="WIDTH" height="HEIGHT"/>
                <jr:list>
                  <datasetRun subDataset="subDatasetName">
                    <dataSourceExpression>
                      <![CDATA[((net.sf.jasperreports.engine.data.JsonDataSource)$P{REPORT_DATA_SOURCE}).subDataSource("fieldWithArray")]]>
                    </dataSourceExpression>
                  </datasetRun>
                  <jr:listContents width="WIDTH" height="HEIGHT">
                    <frame>
                    ...
                    </frame>
                  </jr:listContents>
                </jr:list>
              </componentElement>
            </band>
            ```
            
            Формат вывода:
            - Выведи только валидный JRXML.
            - Без пояснений, комментариев, markdown, XML-комментариев.
            - Не экранируй строки: сохраняй <![CDATA[...]]> секции без изменений, не заменяй их на &amp;, &lt;, &gt;.
            - Не экранируй символы: выводи сырой текст без \\", \\n, \\t и других escape-последовательностей.
            - Вывод оберни в один XML-блок (code fence с языком xml).
            - Никакого текста до и после блока.
            - Внутри блока - корректное XML-форматирование (отступы и переносы строк).
            """;

    private final ChatClient chatClient;

    public RepeatingBlocksStep(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public void execute(PipelineContext context) {
        String jrxml = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(context.getLastJrxml())
                .call()
                .content();
        context.setLastJrxml(jrxml);
    }
}
