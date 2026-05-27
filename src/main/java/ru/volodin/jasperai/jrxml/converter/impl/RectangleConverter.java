package ru.volodin.jasperai.jrxml.converter.impl;

import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.design.JRDesignElement;
import net.sf.jasperreports.engine.design.JRDesignRectangle;
import net.sf.jasperreports.engine.design.JRDesignStyle;
import net.sf.jasperreports.engine.type.ModeEnum;
import org.springframework.stereotype.Component;
import ru.volodin.jasperai.domain.ElementType;
import ru.volodin.jasperai.domain.JrxmlElement;
import ru.volodin.jasperai.jrxml.converter.CommonAttributesApplicator;
import ru.volodin.jasperai.jrxml.converter.JrxmlElementConverter;
import ru.volodin.jasperai.jrxml.style.StyleKey;

import java.awt.Color;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RectangleConverter implements JrxmlElementConverter {

    private final CommonAttributesApplicator applicator;

    @Override
    public ElementType getSupportedType() {
        return ElementType.RECTANGLE;
    }

    @Override
    public JRDesignElement convert(JrxmlElement element, Map<StyleKey, JRDesignStyle> stylesMap) {
        JRDesignRectangle rectangle = new JRDesignRectangle(null);
        applyBackcolor(rectangle, element);
        applyLinePen(rectangle, element);
        applicator.applyCommonAttributes(rectangle, element, stylesMap);
        return rectangle;
    }

    private void applyBackcolor(JRDesignRectangle target, JrxmlElement element) {
        Color backcolor = CommonAttributesApplicator.decodeColor(element.getBackgroundColorHex());
        if (backcolor != null) {
            target.setMode(ModeEnum.OPAQUE);
            target.setBackcolor(backcolor);
        }
    }

    private void applyLinePen(JRDesignRectangle target, JrxmlElement element) {
        if (element.getBorderWidth() != null && element.getBorderWidth() > 0) {
            target.getLinePen().setLineWidth((float) element.getBorderWidth());
        }
        Color borderColor = CommonAttributesApplicator.decodeColor(element.getBorderColorHex());
        if (borderColor != null) {
            target.getLinePen().setLineColor(borderColor);
        }
    }
}
