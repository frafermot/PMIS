package com.example.document;

import com.example.project.Project;
import com.example.task.Task;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DocumentHtmlHelper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Replaces placeholders and fills metadata in the first/any tables matching key terms.
     */
    public static String fillProjectMetadata(String html, Project project) {
        if (html == null) return "";
        if (project == null) return html;

        String pName = project.getName() != null ? project.getName() : "";
        String pId = project.getId() != null ? project.getId().toString() : "";
        String pSponsor = project.getSponsor() != null ? project.getSponsor().getName() : "";
        String pDirector = project.getDirector() != null ? project.getDirector().getName() : "";
        
        String pDate = project.getStartDate() != null ? 
                project.getStartDate().format(DATE_FORMATTER) : 
                LocalDate.now().format(DATE_FORMATTER);

        String period = "";
        if (project.getStartDate() != null && project.getEndDate() != null) {
            period = project.getStartDate().format(DATE_FORMATTER) + " - " + project.getEndDate().format(DATE_FORMATTER);
        }

        // 1. Classical string placeholder replacement
        html = html.replace("{{NOMBRE_PROYECTO}}", pName)
                   .replace("{{FECHA}}", pDate)
                   .replace("{{DIRECTOR}}", pDirector)
                   .replace("{{SPONSOR}}", pSponsor)
                   .replace("{{PERIODO}}", period);

        // 2. Intelligent cell-by-cell table matching using JSoup
        try {
            Document doc = Jsoup.parseBodyFragment(html);
            Elements tds = doc.select("td");
            for (int i = 0; i < tds.size() - 1; i++) {
                Element current = tds.get(i);
                String cellText = current.text().toLowerCase()
                        .replace(":", "")
                        .replace("\u00a0", " ")
                        .trim();

                // If this is a label cell, fill the immediately following cell if it's currently empty/spacer
                if (cellText.equals("nombre del proyecto") || cellText.equals("nombre proyecto")) {
                    updateNextCellIfEmpty(tds.get(i + 1), pName);
                } else if (cellText.equals("código del proyecto") || cellText.equals("código proyecto")
                        || cellText.equals("codigo del proyecto") || cellText.equals("codigo proyecto")) {
                    updateNextCellIfEmpty(tds.get(i + 1), pId);
                } else if (cellText.equals("patrocinador del proyecto") || cellText.equals("patrocinador proyecto")
                        || cellText.equals("propietario del proyecto") || cellText.equals("propietario proyecto")) {
                    updateNextCellIfEmpty(tds.get(i + 1), pSponsor);
                } else if (cellText.equals("director del proyecto") || cellText.equals("director proyecto") || cellText.equals("director")) {
                    updateNextCellIfEmpty(tds.get(i + 1), pDirector);
                } else if (cellText.equals("fecha de creación") || cellText.equals("fecha de creacion")
                        || cellText.equals("fecha creacion") || cellText.equals("fecha creación")) {
                    updateNextCellIfEmpty(tds.get(i + 1), pDate);
                }
            }
            return doc.body().html();
        } catch (Exception e) {
            e.printStackTrace();
            return html;
        }
    }

    private static void updateNextCellIfEmpty(Element nextCell, String value) {
        String existing = nextCell.text().replace("\u00a0", " ").trim();
        // Only fill if it is empty or is a simple placeholder/blank
        if (existing.isEmpty() || existing.equals("-")) {
            nextCell.empty().text(value);
        }
    }

    /**
     * Parses the HTML, locates the activities table, clears its tbody and populates it with tasks.
     */
    public static String updateActivitiesTableInHtml(String html, List<Task> tasks) {
        if (html == null) return "";
        try {
            Document doc = Jsoup.parseBodyFragment(html);
            
            // Look for table with activities-table class or table where the first row contains "ACTIVIDAD"
            Element table = doc.select("table.activities-table").first();
            if (table == null) {
                for (Element t : doc.select("table")) {
                    Element firstRow = t.select("tr").first();
                    if (firstRow != null && firstRow.text().toUpperCase().contains("ACTIVIDAD")) {
                        table = t;
                        break;
                    }
                }
            }

            if (table == null) {
                return html;
            }

            // Find or create tbody
            Element tbody = table.select("tbody").first();
            if (tbody == null) {
                tbody = table.appendElement("tbody");
            } else {
                tbody.empty();
            }

            if (tasks == null || tasks.isEmpty()) {
                // Add one empty spacer row
                Element tr = tbody.appendElement("tr");
                for (int c = 0; c < 8; c++) {
                    tr.appendElement("td").html("&nbsp;");
                }
            } else {
                for (Task task : tasks) {
                    Element tr = tbody.appendElement("tr");

                    // Set styles for summary group rows for extra premium feel
                    if (task.isGroup()) {
                        tr.attr("style", "background-color: #fcfcfc; font-weight: bold;");
                    }

                    // 1. Código EDT (WBS)
                    String wbs = task.getWbsCode() != null ? task.getWbsCode() : "";
                    tr.appendElement("td").text(wbs);

                    // 2. Actividad (Name with depth indentation & bold if group)
                    String name = task.getName() != null ? task.getName() : "";
                    Element tdName = tr.appendElement("td");
                    
                    // Add micro-indentation for sub-tasks based on WBS hierarchy dots
                    if (wbs.contains(".")) {
                        String[] parts = wbs.split("\\.");
                        int dots = parts.length - 2; // e.g. "1.1" has length 2 -> depth 0. "1.1.3" has length 3 -> depth 1.
                        if (dots > 0) {
                            String padding = "\u00a0\u00a0\u00a0\u00a0".repeat(dots);
                            tdName.appendElement("span").html(padding);
                        }
                    }

                    if (task.isGroup()) {
                        tdName.appendElement("strong").text(name);
                    } else if (task.isMilestone()) {
                        tdName.appendElement("span").text("♦ " + name).attr("style", "color: #9c27b0; font-style: italic;");
                    } else {
                        tdName.appendText(name);
                    }

                    // 3. Descripción
                    String desc = task.getDescription() != null ? task.getDescription() : "";
                    tr.appendElement("td").text(desc);

                    // 4. Fecha Inicio
                    String start = task.getStartDate() != null ? task.getStartDate().format(TIME_FORMATTER) : "";
                    tr.appendElement("td").text(start);

                    // 5. Fecha Fin
                    String end = task.getEndDate() != null ? task.getEndDate().format(TIME_FORMATTER) : "";
                    tr.appendElement("td").text(end);

                    // 6. Duración
                    String duration = "";
                    if (task.getDuration() != null) {
                        String unit = (task.getProject() != null && "DAYS".equals(task.getProject().getDurationUnit())) ? "d" : "h";
                        duration = task.getDuration() + unit;
                    }
                    tr.appendElement("td").text(duration);

                    // 7. Responsable
                    String assignee = task.getAssignee() != null ? task.getAssignee().getName() : "";
                    tr.appendElement("td").text(assignee);

                    // 8. Predecesora
                    String predecessor = "";
                    if (task.getPredecessor() != null) {
                        String predWbs = task.getPredecessor().getWbsCode();
                        predecessor = (predWbs != null ? predWbs + " " : "") + task.getPredecessor().getName();
                    }
                    tr.appendElement("td").text(predecessor);
                }
            }

            return doc.body().html();
        } catch (Exception e) {
            e.printStackTrace();
            return html;
        }
    }
}
