set(RUNTIME_MODULES
    Settings
    Platform
    GuestState
    Ir
    Arm64Decoder
    Interpreter
    LlvmBackend
    Apk
    PackageRegistry
    ElfLoader
    CodeMap
    AbiBridge
    TranslationCache
    GuestSync
    GuestRuntime
    GuestLibc
    GuestSyscall
    AheadOfTime
    JniBridge
    NativeActivity
    NativeWindow
    Rasterizer
    FontStack
    WaylandClient
    VulkanWsi
    GlesWsi
    Sandbox
    AudioDevice
    ControlChannel
    Diagnostics
    ApkSignature
    Overlay
)

foreach(module ${RUNTIME_MODULES})
    add_library(${module} INTERFACE)
    target_include_directories(${module} INTERFACE ${CMAKE_CURRENT_SOURCE_DIR}/${module}/include)
endforeach()

foreach(module ${RUNTIME_MODULES})
    if(NOT ${module} STREQUAL "Settings")
        target_link_libraries(${module} INTERFACE Settings $<TARGET_NAME_IF_EXISTS:SettingsImpl>)
    endif()
endforeach()

configure_file(
    ${CMAKE_CURRENT_SOURCE_DIR}/Platform/include/Platform/DeviceIdentity.hpp.in
    ${CMAKE_CURRENT_BINARY_DIR}/gen/Platform/DeviceIdentity.hpp
    @ONLY
)
target_include_directories(Platform INTERFACE ${CMAKE_CURRENT_BINARY_DIR}/gen)

target_link_libraries(Ir INTERFACE GuestState)
target_link_libraries(Arm64Decoder INTERFACE Ir GuestState)
target_link_libraries(Interpreter INTERFACE Ir GuestState)
target_link_libraries(LlvmBackend INTERFACE Ir GuestState Interpreter Platform)
target_link_libraries(AbiBridge INTERFACE GuestState Platform)
target_link_libraries(ElfLoader INTERFACE GuestState Platform)
target_link_libraries(CodeMap INTERFACE ElfLoader GuestState)
target_link_libraries(GuestRuntime INTERFACE ElfLoader AbiBridge Ir Arm64Decoder Interpreter LlvmBackend GuestState Platform Diagnostics)
target_link_libraries(GuestLibc INTERFACE GuestRuntime AbiBridge GuestSyscall Platform)
target_link_libraries(VulkanWsi INTERFACE GuestRuntime AbiBridge Platform)
target_link_libraries(GlesWsi INTERFACE GuestRuntime AbiBridge Platform Overlay)
target_link_libraries(GuestSyscall INTERFACE GuestState Diagnostics)
target_link_libraries(JniBridge INTERFACE GuestRuntime AbiBridge Diagnostics)
target_link_libraries(AheadOfTime INTERFACE GuestRuntime CodeMap Arm64Decoder LlvmBackend TranslationCache)
target_link_libraries(NativeActivity INTERFACE GuestRuntime JniBridge AbiBridge Platform)
target_link_libraries(Apk INTERFACE Platform)

target_compile_definitions(Platform INTERFACE RUNTIME_BUILD_SDK_VERSION=${RUNTIME_DECLARED_SDK_VERSION})

set(RUNTIME_JNI_INCLUDE_DIR ${RUNTIME_AOSP_DIR}/libnativehelper/include_jni
    CACHE PATH "AOSP jni.h, authoritative for the ART we embed")

target_include_directories(JniBridge SYSTEM INTERFACE ${RUNTIME_JNI_INCLUDE_DIR})

add_library(SettingsImpl STATIC Settings/Settings.cpp)
target_link_libraries(SettingsImpl PUBLIC Settings)
target_compile_options(SettingsImpl PRIVATE -Wall -Wextra -Werror)

add_library(PlatformImpl STATIC Platform/Platform.cpp)
target_link_libraries(PlatformImpl PUBLIC Platform Settings PRIVATE SettingsImpl)
target_compile_options(PlatformImpl PRIVATE -Wall -Wextra -Werror)

add_library(GuestStateImpl STATIC GuestState/GuestState.cpp)
target_link_libraries(GuestStateImpl PUBLIC GuestState)
target_compile_options(GuestStateImpl PRIVATE -Wall -Wextra -Werror)

add_library(IrImpl STATIC Ir/Ir.cpp)
target_link_libraries(IrImpl PUBLIC Ir GuestStateImpl)
target_compile_options(IrImpl PRIVATE -Wall -Wextra -Werror)

add_library(Arm64DecoderImpl STATIC Arm64Decoder/Arm64Decoder.cpp)
target_link_libraries(Arm64DecoderImpl PUBLIC Arm64Decoder IrImpl GuestStateImpl)
target_compile_options(Arm64DecoderImpl PRIVATE -Wall -Wextra -Werror)

add_library(InterpreterImpl STATIC Interpreter/Interpreter.cpp Interpreter/IrSemantics.cpp)
target_link_libraries(InterpreterImpl PUBLIC Interpreter IrImpl GuestStateImpl)
target_compile_options(InterpreterImpl PRIVATE -Wall -Wextra -Werror)

add_library(TranslationCacheImpl STATIC TranslationCache/TranslationCache.cpp)
target_link_libraries(TranslationCacheImpl PUBLIC TranslationCache)
target_compile_options(TranslationCacheImpl PRIVATE -Wall -Wextra -Werror)

