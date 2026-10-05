package dev.assemble.app.core.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * Configuração do cliente Firebase lida do `local.properties` (via BuildConfig), sem
 * `google-services.json` no repositório. Não é segredo: identifica o projeto para o login.
 */
data class FirebaseSettings(val apiKey: String, val applicationId: String, val projectId: String) {
    val isComplete: Boolean get() = apiKey.isNotBlank() && applicationId.isNotBlank() && projectId.isNotBlank()
}

object FirebaseSetup {
    /** App Firebase padrão; inicializa uma vez com [settings]. */
    fun initialize(context: Context, settings: FirebaseSettings): FirebaseApp {
        FirebaseApp.getApps(context).firstOrNull()?.let { return it }
        val options = FirebaseOptions.Builder()
            .setApiKey(settings.apiKey)
            .setApplicationId(settings.applicationId)
            .setProjectId(settings.projectId)
            .build()
        return FirebaseApp.initializeApp(context, options)
    }
}
