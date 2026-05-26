package com.example.document;

public enum DocumentType {

    // ── INICIO ───────────────────────────────────────────────────────────────
    ACTA_CONSTITUCION("Acta de Constitución", "Inicio", "inicio/acta-de-constitucion"),
    REGISTRO_INTERESADOS("Registro de Interesados", "Inicio", "inicio/registro-de-interesados"),
    REGISTRO_SUPUESTOS("Registro de Supuestos", "Inicio", "inicio/registro-de-supuestos"),

    // ── PLANIFICACIÓN ────────────────────────────────────────────────────────
    PLAN_DIRECCION_PROYECTOS("Plan de Dirección de Proyectos", "Planificación - A. Integración", "planificacion/A. Integracion/plan-de-direccion-de-proyectos"),
    PLAN_GESTION_COMUNICACIONES("Plan de Gestión de las Comunicaciones", "Planificación - B. Comunicaciones", "planificacion/B. Comunicaciones/plan-de-gestion-de-las-comunicaciones"),
    DICCIONARIO_EDT("Diccionario de la EDT", "Planificación - C. Alcance", "planificacion/C. Alcance/diccionario-de-la-edt"),
    ENUNCIADO_ALCANCE("Enunciado del Alcance", "Planificación - C. Alcance", "planificacion/C. Alcance/enunciado-del-alcance"),
    MATRIZ_TRAZABILIDAD_REQUISITOS("Matriz de Trazabilidad de los Requisitos", "Planificación - C. Alcance", "planificacion/C. Alcance/matriz-de-trazabilidad-de-los-requisitos"),
    PLAN_GESTION_REQUISITOS("Plan de Gestión de los Requisitos", "Planificación - C. Alcance", "planificacion/C. Alcance/plan-de-gestion-de-los-requisitos"),
    PLAN_GESTION_ALCANCE("Plan de Gestión del Alcance", "Planificación - C. Alcance", "planificacion/C. Alcance/plan-de-gestion-del-alcance"),
    REGISTRO_REQUISITOS("Registro de Requisitos", "Planificación - C. Alcance", "planificacion/C. Alcance/registro-de-requisitos"),
    ATRIBUTOS_ACTIVIDAD("Atributos de Actividad", "Planificación - D. Cronograma", "planificacion/D. Cronograma/atributos-de-actividad"),
    LISTA_ACTIVIDADES("Lista de Actividades", "Planificación - D. Cronograma", "planificacion/D. Cronograma/lista-de-actividades"),
    LISTA_HITOS("Lista de Hitos", "Planificación - D. Cronograma", "planificacion/D. Cronograma/lista-de-hitos"),
    PLAN_GESTION_CRONOGRAMA("Plan de Gestión del Cronograma", "Planificación - D. Cronograma", "planificacion/D. Cronograma/plan-de-gestion-del-cronograma"),
    PLAN_GESTION_CALIDAD("Plan de Gestión de Calidad", "Planificación - E. Calidad", "planificacion/E. Calidad/plan-de-gestion-de-calidad"),
    PLAN_GESTION_RIESGOS("Plan de Gestión de Riesgos", "Planificación - F. Riesgos", "planificacion/F. Riesgos/plan-de-gestion-de-riesgos"),
    REGISTRO_RIESGOS("Registro de Riesgos", "Planificación - F. Riesgos", "planificacion/F. Riesgos/registro-de-riesgos"),
    MATRIZ_ASIGNACION_RESPONSABILIDADES("Matriz de Asignación de Responsabilidades", "Planificación - G. Recursos", "planificacion/G. Recursos/matriz-de-asignacion-de-responsabilidades"),
    PLAN_GESTION_RECURSOS("Plan de Gestión de Recursos", "Planificación - G. Recursos", "planificacion/G. Recursos/plan-de-gestion-de-recursos"),
    PLAN_GESTION_ADQUISICIONES("Plan de Gestión de Adquisiciones", "Planificación - H. Adquisiciones", "planificacion/H. Adquisiciones/plan-de-gestion-de-adquisiciones"),
    PLAN_GESTION_COSTES("Plan de Gestión de los Costes", "Planificación - I. Costes", "planificacion/I. Costes/plan-de-gestion-de-los-costes"),
    PLAN_GESTION_CAMBIO("Plan de Gestión del Cambio", "Planificación - J. Cambios", "planificacion/J. Cambios/plan-de-gestion-del-cambio"),
    SOLICITUD_CAMBIO("Solicitud de Cambio", "Planificación - J. Cambios", "planificacion/J. Cambios/solicitud-de-cambio"),
    PLAN_GESTION_CONFIGURACION("Plan de Gestión de la Configuración", "Planificación - K. Configuración", "planificacion/K. Configuracion/plan-de-gestion-de-la-configuracion"),

    // ── EJECUCIÓN ────────────────────────────────────────────────────────────
    INFORME_DESEMPENO_EQUIPO("Informe de Desempeño del Equipo de Trabajo", "Ejecución - Diario", "ejecucion/Diario/informe-de-desempeno-del-equipo-de-trabajo"),
    REGISTRO_CAMBIOS("Registro de Cambios", "Ejecución", "ejecucion/registro-de-cambios"),
    REGISTRO_INCIDENCIAS("Registro de Incidencias", "Ejecución", "ejecucion/registro-de-incidencias"),
    REGISTRO_DECISIONES("Registro de Decisiones", "Ejecución", "ejecucion/registro-de-decisiones"),

    // ── CIERRE ───────────────────────────────────────────────────────────────
    INFORME_CIERRE("Informe de Cierre", "Cierre", "cierre/informe-de-cierre"),
    LECCIONES_APRENDIDAS("Lecciones Aprendidas", "Cierre", "cierre/lecciones-aprendidas");

    private final String label;
    private final String processGroup;
    private final String templatePathWithoutExt;

    DocumentType(String label, String processGroup, String templatePathWithoutExt) {
        this.label = label;
        this.processGroup = processGroup;
        this.templatePathWithoutExt = templatePathWithoutExt;
    }

    public String getLabel() {
        return label;
    }

    public String getProcessGroup() {
        return processGroup;
    }

    public String getTemplatePath() {
        return templatePathWithoutExt + ".html";
    }

    public String getPhaseLabel() {
        return processGroup;
    }

    public boolean isMultiple() {
        return this == INFORME_DESEMPENO_EQUIPO;
    }
}
