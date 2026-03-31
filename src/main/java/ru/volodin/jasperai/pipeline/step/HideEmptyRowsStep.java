package ru.volodin.jasperai.pipeline.step;

import org.springframework.stereotype.Component;
import ru.volodin.jasperai.domain.ElementType;
import ru.volodin.jasperai.domain.JrxmlElement;
import ru.volodin.jasperai.domain.JrxmlFrame;
import ru.volodin.jasperai.domain.JrxmlTemplateData;
import ru.volodin.jasperai.pipeline.PipelineContext;
import ru.volodin.jasperai.pipeline.PipelineStep;

import java.util.List;

@Component
public class HideEmptyRowsStep implements PipelineStep {

    @Override
    public void execute(PipelineContext context) {
        JrxmlTemplateData data = context.getJrxmlTemplateData();
        data.getElements().stream()
                .filter(e -> e.getType() == ElementType.TEXT_FIELD)
                .forEach(e -> e.setIsRemoveLineWhenBlank(true));
        if (data.getFrames() != null) {
            data.getFrames().forEach(this::applyToFrame);
        }
    }

    private void applyToFrame(JrxmlFrame frame) {
        List<JrxmlElement> textFields = frame.getElements().stream()
                .filter(e -> e.getType() == ElementType.TEXT_FIELD)
                .toList();
        if (textFields.isEmpty()) {
            return;
        }
        frame.setIsRemoveLineWhenBlank(true);
        // TODO: Текущая логика скрывает фрейм если первое поле пусто. Рассмотреть вариант скрывать фрейм только если все текстовые поля пусты одновременно.
        frame.setPrintWhenFieldName(textFields.get(0).getContent());
        textFields.forEach(e -> e.setIsRemoveLineWhenBlank(true));
    }
}
