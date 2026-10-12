package br.com.armarinhomanaos.bottons.features.pedido

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.armarinhomanaos.bottons.ui.*
import br.com.armarinhomanaos.bottons.ui.theme.AppColor


/** QR Pix em tela cheia; o polling do ViewModel detecta o pagamento (webhook Asaas) e mostra o sucesso. */
@Composable
fun PixScreen(numero: String, aoVoltar: () -> Unit, aoInicio: () -> Unit) {
    val vm: PedidoViewModel = viewModel(key = numero, factory = PedidoVmFactory(numero))
    val s by vm.s.collectAsState()
    val ctx = LocalContext.current
    val p = s.pedido
    val pix = s.pix
    var copiado by remember { mutableStateOf(false) }

    Scaffold(containerColor = AppColor.Bg, topBar = { BarraTopo("Pagamento Pix", aoVoltar) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when {
                p?.pago == true -> {
                    Spacer(Modifier.height(40.dp))
                    Icon(Icons.Filled.CheckCircle, null, tint = AppColor.Green, modifier = Modifier.size(120.dp))
                    Text("Pagamento confirmado!", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                    Text("Pedido ${p.number} entrou em produção. Avisamos no WhatsApp quando estiver pronto.", fontSize = 15.sp, color = AppColor.TextSecond, modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(Modifier.height(16.dp))
                    BotaoPrimario("Novo atendimento", aoInicio)
                }
                pix?.qr_base64 == null -> { CircularProgressIndicator(color = AppColor.Primary); Text("Gerando o Pix…", color = AppColor.TextSecond) }
                else -> {
                    Text(p?.total ?: reais(pix.amount_cents), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                    Text("Escaneie com o app do banco", fontSize = 14.sp, color = AppColor.TextSecond)
                    val bmp = remember(pix.qr_base64) { bitmapDeBase64(pix.qr_base64) }
                    if (bmp != null) Image(bmp.asImageBitmap(), "QR Pix", Modifier.size(300.dp).clip(RoundedCornerShape(16.dp)).background(Color.White).padding(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(Modifier.size(16.dp), color = AppColor.Primary, strokeWidth = 2.dp)
                        Text("Aguardando o pagamento…", fontSize = 13.sp, color = AppColor.TextSecond)
                    }
                    pix.copia_cola?.let { cc ->
                        BotaoSecundario(if (copiado) "Código copiado ✔" else "Copiar código Pix", onClick = {
                            (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Pix", cc)); copiado = true
                        })
                    }
                    RodapeSecao("Pagou e não confirmou em 1 minuto? Toque em voltar e use \"Pagou na maquininha / dinheiro\" só se o pagamento foi fora do Pix.")
                }
            }
        }
    }
}
