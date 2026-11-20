package com.faraon.firmwareemulator.rom

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/** Analyzes a ROM zip (OTA / recovery): detects the firmware family and key properties. */
object RomAnalyzer {

    fun analyze(fileName: String, sizeBytes: Long, input: InputStream): RomInfo {
        val entries = mutableListOf<String>()
        var metadata = emptyMap<String, String>()
        var buildProp = emptyMap<String, String>()
        var hasPayload = false

        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name
                if (entries.size < 200) entries += name
                when (name) {
                    "META-INF/com/android/metadata" -> metadata = parseProps(zip.readCapped())
                    "system/build.prop", "system/system/build.prop" -> buildProp = parseProps(zip.readCapped())
                    "payload.bin" -> hasPayload = true
                }
                // Do not stream a multi-gigabyte archive to the end once everything needed is found.
                if (buildProp.isNotEmpty()) break
                if (hasPayload && metadata.isNotEmpty()) break
            }
        }

        val props = metadata + buildProp
        val family = detectFamily(props, fileName)
        val notes = buildList {
            if (hasPayload && buildProp.isEmpty()) {
                add("A/B OTA (payload.bin): extract system/boot/vendor images with payload-dumper before running.")
            }
            add(
                "Images of physical devices (vendor/boot for a specific SoC) will not boot in QEMU without adaptation. " +
                    "GSI / generic builds (including custom ones like Evolution X GSI) are the realistic target.",
            )
        }

        return RomInfo(
            fileName = fileName,
            sizeBytes = sizeBytes,
            family = family,
            androidVersion = props["ro.build.version.release"],
            device = props["ro.product.device"] ?: props["pre-device"],
            buildId = props["ro.build.display.id"] ?: props["post-build"],
            cpuAbi = props["ro.product.cpu.abi"],
            hasPayload = hasPayload,
            entries = entries,
            notes = notes,
        )
    }

    fun detectFamily(props: Map<String, String>, fileName: String): RomFamily {
        val n = fileName.lowercase()
        val keys = props.keys
        fun hasKey(prefix: String) = keys.any { it.startsWith(prefix) }
        return when {
            hasKey("ro.evolution") || "evolution" in n -> RomFamily.EVOLUTION_X
            hasKey("ro.lineage") || "lineage" in n -> RomFamily.LINEAGE
            hasKey("ro.pixelexperience") || "pixelexperience" in n -> RomFamily.PIXEL_EXPERIENCE
            hasKey("ro.mi.os.version") || "hyperos" in n ||
                props["ro.build.version.incremental"]?.startsWith("OS") == true -> RomFamily.HYPEROS
            hasKey("ro.miui") || "miui" in n -> RomFamily.MIUI
            "realmeui" in n || "realme ui" in n -> RomFamily.REALME_UI
            hasKey("ro.build.version.oplusrom") || hasKey("ro.oplus") ||
                "coloros" in n || "oxygenos" in n -> RomFamily.COLOROS
            hasKey("ro.build.version.oneui") || "oneui" in n -> RomFamily.ONE_UI
            else -> RomFamily.AOSP
        }
    }

    fun parseProps(text: String): Map<String, String> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && '=' in it }
            .associate { line ->
                val i = line.indexOf('=')
                line.substring(0, i).trim() to line.substring(i + 1).trim()
            }

    private fun InputStream.readCapped(max: Int = 1 shl 20): String {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        while (out.size() < max) {
            val r = read(buf)
            if (r < 0) break
            out.write(buf, 0, r)
        }
        return out.toString(Charsets.UTF_8.name())
    }
}
