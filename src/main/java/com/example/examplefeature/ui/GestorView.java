// Ubicación: com/example/examplefeature/ui/GestorView.java
package com.example.examplefeature.ui;

import com.example.base.ui.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "gestores", layout = MainLayout.class)
@PageTitle("Registro de Gestores")
@Menu(order = 1, icon = "vaadin:users", title = "Registro de Gestores")
public class GestorView extends VerticalLayout {

    public GestorView() {
        add(new H2("Vista de Registro de Gestores"));
        // ... aquí va tu formulario o grid de gestores
    }
}