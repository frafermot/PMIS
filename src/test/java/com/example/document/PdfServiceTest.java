package com.example.document;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PdfServiceTest {

    private final PdfService pdfService = new PdfService();

    @Test
    void testGeneratePdfFromHtml() throws Exception {
        String html = "<html><body><h1>Hello World</h1></body></html>";
        byte[] pdfBytes = pdfService.generatePdfFromHtml(html);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }
}
