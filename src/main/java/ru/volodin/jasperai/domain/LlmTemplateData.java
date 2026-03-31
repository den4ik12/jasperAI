package ru.volodin.jasperai.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LlmTemplateData {

    @JsonProperty(required = true)
    @JsonPropertyDescription("Плоский список всех визуальных элементов, извлеченных из HTML/CSS.")
    private List<LlmElement> elements;
}
