# Android-Compatibility-Layer
A translation layer for Android on Linux. Run Android Apps and Games on Linux

## Introduction

ACL (Android Compatibility Layer) runs an APK directly on the Linux system without using virtual machines, containers or Android system images.

```
acl myapp.apk
```

Usually an Android app is divided into two parts. Dex and Native code. ACL treats them as follows:
- Dex bytecode runs on a ART build from AOSP source code.
- ARM64 native libraries are translated ahead of time to x86_64 by LLVM or ran directly using JIT

The Android framework itself is reimplemented on Linux. This includes using wayland for windows, handling for Vulkan and GLES, FreeType and HarfBuzz for text, SQLite for storage and PipeWire for audio.

## Disclaimer

This project is still in a very early stage and app support is still very limited. If you find an app that isn't supported, please open a Github issue and I'll do my best to support it.

## Supported

- x86-64 Linux with only Wayland support for now
- Vulkan or OpenGL ES driver
- `arm64-v8a` and `x86_64` APKs

## What is different here?

Most of the other projects I've seen either boot a full Android image in a container (Waydroid, Anbox) and still need a native bridge for ARM code.

This project does it differently:

- Java and Kotlin code is never translated as dex is ran by x86_64 ART directly on the system
- ARM64 code is recompiled ahead of time. This is done with the help of LLVM.
- Graphics go straight to the host gpu rather than being emulated.

## Why it's faster for gaming

Most Android games are mostly native code. Emulating this code is slow, especially emulation of ARM64 on x86_64. That's why we

- The entire native library is translated to x86_64. That means it can run natively on linux without any soft of translation.
- No memory access slowdown
- Native graphics

## Features

- Install and run apks
- Split apks and multidex support
- Activity lifecycle, back state, intents, saved instance state, fragments, services, broadcasts, alarms, jobs
- Canvas, Paint, Path, Region, Bitmap
- SufaceView, NativeActivity
- SQLite, Handler/Looper/MessageQueue
- Audio
- Touch

## What's missing

- SystemUI
- No binder, IPC
- No rotation
- No camera, sensors, gps, telephony, bluetooth, nfc, vibration
- No WebView, no MediaCodec, no hardware view decode
- No 32 bit arm support
- No multi window
- No permission UI
- LLVM sometimes crashes on big libraries

Still early. Expect bugs.

## Security

Since all the code runs on the host system, there are several security measures in place. Those include:

- seccomp
- landlock
- private pid namespace
- private network namespace

You can turn off sandboxing with `--no-sandbox` but it's not recommended for untrusted apps.

## Building and Installation

### Release bundle

```
tar xzf acl-0.1.0-linux-x86_64.tar.gz
cd acl-0.1.0-linux-x86_64
./bin/acl myapp.apk
```

It includes the Android runtime, `framework.jar`, the framework resources, fonts, the linker and everything else it needs.

### From source

Needs clang, CMake 3.28+, Ninja, LLVM 19+ with headers, FreeType, HarfBuzz, SQLite, libpng, libjpeg,
libwebp, Wayland, libxkbcommon.

```
./tools/setup-host.sh                      # AOSP host ART + SDK into .toolchain/
cmake -S . -B build -G Ninja
cmake --build build -j"$(nproc)"
```

`setup-host.sh` clones a reduced AOSP manifest and builds host ART. `--check`
reports what is present; phases (`packages`, `aosp`, `art`, `sdk`, `env`) can run individually.

`build/src/acl` finds `.toolchain/` and `build/java/framework.jar` by walking up
from itself.

```
cmake --build build --target dist
cmake --build build --target bundle
ctest --test-dir build --output-on-failure
```

`dist` is not part of the default build, it copies ~350 MB each time.

## Running it

| Command | |
|---|---|
| `acl app.apk` | Run an APK from a path |
| `acl --install app.apk` | Copy into the data root, record it, exit |
| `acl --launch com.example.app` | Run an installed package with its persistent data |
| `acl --uninstall com.example.app` | Forget a package and delete its data |
| `acl --list` | Print what is installed |

**Translation**: `--aot` (default), `--jit`, `--cache-root DIR`, `--linker PATH`

**Window**: `--portrait` (default, 1080x2400), `--landscape`, `--window WxH`, `--density N`,
`--fullscreen`, `--decorated`, `--no-vsync`, `--fps`

**Storage**: `--data-root DIR` (default `~/.local/share/acl`), `--abi NAME`, `--keep-data`

**Permissions**: `--network`, `--no-sandbox`

**Debugging**: `--verbose`, `--quiet`, `--strict`, `--trace AREAS`, `--log FILE`,
`--frame-dump FILE`, `--guest-frame-dump FILE`, `--max-frames N`, `--guest-env K=V`

**Overrides** (a bundle finds these itself): `--host-root`, `--framework-jar`, `--framework-res`,
`--font-dir`, `--guest-shim`

## Credits

ACL would not be possible without:

- [AOSP](https://source.android.com): ART, `libandroidfw`, the framework sources and the arm64
  native bridge stubs
- [LLVM](https://llvm.org): the translation backend and `lld`
- [FreeType](https://freetype.org) and [HarfBuzz](https://harfbuzz.github.io) text
- [Roboto](https://github.com/googlefonts/roboto): the bundled fonts
- [Android Translation Layer](https://gitlab.com/android_translation_layer/android_translation_layer): the inspiration for all of this

## License

This project is licensed under MIT. See [LICENSE](LICENSE).

A release bundle redistributes third-party components under their own terms. See [NOTICE](NOTICE).

## Contact & Support

- [GitHub Issues](https://github.com/reveny/Android-Compatibility-Layer/issues)
- Email: contact@reveny.me
- Telegram Contact: https://t.me/revenyy
- Telegram Group: https://t.me/reveny1