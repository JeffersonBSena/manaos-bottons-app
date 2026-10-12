package br.com.armarinhomanaos.bottons.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.armarinhomanaos.bottons.Graph
import br.com.armarinhomanaos.bottons.data.api.Pedido
import br.com.armarinhomanaos.bottons.ui.*
import br.com.armarinhomanaos.bottons.ui.theme.AparenciaStore
import br.com.armarinhomanaos.bottons.ui.theme.AppColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun corStatus(status: String): Color = when (status) {
    "awaiting_art", "awaiting_approval" -> AppColor.FarolPend
    "pending_payment" -> AppColor.Orange
    "paid", "in_production", "quality_check", "packing" -> AppColor.FarolInfo
    "ready_for_pickup", "ready_to_ship", "delivered", "completed" -> AppColor.FarolOk
    "rework", "cancelled", "refunded" -> AppColor.FarolNao
    else -> AppColor.Slate
}

@Composable
fun HomeScreen(aoNovoPedido: () -> Unit, aoAbrirPedido: (String) -> Unit, aoConfigurar: () -> Unit) {
    val ctx = LocalContext.current
    var pedidos by remember { mutableStateOf<List<Pedido>>(emptyList()) }
    var erro by remember { mutableStateOf<String?>(null) }
    var carregando by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    suspend fun carregar() {
        runCatching { Graph.api().pedidos() }
            .onSuccess { pedidos = it; erro = null }
            .onFailure { erro = "Sem conexão com o servidor" }
        carregando = false
    }
    LaunchedEffect(Unit) { while (true) { carregar(); delay(8000) } }

    Scaffold(containerColor = AppColor.Bg, topBar = {
        BarraTopo("Bottons", acoes = {
            IconButton(onClick = { AparenciaStore.proxima(ctx) }) { Icon(Icons.Filled.DarkMode, "Aparência: ${AparenciaStore.atual.titulo}") }
            IconButton(onClick = { scope.launch { carregar() } }) { Icon(Icons.Filled.Refresh, "Atualizar") }
            IconButton(onClick = aoConfigurar) { Icon(Icons.Filled.Settings, "Configuração") }
        })
    }) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                // Hero (gradiente da marca) + CTA
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(AppColor.Navy, AppColor.PrimaryDark))).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text("Seu botton na hora ✨", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Escolha o tamanho, mande a foto e pague no Pix. Pronto em minutos.", fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f))
                    BotaoPrimario("Novo pedido", aoNovoPedido, cor = AppColor.Orange, icone = { Icon(Icons.Filled.Add, null, tint = Color.White) })
                }
            }
            item { TituloSecao("Pedidos em andamento", direita = if (pedidos.isNotEmpty()) "${pedidos.size}" else null) }
            if (carregando) item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppColor.Primary) } }
            else if (erro != null) item { CartaoSecao { Text(erro!!, color = AppColor.Red, modifier = Modifier.padding(16.dp)) } }
            else if (pedidos.isEmpty()) item { CartaoSecao { Text("Nenhum pedido aberto. Toque em Novo pedido.", color = AppColor.TextSecond, modifier = Modifier.padding(16.dp)) } }
            else item {
                CartaoSecao {
                    pedidos.forEachIndexed { i, p ->
                        if (i > 0) DivisorLinha()
                        LinhaFarol(corStatus(p.status), p.customer.name ?: p.number, "${p.number} · ${p.units} bottons · ${p.total}",
                            onClick = { aoAbrirPedido(p.number) }) {
                            Pilula(p.status_label, corStatus(p.status))
                            Icon(Icons.Filled.ChevronRight, null, tint = AppColor.TextMuted)
                        }
                    }
                }
            }
            item { RodapeSecao("Produção e entrega são controladas no painel web (admin).") }
        }
    }
}
