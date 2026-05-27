package ru.volodin.jasperai.pipeline.step;

import org.junit.jupiter.api.Test;
import ru.volodin.jasperai.domain.ElementType;
import ru.volodin.jasperai.domain.JrxmlElement;
import ru.volodin.jasperai.domain.JrxmlFrame;
import ru.volodin.jasperai.domain.JrxmlTemplateData;
import ru.volodin.jasperai.pipeline.PipelineContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DynamicStretchStepTest {

    @Test
    void stretchesOnlyDynamicTextAndKeepsStaticTextFixedInsideMixedFrame() {
        JrxmlElement label = JrxmlElement.builder()
                .type(ElementType.STATIC_TEXT)
                .content("Назначение")
                .build();
        JrxmlElement value = JrxmlElement.builder()
                .type(ElementType.TEXT_FIELD)
                .content("paymentPurpose")
                .build();
        PipelineContext context = new PipelineContext();
        context.setJrxmlTemplateData(JrxmlTemplateData.builder()
                .elements(List.of())
                .frames(List.of(JrxmlFrame.builder()
                        .elements(List.of(label, value))
                        .build()))
                .build());

        new DynamicStretchStep().execute(context);

        assertEquals("Float", label.getPositionType());
        assertNull(label.getTextAdjust());
        assertNull(label.getStretchType());

        assertEquals("Float", value.getPositionType());
        assertEquals("StretchHeight", value.getTextAdjust());
        assertEquals("Top", value.getVerticalAlignment());
        assertNull(value.getStretchType());
    }
}
