package io.github.mohuddle.overhead.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverheadScreen(
    state: ScreenState,
    draft: String,
    onDraft: (String) -> Unit,
    onSet: () -> Unit,
    onDevice: () -> Unit,
    onWatch: () -> Unit,
    onNotify: () -> Unit,
    onClear: () -> Unit,
    onRing: (Int) -> Unit,
    onRow: (String) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Overhead") },
                actions = {
                    Surface(
                        color = if (state.watching) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(end = 16.dp),
                    ) {
                        Text(
                            text = state.badge,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = state.statusLine,
                color = if (state.statusAccent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            OutlinedTextField(
                value = draft,
                onValueChange = onDraft,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(state.placeholder) },
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSet) { Text("Set") }
                OutlinedButton(onClick = onDevice) { Text("Device") }
            }
            state.placeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.deviceError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.placeLine?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.rings.forEach { chip ->
                    FilterChip(
                        selected = chip.selected,
                        onClick = { onRing(chip.ring) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(chip.label)
                                Text(
                                    " ${chip.count}",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onWatch) { Text(state.watchLabel) }
                OutlinedButton(onClick = onNotify) { Text(state.notifyLabel) }
                if (state.clearVisible) TextButton(onClick = onClear) { Text("Clear") }
            }
            state.notifyError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.emptyCopy?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.rows.forEach { row ->
                TextButton(onClick = { onRow(row.url) }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        row.line,
                        color = if (row.insideOneMile) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            state.caption?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
