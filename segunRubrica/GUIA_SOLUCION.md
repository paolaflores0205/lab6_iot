# Guía de práctica: Mis gastos — ejemplo según la rúbrica

Esta es otra solución, independiente del Mundial, en **Java + XML + View Binding + FirebaseUI 9.1.1 + Cloud Firestore**. Usa el paquete que ya tenía tu proyecto: **`com.example.ola`**. No se cambió el proyecto `soluLab6CicloPasado`.

Empieza por [ENUNCIADO_PRACTICA.md](ENUNCIADO_PRACTICA.md) si quieres intentar resolverlo antes de leer la solución.

## 0. Qué está preparado y qué debes configurar

| Ya implementado | Configuración externa necesaria |
|---|---|
| Logo, interfaz en español, registro/acceso y recuperación. | Crear/registrar app Firebase y habilitar correo/contraseña. |
| Acceso Google con FirebaseUI. | Habilitar Google, registrar firma y descargar JSON actualizado. |
| Acceso Facebook con SDK y FirebaseUI. | Crear app Meta, configurar OAuth/Firebase y App ID/Client Token. |
| CRUD con datos separados por UID. | Crear Cloud Firestore y publicar `firestore.rules`. |
| Lista y estadísticas en tiempo real. | Permisos correctos y conexión a tu Firebase. |
| Barras por monto/categoría y circular por cantidad/estado. | Ninguna librería adicional de gráficos; se dibujan con Canvas. |
| Filtro mensual que afecta ambos gráficos y resumen. | Tener gastos con fechas en uno o varios meses. |

**El proyecto compila sin JSON**, pero en ese estado muestra la pantalla inicial con instrucciones y acceso deshabilitado. No contiene datos simulados ni una conexión al Firebase del ejercicio anterior. Compilar no configura tu base ni tus credenciales sociales.

## 1. Etapa Firebase: registrar la aplicación y agregar el JSON

