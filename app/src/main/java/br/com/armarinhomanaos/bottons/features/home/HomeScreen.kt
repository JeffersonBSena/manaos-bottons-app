package br.com.armarinhomanaos.bottons.features.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.armarinhomanaos.bottons.Graph
import br.com.armarinhomanaos.bottons.R
import br.com.armarinhomanaos.bottons.data.api.Kit
import br.com.armarinhomanaos.bottons.data.api.Produto
import br.com.armarinhomanaos.bottons.features.pedido.reais
import br.com.armarinhomanaos.bottons.ui.*
import br.com.armarinhomanaos.bottons.ui.theme.AparenciaStore
import br.com.armarinhomanaos.bottons.ui.theme.AppColor
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

fun corStatus(status: String): Color = when (status) {
    "awaiting_art", "awaiting_approval" -> AppColor.FarolPend
    "pending_payment" -> AppColor.Orange
    "paid", "in_production", "quality_check", "packing" -> AppColor.FarolInfo
    "ready_for_pickup", "ready_to_ship", "delivered", "completed" -> AppColor.FarolOk
    "rework", "cancelled", "refunded" -> AppColor.FarolNao
    else -> AppColor.Slate
}

/**
 * Início do stand: vitrine (tamanhos com preço, kits com desconto, inspirações) que abre o pedido
 * já montado, e a fila de pedidos em andamento para o atendente.
 */
@Composable
fun HomeScreen(aoNovoPedido: () -> Unit, aoKit: (String) -> Unit, aoTamanho: (Int) -> Unit, aoFila: () -> Unit, aoConfigurar: () -> Unit) {
    val ctx = LocalContext.current
    var pedirPin by remember { mutableStateOf(false) }
    var produtos by remember { mutableStateOf<List<Produto>>(emptyList()) }
    var kits by remember { mutableStateOf<List<Kit>>(emptyList()) }
    var inspiracoes by remember { mutableStateOf<List<String>>(emptyList()) }
    var minimo by remember { mutableIntStateOf(5) }
    var erro by remember { mutableStateOf<String?>(null) }

    suspend fun carregar() {
        runCatching { Graph.api().produtos() }
            .onSuccess { produtos = it.products; kits = it.kits; inspiracoes = it.inspiracoes; minimo = it.min_units_per_order; erro = null }
            .onFailure { erro = "Sem conexão com o servidor" }
    }
    LaunchedEffect(Unit) { while (true) { carregar(); delay(60_000) } }

    if (pedirPin) PinDialog(aoFechar = { pedirPin = false }, aoOk = { pedirPin = false; aoFila() })

    Scaffold(containerColor = AppColor.Bg, topBar = {
        BarraTopo("Bottons Manaós", acoes = {
            IconButton(onClick = { AparenciaStore.proxima(ctx) }) { Icon(Icons.Filled.DarkMode, "Aparência") }
            IconButton(onClick = { pedirPin = true }) { Icon(Icons.Filled.Lock, "Atendente") }
            IconButton(onClick = aoConfigurar) { Icon(Icons.Filled.Settings, "Configuração") }
        })
    }) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Hero
            item {
                Column(
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(AppColor.Navy, AppColor.PrimaryDark))).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Image(painterResource(R.drawable.logo_manaos), "Manaós", Modifier.size(96.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Seu botton na hora ✨", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Escolha o tamanho ou um kit, mande a foto e pague no Pix. Pronto em minutos.", fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f))
                        }
                    }
                    Button(onClick = aoNovoPedido, colors = ButtonDefaults.buttonColors(containerColor = AppColor.Orange, contentColor = AppColor.Navy), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Icon(Icons.Filled.Add, null); Spacer(Modifier.width(8.dp)); Text("Novo pedido", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Tamanhos e preços
            if (produtos.isNotEmpty()) {
                item { TituloSecao("Tamanhos e preços", Modifier.padding(horizontal = 16.dp), direita = "toque para pedir · mín. $minimo") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(produtos) { p -> CartaoTamanho(p, inspiracoes.getOrNull(produtos.indexOf(p) % maxOf(1, inspiracoes.size))) { aoTamanho(p.id) } }
                    }
                }
            }

            // Kits
            if (kits.isNotEmpty()) {
                item { TituloSecao("Kits prontos", Modifier.padding(horizontal = 16.dp), direita = "com desconto") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(kits) { k -> CartaoKit(k) { aoKit(k.id) } }
                    }
                }
            }

            // Inspirações
            if (inspiracoes.isNotEmpty()) {
                item { TituloSecao("Inspire-se", Modifier.padding(horizontal = 16.dp), direita = "sua foto, seu time, seu ídolo") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(inspiracoes) { url ->
                            Box(Modifier.size(112.dp).clip(CircleShape).background(Color.White).border(3.dp, AppColor.Primary, CircleShape).clickable { aoNovoPedido() }) {
                                AsyncImage(url, null, Modifier.fillMaxSize().padding(4.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            }
                        }
                    }
                }
            }

            if (erro != null) item { Box(Modifier.padding(horizontal = 16.dp)) { CartaoSecao { Text(erro!!, color = AppColor.Red, modifier = Modifier.padding(16.dp)) } } }
            item { RodapeSecao("Acompanhe seu pedido pelo link que chega no WhatsApp. Pedido mínimo: $minimo bottons.", ) }

        }
    }
}

