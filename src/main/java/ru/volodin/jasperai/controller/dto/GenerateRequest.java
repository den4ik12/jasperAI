package ru.volodin.jasperai.controller.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import ru.volodin.jasperai.domain.LlmTemplateData;
import ru.volodin.jasperai.domain.PageFormat;

import java.util.List;

@Data
@NoArgsConstructor
public class GenerateRequest {
    private String html;
    private List<Coordinate> coordinates;
    private LlmTemplateData llmTemplateData;
    private PageFormat targetFormat;
    private Integer pageWidth;
    private Integer pageHeight;
}
