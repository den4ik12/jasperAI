package ru.volodin.jasperai.pipeline.step;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.volodin.jasperai.controller.dto.Coordinate;
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
}
