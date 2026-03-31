package ru.volodin.jasperai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.volodin.jasperai.pipeline.Pipeline;
import ru.volodin.jasperai.pipeline.PipelineContext;
import ru.volodin.jasperai.pipeline.step.CoordinateEnrichmentStep;
import ru.volodin.jasperai.pipeline.step.CoordinateScalingStep;
import ru.volodin.jasperai.pipeline.step.DynamicStretchStep;
import ru.volodin.jasperai.pipeline.step.EmptyStaticTextFilterStep;
import ru.volodin.jasperai.pipeline.step.FrameGroupingStep;
import ru.volodin.jasperai.pipeline.step.HideEmptyRowsStep;
import ru.volodin.jasperai.pipeline.step.JrxmlGenerationStep;
import ru.volodin.jasperai.pipeline.step.JrxmlValidationStep;
import ru.volodin.jasperai.controller.dto.GenerateRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerationService {

    private final EmptyStaticTextFilterStep emptyStaticTextFilterStep;
    private final CoordinateEnrichmentStep coordinateEnrichmentStep;
    private final CoordinateScalingStep coordinateScalingStep;
    private final FrameGroupingStep frameGroupingStep;
    private final DynamicStretchStep dynamicStretchStep;
    private final HideEmptyRowsStep hideEmptyRowsStep;
    private final JrxmlGenerationStep jrxmlGenerationStep;
    private final JrxmlValidationStep jrxmlValidationStep;

    public String generateValidReport(GenerateRequest request) throws Exception {
        PipelineContext context = new PipelineContext();
        context.setInputHtml(request.getHtml());
        context.setInputCoordinates(request.getCoordinates());
        context.setLlmTemplateData(request.getLlmTemplateData());
        context.setTargetFormat(request.getTargetFormat());
        context.setSourcePageWidth(request.getPageWidth());
        context.setSourcePageHeight(request.getPageHeight());

        Pipeline.builder()
                .step(emptyStaticTextFilterStep)
                .step(coordinateEnrichmentStep)
                .step(coordinateScalingStep)
                .step(frameGroupingStep)
                .step(dynamicStretchStep)
                .step(hideEmptyRowsStep)
                .step(jrxmlGenerationStep)
                .step(jrxmlValidationStep)
                .build()
                .execute(context);

        return context.getLastJrxml();
    }
}
