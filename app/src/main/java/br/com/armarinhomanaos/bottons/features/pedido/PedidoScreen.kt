package br.com.armarinhomanaos.bottons.features.pedido

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.com.armarinhomanaos.bottons.data.api.Item
import br.com.armarinhomanaos.bottons.data.api.Params
import br.com.armarinhomanaos.bottons.data.api.Pedido
import br.com.armarinhomanaos.bottons.features.home.corStatus
import br.com.armarinhomanaos.bottons.ui.*
import br.com.armarinhomanaos.bottons.ui.theme.AppColor
import coil.compose.AsyncImage


@Composable
fun PedidoScreen(numero: String, aoVoltar: () -> Unit, aoPix: () -> Unit) {
    val vm: PedidoViewModel = viewModel(key = numero, factory = PedidoVmFactory(numero))
    val s by vm.s.collectAsState()
    val ctx = LocalContext.current
    var itemFoto by remember { mutableStateOf<Item?>(null) }      // controla o diálogo
    var itemAlvo by remember { mutableStateOf<Item?>(null) }      // quem recebe a foto (sobrevive ao fechar do diálogo)
    var itemAjuste by remember { mutableStateOf<Item?>(null) }
    var confirmaManual by remember { mutableStateOf(false) }
    var confirmaCancelar by remember { mutableStateOf(false) }
    var cpf by remember { mutableStateOf("") }
    val p = s.pedido

    SeletorFotoDialog(visivel = itemFoto != null, aoFechar = { itemFoto = null }, aoEscolher = { uri -> itemAlvo?.let { vm.enviarArte(ctx, it, uri) } })
    itemAjuste?.let { item -> AjusteDialog(item, ocupado = s.ocupado, aoFechar = { itemAjuste = null }, aoAplicar = { vm.ajustar(item, it); itemAjuste = null }) }

    if (s.pedirCpf) AlertDialog(
        onDismissRequest = vm::fecharCpf, title = { Text("CPF para o Pix") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("O Pix exige o CPF do cliente (só números).", color = AppColor.TextSecond, fontSize = 14.sp)
            AppTextField("CPF *", cpf, { cpf = it.filter { c -> c.isDigit() }.take(11) }, tipo = KeyboardType.Number, placeholder = "00000000000")
        } },
        confirmButton = { TextButton(enabled = cpf.length == 11, onClick = { vm.aprovar(cpf, aoPix) }) { Text("Gerar Pix") } },
        dismissButton = { TextButton(onClick = vm::fecharCpf) { Text("Cancelar") } },
    )
    if (confirmaManual) AlertDialog(
        onDismissRequest = { confirmaManual = false }, title = { Text("Pagou fora do Pix?") },
        text = { Text("Confirme como o cliente pagou. O pedido entra em produção na hora.") },
        confirmButton = { TextButton(onClick = { confirmaManual = false; vm.pagarManual("maquininha") }) { Text("Maquininha") } },
        dismissButton = { TextButton(onClick = { confirmaManual = false; vm.pagarManual("dinheiro") }) { Text("Dinheiro") } },
    )
    if (confirmaCancelar) AlertDialog(
        onDismissRequest = { confirmaCancelar = false }, title = { Text("Cancelar pedido?") }, text = { Text("As fotos enviadas serão descartadas.") },
        confirmButton = { TextButton(onClick = { confirmaCancelar = false; vm.cancelar(aoVoltar) }) { Text("Cancelar pedido", color = AppColor.Red) } },
        dismissButton = { TextButton(onClick = { confirmaCancelar = false }) { Text("Voltar") } },
    )
    s.erro?.let { e -> AlertDialog(onDismissRequest = vm::limparErro, title = { Text("Ops") }, text = { Text(e) }, confirmButton = { TextButton(onClick = vm::limparErro) { Text("OK") } }) }

    Scaffold(containerColor = AppColor.Bg, topBar = { BarraTopo(p?.number ?: "Pedido", aoVoltar) }, bottomBar = {
        if (p != null) Column(Modifier.fillMaxWidth().background(AppColor.Card).navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                p.pago -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.CheckCircle, null, tint = AppColor.Green, modifier = Modifier.size(28.dp))
                    Column { Text(if (p.status == "paid") "Pago — entrou em produção" else "Pago · ${p.status_label}", fontWeight = FontWeight.Bold, color = AppColor.TextPrimary); Text("Acompanhe a produção no painel.", fontSize = 13.sp, color = AppColor.TextSecond) }
                }
                p.aguardandoPix -> BotaoPrimario("Mostrar Pix · ${p.total}", aoPix)
                p.emAprovacao -> {
                    if (!p.all_arts_ready) Text("Envie a foto de todos os itens para liberar o pagamento.", fontSize = 13.sp, color = AppColor.TextSecond)
                    BotaoPrimario("Aprovar e gerar Pix · ${p.total}", habilitado = p.all_arts_ready, carregando = s.ocupado, onClick = { vm.aprovar(null, aoPix) })
                    BotaoSecundario("Pagou na maquininha / dinheiro", habilitado = p.all_arts_ready && !s.ocupado, onClick = { confirmaManual = true })
                }
                else -> Text(p.status_label, color = AppColor.TextSecond)
            }
            if (!p.pago && !p.encerrado) TextButton(onClick = { confirmaCancelar = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Cancelar pedido", color = AppColor.Red, fontSize = 13.sp) }
        }
    }) { pad ->
        if (p == null) { Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppColor.Primary) }; return@Scaffold }
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p.customer.name ?: "—", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                    Text("${p.units} bottons · ${p.total}", fontSize = 14.sp, color = AppColor.TextSecond)
                }
                Pilula(p.status_label, corStatus(p.status))
            }

            if (!p.pago && p.upload_url != null) {
                TituloSecao("Foto pelo celular do cliente")
                CartaoSecao {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        QrImagem(p.upload_url, 150.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Aponte a câmera do celular", fontWeight = FontWeight.SemiBold, color = AppColor.TextPrimary)
                            Text("Abre uma página para escolher a foto da galeria. Sem instalar nada.", fontSize = 13.sp, color = AppColor.TextSecond)
                            Text("Código: ${p.art_code}", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColor.Primary)
                            Text("O link também foi enviado no WhatsApp.", fontSize = 12.sp, color = AppColor.TextMuted)
                        }
                    }
                }
            }

            TituloSecao("Itens")
            CartaoSecao {
                p.items.forEachIndexed { i, item ->
                    if (i > 0) DivisorLinha()
                    LinhaItem(item, p, enviando = s.enviandoItem == item.id, aoFoto = { itemAlvo = item; itemFoto = item }, aoAjustar = { itemAjuste = item })
                }
            }
            RodapeSecao("O círculo rosa é a borda do botton; o que está fora dele vira a dobra. Mantenha rostos dentro da linha tracejada.")
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LinhaItem(item: Item, p: Pedido, enviando: Boolean, aoFoto: () -> Unit, aoAjustar: () -> Unit) {
    val art = item.art
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(96.dp).clip(CircleShape).background(AppColor.GraySoft).border(2.dp, if (art?.pronta == true) AppColor.Primary else AppColor.Border, CircleShape), contentAlignment = Alignment.Center) {
            when {
                enviando || art?.processando == true -> CircularProgressIndicator(color = AppColor.Primary, modifier = Modifier.size(28.dp))
                art?.preview_url != null -> AsyncImage(art.preview_url, "preview", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else -> Icon(Icons.Filled.PhotoCamera, null, tint = AppColor.TextMuted, modifier = Modifier.size(32.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${item.name} × ${item.qty}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AppColor.TextPrimary)
            when {
                enviando -> Text("Enviando foto…", fontSize = 13.sp, color = AppColor.TextSecond)
                art == null -> Text("Sem foto ainda", fontSize = 13.sp, color = AppColor.FarolPend)
                art.processando -> Text("Gerando o preview…", fontSize = 13.sp, color = AppColor.TextSecond)
                art.status == "failed" -> Text(art.error ?: "Não deu para ler a imagem", fontSize = 13.sp, color = AppColor.Red)
                else -> {
                    Text("Arte pronta · ${art.dpi_effective ?: 0} DPI", fontSize = 13.sp, color = AppColor.Green)
                    art.quality_warning?.let { Text("⚠️ $it", fontSize = 12.sp, color = AppColor.Amber) }
                }
            }
            if (!p.pago) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = aoFoto, enabled = !enviando, colors = ButtonDefaults.filledTonalButtonColors(containerColor = AppColor.PrimarySoft, contentColor = AppColor.Primary)) {
                    Icon(Icons.Filled.PhotoCamera, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(if (art == null) "Enviar foto" else "Trocar")
                }
                if (art?.pronta == true) FilledTonalButton(onClick = aoAjustar, colors = ButtonDefaults.filledTonalButtonColors(containerColor = AppColor.GraySoft, contentColor = AppColor.TextPrimary)) {
                    Icon(Icons.Filled.Tune, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Ajustar")
                }
            }
        }
    }
}

/** Zoom / posição / rotação — o servidor regera preview e print com os mesmos parâmetros. */
@Composable
private fun AjusteDialog(item: Item, ocupado: Boolean, aoFechar: () -> Unit, aoAplicar: (Params) -> Unit) {
    val ini = item.art?.params ?: Params()
    var zoom by remember { mutableFloatStateOf(ini.zoom) }
    var x by remember { mutableFloatStateOf(ini.x) }
    var y by remember { mutableFloatStateOf(ini.y) }
    var rot by remember { mutableFloatStateOf(ini.rotation) }
    AlertDialog(
        onDismissRequest = aoFechar, title = { Text("Ajustar ${item.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item.art?.preview_url?.let { AsyncImage(it, null, Modifier.size(160.dp).clip(CircleShape).align(Alignment.CenterHorizontally), contentScale = ContentScale.Crop) }
                Text("Preview atual. Depois de aplicar, o novo preview leva alguns segundos.", fontSize = 12.sp, color = AppColor.TextSecond)
                Controle("Zoom ${"%.1f".format(zoom)}×", zoom, 1f..3f) { zoom = it }
                Controle("Horizontal", x, -1f..1f) { x = it }
                Controle("Vertical", y, -1f..1f) { y = it }
                Controle("Rotação ${rot.toInt()}°", rot, -180f..180f) { rot = it }
                TextButton(onClick = { zoom = 1f; x = 0f; y = 0f; rot = 0f }) { Text("Centralizar") }
            }
        },
        confirmButton = { TextButton(enabled = !ocupado, onClick = { aoAplicar(Params(zoom, x, y, rot)) }) { Text("Aplicar") } },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Fechar") } },
    )
}

@Composable
private fun Controle(rotulo: String, v: Float, faixa: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column {
        Text(rotulo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColor.TextPrimary)
        Slider(value = v, onValueChange = onChange, valueRange = faixa, colors = SliderDefaults.colors(thumbColor = AppColor.Primary, activeTrackColor = AppColor.Primary))
    }
}
