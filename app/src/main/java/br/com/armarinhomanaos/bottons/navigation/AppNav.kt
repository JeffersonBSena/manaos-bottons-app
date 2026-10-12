package br.com.armarinhomanaos.bottons.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.armarinhomanaos.bottons.Graph
import br.com.armarinhomanaos.bottons.features.home.HomeScreen
import br.com.armarinhomanaos.bottons.features.pedido.NovoPedidoScreen
import br.com.armarinhomanaos.bottons.features.pedido.PedidoScreen
import br.com.armarinhomanaos.bottons.features.pedido.PixScreen
import br.com.armarinhomanaos.bottons.features.setup.SetupScreen

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val inicio = if (Graph.prefs.configurado) "home" else "setup"
    NavHost(navController = nav, startDestination = inicio) {
        composable("setup") { SetupScreen(aoPronto = { nav.navigate("home") { popUpTo("setup") { inclusive = true } } }, aoVoltar = if (Graph.prefs.configurado) ({ nav.popBackStack() }) else null) }
        composable("home") {
            HomeScreen(
                aoNovoPedido = { nav.navigate("novo") },
                aoAbrirPedido = { n -> nav.navigate("pedido/$n") },
                aoConfigurar = { nav.navigate("setup") },
            )
        }
        composable("novo") { NovoPedidoScreen(aoVoltar = { nav.popBackStack() }, aoCriado = { n -> nav.navigate("pedido/$n") { popUpTo("home") } }) }
        composable("pedido/{n}") { back ->
            val n = back.arguments?.getString("n").orEmpty()
            PedidoScreen(numero = n, aoVoltar = { nav.popBackStack("home", inclusive = false) }, aoPix = { nav.navigate("pix/$n") })
        }
        composable("pix/{n}") { back ->
            val n = back.arguments?.getString("n").orEmpty()
            PixScreen(numero = n, aoVoltar = { nav.popBackStack() }, aoInicio = { nav.popBackStack("home", inclusive = false) })
        }
    }
}
