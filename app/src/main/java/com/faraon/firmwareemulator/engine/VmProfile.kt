package com.faraon.firmwareemulator.engine

import java.io.File

enum class GuestAbi(val title: String, val libName: String) {
    ARM64("ARM64", "libqemu-system-aarch64.so"),
    X86_64("x86_64", "libqemu-system-x86_64.so"),
}

data class VmProfile(
    val name: String,
    val abi: GuestAbi,
    val ramMb: Int,
    val cores: Int,
    val disk: File,
    val kernel: File?,
    val kernelCmdline: String = DEFAULT_CMDLINE,
    val vncDisplay: Int = 0,
) {
    companion object {
        const val DEFAULT_CMDLINE =
            "console=ttyAMA0 androidboot.hardware=ranchu androidboot.selinux=permissive"
    }
}
