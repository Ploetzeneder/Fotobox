package com.fotobox.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import com.fotobox.app.data.models.CountdownDuration
import com.fotobox.app.data.models.PhotoFilter
import com.fotobox.app.data.models.StripBackground
import com.fotobox.app.data.models.StripLayout
import com.fotobox.app.ui.viewmodels.SettingsViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenConnect: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val aiGenerating by viewModel.aiGenerating.collectAsState()
    val aiError by viewModel.aiError.collectAsState()
    val isCloudConnected = settings.cloudToken.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Zurück", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                "Einstellungen",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Fotobienchen Cloud
            SettingsSection("Fotobienchen Cloud") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isCloudConnected) Color(0xFF1B5E20).copy(0.3f)
                            else Color.White.copy(0.05f)
                        )
                        .clickable { onOpenConnect() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isCloudConnected) Icons.Default.CheckCircle else Icons.Default.CloudOff,
                        null,
                        tint = if (isCloudConnected) Color(0xFF4CAF50) else Color.White.copy(0.4f),
                        modifier = Modifier.size(32.dp)
                    )
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(
                            if (isCloudConnected) "Verbunden" else "Nicht verbunden",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = if (isCloudConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            if (isCloudConnected) settings.cloudCustomerName
                            else "Tippen zum Verbinden",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                            )
                        )
                    }
                    Icon(Icons.Default.ArrowForward, null, tint = Color.White.copy(0.3f))
                }
                if (isCloudConnected) {
                    Spacer(modifier = Modifier.height(4.dp))
                    SettingsToggle(
                        label = "Auto-Upload",
                        description = "Fotos & Audio automatisch hochladen",
                        checked = settings.autoUploadCloud,
                        onToggle = { viewModel.updateAutoUpload(it) }
                    )
                }
            }

            // Veranstaltung
            SettingsSection("Veranstaltung") {
                OutlinedTextField(
                    value = settings.eventName,
                    onValueChange = { viewModel.updateEventName(it) },
                    label = { Text("Event-Name (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { viewModel.save() }),
                    singleLine = true
                )
            }

            // Countdown
            SettingsSection("Countdown") {
                SegmentedPicker(
                    options = CountdownDuration.entries.map { it.label },
                    selectedIndex = CountdownDuration.entries.indexOf(settings.countdownDuration),
                    onSelect = { viewModel.updateCountdown(CountdownDuration.entries[it]) }
                )
            }

            // Strip Layout
            SettingsSection("Foto-Layout") {
                LayoutPicker(
                    layouts = StripLayout.entries,
                    selected = settings.stripLayout,
                    onSelect = { viewModel.updateLayout(it) }
                )
            }

            // Filter
            SettingsSection("Standard-Filter") {
                SegmentedPicker(
                    options = PhotoFilter.entries.map { it.label },
                    selectedIndex = PhotoFilter.entries.indexOf(settings.defaultFilter),
                    onSelect = { viewModel.updateFilter(PhotoFilter.entries[it]) }
                )
            }

            // Streifen-Hintergrund
            SettingsSection("Streifen-Hintergrund") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StripBackground.entries.forEach { bg ->
                        val isSelected = bg == settings.stripBackground
                        val r = (bg.colorArgb shr 16) and 0xFF
                        val g = (bg.colorArgb shr 8) and 0xFF
                        val b = bg.colorArgb and 0xFF
                        val isLight = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0 > 0.5
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(bg.colorArgb))
                                .then(
                                    if (isSelected) Modifier.border(
                                        3.dp,
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(8.dp)
                                    ) else Modifier
                                )
                                .clickable { viewModel.updateStripBackground(bg) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                bg.label,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = if (isLight) Color.DarkGray else Color.White
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Druckoptionen
            SettingsSection("Druckoptionen") {
                SettingsToggle(
                    label = "Auto-Druck",
                    description = "Streifen automatisch nach jeder Aufnahme drucken",
                    checked = settings.autoPrint,
                    onToggle = { viewModel.updateAutoPrint(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Druckexemplare",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            "Anzahl der Ausdrucke pro Aufnahme",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                            )
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.updatePrintCopies(settings.printCopies - 1) },
                            enabled = settings.printCopies > 1,
                            modifier = Modifier.size(40.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text("−", style = MaterialTheme.typography.titleLarge)
                        }
                        Text(
                            "${settings.printCopies}",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.Center
                        )
                        FilledTonalButton(
                            onClick = { viewModel.updatePrintCopies(settings.printCopies + 1) },
                            enabled = settings.printCopies < 5,
                            modifier = Modifier.size(40.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text("+", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }

            // Kiosk-Automatik
            SettingsSection("Kiosk-Automatik") {
                val delayOptions = listOf(0 to "Aus", 15 to "15 Sek", 30 to "30 Sek", 60 to "1 Min", 120 to "2 Min")
                val selectedDelayIdx = delayOptions.indexOfFirst { it.first == settings.autoReturnDelay }
                    .let { if (it < 0) 0 else it }
                Column {
                    Text(
                        "Auto-Rückkehr",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        "Nach der Fotovorschau automatisch zum Start",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SegmentedPicker(
                        options = delayOptions.map { it.second },
                        selectedIndex = selectedDelayIdx,
                        onSelect = { viewModel.updateAutoReturnDelay(delayOptions[it].first) }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                val idleOptions = listOf(0 to "Aus", 30 to "30 Sek", 60 to "1 Min", 120 to "2 Min", 300 to "5 Min")
                val selectedIdleIdx = idleOptions.indexOfFirst { it.first == settings.idleSlideshowDelay }
                    .let { if (it < 0) 0 else it }
                Column {
                    Text(
                        "Diashow bei Inaktivität",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        "Slideshow automatisch starten wenn niemand die App benutzt",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SegmentedPicker(
                        options = idleOptions.map { it.second },
                        selectedIndex = selectedIdleIdx,
                        onSelect = { viewModel.updateIdleSlideshowDelay(idleOptions[it].first) }
                    )
                }
            }

            // Kamera
            SettingsSection("Kamera-Optionen") {
                SettingsToggle(
                    label = "Blitz-Effekt",
                    description = "Weißer Bildschirm beim Aufnehmen",
                    checked = settings.useFlash,
                    onToggle = { viewModel.updateFlash(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggle(
                    label = "Live-Filter-Vorschau",
                    description = "Gewählter Filter live im Kamerabild anzeigen",
                    checked = settings.showLiveFilter,
                    onToggle = { viewModel.updateShowLiveFilter(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggle(
                    label = "Kiosk-Modus",
                    description = "Vollbild ohne Statusleiste",
                    checked = settings.kioskMode,
                    onToggle = { viewModel.updateKiosk(it) }
                )
                if (settings.kioskMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = settings.settingsPin,
                        onValueChange = { v ->
                            if (v.length <= 6 && v.all { it.isDigit() }) viewModel.updateSettingsPin(v)
                        },
                        label = { Text("Einstellungen-PIN (optional)") },
                        placeholder = { Text("z.B. 1234") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Text(
                        "Leer lassen = kein PIN-Schutz",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // KI-Hintergrund
            SettingsSection("KI-Hintergrund (DALL-E 3)") {
                OutlinedTextField(
                    value = settings.aiApiKey,
                    onValueChange = { viewModel.updateAiApiKey(it) },
                    label = { Text("OpenAI API-Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    placeholder = { Text("sk-...") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.generateAiBackground() },
                        enabled = !aiGenerating && settings.aiApiKey.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        if (aiGenerating) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Hintergrund generieren")
                        }
                    }
                    OutlinedButton(
                        onClick = { viewModel.clearAiBackground() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Zurücksetzen")
                    }
                }
                aiError?.let { err ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(err, style = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFFEF5350)))
                }
                Text(
                    "Generiert ein Event-spezifisches Hintergrundbild für den Startbildschirm. ~0,04€ pro Bild.",
                    style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f))
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp)
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SegmentedPicker(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEachIndexed { i, label ->
            val isSelected = i == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                    .clickable { onSelect(i) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun LayoutPicker(
    layouts: List<StripLayout>,
    selected: StripLayout,
    onSelect: (StripLayout) -> Unit
) {
    // LazyVerticalGrid needs a fixed height when inside a verticalScroll parent
    val rows = (layouts.size + 2) / 3
    val itemHeight = 64.dp
    val gridHeight = itemHeight * rows + 4.dp * (rows - 1)
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.height(gridHeight),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        userScrollEnabled = false
    ) {
        items(layouts) { layout ->
            val isSelected = layout == selected
            Box(
                modifier = Modifier
                    .height(itemHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface
                    )
                    .then(
                        if (isSelected) Modifier else Modifier.border(
                            1.dp, MaterialTheme.colorScheme.outline.copy(0.3f), RoundedCornerShape(8.dp)
                        )
                    )
                    .clickable { onSelect(layout) }
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        layout.icon,
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        layout.label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = androidx.compose.ui.unit.TextUnit(
                                9f, androidx.compose.ui.unit.TextUnitType.Sp
                            )
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsToggle(
    label: String,
    description: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Text(
                description,
                style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f))
            )
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}
