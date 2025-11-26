// Ubicación: com/example/examplefeature/ui/PortfolioView.java
package com.example.examplefeature.ui;

import com.example.base.ui.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class) 
@PageTitle("Portafolios")
@Menu(order = 0, icon = "vaadin:briefcase", title = "Portafolios")
public class PortfolioView extends VerticalLayout {

    public PortfolioView() {
        add(new H2("Vista de Portafolios"));
        // ... aquí va el contenido de tu grid de portafolios
    }
}