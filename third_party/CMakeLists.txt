set(AOSP_HOST_LIB ${RUNTIME_HOST_OUT}/lib64)

foreach(lib base log utils cutils ziparchive)
    add_library(aosp_${lib} SHARED IMPORTED GLOBAL)
    set_target_properties(aosp_${lib} PROPERTIES IMPORTED_LOCATION ${AOSP_HOST_LIB}/lib${lib}.so)
endforeach()

set(ANDROIDFW_DIR ${CMAKE_CURRENT_SOURCE_DIR}/androidfw)

add_library(androidfw STATIC
    ${ANDROIDFW_DIR}/ApkAssets.cpp
    ${ANDROIDFW_DIR}/ApkParsing.cpp
    ${ANDROIDFW_DIR}/Asset.cpp
    ${ANDROIDFW_DIR}/AssetDir.cpp
    ${ANDROIDFW_DIR}/AssetManager.cpp
    ${ANDROIDFW_DIR}/AssetManager2.cpp
    ${ANDROIDFW_DIR}/AssetsProvider.cpp
    ${ANDROIDFW_DIR}/AttributeResolution.cpp
    ${ANDROIDFW_DIR}/BigBuffer.cpp
    ${ANDROIDFW_DIR}/BigBufferStream.cpp
    ${ANDROIDFW_DIR}/ChunkIterator.cpp
    ${ANDROIDFW_DIR}/ConfigDescription.cpp
    ${ANDROIDFW_DIR}/FileStream.cpp
    ${ANDROIDFW_DIR}/Idmap.cpp
    ${ANDROIDFW_DIR}/LoadedArsc.cpp
    ${ANDROIDFW_DIR}/Locale.cpp
    ${ANDROIDFW_DIR}/LocaleData.cpp
    ${ANDROIDFW_DIR}/LocaleDataLookup.cpp
    ${ANDROIDFW_DIR}/misc.cpp
    ${ANDROIDFW_DIR}/NinePatch.cpp
    ${ANDROIDFW_DIR}/ObbFile.cpp
    ${ANDROIDFW_DIR}/PathUtils.cpp
    ${ANDROIDFW_DIR}/PngChunkFilter.cpp
    ${ANDROIDFW_DIR}/PosixUtils.cpp
    ${ANDROIDFW_DIR}/ResourceTimer.cpp
    ${ANDROIDFW_DIR}/ResourceTypes.cpp
    ${ANDROIDFW_DIR}/ResourceUtils.cpp
    ${ANDROIDFW_DIR}/StreamingZipInflater.cpp
    ${ANDROIDFW_DIR}/StringPool.cpp
    ${ANDROIDFW_DIR}/TypeWrappers.cpp
    ${ANDROIDFW_DIR}/Util.cpp
    ${ANDROIDFW_DIR}/ZipFileRO.cpp
    ${ANDROIDFW_DIR}/ZipUtils.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/incfs/util/map_ptr.cpp
)

target_include_directories(androidfw SYSTEM PUBLIC
    ${ANDROIDFW_DIR}/include
    ${ANDROIDFW_DIR}/include_pathutils
    ${CMAKE_CURRENT_SOURCE_DIR}/incfs/util/include
    ${CMAKE_CURRENT_SOURCE_DIR}/native/include
    ${RUNTIME_AOSP_DIR}/system/libbase/include
    ${RUNTIME_AOSP_DIR}/system/logging/liblog/include
    ${RUNTIME_AOSP_DIR}/system/core/libutils/include
    ${RUNTIME_AOSP_DIR}/system/core/libcutils/include
    ${RUNTIME_AOSP_DIR}/system/core/libsystem/include
    ${RUNTIME_AOSP_DIR}/system/libziparchive/include
    ${RUNTIME_AOSP_DIR}/external/fmtlib/include
)

add_library(aosp_cxx SHARED IMPORTED GLOBAL)
set_target_properties(aosp_cxx PROPERTIES IMPORTED_LOCATION ${AOSP_HOST_LIB}/libc++.so)

set(AOSP_CLANG_DIR ${RUNTIME_AOSP_DIR}/prebuilts/clang/host/linux-x86/clang-r547379
    CACHE PATH "AOSP clang prebuilt, source of the libc++ the AOSP host libs were built against")

target_link_libraries(androidfw PUBLIC aosp_base aosp_log aosp_utils aosp_cutils aosp_ziparchive aosp_cxx PkgConfig::ZLIB)

target_compile_options(androidfw PRIVATE
    -std=gnu++2b
    -Wno-everything
    -nostdinc++
    -fno-rtti
    "SHELL:-isystem ${AOSP_CLANG_DIR}/include/c++/v1"
    "SHELL:-include ${CMAKE_CURRENT_SOURCE_DIR}/androidfw_prelude.h"
)
target_link_options(androidfw PRIVATE -nostdlib++)

set_target_properties(androidfw PROPERTIES
    POSITION_INDEPENDENT_CODE ON
    CXX_SCAN_FOR_MODULES OFF
)

add_library(androidfw_c STATIC ${CMAKE_CURRENT_SOURCE_DIR}/androidfw_c/AndroidfwC.cpp)
target_include_directories(androidfw_c PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/androidfw_c)
target_link_libraries(androidfw_c PRIVATE androidfw)
target_compile_options(androidfw_c PRIVATE
    -std=gnu++2b
    -Wall -Wextra -Werror
    -nostdinc++
    -fno-rtti
    "SHELL:-isystem ${AOSP_CLANG_DIR}/include/c++/v1"
    "SHELL:-include ${CMAKE_CURRENT_SOURCE_DIR}/androidfw_prelude.h"
)
set_target_properties(androidfw_c PROPERTIES
    POSITION_INDEPENDENT_CODE ON
    CXX_SCAN_FOR_MODULES OFF
)
