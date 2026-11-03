package com.example.ola;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ola.databinding.ActivityFormularioGastoBinding;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FormularioGastoActivity extends AppCompatActivity {
    private ActivityFormularioGastoBinding binding;
    private String gastoId;
    private Date fecha;
    private boolean guardando;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        binding=ActivityFormularioGastoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Pantallas.margenes(binding.getRoot());
        if (!Pantallas.sesion(this)) return;
        Pantallas.barra(this,binding.navegacion,R.id.navGastos);
        ArrayAdapter<String> categorias=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,Pantallas.CATEGORIAS);
        categorias.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spCategoria.setAdapter(categorias);
        ArrayAdapter<String> estados=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,Pantallas.ESTADOS);
        estados.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spEstado.setAdapter(estados);
        binding.btnFecha.setOnClickListener(v -> calendario());
        binding.btnGuardar.setOnClickListener(v -> guardar());
        binding.btnCancelar.setOnClickListener(v -> finish());
        gastoId=getIntent().getStringExtra("gastoId");
        if (state!=null && state.containsKey("fecha")) fecha=new Date(state.getLong("fecha"));
        mostrarFecha();
        if (gastoId!=null) {
            binding.tvTitulo.setText("Editar gasto");
            binding.btnGuardar.setText("Actualizar gasto");
            binding.btnGuardar.setEnabled(false);
            cargar(state!=null);
        }
    }
    @Override protected void onStart() { super.onStart(); Pantallas.sesion(this); }
    @Override protected void onSaveInstanceState(Bundle state) {
        if (fecha!=null) state.putLong("fecha",fecha.getTime());
        super.onSaveInstanceState(state);
    }
    private void calendario() {
        Calendar c=Calendar.getInstance();
        if (fecha!=null) c.setTime(fecha);
        new DatePickerDialog(this,(selector,anio,mes,dia) -> {
            Calendar elegida=Calendar.getInstance();
            elegida.clear(); elegida.set(anio,mes,dia);
            fecha=elegida.getTime(); mostrarFecha();
        },c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();
    }
    private void mostrarFecha() {
        binding.btnFecha.setText(fecha==null ? "Seleccionar fecha" : android.text.format.DateFormat.format("dd/MM/yyyy",fecha));
    }
    private void cargar(boolean restaurando) {
        Pantallas.gastos().document(gastoId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) { mensaje("Este gasto ya no existe"); finish(); return; }
            Gasto g=doc.toObject(Gasto.class);
            if (g==null) return;
            if (!restaurando) {
                binding.etDescripcion.setText(g.getDescripcion());
                binding.etMonto.setText(Dinero.texto(g.getMontoCentimos()));
                fecha=g.getFecha(); mostrarFecha();
                for (int i=0;i<Pantallas.CATEGORIAS.length;i++)
                    if (Pantallas.CATEGORIAS[i].equals(g.getCategoria())) binding.spCategoria.setSelection(i);
                for (int i=0;i<Pantallas.ESTADOS.length;i++)
                    if (Pantallas.ESTADOS[i].equals(g.getEstado())) binding.spEstado.setSelection(i);
            }
            binding.btnGuardar.setEnabled(true);
        }).addOnFailureListener(e -> mensaje("No se pudo cargar: "+e.getMessage()));
    }
    private void guardar() {
        if (guardando || !Pantallas.sesion(this)) return;
        String descripcion=binding.etDescripcion.getText().toString().trim();
        if (descripcion.isEmpty() || descripcion.length()>100 || fecha==null) {
            mensaje("Ingresa una descripción de hasta 100 caracteres y una fecha"); return;
        }
        long monto;
        try { monto=Dinero.leerCentimos(binding.etMonto.getText().toString()); }
        catch (IllegalArgumentException e) { binding.etMonto.setError(e.getMessage()); return; }
        Map<String,Object> campos=new HashMap<>();
        campos.put("descripcion",descripcion);
        campos.put("categoria",binding.spCategoria.getSelectedItem().toString());
        campos.put("montoCentimos",monto);
        campos.put("fecha",fecha);
        campos.put("estado",binding.spEstado.getSelectedItem().toString());
        ocupado(true);
        if (gastoId==null) {
            Pantallas.gastos().add(campos).addOnSuccessListener(doc -> exito("Gasto registrado"))
                    .addOnFailureListener(this::fallo);
        } else {
            Pantallas.gastos().document(gastoId).update(campos)
                    .addOnSuccessListener(v -> exito("Gasto actualizado"))
                    .addOnFailureListener(this::fallo);
        }
    }
    private void ocupado(boolean valor) {
        guardando=valor;
        binding.btnGuardar.setEnabled(!valor); binding.btnCancelar.setEnabled(!valor);
        binding.etDescripcion.setEnabled(!valor); binding.etMonto.setEnabled(!valor);
        binding.spCategoria.setEnabled(!valor); binding.spEstado.setEnabled(!valor); binding.btnFecha.setEnabled(!valor);
        binding.tvAviso.setText(valor ? "Guardando; espera la confirmación de Firebase…" : "");
        binding.navegacion.setVisibility(valor ? android.view.View.GONE : android.view.View.VISIBLE);
    }
    private void exito(String texto) { mensaje(texto); finish(); }
    private void fallo(Exception e) { ocupado(false); mensaje("No se pudo guardar: "+e.getMessage()); }
    private void mensaje(String texto) { Toast.makeText(this,texto,Toast.LENGTH_LONG).show(); }
}
