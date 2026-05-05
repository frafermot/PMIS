package com.example.document;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document.OutputSettings;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class PdfService {

    /**
     * Generates a PDF byte array from an HTML string.
     * Uses jsoup to ensure the HTML is well-formed XHTML for Flying Saucer.
     */
    public byte[] generatePdfFromHtml(String htmlContent) throws Exception {
        // Clean and normalize HTML to XHTML for Flying Saucer
        org.jsoup.nodes.Document document = Jsoup.parse(htmlContent);
        document.outputSettings().syntax(OutputSettings.Syntax.xml);
        String xhtml = document.html();

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(xhtml);
            renderer.layout();
            renderer.createPDF(os);
            return os.toByteArray();
        }
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }
}
