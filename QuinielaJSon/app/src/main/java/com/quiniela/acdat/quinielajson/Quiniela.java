package com.quiniela.acdat.quinielajson;

import android.app.Application;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Quiniela extends Application{
    private QuinielaPremiada premiada;
    public ArrayList<Jornada> jornadas;
    public ArrayList<Apuesta> apuestas;
    public long coste;
    public AtomicInteger numeroApuestas = new AtomicInteger(0);

    public QuinielaPremiada getPremiada() {
        return premiada;
    }

    public void setPremiada(QuinielaPremiada premiada) {
        this.premiada = premiada;
    }

    public void setNumeroApuestas(AtomicInteger numeroApuestas) {
        this.numeroApuestas = numeroApuestas;
    }

    public ArrayList<Apuesta> getApuestas() {
        return apuestas;
    }

    public void setApuestas(ArrayList<Apuesta> apuestas) {
        this.apuestas = apuestas;
    }

    public ArrayList<Jornada> getJornadas() {
        return jornadas;
    }

    public void setJornadas(ArrayList<Jornada> jornadas) {
        this.jornadas = jornadas;
    }

    public long getCoste() {
        return coste;
    }

    public void setCoste(long coste) {
        this.coste = coste;
    }

    public AtomicInteger getNumeroApuestas() {
        return numeroApuestas;
    }
}
