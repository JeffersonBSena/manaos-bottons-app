package br.com.armarinhomanaos.bottons.features.pedido

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
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
fun PedidoScreen(numero: String, aoVoltar: () -> Unit, aoPix: () -> Unit, abrirQr: Boolean = false) {
    val vm: PedidoViewModel = viewModel(key = numero, factory = PedidoVmFactory(numero))
    val s by vm.s.collectAsState()
    val ctx = LocalContext.current
    var itemFoto by remember { mutableStateOf<Item?>(null) }      // controla o diálogo
    var itemAlvo by remember { mutableStateOf<Item?>(null) }      // quem recebe a foto (sobrevive ao fechar do diálogo)
    var itemAjuste by remember { mutableStateOf<Item?>(null) }
    var confirmaManual by remember { mutableStateOf(false) }
    var confirmaCancelar by remember { mutableStateOf(false) }
    var mostrarQr by remember { mutableStateOf(abrirQr) }
    var cpf by remember { mutableStateOf("") }
    val p = s.pedido

    SeletorFotoDialog(visivel = itemFoto != null, aoFechar = { itemFoto = null }, aoEscolher = { uri -> itemAlvo?.let { vm.enviarArte(ctx, it, uri) } })
    itemAjuste?.let { item -> AjusteDialog(item, vm = vm, ocupado = s.ocupado, aoFechar = { itemAjuste = null }, aoAplicar = { vm.ajustar(item, it); itemAjuste = null }) }

    if (s.pedirCpf) AlertDialog(
        onDismissRequest = vm::fecharCpf, title = { Text("CPF para o Pix") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("O Pix exige o CPF do cliente (só números).", color = AppColor.TextSecond, fontSize = 14.sp)
            AppTextField("CPF *", cpf, { cpf = it.filter { c -> c.isDigit() }.take(11) }, tipo = KeyboardType.Number, placeholder = "00000000000")
        } },
        confirmButton = { TextButton(enabled = cpf.length == 11, onClick = { vm.aprovar(cpf, aoPix) }) { Text("Gerar Pix") } },
        dismissButton = { TextButton(onClick = vm::fecharCpf) { Text("Cancelar") } },
    )
    if (mostrarQr && p?.upload_url != null) QrModal(p, aoFechar = { mostrarQr = false })
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
                TituloSecao("Passo 1 · Mande suas fotos")
                CartaoSecao {
                    Row(Modifier.clickable { mostrarQr = true }.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        QrImagem(p.upload_url, 120.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Escaneie com o seu celular e escolha as fotos", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = AppColor.TextPrimary)
                            Text("Uma foto para cada botton. Não precisa centralizar, recortar nem se preocupar com a resolução: o app faz isso sozinho depois do envio.", fontSize = 13.sp, color = AppColor.TextSecond)
                            Text("Toque para ampliar o QR", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColor.Primary)
                        }
                    }
                }
                TituloSecao("Passo 2 · Confira e aprove", direita = "o atendente ajuda aqui")
            }

            val semFoto = p.items.count { it.art == null }
            TituloSecao("Bottons · uma foto em cada", direita = if (semFoto > 0) "$semFoto sem foto" else "todos com foto")
            CartaoSecao {
                val porProduto = p.items.groupBy { it.product_id }
                p.items.forEachIndexed { i, item ->
                    if (i > 0) DivisorLinha()
                    val grupo = porProduto[item.product_id] ?: listOf(item)
                    val rotulo = if (grupo.size > 1) "${item.name} · ${grupo.indexOf(item) + 1} de ${grupo.size}" else item.name
                    LinhaItem(item, p, rotulo, outrosSemFoto = p.items.count { it.id != item.id && it.art == null }, enviando = s.enviandoItem == item.id, ocupado = s.ocupado,
                        aoFoto = { itemAlvo = item; itemFoto = item }, aoAjustar = { itemAjuste = item }, aoCopiar = { vm.copiarParaOutros(item) })
                }
            }
            RodapeSecao("O círculo vermelho é a borda do botton; o que está fora dele vira a dobra. Rostos dentro da linha tracejada.")
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LinhaItem(item: Item, p: Pedido, rotulo: String, outrosSemFoto: Int, enviando: Boolean, ocupado: Boolean, aoFoto: () -> Unit, aoAjustar: () -> Unit, aoCopiar: () -> Unit) {
    val art = item.art
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(96.dp).clip(CircleShape).background(AppColor.GraySoft).border(2.dp, if (art?.pronta == true) AppColor.Primary else AppColor.Border, CircleShape), contentAlignment = Alignment.Center) {
            when {
                enviando || art?.processando == true -> CircularProgressIndicator(color = AppColor.Primary, modifier = Modifier.size(28.dp))
                art?.preview_url != null -> AsyncImage(art.preview_url, "preview", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else -> Icon(Icons.Filled.PhotoCamera, null, tint = AppColor.TextMuted, modifier = Modifier.size(32.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(rotulo, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AppColor.TextPrimary)
            when {
                enviando -> Text("Enviando foto…", fontSize = 13.sp, color = AppColor.TextSecond)
                art == null -> Text("Sem foto ainda", fontSize = 13.sp, color = AppColor.FarolPend)
                art.processando -> Text("Gerando o preview…", fontSize = 13.sp, color = AppColor.TextSecond)
                art.status == "failed" -> Text(art.error ?: "Não deu para ler a imagem", fontSize = 13.sp, color = AppColor.Red)
                else -> {
                    Text("Foto pronta · ${art.dpi_effective ?: 0} DPI" + when (art.fill) { "ai" -> " · fundo completado por IA ✨"; "local" -> " · fundo completado"; else -> "" }, fontSize = 13.sp, color = AppColor.Green)
                    art.quality_warning?.let { Text("⚠️ $it", fontSize = 12.sp, color = AppColor.Amber) }
                }
            }
            if (!p.pago) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = aoFoto, enabled = !enviando, colors = ButtonDefaults.filledTonalButtonColors(containerColor = AppColor.PrimarySoft, contentColor = AppColor.Primary)) {
                        Icon(Icons.Filled.PhotoCamera, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(if (art == null) "Enviar foto" else "Trocar foto")
                    }
                    if (art?.pronta == true && outrosSemFoto > 0) FilledTonalButton(onClick = aoCopiar, enabled = !ocupado, colors = ButtonDefaults.filledTonalButtonColors(containerColor = AppColor.GreenSoft, contentColor = AppColor.Green)) {
                        Icon(Icons.Filled.ContentCopy, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Usar nos outros $outrosSemFoto")
                    }
                }
                if (art?.pronta == true) TextButton(onClick = aoAjustar, contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.Filled.Tune, null, Modifier.size(16.dp), tint = AppColor.Primary); Spacer(Modifier.width(6.dp))
                    Text("Quer aproximar? Ajustar zoom", color = AppColor.Primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * Ajuste ao vivo: o enquadramento é automático (centralizado, recorte circular no tamanho exato do botton).
 * A única escolha do cliente é o zoom. O preview replica a regra do ArtRenderer (cover × zoom).
 */
@Composable
private fun AjusteDialog(item: Item, vm: PedidoViewModel, ocupado: Boolean, aoFechar: () -> Unit, aoAplicar: (Params) -> Unit) {
    val ini = item.art?.params ?: Params()
    var zoom by remember { mutableFloatStateOf(ini.zoom) }
    var bmp by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var falhou by remember { mutableStateOf(false) }
    LaunchedEffect(item.id) { bmp = vm.carregarOriginal(item)?.asImageBitmap(); if (bmp == null) falhou = true }
    val spec = item.spec

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Zoom da foto · ${item.name}") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
                    if (bmp == null && !falhou) CircularProgressIndicator(color = AppColor.Primary)
                    else PreviewAoVivo(bmp, Params(zoom, 0f, 0f, 0f), spec, 300.dp)
                }
                Text("A foto inteira é encaixada e centralizada no botton, sem cortar. O zoom aproxima a partir daí.", fontSize = 13.sp, color = AppColor.TextSecond, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    BotaoSeta(Icons.Filled.ZoomOut, "menos zoom") { zoom = (zoom - 0.1f).coerceAtLeast(1f) }
                    Text("${"%.1f".format(zoom)}×", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary, modifier = Modifier.width(56.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    BotaoSeta(Icons.Filled.ZoomIn, "mais zoom") { zoom = (zoom + 0.1f).coerceAtMost(3f) }
                }
                Slider(value = zoom, onValueChange = { zoom = it }, valueRange = 1f..3f, colors = SliderDefaults.colors(thumbColor = AppColor.Primary, activeTrackColor = AppColor.Primary), modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { zoom = 1f }) { Text("Voltar ao automático (1,0×)") }
            }
        },
        confirmButton = { Button(enabled = !ocupado, onClick = { aoAplicar(Params(zoom, 0f, 0f, 0f)) }, colors = ButtonDefaults.buttonColors(containerColor = AppColor.Primary)) { Text("Aplicar") } },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
    )
}

@Composable
private fun BotaoSeta(icone: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(52.dp), colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = AppColor.PrimarySoft, contentColor = AppColor.Primary)) { Icon(icone, desc) }
}

@Composable
private fun PreviewAoVivo(bmp: androidx.compose.ui.graphics.ImageBitmap?, p: Params, spec: br.com.armarinhomanaos.bottons.data.api.Spec?, tam: androidx.compose.ui.unit.Dp) {
    androidx.compose.foundation.Canvas(Modifier.size(tam).clip(CircleShape).background(Color.White)) {
        val n = size.minDimension; val c = n / 2f
        val corte = spec?.corte_mm?.takeIf { it > 0 } ?: 57
        val rb = c * (spec?.botton_mm ?: 44) / corte.toFloat()
        val rs = c * (spec?.seguro_mm ?: 40) / corte.toFloat()
        if (bmp != null) {
            val w = bmp.width; val h = bmp.height
            // fundo: foto em "cover" escurecida (o servidor desfoca; aqui só esmaece)
            val cs = n / minOf(w, h) * 1.08f
            val cw = (w * cs).toInt(); val ch = (h * cs).toInt()
            drawImage(bmp, dstOffset = androidx.compose.ui.unit.IntOffset(((n - cw) / 2).toInt(), ((n - ch) / 2).toInt()), dstSize = androidx.compose.ui.unit.IntSize(cw, ch), alpha = 0.35f)
            // foto inteira: diagonal = 96% do diâmetro do botton, centralizada (mesma regra do servidor)
            val diag = kotlin.math.hypot(w.toFloat(), h.toFloat())
            val escala = (2f * rb * 0.96f) / diag * p.zoom
            val dw = (w * escala).toInt(); val dh = (h * escala).toInt()
            drawImage(bmp, dstOffset = androidx.compose.ui.unit.IntOffset((c - dw / 2f).toInt(), (c - dh / 2f).toInt()), dstSize = androidx.compose.ui.unit.IntSize(dw, dh))
        }
        drawCircle(Color.White.copy(alpha = 0.45f), radius = c, style = androidx.compose.ui.graphics.drawscope.Stroke(width = (c - rb) * 2f))
        drawCircle(Color(0xFF808080), radius = c - 1f, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
        drawCircle(AppColor.Primary, radius = rb, style = androidx.compose.ui.graphics.drawscope.Stroke(4f))
        drawCircle(Color.White, radius = rs, style = androidx.compose.ui.graphics.drawscope.Stroke(2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 10f))))
    }
}
/** Modal em tela cheia do QR: o passo do cliente é só mandar as fotos; o resto é automático. */
@Composable
private fun QrModal(p: Pedido, aoFechar: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = aoFechar, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth(0.9f).clip(RoundedCornerShape(24.dp)).background(AppColor.Card).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Pilula("PASSO 1 DE 3", AppColor.Primary)
            Text("Mande suas fotos pelo celular", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            QrImagem(p.upload_url!!, 340.dp)
            Text("Código: ${p.art_code}", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = AppColor.Primary)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                PassoLinha("1", "Abra a câmera do celular e aponte para o QR.")
                PassoLinha("2", "Toque em \"Escolher foto\" em cada botton e pegue da sua galeria. Pode ser a mesma foto ou uma diferente em cada.")
                PassoLinha("3", "Pronto! O app centraliza, recorta em círculo no tamanho certo e confere a resolução sozinho.")
            }
            Text("Nada de ajustar enquadramento: isso é automático. Se quiser, o atendente aproxima o zoom depois.", fontSize = 13.sp, color = AppColor.TextSecond, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text("O link também chegou no seu WhatsApp.", fontSize = 12.sp, color = AppColor.TextMuted)
            BotaoPrimario("Entendi", aoFechar)
        }
    }
}

@Composable
private fun PassoLinha(n: String, texto: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(AppColor.Primary), contentAlignment = Alignment.Center) { Text(n, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        Text(texto, fontSize = 15.sp, color = AppColor.TextPrimary, modifier = Modifier.weight(1f))
    }
}
