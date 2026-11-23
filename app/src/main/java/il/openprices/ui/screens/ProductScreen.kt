package il.openprices.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import il.openprices.AppViewModel
import il.openprices.data.ChainGroup
import il.openprices.data.StorePrice
import il.openprices.ui.theme.LocalExtra

@Composable
fun ProductScreen(vm: AppViewModel) {
    val p = vm.product ?: return
    val groups = vm.groups
    val cheapest = groups.firstOrNull()?.best
    val extra = LocalExtra.current

    Column(Modifier.fillMaxSize()) {
        TopBar("", onBack = { vm.back() })
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            // ---- product header ----
            item {
                Column(Modifier.padding(horizontal = 4.dp)) {
                    Text(p.name, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (p.size.isNotBlank()) Pill(p.size, MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurface)
                        if (p.manufacturer.isNotBlank()) Pill(p.manufacturer, MaterialTheme.colorScheme.surfaceContainerHigh,
                            MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (p.barcode.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text("ברקוד ${p.barcode}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            // ---- price summary ----
            item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("הכי זול בעיר", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(money(cheapest ?: p.minPrice), style = MaterialTheme.typography.displaySmall
                            .copy(fontWeight = FontWeight.Bold), color = extra.best)
                        val cheapChains = groups.filter { it.best == cheapest }.map { it.chain }
                        if (cheapChains.isNotEmpty()) Text(
                            "ב" + cheapChains.joinToString(", "), style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Stat("${groups.size}", "רשתות")
                        Spacer(Modifier.height(8.dp))
                        Stat("${vm.prices.size}", "סניפים")
                        if (p.maxPrice > p.minPrice) {
                            Spacer(Modifier.height(8.dp))
                            Stat(money(p.maxPrice), "היקר ביותר")
                        }
                    }
                }
            }
            item { SectionTitle("מחירים לפי רשת") }
            if (vm.loadingPrices) item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            items(groups, key = { it.chain }) { g -> ChainCard(g, cheapest = g.best == cheapest) }
        }
        AddBar(vm)
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.End) {
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ChainCard(g: ChainGroup, cheapest: Boolean) {
    var open by rememberSaveable(g.chain) { mutableStateOf(false) }
    val extra = LocalExtra.current
    val rotation by animateFloatAsState(if (open) 180f else 0f, label = "chevron")
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(enabled = g.stores.size > 1) { open = !open }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChainLogo(g.chain, 44)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(g.chain, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false))
                    if (cheapest) { Spacer(Modifier.width(8.dp)); CheapestBadge() }
                }
                Text(
                    if (g.stores.size == 1) listOf(g.stores[0].storeName, g.stores[0].address).filter { it.isNotBlank() }
                        .distinct().joinToString(" · ") else "${g.stores.size} סניפים",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(money(g.best), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = if (cheapest) extra.best else MaterialTheme.colorScheme.onSurface)
                when {
                    g.worst > g.best -> Text("עד ${money(g.worst)}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    g.stores[0].effective < g.stores[0].price -> Text(money(g.stores[0].price),
                        style = MaterialTheme.typography.labelSmall.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (g.stores.size > 1) Icon(Icons.Rounded.ExpandMore, if (open) "סגירה" else "כל הסניפים",
                Modifier.padding(start = 4.dp).rotate(rotation), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        g.promo?.let { pr ->
            PromoTag(pr.promoText!!, pr.promoEnd, Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp))
        }
        AnimatedVisibility(open) {
            Column {
                g.stores.forEach { s ->
                    HorizontalDivider(Modifier.padding(horizontal = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    BranchRow(s)
                }
            }
        }
    }
}

@Composable
private fun BranchRow(s: StorePrice) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(s.storeName.ifBlank { s.address }, style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            if (s.address.isNotBlank() && s.address != s.storeName) Text(s.address, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(money(s.effective), style = MaterialTheme.typography.titleSmall)
    }
}

/** Sticky bottom bar: add to basket, or a quantity stepper once it is in the basket. */
@Composable
private fun AddBar(vm: AppViewModel) {
    val p = vm.product ?: return
    val qty = vm.qtyOf(p.id)
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 8.dp) {
        Row(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (qty == 0) {
                Button(onClick = { vm.addToList(p) }, Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Rounded.ShoppingBasket, null); Spacer(Modifier.width(8.dp))
                    Text("הוספה לסל", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Column(Modifier.weight(1f)) {
                    Text("בסל שלך", style = MaterialTheme.typography.titleMedium)
                    Text("${vm.list.size} מוצרים בסל", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalIconButton(onClick = { vm.setQty(p.id, qty - 1) }, Modifier.size(44.dp)) {
                    Icon(Icons.Rounded.Remove, "פחות")
                }
                Text("$qty", Modifier.width(44.dp), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                FilledTonalIconButton(onClick = { vm.setQty(p.id, qty + 1) }, Modifier.size(44.dp)) {
                    Icon(Icons.Rounded.Add, "עוד")
                }
            }
        }
    }
}
