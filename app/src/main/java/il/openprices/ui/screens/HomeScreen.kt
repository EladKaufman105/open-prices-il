package il.openprices.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import il.openprices.AppViewModel
import il.openprices.Screen
import il.openprices.data.Product

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val scan = {
        GmsBarcodeScanning.getClient(context).startScan()
            .addOnSuccessListener { b -> b.rawValue?.let { vm.onScanned(it) } }
        Unit
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().imePadding()) {
            // ---- header ----
            Row(
                Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 20.dp, end = 8.dp, top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("מחירון", style = MaterialTheme.typography.headlineMedium)
                    Row(
                        Modifier.clip(RoundedCornerShape(8.dp)).clickable { vm.go(Screen.CityPicker); vm.loadIndex() },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.LocationOn, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text(vm.cityName ?: "בחרו עיר", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = { vm.go(Screen.Settings) }) { Icon(Icons.Rounded.Settings, "הגדרות") }
            }
            if (vm.downloading != null) {
                LinearProgressIndicator({ vm.downloadProgress }, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp))
            }

            // ---- search field ----
            TextField(
                value = vm.query,
                onValueChange = vm::onQuery,
                placeholder = { Text("חיפוש מוצר או ברקוד") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = {
                    Row {
                        if (vm.query.isNotEmpty()) IconButton(onClick = { vm.onQuery("") }) { Icon(Icons.Rounded.Close, "ניקוי") }
                        IconButton(onClick = scan) { Icon(Icons.Rounded.QrCodeScanner, "סריקת ברקוד", tint = MaterialTheme.colorScheme.primary) }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.commitSearch(vm.query); focus.clearFocus() }),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            )

            if (vm.query.isBlank()) {
                // ---- empty state: recent searches ----
                Column(Modifier.padding(horizontal = 20.dp)) {
                    if (vm.recent.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("חיפושים אחרונים", style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            TextButton(onClick = vm::clearRecent) { Text("ניקוי") }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            vm.recent.forEach { r ->
                                Row(
                                    Modifier.clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                        .clickable { vm.onQuery(r) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Rounded.History, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(6.dp))
                                    Text(r, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    } else {
                        Spacer(Modifier.height(40.dp))
                        Text("מה קונים היום?", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "חפשו מוצר לפי שם, או סרקו ברקוד, ותראו את המחיר בכל הסניפים ב${vm.cityName ?: "עיר שלכם"}.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                // ---- results ----
                if (!vm.searching && vm.results.isEmpty()) {
                    Text("לא נמצאו מוצרים", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        Column(Modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainerLowest)) {
                            vm.results.forEachIndexed { i, p ->
                                if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                ProductRow(p) { focus.clearFocus(); vm.open(p) }
                            }
                        }
                    }
                }
            }
        }

        // ---- basket button ----
        ExtendedFloatingActionButton(
            onClick = { vm.go(Screen.Basket); vm.refreshBasket() },
            icon = {
                BadgedBox(badge = { if (vm.list.isNotEmpty()) Badge { Text("${vm.list.size}") } }) {
                    Icon(Icons.Rounded.ShoppingBasket, null)
                }
            },
            text = { Text("הסל שלי") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars).padding(20.dp),
        )
    }
}

@Composable
fun ProductRow(p: Product, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(p.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val sub = listOf(p.size, p.manufacturer).filter { it.isNotBlank() }.joinToString(" · ")
            if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(money(p.minPrice), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary)
            Text(
                if (p.maxPrice > p.minPrice) "עד ${money(p.maxPrice)} · ${p.storeCount} סניפים" else "${p.storeCount} סניפים",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
