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
public class LlmGroupLink {

    @JsonProperty(required = true)
    @JsonPropertyDescription("Значение атрибута id HTML-элемента, которому назначается groupId. Должно совпадать с одним из id из переданного whitelist.")
    private String elementId;

    @JsonProperty(required = true)
    @JsonPropertyDescription("Идентификатор смысловой пары «подпись → значение». Равен id HTML-элемента подписи (STATIC_TEXT) из этой пары. У подписи и у значения groupId одинаковый.")
    private String groupId;
}
