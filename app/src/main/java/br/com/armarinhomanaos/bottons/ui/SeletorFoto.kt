package br.com.armarinhomanaos.bottons.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import br.com.armarinhomanaos.bottons.util.Imagem

/** Câmera ou galeria (mesmo diálogo do Campanha 2026). */
@Composable
fun SeletorFotoDialog(visivel: Boolean, aoFechar: () -> Unit, aoEscolher: (Uri) -> Unit) {
    val ctx = LocalContext.current
    var uriCamera by remember { mutableStateOf<Uri?>(null) }

    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) aoEscolher(uri) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> uriCamera?.let { if (ok) aoEscolher(it) } }

    fun abrirCamera() {
        val (_, uri) = Imagem.arquivoCamera(ctx)
        uriCamera = uri
        camera.launch(uri)
    }
    val permissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) abrirCamera() }
    fun tirarFoto() {
        val tem = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (tem) abrirCamera() else permissao.launch(Manifest.permission.CAMERA)
    }

    if (!visivel) return
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Arte do botton") },
        text = { Text("De onde vem a foto? Use a maior qualidade possível.") },
        confirmButton = { TextButton(onClick = { aoFechar(); tirarFoto() }) { Text("Tirar foto") } },
        dismissButton = {
            TextButton(onClick = { aoFechar(); galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Galeria") }
        },
    )
}
