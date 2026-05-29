package com.example.examplefeature.ui;

import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("Login | PMIS")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm login = new LoginForm();

    private final com.vaadin.flow.spring.security.AuthenticationContext authContext;

    public LoginView(com.vaadin.flow.spring.security.AuthenticationContext authContext) {
        this.authContext = authContext;

        addClassName("login-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        LoginI18n i18n = LoginI18n.createDefault();
        LoginI18n.Form i18nForm = i18n.getForm();
        i18nForm.setTitle("Iniciar sesión");
        i18nForm.setUsername("UVUS");
        i18nForm.setPassword("Contraseña");
        i18nForm.setSubmit("Entrar");
        i18nForm.setForgotPassword("¿Has olvidado tu contraseña?");
        i18n.setForm(i18nForm);

        LoginI18n.ErrorMessage i18nErrorMessage = i18n.getErrorMessage();
        i18nErrorMessage.setTitle("UVUS o contraseña incorrectos");
        i18nErrorMessage.setMessage("Comprueba que has introducido tu UVUS y contraseña correctamente e inténtalo de nuevo.");
        i18n.setErrorMessage(i18nErrorMessage);

        login.setI18n(i18n);
        login.setAction("login");
        login.addForgotPasswordListener(e -> 
                com.vaadin.flow.component.UI.getCurrent().navigate("forgot-password"));

        add(new H1("PMIS Login"), login);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if (authContext.isAuthenticated()) {
            beforeEnterEvent.forwardTo("");
            return;
        }

        // inform the user about an authentication error
        if (beforeEnterEvent.getLocation()
                .getQueryParameters()
                .getParameters()
                .containsKey("error")) {
            login.setError(true);
        }
    }
}
