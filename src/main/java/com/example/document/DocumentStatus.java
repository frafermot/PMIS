package com.example.document;

public enum DocumentStatus {
    POR_CREAR("Por Crear"),
    EN_PROCESO("En Proceso"),
    FIRMADO("Firmado"),
    ENVIADO("Enviado"),
    VALORADO("Valorado");

    private final String label;

    DocumentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