add_library(LlvmBackendImpl STATIC LlvmBackend/LlvmBackend.cpp)
target_link_libraries(LlvmBackendImpl PUBLIC LlvmBackend InterpreterImpl IrImpl GuestStateImpl PRIVATE LLVM)
target_include_directories(LlvmBackendImpl SYSTEM PRIVATE ${LLVM_INCLUDE_DIRS})
target_compile_options(LlvmBackendImpl PRIVATE -Werror)
set_target_properties(LlvmBackendImpl PROPERTIES CXX_SCAN_FOR_MODULES OFF)

add_library(ElfLoaderImpl STATIC ElfLoader/ElfLoader.cpp)
target_link_libraries(ElfLoaderImpl PUBLIC ElfLoader GuestStateImpl PlatformImpl)
target_compile_options(ElfLoaderImpl PRIVATE -Wall -Wextra -Werror)

add_library(CodeMapImpl STATIC CodeMap/CodeMap.cpp)
target_link_libraries(CodeMapImpl PUBLIC CodeMap ElfLoaderImpl)
target_compile_options(CodeMapImpl PRIVATE -Wall -Wextra -Werror)

add_library(AbiBridgeImpl STATIC AbiBridge/AbiBridge.cpp)
target_link_libraries(AbiBridgeImpl PUBLIC AbiBridge GuestStateImpl PlatformImpl)
target_compile_options(AbiBridgeImpl PRIVATE -Wall -Wextra -Werror)

add_library(GuestSyscallImpl STATIC GuestSyscall/GuestSyscall.cpp)
target_link_libraries(GuestSyscallImpl PUBLIC GuestSyscall GuestStateImpl DiagnosticsImpl)
target_compile_options(GuestSyscallImpl PRIVATE -Wall -Wextra -Werror)

add_library(GuestSyncImpl STATIC GuestSync/GuestSync.cpp)
target_link_libraries(GuestSyncImpl PUBLIC GuestSync Threads::Threads)
target_compile_options(GuestSyncImpl PRIVATE -Wall -Wextra -Werror)

add_library(GuestRuntimeImpl STATIC GuestRuntime/GuestRuntime.cpp)
target_link_libraries(GuestRuntimeImpl PUBLIC GuestRuntime ElfLoaderImpl AbiBridgeImpl Arm64DecoderImpl InterpreterImpl GuestSyscallImpl
                      GuestSyncImpl LlvmBackendImpl)
target_compile_options(GuestRuntimeImpl PRIVATE -Wall -Wextra -Werror)

add_executable(PlatformUnitTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/platform/PlatformUnit.cpp)
target_link_libraries(PlatformUnitTest PRIVATE PlatformImpl SettingsImpl)
target_compile_options(PlatformUnitTest PRIVATE -Wall -Wextra -Werror)

add_library(ApkSignatureImpl STATIC ApkSignature/ApkSignature.cpp ApkSignature/ApkSignatureInternal.cpp)
target_link_libraries(ApkSignatureImpl PUBLIC ApkSignature)
target_compile_options(ApkSignatureImpl PRIVATE -Wall -Wextra -Werror)

add_library(ApkImpl STATIC Apk/Apk.cpp)
target_link_libraries(ApkImpl PUBLIC Apk Platform PlatformImpl PRIVATE androidfw_c PkgConfig::ZLIB)
target_compile_options(ApkImpl PRIVATE -Werror)

add_library(PackageRegistryImpl STATIC PackageRegistry/PackageRegistry.cpp)
target_link_libraries(PackageRegistryImpl PUBLIC PackageRegistry Apk PRIVATE ApkImpl)
target_compile_options(PackageRegistryImpl PRIVATE -Wall -Wextra -Werror)

add_executable(InterpreterVectorsTest
    ${CMAKE_CURRENT_SOURCE_DIR}/../tests/vectors/InterpreterVectors.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/../tests/vectors/src/VectorCases.cpp
)
target_include_directories(InterpreterVectorsTest PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/../tests/vectors/src)
target_link_libraries(InterpreterVectorsTest PRIVATE Arm64DecoderImpl InterpreterImpl)
target_compile_options(InterpreterVectorsTest PRIVATE -Wall -Wextra -Werror)

add_executable(CorpusCoverageTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/arm64/CorpusCoverage.cpp)
target_link_libraries(CorpusCoverageTest PRIVATE Arm64DecoderImpl)
target_compile_options(CorpusCoverageTest PRIVATE -Wall -Wextra -Werror)

add_executable(DecodeDifferentialTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/arm64/DecodeDifferential.cpp)
target_link_libraries(DecodeDifferentialTest PRIVATE Arm64DecoderImpl InterpreterImpl)
target_compile_options(DecodeDifferentialTest PRIVATE -Wall -Wextra -Werror)

add_library(aclguest SHARED GuestShim/GuestShim.cpp)
target_include_directories(aclguest SYSTEM PRIVATE ${RUNTIME_JNI_INCLUDE_DIR})
target_link_libraries(aclguest PRIVATE ${CMAKE_DL_LIBS})
target_compile_options(aclguest PRIVATE -Werror)
set_target_properties(aclguest PROPERTIES CXX_SCAN_FOR_MODULES OFF)

add_library(AudioDeviceImpl STATIC AudioDevice/AudioDevice.cpp)
target_link_libraries(AudioDeviceImpl PUBLIC AudioDevice)
target_compile_options(AudioDeviceImpl PRIVATE -Wall -Wextra -Werror)

