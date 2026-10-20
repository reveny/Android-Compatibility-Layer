set(ACL_DIST_NAME "acl-${PROJECT_VERSION}-linux-x86_64")
set(ACL_DIST_DIR "${CMAKE_BINARY_DIR}/dist/${ACL_DIST_NAME}")

set(ACL_DIST_HOST_ROOT "")

foreach(candidate "${RUNTIME_HOST_ROOT}" "${CMAKE_SOURCE_DIR}/.toolchain/aosp/master-art/out/host/linux-x86")
    if(candidate AND EXISTS "${candidate}/lib64/libart.so")
        set(ACL_DIST_HOST_ROOT "${candidate}")
        break()
    endif()
endforeach()

set(ACL_DIST_FRAMEWORK_RES "")

foreach(candidate "${RUNTIME_FRAMEWORK_RES}"
                  "${CMAKE_SOURCE_DIR}/.toolchain/android-sdk/platforms/android-${RUNTIME_DECLARED_SDK_VERSION}/android.jar")
    if(candidate AND EXISTS "${candidate}")
        set(ACL_DIST_FRAMEWORK_RES "${candidate}")
        break()
    endif()
endforeach()

find_program(ACL_DIST_LINKER ld.lld)

add_custom_target(dist
    COMMAND ${CMAKE_COMMAND}
            -DACL_DIST_DIR=${ACL_DIST_DIR}
            -DACL_SOURCE_DIR=${CMAKE_SOURCE_DIR}
            -DACL_VERSION=${PROJECT_VERSION}
            -DACL_BINARY=$<TARGET_FILE:acl>
            -DACL_GUEST_SHIM=$<TARGET_FILE:aclguest>
            -DACL_FRAMEWORK_JAR=${CMAKE_BINARY_DIR}/java/framework.jar
            -DACL_FRAMEWORK_RES=${ACL_DIST_FRAMEWORK_RES}
            -DACL_HOST_ROOT=${ACL_DIST_HOST_ROOT}
            -DACL_FONT_DIR=${RUNTIME_FONT_DIR}
            -DACL_LINKER=${ACL_DIST_LINKER}
            -P ${CMAKE_SOURCE_DIR}/tools/assemble-dist.cmake
    DEPENDS acl aclguest FrameworkJar
    COMMENT "Assembling ${ACL_DIST_NAME}"
    VERBATIM
)

add_custom_target(bundle
    COMMAND ${CMAKE_COMMAND} -E tar czf "${ACL_DIST_NAME}.tar.gz" "${ACL_DIST_NAME}"
    COMMAND ${CMAKE_COMMAND} -DACL_FILE=${CMAKE_BINARY_DIR}/dist/${ACL_DIST_NAME}.tar.gz
            -P ${CMAKE_SOURCE_DIR}/tools/checksum.cmake
    WORKING_DIRECTORY "${CMAKE_BINARY_DIR}/dist"
    DEPENDS dist
    COMMENT "Packing ${ACL_DIST_NAME}.tar.gz"
    VERBATIM
)
