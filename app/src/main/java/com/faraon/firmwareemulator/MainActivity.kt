package com.faraon.firmwareemulator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faraon.firmwareemulator.engine.GuestAbi
import com.faraon.firmwareemulator.rom.RomInfo

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            MaterialTheme(colorScheme = colors) { EmulatorScreen() }
        }
    }
}

@Composable
fun EmulatorScreen(vm: EmulatorViewModel = viewModel()) {
    val st by vm.state.collectAsState()
    val romPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> u?.let(vm::onRomPicked) }
    val diskPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> u?.let(vm::onDiskPicked) }
    val kernelPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> u?.let(vm::onKernelPicked) }

    Scaffold { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text("Firmware Emulator", style = MaterialTheme.typography.headlineMedium) }

            item { PickerRow("ROM (zip)", st.rom?.fileName, "Pick") { romPicker.launch(arrayOf("*/*")) } }
            st.rom?.let { item { RomCard(it) } }

            item { PickerRow("Disk image (raw / system.img)", st.diskName, "Pick") { diskPicker.launch(arrayOf("*/*")) } }
            item { PickerRow("Kernel (optional)", st.kernelName, "Pick") { kernelPicker.launch(arrayOf("*/*")) } }

            item {
                Column {
                    Text("Guest architecture")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GuestAbi.entries.forEach { abi ->
                            FilterChip(
                                selected = st.abi == abi,
                                onClick = { vm.setAbi(abi) },
                                label = { Text(abi.title) },
                            )
                        }
                    }
                }
            }
            item {
                Column {
                    Text("RAM: ${st.ramMb} MB")
                    Slider(
                        value = st.ramMb.toFloat(),
                        onValueChange = { vm.setRam((it / 256).toInt().coerceAtLeast(1) * 256) },
                        valueRange = 512f..8192f,
                    )
                    Text("CPU cores: ${st.cores}")
                    Slider(
                        value = st.cores.toFloat(),
                        onValueChange = { vm.setCores(it.toInt().coerceAtLeast(1)) },
                        valueRange = 1f..8f,
                        steps = 6,
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = vm::start, enabled = !st.running) { Text("Start") }
                    OutlinedButton(onClick = vm::stop, enabled = st.running) { Text("Stop") }
                }
            }
            st.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }

            item { Text("Guest console", style = MaterialTheme.typography.titleMedium) }
            items(st.log) { line -> Text(line, fontFamily = FontFamily.Monospace, fontSize = 11.sp) }
        }
    }
}

@Composable
private fun PickerRow(title: String, value: String?, button: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(value ?: "not selected", style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(onClick = onClick) { Text(button) }
    }
}

@Composable
private fun RomCard(rom: RomInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(rom.family.title, style = MaterialTheme.typography.titleMedium)
            rom.androidVersion?.let { Text("Android $it") }
            rom.device?.let { Text("Device: $it") }
            rom.buildId?.let { Text("Build: $it") }
            rom.cpuAbi?.let { Text("ABI: $it") }
            if (rom.hasPayload) Text("Type: A/B OTA (payload.bin)")
            rom.notes.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
        }
    }
}
