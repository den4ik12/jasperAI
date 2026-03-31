package ru.volodin.jasperai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.volodin.jasperai.domain.LlmTemplateData;
import ru.volodin.jasperai.pipeline.Pipeline;
import ru.volodin.jasperai.pipeline.PipelineContext;
import ru.volodin.jasperai.pipeline.step.HtmlStructuredExtractionStep;

@Service
@RequiredArgsConstructor
public class ElementExtractionService {

    private final HtmlStructuredExtractionStep htmlStructuredExtractionStep;

    public LlmTemplateData extract(String html) throws Exception {
        PipelineContext context = new PipelineContext();
        context.setInputHtml(html);

        Pipeline.builder()
                .step(htmlStructuredExtractionStep)
                .build()
                .execute(context);

        return context.getLlmTemplateData();
    }
}
