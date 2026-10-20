#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "${SCRIPT_DIR}")"

includeDir="${1:-/usr/include}"
output="${2:-${PROJECT_DIR}/src/GlesWsi/GlesEntryPointList.hpp}"

glesHeaders=("${includeDir}/GLES2/gl2.h" "${includeDir}/GLES3/gl3.h" "${includeDir}/GLES3/gl31.h" "${includeDir}/GLES3/gl32.h")
eglHeaders=("${includeDir}/EGL/egl.h")

for header in "${glesHeaders[@]}" "${eglHeaders[@]}"; do
    if [ ! -f "$header" ]; then
        echo "missing header: $header" >&2
        exit 1
    fi
done

names_from_gles()
{
    grep -hE '^GL_APICALL .* GL_APIENTRY [a-zA-Z0-9_]+ *\(' "$@" \
        | sed -E 's/^.*GL_APIENTRY +([a-zA-Z0-9_]+) *\(.*$/\1/'
}

names_from_egl()
{
    grep -hE '^EGLAPI .* EGLAPIENTRY [a-zA-Z0-9_]+ *\(' "$@" \
        | sed -E 's/^.*EGLAPIENTRY +([a-zA-Z0-9_]+) *\(.*$/\1/'
}

interposed="eglGetDisplay|eglGetPlatformDisplay|eglCreateWindowSurface|eglSwapBuffers|eglGetProcAddress|glGetString|glGetStringi|glDebugMessageCallback"

glesNames="$(names_from_gles "${glesHeaders[@]}" | sort -u | grep -vE "^(${interposed})$" || true)"
eglNames="$(names_from_egl "${eglHeaders[@]}" | sort -u | grep -vE "^(${interposed})$" || true)"

glesCount="$(printf '%s\n' "$glesNames" | grep -c . || true)"
eglCount="$(printf '%s\n' "$eglNames" | grep -c . || true)"

if [ "$glesCount" -lt 100 ] || [ "$eglCount" -lt 10 ]; then
    echo "refusing to write a suspiciously short list: $glesCount gl, $eglCount egl" >&2
    exit 1
fi

{
    echo "#pragma once"
    echo
    echo "#define RUNTIME_GLES_ENTRY_POINTS(FORWARD) \\"
    printf '%s\n' "$glesNames" | sed 's/^/    FORWARD(/; s/$/) \\/'
    echo "    /* end */"
    echo
    echo "#define RUNTIME_EGL_ENTRY_POINTS(FORWARD) \\"
    printf '%s\n' "$eglNames" | sed 's/^/    FORWARD(/; s/$/) \\/'
    echo "    /* end */"
} > "$output"

echo "wrote $output: $glesCount gl entry points, $eglCount egl entry points"