if(RUNTIME_ENABLE_AUDIO)
    target_compile_definitions(AudioDeviceImpl PRIVATE RUNTIME_HAVE_AUDIO=1)
    target_link_libraries(AudioDeviceImpl PRIVATE PkgConfig::PIPEWIRE)
endif()

add_library(NativeActivityImpl STATIC NativeActivity/NativeActivity.cpp NativeActivity/NativeActivityJni.cpp
            NativeActivity/NativeWindowBridge.cpp NativeActivity/AudioBridge.cpp)
target_link_libraries(NativeActivityImpl PUBLIC NativeActivity GuestRuntimeImpl JniBridgeImpl AbiBridgeImpl AudioDeviceImpl)
target_include_directories(NativeActivityImpl SYSTEM PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/NativeActivity/compat
                           ${CMAKE_SOURCE_DIR}/third_party/native/include ${RUNTIME_JNI_INCLUDE_DIR})
target_compile_options(NativeActivityImpl PRIVATE -Wall -Wextra -Werror)

add_library(OverlayImpl STATIC Overlay/Overlay.cpp Overlay/OverlayFont.cpp Overlay/FrameRate.cpp)
target_link_libraries(OverlayImpl PUBLIC Overlay)
target_compile_options(OverlayImpl PRIVATE -Wall -Wextra -Werror)

if(RUNTIME_ENABLE_VULKAN)
    add_library(OverlayVulkanImpl STATIC Overlay/OverlayVulkan.cpp)
    target_link_libraries(OverlayVulkanImpl PUBLIC Overlay OverlayImpl PRIVATE Vulkan::Headers)
    target_compile_options(OverlayVulkanImpl PRIVATE -Wall -Wextra -Werror)

    add_library(VulkanWsiImpl STATIC VulkanWsi/VulkanWsi.cpp)
    target_link_libraries(VulkanWsiImpl PUBLIC VulkanWsi GuestRuntimeImpl AbiBridgeImpl Overlay
                          PRIVATE OverlayVulkanImpl OverlayImpl Vulkan::Headers ${CMAKE_DL_LIBS})
    target_compile_options(VulkanWsiImpl PRIVATE -Wall -Wextra -Werror)
endif()

if(RUNTIME_ENABLE_GLES)
    add_library(GlesWsiImpl STATIC GlesWsi/GlesWsi.cpp)
    target_link_libraries(GlesWsiImpl PUBLIC GlesWsi GuestRuntimeImpl AbiBridgeImpl Overlay PRIVATE OverlayImpl ${CMAKE_DL_LIBS})
    target_include_directories(GlesWsiImpl PRIVATE ${EGL_INCLUDE_DIRS} ${GLESV2_INCLUDE_DIRS} ${WAYLAND_EGL_INCLUDE_DIRS})
    target_compile_options(GlesWsiImpl PRIVATE -Wall -Wextra -Werror)
endif()

add_library(SandboxImpl STATIC Sandbox/Sandbox.cpp Sandbox/SyscallFilter.cpp Sandbox/Isolation.cpp)
target_link_libraries(SandboxImpl PUBLIC Sandbox)
target_compile_options(SandboxImpl PRIVATE -Wall -Wextra -Werror)

add_executable(SandboxUnitTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/sandbox/SandboxUnit.cpp)
target_link_libraries(SandboxUnitTest PRIVATE SandboxImpl)
target_compile_options(SandboxUnitTest PRIVATE -Wall -Wextra -Werror)

add_library(AheadOfTimeImpl STATIC AheadOfTime/AheadOfTime.cpp)
target_link_libraries(AheadOfTimeImpl PUBLIC AheadOfTime GuestRuntimeImpl CodeMapImpl Arm64DecoderImpl LlvmBackendImpl
                      TranslationCacheImpl PRIVATE ${CMAKE_DL_LIBS})
target_compile_options(AheadOfTimeImpl PRIVATE -Wall -Wextra -Werror)

add_library(JniBridgeImpl STATIC JniBridge/JniBridge.cpp)
target_link_libraries(JniBridgeImpl PUBLIC JniBridge GuestRuntimeImpl AbiBridgeImpl)
target_compile_options(JniBridgeImpl PRIVATE -Wall -Wextra -Werror)

add_library(GuestLibcImpl STATIC GuestLibc/GuestLibc.cpp GuestLibc/HostBridgeLinux.cpp)
target_link_libraries(GuestLibcImpl PUBLIC GuestLibc GuestRuntimeImpl GuestSyscallImpl PRIVATE PkgConfig::ZLIB)
target_compile_options(GuestLibcImpl PRIVATE -Wall -Wextra -Werror)

add_executable(TombstoneHarness ${CMAKE_CURRENT_SOURCE_DIR}/../tests/tombstone/TombstoneHarness.cpp)
target_link_libraries(TombstoneHarness PRIVATE AheadOfTimeImpl GuestLibcImpl DiagnosticsImpl)
target_link_options(TombstoneHarness PRIVATE -Wl,--export-dynamic)
target_compile_options(TombstoneHarness PRIVATE -Wall -Wextra -Werror)

add_executable(GuestLibGateTest
    ${CMAKE_CURRENT_SOURCE_DIR}/../tests/guestlib/GuestLibGate.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/../tests/guestlib/src/hello.c
)
target_link_libraries(GuestLibGateTest PRIVATE GuestLibcImpl)
target_compile_options(GuestLibGateTest PRIVATE -Wall -Wextra -Werror)

