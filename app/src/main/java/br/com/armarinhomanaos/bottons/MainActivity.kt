package br.com.armarinhomanaos.bottons

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.armarinhomanaos.bottons.navigation.AppNav
import br.com.armarinhomanaos.bottons.ui.theme.BottonsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Quiosque: a tela nunca apaga enquanto o app estiver na frente (a tela de descanso cuida do resto).
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent { BottonsTheme { AppNav() } }
    }
}
