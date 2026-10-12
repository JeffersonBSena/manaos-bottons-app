package br.com.armarinhomanaos.bottons.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Lê a foto (câmera/galeria), corrige EXIF e manda como JPEG q92 com lado maior até 3000 px —
 * botton de 58 mm precisa de ~850 px no diâmetro de corte a 300 DPI, então não reduz muito.
 */
object Imagem {
    fun parteMultipart(ctx: Context, uri: Uri, maxLado: Int = 3000): MultipartBody.Part? {
        val jpeg = comprimir(ctx, uri, maxLado) ?: return null
        return MultipartBody.Part.createFormData("file", "foto.jpg", jpeg.toRequestBody("image/jpeg".toMediaType()))
    }

    private fun inSample(w: Int, h: Int, maxLado: Int): Int {
        var s = 1; var maior = maxOf(w, h)
        while (maior > 0 && maior / 2 >= maxLado) { maior /= 2; s *= 2 }
        return s
    }

    private fun comprimir(ctx: Context, uri: Uri, maxLado: Int): ByteArray? {
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) } ?: return null
        val opts = BitmapFactory.Options().apply { inSampleSize = inSample(limites.outWidth, limites.outHeight, maxLado) }
        val original = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        val rot = orientar(ctx, uri, original)
        val maior = maxOf(rot.width, rot.height)
        val bmp = if (maior > maxLado) {
            val e = maxLado.toFloat() / maior
            Bitmap.createScaledBitmap(rot, (rot.width * e).toInt(), (rot.height * e).toInt(), true)
        } else rot
        return ByteArrayOutputStream().use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 92, out); out.toByteArray() }
    }

    private fun orientar(ctx: Context, uri: Uri, bmp: Bitmap): Bitmap {
        val o = runCatching {
            ctx.contentResolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
        val m = Matrix()
        when (o) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            else -> return bmp
        }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }

    fun arquivoCamera(ctx: Context): Pair<File, Uri> {
        val dir = File(ctx.cacheDir, "fotos").apply { mkdirs() }
        val f = File.createTempFile("captura_", ".jpg", dir)
        return f to FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
    }
}
