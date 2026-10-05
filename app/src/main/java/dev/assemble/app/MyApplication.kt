package dev.assemble.app

import android.app.Application
import dev.assemble.app.core.network.ImageLoading

/** Application: instala o carregador de imagens e cria o [AppContainer] usado pela UI em Compose. */
class MyApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        ImageLoading.install()
        container = AppContainer(this)
    }
}
