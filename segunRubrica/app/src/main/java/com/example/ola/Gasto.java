package com.example.ola;

import com.google.firebase.firestore.Exclude;
import java.util.Date;

public class Gasto {
    private String id;
    private String descripcion;
    private String categoria;
    private long montoCentimos;
    private Date fecha;
    private String estado;

    public Gasto() { }
    @Exclude public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public long getMontoCentimos() { return montoCentimos; }
    public void setMontoCentimos(long montoCentimos) { this.montoCentimos = montoCentimos; }
    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
