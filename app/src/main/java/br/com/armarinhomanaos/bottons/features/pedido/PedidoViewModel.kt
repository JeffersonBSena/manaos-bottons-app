package br.com.armarinhomanaos.bottons.features.pedido

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.armarinhomanaos.bottons.Graph
import br.com.armarinhomanaos.bottons.data.Network
import br.com.armarinhomanaos.bottons.data.api.*
import br.com.armarinhomanaos.bottons.util.Imagem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

data class PedidoState(
    val pedido: Pedido? = null,
    val carregando: Boolean = true,
    val enviandoItem: Int? = null,
    val ocupado: Boolean = false,
    val erro: String? = null,
    val pedirCpf: Boolean = false,
    val pix: Pix? = null,
)

/** Estado do pedido com polling (2,5 s enquanto houver arte processando / aguardando pagamento). */
class PedidoViewModel(private val numero: String) : ViewModel() {
    private val _s = MutableStateFlow(PedidoState())
    val s: StateFlow<PedidoState> = _s
    private var poll: Job? = null

    init { iniciarPolling() }

    private fun iniciarPolling() {
        poll?.cancel()
        poll = viewModelScope.launch {
            while (true) {
                recarregar()
                val p = _s.value.pedido
                val rapido = p == null || p.items.any { it.art?.processando == true } || p.aguardandoPix || p.emAprovacao
                delay(if (rapido) 2500 else 8000)
            }
        }
    }

    suspend fun recarregar() {
        runCatching { Graph.api().pedido(numero) }
            .onSuccess { p -> _s.update { it.copy(pedido = p, carregando = false, pix = p.pix ?: it.pix) } }
            .onFailure { e -> _s.update { it.copy(carregando = false, erro = it.erro ?: "Sem conexão: ${e.message?.take(60)}") } }
    }

    fun limparErro() = _s.update { it.copy(erro = null) }

    fun enviarArte(ctx: Context, item: Item, uri: Uri) {
        viewModelScope.launch {
            _s.update { it.copy(enviandoItem = item.id, erro = null) }
            val parte = withContext(Dispatchers.IO) { Imagem.parteMultipart(ctx, uri) }
            if (parte == null) { _s.update { it.copy(enviandoItem = null, erro = "Não consegui ler essa imagem.") }; return@launch }
            val params = Network.json.encodeToString(Params.serializer(), Params()).toRequestBody("application/json".toMediaType())
            runCatching { Graph.api().enviarArte(numero, item.id, parte, params) }
                .onSuccess { r -> if (!r.isSuccessful) _s.update { it.copy(erro = Network.erro(r).message) } }
                .onFailure { e -> _s.update { it.copy(erro = "Falha no envio: ${e.message?.take(60)}") } }
            _s.update { it.copy(enviandoItem = null) }
            recarregar()
        }
    }

    fun ajustar(item: Item, p: Params) {
        viewModelScope.launch {
            _s.update { it.copy(ocupado = true, erro = null) }
            runCatching { Graph.api().ajustarArte(numero, item.id, p) }
                .onSuccess { r -> if (!r.isSuccessful) _s.update { it.copy(erro = Network.erro(r).message) } }
                .onFailure { e -> _s.update { it.copy(erro = "Falha: ${e.message?.take(60)}") } }
            _s.update { it.copy(ocupado = false) }
            recarregar()
        }
    }

    /** Aprova e gera o Pix. Se o servidor pedir CPF, abre o diálogo. */
    fun aprovar(cpf: String? = null, aoPix: () -> Unit) {
        viewModelScope.launch {
            _s.update { it.copy(ocupado = true, erro = null, pedirCpf = false) }
            runCatching { Graph.api().aprovar(numero, AprovarBody(cpf?.filter { c -> c.isDigit() }?.ifBlank { null })) }
                .onSuccess { r ->
                    if (r.isSuccessful && r.body() != null) {
                        _s.update { it.copy(pedido = r.body()!!.order, pix = r.body()!!.pix) }
                        aoPix()
                    } else {
                        val e = Network.erro(r)
                        _s.update { it.copy(pedirCpf = e.needs_cpf, erro = if (e.needs_cpf) null else e.message) }
                    }
                }
                .onFailure { e -> _s.update { it.copy(erro = "Falha: ${e.message?.take(60)}") } }
            _s.update { it.copy(ocupado = false) }
        }
    }

    fun fecharCpf() = _s.update { it.copy(pedirCpf = false) }

    fun pagarManual(metodo: String) {
        viewModelScope.launch {
            _s.update { it.copy(ocupado = true, erro = null) }
            runCatching { Graph.api().pagarManual(numero, PagarManualBody(metodo)) }
                .onSuccess { r -> if (r.isSuccessful && r.body() != null) _s.update { it.copy(pedido = r.body()) } else _s.update { it.copy(erro = Network.erro(r).message) } }
                .onFailure { e -> _s.update { it.copy(erro = "Falha: ${e.message?.take(60)}") } }
            _s.update { it.copy(ocupado = false) }
        }
    }

    fun cancelar(aoFim: () -> Unit) {
        viewModelScope.launch {
            runCatching { Graph.api().cancelar(numero) }
                .onSuccess { r -> if (r.isSuccessful) aoFim() else _s.update { it.copy(erro = Network.erro(r).message) } }
                .onFailure { e -> _s.update { it.copy(erro = "Falha: ${e.message?.take(60)}") } }
        }
    }
}

class PedidoVmFactory(private val n: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = PedidoViewModel(n) as T
}
