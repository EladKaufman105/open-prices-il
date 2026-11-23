package il.openprices.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import il.openprices.AppViewModel
import il.openprices.Screen

@Composable
fun SettingsScreen(vm: AppViewModel) {
    Column(Modifier.fillMaxSize()) {
        TopBar("הגדרות")
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card {
                Row(
                    Modifier.fillMaxWidth().clickable { vm.go(Screen.CityPicker); vm.loadIndex() }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("עיר", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(vm.cityName ?: "-", style = MaterialTheme.typography.titleMedium)
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("ערכת נושא", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    val labels = listOf("מערכת", "בהיר", "כהה")
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        labels.forEachIndexed { i, l ->
                            SegmentedButton(
                                selected = vm.themeMode == i,
                                onClick = { vm.setTheme(i) },
                                shape = SegmentedButtonDefaults.itemShape(i, labels.size),
                            ) { Text(l) }
                        }
                    }
                }
            }
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("נתונים", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("עודכנו: ${vm.builtAt ?: "-"}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "המחירים מבוססים על הקבצים שהרשתות מפרסמות לפי חוק שקיפות המחירים, ומתעדכנים פעם ביום. " +
                            "ייתכנו הבדלים מהמחיר בקופה.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun Card(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainerLowest)) {
        content()
    }
}
