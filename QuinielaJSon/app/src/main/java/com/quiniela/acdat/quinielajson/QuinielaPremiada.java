package com.quiniela.acdat.quinielajson;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class QuinielaPremiada {
    @Expose
    String jornada;
    @Expose
    long totalGanado;
    @Expose
    long totalInvertido;
    @SerializedName("quinielas")
    @Expose
    ArrayList<Jornada> jornadas;

    public long getTotalInvertido() {
        return totalInvertido;
    }

    public void setTotalInvertido(long totalInvertido) {
        this.totalInvertido = totalInvertido;
    }

    public ArrayList<Jornada> getJornadas() {
        return jornadas;
    }

    public void setJornadas(ArrayList<Jornada> jornadas) {
        this.jornadas = jornadas;
    }

    public long getTotalGanado() {
        return totalGanado;
    }

    public void setTotalGanado(long totalGanado) {
        this.totalGanado = totalGanado;
    }

    public String getJornada() {
        return jornada;
    }

    public void setJornada(String jornada) {
        this.jornada = jornada;
    }

}
