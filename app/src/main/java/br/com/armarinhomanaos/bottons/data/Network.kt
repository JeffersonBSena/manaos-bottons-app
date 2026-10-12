package br.com.armarinhomanaos.bottons.data

import br.com.armarinhomanaos.bottons.data.api.BottonsApi
import br.com.armarinhomanaos.bottons.data.api.ErroApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object Network {
    val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true; explicitNulls = false }

    fun create(prefs: Prefs): BottonsApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).writeTimeout(120, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder().header("Accept", "application/json")
                    .apply { prefs.token?.let { header("Authorization", "Bearer $it") } }.build()
                chain.proceed(req)
            }
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()
        return Retrofit.Builder().baseUrl(prefs.baseUrl).client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build().create(BottonsApi::class.java)
    }

    /** Mensagem legível de uma resposta de erro (422/401/5xx). */
    fun <T> erro(r: Response<T>): ErroApi {
        val corpo = runCatching { r.errorBody()?.string() }.getOrNull().orEmpty()
        return runCatching { json.decodeFromString(ErroApi.serializer(), corpo) }.getOrElse {
            ErroApi(message = when (r.code()) {
                401 -> "Token inválido. Confira a configuração."
                404 -> "Não encontrado."
                else -> "Erro ${r.code()} no servidor."
            })
        }
    }
}
