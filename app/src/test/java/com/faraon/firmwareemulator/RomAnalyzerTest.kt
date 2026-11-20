package com.faraon.firmwareemulator

import com.faraon.firmwareemulator.engine.GuestAbi
import com.faraon.firmwareemulator.engine.QemuCommand
import com.faraon.firmwareemulator.engine.VmProfile
import com.faraon.firmwareemulator.rom.RomAnalyzer
import com.faraon.firmwareemulator.rom.RomFamily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class RomAnalyzerTest {

    private fun zip(vararg files: Pair<String, String>): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { z ->
            files.forEach { (name, body) ->
                z.putNextEntry(ZipEntry(name))
                z.write(body.toByteArray())
                z.closeEntry()
            }
        }
        return bos.toByteArray()
    }

    @Test
    fun detectsHyperOsFromBuildProp() {
        val data = zip(
            "system/build.prop" to "ro.mi.os.version.name=OS1.0\nro.build.version.release=14\nro.product.device=marble\n",
        )
        val info = RomAnalyzer.analyze("rom.zip", data.size.toLong(), ByteArrayInputStream(data))
        assertEquals(RomFamily.HYPEROS, info.family)
        assertEquals("14", info.androidVersion)
        assertEquals("marble", info.device)
    }

    @Test
    fun detectsEvolutionXFromFileNameAndPayload() {
        val data = zip(
            "META-INF/com/android/metadata" to "post-build=google/x\npre-device=generic\n",
            "payload.bin" to "xxxx",
        )
        val info = RomAnalyzer.analyze("EvolutionX-10-gsi.zip", data.size.toLong(), ByteArrayInputStream(data))
        assertEquals(RomFamily.EVOLUTION_X, info.family)
        assertTrue(info.hasPayload)
    }

    @Test
    fun buildsQemuCommand() {
        val p = VmProfile("t", GuestAbi.ARM64, 2048, 2, File("/d/disk.img"), File("/d/kernel"))
        val cmd = QemuCommand.build("/bin/qemu", p)
        assertEquals("/bin/qemu", cmd.first())
        assertTrue(cmd.containsAll(listOf("-M", "virt", "-kernel", "/d/kernel", "-m", "2048")))
    }
}
