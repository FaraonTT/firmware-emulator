package com.faraon.firmwareemulator.engine

/** Pure (Android-free) QEMU command-line builder. */
object QemuCommand {

    fun build(binary: String, p: VmProfile): List<String> {
        val arm = p.abi == GuestAbi.ARM64
        val args = mutableListOf(binary)

        if (arm) args += listOf("-M", "virt", "-cpu", "cortex-a72")
        else args += listOf("-M", "q35", "-cpu", "qemu64")

        args += listOf("-smp", p.cores.toString(), "-m", p.ramMb.toString(), "-accel", "tcg")

        p.kernel?.let { args += listOf("-kernel", it.path, "-append", p.kernelCmdline) }

        args += listOf("-drive", "file=${p.disk.path},if=none,id=hd0,format=raw")
        args += listOf("-device", if (arm) "virtio-blk-device,drive=hd0" else "virtio-blk-pci,drive=hd0")

        args += listOf("-netdev", "user,id=n0")
        args += listOf("-device", if (arm) "virtio-net-device,netdev=n0" else "virtio-net-pci,netdev=n0")

        args += listOf("-serial", "stdio", "-display", "none", "-vnc", "127.0.0.1:${p.vncDisplay}")
        return args
    }
}
