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
import ru.volodin.jasperai.pipeline.step.FontSizeNormalizationStep;
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
    private final FontSizeNormalizationStep fontSizeNormalizationStep;
    private final JrxmlGenerationStep jrxmlGenerationStep;
    private final JrxmlValidationStep jrxmlValidationStep;

    public String generateValidReport(GenerateRequest request) throws Exception {
        PipelineContext context = new PipelineContext();
        context.setInputHtml(request.getHtml());
        context.setInputCoordinates(request.getCoordinates());
        context.setLlmTemplateData(request.getLlmTemplateData());
        context.setTargetFormat(request.getTargetFormat());
        context.setPrintArea(request.getPrintArea());
        context.setSourcePageWidth(resolvePageWidth(request));
        context.setSourcePageHeight(resolvePageHeight(request));

        Pipeline.builder()
                .step(emptyStaticTextFilterStep)
                .step(coordinateEnrichmentStep)
                .step(coordinateScalingStep)
                .step(frameGroupingStep)
                .step(dynamicStretchStep)
                .step(hideEmptyRowsStep)
                .step(fontSizeNormalizationStep)
                .step(jrxmlGenerationStep)
                .step(jrxmlValidationStep)
                .build()
                .execute(context);

        return context.getLastJrxml();
    }

    private Integer resolvePageWidth(GenerateRequest request) {
        if (request.getPageWidth() != null) return request.getPageWidth();
        return request.getPrintArea() != null ? request.getPrintArea().getWidth() : null;
    }

    private Integer resolvePageHeight(GenerateRequest request) {
        if (request.getPageHeight() != null) return request.getPageHeight();
        return request.getPrintArea() != null ? request.getPrintArea().getHeight() : null;
    }
}
