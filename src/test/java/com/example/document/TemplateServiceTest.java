package com.example.document;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class TemplateServiceTest {

    @Autowired
    private TemplateService templateService;

    @Test
    public void testLoadTemplate_ValidType() {
        String content = templateService.loadTemplate(DocumentType.ACTA_CONSTITUCION);
        
        assertNotNull(content);
        assertFalse(content.contains("<em>Template not found for"));
        assertFalse(content.contains("<em>Error loading template"));
        assertTrue(content.length() > 0);
    }
}
