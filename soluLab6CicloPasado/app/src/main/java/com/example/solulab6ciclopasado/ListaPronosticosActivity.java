package com.example.solulab6ciclopasado;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.solulab6ciclopasado.databinding.ActivityListaPronosticosBinding;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ListaPronosticosActivity extends AppCompatActivity {
    private ActivityListaPronosticosBinding binding;
    private final ArrayList<String> filas = new ArrayList<>();
    private final ArrayList<Pronostico> pronosticos = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private CollectionReference pronosticosRef;
    private ListenerRegistration escucha;
    private boolean eliminando;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityListaPronosticosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Pantallas.ajustarBarras(binding.getRoot());
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filas);
        binding.listaPronosticos.setAdapter(adapter);
        binding.listaPronosticos.setEmptyView(binding.tvVacio);
        Pantallas.navegar(this, binding.navegacion, R.id.navPronosticos);
        binding.btnNuevo.setOnClickListener(view ->
                startActivity(new Intent(this, PronosticosActivity.class)));
        binding.listaPronosticos.setOnItemClickListener((parent, view, posicion, id) -> {
            if (eliminando) return;
            Pronostico seleccionado = pronosticos.get(posicion);
            if (!"Pendiente".equals(seleccionado.getEstado())) {
                mensaje("Este pronóstico está cerrado: no se puede editar ni eliminar");
                return;
            }
            new AlertDialog.Builder(this).setTitle("¿Qué deseas hacer?")
                    .setItems(new String[]{"Editar", "Eliminar"}, (dialog, opcion) -> {
                        if (opcion == 0) {
                            Intent intent = new Intent(this, PronosticosActivity.class);
                            intent.putExtra("pronosticoId", seleccionado.getId());
                            startActivity(intent);
                        } else {
                            new AlertDialog.Builder(this).setTitle("Eliminar pronóstico")
                                    .setMessage("¿Seguro que deseas eliminar " + seleccionado.getSeleccionA()
                                            + " vs " + seleccionado.getSeleccionB() + "?")
                                    .setNegativeButton("Cancelar", null)
                                    .setPositiveButton("Eliminar", (confirmacion, boton) ->
                                            eliminarPronostico(seleccionado)).show();
                        }
                    }).show();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!Pantallas.comprobarSesion(this)) return;
        pronosticosRef = Pantallas.pronosticos();
        // Una única escucha activa: snapshot inicial y todos los cambios posteriores.
        if (escucha != null) escucha.remove();
        escucha = pronosticosRef.addSnapshotListener((documentos, error) -> {
            if (error != null) {
                binding.tvVacio.setText("No se pudieron cargar los pronósticos");
                mensaje("Error de lectura: " + error.getMessage());
                return;
            }
            if (documentos == null) return;
            filas.clear();
            pronosticos.clear();
            SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            for (QueryDocumentSnapshot documento : documentos) {
                Pronostico p = documento.toObject(Pronostico.class);
                p.setId(documento.getId());
                pronosticos.add(p);
                String fecha = p.getFechaPartido() == null ? "Sin fecha" : formato.format(p.getFechaPartido());
                filas.add(p.getSeleccionA() + " vs " + p.getSeleccionB()
                        + "\nFecha: " + fecha
                        + "\nResultado: " + p.getGolesA() + " - " + p.getGolesB()
                        + " | " + p.getEstado());
            }
            binding.tvVacio.setText("No tienes pronósticos. Pulsa Nuevo pronóstico para crear uno.");
            binding.tvTotal.setText("Total de pronósticos: " + pronosticos.size());
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    protected void onStop() {
        if (escucha != null) {
            escucha.remove();
            escucha = null;
        }
        super.onStop();
    }

    private void eliminarPronostico(Pronostico seleccionado) {
        if (eliminando || !Pantallas.comprobarSesion(this)) return;
        eliminando = true;
        DocumentReference documento = pronosticosRef.document(seleccionado.getId());
        // Evita borrar un registro que otro cliente haya cerrado desde que lo vimos.
        FirebaseFirestore.getInstance().runTransaction(transaccion -> {
            if (!"Pendiente".equals(transaccion.get(documento).getString("estado"))) {
                throw new IllegalStateException("Solo puedes eliminar pronósticos pendientes");
            }
            transaccion.delete(documento);
            return null;
        }).addOnSuccessListener(unused -> {
            eliminando = false;
            mensaje("Pronóstico eliminado");
            // El listener actualiza las dos listas. No borramos la fila una segunda vez.
        }).addOnFailureListener(error -> {
            eliminando = false;
            mensaje("No se pudo eliminar: " + error.getMessage());
        });
    }

    private void mensaje(String texto) {
        Toast.makeText(this, texto, Toast.LENGTH_LONG).show();
    }
}
