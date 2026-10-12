package br.com.armarinhomanaos.bottons.ui

import android.content.Context
import android.net.Uri
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import br.com.armarinhomanaos.bottons.ui.theme.AppColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * Tela de descanso do stand: vídeo em loop, tela cheia, sem som. Qualquer toque fecha.
 * O vídeo é baixado uma vez para o cache (reaproveitado por 24 h) e tocado do arquivo local,
 * então o loop não gasta rede. Sem vídeo (offline / URL vazia) cai numa animação da marca.
 */
object DescansoVideo {
    private val http = OkHttpClient()

    fun arquivo(ctx: Context) = File(ctx.cacheDir, "descanso.mp4")

    /** Garante o arquivo local; devolve-o ou null se não conseguiu. Chamar fora da main thread. */
    suspend fun garantir(ctx: Context, url: String): File? = withContext(Dispatchers.IO) {
        val f = arquivo(ctx)
        val fresco = f.exists() && f.length() > 0 && System.currentTimeMillis() - f.lastModified() < 24L * 3600_000
        if (fresco) return@withContext f
        if (url.isBlank()) return@withContext f.takeIf { it.exists() && it.length() > 0 }
        runCatching {
            http.newCall(Request.Builder().url(url).build()).execute().use { r ->
                if (!r.isSuccessful) error("HTTP ${r.code}")
                val tmp = File(ctx.cacheDir, "descanso.tmp")
                tmp.outputStream().use { out -> r.body!!.byteStream().copyTo(out) }
                if (tmp.length() < 1024) error("vazio")
                tmp.renameTo(f)
            }
            f
        }.getOrElse { f.takeIf { it.exists() && it.length() > 0 } }  // offline: usa o que tiver
    }
}

@Composable
fun DescansoOverlay(visivel: Boolean, aoTocar: () -> Unit) {
    if (!visivel) return
    val ctx = LocalContext.current
    var arquivo by remember { mutableStateOf<File?>(null) }
    var falhou by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        arquivo = DescansoVideo.garantir(ctx, br.com.armarinhomanaos.bottons.Graph.prefs.videoUrl)
        if (arquivo == null) falhou = true
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .pointerInput(Unit) { awaitPointerEventScope { while (true) { awaitPointerEvent(); aoTocar() } } },
    ) {
        val f = arquivo
        if (f != null && !falhou) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { c -> VideoCover(c, f) { falhou = true } },
                onRelease = { it.liberar() },
            )
        } else {
            AnimacaoMarca()
        }
        // Rodapé convidativo (sempre, por cima do vídeo)
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                .padding(top = 80.dp, bottom = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Pulso { alpha ->
                Text("Toque na tela para fazer seu botton", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = alpha))
            }
            Text("Bottons Manaós • a partir de 5 unidades", fontSize = 15.sp, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun Pulso(conteudo: @Composable (Float) -> Unit) {
    val t = rememberInfiniteTransition(label = "pulso")
    val a by t.animateFloat(0.55f, 1f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "alpha")
    conteudo(a)
}

/** Fallback sem vídeo: gradiente da marca girando devagar + botton pulsando. */
@Composable
private fun AnimacaoMarca() {
    val t = rememberInfiniteTransition(label = "marca")
    val ang by t.animateFloat(0f, 360f, infiniteRepeatable(tween(16000, easing = LinearEasing)), label = "ang")
    val esc by t.animateFloat(0.9f, 1.05f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "esc")
    val rad = Math.toRadians(ang.toDouble())
    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(
                listOf(AppColor.NavyDark, AppColor.Navy, AppColor.PrimaryDark, AppColor.Primary),
                start = androidx.compose.ui.geometry.Offset((1000 + 1000 * Math.cos(rad)).toFloat(), (1000 + 1000 * Math.sin(rad)).toFloat()),
                end = androidx.compose.ui.geometry.Offset((1000 - 1000 * Math.cos(rad)).toFloat(), (1000 - 1000 * Math.sin(rad)).toFloat()),
            ),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
            Box(Modifier.size((260 * esc).dp).background(Color.White, androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                Box(Modifier.size((200 * esc).dp).background(AppColor.Primary, androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                    Box(Modifier.size((130 * esc).dp).background(Color.White, androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                        Box(Modifier.size((70 * esc).dp).background(AppColor.Orange, androidx.compose.foundation.shape.CircleShape))
                    }
                }
            }
            Text("Seu botton na hora ✨", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Sua foto, sua arte, seu time. Pronto em minutos.", fontSize = 20.sp, color = Color.White.copy(alpha = 0.85f))
        }
    }
}

/**
 * Vídeo em "cover" (preenche a tela cortando as bordas), como o `resizeMode=zoom` do ExoPlayer,
 * sem dependência extra: TextureView + MediaPlayer, com a matriz de escala recalculada no layout.
 * (VideoView só faz letterbox.)
 */
private class VideoCover(ctx: Context, private val arquivo: File, private val aoFalhar: () -> Unit) : TextureView(ctx), TextureView.SurfaceTextureListener {
    private var mp: MediaPlayer? = null
    private var vw = 0
    private var vh = 0

    init { surfaceTextureListener = this }

    override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
        runCatching {
            mp = MediaPlayer().apply {
                setDataSource(arquivo.absolutePath)
                setSurface(Surface(st))
                isLooping = true
                setVolume(0f, 0f)
                setOnVideoSizeChangedListener { _, cw, ch -> vw = cw; vh = ch; ajustar() }
                setOnPreparedListener { it.start() }
                setOnErrorListener { _, _, _ -> aoFalhar(); true }
                prepareAsync()
            }
        }.onFailure { aoFalhar() }
    }

    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) = ajustar()
    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean { liberar(); return true }
    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}

    private fun ajustar() {
        if (vw == 0 || vh == 0 || width == 0 || height == 0) return
        val escala = maxOf(width.toFloat() / vw, height.toFloat() / vh)
        val sx = vw * escala / width
        val sy = vh * escala / height
        setTransform(Matrix().apply { setScale(sx, sy, width / 2f, height / 2f) })
    }

    fun liberar() { runCatching { mp?.stop(); mp?.release() }; mp = null }
}
