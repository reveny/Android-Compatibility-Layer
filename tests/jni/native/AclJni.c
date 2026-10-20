#include <jni.h>
#include <pthread.h>
#include <stdio.h>
#include <string.h>

#define ACL_EXPORT __attribute__((visibility("default")))

static JavaVM* gVm = NULL;
static jclass gBridge = NULL;

static jint BoundUpcall(JNIEnv* env, jclass owner, jint value)
{
    jmethodID doubled = (*env)->GetStaticMethodID(env, owner, "doubled", "(I)I");
    if (!doubled) return -1;
    return (*env)->CallStaticIntMethod(env, owner, doubled, value);
}

static jstring BoundStringRoundTrip(JNIEnv* env, jclass owner, jstring value)
{
    const char* incoming = (*env)->GetStringUTFChars(env, value, NULL);
    if (!incoming) return NULL;

    char decorated[256];
    snprintf(decorated, sizeof(decorated), "%s-native", incoming);
    (*env)->ReleaseStringUTFChars(env, value, incoming);

    jstring produced = (*env)->NewStringUTF(env, decorated);
    if (!produced) return NULL;

    jmethodID decorate = (*env)->GetStaticMethodID(env, owner, "decorate", "(Ljava/lang/String;)Ljava/lang/String;");
    if (!decorate) return NULL;

    return (jstring)(*env)->CallStaticObjectMethod(env, owner, decorate, produced);
}

static jint BoundInstanceUpcall(JNIEnv* env, jclass owner, jobject target, jint value)
{
    (void)owner;

    if (!target) return -1;

    jclass targetClass = (*env)->GetObjectClass(env, target);
    if (!targetClass) return -1;

    jmethodID scaled = (*env)->GetMethodID(env, targetClass, "scaled", "(I)I");
    if (!scaled) return -1;

    return (*env)->CallIntMethod(env, target, scaled, value);
}

static jint BoundMutateArray(JNIEnv* env, jclass owner, jbyteArray buffer)
{
    (void)owner;

    jsize length = (*env)->GetArrayLength(env, buffer);
    jbyte* elements = (*env)->GetByteArrayElements(env, buffer, NULL);
    if (!elements) return -1;

    jint sum = 0;
    for (jsize index = 0; index < length; ++index)
    {
        sum += elements[index];
        elements[index] = (jbyte)(elements[index] + 10);
    }

    (*env)->ReleaseByteArrayElements(env, buffer, elements, 0);
    return sum;
}

static jint BoundBumpField(JNIEnv* env, jclass owner, jobject target, jint delta)
{
    (void)owner;

    jclass targetClass = (*env)->GetObjectClass(env, target);
    if (!targetClass) return -1;

    jfieldID seed = (*env)->GetFieldID(env, targetClass, "seed", "I");
    if (!seed) return -1;

    jint previous = (*env)->GetIntField(env, target, seed);
    (*env)->SetIntField(env, target, seed, previous + delta);

    return previous;
}

static jstring BoundCatchJavaException(JNIEnv* env, jclass owner, jint value)
{
    jmethodID refuse = (*env)->GetStaticMethodID(env, owner, "refuse", "(I)V");
    if (!refuse) return NULL;

    (*env)->CallStaticVoidMethod(env, owner, refuse, value);
    if (!(*env)->ExceptionCheck(env)) return (*env)->NewStringUTF(env, "no exception");

    jthrowable thrown = (*env)->ExceptionOccurred(env);
    (*env)->ExceptionClear(env);

    jclass throwableClass = (*env)->GetObjectClass(env, thrown);
    jmethodID getMessage = (*env)->GetMethodID(env, throwableClass, "getMessage", "()Ljava/lang/String;");
    if (!getMessage) return NULL;

    return (jstring)(*env)->CallObjectMethod(env, thrown, getMessage);
}

static jint BoundThrowFromNative(JNIEnv* env, jclass owner, jint value)
{
    (void)owner;

    jclass failure = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (!failure) return -1;

    char message[128];
    snprintf(message, sizeof(message), "native refused %d", (int)value);
    (*env)->ThrowNew(env, failure, message);

    return 0;
}

struct AttachedWork
{
    jint value;
    jint result;
};

static void* AttachedEntry(void* argument)
{
    struct AttachedWork* work = (struct AttachedWork*)argument;
    work->result = -1;

    JNIEnv* env = NULL;
    if ((*gVm)->AttachCurrentThread(gVm, &env, NULL) != JNI_OK) return NULL;

    if (gBridge)
    {
        jmethodID doubled = (*env)->GetStaticMethodID(env, gBridge, "doubled", "(I)I");
        if (doubled) work->result = (*env)->CallStaticIntMethod(env, gBridge, doubled, work->value);
    }

    (*gVm)->DetachCurrentThread(gVm);
    return NULL;
}

static jint BoundAttachedThreadUpcall(JNIEnv* env, jclass owner, jint value)
{
    (void)env;
    (void)owner;

    struct AttachedWork work;
    work.value = value;
    work.result = -1;

    pthread_t thread;
    if (pthread_create(&thread, NULL, AttachedEntry, &work) != 0) return -1;
    pthread_join(thread, NULL);

    return work.result;
}

ACL_EXPORT jint JNICALL Java_com_example_jni_NativeBridge_manglingUpcall(JNIEnv* env, jclass owner, jint value)
{
    return BoundUpcall(env, owner, value) + 1;
}

static const JNINativeMethod BRIDGE_METHODS[] = {
    {"registeredUpcall", "(I)I", (void*)&BoundUpcall},
    {"stringRoundTrip", "(Ljava/lang/String;)Ljava/lang/String;", (void*)&BoundStringRoundTrip},
    {"instanceUpcall", "(Lcom/example/jni/NativeBridge;I)I", (void*)&BoundInstanceUpcall},
    {"mutateArray", "([B)I", (void*)&BoundMutateArray},
    {"bumpField", "(Lcom/example/jni/NativeBridge;I)I", (void*)&BoundBumpField},
    {"catchJavaException", "(I)Ljava/lang/String;", (void*)&BoundCatchJavaException},
    {"throwFromNative", "(I)I", (void*)&BoundThrowFromNative},
    {"attachedThreadUpcall", "(I)I", (void*)&BoundAttachedThreadUpcall},
};

ACL_EXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved)
{
    (void)reserved;

    gVm = vm;

    JNIEnv* env = NULL;
    if ((*vm)->GetEnv(vm, (void**)&env, JNI_VERSION_1_6) != JNI_OK)
    {
        fprintf(stderr, "[acljni] GetEnv failed\n");
        return JNI_ERR;
    }

    jclass bridge = (*env)->FindClass(env, "com/example/jni/NativeBridge");
    if (!bridge)
    {
        fprintf(stderr, "[acljni] NativeBridge not found\n");
        return JNI_ERR;
    }

    int count = (int)(sizeof(BRIDGE_METHODS) / sizeof(BRIDGE_METHODS[0]));
    if ((*env)->RegisterNatives(env, bridge, BRIDGE_METHODS, count) != JNI_OK)
    {
        fprintf(stderr, "[acljni] RegisterNatives failed\n");
        return JNI_ERR;
    }

    gBridge = (*env)->NewGlobalRef(env, bridge);
    if (!gBridge)
    {
        fprintf(stderr, "[acljni] NewGlobalRef failed\n");
        return JNI_ERR;
    }

    fprintf(stderr, "[acljni] JNI_OnLoad registered %d methods and cached a global class reference\n", count);
    fflush(stderr);

    return JNI_VERSION_1_6;
}
