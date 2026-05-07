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

import jakarta.servlet.http.HttpServletResponse;

@PageTitle("Error Interno")
@Route("500")
@AnonymousAllowed
public class InternalServerError extends VerticalLayout implements HasErrorParameter<Exception> {

    public InternalServerError() {
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        Icon icon = VaadinIcon.WARNING.create();
        icon.setSize("100px");
        icon.addClassNames(LumoUtility.TextColor.WARNING, LumoUtility.Margin.Bottom.LARGE);

        H1 title = new H1("500");
        title.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.SMALL);

        Paragraph text = new Paragraph("Ocurrió un error inesperado en el servidor. Por favor, inténtalo de nuevo más tarde.");
        text.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.LARGE, LumoUtility.Margin.Bottom.LARGE);

        Button button = new Button("Volver al Inicio", e -> UI.getCurrent().navigate(""));
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(icon, title, text, button);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<Exception> parameter) {
        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }
}
