package com.quiniela.acdat.quinielajson;

public class Apuesta {
    String signos;

    public Apuesta (String linea){
        signos = linea;
    }

    public String getSignos() {
        return signos;
    }

    public void setSignos(String signos) {
        this.signos = signos;
    }
}
