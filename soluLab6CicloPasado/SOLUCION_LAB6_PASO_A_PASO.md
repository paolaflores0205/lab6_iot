# Lab6 del ciclo pasado: solución guiada para repasar

Proyecto: **soluLab6CicloPasado**, Java + XML + View Binding + FirebaseUI 9.1.1 + Cloud Firestore. Este documento recorre lo que hicimos juntos y los pasos añadidos para terminar la práctica. Es una guía de estudio, no una entrega oficial del laboratorio de hoy.

## 0. Si tienes poco tiempo, empieza aquí

Lee en este orden: pasos **4 (ruta/seguridad), 8 (guardar), 10–12 (leer/ID/seleccionar), 15 (editar), 16 (tiempo real), 17 (contar/filtrar)**. Son los patrones que más se reutilizan si cambian Mundial por inventario, reservas, tareas o sensores.

Antes de ejecutar: revisa [CONFIGURACION_FIREBASE.md](CONFIGURACION_FIREBASE.md). Falta publicar el archivo final `firestore.rules` en la consola y habilitar las credenciales sociales reales. La app no puede configurar por sí sola una OAuth App de GitHub ni las firmas de Google.

Lo esencial:

```text
Authentication identifica al usuario → obtenemos su UID
Ruta = usuarios/UID/pronosticos
Crear = colección.add(datos)
Leer = colección.get() o addSnapshotListener(...)
ID real = documento.getId()
Editar = documento.update(campos)
Eliminar = documento.delete()
Lista = ArrayList de objetos + ArrayList de textos + ArrayAdapter
Gráficos = filtrar objetos → contar desde cero → redibujar
```

## 1. Qué pide y dónde se resolvió

| Requisito | Archivo o pantalla final |
|---|---|
| Acceso/registro en español y logo | `MainActivity.java`, `activity_main.xml`, FirebaseUI y `logo_mundial.xml`. |
| Recuperación de contraseña | Botón propio en `MainActivity` y flujo de correo FirebaseUI. |
| Google y GitHub del lab antiguo | Botones y proveedores de `MainActivity`; configuración externa en el documento adjunto. |
| Google y Facebook de la rúbrica | Google más opción Facebook activable por Gradle; requiere configuración real en Meta/Firebase. |
| Barra Mis pronósticos / Estadísticas / Cerrar sesión | `res/menu/navegacion.xml` y `Pantallas.java`. |
| Crear y editar con el mismo formulario | `PronosticosActivity.java`, `activity_pronosticos.xml`. |
| Lista con equipos, fecha, goles y estado | `ListaPronosticosActivity.java`. |
| Pendiente automático al crear | `guardarPronostico()` y `firestore.rules`. |
| Solo editar/eliminar pendientes | Comprobación visual, transacciones y reglas finales. |
| Confirmación de borrado | `AlertDialog` en `ListaPronosticosActivity`. |
| Datos y estadísticas en tiempo real | Listeners en `onStart()`; se retiran en `onStop()`. |
| Notificaciones de registro/actualización | `Notificaciones.java`, permiso y canal Android. |
| Dos gráficos con filtro de selección | `EstadisticasActivity`, `ResumenPronosticos` y `GraficoView`. |

## 2. Mapa de archivos para no perderse

Las clases Java están en `app/src/main/java/com/example/solulab6ciclopasado/`.

| Clase | Qué necesitas entender |
|---|---|
| `MainActivity` | Acceso, recibir resultado, recuperar contraseña, saltar el login si hay sesión. |
| `Pronostico` | Modelo de los campos y el ID. |
| `PronosticosActivity` | Leer formulario, validar, crear o actualizar según si recibimos ID. |
| `ListaPronosticosActivity` | Leer colección, mostrar filas, seleccionar, editar/eliminar. |
| `EstadisticasActivity` | Listener, filtro y actualización de gráficos. |
| `ResumenPronosticos` | Un bucle que cuenta los registros del filtro. |
| `Pantallas` | Ruta por UID, navegación, cerrar sesión y márgenes de barras del sistema. |
| `Notificaciones` | Permiso, canal y aviso local. |
| `GraficoView` | Dibujo de barras/circular. Puedes estudiar su uso sin memorizar el dibujo. |

