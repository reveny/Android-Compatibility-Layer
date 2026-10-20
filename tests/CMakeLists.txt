set(FIXTURE_ENV ${CMAKE_COMMAND} -E env
    RUNTIME_D8_JAR=${RUNTIME_D8_JAR}
    ANDROID_SDK_ROOT=${RUNTIME_ANDROID_SDK_ROOT})

set(MINIMAL_APK ${CMAKE_CURRENT_BINARY_DIR}/minimal/minimal.apk)
set(WINDOW_APK ${CMAKE_CURRENT_BINARY_DIR}/window/window.apk)

file(GLOB_RECURSE MINIMAL_SOURCES CONFIGURE_DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/minimal/src/*.java)
file(GLOB_RECURSE WINDOW_SOURCES CONFIGURE_DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/window/src/*.java)

add_custom_command(
    OUTPUT ${MINIMAL_APK}
    COMMAND ${FIXTURE_ENV} ${CMAKE_SOURCE_DIR}/tools/build-fixture-apk.sh ${CMAKE_CURRENT_SOURCE_DIR}/minimal ${MINIMAL_APK}
    DEPENDS ${MINIMAL_SOURCES}
            ${CMAKE_CURRENT_SOURCE_DIR}/minimal/AndroidManifest.xml
            ${CMAKE_SOURCE_DIR}/tools/build-fixture-apk.sh
    COMMENT "Building minimal.apk"
    VERBATIM
)

add_custom_command(
    OUTPUT ${WINDOW_APK}
    COMMAND ${FIXTURE_ENV} ${CMAKE_SOURCE_DIR}/tools/build-fixture-apk.sh ${CMAKE_CURRENT_SOURCE_DIR}/window ${WINDOW_APK}
    DEPENDS ${WINDOW_SOURCES}
            ${CMAKE_CURRENT_SOURCE_DIR}/window/AndroidManifest.xml
            ${CMAKE_SOURCE_DIR}/tools/build-fixture-apk.sh
    COMMENT "Building window.apk"
    VERBATIM
)

set(VIEWTRANSFORM_APK ${CMAKE_CURRENT_BINARY_DIR}/viewtransform/viewtransform.apk)
file(GLOB_RECURSE VIEWTRANSFORM_SOURCES CONFIGURE_DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/viewtransform/src/*.java)

add_custom_command(
    OUTPUT ${VIEWTRANSFORM_APK}
    COMMAND ${FIXTURE_ENV} ${CMAKE_SOURCE_DIR}/tools/build-fixture-apk.sh ${CMAKE_CURRENT_SOURCE_DIR}/viewtransform ${VIEWTRANSFORM_APK}
    DEPENDS ${VIEWTRANSFORM_SOURCES}
            ${CMAKE_CURRENT_SOURCE_DIR}/viewtransform/AndroidManifest.xml
            ${CMAKE_SOURCE_DIR}/tools/build-fixture-apk.sh
    COMMENT "Building viewtransform.apk"
    VERBATIM
)

add_custom_target(ViewTransformApk ALL DEPENDS ${VIEWTRANSFORM_APK})

set(STRICT_APK ${CMAKE_CURRENT_BINARY_DIR}/strict/strict.apk)
file(GLOB_RECURSE STRICT_SOURCES CONFIGURE_DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/strict/src/*.java)

add_custom_command(
    OUTPUT ${STRICT_APK}
    COMMAND ${FIXTURE_ENV} ${CMAKE_SOURCE_DIR}/tools/build-fixture-apk.sh ${CMAKE_CURRENT_SOURCE_DIR}/strict ${STRICT_APK}
    DEPENDS ${STRICT_SOURCES}
            ${CMAKE_CURRENT_SOURCE_DIR}/strict/AndroidManifest.xml
            ${CMAKE_SOURCE_DIR}/tools/build-fixture-apk.sh
    COMMENT "Building strict.apk"
    VERBATIM
)

set(SPLIT_APK ${CMAKE_CURRENT_BINARY_DIR}/split/split_config.extra.apk)
file(GLOB_RECURSE SPLIT_RESOURCES CONFIGURE_DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/split/res/* ${CMAKE_CURRENT_SOURCE_DIR}/split/assets/*)

add_custom_command(
    OUTPUT ${SPLIT_APK}
    COMMAND ${FIXTURE_ENV} ${CMAKE_SOURCE_DIR}/tools/build-fixture-apk.sh ${CMAKE_CURRENT_SOURCE_DIR}/split ${SPLIT_APK}
    DEPENDS ${SPLIT_RESOURCES}
            ${CMAKE_CURRENT_SOURCE_DIR}/split/AndroidManifest.xml
            ${CMAKE_SOURCE_DIR}/tools/build-fixture-apk.sh
    COMMENT "Building split_config.extra.apk"
    VERBATIM
)

set(JNI_APK ${CMAKE_CURRENT_BINARY_DIR}/jni/jni.apk)
set(JNI_LIB_STAGE ${CMAKE_CURRENT_BINARY_DIR}/jni/jnilibs/x86_64)
file(GLOB_RECURSE JNI_SOURCES CONFIGURE_DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/jni/src/*.java)
file(GLOB_RECURSE JNI_ASSETS CONFIGURE_DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/jni/assets/*)

add_library(AclJniFixture SHARED ${CMAKE_CURRENT_SOURCE_DIR}/jni/native/AclJni.c)
target_include_directories(AclJniFixture SYSTEM PRIVATE ${RUNTIME_JNI_INCLUDE_DIR})
target_link_libraries(AclJniFixture PRIVATE Threads::Threads)
target_compile_options(AclJniFixture PRIVATE -Wall -Wextra -Werror -fvisibility=hidden -fno-exceptions)
set_target_properties(AclJniFixture PROPERTIES
    OUTPUT_NAME acljni
    PREFIX lib
    LIBRARY_OUTPUT_DIRECTORY ${JNI_LIB_STAGE}
)

add_custom_command(
    OUTPUT ${JNI_APK}
    COMMAND ${CMAKE_COMMAND} -E env FIXTURE_NATIVE_LIBS=${CMAKE_CURRENT_BINARY_DIR}/jni/jnilibs
            RUNTIME_D8_JAR=${RUNTIME_D8_JAR}
            ANDROID_SDK_ROOT=${RUNTIME_ANDROID_SDK_ROOT}
            ${CMAKE_SOURCE_DIR}/tools/build-fixture-apk.sh ${CMAKE_CURRENT_SOURCE_DIR}/jni ${JNI_APK}
    DEPENDS ${JNI_SOURCES}
            ${JNI_ASSETS}
            AclJniFixture
            ${CMAKE_CURRENT_SOURCE_DIR}/jni/AndroidManifest.xml
            ${CMAKE_SOURCE_DIR}/tools/build-fixture-apk.sh
    COMMENT "Building jni.apk"
    VERBATIM
)

add_custom_target(JniApk ALL DEPENDS ${JNI_APK})
set(RUNTIME_JNI_APK ${JNI_APK} CACHE INTERNAL "")

set(SHAPES_APK ${CMAKE_CURRENT_BINARY_DIR}/graphics/graphics.apk)
set(SHAPES_ASSET ${CMAKE_CURRENT_SOURCE_DIR}/graphics/assets/badge.png)
file(GLOB_RECURSE SHAPES_SOURCES CONFIGURE_DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/graphics/src/com/*.java)
file(GLOB_RECURSE SHAPES_RESOURCES CONFIGURE_DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/graphics/res/*)

add_executable(MakeTestPng ${CMAKE_SOURCE_DIR}/tools/make-test-png.c)
target_link_libraries(MakeTestPng PRIVATE PkgConfig::PNG)

add_custom_command(
    OUTPUT ${SHAPES_ASSET}
    COMMAND ${CMAKE_COMMAND} -E make_directory ${CMAKE_CURRENT_SOURCE_DIR}/graphics/assets
    COMMAND MakeTestPng ${SHAPES_ASSET}
    DEPENDS MakeTestPng
    COMMENT "Writing the fixture's test png"
    VERBATIM
)

add_custom_command(
    OUTPUT ${SHAPES_APK}
    COMMAND ${FIXTURE_ENV} ${CMAKE_SOURCE_DIR}/tools/build-fixture-apk.sh ${CMAKE_CURRENT_SOURCE_DIR}/graphics ${SHAPES_APK}
    DEPENDS ${SHAPES_SOURCES}
            ${SHAPES_RESOURCES}
            ${SHAPES_ASSET}
            ${CMAKE_CURRENT_SOURCE_DIR}/graphics/AndroidManifest.xml
            ${CMAKE_SOURCE_DIR}/tools/build-fixture-apk.sh
    COMMENT "Building graphics.apk"
    VERBATIM
)

add_custom_target(MinimalApk ALL DEPENDS ${MINIMAL_APK})
add_custom_target(WindowApk ALL DEPENDS ${WINDOW_APK})
add_custom_target(StrictApk ALL DEPENDS ${STRICT_APK})
add_custom_target(ShapesApk ALL DEPENDS ${SHAPES_APK})
add_custom_target(SplitApk ALL DEPENDS ${SPLIT_APK})

set(RUNTIME_MINIMAL_APK ${MINIMAL_APK} CACHE INTERNAL "")
set(RUNTIME_WINDOW_APK ${WINDOW_APK} CACHE INTERNAL "")
set(RUNTIME_STRICT_APK ${STRICT_APK} CACHE INTERNAL "")
set(RUNTIME_SHAPES_APK ${SHAPES_APK} CACHE INTERNAL "")
set(RUNTIME_SPLIT_APK ${SPLIT_APK} CACHE INTERNAL "")
