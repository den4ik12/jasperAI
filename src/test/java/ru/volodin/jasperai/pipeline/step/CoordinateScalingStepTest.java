package ru.volodin.jasperai.pipeline.step;

import org.junit.jupiter.api.Test;
import ru.volodin.jasperai.domain.ElementType;
import ru.volodin.jasperai.domain.JrxmlElement;
import ru.volodin.jasperai.domain.JrxmlFrame;
import ru.volodin.jasperai.domain.JrxmlTemplateData;
import ru.volodin.jasperai.domain.PageFormat;
import ru.volodin.jasperai.pipeline.PipelineContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CoordinateScalingStepTest {

    private final CoordinateScalingStep step = new CoordinateScalingStep();

    @Test
    void noOpWhenPageWidthAlreadyMatchesTarget() {
        JrxmlElement el = sampleElement(100, 200, 300, 50);
        el.setFontSize(14);
        el.setBorderWidth(1);

        JrxmlTemplateData data = JrxmlTemplateData.builder()
                .pageWidth(595)
                .pageHeight(842)
                .elements(List.of(el))
                .build();

        step.execute(contextWith(data, PageFormat.A4));

        assertEquals(595, data.getPageWidth());
        assertEquals(842, data.getPageHeight());
        assertEquals(100, el.getX());
        assertEquals(200, el.getY());
        assertEquals(300, el.getWidth());
        assertEquals(50, el.getHeight());
        assertEquals(14, el.getFontSize());
        assertEquals(1, el.getBorderWidth());
    }

    @Test
    void defaultsToA4WhenTargetFormatNotSet() {
        JrxmlElement el = sampleElement(794, 0, 100, 100);
        JrxmlTemplateData data = JrxmlTemplateData.builder()
                .pageWidth(794)
                .pageHeight(1123)
                .elements(List.of(el))
                .build();

        PipelineContext ctx = new PipelineContext();
        ctx.setJrxmlTemplateData(data);

        step.execute(ctx);

        assertEquals(595, data.getPageWidth());
        assertEquals(842, data.getPageHeight());
    }

    @Test
    void downscalesFromBrowser794ToA4() {
        JrxmlElement el = sampleElement(100, 200, 400, 80);
        el.setFontSize(16);
        el.setLineHeight(20);
        el.setBorderWidth(2);

        JrxmlTemplateData data = JrxmlTemplateData.builder()
                .pageWidth(794)
                .pageHeight(1123)
                .elements(List.of(el))
                .build();

        step.execute(contextWith(data, PageFormat.A4));

        double k = 595.0 / 794.0;
        assertEquals(595, data.getPageWidth());
        assertEquals((int) Math.round(1123 * k), data.getPageHeight());
        assertEquals((int) Math.round(100 * k), el.getX());
        assertEquals((int) Math.round(200 * k), el.getY());
        assertEquals((int) Math.round(400 * k), el.getWidth());
        assertEquals((int) Math.round(80 * k), el.getHeight());
        assertEquals((int) Math.round(16 * k), el.getFontSize());
        assertEquals((int) Math.round(20 * k), el.getLineHeight());
        assertEquals((int) Math.round(2 * k), el.getBorderWidth());
    }

    @Test
    void upscalesFromA4ToA3() {
        JrxmlElement el = sampleElement(10, 20, 100, 40);
        el.setFontSize(10);

        JrxmlTemplateData data = JrxmlTemplateData.builder()
                .pageWidth(595)
                .pageHeight(842)
                .elements(List.of(el))
                .build();

        step.execute(contextWith(data, PageFormat.A3));

        double k = 842.0 / 595.0;
        assertEquals(842, data.getPageWidth());
        assertEquals((int) Math.round(842 * k), data.getPageHeight());
        assertEquals((int) Math.round(10 * k), el.getX());
        assertEquals((int) Math.round(20 * k), el.getY());
        assertEquals((int) Math.round(100 * k), el.getWidth());
        assertEquals((int) Math.round(40 * k), el.getHeight());
        assertEquals((int) Math.round(10 * k), el.getFontSize());
    }

    @Test
    void thinBorderWidthFlooredToOneWhenRoundsToZero() {
        JrxmlElement el = sampleElement(0, 0, 10, 10);
        el.setBorderWidth(1);

        JrxmlTemplateData data = JrxmlTemplateData.builder()
                .pageWidth(1440)
                .pageHeight(900)
                .elements(List.of(el))
                .build();

        step.execute(contextWith(data, PageFormat.A4));

        assertEquals(1, el.getBorderWidth(), "1px border rounds to 0 but must be forced to 1");
    }

    @Test
    void zeroBorderWidthStaysZero() {
        JrxmlElement el = sampleElement(0, 0, 10, 10);
        el.setBorderWidth(0);

        JrxmlTemplateData data = JrxmlTemplateData.builder()
                .pageWidth(1440)
                .pageHeight(900)
                .elements(List.of(el))
                .build();

        step.execute(contextWith(data, PageFormat.A4));

        assertEquals(0, el.getBorderWidth(), "zero border must not be upgraded to 1");
    }

    @Test
    void stylePropertiesAreNotAffected() {
        JrxmlElement el = JrxmlElement.builder()
                .type(ElementType.STATIC_TEXT)
                .x(100).y(100).width(200).height(50)
                .content("Hello World")
                .fontFamily("Arial")
                .isBold(Boolean.TRUE)
                .colorHex("#FF00FF")
                .backgroundColorHex("#00FF00")
                .borderColorHex("#000000")
                .horizontalAlignment("Center")
                .verticalAlignment("Middle")
                .textAdjustStretch(Boolean.TRUE)
                .build();

        JrxmlTemplateData data = JrxmlTemplateData.builder()
                .pageWidth(794)
                .pageHeight(1123)
                .elements(List.of(el))
                .build();

        step.execute(contextWith(data, PageFormat.A4));

        assertEquals("Hello World", el.getContent());
        assertEquals("Arial", el.getFontFamily());
        assertEquals(Boolean.TRUE, el.getIsBold());
        assertEquals("#FF00FF", el.getColorHex());
        assertEquals("#00FF00", el.getBackgroundColorHex());
        assertEquals("#000000", el.getBorderColorHex());
        assertEquals("Center", el.getHorizontalAlignment());
        assertEquals("Middle", el.getVerticalAlignment());
        assertEquals(Boolean.TRUE, el.getTextAdjustStretch());
        assertEquals(ElementType.STATIC_TEXT, el.getType());
    }

    @Test
    void nullNumericFieldsAreSkipped() {
        JrxmlElement el = JrxmlElement.builder()
                .type(ElementType.TEXT_FIELD)
                .x(50).y(60).width(100).height(20)
                .content("amount")
                .build();

        JrxmlTemplateData data = JrxmlTemplateData.builder()
                .pageWidth(794)
                .pageHeight(1123)
                .elements(List.of(el))
                .build();

        step.execute(contextWith(data, PageFormat.A4));

        assertNull(el.getFontSize());
        assertNull(el.getLineHeight());
        assertNull(el.getBorderWidth());
    }

    @Test
    void nestedFrameElementsAreScaled() {
        JrxmlElement inner = sampleElement(50, 60, 200, 40);
        inner.setFontSize(12);

        JrxmlFrame frame = JrxmlFrame.builder()
                .x(10).y(20).width(300).height(100)
                .elements(List.of(inner))
                .build();

        JrxmlElement topLevel = sampleElement(0, 0, 100, 20);

        JrxmlTemplateData data = JrxmlTemplateData.builder()
                .pageWidth(794)
                .pageHeight(1123)
                .elements(List.of(topLevel))
                .frames(List.of(frame))
                .build();

        step.execute(contextWith(data, PageFormat.A4));

        double k = 595.0 / 794.0;
        assertEquals((int) Math.round(10 * k), frame.getX());
        assertEquals((int) Math.round(20 * k), frame.getY());
        assertEquals((int) Math.round(300 * k), frame.getWidth());
        assertEquals((int) Math.round(100 * k), frame.getHeight());

        assertEquals((int) Math.round(50 * k), inner.getX());
        assertEquals((int) Math.round(60 * k), inner.getY());
        assertEquals((int) Math.round(200 * k), inner.getWidth());
        assertEquals((int) Math.round(40 * k), inner.getHeight());
        assertEquals((int) Math.round(12 * k), inner.getFontSize());

        assertEquals((int) Math.round(100 * k), topLevel.getWidth());
    }

    @Test
    void nullJrxmlTemplateDataIsSafe() {
        PipelineContext ctx = new PipelineContext();
        step.execute(ctx);
    }

    @Test
    void nullPageWidthIsSafe() {
        JrxmlTemplateData data = JrxmlTemplateData.builder()
                .pageHeight(1123)
                .elements(List.of())
                .build();

        step.execute(contextWith(data, PageFormat.A4));

        assertNull(data.getPageWidth());
        assertEquals(1123, data.getPageHeight());
    }

    @Test
    void fontSizeNeverCollapsesToZero() {
        JrxmlElement el = sampleElement(0, 0, 10, 10);
        el.setFontSize(1);

        JrxmlTemplateData data = JrxmlTemplateData.builder()
                .pageWidth(3000)
                .pageHeight(4000)
                .elements(List.of(el))
                .build();

        step.execute(contextWith(data, PageFormat.A4));

        assertEquals(1, el.getFontSize(), "tiny font that rounds to 0 must be clamped to 1");
    }

    @Test
    void letterFormatScalesCorrectly() {
        JrxmlElement el = sampleElement(100, 100, 200, 40);

        JrxmlTemplateData data = JrxmlTemplateData.builder()
                .pageWidth(612)
                .pageHeight(792)
                .elements(List.of(el))
                .build();

        step.execute(contextWith(data, PageFormat.LETTER));

        assertEquals(612, data.getPageWidth());
        assertEquals(792, data.getPageHeight());
        assertEquals(100, el.getX());
        assertEquals(200, el.getWidth());
    }

    private static JrxmlElement sampleElement(int x, int y, int w, int h) {
        return JrxmlElement.builder()
                .type(ElementType.TEXT_FIELD)
                .x(x).y(y).width(w).height(h)
                .content("v")
                .build();
    }

    private static PipelineContext contextWith(JrxmlTemplateData data, PageFormat format) {
        PipelineContext ctx = new PipelineContext();
        ctx.setJrxmlTemplateData(data);
        ctx.setTargetFormat(format);
        return ctx;
    }
}
