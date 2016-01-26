package com.quiniela.acdat.quinielajson;

import com.google.gson.annotations.Expose;


public class Acierto {
    @Expose
    int numeroAciertos;
    @Expose
    long premio;
    @Expose
    String columna;

    public Acierto(int numero, long recaudacion, String columna) {
        numeroAciertos=numero;
        this.premio =recaudacion;
        this.columna = columna;
    }

    public int getNumeroAciertos() {
        return numeroAciertos;
    }

    public void setNumeroAciertos(int numeroAciertos) {
        this.numeroAciertos = numeroAciertos;
    }

    public long getPremio() {
        return premio;
    }

    public void setPremio(long premio) {
        this.premio = premio;
    }

    public String getColumna() {
        return columna;
    }

    public void setColumna(String columna) {
        this.columna = columna;
    }
}
