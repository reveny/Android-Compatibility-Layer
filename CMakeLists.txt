cmake_minimum_required(VERSION 3.28)

if(NOT DEFINED CMAKE_C_COMPILER)
    find_program(RUNTIME_CLANG clang REQUIRED)
    set(CMAKE_C_COMPILER ${RUNTIME_CLANG})
endif()
if(NOT DEFINED CMAKE_CXX_COMPILER)
    find_program(RUNTIME_CLANGXX clang++ REQUIRED)
    set(CMAKE_CXX_COMPILER ${RUNTIME_CLANGXX})
endif()

project(acl VERSION 0.1.0 LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE RelWithDebInfo CACHE STRING "" FORCE)
endif()

add_compile_options(-Wall -Wextra -Wpedantic)

option(RUNTIME_HARDENED "Build the runtime with exploit mitigations" ON)

if(RUNTIME_HARDENED)
    add_compile_options(
        -fstack-protector-strong
        -fstack-clash-protection
        $<$<NOT:$<CONFIG:Debug>>:-D_FORTIFY_SOURCE=3>)
    add_link_options(-Wl,-z,relro -Wl,-z,now -Wl,-z,noexecstack -Wl,-z,separate-code)
    set(CMAKE_POSITION_INDEPENDENT_CODE ON)
endif()

if(EXISTS "${CMAKE_SOURCE_DIR}/.toolchain/aosp/master-art")
    set(RUNTIME_DEFAULT_AOSP_DIR "${CMAKE_SOURCE_DIR}/.toolchain/aosp/master-art")
else()
    set(RUNTIME_DEFAULT_AOSP_DIR "$ENV{HOME}/aosp/master-art")
endif()

set(RUNTIME_AOSP_DIR "${RUNTIME_DEFAULT_AOSP_DIR}" CACHE PATH "AOSP master-art checkout")

if(EXISTS "${CMAKE_SOURCE_DIR}/.toolchain/android-sdk")
    set(RUNTIME_DEFAULT_SDK_DIR "${CMAKE_SOURCE_DIR}/.toolchain/android-sdk")
elseif(DEFINED ENV{ANDROID_SDK_ROOT})
    set(RUNTIME_DEFAULT_SDK_DIR "$ENV{ANDROID_SDK_ROOT}")
else()
    set(RUNTIME_DEFAULT_SDK_DIR "$ENV{HOME}/Android/Sdk")
endif()

set(RUNTIME_ANDROID_SDK_ROOT "${RUNTIME_DEFAULT_SDK_DIR}" CACHE PATH "Android SDK used to build the test fixtures")

set(RUNTIME_TEST_ENVIRONMENT
    "AOSP_DIR=${RUNTIME_AOSP_DIR}"
    "RUNTIME_HOST_ROOT=${RUNTIME_HOST_OUT}"
    "ANDROID_SDK_ROOT=${RUNTIME_ANDROID_SDK_ROOT}"
    "RUNTIME_FRAMEWORK_RES=${RUNTIME_ANDROID_SDK_ROOT}/platforms/android-${RUNTIME_DECLARED_SDK_VERSION}/android.jar")
set(RUNTIME_HOST_OUT "${RUNTIME_AOSP_DIR}/out/host/linux-x86" CACHE PATH "AOSP host output directory")

if(NOT EXISTS "${RUNTIME_HOST_OUT}/lib64/libartbase.so" OR NOT EXISTS "${RUNTIME_HOST_OUT}/framework/d8.jar")
    message(FATAL_ERROR
        "No host ART build at ${RUNTIME_HOST_OUT}.\n"
        "third_party/androidfw links its lib64, and java/ needs its framework/d8.jar, so the build "
        "cannot proceed without it.\n"
        "Run tools/setup-host.sh to fetch and build one into .toolchain/, or point RUNTIME_AOSP_DIR "
        "at an existing checkout.")
endif()
set(RUNTIME_DECLARED_SDK_VERSION 34 CACHE STRING "Frozen Android platform level the runtime declares")

set(RUNTIME_DECLARED_RELEASE "14" CACHE STRING "Android release string reported by Build.VERSION.RELEASE")

