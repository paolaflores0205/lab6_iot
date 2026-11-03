package com.example.ola;

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

public class Pantallas {
    public static final String[] CATEGORIAS = {"Alimentación", "Transporte", "Estudios", "Otros"};
    public static final String[] ESTADOS = {"Pendiente", "Pagado"};

    public static boolean sesion(AppCompatActivity activity) {
        if (activity.getResources().getBoolean(R.bool.firebase_configurado)
                && FirebaseAuth.getInstance().getCurrentUser() != null) return true;
        acceso(activity);
        return false;
    }
    public static CollectionReference gastos() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) throw new IllegalStateException("No hay sesión");
        return FirebaseFirestore.getInstance().collection("usuarios")
                .document(user.getUid()).collection("gastos");
    }
    public static void margenes(View root) {
        int l=root.getPaddingLeft(), t=root.getPaddingTop(), r=root.getPaddingRight(), b=root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets i = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(l+i.left, t+i.top, r+i.right, b+i.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }
    public static void barra(AppCompatActivity activity, BottomNavigationView nav, int actual) {
        nav.setSelectedItemId(actual);
        nav.setOnItemSelectedListener(item -> { navegar(activity,item.getItemId()); return true; });
        nav.setOnItemReselectedListener(item -> navegar(activity,item.getItemId()));
    }
    private static void navegar(AppCompatActivity activity, int opcion) {
        if (opcion == R.id.navSalir) {
            AuthUI.getInstance().signOut(activity).addOnCompleteListener(task -> {
                if (task.isSuccessful()) acceso(activity);
                else Toast.makeText(activity,"No se pudo cerrar sesión",Toast.LENGTH_LONG).show();
            });
            return;
        }
        Class<?> destino = opcion == R.id.navEstadisticas ? EstadisticasActivity.class : GastosActivity.class;
        if (activity.getClass() == destino) return;
        Intent intent = new Intent(activity,destino);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
        activity.finish();
    }
    public static void acceso(AppCompatActivity activity) {
        Intent intent = new Intent(activity,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}
