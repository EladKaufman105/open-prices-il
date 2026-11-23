package il.openprices.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import il.openprices.ui.theme.LocalExtra
import java.util.Locale

fun money(v: Double): String = "₪" + String.format(Locale.US, "%.2f", v)

/** Chain display name (as written by the pipeline) -> drawable suffix (logo_<slug>). */
private val chainSlugs = mapOf(
    "שופרסל" to "shufersal", "רמי לוי" to "rami_levy", "אושר עד" to "osher_ad", "יוחננוף" to "yohananof",
    "טיב טעם" to "tiv_taam", "סטופ מרקט" to "stop_market", "סופר יודה" to "super_yuda", "קשת טעמים" to "keshet",
    "פוליצר" to "polizer", "סיטי מרקט" to "city_market", "פרשמרקט" to "fresh_market", "יילו" to "yellow",
    "אלונית / AM:PM" to "dor_alon", "סאלח דבאח" to "salach_dabach", "קינג סטור" to "king_store",
    "מעיין 2000" to "maayan_2000", "שפע ברכת השם" to "shefa", "שוק העיר" to "shuk_ahir", "סופר ספיר" to "super_sapir",
    "ברקת" to "bareket", "גוד פארם" to "good_pharm", "וולט מרקט" to "wolt", "זול ובגדול" to "zol_vebegadol",
    "משנת יוסף" to "mishnat_yosef", "ויקטורי" to "victory", "קרפור" to "carrefour", "חצי חינם" to "hazi_hinam",
    "סופר-פארם" to "super_pharm", "מחסני השוק" to "mahsani_ashuk",
)

private val chainPalette = listOf(
    Color(0xFF0E9F5A), Color(0xFF1570EF), Color(0xFFD92D20), Color(0xFFDC6803), Color(0xFF7A5AF8),
    Color(0xFF0E7090), Color(0xFFC11574), Color(0xFF4E5BA6), Color(0xFF669F2A), Color(0xFFB54708),
)

/** Brand-ish tile colours for chains without a logo image. */
private val brandColors = mapOf(
    "שופרסל" to Color(0xFFE30613), "רמי לוי" to Color(0xFF0057A8), "יוחננוף" to Color(0xFFD7262D),
    "טיב טעם" to Color(0xFF00843D), "אושר עד" to Color(0xFF0072BC), "קשת טעמים" to Color(0xFF7A3E9D),
    "סיטי מרקט" to Color(0xFFD71920), "פרשמרקט" to Color(0xFF6AA121), "סופר יודה" to Color(0xFF1B4F9C),
    "פוליצר" to Color(0xFF0E7090), "קינג סטור" to Color(0xFFB8860B), "שוק העיר" to Color(0xFFDC6803),
    "מעיין 2000" to Color(0xFF1570EF), "סאלח דבאח" to Color(0xFF2E7D32), "שפע ברכת השם" to Color(0xFF4E5BA6),
    "ברקת" to Color(0xFFC11574), "משנת יוסף" to Color(0xFF475467),
)

fun chainColor(chain: String): Color =
    brandColors[chain] ?: chainPalette[(chain.hashCode() and 0x7fffffff) % chainPalette.size]

/** Store logo on a white tile (logos are designed for light backgrounds); falls back to an initial. */
@SuppressLint("DiscouragedApi")
@Composable
fun ChainLogo(chain: String, size: Int = 40) {
    val context = LocalContext.current
    val res = remember(chain) {
        chainSlugs[chain]?.let { context.resources.getIdentifier("logo_$it", "drawable", context.packageName) } ?: 0
    }
    val shape = RoundedCornerShape((size * 0.28f).dp)
    if (res != 0) {
        Box(
            Modifier.size(size.dp).clip(shape).background(Color.White)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(res), chain, Modifier.size((size * 0.78f).dp), contentScale = ContentScale.Fit)
        }
    } else {
        // no logo file: the chain's name as a small wordmark on its brand colour
        val c = chainColor(chain)
        val name = chain.substringBefore(" /").trim()
        Box(Modifier.size(size.dp).clip(shape).background(c).padding(2.dp), contentAlignment = Alignment.Center) {
            if (size >= 36 && name.length <= 12) Text(
                name, color = Color.White, textAlign = TextAlign.Center, maxLines = 2,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold, fontSize = (size * 0.24f).sp, lineHeight = (size * 0.27f).sp,
                ),
            ) else Text(
                name.take(1), color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
fun TopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            // auto-mirrored: points right in RTL
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "חזרה") }
        } else {
            Spacer(Modifier.width(16.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), content = actions)
    }
}

@Composable
fun Pill(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier, bold: Boolean = false) {
    Box(modifier.clip(CircleShape).background(bg).padding(horizontal = 10.dp, vertical = 3.dp)) {
        Text(
            text, color = fg, maxLines = 1,
            style = MaterialTheme.typography.labelMedium.let { if (bold) it.copy(fontWeight = FontWeight.SemiBold) else it },
        )
    }
}

@Composable
fun CheapestBadge() {
    val e = LocalExtra.current
    Pill("הכי זול", e.bestContainer, e.best, bold = true)
}

/** Small inline promotion tag: icon + one or two lines of text. */
@Composable
fun PromoTag(text: String, end: String?, modifier: Modifier = Modifier) {
    val e = LocalExtra.current
    Row(
        modifier.clip(RoundedCornerShape(10.dp)).background(e.promoContainer).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.LocalOffer, null, Modifier.size(14.dp), tint = e.promo)
        Spacer(Modifier.width(6.dp))
        val until = end?.take(10)?.split("-")?.takeIf { it.size == 3 }?.let { "${it[2]}/${it[1]}" }
        Text(
            text + (if (until != null) "  ·  עד $until" else ""),
            style = MaterialTheme.typography.labelMedium, color = e.promo, maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
    )
}
