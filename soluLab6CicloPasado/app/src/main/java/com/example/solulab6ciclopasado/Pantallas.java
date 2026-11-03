package com.example.solulab6ciclopasado;

import android.content.Intent;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.firebase.ui.auth.AuthUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

/** Código repetido de navegación. La lógica del CRUD sigue en las Activities. */
public class Pantallas {
    public static boolean comprobarSesion(AppCompatActivity activity) {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) return true;
        volverAlAcceso(activity);
        return false;
    }

    public static CollectionReference pronosticos() {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) throw new IllegalStateException("No hay una sesión iniciada");
        return FirebaseFirestore.getInstance().collection("usuarios")
                .document(usuario.getUid()).collection("pronosticos");
    }

    public static void ajustarBarras(View root) {
        int izquierda = root.getPaddingLeft();
        int arriba = root.getPaddingTop();
        int derecha = root.getPaddingRight();
        int abajo = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets barras = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            view.setPadding(izquierda + barras.left, arriba + barras.top,
                    derecha + barras.right, abajo + barras.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    public static void navegar(AppCompatActivity activity, BottomNavigationView barra, int actual) {
        barra.setSelectedItemId(actual);
        barra.setOnItemSelectedListener(item -> {
            abrirOpcion(activity, item.getItemId());
            return true;
        });
        barra.setOnItemReselectedListener(item -> abrirOpcion(activity, item.getItemId()));
    }

    private static void abrirOpcion(AppCompatActivity activity, int opcion) {
        if (opcion == R.id.navCerrar) {
            AuthUI.getInstance().signOut(activity).addOnCompleteListener(task -> {
                if (task.isSuccessful()) volverAlAcceso(activity);
                else Toast.makeText(activity, "No se pudo cerrar sesión", Toast.LENGTH_LONG).show();
            });
            return;
        }
        Class<?> destino = opcion == R.id.navEstadisticas
                ? EstadisticasActivity.class : ListaPronosticosActivity.class;
        if (activity.getClass() == destino) return;
        Intent intent = new Intent(activity, destino);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
        activity.finish();
    }

    public static void volverAlAcceso(AppCompatActivity activity) {
        Intent intent = new Intent(activity, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}
