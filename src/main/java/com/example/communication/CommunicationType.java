package com.example.communication;

public enum CommunicationType {
    CHANGE_REQUEST("Solicitud de Cambio"),
    INCIDENT("Incidencia"),
    MEETING("Reunión");

    private final String label;

    CommunicationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