Los XML están en `app/src/main/res/layout/`. Los IDs XML se convierten en campos de la clase Binding.

## 3. Lo que hicimos juntos: configuración inicial

### Paso 1. Crear el proyecto y registrarlo en Firebase

Creamos una Empty Views Activity con Java. En Firebase registramos el `applicationId` exacto:

```text
com.example.solulab6ciclopasado
```

El nombre visible del proyecto puede ser diferente: Firebase necesita que coincida el **identificador de la app**, no el nombre de la carpeta. Descargamos `google-services.json` y lo colocamos en `app/`, junto a su Gradle.

### Paso 2. Corregir el Gradle

El error que apareció era por copiar el plugin Android `8.6.0` del ejemplo sobre un proyecto que ya usaba `9.3.3`. Conservamos el alias de la plantilla.

En `build.gradle` **del proyecto**, el bloque declara plugins disponibles:

```groovy
plugins {
    alias(libs.plugins.android.application) apply false
    id 'com.google.gms.google-services' version '4.5.0' apply false
}
```

En `app/build.gradle`, el bloque los **aplica**:

```groovy
plugins {
    alias(libs.plugins.android.application)
    id 'com.google.gms.google-services'
}
```

`apply false` corresponde al nivel donde solo declaras el plugin. Dentro de `app` sí lo necesitamos aplicado. No copies todo el Gradle de otro proyecto: integra las líneas necesarias.

En `dependencies` de `app` agregamos:

```groovy
implementation platform('com.google.firebase:firebase-bom:35.1.0')
implementation 'com.google.firebase:firebase-auth'
implementation 'com.google.firebase:firebase-firestore'
implementation 'com.firebaseui:firebase-ui-auth:9.1.1'
```

La BoM coordina versiones de los SDK Firebase; FirebaseUI se versiona aparte. La dependencia de Analytics que ya tenías se conserva, pero no es necesaria para el CRUD.

### Paso 3. Activar View Binding

Dentro de `android` del Gradle de `app`:

```groovy
buildFeatures {
    viewBinding true
}
```

Después de Sync, cada XML genera su Binding. Por ejemplo, `activity_pronosticos.xml` genera `ActivityPronosticosBinding`.

En cada Activity:

```java
// Campo de la clase, fuera de onCreate:
private ActivityPronosticosBinding binding;

// Dentro de onCreate, después de super.onCreate:
binding = ActivityPronosticosBinding.inflate(getLayoutInflater());
setContentView(binding.getRoot());
```

`setContentView` es obligatorio para mostrar el diseño. La pantalla blanca que vimos se debía a que faltaba cargar el XML en `PronosticosActivity`.

### Paso 4. Base, ruta por usuario y reglas

Habilitamos Correo/contraseña en Authentication y creamos Cloud Firestore `(default)`. Authentication no guarda pronósticos: guarda cuentas y entrega un UID.

La referencia original, dentro de una Activity autenticada, fue:

```java
FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
if (usuario == null) {
    finish();
    return;
}

CollectionReference ref = FirebaseFirestore.getInstance()
        .collection("usuarios")
        .document(usuario.getUid())
        .collection("pronosticos");
```

En la versión final ese mismo código está en `Pantallas.pronosticos()` para no repetirlo. Se utiliza después de comprobar la sesión.

```text
usuarios                 colección
  UID_ANA                documento de ruta del usuario
    pronosticos          subcolección
      abc123             documento de un pronóstico
        seleccionA: ...  campos
```

Las reglas son **el control de permisos del servidor**, no una consulta. El `match` coincide con una ruta y la condición decide si permite la petición. Al principio publicamos propietario autenticado; las reglas finales añaden validaciones y bloqueo por estado. Están en `firestore.rules` y hay que publicarlas en la consola.

## 4. Lo que hicimos juntos: autenticación y navegación

### Paso 5. Seguir FirebaseUI del profesor

Primero planteamos campos propios; después revisamos Clase10 y seguimos FirebaseUI. Eliminamos los EditText del acceso porque FirebaseUI ya proporciona las pantallas para registrarse e ingresar.

En `MainActivity`, el launcher va como **campo de la clase**, fuera de `onCreate`:

