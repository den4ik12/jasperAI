package ru.volodin.jasperai.pipeline.step;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.volodin.jasperai.pipeline.PipelineContext;
import ru.volodin.jasperai.pipeline.PipelineStep;
import ru.volodin.jasperai.service.validation.JasperCompilerService;
import ru.volodin.jasperai.service.validation.ReportFixerService;

@Slf4j
@Component
@RequiredArgsConstructor
public class JrxmlValidationStep implements PipelineStep {

    private final JasperCompilerService compilerService;
    private final ReportFixerService fixerService;
    @Value("${jasper.generation.max-attempts:3}")
    private int maxAttempts;

    @Override
    public void execute(PipelineContext context) {
        boolean isValid = false;
        String lastErrorMessage = null;
        for (int i = 1; i <= maxAttempts; i++) {
            String lastJrxml = context.getLastJrxml();
            if (lastJrxml == null || lastJrxml.isEmpty()) {
                break;
            }
            try {
                isValid = compilerService.compile(lastJrxml);
                break;
            } catch (Exception e) {
                lastErrorMessage = e.getMessage();
                log.info("Ошибка компиляции {}", lastErrorMessage);
                String fixedJrxml = fixerService.fixReport(lastJrxml, lastErrorMessage);
                context.setLastJrxml(fixedJrxml);
            }
        }
        if (!isValid) {
            throw new RuntimeException("Невозможно скомпилировать jrxml: " + lastErrorMessage);
        }
    }
}
