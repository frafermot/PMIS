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
import java.util.ArrayList;
import java.util.stream.Collectors;

public class DocumentHtmlHelper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

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

        html = html.replace("{{NOMBRE_PROYECTO}}", pName)
                   .replace("{{FECHA}}", pDate)
                   .replace("{{DIRECTOR}}", pDirector)
                   .replace("{{SPONSOR}}", pSponsor)
                   .replace("{{PERIODO}}", period);

        try {
            Document doc = Jsoup.parseBodyFragment(html);
            Elements tds = doc.select("td");
            for (int i = 0; i < tds.size() - 1; i++) {
                Element current = tds.get(i);
                String cellText = current.text().toLowerCase()
                        .replace(":", "")
                        .replace("\u00a0", " ")
                        .trim();

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
        if (existing.isEmpty() || existing.equals("-")) {
            nextCell.empty().text(value);
        }
    }

    public static String updateActivitiesTableInHtml(String html, List<Task> tasks) {
        if (html == null) return "";
        try {
            Document doc = Jsoup.parseBodyFragment(html);
            
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

            Element tbody = table.select("tbody").first();
            Element thead = table.select("thead").first();
            Element headerRow = null;
            
            if (thead != null) {
                headerRow = thead.select("tr").first();
            }
            
            if (tbody == null) {
                tbody = table.appendElement("tbody");
                if (thead == null) {
                    headerRow = table.select("tr").first();
                }
            } else {
                if (thead == null) {
                    Elements rows = tbody.select("> tr");
                    if (!rows.isEmpty()) {
                        headerRow = rows.first().clone();
                    }
                }
                tbody.empty();
                if (thead == null && headerRow != null) {
                    tbody.appendChild(headerRow);
                }
            }

            List<String> headers = new ArrayList<>();
            if (headerRow != null) {
                for (Element cell : headerRow.select("th, td")) {
                    headers.add(cell.text().toLowerCase().replace("\u00a0", " ").trim());
                }
            } else {
                headers = java.util.Arrays.asList("id", "nombre", "descripción", "id paquete de trabajo");
            }

            if (tasks == null || tasks.isEmpty()) {
                Element tr = tbody.appendElement("tr");
                for (int c = 0; c < headers.size(); c++) {
                    tr.appendElement("td").html("&nbsp;");
                }
            } else {
                for (Task task : tasks) {
                    Element tr = tbody.appendElement("tr");

                    if (task.isGroup()) {
                        tr.attr("style", "background-color: #fcfcfc; font-weight: bold;");
                    }

                    String wbs = task.getWbsCode() != null ? task.getWbsCode() : "";

                    for (String header : headers) {
                        Element td = tr.appendElement("td");
                        
                        if (header.contains("id paquete") || header.contains("wbs") || header.contains("código") || header.contains("codigo")) {
                            td.text(wbs);
                        } else if (header.equals("id")) {
                            td.text(task.getId() != null ? task.getId().toString() : "");
                        } else if (header.contains("nombre") || header.contains("actividad")) {
                            if (wbs.contains(".")) {
                                int dots = wbs.split("\\.").length - 2;
                                if (dots > 0) {
                                    td.appendElement("span").html("\u00a0\u00a0\u00a0\u00a0".repeat(dots));
                                }
                            }
                            String name = task.getName() != null ? task.getName() : "";
                            if (task.isGroup()) {
                                td.appendElement("strong").text(name);
                            } else if (task.isMilestone()) {
                                td.appendElement("span").text("♦ " + name).attr("style", "color: #9c27b0; font-style: italic;");
                            } else {
                                td.appendText(name);
                            }
                        } else if (header.contains("descrip")) {
                            td.text(task.getDescription() != null ? task.getDescription() : "");
                        } else if (header.contains("fecha") && (header.contains("inicio") || header.contains("comienzo"))) {
                            td.text(task.getStartDate() != null ? task.getStartDate().format(DATE_FORMATTER) : "");
                        } else if (header.contains("fecha") && (header.contains("fin") || header.contains("vencimiento"))) {
                            td.text(task.getEndDate() != null ? task.getEndDate().format(DATE_FORMATTER) : "");
                        } else if (header.contains("fecha")) {
                            td.text(task.getStartDate() != null ? task.getStartDate().format(DATE_FORMATTER) : "");
                        } else if (header.contains("duraci")) {
                            td.text(task.getDuration() != null && !task.isMilestone() ? task.getDuration().toString() : "");
                        } else if (header.contains("recurso") || header.contains("asignado")) {
                            td.text(task.getAssignee() != null ? task.getAssignee().getName() : "");
                        } else {
                            td.html("&nbsp;");
                        }
                    }
                }
            }

            return doc.body().html();
        } catch (Exception e) {
            e.printStackTrace();
            return html;
        }
    }

    public static String updateMilestonesTableInHtml(String html, List<Task> tasks) {
        if (html == null) return "";
        try {
            Document doc = Jsoup.parseBodyFragment(html);

            Element table = null;
            for (Element t : doc.select("table")) {
                Element firstRow = t.select("tr").first();
                if (firstRow != null && firstRow.text().toUpperCase().contains("HITO")) {
                    table = t;
                    break;
                }
            }

            if (table == null) return html;

            Element tbody = table.select("tbody").first();
            Element thead = table.select("thead").first();
            Element headerRow = null;
            
            if (thead != null) {
                headerRow = thead.select("tr").first();
            }
            
            if (tbody == null) {
                tbody = table.appendElement("tbody");
                if (thead == null) {
                    headerRow = table.select("tr").first();
                }
            } else {
                if (thead == null) {
                    Elements rows = tbody.select("> tr");
                    if (!rows.isEmpty()) {
                        headerRow = rows.first().clone();
                    }
                }
                tbody.empty();
                if (thead == null && headerRow != null) {
                    tbody.appendChild(headerRow);
                }
            }

            List<String> headers = new ArrayList<>();
            if (headerRow != null) {
                for (Element cell : headerRow.select("th, td")) {
                    headers.add(cell.text().toLowerCase().replace("\u00a0", " ").trim());
                }
            } else {
                headers = java.util.Arrays.asList("id del hito", "nombre del hito", "descripción del hito", "fecha límite");
            }

            List<Task> milestones = tasks == null ? new ArrayList<>() : 
                tasks.stream().filter(Task::isMilestone).collect(Collectors.toList());

            if (milestones.isEmpty()) {
                Element tr = tbody.appendElement("tr");
                for (int c = 0; c < headers.size(); c++) {
                    tr.appendElement("td").html("&nbsp;");
                }
            } else {
                for (Task task : milestones) {
                    Element tr = tbody.appendElement("tr");
                    String wbs = task.getWbsCode() != null ? task.getWbsCode() : "";

                    for (String header : headers) {
                        Element td = tr.appendElement("td");
                        
                        if (header.contains("id hito") || header.contains("id del hito") || header.equals("id")) {
                            td.text(task.getId() != null ? task.getId().toString() : "");
                        } else if (header.contains("id paquete") || header.contains("wbs")) {
                            td.text(wbs);
                        } else if (header.contains("nombre") || header.contains("hito")) {
                            td.text(task.getName() != null ? task.getName() : "");
                        } else if (header.contains("descrip")) {
                            td.text(task.getDescription() != null ? task.getDescription() : "");
                        } else if (header.contains("fecha") && (header.contains("inicio") || header.contains("comienzo"))) {
                            td.text(task.getStartDate() != null ? task.getStartDate().format(DATE_FORMATTER) : "");
                        } else if (header.contains("fecha") && (header.contains("fin") || header.contains("límite") || header.contains("limite") || header.contains("vencimiento"))) {
                            td.text(task.getEndDate() != null ? task.getEndDate().format(DATE_FORMATTER) : "");
                        } else if (header.contains("fecha")) {
                            td.text(task.getEndDate() != null ? task.getEndDate().format(DATE_FORMATTER) : "");
                        } else {
                            td.html("&nbsp;");
                        }
                    }
                }
            }

            return doc.body().html();
        } catch (Exception e) {
            e.printStackTrace();
            return html;
        }
    }

    public static String updateWbsDictionaryTableInHtml(String html, List<Task> tasks) {
        if (html == null) return "";
        try {
            Document doc = Jsoup.parseBodyFragment(html);
            Element container = doc.select("#wbs-dictionary-container").first();
            
            if (container == null) return html;

            Element template = container.select(".wbs-card-template").first();
            if (template == null) return html;
            
            Element templateClone = template.clone();
            container.empty();
            
            // Re-insert the template as hidden so it's available for future updates
            template.attr("style", template.attr("style") + "; display:none;");
            container.appendChild(template);

            // Treat all non-milestone tasks as work packages to display them
            List<Task> workPackages = tasks.stream().filter(t -> !t.isMilestone()).collect(Collectors.toList());
            if (workPackages.isEmpty()) workPackages.addAll(tasks);

            for (Task wp : workPackages) {
                Element card = templateClone.clone();
                card.attr("style", card.attr("style").replace("display:none", "").replace("display: none", ""));
                
                String wpId = wp.getWbsCode() != null ? wp.getWbsCode() : (wp.getId() != null ? wp.getId().toString() : "");
                String wpName = wp.getName() != null ? wp.getName() : "";
                String wpDesc = wp.getDescription() != null ? wp.getDescription() : "";
                
                // Populate fields by finding the label cells
                Elements tds = card.select("td");
                for (int i = 0; i < tds.size(); i++) {
                    Element current = tds.get(i);
                    String cellText = current.text().toLowerCase().replace("\u00a0", " ").trim();
                    
                    if (cellText.equals("nombre del paquete de trabajo")) {
                        if (i + 1 < tds.size()) updateNextCellIfEmpty(tds.get(i + 1), wpName);
                    } else if (cellText.equals("código de cuenta") || cellText.equals("codigo de cuenta")) {
                        if (i + 1 < tds.size()) updateNextCellIfEmpty(tds.get(i + 1), wpId);
                    } else if (cellText.equals("descripción del trabajo") || cellText.equals("descripcion del trabajo")) {
                        Element tr = current.parent();
                        Element tbody = tr.parent();
                        int cellIndex = tr.children().indexOf(current);
                        int rowIndex = tbody.children().indexOf(tr);
                        if (rowIndex + 1 < tbody.children().size()) {
                            Element nextRow = tbody.child(rowIndex + 1);
                            if (cellIndex < nextRow.children().size()) {
                                updateNextCellIfEmpty(nextRow.child(cellIndex), wpDesc);
                            }
                        }
                    }
                }

                for (Element table : card.select("table")) {
                    Element firstRow = table.select("tr").first();
                    if (firstRow != null) {
                        String headerText = firstRow.text().toUpperCase();
                        if (headerText.contains("HITO")) {
                            Element tbody = table.select("tbody").first();
                            if (tbody != null) {
                                Elements rows = tbody.select("> tr");
                                Element hRow = null;
                                if (!rows.isEmpty()) hRow = rows.first().clone();
                                tbody.empty();
                                if (hRow != null) tbody.appendChild(hRow);

                                List<Task> ms = tasks.stream().filter(t -> t.isMilestone() && t.getWbsCode() != null && t.getWbsCode().startsWith(wpId)).collect(Collectors.toList());
                                if (ms.isEmpty()) {
                                    Element tr = tbody.appendElement("tr");
                                    tr.appendElement("td").html("&nbsp;");
                                    tr.appendElement("td").html("&nbsp;");
                                    tr.appendElement("td").html("&nbsp;");
                                    tr.appendElement("td").html("&nbsp;");
                                } else {
                                    for (Task m : ms) {
                                        Element tr = tbody.appendElement("tr");
                                        tr.appendElement("td").text(m.getName());
                                        tr.appendElement("td").text(m.getEndDate() != null ? m.getEndDate().format(DATE_FORMATTER) : "");
                                        tr.appendElement("td").html("&nbsp;");
                                        tr.appendElement("td").html("&nbsp;");
                                    }
                                }
                            }
                        } else if (headerText.contains("RECURSO")) {
                            Element tbody = table.select("tbody").first();
                            if (tbody != null) {
                                Elements rows = tbody.select("> tr");
                                // The resources table has 3 header rows
                                Element hRow1 = rows.size() > 0 ? rows.get(0).clone() : null;
                                Element hRow2 = rows.size() > 1 ? rows.get(1).clone() : null;
                                Element hRow3 = rows.size() > 2 ? rows.get(2).clone() : null;
                                
                                tbody.empty();
                                if (hRow1 != null) tbody.appendChild(hRow1);
                                if (hRow2 != null && hRow2.text().toUpperCase().contains("ACTIVIDAD")) tbody.appendChild(hRow2);
                                if (hRow3 != null && hRow3.text().toUpperCase().contains("HORAS")) tbody.appendChild(hRow3);

                                Element tr = tbody.appendElement("tr");
                                tr.appendElement("td").text(wpId); // ID
                                tr.appendElement("td").text(wpName); // Actividad
                                
                                String resourceType = "";
                                if (wp.getAssignee() != null && wp.getAssignee().getResource() != null && wp.getAssignee().getResource().getResourceType() != null) {
                                    resourceType = wp.getAssignee().getResource().getResourceType();
                                } else if (wp.getAssignee() != null) {
                                    resourceType = wp.getAssignee().getName();
                                }
                                tr.appendElement("td").text(resourceType); // Recurso
                                tr.appendElement("td").text(wp.getDuration() != null ? wp.getDuration().toString() : ""); // Horas
                                tr.appendElement("td").html("&nbsp;"); // Tarifa
                                tr.appendElement("td").html("&nbsp;"); // Total
                                tr.appendElement("td").html("&nbsp;"); // Unidades
                                tr.appendElement("td").html("&nbsp;"); // Coste Unitario
                                tr.appendElement("td").html("&nbsp;"); // Total
                                tr.appendElement("td").html("&nbsp;"); // Coste Total
                            }
                        }
                    }
                }
                
                container.appendChild(card);
            }
            
            return doc.body().html();
        } catch (Exception e) {
            e.printStackTrace();
            return html;
        }
    }
}
