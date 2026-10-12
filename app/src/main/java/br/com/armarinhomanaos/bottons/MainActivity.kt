package br.com.armarinhomanaos.bottons

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.armarinhomanaos.bottons.navigation.AppNav
import br.com.armarinhomanaos.bottons.ui.theme.BottonsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { BottonsTheme { AppNav() } }
    }
}
