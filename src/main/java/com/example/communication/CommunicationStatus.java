package com.example.communication;

public enum CommunicationStatus {
    OPEN("Abierto"),
    IN_PROGRESS("En Progreso"),
    RESOLVED("Resuelto"),
    CLOSED("Cerrado");

    private final String label;

    CommunicationStatus(String label) {
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
