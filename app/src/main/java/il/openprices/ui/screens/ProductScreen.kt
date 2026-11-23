package il.openprices.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import il.openprices.AppViewModel
import il.openprices.data.StorePrice
import il.openprices.ui.theme.LocalExtra

@Composable
fun ProductScreen(vm: AppViewModel) {
    val p = vm.product ?: return
    val prices = vm.prices
    val best = prices.firstOrNull()?.effective
    val inList = vm.inList(p.id)

    Column(Modifier.fillMaxSize()) {
        TopBar("", onBack = { vm.back() })
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(20.dp)
                ) {
                    Text(p.name, style = MaterialTheme.typography.headlineSmall)
                    val sub = listOf(p.size, p.manufacturer).filter { it.isNotBlank() }.joinToString(" · ")
                    if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (p.barcode.isNotBlank()) Text("ברקוד ${p.barcode}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text("המחיר הזול ביותר", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(money(best ?: p.minPrice), style = MaterialTheme.typography.displaySmall,
                                color = LocalExtra.current.best)
                        }
                        if (p.maxPrice > p.minPrice) Column(horizontalAlignment = Alignment.End) {
                            Text("פער", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(money(p.maxPrice - p.minPrice), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    if (inList) {
                        FilledTonalButton(onClick = { vm.toggleList(p) }, Modifier.fillMaxWidth().height(48.dp)) {
                            Icon(Icons.Rounded.Check, null); Spacer(Modifier.width(8.dp)); Text("ברשימה · הסרה")
                        }
                    } else {
                        Button(onClick = { vm.toggleList(p) }, Modifier.fillMaxWidth().height(48.dp)) {
                            Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("הוספה לסל")
                        }
                    }
                }
            }
            item {
                Text("${prices.size} סניפים", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
            }
            if (prices.isEmpty()) item {
                CircularProgressIndicator(Modifier.padding(24.dp))
            }
            itemsIndexed(prices, key = { _, s -> s.storeId }) { i, s -> StoreRow(s, cheapest = i == 0 || s.effective == best) }
        }
    }
}

@Composable
private fun StoreRow(s: StorePrice, cheapest: Boolean) {
    val extra = LocalExtra.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(if (cheapest) extra.bestContainer else MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChainDot(s.chain)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(s.chain, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(listOf(s.storeName, s.address).filter { it.isNotBlank() }.distinct().joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(money(s.effective), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (cheapest) extra.best else MaterialTheme.colorScheme.onSurface)
                if (s.effective < s.price) Text(money(s.price), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (!s.promoText.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.clip(RoundedCornerShape(10.dp)).background(extra.promoContainer).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.LocalOffer, null, Modifier.size(14.dp), tint = extra.promo)
                Spacer(Modifier.width(6.dp))
                val end = s.promoEnd?.take(10)?.split("-")?.reversed()?.joinToString("/")
                Text(s.promoText + (if (!end.isNullOrBlank()) " · עד $end" else ""),
                    style = MaterialTheme.typography.labelMedium, color = extra.promo, maxLines = 2)
            }
        }
    }
}