set(RUNTIME_DEVICE_BRAND "acl" CACHE STRING "Build.BRAND")
set(RUNTIME_DEVICE_MANUFACTURER "ACL" CACHE STRING "Build.MANUFACTURER")
set(RUNTIME_DEVICE_MODEL "ACL-A1" CACHE STRING "Build.MODEL")
set(RUNTIME_DEVICE_NAME "acl_arm64" CACHE STRING "Build.DEVICE, the codename")
set(RUNTIME_DEVICE_PRODUCT "acl_arm64" CACHE STRING "Build.PRODUCT")
set(RUNTIME_DEVICE_BOARD "acl" CACHE STRING "Build.BOARD")
set(RUNTIME_DEVICE_HARDWARE "acl" CACHE STRING "Build.HARDWARE")
set(RUNTIME_DEVICE_SOC_MANUFACTURER "ACL" CACHE STRING "Build.SOC_MANUFACTURER")
set(RUNTIME_DEVICE_SOC_MODEL "ACL8000" CACHE STRING "Build.SOC_MODEL")
set(RUNTIME_BUILD_ID "AG1A.250824.001" CACHE STRING "Build.ID, in the platform's four-character dotted form")
set(RUNTIME_BUILD_INCREMENTAL "250824001" CACHE STRING "Build.VERSION.INCREMENTAL")
set(RUNTIME_BUILD_TYPE "user" CACHE STRING "Build.TYPE")
set(RUNTIME_BUILD_TAGS "release-keys" CACHE STRING "Build.TAGS")
set(RUNTIME_BUILD_HOST "acl-build" CACHE STRING "Build.HOST")
set(RUNTIME_BUILD_USER "acl" CACHE STRING "Build.USER")
set(RUNTIME_SECURITY_PATCH "2025-08-01" CACHE STRING "Build.VERSION.SECURITY_PATCH, which must be YYYY-MM-DD")
set(RUNTIME_DEVICE_MEMORY_MB 12288 CACHE STRING "Megabytes of memory the guest is told the device has")
set(RUNTIME_DEVICE_GPU "ACL Graphics" CACHE STRING "Renderer name the guest is told it is drawing with")
set(RUNTIME_DEVICE_GPU_VENDOR "ACL" CACHE STRING "Vendor name the guest is told its renderer comes from")
set(RUNTIME_DECLARED_GL_ES_VERSION "3.2" CACHE STRING "OpenGL ES version the guest is told the device supports")
set(RUNTIME_NDK_API_LEVEL ${RUNTIME_DECLARED_SDK_VERSION} CACHE STRING "NDK API level for guest-side cross-compilation")

set(RUNTIME_LLVM_MINIMUM_VERSION 20)
find_package(LLVM REQUIRED CONFIG HINTS /usr/lib/cmake/llvm)
if(LLVM_VERSION_MAJOR LESS RUNTIME_LLVM_MINIMUM_VERSION)
    message(FATAL_ERROR "LLVM ${LLVM_PACKAGE_VERSION} found; ${RUNTIME_LLVM_MINIMUM_VERSION} or newer required")
endif()
message(STATUS "LLVM ${LLVM_PACKAGE_VERSION} at ${LLVM_DIR}")

option(RUNTIME_ENABLE_GRAPHICS "Build the Wayland client and put a window on screen" ON)
option(RUNTIME_ENABLE_VULKAN "Forward the guest's Vulkan calls to the host driver — the M9x WSI path" ON)
option(RUNTIME_ENABLE_GLES "Forward the guest's OpenGL ES calls to the host driver — the V7 path" ON)
option(RUNTIME_ENABLE_DIRECT_BUFFERS "Require GBM/DRM for guest-allocated window buffers — the M11 path" OFF)
option(RUNTIME_ENABLE_AUDIO "Require PipeWire — the AAudio output path" ON)
option(RUNTIME_ENABLE_INPUT "Require libinput" OFF)

find_package(PkgConfig REQUIRED)
find_package(Threads REQUIRED)
pkg_check_modules(ZLIB REQUIRED IMPORTED_TARGET zlib)
pkg_check_modules(SQLITE3 REQUIRED IMPORTED_TARGET sqlite3)
pkg_check_modules(FREETYPE REQUIRED IMPORTED_TARGET freetype2)
pkg_check_modules(HARFBUZZ REQUIRED IMPORTED_TARGET harfbuzz)
pkg_check_modules(PNG REQUIRED IMPORTED_TARGET libpng)
pkg_check_modules(JPEG REQUIRED IMPORTED_TARGET libjpeg)
pkg_check_modules(WEBP REQUIRED IMPORTED_TARGET libwebp)

