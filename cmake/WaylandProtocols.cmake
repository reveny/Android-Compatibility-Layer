function(runtime_add_wayland_protocol target xmlPath basename)
    set(headerPath ${CMAKE_CURRENT_BINARY_DIR}/wayland-generated/${basename}-client-protocol.h)
    set(sourcePath ${CMAKE_CURRENT_BINARY_DIR}/wayland-generated/${basename}-protocol.c)

    add_custom_command(
        OUTPUT ${headerPath}
        COMMAND ${CMAKE_COMMAND} -E make_directory ${CMAKE_CURRENT_BINARY_DIR}/wayland-generated
        COMMAND ${RUNTIME_WAYLAND_SCANNER} client-header ${xmlPath} ${headerPath}
        DEPENDS ${xmlPath}
        COMMENT "wayland-scanner client-header ${basename}"
        VERBATIM
    )

    add_custom_command(
        OUTPUT ${sourcePath}
        COMMAND ${CMAKE_COMMAND} -E make_directory ${CMAKE_CURRENT_BINARY_DIR}/wayland-generated
        COMMAND ${RUNTIME_WAYLAND_SCANNER} private-code ${xmlPath} ${sourcePath}
        DEPENDS ${xmlPath}
        COMMENT "wayland-scanner private-code ${basename}"
        VERBATIM
    )

    target_sources(${target} PRIVATE ${headerPath} ${sourcePath})
    target_include_directories(${target} PUBLIC ${CMAKE_CURRENT_BINARY_DIR}/wayland-generated)
endfunction()
