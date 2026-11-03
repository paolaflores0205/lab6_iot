# Configuración externa de la solución

El código está en este proyecto Android. Esta página contiene lo que se configura en Firebase/Google/GitHub/Meta y no queda habilitado por compilar un APK. No se han cambiado tus credenciales ni se han publicado reglas desde esta sesión.

## 1. Lo que ya probaste durante la práctica

- `applicationId`: `com.example.solulab6ciclopasado`.
- Archivo `app/google-services.json` instalado.
- Correo/contraseña habilitado en Authentication.
- Base Cloud Firestore `(default)` creada.
- Registro, acceso, escritura, lectura y eliminación probados por ti antes de completar la solución.

## 2. Publicar las reglas finales (necesario)

Abre `firestore.rules` en la raíz de este proyecto. Copia **todo su contenido** en Firebase Console → Firestore Database → Reglas y pulsa **Publicar**.

El archivo local por sí solo no cambia la base en la nube. Las reglas finales:

- Permiten acceder solo a `usuarios/UID_PROPIO/pronosticos`.
- Exigen selecciones distintas, fecha timestamp, goles enteros no negativos y estado válido.
- Crean documentos únicamente con estado Pendiente.
- Permiten editar/eliminar únicamente si el estado guardado todavía es Pendiente.

No hay que crear índices compuestos: las consultas actuales leen directamente la subcolección del usuario y filtran estadísticas en memoria. No hay que borrar tus datos anteriores.

## 3. Google, pedido tanto en el lab antiguo como en la rúbrica

1. Authentication → Métodos de acceso → Google → Habilitar. Elige el correo de soporte y guarda.
2. Configuración del proyecto → Tus apps → app Android → Agregar huella digital.
3. La firma **debug de esta computadora**, obtenida con `./gradlew signingReport`, es:

```text
SHA-1:
18:D3:41:D3:85:9C:AB:E2:1D:4C:27:E8:82:28:83:81:75:DC:90:48

SHA-256:
2A:30:AA:59:B3:0A:3D:A2:B9:7A:E5:76:4A:9C:AC:73:70:3F:AA:53:35:45:E1:25:08:D0:9F:DF:3F:57:5A:0B
```

4. Registra SHA-1 y SHA-256. Si usas otro equipo o firma, obtén sus propias huellas.
5. Descarga nuevamente `google-services.json` y reemplaza **`app/google-services.json`**.
6. Sincroniza, ejecuta y pulsa **Acceder con Google**. Prueba en un emulador/dispositivo con servicios Google.

El JSON actual se registró para correo y no contiene clientes OAuth en el momento de la revisión. El botón y su flujo están implementados; Google requiere completar estos pasos externos antes de funcionar.

