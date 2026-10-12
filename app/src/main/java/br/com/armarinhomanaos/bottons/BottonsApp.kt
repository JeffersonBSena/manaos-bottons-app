package br.com.armarinhomanaos.bottons

import android.app.Application
import br.com.armarinhomanaos.bottons.data.Network
import br.com.armarinhomanaos.bottons.data.Prefs
import br.com.armarinhomanaos.bottons.data.api.BottonsApi

/** Service locator mínimo (como o Graph do Campanha 2026). */
object Graph {
    lateinit var prefs: Prefs
    @Volatile var api: BottonsApi? = null
        private set

    fun api(): BottonsApi = api ?: Network.create(prefs).also { api = it }
    /** Depois de trocar URL/token. */
    fun reconstruir() { api = Network.create(prefs) }
}

class BottonsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.prefs = Prefs(this)
    }
}
