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
public class LlmGroupLinksResponse {

    @JsonProperty(required = true)
    @JsonPropertyDescription("Список связей между элементами и их groupId. Включай только те elementId, которые реально входят в смысловую пару «подпись → значение». Элементы без пары не перечисляй.")
    private List<LlmGroupLink> links;
}