add_executable(GuestLibHostCallTest
    ${CMAKE_CURRENT_SOURCE_DIR}/../tests/guestlib/GuestLibHostCall.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/../tests/guestlib/src/hello.c
)
target_link_libraries(GuestLibHostCallTest PRIVATE GuestLibcImpl)
target_compile_options(GuestLibHostCallTest PRIVATE -Wall -Wextra -Werror)

add_executable(AheadOfTimeGateTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/aheadoftime/AheadOfTimeGate.cpp)
target_link_libraries(AheadOfTimeGateTest PRIVATE AheadOfTimeImpl GuestLibcImpl)
target_link_options(AheadOfTimeGateTest PRIVATE -Wl,--export-dynamic)
target_compile_options(AheadOfTimeGateTest PRIVATE -Wall -Wextra -Werror)

add_executable(FunctionDifferentialTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/tiers/FunctionDifferential.cpp)
target_link_libraries(FunctionDifferentialTest PRIVATE GuestLibcImpl Arm64DecoderImpl LlvmBackendImpl)
target_compile_options(FunctionDifferentialTest PRIVATE -Wall -Wextra -Werror)

add_executable(CodeMapUnitTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/codemap/CodeMapUnit.cpp)
target_link_libraries(CodeMapUnitTest PRIVATE CodeMapImpl GuestLibcImpl Arm64DecoderImpl LlvmBackendImpl)
target_compile_options(CodeMapUnitTest PRIVATE -Wall -Wextra -Werror)

add_executable(JniBridgeGateTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/jnibridge/JniBridgeGate.cpp)
target_link_libraries(JniBridgeGateTest PRIVATE GuestLibcImpl JniBridgeImpl ArtRuntime)
target_link_options(JniBridgeGateTest PRIVATE -Wl,--export-dynamic)
target_compile_options(JniBridgeGateTest PRIVATE -Werror)

add_executable(InvalidationUnitTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/tiers/InvalidationUnit.cpp)
target_link_libraries(InvalidationUnitTest PRIVATE GuestRuntimeImpl)
target_compile_options(InvalidationUnitTest PRIVATE -Wall -Wextra -Werror)

add_executable(TranslationCacheUnitTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/cache/TranslationCacheUnit.cpp)
target_link_libraries(TranslationCacheUnitTest PRIVATE TranslationCacheImpl)
target_compile_options(TranslationCacheUnitTest PRIVATE -Wall -Wextra -Werror)

add_executable(BenchTiersTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/bench/BenchTiers.cpp)
target_link_libraries(BenchTiersTest PRIVATE GuestLibcImpl AheadOfTimeImpl)
target_link_options(BenchTiersTest PRIVATE -Wl,--export-dynamic)
target_compile_options(BenchTiersTest PRIVATE -Wall -Wextra -Werror)

add_executable(DiffTiersTest
    ${CMAKE_CURRENT_SOURCE_DIR}/../tests/tiers/DiffTiers.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/../tests/vectors/src/VectorCases.cpp
)
target_include_directories(DiffTiersTest PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/../tests/vectors/src)
target_link_libraries(DiffTiersTest PRIVATE LlvmBackendImpl Arm64DecoderImpl InterpreterImpl)
target_compile_options(DiffTiersTest PRIVATE -Wall -Wextra -Werror)

add_executable(OrderingGateTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/ordering/OrderingGate.cpp)
target_link_libraries(OrderingGateTest PRIVATE GuestLibcImpl)
target_compile_options(OrderingGateTest PRIVATE -Wall -Wextra -Werror)

add_executable(GuestLibcUnitTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/guestlib/GuestLibcUnit.cpp)
target_link_libraries(GuestLibcUnitTest PRIVATE GuestLibcImpl)
target_compile_options(GuestLibcUnitTest PRIVATE -Wall -Wextra -Werror)

add_executable(AbiBridgeMarshalUnitTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/abibridge/AbiBridgeMarshalUnit.cpp)
target_link_libraries(AbiBridgeMarshalUnitTest PRIVATE AbiBridgeImpl)
target_compile_options(AbiBridgeMarshalUnitTest PRIVATE -Wall -Wextra -Werror)

add_executable(ElfLoaderUnitTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/elf/ElfLoaderUnit.cpp)
target_link_libraries(ElfLoaderUnitTest PRIVATE GuestRuntimeImpl)
target_compile_options(ElfLoaderUnitTest PRIVATE -Wall -Wextra -Werror)

add_executable(ElfReadelfDifferentialTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/elf/ElfReadelfDifferential.cpp)
target_link_libraries(ElfReadelfDifferentialTest PRIVATE GuestRuntimeImpl)
target_compile_options(ElfReadelfDifferentialTest PRIVATE -Wall -Wextra -Werror)

add_executable(ApkZipUnitTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/apk/ApkZipUnit.cpp)
target_link_libraries(ApkZipUnitTest PRIVATE ApkImpl PkgConfig::ZLIB)
target_compile_options(ApkZipUnitTest PRIVATE -Werror)

add_library(ArtRuntime STATIC
    ArtRuntime/ArtRuntime.cpp
    ArtRuntime/IcuSetup.cpp
)
target_include_directories(ArtRuntime
    PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/ArtRuntime/include
    SYSTEM PUBLIC ${RUNTIME_JNI_INCLUDE_DIR}
)
target_link_libraries(ArtRuntime PUBLIC Platform PRIVATE ${CMAKE_DL_LIBS})
target_compile_options(ArtRuntime PRIVATE -Werror)

