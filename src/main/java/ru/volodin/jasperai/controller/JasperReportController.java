package ru.volodin.jasperai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.volodin.jasperai.controller.dto.GenerateRequest;
import ru.volodin.jasperai.controller.dto.TestDataRequest;
import ru.volodin.jasperai.domain.LlmTemplateData;
import ru.volodin.jasperai.service.LlmVariant;
import ru.volodin.jasperai.service.ElementExtractionService;
import ru.volodin.jasperai.service.ReportPreviewService;
import ru.volodin.jasperai.service.ReportGenerationService;
import ru.volodin.jasperai.service.TestDataService;

import java.util.List;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class JasperReportController {

    private final ElementExtractionService elementExtractionService;
    private final ReportGenerationService reportGenerationService;
    private final ReportPreviewService reportPreviewService;
    private final TestDataService testDataService;

    @PostMapping(value = "/extract",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LlmTemplateData> extract(@RequestBody GenerateRequest request) throws Exception {
        log.info("Extract elements from HTML");
        LlmTemplateData data = elementExtractionService.extract(request.getHtml());
        return ResponseEntity.ok(data);
    }

    @PostMapping(value = "/generate",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> generate(@RequestBody GenerateRequest request) throws Exception {
        log.info("Generate: {}", request.getCoordinates());
        String jrxml = reportGenerationService.generateValidReport(request);
        return ResponseEntity.ok(jrxml);
    }

    @PostMapping(value = "/preview/pdf",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = "application/pdf")
    public ResponseEntity<byte[]> previewPdf(@RequestBody GenerateRequest request) throws Exception {
        log.info("Generate PDF preview");
        byte[] pdf = reportPreviewService.renderPdf(request);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/pdf"))
                .body(pdf);
    }

    @PostMapping(value = "/test-data",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<LlmVariant>> testData(@RequestBody TestDataRequest request) {
        log.info("Generate test data for JRXML");
        return ResponseEntity.ok(testDataService.generate(request.getJrxml()));
    }
}