Fuente: [Firebase Authentication con Google](https://firebase.google.com/docs/auth/android/google-signin).

## 4. GitHub, pedido en el enunciado 2026-1

1. En Firebase Console → Authentication → Métodos de acceso → GitHub, abre la configuración y copia la URI de redirección que te muestra Firebase.
2. En tu cuenta GitHub → Settings → Developer settings → OAuth Apps → New OAuth App.
3. Pon un nombre y una Homepage URL válida de tu aplicación/proyecto. En **Authorization callback URL** pega exactamente la URI de Firebase.
4. Obtén **Client ID** y genera el **Client Secret**.
5. Regresa al proveedor GitHub de Firebase, coloca ambas credenciales, habilita y guarda.
6. Ejecuta la app y pulsa **Acceder con GitHub**. Completa el navegador OAuth.

No pegues el Client Secret en Java, XML ni en este Markdown: va en la consola del proveedor en Firebase. `GitHubBuilder` de FirebaseUI construye el flujo; no registra una OAuth App automáticamente.

Fuente: [GitHub en Firebase Android](https://firebase.google.com/docs/auth/android/github-auth).

## 5. Facebook, ampliación para la rúbrica actual

El lab antiguo solicita Google + GitHub. La rúbrica disponible solicita Google + Facebook. El proyecto conserva GitHub y trae Facebook como opción de compilación para que puedas practicar ambos alcances.

Para activar Facebook:

1. Crea/configura una app en Meta for Developers y su producto Facebook Login. Registra paquete Android, Activity y hashes de firma que solicite Meta; para el acceso el paquete es `com.example.solulab6ciclopasado` y la Activity es `com.example.solulab6ciclopasado.MainActivity`.
2. Habilita Facebook en Authentication colocando **App ID y App Secret** de Meta.
3. Agrega la URI OAuth de Firebase en las URI de redirección válidas de Facebook Login en Meta.
4. Obtén el **Client Token** de Meta. Es distinto del App Secret.
5. Agrega estas propiedades reales a `gradle.properties` del proyecto (o a tus propiedades personales de Gradle):

```properties
facebook=true
facebookAppId=TU_APP_ID_REAL
facebookClientToken=TU_CLIENT_TOKEN_REAL
```

6. Sincroniza y ejecuta. Gradle incorpora `facebook-login:18.3.0`, genera sus recursos y activa el botón Facebook. El Manifest incluye el ClientToken; la integración FirebaseUI aporta las Activities y el esquema de retorno de Facebook.
7. En modo desarrollo de Meta, prueba con un usuario habilitado como rol/tester de la aplicación.

Si no configuras esas propiedades, el proyecto compila para el lab antiguo, sin añadir el SDK de Facebook ni mostrar un acceso que todavía no tiene credenciales. No uses los marcadores `TU_...` literalmente. No coloques el **App Secret** en el proyecto Android.

Se comprobó la compilación base y la compilación con `-Pfacebook=true` (SDK 18.3.0). El acceso real con Facebook debe probarse al completar la configuración de Meta; no se ha autenticado ninguna cuenta social desde esta sesión.

Fuentes: [Facebook + Firebase](https://firebase.google.com/docs/auth/android/facebook-login), [FirebaseUI 9.1.1](https://github.com/firebase/FirebaseUI-Android/blob/9.1.1/auth/README.md), [releases oficiales del SDK de Meta](https://github.com/meta/facebook-android-sdk/releases).

## 6. Notificaciones locales

Al abrir el formulario Android solicita el permiso de notificaciones. Acéptalo para recibir los avisos en la bandeja después de registrar/actualizar.

Si lo deniegas, el guardado sigue funcionando y el Toast confirma el resultado, pero Android no mostrará el aviso del sistema. Puedes habilitarlo luego en Ajustes → Aplicaciones → Mis pronósticos → Notificaciones. No se usa FCM: son avisos locales como en Lab5.

## 7. Comprobar la solución en cinco minutos

1. Ejecuta con Android Studio. Si la sesión sigue activa, entrarás directamente a Mis pronósticos.
2. Nuevo pronóstico → completa campos y fecha → Guardar. Debe aparecer en lista y consola con estado Pendiente.
3. Toca una fila → Editar → cambia goles y deja Pendiente → Actualizar. Debe conservar el ID.
4. Edita otra vez → cambia estado a Acertado o Fallado → Actualizar. La fila queda bloqueada.
5. En otra fila pendiente, prueba Cancelar al eliminar y luego confirmar.
6. Abre Estadísticas. Verás total, tres conteos, barras y circular. Cambia el filtro de selección.
7. Modifica un registro propio desde la consola mientras estás en lista/estadísticas: deben actualizarse sin botón de recarga.
8. Cierra sesión y usa otra cuenta: no debe ver los documentos de la primera.
9. Prueba recuperación de contraseña, Google y GitHub después de habilitar sus configuraciones.

## 8. Límites de la verificación

La compilación y las pruebas locales comprueban Java, recursos y cálculo de estadísticas. No sustituyen una prueba conectada a tu cuenta Firebase. No había un emulador conectado al verificar la solución final; no se validaron visualmente las pantallas ni los flujos sociales completos. Las reglas finales están listas para publicar, pero no se han desplegado a tu proyecto.

Resultados: `assembleDebug` correcto; cinco pruebas locales correctas (cuatro de estadísticas y una de la plantilla); `lintDebug` sin errores, con advertencias de estilo/recursos y versiones. La opción Facebook también compila. El APK final se deja en la configuración base del lab antiguo.
