package com.example.document;

public enum DocumentType {

    // ── INICIO ───────────────────────────────────────────────────────────────
    ACTA_CONSTITUCION("Acta de Constitución", "inicio", "acta-de-constitucion"),
    REGISTRO_INTERESADOS("Registro de Interesados", "inicio", "registro-de-interesados"),
    REGISTRO_SUPUESTOS("Registro de Supuestos", "inicio", "registro-de-supuestos"),

    // ── PLANIFICACIÓN ────────────────────────────────────────────────────────
    PLAN_GESTION("Plan de Gestión del Proyecto", "planificacion", "plan-de-gestion"),
    PLAN_COMUNICACIONES("Plan de Comunicaciones", "planificacion", "plan-de-comunicaciones"),
    REGISTRO_RIESGOS("Registro de Riesgos", "planificacion", "registro-de-riesgos"),
    PLAN_CALIDAD("Plan de Calidad", "planificacion", "plan-de-calidad"),

    // ── EJECUCIÓN ────────────────────────────────────────────────────────────
    INFORME_ESTADO("Informe de Estado", "ejecucion", "informe-de-estado"),
    REGISTRO_CAMBIOS("Registro de Cambios", "ejecucion", "registro-de-cambios"),
    ACTA_REUNION("Acta de Reunión", "ejecucion", "acta-de-reunion"),

    // ── CIERRE ───────────────────────────────────────────────────────────────
    ACTA_CIERRE("Acta de Cierre", "cierre", "acta-de-cierre"),
    INFORME_FINAL("Informe Final", "cierre", "informe-final");

    private final String label;
    private final String phase;
    private final String slug;

    DocumentType(String label, String phase, String slug) {
        this.label = label;
        this.phase = phase;
        this.slug = slug;
    }

    public String getLabel() {
        return label;
    }

    /** Returns one of: inicio, planificacion, ejecucion, cierre */
    public String getPhase() {
        return phase;
    }

    /**
     * Template path relative to classpath:templates/ e.g.
     * "inicio/acta-de-constitucion.html"
     */
    public String getTemplatePath() {
        return phase + "/" + slug + ".html";
    }

    public String getPhaseLabel() {
        return switch (phase) {
            case "inicio" -> "Inicio";
            case "planificacion" -> "Planificación";
            case "ejecucion" -> "Ejecución";
            case "cierre" -> "Cierre";
            default -> phase;
        };
    }
}
