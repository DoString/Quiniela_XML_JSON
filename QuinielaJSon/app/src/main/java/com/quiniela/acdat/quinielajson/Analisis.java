package com.quiniela.acdat.quinielajson;

import android.os.Environment;
import android.util.Xml;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class Analisis {
    public static ArrayList<Jornada> getJornadas(File file) throws IOException, XmlPullParserException {
        ArrayList<Jornada> jornadas = new ArrayList<>();
        XmlPullParser xmlPullParser = Xml.newPullParser();
        xmlPullParser.setInput(new FileReader(file));

        Jornada jornada = null;
        Partido partido;
        Resultado resultado;
        Premio premio;

        int event = xmlPullParser.getEventType();

        while (event != XmlPullParser.END_DOCUMENT) {
            switch (event) {
                case XmlPullParser.START_TAG:
                    if (xmlPullParser.getName().equalsIgnoreCase("quiniela")) {
                        jornada = new Jornada();
                        jornada.setJornada(Integer.parseInt(xmlPullParser.getAttributeValue(0)));
                        jornada.setPrecio(Integer.parseInt(xmlPullParser.getAttributeValue(9)));
                        for (int i = 0; i < xmlPullParser.getAttributeCount() - 4; i++) {
                            premio = new Premio();
                            premio.id = 15 - i;
                            premio.cantidad = Long.parseLong(xmlPullParser.getAttributeValue(i + 3));
                            jornada.getPremios().add(premio);
                        }
                    }
                    if (xmlPullParser.getName().equalsIgnoreCase("partit")) {
                        partido = new Partido();
                        partido.setNumero(Integer.parseInt(xmlPullParser.getAttributeValue(0)));
                        partido.setLocal(xmlPullParser.getAttributeValue(1));
                        partido.setVisitante(xmlPullParser.getAttributeValue(2));
                        resultado = new Resultado();

                        resultado.setSigno(xmlPullParser.getAttributeValue(6));
                        if (resultado.getSigno().isEmpty()) {
                            // este es el fin de documento
                            // que es alcanzado cuando signo vale ""
                            // es decir no hay mas resultados
                            event = XmlPullParser.END_DOCUMENT;
                            continue;
                        }

                        partido.setResultado(resultado);
                        if (jornada != null)
                            jornada.getPartidos().add(partido);
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (xmlPullParser.getName().equals("quiniela"))
                        jornadas.add(jornada);
                    break;
            }
            event = xmlPullParser.next();
        }
        return jornadas;
    }

    public static ArrayList<Jornada> getJornadas(JSONObject object) throws JSONException {
        JSONArray quiniela = object.getJSONObject("quinielista").getJSONArray("quiniela");
        Jornada jornada;
        Premio premio;
        Partido partido;
        JSONArray partit;
        Resultado resultado;
        ArrayList<Jornada> jornadas = new ArrayList<>();
        // cada objeto del array quiniela es una jornada
        for (int i = 0; i < quiniela.length(); i++) {
            jornada = new Jornada();
            jornada.setJornada(Integer.parseInt(quiniela.getJSONObject(i).getString("_jornada")));
            jornada.setRecaudacion(Long.parseLong(quiniela.getJSONObject(i).getString("_recaudacion")));
            ArrayList<Premio> premios = new ArrayList<>();
            // son 6 premios
            String[] ps = new String[]{"_el15", "_el14", "_el13", "_el12", "_el11", "_el10"};
            for (int j = 0; j < ps.length; j++) {
                premio = new Premio();
                premio.setCantidad(Long.parseLong(quiniela.getJSONObject(i).getString(ps[j])));
                premio.setId(15 - j);
                premios.add(premio);
            }
            jornada.setPremios(premios);
            // fin premios
            jornada.setPrecio(Integer.parseInt(quiniela.getJSONObject(i).getString("_apuesta")));
            // partidos
            partit = quiniela.getJSONObject(i).getJSONArray("partit");
            ArrayList<Partido> partidos = new ArrayList<>();
            for (int k = 0; k < partit.length(); k++) {
                partido = new Partido();
                partido.setLocal(partit.getJSONObject(k).getString("_equipo1"));
                partido.setVisitante(partit.getJSONObject(k).getString("_equipo2"));
                resultado = new Resultado();
                resultado.setSigno(partit.getJSONObject(k).getString("_sig"));
                // fin de jornadas
                if (resultado.getSigno().isEmpty()) {
                    return jornadas;
                }
                partido.setResultado(resultado);
                partidos.add(partido);
            }
            jornada.setPartidos(partidos);
            // fin partidos
            jornadas.add(jornada);
        }
        return jornadas;
    }
    public static void GuardarXMLEnMemoriaExterna(Quiniela quiniela, String f, int in, int fin) throws IOException {
        if (quiniela.getPremiada().getJornadas() == null || quiniela.getPremiada().getJornadas().size() <= 0)
            return;
        long total = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("<QuinielasPremiadas>\n");
        for(int i=in; i < fin; i++){
            long totalCantidad = 0;
            if (quiniela.getJornadas().get(i).getAciertos().size() > 0) {
                for (Acierto a : quiniela.getJornadas().get(i).getAciertos()) {
                    totalCantidad += a.getPremio();
                }
                sb.append("\t<Quiniela jornada=" + "\"" + quiniela.getJornadas().get(i).getJornada()+"\" " + "aciertos=" +"\"" + quiniela.getJornadas().get(i).getAciertos().size() +"\" ");
                sb.append("totalGanado=" + "\"" +totalCantidad +"\" >\n");
                total += totalCantidad;
                for (Acierto a : quiniela.getJornadas().get(i).getAciertos()) {
                    sb.append("\t\t<Acierto apuesta=" + "\""+a.getColumna()+"\" " + "numAciertos=" + "\""+a.getNumeroAciertos()+"\" " + "ganado=" + "\""+a.getPremio()+"\" />\n" );
                }
                sb.append("\t</Quiniela>");
            }

        }
        sb.append("\t<TotalGanado cantidad=" + "\"" + total +"\" />\n");
        sb.append("\t<TotalInvertido cantidad=" + "\"" + quiniela.getJornadas().get(0).getPrecio() * quiniela.getApuestas().size() +"\" />\n");
        sb.append("</QuinielasPremiadas>");
        OutputStreamWriter out;
        File miFichero, tarjeta;
        tarjeta = Environment.getExternalStorageDirectory();
        miFichero = new File(tarjeta.getAbsolutePath(), f);
        out = new FileWriter(miFichero);
        out.write(sb.toString());
        out.close();

    }
    public static void GuardarJSONenMemoriaExterna(Quiniela quiniela, String f) throws IOException, JSONException {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        builder.excludeFieldsWithoutExposeAnnotation();
        Gson gson = builder.create();
        QuinielaPremiada premiada = quiniela.getPremiada();
        long total = 0;
        for(Jornada jornada : premiada.getJornadas()){
            long totalCantidad = 0;
            if (jornada.getAciertos().size() > 0){
                for (Acierto a : jornada.getAciertos()) {
                    totalCantidad += a.getPremio();
                    total += totalCantidad;
                }
                jornada.setTotalGanado(totalCantidad);
            }
        }
        premiada.setTotalGanado(total);
        premiada.setTotalInvertido(quiniela.getJornadas().get(0).getPrecio() * quiniela.getApuestas().size());
        OutputStreamWriter out;
        File miFichero, tarjeta;
        tarjeta = Environment.getExternalStorageDirectory();
        miFichero = new File(tarjeta.getAbsolutePath(), f);
        out = new FileWriter(miFichero);

        String content = gson.toJson(premiada);
        out.write(content);
        out.close();
    }
}
