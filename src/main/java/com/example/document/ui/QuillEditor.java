package com.example.document.ui;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;

@Tag("div")
@JsModule("./quill-editor.js")
@NpmPackage(value = "quill", version = "1.3.7")
public class QuillEditor extends Component implements HasSize {

    private String content = "";

    public QuillEditor() {
        getElement().getStyle().set("height", "500px"); // altura visible
        // Inicializa Quill y pasa el ID del componente para callbacks
        getElement().executeJs("initQuill($0)", getElement());
    }

    public void setValue(String html) {
        this.content = html != null ? html : "";
        getElement().executeJs("setContent($0, $1)", getElement(), this.content);
    }

    public void clear() {
        setValue("");
    }

    public String getValue() {
        return content;
    }

    // Llamado desde JS cada vez que el contenido cambia
    @ClientCallable
    public void updateContent(String html) {
        this.content = html;
    }
}
