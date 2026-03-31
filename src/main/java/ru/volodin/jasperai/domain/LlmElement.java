package ru.volodin.jasperai.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LlmElement {

    @JsonProperty(required = true)
    @JsonPropertyDescription("Значение атрибута id соответствующего HTML-элемента.")
    private String elementId;

    @JsonProperty(required = true)
    @JsonPropertyDescription("Тип элемента: STATIC_TEXT для статических надписей, TEXT_FIELD для динамически меняющихся значений, RECTANGLE для фигур/фонов, IMAGE для картинок.")
    private ElementType type;

    @JsonProperty(required = true)
    @JsonPropertyDescription("Для STATIC_TEXT: точный текст. Для TEXT_FIELD: сгенерируй логичное имя параметра в camelCase (например, 'amount', 'operationNumber').")
    private String content;

    @JsonProperty
    @JsonPropertyDescription("Шрифт (font-family), извлеченный из CSS.")
    private String fontFamily;

    @JsonProperty
    @JsonPropertyDescription("Размер шрифта (font-size) из CSS в виде целого числа.")
    private Integer fontSize;

    @JsonProperty
    @JsonPropertyDescription("true, если font-weight в CSS равен Bold или >= 700.")
    private Boolean isBold;

    @JsonProperty
    @JsonPropertyDescription("Цвет текста из CSS. Возвращай значение как есть (например, #000000, rgb(0,0,0), rgba(0,0,0,1)).")
    private String colorHex;

    @JsonProperty
    @JsonPropertyDescription("Выравнивание текста по горизонтали. Может быть 'Left', 'Center' или 'Right'.")
    private String horizontalAlignment;

    @JsonProperty
    @JsonPropertyDescription("Выравнивание текста по вертикали. Обычно 'Middle', если используется flex/justify-content, иначе 'Top'.")
    private String verticalAlignment;

    @JsonProperty
    @JsonPropertyDescription("Установи в true ТОЛЬКО если в CSS самого элемента явно задано 'white-space: pre-wrap' или 'height: auto'. Значение 'white-space: pre-line' — не является основанием.")
    private Boolean textAdjustStretch;

    @JsonProperty
    @JsonPropertyDescription("Межстрочный интервал из CSS самого элемента, только если задан в явных пикселях (не %, не em, не 'normal').")
    private Integer lineHeight;

    @JsonProperty
    @JsonPropertyDescription("Цвет фона из CSS как есть. Используется для элементов RECTANGLE.")
    private String backgroundColorHex;

    @JsonProperty
    @JsonPropertyDescription("URL или путь к файлу, извлеченный из свойства 'background-image' в CSS для элементов IMAGE.")
    private String imageUrl;

    @JsonProperty
    @JsonPropertyDescription("Если background-size равен 'cover', верни 'FillFrame', если 'contain', верни 'RetainShape'.")
    private String scaleImage;

    @JsonProperty
    @JsonPropertyDescription("""
            Идентификатор логической группы, в которую входит этот элемент. \
            Группа — это набор элементов, которые визуально составляют единый блок: \
            например, строка «Метка + Значение», секция с заголовком и содержимым, блок с иконкой и подписью. \
            В качестве groupId используй значение атрибута id их общего HTML-родителя (ближайшего контейнера, \
            объединяющего все элементы группы). \
            Все элементы одной группы должны иметь одинаковый groupId. \
            Элементы, которые не входят ни в какую смысловую группу, оставь без groupId. \
            Позже из каждой группы будет создан отдельный элемент <frame> в JasperReports.""")
    private String groupId;

    @JsonProperty
    @JsonPropertyDescription("Толщина рамки в пикселях, если указана.")
    private Integer borderWidth;

    @JsonProperty
    @JsonPropertyDescription("Цвет рамки из CSS как есть, если указан.")
    private String borderColorHex;
}
