package ru.volodin.jasperai.pipeline.step;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.volodin.jasperai.domain.JrxmlElement;
import ru.volodin.jasperai.domain.JrxmlFrame;
import ru.volodin.jasperai.domain.JrxmlTemplateData;
import ru.volodin.jasperai.domain.PageFormat;
import ru.volodin.jasperai.pipeline.PipelineContext;
import ru.volodin.jasperai.pipeline.PipelineStep;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoordinateScalingStep implements PipelineStep {

    private static final PageFormat DEFAULT_FORMAT = PageFormat.A4;
    private static final double EPSILON = 1e-9;

    @Override
    public void execute(PipelineContext context) {
        JrxmlTemplateData jrxmlData = context.getJrxmlTemplateData();
        if (jrxmlData == null || jrxmlData.getPageWidth() == null || jrxmlData.getPageWidth() <= 0) {
            return;
        }

        PageFormat targetFormat = context.getTargetFormat() != null ? context.getTargetFormat() : DEFAULT_FORMAT;
        double scaleFactor = (double) targetFormat.getWidth() / jrxmlData.getPageWidth();

        log.info("CoordinateScalingStep: target={}, pageWidth={}, pageHeight={}, scaleFactor={}",
                targetFormat, jrxmlData.getPageWidth(), jrxmlData.getPageHeight(), scaleFactor);

        if (Math.abs(scaleFactor - 1.0) < EPSILON) {
            jrxmlData.setPageWidth(targetFormat.getWidth());
            return;
        }

        scaleElements(jrxmlData.getElements(), scaleFactor);
        scaleFrames(jrxmlData.getFrames(), scaleFactor);
        scalePage(jrxmlData, scaleFactor, targetFormat);

        log.info("After scaling: pageWidth={}, pageHeight={}",
                jrxmlData.getPageWidth(), jrxmlData.getPageHeight());
    }

    private void scaleElements(List<JrxmlElement> elements, double scaleFactor) {
        if (elements == null) return;
        for (JrxmlElement element : elements) {
            scaleElement(element, scaleFactor);
        }
    }

    private void scaleElement(JrxmlElement element, double scaleFactor) {
        if (element.getX() != null) {
            element.setX(roundScaled(element.getX(), scaleFactor));
        }
        if (element.getY() != null) {
            element.setY(roundScaled(element.getY(), scaleFactor));
        }
        if (element.getWidth() != null) {
            element.setWidth(roundScaled(element.getWidth(), scaleFactor));
        }
        if (element.getHeight() != null) {
            element.setHeight(roundScaled(element.getHeight(), scaleFactor));
        }
        if (element.getFontSize() != null) {
            element.setFontSize(Math.max(1, roundScaled(element.getFontSize(), scaleFactor)));
        }
        if (element.getLineHeight() != null) {
            element.setLineHeight(roundScaled(element.getLineHeight(), scaleFactor));
        }
        if (element.getBorderWidth() != null) {
            element.setBorderWidth(scaleBorderWidth(element.getBorderWidth(), scaleFactor));
        }
    }

    private void scaleFrames(List<JrxmlFrame> frames, double scaleFactor) {
        if (frames == null) return;
        for (JrxmlFrame frame : frames) {
            if (frame.getX() != null) frame.setX(roundScaled(frame.getX(), scaleFactor));
            if (frame.getY() != null) frame.setY(roundScaled(frame.getY(), scaleFactor));
            if (frame.getWidth() != null) frame.setWidth(roundScaled(frame.getWidth(), scaleFactor));
            if (frame.getHeight() != null) frame.setHeight(roundScaled(frame.getHeight(), scaleFactor));
            scaleElements(frame.getElements(), scaleFactor);
        }
    }

    private void scalePage(JrxmlTemplateData data, double scaleFactor, PageFormat targetFormat) {
        data.setPageWidth(targetFormat.getWidth());
        if (data.getPageHeight() != null) {
            data.setPageHeight(roundScaled(data.getPageHeight(), scaleFactor));
        }
    }

    private int scaleBorderWidth(int original, double scaleFactor) {
        if (original <= 0) {
            return original;
        }
        int scaled = roundScaled(original, scaleFactor);
        return Math.max(scaled, 1);
    }

    private int roundScaled(int value, double scaleFactor) {
        return (int) Math.round(value * scaleFactor);
    }
}
