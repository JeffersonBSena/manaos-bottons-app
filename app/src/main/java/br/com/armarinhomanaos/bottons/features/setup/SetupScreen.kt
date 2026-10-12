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
    var erro by remember { mutableStateOf<String?>(null) }
    var carregando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(containerColor = AppColor.Bg, topBar = { BarraTopo("Configuração", aoVoltar) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Marca (BrandMark do Campanha 2026)
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(72.dp).clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(AppColor.Primary, AppColor.PrimaryDark))), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Verified, null, tint = Color.White, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.height(10.dp))
                Text("Bottons Manaós", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                Text("Tablet do stand • conecte ao servidor", fontSize = 13.sp, color = AppColor.TextSecond)
            }
            Column(Modifier.cardApp(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AppTextField("Servidor *", url, { url = it }, placeholder = "https://armarinhomanaos.com.br", dica = "Endereço do site; o /api é acrescentado sozinho.")
                AppTextField("Token do quiosque *", token, { token = it }, placeholder = "1|abc…", senha = true,
                    dica = "No servidor: php artisan kiosk:token \"Tablet 1\"", erro = erro)
                BotaoPrimario("Testar e salvar", carregando = carregando, habilitado = token.isNotBlank(), onClick = {
                    erro = null; carregando = true
                    prefs.baseUrl = url; prefs.token = token
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
