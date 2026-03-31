package ru.volodin.jasperai.pipeline.step;

import org.springframework.stereotype.Component;
import ru.volodin.jasperai.domain.ElementType;
import ru.volodin.jasperai.domain.JrxmlElement;
import ru.volodin.jasperai.domain.JrxmlFrame;
import ru.volodin.jasperai.domain.JrxmlTemplateData;
import ru.volodin.jasperai.pipeline.PipelineContext;
import ru.volodin.jasperai.pipeline.PipelineStep;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FrameGroupingStep implements PipelineStep {

    @Override
    public void execute(PipelineContext context) {
        JrxmlTemplateData data = context.getJrxmlTemplateData();
        List<JrxmlElement> elements = new ArrayList<>(data.getElements());

        elements.sort(getElementPrioritySorter());

        Map<String, List<JrxmlElement>> grouped = elements.stream()
                .filter(e -> e.getGroupId() != null)
                .collect(Collectors.groupingBy(JrxmlElement::getGroupId));

        data.setElements(elements);

        if (grouped.isEmpty()) {
            return;
        }

        List<JrxmlFrame> frames = grouped.values().stream()
                .map(this::buildFrame)
                .collect(Collectors.toList());

        data.setFrames(frames);
        elements.removeIf(e -> e.getGroupId() != null);
    }

    private JrxmlFrame buildFrame(List<JrxmlElement> elements) {
        int frameX = elements.stream().mapToInt(JrxmlElement::getX).min().orElse(0);
        int frameY = elements.stream().mapToInt(JrxmlElement::getY).min().orElse(0);
        int frameWidth = elements.stream()
                .mapToInt(e -> e.getX() + Math.max(e.getWidth(), 1))
                .max().orElse(0) - frameX;
        int frameHeight = elements.stream()
                .mapToInt(e -> e.getY() + Math.max(e.getHeight(), 1))
                .max().orElse(0) - frameY;

        adjustElementCoordinates(elements, frameX, frameY);
        elements.sort(getElementPrioritySorter());

        return JrxmlFrame.builder()
                .x(frameX)
                .y(frameY)
                .width(frameWidth)
                .height(frameHeight)
                .elements(elements)
                .build();
    }

    private void adjustElementCoordinates(List<JrxmlElement> elements, int frameX, int frameY) {
        elements.forEach(e -> {
            e.setX(e.getX() - frameX);
            e.setY(e.getY() - frameY);
        });
    }

    private Comparator<JrxmlElement> getElementPrioritySorter() {
        return Comparator.comparingInt(e -> e.getType() == ElementType.RECTANGLE || e.getType() == ElementType.IMAGE ? 0 : 1);
    }
}
