package br.com.armarinhomanaos.bottons.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Paleta do app — mesma estrutura do Campanha 2026 (AppColor): cores de marca fixas,
 * neutros que trocam com o modo escuro via `escuro` (mutableState lido pelos composables).
 * Marca Armarinho Manaós: rosa (pink-600 da loja) + âmbar.
 */
object AppColor {
    var escuro by mutableStateOf(false)
        internal set

    // Marca
    val Navy = Color(0xFF5B1A3A)        // vinho (cabeçalhos/gradientes)
    val NavyDark = Color(0xFF3F1129)
    val Primary = Color(0xFFDB2777)     // rosa — tint global
    val PrimaryDark = Color(0xFFBE185D)
    val Orange = Color(0xFFF59E0B)      // âmbar — CTA secundário/destaques
    val OrangeDark = Color(0xFFD97706)

    val NavyTinta: Color get() = if (escuro) Color(0xFFF9A8D4) else Navy

    // Semânticas
    val Green = Color(0xFF2E9E5B)
    val Red = Color(0xFFD63027)
    val Amber = Color(0xFFE8A100)
    val Blue = Color(0xFF2563EB)
    val Slate = Color(0xFF64748B)

    // Neutros (claro = systemGroupedBackground do iOS; escuro = superfícies iOS dark)
    val Bg: Color get() = if (escuro) Color(0xFF000000) else Color(0xFFF2F2F7)
    val Card: Color get() = if (escuro) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val Field: Color get() = if (escuro) Color(0xFF2C2C2E) else Color(0xFFF3F4F6)
    val FieldBorder: Color get() = if (escuro) Color(0xFF3A3A3C) else Color(0xFFD8DEE6)
    val Border: Color get() = if (escuro) Color(0xFF38383A) else Color(0xFFE4E6EA)
    val TextPrimary: Color get() = if (escuro) Color(0xFFF2F2F7) else Color(0xFF1C2733)
    val TextSecond: Color get() = if (escuro) Color(0xFFAEAEB2) else Color(0xFF5A6875)
    val TextMuted: Color get() = if (escuro) Color(0xFF8E8E93) else Color(0xFF9AA4B0)

    // Soft
    val RedSoft: Color get() = if (escuro) Color(0xFF3B2422) else Color(0xFFFBEAE8)
    val AmberSoft: Color get() = if (escuro) Color(0xFF3B3320) else Color(0xFFFDF3DC)
    val BlueSoft: Color get() = if (escuro) Color(0xFF1E2A44) else Color(0xFFE3ECFB)
    val GraySoft: Color get() = if (escuro) Color(0xFF2C2C2E) else Color(0xFFEDEFF2)
    val GreenSoft: Color get() = if (escuro) Color(0xFF1F3628) else Color(0xFFE3F4EA)
    val OrangeSoft: Color get() = if (escuro) Color(0xFF3E2E18) else Color(0xFFFEF3DF)
    val PrimarySoft: Color get() = if (escuro) Color(0xFF3B1F2E) else Color(0xFFFCE7F3)

    // Farol dos status do pedido
    val FarolOk = Color(0xFF34C759)
    val FarolPend = Color(0xFFFF9F0A)
    val FarolInfo = Color(0xFF0A84FF)
    val FarolNao = Color(0xFFFF3B30)
}