if(RUNTIME_ENABLE_GRAPHICS)
    add_library(WaylandClientImpl STATIC WaylandClient/WaylandClient.cpp)
    target_link_libraries(WaylandClientImpl PUBLIC WaylandClient PRIVATE PkgConfig::WAYLAND_CLIENT PkgConfig::XKBCOMMON)
    target_compile_options(WaylandClientImpl PRIVATE -Werror)
    set_target_properties(WaylandClientImpl PROPERTIES CXX_SCAN_FOR_MODULES OFF)

    runtime_add_wayland_protocol(WaylandClientImpl
        ${RUNTIME_WAYLAND_PROTOCOLS_DIR}/stable/xdg-shell/xdg-shell.xml xdg-shell)
    runtime_add_wayland_protocol(WaylandClientImpl
        ${RUNTIME_WAYLAND_PROTOCOLS_DIR}/stable/presentation-time/presentation-time.xml presentation-time)
    runtime_add_wayland_protocol(WaylandClientImpl
        ${RUNTIME_WAYLAND_PROTOCOLS_DIR}/unstable/xdg-decoration/xdg-decoration-unstable-v1.xml xdg-decoration-unstable-v1)

    add_executable(WindowSmokeTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/window/WindowSmokeTest.cpp)
    target_link_libraries(WindowSmokeTest PRIVATE WaylandClientImpl RasterizerImpl)
    target_compile_options(WindowSmokeTest PRIVATE -Werror)

    add_executable(WindowResizeTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/window/WindowResizeTest.cpp)
    target_link_libraries(WindowResizeTest PRIVATE WaylandClientImpl RasterizerImpl)
    target_compile_options(WindowResizeTest PRIVATE -Wall -Wextra -Werror)
else()
    add_library(WaylandClientImpl STATIC WaylandClient/WaylandClientHeadless.cpp)
    target_link_libraries(WaylandClientImpl PUBLIC WaylandClient)
    target_compile_options(WaylandClientImpl PRIVATE -Werror)
    set_target_properties(WaylandClientImpl PROPERTIES CXX_SCAN_FOR_MODULES OFF)
endif()

add_library(DiagnosticsImpl STATIC Diagnostics/Diagnostics.cpp)
target_link_libraries(DiagnosticsImpl PUBLIC Diagnostics)
target_compile_options(DiagnosticsImpl PRIVATE -Wall -Wextra -Werror)

add_executable(SignatureUnitTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/signature/SignatureUnit.cpp)
target_link_libraries(SignatureUnitTest PRIVATE ApkSignatureImpl)
target_compile_options(SignatureUnitTest PRIVATE -Wall -Wextra -Werror)

add_executable(DiagnosticsUnitTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/diagnostics/DiagnosticsUnit.cpp)
target_link_libraries(DiagnosticsUnitTest PRIVATE DiagnosticsImpl)
target_compile_options(DiagnosticsUnitTest PRIVATE -Wall -Wextra -Werror)

add_library(ControlChannelImpl STATIC ControlChannel/ControlChannel.cpp)
target_link_libraries(ControlChannelImpl PUBLIC ControlChannel)
target_compile_options(ControlChannelImpl PRIVATE -Wall -Wextra -Werror)

add_executable(ControlChannelUnitTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/control/ControlChannelUnit.cpp)
target_link_libraries(ControlChannelUnitTest PRIVATE ControlChannelImpl)
target_compile_options(ControlChannelUnitTest PRIVATE -Wall -Wextra -Werror)

add_executable(HostedWindowHarness ${CMAKE_CURRENT_SOURCE_DIR}/../tests/hosted/HostedWindowHarness.cpp)
target_link_libraries(HostedWindowHarness PRIVATE ControlChannelImpl)
target_compile_options(HostedWindowHarness PRIVATE -Wall -Wextra -Werror)

add_library(ImageCodec INTERFACE)
target_include_directories(ImageCodec INTERFACE ${CMAKE_CURRENT_SOURCE_DIR}/ImageCodec/include)

add_library(ImageCodecImpl STATIC ImageCodec/ImageCodec.cpp)
target_link_libraries(ImageCodecImpl PUBLIC ImageCodec PRIVATE PkgConfig::PNG PkgConfig::JPEG PkgConfig::WEBP)
target_compile_options(ImageCodecImpl PRIVATE -Werror)

add_executable(ImageCodecUnitTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/image/ImageCodecUnit.cpp)
target_link_libraries(ImageCodecUnitTest PRIVATE ImageCodecImpl PkgConfig::PNG PkgConfig::JPEG PkgConfig::WEBP
                      PkgConfig::ZLIB)
target_compile_options(ImageCodecUnitTest PRIVATE -Werror)

add_library(RasterizerImpl STATIC Rasterizer/Rasterizer.cpp Rasterizer/VectorPath.cpp Rasterizer/VectorFill.cpp
            Rasterizer/VectorState.cpp)
target_link_libraries(RasterizerImpl PUBLIC Rasterizer PRIVATE PkgConfig::FREETYPE)
target_compile_options(RasterizerImpl PRIVATE -Werror)

add_executable(RasterizerGoldenTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/rasterizer/RasterizerGolden.cpp)
target_link_libraries(RasterizerGoldenTest PRIVATE RasterizerImpl)
target_compile_options(RasterizerGoldenTest PRIVATE -Werror)

add_executable(VectorUnitTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/rasterizer/VectorUnit.cpp)
target_link_libraries(VectorUnitTest PRIVATE RasterizerImpl)
target_compile_options(VectorUnitTest PRIVATE -Werror)

