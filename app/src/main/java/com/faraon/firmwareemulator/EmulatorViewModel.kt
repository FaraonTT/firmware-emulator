package com.faraon.firmwareemulator

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faraon.firmwareemulator.engine.GuestAbi
import com.faraon.firmwareemulator.engine.QemuEngine
import com.faraon.firmwareemulator.engine.VmProfile
import com.faraon.firmwareemulator.rom.RomAnalyzer
import com.faraon.firmwareemulator.rom.RomInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class UiState(
    val rom: RomInfo? = null,
    val diskName: String? = null,
    val kernelName: String? = null,
    val ramMb: Int = 2048,
    val cores: Int = 2,
    val abi: GuestAbi = GuestAbi.ARM64,
    val running: Boolean = false,
    val log: List<String> = emptyList(),
    val error: String? = null,
)

class EmulatorViewModel(private val app: Application) : AndroidViewModel(app) {

    private val engine = QemuEngine(app)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var diskUri: Uri? = null
    private var kernelUri: Uri? = null
    private var job: Job? = null

    fun onRomPicked(uri: Uri) {
        viewModelScope.launch {
            try {
                val info = withContext(Dispatchers.IO) {
                    val (name, size) = queryMeta(uri)
                    val stream = app.contentResolver.openInputStream(uri) ?: error("Cannot open file")
                    stream.use { RomAnalyzer.analyze(name, size, it) }
                }
                _state.update { it.copy(rom = info, error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to analyze ROM: ${e.message}") }
            }
        }
    }

    fun onDiskPicked(uri: Uri) {
        diskUri = uri
        _state.update { it.copy(diskName = queryMeta(uri).first) }
    }

    fun onKernelPicked(uri: Uri) {
        kernelUri = uri
        _state.update { it.copy(kernelName = queryMeta(uri).first) }
    }

    fun setRam(mb: Int) = _state.update { it.copy(ramMb = mb) }
    fun setCores(n: Int) = _state.update { it.copy(cores = n) }
    fun setAbi(abi: GuestAbi) = _state.update { it.copy(abi = abi) }

    fun start() {
        val disk = diskUri
        if (disk == null) {
            _state.update { it.copy(error = "Pick a disk image (system.img / raw)") }
            return
        }
        if (_state.value.running) return
        job = viewModelScope.launch {
            _state.update { it.copy(running = true, error = null, log = emptyList()) }
            try {
                val s = _state.value
                val dir = File(app.filesDir, "vm").apply { mkdirs() }
                val diskFile = copyTo(disk, File(dir, "disk.img"))
                val kernelFile = kernelUri?.let { copyTo(it, File(dir, "kernel")) }
                val profile = VmProfile(
                    name = s.rom?.family?.title ?: "vm",
                    abi = s.abi,
                    ramMb = s.ramMb,
                    cores = s.cores,
                    disk = diskFile,
                    kernel = kernelFile,
                )
                val code = engine.run(profile, ::appendLog)
                appendLog("QEMU exited with code $code")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: e.toString()) }
            } finally {
                _state.update { it.copy(running = false) }
            }
        }
    }

    fun stop() {
        engine.stop()
        job?.cancel()
    }

    override fun onCleared() {
        engine.stop()
    }

    private fun appendLog(line: String) {
        _state.update { it.copy(log = (it.log + line).takeLast(500)) }
    }

    private suspend fun copyTo(uri: Uri, dest: File): File = withContext(Dispatchers.IO) {
        val input = app.contentResolver.openInputStream(uri) ?: error("Cannot open ${dest.name}")
        input.use { src -> dest.outputStream().use { src.copyTo(it) } }
        dest
    }

    private fun queryMeta(uri: Uri): Pair<String, Long> {
        var name = uri.lastPathSegment ?: "file"
        var size = 0L
        app.contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val si = c.getColumnIndex(OpenableColumns.SIZE)
                if (ni >= 0) name = c.getString(ni)
                if (si >= 0) size = c.getLong(si)
            }
        }
        return name to size
    }
}
