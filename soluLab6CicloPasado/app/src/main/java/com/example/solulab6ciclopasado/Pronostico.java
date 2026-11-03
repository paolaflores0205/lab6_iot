package com.example.solulab6ciclopasado;

import java.util.Date;
import com.google.firebase.firestore.Exclude;

public class Pronostico {
    private String id;
    private String seleccionA;
    private String seleccionB;
    private Date fechaPartido;
    private int golesA;
    private int golesB;
    private String estado;

    public Pronostico() {
    }

    @Exclude
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSeleccionA() {
        return seleccionA;
    }

    public void setSeleccionA(String seleccionA) {
        this.seleccionA = seleccionA;
    }

    public String getSeleccionB() {
        return seleccionB;
    }

    public void setSeleccionB(String seleccionB) {
        this.seleccionB = seleccionB;
    }

    public Date getFechaPartido() {
        return fechaPartido;
    }

    public void setFechaPartido(Date fechaPartido) {
        this.fechaPartido = fechaPartido;
    }

    public int getGolesA() {
        return golesA;
    }

    public void setGolesA(int golesA) {
        this.golesA = golesA;
    }

    public int getGolesB() {
        return golesB;
    }

    public void setGolesB(int golesB) {
        this.golesB = golesB;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