```java
private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
        new FirebaseAuthUIActivityResultContract(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                // Ahora podemos navegar a la pantalla autenticada.
            }
        });
```

Tiene contrato y callback: abre otro flujo y recibe su resultado al terminar. En la versión final también se verifica que exista un usuario antes de navegar.

El click construye el Intent y lo lanza:

```java
Intent intent = AuthUI.getInstance().createSignInIntentBuilder()
        .setAvailableProviders(Collections.singletonList(
                new AuthUI.IdpConfig.EmailBuilder().build()))
        .setLogo(R.drawable.logo_mundial)
        .build();
signInLauncher.launch(intent);
```

En el código final `lanzarAcceso(proveedor)` reúne estas líneas y añade tema/manejo de errores. Los botones eligen el proveedor: EmailBuilder, GoogleBuilder o GitHubBuilder.

Registrarse crea una cuenta y también inicia sesión. Acceder comprueba una cuenta existente. Ambas operaciones exitosas pueden devolver `RESULT_OK`. Tener Gmail no significa haber creado ya una cuenta con contraseña en esta aplicación.

### Paso 6. Logo, idioma y recuperación

Usamos primero `pucp` del profesor. La versión final conserva ese archivo y añade un logo vectorial de fútbol `logo_mundial.xml`, sin descargas externas.

```java
.setLogo(R.drawable.logo_mundial)
```

El recurso no lleva extensión en Java/XML. Para correo en español se usa `FirebaseAuth.setLanguageCode("es")`; para las pantallas se configura el idioma de la app con `AppCompatDelegate.setApplicationLocales(...)`. Son configuraciones distintas.

Se añadió un botón Recuperar contraseña que pide correo válido y ejecuta `sendPasswordResetEmail(email)`. El resultado se muestra sin intentar descubrir si la cuenta existe.

### Paso 7. Abrir otra Activity y cerrar sesión

Creamos Activities con Empty Views Activity y registramos sus layouts/bindings.

```java
Intent intent = new Intent(this, ListaPronosticosActivity.class);
startActivity(intent);
```

`finish()` cierra la Activity actual. Cuando abrimos el formulario desde la lista no lo usamos: queremos volver a la lista al terminar. Al cerrar sesión sí limpiamos toda la pila para evitar regresar a datos del usuario anterior.

El código que ya usamos:

```java
AuthUI.getInstance().signOut(this).addOnCompleteListener(task -> {
    if (task.isSuccessful()) {
        // Volver a MainActivity y limpiar navegación.
    }
});
```

En la versión final lo centraliza `Pantallas`. Además `MainActivity.onStart()` reconoce una sesión ya iniciada y abre directamente Mis pronósticos.

## 5. Lo que hicimos juntos: creación del pronóstico

### Paso 8. Modelo y primera escritura de prueba

Creamos `Pronostico.java` con campos privados:

```java
private String id;
private String seleccionA;
private String seleccionB;
private Date fechaPartido;
private int golesA;
private int golesB;
private String estado;
```

Agregamos constructor público vacío y generamos getters/setters con **Alt + Insert → Getter and Setter**. El constructor vacío permite `toObject(Pronostico.class)`.

Primero guardamos un objeto fijo Perú–Brasil para probar la conexión:

```java
Pronostico p = new Pronostico();
p.setSeleccionA("Perú");
p.setSeleccionB("Brasil");
// Completar fecha, goles y estado...
ref.add(p).addOnSuccessListener(documento -> {
    // Escritura confirmada.
});
```

Ese botón fue una prueba de Firebase, no el formulario definitivo. Luego reemplazamos sus valores por los campos de pantalla. El ID del botón sigue llamándose `btnGuardarPrueba` para conservar lo que aprendiste; ahora guarda datos reales.

### Paso 9. Formulario, calendario y validación

Creamos `etSeleccionA`, `etSeleccionB`, `etGolesA`, `etGolesB` y `btnFecha`. Ahora están en un `ScrollView` con `LinearLayout` para evitar superposiciones y poder llegar a Guardar con el teclado abierto.

En un click, primero leemos los textos:

