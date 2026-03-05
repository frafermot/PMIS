package com.example.document;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class TemplateService {

    /**
     * Loads the HTML template for a given DocumentType.
     * Returns an empty string if the template file is not found.
     */
    public String loadTemplate(DocumentType type) {
        String path = "templates/" + type.getTemplatePath();
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (!resource.exists()) {
                return "<p><em>Template not found for " + type.getLabel() + "</em></p>";
            }
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "<p><em>Error loading template: " + e.getMessage() + "</em></p>";
        }
    }
}
