package br.com.armarinhomanaos.bottons.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Sistema / Claro / Escuro — igual ao Campanha 2026 (prefs "bottons", chave "aparencia"). */
enum class Aparencia(val titulo: String) {
    SISTEMA("Sistema"), CLARO("Claro"), ESCURO("Escuro");

    companion object {
        fun deChave(v: String?): Aparencia = entries.firstOrNull { it.name.equals(v, ignoreCase = true) } ?: SISTEMA
    }
}

object AparenciaStore {
    private const val CHAVE = "aparencia"
    private const val PREFS = "bottons"

    var atual by mutableStateOf(Aparencia.SISTEMA)
        private set
    private var carregado = false

    fun iniciar(ctx: Context) {
        if (carregado) return
        carregado = true
        atual = Aparencia.deChave(ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CHAVE, null))
    }

    fun definir(ctx: Context, a: Aparencia) {
        atual = a
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(CHAVE, a.name.lowercase()).apply()
    }

    fun proxima(ctx: Context) {
        val e = Aparencia.entries
        definir(ctx, e[(atual.ordinal + 1) % e.size])
    }
}
