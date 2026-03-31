package ru.volodin.jasperai.pipeline;

import lombok.Getter;
import lombok.Setter;
import ru.volodin.jasperai.controller.dto.Coordinate;
import ru.volodin.jasperai.domain.JrxmlTemplateData;
import ru.volodin.jasperai.domain.LlmTemplateData;
import ru.volodin.jasperai.domain.PageFormat;

import java.util.List;

@Getter
@Setter
public class PipelineContext {

    private String inputHtml;
    private List<Coordinate> inputCoordinates;
    private String lastJrxml;
    private LlmTemplateData llmTemplateData;
    private JrxmlTemplateData jrxmlTemplateData;
    private PageFormat targetFormat;
    private Integer sourcePageWidth;
    private Integer sourcePageHeight;
}
