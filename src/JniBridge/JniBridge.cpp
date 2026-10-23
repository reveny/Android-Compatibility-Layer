#include "JniBridge/JniBridge.hpp"

#include <atomic>
#include <cstddef>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <deque>
#include <dlfcn.h>
#include <map>
#include <mutex>
#include <unordered_map>
#include <utility>
#include <vector>

#include "Diagnostics/Diagnostics.hpp"
#include "LlvmBackend/LlvmBackend.hpp"
#include "Settings/Settings.hpp"

static constexpr int GUEST_INTEGER_ARGUMENT_SLOTS = 8;
static constexpr int GUEST_VECTOR_ARGUMENT_SLOTS = 8;
static constexpr uint64_t GUEST_POINTER_BYTES = 8;
static constexpr uint64_t JNI_NATIVE_METHOD_BYTES = 24;
static constexpr uint64_t MAX_GUEST_STRING_BYTES = 65536;
static constexpr int MAX_REGISTERED_NATIVES = 4096;

static constexpr int SLOT_GET_VERSION = (int)(offsetof(JNINativeInterface, GetVersion) / sizeof(void*));
static constexpr int SLOT_FIND_CLASS = (int)(offsetof(JNINativeInterface, FindClass) / sizeof(void*));
static constexpr int SLOT_GET_OBJECT_CLASS = (int)(offsetof(JNINativeInterface, GetObjectClass) / sizeof(void*));
static constexpr int SLOT_EXCEPTION_OCCURRED = (int)(offsetof(JNINativeInterface, ExceptionOccurred) / sizeof(void*));
static constexpr int SLOT_EXCEPTION_DESCRIBE = (int)(offsetof(JNINativeInterface, ExceptionDescribe) / sizeof(void*));
static constexpr int SLOT_EXCEPTION_CLEAR = (int)(offsetof(JNINativeInterface, ExceptionClear) / sizeof(void*));
static constexpr int SLOT_EXCEPTION_CHECK = (int)(offsetof(JNINativeInterface, ExceptionCheck) / sizeof(void*));
static constexpr int SLOT_NEW_GLOBAL_REF = (int)(offsetof(JNINativeInterface, NewGlobalRef) / sizeof(void*));
static constexpr int SLOT_DELETE_GLOBAL_REF = (int)(offsetof(JNINativeInterface, DeleteGlobalRef) / sizeof(void*));
static constexpr int SLOT_DELETE_LOCAL_REF = (int)(offsetof(JNINativeInterface, DeleteLocalRef) / sizeof(void*));
static constexpr int SLOT_GET_METHOD_ID = (int)(offsetof(JNINativeInterface, GetMethodID) / sizeof(void*));
static constexpr int SLOT_GET_STATIC_METHOD_ID = (int)(offsetof(JNINativeInterface, GetStaticMethodID) / sizeof(void*));
static constexpr int SLOT_NEW_STRING_UTF = (int)(offsetof(JNINativeInterface, NewStringUTF) / sizeof(void*));
static constexpr int SLOT_GET_STRING_UTF_LENGTH = (int)(offsetof(JNINativeInterface, GetStringUTFLength) / sizeof(void*));
static constexpr int SLOT_GET_STRING_UTF_CHARS = (int)(offsetof(JNINativeInterface, GetStringUTFChars) / sizeof(void*));
static constexpr int SLOT_RELEASE_STRING_UTF_CHARS = (int)(offsetof(JNINativeInterface, ReleaseStringUTFChars) / sizeof(void*));
static constexpr int SLOT_REGISTER_NATIVES = (int)(offsetof(JNINativeInterface, RegisterNatives) / sizeof(void*));
static constexpr int SLOT_GET_JAVA_VM = (int)(offsetof(JNINativeInterface, GetJavaVM) / sizeof(void*));
static constexpr int SLOT_NEW_OBJECT = (int)(offsetof(JNINativeInterface, NewObject) / sizeof(void*));
static constexpr int SLOT_ALLOC_OBJECT = (int)(offsetof(JNINativeInterface, AllocObject) / sizeof(void*));
static constexpr int SLOT_GET_FIELD_ID = (int)(offsetof(JNINativeInterface, GetFieldID) / sizeof(void*));
static constexpr int SLOT_GET_STATIC_FIELD_ID = (int)(offsetof(JNINativeInterface, GetStaticFieldID) / sizeof(void*));
static constexpr int SLOT_GET_OBJECT_FIELD = (int)(offsetof(JNINativeInterface, GetObjectField) / sizeof(void*));
static constexpr int SLOT_SET_OBJECT_FIELD = (int)(offsetof(JNINativeInterface, SetObjectField) / sizeof(void*));
static constexpr int SLOT_GET_STATIC_OBJECT_FIELD = (int)(offsetof(JNINativeInterface, GetStaticObjectField) / sizeof(void*));
static constexpr int SLOT_SET_STATIC_OBJECT_FIELD = (int)(offsetof(JNINativeInterface, SetStaticObjectField) / sizeof(void*));
static constexpr int SLOT_GET_ARRAY_LENGTH = (int)(offsetof(JNINativeInterface, GetArrayLength) / sizeof(void*));
static constexpr int SLOT_GET_OBJECT_ARRAY_ELEMENT = (int)(offsetof(JNINativeInterface, GetObjectArrayElement) / sizeof(void*));
static constexpr int SLOT_SET_OBJECT_ARRAY_ELEMENT = (int)(offsetof(JNINativeInterface, SetObjectArrayElement) / sizeof(void*));
static constexpr int SLOT_NEW_OBJECT_ARRAY = (int)(offsetof(JNINativeInterface, NewObjectArray) / sizeof(void*));
static constexpr int SLOT_IS_SAME_OBJECT = (int)(offsetof(JNINativeInterface, IsSameObject) / sizeof(void*));
static constexpr int SLOT_IS_INSTANCE_OF = (int)(offsetof(JNINativeInterface, IsInstanceOf) / sizeof(void*));
static constexpr int SLOT_GET_SUPERCLASS = (int)(offsetof(JNINativeInterface, GetSuperclass) / sizeof(void*));
static constexpr int SLOT_FROM_REFLECTED_METHOD = (int)(offsetof(JNINativeInterface, FromReflectedMethod) / sizeof(void*));
static constexpr int SLOT_FROM_REFLECTED_FIELD = (int)(offsetof(JNINativeInterface, FromReflectedField) / sizeof(void*));
static constexpr int SLOT_TO_REFLECTED_METHOD = (int)(offsetof(JNINativeInterface, ToReflectedMethod) / sizeof(void*));
static constexpr int SLOT_TO_REFLECTED_FIELD = (int)(offsetof(JNINativeInterface, ToReflectedField) / sizeof(void*));
static constexpr int SLOT_IS_ASSIGNABLE_FROM = (int)(offsetof(JNINativeInterface, IsAssignableFrom) / sizeof(void*));
static constexpr int SLOT_NEW_LOCAL_REF = (int)(offsetof(JNINativeInterface, NewLocalRef) / sizeof(void*));
static constexpr int SLOT_NEW_WEAK_GLOBAL_REF = (int)(offsetof(JNINativeInterface, NewWeakGlobalRef) / sizeof(void*));
static constexpr int SLOT_DELETE_WEAK_GLOBAL_REF = (int)(offsetof(JNINativeInterface, DeleteWeakGlobalRef) / sizeof(void*));
static constexpr int SLOT_PUSH_LOCAL_FRAME = (int)(offsetof(JNINativeInterface, PushLocalFrame) / sizeof(void*));
static constexpr int SLOT_POP_LOCAL_FRAME = (int)(offsetof(JNINativeInterface, PopLocalFrame) / sizeof(void*));
static constexpr int SLOT_ENSURE_LOCAL_CAPACITY = (int)(offsetof(JNINativeInterface, EnsureLocalCapacity) / sizeof(void*));
static constexpr int SLOT_MONITOR_ENTER = (int)(offsetof(JNINativeInterface, MonitorEnter) / sizeof(void*));
static constexpr int SLOT_MONITOR_EXIT = (int)(offsetof(JNINativeInterface, MonitorExit) / sizeof(void*));
static constexpr int SLOT_THROW = (int)(offsetof(JNINativeInterface, Throw) / sizeof(void*));
static constexpr int SLOT_THROW_NEW = (int)(offsetof(JNINativeInterface, ThrowNew) / sizeof(void*));
static constexpr int SLOT_GET_STRING_LENGTH = (int)(offsetof(JNINativeInterface, GetStringLength) / sizeof(void*));
static constexpr int SLOT_GET_STRING_UTF_REGION = (int)(offsetof(JNINativeInterface, GetStringUTFRegion) / sizeof(void*));
static constexpr int SLOT_NEW_STRING = (int)(offsetof(JNINativeInterface, NewString) / sizeof(void*));
static constexpr int SLOT_GET_STRING_CHARS = (int)(offsetof(JNINativeInterface, GetStringChars) / sizeof(void*));
static constexpr int SLOT_RELEASE_STRING_CHARS = (int)(offsetof(JNINativeInterface, ReleaseStringChars) / sizeof(void*));
static constexpr int SLOT_GET_STRING_REGION = (int)(offsetof(JNINativeInterface, GetStringRegion) / sizeof(void*));
static constexpr int SLOT_GET_STRING_CRITICAL = (int)(offsetof(JNINativeInterface, GetStringCritical) / sizeof(void*));
static constexpr int SLOT_RELEASE_STRING_CRITICAL = (int)(offsetof(JNINativeInterface, ReleaseStringCritical) / sizeof(void*));
static constexpr int SLOT_GET_PRIMITIVE_ARRAY_CRITICAL = (int)(offsetof(JNINativeInterface, GetPrimitiveArrayCritical) / sizeof(void*));
static constexpr int SLOT_RELEASE_PRIMITIVE_ARRAY_CRITICAL =
    (int)(offsetof(JNINativeInterface, ReleasePrimitiveArrayCritical) / sizeof(void*));
static constexpr int SLOT_GET_OBJECT_REF_TYPE = (int)(offsetof(JNINativeInterface, GetObjectRefType) / sizeof(void*));
static constexpr int SLOT_UNREGISTER_NATIVES = (int)(offsetof(JNINativeInterface, UnregisterNatives) / sizeof(void*));
static constexpr int SLOT_FATAL_ERROR = (int)(offsetof(JNINativeInterface, FatalError) / sizeof(void*));
static constexpr int SLOT_DEFINE_CLASS = (int)(offsetof(JNINativeInterface, DefineClass) / sizeof(void*));
static constexpr uint64_t MAXIMUM_GUEST_STRING_CHARS = 1u << 28;
static constexpr int SLOT_NEW_DIRECT_BYTE_BUFFER = (int)(offsetof(JNINativeInterface, NewDirectByteBuffer) / sizeof(void*));
static constexpr int SLOT_GET_DIRECT_BUFFER_ADDRESS = (int)(offsetof(JNINativeInterface, GetDirectBufferAddress) / sizeof(void*));
static constexpr int SLOT_GET_DIRECT_BUFFER_CAPACITY = (int)(offsetof(JNINativeInterface, GetDirectBufferCapacity) / sizeof(void*));

struct PrimitiveFieldSlot
{
    int slot;
    char letter;
    bool isStatic;
    bool writing;
};

enum class ArrayAction : uint8_t
{
    Create,
    Borrow,
    Return,
    ReadRegion,
    WriteRegion
};

struct PrimitiveArraySlot
{
    int slot;
    char letter;
    ArrayAction action;
};

static const PrimitiveArraySlot PRIMITIVE_ARRAY_SLOTS[] = {
    {(int)(offsetof(JNINativeInterface, NewBooleanArray) / sizeof(void*)), 'Z', ArrayAction::Create},
    {(int)(offsetof(JNINativeInterface, NewByteArray) / sizeof(void*)), 'B', ArrayAction::Create},
    {(int)(offsetof(JNINativeInterface, NewCharArray) / sizeof(void*)), 'C', ArrayAction::Create},
    {(int)(offsetof(JNINativeInterface, NewShortArray) / sizeof(void*)), 'S', ArrayAction::Create},
    {(int)(offsetof(JNINativeInterface, NewIntArray) / sizeof(void*)), 'I', ArrayAction::Create},
    {(int)(offsetof(JNINativeInterface, NewLongArray) / sizeof(void*)), 'J', ArrayAction::Create},
    {(int)(offsetof(JNINativeInterface, NewFloatArray) / sizeof(void*)), 'F', ArrayAction::Create},
    {(int)(offsetof(JNINativeInterface, NewDoubleArray) / sizeof(void*)), 'D', ArrayAction::Create},
    {(int)(offsetof(JNINativeInterface, GetBooleanArrayElements) / sizeof(void*)), 'Z', ArrayAction::Borrow},
    {(int)(offsetof(JNINativeInterface, GetByteArrayElements) / sizeof(void*)), 'B', ArrayAction::Borrow},
    {(int)(offsetof(JNINativeInterface, GetCharArrayElements) / sizeof(void*)), 'C', ArrayAction::Borrow},
    {(int)(offsetof(JNINativeInterface, GetShortArrayElements) / sizeof(void*)), 'S', ArrayAction::Borrow},
    {(int)(offsetof(JNINativeInterface, GetIntArrayElements) / sizeof(void*)), 'I', ArrayAction::Borrow},
    {(int)(offsetof(JNINativeInterface, GetLongArrayElements) / sizeof(void*)), 'J', ArrayAction::Borrow},
    {(int)(offsetof(JNINativeInterface, GetFloatArrayElements) / sizeof(void*)), 'F', ArrayAction::Borrow},
    {(int)(offsetof(JNINativeInterface, GetDoubleArrayElements) / sizeof(void*)), 'D', ArrayAction::Borrow},
    {(int)(offsetof(JNINativeInterface, ReleaseBooleanArrayElements) / sizeof(void*)), 'Z', ArrayAction::Return},
    {(int)(offsetof(JNINativeInterface, ReleaseByteArrayElements) / sizeof(void*)), 'B', ArrayAction::Return},
    {(int)(offsetof(JNINativeInterface, ReleaseCharArrayElements) / sizeof(void*)), 'C', ArrayAction::Return},
    {(int)(offsetof(JNINativeInterface, ReleaseShortArrayElements) / sizeof(void*)), 'S', ArrayAction::Return},
    {(int)(offsetof(JNINativeInterface, ReleaseIntArrayElements) / sizeof(void*)), 'I', ArrayAction::Return},
    {(int)(offsetof(JNINativeInterface, ReleaseLongArrayElements) / sizeof(void*)), 'J', ArrayAction::Return},
    {(int)(offsetof(JNINativeInterface, ReleaseFloatArrayElements) / sizeof(void*)), 'F', ArrayAction::Return},
    {(int)(offsetof(JNINativeInterface, ReleaseDoubleArrayElements) / sizeof(void*)), 'D', ArrayAction::Return},
    {(int)(offsetof(JNINativeInterface, GetBooleanArrayRegion) / sizeof(void*)), 'Z', ArrayAction::ReadRegion},
    {(int)(offsetof(JNINativeInterface, GetByteArrayRegion) / sizeof(void*)), 'B', ArrayAction::ReadRegion},
    {(int)(offsetof(JNINativeInterface, GetCharArrayRegion) / sizeof(void*)), 'C', ArrayAction::ReadRegion},
    {(int)(offsetof(JNINativeInterface, GetShortArrayRegion) / sizeof(void*)), 'S', ArrayAction::ReadRegion},
    {(int)(offsetof(JNINativeInterface, GetIntArrayRegion) / sizeof(void*)), 'I', ArrayAction::ReadRegion},
    {(int)(offsetof(JNINativeInterface, GetLongArrayRegion) / sizeof(void*)), 'J', ArrayAction::ReadRegion},
    {(int)(offsetof(JNINativeInterface, GetFloatArrayRegion) / sizeof(void*)), 'F', ArrayAction::ReadRegion},
    {(int)(offsetof(JNINativeInterface, GetDoubleArrayRegion) / sizeof(void*)), 'D', ArrayAction::ReadRegion},
    {(int)(offsetof(JNINativeInterface, SetBooleanArrayRegion) / sizeof(void*)), 'Z', ArrayAction::WriteRegion},
    {(int)(offsetof(JNINativeInterface, SetByteArrayRegion) / sizeof(void*)), 'B', ArrayAction::WriteRegion},
    {(int)(offsetof(JNINativeInterface, SetCharArrayRegion) / sizeof(void*)), 'C', ArrayAction::WriteRegion},
    {(int)(offsetof(JNINativeInterface, SetShortArrayRegion) / sizeof(void*)), 'S', ArrayAction::WriteRegion},
    {(int)(offsetof(JNINativeInterface, SetIntArrayRegion) / sizeof(void*)), 'I', ArrayAction::WriteRegion},
    {(int)(offsetof(JNINativeInterface, SetLongArrayRegion) / sizeof(void*)), 'J', ArrayAction::WriteRegion},
    {(int)(offsetof(JNINativeInterface, SetFloatArrayRegion) / sizeof(void*)), 'F', ArrayAction::WriteRegion},
    {(int)(offsetof(JNINativeInterface, SetDoubleArrayRegion) / sizeof(void*)), 'D', ArrayAction::WriteRegion},
};

