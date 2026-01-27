package com.example.examplefeature.ui;

import com.example.base.ui.MainLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import jakarta.annotation.security.PermitAll;

@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "inicio", layout = MainLayout.class)
@PageTitle("Inicio")
@Menu(order = 0, icon = "vaadin:home", title = "Inicio")
@PermitAll
public class HomeView extends VerticalLayout {

    public HomeView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        add(
                new H1("Bienvenido a PMIS"),
                new H2("Sistema de Gestión de Proyectos"),
                new Paragraph("Seleccione una opción del menú para comenzar."));
    }
}
