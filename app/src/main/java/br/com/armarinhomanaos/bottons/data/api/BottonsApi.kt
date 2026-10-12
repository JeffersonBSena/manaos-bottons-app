package br.com.armarinhomanaos.bottons.data.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface BottonsApi {
    @GET("products") suspend fun produtos(): ProdutosResp
    @GET("orders") suspend fun pedidos(): List<Pedido>
    @POST("orders") suspend fun criar(@Body b: NovoPedidoBody): Response<Pedido>
    @GET("orders/{n}") suspend fun pedido(@Path("n") n: String): Pedido

    @Multipart
    @POST("orders/{n}/items/{item}/art")
    suspend fun enviarArte(@Path("n") n: String, @Path("item") item: Int, @Part file: MultipartBody.Part, @Part("params") params: RequestBody): Response<UploadResp>

    @PATCH("orders/{n}/items/{item}/art")
    suspend fun ajustarArte(@Path("n") n: String, @Path("item") item: Int, @Body p: Params): Response<AjusteResp>

    @POST("orders/{n}/items/{item}/art/copy")
    suspend fun copiarArte(@Path("n") n: String, @Path("item") item: Int, @Body b: CopiarBody = CopiarBody()): Response<Pedido>

    @GET("orders/{n}/items/{item}/original")
    suspend fun original(@Path("n") n: String, @Path("item") item: Int): Response<okhttp3.ResponseBody>

    @POST("orders/{n}/approve") suspend fun aprovar(@Path("n") n: String, @Body b: AprovarBody): Response<AprovarResp>
    @POST("orders/{n}/pay/manual") suspend fun pagarManual(@Path("n") n: String, @Body b: PagarManualBody): Response<Pedido>
    @POST("orders/{n}/cancel") suspend fun cancelar(@Path("n") n: String): Response<Pedido>
}
