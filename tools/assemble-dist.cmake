cmake_minimum_required(VERSION 3.28)

if(NOT ACL_DIST_DIR OR NOT ACL_BINARY OR NOT ACL_HOST_ROOT)
    message(FATAL_ERROR "assemble-dist.cmake needs ACL_DIST_DIR, ACL_BINARY and ACL_HOST_ROOT")
endif()

set(RUNTIME_JARS
    core-oj-hostdex.jar
    core-libart-hostdex.jar
    okhttp-hostdex.jar
    bouncycastle-hostdex.jar
    apache-xml-hostdex.jar
    conscrypt-hostdex.jar
    core-icu4j-hostdex.jar
)

set(SYSTEM_LIBRARY_EXCLUDES
    "ld-linux"
    "linux-vdso"
    "libc.so"
    "libm.so"
    "libdl.so"
    "libpthread.so"
    "librt.so"
    "libresolv.so"
    "libnsl.so"
    "libutil.so"
    "libvulkan"
    "libEGL"
    "libGL"
    "libgbm"
    "libdrm"
)

function(acl_copy_file source destination)
    if(NOT EXISTS "${source}")
        message(FATAL_ERROR "the build did not produce ${source}")
    endif()

    file(COPY "${source}" DESTINATION "${destination}" USE_SOURCE_PERMISSIONS)
endfunction()

file(MAKE_DIRECTORY "${ACL_DIST_DIR}/bin")
file(MAKE_DIRECTORY "${ACL_DIST_DIR}/lib")
file(MAKE_DIRECTORY "${ACL_DIST_DIR}/java")
file(MAKE_DIRECTORY "${ACL_DIST_DIR}/fonts")
file(MAKE_DIRECTORY "${ACL_DIST_DIR}/art/framework")

acl_copy_file("${ACL_BINARY}" "${ACL_DIST_DIR}/bin")
acl_copy_file("${ACL_GUEST_SHIM}" "${ACL_DIST_DIR}/bin")
acl_copy_file("${ACL_FRAMEWORK_JAR}" "${ACL_DIST_DIR}/java")

file(GLOB FONT_FILES "${ACL_FONT_DIR}/*.ttf")
if(NOT FONT_FILES)
    message(FATAL_ERROR "no fonts found under ${ACL_FONT_DIR}")
endif()
file(COPY ${FONT_FILES} DESTINATION "${ACL_DIST_DIR}/fonts")

if(ACL_FRAMEWORK_RES AND EXISTS "${ACL_FRAMEWORK_RES}")
    file(MAKE_DIRECTORY "${ACL_DIST_DIR}/android")
    file(COPY "${ACL_FRAMEWORK_RES}" DESTINATION "${ACL_DIST_DIR}/android")
    get_filename_component(RESOURCE_NAME "${ACL_FRAMEWORK_RES}" NAME)
    if(NOT RESOURCE_NAME STREQUAL "android.jar")
        file(RENAME "${ACL_DIST_DIR}/android/${RESOURCE_NAME}" "${ACL_DIST_DIR}/android/android.jar")
    endif()
else()
    message(WARNING "no framework resource archive was found, so the bundle cannot read layout attributes")
endif()

file(GLOB ART_LIBRARIES "${ACL_HOST_ROOT}/lib64/*.so")
if(NOT ART_LIBRARIES)
    message(FATAL_ERROR "no runtime libraries under ${ACL_HOST_ROOT}/lib64")
endif()
file(COPY ${ART_LIBRARIES} DESTINATION "${ACL_DIST_DIR}/art/lib64")

foreach(jar ${RUNTIME_JARS})
    acl_copy_file("${ACL_HOST_ROOT}/framework/${jar}" "${ACL_DIST_DIR}/art/framework")
endforeach()

foreach(module com.android.i18n com.android.tzdata)
    if(EXISTS "${ACL_HOST_ROOT}/${module}")
        file(COPY "${ACL_HOST_ROOT}/${module}" DESTINATION "${ACL_DIST_DIR}/art")
    endif()
endforeach()

if(ACL_LINKER AND EXISTS "${ACL_LINKER}")
    file(COPY "${ACL_LINKER}" DESTINATION "${ACL_DIST_DIR}/bin" USE_SOURCE_PERMISSIONS)
endif()

function(acl_resolved_libraries binary output)
    execute_process(COMMAND ldd "${binary}" OUTPUT_VARIABLE listing ERROR_QUIET RESULT_VARIABLE status)

    if(NOT status EQUAL 0)
        set(${output} "" PARENT_SCOPE)
        return()
    endif()

    string(REPLACE "\n" ";" lines "${listing}")
    set(found "")

    foreach(line ${lines})
        if(NOT line MATCHES "=> +(/[^ ]+)")
            continue()
        endif()

        set(resolved "${CMAKE_MATCH_1}")
        set(skip FALSE)

        foreach(pattern ${SYSTEM_LIBRARY_EXCLUDES})
            if(resolved MATCHES "${pattern}")
                set(skip TRUE)
                break()
            endif()
        endforeach()

        get_filename_component(real "${resolved}" REALPATH)
        if(real MATCHES "^${ACL_HOST_ROOT}")
            set(skip TRUE)
        endif()

        if(NOT skip)
            list(APPEND found "${resolved}")
        endif()
    endforeach()

    set(${output} "${found}" PARENT_SCOPE)
endfunction()

acl_resolved_libraries("${ACL_BINARY}" BINARY_LIBRARIES)
set(BUNDLED_LIBRARIES ${BINARY_LIBRARIES})

if(ACL_LINKER AND EXISTS "${ACL_LINKER}")
    acl_resolved_libraries("${ACL_LINKER}" LINKER_LIBRARIES)
    list(APPEND BUNDLED_LIBRARIES ${LINKER_LIBRARIES})
endif()

list(REMOVE_DUPLICATES BUNDLED_LIBRARIES)

foreach(library ${BUNDLED_LIBRARIES})
    get_filename_component(name "${library}" NAME)
    get_filename_component(real "${library}" REALPATH)
    file(COPY "${real}" DESTINATION "${ACL_DIST_DIR}/lib")
    get_filename_component(realName "${real}" NAME)

    if(NOT realName STREQUAL name)
        file(CREATE_LINK "${realName}" "${ACL_DIST_DIR}/lib/${name}" SYMBOLIC)
    endif()
endforeach()

list(LENGTH BUNDLED_LIBRARIES LIBRARY_COUNT)

foreach(document LICENSE NOTICE README.md)
    if(EXISTS "${ACL_SOURCE_DIR}/${document}")
        file(COPY "${ACL_SOURCE_DIR}/${document}" DESTINATION "${ACL_DIST_DIR}")
    endif()
endforeach()

file(WRITE "${ACL_DIST_DIR}/VERSION" "${ACL_VERSION}\n")

message(STATUS "bundle at ${ACL_DIST_DIR}: ${LIBRARY_COUNT} host librar(ies), ${ACL_HOST_ROOT} runtime")
