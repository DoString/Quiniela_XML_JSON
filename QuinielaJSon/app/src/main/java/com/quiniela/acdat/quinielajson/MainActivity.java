package com.quiniela.acdat.quinielajson;

import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.SyncHttpClient;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity implements Worker.WorkerCallbacks, View.OnClickListener {
    // la aplicacion
    Quiniela quiniela;

    // cliente sincrono http
    SyncHttpClient client;

    // interfaz
    RadioButton xml, json;
    EditText res, ap, pr, in, fin;
    Button cal;
    TextView info;

    // ficheros
    static String FICHERO_MEM = "";
    static String URL_APUESTAS = "";
    static String URL_XML = "";
    static String URL_JSON = "";

    // tiempo transcurrido inicial
    Date d1;

    // multitareas
    Worker worker;
    static final int PROCESADORES = Runtime.getRuntime().availableProcessors();
    int tareaEstado = 0; // negativo = error; 2= ficheros descargados; 3= escrutinio realizado; 4= fichero escrito.

    // rango de jornadas
    int jornadaInicial, jornadaFinal;

    // dialogo tareas
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        res = (EditText) findViewById(R.id.RES);
        ap = (EditText) findViewById(R.id.AP);
        pr = (EditText) findViewById(R.id.PREM);
        in = (EditText) findViewById(R.id.IN);
        fin = (EditText) findViewById(R.id.FIN);

        xml = (RadioButton) findViewById(R.id.XML);
        json = (RadioButton) findViewById(R.id.JSON);
        info = (TextView) findViewById(R.id.INFO);
        cal = (Button) findViewById(R.id.CAL);
        cal.setOnClickListener(this);

        client = new SyncHttpClient();
        client.setMaxRetriesAndTimeout(0, 0);
        client.setConnectTimeout(5000);

        quiniela = (Quiniela) getApplicationContext();
        quiniela.getNumeroApuestas().set(0);

        worker = new Worker(PROCESADORES * 2, PROCESADORES * 2, 30L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), this);
        worker.setCallbacks(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ------------------------------------------------------------------
    //                    METODOS DE DESACRGA
    // ------------------------------------------------------------------

    private void descargarApuestas(String URL) {
        client.get(URL, new FileAsyncHttpResponseHandler(this) {
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                final Throwable t = throwable;
                switch (statusCode) {
                    case 404:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Apuestas: Error al descargar el fichero de apuestas: El fichero no existe", Toast.LENGTH_LONG).show();
                            }
                        });
                        break;
                    //Toast.makeText(MainActivity.this, "Apuestas Error al descargar los resultados de la quiniela: El fichero no existe", Toast.LENGTH_LONG).show(); break;
                    case 403:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Apuestas: Error al descargar al descargar el fichero de apuestas: Acceso denegado", Toast.LENGTH_LONG).show();
                            }
                        });
                        break;
                    //Toast.makeText(MainActivity.this, "Apuestas Error al descargar los resultados de la quiniela: Acceso denegado", Toast.LENGTH_LONG).show();break;
                    default:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Apuestas: Error al descargar el fichero de apuestas: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                        //Toast.makeText(MainActivity.this, "Apuestas Error al descargar los resultados de la quiniela: codigo " + statusCode , Toast.LENGTH_LONG).show();
                }
                tareaEstado = -1;
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, File file) {
                try {
                    leerApuestas(new FileInputStream(file));
                    tareaEstado++;
                } catch (FileNotFoundException e) {
                    // no deberia ocurrir...
                }
            }
        });
    }

    private void descargarJornadasXML(String url) {
        client.get(url, new FileAsyncHttpResponseHandler(this) {

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                final int st = statusCode;
                final Throwable t = throwable;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switch (st) {
                            case 404:
                                Toast.makeText(MainActivity.this, "XML Error al descargar los resultados de la quiniela: El fichero no existe", Toast.LENGTH_LONG).show();
                                break;
                            case 403:
                                Toast.makeText(MainActivity.this, "XML Error al descargar los resultados de la quiniela: Acceso denegado", Toast.LENGTH_LONG).show();
                                break;
                            default:
                                Toast.makeText(MainActivity.this, "XML Error al descargar los resultados de la quiniela:" + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });

                tareaEstado = -1;
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, File file) {
                try {

                    quiniela.setJornadas(Analisis.getJornadas(file));
                    //leerApuestas(getAssets().open("apuestas.txt"));
                    jornadaFinal = Math.min(quiniela.getJornadas().size(), jornadaFinal);
                    d1 = new Date();
                    tareaEstado++;
                    //res.setText("Escrutadas " + quiniela.getNumeroApuestas() + "  TIEMPO: " + diff + " segundos");
                } catch (FileNotFoundException e) {
                    Toast.makeText(MainActivity.this, "Error al procesar los resultados de la quiniela: El fichero no existe", Toast.LENGTH_SHORT).show();
                } catch (XmlPullParserException e) {
                    Toast.makeText(MainActivity.this, "Error al procesar los resultados de la quiniela", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this, "Error al leer el fichero de apuestas", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void descargarJornadasJSON(String url) {
        client.get(url, new JsonHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                final int st = statusCode;
                final Throwable t = throwable;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switch (st) {
                            case 404:
                                Toast.makeText(MainActivity.this, "JSON Error al descargar los resultados de la quiniela: El fichero no existe", Toast.LENGTH_LONG).show();
                                break;
                            case 403:
                                Toast.makeText(MainActivity.this, "JSON Error al descargar los resultados de la quiniela: Acceso denegado", Toast.LENGTH_LONG).show();
                                break;
                            default:
                                Toast.makeText(MainActivity.this, "JSON Error al descargar los resultados de la quiniela: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });

                tareaEstado = -1;
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                final int st = statusCode;
                final Throwable t = throwable;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switch (st) {
                            case 404:
                                Toast.makeText(MainActivity.this, "JSON Error al descargar los resultados de la quiniela: El fichero no existe", Toast.LENGTH_LONG).show();
                                break;
                            case 403:
                                Toast.makeText(MainActivity.this, "JSON Error al descargar los resultados de la quiniela: Acceso denegado", Toast.LENGTH_LONG).show();
                                break;
                            default:
                                Toast.makeText(MainActivity.this, "JSON Error al descargar los resultados de la quiniela: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
                tareaEstado = -1;
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                try {
                    quiniela.setJornadas(Analisis.getJornadas(response));
                    // aseguramos que la jornada final
                    // es la ultima en el array de jornadas
                    jornadaFinal = Math.min(quiniela.getJornadas().size(), jornadaFinal);
                    d1 = new Date();
                    tareaEstado++;
                } catch (JSONException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Error al procesar los datos JSON", Toast.LENGTH_SHORT).show();
                        }
                    });

                }
            }
        });
    }




    // METODOS L/E FICHEROS

    private void escribirFicheroXML() {

        try {
            Analisis.GuardarXMLEnMemoriaExterna(this.quiniela, FICHERO_MEM, jornadaInicial, jornadaFinal);
            tareaEstado++;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Se ha creado el fichero " + FICHERO_MEM + " en memoria externa", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            tareaEstado = -1;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Error al guardar el fichero XML", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void escribirFicheroJSON() {
        try {
            Analisis.GuardarJSONenMemoriaExterna(this.quiniela, FICHERO_MEM);
            tareaEstado++;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Se ha creado el fichero " + FICHERO_MEM + " en memoria externa", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException | JSONException e) {
            tareaEstado = -1;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Error al guardar el fichero JSON", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void leerApuestas(InputStream file) {
        BufferedReader br = new BufferedReader(new InputStreamReader(file));
        String linea;
        Apuesta apuesta;
        quiniela.setApuestas(new ArrayList<Apuesta>());
        try {
            while ((linea = br.readLine()) != null) {
                if (!linea.isEmpty() || linea.length() == 16) {
                    apuesta = new Apuesta(linea);
                    quiniela.getApuestas().add(apuesta);
                }
            }
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, "Error al leer el fichero apuestas", Toast.LENGTH_SHORT).show();
        }
    }




    // METODO ESCRUTAR

    private void ComprobarQuiniela(ArrayList<Jornada> jornadas, ArrayList<Apuesta> apuestas) {
        QuinielaPremiada premiada = new QuinielaPremiada();
        premiada.setJornadas(new ArrayList<Jornada>());
        String res = "";
        int aciertos = 0;

        for (int k = jornadaInicial; k < jornadaFinal; k++) {
            for (Partido partido : jornadas.get(k).getPartidos()) {
                res += partido.getResultado().getSigno();
            }
            for (int i = 0; i < apuestas.size(); i++) {
                // si coincide toda la fila es un pleno al 15
                if (res.equals(apuestas.get(i).getSignos())) {
                    aciertos = 15;
                    jornadas.get(k).getAciertos().add(new Acierto(aciertos, jornadas.get(k).getPremios().get(0).getCantidad(), apuestas.get(i).getSignos()));
                    aciertos = 0;
                    quiniela.getNumeroApuestas().getAndAdd(1);
                    continue;
                }
                // si no coincide se descarta comprobar
                // el pleno al quince, los dos ultimos caracteres
                // no son cotejados, solo puede optar a un máximo de 14 aciertos.
                char[] resultado = res.toCharArray();
                char[] apues = apuestas.get(i).getSignos().toCharArray();
                for (int j = 0; j < res.length() - 2; j++) {
                    // si vamos a comprobar el pleno al quince
                    // pero no hay 14 aciertos salimos.
                    if (resultado[j] == apues[j])
                        aciertos++;
                }

                // añadimos el acierto a la jornada si lo hay.
                if (aciertos > 9) {
                    jornadas.get(k).addAcierto(new Acierto(aciertos, jornadas.get(k).getPremios().get(15 - aciertos).getCantidad(), apuestas.get(i).getSignos()));
                }
                aciertos = 0;
                quiniela.getNumeroApuestas().getAndAdd(1);
                dialog.incrementProgressBy(1);
            }
            res = "";
            if (jornadas.get(k).getAciertos().size() > 0)
                premiada.getJornadas().add(jornadas.get(k));
        }
        // guardamos la quiniela premiada.
        quiniela.setPremiada(premiada);
        tareaEstado++;
    }





    // --------------------------------------------------------
    //                   TAREAS ASINCRONAS
    // --------------------------------------------------------

    private void tareaDescargarficheros(boolean xml) {
        dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setCancelable(false);
        dialog.setMessage("Descargando ficheros...");
        dialog.setMax(2);
        dialog.setProgress(0);
        dialog.show();

        worker.execute(new Runnable() {
            @Override
            public void run() {
                descargarApuestas(URL_APUESTAS);
            }
        });
        if (xml) {
            worker.execute(new Runnable() {
                @Override
                public void run() {
                    descargarJornadasXML(URL_XML);
                }
            });
        } else {
            worker.execute(new Runnable() {
                @Override
                public void run() {
                    descargarJornadasJSON(URL_JSON);
                }
            });
        }
    }

    private void tareaEscrutar() {
        dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setCancelable(false);
        dialog.setMessage("Escrutando...");
        dialog.setMax(quiniela.getApuestas().size() * (jornadaFinal - jornadaInicial));
        dialog.setProgress(0);
        dialog.show();
        quiniela.setNumeroApuestas(new AtomicInteger(0));


        worker.execute(new Runnable() {
            @Override
            public void run() {
                ComprobarQuiniela(quiniela.getJornadas(), quiniela.getApuestas());
            }
        });
    }

    private void tareaEscribir(boolean xml) {
        dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setCancelable(false);
        dialog.setMessage("Guardando Ficheros...");
        dialog.setMax(1);
        dialog.setProgress(0);
        dialog.show();

        if (xml) {
            worker.execute(new Runnable() {
                @Override
                public void run() {
                    escribirFicheroXML();
                }
            });
        } else {
            worker.execute(new Runnable() {
                @Override
                public void run() {
                    escribirFicheroJSON();
                }
            });
        }
    }





    // -----------------------------------------------------------
    //                         CALLBACKS
    // -----------------------------------------------------------
    @Override
    public void OnIniciarTarea(Thread t, Runnable r) {

    }

    @Override
    public void OnfinalizadaTarea(Throwable t, Runnable r) {

        switch (tareaEstado){
            case 1: dialog.incrementProgressBy(1); break;
            case 2:
                dialog.incrementProgressBy(1);
                dialog.dismiss();
                // comprobamos si se han descargado los ficheros
                if (quiniela.getApuestas() == null || quiniela.getApuestas().size() <= 0)
                    return;
                if (quiniela.getJornadas() == null || quiniela.getJornadas().size() <= 0)
                    return;
                tareaEscrutar();
                break;
            case 3:
                dialog.dismiss();
                if (quiniela.getNumeroApuestas().get() > 0) {
                    Date d2 = new Date();
                    info.setText("Total apuestas escrutadas: " + quiniela.getNumeroApuestas().get() + " Tiempo: " + (d2.getTime() - d1.getTime()) / 1000 + " segundos");
                    tareaEscribir(xml.isChecked());
                }else {
                    info.setText("No se ha escrutado ninguna quiniela... quizás esa jornada todavia no se ha jugado?");
                    cal.setEnabled(true);
                }
                break;
            case 4:
                dialog.incrementProgressBy(1);
                dialog.dismiss();
                break;
            default:
                if (dialog.isShowing())
                    dialog.dismiss();
                break;

        }

        if (tareaEstado <= 0 || tareaEstado == 4)
            cal.setEnabled(true);
    }

    @Override
    public void OnWorkerTerminado() {

    }

    @Override
    public void onClick(View v) {
        if (v == cal) {
            quiniela.setApuestas(null);
            quiniela.setJornadas(null);
            quiniela.setNumeroApuestas(new AtomicInteger(0));
            tareaEstado = 0;
            String URL = res.getText().toString();
            String AP = ap.getText().toString();
            String PR = pr.getText().toString();
            URI uri;
            if (this.in.getText().toString().isEmpty() ||
                    this.fin.getText().toString().isEmpty()) {
                Toast.makeText(MainActivity.this, "Debes seleccionar un rango de jornadas", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                jornadaInicial = Integer.parseInt(this.in.getText().toString()) - 1;
                jornadaFinal = Integer.parseInt(this.fin.getText().toString());
                if (jornadaInicial < 0 || jornadaFinal < 1 || (jornadaFinal - jornadaInicial) <= 0) {
                    Toast.makeText(MainActivity.this, "Rango de jornadas inválido", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(MainActivity.this, "Rango de jornadas inválido", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                if (!Uri.parse(AP).getLastPathSegment().contains(".txt")) {
                    Toast.makeText(MainActivity.this, "Ruta de apuestas incorrecta, el fichero debe tener extension txt", Toast.LENGTH_SHORT).show();
                    return;
                }
                uri = new URI(AP);
                URL_APUESTAS = AP;
            } catch (URISyntaxException | NullPointerException e) {
                Toast.makeText(MainActivity.this, "Formato de URL incorrecto: Fichero apuestas", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                uri = new URI(URL);
            } catch (URISyntaxException e) {
                Toast.makeText(MainActivity.this, "Formato de URL incorrecto: Fichero resultados", Toast.LENGTH_SHORT).show();
                return;
            }
            if (xml.isChecked()) {
                try {
                    if (!Uri.parse(URL).getLastPathSegment().contains(".xml") ||
                            !PR.contains(".xml")) {
                        Toast.makeText(MainActivity.this, "el fichero debe tener extension xml", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NullPointerException e) {
                    Toast.makeText(MainActivity.this, "el fichero debe tener extension xml", Toast.LENGTH_SHORT).show();
                    return;
                }
                FICHERO_MEM = PR;
                URL_XML = URL;
            } else if (json.isChecked()) {
                try {
                    if (!Uri.parse(URL).getLastPathSegment().contains("json") ||
                            !PR.contains(".json")) {
                        Toast.makeText(MainActivity.this, "el fichero debe tener extension json", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "el fichero debe tener extension json", Toast.LENGTH_SHORT).show();
                    return;
                }
                FICHERO_MEM = PR;
                URL_JSON = URL;
            }
            tareaDescargarficheros(xml.isChecked());
            cal.setEnabled(false);
        }
    }
}