1. Abre [Firebase Console](https://console.firebase.google.com/).
2. Crea un proyecto para esta práctica o registra otra app Android en un proyecto existente. Para mantener separados tus ejercicios, recomiendo un proyecto de práctica distinto.
3. Registra la app Android con el identificador exacto **`com.example.ola`**. No pongas `segunRubrica`: ese es el nombre de la carpeta, no el `applicationId`.
4. Descarga `google-services.json`.
5. Colócalo en **`segunRubrica/app/google-services.json`**, junto a `app/build.gradle`.
6. Sincroniza Gradle y vuelve a ejecutar. Si copiaste el JSON después de compilar, la aplicación instalada anterior no se actualizará sola.

En `app/build.gradle` ya están las dependencias:

```groovy
implementation platform('com.google.firebase:firebase-bom:35.1.0')
implementation 'com.google.firebase:firebase-auth'
implementation 'com.google.firebase:firebase-firestore'
implementation 'com.firebaseui:firebase-ui-auth:9.1.1'
implementation 'com.facebook.android:facebook-login:18.3.0'
```

El plugin Google Services está declarado en el Gradle raíz y se aplica al módulo **cuando encuentra el JSON**. `firebase_configurado` es un recurso booleano generado al compilar para proteger la pantalla antes de que exista esa configuración. No escribas manualmente `google_app_id` ni un JSON inventado.

View Binding y generación de recursos están activados dentro de `android`:

```groovy
buildFeatures {
    viewBinding true
    resValues true
}
```

Fuente: [configuración oficial Firebase Android](https://firebase.google.com/docs/android/setup).

## 2. Etapa acceso: habilitar correo y contraseña

En Firebase Console → Authentication → Comenzar → Métodos de acceso, habilita **Correo electrónico/contraseña**. No necesitas activar vínculo de acceso por correo.

Ejecuta y pulsa **Acceder o registrarse con correo**. FirebaseUI permite crear una cuenta o ingresar con una existente. La app también tiene un botón propio para recuperar contraseña.

Registra una cuenta de prueba y comprueba que aparezca en Authentication → Usuarios. La navegación te llevará a Mis gastos. Hasta crear Firestore/publicar reglas en la etapa 5, la lectura puede fallar: autenticación y permisos de base son configuraciones distintas.

`MainActivity.java` tiene tres bloques fundamentales:

```java
// Campo de clase: recibe el resultado de FirebaseUI.
private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(
        new FirebaseAuthUIActivityResultContract(), result -> {
            // Si terminó correctamente y existe usuario: entrar().
        });
```

```java
// Dentro de acceder(): prepara y abre la pantalla del proveedor elegido.
Intent intent = AuthUI.getInstance().createSignInIntentBuilder()
        .setAvailableProviders(Collections.singletonList(proveedor))
        .setLogo(R.drawable.logo_gastos)
        .setTheme(R.style.TemaAcceso)
        .build();
launcher.launch(intent);
```

```java
// Dentro de onStart(): si existe sesión, no volvemos a pedir credenciales.
if (FirebaseAuth.getInstance().getCurrentUser() != null) entrar();
```

El código real comprueba primero que exista configuración Firebase. Registro exitoso también inicia sesión. La recuperación usa `sendPasswordResetEmail(correo)`. El idioma de la app se establece con AppCompat; el idioma de correos se establece con `FirebaseAuth.setLanguageCode("es")`.

## 3. Etapa Google: proveedor y firma de la aplicación

1. Authentication → Métodos de acceso → Google → Habilitar y seleccionar correo de soporte.
2. Configuración del proyecto → app Android `com.example.ola` → Agregar huella digital.
3. Agrega la firma debug de esta computadora, obtenida con `./gradlew signingReport`:

```text
SHA-1: 18:D3:41:D3:85:9C:AB:E2:1D:4C:27:E8:82:28:83:81:75:DC:90:48
SHA-256: 2A:30:AA:59:B3:0A:3D:A2:B9:7A:E5:76:4A:9C:AC:73:70:3F:AA:53:35:45:E1:25:08:D0:9F:DF:3F:57:5A:0B
```

4. Descarga nuevamente `google-services.json` y reemplaza el de **esta app**.
5. Sync y Run. Cierra sesión si estabas dentro para ver Acceder con Google.
6. Prueba con dispositivo/emulador con servicios Google.

Si usas otra computadora o firma debes registrar sus huellas. El código comprueba que exista el recurso OAuth `default_web_client_id`; ese recurso lo genera Google Services a partir del JSON actualizado, no debes inventarlo.

Fuente: [Google + Firebase Android](https://firebase.google.com/docs/auth/android/google-signin).

## 4. Etapa Facebook: Meta, Firebase y Gradle

Facebook **sí está incorporado en esta solución**, no como una tarea de código futura. Lo que falta son los identificadores y la configuración real de tus consolas.

1. En [Meta for Developers](https://developers.facebook.com/), crea/configura una app con Facebook Login. Las opciones pueden variar según el tipo de app/caso de uso que muestre Meta.
2. En la integración Android configura:

```text
Package name: com.example.ola
Activity/Class name: com.example.ola.MainActivity
```

3. Registra el **Key Hash** de la firma debug. Meta solicita Base64 del SHA-1 del certificado, no el SHA-1 con dos puntos que pide Firebase. Para esta computadora es:

```text
GNNB04Wcq+IdTCfogiiDgXXckEg=
```

Si cambias de firma, hay que obtener el hash del certificado correspondiente. No confundas Key Hash de Meta, SHA-1 de Firebase y Client Token: tienen funciones diferentes.

4. En Authentication → Métodos de acceso → Facebook, habilita el proveedor y coloca **App ID y App Secret** de Meta.
5. Copia la URI OAuth que muestra Firebase (termina en `/__/auth/handler`) a las URI de redirección válidas de Facebook Login en Meta. Usa la URI exacta de tu proyecto.
6. En Meta obtén el **Client Token**. No es el App Secret.
7. En `gradle.properties` del proyecto, agrega los valores reales:

```properties
facebookAppId=TU_APP_ID_REAL
facebookClientToken=TU_CLIENT_TOKEN_REAL
```

8. Sync y Run. La configuración de Gradle genera estos recursos: `facebook_application_id`, `facebook_login_protocol_scheme` (`fb` + App ID), `facebook_client_token` y `facebook_configurado`.
9. El Manifest contiene el Client Token y la integración FirebaseUI incorpora las Activities y el esquema de retorno. No copies de nuevo sus declaraciones sin revisar el **Merged Manifest**.
10. Si Meta está en modo desarrollo, usa una cuenta con rol autorizado (administrador/desarrollador/tester) o una cuenta de prueba habilitada según el panel de Meta.
11. Cierra sesión y pulsa **Acceder con Facebook** para probar el flujo completo.

El **App Secret** va en Firebase Console, no en Java, XML ni Gradle. El App ID y Client Token son configuración cliente. Hasta completar las propiedades, el botón muestra qué falta configurar; no finge un acceso exitoso.

Fuentes: [Facebook + Firebase](https://firebase.google.com/docs/auth/android/facebook-login), [FirebaseUI 9.1.1](https://github.com/firebase/FirebaseUI-Android/blob/9.1.1/auth/README.md).

## 5. Etapa Firestore: base, estructura y reglas

1. En Firebase Console abre Firestore Database → Crear base de datos.
2. Selecciona Standard, base `(default)` y una ubicación apropiada. La ubicación queda fija después de crearla.
3. Si pide modo de seguridad, empieza con Producción.
4. Abre **`firestore.rules`** de este proyecto. Copia todo su contenido en Firestore → Reglas → **Publicar**.
5. No crees documentos manualmente: los creará el botón Guardar.

La estructura será:

```text
usuarios
  UID_DEL_USUARIO
    gastos
      ID_AUTOMATICO
        descripcion: "Almuerzo"
        categoria: "Alimentación"
        montoCentimos: 1250
        fecha: timestamp
        estado: "Pagado"
```

El documento padre `usuarios/UID` puede no tener campos; eso no impide que exista su subcolección. No necesitas crear un perfil para usar este CRUD.

`Pantallas.gastos()` construye la misma ruta para crear, leer, editar y borrar:

```java
FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
return FirebaseFirestore.getInstance().collection("usuarios")
        .document(user.getUid()).collection("gastos");
```

Las reglas comprueban que el UID autenticado sea el de la ruta. Además validan descripción, categoría, entero positivo para céntimos, fecha timestamp y estado válido. Por ejemplo:

```javascript
function propietario() {
  return request.auth != null && request.auth.uid == uid;
}
allow read, delete: if propietario();
allow create, update: if propietario() && valido();
```

El archivo completo contiene también `valido()`. **El archivo local no se publica automáticamente** y las reglas de otro proyecto no protegen esta base. No se necesita índice compuesto: consultamos la subcolección y filtramos el pequeño conjunto de práctica en memoria.

## 6. Etapa modelo y formulario: qué archivo tocar primero

Clases: `app/src/main/java/com/example/ola/`. Layouts: `app/src/main/res/layout/`.

| Archivo | Función |
|---|---|
| `Gasto.java` | Campos, constructor vacío y getters/setters. |
| `Dinero.java` | Convertir importe escrito a céntimos y mostrarlo. |
| `FormularioGastoActivity.java` | Calendario, validación, creación y edición. |
| `activity_formulario_gasto.xml` | Descripción, importe, categoría, estado, fecha, botones. |

Un archivo Java lleva primero `package`, después imports y después la clase. Las variables de pantalla van en la clase, fuera de los métodos. Los clicks se conectan dentro de `onCreate`, después de inflar Binding. Los métodos como `guardar()` y `cargar()` se colocan fuera de `onCreate`, pero dentro de la clase.

El patrón Binding de esta pantalla es:

```java
// Campo de clase.
private ActivityFormularioGastoBinding binding;

// Dentro de onCreate.
binding = ActivityFormularioGastoBinding.inflate(getLayoutInflater());
setContentView(binding.getRoot());
binding.btnGuardar.setOnClickListener(v -> guardar());
```

El formulario usa un ScrollView para que el teclado no esconda el botón Guardar. Los Spinners usan categorías y estados de `Pantallas.CATEGORIAS` / `Pantallas.ESTADOS`. La fecha se elige con DatePickerDialog y se conserva como `Date`, no como texto.

En esta práctica tanto gastos pendientes como pagados son editables y eliminables. **No trasladamos la regla de “solo Pendiente” del Mundial**, porque la rúbrica no la exige: cada nuevo enunciado define sus restricciones de negocio.

## 7. Etapa C: crear desde el formulario

En `FormularioGastoActivity.guardar()`:

1. Leer descripción e importe.
2. Validar descripción no vacía (máximo 100 caracteres) y fecha elegida.
3. Convertir el monto y rechazar cero, negativos, precisión mayor a dos decimales o exceso del límite.
4. Construir el mapa de cinco campos.
5. Si no existe `gastoId`, usar `add()`.
6. Esperar éxito o error. Durante la escritura se deshabilitan controles para evitar envíos dobles.

```java
Map<String, Object> campos = new HashMap<>();
campos.put("descripcion", descripcion);
campos.put("categoria", binding.spCategoria.getSelectedItem().toString());
campos.put("montoCentimos", monto);
campos.put("fecha", fecha);
campos.put("estado", binding.spEstado.getSelectedItem().toString());

Pantallas.gastos().add(campos)
        .addOnSuccessListener(doc -> exito("Gasto registrado"))
        .addOnFailureListener(this::fallo);
```

`this::fallo` significa llamar a nuestro método `fallo(error)` cuando Firebase entrega el error. No necesitamos un algoritmo de red ni Threads propios para estas llamadas.

### Por qué céntimos

Si escribes `12.50`, guardamos `1250` como entero. Así `0.10 + 0.20` son `10 + 20 = 30` céntimos. `Dinero.leerCentimos()` usa BigDecimal para hacer la conversión exacta, no `double * 100`.

Para entender el CRUD puedes considerar esa conversión una función ya resuelta:

```java
long monto = Dinero.leerCentimos(binding.etMonto.getText().toString());
```

`Dinero.soles(1250)` devuelve `S/ 12.50`. El límite de práctica es S/ 1 000 000 000.00 por registro. En otro tema reemplaza esta función por la validación del tipo que necesites.

## 8. Etapa R: listar y escuchar cambios

`GastosActivity` contiene dos arreglos:

```java
private final ArrayList<Gasto> gastos = new ArrayList<>();
private final ArrayList<String> filas = new ArrayList<>();
```

El primero guarda objetos con todos sus campos e ID; el segundo es lo que dibuja el adaptador:

```java
adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filas);
binding.listaGastos.setAdapter(adapter);
```

Dentro de `onStart()` se comprueba sesión y se registra `addSnapshotListener`. En el callback:

```java
gastos.clear();
filas.clear();
for (QueryDocumentSnapshot doc : snapshot) {
    Gasto g = doc.toObject(Gasto.class);
    g.setId(doc.getId());
    gastos.add(g);
    // Agregar texto con descripción, categoría, fecha, monto y estado.
}
adapter.notifyDataSetChanged();
```

`toObject()` copia campos; `getId()` recupera la identidad de la ruta. `getId()` del modelo tiene `@Exclude` para no serializarla como campo.

En `onStop()` se ejecuta `escucha.remove()` y se pone la variable a null. Al volver del formulario se dispara `onStart()` y recibimos una nueva lectura inicial. No hace falta recargar manualmente ni reiniciar la pantalla.

## 9. Etapa U: seleccionar y actualizar el mismo documento

El ListView usa `setOnItemClickListener`: el parámetro `posicion` permite recuperar `gastos.get(posicion)`. Un botón usa `setOnClickListener` porque ya representa una sola vista.

Al seleccionar Editar:

```java
Intent intent = new Intent(this, FormularioGastoActivity.class);
intent.putExtra("gastoId", elegido.getId());
startActivity(intent);
```

En el formulario:

```java
gastoId = getIntent().getStringExtra("gastoId");
```

Si hay ID, `cargar()` consulta ese documento con `get()` y llena los campos. Si no existe ID, el formulario sirve para crear. Al guardar, las dos rutas comparten validación y mapa:

```java
if (gastoId == null) {
    // Crear: Pantallas.gastos().add(campos)
} else {
    Pantallas.gastos().document(gastoId).update(campos);
}
```

El código real agrega callbacks de éxito/fallo en ambas ramas. **No uses `add()` en edición**, porque crearía otro registro. `update()` falla si el documento ya no existe, en lugar de recrearlo accidentalmente.

## 10. Etapa D: confirmar y eliminar

En `GastosActivity` la opción Eliminar abre un AlertDialog con Cancelar y Eliminar. Solo la confirmación llama a:

```java
Pantallas.gastos().document(gasto.getId()).delete()
        .addOnSuccessListener(v -> mensaje("Gasto eliminado"))
        .addOnFailureListener(e -> mensaje("No se pudo eliminar: " + e.getMessage()));
```

El código final también evita dos borrados simultáneos con `borrando`. La escucha reconstruye las filas y los totales; no hay que restar y borrar manualmente otra vez en el éxito.

## 11. Etapa estadísticas: dos gráficos y filtro

`EstadisticasActivity` tiene su propio listener sobre los gastos del usuario. Crea un Spinner de meses presentes en los documentos, usando `yyyy-MM`, por ejemplo `2026-11`.

Cuando llega un snapshot o cambia el mes, llama a `actualizar()`:

```java
ResumenGastos resumen = ResumenGastos.calcular(gastos, filtro);
```

`ResumenGastos.calcular()` empieza sus contadores en cero y recorre la lista:

1. Si el filtro es un mes concreto y el registro no corresponde, `continue`.
2. Sumar uno a cantidad y añadir importe al total.
3. Añadir importe a la categoría correspondiente.
4. Contar si está Pagado o Pendiente.

El filtro se aplica **antes de cualquier conteo**, por eso ambos gráficos y el resumen son coherentes.

```text
Lista de gastos del usuario
    → filtro de mes
    → un resumen nuevo
        → barras: importe de Alimentación/Transporte/Estudios/Otros
        → circular: cantidad de Pendientes/Pagados
        → textos: cantidad y monto total
```

El código de dibujo está aislado en `GraficoView`. Desde la Activity solo se llama:

```java
binding.graficoBarras.setDatos(Pantallas.CATEGORIAS, montos, false);
binding.graficoCircular.setDatos(
        new String[]{"Pendientes", "Pagados"},
        new double[]{resumen.pendientes, resumen.pagados}, true);
```

Para dibujar convertimos céntimos a soles con `double`; esa conversión es solo visual. Los importes del modelo y las sumas siguen siendo enteros exactos.

Si el total es cero se muestra Sin gastos para este filtro. Si usas una librería de gráficos en tu laboratorio, conserva `ResumenGastos` y reemplaza el dibujo por la API de esa librería. Lo importante para estudiar es leer → filtrar → contar/sumar → actualizar ambos datasets.

## 12. Etapa navegación y sesión

`res/menu/navegacion.xml` define Mis gastos, Estadísticas y Cerrar sesión. Se usa un BottomNavigationView en lista, formulario y estadísticas.

`Pantallas.barra()` conecta las opciones. La clase también centraliza:

- Comprobar sesión antes de entrar a datos.
- Obtener la referencia por UID.
- Cerrar con `AuthUI.signOut()` y limpiar toda la pila usando NEW_TASK/CLEAR_TASK.
- Ajustar márgenes de barras del sistema/teclado para EdgeToEdge.

Estas funciones son de apoyo. La lógica que conviene estudiar está en el formulario, listener, selección por ID y resumen.

## 13. Cómo comprobarlo por etapas

| Prueba | Resultado esperado |
|---|---|
| Sin JSON | Compila y explica que falta conectar Firebase; no intenta autenticar. |
| Correo habilitado + JSON | Permite registro e ingreso; usuario visible en Auth. |
| Cerrar y abrir | Conserva sesión y abre Mis gastos. |
| Crear gasto | Aparece en lista y consola con tipos correctos. |
| Campos vacíos / monto cero / tres decimales | Rechaza sin escribir. |
| Editar | Conserva ID y número de documentos. |
| Cancelar borrado | El documento permanece. |
| Confirmar borrado | Desaparece de consola/lista. |
| Fechas en octubre y noviembre | El filtro mensual cambia ambos gráficos y el total. |
| Cambiar dato desde consola | Lista/estadísticas actualizan sin botón de recarga. |
| Otra cuenta | No ve los gastos de la primera. |
| Recuperación | Procesa solicitud y envía correo cuando corresponde. |
| Google / Facebook | Completan OAuth después de configurar sus credenciales. |

No hay datos precargados para no confundirlos con persistencia real. Puedes crear: Almuerzo S/ 12.50 Alimentación Pagado, Pasaje S/ 3.50 Transporte Pendiente, Copias S/ 8.00 Estudios Pagado; usa fechas en distintos meses.

## 14. Qué repetir si cambia la temática

1. Identificar campos y restricciones del enunciado.
2. Configurar Auth y Firestore.
3. Elegir una ruta por usuario y publicar reglas coherentes.
4. Crear modelo y formulario con Binding.
5. Leer y validar campos; `add()` para crear.
6. Listener → limpiar listas → convertir → conservar ID → actualizar adapter.
7. Pasar ID al formulario; cargar y `update()` para editar.
8. Confirmar y `delete()` para borrar.
9. Filtrar objetos antes de calcular los dos gráficos.
10. Probar dos usuarios y errores antes de mejorar el diseño.

No memorices nombres como `gastoId`: comprende qué dato identifica al documento. En un lab de sensores podría ser `lecturaId`, la categoría podría ser tipo de sensor y el gráfico podría sumar lecturas por dispositivo.

## 15. Alcance y verificación

Este ejemplo cubre los criterios de la rúbrica con otra temática. No añade restricciones de “solo pendiente”, notificaciones ni GitHub del lab anterior, porque no aparecen en esta rúbrica general.

La compilación verifica clases, recursos y dependencias. Las pruebas `GastosTest` comprueban importes exactos, validaciones y coherencia del filtro para ambos gráficos. Los proveedores y Firestore reales requieren completar las etapas 1–5. No se han creado proyectos en tus cuentas, desplegado reglas ni autenticado usuarios sociales automáticamente.

Verificación realizada en este proyecto: `assembleDebug` correcto sin JSON; `testDebugUnitTest` con **9 pruebas y 0 fallos** (8 propias y 1 de la plantilla); `lintDebug` sin errores, con advertencias de estilo/recursos y versiones. Los XML también se comprobaron como documentos válidos. No se probaron visualmente las pantallas en un emulador ni los flujos contra cuentas reales en esta sesión. El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`; debes recompilarlo después de configurar Firebase.

Fuentes complementarias: [modelo de Firestore](https://firebase.google.com/docs/firestore/data-model), [escrituras](https://firebase.google.com/docs/firestore/manage-data/add-data), [listeners](https://firebase.google.com/docs/firestore/query-data/listen), [condiciones de seguridad](https://firebase.google.com/docs/firestore/security/rules-conditions).
