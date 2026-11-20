# Firmware Emulator

Android app (Kotlin + Jetpack Compose) for importing, analyzing and running Android firmware (MIUI, HyperOS, ColorOS, realme UI, Evolution X, LineageOS, PixelExperience, ...) in a virtual machine through a pluggable QEMU engine.

## What works

- **ROM analyzer** (`RomAnalyzer`): reads the zip, parses `META-INF/com/android/metadata` and `build.prop`, detects the family, Android version, device and `payload.bin` (A/B OTA).
- **VM profile**: guest architecture (ARM64 / x86_64), RAM, cores, disk image, kernel.
- **QEMU engine** (`QemuEngine`, `QemuCommand`): builds the command line, starts the process, streams the guest console into the UI. VNC is exposed on `127.0.0.1:5900`.
- Compose UI, unit tests.

## Honest limitations

- "Any firmware" is not possible in the strict sense: real-phone ROMs (MIUI/HyperOS/ColorOS) contain vendor/boot images for a specific SoC and will not boot on generic QEMU hardware without adaptation. GSI / generic builds (including custom ones such as Evolution X GSI) are the realistic target.
- Extracting images from `payload.bin` is not implemented yet: unpack with an external payload-dumper and pass a raw image (sparse -> raw: `simg2img`).
- QEMU binaries are not included. Build QEMU for Android (NDK) and place them in `app/src/main/jniLibs/arm64-v8a/`:
  - `libqemu-system-aarch64.so`
  - `libqemu-system-x86_64.so`
- TCG emulation (no hardware virtualization) is slow; an in-app VNC viewer is the next step.

## Build

```bash
gradle test assembleDebug
```

APK: `app/build/outputs/apk/debug/`.

## Roadmap

1. `payload.bin` extraction in Kotlin
2. Built-in VNC viewer
3. Saved VM profiles, snapshots
4. More cores (MCU, retro) behind the engine interface
