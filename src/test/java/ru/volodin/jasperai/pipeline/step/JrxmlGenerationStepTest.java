package ru.volodin.jasperai.pipeline.step;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.volodin.jasperai.controller.dto.Coordinate;
import ru.volodin.jasperai.domain.ElementType;
import ru.volodin.jasperai.domain.JrxmlElement;
import ru.volodin.jasperai.domain.JrxmlTemplateData;
import ru.volodin.jasperai.domain.LlmTemplateData;
import ru.volodin.jasperai.jrxml.converter.CommonAttributesApplicator;
import ru.volodin.jasperai.jrxml.converter.JrxmlElementConverter;
import ru.volodin.jasperai.jrxml.converter.impl.ImageConverter;
import ru.volodin.jasperai.jrxml.converter.impl.RectangleConverter;
import ru.volodin.jasperai.jrxml.converter.impl.StaticTextConverter;
import ru.volodin.jasperai.jrxml.converter.impl.TextFieldConverter;
import ru.volodin.jasperai.pipeline.PipelineContext;
import ru.volodin.jasperai.pipeline.TestPipelineFactory;
import ru.volodin.jasperai.service.validation.JasperCompilerService;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JrxmlGenerationStepTest {

    private static final Pattern UUID_ATTR = Pattern.compile(" uuid=\"[0-9a-fA-F-]+\"");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JasperCompilerService compilerService = new JasperCompilerService();
    private final TestPipelineFactory testPipelineFactory = new TestPipelineFactory();

    static Stream<Arguments> testCases() {
        return Stream.of(
                Arguments.of("c2c/c2c-llmData-expected.json", "c2c/c2c-excepted.jrxml", "c2c/c2c-excepted-coordinates.json"),
                Arguments.of("credit/credit-llmData-expected.json", "credit/credit-excepted.jrxml", "credit/credit-excepted-coordinates.json"),
                Arguments.of("reclamation/reclamation-llmData-expected.json", "reclamation/reclamation-excepted.jrxml", "reclamation/reclamation-excepted-coordinates.json")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    void generatesExpectedJrxml(String actualLlmData, String expectedJrxml, String actualCoordinates) throws Exception {
        PipelineContext context = buildContext(actualLlmData, actualCoordinates);

        testPipelineFactory.buildJrxmlGenerationOnlyPipeline().execute(context);
        compilerService.compile(context.getLastJrxml());

        String expected = normalize(Files.readString(Path.of(resource(expectedJrxml).toURI())));
        assertEquals(expected, normalize(context.getLastJrxml()));
    }

    @Test
    void skipsElementsWithoutGeometryWhenCalculatingBandHeight() {
        PipelineContext context = new PipelineContext();
        context.setJrxmlTemplateData(JrxmlTemplateData.builder()
                .pageWidth(595)
                .pageHeight(842)
                .elements(List.of(
                        JrxmlElement.builder()
                                .type(ElementType.STATIC_TEXT)
                                .x(10)
                                .width(100)
                                .height(20)
                                .content("Missing y")
                                .build(),
                        JrxmlElement.builder()
                                .type(ElementType.STATIC_TEXT)
                                .x(10)
                                .y(20)
                                .width(100)
                                .height(20)
                                .content("Complete")
                                .build()
                ))
                .build());

        assertDoesNotThrow(() -> new JrxmlGenerationStep(buildConverters()).execute(context));
    }

    @Test
    void textFieldKeepsSimpleFieldExpressionAndEnablesCharacterWrapping() {
        TextFieldConverter converter = new TextFieldConverter(new CommonAttributesApplicator());
        JRDesignTextField textField = (JRDesignTextField) converter.convert(JrxmlElement.builder()
                .type(ElementType.TEXT_FIELD)
                .content("recipientAddress")
                .build(), Map.of());

        assertEquals("$F{recipientAddress}", textField.getExpression().getText());
        assertEquals("true", textField.getPropertiesMap()
                .getProperty("net.sf.jasperreports.text.truncate.at.char"));
    }

    private PipelineContext buildContext(String actualLlmData, String actualCoordinates) throws Exception {
        JsonNode root = objectMapper.readTree(resource(actualLlmData));
        PipelineContext context = new PipelineContext();
        context.setSourcePageWidth(595);
        context.setSourcePageHeight(842);
        context.setLlmTemplateData(objectMapper.treeToValue(root, LlmTemplateData.class));
        context.setInputCoordinates(objectMapper.readValue(resource(actualCoordinates),
                                                               new TypeReference<>() {}));
        return context;
    }

    private URL resource(String path) {
        return getClass().getClassLoader().getResource(path);
    }

    private String normalize(String jrxml) {
        return UUID_ATTR.matcher(jrxml).replaceAll("").replace("\r\n", "\n").trim();
    }

    private List<JrxmlElementConverter> buildConverters() {
        CommonAttributesApplicator applicator = new CommonAttributesApplicator();
        return List.of(
                new StaticTextConverter(applicator),
                new TextFieldConverter(applicator),
                new RectangleConverter(applicator),
                new ImageConverter(applicator)
        );
    }
}
