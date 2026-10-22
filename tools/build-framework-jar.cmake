if(NOT IS_DIRECTORY "${CLASSES_DIR}")
    file(MAKE_DIRECTORY "${CLASSES_DIR}")
endif()

if(NOT IS_DIRECTORY "${DEX_DIR}")
    file(MAKE_DIRECTORY "${DEX_DIR}")
endif()

file(GLOB_RECURSE STALE_CLASSES "${CLASSES_DIR}/*.class")
if(STALE_CLASSES)
    file(REMOVE ${STALE_CLASSES})
endif()

file(GLOB STALE_DEX "${DEX_DIR}/*.dex")
if(STALE_DEX)
    file(REMOVE ${STALE_DEX})
endif()

if(NOT EXISTS "${SOURCE_FILE}")
    message(FATAL_ERROR "the framework source list ${SOURCE_FILE} is missing")
endif()

set(JAVAC_CLASSPATH_ARGS)
if(EXTRA_CLASSPATH)
    set(JAVAC_CLASSPATH_ARGS -classpath "${EXTRA_CLASSPATH}")
endif()

execute_process(
    COMMAND ${JAVAC} --release 11 -nowarn ${JAVAC_CLASSPATH_ARGS} -d "${CLASSES_DIR}" "@${SOURCE_FILE}"
    RESULT_VARIABLE JAVAC_STATUS
)
if(NOT JAVAC_STATUS EQUAL 0)
    message(FATAL_ERROR "javac failed with status ${JAVAC_STATUS}")
endif()

file(GLOB_RECURSE COMPILED_CLASSES "${CLASSES_DIR}/*.class")
if(NOT COMPILED_CLASSES)
    message(FATAL_ERROR "javac produced no class files in ${CLASSES_DIR}")
endif()

execute_process(
    COMMAND ${JAVA} -cp "${D8_JAR}" com.android.tools.r8.D8
            --release --min-api ${MIN_API}
            --output "${DEX_DIR}"
            ${COMPILED_CLASSES}
    RESULT_VARIABLE D8_STATUS
)
if(NOT D8_STATUS EQUAL 0)
    message(FATAL_ERROR "d8 failed with status ${D8_STATUS}")
endif()

if(NOT EXISTS "${DEX_DIR}/classes.dex")
    message(FATAL_ERROR "d8 produced no classes.dex in ${DEX_DIR}")
endif()

file(REMOVE "${JAR_PATH}")

execute_process(
    COMMAND ${CMAKE_COMMAND} -E tar cf "${JAR_PATH}" --format=zip classes.dex
    WORKING_DIRECTORY "${DEX_DIR}"
    RESULT_VARIABLE ZIP_STATUS
)
if(NOT ZIP_STATUS EQUAL 0)
    message(FATAL_ERROR "packaging ${JAR_PATH} failed with status ${ZIP_STATUS}")
endif()
