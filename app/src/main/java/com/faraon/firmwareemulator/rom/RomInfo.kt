package com.faraon.firmwareemulator.rom

enum class RomFamily(val title: String) {
    MIUI("MIUI"),
    HYPEROS("HyperOS"),
    COLOROS("ColorOS / OxygenOS"),
    REALME_UI("realme UI"),
    ONE_UI("One UI"),
    EVOLUTION_X("Evolution X"),
    LINEAGE("LineageOS"),
    PIXEL_EXPERIENCE("PixelExperience"),
    AOSP("AOSP / unknown"),
}

data class RomInfo(
    val fileName: String,
    val sizeBytes: Long,
    val family: RomFamily,
    val androidVersion: String?,
    val device: String?,
    val buildId: String?,
    val cpuAbi: String?,
    val hasPayload: Boolean,
    val entries: List<String>,
    val notes: List<String>,
)