static const PrimitiveFieldSlot PRIMITIVE_FIELD_SLOTS[] = {
    {(int)(offsetof(JNINativeInterface, GetBooleanField) / sizeof(void*)), 'Z', false, false},
    {(int)(offsetof(JNINativeInterface, GetByteField) / sizeof(void*)), 'B', false, false},
    {(int)(offsetof(JNINativeInterface, GetCharField) / sizeof(void*)), 'C', false, false},
    {(int)(offsetof(JNINativeInterface, GetShortField) / sizeof(void*)), 'S', false, false},
    {(int)(offsetof(JNINativeInterface, GetIntField) / sizeof(void*)), 'I', false, false},
    {(int)(offsetof(JNINativeInterface, GetLongField) / sizeof(void*)), 'J', false, false},
    {(int)(offsetof(JNINativeInterface, GetFloatField) / sizeof(void*)), 'F', false, false},
    {(int)(offsetof(JNINativeInterface, GetDoubleField) / sizeof(void*)), 'D', false, false},
    {(int)(offsetof(JNINativeInterface, SetBooleanField) / sizeof(void*)), 'Z', false, true},
    {(int)(offsetof(JNINativeInterface, SetByteField) / sizeof(void*)), 'B', false, true},
    {(int)(offsetof(JNINativeInterface, SetCharField) / sizeof(void*)), 'C', false, true},
    {(int)(offsetof(JNINativeInterface, SetShortField) / sizeof(void*)), 'S', false, true},
    {(int)(offsetof(JNINativeInterface, SetIntField) / sizeof(void*)), 'I', false, true},
    {(int)(offsetof(JNINativeInterface, SetLongField) / sizeof(void*)), 'J', false, true},
    {(int)(offsetof(JNINativeInterface, SetFloatField) / sizeof(void*)), 'F', false, true},
    {(int)(offsetof(JNINativeInterface, SetDoubleField) / sizeof(void*)), 'D', false, true},
    {(int)(offsetof(JNINativeInterface, GetStaticBooleanField) / sizeof(void*)), 'Z', true, false},
    {(int)(offsetof(JNINativeInterface, GetStaticByteField) / sizeof(void*)), 'B', true, false},
    {(int)(offsetof(JNINativeInterface, GetStaticCharField) / sizeof(void*)), 'C', true, false},
    {(int)(offsetof(JNINativeInterface, GetStaticShortField) / sizeof(void*)), 'S', true, false},
    {(int)(offsetof(JNINativeInterface, GetStaticIntField) / sizeof(void*)), 'I', true, false},
    {(int)(offsetof(JNINativeInterface, GetStaticLongField) / sizeof(void*)), 'J', true, false},
    {(int)(offsetof(JNINativeInterface, GetStaticFloatField) / sizeof(void*)), 'F', true, false},
    {(int)(offsetof(JNINativeInterface, GetStaticDoubleField) / sizeof(void*)), 'D', true, false},
    {(int)(offsetof(JNINativeInterface, SetStaticBooleanField) / sizeof(void*)), 'Z', true, true},
    {(int)(offsetof(JNINativeInterface, SetStaticByteField) / sizeof(void*)), 'B', true, true},
    {(int)(offsetof(JNINativeInterface, SetStaticCharField) / sizeof(void*)), 'C', true, true},
    {(int)(offsetof(JNINativeInterface, SetStaticShortField) / sizeof(void*)), 'S', true, true},
    {(int)(offsetof(JNINativeInterface, SetStaticIntField) / sizeof(void*)), 'I', true, true},
    {(int)(offsetof(JNINativeInterface, SetStaticLongField) / sizeof(void*)), 'J', true, true},
    {(int)(offsetof(JNINativeInterface, SetStaticFloatField) / sizeof(void*)), 'F', true, true},
    {(int)(offsetof(JNINativeInterface, SetStaticDoubleField) / sizeof(void*)), 'D', true, true},
};

static uint64_t ElementBytes(char letter)
{
    switch (letter)
    {
        case 'Z':
        case 'B': return 1;
        case 'C':
        case 'S': return 2;
        case 'I':
        case 'F': return 4;
        case 'J':
        case 'D': return 8;
        default: break;
    }

    return 0;
}

static void* BorrowArray(JNIEnv* env, char letter, jarray array, jboolean* copied)
{
    switch (letter)
    {
        case 'Z': return env->GetBooleanArrayElements((jbooleanArray)array, copied);
        case 'B': return env->GetByteArrayElements((jbyteArray)array, copied);
        case 'C': return env->GetCharArrayElements((jcharArray)array, copied);
        case 'S': return env->GetShortArrayElements((jshortArray)array, copied);
        case 'I': return env->GetIntArrayElements((jintArray)array, copied);
        case 'J': return env->GetLongArrayElements((jlongArray)array, copied);
        case 'F': return env->GetFloatArrayElements((jfloatArray)array, copied);
        case 'D': return env->GetDoubleArrayElements((jdoubleArray)array, copied);
        default: break;
    }

    return nullptr;
}

static void ReturnArray(JNIEnv* env, char letter, jarray array, void* elements, jint mode)
{
    switch (letter)
    {
        case 'Z': env->ReleaseBooleanArrayElements((jbooleanArray)array, (jboolean*)elements, mode); return;
        case 'B': env->ReleaseByteArrayElements((jbyteArray)array, (jbyte*)elements, mode); return;
        case 'C': env->ReleaseCharArrayElements((jcharArray)array, (jchar*)elements, mode); return;
        case 'S': env->ReleaseShortArrayElements((jshortArray)array, (jshort*)elements, mode); return;
        case 'I': env->ReleaseIntArrayElements((jintArray)array, (jint*)elements, mode); return;
        case 'J': env->ReleaseLongArrayElements((jlongArray)array, (jlong*)elements, mode); return;
        case 'F': env->ReleaseFloatArrayElements((jfloatArray)array, (jfloat*)elements, mode); return;
        case 'D': env->ReleaseDoubleArrayElements((jdoubleArray)array, (jdouble*)elements, mode); return;
        default: break;
    }
}

static jarray CreateArray(JNIEnv* env, char letter, jsize length)
{
    switch (letter)
    {
        case 'Z': return env->NewBooleanArray(length);
        case 'B': return env->NewByteArray(length);
        case 'C': return env->NewCharArray(length);
        case 'S': return env->NewShortArray(length);
        case 'I': return env->NewIntArray(length);
        case 'J': return env->NewLongArray(length);
        case 'F': return env->NewFloatArray(length);
        case 'D': return env->NewDoubleArray(length);
        default: break;
    }

    return nullptr;
}

static void ReadArrayRegion(JNIEnv* env, char letter, jarray array, jsize start, jsize count, void* destination)
{
    switch (letter)
    {
        case 'Z': env->GetBooleanArrayRegion((jbooleanArray)array, start, count, (jboolean*)destination); return;
        case 'B': env->GetByteArrayRegion((jbyteArray)array, start, count, (jbyte*)destination); return;
        case 'C': env->GetCharArrayRegion((jcharArray)array, start, count, (jchar*)destination); return;
        case 'S': env->GetShortArrayRegion((jshortArray)array, start, count, (jshort*)destination); return;
        case 'I': env->GetIntArrayRegion((jintArray)array, start, count, (jint*)destination); return;
        case 'J': env->GetLongArrayRegion((jlongArray)array, start, count, (jlong*)destination); return;
        case 'F': env->GetFloatArrayRegion((jfloatArray)array, start, count, (jfloat*)destination); return;
        case 'D': env->GetDoubleArrayRegion((jdoubleArray)array, start, count, (jdouble*)destination); return;
        default: break;
    }
}

static void WriteArrayRegion(JNIEnv* env, char letter, jarray array, jsize start, jsize count, const void* source)
{
    switch (letter)
    {
        case 'Z': env->SetBooleanArrayRegion((jbooleanArray)array, start, count, (const jboolean*)source); return;
        case 'B': env->SetByteArrayRegion((jbyteArray)array, start, count, (const jbyte*)source); return;
        case 'C': env->SetCharArrayRegion((jcharArray)array, start, count, (const jchar*)source); return;
        case 'S': env->SetShortArrayRegion((jshortArray)array, start, count, (const jshort*)source); return;
        case 'I': env->SetIntArrayRegion((jintArray)array, start, count, (const jint*)source); return;
        case 'J': env->SetLongArrayRegion((jlongArray)array, start, count, (const jlong*)source); return;
        case 'F': env->SetFloatArrayRegion((jfloatArray)array, start, count, (const jfloat*)source); return;
        case 'D': env->SetDoubleArrayRegion((jdoubleArray)array, start, count, (const jdouble*)source); return;
        default: break;
    }
}

