package ru.volodin.jasperai.jrxml.converter.impl;

import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.design.JRDesignElement;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignImage;
import net.sf.jasperreports.engine.design.JRDesignStyle;
import net.sf.jasperreports.engine.type.ScaleImageEnum;
import org.springframework.stereotype.Component;
import ru.volodin.jasperai.domain.ElementType;
import ru.volodin.jasperai.domain.JrxmlElement;
import ru.volodin.jasperai.jrxml.converter.CommonAttributesApplicator;
import ru.volodin.jasperai.jrxml.converter.JrxmlElementConverter;
import ru.volodin.jasperai.jrxml.style.StyleKey;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ImageConverter implements JrxmlElementConverter {

    private final CommonAttributesApplicator applicator;

    @Override
    public ElementType getSupportedType() {
        return ElementType.IMAGE;
    }

    @Override
    public JRDesignElement convert(JrxmlElement element, Map<StyleKey, JRDesignStyle> stylesMap) {
        JRDesignImage image = new JRDesignImage(null);
        image.setExpression(buildExpression(element));
        applyScaleImage(image, element);
        applicator.applyCommonAttributes(image, element, stylesMap);
        return image;
    }

    private JRDesignExpression buildExpression(JrxmlElement element) {
        JRDesignExpression expression = new JRDesignExpression();
        expression.setText("\"" + (element.getImageUrl() != null ? element.getImageUrl() : "") + "\"");
        return expression;
    }

    private void applyScaleImage(JRDesignImage target, JrxmlElement element) {
        if (element.getScaleImage() == null) {
            return;
        }
        ScaleImageEnum scale = ScaleImageEnum.getByName(element.getScaleImage());
        if (scale != null) {
            target.setScaleImage(scale);
        }
    }
}
