package ru.volodin.jasperai.pipeline.step;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.volodin.jasperai.domain.LlmTemplateData;
import ru.volodin.jasperai.pipeline.PipelineContext;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class HtmlStructuredExtractionStepTest {

    @Autowired
    private HtmlStructuredExtractionStep step;

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    static Stream<Arguments> testCases() {
        return Stream.of(
//                Arguments.of("sample/sample.html", "sample/sample-llmData-expected.json"),
//                Arguments.of("credit/credit.html", "credit/credit-llmData-expected.json"),
                Arguments.of("c2c/c2c.html", "c2c/c2c-llmData-expected.json")
//                Arguments.of("reclamation/reclamation.html", "reclamation/reclamation-llmData-expected.json")
        );
    }

    @ParameterizedTest
    @MethodSource("testCases")
    void extractedDataMatchesReference(String htmlResource, String expectedJsonResource) throws IOException, URISyntaxException {
        PipelineContext context = new PipelineContext();
        context.setInputHtml(Files.readString(
                Paths.get(getClass().getClassLoader().getResource(htmlResource).toURI())
        ));

        step.execute(context);

        String actualLlmData = beautify(context.getLlmTemplateData());

        URL expectedUrl = getClass().getClassLoader().getResource(expectedJsonResource);

        LlmTemplateData expectedLlmData = objectMapper.readValue(
                Paths.get(expectedUrl.toURI()).toFile(),
                LlmTemplateData.class
        );
        String expectedJson = beautify(expectedLlmData);

        assertThat(actualLlmData).isEqualTo(expectedJson);
    }

    private String beautify(LlmTemplateData data) throws IOException {
        JsonNode node = objectMapper.valueToTree(data);
        return objectMapper.writeValueAsString(node);
    }
}