/** Cartão de tamanho: botton proporcional (com uma arte de exemplo), mm e preço unitário. */
@Composable
private fun CartaoTamanho(p: Produto, arteUrl: String?, onClick: () -> Unit) {
    val mm = p.spec.botton_mm
    val diam = (56 + mm * 1.6f).dp
    Column(
        Modifier.width(150.dp).clip(RoundedCornerShape(16.dp)).background(AppColor.Card).border(1.dp, AppColor.Border, RoundedCornerShape(16.dp))
            .clickable { onClick() }.padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.height(160.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(diam).clip(CircleShape).background(AppColor.Primary), contentAlignment = Alignment.Center) {
                Box(Modifier.size(diam * 0.86f).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    if (arteUrl != null) AsyncImage(arteUrl, null, Modifier.size(diam * 0.8f).clip(CircleShape), contentScale = ContentScale.Crop)
                    else Box(Modifier.size(diam * 0.6f).clip(CircleShape).background(AppColor.Orange))
                }
            }
        }
        Text("$mm mm", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
        Text("${p.price} cada", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColor.Primary)
        Text("corte ${p.spec.corte_mm} mm", fontSize = 11.sp, color = AppColor.TextMuted)
    }
}

/** Cartão de kit: nome, descrição, itens, preço com o "de/por". */
@Composable
private fun CartaoKit(k: Kit, onClick: () -> Unit) {
    Column(
        Modifier.width(240.dp).clip(RoundedCornerShape(16.dp)).background(AppColor.Card).border(1.dp, AppColor.Border, RoundedCornerShape(16.dp))
            .clickable { onClick() }.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(k.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary, modifier = Modifier.weight(1f))
            if (k.discount_pct > 0) Pilula("-${k.discount_pct.toInt()}%", AppColor.Green)
        }
        Text(k.description, fontSize = 13.sp, color = AppColor.TextSecond, minLines = 2, maxLines = 2)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            k.items.forEach { Text("• ${it.qty}× ${it.name}", fontSize = 13.sp, color = AppColor.TextPrimary) }
        }
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (k.discount_cents > 0) Text(reais(k.subtotal_cents), fontSize = 13.sp, color = AppColor.TextMuted, textDecoration = TextDecoration.LineThrough)
            Text(k.price, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColor.Primary)
        }
        Text("${k.units} bottons · ${reais(k.price_cents / maxOf(1, k.units))} cada", fontSize = 12.sp, color = AppColor.TextSecond)
    }
}

/** PIN do atendente (Configuração) — a fila de pedidos não fica à vista dos clientes. */
@Composable
fun PinDialog(aoFechar: () -> Unit, aoOk: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Área do atendente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Digite o PIN para ver os pedidos em andamento.", fontSize = 14.sp, color = AppColor.TextSecond)
                AppTextField("PIN *", pin, { pin = it.filter { c -> c.isDigit() }.take(6); erro = false },
                    tipo = androidx.compose.ui.text.input.KeyboardType.NumberPassword, placeholder = "••••", erro = if (erro) "PIN incorreto" else null)
            }
        },
        confirmButton = { TextButton(enabled = pin.length >= 4, onClick = { if (pin == Graph.prefs.pinAtendente) aoOk() else erro = true }) { Text("Entrar") } },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
    )
}
