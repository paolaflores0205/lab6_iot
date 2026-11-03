package com.example.clase10;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.clase10.databinding.ActivityMain2Binding;
import com.example.clase10.dtos.Contacto;
import com.example.clase10.dtos.Usuario;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class MainActivity2 extends AppCompatActivity {

    ActivityMain2Binding binding;
    FirebaseFirestore db;
    ListenerRegistration snapshotListener;
    FirebaseUser currentUser;

    private static final int TOAST_DURATION = Toast.LENGTH_LONG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMain2Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();

        currentUser = FirebaseAuth.getInstance().getCurrentUser();


        binding.btnRegistrarUsuario.setOnClickListener(view -> {
            String nombre = binding.textFieldNombre.getEditText().getText().toString();
            String apellido = binding.textFieldApellido.getEditText().getText().toString();
            String correo = binding.textFieldCorreo.getEditText().getText().toString();
            String telefono = binding.textFieldTelefono.getEditText().getText().toString();

            Contacto contacto = new Contacto(nombre, apellido, correo, telefono);

            if (currentUser != null) {
                String uid = currentUser.getUid();
                db.collection("usuarios_por_auth")
                        .document(uid)
                        .collection("mis_contactos")
                        .add(contacto)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(this, "Usuario grabado", TOAST_DURATION).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Algo pasó al guardar ", TOAST_DURATION).show();
                        });
            } else {
                Toast.makeText(MainActivity2.this, "No está logueado", TOAST_DURATION).show();
            }

        });

        binding.btnTiempoReal.setOnClickListener(view -> {

            if (currentUser != null) {
                String uid = currentUser.getUid();

                snapshotListener = db.collection("usuarios_por_auth")
                        .document(uid)
                        .collection("mis_contactos")
                        .addSnapshotListener((collection, error) -> {

                            if (error != null) {
                                Log.w("msg-test", "Listen failed.", error);
                                return;
                            }

                            Log.d("msg-test", "---- Datos en tiempo real ----");
                            for (QueryDocumentSnapshot doc : collection) {
                                Contacto contacto = doc.toObject(Contacto.class);
                                Log.d("msg-test", "Colleccion:  " + contacto.getNombre() );
                                Toast.makeText(this, String.format("Nombre: %s | apellido: %s",
                                                contacto.getNombre(), contacto.getApellido()),
                                        Toast.LENGTH_LONG).show();
                            }

                        });
            } else {
                Toast.makeText(MainActivity2.this, "No está logueado",
                        TOAST_DURATION).show();
            }

        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (snapshotListener != null)
            snapshotListener.remove();
    }
}