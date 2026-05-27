package ru.volodin.jasperai.service.validation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class JasperCompilerServiceTest {

    private final JasperCompilerService compilerService = new JasperCompilerService();

    @Test
    void compilesValidJrxml() throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader()
                .getResource("sample/sample.jrxml");
        var path = Paths.get(resource.toURI());
        String jrxml = Files.readString(path);

        assertDoesNotThrow(() -> compilerService.compile(jrxml));
    }
}