add_library(FontStackImpl STATIC
    FontStack/FontStack.cpp
    FontStack/FontRegistry.cpp
    FontStack/GlyphCache.cpp
    FontStack/ShapeCache.cpp
)
target_link_libraries(FontStackImpl PUBLIC FontStack PRIVATE PkgConfig::FREETYPE PkgConfig::HARFBUZZ)
target_compile_definitions(FontStackImpl PRIVATE RUNTIME_DEFAULT_FONT_DIR="${RUNTIME_FONT_DIR}")
target_compile_options(FontStackImpl PRIVATE -Werror)
set_target_properties(FontStackImpl PROPERTIES CXX_SCAN_FOR_MODULES OFF)

add_executable(FontStackUnitTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/text/FontStackUnit.cpp)
target_link_libraries(FontStackUnitTest PRIVATE FontStackImpl)
target_compile_options(FontStackUnitTest PRIVATE -Werror)

add_executable(TextMetricsHarness ${CMAKE_CURRENT_SOURCE_DIR}/../tests/text/TextMetricsHarness.cpp)
target_link_libraries(TextMetricsHarness PRIVATE FontStackImpl)
target_compile_options(TextMetricsHarness PRIVATE -Werror)

add_executable(TextGoldenTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/text/TextGolden.cpp)
target_link_libraries(TextGoldenTest PRIVATE FontStackImpl RasterizerImpl PkgConfig::FREETYPE)
target_compile_definitions(TextGoldenTest PRIVATE RUNTIME_DEFAULT_FONT_DIR="${RUNTIME_FONT_DIR}")
target_compile_options(TextGoldenTest PRIVATE -Werror)


add_library(FrameworkJni STATIC FrameworkJni/FrameworkJni.cpp FrameworkJni/WindowBridge.cpp FrameworkJni/HostedWindow.cpp
                                FrameworkJni/ResourceBridge.cpp
                                FrameworkJni/TextBridge.cpp FrameworkJni/SqliteBridge.cpp
                                FrameworkJni/GraphicsBridge.cpp FrameworkJni/BitmapBridge.cpp)
target_include_directories(FrameworkJni PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/FrameworkJni/include)
target_link_libraries(FrameworkJni PUBLIC ArtRuntime Apk PackageRegistry Rasterizer FontStack WaylandClient ControlChannel Diagnostics
                                          ApkSignature Overlay
                      PRIVATE ApkImpl PackageRegistryImpl RasterizerImpl FontStackImpl WaylandClientImpl ImageCodecImpl androidfw_c
                              ControlChannelImpl ApkSignatureImpl OverlayImpl PkgConfig::SQLITE3)
target_compile_options(FrameworkJni PRIVATE -Werror)

if(RUNTIME_ENABLE_VULKAN)
    target_link_libraries(FrameworkJni PRIVATE VulkanWsiImpl)
    target_compile_definitions(FrameworkJni PRIVATE RUNTIME_HAVE_VULKAN=1)
endif()

if(RUNTIME_ENABLE_GLES)
    target_link_libraries(FrameworkJni PRIVATE GlesWsiImpl)
    target_compile_definitions(FrameworkJni PRIVATE RUNTIME_HAVE_GLES=1)
endif()

add_executable(acl Launcher/Main.cpp Launcher/Launcher.cpp)
target_include_directories(acl PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/Launcher/include)
target_link_libraries(acl PRIVATE FrameworkJni ApkImpl ArtRuntime GuestLibcImpl JniBridgeImpl AheadOfTimeImpl NativeActivityImpl SandboxImpl DiagnosticsImpl
                      TranslationCacheImpl SettingsImpl)
target_compile_definitions(acl PRIVATE RUNTIME_DEFAULT_FONT_DIR="${RUNTIME_FONT_DIR}")
if(RUNTIME_ENABLE_VULKAN)
    target_link_libraries(acl PRIVATE VulkanWsiImpl)
    target_compile_definitions(acl PRIVATE RUNTIME_HAVE_VULKAN=1)
endif()
if(RUNTIME_ENABLE_GLES)
    target_link_libraries(acl PRIVATE GlesWsiImpl)
    target_compile_definitions(acl PRIVATE RUNTIME_HAVE_GLES=1)
endif()

set_target_properties(acl PROPERTIES BUILD_RPATH "$ORIGIN/../lib;$ORIGIN/../art/lib64")

add_dependencies(acl aclguest)
target_link_options(acl PRIVATE -Wl,--export-dynamic)
target_compile_options(acl PRIVATE -Werror)
add_dependencies(acl FrameworkJar)

add_executable(ArtRuntimeSmokeTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/artruntime/ArtRuntimeSmokeTest.cpp)
target_link_libraries(ArtRuntimeSmokeTest PRIVATE ArtRuntime)
target_link_options(ArtRuntimeSmokeTest PRIVATE -Wl,--export-dynamic)

add_custom_target(ContractCheck ALL)

foreach(module ${RUNTIME_MODULES})
    set(generated ${CMAKE_CURRENT_BINARY_DIR}/contract_${module}.cpp)
    file(GENERATE OUTPUT ${generated} CONTENT "#include \"${module}/${module}.hpp\"\n")
    add_library(contract_${module} OBJECT ${generated})
    target_link_libraries(contract_${module} PRIVATE ${module})
    target_compile_options(contract_${module} PRIVATE -Werror)
    add_dependencies(ContractCheck contract_${module})
endforeach()

