package br.com.armarinhomanaos.bottons.ui

import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/** Gera o bitmap de um QR (ZXing core). */
fun qrBitmap(texto: String, tam: Int = 640): Bitmap {
    val m = QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, tam, tam, mapOf(EncodeHintType.MARGIN to 1))
    val bmp = Bitmap.createBitmap(tam, tam, Bitmap.Config.RGB_565)
    for (x in 0 until tam) for (y in 0 until tam) bmp.setPixel(x, y, if (m[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
    return bmp
}

fun bitmapDeBase64(b64: String): Bitmap? = runCatching {
    val bytes = Base64.decode(b64, Base64.DEFAULT)
    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}.getOrNull()

/** QR sempre sobre fundo branco (no escuro também, senão o leitor não lê). */
@Composable
fun QrImagem(texto: String, tam: Dp, modifier: Modifier = Modifier) {
    val bmp = remember(texto) { qrBitmap(texto) }
    Image(bmp.asImageBitmap(), "QR", modifier.size(tam).clip(RoundedCornerShape(12.dp)).background(Color.White).padding(8.dp))
}
