package com.example.solulab6ciclopasado;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.example.solulab6ciclopasado.databinding.ActivityPronosticosBinding;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PronosticosActivity extends AppCompatActivity {
    private ActivityPronosticosBinding binding;
    private CollectionReference pronosticosRef;
    private Date fechaPartido;
    private String pronosticoId;
    private boolean guardando;
    private boolean editable = true;
    private final String[] estados = {"Pendiente", "Acertado", "Fallado"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityPronosticosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Pantallas.ajustarBarras(binding.getRoot());
        if (!Pantallas.comprobarSesion(this)) return;
        Pantallas.navegar(this, binding.navegacion, R.id.navPronosticos);
        pronosticosRef = Pantallas.pronosticos();
        Notificaciones.preparar(this);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, estados);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spEstado.setAdapter(adapter);
        binding.btnFecha.setOnClickListener(view -> elegirFecha());
        binding.btnGuardarPrueba.setOnClickListener(view -> guardarPronostico());
        binding.btnCancelar.setOnClickListener(view -> finish());

        pronosticoId = getIntent().getStringExtra("pronosticoId");
        if (savedInstanceState != null) {
            long fecha = savedInstanceState.getLong("fecha", -1);
            if (fecha != -1) fechaPartido = new Date(fecha);
        }
        mostrarFecha();
        if (pronosticoId == null) {
            binding.spEstado.setEnabled(false); // En creación siempre Pendiente.
        } else {
            binding.tvPronosticos.setText("Editar pronóstico");
            binding.btnGuardarPrueba.setText("Actualizar pronóstico");
            binding.btnGuardarPrueba.setEnabled(false);
            cargarPronostico(savedInstanceState != null);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Pantallas.comprobarSesion(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (fechaPartido != null) outState.putLong("fecha", fechaPartido.getTime());
        super.onSaveInstanceState(outState);
    }

    private void elegirFecha() {
        Calendar fecha = Calendar.getInstance();
        if (fechaPartido != null) fecha.setTime(fechaPartido);
        new DatePickerDialog(this, (selector, anio, mes, dia) -> {
            Calendar elegida = Calendar.getInstance();
            elegida.clear();
            elegida.set(anio, mes, dia);
            fechaPartido = elegida.getTime();
            mostrarFecha();
        }, fecha.get(Calendar.YEAR), fecha.get(Calendar.MONTH),
                fecha.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void mostrarFecha() {
        binding.btnFecha.setText(fechaPartido == null ? "Seleccionar fecha"
                : android.text.format.DateFormat.format("dd/MM/yyyy", fechaPartido));
    }

    private void cargarPronostico(boolean conservarBorrador) {
        pronosticosRef.document(pronosticoId).get().addOnSuccessListener(documento -> {
            if (!documento.exists()) {
                mensaje("El pronóstico ya no existe");
                finish();
                return;
            }
            Pronostico p = documento.toObject(Pronostico.class);
            if (p == null) return;
            editable = "Pendiente".equals(p.getEstado());
            if (!editable) {
                mensaje("El pronóstico está cerrado y no se puede modificar");
                finish();
                return;
            }
            if (!conservarBorrador) {
                binding.etSeleccionA.setText(p.getSeleccionA());
                binding.etSeleccionB.setText(p.getSeleccionB());
                binding.etGolesA.setText(String.valueOf(p.getGolesA()));
                binding.etGolesB.setText(String.valueOf(p.getGolesB()));
                fechaPartido = p.getFechaPartido();
                mostrarFecha();
                for (int i = 0; i < estados.length; i++) {
                    if (estados[i].equals(p.getEstado())) binding.spEstado.setSelection(i);
                }
            }
            binding.btnGuardarPrueba.setEnabled(!guardando);
        }).addOnFailureListener(error -> mensaje("No se pudo cargar: " + error.getMessage()));
    }

    private void guardarPronostico() {
        if (guardando || !editable || !Pantallas.comprobarSesion(this)) return;
        String a = binding.etSeleccionA.getText().toString().trim().replaceAll("\\s+", " ");
        String b = binding.etSeleccionB.getText().toString().trim().replaceAll("\\s+", " ");
        if (a.isEmpty() || b.isEmpty() || fechaPartido == null) {
            mensaje("Completa las selecciones y elige una fecha");
            return;
        }
        if (a.equalsIgnoreCase(b)) {
            mensaje("Las selecciones deben ser diferentes");
            return;
        }
        int ga, gb;
        try {
            ga = Integer.parseInt(binding.etGolesA.getText().toString().trim());
            gb = Integer.parseInt(binding.etGolesB.getText().toString().trim());
            if (ga < 0 || gb < 0) throw new NumberFormatException();
        } catch (NumberFormatException error) {
            mensaje("Los goles deben ser enteros válidos mayores o iguales a cero");
            return;
        }
        // Un mapa permite usar los mismos campos en creación y actualización.
        Map<String, Object> campos = new HashMap<>();
        campos.put("seleccionA", a);
        campos.put("seleccionB", b);
        campos.put("fechaPartido", fechaPartido);
        campos.put("golesA", ga);
        campos.put("golesB", gb);
        campos.put("estado", pronosticoId == null ? "Pendiente"
                : binding.spEstado.getSelectedItem().toString());
        ocupado(true);
        if (pronosticoId == null) {
            pronosticosRef.add(campos)
                    .addOnSuccessListener(doc -> guardado("Pronóstico registrado correctamente"))
                    .addOnFailureListener(this::falloAlGuardar);
        } else {
            DocumentReference documento = pronosticosRef.document(pronosticoId);
            // La transacción comprueba el estado ACTUAL, incluso si otra pantalla lo cambió.
            FirebaseFirestore.getInstance().runTransaction(transaccion -> {
                if (!"Pendiente".equals(transaccion.get(documento).getString("estado"))) {
                    throw new IllegalStateException("Solo puedes editar pronósticos pendientes");
                }
                transaccion.update(documento, campos);
                return null;
            }).addOnSuccessListener(unused -> guardado("Pronóstico actualizado correctamente"))
                    .addOnFailureListener(this::falloAlGuardar);
        }
    }

    private void ocupado(boolean valor) {
        guardando = valor;
        binding.btnGuardarPrueba.setEnabled(!valor && editable);
        binding.etSeleccionA.setEnabled(!valor);
        binding.etSeleccionB.setEnabled(!valor);
        binding.etGolesA.setEnabled(!valor);
        binding.etGolesB.setEnabled(!valor);
        binding.btnFecha.setEnabled(!valor);
        binding.spEstado.setEnabled(!valor && pronosticoId != null);
        binding.btnCancelar.setEnabled(!valor);
        binding.navegacion.setVisibility(valor ? android.view.View.GONE : android.view.View.VISIBLE);
        binding.tvAviso.setText(valor ? "Guardando; espera la confirmación de Firebase…" : "");
    }

    private void guardado(String texto) {
        mensaje(texto);
        Notificaciones.mostrar(this, texto);
        finish(); // La lista que queda debajo se vuelve a conectar en onStart().
    }

    private void falloAlGuardar(Exception error) {
        ocupado(false);
        mensaje("No se pudo guardar: " + error.getMessage());
    }

    private void mensaje(String texto) {
        Toast.makeText(this, texto, Toast.LENGTH_LONG).show();
    }
}
