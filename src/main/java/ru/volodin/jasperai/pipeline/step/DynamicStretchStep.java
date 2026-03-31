package ru.volodin.jasperai.pipeline.step;

import org.springframework.stereotype.Component;
import ru.volodin.jasperai.domain.JrxmlElement;
import ru.volodin.jasperai.domain.JrxmlTemplateData;
import ru.volodin.jasperai.pipeline.PipelineContext;
import ru.volodin.jasperai.pipeline.PipelineStep;

import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static ru.volodin.jasperai.domain.ElementType.STATIC_TEXT;
import static ru.volodin.jasperai.domain.ElementType.TEXT_FIELD;

@Component
public class DynamicStretchStep implements PipelineStep {

    @Override
    public void execute(PipelineContext context) {
        JrxmlTemplateData data = context.getJrxmlTemplateData();
        data.setBandSplitType("Stretch");
        applyStretchToElements(data.getElements());

        if (isNotEmpty(data.getFrames())) {
            data.getFrames().forEach(frame -> {
                frame.setPositionType("Float");
                applyStretchToElements(frame.getElements());
            });
        }
    }

    private void applyStretchToElements(List<JrxmlElement> elements) {
        elements.stream()
                .filter(e -> {
                    var elementType = e.getType();
                    return elementType == TEXT_FIELD || elementType == STATIC_TEXT;
                })
                .forEach(e -> {
                    e.setTextAdjust("StretchHeight");
                    e.setPositionType("Float");
                });
    }
}
