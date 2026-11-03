package com.example.solulab6ciclopasado;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.EditText;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import com.example.solulab6ciclopasado.databinding.ActivityMainBinding;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.google.firebase.auth.FirebaseAuth;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(), result -> {
                if (result.getResultCode() == RESULT_OK
                        && FirebaseAuth.getInstance().getCurrentUser() != null) {
                    abrirPronosticos();
                } else {
                    binding.btnIngresar.setEnabled(true);
                    binding.tvMensaje.setText("Acceso cancelado o no completado. Puedes intentarlo nuevamente.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Configura también las pantallas de FirebaseUI en español.
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("es"));
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Pantallas.ajustarBarras(binding.getRoot());
        FirebaseAuth.getInstance().setLanguageCode("es");

        binding.btnIngresar.setOnClickListener(view ->
                lanzarAcceso(new AuthUI.IdpConfig.EmailBuilder().build()));
        binding.btnGoogle.setOnClickListener(view ->
                lanzarAcceso(new AuthUI.IdpConfig.GoogleBuilder().build()));
        binding.btnGithub.setOnClickListener(view ->
                lanzarAcceso(new AuthUI.IdpConfig.GitHubBuilder().build()));
        binding.btnRecuperar.setOnClickListener(view -> recuperarClave());
        // Facebook es la ampliación de la rúbrica 2026-2; ver CONFIGURACION_FIREBASE.md.
        if (getResources().getBoolean(R.bool.facebook_habilitado)) {
            binding.btnFacebook.setVisibility(android.view.View.VISIBLE);
            binding.btnFacebook.setOnClickListener(view ->
                    lanzarAcceso(new AuthUI.IdpConfig.FacebookBuilder().build()));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) abrirPronosticos();
    }

    private void lanzarAcceso(AuthUI.IdpConfig proveedor) {
        try {
            Intent intent = AuthUI.getInstance().createSignInIntentBuilder()
                    .setAvailableProviders(Collections.singletonList(proveedor))
                    .setLogo(R.drawable.logo_mundial)
                    .setTheme(R.style.TemaAcceso)
                    .build();
            binding.btnIngresar.setEnabled(false);
            signInLauncher.launch(intent);
        } catch (Exception error) {
            binding.btnIngresar.setEnabled(true);
            binding.tvMensaje.setText("No se pudo abrir el acceso. Revisa la configuración del proveedor.");
            android.util.Log.e("AUTH", "Configuración de acceso", error);
        }
    }

    private void abrirPronosticos() {
        Intent intent = new Intent(this, ListaPronosticosActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void recuperarClave() {
        EditText correo = new EditText(this);
        correo.setHint("Correo electrónico");
        correo.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle("Recuperar contraseña").setView(correo)
                .setNegativeButton("Cancelar", null).setPositiveButton("Enviar", null).create();
        dialogo.setOnShowListener(unused -> dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String email = correo.getText().toString().trim();
                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        correo.setError("Ingresa un correo válido");
                        return;
                    }
                    dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                            .addOnCompleteListener(task -> {
                                dialogo.dismiss();
                                binding.tvMensaje.setText(task.isSuccessful()
                                        ? "Si corresponde a una cuenta, recibirás un correo. Revisa spam."
                                        : "No se pudo enviar la solicitud. Revisa tu conexión e inténtalo nuevamente.");
                            });
                }));
        dialogo.show();
    }
}
