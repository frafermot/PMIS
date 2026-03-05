package com.example.document.ui;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;

/**
 * Vaadin server-side wrapper for a CKEditor 5 Classic Build instance.
 * <p>
 * The editor is rendered inside a {@code <div>} shell that also hosts
 * a "Ver HTML" toggle button and a raw-HTML textarea.
 * </p>
 */
@Tag("div")
@JsModule("./ckeditor-editor.js")
@NpmPackage(value = "@ckeditor/ckeditor5-build-classic", version = "41.4.2")
public class CKEditorField extends Component implements HasSize {

    private String content = "";

    public CKEditorField() {
        getElement().getStyle().set("display", "flex");
        getElement().getStyle().set("flex-direction", "column");
        getElement().getStyle().set("height", "100%");
        // Pass null: content always arrives via setValue() which uses the pending-queue
        // path
        getElement().executeJs("initCKEditor($0, null)", getElement());
    }

    /** Sets the HTML content in the editor. */
    public void setValue(String html) {
        this.content = html != null ? html : "";
        getElement().executeJs("setCKEditorContent($0, $1)", getElement(), this.content);
    }

    public void clear() {
        setValue("");
    }

    /** Returns the last HTML content reported by the editor. */
    public String getValue() {
        return content;
    }

    public void setReadOnly(boolean readOnly) {
        getElement().executeJs("setCKEditorReadOnly($0, $1)", getElement(), readOnly);
    }

    /** Called from JavaScript whenever editor content changes. */
    @ClientCallable
    public void updateContent(String html) {
        this.content = html;
    }
}
