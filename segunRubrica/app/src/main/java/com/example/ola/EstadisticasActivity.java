package com.example.ola;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ola.databinding.ActivityEstadisticasBinding;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class EstadisticasActivity extends AppCompatActivity {
    private ActivityEstadisticasBinding binding;
    private final ArrayList<Gasto> gastos=new ArrayList<>();
    private final ArrayList<String> meses=new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private ListenerRegistration escucha;
    private String filtro="Todos los meses";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); EdgeToEdge.enable(this);
        binding=ActivityEstadisticasBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot()); Pantallas.margenes(binding.getRoot());
        Pantallas.barra(this,binding.navegacion,R.id.navEstadisticas);
        meses.add("Todos los meses");
        adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,meses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spMes.setAdapter(adapter);
        binding.spMes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent,View view,int posicion,long id) {
                filtro=meses.get(posicion); actualizar();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }
    @Override protected void onStart() {
        super.onStart();
        if (!Pantallas.sesion(this)) return;
        if (escucha!=null) escucha.remove();
        escucha=Pantallas.gastos().addSnapshotListener((snapshot,error) -> {
            if (error!=null) { Toast.makeText(this,error.getMessage(),Toast.LENGTH_LONG).show(); return; }
            if (snapshot==null) return;
            gastos.clear();
            ArrayList<String> disponibles=new ArrayList<>();
            SimpleDateFormat formato=new SimpleDateFormat("yyyy-MM",Locale.ROOT);
            for (QueryDocumentSnapshot doc:snapshot) {
                Gasto g=doc.toObject(Gasto.class); gastos.add(g);
                if (g.getFecha()!=null) {
                    String mes=formato.format(g.getFecha());
                    if (!disponibles.contains(mes)) disponibles.add(mes);
                }
            }
            Collections.sort(disponibles);
            meses.clear(); meses.add("Todos los meses"); meses.addAll(disponibles);
            adapter.notifyDataSetChanged();
            int posicion=meses.indexOf(filtro);
            if (posicion<0) { posicion=0; filtro=meses.get(0); }
            binding.spMes.setSelection(posicion);
            actualizar();
        });
    }
    private void actualizar() {
        ResumenGastos resumen=ResumenGastos.calcular(gastos,filtro);
        binding.tvResumen.setText("Gastos: "+resumen.cantidad+" · Total: "+Dinero.soles(resumen.totalCentimos)
                +"\nPagados: "+resumen.pagados+" · Pendientes: "+resumen.pendientes);
        double[] montos=new double[4];
        for (int i=0;i<4;i++) montos[i]=resumen.porCategoria[i]/100.0;
        binding.graficoBarras.setDatos(Pantallas.CATEGORIAS,montos,false);
        binding.graficoCircular.setDatos(new String[]{"Pendientes","Pagados"},
                new double[]{resumen.pendientes,resumen.pagados},true);
    }
    @Override protected void onStop() {
        if (escucha!=null) { escucha.remove(); escucha=null; }
        super.onStop();
    }
}
