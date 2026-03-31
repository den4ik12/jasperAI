package ru.volodin.jasperai.pipeline.step;

import org.springframework.stereotype.Component;
import ru.volodin.jasperai.domain.ElementType;
import ru.volodin.jasperai.domain.LlmElement;
import ru.volodin.jasperai.domain.LlmTemplateData;
import ru.volodin.jasperai.pipeline.PipelineContext;
import ru.volodin.jasperai.pipeline.PipelineStep;

import java.util.List;

@Component
public class EmptyStaticTextFilterStep implements PipelineStep {

    @Override
    public void execute(PipelineContext context) {
        LlmTemplateData data = context.getLlmTemplateData();
        List<LlmElement> filtered = data.getElements().stream()
                .filter(this::isNotEmptyStaticText)
                .toList();
        data.setElements(filtered);
    }

    private boolean isNotEmptyStaticText(LlmElement element) {
        if (element.getType() != ElementType.STATIC_TEXT) {
            return true;
        }
        String content = element.getContent();
        return content != null && !content.isBlank();
    }
}
