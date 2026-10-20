cmake_minimum_required(VERSION 3.28)

if(NOT ACL_FILE OR NOT EXISTS "${ACL_FILE}")
    message(FATAL_ERROR "checksum.cmake needs ACL_FILE to name an existing file")
endif()

file(SHA256 "${ACL_FILE}" digest)
get_filename_component(name "${ACL_FILE}" NAME)

file(WRITE "${ACL_FILE}.sha256" "${digest}  ${name}\n")

message(STATUS "sha256 ${digest}  ${name}")
