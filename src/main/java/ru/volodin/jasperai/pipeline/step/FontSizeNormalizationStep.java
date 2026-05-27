package ru.volodin.jasperai.pipeline.step;

import org.springframework.stereotype.Component;
import ru.volodin.jasperai.domain.JrxmlElement;
import ru.volodin.jasperai.domain.JrxmlTemplateData;
import ru.volodin.jasperai.pipeline.PipelineContext;
import ru.volodin.jasperai.pipeline.PipelineStep;

import java.util.stream.Stream;

@Component
public class FontSizeNormalizationStep implements PipelineStep {

    @Override
    public void execute(PipelineContext context) {
        JrxmlTemplateData data = context.getJrxmlTemplateData();
        allElements(data)
                .filter(e -> e.getFontSize() != null)
                .forEach(e -> e.setFontSize(e.getFontSize() - 1));
    }

    private Stream<JrxmlElement> allElements(JrxmlTemplateData data) {
        Stream<JrxmlElement> baseElements = data.getElements() == null ? Stream.empty() : data.getElements().stream();
        Stream<JrxmlElement> frameElements = data.getFrames() == null ? Stream.empty() :
                data.getFrames().stream().flatMap(f -> f.getElements() == null ? Stream.empty() : f.getElements().stream());
        return Stream.concat(baseElements, frameElements);
    }
}
