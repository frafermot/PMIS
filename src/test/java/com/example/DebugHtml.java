package com.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import com.example.document.DocumentHtmlHelper;
import com.example.task.Task;

public class DebugHtml {
    public static void main(String[] args) throws Exception {
        String htmlPath = "src/main/resources/templates/planificacion/C. Alcance/diccionario-de-la-edt.html";
        String html = new String(Files.readAllBytes(Paths.get(htmlPath)));
        
        Task t1 = new Task();
        t1.setId(1L);
        t1.setName("Task 1");
        t1.setDescription("Desc 1");
        t1.setWbsCode("1.1");
        
        Task t2 = new Task();
        t2.setId(2L);
        t2.setName("Milestone 1");
        t2.setMilestone(true);
        t2.setWbsCode("1.1.1");
        
        List<Task> tasks = Arrays.asList(t1, t2);
        
        String output = DocumentHtmlHelper.updateWbsDictionaryTableInHtml(html, tasks);
        
        System.out.println("Output length: " + output.length());
        
        // Print the contents of the wbs-dictionary-container
        org.jsoup.nodes.Document parsed = org.jsoup.Jsoup.parseBodyFragment(output);
        org.jsoup.nodes.Element container = parsed.select("#wbs-dictionary-container").first();
        if (container != null) {
            System.out.println(container.outerHtml());
        }
    }
}
