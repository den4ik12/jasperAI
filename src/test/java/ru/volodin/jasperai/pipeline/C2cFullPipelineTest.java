//package ru.volodin.jasperai.pipeline;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.SerializationFeature;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import ru.volodin.jasperai.controller.dto.Coordinate;
//import ru.volodin.jasperai.domain.LlmTemplateData;
//import ru.volodin.jasperai.pipeline.PipelineContext;
//import ru.volodin.jasperai.pipeline.step.HtmlStructuredExtractionStep;
//import ru.volodin.jasperai.service.ReportGenerationService;
//import ru.volodin.jasperai.service.validation.JasperCompilerService;
//
//import java.io.IOException;
//import java.net.URL;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//
//@SpringBootTest
//class C2cFullPipelineTest {
//
//    @Autowired
//    private ReportGenerationService reportGenerationService;
//
//    @Autowired
//    private HtmlStructuredExtractionStep extractionStep;
//
//    private final JasperCompilerService compilerService = new JasperCompilerService();
//    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
//
//    @Test
//    void c2cFullPipelineWithLlmAndBrowserCoordinates() throws Exception {
//        String html = Files.readString(Path.of(
//                getClass().getClassLoader().getResource("c2c/c2c.html").toURI()
//        ));
//        String coordinatesJsonStr = Files.readString(Path.of(
//                getClass().getClassLoader().getResource("c2c/c2c-excepted-coordinates.json").toURI()
//        ));
//        List<Coordinate> coordinates = Arrays.asList(objectMapper.readValue(coordinatesJsonStr, Coordinate[].class));
//
//        PipelineContext extractCtx = new PipelineContext();
//        extractCtx.setHtml(html);
//        extractionStep.execute(extractCtx);
////        LlmTemplateData expectedExtract = objectMapper.readValue(
////                Path.of(getClass().getClassLoader().getResource("c2c/c2c-llmData-expected.json").toURI()).toFile(),
////                LlmTemplateData.class
////        );
////        assertThat(beautify(extractCtx.getLlmTemplateData())).isEqualTo(beautify(expectedExtract));
//
//        String jrxml = reportGenerationService.generateValidReport(html, coordinates);
//        assertNotNull(jrxml);
//
//        compilerService.compile(jrxml);
//
//        String etalonName = "c2c/c2c-excepted.jrxml";
//        URL etalonUrl = getClass().getClassLoader().getResource(etalonName);
//
//        String previous = Files.readString(Path.of(etalonUrl.toURI()));
//        assertThat(jrxml).isEqualTo(previous);
//    }
//
//    private String beautify(LlmTemplateData data) throws IOException {
//        JsonNode node = objectMapper.valueToTree(data);
//        normalize(node);
//        return objectMapper.writeValueAsString(node);
//    }
//
//    private void normalize(JsonNode node) {
//        if (node.isObject()) {
//            ObjectNode obj = (ObjectNode) node;
//            List<String> toRemove = new ArrayList<>();
//            obj.fields().forEachRemaining(e -> {
//                if (e.getValue().isBoolean() && !e.getValue().booleanValue()) {
//                    toRemove.add(e.getKey());
//                } else if (e.getValue().isTextual() && e.getValue().asText().isEmpty()) {
//                    toRemove.add(e.getKey());
//                } else {
//                    normalize(e.getValue());
//                }
//            });
//            toRemove.add("lineHeight");
//            if ("TEXT_FIELD".equals(obj.path("type").asText())) {
//                toRemove.add("content");
//            }
//            obj.remove(toRemove);
//        } else if (node.isArray()) {
//            node.forEach(this::normalize);
//        }
//    }
//}
