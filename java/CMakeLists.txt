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
string(REPLACE ";" "\n" FRAMEWORK_SOURCE_LINES "${FRAMEWORK_SOURCES}")
set(FRAMEWORK_SOURCE_FILE ${CMAKE_CURRENT_BINARY_DIR}/framework-sources.txt)
file(CONFIGURE OUTPUT ${FRAMEWORK_SOURCE_FILE} CONTENT "${FRAMEWORK_SOURCE_LINES}\n" @ONLY)

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
            -DSOURCE_FILE=${FRAMEWORK_SOURCE_FILE}
            -P ${CMAKE_SOURCE_DIR}/tools/build-framework-jar.cmake
    DEPENDS ${FRAMEWORK_SOURCES} ${FRAMEWORK_SOURCE_FILE} ${CMAKE_SOURCE_DIR}/tools/build-framework-jar.cmake
    COMMENT "Building framework.jar"
    VERBATIM
)

add_custom_target(FrameworkJar ALL DEPENDS ${FRAMEWORK_JAR})

set(RUNTIME_FRAMEWORK_JAR ${FRAMEWORK_JAR} CACHE INTERNAL "")

file(GLOB_RECURSE APACHE_HTTP_SOURCES CONFIGURE_DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/libraries/org.apache.http.legacy/*.java)
string(REPLACE ";" "\n" APACHE_HTTP_SOURCE_LINES "${APACHE_HTTP_SOURCES}")
set(APACHE_HTTP_SOURCE_FILE ${CMAKE_CURRENT_BINARY_DIR}/apache-http-sources.txt)
file(CONFIGURE OUTPUT ${APACHE_HTTP_SOURCE_FILE} CONTENT "${APACHE_HTTP_SOURCE_LINES}\n" @ONLY)

set(APACHE_HTTP_JAR ${CMAKE_CURRENT_BINARY_DIR}/org.apache.http.legacy.jar)

add_custom_command(
    OUTPUT ${APACHE_HTTP_JAR}
    COMMAND ${CMAKE_COMMAND}
            -DCLASSES_DIR=${CMAKE_CURRENT_BINARY_DIR}/apache-http-classes
            -DDEX_DIR=${CMAKE_CURRENT_BINARY_DIR}/apache-http-dex
            -DJAR_PATH=${APACHE_HTTP_JAR}
            -DJAVAC=${RUNTIME_JAVAC}
            -DJAVA=${RUNTIME_JAVA}
            -DD8_JAR=${RUNTIME_D8_JAR}
            -DMIN_API=${RUNTIME_DECLARED_SDK_VERSION}
            -DSOURCE_FILE=${APACHE_HTTP_SOURCE_FILE}
            -P ${CMAKE_SOURCE_DIR}/tools/build-framework-jar.cmake
    DEPENDS ${APACHE_HTTP_SOURCES} ${APACHE_HTTP_SOURCE_FILE} ${CMAKE_SOURCE_DIR}/tools/build-framework-jar.cmake
    COMMENT "Building org.apache.http.legacy.jar"
    VERBATIM
)

add_custom_target(ApacheHttpJar ALL DEPENDS ${APACHE_HTTP_JAR})

set(RUNTIME_APACHE_HTTP_JAR ${APACHE_HTTP_JAR} CACHE INTERNAL "")