add_test(NAME contract-headers-compile
         COMMAND ${CMAKE_COMMAND}
                 -DBINARY_DIR=${CMAKE_CURRENT_BINARY_DIR}
                 -DSOURCE_DIR=${CMAKE_CURRENT_SOURCE_DIR}
                 -P ${CMAKE_CURRENT_SOURCE_DIR}/CheckContractObjects.cmake)

add_test(NAME apk-zip-unit
         COMMAND ApkZipUnitTest)

add_test(NAME interpreter-vectors
         COMMAND InterpreterVectorsTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/vectors/golden-arm64-t0.txt)

add_test(NAME abibridge-marshal
         COMMAND AbiBridgeMarshalUnitTest)

add_test(NAME elfloader-unit
         COMMAND ElfLoaderUnitTest)

add_test(NAME guestlibc-unit
         COMMAND GuestLibcUnitTest)

add_test(NAME tier-differential
         COMMAND DiffTiersTest)

add_test(NAME invalidation-unit
         COMMAND InvalidationUnitTest)

file(MAKE_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/cache-scratch)
add_test(NAME translation-cache-unit
         COMMAND TranslationCacheUnitTest ${CMAKE_CURRENT_BINARY_DIR}/cache-scratch)

add_test(NAME control-channel-unit
         COMMAND ControlChannelUnitTest)

add_test(NAME platform-unit
         COMMAND PlatformUnitTest)

add_test(NAME diagnostics-unit
         COMMAND DiagnosticsUnitTest ${CMAKE_CURRENT_BINARY_DIR}/diagnostics-scratch)

add_test(NAME signature-unit
         COMMAND SignatureUnitTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/app-debug.apk
                 ${CMAKE_CURRENT_BINARY_DIR}/signature-scratch)

add_test(NAME tombstone-gate
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-tombstone-gate.sh ${CMAKE_BINARY_DIR})
set_tests_properties(tombstone-gate PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 420)

add_test(NAME bench-tiers
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/bench-tiers.sh ${CMAKE_BINARY_DIR})
set_tests_properties(bench-tiers PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 900)

add_test(NAME guest-libc-gate-t1
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-guest-libc-gate.sh ${CMAKE_BINARY_DIR} t1)
set_tests_properties(guest-libc-gate-t1 PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 600)

add_test(NAME ordering-gate
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-ordering-gate.sh ${CMAKE_BINARY_DIR} t1)
set_tests_properties(ordering-gate PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 600)

add_test(NAME elf-readelf-differential
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-elf-differential.sh ${CMAKE_BINARY_DIR})
set_tests_properties(elf-readelf-differential PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 300)

add_test(NAME guest-elf-gate
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-guest-elf-gate.sh ${CMAKE_BINARY_DIR})
set_tests_properties(guest-elf-gate PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 300)

add_test(NAME aheadoftime-gate
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-aheadoftime-gate.sh ${CMAKE_BINARY_DIR})
set_tests_properties(aheadoftime-gate PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 600)

add_test(NAME function-differential
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-function-differential.sh ${CMAKE_BINARY_DIR})
set_tests_properties(function-differential PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 300)

add_test(NAME codemap-unit
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-codemap-unit.sh ${CMAKE_BINARY_DIR})
set_tests_properties(codemap-unit PROPERTIES TIMEOUT 300)

add_test(NAME guest-jni-gate
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-guest-jni-gate.sh ${CMAKE_BINARY_DIR})
set_tests_properties(guest-jni-gate PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 300)

add_test(NAME guest-jni-gate-t1
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-guest-jni-gate.sh ${CMAKE_BINARY_DIR} t1)
set_tests_properties(guest-jni-gate-t1 PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 300)

add_test(NAME guest-libc-gate
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-guest-libc-gate.sh ${CMAKE_BINARY_DIR})
set_tests_properties(guest-libc-gate PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 300)

add_test(NAME arm64-decode-differential
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-decode-differential.sh ${CMAKE_BINARY_DIR})
set_tests_properties(arm64-decode-differential PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 300)

add_test(NAME rasterizer-golden
         COMMAND RasterizerGoldenTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/rasterizer/pattern-golden.raw)

add_test(NAME vector-unit
         COMMAND VectorUnitTest)

add_test(NAME image-codec
         COMMAND ImageCodecUnitTest)

add_test(NAME graphics-gate
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-graphics-gate.sh ${CMAKE_BINARY_DIR})
set_tests_properties(graphics-gate PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 120)

add_test(NAME shapes-gate
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-shapes-gate.sh ${CMAKE_BINARY_DIR})
set_tests_properties(shapes-gate PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 300)

add_test(NAME drawables-gate
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-drawables-gate.sh ${CMAKE_BINARY_DIR})
set_tests_properties(drawables-gate PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 300)

add_test(NAME interaction-gate
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-interaction-gate.sh ${CMAKE_BINARY_DIR})
set_tests_properties(interaction-gate PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 300)

add_test(NAME fontstack-unit
         COMMAND FontStackUnitTest)

add_test(NAME text-golden
         COMMAND TextGoldenTest ${CMAKE_CURRENT_SOURCE_DIR}/../tests/text/text-golden.raw)

add_test(NAME text-metrics
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-text-metrics-gate.sh ${CMAKE_BINARY_DIR})

add_test(NAME android-text-metrics
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/capture-text-metrics.sh --check)
set_tests_properties(android-text-metrics PROPERTIES SKIP_RETURN_CODE 77)

