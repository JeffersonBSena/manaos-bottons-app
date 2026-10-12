package br.com.armarinhomanaos.bottons.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.armarinhomanaos.bottons.Graph
import br.com.armarinhomanaos.bottons.features.home.HomeScreen
import br.com.armarinhomanaos.bottons.features.pedido.NovoPedidoScreen
import br.com.armarinhomanaos.bottons.features.pedido.PedidoScreen
import br.com.armarinhomanaos.bottons.features.pedido.PixScreen
import br.com.armarinhomanaos.bottons.features.setup.SetupScreen
import br.com.armarinhomanaos.bottons.ui.DescansoOverlay
import kotlinx.coroutines.delay

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val inicio = if (Graph.prefs.configurado) "home" else "setup"
    val rota = nav.currentBackStackEntryAsState().value?.destination?.route

    // Ociosidade: qualquer toque (em qualquer tela) zera o relógio. Descanso só aparece na Início,
    // para nunca cobrir um Pix ou um pedido no meio do atendimento.
    var ultimoToque by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var descanso by remember { mutableStateOf(false) }
    LaunchedEffect(rota) {
        while (true) {
            delay(1000)
            val limite = Graph.prefs.descansoSegundos
            if (limite > 0 && rota == "home" && !descanso && System.currentTimeMillis() - ultimoToque > limite * 1000L) descanso = true
        }
    }

    Box(
        Modifier.fillMaxSize().pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) { awaitPointerEvent(PointerEventPass.Initial); ultimoToque = System.currentTimeMillis() }
            }
        },
    ) {
        NavHost(navController = nav, startDestination = inicio) {
            composable("setup") { SetupScreen(aoPronto = { nav.navigate("home") { popUpTo("setup") { inclusive = true } } }, aoVoltar = if (Graph.prefs.configurado) ({ nav.popBackStack() }) else null) }
            composable("home") {
                HomeScreen(
                    aoNovoPedido = { nav.navigate("novo") },
                    aoKit = { id -> nav.navigate("novo?kit=$id") },
                    aoTamanho = { id -> nav.navigate("novo?produto=$id") },
                    aoFila = { nav.navigate("fila") },
                    aoConfigurar = { nav.navigate("setup") },
                )
            }
            composable("fila") { br.com.armarinhomanaos.bottons.features.home.FilaScreen(aoVoltar = { nav.popBackStack() }, aoAbrirPedido = { n -> nav.navigate("pedido/$n") }) }
            composable(
                "novo?kit={kit}&produto={produto}",
                arguments = listOf(
                    androidx.navigation.navArgument("kit") { nullable = true; defaultValue = null },
                    androidx.navigation.navArgument("produto") { nullable = true; defaultValue = null },
                ),
            ) { back ->
                NovoPedidoScreen(
                    kitId = back.arguments?.getString("kit"),
                    produtoId = back.arguments?.getString("produto")?.toIntOrNull(),
                    aoVoltar = { nav.popBackStack() },
                    aoCriado = { n -> nav.navigate("pedido/$n?novo=1") { popUpTo("home") } },
                )
            }
            composable("pedido/{n}?novo={novo}", arguments = listOf(androidx.navigation.navArgument("novo") { nullable = true; defaultValue = null })) { back ->
                val n = back.arguments?.getString("n").orEmpty()
                PedidoScreen(numero = n, abrirQr = back.arguments?.getString("novo") == "1", aoVoltar = { nav.popBackStack("home", inclusive = false) }, aoPix = { nav.navigate("pix/$n") })
            }
            composable("pix/{n}") { back ->
                val n = back.arguments?.getString("n").orEmpty()
                PixScreen(numero = n, aoVoltar = { nav.popBackStack() }, aoInicio = { nav.popBackStack("home", inclusive = false) })
            }
        }
        DescansoOverlay(visivel = descanso, aoTocar = { descanso = false; ultimoToque = System.currentTimeMillis() })
    }
}
