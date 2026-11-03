# Firebase para el Lab6: teoría, autenticación y CRUD paso a paso

Guía de estudio para Android **Java + vistas XML + View Binding**. Preparada con las clases 09.1, 09.2 y 10 de `Clases`, el laboratorio 2026-1 y la rúbrica de `Labs/Lab6`. Consulta de documentación oficial: 3 de noviembre de 2026.

El ejemplo conductor usa pronósticos para que puedas relacionarlo con el laboratorio anterior. **El enunciado de hoy puede usar otra temática.** Aprende el recorrido de los datos y cambia nombres, campos y validaciones. Esta guía contiene ejemplos para montar una práctica; no modifica tu proyecto Android existente.

## Índice

1. [Qué priorizar según la rúbrica](#1-qué-priorizar-según-la-rúbrica)
2. [Teoría esencial](#2-teoría-esencial)
3. [Preparar Firebase y Android desde cero](#3-preparar-firebase-y-android-desde-cero)
4. [Autenticación con correo: archivos completos](#4-autenticación-con-correo-archivos-completos)
5. [Autenticación social y FirebaseUI de la clase](#5-autenticación-social-y-firebaseui-de-la-clase)
6. [Firestore y teoría del CRUD](#6-firestore-y-teoría-del-crud)
7. [CRUD funcional: archivos y código](#7-crud-funcional-archivos-y-código)
8. [Receta para repetir el CRUD en el laboratorio](#8-receta-para-repetir-el-crud-en-el-laboratorio)
9. [Filtros, estadísticas y gráficos](#9-filtros-estadísticas-y-gráficos)
10. [Si exigen Realtime Database](#10-si-exigen-realtime-database)
11. [Plan de 60 a 90 minutos y errores frecuentes](#11-plan-de-60-a-90-minutos-y-errores-frecuentes)
12. [Fuentes y aclaraciones de las diapositivas](#12-fuentes-y-aclaraciones-de-las-diapositivas)

## 1. Qué priorizar según la rúbrica

| Criterio actual | Máximo | Qué preparar |
|---|---:|---|
| Interfaces | 2 | Español, logo, acceso claro a ingreso, registro y recuperación. |
| Inicio de sesión | 5 | Correo/contraseña y proveedores **Google y Facebook**. |
| Firebase Database | 5 | Crear, leer, editar y eliminar con persistencia y consistencia. |
| Gráficos | 4 | Al menos **dos gráficos** con datos almacenados y filtro solicitado. |
| Funcionalidad | 4 | Flujos completos, validaciones y ejecución sin errores. |

**Diferencia importante:** el enunciado 2026-1 pedía Google y **GitHub**; la rúbrica disponible pide Google y **Facebook**. El lab anterior permitía Realtime Database **o Cloud Firestore**. La rúbrica actual dice genéricamente Firebase Database. La guía desarrolla Firestore porque es la base trabajada en clase 09.2; confirma la tecnología en el enunciado de hoy.

El lab anterior también pedía: pronósticos propios, estado inicial Pendiente, edición/eliminación solo si estaban pendientes, confirmación antes de eliminar, estadísticas automáticas y notificaciones de registro/edición. Son referencias para practicar, no una predicción del enunciado nuevo.

## 2. Teoría esencial

### 2.1. Firebase, BaaS y servicios

**BaaS (Backend as a Service):** utilizas servicios ya implementados en la nube en vez de programar un servidor completo. Tu app mantiene la interfaz, la lógica de presentación y las llamadas al SDK. Sigues siendo responsable del modelo de datos, las validaciones y los permisos.

| Servicio | Para qué sirve | Ejemplo |
|---|---|---|
| Authentication | Identificar al usuario | Correo, Google, Facebook; devuelve un UID. |
| Cloud Firestore | Guardar datos como colecciones/documentos | Pronósticos de cada usuario. |
| Realtime Database | Guardar datos en un árbol JSON | Estado compartido que cambia en tiempo real. |
| Cloud Storage | Guardar archivos | Fotos y documentos; no es Firestore. |
| FCM | Enviar mensajes push | Aviso enviado desde un servidor. |
| Hosting | Publicar contenido web | Sitio web. |

Una notificación local después de guardar **no requiere FCM**. Se puede emitir desde Android después de confirmarse la escritura.

### 2.2. Autenticación y autorización son tareas distintas

- **Autenticación:** ¿quién eres? `FirebaseAuth` entrega un `FirebaseUser` y su `uid`.
- **Autorización:** ¿qué puedes leer o modificar? Lo controlan las reglas de Firestore.
- El `google-services.json` identifica el proyecto/app. No inicia sesión ni concede acceso a documentos.
- El UID identifica al usuario en Authentication; el ID de documento identifica un registro en Firestore. Un usuario puede tener muchos documentos con IDs diferentes.
- Registrar una cuenta **no crea automáticamente** un documento de perfil en `usuarios`. Eso se programa si hace falta.
- No guardes contraseñas en Firestore: Authentication gestiona las credenciales.

```text
Pantalla de login
  → Firebase Authentication verifica credenciales
  → FirebaseUser con UID
  → la app consulta usuarios/UID/pronosticos
  → las reglas comprueban que el UID de la ruta sea el del usuario autenticado
  → el listener actualiza lista y estadísticas
```

### 2.3. Asincronía: lo que más errores provoca

Las operaciones de Firebase no entregan su resultado inmediatamente. Devuelven una tarea; el resultado se procesa en un callback.

```java
// Incorrecto como secuencia lógica:
// db.collection("...").get();
// mostrarDatos(); // Aquí todavía podría no haber llegado la respuesta.

// Correcto: procesar dentro del resultado.
db.collection("ejemplo").get()
        .addOnSuccessListener(snapshot -> {
            // Aquí ya tienes la respuesta de esta tarea.
        })
        .addOnFailureListener(e -> {
            // Aquí manejas la falla.
        });
```

Este fragmento solo explica callbacks; la colección real se define más adelante. Para los SDK Android de Auth y Firestore no necesitas envolver cada llamada en un Thread ni usar Retrofit. Evita bloquear la interfaz esperando una tarea.

## 3. Preparar Firebase y Android desde cero

### 3.1. Orden recomendado

1. Crea en Android Studio un proyecto **Empty Views Activity**, lenguaje **Java**. Conserva el tema y las dependencias Android generadas.
2. Revisa el `applicationId` en el archivo Gradle del módulo `app`. Ejemplo de esta guía: `com.example.lab6firebase`.
3. En [Firebase Console](https://console.firebase.google.com/), crea el proyecto usando la cuenta institucional que indica la clase. Analytics no es necesario para este CRUD.
4. Registra una aplicación **Android** con el `applicationId` exacto. Si no coincide, la conexión no queda bien configurada.
5. Descarga `google-services.json` y colócalo en **`app/google-services.json`**, junto al Gradle de `app`; no en `res`, `java` ni en la raíz general.
6. Agrega el plugin Google Services y las dependencias de Auth/Firestore como se muestra abajo. Sincroniza Gradle.
7. En Authentication, habilita **Email/Password** en los proveedores de acceso. Google/Facebook se configuran aparte.
8. En Cloud Firestore, crea la base Standard `(default)`, elige la ubicación y publica las reglas de la sección 7. No basta con agregar la dependencia.
9. Ejecuta la app y prueba una operación real: abrir sin fallar no demuestra que tengas permiso para guardar.

### 3.2. Qué archivo hace qué

```text
ProyectoAndroid/
├── build.gradle.kts                 ← plugins generales; o build.gradle
├── settings.gradle.kts              ← repositorios; o settings.gradle
└── app/
    ├── google-services.json         ← descargado de Firebase
    ├── build.gradle.kts             ← plugins y dependencias de la app
    └── src/main/
        ├── AndroidManifest.xml      ← Internet y Activities
        ├── java/com/example/lab6firebase/
        │   ├── LoginActivity.java
        │   ├── MainActivity.java
        │   └── Pronostico.java
        └── res/
            ├── drawable/logo_lab.xml
            └── layout/
                ├── activity_login.xml
                └── activity_main.xml
```

**Convención de ubicación:** `package` va primero en un archivo Java, después `import`, después `public class`. Las variables de la pantalla van dentro de la clase y fuera de los métodos. Los métodos van dentro de la clase, al mismo nivel que `onCreate()`. Los listeners de botones se conectan en `onCreate()` **después de inflar el binding**. No pegues un método dentro de otro.

### 3.3. Gradle: elige solamente la sintaxis de tu proyecto

Versiones de referencia verificadas en la guía oficial: BoM `35.1.0`, Google Services `4.5.0`. Si el docente entrega un proyecto ya configurado y funcional, conserva sus versiones compatibles. No cambies AGP, Gradle o SDK al azar. [Configuración oficial](https://firebase.google.com/docs/android/setup).

**Si los archivos terminan en `.kts` (Kotlin DSL; tu código Android puede seguir siendo Java):**

En `build.gradle.kts` **de la raíz**, agrega esta línea dentro del `plugins` existente:

```kotlin
id("com.google.gms.google-services") version "4.5.0" apply false
```

En `app/build.gradle.kts`, agrega cada línea en su bloque correspondiente; conserva lo que ya existe:

```kotlin
plugins {
    // Conserva aquí el plugin Android generado, sea id(...) o alias(...).
    id("com.google.gms.google-services")
}

android {
    // Conserva namespace, defaultConfig, compileSdk, etc.
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Conserva AppCompat, Material y demás dependencias de tu proyecto.
    implementation(platform("com.google.firebase:firebase-bom:35.1.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
}
```

**Si terminan en `.gradle` (Groovy):**

En el `plugins` del Gradle **de la raíz**:

```groovy
id 'com.google.gms.google-services' version '4.5.0' apply false
```

En `app/build.gradle`, integra lo siguiente en los bloques existentes:

```groovy
plugins {
    // Conserva el plugin Android existente.
    id 'com.google.gms.google-services'
}
android {
    // Conserva la configuración existente.
    buildFeatures {
        viewBinding true
    }
}
dependencies {
    implementation platform('com.google.firebase:firebase-bom:35.1.0')
    implementation 'com.google.firebase:firebase-auth'
    implementation 'com.google.firebase:firebase-firestore'
}
```

La **BoM coordina versiones**; no instala Auth ni Firestore por sí sola. Al usarla, no agregues versiones individuales a esos dos SDK. FirebaseUI y Facebook son dependencias adicionales, fuera de la BoM.

Si tu proyecto ya usa `alias(libs.plugins.google.services)`, conserva ese alias y su versión en `gradle/libs.versions.toml`: no declares otra vez el mismo plugin con `id`. Los repositorios del proyecto deben incluir `google()` y `mavenCentral()`; normalmente la plantilla ya los trae. Finalmente, **Sync Now**.

### 3.4. Manifest

En `app/src/main/AndroidManifest.xml`, el permiso va dentro de `<manifest>` y fuera de `<application>`. Registra ambas Activities dentro de `<application>`. Conserva los demás atributos de la plantilla.

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

```xml
<!-- Estos elementos van dentro de application. -->
<activity android:name=".MainActivity" android:exported="false" />
<activity android:name=".LoginActivity" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

Deja **un solo launcher**: si `MainActivity` ya tenía ese `intent-filter`, quítalo de allí al moverlo a `LoginActivity`. Internet es un permiso normal; no se solicita con un diálogo en ejecución.

## 4. Autenticación con correo: archivos completos

Esta ruta usa el SDK de Auth y una pantalla propia. Es suficiente para entender registro, ingreso, recuperación y sesión. Después puedes agregar proveedores sociales; no necesitas reemplazar el CRUD.

### 4.1. Logo y XML de login

Crea `app/src/main/res/drawable/logo_lab.xml` con este contenido. Es un logo de práctica; cámbialo por uno representativo del tema que te pidan.

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="64dp" android:height="64dp"
    android:viewportWidth="64" android:viewportHeight="64">
    <path android:fillColor="#1565C0" android:pathData="M4,4h56v56h-56z" />
    <path android:fillColor="#FFFFFF" android:pathData="M16,16h32v8h-24v8h20v8h-20v12h-8z" />
</vector>
```

Crea `app/src/main/res/layout/activity_login.xml`. **Archivo completo:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:fillViewport="true">
    <LinearLayout android:layout_width="match_parent"
        android:layout_height="wrap_content" android:orientation="vertical"
        android:padding="24dp">
        <ImageView android:layout_width="72dp" android:layout_height="72dp"
            android:src="@drawable/logo_lab" android:contentDescription="Logo del laboratorio" />
        <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
            android:text="Mis pronósticos" android:textSize="26sp" />
        <EditText android:id="@+id/etCorreo" android:layout_width="match_parent"
            android:layout_height="wrap_content" android:hint="Correo electrónico"
            android:inputType="textEmailAddress" />
        <EditText android:id="@+id/etClave" android:layout_width="match_parent"
            android:layout_height="wrap_content" android:hint="Contraseña"
            android:inputType="textPassword" />
        <Button android:id="@+id/btnIngresar" android:layout_width="match_parent"
            android:layout_height="wrap_content" android:text="Iniciar sesión" />
        <Button android:id="@+id/btnRegistrar" android:layout_width="match_parent"
            android:layout_height="wrap_content" android:text="Crear cuenta" />
        <Button android:id="@+id/btnRecuperar" android:layout_width="match_parent"
            android:layout_height="wrap_content" android:text="Recuperar contraseña" />
        <TextView android:id="@+id/tvMensaje" android:layout_width="match_parent"
            android:layout_height="wrap_content" />
    </LinearLayout>
</ScrollView>
```

`activity_login.xml` genera `ActivityLoginBinding`; `etCorreo` se usa como `binding.etCorreo`. Los textos directos simplifican la práctica; si el lab pide recursos/internacionalización, pásalos a `res/values/strings.xml`.

### 4.2. LoginActivity.java completo

Crea `LoginActivity.java` en el paquete de tu app. **Cambia la línea `package` y el import del binding si tu paquete es otro.** El destino `MainActivity` se implementa en la sección 7.

```java
package com.example.lab6firebase;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import androidx.appcompat.app.AppCompatActivity;
import com.example.lab6firebase.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        auth = FirebaseAuth.getInstance();
        auth.setLanguageCode("es");

        binding.btnIngresar.setOnClickListener(v -> autenticar(false));
        binding.btnRegistrar.setOnClickListener(v -> autenticar(true));
        binding.btnRecuperar.setOnClickListener(v -> recuperar());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (auth.getCurrentUser() != null) abrirPrincipal();
    }

    private String correo() {
        return binding.etCorreo.getText().toString().trim();
    }

    private boolean validarCorreo() {
        if (!Patterns.EMAIL_ADDRESS.matcher(correo()).matches()) {
            binding.etCorreo.setError("Ingresa un correo válido");
            return false;
        }
        return true;
    }

    private void ocupado(boolean valor) {
        binding.btnIngresar.setEnabled(!valor);
        binding.btnRegistrar.setEnabled(!valor);
        binding.btnRecuperar.setEnabled(!valor);
    }

    private void autenticar(boolean registro) {
        if (!validarCorreo()) return;
        // No usar trim() en contraseñas: los espacios pueden ser parte de ellas.
        String clave = binding.etClave.getText().toString();
        if (clave.isEmpty() || (registro && clave.length() < 6)) {
            binding.etClave.setError(registro
                    ? "Usa al menos 6 caracteres y cumple la política del proyecto"
                    : "Ingresa tu contraseña");
            return;
        }
        ocupado(true);
        if (registro) {
            auth.createUserWithEmailAndPassword(correo(), clave)
                    .addOnCompleteListener(task -> {
                        ocupado(false);
                        if (task.isSuccessful()) {
                            abrirPrincipal();
                        } else {
                            Log.e("AUTH", "Registro falló", task.getException());
                            binding.tvMensaje.setText(
                                    "No se pudo crear la cuenta. Revisa los datos y la política de contraseña.");
                        }
                    });
        } else {
            auth.signInWithEmailAndPassword(correo(), clave)
                    .addOnCompleteListener(task -> {
                        ocupado(false);
                        if (task.isSuccessful()) {
                            abrirPrincipal();
                        } else {
                            Log.e("AUTH", "Ingreso falló", task.getException());
                            binding.tvMensaje.setText(
                                    "No se pudo ingresar. Revisa tus credenciales y conexión.");
                        }
                    });
        }
    }

    private void recuperar() {
        if (!validarCorreo()) return;
        ocupado(true);
        auth.sendPasswordResetEmail(correo()).addOnCompleteListener(task -> {
            ocupado(false);
            if (task.isSuccessful()) {
                binding.tvMensaje.setText(
                        "Si corresponde a una cuenta, recibirás un correo de recuperación. Revisa spam.");
            } else {
                Log.e("AUTH", "Recuperación falló", task.getException());
                binding.tvMensaje.setText("No se pudo procesar la solicitud. Intenta nuevamente.");
            }
        });
    }

    private void abrirPrincipal() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
```

El mínimo local de 6 caracteres no reemplaza la política de contraseña que configures en Firebase: el servidor puede pedir más. `createUserWithEmailAndPassword()` registra **y deja iniciada la sesión**. `signInWithEmailAndPassword()` solo ingresa a una cuenta existente. La sesión se conserva al cerrar y reabrir la app; compruébala con `getCurrentUser()`.

Fuentes: [Auth por contraseña](https://firebase.google.com/docs/auth/android/password-auth), [gestión de usuarios y recuperación](https://firebase.google.com/docs/auth/android/manage-users).

### 4.3. Verificación de correo, si la solicitan

Registro no equivale a correo verificado. Para enviar el correo, coloca este bloque dentro del éxito del registro, antes de navegar, si el enunciado lo requiere:

```java
if (auth.getCurrentUser() != null) {
    auth.getCurrentUser().sendEmailVerification()
            .addOnSuccessListener(unused ->
                    binding.tvMensaje.setText("Correo de verificación enviado"))
            .addOnFailureListener(e ->
                    binding.tvMensaje.setText("No se pudo enviar la verificación"));
}
```

Para comprobar después de pulsar el enlace del correo, agrega un botón y ejecuta esto en su click, dentro de `LoginActivity`:

```java
if (auth.getCurrentUser() != null) {
    auth.getCurrentUser().reload().addOnSuccessListener(unused -> {
        if (auth.getCurrentUser() != null
                && auth.getCurrentUser().isEmailVerified()) {
            // Si las reglas comprueban email_verified, renueva también el token.
            auth.getCurrentUser().getIdToken(true)
                    .addOnSuccessListener(token -> abrirPrincipal())
                    .addOnFailureListener(e -> binding.tvMensaje.setText("Reintenta la validación"));
        } else {
            binding.tvMensaje.setText("Aún debes verificar tu correo");
        }
    }).addOnFailureListener(e -> binding.tvMensaje.setText("No se pudo actualizar la cuenta"));
}
```

**Si haces obligatoria la verificación**, modifica también el acceso automático de `onStart()` y el éxito del login/registro: no deben navegar sin comprobarla. El ejemplo base permite usuarios no verificados; sus reglas son coherentes con esa decisión.

## 5. Autenticación social y FirebaseUI de la clase

### 5.1. Dos caminos de implementación

| Camino | Tú programas | Ventaja para estudiar |
|---|---|---|
| SDK directo | Pantallas y llamadas Auth; integración de cada proveedor | Entiendes cada operación y controlas toda la interfaz. |
| FirebaseUI | Configuración de proveedores, lanzamiento y resultado | Flujos ya construidos, como en la clase 10. |

El CRUD usa `FirebaseAuth.getInstance().getCurrentUser()` en ambos caminos. El proveedor cambia cómo ingresas, no cómo obtienes el UID después.

**Versiones:** el código `AuthUI`, `IdpConfig` y `FirebaseAuthUIActivityResultContract` de la clase corresponde a la API clásica. La documentación de la rama principal ya muestra una API Compose diferente. Para reproducir esa clase, el siguiente ejemplo fija **FirebaseUI 8.0.2**, y no se presenta como la versión más reciente. No copies APIs clásicas sobre una dependencia de una versión mayor distinta. Esta combinación debe sincronizarse y probarse en tu proyecto antes del laboratorio. [Documentación de la versión clásica](https://github.com/firebase/FirebaseUI-Android/blob/8.0.2/auth/README.md).

### 5.2. Configurar Google antes de agregar el botón

1. Authentication → proveedores → habilita Google y selecciona el correo de soporte.
2. Obtén la huella de la firma con Gradle `signingReport`. Desde la terminal de **la raíz del proyecto Android**, en Windows: `./gradlew signingReport`.
3. En configuración del proyecto Firebase → app Android, registra la **SHA-1** de la variante que ejecutas; añade SHA-256 si la configuración del servicio lo requiere. No confundas la firma debug de otra computadora con la tuya.
4. Descarga nuevamente `google-services.json` tras configurar Google y reemplaza el de `app/`.
5. Sincroniza y prueba en un dispositivo/emulador con los servicios de Google adecuados.

Si lo implementas directamente con SDK actual, el flujo es Credential Manager → token de Google → `GoogleAuthProvider.getCredential(idToken, null)` → `auth.signInWithCredential(...)`. FirebaseUI clásico encapsula su propio flujo. [Guía oficial de Google](https://firebase.google.com/docs/auth/android/google-signin).

### 5.3. Ejemplo opcional FirebaseUI clásico, separado del login manual

En `dependencies` de `app/build.gradle`:

```groovy
implementation 'com.firebaseui:firebase-ui-auth:8.0.2'
```

Equivalente `.kts`: `implementation("com.firebaseui:firebase-ui-auth:8.0.2")`.

La siguiente es una **alternativa completa** para `LoginActivity.java`. Reemplaza la clase manual de la sección 4; no pegues dos clases con el mismo nombre. Reutiliza el XML, emplea el botón Iniciar sesión para abrir el flujo y oculta los campos/botones manuales porque FirebaseUI proporciona esos pasos.

```java
package com.example.lab6firebase;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import com.example.lab6firebase.databinding.ActivityLoginBinding;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.google.firebase.auth.FirebaseAuth;
import java.util.Arrays;

public class LoginActivity extends AppCompatActivity {
    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(), result -> {
                if (result.getResultCode() == RESULT_OK
                        && FirebaseAuth.getInstance().getCurrentUser() != null) {
                    entrar();
                } else {
                    Toast.makeText(this, "Ingreso cancelado o no completado", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityLoginBinding binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        FirebaseAuth.getInstance().setLanguageCode("es");
        binding.etCorreo.setVisibility(View.GONE);
        binding.etClave.setVisibility(View.GONE);
        binding.btnRegistrar.setVisibility(View.GONE);
        binding.btnRecuperar.setVisibility(View.GONE);
        binding.tvMensaje.setText("Ingresa o crea una cuenta desde Iniciar sesión");
        binding.btnIngresar.setOnClickListener(v -> {
            Intent intent = AuthUI.getInstance().createSignInIntentBuilder()
                    .setAvailableProviders(Arrays.asList(
                            new AuthUI.IdpConfig.EmailBuilder().build(),
                            new AuthUI.IdpConfig.GoogleBuilder().build()))
                    .setLogo(R.drawable.logo_lab)
                    .setIsSmartLockEnabled(false)
                    .build();
            launcher.launch(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) entrar();
    }

    private void entrar() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
```

El launcher se registra como campo de la Activity, antes de que esté iniciada. No lo registres recién dentro del click. El logo existe porque lo creaste antes. Para tema propio puedes agregar `.setTheme(R.style.TuTemaExistente)` al builder, usando el nombre real de tu tema; no inventes un recurso que no existe.

**Idioma:** `auth.setLanguageCode("es")` configura los correos de Auth; no fuerza por sí solo todos los textos de las Activities de FirebaseUI. FirebaseUI clásico incluye traducciones y toma el idioma de recursos de Android. Prueba con el dispositivo/app en español y conserva los recursos `es`. Si hay que cambiar textos concretos, revisa sus nombres en los recursos de la versión usada. En el login manual tus etiquetas ya están en español.

### 5.4. Facebook: qué falta además de FacebookBuilder

Habilitar el botón no completa la integración. Antes necesitas configurar una app en Meta, su integración Android y las credenciales del proveedor.

1. En Meta for Developers, crea/configura la app y Facebook Login; registra paquete, Activity y hash de firma requeridos para tu aplicación Android.
2. En Authentication → Facebook, habilita el proveedor y coloca el **App ID y App Secret** obtenidos en Meta.
3. Copia la URI de redirección OAuth que muestra Firebase a las URI permitidas de Facebook Login en Meta.
4. Agrega la dependencia **`com.facebook.android:facebook-login`** con una versión explícita compatible con el SDK/FirebaseUI del proyecto del curso. No uses una versión dinámica `+`. El README clásico muestra una versión antigua de ejemplo, no una garantía de compatibilidad actual.
5. En `res/values/strings.xml`, dentro del `<resources>` existente, define los valores reales:

```xml
<string name="facebook_application_id" translatable="false">TU_APP_ID</string>
<string name="facebook_login_protocol_scheme" translatable="false">fbTU_APP_ID</string>
<string name="facebook_client_token" translatable="false">TU_CLIENT_TOKEN</string>
```

6. Para SDKs que lo requieran, agrega dentro de `<application>` del Manifest:

```xml
<meta-data android:name="com.facebook.sdk.ApplicationId"
    android:value="@string/facebook_application_id" />
<meta-data android:name="com.facebook.sdk.ClientToken"
    android:value="@string/facebook_client_token" />
```

7. Completa las Activities/intent-filters que indique la versión de Facebook elegida, revisando también el **Merged Manifest**. En modo desarrollo, prueba con una cuenta autorizada como rol/tester en Meta.
8. Solo entonces agrega `new AuthUI.IdpConfig.FacebookBuilder().build()` a la lista de proveedores del ejemplo FirebaseUI.

El App Secret se configura en la consola del proveedor/Firebase, **no** se coloca como cadena dentro de Android. Los valores `TU_...` son marcadores que debes sustituir. Este apartado es un checklist de configuración social, no una integración Facebook lista para ejecutar sin credenciales. [Configuración oficial Facebook + Firebase](https://firebase.google.com/docs/auth/android/facebook-login).

### 5.5. GitHub, para practicar el lab antiguo

Si el enunciado pide GitHub, configura una OAuth App de GitHub y su callback con la URI que muestre Firebase, habilita GitHub en Authentication con sus credenciales y agrega `new AuthUI.IdpConfig.GitHubBuilder().build()` al listado clásico. No lo confundas con Facebook en la rúbrica actual.

### 5.6. Cerrar sesión según el camino elegido

- SDK manual: `FirebaseAuth.getInstance().signOut();` y luego volver al login limpiando la pila de navegación.
- Si usas FirebaseUI, en el método de salida de `MainActivity` usa `AuthUI.getInstance().signOut(this)` y navega **en su callback exitoso**. Agrega el import `com.firebase.ui.auth.AuthUI`.

Cerrar sesión no borra documentos de Firestore ni garantiza limpiar toda su caché local. Vacía también la lista visible y retira listeners al abandonar la pantalla. La sección 7 implementa esto para el SDK manual.

## 6. Firestore y teoría del CRUD

### 6.1. Colección → documento → campos

```text
usuarios                            colección
  UID_DE_ANA                        documento de ruta del usuario
    pronosticos                     subcolección
      ID_AUTOMATICO_1               documento de pronóstico
        seleccionA: "Perú"          string
        seleccionB: "Brasil"        string
        fechaPartido: Timestamp     fecha/hora real, no texto de presentación
        golesA: 1                   entero
        golesB: 0                   entero
        estado: "Pendiente"         string
```

La ruta alterna colección/documento. `usuarios/uid/pronosticos` es una colección; `usuarios/uid/pronosticos/id` es un documento. Una subcolección puede existir aunque el documento padre no tenga campos y aparezca como inexistente en la consola: puedes crear el perfil padre si lo necesitas, pero no es obligatorio para este CRUD.

NoSQL no significa que puedas usar tipos inconsistentes: si guardas `golesA` como número, no lo guardes después como texto. Para fechas usa `Date`/`Timestamp`; el formato `dd/MM/yyyy` es para mostrar. Los identificadores como DNI se suelen guardar como texto para conservar ceros iniciales.

### 6.2. Diccionario de operaciones

| Operación | Código sobre la referencia apropiada | Comportamiento |
|---|---|---|
| Crear con ID automático | `coleccion.add(datos)` | Cada invocación crea otro documento. |
| Elegir/generar referencia | `coleccion.document(id)` / `coleccion.document()` | Solo obtiene referencia; aún no escribe. |
| Crear o reemplazar | `documento.set(datos)` | Si existe, reemplaza sus campos. |
| Guardar combinando | `documento.set(datos, SetOptions.merge())` | Conserva otros campos; puede crear el documento. |
| Leer una vez | `documento.get()` / `consulta.get()` | Una respuesta; no escucha cambios posteriores. |
| Escuchar | `consulta.addSnapshotListener(...)` | Snapshot inicial y actualizaciones posteriores. |
| Editar campos | `documento.update("estado", "Acertado")` | Cambia campos; falla si no existe el documento. |
| Borrar documento | `documento.delete()` | Elimina ese documento, no sus subcolecciones. |

Fuentes: [escrituras](https://firebase.google.com/docs/firestore/manage-data/add-data), [borrado](https://firebase.google.com/docs/firestore/manage-data/delete-data), [listeners](https://firebase.google.com/docs/firestore/query-data/listen).

### 6.3. Cuatro ideas que debes recordar

1. **Para editar o borrar necesitas el ID real.** Se obtiene con `doc.getId()`. No uses el número de fila ni el nombre del equipo como si fuera ID.
2. **No uses `add()` para editar.** Crearías un duplicado. Usa `.document(idSeleccionado).update(...)`.
3. **Antes de reconstruir una lista desde un snapshot, límpiala.** Si siempre agregas a la misma lista, acumularás duplicados.
4. **La seguridad está en las reglas.** Ocultar un botón o filtrar una lista en Java no impide que otro cliente intente escribir.

## 7. CRUD funcional: archivos y código

Montaremos una pantalla de práctica con formulario, lista y resumen. Se usa `ListView` para concentrarse en Firebase; si el enunciado exige `RecyclerView`, conserva el acceso a datos y cambia la presentación. Esta pantalla no incorpora todavía barra de navegación, dos gráficos ni notificaciones del sistema: esos requisitos se tratan después.

### 7.1. Primero: reglas coherentes con las rutas

En **Firebase Console → Firestore Database → Rules**, reemplaza las reglas para esta práctica y pulsa **Publicar**. Esto **no va en un archivo Java**. Si ya tienes otras colecciones/proyecto compartido, integra los permisos específicos sin reemplazar reglas ajenas.

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /usuarios/{uid}/pronosticos/{id} {
      function propietario() {
        return request.auth != null && request.auth.uid == uid;
      }
      function datosValidos() {
        let d = request.resource.data;
        return d.keys().hasAll(['seleccionA', 'seleccionB', 'fechaPartido',
                               'golesA', 'golesB', 'estado'])
          && d.keys().hasOnly(['seleccionA', 'seleccionB', 'fechaPartido',
                                'golesA', 'golesB', 'estado'])
          && d.seleccionA is string && d.seleccionA.size() > 0
          && d.seleccionB is string && d.seleccionB.size() > 0
          && d.seleccionA != d.seleccionB
          && d.fechaPartido is timestamp
          && d.golesA is int && d.golesA >= 0
          && d.golesB is int && d.golesB >= 0
          && d.estado in ['Pendiente', 'Acertado', 'Fallado'];
      }
      allow read: if propietario();
      allow create: if propietario() && datosValidos()
                    && request.resource.data.estado == 'Pendiente';
      allow update: if propietario() && resource.data.estado == 'Pendiente'
                    && datosValidos();
      allow delete: if propietario() && resource.data.estado == 'Pendiente';
    }
  }
}
```

`resource.data` es el documento **actual**; `request.resource.data` es cómo quedará después de crear/editar. Al borrar no hay documento nuevo: revisas el actual. Una vez que el estado deja de ser Pendiente, las reglas bloquean otra edición o eliminación, aunque un dispositivo conserve una lista desactualizada.

Las reglas anteriores validan equipos distintos con comparación exacta. La app también compara ignorando mayúsculas; para una app más estricta usa un catálogo de selecciones (Spinner) o guarda códigos normalizados. Un usuario no debe poder evitar la validación cambiando mayúsculas desde otro cliente.

Estas reglas no permiten escribir perfiles en `usuarios/{uid}`: esa operación necesitaría su propio bloque. Las reglas **no filtran resultados**: solo autorizan consultas compatibles con sus condiciones. Aquí consultamos la subcolección de un UID concreto. [Condiciones de seguridad](https://firebase.google.com/docs/firestore/security/rules-conditions).

### 7.2. Modelo Pronostico.java completo

En el mismo paquete que las Activities, crea `Pronostico.java`:

```java
package com.example.lab6firebase;

import com.google.firebase.firestore.Exclude;
import java.util.Date;

public class Pronostico {
    @Exclude public String id;
    public String seleccionA;
    public String seleccionB;
    public Date fechaPartido;
    public Long golesA;
    public Long golesB;
    public String estado;

    // Necesario para que Firestore pueda construir el objeto con toObject().
    public Pronostico() { }
}
```

Aquí usamos campos públicos para reducir código. La alternativa es campos privados con constructor vacío y getters/setters públicos. `Long` representa enteros recuperados de Firestore; `Date` se almacena como timestamp. `@Exclude` evita guardar `id` como campo cuando serializas el objeto; el ID ya forma parte de la ruta.

### 7.3. activity_main.xml completo

Reemplaza el contenido de `app/src/main/res/layout/activity_main.xml`. El formulario es deliberadamente sencillo para practicar las operaciones.

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:padding="16dp">
    <TextView android:id="@+id/tvUsuario" android:layout_width="match_parent"
        android:layout_height="wrap_content" android:text="Usuario" />
    <EditText android:id="@+id/etA" android:layout_width="match_parent"
        android:layout_height="wrap_content" android:hint="Selección A"
        android:inputType="textCapWords" />
    <EditText android:id="@+id/etB" android:layout_width="match_parent"
        android:layout_height="wrap_content" android:hint="Selección B"
        android:inputType="textCapWords" />
    <Button android:id="@+id/btnFecha" android:layout_width="match_parent"
        android:layout_height="wrap_content" android:text="Seleccionar fecha" />
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="horizontal">
        <EditText android:id="@+id/etGolesA" android:layout_width="0dp"
            android:layout_height="wrap_content" android:layout_weight="1"
            android:hint="Goles A" android:inputType="number" />
        <EditText android:id="@+id/etGolesB" android:layout_width="0dp"
            android:layout_height="wrap_content" android:layout_weight="1"
            android:hint="Goles B" android:inputType="number" />
    </LinearLayout>
    <Spinner android:id="@+id/spEstado" android:layout_width="match_parent"
        android:layout_height="48dp" />
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="horizontal">
        <Button android:id="@+id/btnNuevo" android:layout_width="0dp"
            android:layout_height="wrap_content" android:layout_weight="1" android:text="Nuevo" />
        <Button android:id="@+id/btnGuardar" android:layout_width="0dp"
            android:layout_height="wrap_content" android:layout_weight="1" android:text="Guardar" />
        <Button android:id="@+id/btnEliminar" android:layout_width="0dp"
            android:layout_height="wrap_content" android:layout_weight="1" android:text="Eliminar" />
    </LinearLayout>
    <TextView android:id="@+id/tvResumen" android:layout_width="match_parent"
        android:layout_height="wrap_content" android:text="Sin datos" />
    <ListView android:id="@+id/listaPronosticos" android:layout_width="match_parent"
        android:layout_height="0dp" android:layout_weight="1" />
    <Button android:id="@+id/btnSalir" android:layout_width="match_parent"
        android:layout_height="wrap_content" android:text="Cerrar sesión" />
</LinearLayout>
```

En teléfonos pequeños el formulario puede ocupar casi toda la altura; para una entrega final puedes moverlo a otra Activity o diálogo. Si la plantilla usa edge-to-edge y aparece contenido debajo de las barras del sistema, conserva/adapta su manejo de insets al root del nuevo layout; no mantengas referencias a un ID que eliminaste.

### 7.4. MainActivity.java completo

Este archivo implementa: comprobar sesión → construir ruta por UID → escuchar en `onStart` → seleccionar una fila → crear/editar → confirmar borrado → retirar listener en `onStop`.

Reemplaza `MainActivity.java`, adapta paquete/import del binding y conserva exactamente los IDs del XML anterior.

```java
package com.example.lab6firebase;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.lab6firebase.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private FirebaseAuth auth;
    private CollectionReference ref;
    private ListenerRegistration escucha;
    private final ArrayList<Pronostico> datos = new ArrayList<>();
    private final ArrayList<String> filas = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private final String[] estados = {"Pendiente", "Acertado", "Fallado"};
    private final SimpleDateFormat formato =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private Date fechaElegida;
    private String idSeleccionado;
    private String estadoSeleccionado;
    private boolean escribiendo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        auth = FirebaseAuth.getInstance();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filas);
        binding.listaPronosticos.setAdapter(adapter);
        ArrayAdapter<String> estadosAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, estados);
        estadosAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spEstado.setAdapter(estadosAdapter);

        binding.btnFecha.setOnClickListener(v -> elegirFecha());
        binding.btnNuevo.setOnClickListener(v -> limpiarFormulario());
        binding.btnGuardar.setOnClickListener(v -> guardar());
        binding.btnEliminar.setOnClickListener(v -> confirmarEliminar());
        binding.btnSalir.setOnClickListener(v -> cerrarSesion());
        binding.listaPronosticos.setOnItemClickListener((parent, view, posicion, id) -> {
            if (!escribiendo) seleccionar(datos.get(posicion));
        });
        limpiarFormulario();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            volverLogin();
            return;
        }
        binding.tvUsuario.setText("Usuario: " + user.getEmail());
        ref = FirebaseFirestore.getInstance().collection("usuarios")
                .document(user.getUid()).collection("pronosticos");
        escuchar();
    }

    @Override
    protected void onStop() {
        detenerEscucha();
        super.onStop();
    }

    private void detenerEscucha() {
        if (escucha != null) {
            escucha.remove();
            escucha = null;
        }
    }

    private void escuchar() {
        detenerEscucha();
        escucha = ref.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                error("No se pudo leer", error);
                return;
            }
            if (snapshot == null) return;
            datos.clear();
            filas.clear();
            int pendientes = 0, acertados = 0, fallados = 0;
            for (QueryDocumentSnapshot doc : snapshot) {
                Pronostico p = doc.toObject(Pronostico.class);
                p.id = doc.getId();
                datos.add(p);
                String fecha = p.fechaPartido == null ? "Sin fecha" : formato.format(p.fechaPartido);
                filas.add(p.seleccionA + " - " + p.seleccionB + "\n"
                        + fecha + " | " + p.golesA + ":" + p.golesB + " | " + p.estado);
                if ("Pendiente".equals(p.estado)) pendientes++;
                if ("Acertado".equals(p.estado)) acertados++;
                if ("Fallado".equals(p.estado)) fallados++;
            }
            adapter.notifyDataSetChanged();
            binding.tvResumen.setText("Total: " + datos.size() + " | Pendientes: " + pendientes
                    + " | Acertados: " + acertados + " | Fallados: " + fallados);
        });
    }

    private void elegirFecha() {
        Calendar cal = Calendar.getInstance();
        if (fechaElegida != null) cal.setTime(fechaElegida);
        new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar elegida = Calendar.getInstance();
            elegida.clear();
            elegida.set(year, month, day); // month ya viene entre 0 y 11.
            fechaElegida = elegida.getTime();
            binding.btnFecha.setText(formato.format(fechaElegida));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void seleccionar(Pronostico p) {
        idSeleccionado = p.id;
        estadoSeleccionado = p.estado;
        fechaElegida = p.fechaPartido;
        binding.etA.setText(p.seleccionA);
        binding.etB.setText(p.seleccionB);
        binding.etGolesA.setText(p.golesA == null ? "" : String.valueOf(p.golesA));
        binding.etGolesB.setText(p.golesB == null ? "" : String.valueOf(p.golesB));
        binding.btnFecha.setText(fechaElegida == null ? "Seleccionar fecha" : formato.format(fechaElegida));
        for (int i = 0; i < estados.length; i++) {
            if (estados[i].equals(p.estado)) binding.spEstado.setSelection(i);
        }
        boolean pendiente = "Pendiente".equals(p.estado);
        binding.btnGuardar.setText("Actualizar");
        binding.btnGuardar.setEnabled(pendiente);
        binding.btnEliminar.setEnabled(pendiente);
        binding.spEstado.setEnabled(pendiente);
        if (!pendiente) mensaje("Este pronóstico ya está cerrado");
    }

    private void limpiarFormulario() {
        idSeleccionado = null;
        estadoSeleccionado = null;
        fechaElegida = null;
        binding.etA.setText("");
        binding.etB.setText("");
        binding.etGolesA.setText("");
        binding.etGolesB.setText("");
        binding.btnFecha.setText("Seleccionar fecha");
        binding.spEstado.setSelection(0);
        binding.spEstado.setEnabled(false); // Al crear, estado automático.
        binding.btnGuardar.setText("Guardar nuevo");
        binding.btnGuardar.setEnabled(true);
        binding.btnEliminar.setEnabled(false);
    }

    private Map<String, Object> leerFormulario() {
        String a = binding.etA.getText().toString().trim();
        String b = binding.etB.getText().toString().trim();
        if (a.isEmpty() || b.isEmpty() || a.equalsIgnoreCase(b)) {
            mensaje("Ingresa dos selecciones diferentes");
            return null;
        }
        if (fechaElegida == null) {
            mensaje("Selecciona la fecha");
            return null;
        }
        long ga, gb;
        try {
            ga = Long.parseLong(binding.etGolesA.getText().toString().trim());
            gb = Long.parseLong(binding.etGolesB.getText().toString().trim());
            if (ga < 0 || gb < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            mensaje("Los goles deben ser enteros válidos, mayores o iguales a cero");
            return null;
        }
        Map<String, Object> mapa = new HashMap<>();
        mapa.put("seleccionA", a);
        mapa.put("seleccionB", b);
        mapa.put("fechaPartido", fechaElegida);
        mapa.put("golesA", ga);
        mapa.put("golesB", gb);
        mapa.put("estado", idSeleccionado == null
                ? "Pendiente" : binding.spEstado.getSelectedItem().toString());
        return mapa;
    }

    private void ocupado(boolean valor) {
        escribiendo = valor;
        binding.btnNuevo.setEnabled(!valor);
        binding.btnSalir.setEnabled(!valor);
        boolean editable = idSeleccionado == null || "Pendiente".equals(estadoSeleccionado);
        binding.btnGuardar.setEnabled(!valor && editable);
        binding.btnEliminar.setEnabled(!valor && idSeleccionado != null && editable);
    }

    private void guardar() {
        if (escribiendo || ref == null || auth.getCurrentUser() == null) return;
        if (idSeleccionado != null && !"Pendiente".equals(estadoSeleccionado)) {
            mensaje("Solo puedes editar pendientes");
            return;
        }
        Map<String, Object> mapa = leerFormulario();
        if (mapa == null) return;
        ocupado(true);
        if (idSeleccionado == null) {
            ref.add(mapa).addOnSuccessListener(doc -> {
                ocupado(false);
                limpiarFormulario();
                mensaje("Pronóstico registrado");
            }).addOnFailureListener(e -> {
                ocupado(false);
                error("No se pudo registrar", e);
            });
        } else {
            String idEditar = idSeleccionado;
            ref.document(idEditar).update(mapa).addOnSuccessListener(unused -> {
                ocupado(false);
                limpiarFormulario();
                mensaje("Pronóstico actualizado");
            }).addOnFailureListener(e -> {
                ocupado(false);
                error("No se pudo actualizar", e);
            });
        }
    }

    private void confirmarEliminar() {
        if (escribiendo || ref == null || idSeleccionado == null) return;
        if (!"Pendiente".equals(estadoSeleccionado)) {
            mensaje("Solo puedes eliminar pendientes");
            return;
        }
        String idBorrar = idSeleccionado;
        new AlertDialog.Builder(this)
                .setTitle("Eliminar pronóstico")
                .setMessage("¿Deseas eliminar este registro? Esta acción no se puede deshacer.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    ocupado(true);
                    ref.document(idBorrar).delete().addOnSuccessListener(unused -> {
                        ocupado(false);
                        limpiarFormulario();
                        mensaje("Pronóstico eliminado");
                    }).addOnFailureListener(e -> {
                        ocupado(false);
                        error("No se pudo eliminar", e);
                    });
                }).show();
    }

    private void cerrarSesion() {
        detenerEscucha();
        datos.clear();
        filas.clear();
        adapter.notifyDataSetChanged();
        auth.signOut();
        volverLogin();
    }

    private void volverLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void mensaje(String texto) {
        Toast.makeText(this, texto, Toast.LENGTH_SHORT).show();
    }

    private void error(String texto, Exception e) {
        Log.e("FIRESTORE", texto, e);
        mensaje(texto + ". Revisa conexión, reglas y Logcat.");
    }
}
```

**Cómo usar la pantalla:** completa campos y fecha → Guardar nuevo. Toca una fila → el formulario se llena → modifica y pulsa Actualizar. Para crear otro registro pulsa Nuevo antes de completar el formulario. Eliminar abre confirmación y borra el ID seleccionado. El resumen y la lista se recalculan desde el listener; no se vuelven a incrementar manualmente en el callback de escritura.

**Qué garantiza y qué debes adaptar:** las reglas rechazan cambios sobre pronósticos cerrados incluso si la pantalla conserva un estado antiguo. Este ejemplo no conserva el borrador durante una rotación de pantalla; si lo necesitan, guarda el estado o usa un ViewModel. Los tipos/campos deben coincidir con el modelo: documentos previos de otra práctica pueden necesitar migración o una colección distinta.

Un `Toast` informa en pantalla, pero **no sustituye una notificación Android** si el enunciado exige notificación en la bandeja. En ese caso llama a tu función de notificación desde el éxito de `add()`/`update()`, crea su canal desde Android 8 y solicita `POST_NOTIFICATIONS` desde Android 13 según corresponda. Puedes reutilizar lo visto en Lab5. No anuncies éxito antes de confirmar la escritura.

### 7.5. Lectura única y actualización puntual: variantes pequeñas

Los siguientes métodos son alternativas didácticas. Se colocan **dentro de `MainActivity`, fuera de `onCreate()`**, y se invocan después de inicializar `ref` y con un ID real. No los necesitas para ejecutar el archivo completo anterior.

```java
private void leerUnaVez(String id) {
    ref.document(id).get().addOnSuccessListener(doc -> {
        if (!doc.exists()) {
            mensaje("No existe ese pronóstico");
            return;
        }
        Pronostico p = doc.toObject(Pronostico.class);
        if (p != null) {
            p.id = doc.getId();
            seleccionar(p);
        }
    }).addOnFailureListener(e -> error("Lectura fallida", e));
}

private void marcarAcertado(String id) {
    ref.document(id).update("estado", "Acertado")
            .addOnSuccessListener(unused -> mensaje("Estado actualizado"))
            .addOnFailureListener(e -> error("Cambio rechazado", e));
}
```

Si usas una lista con RecyclerView, el adapter debe avisar qué **objeto/ID** se seleccionó, por ejemplo mediante una interfaz de callback. La Activity conserva ese ID y ejecuta el mismo `.document(id).update(...)` o `.delete()`. No guardes un `position` como identidad permanente: las posiciones cambian cuando cambia la lista.

## 8. Receta para repetir el CRUD en el laboratorio

Esta es la secuencia que conviene repetir **después de entender** la sección 7.

### Paso 1. Traducir el enunciado a datos

En un papel identifica: entidad, campos, tipos, propietario y restricciones. Ejemplo:

```text
Entidad: pronóstico
Campos: seleccionA String, seleccionB String, fecha Date,
        golesA entero, golesB entero, estado String
Propietario: usuario autenticado (UID)
Restricciones: equipos distintos; goles >= 0; nuevo siempre Pendiente;
               editar/borrar solo pendientes.
```

Si fuera un inventario: `producto`, `nombre`, `cantidad`, `categoria`. El patrón CRUD sigue igual; las reglas, el modelo y el formulario deben cambiar juntos.

### Paso 2. Preparar conexión y sesión antes del formulario

- Verifica JSON en `app/`, plugin, BoM, SDK, Sync y permiso Internet.
- Habilita el proveedor de acceso y crea la base.
- Inicia sesión y comprueba que `getCurrentUser()` no sea null.
- Obtén `user.getUid()`; nunca llames `getUid()` sobre un usuario null.

### Paso 3. Elegir una única ruta

Escribe la ruta que usarás para todas las operaciones:

```java
ref = FirebaseFirestore.getInstance().collection("usuarios")
        .document(user.getUid()).collection("pronosticos");
```

Este bloque va en el inicio de la pantalla autenticada, después de comprobar `user`. No escribas en `pronosticos` global y después intentes leer `usuarios/uid/pronosticos`: son lugares distintos.

### Paso 4. Publicar reglas para esa ruta

Empieza por comprobar propietario y añade las restricciones del enunciado. Publica y revisa permisos. Si cambia el nombre de la colección en Java, cambia el bloque `match` correspondiente.

### Paso 5. Crear el modelo y el formulario

- Modelo Java con tipos correctos y constructor vacío.
- XML con IDs claros.
- Binding activado; infla antes de usar sus vistas.
- Campos de clase para referencia, lista, listener e `idSeleccionado`.

### Paso 6. Implementar C: crear

Click Guardar → leer formulario → validar → construir objeto/mapa → `ref.add(datos)` → manejar éxito/fallo. Comprueba el documento en la consola con sus tipos. Desactiva el botón durante la escritura para evitar envíos dobles.

### Paso 7. Implementar R: leer

En `onStart`, adjunta **un** listener sobre `ref`. Dentro: error/null → limpiar listas → convertir documentos → conservar `doc.getId()` → refrescar adapter y resumen. En `onStop`, `remove()`. Prueba cambiando un campo desde Firebase Console y observa la app.

### Paso 8. Implementar U: actualizar

Al tocar fila, conserva ID y carga campos en formulario. Verifica si se permite editar. El botón usa `ref.document(id).update(mapa)` y el callback maneja resultado. Si no hay ID, estás creando; si hay ID, estás editando. Usa la misma validación de campos que en creación y reglas específicas para transiciones.

### Paso 9. Implementar D: borrar

Selecciona un documento permitido → diálogo de confirmación → guardar ID en variable local → `ref.document(id).delete()` → manejar resultado. No elimines únicamente la fila del adapter: debe desaparecer de Firebase. El listener reconstruirá la lista.

### Paso 10. Añadir filtros y gráficos

Mantén una lista completa, deriva la lista filtrada y calcula ambos gráficos con ese mismo subconjunto. Si el filtro se hace en Firebase, retira el listener anterior antes de registrar el nuevo.

### Paso 11. Prueba mínima antes de continuar

| Prueba | Resultado esperado |
|---|---|
| Crear dos registros | Dos IDs diferentes y datos en consola. |
| Reabrir app | Sesión y documentos se recuperan. |
| Editar uno | Cambia ese ID; no aparece un tercero. |
| Cancelar eliminación | El documento permanece. |
| Confirmar eliminación | Desaparece de consola y lista. |
| Cerrar un registro | Se bloquea su edición y borrado. |
| Ingresar con usuario B | No ve los documentos de A. |
| Cambiar dato en consola | La app actualiza lista/resumen. |
| Campo vacío, goles inválidos | No escribe y muestra un mensaje. |

## 9. Filtros, estadísticas y gráficos

### 9.1. Filtrar en Firebase o en memoria

Para pocas filas de una práctica, puedes leer los documentos del usuario y filtrar esa lista en Java. Si necesitas que Firebase devuelva solo un subconjunto, usa una consulta. Ejemplo **alternativo al listener sin filtro**, dentro de un método de `MainActivity`, con `ref` inicializada:

```java
detenerEscucha();
escucha = ref.whereEqualTo("estado", "Pendiente")
        .addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                error("Filtro falló", error);
                return;
            }
            if (snapshot == null) return;
            // Reconstruye aquí la lista como en escuchar(), usando este snapshot.
        });
```

Este fragmento muestra **solo el cambio de consulta**; debes reutilizar el procesamiento del snapshot de `escuchar()` para mostrar resultados. Si combinas filtros y orden, puede requerirse un índice compuesto; Logcat devuelve un enlace para crearlo. `orderBy("fechaPartido")` solo incluye documentos que tengan ese campo.

Con la ruta alternativa global `pronosticos/{id}`, normalmente guardarías `usuarioId` y consultarías con `.whereEqualTo("usuarioId", uid)`; también necesitarías reglas basadas en ese campo. No mezcles esa estrategia con las reglas por subcolección de esta guía.

### 9.2. Cálculo de estadísticas

El ejemplo completo ya cuenta los estados en cada snapshot. Para gráficos con filtro usa este proceso:

```text
snapshot nuevo o cambio de filtro
  → guardar lista completa
  → aplicar filtro elegido
  → contar categorías desde cero
  → reemplazar datos de gráfico 1 y gráfico 2
  → solicitar redibujado
```

Ejemplo de método adicional dentro de `MainActivity`, si necesitas obtener los conteos en un arreglo:

```java
private int[] contarEstados(ArrayList<Pronostico> fuente) {
    int[] conteos = new int[3]; // Pendiente, Acertado, Fallado.
    for (Pronostico p : fuente) {
        if ("Pendiente".equals(p.estado)) conteos[0]++;
        else if ("Acertado".equals(p.estado)) conteos[1]++;
        else if ("Fallado".equals(p.estado)) conteos[2]++;
    }
    return conteos;
}
```

Para un circular: `porcentaje = total == 0 ? 0 : 100f * cantidad / total`. Para barras: una categoría y su conteo. El número de filas cambia también al eliminar: vuelve a calcular, no solo al crear/editar.

### 9.3. Cómo llevarlo a los dos gráficos de la rúbrica

Una opción de práctica es **barras por estado** y **circular por estado**, ambos vinculados a un filtro por selección/equipo. El dato concreto y la presentación los define el enunciado. Utiliza la librería de gráficos que te hayan proporcionado o una View que dibuje los datos; Firebase entrega documentos, no gráficos.

Orden de implementación:

1. Agrega las dos vistas de gráfico al XML de estadísticas y sus dependencias, si corresponden.
2. Crea una función `actualizarEstadisticas(listaFiltrada)`.
3. Dentro, calcula los conteos y sustituye el dataset de ambas vistas; no acumules valores sobre el dataset anterior.
4. Invócala al recibir un snapshot y cuando cambie el Spinner de filtro.
5. Muestra estado vacío cuando no haya datos; evita dividir por cero.
6. Comprueba que otro usuario no se mezcle en los conteos.

**Un TextView con totales, como `tvResumen` del ejemplo, no cumple por sí solo el criterio de dos gráficos.** Este apartado explica cómo alimentar los gráficos; no incluye una librería concreta que quizá no sea la del curso.

Si separas lista y estadísticas en Fragments, usa un ViewModel compartido o un acceso a datos común. Para una práctica pequeña también puedes alternar dos contenedores de una misma Activity. La navegación debe ofrecer las opciones que pida el nuevo enunciado.

## 10. Si exigen Realtime Database

Es otro SDK y otro modelo. No se programan nodos de Realtime Database con `collection()` y `document()`. Para esta alternativa agrega `implementation("com.google.firebase:firebase-database")` en `.kts` o `implementation 'com.google.firebase:firebase-database'` en Groovy, manteniendo la BoM. Crea **Realtime Database** en consola y revisa sus propias reglas.

| Firestore | Realtime Database |
|---|---|
| `FirebaseFirestore` | `FirebaseDatabase` |
| Colecciones y documentos | Nodos de árbol JSON |
| `collection(...).document(...)` | `getReference(...).child(...)` |
| `add()` | `push()` + `setValue()` |
| `update()` | `updateChildren()` |
| `delete()` | `removeValue()` |
| `addSnapshotListener()` | `addValueEventListener()` |

Ejemplo **independiente**, no para pegar sin cambios en la MainActivity de Firestore. Imports arriba del archivo Java:

```java
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
```

Dentro de una Activity autenticada, después de obtener un UID válido, este patrón usa un mapa de datos compatible con JSON. En RTDB guarda la fecha como milisegundos (`long`), no asumas que se serializará como Timestamp de Firestore:

```java
DatabaseReference nodo = FirebaseDatabase.getInstance()
        .getReference("usuarios").child(uid).child("pronosticos");
String id = nodo.push().getKey();
if (id != null) {
    Map<String, Object> campos = new HashMap<>();
    campos.put("seleccionA", "Perú");
    campos.put("seleccionB", "Brasil");
    campos.put("fechaPartido", System.currentTimeMillis());
    campos.put("golesA", 1L);
    campos.put("golesB", 0L);
    campos.put("estado", "Pendiente");
    nodo.child(id).setValue(campos)
            .addOnSuccessListener(unused -> { /* creación confirmada */ })
            .addOnFailureListener(e -> { /* mostrar error */ });
    // Para editar y borrar posteriormente el MISMO ID:
    // nodo.child(id).updateChildren(cambios);
    // nodo.child(id).removeValue();
}
```

`uid` debe ser una variable ya obtenida desde Auth; `Map`/`HashMap` requieren sus imports. Los comentarios indican dónde manejar UI, no constituyen una pantalla RTDB completa. Si la instancia por defecto no resuelve la base creada, usa `FirebaseDatabase.getInstance("URL_REAL_DE_TU_BASE")`, copiando la URL exacta de la consola.

Para escucha continua se usa un campo `ValueEventListener` y se retira con `nodo.removeEventListener(listener)`. Las reglas de Firestore **no protegen RTDB**; deben definirse en su propia pestaña de Rules. [Lectura y escritura en RTDB](https://firebase.google.com/docs/database/android/read-and-write).

## 11. Plan de 60 a 90 minutos y errores frecuentes

### 11.1. Distribución del tiempo

| Tiempo aproximado | Objetivo verificable |
|---|---|
| 0–8 min | Leer todo, identificar campos/restricciones y crear mapa de archivos. |
| 8–18 min | Configuración Firebase/Gradle; app compila; proveedores/base listos. |
| 18–30 min | Ingreso, registro, recuperación y sesión; probar UID. |
| 30–50 min | Modelo, creación y lista en tiempo real, editar y borrar. |
| 50–65 min | Filtro y gráficos, navegación solicitada. |
| 65–78 min | Completar proveedores sociales y detalles pedidos. |
| 78–90 min | Probar dos cuentas, errores, persistencia y entrega. |

Este cronograma es orientativo para 90 minutos. Para 60 minutos reduce el diseño visual y mantén ciclos cortos de prueba. **La configuración social conviene ensayarla antes**, porque depende de consolas, firmas y credenciales; no es solo pegar un botón al final. No hay garantía de que una implementación parcial obtenga cierto puntaje.

### 11.2. Tabla de diagnóstico

| Síntoma | Revisión concreta |
|---|---|
| `No matching client found for package name` | `applicationId` y app registrada en el JSON deben coincidir. |
| No existe `Activity...Binding` | Activa View Binding, Sync, revisa XML y nombre del layout. |
| `R.id...` o `binding...` no existe | ID mal escrito o ejemplo pegado con otro XML. |
| Provider disabled / operation not allowed | Habilita el proveedor correcto en Authentication. |
| `PERMISSION_DENIED` | Sesión, UID, ruta, reglas publicadas, estado y tipos de campos. |
| Login funciona pero no guarda | Auth y Firestore tienen configuración/permisos separados. |
| Lista vacía | Revisa proyecto, colección exacta, UID y errores del listener. |
| Se duplica cada fila al cambiar un dato | Limpia listas antes de recorrer cada snapshot. |
| Editar crea otro registro | Estás usando `add()` o perdiste `idSeleccionado`. |
| Borrar quita la fila pero vuelve | Solo borraste en memoria; usa `document(id).delete()`. |
| Error al convertir `toObject()` | Constructor vacío, tipos y nombres del modelo no coinciden. |
| Google falla por configuración (p. ej. error 10 en flujos clásicos) | SHA de firma, paquete, cliente OAuth y JSON actualizado. |
| Recuperación no llega | Correo, spam, plantilla y configuración del proyecto. |
| Listener se ejecuta varias veces | Registraste más de uno; retira antes de volver a escuchar. |
| Índice requerido | Crea el índice que enlaza el error de Firestore y espera a que esté listo. |
| Gráfico usa datos anteriores | Recalcula al cambiar datos **y** filtro; reemplaza datasets. |

Firestore en Android puede servir datos en caché y reflejar escrituras locales antes de la confirmación remota. Para demostrar persistencia, comprueba el servidor en consola con conexión; un cambio visual inmediato por sí solo no prueba que se aceptó. Con red ausente, una escritura puede quedar pendiente y el ejemplo mantiene los botones ocupados hasta recibir resultado.

### 11.3. Chuleta final

```text
Conectar  = app/google-services.json + plugin + BoM + SDK + Sync
Entrar    = habilitar proveedor + Auth + callback + comprobar user
Propietario = user.getUid() + ruta/reglas coherentes
Crear     = ref.add(datos)
Leer      = ref.get() o ref.addSnapshotListener(...)
Identidad = doc.getId(), conservarlo al mostrar la fila
Editar    = ref.document(id).update(campos)
Borrar    = confirmar + ref.document(id).delete()
Tiempo real = limpiar listas + reconstruir + refrescar adapter/gráficos
Salir     = retirar listener + vaciar UI + signOut + limpiar navegación
```

## 12. Fuentes y aclaraciones de las diapositivas

### Material local utilizado

- `../../Clases/Clase 09.1 - Firebase_25años.pdf`: BaaS y servicios Firebase.
- `../../Clases/Clase 09.2 - Firebase - Cloud Firestore_25años.pdf`: configuración, modelo documental, escrituras, reglas y lecturas/listeners.
- `../../Clases/Clase 10 - Firebase Authentication_25años.pdf`: proveedores, SDK/FirebaseUI, sesión y verificación de correo.
- `lab_iot-2026-1.pdf`: ejemplo de pronósticos, restricciones y alcance histórico.
- `Rúbrica Laboratorio 6 - IOT.pdf`: criterios y puntajes de la prueba presencial.

Los ejemplos y el orden de implementación de esta guía son material de estudio añadido; no son una transcripción ni una solución oficial del enunciado nuevo.

### Aclaraciones para evitar copiar errores de contexto

1. Algunas diapositivas de clase 09.2 titulan “Realtime Database”, pero el código usa `FirebaseFirestore`: ese código pertenece a **Cloud Firestore**.
2. El material mezcla ejemplos con distintas versiones de BoM y requisitos Android antiguos. Usa una configuración compatible completa, no todas las cifras juntas.
3. `get()` hace una lectura única; no permanece escuchando cambios como `addSnapshotListener()`.
4. Firestore entrega snapshots con datos tipados; no necesitas convertir primero a texto plano ni usar Gson para `toObject()`.
5. La sesión de Auth puede persistir. `signOut()` termina la sesión; no equivale a borrar la base ni toda la caché Firestore.
6. La clase menciona desactivar protección contra enumeración para ciertos flujos de FirebaseUI. No es un requisito general del SDK de correo usado aquí: evita depender de una consulta previa para descubrir si existe una cuenta. Evalúa ese ajuste solo si reproduces un flujo clásico que lo necesite.
7. La verificación de email y los permisos son decisiones explícitas: no asumas que cualquier proveedor garantiza cualquier atributo. Si una función depende de correo verificado, comprueba `isEmailVerified()` y/o el token en reglas.

### Referencias oficiales para ampliar

- [Configurar Firebase en Android](https://firebase.google.com/docs/android/setup).
- [Modelo de datos Firestore](https://firebase.google.com/docs/firestore/data-model).
- [Crear y modificar documentos](https://firebase.google.com/docs/firestore/manage-data/add-data).
- [Leer documentos una vez](https://firebase.google.com/docs/firestore/query-data/get-data).
- [Escuchar cambios](https://firebase.google.com/docs/firestore/query-data/listen).
- [Consultas y filtros](https://firebase.google.com/docs/firestore/query-data/queries).
- [Eliminar documentos](https://firebase.google.com/docs/firestore/manage-data/delete-data).
- [Reglas y condiciones](https://firebase.google.com/docs/firestore/security/rules-conditions).
- [Authentication Android](https://firebase.google.com/docs/auth/android/start).
- [Correo y contraseña](https://firebase.google.com/docs/auth/android/password-auth).
- [Gestión de usuarios](https://firebase.google.com/docs/auth/android/manage-users).
- [Google](https://firebase.google.com/docs/auth/android/google-signin) y [Facebook](https://firebase.google.com/docs/auth/android/facebook-login).
- [FirebaseUI clásico 8.0.2, API de las clases](https://github.com/firebase/FirebaseUI-Android/blob/8.0.2/auth/README.md).
- [FirebaseUI en la rama principal, API diferente](https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md).

**Verificación de esta guía:** se revisó coherencia entre nombres de archivos, IDs de XML, bindings, modelo, campos y rutas/reglas. Los ejemplos Android no se compilaron ni se ejecutaron contra un proyecto Firebase; debes hacer Sync y probarlos con tu paquete, firma, JSON y credenciales reales antes de reutilizarlos en el lab.
