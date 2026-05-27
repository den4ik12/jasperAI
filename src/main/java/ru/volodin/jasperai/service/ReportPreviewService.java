package ru.volodin.jasperai.service;

import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JRElement;
import net.sf.jasperreports.engine.JRElementGroup;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRStyle;
import net.sf.jasperreports.engine.JRBand;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.design.JRDesignImage;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.OnErrorTypeEnum;
import net.sf.jasperreports.engine.util.JRStyledText;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import org.springframework.stereotype.Service;
import ru.volodin.jasperai.controller.dto.GenerateRequest;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReportPreviewService {

    private static final List<String> SAMPLE_VALUES = List.of(
            "ABCDCC",
            "QWERTY",
            "ZXCVBN",
            "MNOPQR",
            "KLMNOP",
            "TUVWXY",
            "BCDFGH",
            "JKLMNO",
            "RSTUVW",
            "XYZABC"
    );
    private static final List<Path> PREVIEW_FONT_CANDIDATES = List.of(
            Path.of("/System/Library/Fonts/Supplemental/Arial Unicode.ttf"),
            Path.of("/System/Library/Fonts/Supplemental/Arial.ttf"),
            Path.of("/Library/Fonts/Arial Unicode.ttf"),
            Path.of("/Library/Fonts/Arial.ttf")
    );

    private final ReportGenerationService reportGenerationService;

    public byte[] renderPdf(GenerateRequest request) throws Exception {
        String jrxml = reportGenerationService.generateValidReport(request);
        JasperReport report = compileForPreview(jrxml);
        JasperPrint print = JasperFillManager.fillReport(report, new HashMap<>(), dataSourceFor(report));
        return JasperExportManager.exportReportToPdf(print);
    }

    private JasperReport compileForPreview(String jrxml) throws JRException {
        byte[] bytes = jrxml.getBytes(StandardCharsets.UTF_8);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            JasperDesign design = JRXmlLoader.load(inputStream);
            prepareDesignForPreview(design);
            return JasperCompileManager.compileReport(design);
        } catch (JRException e) {
            throw e;
        } catch (Exception e) {
            throw new JRException("Failed to compile JRXML for preview", e);
        }
    }

    private void prepareDesignForPreview(JasperDesign design) {
        design.setProperty(JRStyledText.PROPERTY_AWT_IGNORE_MISSING_FONT, "true");
        usePreviewFontFallback(design);
        for (JRBand band : design.getAllBands()) {
            if (band != null) {
                applyImageFallback(band);
            }
        }
    }

    private void usePreviewFontFallback(JasperDesign design) {
        Optional<Path> previewFont = findPreviewFont();
        for (JRStyle style : design.getStylesList()) {
            style.setFontName("Arial");
            if (previewFont.isPresent()) {
                style.setPdfFontName(previewFont.get().toString());
                style.setPdfEncoding("Identity-H");
                style.setPdfEmbedded(true);
            } else {
                style.setPdfFontName("Helvetica");
                style.setPdfEncoding("Cp1251");
                style.setPdfEmbedded(false);
            }
        }
    }

    private Optional<Path> findPreviewFont() {
        return PREVIEW_FONT_CANDIDATES.stream()
                .filter(Files::isRegularFile)
                .findFirst();
    }

    private void applyImageFallback(JRElementGroup group) {
        for (JRElement element : group.getElements()) {
            if (element instanceof JRDesignImage image) {
                image.setOnErrorType(OnErrorTypeEnum.BLANK);
            }
            if (element instanceof JRElementGroup childGroup) {
                applyImageFallback(childGroup);
            }
        }
    }

    private JRDataSource dataSourceFor(JasperReport report) {
        JRField[] fields = report.getFields();
        if (fields == null || fields.length == 0) {
            return new JREmptyDataSource(1);
        }

        Map<String, Object> record = new HashMap<>();
        for (int i = 0; i < fields.length; i++) {
            record.put(fields[i].getName(), sampleValueFor(i));
        }
        return new JRMapCollectionDataSource(List.of(record));
    }

    private String sampleValueFor(int index) {
        return SAMPLE_VALUES.get(index % SAMPLE_VALUES.size());
    }
}
