# RangeEmulator: Desktop-Class Virtualization on Your Android

RangeEmulator is a powerful yet simple tool that lets you run full desktop operating systems like Linux and Windows directly on your Android phone. By using modern virtualization technology, it turns your mobile device into a portable workstation.

## Why Use Your Phone?

Modern flagship Android devices (equipped with Snapdragon 8 Gen 2/3 or similar) are often **more powerful** than older desktop computers or budget laptops. 

A high-end phone running RangeEmulator can outperform a 1st Generation Intel Core i3 PC. If you have a powerful mobile device, why settle for a slow old computer? RangeEmulator lets you harness that power for programming, professional tools, or lightweight gaming anywhere you go.

## Key Features

- **Full Speed Emulation**: Uses KVM (Hardware Acceleration) to run systems at nearly the same speed as your phone's native hardware.
- **🐧 Linux GPU & Performance**
- **3D Acceleration:** If GPU is enabled, Linux guests will use `virtio-gpu-gl-pci`. Ensure your guest OS has `mesa-vulkan-virtio` or `libgl1-mesa-dri` installed for full hardware acceleration.
- **Dual Display:** Linux now supports a secondary `ramfb` display for boot logs and basic output alongside the high-performance GPU display.
- **Wayland Support:** For the smoothest experience with VirGL, we recommend using a Wayland-based compositor in your guest OS.
- **Modern Driver Support**: Specialized "Virtio" drivers ensure your virtual storage and internet are fast and responsive.
- **Graphics Acceleration**: Built-in 3D graphics support for a smooth visual experience in Linux and Windows ARM64.
- **Windows Optimized**: Unique features like "RamFB" support make sure Windows boots without black screens or crashes.
- **Universal Display**: Accessible via low-latency SPICE or standard VNC protocols.

## What Can You Do With It?

- **Programming on the Go**: Run a full Linux environment with Visual Studio Code, Docker, and compilers. Code and build your projects anywhere.
- **Run PC Software**: Use Windows ARM64 to run essential professional tools and legacy applications that aren't available on Android.
- **Mobile Server**: Host local websites, development databases, or network tools right from your pocket.
- **Retro Gaming**: Experience classic PC titles or specialized ARM Windows games.

## Quick Technical Overview

- **Firmware**: Uses UEFI (EDK2) for a modern, secure boot experience.
- **Storage**: Optimized for fast disk access using high-speed Virtio-BLK drivers.
- **Display**: High-resolution output with dynamic resizing support.

RangeEmulator is designed to be the bridge between your powerful mobile hardware and the desktop software you need.
