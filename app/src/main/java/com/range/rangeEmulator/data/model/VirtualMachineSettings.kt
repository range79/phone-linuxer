package com.range.rangeEmulator.data.model

import com.range.rangeEmulator.data.enums.*
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class VirtualMachineSettings(
    val id: String = UUID.randomUUID().toString(),
    val vmName: String,
    val osType: OsType = OsType.LINUX,
    val cpuModel: CpuModel = CpuModel.MAX,
    val cpuCores: Int = 4,
    val ramMB: Int = 2048,
    val screenType: ScreenType = ScreenType.VNC,

    val screenWidth: Int = 1280,
    val screenHeight: Int = 720,

    val isGpuEnabled: Boolean = true,
    val spicePort: Int = 5901,
    val vncPort: Int = 5900,

    val isoUris: List<String> = emptyList(),
    val diskImgPath: String? = null,
    val diskFormat: DiskFormat = DiskFormat.QCOW2,
    val diskSizeGB: Int = 20,

    val easyInstall: Boolean = false,
    val easyInstallSettings: EasyInstallSettings? = null,
    val networkMode: NetworkMode = NetworkMode.USER,
    val tbSizeMB: Int = 512,
    val isTurboEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val state: VmState = VmState.INACTIVE,
)

fun VirtualMachineSettings.buildFullCommand(isSetupMode: Boolean = false): List<String> {
    val cmd = mutableListOf<String>()

    cmd.add("qemu_executable")

    if (isTurboEnabled) {
        // Bypasses host power management for guest vCPUs to maximize throughput
        cmd.add("-overcommit")
        cmd.add("cpu-pm=on")
    }

    cmd.add("-machine")
    if (cpuModel.getArch() == "aarch64") {
        cmd.add("virt,gic-version=max,its=on,highmem=on")
    } else {
        cmd.add("q35")
    }

    cmd.add("-boot")
    cmd.add("menu=on,strict=on")

    cmd.add("-cpu")
    val cpuParam = if (cpuModel == CpuModel.HOST) "host" else cpuModel.toQemuParam()
    if (cpuModel == CpuModel.HOST && isTurboEnabled) {
        // PMU: Performance Monitoring, LSE: Large System Extensions, Cache Info for better guest scheduling
        cmd.add("$cpuParam,pmu=on,lse=on,host-cache-info=on,l3-cache=on")
    } else if (cpuModel.getArch() == "aarch64" && osType == OsType.WINDOWS && cpuModel == CpuModel.MAX) {
        cmd.add("$cpuParam,pauth=on")
    } else {
        cmd.add(cpuParam)
    }

    cmd.add("-accel")
    val finalTbSize = if (isTurboEnabled) tbSizeMB.coerceAtLeast(1024) else tbSizeMB
    if (cpuModel == CpuModel.HOST) {
        cmd.add("kvm:tcg,thread=multi,tb-size=${finalTbSize}")
    } else {
        cmd.add("tcg,thread=multi,tb-size=${finalTbSize}")
    }

    cmd.add("-object")
    cmd.add("iothread,id=iothread0")


    cmd.add("-device")
    cmd.add("qemu-xhci,id=usb")

    if (cpuCores > 1) {
        cmd.add("-smp")
        cmd.add("cores=$cpuCores,threads=1,sockets=1")
    } else {
        cmd.add("-smp")
        cmd.add("1")
    }
    cmd.add("-m")
    cmd.add("${ramMB}M")

    if (isoUris.isNotEmpty() || isSetupMode || easyInstall) {
        isoUris.forEachIndexed { index, uri ->
            cmd.add("-drive")
            cmd.add("file=$uri,format=raw,if=none,id=cd$index,readonly=on,cache=writeback,aio=threads")
            cmd.add("-device")
            if (cpuModel.getArch() == "aarch64") {
                // Use virtio-blk-pci for better performance on ARM64
                cmd.add("virtio-blk-pci,drive=cd$index,bootindex=$index,disable-legacy=on,disable-modern=off")
            } else {
                cmd.add("virtio-blk-pci,drive=cd$index,bootindex=${index},iothread=iothread0,disable-legacy=on,disable-modern=off")
            }
        }
    }

    diskImgPath?.let {
        val formatName = diskFormat.name.lowercase()
        cmd.add("-drive")
        cmd.add("file=$it,format=$formatName,if=none,id=drive0,cache=writeback,discard=on,detect-zeroes=on,aio=threads")
        cmd.add("-device")
        cmd.add("virtio-blk-pci,drive=drive0,bootindex=${isoUris.size + 10},iothread=iothread0,disable-legacy=on,disable-modern=off")
    }

    cmd.addAll(getDisplayArgs())

    cmd.addAll(getNetworkArgs())

    cmd.add("-device")
    cmd.add("usb-tablet,bus=usb.0")
    cmd.add("-device")
    cmd.add("usb-kbd,bus=usb.0")

    cmd.add("-device")
    cmd.add("virtio-tablet-pci,disable-legacy=on,disable-modern=off")
    cmd.add("-device")
    cmd.add("virtio-keyboard-pci,disable-legacy=on,disable-modern=off")
    cmd.add("-device")
    cmd.add("virtio-rng-pci,disable-legacy=on,disable-modern=off")

    cmd.add("-nodefaults")
    cmd.add("-no-user-config")
    cmd.add("-rtc")
    val rtcArgs = if (isTurboEnabled) {
        "base=utc,clock=host"
    } else if (cpuModel.getArch() == "aarch64") {
        "base=utc,clock=rt"
    } else {
        "base=utc,clock=rt,driftfix=slew"
    }
    cmd.add(rtcArgs)

    cmd.add("-serial")
    cmd.add("stdio")

    return cmd
}