```java
String a = binding.etSeleccionA.getText().toString().trim();
String b = binding.etSeleccionB.getText().toString().trim();
```

Validamos vacíos, selecciones diferentes, fecha seleccionada y goles enteros no negativos. `inputType="number"` ayuda con el teclado, pero no sustituye la validación Java. `Integer.parseInt()` va dentro de `try/catch`.

La fecha se elige con `DatePickerDialog` y se conserva en una variable `Date` de la clase:

```java
Calendar elegida = Calendar.getInstance();
elegida.clear();
elegida.set(anio, mes, dia);
fechaPartido = elegida.getTime();
```

`mes` ya va de 0 a 11: solo sumamos uno al mostrar manualmente el texto. Firestore almacena Date como timestamp; no guardamos el texto `dd/MM/yyyy` como fecha.

## 6. Lo que hicimos juntos: lista, selección y borrado

### Paso 10. ListView y adaptador

Creamos `ListaPronosticosActivity` y un `ListView`. Elegimos `ListView` para reducir código; el enunciado anterior pide una lista, no obliga a RecyclerView.

Fuera de `onCreate`, declaramos:

```java
private final ArrayList<String> filas = new ArrayList<>();
private final ArrayList<Pronostico> pronosticos = new ArrayList<>();
private ArrayAdapter<String> adapter;
```

Dentro de `onCreate`, después del binding:

```java
adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filas);
binding.listaPronosticos.setAdapter(adapter);
```

El adaptador conecta los textos con las filas. Un RecyclerView también usa adaptador, pero normalmente una clase propia y ViewHolder. El adaptador no consulta Firebase por sí solo.

### Paso 11. Leer una vez con get()

La primera lectura que programamos fue:

```java
ref.get().addOnSuccessListener(documentos -> {
    filas.clear();
    pronosticos.clear();
    for (QueryDocumentSnapshot documento : documentos) {
        Pronostico p = documento.toObject(Pronostico.class);
        p.setId(documento.getId());
        pronosticos.add(p);
        filas.add(p.getSeleccionA() + " vs " + p.getSeleccionB());
    }
    adapter.notifyDataSetChanged();
});
```

Cada snapshot debe **reconstruir** las listas, no seguir agregando encima. La versión final muestra también fecha, goles y estado, y sustituye `get()` por tiempo real.

### Paso 12. Por qué conservar el ID

El documento tiene ruta y campos. `toObject()` convierte los campos; no añade automáticamente el ID que está en la ruta.

```text
usuarios/miUID/pronosticos/abc123
                           ↑ ID del documento
```

```java
p.setId(documento.getId());
```

Esto solo guarda en memoria la identificación del documento original. No cambia Firebase. En la versión final `getId()` tiene `@Exclude` para que el ID no vuelva a escribirse como un campo al serializar el modelo.

`filas` y `pronosticos` conservan el mismo orden: la posición de una fila permite obtener su objeto completo.

### Paso 13. Click de botón frente a click de fila

Un botón usa `setOnClickListener`. El ListView usa `setOnItemClickListener` porque necesitamos la posición del elemento:

```java
binding.listaPronosticos.setOnItemClickListener((parent, view, posicion, id) -> {
    Pronostico seleccionado = pronosticos.get(posicion);
    // Mostrar opciones para ese objeto.
});
```

El `id` del callback pertenece a Android; el ID Firebase es `seleccionado.getId()`. Si el estado no es Pendiente, se muestra un aviso y se sale con `return`. Si es Pendiente, un AlertDialog ofrece Editar y Eliminar.

### Paso 14. Confirmar y borrar

El menú Eliminar abre otro diálogo. Cancelar no escribe nada. Confirmar llama a `eliminarPronostico(seleccionado)`.

El patrón básico que programamos fue:

```java
ref.document(seleccionado.getId()).delete();
```

Después quitábamos la fila localmente. En la versión final el listener se encarga de reconstruir ambas listas; ya no borramos manualmente la fila para evitar hacerlo dos veces. El borrado final usa una transacción que vuelve a comprobar Pendiente en el servidor antes de eliminar.

## 7. Pasos que se completaron después, sin más confirmaciones

### Paso 15. Terminar Editar usando el mismo formulario

Desde la lista enviamos el ID:

