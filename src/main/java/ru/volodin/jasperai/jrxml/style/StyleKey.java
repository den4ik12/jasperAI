package ru.volodin.jasperai.jrxml.style;

import ru.volodin.jasperai.domain.JrxmlElement;

public record StyleKey(String fontFamily, Integer fontSize, Boolean isBold) {

    public static StyleKey from(JrxmlElement e) {
        return new StyleKey(e.getFontFamily(), e.getFontSize(), e.getIsBold());
    }

    public boolean isEmpty() {
        return fontFamily == null && fontSize == null && isBold == null;
    }

    public String generateName() {
        String font = fontFamily != null
                ? fontFamily.replaceAll("[\"']", "").replaceAll("[^a-zA-Z0-9]", "")
                : "Default";
        String size = fontSize != null ? String.valueOf(fontSize) : "0";
        String bold = Boolean.TRUE.equals(isBold) ? "B" : "R";
        return "Style_" + font + "_" + size + "_" + bold;
    }
}
