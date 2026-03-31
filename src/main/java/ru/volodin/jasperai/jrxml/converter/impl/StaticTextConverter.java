package ru.volodin.jasperai.jrxml.converter.impl;

import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.design.JRDesignElement;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignStyle;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
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
public class StaticTextConverter implements JrxmlElementConverter {

    private final CommonAttributesApplicator applicator;

    @Override
    public ElementType getSupportedType() {
        return ElementType.STATIC_TEXT;
    }

    @Override
    public JRDesignElement convert(JrxmlElement element, Map<StyleKey, JRDesignStyle> stylesMap) {
        JRDesignStaticText staticText = new JRDesignStaticText();
        staticText.setText(element.getContent() != null ? element.getContent() : "");
        applyAlignment(staticText, element);
        applicator.applyCommonAttributes(staticText, element, stylesMap);
        return staticText;
    }

    private void applyAlignment(JRDesignStaticText target, JrxmlElement element) {
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
