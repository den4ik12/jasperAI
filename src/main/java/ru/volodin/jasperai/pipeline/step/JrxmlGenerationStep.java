package ru.volodin.jasperai.pipeline.step;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.type.PositionTypeEnum;
import net.sf.jasperreports.engine.type.SplitTypeEnum;
import net.sf.jasperreports.engine.xml.JRXmlWriter;
import org.springframework.stereotype.Component;
import ru.volodin.jasperai.domain.ElementType;
import ru.volodin.jasperai.domain.JrxmlElement;
import ru.volodin.jasperai.domain.JrxmlFrame;
import ru.volodin.jasperai.domain.JrxmlTemplateData;
import ru.volodin.jasperai.jrxml.converter.JrxmlElementConverter;
import ru.volodin.jasperai.jrxml.style.StyleKey;
import ru.volodin.jasperai.pipeline.PipelineContext;
import ru.volodin.jasperai.pipeline.PipelineStep;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class JrxmlGenerationStep implements PipelineStep {

    private final Map<ElementType, JrxmlElementConverter> converters;

    public JrxmlGenerationStep(List<JrxmlElementConverter> converterList) {
        this.converters = converterList.stream()
                .collect(Collectors.toMap(JrxmlElementConverter::getSupportedType, Function.identity()));
    }

    @Override
    public void execute(PipelineContext context) {
        JrxmlTemplateData data = context.getJrxmlTemplateData();
        data.setPageWidth(context.getSourcePageWidth());
        data.setPageHeight(context.getSourcePageHeight());

        // TODO: Вынести нормализацию шрифта в отдельный шаг пайплайна (PreProcessorStep)
        normalizeFontSize(data);

        try {
            JasperDesign design = generateJasperDesign(data);
            String xmlContent = JRXmlWriter.writeReport(design, "UTF-8");
            context.setLastJrxml(xmlContent);
        } catch (JRException e) {
            throw new RuntimeException("Error generating JRXML via Jasper API", e);
        }
    }

    private void normalizeFontSize(JrxmlTemplateData data) {
        allElements(data)
                .filter(e -> e.getFontSize() != null)
                .forEach(e -> e.setFontSize(e.getFontSize() - 1));
    }

    private JasperDesign generateJasperDesign(JrxmlTemplateData data) throws JRException {
        JasperDesign design = new JasperDesign();
        design.setName("GeneratedReport");
        design.setPageWidth(data.getPageWidth());
        design.setPageHeight(data.getPageHeight());
        design.setColumnWidth(data.getPageWidth());
        design.setLeftMargin(0);
        design.setRightMargin(0);
        design.setTopMargin(0);
        design.setBottomMargin(0);

        Map<StyleKey, JRDesignStyle> stylesMap = buildStylesMap(data);
        for (JRDesignStyle style : stylesMap.values()) {
            design.addStyle(style);
        }

        for (JRDesignField field : generateFields(data)) {
            design.addField(field);
        }

        JRDesignBand detailBand = generateDetailBand(data, stylesMap);
        ((JRDesignSection) design.getDetailSection()).addBand(detailBand);

        return design;
    }

    private Map<StyleKey, JRDesignStyle> buildStylesMap(JrxmlTemplateData data) {
        return allElements(data)
                .map(StyleKey::from)
                .filter(key -> !key.isEmpty())
                .distinct()
                .collect(Collectors.toMap(
                        Function.identity(),
                        this::createJasperStyle,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    private JRDesignStyle createJasperStyle(StyleKey key) {
        JRDesignStyle style = new JRDesignStyle();
        style.setName(key.generateName());
        if (key.fontFamily() != null) style.setFontName(key.fontFamily().replaceAll("[\"']", "").trim());
        if (key.fontSize() != null) style.setFontSize(key.fontSize().floatValue());
        if (key.isBold() != null) style.setBold(key.isBold());
        return style;
    }

    private List<JRDesignField> generateFields(JrxmlTemplateData data) {
        return allElements(data)
                .filter(e -> e.getType() == ElementType.TEXT_FIELD && e.getContent() != null)
                .map(JrxmlElement::getContent)
                .distinct()
                .map(fieldName -> {
                    JRDesignField field = new JRDesignField();
                    field.setName(fieldName);
                    field.setValueClass(String.class);
                    return field;
                })
                .toList();
    }

    private JRDesignBand generateDetailBand(JrxmlTemplateData data, Map<StyleKey, JRDesignStyle> styles) {
        JRDesignBand detailBand = new JRDesignBand();
        detailBand.setHeight(calculateBandHeight(data));

        if (data.getBandSplitType() != null) {
            detailBand.setSplitType(SplitTypeEnum.getByName(data.getBandSplitType()));
        }

        if (data.getElements() != null) {
            data.getElements().stream()
                    .map(e -> convertElement(e, styles))
                    .forEach(detailBand::addElement);
        }

        if (data.getFrames() != null) {
            data.getFrames().stream()
                    .map(frameData -> createDesignFrame(frameData, styles))
                    .forEach(detailBand::addElement);
        }

        return detailBand;
    }

    private JRDesignElement convertElement(JrxmlElement e, Map<StyleKey, JRDesignStyle> styles) {
        JrxmlElementConverter converter = converters.get(e.getType());
        if (converter == null) {
            throw new IllegalArgumentException("Unsupported element type: " + e.getType());
        }
        return converter.convert(e, styles);
    }

    private JRDesignFrame createDesignFrame(JrxmlFrame frameData, Map<StyleKey, JRDesignStyle> styles) {
        JRDesignFrame frame = new JRDesignFrame(null);
        frame.setX(frameData.getX());
        frame.setY(frameData.getY());
        frame.setWidth(frameData.getWidth());
        frame.setHeight(frameData.getHeight());

        if (frameData.getPositionType() != null) {
            frame.setPositionType(PositionTypeEnum.getByName(frameData.getPositionType()));
        }
        frame.setRemoveLineWhenBlank(Boolean.TRUE.equals(frameData.getIsRemoveLineWhenBlank()));

        if (frameData.getPrintWhenFieldName() != null) {
            frame.setPrintWhenExpression(new JRDesignExpression("$F{" + frameData.getPrintWhenFieldName() + "} != null"));
        }

        if (frameData.getElements() != null) {
            frameData.getElements().stream()
                    .map(child -> convertElement(child, styles))
                    .forEach(frame::addElement);
        }

        return frame;
    }

    private int calculateBandHeight(JrxmlTemplateData data) {
        int maxElements = data.getElements() == null ? 0 :
                data.getElements().stream().mapToInt(e -> e.getY() + e.getHeight()).max().orElse(0);

        int maxFrames = data.getFrames() == null ? 0 :
                data.getFrames().stream().mapToInt(f -> f.getY() + f.getHeight()).max().orElse(0);

        return Math.max(maxElements, maxFrames);
    }

    private Stream<JrxmlElement> allElements(JrxmlTemplateData data) {
        Stream<JrxmlElement> baseElements = data.getElements() == null ? Stream.empty() : data.getElements().stream();
        Stream<JrxmlElement> frameElements = data.getFrames() == null ? Stream.empty() :
                data.getFrames().stream().flatMap(f -> f.getElements() == null ? Stream.empty() : f.getElements().stream());

        return Stream.concat(baseElements, frameElements);
    }
}
