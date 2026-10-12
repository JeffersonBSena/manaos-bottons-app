package br.com.armarinhomanaos.bottons.features.pedido

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.armarinhomanaos.bottons.Graph
import br.com.armarinhomanaos.bottons.data.Network
import br.com.armarinhomanaos.bottons.data.api.ItemBody
import br.com.armarinhomanaos.bottons.data.api.NovoPedidoBody
import br.com.armarinhomanaos.bottons.data.api.Produto
import br.com.armarinhomanaos.bottons.ui.*
import br.com.armarinhomanaos.bottons.ui.theme.AppColor
import kotlinx.coroutines.launch

fun reais(cents: Int): String = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("pt", "BR")).format(cents / 100.0).replace("\u00a0", " ")

@Composable
fun NovoPedidoScreen(aoVoltar: () -> Unit, aoCriado: (String) -> Unit, kitId: String? = null, produtoId: Int? = null) {
    var produtos by remember { mutableStateOf<List<Produto>>(emptyList()) }
    var kits by remember { mutableStateOf<List<br.com.armarinhomanaos.bottons.data.api.Kit>>(emptyList()) }
    var minimo by remember { mutableIntStateOf(5) }
    val qtds = remember { mutableStateMapOf<Int, Int>() }
    var nome by remember { mutableStateOf("") }
    var fone by remember { mutableStateOf("") }
    var cpf by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }
    var enviando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        runCatching { Graph.api().produtos() }.onSuccess { r ->
            produtos = r.products; minimo = r.min_units_per_order; kits = r.kits
            // Pré-seleção vinda da Início: kit (itens do kit) ou um tamanho (quantidade mínima)
            r.kits.firstOrNull { it.id == kitId }?.let { k -> qtds.clear(); k.items.forEach { qtds[it.product_id] = it.qty } }
            if (produtoId != null && qtds.isEmpty()) qtds[produtoId] = r.min_units_per_order
        }.onFailure { erro = "Não foi possível carregar os tamanhos." }
    }

    val unidades = qtds.values.sum()
    val subtotal = produtos.sumOf { (qtds[it.id] ?: 0) * it.price_cents }
    // Kit vale enquanto os itens forem exatamente os do kit (mesma regra do servidor)
    val kit = kits.firstOrNull { k -> k.id == kitId && k.items.associate { it.product_id to it.qty } == qtds.filter { it.value > 0 }.toMap() }
    val desconto = kit?.discount_cents ?: 0
    val total = subtotal - desconto
    val foneDigitos = fone.filter { it.isDigit() }
    val valido = nome.isNotBlank() && foneDigitos.length >= 10 && unidades >= minimo

    Scaffold(containerColor = AppColor.Bg, topBar = { BarraTopo("Novo pedido", aoVoltar) }, bottomBar = {
        Column(Modifier.background(AppColor.Card).navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (kit != null) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${kit.name} · ${kit.discount_pct.toInt()}% off", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColor.Green, modifier = Modifier.weight(1f))
                Text(reais(subtotal), fontSize = 13.sp, color = AppColor.TextMuted, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
            }
            Row(Modifier.fillMaxWidth()) {
                Text("$unidades botton${if (unidades == 1) "" else "s"}", color = AppColor.TextSecond, modifier = Modifier.weight(1f))
                Text(reais(total), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColor.TextPrimary)
            }
            if (unidades in 1 until minimo) Text("Pedido mínimo: $minimo bottons", fontSize = 12.sp, color = AppColor.Red)
            BotaoPrimario("Criar pedido", habilitado = valido, carregando = enviando, onClick = {
                enviando = true; erro = null
                scope.launch {
                    val body = NovoPedidoBody(nome.trim(), foneDigitos, cpf.filter { it.isDigit() }.ifBlank { null },
                        items = qtds.filter { it.value > 0 }.map { ItemBody(it.key, it.value) }, kit_id = kit?.id)
                    runCatching { Graph.api().criar(body) }
                        .onSuccess { r -> if (r.isSuccessful && r.body() != null) aoCriado(r.body()!!.number) else erro = Network.erro(r).message }
                        .onFailure { erro = "Falha de conexão: ${it.message?.take(60)}" }
                    enviando = false
                }
            })
        }
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TituloSecao("Cliente")
            CartaoSecao {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppTextField("Nome *", nome, { nome = it }, placeholder = "Como o cliente quer ser chamado")
                    AppTextField("WhatsApp *", fone, { fone = it }, tipo = KeyboardType.Phone, placeholder = "92 99999-0000", dica = "Recebe o link para mandar a foto e os avisos do pedido.")
                    AppTextField("CPF", cpf, { cpf = it }, tipo = KeyboardType.Number, placeholder = "Só para pagar no Pix", dica = "Pode deixar em branco e informar na hora do Pix.")
                }
            }
            TituloSecao("Tamanhos", direita = "mín. $minimo")
            CartaoSecao {
                if (produtos.isEmpty()) Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    if (erro == null) CircularProgressIndicator(color = AppColor.Primary) else Text(erro!!, color = AppColor.Red)
                }
                produtos.forEachIndexed { i, p ->
                    if (i > 0) DivisorLinha()
                    val q = qtds[p.id] ?: 0
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        // bolinha proporcional ao tamanho
                        Box(Modifier.size((18 + p.spec.botton_mm / 2).dp).clip(CircleShape).background(AppColor.PrimarySoft), contentAlignment = Alignment.Center) {
                            Box(Modifier.size((8 + p.spec.botton_mm / 3).dp).clip(CircleShape).background(AppColor.Primary))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AppColor.TextPrimary)
                            Text("${p.price} cada", fontSize = 13.sp, color = AppColor.TextSecond)
                        }
                        Stepper(q, { qtds[p.id] = it })
                    }
                }
            }
            if (erro != null && produtos.isNotEmpty()) Text(erro!!, color = AppColor.Red, fontSize = 13.sp)
            RodapeSecao("Cada botton terá o seu próprio espaço para foto. Pode ser uma foto diferente em cada um ou a mesma em todos.")
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Stepper(valor: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilledTonalIconButton(onClick = { if (valor > 0) onChange(valor - 1) }, modifier = Modifier.size(36.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = AppColor.GraySoft)) { Icon(Icons.Filled.Remove, "menos", tint = AppColor.TextPrimary) }
        Text("$valor", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (valor > 0) AppColor.Primary else AppColor.TextMuted, modifier = Modifier.width(32.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        FilledIconButton(onClick = { onChange(valor + 1) }, modifier = Modifier.size(36.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = AppColor.Primary)) { Icon(Icons.Filled.Add, "mais", tint = Color.White) }
    }
}
