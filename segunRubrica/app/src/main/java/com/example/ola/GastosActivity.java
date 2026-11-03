package com.example.ola;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ola.databinding.ActivityGastosBinding;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class GastosActivity extends AppCompatActivity {
    private ActivityGastosBinding binding;
    private final ArrayList<Gasto> gastos=new ArrayList<>();
    private final ArrayList<String> filas=new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private ListenerRegistration escucha;
    private boolean borrando;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        binding=ActivityGastosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Pantallas.margenes(binding.getRoot());
        Pantallas.barra(this,binding.navegacion,R.id.navGastos);
        adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,filas);
        binding.listaGastos.setAdapter(adapter);
        binding.listaGastos.setEmptyView(binding.tvVacio);
        binding.btnNuevo.setOnClickListener(v -> startActivity(new Intent(this,FormularioGastoActivity.class)));
        binding.listaGastos.setOnItemClickListener((parent,view,posicion,id) -> {
            if (borrando) return;
            Gasto elegido=gastos.get(posicion);
            new AlertDialog.Builder(this).setTitle(elegido.getDescripcion())
                    .setItems(new String[]{"Editar","Eliminar"},(dialog,opcion) -> {
                        if (opcion==0) {
                            Intent intent=new Intent(this,FormularioGastoActivity.class);
                            intent.putExtra("gastoId",elegido.getId());
                            startActivity(intent);
                        } else new AlertDialog.Builder(this).setTitle("Eliminar gasto")
                                .setMessage("¿Eliminar este gasto definitivamente?")
                                .setNegativeButton("Cancelar",null)
                                .setPositiveButton("Eliminar",(d,b) -> eliminar(elegido)).show();
                    }).show();
        });
    }
    @Override protected void onStart() {
        super.onStart();
        if (!Pantallas.sesion(this)) return;
        if (escucha!=null) escucha.remove();
        escucha=Pantallas.gastos().addSnapshotListener((snapshot,error) -> {
            if (error!=null) { mensaje("Error al leer: "+error.getMessage()); return; }
            if (snapshot==null) return;
            gastos.clear();
            filas.clear();
            SimpleDateFormat formato=new SimpleDateFormat("dd/MM/yyyy",Locale.getDefault());
            long total=0;
            for (QueryDocumentSnapshot doc:snapshot) {
                Gasto g=doc.toObject(Gasto.class);
                g.setId(doc.getId());
                gastos.add(g);
                total+=g.getMontoCentimos();
                String fecha=g.getFecha()==null ? "Sin fecha" : formato.format(g.getFecha());
                filas.add(g.getDescripcion()+"\n"+g.getCategoria()+" · "+fecha
                        +"\n"+Dinero.soles(g.getMontoCentimos())+" · "+g.getEstado());
            }
            binding.tvResumen.setText(gastos.size()+" gastos · Total: "+Dinero.soles(total));
            binding.tvVacio.setText("Aún no tienes gastos. Pulsa Nuevo gasto.");
            adapter.notifyDataSetChanged();
        });
    }
    @Override protected void onStop() {
        if (escucha!=null) { escucha.remove(); escucha=null; }
        super.onStop();
    }
    private void eliminar(Gasto gasto) {
        if (borrando || !Pantallas.sesion(this)) return;
        borrando=true;
        Pantallas.gastos().document(gasto.getId()).delete()
                .addOnSuccessListener(v -> { borrando=false; mensaje("Gasto eliminado"); })
                .addOnFailureListener(e -> { borrando=false; mensaje("No se pudo eliminar: "+e.getMessage()); });
        // La escucha se encarga de quitar la fila y actualizar el total.
    }
    private void mensaje(String texto) { Toast.makeText(this,texto,Toast.LENGTH_LONG).show(); }
}