private fun VirtualMachineSettings.getNetworkArgs(): List<String> {
    val args = mutableListOf<String>()
    when (networkMode) {
        NetworkMode.USER -> {
            args.add("-netdev")
            args.add("user,id=net0,hostfwd=tcp::2222-:22")
            args.add("-device")
            args.add("virtio-net-pci,netdev=net0,disable-legacy=on,disable-modern=off")
        }
        NetworkMode.NONE -> {
            args.add("-net")
            args.add("none")
        }
    }
    return args
}

private fun VirtualMachineSettings.getDisplayArgs(): List<String> {
    val args = mutableListOf<String>()

    val isArm = cpuModel.getArch() == "aarch64"
    
    if (isGpuEnabled) {
        args.add("-device")
        val resParams = if (screenWidth > 0 && screenHeight > 0) ",xres=$screenWidth,yres=$screenHeight" else ""
        
        val gpuDevice = if (isArm) "virtio-gpu-pci" else "virtio-vga"
        args.add("${gpuDevice}${resParams},edid=on,disable-legacy=on,disable-modern=off")
    } else {
        args.add("-device")
        val gpuDevice = if (isArm) "virtio-gpu-pci" else "virtio-vga"
        args.add("${gpuDevice},edid=on,disable-legacy=on,disable-modern=off")
    }

    args.add("-display")
    args.add("none")

    val biosFile = if (cpuModel.getArch() == "aarch64") "edk2-aarch64-code.fd" else "edk2-i386-code.fd"
    args.add("-bios")
    args.add(biosFile)

    val vncIndex = vncPort - 5900
    args.add("-vnc")
    args.add("0.0.0.0:$vncIndex")

    if (screenType == ScreenType.SPICE) {
        args.add("-spice")
        args.add("port=$spicePort,addr=0.0.0.0,disable-ticketing=on")

        args.add("-device")
        args.add("virtio-serial-pci,disable-legacy=on,disable-modern=off")
        args.add("-device")
        args.add("virtserialport,chardev=spicechannel0,name=com.redhat.spice.0")
        args.add("-chardev")
        args.add("spicevmc,id=spicechannel0,name=vdagent")
    }

    return args
}