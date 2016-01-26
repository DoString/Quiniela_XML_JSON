package com.quiniela.acdat.quinielajson;

import android.app.Activity;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Worker extends ThreadPoolExecutor {
    WorkerCallbacks callbacks;
    Activity activity;
    public Worker(int tamanioPool, int tamanioPoolMAX, long TiempoDeVidaDeHilosIDDLE, TimeUnit unidadTiempo, BlockingQueue<Runnable> ColaEspera, Activity activity) {
        super(tamanioPool, tamanioPoolMAX, TiempoDeVidaDeHilosIDDLE, unidadTiempo, ColaEspera);
        this.activity = activity;
    }


    public void setCallbacks(WorkerCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        final Thread th = t;
        final Runnable rn =r;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                callbacks.OnIniciarTarea(th, rn);
            }
        });
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        final Throwable tr = t;
        final Runnable rn =r;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                callbacks.OnfinalizadaTarea(tr, rn);
            }
        });

    }

    @Override
    protected void terminated() {
        super.terminated();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                callbacks.OnWorkerTerminado();
            }
        });
    }

    public interface WorkerCallbacks {
        void OnWorkerTerminado();
        void OnIniciarTarea(Thread t, Runnable r);
        void OnfinalizadaTarea(Throwable t, Runnable r);
    }
}