static AbiBridge::Result PrimitiveArrayAccess(JNIEnv* env, const PrimitiveArraySlot& slot, GuestState::Registers& registers,
                                              const GuestState::MemoryAccessor& memory)
{
    uint64_t width = ElementBytes(slot.letter);
    if (width == 0) return AbiBridge::Result::UnsupportedSignature;

    if (slot.action == ArrayAction::Create)
    {
        registers.general[0] = (uint64_t)CreateArray(env, slot.letter, (jsize)(int32_t)(uint32_t)registers.general[1]);
        return AbiBridge::Result::Ok;
    }

    jarray array = (jarray)registers.general[1];

    if (slot.action == ArrayAction::Borrow)
    {
        jboolean copied = JNI_FALSE;
        void* elements = BorrowArray(env, slot.letter, array, &copied);

        uint64_t destination = registers.general[2];
        if (destination != 0 && !memory.Write(memory.context, (GuestState::GuestAddress)destination, &copied, sizeof(copied)))
            return AbiBridge::Result::MalformedArguments;

        registers.general[0] = (uint64_t)elements;
        return AbiBridge::Result::Ok;
    }

    if (slot.action == ArrayAction::Return)
    {
        ReturnArray(env, slot.letter, array, (void*)registers.general[2], (jint)(int32_t)(uint32_t)registers.general[3]);
        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    jsize start = (jsize)(int32_t)(uint32_t)registers.general[2];
    jsize count = (jsize)(int32_t)(uint32_t)registers.general[3];
    uint64_t buffer = registers.general[4];

    if (count < 0 || buffer == 0)
    {
        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    std::vector<uint8_t> staging((size_t)((uint64_t)count * width));

    if (slot.action == ArrayAction::ReadRegion)
    {
        ReadArrayRegion(env, slot.letter, array, start, count, staging.data());

        if (!staging.empty() && !memory.Write(memory.context, (GuestState::GuestAddress)buffer, staging.data(), staging.size()))
            return AbiBridge::Result::MalformedArguments;

        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    if (!staging.empty() && !memory.Read(memory.context, (GuestState::GuestAddress)buffer, staging.data(), staging.size()))
        return AbiBridge::Result::MalformedArguments;

    WriteArrayRegion(env, slot.letter, array, start, count, staging.data());
    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result PrimitiveFieldAccess(JNIEnv* env, const PrimitiveFieldSlot& slot, GuestState::Registers& registers)
{
    jobject receiver = (jobject)registers.general[1];
    jclass owner = (jclass)registers.general[1];
    jfieldID field = (jfieldID)registers.general[2];

    if (!slot.writing)
    {
        switch (slot.letter)
        {
            case 'Z':
                registers.general[0] = (slot.isStatic ? env->GetStaticBooleanField(owner, field) : env->GetBooleanField(receiver, field))
                                           ? 1u
                                           : 0u;
                return AbiBridge::Result::Ok;
            case 'B':
                registers.general[0] = (uint64_t)(uint32_t)(int32_t)(slot.isStatic ? env->GetStaticByteField(owner, field)
                                                                                   : env->GetByteField(receiver, field));
                return AbiBridge::Result::Ok;
            case 'C':
                registers.general[0] = (uint64_t)(uint32_t)(slot.isStatic ? env->GetStaticCharField(owner, field)
                                                                          : env->GetCharField(receiver, field));
                return AbiBridge::Result::Ok;
            case 'S':
                registers.general[0] = (uint64_t)(uint32_t)(int32_t)(slot.isStatic ? env->GetStaticShortField(owner, field)
                                                                                   : env->GetShortField(receiver, field));
                return AbiBridge::Result::Ok;
            case 'I':
                registers.general[0] = (uint64_t)(uint32_t)(slot.isStatic ? env->GetStaticIntField(owner, field)
                                                                          : env->GetIntField(receiver, field));
                return AbiBridge::Result::Ok;
            case 'J':
                registers.general[0] =
                    (uint64_t)(slot.isStatic ? env->GetStaticLongField(owner, field) : env->GetLongField(receiver, field));
                return AbiBridge::Result::Ok;
            case 'F':
            {
                float produced = slot.isStatic ? env->GetStaticFloatField(owner, field) : env->GetFloatField(receiver, field);
                uint32_t bits = 0;
                memcpy(&bits, &produced, sizeof(bits));
                registers.vector[0].low = bits;
                registers.vector[0].high = 0;
                return AbiBridge::Result::Ok;
            }
            default:
            {
                double produced = slot.isStatic ? env->GetStaticDoubleField(owner, field) : env->GetDoubleField(receiver, field);
                uint64_t bits = 0;
                memcpy(&bits, &produced, sizeof(bits));
                registers.vector[0].low = bits;
                registers.vector[0].high = 0;
                return AbiBridge::Result::Ok;
            }
        }
    }

    switch (slot.letter)
    {
        case 'Z':
        {
            jboolean value = (registers.general[3] & 1) != 0 ? JNI_TRUE : JNI_FALSE;
            if (slot.isStatic) env->SetStaticBooleanField(owner, field, value);
            else env->SetBooleanField(receiver, field, value);
            return AbiBridge::Result::Ok;
        }
        case 'B':
        {
            jbyte value = (jbyte)registers.general[3];
            if (slot.isStatic) env->SetStaticByteField(owner, field, value);
            else env->SetByteField(receiver, field, value);
            return AbiBridge::Result::Ok;
        }
        case 'C':
        {
            jchar value = (jchar)registers.general[3];
            if (slot.isStatic) env->SetStaticCharField(owner, field, value);
            else env->SetCharField(receiver, field, value);
            return AbiBridge::Result::Ok;
        }
        case 'S':
        {
            jshort value = (jshort)registers.general[3];
            if (slot.isStatic) env->SetStaticShortField(owner, field, value);
            else env->SetShortField(receiver, field, value);
            return AbiBridge::Result::Ok;
        }
        case 'I':
        {
            jint value = (jint)(uint32_t)registers.general[3];
            if (slot.isStatic) env->SetStaticIntField(owner, field, value);
            else env->SetIntField(receiver, field, value);
            return AbiBridge::Result::Ok;
        }
        case 'J':
        {
            jlong value = (jlong)registers.general[3];
            if (slot.isStatic) env->SetStaticLongField(owner, field, value);
            else env->SetLongField(receiver, field, value);
            return AbiBridge::Result::Ok;
        }
        case 'F':
        {
            uint32_t bits = (uint32_t)registers.vector[0].low;
            float value = 0.0f;
            memcpy(&value, &bits, sizeof(value));
            if (slot.isStatic) env->SetStaticFloatField(owner, field, value);
            else env->SetFloatField(receiver, field, value);
            return AbiBridge::Result::Ok;
        }
        default:
        {
            uint64_t bits = registers.vector[0].low;
            double value = 0.0;
            memcpy(&value, &bits, sizeof(value));
            if (slot.isStatic) env->SetStaticDoubleField(owner, field, value);
            else env->SetDoubleField(receiver, field, value);
            return AbiBridge::Result::Ok;
        }
    }
}

enum class CallVariant : uint8_t
{
    Standard,
    VaList,
    Array
};

enum class CallTarget : uint8_t
{
    Instance,
    Static,
    Nonvirtual
};

struct CallSlot
{
    int slot;
    CallTarget target;
};

static_assert(offsetof(JNINativeInterface, CallObjectMethodV) == offsetof(JNINativeInterface, CallObjectMethod) + sizeof(void*),
              "the V variant must sit one slot after its varargs form, which is what lets one table cover all three");
static_assert(offsetof(JNINativeInterface, CallObjectMethodA) == offsetof(JNINativeInterface, CallObjectMethod) + 2 * sizeof(void*),
              "the A variant must sit two slots after its varargs form");
static_assert(offsetof(JNINativeInterface, CallStaticObjectMethodA) == offsetof(JNINativeInterface, CallStaticObjectMethod) +
                                                                           2 * sizeof(void*),
              "the static family must have the same three-slot shape");

static_assert(offsetof(JNINativeInterface, CallNonvirtualObjectMethodA) == offsetof(JNINativeInterface, CallNonvirtualObjectMethod) +
                                                                               2 * sizeof(void*),
              "the nonvirtual family must have the same three-slot shape");

static const CallSlot CALL_SLOTS[] = {
    {(int)(offsetof(JNINativeInterface, CallObjectMethod) / sizeof(void*)), CallTarget::Instance},
    {(int)(offsetof(JNINativeInterface, CallBooleanMethod) / sizeof(void*)), CallTarget::Instance},
    {(int)(offsetof(JNINativeInterface, CallByteMethod) / sizeof(void*)), CallTarget::Instance},
    {(int)(offsetof(JNINativeInterface, CallCharMethod) / sizeof(void*)), CallTarget::Instance},
    {(int)(offsetof(JNINativeInterface, CallShortMethod) / sizeof(void*)), CallTarget::Instance},
    {(int)(offsetof(JNINativeInterface, CallIntMethod) / sizeof(void*)), CallTarget::Instance},
    {(int)(offsetof(JNINativeInterface, CallLongMethod) / sizeof(void*)), CallTarget::Instance},
    {(int)(offsetof(JNINativeInterface, CallFloatMethod) / sizeof(void*)), CallTarget::Instance},
    {(int)(offsetof(JNINativeInterface, CallDoubleMethod) / sizeof(void*)), CallTarget::Instance},
    {(int)(offsetof(JNINativeInterface, CallVoidMethod) / sizeof(void*)), CallTarget::Instance},
    {(int)(offsetof(JNINativeInterface, CallNonvirtualObjectMethod) / sizeof(void*)), CallTarget::Nonvirtual},
    {(int)(offsetof(JNINativeInterface, CallNonvirtualBooleanMethod) / sizeof(void*)), CallTarget::Nonvirtual},
    {(int)(offsetof(JNINativeInterface, CallNonvirtualByteMethod) / sizeof(void*)), CallTarget::Nonvirtual},
    {(int)(offsetof(JNINativeInterface, CallNonvirtualCharMethod) / sizeof(void*)), CallTarget::Nonvirtual},
    {(int)(offsetof(JNINativeInterface, CallNonvirtualShortMethod) / sizeof(void*)), CallTarget::Nonvirtual},
    {(int)(offsetof(JNINativeInterface, CallNonvirtualIntMethod) / sizeof(void*)), CallTarget::Nonvirtual},
    {(int)(offsetof(JNINativeInterface, CallNonvirtualLongMethod) / sizeof(void*)), CallTarget::Nonvirtual},
    {(int)(offsetof(JNINativeInterface, CallNonvirtualFloatMethod) / sizeof(void*)), CallTarget::Nonvirtual},
    {(int)(offsetof(JNINativeInterface, CallNonvirtualDoubleMethod) / sizeof(void*)), CallTarget::Nonvirtual},
    {(int)(offsetof(JNINativeInterface, CallNonvirtualVoidMethod) / sizeof(void*)), CallTarget::Nonvirtual},
    {(int)(offsetof(JNINativeInterface, CallStaticObjectMethod) / sizeof(void*)), CallTarget::Static},
    {(int)(offsetof(JNINativeInterface, CallStaticBooleanMethod) / sizeof(void*)), CallTarget::Static},
    {(int)(offsetof(JNINativeInterface, CallStaticByteMethod) / sizeof(void*)), CallTarget::Static},
    {(int)(offsetof(JNINativeInterface, CallStaticCharMethod) / sizeof(void*)), CallTarget::Static},
    {(int)(offsetof(JNINativeInterface, CallStaticShortMethod) / sizeof(void*)), CallTarget::Static},
    {(int)(offsetof(JNINativeInterface, CallStaticIntMethod) / sizeof(void*)), CallTarget::Static},
    {(int)(offsetof(JNINativeInterface, CallStaticLongMethod) / sizeof(void*)), CallTarget::Static},
    {(int)(offsetof(JNINativeInterface, CallStaticFloatMethod) / sizeof(void*)), CallTarget::Static},
    {(int)(offsetof(JNINativeInterface, CallStaticDoubleMethod) / sizeof(void*)), CallTarget::Static},
    {(int)(offsetof(JNINativeInterface, CallStaticVoidMethod) / sizeof(void*)), CallTarget::Static},
};

static constexpr int INVOKE_SLOT_DESTROY = (int)(offsetof(JNIInvokeInterface, DestroyJavaVM) / sizeof(void*));
static constexpr int INVOKE_SLOT_ATTACH = (int)(offsetof(JNIInvokeInterface, AttachCurrentThread) / sizeof(void*));
static constexpr int INVOKE_SLOT_DETACH = (int)(offsetof(JNIInvokeInterface, DetachCurrentThread) / sizeof(void*));
static constexpr int INVOKE_SLOT_GET_ENV = (int)(offsetof(JNIInvokeInterface, GetEnv) / sizeof(void*));

struct JniBridgeState;

struct SlotBinding
{
    JniBridgeState* state = nullptr;
    int slot = 0;
    bool invokeInterface = false;
};

struct JniBridgeState
{
    std::atomic<bool> retired{false};
    GuestRuntime::Instance* runtime = nullptr;
    JavaVM* javaVm = nullptr;
    GuestState::GuestAddress guestVm{};
    GuestState::GuestAddress guestEnv{};
    GuestState::GuestAddress nativeTable{};
    GuestState::GuestAddress invokeTable{};
    std::deque<SlotBinding> bindings;
    std::vector<JniBridge::RegisteredNative> registeredNatives;
    std::unordered_map<const void*, std::string> methodDescriptors;
    std::unordered_map<const void*, std::string> methodNames;
    bool traceCalls = false;
    JniBridge::LibraryTranslator translator = nullptr;
    void* translatorContext = nullptr;
    std::vector<LlvmBackend::CompiledFunction> thunks;
    LlvmBackend::Session backend;
    JniBridge::Statistics statistics;
    std::recursive_mutex mutex;
};

static bool IsJniValueLetter(char letter)
{
    return letter == 'Z' || letter == 'B' || letter == 'C' || letter == 'S' || letter == 'I' || letter == 'J' || letter == 'F' ||
           letter == 'D' || letter == 'L';
}

static bool SplitDescriptor(const char* descriptor, std::vector<char>& argumentLetters, char& returnLetter)
{
    argumentLetters.clear();
    returnLetter = 'V';

    if (!descriptor || descriptor[0] != '(') return false;

    uint64_t cursor = 1;
    while (descriptor[cursor] != '\0' && descriptor[cursor] != ')')
    {
        bool isArray = descriptor[cursor] == '[';
        while (descriptor[cursor] == '[') cursor += 1;
        if (!IsJniValueLetter(descriptor[cursor])) return false;

        char letter = descriptor[cursor];
        if (letter == 'L')
        {
            while (descriptor[cursor] != '\0' && descriptor[cursor] != ';') cursor += 1;
            if (descriptor[cursor] == '\0') return false;
        }

        argumentLetters.push_back(isArray ? '[' : letter);
        cursor += 1;
    }

    if (descriptor[cursor] != ')') return false;

    cursor += 1;
    bool returnIsArray = descriptor[cursor] == '[';
    while (descriptor[cursor] == '[') cursor += 1;
    if (descriptor[cursor] != 'V' && !IsJniValueLetter(descriptor[cursor])) return false;

    returnLetter = returnIsArray ? '[' : descriptor[cursor];
    return true;
}

static AbiBridge::Argument ArgumentForLetter(char letter)
{
    AbiBridge::Argument argument;

    switch (letter)
    {
        case 'Z':
        case 'B':
        case 'C':
        case 'S':
        case 'I':
            argument.argumentClass = AbiBridge::ArgumentClass::Integer;
            argument.sizeInBytes = 4;
            break;

        case 'J':
            argument.argumentClass = AbiBridge::ArgumentClass::Integer;
            argument.sizeInBytes = 8;
            break;

        case 'F':
            argument.argumentClass = AbiBridge::ArgumentClass::Float;
            argument.sizeInBytes = 4;
            break;

        case 'D':
            argument.argumentClass = AbiBridge::ArgumentClass::Double;
            argument.sizeInBytes = 8;
            break;

        case 'L':
        case '[':
            argument.argumentClass = AbiBridge::ArgumentClass::Pointer;
            argument.sizeInBytes = 8;
            break;

        default: break;
    }

    argument.alignmentInBytes = argument.sizeInBytes;
    return argument;
}

extern "C" uint64_t AclJniEnterGuest(void* context, uint64_t guestFunction, const char* descriptor, const uint64_t* slots)
{
    JniBridgeState* state = (JniBridgeState*)context;

    std::vector<char> argumentLetters;
    char returnLetter = 'V';
    if (!SplitDescriptor(descriptor, argumentLetters, returnLetter)) return 0;

    AbiBridge::Signature signature;
    signature.returnValue = ArgumentForLetter(returnLetter);
    signature.arguments[0] = ArgumentForLetter('L');
    signature.arguments[1] = ArgumentForLetter('L');

    uint64_t count = 2;
    for (char letter : argumentLetters)
    {
        if (count >= AbiBridge::MAX_ARGUMENTS) return 0;

        signature.arguments[count] = ArgumentForLetter(letter);
        count += 1;
    }

    signature.totalArgumentCount = (uint8_t)count;
    signature.namedArgumentCount = (uint8_t)count;

    std::vector<uint64_t> substituted(slots, slots + count);
    substituted[0] = (uint64_t)state->guestEnv;

    AbiBridge::ArgumentBuffer buffer;
    buffer.slots = substituted.data();
    buffer.slotCount = (uint32_t)count;
    buffer.slotStrideBytes = sizeof(uint64_t);

    uint64_t returned = 0;
    AbiBridge::Result entered = AbiBridge::CallGuestFromHost(GuestRuntime::Caller(*state->runtime), (GuestState::GuestAddress)guestFunction,
                                                             signature, buffer, &returned, sizeof(returned));

    if (entered != AbiBridge::Result::Ok)
    {
        fprintf(stderr, "[jnibridge] entering guest native %016llx%s failed: %s\n", (unsigned long long)guestFunction, descriptor,
                AbiBridge::ResultName(entered));
        return 0;
    }

    return returned;
}

static JniBridgeState* Implementation(const JniBridge::Instance& bridge)
{
    return (JniBridgeState*)bridge.implementation;
}

static JNIEnv* HostEnv(JniBridgeState* state)
{
    if (!state->javaVm) return nullptr;

    JNIEnv* env = nullptr;
    if (state->javaVm->GetEnv((void**)&env, JNI_VERSION_1_6) == JNI_OK) return env;
    if (state->javaVm->AttachCurrentThread(&env, nullptr) == JNI_OK) return env;

    return nullptr;
}

static void DetachExitingThread(void* context)
{
    JniBridgeState* state = (JniBridgeState*)context;
    if (!state || !state->javaVm) return;

    JNIEnv* attached = nullptr;
    if (state->javaVm->GetEnv((void**)&attached, JNI_VERSION_1_6) != JNI_OK) return;

    state->javaVm->DetachCurrentThread();
}

static bool ReadGuestString(const GuestState::MemoryAccessor& memory, uint64_t address, std::string& text)
{
    text.clear();
    if (address == 0) return false;

    for (uint64_t index = 0; index < MAX_GUEST_STRING_BYTES; ++index)
    {
        char character = 0;
        if (!memory.Read(memory.context, (GuestState::GuestAddress)(address + index), &character, 1)) return false;
        if (character == '\0') return true;

        text.push_back(character);
    }

    return false;
}

static bool WriteGuestPointer(const GuestState::MemoryAccessor& memory, uint64_t address, uint64_t value)
{
    return memory.Write(memory.context, (GuestState::GuestAddress)address, &value, sizeof(value));
}

static bool ReadGuestPointer(const GuestState::MemoryAccessor& memory, uint64_t address, uint64_t& value)
{
    return memory.Read(memory.context, (GuestState::GuestAddress)address, &value, sizeof(value));
}

static thread_local jobject tLoadingClassLoader = nullptr;
static std::mutex gApplicationLoaderLock;
static jobject gApplicationClassLoader = nullptr;

static void RememberApplicationLoader(JNIEnv* env, jobject loader)
{
    if (!loader) return;

    std::lock_guard<std::mutex> guard(gApplicationLoaderLock);
    if (!gApplicationClassLoader) gApplicationClassLoader = env->NewGlobalRef(loader);
}

static jclass FindClassThroughLoader(JNIEnv* env, const std::string& name)
{
    jobject loader = tLoadingClassLoader;
    if (!loader)
    {
        std::lock_guard<std::mutex> guard(gApplicationLoaderLock);
        loader = gApplicationClassLoader;
    }

    if (!loader) return nullptr;

    jthrowable pending = env->ExceptionOccurred();
    env->ExceptionClear();

    std::string dotted = name;
    for (char& character : dotted)
    {
        if (character == '/') character = '.';
    }

    jclass loaderClass = env->FindClass("java/lang/ClassLoader");
    jmethodID loadClass = loaderClass ? env->GetMethodID(loaderClass, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;") : nullptr;
    if (loaderClass) env->DeleteLocalRef(loaderClass);

    jclass found = nullptr;
    if (loadClass)
    {
        jstring requested = env->NewStringUTF(dotted.c_str());
        found = (jclass)env->CallObjectMethod(loader, loadClass, requested);
        env->DeleteLocalRef(requested);
    }

    if (found && !env->ExceptionCheck())
    {
        if (pending) env->DeleteLocalRef(pending);
        return found;
    }

    env->ExceptionClear();
    if (found) env->DeleteLocalRef(found);

    if (pending)
    {
        env->Throw(pending);
        env->DeleteLocalRef(pending);
    }

    return nullptr;
}

static AbiBridge::Result UnimplementedSlot(JniBridgeState* state, const SlotBinding* binding)
{
    {
        std::lock_guard<std::recursive_mutex> guard(state->mutex);
        state->statistics.unimplementedSlotCalls += 1;
    }

    char described[96];
    snprintf(described, sizeof(described), "%s slot %d (%s)", binding->invokeInterface ? "JNIInvokeInterface" : "JNINativeInterface",
             binding->slot, JniBridge::SlotName(binding->slot));
    Diagnostics::NoteGap(Diagnostics::Gap::JniSlot, described);

    fprintf(stderr, "[jnibridge] the guest called %s slot %d (%s), which is not implemented\n",
            binding->invokeInterface ? "JNIInvokeInterface" : "JNINativeInterface", binding->slot, JniBridge::SlotName(binding->slot));

    return AbiBridge::Result::UnknownSymbol;
}

static AbiBridge::Result InvokeSlotHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    const SlotBinding* binding = (const SlotBinding*)context;
    JniBridgeState* state = binding->state;

    if (!state || state->retired.load(std::memory_order_acquire))
    {
        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    std::lock_guard<std::recursive_mutex> guard(state->mutex);
    state->statistics.slotCalls += 1;

    if (binding->slot == INVOKE_SLOT_GET_ENV)
    {
        uint64_t destination = registers.general[1];
        if (destination == 0 || !WriteGuestPointer(memory, destination, (uint64_t)state->guestEnv))
        {
            registers.general[0] = (uint64_t)(uint32_t)JNI_ERR;
            return AbiBridge::Result::Ok;
        }

        registers.general[0] = (uint64_t)(uint32_t)JNI_OK;
        return AbiBridge::Result::Ok;
    }

    if (binding->slot == INVOKE_SLOT_ATTACH)
    {
        uint64_t destination = registers.general[1];
        if (!HostEnv(state) || destination == 0 || !WriteGuestPointer(memory, destination, (uint64_t)state->guestEnv))
        {
            registers.general[0] = (uint64_t)(uint32_t)JNI_ERR;
            return AbiBridge::Result::Ok;
        }

        registers.general[0] = (uint64_t)(uint32_t)JNI_OK;
        return AbiBridge::Result::Ok;
    }

    if (binding->slot == INVOKE_SLOT_DETACH)
    {
        registers.general[0] = (uint64_t)(uint32_t)state->javaVm->DetachCurrentThread();
        return AbiBridge::Result::Ok;
    }

    if (binding->slot == INVOKE_SLOT_DESTROY)
    {
        fprintf(stderr, "[jnibridge] the guest called DestroyJavaVM, which acl owns and will not honour\n");
        registers.general[0] = (uint64_t)(uint32_t)JNI_ERR;
        return AbiBridge::Result::Ok;
    }

    return UnimplementedSlot(state, binding);
}

static AbiBridge::Result RegisterNativesSlot(JniBridgeState* state, JNIEnv* env, GuestState::Registers& registers,
                                             const GuestState::MemoryAccessor& memory)
{
    std::lock_guard<std::recursive_mutex> guard(state->mutex);

    uint64_t declaringClass = registers.general[1];
    uint64_t table = registers.general[2];
    int32_t count = (int32_t)(uint32_t)registers.general[3];

    if (count == 0 && declaringClass != 0)
    {
        registers.general[0] = (uint64_t)(uint32_t)JNI_OK;
        return AbiBridge::Result::Ok;
    }

    if (count < 0 || count > MAX_REGISTERED_NATIVES || table == 0)
    {
        fprintf(stderr, "[jnibridge] RegisterNatives was called with %d methods at %016llx\n", count, (unsigned long long)table);
        registers.general[0] = (uint64_t)(uint32_t)JNI_ERR;
        return AbiBridge::Result::Ok;
    }

    for (int32_t index = 0; index < count; ++index)
    {
        uint64_t entry = table + (uint64_t)index * JNI_NATIVE_METHOD_BYTES;

        uint64_t namePointer = 0;
        uint64_t signaturePointer = 0;
        uint64_t functionPointer = 0;

        if (!ReadGuestPointer(memory, entry, namePointer) ||
            !ReadGuestPointer(memory, entry + GUEST_POINTER_BYTES, signaturePointer) ||
            !ReadGuestPointer(memory, entry + 2 * GUEST_POINTER_BYTES, functionPointer))
        {
            registers.general[0] = (uint64_t)(uint32_t)JNI_ERR;
            return AbiBridge::Result::Ok;
        }

        JniBridge::RegisteredNative native;
        if (!ReadGuestString(memory, namePointer, native.methodName) ||
            !ReadGuestString(memory, signaturePointer, native.descriptor))
        {
            registers.general[0] = (uint64_t)(uint32_t)JNI_ERR;
            return AbiBridge::Result::Ok;
        }

        native.guestFunction = (GuestState::GuestAddress)functionPointer;
        native.declaringClass = (void*)declaringClass;

        state->registeredNatives.push_back(std::move(native));
    }

    std::vector<JNINativeMethod> hostMethods;
    uint64_t first = state->registeredNatives.size() - (uint64_t)count;

    for (int32_t index = 0; index < count; ++index)
    {
        const JniBridge::RegisteredNative& native = state->registeredNatives[(size_t)(first + (uint64_t)index)];

        LlvmBackend::JniThunkRequest request;
        request.descriptor = native.descriptor.c_str();
        request.guestFunction = (uint64_t)native.guestFunction;
        request.enterGuest = &AclJniEnterGuest;
        request.context = state;

        LlvmBackend::CompiledFunction thunk;
        LlvmBackend::Result emitted = LlvmBackend::EmitJniThunk(state->backend, request, thunk);

        if (emitted != LlvmBackend::Result::Ok)
        {
            fprintf(stderr, "[jnibridge] cannot build a host thunk for %s%s: %s\n", native.methodName.c_str(), native.descriptor.c_str(),
                    LlvmBackend::ResultName(emitted));
            registers.general[0] = (uint64_t)(uint32_t)JNI_ERR;
            return AbiBridge::Result::Ok;
        }

        fprintf(stderr, "[jnibridge] RegisterNatives %s%s -> guest %016llx via host thunk %p\n", native.methodName.c_str(),
                native.descriptor.c_str(), (unsigned long long)native.guestFunction, thunk.hostEntryPoint);

        state->thunks.push_back(thunk);

        JNINativeMethod hostMethod;
        hostMethod.name = (char*)native.methodName.c_str();
        hostMethod.signature = (char*)native.descriptor.c_str();
        hostMethod.fnPtr = thunk.hostEntryPoint;
        hostMethods.push_back(hostMethod);
    }

    jint registered = env->RegisterNatives((jclass)declaringClass, hostMethods.data(), count);
    if (registered != JNI_OK)
    {
        fprintf(stderr, "[jnibridge] the host RegisterNatives refused %d method(s)\n", count);
        registers.general[0] = (uint64_t)(uint32_t)registered;
        return AbiBridge::Result::Ok;
    }

    state->statistics.registeredNatives += (uint64_t)count;
    registers.general[0] = (uint64_t)(uint32_t)JNI_OK;
    return AbiBridge::Result::Ok;
}

struct VarargCursor
{
    CallVariant variant = CallVariant::Standard;
    const GuestState::Registers* registers = nullptr;
    const GuestState::MemoryAccessor* memory = nullptr;
    int integerIndex = 3;
    int vectorIndex = 0;
    uint64_t stackCursor = 0;
    uint64_t arrayCursor = 0;
    uint64_t vaStack = 0;
    uint64_t vaGeneralTop = 0;
    uint64_t vaVectorTop = 0;
    int32_t vaGeneralOffset = 0;
    int32_t vaVectorOffset = 0;
    bool faulted = false;
};

static uint64_t ReadGuestWord(VarargCursor& cursor, uint64_t address)
{
    uint64_t value = 0;
    if (!cursor.memory->Read(cursor.memory->context, (GuestState::GuestAddress)address, &value, sizeof(value))) cursor.faulted = true;

    return value;
}

static bool LoadGuestVaList(VarargCursor& cursor, uint64_t address)
{
    uint64_t stack = ReadGuestWord(cursor, address);
    uint64_t generalTop = ReadGuestWord(cursor, address + 8);
    uint64_t vectorTop = ReadGuestWord(cursor, address + 16);

    uint64_t offsets = ReadGuestWord(cursor, address + 24);
    if (cursor.faulted) return false;

    cursor.vaStack = stack;
    cursor.vaGeneralTop = generalTop;
    cursor.vaVectorTop = vectorTop;
    cursor.vaGeneralOffset = (int32_t)(uint32_t)offsets;
    cursor.vaVectorOffset = (int32_t)(uint32_t)(offsets >> 32);

    return true;
}

static uint64_t NextStackSlot(VarargCursor& cursor)
{
    uint64_t value = ReadGuestWord(cursor, cursor.stackCursor);
    cursor.stackCursor += sizeof(uint64_t);
    return value;
}

static uint64_t NextInteger(VarargCursor& cursor)
{
    if (cursor.variant == CallVariant::Array)
    {
        uint64_t value = ReadGuestWord(cursor, cursor.arrayCursor);
        cursor.arrayCursor += sizeof(uint64_t);
        return value;
    }

    if (cursor.variant == CallVariant::VaList)
    {
        if (cursor.vaGeneralOffset < 0)
        {
            uint64_t value = ReadGuestWord(cursor, cursor.vaGeneralTop + (uint64_t)(int64_t)cursor.vaGeneralOffset);
            cursor.vaGeneralOffset += 8;
            return value;
        }

        uint64_t value = ReadGuestWord(cursor, cursor.vaStack);
        cursor.vaStack += sizeof(uint64_t);
        return value;
    }

    if (cursor.integerIndex < GUEST_INTEGER_ARGUMENT_SLOTS)
    {
        uint64_t value = cursor.registers->general[cursor.integerIndex];
        cursor.integerIndex += 1;
        return value;
    }

    return NextStackSlot(cursor);
}

static uint64_t NextVector(VarargCursor& cursor)
{
    if (cursor.variant == CallVariant::Array)
    {
        uint64_t value = ReadGuestWord(cursor, cursor.arrayCursor);
        cursor.arrayCursor += sizeof(uint64_t);
        return value;
    }

    if (cursor.variant == CallVariant::VaList)
    {
        if (cursor.vaVectorOffset < 0)
        {
            uint64_t value = ReadGuestWord(cursor, cursor.vaVectorTop + (uint64_t)(int64_t)cursor.vaVectorOffset);
            cursor.vaVectorOffset += 16;
            return value;
        }

        uint64_t value = ReadGuestWord(cursor, cursor.vaStack);
        cursor.vaStack += sizeof(uint64_t);
        return value;
    }

    if (cursor.vectorIndex < GUEST_VECTOR_ARGUMENT_SLOTS)
    {
        uint64_t value = cursor.registers->vector[cursor.vectorIndex].low;
        cursor.vectorIndex += 1;
        return value;
    }

    return NextStackSlot(cursor);
}

static bool BuildCallArguments(const std::vector<char>& letters, VarargCursor& cursor, std::vector<jvalue>& values)
{
    values.clear();

    for (char letter : letters)
    {
        jvalue value;
        memset(&value, 0, sizeof(value));

        switch (letter)
        {
            case 'Z': value.z = (jboolean)(NextInteger(cursor) & 1); break;
            case 'B': value.b = (jbyte)NextInteger(cursor); break;
            case 'C': value.c = (jchar)NextInteger(cursor); break;
            case 'S': value.s = (jshort)NextInteger(cursor); break;
            case 'I': value.i = (jint)(uint32_t)NextInteger(cursor); break;
            case 'J': value.j = (jlong)NextInteger(cursor); break;
            case 'L':
            case '[': value.l = (jobject)NextInteger(cursor); break;

            case 'F':
            {
                if (cursor.variant == CallVariant::Array)
                {
                    uint32_t bits = (uint32_t)NextVector(cursor);
                    memcpy(&value.f, &bits, sizeof(value.f));
                    break;
                }

                uint64_t bits = NextVector(cursor);
                double promoted;
                memcpy(&promoted, &bits, sizeof(promoted));
                value.f = (jfloat)promoted;
                break;
            }

            case 'D':
            {
                uint64_t bits = NextVector(cursor);
                memcpy(&value.d, &bits, sizeof(value.d));
                break;
            }

            default: return false;
        }

        values.push_back(value);
    }

    return !cursor.faulted;
}

static bool MethodDescriptorFor(JniBridgeState* state, JNIEnv* env, jclass owner, jmethodID method, bool isStatic, std::string& descriptor);

static AbiBridge::Result NewObjectSlot(JniBridgeState* state, JNIEnv* env, CallVariant variant, GuestState::Registers& registers,
                                       const GuestState::MemoryAccessor& memory)
{
    jclass owner = (jclass)registers.general[1];
    jmethodID method = (jmethodID)registers.general[2];

    std::string descriptor;
    if (!MethodDescriptorFor(state, env, owner, method, false, descriptor))
    {
        fprintf(stderr, "[jnibridge] NewObject used jmethodID %p, whose descriptor could not be recovered by reflection, so its "
                        "arguments cannot be marshalled\n",
                (void*)method);
        return AbiBridge::Result::MalformedArguments;
    }

    std::vector<char> letters;
    char returnLetter = 'V';
    if (!SplitDescriptor(descriptor.c_str(), letters, returnLetter)) return AbiBridge::Result::MalformedArguments;

    VarargCursor cursor;
    cursor.variant = variant;
    cursor.registers = &registers;
    cursor.memory = &memory;
    cursor.stackCursor = registers.stackPointer;

    if (variant == CallVariant::Array) cursor.arrayCursor = registers.general[3];
    if (variant == CallVariant::VaList && !LoadGuestVaList(cursor, registers.general[3])) return AbiBridge::Result::MalformedArguments;

    std::vector<jvalue> values;
    if (!BuildCallArguments(letters, cursor, values)) return AbiBridge::Result::MalformedArguments;

    const jvalue* arguments = values.empty() ? nullptr : values.data();
    registers.general[0] = (uint64_t)env->NewObjectA(owner, method, arguments);
    return AbiBridge::Result::Ok;
}

static bool AppendTypeDescriptor(JNIEnv* env, jclass type, std::string& descriptor)
{
    if (!type) return false;

    jclass classClass = env->FindClass("java/lang/Class");
    if (!classClass) return false;

    jmethodID getName = env->GetMethodID(classClass, "getName", "()Ljava/lang/String;");
    jmethodID isPrimitive = env->GetMethodID(classClass, "isPrimitive", "()Z");
    jmethodID isArray = env->GetMethodID(classClass, "isArray", "()Z");
    env->DeleteLocalRef(classClass);
    if (!getName || !isPrimitive || !isArray) return false;

    jstring nameObject = (jstring)env->CallObjectMethod(type, getName);
    if (!nameObject || env->ExceptionCheck()) return false;

    const char* chars = env->GetStringUTFChars(nameObject, nullptr);
    std::string name = chars ? chars : "";
    if (chars) env->ReleaseStringUTFChars(nameObject, chars);
    env->DeleteLocalRef(nameObject);

    for (char& character : name)
    {
        if (character == '.') character = '/';
    }

    if (env->CallBooleanMethod(type, isPrimitive))
    {
        static const struct
        {
            const char* name;
            char letter;
        } PRIMITIVES[] = {{"boolean", 'Z'}, {"byte", 'B'}, {"char", 'C'}, {"short", 'S'}, {"int", 'I'},
                          {"long", 'J'},    {"float", 'F'}, {"double", 'D'}, {"void", 'V'}};

        for (const auto& primitive : PRIMITIVES)
        {
            if (name != primitive.name) continue;

            descriptor.push_back(primitive.letter);
            return true;
        }

        return false;
    }

    if (env->CallBooleanMethod(type, isArray)) descriptor += name;
    else descriptor += "L" + name + ";";

    return !env->ExceptionCheck();
}

static bool DescribeMethodByReflection(JNIEnv* env, jclass owner, jmethodID method, bool isStatic, std::string& descriptor)
{
    descriptor.clear();
    if (!owner || !method) return false;

    jobject reflected = env->ToReflectedMethod(owner, method, isStatic ? JNI_TRUE : JNI_FALSE);
    if (!reflected || env->ExceptionCheck())
    {
        env->ExceptionClear();
        return false;
    }

    jclass reflectedClass = env->GetObjectClass(reflected);
    jmethodID getParameterTypes = env->GetMethodID(reflectedClass, "getParameterTypes", "()[Ljava/lang/Class;");
    jmethodID getReturnType = env->GetMethodID(reflectedClass, "getReturnType", "()Ljava/lang/Class;");
    env->ExceptionClear();

    bool described = false;
    jobjectArray parameters = getParameterTypes ? (jobjectArray)env->CallObjectMethod(reflected, getParameterTypes) : nullptr;

    if (parameters && !env->ExceptionCheck())
    {
        descriptor = "(";
        described = true;

        jsize count = env->GetArrayLength(parameters);
        for (jsize index = 0; index < count && described; index++)
        {
            jclass parameter = (jclass)env->GetObjectArrayElement(parameters, index);
            described = AppendTypeDescriptor(env, parameter, descriptor);
            env->DeleteLocalRef(parameter);
        }

        descriptor += ")";

        if (described && getReturnType)
        {
            jclass returned = (jclass)env->CallObjectMethod(reflected, getReturnType);
            described = AppendTypeDescriptor(env, returned, descriptor);
            if (returned) env->DeleteLocalRef(returned);
        }
        else if (described)
        {
            descriptor.push_back('V');
        }
    }

    if (env->ExceptionCheck()) described = false;
    env->ExceptionClear();

    if (parameters) env->DeleteLocalRef(parameters);
    env->DeleteLocalRef(reflectedClass);
    env->DeleteLocalRef(reflected);

    if (!described) descriptor.clear();
    return described;
}

static bool MethodDescriptorFor(JniBridgeState* state, JNIEnv* env, jclass owner, jmethodID method, bool isStatic, std::string& descriptor)
{
    {
        std::lock_guard<std::recursive_mutex> guard(state->mutex);
        auto found = state->methodDescriptors.find((const void*)method);
        if (found != state->methodDescriptors.end())
        {
            descriptor = found->second;
            return true;
        }
    }

    if (!DescribeMethodByReflection(env, owner, method, isStatic, descriptor)) return false;

    std::lock_guard<std::recursive_mutex> guard(state->mutex);
    state->methodDescriptors[(const void*)method] = descriptor;
    return true;
}

struct CallOperands
{
    CallTarget target;
    jobject receiver;
    jclass owner;
    jmethodID method;
    const jvalue* arguments;
};

static void InvokeVoid(JNIEnv* env, const CallOperands& call)
{
    if (call.target == CallTarget::Static) env->CallStaticVoidMethodA(call.owner, call.method, call.arguments);
    else if (call.target == CallTarget::Nonvirtual) env->CallNonvirtualVoidMethodA(call.receiver, call.owner, call.method, call.arguments);
    else env->CallVoidMethodA(call.receiver, call.method, call.arguments);
}

static jobject InvokeObject(JNIEnv* env, const CallOperands& call)
{
    if (call.target == CallTarget::Static) return env->CallStaticObjectMethodA(call.owner, call.method, call.arguments);
    if (call.target == CallTarget::Nonvirtual) return env->CallNonvirtualObjectMethodA(call.receiver, call.owner, call.method, call.arguments);
    return env->CallObjectMethodA(call.receiver, call.method, call.arguments);
}

static jboolean InvokeBoolean(JNIEnv* env, const CallOperands& call)
{
    if (call.target == CallTarget::Static) return env->CallStaticBooleanMethodA(call.owner, call.method, call.arguments);
    if (call.target == CallTarget::Nonvirtual) return env->CallNonvirtualBooleanMethodA(call.receiver, call.owner, call.method, call.arguments);
    return env->CallBooleanMethodA(call.receiver, call.method, call.arguments);
}

static jbyte InvokeByte(JNIEnv* env, const CallOperands& call)
{
    if (call.target == CallTarget::Static) return env->CallStaticByteMethodA(call.owner, call.method, call.arguments);
    if (call.target == CallTarget::Nonvirtual) return env->CallNonvirtualByteMethodA(call.receiver, call.owner, call.method, call.arguments);
    return env->CallByteMethodA(call.receiver, call.method, call.arguments);
}

static jchar InvokeChar(JNIEnv* env, const CallOperands& call)
{
    if (call.target == CallTarget::Static) return env->CallStaticCharMethodA(call.owner, call.method, call.arguments);
    if (call.target == CallTarget::Nonvirtual) return env->CallNonvirtualCharMethodA(call.receiver, call.owner, call.method, call.arguments);
    return env->CallCharMethodA(call.receiver, call.method, call.arguments);
}

static jshort InvokeShort(JNIEnv* env, const CallOperands& call)
{
    if (call.target == CallTarget::Static) return env->CallStaticShortMethodA(call.owner, call.method, call.arguments);
    if (call.target == CallTarget::Nonvirtual) return env->CallNonvirtualShortMethodA(call.receiver, call.owner, call.method, call.arguments);
    return env->CallShortMethodA(call.receiver, call.method, call.arguments);
}

static jint InvokeInt(JNIEnv* env, const CallOperands& call)
{
    if (call.target == CallTarget::Static) return env->CallStaticIntMethodA(call.owner, call.method, call.arguments);
    if (call.target == CallTarget::Nonvirtual) return env->CallNonvirtualIntMethodA(call.receiver, call.owner, call.method, call.arguments);
    return env->CallIntMethodA(call.receiver, call.method, call.arguments);
}

static jlong InvokeLong(JNIEnv* env, const CallOperands& call)
{
    if (call.target == CallTarget::Static) return env->CallStaticLongMethodA(call.owner, call.method, call.arguments);
    if (call.target == CallTarget::Nonvirtual) return env->CallNonvirtualLongMethodA(call.receiver, call.owner, call.method, call.arguments);
    return env->CallLongMethodA(call.receiver, call.method, call.arguments);
}

static jfloat InvokeFloat(JNIEnv* env, const CallOperands& call)
{
    if (call.target == CallTarget::Static) return env->CallStaticFloatMethodA(call.owner, call.method, call.arguments);
    if (call.target == CallTarget::Nonvirtual) return env->CallNonvirtualFloatMethodA(call.receiver, call.owner, call.method, call.arguments);
    return env->CallFloatMethodA(call.receiver, call.method, call.arguments);
}

static jdouble InvokeDouble(JNIEnv* env, const CallOperands& call)
{
    if (call.target == CallTarget::Static) return env->CallStaticDoubleMethodA(call.owner, call.method, call.arguments);
    if (call.target == CallTarget::Nonvirtual) return env->CallNonvirtualDoubleMethodA(call.receiver, call.owner, call.method, call.arguments);
    return env->CallDoubleMethodA(call.receiver, call.method, call.arguments);
}

static AbiBridge::Result CallMethodSlot(JniBridgeState* state, JNIEnv* env, CallTarget target, CallVariant variant,
                                        GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    bool nonvirtual = target == CallTarget::Nonvirtual;
    bool isStatic = target == CallTarget::Static;
    int methodRegister = nonvirtual ? 3 : 2;

    jobject receiver = (jobject)registers.general[1];
    jmethodID method = (jmethodID)registers.general[methodRegister];

    jclass lookupOwner = nullptr;
    bool ownsLookupOwner = false;

    if (isStatic) lookupOwner = (jclass)receiver;
    else if (nonvirtual) lookupOwner = (jclass)registers.general[2];

    std::string descriptor;
    bool known = false;
    {
        std::lock_guard<std::recursive_mutex> guard(state->mutex);
        auto cached = state->methodDescriptors.find((const void*)method);
        if (cached != state->methodDescriptors.end())
        {
            descriptor = cached->second;
            known = true;
        }
    }

    if (!known)
    {
        if (!lookupOwner && receiver)
        {
            lookupOwner = env->GetObjectClass(receiver);
            ownsLookupOwner = true;
        }

        known = MethodDescriptorFor(state, env, lookupOwner, method, isStatic, descriptor);
        if (ownsLookupOwner) env->DeleteLocalRef(lookupOwner);
    }

    if (!known)
    {
        fprintf(stderr, "[jnibridge] a Call<Type>Method used jmethodID %p, whose descriptor could not be recovered by reflection, so "
                        "its arguments cannot be marshalled\n",
                (void*)method);
        return AbiBridge::Result::MalformedArguments;
    }

    if (state->traceCalls)
    {
        std::lock_guard<std::recursive_mutex> guard(state->mutex);
        auto named = state->methodNames.find((const void*)method);
        const char* kind = isStatic ? "static " : (nonvirtual ? "nonvirtual " : "");
        fprintf(stderr, "[jnicall] %s%s%s\n", kind, named == state->methodNames.end() ? "?" : named->second.c_str(), descriptor.c_str());
        fflush(stderr);
    }

    std::vector<char> letters;
    char returnLetter = 'V';
    if (!SplitDescriptor(descriptor.c_str(), letters, returnLetter)) return AbiBridge::Result::MalformedArguments;

    int argumentRegister = methodRegister + 1;

    VarargCursor cursor;
    cursor.variant = variant;
    cursor.registers = &registers;
    cursor.memory = &memory;
    cursor.stackCursor = registers.stackPointer;
    cursor.integerIndex = argumentRegister;

    if (variant == CallVariant::Array) cursor.arrayCursor = registers.general[argumentRegister];
    if (variant == CallVariant::VaList && !LoadGuestVaList(cursor, registers.general[argumentRegister]))
    {
        return AbiBridge::Result::MalformedArguments;
    }

    std::vector<jvalue> values;
    if (!BuildCallArguments(letters, cursor, values)) return AbiBridge::Result::MalformedArguments;

    CallOperands call;
    call.target = target;
    call.receiver = receiver;
    call.owner = nonvirtual ? (jclass)registers.general[2] : (jclass)receiver;
    call.method = method;
    call.arguments = values.empty() ? nullptr : values.data();

    switch (returnLetter)
    {
        case 'V': InvokeVoid(env, call); return AbiBridge::Result::Ok;

        case 'L':
        case '[': registers.general[0] = (uint64_t)InvokeObject(env, call); return AbiBridge::Result::Ok;

        case 'Z': registers.general[0] = InvokeBoolean(env, call) ? 1 : 0; return AbiBridge::Result::Ok;
        case 'B': registers.general[0] = (uint64_t)(uint32_t)InvokeByte(env, call); return AbiBridge::Result::Ok;
        case 'C': registers.general[0] = (uint64_t)(uint32_t)InvokeChar(env, call); return AbiBridge::Result::Ok;
        case 'S': registers.general[0] = (uint64_t)(uint32_t)InvokeShort(env, call); return AbiBridge::Result::Ok;
        case 'I': registers.general[0] = (uint64_t)(uint32_t)InvokeInt(env, call); return AbiBridge::Result::Ok;
        case 'J': registers.general[0] = (uint64_t)InvokeLong(env, call); return AbiBridge::Result::Ok;

        case 'F':
        {
            float returned = InvokeFloat(env, call);

            uint32_t bits = 0;
            memcpy(&bits, &returned, sizeof(bits));
            registers.vector[0].low = bits;
            registers.vector[0].high = 0;
            return AbiBridge::Result::Ok;
        }

        case 'D':
        {
            double returned = InvokeDouble(env, call);

            uint64_t bits = 0;
            memcpy(&bits, &returned, sizeof(bits));
            registers.vector[0].low = bits;
            registers.vector[0].high = 0;
            return AbiBridge::Result::Ok;
        }

        default: break;
    }

    return AbiBridge::Result::MalformedArguments;
}

static AbiBridge::Result NativeSlotHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    const SlotBinding* binding = (const SlotBinding*)context;
    JniBridgeState* state = binding->state;

    if (!state || state->retired.load(std::memory_order_acquire))
    {
        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    {
        std::lock_guard<std::recursive_mutex> guard(state->mutex);
        state->statistics.slotCalls += 1;
    }

    if (registers.general[0] != (uint64_t)state->guestEnv)
    {
        fprintf(stderr, "[jnibridge] slot %d was entered with JNIEnv* %016llx, which is not the guest-visible environment %016llx\n",
                binding->slot, (unsigned long long)registers.general[0], (unsigned long long)state->guestEnv);
        return AbiBridge::Result::MalformedArguments;
    }

    JNIEnv* env = HostEnv(state);
    if (!env) return AbiBridge::Result::GuestFaulted;

    switch (binding->slot)
    {
        case SLOT_GET_VERSION:
        {
            registers.general[0] = (uint64_t)(uint32_t)env->GetVersion();
            return AbiBridge::Result::Ok;
        }

        case SLOT_FIND_CLASS:
        {
            std::string name;
            if (!ReadGuestString(memory, registers.general[1], name)) return AbiBridge::Result::MalformedArguments;

            jclass found = env->FindClass(name.c_str());
            if (!found) found = FindClassThroughLoader(env, name);

            if (!found)
            {
                fprintf(stderr, "[jnibridge] the guest asked for class %s, which the host could not find\n", name.c_str());
            }

            registers.general[0] = (uint64_t)found;
            return AbiBridge::Result::Ok;
        }

        case SLOT_GET_OBJECT_CLASS:
        {
            registers.general[0] = (uint64_t)env->GetObjectClass((jobject)registers.general[1]);
            return AbiBridge::Result::Ok;
        }

        case SLOT_EXCEPTION_OCCURRED:
        {
            registers.general[0] = (uint64_t)env->ExceptionOccurred();
            return AbiBridge::Result::Ok;
        }

        case SLOT_EXCEPTION_DESCRIBE:
        {
            env->ExceptionDescribe();
            return AbiBridge::Result::Ok;
        }

        case SLOT_EXCEPTION_CLEAR:
        {
            env->ExceptionClear();
            return AbiBridge::Result::Ok;
        }

        case SLOT_EXCEPTION_CHECK:
        {
            registers.general[0] = env->ExceptionCheck() ? 1 : 0;
            return AbiBridge::Result::Ok;
        }

        case SLOT_NEW_GLOBAL_REF:
        {
            registers.general[0] = (uint64_t)env->NewGlobalRef((jobject)registers.general[1]);
            return AbiBridge::Result::Ok;
        }

        case SLOT_DELETE_GLOBAL_REF:
        {
            env->DeleteGlobalRef((jobject)registers.general[1]);
            return AbiBridge::Result::Ok;
        }

        case SLOT_DELETE_LOCAL_REF:
        {
            env->DeleteLocalRef((jobject)registers.general[1]);
            return AbiBridge::Result::Ok;
        }

        case SLOT_GET_METHOD_ID:
        case SLOT_GET_STATIC_METHOD_ID:
        {
            std::string name;
            std::string descriptor;
            if (!ReadGuestString(memory, registers.general[2], name)) return AbiBridge::Result::MalformedArguments;
            if (!ReadGuestString(memory, registers.general[3], descriptor)) return AbiBridge::Result::MalformedArguments;

            jclass owner = (jclass)registers.general[1];
            jmethodID method = binding->slot == SLOT_GET_STATIC_METHOD_ID ? env->GetStaticMethodID(owner, name.c_str(), descriptor.c_str())
                                                                         : env->GetMethodID(owner, name.c_str(), descriptor.c_str());

            if (method)
            {
                std::lock_guard<std::recursive_mutex> guard(state->mutex);
                state->methodDescriptors[(const void*)method] = descriptor;
                state->methodNames[(const void*)method] = name;
            }

            registers.general[0] = (uint64_t)method;
            return AbiBridge::Result::Ok;
        }

        case SLOT_NEW_STRING_UTF:
        {
            std::string text;
            if (!ReadGuestString(memory, registers.general[1], text)) return AbiBridge::Result::MalformedArguments;

            if (state->traceCalls) fprintf(stderr, "[jnicall] NewStringUTF \"%s\"\n", text.c_str());

            registers.general[0] = (uint64_t)env->NewStringUTF(text.c_str());
            return AbiBridge::Result::Ok;
        }

        case SLOT_GET_STRING_UTF_LENGTH:
        {
            registers.general[0] = (uint64_t)(uint32_t)env->GetStringUTFLength((jstring)registers.general[1]);
            return AbiBridge::Result::Ok;
        }

        case SLOT_GET_STRING_UTF_CHARS:
        {
            jboolean copied = JNI_FALSE;
            const char* text = env->GetStringUTFChars((jstring)registers.general[1], &copied);

            uint64_t destination = registers.general[2];
            if (destination != 0 && !memory.Write(memory.context, (GuestState::GuestAddress)destination, &copied, sizeof(copied)))
                return AbiBridge::Result::MalformedArguments;

            if (state->traceCalls) fprintf(stderr, "[jnicall] GetStringUTFChars \"%s\"\n", text ? text : "(null)");

            registers.general[0] = (uint64_t)text;
            return AbiBridge::Result::Ok;
        }

        case SLOT_RELEASE_STRING_UTF_CHARS:
        {
            env->ReleaseStringUTFChars((jstring)registers.general[1], (const char*)registers.general[2]);
            return AbiBridge::Result::Ok;
        }

        case SLOT_REGISTER_NATIVES: return RegisterNativesSlot(state, env, registers, memory);

        case SLOT_GET_FIELD_ID:
        case SLOT_GET_STATIC_FIELD_ID:
        {
            std::string name;
            std::string descriptor;

            if (!ReadGuestString(memory, registers.general[2], name) ||
                !ReadGuestString(memory, registers.general[3], descriptor))
            {
                return AbiBridge::Result::MalformedArguments;
            }

            jclass owner = (jclass)registers.general[1];
            jfieldID field = binding->slot == SLOT_GET_FIELD_ID ? env->GetFieldID(owner, name.c_str(), descriptor.c_str())
                                                                : env->GetStaticFieldID(owner, name.c_str(), descriptor.c_str());

            if (!field)
            {
                fprintf(stderr, "[jnibridge] the guest asked for field %s%s, which the host could not find\n", name.c_str(),
                        descriptor.c_str());
            }

            registers.general[0] = (uint64_t)field;
            return AbiBridge::Result::Ok;
        }

        case SLOT_GET_OBJECT_FIELD:
            registers.general[0] = (uint64_t)env->GetObjectField((jobject)registers.general[1], (jfieldID)registers.general[2]);
            return AbiBridge::Result::Ok;

        case SLOT_SET_OBJECT_FIELD:
            env->SetObjectField((jobject)registers.general[1], (jfieldID)registers.general[2], (jobject)registers.general[3]);
            return AbiBridge::Result::Ok;

        case SLOT_GET_STATIC_OBJECT_FIELD:
            registers.general[0] = (uint64_t)env->GetStaticObjectField((jclass)registers.general[1], (jfieldID)registers.general[2]);
            return AbiBridge::Result::Ok;

        case SLOT_SET_STATIC_OBJECT_FIELD:
            env->SetStaticObjectField((jclass)registers.general[1], (jfieldID)registers.general[2], (jobject)registers.general[3]);
            return AbiBridge::Result::Ok;

        case SLOT_GET_ARRAY_LENGTH:
            registers.general[0] = (uint64_t)(uint32_t)env->GetArrayLength((jarray)registers.general[1]);
            return AbiBridge::Result::Ok;

        case SLOT_GET_OBJECT_ARRAY_ELEMENT:
            registers.general[0] =
                (uint64_t)env->GetObjectArrayElement((jobjectArray)registers.general[1], (jsize)registers.general[2]);
            return AbiBridge::Result::Ok;

        case SLOT_SET_OBJECT_ARRAY_ELEMENT:
            env->SetObjectArrayElement((jobjectArray)registers.general[1], (jsize)registers.general[2],
                                       (jobject)registers.general[3]);
            return AbiBridge::Result::Ok;

        case SLOT_NEW_OBJECT_ARRAY:
            registers.general[0] = (uint64_t)env->NewObjectArray((jsize)registers.general[1], (jclass)registers.general[2],
                                                                 (jobject)registers.general[3]);
            return AbiBridge::Result::Ok;

        case SLOT_IS_SAME_OBJECT:
            registers.general[0] = env->IsSameObject((jobject)registers.general[1], (jobject)registers.general[2]) ? 1 : 0;
            return AbiBridge::Result::Ok;

        case SLOT_IS_INSTANCE_OF:
            registers.general[0] = env->IsInstanceOf((jobject)registers.general[1], (jclass)registers.general[2]) ? 1 : 0;
            return AbiBridge::Result::Ok;

        case SLOT_GET_SUPERCLASS:
            registers.general[0] = (uint64_t)env->GetSuperclass((jclass)registers.general[1]);
            return AbiBridge::Result::Ok;

        case SLOT_FROM_REFLECTED_METHOD:
            registers.general[0] = (uint64_t)env->FromReflectedMethod((jobject)registers.general[1]);
            return AbiBridge::Result::Ok;

        case SLOT_FROM_REFLECTED_FIELD:
            registers.general[0] = (uint64_t)env->FromReflectedField((jobject)registers.general[1]);
            return AbiBridge::Result::Ok;

        case SLOT_TO_REFLECTED_METHOD:
            registers.general[0] = (uint64_t)env->ToReflectedMethod((jclass)registers.general[1], (jmethodID)registers.general[2],
                                                                    (jboolean)registers.general[3]);
            return AbiBridge::Result::Ok;

        case SLOT_TO_REFLECTED_FIELD:
            registers.general[0] = (uint64_t)env->ToReflectedField((jclass)registers.general[1], (jfieldID)registers.general[2],
                                                                   (jboolean)registers.general[3]);
            return AbiBridge::Result::Ok;

        case SLOT_IS_ASSIGNABLE_FROM:
            registers.general[0] = env->IsAssignableFrom((jclass)registers.general[1], (jclass)registers.general[2]) ? 1 : 0;
            return AbiBridge::Result::Ok;

        case SLOT_NEW_LOCAL_REF:
            registers.general[0] = (uint64_t)env->NewLocalRef((jobject)registers.general[1]);
            return AbiBridge::Result::Ok;

        case SLOT_NEW_WEAK_GLOBAL_REF:
            registers.general[0] = (uint64_t)env->NewWeakGlobalRef((jobject)registers.general[1]);
            return AbiBridge::Result::Ok;

        case SLOT_DELETE_WEAK_GLOBAL_REF:
            env->DeleteWeakGlobalRef((jweak)registers.general[1]);
            return AbiBridge::Result::Ok;

        case SLOT_PUSH_LOCAL_FRAME:
            registers.general[0] = (uint64_t)(uint32_t)env->PushLocalFrame((jint)registers.general[1]);
            return AbiBridge::Result::Ok;

        case SLOT_POP_LOCAL_FRAME:
            registers.general[0] = (uint64_t)env->PopLocalFrame((jobject)registers.general[1]);
            return AbiBridge::Result::Ok;

        case SLOT_ENSURE_LOCAL_CAPACITY:
            registers.general[0] = (uint64_t)(uint32_t)env->EnsureLocalCapacity((jint)registers.general[1]);
            return AbiBridge::Result::Ok;

        case SLOT_MONITOR_ENTER:
            registers.general[0] = (uint64_t)(uint32_t)env->MonitorEnter((jobject)registers.general[1]);
            return AbiBridge::Result::Ok;

        case SLOT_MONITOR_EXIT:
            registers.general[0] = (uint64_t)(uint32_t)env->MonitorExit((jobject)registers.general[1]);
            return AbiBridge::Result::Ok;

        case SLOT_THROW:
            registers.general[0] = (uint64_t)(uint32_t)env->Throw((jthrowable)registers.general[1]);
            return AbiBridge::Result::Ok;

        case SLOT_THROW_NEW:
        {
            std::string message;
            if (registers.general[2] != 0 && !ReadGuestString(memory, registers.general[2], message))
            {
                return AbiBridge::Result::MalformedArguments;
            }

            registers.general[0] =
                (uint64_t)(uint32_t)env->ThrowNew((jclass)registers.general[1], registers.general[2] ? message.c_str() : nullptr);
            return AbiBridge::Result::Ok;
        }

        case SLOT_GET_STRING_LENGTH:
            registers.general[0] = (uint64_t)(uint32_t)env->GetStringLength((jstring)registers.general[1]);
            return AbiBridge::Result::Ok;

        case SLOT_GET_STRING_UTF_REGION:
        {
            jstring text = (jstring)registers.general[1];
            jsize start = (jsize)registers.general[2];
            jsize count = (jsize)registers.general[3];
            uint64_t out = registers.general[4];

            if (count < 0 || out == 0) return AbiBridge::Result::MalformedArguments;

            std::vector<char> buffer((size_t)count * 4 + 1, '\0');
            env->GetStringUTFRegion(text, start, count, buffer.data());

            if (!memory.Write(memory.context, (GuestState::GuestAddress)out, buffer.data(), strlen(buffer.data()) + 1))
            {
                return AbiBridge::Result::MalformedArguments;
            }

            return AbiBridge::Result::Ok;
        }

        case SLOT_NEW_STRING:
        {
            uint64_t source = registers.general[1];
            jsize length = (jsize)(int32_t)(uint32_t)registers.general[2];
            if (length < 0 || (uint64_t)length > MAXIMUM_GUEST_STRING_CHARS || (length > 0 && source == 0))
            {
                return AbiBridge::Result::MalformedArguments;
            }

            std::vector<jchar> characters((size_t)length);
            if (length > 0 &&
                !memory.Read(memory.context, (GuestState::GuestAddress)source, characters.data(), (uint64_t)length * sizeof(jchar)))
            {
                return AbiBridge::Result::MalformedArguments;
            }

            registers.general[0] = (uint64_t)env->NewString(characters.data(), length);
            return AbiBridge::Result::Ok;
        }

        case SLOT_GET_STRING_CHARS:
        case SLOT_GET_STRING_CRITICAL:
        {
            jboolean copied = JNI_FALSE;
            jstring text = (jstring)registers.general[1];
            const jchar* characters = binding->slot == SLOT_GET_STRING_CHARS ? env->GetStringChars(text, &copied)
                                                                              : env->GetStringCritical(text, &copied);

            uint64_t destination = registers.general[2];
            if (destination != 0 && !memory.Write(memory.context, (GuestState::GuestAddress)destination, &copied, sizeof(copied)))
                return AbiBridge::Result::MalformedArguments;

            registers.general[0] = (uint64_t)characters;
            return AbiBridge::Result::Ok;
        }

        case SLOT_RELEASE_STRING_CHARS:
            env->ReleaseStringChars((jstring)registers.general[1], (const jchar*)registers.general[2]);
            return AbiBridge::Result::Ok;

        case SLOT_RELEASE_STRING_CRITICAL:
            env->ReleaseStringCritical((jstring)registers.general[1], (const jchar*)registers.general[2]);
            return AbiBridge::Result::Ok;

        case SLOT_GET_STRING_REGION:
        {
            jstring text = (jstring)registers.general[1];
            jsize start = (jsize)registers.general[2];
            jsize count = (jsize)registers.general[3];
            uint64_t out = registers.general[4];

            if (count < 0 || (count > 0 && out == 0)) return AbiBridge::Result::MalformedArguments;

            std::vector<jchar> buffer((size_t)count);
            env->GetStringRegion(text, start, count, buffer.data());
            if (env->ExceptionCheck()) return AbiBridge::Result::Ok;

            if (count > 0 && !memory.Write(memory.context, (GuestState::GuestAddress)out, buffer.data(), (uint64_t)count * sizeof(jchar)))
            {
                return AbiBridge::Result::MalformedArguments;
            }

            return AbiBridge::Result::Ok;
        }

        case SLOT_GET_PRIMITIVE_ARRAY_CRITICAL:
        {
            jboolean copied = JNI_FALSE;
            void* elements = env->GetPrimitiveArrayCritical((jarray)registers.general[1], &copied);

            uint64_t destination = registers.general[2];
            if (destination != 0 && !memory.Write(memory.context, (GuestState::GuestAddress)destination, &copied, sizeof(copied)))
                return AbiBridge::Result::MalformedArguments;

            registers.general[0] = (uint64_t)elements;
            return AbiBridge::Result::Ok;
        }

        case SLOT_RELEASE_PRIMITIVE_ARRAY_CRITICAL:
            env->ReleasePrimitiveArrayCritical((jarray)registers.general[1], (void*)registers.general[2],
                                               (jint)(int32_t)(uint32_t)registers.general[3]);
            return AbiBridge::Result::Ok;

        case SLOT_GET_OBJECT_REF_TYPE:
            registers.general[0] = (uint64_t)(uint32_t)env->GetObjectRefType((jobject)registers.general[1]);
            return AbiBridge::Result::Ok;

        case SLOT_UNREGISTER_NATIVES:
            registers.general[0] = (uint64_t)(uint32_t)env->UnregisterNatives((jclass)registers.general[1]);
            return AbiBridge::Result::Ok;

        case SLOT_DEFINE_CLASS:
        {
            std::string name;
            if (registers.general[1] != 0 && !ReadGuestString(memory, registers.general[1], name)) return AbiBridge::Result::MalformedArguments;

            jsize length = (jsize)(int32_t)(uint32_t)registers.general[4];
            if (length < 0 || (length > 0 && registers.general[3] == 0)) return AbiBridge::Result::MalformedArguments;

            std::vector<jbyte> bytes((size_t)length);
            if (length > 0 &&
                !memory.Read(memory.context, (GuestState::GuestAddress)registers.general[3], bytes.data(), (uint64_t)length))
            {
                return AbiBridge::Result::MalformedArguments;
            }

            registers.general[0] = (uint64_t)env->DefineClass(registers.general[1] ? name.c_str() : nullptr,
                                                               (jobject)registers.general[2], bytes.data(), length);
            return AbiBridge::Result::Ok;
        }

        case SLOT_FATAL_ERROR:
        {
            std::string message;
            if (registers.general[1] == 0 || !ReadGuestString(memory, registers.general[1], message)) message = "(no message)";

            fprintf(stderr, "[jnibridge] the guest raised a JNI fatal error: %s\n", message.c_str());
            fflush(stderr);
            env->FatalError(message.c_str());
            return AbiBridge::Result::Ok;
        }

        case SLOT_NEW_DIRECT_BYTE_BUFFER:
            registers.general[0] = (uint64_t)env->NewDirectByteBuffer((void*)registers.general[1], (jlong)registers.general[2]);
            return AbiBridge::Result::Ok;

        case SLOT_GET_DIRECT_BUFFER_ADDRESS:
            registers.general[0] = (uint64_t)env->GetDirectBufferAddress((jobject)registers.general[1]);
            return AbiBridge::Result::Ok;

        case SLOT_GET_DIRECT_BUFFER_CAPACITY:
            registers.general[0] = (uint64_t)env->GetDirectBufferCapacity((jobject)registers.general[1]);
            return AbiBridge::Result::Ok;

        case SLOT_GET_JAVA_VM:
        {
            uint64_t out = registers.general[1];
            if (out == 0) return AbiBridge::Result::MalformedArguments;
            if (!WriteGuestPointer(memory, out, (uint64_t)state->guestVm)) return AbiBridge::Result::MalformedArguments;

            registers.general[0] = 0;
            return AbiBridge::Result::Ok;
        }

        default: break;
    }

    for (const PrimitiveFieldSlot& field : PRIMITIVE_FIELD_SLOTS)
    {
        if (binding->slot == field.slot) return PrimitiveFieldAccess(env, field, registers);
    }

    for (const PrimitiveArraySlot& array : PRIMITIVE_ARRAY_SLOTS)
    {
        if (binding->slot == array.slot) return PrimitiveArrayAccess(env, array, registers, memory);
    }

    if (binding->slot == SLOT_NEW_OBJECT) return NewObjectSlot(state, env, CallVariant::Standard, registers, memory);
    if (binding->slot == SLOT_NEW_OBJECT + 1) return NewObjectSlot(state, env, CallVariant::VaList, registers, memory);
    if (binding->slot == SLOT_NEW_OBJECT + 2) return NewObjectSlot(state, env, CallVariant::Array, registers, memory);
    if (binding->slot == SLOT_ALLOC_OBJECT)
    {
        registers.general[0] = (uint64_t)env->AllocObject((jclass)registers.general[1]);
        return AbiBridge::Result::Ok;
    }

    for (const CallSlot& call : CALL_SLOTS)
    {
        if (binding->slot == call.slot) return CallMethodSlot(state, env, call.target, CallVariant::Standard, registers, memory);
        if (binding->slot == call.slot + 1) return CallMethodSlot(state, env, call.target, CallVariant::VaList, registers, memory);
        if (binding->slot == call.slot + 2) return CallMethodSlot(state, env, call.target, CallVariant::Array, registers, memory);
    }

    return UnimplementedSlot(state, binding);
}

static bool BindSlot(JniBridgeState* state, AbiBridge::Registry& registry, const GuestState::MemoryAccessor& memory, int slot,
                     bool invokeInterface)
{
    SlotBinding binding;
    binding.state = state;
    binding.slot = slot;
    binding.invokeInterface = invokeInterface;
    state->bindings.push_back(binding);

    char symbol[64];
    snprintf(symbol, sizeof(symbol), "jni:%s:%d", invokeInterface ? "vm" : "env", slot);

    AbiBridge::GuestCallHandler handler = invokeInterface ? &InvokeSlotHandler : &NativeSlotHandler;
    if (AbiBridge::RegisterHostHandler(registry, symbol, handler, &state->bindings.back()) != AbiBridge::Result::Ok) return false;

    GuestState::GuestAddress stub{};
    if (AbiBridge::LookupGuestStub(registry, symbol, stub) != AbiBridge::Result::Ok) return false;

    uint64_t table = invokeInterface ? (uint64_t)state->invokeTable : (uint64_t)state->nativeTable;
    return WriteGuestPointer(memory, table + (uint64_t)slot * GUEST_POINTER_BYTES, (uint64_t)stub);
}

JniBridge::Result JniBridge::Create(GuestRuntime::Instance& runtime, JavaVM* javaVm, Instance& bridge)
{
    if (!javaVm) return Result::NoJavaVm;

    JniBridgeState* state = new JniBridgeState();
    state->runtime = &runtime;
    state->javaVm = javaVm;
    state->traceCalls = Settings::Tracing("jnicalls");

    GuestRuntime::SetThreadExitHandler(runtime, &DetachExitingThread, state);

    LlvmBackend::Configuration backendConfiguration;
    if (LlvmBackend::Initialize(backendConfiguration, state->backend) != LlvmBackend::Result::Ok)
    {
        delete state;
        return Result::RegistrationFailed;
    }

    AbiBridge::Registry& registry = GuestRuntime::BridgeRegistry(runtime);
    const GuestState::MemoryAccessor& memory = GuestRuntime::Memory(runtime);

    struct Allocation
    {
        const char* symbolName;
        uint64_t bytes;
        GuestState::GuestAddress* destination;
    };

    const Allocation allocations[] = {
        {"acl:jni:native-table", (uint64_t)NATIVE_INTERFACE_SLOTS * GUEST_POINTER_BYTES, &state->nativeTable},
        {"acl:jni:invoke-table", (uint64_t)INVOKE_INTERFACE_SLOTS * GUEST_POINTER_BYTES, &state->invokeTable},
        {"acl:jni:env", GUEST_POINTER_BYTES, &state->guestEnv},
        {"acl:jni:vm", GUEST_POINTER_BYTES, &state->guestVm},
    };

    for (const Allocation& allocation : allocations)
    {
        if (GuestRuntime::RegisterDataObject(runtime, allocation.symbolName, allocation.bytes, *allocation.destination) !=
            GuestRuntime::Result::Ok)
        {
            delete state;
            return Result::OutOfGuestMemory;
        }
    }

    for (int slot = 0; slot < NATIVE_INTERFACE_SLOTS; ++slot)
    {
        if (!BindSlot(state, registry, memory, slot, false))
        {
            delete state;
            return Result::RegistrationFailed;
        }
    }

    for (int slot = 0; slot < INVOKE_INTERFACE_SLOTS; ++slot)
    {
        if (!BindSlot(state, registry, memory, slot, true))
        {
            delete state;
            return Result::RegistrationFailed;
        }
    }

    if (!WriteGuestPointer(memory, (uint64_t)state->guestEnv, (uint64_t)state->nativeTable) ||
        !WriteGuestPointer(memory, (uint64_t)state->guestVm, (uint64_t)state->invokeTable))
    {
        delete state;
        return Result::OutOfGuestMemory;
    }

    bridge.implementation = state;
    return Result::Ok;
}

void JniBridge::Destroy(Instance& bridge)
{
    JniBridgeState* state = Implementation(bridge);
    if (!state) return;

    RemoveLibraryLoader(bridge);

    state->retired.store(true, std::memory_order_release);
    bridge.implementation = nullptr;
}

static std::mutex loaderMutex;
static JniBridgeState* activeLoaderState = nullptr;
static std::string activeLoaderDirectory;

static bool IsMangledEscape(char following)
{
    return following == '0' || following == '1' || following == '2' || following == '3';
}

static std::string UnescapeMangledPart(const std::string& part)
{
    std::string plain;

    for (size_t index = 0; index < part.size(); ++index)
    {
        if (part[index] != '_' || index + 1 >= part.size())
        {
            plain.push_back(part[index]);
            continue;
        }

        char following = part[index + 1];

        if (following == '1') plain.push_back('_');
        else if (following == '2') plain.push_back(';');
        else if (following == '3') plain.push_back('[');
        else if (following == '0')
        {
            if (index + 5 >= part.size()) break;

            unsigned long codeUnit = strtoul(part.substr(index + 2, 4).c_str(), nullptr, 16);
            if (codeUnit < 0x80)
            {
                plain.push_back((char)codeUnit);
            }
            else if (codeUnit < 0x800)
            {
                plain.push_back((char)(0xc0 | (codeUnit >> 6)));
                plain.push_back((char)(0x80 | (codeUnit & 0x3f)));
            }
            else
            {
                plain.push_back((char)(0xe0 | (codeUnit >> 12)));
                plain.push_back((char)(0x80 | ((codeUnit >> 6) & 0x3f)));
                plain.push_back((char)(0x80 | (codeUnit & 0x3f)));
            }

            index += 4;
        }
        else
        {
            plain.push_back(part[index]);
            continue;
        }

        index += 1;
    }

    return plain;
}

static std::string MangleDescriptorArguments(const std::string& descriptor)
{
    size_t open = descriptor.find('(');
    size_t close = descriptor.find(')');
    if (open == std::string::npos || close == std::string::npos || close < open) return std::string();

    std::string mangled;

    for (size_t index = open + 1; index < close; ++index)
    {
        char character = descriptor[index];

        if (character == '/') mangled += "_";
        else if (character == '_') mangled += "_1";
        else if (character == ';') mangled += "_2";
        else if (character == '[') mangled += "_3";
        else mangled.push_back(character);
    }

    return mangled;
}

static bool SplitExportedNativeName(const std::string& symbol, std::string& className, std::string& methodName,
                                    std::string& mangledArguments)
{
    static const std::string prefix = "Java_";
    if (symbol.compare(0, prefix.size(), prefix) != 0) return false;

    std::string body = symbol.substr(prefix.size());
    mangledArguments.clear();

    for (size_t index = 0; index + 1 < body.size(); ++index)
    {
        if (body[index] != '_' || body[index + 1] != '_') continue;

        mangledArguments = body.substr(index + 2);
        body = body.substr(0, index);
        break;
    }

    std::vector<std::string> parts;
    std::string current;

    for (size_t index = 0; index < body.size(); ++index)
    {
        if (body[index] == '_' && (index + 1 >= body.size() || !IsMangledEscape(body[index + 1])))
        {
            parts.push_back(current);
            current.clear();
            continue;
        }

        current.push_back(body[index]);
        if (body[index] == '_') current.push_back(body[++index]);
    }

    parts.push_back(current);
    if (parts.size() < 2) return false;

    methodName = UnescapeMangledPart(parts.back());
    parts.pop_back();

    className.clear();
    for (size_t index = 0; index < parts.size(); ++index)
    {
        if (index != 0) className.push_back('/');
        className += UnescapeMangledPart(parts[index]);
    }

    return !className.empty() && !methodName.empty();
}

static bool DescribeNativeMethods(JNIEnv* env, const std::string& className, std::vector<std::pair<std::string, std::string>>& described)
{
    described.clear();

    jclass helper = env->FindClass("runtime/NativeSignatures");
    if (!helper)
    {
        env->ExceptionClear();
        return false;
    }

    jmethodID describe = env->GetStaticMethodID(helper, "describe", "(Ljava/lang/String;)[Ljava/lang/String;");
    if (!describe)
    {
        env->ExceptionClear();
        env->DeleteLocalRef(helper);
        return false;
    }

    jstring name = env->NewStringUTF(className.c_str());
    jobjectArray produced = (jobjectArray)env->CallStaticObjectMethod(helper, describe, name);

    env->DeleteLocalRef(name);
    env->DeleteLocalRef(helper);

    if (env->ExceptionCheck())
    {
        env->ExceptionClear();
        return false;
    }

    if (!produced) return false;

    jsize total = env->GetArrayLength(produced);

    for (jsize index = 0; index + 1 < total; index += 2)
    {
        jstring methodName = (jstring)env->GetObjectArrayElement(produced, index);
        jstring descriptor = (jstring)env->GetObjectArrayElement(produced, index + 1);

        const char* rawName = env->GetStringUTFChars(methodName, nullptr);
        const char* rawDescriptor = env->GetStringUTFChars(descriptor, nullptr);

        if (rawName && rawDescriptor) described.emplace_back(rawName, rawDescriptor);

        if (rawName) env->ReleaseStringUTFChars(methodName, rawName);
        if (rawDescriptor) env->ReleaseStringUTFChars(descriptor, rawDescriptor);

        env->DeleteLocalRef(methodName);
        env->DeleteLocalRef(descriptor);
    }

    env->DeleteLocalRef(produced);
    return true;
}

static void BindExportedNatives(JniBridgeState* state, uint32_t moduleId)
{
    const ElfLoader::Library* library = ElfLoader::FindLibrary(GuestRuntime::LibraryNamespace(*state->runtime), moduleId);
    if (!library) return;

    JNIEnv* env = HostEnv(state);
    if (!env) return;

    struct Candidate
    {
        std::string methodName;
        std::string mangledArguments;
        GuestState::GuestAddress guestFunction;
    };

    std::map<std::string, std::vector<Candidate>> byClass;

    for (const ElfLoader::Symbol& symbol : library->exports)
    {
        if (!symbol.isFunction || !symbol.isDefined) continue;

        std::string className;
        std::string methodName;
        std::string mangledArguments;

        if (!SplitExportedNativeName(symbol.name, className, methodName, mangledArguments)) continue;

        byClass[className].push_back({methodName, mangledArguments, symbol.guestAddress});
    }

    uint64_t bound = 0;

    for (const auto& group : byClass)
    {
        std::vector<std::pair<std::string, std::string>> declared;
        if (!DescribeNativeMethods(env, group.first, declared) || declared.empty()) continue;

        jclass owner = env->FindClass(group.first.c_str());
        if (!owner)
        {
            env->ExceptionClear();
            continue;
        }

        struct Binding
        {
            std::string methodName;
            std::string descriptor;
            void* hostEntry;
        };

        std::vector<Binding> bindings;

        for (const Candidate& candidate : group.second)
        {
            const std::string* descriptor = nullptr;
            uint64_t matches = 0;

            for (const auto& entry : declared)
            {
                if (entry.first != candidate.methodName) continue;

                matches += 1;
                if (!candidate.mangledArguments.empty() && MangleDescriptorArguments(entry.second) != candidate.mangledArguments)
                {
                    continue;
                }

                descriptor = &entry.second;
                if (!candidate.mangledArguments.empty()) break;
            }

            if (!descriptor) continue;
            if (candidate.mangledArguments.empty() && matches > 1) continue;

            LlvmBackend::JniThunkRequest request;
            request.descriptor = descriptor->c_str();
            request.guestFunction = (uint64_t)candidate.guestFunction;
            request.enterGuest = &AclJniEnterGuest;
            request.context = state;

            LlvmBackend::CompiledFunction thunk;
            if (LlvmBackend::EmitJniThunk(state->backend, request, thunk) != LlvmBackend::Result::Ok) continue;

            state->thunks.push_back(thunk);
            bindings.push_back({candidate.methodName, *descriptor, thunk.hostEntryPoint});
        }

        std::vector<JNINativeMethod> hostMethods;

        for (const Binding& binding : bindings)
        {
            JNINativeMethod hostMethod;
            hostMethod.name = (char*)binding.methodName.c_str();
            hostMethod.signature = (char*)binding.descriptor.c_str();
            hostMethod.fnPtr = binding.hostEntry;
            hostMethods.push_back(hostMethod);
        }

        if (!hostMethods.empty() && env->RegisterNatives(owner, hostMethods.data(), (jint)hostMethods.size()) == JNI_OK)
        {
            bound += hostMethods.size();
            state->statistics.registeredNatives += hostMethods.size();
        }
        else if (!hostMethods.empty())
        {
            env->ExceptionClear();
            fprintf(stderr, "[jnibridge] the host refused %zu exported native(s) of %s\n", hostMethods.size(), group.first.c_str());
        }

        env->DeleteLocalRef(owner);
    }

    if (bound != 0) fprintf(stderr, "[jnibridge] bound %llu exported native(s) by name\n", (unsigned long long)bound);
}

static bool LoadTranslateAndInitialize(JniBridgeState* state, const std::string& path, uint32_t& moduleId)
{
    moduleId = ElfLoader::INVALID_MODULE_ID;

    GuestRuntime::Result loaded = GuestRuntime::Load(*state->runtime, path, moduleId);
    if (loaded != GuestRuntime::Result::Ok)
    {
        fprintf(stderr, "[jnibridge] cannot load guest library %s: %s\n", path.c_str(), GuestRuntime::ResultName(loaded));
        return false;
    }

    {
        std::lock_guard<std::recursive_mutex> guard(state->mutex);
        state->statistics.guestLibrariesLoaded += 1;
    }

    if (state->translator && !state->translator(state->translatorContext, *state->runtime, moduleId, path.c_str()))
    {
        fprintf(stderr, "[jnibridge] cannot translate %s ahead of time\n", path.c_str());
        return false;
    }

    if (GuestRuntime::RunInitializers(*state->runtime, moduleId) != GuestRuntime::Result::Ok)
    {
        fprintf(stderr, "[jnibridge] the initializers of %s faulted\n", path.c_str());
        return false;
    }

    BindExportedNatives(state, moduleId);
    return true;
}

static std::mutex gLoaderHandleLock;
static constexpr uint64_t PLATFORM_HANDLE_BASE = 0x1000;

static std::unordered_map<uint64_t, uint32_t> gLoaderHandles;
static std::unordered_map<uint64_t, std::string> gPlatformHandles;
static std::string gLoaderError;

static AbiBridge::Result OpenLibraryHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    JniBridgeState* state = (JniBridgeState*)context;

    std::string requested;
    if (registers.general[0] != 0 && !ReadGuestString(memory, registers.general[0], requested))
    {
        return AbiBridge::Result::MalformedArguments;
    }

    if (requested.empty())
    {
        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    std::string leaf = requested;
    size_t separator = leaf.find_last_of('/');
    if (separator != std::string::npos) leaf = leaf.substr(separator + 1);

    AbiBridge::Registry& registry = GuestRuntime::BridgeRegistry(*state->runtime);
    if (AbiBridge::IsProxyLibrary(registry, leaf))
    {
        uint64_t handle = 0;
        {
            std::lock_guard<std::mutex> guard(gLoaderHandleLock);
            handle = PLATFORM_HANDLE_BASE + gPlatformHandles.size();
            gPlatformHandles[handle] = leaf;
            gLoaderError.clear();
        }

        fprintf(stderr, "[jnibridge] the guest opened the platform library %s as handle %llu\n", leaf.c_str(),
                (unsigned long long)handle);
        registers.general[0] = handle;
        return AbiBridge::Result::Ok;
    }

    std::string directory;
    {
        std::lock_guard<std::mutex> guard(loaderMutex);
        directory = activeLoaderDirectory;
    }

    if (directory.empty() || leaf.empty() || leaf.find("..") != std::string::npos)
    {
        std::lock_guard<std::mutex> guard(gLoaderHandleLock);
        gLoaderError = "cannot open " + requested;
        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    uint32_t moduleId = 0;
    if (!LoadTranslateAndInitialize(state, directory + "/" + leaf, moduleId))
    {
        std::lock_guard<std::mutex> guard(gLoaderHandleLock);
        gLoaderError = "cannot open " + requested;
        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    uint64_t handle = (uint64_t)moduleId + 1;
    {
        std::lock_guard<std::mutex> guard(gLoaderHandleLock);
        gLoaderHandles[handle] = moduleId;
        gLoaderError.clear();
    }

    fprintf(stderr, "[jnibridge] the guest opened %s as handle %llu\n", leaf.c_str(), (unsigned long long)handle);
    registers.general[0] = handle;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result FindSymbolHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    JniBridgeState* state = (JniBridgeState*)context;

    uint64_t handle = registers.general[0];

    std::string name;
    if (!ReadGuestString(memory, registers.general[1], name)) return AbiBridge::Result::MalformedArguments;

    uint32_t moduleId = ElfLoader::INVALID_MODULE_ID;
    bool scoped = false;
    bool platform = false;

    {
        std::lock_guard<std::mutex> guard(gLoaderHandleLock);
        auto found = gLoaderHandles.find(handle);

        if (found != gLoaderHandles.end())
        {
            moduleId = found->second;
            scoped = true;
        }
        else
        {
            platform = gPlatformHandles.find(handle) != gPlatformHandles.end();
        }
    }

    GuestState::GuestAddress address{};
    bool resolved = false;

    if (scoped)
    {
        resolved = ElfLoader::LookupExportIn(GuestRuntime::LibraryNamespace(*state->runtime), moduleId, name, address) ==
                   ElfLoader::Result::Ok;
    }
    else if (!platform)
    {
        resolved = GuestRuntime::Lookup(*state->runtime, name, address) == GuestRuntime::Result::Ok;
    }

    if (!resolved)
    {
        AbiBridge::Registry& registry = GuestRuntime::BridgeRegistry(*state->runtime);
        resolved = AbiBridge::LookupGuestStub(registry, name, address) == AbiBridge::Result::Ok;
    }

    if (!resolved)
    {
        fprintf(stderr, "[jnibridge] the guest asked handle %llu for symbol %s, which is not defined there\n",
                (unsigned long long)handle, name.c_str());

        std::lock_guard<std::mutex> guard(gLoaderHandleLock);
        gLoaderError = "cannot find " + name;
        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    {
        std::lock_guard<std::mutex> guard(gLoaderHandleLock);
        gLoaderError.clear();
    }

    registers.general[0] = (uint64_t)address;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result CloseLibraryHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    std::lock_guard<std::mutex> guard(gLoaderHandleLock);
    gLoaderHandles.erase(registers.general[0]);
    gPlatformHandles.erase(registers.general[0]);

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static std::string gReportedLoaderError;

static AbiBridge::Result LibraryErrorHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    std::lock_guard<std::mutex> guard(gLoaderHandleLock);

    if (gLoaderError.empty())
    {
        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    gReportedLoaderError = gLoaderError;
    gLoaderError.clear();

    registers.general[0] = (uint64_t)(uintptr_t)gReportedLoaderError.c_str();
    return AbiBridge::Result::Ok;
}

JniBridge::Result JniBridge::RegisterLoaderFunctions(Instance& bridge)
{
    JniBridgeState* state = (JniBridgeState*)bridge.implementation;
    if (!state) return Result::NotInitialized;

    struct HandlerEntry
    {
        const char* name;
        AbiBridge::GuestCallHandler handler;
    };

    const HandlerEntry handlers[] = {
        {"dlopen", &OpenLibraryHandler},
        {"android_dlopen_ext", &OpenLibraryHandler},
        {"dlsym", &FindSymbolHandler},
        {"dlvsym", &FindSymbolHandler},
        {"dlclose", &CloseLibraryHandler},
        {"dlerror", &LibraryErrorHandler},
        {"__loader_dlopen", &OpenLibraryHandler},
        {"__loader_android_dlopen_ext", &OpenLibraryHandler},
        {"__loader_dlsym", &FindSymbolHandler},
        {"__loader_dlvsym", &FindSymbolHandler},
        {"__loader_dlclose", &CloseLibraryHandler},
        {"__loader_dlerror", &LibraryErrorHandler},
    };

    AbiBridge::Registry& registry = GuestRuntime::BridgeRegistry(*state->runtime);

    for (const HandlerEntry& entry : handlers)
    {
        AbiBridge::Result registered = AbiBridge::RegisterHostHandler(registry, entry.name, entry.handler, state);

        if (registered != AbiBridge::Result::Ok && registered != AbiBridge::Result::AlreadyRegistered)
        {
            fprintf(stderr, "[jnibridge] cannot register %s: %s\n", entry.name, AbiBridge::ResultName(registered));
            return Result::RegistrationFailed;
        }
    }

    return Result::Ok;
}

JniBridge::Result JniBridge::LoadGuestLibrary(Instance& bridge, const std::string& soname, uint32_t& moduleId)
{
    JniBridgeState* state = (JniBridgeState*)bridge.implementation;
    if (!state) return Result::NotInitialized;

    std::string directory;
    {
        std::lock_guard<std::mutex> guard(loaderMutex);
        directory = activeLoaderDirectory;
    }

    if (directory.empty())
    {
        fprintf(stderr, "[jnibridge] %s was requested but no guest library loader is installed\n", soname.c_str());
        return Result::NoLibraryLoader;
    }

    if (soname.empty() || soname.find('/') != std::string::npos || soname.find("..") != std::string::npos)
    {
        fprintf(stderr, "[jnibridge] refusing the guest library name %s: it must be a bare leaf name\n", soname.c_str());
        return Result::LibraryLoadFailed;
    }

    if (!LoadTranslateAndInitialize(state, directory + "/" + soname, moduleId)) return Result::LibraryLoadFailed;

    return Result::Ok;
}

static jint LoadGuestAndRunOnLoad(JniBridgeState* state, const std::string& path, const char* soname);
static jint RunGuestOnLoad(JniBridgeState* state, uint32_t moduleId, const char* soname);

extern "C" jint AclLoadGuestLibrary(const char* soname, JavaVM* vm, void* reserved)
{
    (void)vm;
    (void)reserved;

    JniBridgeState* state = nullptr;
    std::string directory;

    {
        std::lock_guard<std::mutex> guard(loaderMutex);
        state = activeLoaderState;
        directory = activeLoaderDirectory;
    }

    if (!state)
    {
        fprintf(stderr, "[jnibridge] %s was requested but no guest library loader is installed\n", soname);
        return JNI_ERR;
    }

    return LoadGuestAndRunOnLoad(state, directory + "/" + soname, soname);
}

static std::mutex gOnLoadLock;
static std::unordered_map<uint32_t, jint> gOnLoadVersions;

static jint LoadGuestAndRunOnLoad(JniBridgeState* state, const std::string& path, const char* soname)
{
    uint32_t moduleId = ElfLoader::INVALID_MODULE_ID;
    if (!LoadTranslateAndInitialize(state, path, moduleId)) return JNI_ERR;

    {
        std::lock_guard<std::mutex> guard(gOnLoadLock);
        auto earlier = gOnLoadVersions.find(moduleId);
        if (earlier != gOnLoadVersions.end()) return earlier->second;
    }

    jint version = RunGuestOnLoad(state, moduleId, soname);

    if (version != JNI_ERR)
    {
        std::lock_guard<std::mutex> guard(gOnLoadLock);
        gOnLoadVersions[moduleId] = version;
    }

    return version;
}

static jint RunGuestOnLoad(JniBridgeState* state, uint32_t moduleId, const char* soname)
{
    GuestState::GuestAddress entry{};
    if (ElfLoader::LookupExportIn(GuestRuntime::LibraryNamespace(*state->runtime), moduleId, "JNI_OnLoad", entry) !=
        ElfLoader::Result::Ok)
    {
        fprintf(stderr, "[jnibridge] %s has no JNI_OnLoad, so nothing was registered from it\n", soname);
        return JNI_VERSION_1_6;
    }

    GuestState::Registers registers;
    GuestState::Reset(registers);
    registers.general[0] = (uint64_t)state->guestVm;
    registers.general[1] = 0;

    GuestRuntime::Result ran = GuestRuntime::CallGuest(*state->runtime, entry, registers);
    if (ran != GuestRuntime::Result::Ok)
    {
        fprintf(stderr, "[jnibridge] guest JNI_OnLoad in %s failed: %s\n", soname, GuestRuntime::ResultName(ran));
        return JNI_ERR;
    }

    jint version = (jint)(uint32_t)registers.general[0];
    fprintf(stderr, "[jnibridge] guest JNI_OnLoad in %s returned %08x\n", soname, (unsigned)version);

    {
        std::lock_guard<std::recursive_mutex> guard(state->mutex);
        state->statistics.guestJniOnLoadCalls += 1;
        state->statistics.lastJniOnLoadVersion = (uint64_t)(uint32_t)version;
    }

    return version;
}

JniBridge::Result JniBridge::InstallLibraryLoader(Instance& bridge, const std::string& guestLibraryDirectory)
{
    JniBridgeState* state = Implementation(bridge);
    if (!state) return Result::NotInitialized;

    std::lock_guard<std::mutex> guard(loaderMutex);
    activeLoaderState = state;
    activeLoaderDirectory = guestLibraryDirectory;

    return Result::Ok;
}

static constexpr uint16_t ELF_MACHINE_AARCH64 = 183;
static constexpr size_t ELF_HEADER_PROBE_BYTES = 20;

typedef jstring (*HostNativeLoad)(JNIEnv* env, jstring filename, jobject loader, jclass caller);

static bool IsGuestElf(const std::string& path)
{
    FILE* file = fopen(path.c_str(), "rb");
    if (!file) return false;

    unsigned char header[ELF_HEADER_PROBE_BYTES] = {};
    size_t read = fread(header, 1, sizeof(header), file);
    fclose(file);

    if (read < sizeof(header)) return false;
    if (header[0] != 0x7f || header[1] != 'E' || header[2] != 'L' || header[3] != 'F') return false;

    uint16_t machine = (uint16_t)(header[18] | (header[19] << 8));
    return machine == ELF_MACHINE_AARCH64;
}

static HostNativeLoad FindHostNativeLoad()
{
    void* found = dlsym(RTLD_DEFAULT, "JVM_NativeLoad");
    if (found) return (HostNativeLoad)found;

    const char* libraries[] = {"libopenjdkjvm.so", "libopenjdkjvmd.so"};
    for (const char* library : libraries)
    {
        void* handle = dlopen(library, RTLD_NOW | RTLD_NOLOAD);
        if (!handle) continue;

        found = dlsym(handle, "JVM_NativeLoad");
        if (found) return (HostNativeLoad)found;
    }

    fprintf(stderr, "[jnibridge] JVM_NativeLoad was not found in the loaded ART libraries\n");
    return nullptr;
}

static jstring RuntimeNativeLoad(JNIEnv* env, jclass, jstring filename, jobject loader, jclass caller)
{
    if (!filename) return env->NewStringUTF("the library path is null");

    const char* chars = env->GetStringUTFChars(filename, nullptr);
    std::string path = chars ? chars : "";
    if (chars) env->ReleaseStringUTFChars(filename, chars);

    JniBridgeState* state = nullptr;
    {
        std::lock_guard<std::mutex> guard(loaderMutex);
        state = activeLoaderState;
    }

    RememberApplicationLoader(env, loader);

    if (state && IsGuestElf(path))
    {
        size_t slash = path.rfind('/');
        std::string leaf = slash == std::string::npos ? path : path.substr(slash + 1);

        fprintf(stderr, "[jnibridge] System.load of the arm64 library %s goes to the guest runtime\n", path.c_str());

        jobject previousLoader = tLoadingClassLoader;
        tLoadingClassLoader = loader;
        jint loaded = LoadGuestAndRunOnLoad(state, path, leaf.c_str());
        tLoadingClassLoader = previousLoader;

        if (loaded == JNI_ERR)
        {
            return env->NewStringUTF(("dlopen failed: the guest runtime could not load \"" + path + "\"").c_str());
        }

        return nullptr;
    }

    static HostNativeLoad hostNativeLoad = FindHostNativeLoad();
    if (!hostNativeLoad) return env->NewStringUTF("JVM_NativeLoad is unavailable, so the host cannot load native libraries");

    return hostNativeLoad(env, filename, loader, caller);
}

JniBridge::Result JniBridge::InstallRuntimeLoadHook(JNIEnv* env)
{
    if (!env) return Result::NotInitialized;
    if (!FindHostNativeLoad()) return Result::RegistrationFailed;

    jclass runtime = env->FindClass("java/lang/Runtime");
    if (!runtime)
    {
        env->ExceptionClear();
        return Result::RegistrationFailed;
    }

    const JNINativeMethod methods[] = {
        {const_cast<char*>("nativeLoad"), const_cast<char*>("(Ljava/lang/String;Ljava/lang/ClassLoader;Ljava/lang/Class;)Ljava/lang/String;"),
         reinterpret_cast<void*>(&RuntimeNativeLoad)},
    };

    jint registered = env->RegisterNatives(runtime, methods, 1);
    env->DeleteLocalRef(runtime);

    if (registered != JNI_OK)
    {
        env->ExceptionClear();
        fprintf(stderr, "[jnibridge] cannot route System.load through the guest runtime, so arm64 libraries an app unpacks itself "
                        "will not load\n");
        return Result::RegistrationFailed;
    }

    return Result::Ok;
}

void JniBridge::InstallLibraryTranslator(Instance& bridge, LibraryTranslator translator, void* context)
{
    JniBridgeState* state = Implementation(bridge);
    if (!state) return;

    std::lock_guard<std::recursive_mutex> guard(state->mutex);
    state->translator = translator;
    state->translatorContext = context;
}

void JniBridge::RemoveLibraryLoader(Instance& bridge)
{
    JniBridgeState* state = Implementation(bridge);

    std::lock_guard<std::mutex> guard(loaderMutex);
    if (activeLoaderState != state) return;

    activeLoaderState = nullptr;
    activeLoaderDirectory.clear();
}

JniBridge::Result JniBridge::GuestJavaVm(const Instance& bridge, GuestState::GuestAddress& address)
{
    JniBridgeState* state = Implementation(bridge);
    if (!state) return Result::NotInitialized;

    address = state->guestVm;
    return Result::Ok;
}

JniBridge::Result JniBridge::GuestJniEnv(const Instance& bridge, GuestState::GuestAddress& address)
{
    JniBridgeState* state = Implementation(bridge);
    if (!state) return Result::NotInitialized;

    address = state->guestEnv;
    return Result::Ok;
}

const std::vector<JniBridge::RegisteredNative>& JniBridge::RegisteredNatives(const Instance& bridge)
{
    static const std::vector<RegisteredNative> EMPTY;

    JniBridgeState* state = Implementation(bridge);
    if (!state) return EMPTY;

    return state->registeredNatives;
}

JniBridge::Statistics JniBridge::GetStatistics(const Instance& bridge)
{
    JniBridgeState* state = Implementation(bridge);
    if (!state) return Statistics();

    std::lock_guard<std::recursive_mutex> guard(state->mutex);
    return state->statistics;
}

const char* JniBridge::SlotName(int slot)
{
    if (slot == SLOT_GET_VERSION) return "GetVersion";
    if (slot == SLOT_FIND_CLASS) return "FindClass";
    if (slot == SLOT_GET_OBJECT_CLASS) return "GetObjectClass";
    if (slot == SLOT_EXCEPTION_OCCURRED) return "ExceptionOccurred";
    if (slot == SLOT_EXCEPTION_DESCRIBE) return "ExceptionDescribe";
    if (slot == SLOT_EXCEPTION_CLEAR) return "ExceptionClear";
    if (slot == SLOT_EXCEPTION_CHECK) return "ExceptionCheck";
    if (slot == SLOT_NEW_GLOBAL_REF) return "NewGlobalRef";
    if (slot == SLOT_DELETE_GLOBAL_REF) return "DeleteGlobalRef";
    if (slot == SLOT_DELETE_LOCAL_REF) return "DeleteLocalRef";
    if (slot == SLOT_GET_METHOD_ID) return "GetMethodID";
    if (slot == SLOT_GET_STATIC_METHOD_ID) return "GetStaticMethodID";
    if (slot == SLOT_NEW_STRING_UTF) return "NewStringUTF";
    if (slot == SLOT_GET_STRING_UTF_LENGTH) return "GetStringUTFLength";
    if (slot == SLOT_GET_STRING_UTF_CHARS) return "GetStringUTFChars";
    if (slot == SLOT_RELEASE_STRING_UTF_CHARS) return "ReleaseStringUTFChars";
    if (slot == SLOT_REGISTER_NATIVES) return "RegisterNatives";
    if (slot == SLOT_GET_JAVA_VM) return "GetJavaVM";

    return "unnamed, look the index up in jni.h";
}

const char* JniBridge::ResultName(Result result)
{
    switch (result)
    {
        case Result::Ok: return "Ok";
        case Result::NotInitialized: return "NotInitialized";
        case Result::OutOfGuestMemory: return "OutOfGuestMemory";
        case Result::RegistrationFailed: return "RegistrationFailed";
        case Result::BadDescriptor: return "BadDescriptor";
        case Result::GuestFaulted: return "GuestFaulted";
        case Result::NoJavaVm: return "NoJavaVm";
        case Result::NoLibraryLoader: return "NoLibraryLoader";
        case Result::LibraryLoadFailed: return "LibraryLoadFailed";
    }

    return "Unknown";
}