```java
Intent intent = new Intent(this, PronosticosActivity.class);
intent.putExtra("pronosticoId", seleccionado.getId());
startActivity(intent);
```

En `PronosticosActivity.onCreate`, después de preparar referencia y botones:

```java
pronosticoId = getIntent().getStringExtra("pronosticoId");
```

La decisión principal es sencilla:

```text
pronosticoId == null → crear un documento nuevo
pronosticoId != null → cargar y actualizar ese documento
```

`cargarPronostico()` hace `.document(pronosticoId).get()`, comprueba que exista y siga pendiente, convierte el modelo y rellena las vistas. Los goles usan `String.valueOf(...)`: pasar directamente un entero a `setText(int)` puede tratarlo como un recurso Android.

Añadí `spEstado` con Pendiente, Acertado y Fallado. En creación está deshabilitado y se guarda siempre Pendiente; en edición permite elegir otro estado. Una vez guardado Acertado/Fallado no se puede volver a modificar ni borrar.

`guardarPronostico()` reúne la validación y construye un mapa con seis campos:

```java
Map<String, Object> campos = new HashMap<>();
campos.put("seleccionA", a);
campos.put("seleccionB", b);
campos.put("fechaPartido", fechaPartido);
campos.put("golesA", ga);
campos.put("golesB", gb);
campos.put("estado", pronosticoId == null ? "Pendiente"
        : binding.spEstado.getSelectedItem().toString());
```

El mapa evita escribir campos que no queremos modificar, como un ID. El patrón reutilizable de edición es:

```java
ref.document(pronosticoId).update(campos);
```

**Nunca uses `add()` para editar:** crea otro documento. La implementación final envuelve `update` en una transacción:

```java
FirebaseFirestore.getInstance().runTransaction(transaccion -> {
    if (!"Pendiente".equals(transaccion.get(documento).getString("estado"))) {
        throw new IllegalStateException("Solo puedes editar pronósticos pendientes");
    }
    transaccion.update(documento, campos);
    return null;
});
```

Solo necesitas entender la intención: **leer el estado actual y modificar como una operación indivisible**. Evita que un segundo dispositivo cierre el registro entre la lectura y la escritura. Si hay conflicto Firebase reintenta; la transacción necesita conexión. En el callback de transacción no modificamos vistas ni enviamos notificaciones; eso va en el éxito final.

Después del éxito mostramos Toast, emitimos la notificación y cerramos el formulario. Mientras escribe, se deshabilitan controles para evitar pulsaciones duplicadas. La fecha se conserva al recrear la pantalla; el estado de las vistas lo restaura Android.

### Paso 16. Cambiar get() por tiempo real

`get()` lee una sola vez. Para el lab necesitamos que lista y estadísticas reflejen cambios sin pulsar recargar.

Declaramos como campo:

```java
private ListenerRegistration escucha;
```

En `onStart()`, después de comprobar sesión y obtener la referencia:

```java
escucha = ref.addSnapshotListener((documentos, error) -> {
    if (error != null) {
        // Mostrar error y salir.
        return;
    }
    if (documentos == null) return;
    // Limpiar listas, recorrer documentos, convertir y refrescar adapter.
});
```

En `onStop()`:

```java
if (escucha != null) {
    escucha.remove();
    escucha = null;
}
super.onStop();
```

Por eso volver del formulario refresca la lista: `onStart()` registra otra escucha con un snapshot inicial actualizado. No hay que llamar manualmente a `onCreate`, reiniciar la Activity ni incrementar contadores en dos lugares.

### Paso 17. Estadísticas con un bucle y filtro

Creamos `EstadisticasActivity` con listener propio sobre la misma colección. Guarda los pronósticos en un ArrayList y llena un Spinner de selecciones. El filtro inicial es Todas las selecciones.

Cuando llega un snapshot o cambia el Spinner, llama a `actualizarGraficos()`. Esa función utiliza `ResumenPronosticos.contar(pronosticos, filtroActual)`.

La lógica del conteo está aislada para poder probarla sin Firebase:

