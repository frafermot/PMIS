## 1. Guía Básica de Pruebas

*(🚨 **NOTA IMPORTANTE:** Cada vez que crees una cuenta nueva desde la plataforma, la contraseña generada aparecerá en un mensaje abajo a la izquierda de la pantalla).*

### FASE 1: Administrador
> **Objetivo**: Crear gestor principal y la estructura base.
1. Iniciar sesión como **Administrador** (Usuario: `jmcordero` / Contraseña: `password`).
2. Ir a "Registro de Gestores" y crear a un único gestor: el **Gestor Evaluador** **[R6, R7]**. *(Anota la contraseña de abajo a la izquierda)*.
3. En la misma pestaña de "Registro de Gestores", hacer clic en este **Gestor Evaluador**, marcar "Es Admin" y Guardar **[R8]**.
4. Ir a "Portafolios", crear el Portafolio 1 y elegir al **Gestor Evaluador** como su Director **[R1, R9, R10]**.
5. Ir a "PMOs", crear la PMO del portafolio y marcar al **Gestor Evaluador** como Director de la misma **[R4, R5, R15, R16]**.
6. Cerrar sesión.

### FASE 2: Preparación del Gestor
> **Objetivo**: Crear proyecto y usuarios sin cambiar de cuenta.
1. Iniciar sesión con la nueva cuenta del **Gestor Evaluador**. *(Al ser Director de Portafolio, PMO y Programa, puedes hacerlo todo de golpe).*
2. Ir a "Registro de Usuarios de Proyectos" y crear a un nuevo usuario (Ej: **Usuario 1**). *(Anota su contraseña)*. **[R18, R19]**.
3. Ir al portfolio creado, crear el Programa 1 y asignarte a ti mismo (**Gestor Evaluador**) como Director de Programa **[R2, R11, R12]**.
4. Dentro del programa, dale a crear Proyecto **[R3, R13]**.
5. Al crearlo, pon como Patrocinador al **Gestor Evaluador** **[R14]**.
6. Entra al proyecto creado y en el apartado de **Usuarios asignados** despliegalo y añade al usuario **Usuario 1** y asignalo como Director de Proyecto **[R21]**.
7. Cerrar sesión.

### FASE 3: Desarrollo del Proyecto (Director del Proyecto)
> **Objetivo**: Simular el trabajo de PGPI.
1. Iniciar sesión con la cuenta de **Usuario 1** (ahora Director de Proyecto).
2. Ir a la vista de "Proyecto" y asignar equipo de trabajo **[R22]**.
3. Ir al menú **Procesos** > *Grupo de Inicio* > *Acta de Constitución* **[R30,R32]**.
4. Pulsar en **Crear**, rellenar el formulario PGPI y guardar **[R26, R29]**.
5. Ir al Estado del Proyecto y comprobar visualmente que el proceso consta como ejecutado **[R27, R34]**.

### FASE 4: Chat y Comunicación (SCI)
> **Objetivo**: Comunicaciones oficiales.
*(Aún iniciada la sesión de Usuario 1)*
1. Entrar a la sección **Control de comite de cambios** **[R36, R37, R38]**.
2. Clasificar el motivo (Solicitud Cambio, Incidencia...) y Guardar **[R40]**.
3. Confirmar que se queda registrado solo en este proyecto **[R39, R41, R42]**.
4. Cerrar sesión.

### FASE 5: Evaluaciones y Notas (Evaluador)
> **Objetivo**: Históricos, consultas y calificaciones.
1. Iniciar sesión por última vez con el **Gestor Evaluador**.
2. Mirar la vista general y comprobar que puedes ver tus proyectos desde la perspectiva de lectura/consulta **[R24, R25]**.
3. Abrir La Acta de Constitución y ojear el historial de ediciones **[R28, R35]**.
4. Darle una **Valoración Numérica** (ponerle nota) y guardar **[R32, R33]**.
5. En cualquier documento que tenga contenido, abrir el menú **Acciones** y seleccionar **Descargar PDF** para bajar el documento individual. **[R43,R44]**
6. Cerrar sesión.

### FASE 6: Gestión del Cronograma (Gantt y Recursos)
> **Objetivo**: Planificación temporal y asignación de recursos.
1. Iniciar sesión como **Usuario 1** (Director de Proyecto).
2. Entrar en su proyecto y desplegar la sección de **Cronograma**.
3. Definir fechas de inicio y fin si el proyecto es nuevo y pulsar **Crear Cronograma**.
4. Pulsar el botón **Acceder al Cronograma**.
5. Una vez creado, en **Configuracion** verificar que aparece el resumen del **Calendario Laboral** (días y horas de trabajo configurados).
6. En **Configuracion** tambien aparece un boton de **Visibilidad de Columnas** el cual permite configurar las columnas que se muestran en la vista Gantt. Y otro de **Cambiar a dias** para cambiar la duracion de horas a dias.
7. En la vista Gantt, crear una nueva tarea pulsando el botón "+" o haciendo doble clic en el calendario.
8. Ajustar las horas de inicio y fin de la tarea (planificación a nivel de hora) y guardar.
9. Añadir varias tareas que dependan de ellas y asignar un recurso. **Vincular un recurso**, desde la PMO se crean los recursos. Asegurarse de que el recurso se vincula correctamente al perfil desde la PMO.
10. Entrar en el cronograma y ver el coste de cada tarea y el coste total del proyecto.
11. En el boton **Planificacion** elegir las opciones **Asignar EDT** y **Calcular ruta critica**. Ademas de **Fijar linea base** para que se guarde la planificacion.
12. En el icono de **Configuración** en la parte superior derecha, **Visibilidad de las columnas**, añadir las columnas de la linea base y verificar que se muestran correctamente.
13. Cerrar sesión.

---

## 2. Estado de Implementación Actual

1. **Requisitos Activos Integrados:** 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45
2. **Requisitos Pendientes (No desarrollados):** Ninguno


## 3. Peticiones

- Se ha completado la integración del cronograma (Gantt) con planificación a nivel de hora y vinculación de recursos PMO.
- Pendiente: Implementar flujo de firma digital para documentos finalizados.