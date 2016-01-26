package com.quiniela.acdat.quinielajson;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;

public class Jornada {
    ArrayList<Partido> partidos;
    ArrayList<Premio> premios;
    @Expose
    ArrayList<Acierto> aciertos;
    long recaudacion;
    @Expose
    int jornada;
    int precio;
    static int INICIO, FIN;
    @Expose
    long totalGanado;

    public Jornada() {
        partidos = new ArrayList<>();
        premios = new ArrayList<>();
        aciertos = new ArrayList<>();
    }

    public long getTotalGanado() {
        return totalGanado;
    }

    public void setTotalGanado(long totalGanado) {
        this.totalGanado = totalGanado;
    }

    public synchronized void addAcierto(Acierto acierto) {
        aciertos.add(acierto);
    }

    public ArrayList<Acierto> getAciertos() {
        return aciertos;
    }

    public void setAciertos(ArrayList<Acierto> aciertos) {
        this.aciertos = aciertos;
    }

    public int getPrecio() {
        return precio;
    }

    public void setPrecio(int precio) {
        this.precio = precio;
    }

    public int getJornada() {
        return jornada;
    }

    public void setJornada(int jornada) {
        this.jornada = jornada;
    }

    public ArrayList<Partido> getPartidos() {
        return partidos;
    }

    public void setPartidos(ArrayList<Partido> partidos) {
        this.partidos = partidos;
    }

    public ArrayList<Premio> getPremios() {
        return premios;
    }

    public void setPremios(ArrayList<Premio> premios) {
        this.premios = premios;
    }

    public long getRecaudacion() {
        return recaudacion;
    }

    public void setRecaudacion(long recaudacion) {
        this.recaudacion = recaudacion;
    }
}

