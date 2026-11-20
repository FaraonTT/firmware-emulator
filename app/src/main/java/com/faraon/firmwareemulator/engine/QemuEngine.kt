package com.faraon.firmwareemulator.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Emulation engine: runs QEMU shipped as a native library (libqemu-system-*.so).
 * The binaries themselves are not part of the repository (see README).
 */
class QemuEngine(private val context: Context) {

    @Volatile
    private var process: Process? = null

    fun binaryFor(abi: GuestAbi): File? =
        File(context.applicationInfo.nativeLibraryDir, abi.libName).takeIf { it.exists() }

    /** Blocks until QEMU exits, forwarding guest console lines to [onLine]. Returns the exit code. */
    suspend fun run(profile: VmProfile, onLine: (String) -> Unit): Int = withContext(Dispatchers.IO) {
        val bin = binaryFor(profile.abi)
            ?: error("QEMU not found: add ${profile.abi.libName} to app/src/main/jniLibs (see README)")
        val cmd = QemuCommand.build(bin.path, profile)
        onLine("$ " + cmd.joinToString(" "))
        val p = ProcessBuilder(cmd).redirectErrorStream(true).start()
        process = p
        try {
            p.inputStream.bufferedReader().forEachLine(onLine)
            p.waitFor()
        } finally {
            process = null
        }
    }

    fun stop() {
        process?.destroy()
    }
}
