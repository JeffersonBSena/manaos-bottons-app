package br.com.armarinhomanaos.bottons.data.api

import kotlinx.serialization.Serializable

@Serializable data class Spec(val botton_mm: Int = 0, val corte_mm: Int = 0, val seguro_mm: Int = 0, val print_px: Int = 0, val dpi: Int = 300)
@Serializable data class Produto(val id: Int, val sku: String = "", val name: String, val price_cents: Int = 0, val price: String = "", val image_url: String? = null, val spec: Spec = Spec())
@Serializable data class KitItem(val product_id: Int, val sku: String = "", val name: String = "", val qty: Int = 1, val unit_price_cents: Int = 0)
@Serializable data class Kit(
    val id: String, val name: String, val description: String = "", val items: List<KitItem> = emptyList(), val units: Int = 0,
    val subtotal_cents: Int = 0, val discount_cents: Int = 0, val price_cents: Int = 0, val price: String = "", val discount_pct: Float = 0f,
)
@Serializable data class ProdutosResp(val min_units_per_order: Int = 5, val products: List<Produto> = emptyList(), val kits: List<Kit> = emptyList(), val inspiracoes: List<String> = emptyList())

@Serializable data class Params(val zoom: Float = 1f, val x: Float = 0f, val y: Float = 0f, val rotation: Float = 0f)
@Serializable data class Arte(
    val id: Int, val status: String = "uploaded", val preview_url: String? = null, val params: Params = Params(),
    val width: Int? = null, val height: Int? = null, val dpi_effective: Int? = null, val quality_warning: String? = null,
    val uploaded_via: String? = null, val error: String? = null,
) {
    val pronta get() = status == "ready"
    val processando get() = status == "uploaded" || status == "processing"
}
@Serializable data class Item(val id: Int, val product_id: Int, val name: String = "", val qty: Int = 1, val unit_price_cents: Int = 0, val spec: Spec? = null, val art: Arte? = null)
@Serializable data class Cliente(val name: String? = null, val phone: String? = null, val has_cpf: Boolean = false)
@Serializable data class Pix(
    val payment_id: String? = null, val status: String? = null, val amount_cents: Int = 0, val invoice_url: String? = null,
    val qr_base64: String? = null, val copia_cola: String? = null, val expires_at: String? = null,
)
@Serializable data class Pedido(
    val number: String, val status: String = "", val status_label: String = "", val fulfillment: String = "pickup",
    val art_code: String? = null, val upload_url: String? = null, val customer: Cliente = Cliente(),
    val subtotal_cents: Int = 0, val discount_cents: Int = 0, val kit: String? = null,
    val total_cents: Int = 0, val total: String = "", val units: Int = 0, val all_arts_ready: Boolean = false,
    val items: List<Item> = emptyList(), val pix: Pix? = null, val sheets: Int = 0,
    val paid_at: String? = null, val sla_promised_at: String? = null, val created_at: String? = null,
) {
    val pago get() = status in setOf("paid", "in_production", "quality_check", "packing", "ready_for_pickup", "ready_to_ship", "shipped", "delivered", "completed", "rework")
    val emAprovacao get() = status == "awaiting_art" || status == "awaiting_approval"
    val aguardandoPix get() = status == "pending_payment"
    val encerrado get() = status in setOf("cancelled", "refunded", "completed", "delivered")
}

@Serializable data class ItemBody(val product_id: Int, val qty: Int)
@Serializable data class NovoPedidoBody(val name: String, val phone: String, val cpf: String? = null, val fulfillment: String = "pickup", val items: List<ItemBody>, val kit_id: String? = null)
@Serializable data class AprovarBody(val cpf: String? = null)
@Serializable data class PagarManualBody(val method: String)
@Serializable data class UploadResp(val art: Arte, val order: Pedido? = null)
@Serializable data class AjusteResp(val art: Arte)
@Serializable data class AprovarResp(val order: Pedido, val pix: Pix? = null)
@Serializable data class ErroApi(val message: String = "Erro", val needs_cpf: Boolean = false)
@Serializable data class CopiarBody(val to: List<Int> = emptyList())