```java
for (Pronostico p : datos) {
    boolean mostrar = "Todas las selecciones".equals(filtro)
            || filtro.equalsIgnoreCase(p.getSeleccionA())
            || filtro.equalsIgnoreCase(p.getSeleccionB());
    if (!mostrar) continue;
    resumen.total++;
    if ("Pendiente".equals(p.getEstado())) resumen.pendientes++;
    else if ("Acertado".equals(p.getEstado())) resumen.acertados++;
    else if ("Fallado".equals(p.getEstado())) resumen.fallados++;
}
```

`continue` salta únicamente ese registro y sigue el bucle. `return` saldría del método completo: no los confundas.

Los contadores empiezan en cero en cada cálculo. El mismo equipo puede aparecer como A o B; ambos casos se incluyen. No se cuentan registros de otros usuarios porque la consulta ya usa su UID.

### Paso 18. Dos gráficos sin añadir una librería externa

`GraficoView` dibuja barras y circular con Canvas. Se usa dos veces en `activity_estadisticas.xml`.

Desde `EstadisticasActivity` solo importa:

```java
binding.graficoBarras.setDatos(resumen.pendientes, resumen.acertados, resumen.fallados);
binding.graficoCircular.setDatos(resumen.pendientes, resumen.acertados, resumen.fallados);
```

El segundo se configura con `setCircular(true)`. `setDatos()` reemplaza valores y llama a `invalidate()` para redibujar.

Las barras escalan por el máximo; el circular usa `360 * cantidad / total`. Si el total es cero se muestra Sin pronósticos, evitando división por cero. Ambos gráficos y el resumen usan el **mismo filtro**. No necesitas memorizar Canvas para repetir el algoritmo de datos; si te dan una librería de gráficos, cambia la presentación y conserva filtro/conteo.

### Paso 19. Barra de navegación y estructura final

La pantalla de entrada ahora lleva a **ListaPronosticosActivity**. Desde allí Nuevo pronóstico abre el formulario. El flujo final es:

```text
Acceso → Mis pronósticos (lista)
           ├── Nuevo → formulario vacío → guardar → volver a lista
           ├── Tocar pendiente → Editar → formulario con ID → actualizar
           ├── Tocar pendiente → Eliminar → confirmar
           ├── Estadísticas → dos gráficos + filtro
           └── Cerrar sesión → acceso con pila de navegación limpia
```

Se añadió `BottomNavigationView` a lista, formulario y estadísticas. Sus opciones están en `res/menu/navegacion.xml`. `Pantallas.navegar()` conecta los clicks y evita repetir la misma lógica en tres archivos.

`Pantallas.ajustarBarras()` agrega los espacios de las barras del sistema y del teclado. Eso evita que botones o textos queden detrás de ellas cuando se usa EdgeToEdge.

### Paso 20. Notificación real después de guardar

El Toast de la prueba era solo un mensaje momentáneo. El enunciado pide una notificación; ahora `Notificaciones.java` implementa:

1. Canal de avisos.
2. Solicitud de permiso `POST_NOTIFICATIONS`.
3. Notificación local con icono y acción para abrir la lista.

Se llama a `Notificaciones.preparar(this)` al abrir formulario y a `Notificaciones.mostrar(this, texto)` **en el éxito de la escritura**, nunca antes. Si el permiso está denegado, Android no muestra la notificación; la escritura no se pierde.

No necesitas FCM para este aviso local. El patrón viene de Lab5.

### Paso 21. Reglas finales y configuración social

Se creó `firestore.rules` para proteger propietario, campos y transiciones. El control visual del botón no basta: otro cliente podría intentar escribir directamente. Debes publicar estas reglas para que Firebase aplique la protección en el servidor.

Se añadieron Google y GitHub mediante FirebaseUI. Sus botones no sustituyen la habilitación del proveedor, SHA/OAuth y secretos de consola. Los pasos, incluyendo la SHA debug de esta computadora, están en `CONFIGURACION_FIREBASE.md`.

Para la diferencia de la rúbrica, Facebook está preparado como opción `facebook=true` de Gradle: incorpora el SDK, recursos y botón cuando configures Meta. No se activa por defecto porque estamos resolviendo el enunciado antiguo y todavía no hay credenciales de Facebook.

## 8. Receta para otro lab con otra temática

