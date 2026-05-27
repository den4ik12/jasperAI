package ru.volodin.jasperai.pipeline.step;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.volodin.jasperai.controller.dto.Coordinate;
import ru.volodin.jasperai.controller.dto.PrintArea;
import ru.volodin.jasperai.domain.ElementType;
import ru.volodin.jasperai.domain.LlmElement;
import ru.volodin.jasperai.domain.LlmTemplateData;
import ru.volodin.jasperai.jrxml.converter.CommonAttributesApplicator;
import ru.volodin.jasperai.jrxml.converter.JrxmlElementConverter;
import ru.volodin.jasperai.jrxml.converter.impl.ImageConverter;
import ru.volodin.jasperai.jrxml.converter.impl.RectangleConverter;
import ru.volodin.jasperai.jrxml.converter.impl.StaticTextConverter;
import ru.volodin.jasperai.jrxml.converter.impl.TextFieldConverter;
import ru.volodin.jasperai.pipeline.PipelineContext;
import ru.volodin.jasperai.service.validation.JasperCompilerService;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CoordinateEnrichmentIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JasperCompilerService compilerService = new JasperCompilerService();

    @Test
    void c2cWithBrowserCoordinatesGeneratesCompilableJrxml() throws Exception {
        LlmTemplateData llmData;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("c2c/c2c-llmData-expected.json")) {
            llmData = objectMapper.readValue(is, LlmTemplateData.class);
        }

        List<Coordinate> coordinates;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("c2c/c2c-excepted-coordinates.json")) {
            String coordinatesJson = new String(is.readAllBytes());
            coordinates = Arrays.asList(objectMapper.readValue(coordinatesJson, Coordinate[].class));
        }

        PipelineContext context = new PipelineContext();
        context.setLlmTemplateData(llmData);
        context.setInputCoordinates(coordinates);

        new CoordinateEnrichmentStep().execute(context);
        new FrameGroupingStep().execute(context);
        new DynamicStretchStep().execute(context);
        new HideEmptyRowsStep().execute(context);
        CommonAttributesApplicator applicator = new CommonAttributesApplicator();
        List<JrxmlElementConverter> converters = List.of(
                new StaticTextConverter(applicator),
                new TextFieldConverter(applicator),
                new RectangleConverter(applicator),
                new ImageConverter(applicator));
        new JrxmlGenerationStep(converters).execute(context);

        String jrxml = context.getLastJrxml();
        assertNotNull(jrxml);

        compilerService.compile(jrxml);

        String expected = Files.readString(Path.of(getClass().getClassLoader().getResource("c2c/c2c-llm-coordinates.jrxml").toURI()));
        assertEquals(expected, jrxml);
    }

    @Test
    void skipsElementsWithoutMatchingCoordinates() throws Exception {
        PipelineContext context = new PipelineContext();
        context.setLlmTemplateData(LlmTemplateData.builder()
                .elements(List.of(
                        LlmElement.builder()
                                .elementId("existing")
                                .type(ElementType.STATIC_TEXT)
                                .content("Existing")
                                .build(),
                        LlmElement.builder()
                                .elementId("missing")
                                .type(ElementType.TEXT_FIELD)
                                .content("missingField")
                                .build()
                ))
                .build());
        context.setInputCoordinates(List.of(new Coordinate("existing", 10, 20, 100, 30)));

        new CoordinateEnrichmentStep().execute(context);

        assertEquals(1, context.getJrxmlTemplateData().getElements().size());
        assertEquals("existing", context.getJrxmlTemplateData().getElements().get(0).getElementId());
    }

    @Test
    void normalizesCoordinatesAgainstSelectedPrintArea() throws Exception {
        PipelineContext context = new PipelineContext();
        context.setPrintArea(new PrintArea(".target", 100, 200, 300, 400));
        context.setSourcePageWidth(300);
        context.setSourcePageHeight(400);
        context.setLlmTemplateData(LlmTemplateData.builder()
                .elements(List.of(
                        LlmElement.builder()
                                .elementId("inside")
                                .type(ElementType.STATIC_TEXT)
                                .content("Inside")
                                .build(),
                        LlmElement.builder()
                                .elementId("outside")
                                .type(ElementType.STATIC_TEXT)
                                .content("Outside")
                                .build()
                ))
                .build());
        context.setInputCoordinates(List.of(
                new Coordinate("inside", 120, 230, 80, 20),
                new Coordinate("outside", 10, 20, 50, 20)
        ));

        new CoordinateEnrichmentStep().execute(context);

        assertEquals(1, context.getJrxmlTemplateData().getElements().size());
        assertEquals(20, context.getJrxmlTemplateData().getElements().get(0).getX());
        assertEquals(30, context.getJrxmlTemplateData().getElements().get(0).getY());
        assertEquals(300, context.getJrxmlTemplateData().getPageWidth());
        assertEquals(400, context.getJrxmlTemplateData().getPageHeight());
    }
}
