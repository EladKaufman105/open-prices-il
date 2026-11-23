package il.openprices.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import il.openprices.AppViewModel
import il.openprices.data.BasketStore
import il.openprices.ui.theme.LocalExtra

@Composable
fun BasketScreen(vm: AppViewModel) {
    val extra = LocalExtra.current
    Column(Modifier.fillMaxSize()) {
        TopBar("הסל שלי")
        if (vm.list.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.ShoppingBasket, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Text("הסל ריק", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text("הוסיפו מוצרים מעמוד המוצר, ונמצא את הסניף שבו הסל כולו הכי זול.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            return
        }
        val total = vm.list.size
        // best branch of each chain, complete baskets first
        val perChain = vm.basket.groupBy { it.chain }.map { (_, v) -> v.first() }
            .sortedWith(compareByDescending<BasketStore> { it.found }.thenBy { it.total })
        val complete = perChain.filter { it.found == total }
        val best = complete.firstOrNull()
        val worst = complete.lastOrNull()

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (best != null) item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(extra.bestContainer).padding(20.dp)
                ) {
                    Text("הסל הכי זול", style = MaterialTheme.typography.labelLarge, color = extra.best)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChainLogo(best.chain, 40)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(best.chain, style = MaterialTheme.typography.titleMedium)
                            Text(best.storeName, style = MaterialTheme.typography.bodySmall, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(money(best.total), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = extra.best)
                    }
                    if (worst != null && worst.total - best.total >= 0.5) {
                        Spacer(Modifier.height(8.dp))
                        Text("חיסכון של ${money(worst.total - best.total)} לעומת ${worst.chain}",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item { SectionTitle("$total מוצרים") }
            item {
                Column(Modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainerLowest)) {
                    vm.list.forEachIndexed { i, it ->
                        if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Row(Modifier.padding(start = 16.dp, end = 10.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(it.name, Modifier.weight(1f).clickable { vm.openById(it.productId) },
                                style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            FilledTonalIconButton(onClick = { vm.setQty(it.productId, it.qty - 1) }, Modifier.size(36.dp)) {
                                Icon(Icons.Rounded.Remove, "פחות", Modifier.size(18.dp))
                            }
                            Text("${it.qty}", Modifier.width(32.dp), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                            FilledTonalIconButton(onClick = { vm.setQty(it.productId, it.qty + 1) }, Modifier.size(36.dp)) {
                                Icon(Icons.Rounded.Add, "עוד", Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
            item { SectionTitle("השוואה בין רשתות (הסניף הזול בכל רשת)") }
            items(perChain, key = { it.storeId }) { b -> BasketRow(b, total, cheapest = b == best) }
        }
    }
}

@Composable
private fun BasketRow(b: BasketStore, total: Int, cheapest: Boolean) {
    val extra = LocalExtra.current
    val complete = b.found == total
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChainLogo(b.chain, 44)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(b.chain, style = MaterialTheme.typography.titleMedium)
                    if (cheapest) { Spacer(Modifier.width(8.dp)); CheapestBadge() }
                }
                Text(listOf(b.storeName, b.address).filter { it.isNotBlank() }.distinct().joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(money(b.total), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = if (cheapest) extra.best else MaterialTheme.colorScheme.onSurface)
                Text(if (complete) "כל המוצרים" else "${b.found} מתוך $total", style = MaterialTheme.typography.labelSmall,
                    color = if (complete) MaterialTheme.colorScheme.onSurfaceVariant else extra.promo)
            }
        }
        if (b.missing.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("חסר: " + b.missing.joinToString(", "), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
