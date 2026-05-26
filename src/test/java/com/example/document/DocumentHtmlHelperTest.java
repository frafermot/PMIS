package com.example.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.project.Project;
import com.example.task.Task;
import com.example.user.User;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DocumentHtmlHelperTest {

    @Test
    public void testFillProjectMetadata() {
        Project p = new Project();
        p.setId(101L);
        p.setName("Proyecto Antigravity");
        
        User sponsor = new User();
        sponsor.setName("Sponsor Miguel");
        p.setSponsor(sponsor);

        User director = new User();
        director.setName("Director Antigravity");
        p.setDirector(director);

        // Test classic braces
        String html = "<p>{{NOMBRE_PROYECTO}}</p><p>{{SPONSOR}}</p>";
        String result = DocumentHtmlHelper.fillProjectMetadata(html, p);
        assertTrue(result.contains("Proyecto Antigravity"));
        assertTrue(result.contains("Sponsor Miguel"));

        // Test table columns (horizontal layout)
        String tableHtml = "<table><tr><td>Nombre del Proyecto</td><td>-</td><td>Código del Proyecto</td><td>-</td></tr></table>";
        String tableResult = DocumentHtmlHelper.fillProjectMetadata(tableHtml, p);
        Document doc = Jsoup.parseBodyFragment(tableResult);
        assertEquals("Proyecto Antigravity", doc.select("td").get(1).text());
        assertEquals("101", doc.select("td").get(3).text());
    }

    @Test
    public void testUpdateActivitiesTableInHtml() {
        List<Task> tasks = new ArrayList<>();
        Task t1 = new Task();
        t1.setWbsCode("1.1");
        t1.setName("Planificación");
        t1.setGroup(true);
        t1.setStartDate(LocalDateTime.of(2026, 5, 20, 9, 0));
        t1.setEndDate(LocalDateTime.of(2026, 5, 22, 18, 0));
        tasks.add(t1);

        String tableHtml = "<table class=\"activities-table\"><thead><tr><td><strong>CÓDIGO (EDT)</strong></td><td><strong>ACTIVIDAD</strong></td></tr></thead><tbody><tr><td>&nbsp;</td><td>&nbsp;</td></tr></tbody></table>";
        String result = DocumentHtmlHelper.updateActivitiesTableInHtml(tableHtml, tasks);
        Document doc = Jsoup.parseBodyFragment(result);
        
        assertEquals("1.1", doc.select("tbody tr td").get(0).text());
        assertEquals("Planificación", doc.select("tbody tr td").get(1).text());
    }
}
