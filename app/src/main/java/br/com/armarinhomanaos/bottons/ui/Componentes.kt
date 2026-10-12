package br.com.armarinhomanaos.bottons.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.armarinhomanaos.bottons.ui.theme.AppColor

/** Barra superior padrão (igual BarraTopo do Campanha 2026). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraTopo(titulo: String, aoVoltar: (() -> Unit)? = null, acoes: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(titulo, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold) },
        navigationIcon = { if (aoVoltar != null) IconButton(onClick = aoVoltar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
        actions = acoes,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = AppColor.Primary,
        ),
    )
}

/** Cabeçalho de seção em caixa alta (List insetGrouped do iOS). */
@Composable
fun TituloSecao(texto: String, modifier: Modifier = Modifier, direita: String? = null) {
    Row(modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 2.dp)) {
        Text(texto.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        if (direita != null) Text(direita.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun RodapeSecao(texto: String) {
    Text(texto, fontSize = 12.sp, color = AppColor.TextSecond, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp))
}

/** Cartão de seção. */
@Composable
fun CartaoSecao(modifier: Modifier = Modifier, conteudo: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColor.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) { Column(content = conteudo) }
}

@Composable
fun DivisorLinha(recuo: Dp = 16.dp) {
    HorizontalDivider(Modifier.padding(start = recuo), color = AppColor.Border)
}

@Composable
fun Bolinha(cor: Color, tam: Dp = 10.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(tam).clip(CircleShape).background(cor))
}

/** Pílula de status (fundo suave na cor). */
@Composable
fun Pilula(texto: String, cor: Color, modifier: Modifier = Modifier) {
    Text(texto, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = cor, maxLines = 1,
        modifier = modifier.clip(RoundedCornerShape(999.dp)).background(cor.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 4.dp))
}

/** Linha de lista com bolinha do farol, título, secundário e conteúdo à direita. */
@Composable
fun LinhaFarol(
    cor: Color, principal: String, secundario: String? = null, onClick: (() -> Unit)? = null,
    direita: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Bolinha(cor)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(principal, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AppColor.TextPrimary)
            secundario?.takeIf { it.isNotBlank() }?.let { Text(it, fontSize = 13.sp, color = AppColor.TextSecond) }
        }
        direita?.invoke(this)
    }
}

/** Campo rotulado estilo iOS (rótulo acima, caixa preenchida, erro/dica abaixo). */
@Composable
fun CampoRotulado(
    label: String, modifier: Modifier = Modifier, obrigatorio: Boolean = false, erro: String? = null, dica: String? = null,
    conteudo: @Composable RowScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColor.TextPrimary)
            if (obrigatorio) Text("*", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppColor.Red)
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(AppColor.Field)
                .border(1.dp, if (erro == null) AppColor.FieldBorder else AppColor.Red, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), content = conteudo,
        )
        when {
            erro != null -> Text(erro, fontSize = 12.sp, color = AppColor.Red)
            dica != null -> Text(dica, fontSize = 12.sp, color = AppColor.TextSecond)
        }
    }
}

@Composable
fun AppTextField(
    label: String, valor: String, onChange: (String) -> Unit, modifier: Modifier = Modifier,
    tipo: KeyboardType = KeyboardType.Text, placeholder: String = "", erro: String? = null, dica: String? = null,
    senha: Boolean = false,
) {
    val obrig = label.trimEnd().endsWith("*")
    val rotulo = if (obrig) label.trimEnd().removeSuffix("*").trim() else label
    CampoRotulado(label = rotulo, modifier = modifier, obrigatorio = obrig, erro = erro, dica = dica) {
        Box(Modifier.weight(1f)) {
            if (valor.isEmpty() && placeholder.isNotEmpty()) Text(placeholder, fontSize = 16.sp, color = AppColor.TextMuted)
            BasicTextField(
                value = valor, onValueChange = onChange, singleLine = !senha, maxLines = if (senha) 3 else 1,
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = AppColor.TextPrimary),
                keyboardOptions = KeyboardOptions(keyboardType = tipo),
                cursorBrush = SolidColor(AppColor.Primary), modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Botão principal (rosa, 50dp, cheio). */
@Composable
fun BotaoPrimario(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, habilitado: Boolean = true,
                  carregando: Boolean = false, cor: Color = AppColor.Primary, icone: (@Composable () -> Unit)? = null) {
    Button(
        onClick = onClick, enabled = habilitado && !carregando,
        colors = ButtonDefaults.buttonColors(containerColor = cor, disabledContainerColor = cor.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(14.dp), modifier = modifier.fillMaxWidth().height(52.dp),
    ) {
        if (carregando) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        else {
            icone?.let { it(); Spacer(Modifier.width(8.dp)) }
            Text(texto, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Botão secundário (contorno). */
@Composable
fun BotaoSecundario(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, habilitado: Boolean = true, cor: Color = AppColor.Primary) {
    OutlinedButton(
        onClick = onClick, enabled = habilitado, shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = cor),
        modifier = modifier.fillMaxWidth().height(52.dp),
    ) { Text(texto, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
}

fun Modifier.cardApp() = this.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AppColor.Card)
    .border(1.dp, AppColor.Border, RoundedCornerShape(16.dp)).padding(14.dp)