set(RUNTIME_FONT_DIR "${CMAKE_SOURCE_DIR}/third_party/roboto" CACHE PATH "Directory holding the vendored fonts")

if(NOT EXISTS "${RUNTIME_FONT_DIR}/Roboto-Regular.ttf")
    message(FATAL_ERROR "RUNTIME_FONT_DIR ${RUNTIME_FONT_DIR} does not contain Roboto-Regular.ttf")
endif()

pkg_get_variable(RUNTIME_HARFBUZZ_LIBDIR harfbuzz libdir)
find_library(RUNTIME_HARFBUZZ_LIBRARY NAMES harfbuzz HINTS ${RUNTIME_HARFBUZZ_LIBDIR} REQUIRED)
execute_process(
    COMMAND ${CMAKE_COMMAND} -E env objdump -p ${RUNTIME_HARFBUZZ_LIBRARY}
    OUTPUT_VARIABLE RUNTIME_HARFBUZZ_NEEDED
    ERROR_QUIET
)
if(RUNTIME_HARFBUZZ_NEEDED MATCHES "NEEDED[ \t]+libicu")
    message(FATAL_ERROR
        "harfbuzz at ${RUNTIME_HARFBUZZ_LIBRARY} links libicu. "
        "This process already dlopens AOSP ICU 68 RTLD_GLOBAL before JNI_CreateJavaVM; "
        "a second ICU is a hazard. Link plain harfbuzz, never harfbuzz-icu.")
endif()

message(STATUS "freetype ${FREETYPE_VERSION}, harfbuzz ${HARFBUZZ_VERSION}, fonts ${RUNTIME_FONT_DIR}")

if(RUNTIME_ENABLE_GRAPHICS)
    pkg_check_modules(WAYLAND_CLIENT REQUIRED IMPORTED_TARGET wayland-client)
    pkg_check_modules(XKBCOMMON REQUIRED IMPORTED_TARGET xkbcommon)

    pkg_get_variable(RUNTIME_WAYLAND_SCANNER wayland-scanner wayland_scanner)
    if(NOT RUNTIME_WAYLAND_SCANNER)
        find_program(RUNTIME_WAYLAND_SCANNER wayland-scanner REQUIRED)
    endif()

    pkg_get_variable(RUNTIME_WAYLAND_PROTOCOLS_DIR wayland-protocols pkgdatadir)
    if(NOT RUNTIME_WAYLAND_PROTOCOLS_DIR)
        message(FATAL_ERROR "wayland-protocols pkgdatadir not found")
    endif()
    cmake_path(NORMAL_PATH RUNTIME_WAYLAND_PROTOCOLS_DIR)

    message(STATUS "wayland-scanner ${RUNTIME_WAYLAND_SCANNER}, protocols ${RUNTIME_WAYLAND_PROTOCOLS_DIR}")
    include(${CMAKE_SOURCE_DIR}/cmake/WaylandProtocols.cmake)
endif()

if(RUNTIME_ENABLE_VULKAN)
    find_package(Vulkan REQUIRED)
endif()

if(RUNTIME_ENABLE_GLES)
    pkg_check_modules(EGL REQUIRED egl)
    pkg_check_modules(GLESV2 REQUIRED glesv2)
    pkg_check_modules(WAYLAND_EGL REQUIRED wayland-egl)
    message(STATUS "EGL ${EGL_VERSION}, GLES ${GLESV2_VERSION}, wayland-egl ${WAYLAND_EGL_VERSION}, headers only and dlopened at runtime")
endif()

if(RUNTIME_ENABLE_DIRECT_BUFFERS)
    pkg_check_modules(GBM REQUIRED IMPORTED_TARGET gbm)
    pkg_check_modules(LIBDRM REQUIRED IMPORTED_TARGET libdrm)
endif()

if(RUNTIME_ENABLE_AUDIO)
    pkg_check_modules(PIPEWIRE REQUIRED IMPORTED_TARGET libpipewire-0.3)
endif()

if(RUNTIME_ENABLE_INPUT)
    pkg_check_modules(LIBINPUT REQUIRED IMPORTED_TARGET libinput)
endif()

enable_testing()

add_subdirectory(third_party)
add_subdirectory(java)
add_subdirectory(src)
add_subdirectory(tests)
add_subdirectory(tools/icu-preload)

include(tools/dist.cmake)
