configure_file(
    ${CMAKE_CURRENT_SOURCE_DIR}/runtime/PlatformConfig.java.in
    ${CMAKE_CURRENT_BINARY_DIR}/gen/runtime/PlatformConfig.java
    @ONLY
)

file(GLOB_RECURSE FRAMEWORK_SOURCES CONFIGURE_DEPENDS
    ${CMAKE_CURRENT_SOURCE_DIR}/android/*.java
    ${CMAKE_CURRENT_SOURCE_DIR}/javax/*.java
    ${CMAKE_CURRENT_SOURCE_DIR}/org/*.java
    ${CMAKE_CURRENT_SOURCE_DIR}/runtime/*.java)
list(APPEND FRAMEWORK_SOURCES ${CMAKE_CURRENT_BINARY_DIR}/gen/runtime/PlatformConfig.java)
string(REPLACE ";" "|" FRAMEWORK_SOURCE_LIST "${FRAMEWORK_SOURCES}")

set(FRAMEWORK_CLASSES ${CMAKE_CURRENT_BINARY_DIR}/classes)
set(FRAMEWORK_DEX ${CMAKE_CURRENT_BINARY_DIR}/dex)
set(FRAMEWORK_JAR ${CMAKE_CURRENT_BINARY_DIR}/framework.jar)

find_program(RUNTIME_JAVAC javac REQUIRED)
find_program(RUNTIME_JAVA java REQUIRED)

set(RUNTIME_D8_JAR ${RUNTIME_HOST_OUT}/framework/d8.jar CACHE FILEPATH "d8 compiler jar")

add_custom_command(
    OUTPUT ${FRAMEWORK_JAR}
    COMMAND ${CMAKE_COMMAND}
            -DCLASSES_DIR=${FRAMEWORK_CLASSES}
            -DDEX_DIR=${FRAMEWORK_DEX}
            -DJAR_PATH=${FRAMEWORK_JAR}
            -DJAVAC=${RUNTIME_JAVAC}
            -DJAVA=${RUNTIME_JAVA}
            -DD8_JAR=${RUNTIME_D8_JAR}
            -DMIN_API=${RUNTIME_DECLARED_SDK_VERSION}
            "-DSOURCE_LIST=${FRAMEWORK_SOURCE_LIST}"
            -P ${CMAKE_SOURCE_DIR}/tools/build-framework-jar.cmake
    DEPENDS ${FRAMEWORK_SOURCES} ${CMAKE_SOURCE_DIR}/tools/build-framework-jar.cmake
    COMMENT "Building framework.jar"
    VERBATIM
)

add_custom_target(FrameworkJar ALL DEPENDS ${FRAMEWORK_JAR})

set(RUNTIME_FRAMEWORK_JAR ${FRAMEWORK_JAR} CACHE INTERNAL "")
