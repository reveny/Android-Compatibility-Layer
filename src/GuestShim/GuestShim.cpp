#include <dlfcn.h>
#include <jni.h>
#include <stdio.h>
#include <string.h>

typedef jint (*AclGuestLoader)(const char* soname, JavaVM* vm, void* reserved);

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved)
{
    Dl_info info;
    memset(&info, 0, sizeof(info));

    if (dladdr((void*)&JNI_OnLoad, &info) == 0 || !info.dli_fname)
    {
        fprintf(stderr, "[aclguest] the shim cannot determine its own path, so it cannot tell which guest library it stands for\n");
        return JNI_ERR;
    }

    const char* leaf = strrchr(info.dli_fname, '/');
    leaf = leaf ? leaf + 1 : info.dli_fname;

    AclGuestLoader loader = (AclGuestLoader)dlsym(RTLD_DEFAULT, "AclLoadGuestLibrary");
    if (!loader)
    {
        fprintf(stderr, "[aclguest] %s was loaded outside acl, where there is no guest runtime to hand it to\n", leaf);
        return JNI_ERR;
    }

    return loader(leaf, vm, reserved);
}
