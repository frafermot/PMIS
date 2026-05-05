package com.example.base.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

import com.vaadin.flow.router.AccessDeniedException;
import jakarta.servlet.http.HttpServletResponse;

@PageTitle("Acceso Denegado")
@Route("403")
@AnonymousAllowed
public class AccessDeniedError extends VerticalLayout implements HasErrorParameter<AccessDeniedException> {

    public AccessDeniedError() {
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        Icon icon = VaadinIcon.LOCK.create();
        icon.setSize("100px");
        icon.addClassNames(LumoUtility.TextColor.ERROR, LumoUtility.Margin.Bottom.LARGE);

        H1 title = new H1("403");
        title.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.SMALL);

        Paragraph text = new Paragraph("Acceso denegado. No tienes permisos suficientes para ver esta página.");
        text.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.LARGE, LumoUtility.Margin.Bottom.LARGE);

        Button button = new Button("Volver al Inicio", e -> UI.getCurrent().navigate(""));
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(icon, title, text, button);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<AccessDeniedException> parameter) {
        return HttpServletResponse.SC_FORBIDDEN;
    }
}
