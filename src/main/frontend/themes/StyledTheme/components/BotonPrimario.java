// En src/main/java/com/tu/proyecto/components/BotonPrimario.java
package com.tu.proyecto.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;

// Este es tu componente Java reutilizable
public class BotonPrimario extends Button {

    public BotonPrimario(String text) {
        super(text);
        // Aplica el tema "primary" de Lumo (que ahora usa tus colores pastel)
        addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    }
    
    public BotonPrimario(String text, com.vaadin.flow.component.Component icon) {
        super(text, icon);
        addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    }
}