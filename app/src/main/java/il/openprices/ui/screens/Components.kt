package il.openprices.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale

fun money(v: Double): String = "₪" + String.format(Locale.US, "%.2f", v)

/** Stable, distinct colour per chain name. */
private val chainPalette = listOf(
    Color(0xFF0E9F5A), Color(0xFF1570EF), Color(0xFFD92D20), Color(0xFFDC6803), Color(0xFF7A5AF8),
    Color(0xFF0E7090), Color(0xFFC11574), Color(0xFF4E5BA6), Color(0xFF669F2A), Color(0xFFB54708),
)

fun chainColor(chain: String): Color = chainPalette[(chain.hashCode() and 0x7fffffff) % chainPalette.size]

@Composable
fun ChainDot(chain: String, size: Int = 36) {
    val c = chainColor(chain)
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape(10.dp)).background(c.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            chain.trim().take(1),
            color = c,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
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
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            // auto-mirrored: points right in RTL
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "חזרה") }
        } else {
            Box(Modifier.size(12.dp))
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
fun Pill(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(CircleShape).background(bg).padding(horizontal = 10.dp, vertical = 3.dp),
    ) { Text(text, color = fg, style = MaterialTheme.typography.labelMedium, maxLines = 1) }
}
