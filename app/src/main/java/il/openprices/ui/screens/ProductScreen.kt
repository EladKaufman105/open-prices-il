package il.openprices.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import il.openprices.AppViewModel
import il.openprices.data.ChainGroup
import il.openprices.data.StorePrice
import il.openprices.ui.theme.LocalExtra

/** Product photo on a white tile; a neutral placeholder when there is none. */
@Composable
fun ProductImage(url: String, size: Int) {
    val shape = RoundedCornerShape((size * 0.22f).dp)
    Box(
        Modifier.size(size.dp).clip(shape).background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        val placeholder = @Composable {
            Box(Modifier.size(size.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Inventory2, null, Modifier.size((size * 0.45f).dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (url.isBlank()) placeholder() else SubcomposeAsyncImage(
            model = url, contentDescription = null, contentScale = ContentScale.Fit,
            modifier = Modifier.size((size * 0.9f).dp),
            loading = { placeholder() }, error = { placeholder() },
        )
    }
}

/** Price comparison for the selected product, emitted into the search screen's list. */
fun LazyListScope.productComparison(vm: AppViewModel) {
    val p = vm.product ?: return
    item(key = "header") { ProductHeader(vm) }
    if (vm.loadingPrices) item(key = "loading") {
        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }
    val cheapest = vm.groups.firstOrNull()?.best
    items(vm.groups, key = { "g-" + it.chain + p.id }) { g -> ChainCard(g, cheapest = g.best == cheapest) }
}

@Composable
private fun ProductHeader(vm: AppViewModel) {
    val p = vm.product ?: return
    val extra = LocalExtra.current
    val best = vm.groups.firstOrNull()
    val qty = vm.qtyOf(p.id)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProductImage(p.image, 88)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), maxLines = 3,
                    overflow = TextOverflow.Ellipsis)
                if (p.size.isNotBlank()) Text(p.size, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("הכי זול", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(money(best?.best ?: p.minPrice), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = extra.best)
                    if (best != null) {
                        Spacer(Modifier.width(10.dp))
                        ChainLogo(best.chain, 30)
                    }
                }
            }
            if (qty == 0) {
                Button(onClick = { vm.addToList(p) }, Modifier.height(44.dp)) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("לסל")
                }
            } else {
                FilledTonalIconButton(onClick = { vm.setQty(p.id, qty - 1) }, Modifier.size(40.dp)) { Icon(Icons.Rounded.Remove, "פחות") }
                Text("$qty", Modifier.width(36.dp), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                FilledTonalIconButton(onClick = { vm.setQty(p.id, qty + 1) }, Modifier.size(40.dp)) { Icon(Icons.Rounded.Add, "עוד") }
            }
        }
    }
}

@Composable
private fun ChainCard(g: ChainGroup, cheapest: Boolean) {
    var open by rememberSaveable(g.chain) { mutableStateOf(false) }
    val extra = LocalExtra.current
    val rotation by animateFloatAsState(if (open) 180f else 0f, label = "chevron")
    val single = g.stores.size == 1
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(enabled = !single) { open = !open }.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChainLogo(g.chain, 42)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(g.chain, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (single) g.stores[0].storeName.ifBlank { g.stores[0].address } else "${g.stores.size} סניפים",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(money(g.best), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (cheapest) extra.best else MaterialTheme.colorScheme.onSurface)
                val shelf = g.stores[0].price
                if (g.stores[0].effective < shelf) Text(money(shelf),
                    style = MaterialTheme.typography.labelSmall.copy(textDecoration = TextDecoration.LineThrough),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!single) Icon(Icons.Rounded.ExpandMore, null, Modifier.padding(start = 6.dp).rotate(rotation),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        g.promo?.let { pr -> PromoTag(pr.promoText!!, pr.promoEnd, Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp)) }
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
        Text(
            listOf(s.storeName, s.address).filter { it.isNotBlank() }.distinct().joinToString(" · "),
            Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        Text(money(s.effective), style = MaterialTheme.typography.titleSmall)
    }
}
