package ru.volodin.jasperai.jrxml.converter;

import net.sf.jasperreports.engine.design.JRDesignElement;
import net.sf.jasperreports.engine.design.JRDesignStyle;
import ru.volodin.jasperai.domain.ElementType;
import ru.volodin.jasperai.domain.JrxmlElement;
import ru.volodin.jasperai.jrxml.style.StyleKey;

import java.util.Map;

public interface JrxmlElementConverter {

    ElementType getSupportedType();

    JRDesignElement convert(JrxmlElement element, Map<StyleKey, JRDesignStyle> stylesMap);
}
