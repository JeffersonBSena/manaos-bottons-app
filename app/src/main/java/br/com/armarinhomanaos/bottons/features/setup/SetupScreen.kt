package br.com.armarinhomanaos.bottons.features.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.armarinhomanaos.bottons.Graph
import br.com.armarinhomanaos.bottons.ui.AppTextField
import br.com.armarinhomanaos.bottons.ui.BarraTopo
import br.com.armarinhomanaos.bottons.ui.BotaoPrimario
import br.com.armarinhomanaos.bottons.ui.cardApp
import br.com.armarinhomanaos.bottons.ui.theme.AppColor
import kotlinx.coroutines.launch

/** Primeiro acesso: URL do servidor + token do quiosque (gerado no servidor com `kiosk:token`). */
@Composable
fun SetupScreen(aoPronto: () -> Unit, aoVoltar: (() -> Unit)?) {
    val prefs = Graph.prefs
    var url by remember { mutableStateOf(prefs.baseUrl) }
    var token by remember { mutableStateOf(prefs.token.orEmpty()) }
    var video by remember { mutableStateOf(prefs.videoUrl) }
    var descanso by remember { mutableStateOf(prefs.descansoSegundos.toString()) }
    var pin by remember { mutableStateOf(prefs.pinAtendente) }
    var erro by remember { mutableStateOf<String?>(null) }
    var carregando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val ctx = androidx.compose.ui.platform.LocalContext.current

    Scaffold(containerColor = AppColor.Bg, topBar = { BarraTopo("Configuração", aoVoltar) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Marca (BrandMark do Campanha 2026)
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(br.com.armarinhomanaos.bottons.R.drawable.logo_manaos), "Manaós", Modifier.size(140.dp))
                Spacer(Modifier.height(10.dp))
                Text("Bottons Manaós", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                Text("Tablet do stand • conecte ao servidor", fontSize = 13.sp, color = AppColor.TextSecond)
            }
            Column(Modifier.cardApp(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AppTextField("Servidor *", url, { url = it }, placeholder = "https://armarinhomanaos.com.br", dica = "Endereço do site; o /api é acrescentado sozinho.")
                AppTextField("Token do quiosque *", token, { token = it }, placeholder = "1|abc…", senha = true,
                    dica = "No servidor: php artisan kiosk:token \"Tablet 1\"", erro = erro)
                AppTextField("PIN do atendente *", pin, { pin = it.filter { c -> c.isDigit() }.take(6) }, tipo = androidx.compose.ui.text.input.KeyboardType.NumberPassword,
                    placeholder = "1234", dica = "Libera a fila de pedidos (cadeado na Início). Os clientes não veem os pedidos dos outros.")
                AppTextField("Vídeo da tela de descanso", video, { video = it }, placeholder = "https://…/descanso.mp4",
                    dica = "MP4 público (H.264). Vazio = animação da marca. É baixado uma vez e toca em loop sem som.")
                AppTextField("Descanso após (segundos)", descanso, { descanso = it.filter { c -> c.isDigit() }.take(4) },
                    tipo = androidx.compose.ui.text.input.KeyboardType.Number, placeholder = "90", dica = "Sem toque na Início por esse tempo → vídeo. 0 desliga. A tela nunca apaga com o app aberto.")
                BotaoPrimario("Testar e salvar", carregando = carregando, habilitado = token.isNotBlank(), onClick = {
                    erro = null; carregando = true
                    prefs.baseUrl = url; prefs.token = token
                    prefs.videoUrl = video; prefs.descansoSegundos = descanso.toIntOrNull() ?: 90
                    prefs.pinAtendente = pin
                    br.com.armarinhomanaos.bottons.ui.DescansoVideo.arquivo(ctx).delete() // força baixar o vídeo novo
                    Graph.reconstruir()
                    scope.launch {
                        runCatching { Graph.api().produtos() }
                            .onSuccess { carregando = false; aoPronto() }
                            .onFailure { carregando = false; erro = "Não conectou: ${it.message?.take(80)}" }
                    }
                })
            }
        }
    }
}
