package il.openprices.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import il.openprices.AppViewModel
import il.openprices.data.BasketStore
import il.openprices.ui.theme.LocalExtra

@Composable
fun BasketScreen(vm: AppViewModel) {
    Column(Modifier.fillMaxSize()) {
        TopBar("הסל שלי", onBack = { vm.back() })
        if (vm.list.isEmpty()) {
            Column(Modifier.padding(24.dp)) {
                Text("הסל ריק", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text("הוסיפו מוצרים מעמוד המוצר, ונשווה את מחיר הסל כולו בין הסניפים בעיר.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(Modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainerLowest)) {
                    vm.list.forEachIndexed { i, it ->
                        if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(it.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 2,
                                overflow = TextOverflow.Ellipsis)
                            FilledTonalIconButton(onClick = { vm.setQty(it.productId, it.qty - 1) }, Modifier.size(34.dp)) {
                                Icon(Icons.Rounded.Remove, "פחות", Modifier.size(18.dp))
                            }
                            Text("${it.qty}", Modifier.width(32.dp), style = MaterialTheme.typography.titleMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            FilledTonalIconButton(onClick = { vm.setQty(it.productId, it.qty + 1) }, Modifier.size(34.dp)) {
                                Icon(Icons.Rounded.Add, "עוד", Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
            item {
                Text("השוואת סל", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
                Text("הסניפים ממוינים לפי כמות המוצרים שנמצאו ואז לפי המחיר הכולל",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp))
            }
            items(vm.basket.take(30), key = { it.storeId }) { b ->
                BasketRow(b, total = vm.list.size, cheapest = b == vm.basket.firstOrNull())
            }
        }
    }
}

@Composable
private fun BasketRow(b: BasketStore, total: Int, cheapest: Boolean) {
    val extra = LocalExtra.current
    val complete = b.found == total
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(if (cheapest && complete) extra.bestContainer else MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChainDot(b.chain)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(b.chain, style = MaterialTheme.typography.titleSmall)
                Text(listOf(b.storeName, b.address).filter { it.isNotBlank() }.distinct().joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(money(b.total), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = if (cheapest && complete) extra.best else MaterialTheme.colorScheme.onSurface)
                Box(
                    Modifier.clip(CircleShape)
                        .background(if (complete) MaterialTheme.colorScheme.secondaryContainer else extra.promoContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("${b.found}/$total מוצרים", style = MaterialTheme.typography.labelSmall,
                        color = if (complete) MaterialTheme.colorScheme.onSecondaryContainer else extra.promo)
                }
            }
        }
        if (b.missing.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text("חסר: " + b.missing.joinToString(", "), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
