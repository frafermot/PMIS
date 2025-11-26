// Ubicación: com/example/examplefeature/ui/PmoView.java
package com.example.examplefeature.ui;

import com.example.base.ui.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "pmo", layout = MainLayout.class)
@PageTitle("Oficina de Proyectos")
@Menu(order = 2, icon = "vaadin:chart-timeline", title = "Oficina de Proyectos")
public class PmoView extends VerticalLayout {

    public PmoView() {
        add(new H2("Vista de Oficina de Dirección de Proyectos (PMO)"));
        // ... aquí van tus dashboards o reportes de PMO
    }
}