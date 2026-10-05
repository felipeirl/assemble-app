package dev.assemble.app;

import android.app.Application;

import dev.assemble.app.core.network.ImageLoading;
import dev.assemble.app.data.local.FirebaseInit;

/**
 * Application: inicializa o Firebase manual (sem google-services.json real em DEV —
 * ver docs/setup-firebase.md) e cria o {@link AppContainer} usado pela UI em Compose.
 */
public class MyApplication extends Application {
    private AppContainer container;

    @Override
    public void onCreate() {
        super.onCreate();
        ImageLoading.install();
        FirebaseInit.init(this);
        container = new AppContainer(this);
    }

    public AppContainer getContainer() {
        return container;
    }
}
