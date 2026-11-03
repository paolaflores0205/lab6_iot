package com.example.solulab6ciclopasado;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.example.solulab6ciclopasado.databinding.ActivityEstadisticasBinding;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;

public class EstadisticasActivity extends AppCompatActivity {
    private ActivityEstadisticasBinding binding;
    private final ArrayList<Pronostico> pronosticos = new ArrayList<>();
    private final ArrayList<String> equipos = new ArrayList<>();
    private ArrayAdapter<String> filtroAdapter;
    private ListenerRegistration escucha;
    private String filtroActual = "Todas las selecciones";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityEstadisticasBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Pantallas.ajustarBarras(binding.getRoot());
        Pantallas.navegar(this, binding.navegacion, R.id.navEstadisticas);
        binding.graficoCircular.setCircular(true);
        equipos.add("Todas las selecciones");
        filtroAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, equipos);
        filtroAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spFiltro.setAdapter(filtroAdapter);
        binding.spFiltro.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int posicion, long id) {
                filtroActual = equipos.get(posicion);
                actualizarGraficos();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!Pantallas.comprobarSesion(this)) return;
        if (escucha != null) escucha.remove();
        escucha = Pantallas.pronosticos().addSnapshotListener((documentos, error) -> {
            if (error != null) {
                binding.tvResumen.setText("No se pudieron cargar las estadísticas");
                Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
            if (documentos == null) return;
            pronosticos.clear();
            equipos.clear();
            equipos.add("Todas las selecciones");
            for (QueryDocumentSnapshot documento : documentos) {
                Pronostico p = documento.toObject(Pronostico.class);
                pronosticos.add(p);
                agregarEquipo(p.getSeleccionA());
                agregarEquipo(p.getSeleccionB());
            }
            filtroAdapter.notifyDataSetChanged();
            int posicion = equipos.indexOf(filtroActual);
            if (posicion < 0) {
                posicion = 0;
                filtroActual = equipos.get(0);
            }
            binding.spFiltro.setSelection(posicion);
            actualizarGraficos();
        });
    }

    private void agregarEquipo(String equipo) {
        if (equipo == null) return;
        for (String existente : equipos) {
            if (existente.equalsIgnoreCase(equipo)) return;
        }
        equipos.add(equipo);
    }

    private void actualizarGraficos() {
        ResumenPronosticos resumen = ResumenPronosticos.contar(pronosticos, filtroActual);
        binding.tvResumen.setText("Total: " + resumen.total + "\nPendientes: " + resumen.pendientes
                + " | Acertados: " + resumen.acertados + " | Fallados: " + resumen.fallados);
        binding.graficoBarras.setDatos(resumen.pendientes, resumen.acertados, resumen.fallados);
        binding.graficoCircular.setDatos(resumen.pendientes, resumen.acertados, resumen.fallados);
    }

    @Override
    protected void onStop() {
        if (escucha != null) {
            escucha.remove();
            escucha = null;
        }
        super.onStop();
    }
}