if(RUNTIME_ENABLE_GRAPHICS)
    add_test(NAME window-gate
             COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-window-gate.sh ${CMAKE_BINARY_DIR})
    set_tests_properties(window-gate PROPERTIES SKIP_RETURN_CODE 77)

    add_test(NAME resize-gate
             COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-resize-gate.sh ${CMAKE_BINARY_DIR})
    set_tests_properties(resize-gate PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 120)

    add_test(NAME activity-draw-gate
             COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-activity-draw-gate.sh ${CMAKE_BINARY_DIR})
    set_tests_properties(activity-draw-gate PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 180)

    add_test(NAME diagnostics-gate
             COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-diagnostics-gate.sh ${CMAKE_BINARY_DIR})
    set_tests_properties(diagnostics-gate PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 300)

    add_test(NAME layout-gate
             COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-layout-gate.sh ${CMAKE_BINARY_DIR})
    set_tests_properties(layout-gate PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 300)

    add_test(NAME storage-gate
             COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-storage-gate.sh ${CMAKE_BINARY_DIR})
    set_tests_properties(storage-gate PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 300)
endif()

add_test(NAME jni-gate
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-jni-gate.sh ${CMAKE_BINARY_DIR})
set_tests_properties(jni-gate PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 300)

add_test(NAME archive-gate
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-archive-gate.sh ${CMAKE_BINARY_DIR})
set_tests_properties(archive-gate PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 300)

add_test(NAME framework-gate
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-framework-gate.sh ${CMAKE_BINARY_DIR})
set_tests_properties(framework-gate PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 300)

add_test(NAME surface-gate
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-surface-gate.sh ${CMAKE_BINARY_DIR})
set_tests_properties(surface-gate PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 300)

add_test(NAME overlay-gate
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-overlay-gate.sh ${CMAKE_BINARY_DIR})
set_tests_properties(overlay-gate PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 600)

add_test(NAME update-gate
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-update-gate.sh ${CMAKE_BINARY_DIR})
set_tests_properties(update-gate PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 600)

add_test(NAME packaging-gate
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-packaging-gate.sh ${CMAKE_BINARY_DIR})
set_tests_properties(packaging-gate PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 600)

add_test(NAME lifecycle-gate
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-lifecycle-gate.sh ${CMAKE_BINARY_DIR})
set_tests_properties(lifecycle-gate PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 300)

add_test(NAME native-activity-gate
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-native-activity-gate.sh ${CMAKE_BINARY_DIR})
set_tests_properties(native-activity-gate PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 300)

add_test(NAME guest-vulkan-gate
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-guest-vulkan-gate.sh ${CMAKE_BINARY_DIR})
set_tests_properties(guest-vulkan-gate PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 300)

add_test(NAME gles-gate
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-gles-gate.sh ${CMAKE_BINARY_DIR})
set_tests_properties(gles-gate PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 300)

add_test(NAME view-transform-gate
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-view-transform-gate.sh ${CMAKE_BINARY_DIR})
set_tests_properties(view-transform-gate PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 420)

add_test(NAME sandbox-unit COMMAND SandboxUnitTest)
set_tests_properties(sandbox-unit PROPERTIES SKIP_RETURN_CODE 77)

add_test(NAME pacing-gate
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-pacing-gate.sh ${CMAKE_BINARY_DIR})
set_tests_properties(pacing-gate PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 400)

add_test(NAME strict-gate
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-strict-gate.sh ${CMAKE_BINARY_DIR})
set_tests_properties(strict-gate PROPERTIES SKIP_RETURN_CODE 77)

add_test(NAME framework-java
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-framework-tests.sh ${CMAKE_BINARY_DIR})

add_test(NAME artruntime-smoke
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-smoke-test.sh ${CMAKE_BINARY_DIR})
set_tests_properties(artruntime-smoke PROPERTIES SKIP_RETURN_CODE 77)

add_test(NAME boot-gate
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-boot-gate.sh ${CMAKE_BINARY_DIR})
set_tests_properties(boot-gate PROPERTIES SKIP_RETURN_CODE 77)

add_test(NAME provider-gate
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-provider-gate.sh ${CMAKE_BINARY_DIR})
set_tests_properties(provider-gate PROPERTIES SKIP_RETURN_CODE 77)

add_test(NAME arm64-golden-vectors
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/capture-vectors.sh --check)
set_tests_properties(arm64-golden-vectors PROPERTIES SKIP_RETURN_CODE 77)

add_test(NAME arm64-corpus-coverage
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/run-corpus-coverage.sh ${CMAKE_BINARY_DIR})
set_tests_properties(arm64-corpus-coverage PROPERTIES SKIP_RETURN_CODE 77)

add_test(NAME arm64-histogram
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/histogram-arm64.sh --check)
set_tests_properties(arm64-histogram PROPERTIES SKIP_RETURN_CODE 77)

add_test(NAME arm64-litmus
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/capture-litmus.sh --check)
set_tests_properties(arm64-litmus PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 600)

add_test(NAME android-t0-vectors
         COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../tools/capture-t0-vectors.sh --check)
set_tests_properties(android-t0-vectors PROPERTIES SKIP_RETURN_CODE 77 TIMEOUT 300)

get_property(RUNTIME_REGISTERED_TESTS DIRECTORY PROPERTY TESTS)
if(RUNTIME_REGISTERED_TESTS)
    set_tests_properties(${RUNTIME_REGISTERED_TESTS} PROPERTIES ENVIRONMENT "${RUNTIME_TEST_ENVIRONMENT}")
endif()
