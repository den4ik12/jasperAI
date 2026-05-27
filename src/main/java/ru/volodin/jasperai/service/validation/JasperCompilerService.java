package ru.volodin.jasperai.service.validation;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Service
public class JasperCompilerService {

    public boolean compile(String jrxml) throws JRException {
        byte[] bytes = jrxml.getBytes(StandardCharsets.UTF_8);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            JasperCompileManager.compileReport(inputStream);
            return true;
        } catch (JRException e) {
            throw e;
        } catch (Exception e) {
            throw new JRException("Failed to process JRXML", e);
        }
    }
}
