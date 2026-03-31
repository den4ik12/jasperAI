package ru.volodin.jasperai.jrxml.converter;

import net.sf.jasperreports.engine.design.JRDesignElement;
import net.sf.jasperreports.engine.design.JRDesignStyle;
import net.sf.jasperreports.engine.type.PositionTypeEnum;
import org.springframework.stereotype.Component;
import ru.volodin.jasperai.domain.JrxmlElement;
import ru.volodin.jasperai.jrxml.style.StyleKey;

import java.awt.Color;
import java.util.Map;

@Component
public class CommonAttributesApplicator {

    public void applyCommonAttributes(JRDesignElement target,
                                      JrxmlElement source,
                                      Map<StyleKey, JRDesignStyle> stylesMap) {
        applyGeometry(target, source);
        applyPositionType(target, source);
        applyRemoveLineWhenBlank(target, source);
        applyForecolor(target, source);
        applyStyle(target, source, stylesMap);
    }

    private void applyGeometry(JRDesignElement target, JrxmlElement source) {
        if (source.getX() != null) target.setX(source.getX());
        if (source.getY() != null) target.setY(source.getY());
        if (source.getWidth() != null) target.setWidth(source.getWidth());
        if (source.getHeight() != null) target.setHeight(source.getHeight());
    }

    private void applyPositionType(JRDesignElement target, JrxmlElement source) {
        if (source.getPositionType() == null) {
            return;
        }
        PositionTypeEnum positionType = PositionTypeEnum.getByName(source.getPositionType());
        if (positionType != null) {
            target.setPositionType(positionType);
        }
    }

    private void applyRemoveLineWhenBlank(JRDesignElement target, JrxmlElement source) {
        if (Boolean.TRUE.equals(source.getIsRemoveLineWhenBlank())) {
            target.setRemoveLineWhenBlank(true);
        }
    }

    private void applyForecolor(JRDesignElement target, JrxmlElement source) {
        Color color = decodeColor(source.getColorHex());
        if (color != null) {
            target.setForecolor(color);
        }
    }

    private void applyStyle(JRDesignElement target,
                            JrxmlElement source,
                            Map<StyleKey, JRDesignStyle> stylesMap) {
        StyleKey key = StyleKey.from(source);
        if (key.isEmpty()) {
            return;
        }
        JRDesignStyle style = stylesMap.get(key);
        if (style != null) {
            target.setStyle(style);
        }
    }

    public static Color decodeColor(String cssColor) {
        if (cssColor == null || cssColor.isBlank()) {
            return null;
        }
        String s = cssColor.trim();
        try {
            if (s.startsWith("rgba(")) {
                String[] p = s.substring(5, s.length() - 1).split(",");
                int r = (int) Double.parseDouble(p[0].trim());
                int g = (int) Double.parseDouble(p[1].trim());
                int b = (int) Double.parseDouble(p[2].trim());
                double a = Double.parseDouble(p[3].trim());
                return toDarkSafeColor(blend(r, a), blend(g, a), blend(b, a));
            }
            if (s.startsWith("rgb(")) {
                String[] p = s.substring(4, s.length() - 1).split(",");
                return toDarkSafeColor(
                        (int) Double.parseDouble(p[0].trim()),
                        (int) Double.parseDouble(p[1].trim()),
                        (int) Double.parseDouble(p[2].trim()));
            }
            if (s.startsWith("#")) {
                String hex = s.substring(1);
                if (hex.length() == 3) {
                    hex = "" + hex.charAt(0) + hex.charAt(0)
                            + hex.charAt(1) + hex.charAt(1)
                            + hex.charAt(2) + hex.charAt(2);
                }
                int r = Integer.parseInt(hex.substring(0, 2), 16);
                int g = Integer.parseInt(hex.substring(2, 4), 16);
                int b = Integer.parseInt(hex.substring(4, 6), 16);
                if (hex.length() >= 8) {
                    double a = Integer.parseInt(hex.substring(6, 8), 16) / 255.0;
                    return toDarkSafeColor(blend(r, a), blend(g, a), blend(b, a));
                }
                return toDarkSafeColor(r, g, b);
            }
        } catch (Exception ignored) {
        }
        return Color.decode(truncateToSixHex(s));
    }

    private static String truncateToSixHex(String s) {
        if (s.startsWith("#") && s.length() > 7) {
            return s.substring(0, 7);
        }
        return s;
    }

    private static int blend(int channel, double alpha) {
        return (int) Math.round(alpha * channel + (1 - alpha) * 255);
    }

    private static Color toDarkSafeColor(int r, int g, int b) {
        if (r <= 80 && g <= 80 && b <= 80) {
            return Color.BLACK;
        }
        return new Color(r, g, b);
    }
}
