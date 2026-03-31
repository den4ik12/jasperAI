package ru.volodin.jasperai.pipeline.step;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.volodin.jasperai.controller.dto.Coordinate;
import ru.volodin.jasperai.domain.JrxmlElement;
import ru.volodin.jasperai.domain.JrxmlTemplateData;
import ru.volodin.jasperai.domain.LlmElement;
import ru.volodin.jasperai.domain.LlmTemplateData;
import ru.volodin.jasperai.pipeline.PipelineContext;
import ru.volodin.jasperai.pipeline.PipelineStep;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CoordinateEnrichmentStep implements PipelineStep {

    @Override
    public void execute(PipelineContext context) throws Exception {
        LlmTemplateData llmData = context.getLlmTemplateData();
        if (llmData == null || llmData.getElements() == null) {
            return;
        }

        List<Coordinate> validCoords = filterValidCoordinates(context.getInputCoordinates());

        List<JrxmlElement> elements = llmData.getElements().stream()
//                .filter(e -> e.getType() != ElementType.STATIC_TEXT || (e.getContent() != null && !e.getContent().isBlank()))
                .map(llmElement -> toJrxmlElement(llmElement, validCoords))
                .toList();

        context.setJrxmlTemplateData(JrxmlTemplateData.builder()
                .pageWidth(context.getSourcePageWidth())
                .pageHeight(context.getSourcePageHeight())
                .elements(elements)
                .build());
    }

    private JrxmlElement toJrxmlElement(LlmElement llmElement, List<Coordinate> coords) {
        JrxmlElement element = mapFields(llmElement);
        applyCoordinates(element, llmElement.getElementId(), coords);
        return element;
    }

    private JrxmlElement mapFields(LlmElement src) {
        return JrxmlElement.builder()
                .elementId(src.getElementId())
                .type(src.getType())
                .content(src.getContent())
                .fontFamily(src.getFontFamily())
                .fontSize(src.getFontSize())
                .isBold(src.getIsBold())
                .colorHex(src.getColorHex())
                .horizontalAlignment(src.getHorizontalAlignment())
                .verticalAlignment(src.getVerticalAlignment())
                .textAdjustStretch(src.getTextAdjustStretch())
                .lineHeight(src.getLineHeight())
                .backgroundColorHex(src.getBackgroundColorHex())
                .imageUrl(src.getImageUrl())
                .scaleImage(src.getScaleImage())
                .groupId(src.getGroupId())
                .borderWidth(src.getBorderWidth())
                .borderColorHex(src.getBorderColorHex())
                .build();
    }

    private void applyCoordinates(JrxmlElement element, String eid, List<Coordinate> coords) {
        if (eid == null) return;

        Coordinate coord = findById(eid, coords);
        if (coord != null) {
            applyCoord(element, coord);
            return;
        }

        Coordinate ancestorCoord = findAncestorCoord(eid, coords);
        if (ancestorCoord != null) {
            applyCoord(element, ancestorCoord);
            return;
        }

        BoundingBox childBox = childrenBoundingBox(eid, coords);
        if (childBox != null) {
            element.setX(childBox.x);
            element.setY(childBox.y);
            element.setWidth(childBox.width);
            element.setHeight(childBox.height);
        } else {
            System.out.printf("[Enrich] NO MATCH: elementId=%s content=%s%n",
                    eid, element.getContent());
        }
    }

    private List<Coordinate> filterValidCoordinates(List<Coordinate> coordinates) {
        if (coordinates == null) return List.of();
        return coordinates.stream()
                .filter(c -> c.getId() != null && !c.getId().equals("no-id"))
                .toList();
    }

    private Coordinate findById(String id, List<Coordinate> coords) {
        return coords.stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
    }

    private void applyCoord(JrxmlElement element, Coordinate coord) {
        element.setX(coord.getX());
        element.setY(coord.getY());
        element.setWidth(coord.getWidth());
        element.setHeight(coord.getHeight());
    }

    private Coordinate findAncestorCoord(String eid, List<Coordinate> coords) {
        String candidate = eid;
        while (candidate.contains("_")) {
            int lastUnderscore = candidate.lastIndexOf('_');
            candidate = candidate.substring(0, lastUnderscore);
            Coordinate coord = findById(candidate, coords);
            if (coord != null) return coord;
        }
        return null;
    }

    private BoundingBox childrenBoundingBox(String parentId, List<Coordinate> coordinates) {
        String prefix = parentId + "_";
        List<Coordinate> children = coordinates.stream()
                .filter(c -> c.getId().startsWith(prefix))
                .toList();
        if (children.isEmpty()) return null;

        int minX = children.stream().mapToInt(Coordinate::getX).min().getAsInt();
        int minY = children.stream().mapToInt(Coordinate::getY).min().getAsInt();
        int maxRight = children.stream().mapToInt(c -> c.getX() + c.getWidth()).max().getAsInt();
        int maxBottom = children.stream().mapToInt(c -> c.getY() + c.getHeight()).max().getAsInt();
        return new BoundingBox(minX, minY, maxRight - minX, maxBottom - minY);
    }

    private record BoundingBox(int x, int y, int width, int height) {}
}
