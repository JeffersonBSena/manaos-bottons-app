package br.com.armarinhomanaos.bottons.features.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.armarinhomanaos.bottons.Graph
import br.com.armarinhomanaos.bottons.data.api.Pedido
import br.com.armarinhomanaos.bottons.ui.*
import br.com.armarinhomanaos.bottons.ui.theme.AppColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Fila do atendente (atrás do PIN): pedidos abertos no stand. */
@Composable
fun FilaScreen(aoVoltar: () -> Unit, aoAbrirPedido: (String) -> Unit) {
    var pedidos by remember { mutableStateOf<List<Pedido>>(emptyList()) }
    var erro by remember { mutableStateOf<String?>(null) }
    var carregando by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    suspend fun carregar() {
        runCatching { Graph.api().pedidos() }.onSuccess { pedidos = it; erro = null }.onFailure { erro = "Sem conexão com o servidor" }
        carregando = false
    }
    LaunchedEffect(Unit) { while (true) { carregar(); delay(8000) } }

    Scaffold(containerColor = AppColor.Bg, topBar = {
        BarraTopo("Pedidos em andamento", aoVoltar, acoes = { IconButton(onClick = { scope.launch { carregar() } }) { Icon(Icons.Filled.Refresh, "Atualizar") } })
    }) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { TituloSecao("Abertos no stand", direita = if (pedidos.isNotEmpty()) "${pedidos.size}" else null) }
            item {
                when {
                    carregando -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppColor.Primary) }
                    erro != null -> CartaoSecao { Text(erro!!, color = AppColor.Red, modifier = Modifier.padding(16.dp)) }
                    pedidos.isEmpty() -> CartaoSecao { Text("Nenhum pedido aberto.", color = AppColor.TextSecond, modifier = Modifier.padding(16.dp)) }
                    else -> CartaoSecao {
                        pedidos.forEachIndexed { i, p ->
                            if (i > 0) DivisorLinha()
                            LinhaFarol(corStatus(p.status), p.customer.name ?: p.number, "${p.number} · ${p.units} bottons · ${p.total}${p.kit?.let { " · $it" } ?: ""}",
                                onClick = { aoAbrirPedido(p.number) }) {
                                Pilula(p.status_label, corStatus(p.status))
                                Icon(Icons.Filled.ChevronRight, null, tint = AppColor.TextMuted)
                            }
                        }
                    }
                }
            }
            item { RodapeSecao("Produção e entrega são controladas no painel web (admin). Esta tela só aparece com o PIN do atendente.") }
        }
    }
}
