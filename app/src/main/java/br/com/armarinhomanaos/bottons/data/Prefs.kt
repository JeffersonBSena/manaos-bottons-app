package br.com.armarinhomanaos.bottons.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** URL do servidor (prefs simples) + token do quiosque (cifrado, como o TokenStore do Campanha 2026). */
class Prefs(private val ctx: Context) {
    private val simples = ctx.getSharedPreferences("bottons", Context.MODE_PRIVATE)

    private val seguro = run {
        val key = MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        val criar = {
            EncryptedSharedPreferences.create(ctx, "bottons_secure", key,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
        }
        // Backup restaurado de outro aparelho → chave de Keystore inexistente → crash em loop. Apaga e recria.
        runCatching { criar() }.getOrElse { ctx.deleteSharedPreferences("bottons_secure"); criar() }
    }

    var baseUrl: String
        get() = simples.getString("base_url", URL_PADRAO) ?: URL_PADRAO
        set(v) = simples.edit().putString("base_url", normalizar(v)).apply()

    var token: String?
        get() = seguro.getString("token", null)
        set(v) = seguro.edit().apply { if (v == null) remove("token") else putString("token", v.trim()) }.apply()

    val configurado: Boolean get() = !token.isNullOrBlank()

    /** Vídeo da tela de descanso (mp4 público no servidor; troca sem recompilar). */
    var videoUrl: String
        get() = simples.getString("video_url", VIDEO_PADRAO) ?: VIDEO_PADRAO
        set(v) = simples.edit().putString("video_url", v.trim()).apply()

    /** Segundos sem toque na Início até a tela de descanso. 0 = desligada. */
    var descansoSegundos: Int
        get() = simples.getInt("descanso_s", 90)
        set(v) = simples.edit().putInt("descanso_s", v.coerceIn(0, 3600)).apply()

    companion object {
        const val URL_PADRAO = "https://armarinhomanaos.com.br/api/"
        const val VIDEO_PADRAO = "https://armarinhomanaos.com.br/kiosk/descanso.mp4"
        fun normalizar(u: String): String {
            var s = u.trim()
            if (!s.startsWith("http")) s = "https://$s"
            if (!s.endsWith("/")) s += "/"
            if (!s.endsWith("api/")) s += "api/"
            return s
        }
    }
}