1. Lee campos, tipos, restricciones, gráficos y proveedores exigidos.
2. Prepara Firebase/JSON/plugins/SDK y habilita los servicios.
3. Obtén sesión y UID; elige una ruta de colección que usarás en todas las operaciones.
4. Publica reglas que coincidan con esa ruta y con las restricciones.
5. Crea modelo con constructor vacío, getters/setters e ID excluido.
6. Crea formulario XML y Binding; primero valida, después escribe.
7. Usa `add()` para crear; estado inicial automático si corresponde.
8. Lee colección, convierte con `toObject()`, conserva `getId()` y muestra adapter.
9. Selecciona objeto por posición y pasa su ID a edición.
10. Si hay ID, carga documento y usa `update`; si no hay ID, crea.
11. Confirma el borrado y usa el ID del documento original.
12. Para tiempo real, listener en `onStart` y retirada en `onStop`.
13. Para estadísticas, filtra registros, cuenta desde cero y reemplaza datos de ambos gráficos.
14. Notifica después del éxito; prueba sesión y separación de dos usuarios.

Ejemplo de cambio de tema: reemplaza Pronostico por Producto, selecciones por nombre/categoría, goles por cantidad/precio y estado por Disponible/Agotado. Cambia también reglas, XML, validaciones y agrupaciones de gráficos. No cambies solo las etiquetas visuales.

## 9. Comprobaciones y errores para recordar

| Caso | Qué comprobar |
|---|---|
| Pantalla blanca | Que su Activity infle el Binding y llame a `setContentView`. |
| Error de plugin | No mezclar versiones Android ni poner `apply false` en el módulo app. |
| Guardar crea duplicados al editar | Revisar que el ID llegue y no se ejecute `add()` en edición. |
| Firebase rechaza escritura | Sesión, ruta, reglas publicadas y tipos/estado de campos. |
| Lista duplica filas | Limpiar ambos ArrayList antes de recorrer snapshot. |
| Lista no cambia al volver | Registrar lectura en `onStart`, no solo en `onCreate`. |
| Gráfico conserva valores anteriores | Calcular desde cero al cambiar datos y filtro. |
| Notificación no aparece | Permiso de Android y canal habilitado. |
| Google no abre/completa | Proveedor, SHA del equipo actual, JSON actualizado y cliente OAuth. |
| GitHub no completa | OAuth App, callback exacto y credenciales en Firebase. |
| Facebook no aparece | Activar configuración Gradle después de completar Meta/Firebase. |

Prueba especialmente: edición pendiente sin duplicar ID; pasar a Acertado/Fallado; intentar modificar cerrado; cancelar borrado; dos usuarios; filtro sin resultados; cambios desde consola; cerrar y reabrir la app.

Las pruebas de `ResumenPronosticosTest` comprueban lista vacía, clasificación de estados, equipo en A/B ignorando mayúsculas y recálculo después de cambiar filtro/eliminar registros. Pasaron esas cuatro pruebas y la prueba de la plantilla: **5 pruebas, 0 fallos**. `assembleDebug` compila correctamente y `lintDebug` termina sin errores (con advertencias de estilo/recursos y versiones). También compila la opción adicional `-Pfacebook=true`. No se ha probado la solución final en un emulador ni autenticado proveedores sociales durante esta sesión.

## 10. Referencias para ampliar solo lo necesario

- Ejemplos locales del profesor: `../Clase9-main/Clase9-main` y `../Clase10-main/Clase10-main`.
- [FirebaseUI 9.1.1](https://github.com/firebase/FirebaseUI-Android/blob/9.1.1/auth/README.md).
- [Listeners de Firestore](https://firebase.google.com/docs/firestore/query-data/listen).
- [Escrituras y update](https://firebase.google.com/docs/firestore/manage-data/add-data).
- [Transacciones](https://firebase.google.com/docs/firestore/manage-data/transactions).
- [Condiciones de reglas](https://firebase.google.com/docs/firestore/security/rules-conditions).
- [Google](https://firebase.google.com/docs/auth/android/google-signin), [GitHub](https://firebase.google.com/docs/auth/android/github-auth) y [Facebook](https://firebase.google.com/docs/auth/android/facebook-login).
