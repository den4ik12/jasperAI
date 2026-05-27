package ru.volodin.jasperai.pipeline.step;

import org.junit.jupiter.api.Test;
import ru.volodin.jasperai.domain.ElementType;
import ru.volodin.jasperai.domain.JrxmlElement;
import ru.volodin.jasperai.domain.JrxmlFrame;
import ru.volodin.jasperai.domain.JrxmlTemplateData;
import ru.volodin.jasperai.pipeline.PipelineContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FrameGroupingStepTest {

    @Test
    void groupsElementsWithSameGroupIdIntoFrameAndKeepsOthersFlat() {
        JrxmlElement label = element("label", "row1", ElementType.STATIC_TEXT, 10, 20, 40, 12);
        JrxmlElement value = element("value", "row1", ElementType.TEXT_FIELD, 70, 20, 80, 14);
        JrxmlElement title = element("title", null, ElementType.STATIC_TEXT, 10, 5, 100, 12);

        PipelineContext context = new PipelineContext();
        context.setJrxmlTemplateData(JrxmlTemplateData.builder()
                .elements(List.of(label, value, title))
                .build());

        new FrameGroupingStep().execute(context);

        JrxmlTemplateData data = context.getJrxmlTemplateData();
        assertEquals(List.of(title), data.getElements());
        assertEquals(1, data.getFrames().size());

        JrxmlFrame frame = data.getFrames().get(0);
        assertEquals(10, frame.getX());
        assertEquals(20, frame.getY());
        assertEquals(140, frame.getWidth());
        assertEquals(14, frame.getHeight());
        assertEquals(0, label.getX());
        assertEquals(0, label.getY());
        assertEquals(60, value.getX());
        assertEquals(0, value.getY());
        assertEquals(List.of(label, value), frame.getElements());
    }

    private JrxmlElement element(String id, String groupId, ElementType type, int x, int y, int width, int height) {
        return JrxmlElement.builder()
                .elementId(id)
                .groupId(groupId)
                .type(type)
                .x(x)
                .y(y)
                .width(width)
                .height(height)
                .content(id)
                .build();
    }
}
