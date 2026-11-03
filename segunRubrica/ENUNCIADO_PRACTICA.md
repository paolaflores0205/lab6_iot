# Simulacro: Mis gastos

Ejercicio de práctica creado para cubrir los criterios de la rúbrica del Lab6. No es el enunciado oficial de la evaluación.

Desarrolla una aplicación Android que permita a cada usuario registrar sus gastos personales y consultar estadísticas.

## Acceso e interfaz (2 + 5 puntos de la rúbrica)

- Pantalla en español con logo y opciones claras de acceso/registro.
- Correo y contraseña, Google y Facebook mediante Firebase Authentication.
- Recuperación de contraseña.
- Conservar la sesión al reabrir y permitir cerrar sesión sin volver a datos privados con Atrás.

## CRUD (5 puntos)

Cada gasto contiene descripción, categoría, importe en soles, fecha y estado (Pendiente/Pagado).

- Crear: validar campos, importe positivo con hasta dos decimales y fecha elegida.
- Leer: listar solo los gastos del usuario con todos sus campos; sincronizar cambios en tiempo real.
- Editar: cargar los valores y actualizar el mismo documento.
- Eliminar: pedir confirmación y borrar de Firestore.
- Separar datos por UID y protegerlos con reglas.

Las categorías son Alimentación, Transporte, Estudios y Otros. En esta práctica se pueden editar/eliminar tanto pendientes como pagados; la restricción de solo pendientes pertenecía al lab del Mundial y no a la rúbrica general.

## Estadísticas (4 puntos)

- Gráfico de barras: monto total por categoría.
- Gráfico circular: cantidad de registros por estado.
- Filtro por mes (año-mes) que afecta los dos gráficos y los totales.
- Actualización automática al cambiar los documentos.
- Manejar un mes vacío sin dividir por cero ni mostrar datos anteriores.

## Funcionalidad (4 puntos)

- Navegación entre Mis gastos, Estadísticas y Cerrar sesión.
- Formulario reutilizado para creación/edición.
- Validación y tratamiento de errores de Firebase.
- Probar dos usuarios, edición sin duplicados y cancelación de borrado.

La solución está en el proyecto. Sigue `GUIA_SOLUCION.md` para configurarla y entenderla por etapas.
