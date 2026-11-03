package com.example.ola;

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
import com.example.ola.databinding.ActivityMainBinding;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.google.firebase.auth.FirebaseAuth;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(), result -> {
                habilitar(true);
                if (result.getResultCode() == RESULT_OK && FirebaseAuth.getInstance().getCurrentUser()!=null) entrar();
                else binding.tvMensaje.setText("Acceso cancelado o no completado. Inténtalo nuevamente.");
            });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("es"));
        EdgeToEdge.enable(this);
        binding=ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Pantallas.margenes(binding.getRoot());
        if (!getResources().getBoolean(R.bool.firebase_configurado)) {
            binding.tvMensaje.setText("Primero conecta esta app con Firebase: agrega app/google-services.json y sincroniza. Sigue GUIA_SOLUCION.md, etapa 1.");
            habilitar(false);
            return;
        }
        FirebaseAuth.getInstance().setLanguageCode("es");
        binding.btnCorreo.setOnClickListener(v -> acceder("correo"));
        binding.btnGoogle.setOnClickListener(v -> acceder("google"));
        binding.btnFacebook.setOnClickListener(v -> acceder("facebook"));
        binding.btnRecuperar.setOnClickListener(v -> recuperar());
    }
    @Override protected void onStart() {
        super.onStart();
        if (getResources().getBoolean(R.bool.firebase_configurado)
                && FirebaseAuth.getInstance().getCurrentUser()!=null) entrar();
    }
    private void habilitar(boolean valor) {
        binding.btnCorreo.setEnabled(valor);
        binding.btnGoogle.setEnabled(valor);
        binding.btnFacebook.setEnabled(valor);
        binding.btnRecuperar.setEnabled(valor);
    }
    private void acceder(String tipo) {
        try {
            AuthUI.IdpConfig proveedor;
            if ("google".equals(tipo)) {
                int recurso=getResources().getIdentifier("default_web_client_id","string",getPackageName());
                if (recurso==0 || getString(recurso).isEmpty()) {
                    binding.tvMensaje.setText("Habilita Google en Firebase, registra tu SHA y descarga el JSON actualizado. Ver etapa 3 de la guía.");
                    return;
                }
                proveedor=new AuthUI.IdpConfig.GoogleBuilder().build();
            } else if ("facebook".equals(tipo)) {
                if (!getResources().getBoolean(R.bool.facebook_configurado)) {
                    binding.tvMensaje.setText("Completa Facebook en Meta/Firebase y las propiedades de Gradle. Ver etapa 4 de la guía.");
                    return;
                }
                proveedor=new AuthUI.IdpConfig.FacebookBuilder().build();
            } else proveedor=new AuthUI.IdpConfig.EmailBuilder().build();
            Intent intent=AuthUI.getInstance().createSignInIntentBuilder()
                    .setAvailableProviders(Collections.singletonList(proveedor))
                    .setLogo(R.drawable.logo_gastos).setTheme(R.style.TemaAcceso).build();
            habilitar(false);
            launcher.launch(intent);
        } catch (Exception e) {
            habilitar(true);
            binding.tvMensaje.setText("No se pudo abrir el acceso. Revisa la configuración del proveedor.");
            android.util.Log.e("ACCESO","Error de proveedor",e);
        }
    }
    private void entrar() {
        Intent intent=new Intent(this,GastosActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    private void recuperar() {
        EditText correo=new EditText(this);
        correo.setHint("Correo electrónico");
        correo.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        AlertDialog dialogo=new AlertDialog.Builder(this).setTitle("Recuperar contraseña")
                .setView(correo).setNegativeButton("Cancelar",null).setPositiveButton("Enviar",null).create();
        dialogo.setOnShowListener(d -> dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String texto=correo.getText().toString().trim();
            if (!Patterns.EMAIL_ADDRESS.matcher(texto).matches()) { correo.setError("Correo inválido"); return; }
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            FirebaseAuth.getInstance().sendPasswordResetEmail(texto).addOnCompleteListener(task -> {
                dialogo.dismiss();
                binding.tvMensaje.setText(task.isSuccessful()
                        ? "Si corresponde a una cuenta, recibirás un correo. Revisa spam."
                        : "No se pudo enviar la solicitud. Revisa tu conexión.");
            });
        }));
        dialogo.show();
    }
}
