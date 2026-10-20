add_library(icu_preload SHARED IcuPreload.cpp)
target_include_directories(icu_preload PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
target_link_libraries(icu_preload PRIVATE ${CMAKE_DL_LIBS})
target_compile_options(icu_preload PRIVATE -fvisibility=hidden -Werror)
target_link_options(icu_preload PRIVATE -Wl,--as-needed)
set_target_properties(icu_preload PROPERTIES
    OUTPUT_NAME icu_preload
    PREFIX lib
)

set(PROBE_DEX ${CMAKE_BINARY_DIR}/tests/probe/probe.dex)
add_custom_command(
    OUTPUT ${PROBE_DEX}
    COMMAND ${CMAKE_COMMAND} -E env OUTPUT_DEX=${PROBE_DEX}
            RUNTIME_D8_JAR=${RUNTIME_D8_JAR}
            ANDROID_SDK_ROOT=${RUNTIME_ANDROID_SDK_ROOT}
            ${CMAKE_CURRENT_SOURCE_DIR}/../build-probe.sh
    DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/../../tests/probe/src/Probe.java
            ${CMAKE_CURRENT_SOURCE_DIR}/../build-probe.sh
    COMMENT "Building probe.dex"
    VERBATIM
)
add_custom_target(ProbeDex ALL DEPENDS ${PROBE_DEX})

add_test(NAME probe-gate COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/../run-probe-gate.sh ${PROBE_DEX})
set_tests_properties(probe-gate PROPERTIES ENVIRONMENT "BUILD_DIR=${CMAKE_BINARY_DIR};${RUNTIME_TEST_ENVIRONMENT}")
