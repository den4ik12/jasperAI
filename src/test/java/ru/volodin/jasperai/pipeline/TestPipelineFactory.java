package ru.volodin.jasperai.pipeline;

import ru.volodin.jasperai.jrxml.converter.CommonAttributesApplicator;
import ru.volodin.jasperai.jrxml.converter.JrxmlElementConverter;
import ru.volodin.jasperai.jrxml.converter.impl.ImageConverter;
import ru.volodin.jasperai.jrxml.converter.impl.RectangleConverter;
import ru.volodin.jasperai.jrxml.converter.impl.StaticTextConverter;
import ru.volodin.jasperai.jrxml.converter.impl.TextFieldConverter;
import ru.volodin.jasperai.pipeline.step.CoordinateEnrichmentStep;
import ru.volodin.jasperai.pipeline.step.DynamicStretchStep;
import ru.volodin.jasperai.pipeline.step.FrameGroupingStep;
import ru.volodin.jasperai.pipeline.step.HideEmptyRowsStep;
import ru.volodin.jasperai.pipeline.step.JrxmlGenerationStep;

import java.util.List;

public class TestPipelineFactory {

    public Pipeline buildJrxmlGenerationOnlyPipeline() {
        List<JrxmlElementConverter> converters = buildConverters();
        return Pipeline.builder()
                .step(new CoordinateEnrichmentStep())
                .step(new FrameGroupingStep())
                .step(new DynamicStretchStep())
                .step(new HideEmptyRowsStep())
                .step(new JrxmlGenerationStep(converters))
                .build();
    }

    private List<JrxmlElementConverter> buildConverters() {
        CommonAttributesApplicator applicator = new CommonAttributesApplicator();
        return List.of(
                new StaticTextConverter(applicator),
                new TextFieldConverter(applicator),
                new RectangleConverter(applicator),
                new ImageConverter(applicator)
        );
    }
}
