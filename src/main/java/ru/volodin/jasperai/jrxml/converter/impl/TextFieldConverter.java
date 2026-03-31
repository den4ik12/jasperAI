package ru.volodin.jasperai.jrxml.converter.impl;

import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.design.JRDesignElement;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignStyle;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.engine.type.TextAdjustEnum;
import net.sf.jasperreports.engine.type.VerticalTextAlignEnum;
import org.springframework.stereotype.Component;
import ru.volodin.jasperai.domain.ElementType;
import ru.volodin.jasperai.domain.JrxmlElement;
import ru.volodin.jasperai.jrxml.converter.CommonAttributesApplicator;
import ru.volodin.jasperai.jrxml.converter.JrxmlElementConverter;
import ru.volodin.jasperai.jrxml.style.StyleKey;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class TextFieldConverter implements JrxmlElementConverter {

    private final CommonAttributesApplicator applicator;

    @Override
    public ElementType getSupportedType() {
        return ElementType.TEXT_FIELD;
    }

    @Override
    public JRDesignElement convert(JrxmlElement element, Map<StyleKey, JRDesignStyle> stylesMap) {
        JRDesignTextField textField = new JRDesignTextField();
        textField.setExpression(buildExpression(element));
        applyTextAdjust(textField, element);
        applyAlignment(textField, element);
        applicator.applyCommonAttributes(textField, element, stylesMap);
        return textField;
    }

    private JRDesignExpression buildExpression(JrxmlElement element) {
        JRDesignExpression expression = new JRDesignExpression();
        expression.setText("$F{" + element.getContent() + "}");
        return expression;
    }

    private void applyTextAdjust(JRDesignTextField target, JrxmlElement element) {
        if (element.getTextAdjust() == null) {
            return;
        }
        TextAdjustEnum adjust = TextAdjustEnum.getByName(element.getTextAdjust());
        if (adjust != null) {
            target.setTextAdjust(adjust);
        }
    }

    private void applyAlignment(JRDesignTextField target, JrxmlElement element) {
        if (element.getHorizontalAlignment() != null) {
            HorizontalTextAlignEnum h = HorizontalTextAlignEnum.getByName(element.getHorizontalAlignment());
            if (h != null) target.setHorizontalTextAlign(h);
        }
        if (element.getVerticalAlignment() != null) {
            VerticalTextAlignEnum v = VerticalTextAlignEnum.getByName(element.getVerticalAlignment());
            if (v != null) target.setVerticalTextAlign(v);
        }
    }
}
