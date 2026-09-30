package dev.assemble.app;

import android.app.Application;

import dev.assemble.app.data.local.FirebaseInit;

/**
 * Application da camada de dados (Nexo).
 * Sem Activity: UI pertence ao Vetor. Apenas inicializa o Firebase manual
 * (sem google-services.json real em DEV — ver docs/setup-firebase.md).
 */
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseInit.init(this);
    }
}
