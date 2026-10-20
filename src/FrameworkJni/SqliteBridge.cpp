#include "FrameworkJni/SqliteBridge.hpp"

#include <cstring>
#include <sqlite3.h>
#include <string>

static sqlite3* DatabaseOf(jlong handle)
{
    return reinterpret_cast<sqlite3*>(static_cast<intptr_t>(handle));
}

static sqlite3_stmt* StatementOf(jlong handle)
{
    return reinterpret_cast<sqlite3_stmt*>(static_cast<intptr_t>(handle));
}

static std::string ReadString(JNIEnv* env, jstring value)
{
    if (!value) return std::string();

    const char* bytes = env->GetStringUTFChars(value, nullptr);
    std::string copied = bytes ? bytes : "";
    if (bytes) env->ReleaseStringUTFChars(value, bytes);

    return copied;
}

static jlong SqliteOpen(JNIEnv* env, jclass, jstring path, jboolean readOnly)
{
    std::string target = ReadString(env, path);

    int flags = readOnly ? SQLITE_OPEN_READONLY : (SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE);

    sqlite3* database = nullptr;
    if (sqlite3_open_v2(target.c_str(), &database, flags, nullptr) != SQLITE_OK)
    {
        if (database) sqlite3_close_v2(database);
        return 0;
    }

    sqlite3_busy_timeout(database, 5000);
    sqlite3_extended_result_codes(database, 1);

    return static_cast<jlong>(reinterpret_cast<intptr_t>(database));
}

static void SqliteClose(JNIEnv*, jclass, jlong handle)
{
    sqlite3* database = DatabaseOf(handle);
    if (database) sqlite3_close_v2(database);
}

static jint SqliteExec(JNIEnv* env, jclass, jlong handle, jstring sql)
{
    sqlite3* database = DatabaseOf(handle);
    if (!database) return SQLITE_MISUSE;

    std::string statement = ReadString(env, sql);
    return sqlite3_exec(database, statement.c_str(), nullptr, nullptr, nullptr);
}

static jlong SqlitePrepare(JNIEnv* env, jclass, jlong handle, jstring sql)
{
    sqlite3* database = DatabaseOf(handle);
    if (!database) return 0;

    std::string statement = ReadString(env, sql);

    sqlite3_stmt* prepared = nullptr;
    if (sqlite3_prepare_v2(database, statement.c_str(), -1, &prepared, nullptr) != SQLITE_OK) return 0;

    return static_cast<jlong>(reinterpret_cast<intptr_t>(prepared));
}

static void SqliteFinalize(JNIEnv*, jclass, jlong statement)
{
    sqlite3_stmt* prepared = StatementOf(statement);
    if (prepared) sqlite3_finalize(prepared);
}

static jint SqliteStep(JNIEnv*, jclass, jlong statement)
{
    sqlite3_stmt* prepared = StatementOf(statement);
    if (!prepared) return SqliteBridge::STEP_ERROR;

    int outcome = sqlite3_step(prepared);
    if (outcome == SQLITE_ROW) return SqliteBridge::STEP_ROW;
    if (outcome == SQLITE_DONE) return SqliteBridge::STEP_DONE;

    return SqliteBridge::STEP_ERROR;
}

static void SqliteReset(JNIEnv*, jclass, jlong statement)
{
    sqlite3_stmt* prepared = StatementOf(statement);
    if (!prepared) return;

    sqlite3_reset(prepared);
    sqlite3_clear_bindings(prepared);
}

static void SqliteBindNull(JNIEnv*, jclass, jlong statement, jint index)
{
    sqlite3_stmt* prepared = StatementOf(statement);
    if (prepared) sqlite3_bind_null(prepared, index);
}

static void SqliteBindLong(JNIEnv*, jclass, jlong statement, jint index, jlong value)
{
    sqlite3_stmt* prepared = StatementOf(statement);
    if (prepared) sqlite3_bind_int64(prepared, index, value);
}

static void SqliteBindDouble(JNIEnv*, jclass, jlong statement, jint index, jdouble value)
{
    sqlite3_stmt* prepared = StatementOf(statement);
    if (prepared) sqlite3_bind_double(prepared, index, value);
}

static void SqliteBindString(JNIEnv* env, jclass, jlong statement, jint index, jstring value)
{
    sqlite3_stmt* prepared = StatementOf(statement);
    if (!prepared) return;

    std::string text = ReadString(env, value);
    sqlite3_bind_text(prepared, index, text.c_str(), static_cast<int>(text.size()), SQLITE_TRANSIENT);
}

static void SqliteBindBlob(JNIEnv* env, jclass, jlong statement, jint index, jbyteArray value)
{
    sqlite3_stmt* prepared = StatementOf(statement);
    if (!prepared) return;

    if (!value)
    {
        sqlite3_bind_null(prepared, index);
        return;
    }

    jsize length = env->GetArrayLength(value);
    jbyte* bytes = env->GetByteArrayElements(value, nullptr);

    sqlite3_bind_blob(prepared, index, bytes, static_cast<int>(length), SQLITE_TRANSIENT);
    env->ReleaseByteArrayElements(value, bytes, JNI_ABORT);
}

static jint SqliteColumnCount(JNIEnv*, jclass, jlong statement)
{
    sqlite3_stmt* prepared = StatementOf(statement);
    return prepared ? sqlite3_column_count(prepared) : 0;
}

static jstring SqliteColumnName(JNIEnv* env, jclass, jlong statement, jint index)
{
    sqlite3_stmt* prepared = StatementOf(statement);
    if (!prepared) return nullptr;

    const char* name = sqlite3_column_name(prepared, index);
    return name ? env->NewStringUTF(name) : nullptr;
}

static jint SqliteColumnType(JNIEnv*, jclass, jlong statement, jint index)
{
    sqlite3_stmt* prepared = StatementOf(statement);
    if (!prepared) return SQLITE_NULL;

    return sqlite3_column_type(prepared, index);
}

static jlong SqliteColumnLong(JNIEnv*, jclass, jlong statement, jint index)
{
    sqlite3_stmt* prepared = StatementOf(statement);
    return prepared ? sqlite3_column_int64(prepared, index) : 0;
}

static jdouble SqliteColumnDouble(JNIEnv*, jclass, jlong statement, jint index)
{
    sqlite3_stmt* prepared = StatementOf(statement);
    return prepared ? sqlite3_column_double(prepared, index) : 0.0;
}

static jstring SqliteColumnString(JNIEnv* env, jclass, jlong statement, jint index)
{
    sqlite3_stmt* prepared = StatementOf(statement);
    if (!prepared) return nullptr;

    const unsigned char* text = sqlite3_column_text(prepared, index);
    return text ? env->NewStringUTF(reinterpret_cast<const char*>(text)) : nullptr;
}

static jbyteArray SqliteColumnBlob(JNIEnv* env, jclass, jlong statement, jint index)
{
    sqlite3_stmt* prepared = StatementOf(statement);
    if (!prepared) return nullptr;

    const void* bytes = sqlite3_column_blob(prepared, index);
    int length = sqlite3_column_bytes(prepared, index);
    if (!bytes || length < 0) return nullptr;

    jbyteArray copied = env->NewByteArray(length);
    if (!copied) return nullptr;

    env->SetByteArrayRegion(copied, 0, length, reinterpret_cast<const jbyte*>(bytes));
    return copied;
}

static jint SqliteChanges(JNIEnv*, jclass, jlong handle)
{
    sqlite3* database = DatabaseOf(handle);
    return database ? sqlite3_changes(database) : 0;
}

static jlong SqliteLastInsertRowId(JNIEnv*, jclass, jlong handle)
{
    sqlite3* database = DatabaseOf(handle);
    return database ? sqlite3_last_insert_rowid(database) : 0;
}

static jstring SqliteErrorMessage(JNIEnv* env, jclass, jlong handle)
{
    sqlite3* database = DatabaseOf(handle);
    const char* message = database ? sqlite3_errmsg(database) : "no database";

    return env->NewStringUTF(message ? message : "");
}

static jint SqliteErrorCode(JNIEnv*, jclass, jlong handle)
{
    sqlite3* database = DatabaseOf(handle);
    return database ? sqlite3_extended_errcode(database) : SQLITE_MISUSE;
}

static jint SqliteParameterCount(JNIEnv*, jclass, jlong statement)
{
    sqlite3_stmt* prepared = StatementOf(statement);
    return prepared ? sqlite3_bind_parameter_count(prepared) : 0;
}

static const JNINativeMethod SQLITE_METHODS[] = {
    {"nativeOpen", "(Ljava/lang/String;Z)J", reinterpret_cast<void*>(&SqliteOpen)},
    {"nativeClose", "(J)V", reinterpret_cast<void*>(&SqliteClose)},
    {"nativeExec", "(JLjava/lang/String;)I", reinterpret_cast<void*>(&SqliteExec)},
    {"nativePrepare", "(JLjava/lang/String;)J", reinterpret_cast<void*>(&SqlitePrepare)},
    {"nativeFinalize", "(J)V", reinterpret_cast<void*>(&SqliteFinalize)},
    {"nativeStep", "(J)I", reinterpret_cast<void*>(&SqliteStep)},
    {"nativeReset", "(J)V", reinterpret_cast<void*>(&SqliteReset)},
    {"nativeBindNull", "(JI)V", reinterpret_cast<void*>(&SqliteBindNull)},
    {"nativeBindLong", "(JIJ)V", reinterpret_cast<void*>(&SqliteBindLong)},
    {"nativeBindDouble", "(JID)V", reinterpret_cast<void*>(&SqliteBindDouble)},
    {"nativeBindString", "(JILjava/lang/String;)V", reinterpret_cast<void*>(&SqliteBindString)},
    {"nativeBindBlob", "(JI[B)V", reinterpret_cast<void*>(&SqliteBindBlob)},
    {"nativeColumnCount", "(J)I", reinterpret_cast<void*>(&SqliteColumnCount)},
    {"nativeColumnName", "(JI)Ljava/lang/String;", reinterpret_cast<void*>(&SqliteColumnName)},
    {"nativeColumnType", "(JI)I", reinterpret_cast<void*>(&SqliteColumnType)},
    {"nativeColumnLong", "(JI)J", reinterpret_cast<void*>(&SqliteColumnLong)},
    {"nativeColumnDouble", "(JI)D", reinterpret_cast<void*>(&SqliteColumnDouble)},
    {"nativeColumnString", "(JI)Ljava/lang/String;", reinterpret_cast<void*>(&SqliteColumnString)},
    {"nativeColumnBlob", "(JI)[B", reinterpret_cast<void*>(&SqliteColumnBlob)},
    {"nativeChanges", "(J)I", reinterpret_cast<void*>(&SqliteChanges)},
    {"nativeLastInsertRowId", "(J)J", reinterpret_cast<void*>(&SqliteLastInsertRowId)},
    {"nativeErrorMessage", "(J)Ljava/lang/String;", reinterpret_cast<void*>(&SqliteErrorMessage)},
    {"nativeErrorCode", "(J)I", reinterpret_cast<void*>(&SqliteErrorCode)},
    {"nativeParameterCount", "(J)I", reinterpret_cast<void*>(&SqliteParameterCount)},
};

static const ArtRuntime::NativeMethodTable TABLES[] = {
    {SqliteBridge::SQLITE_CLASS, SQLITE_METHODS, static_cast<int>(sizeof(SQLITE_METHODS) / sizeof(SQLITE_METHODS[0]))},
};

const ArtRuntime::NativeMethodTable* SqliteBridge::Tables(int& count)
{
    count = static_cast<int>(sizeof(TABLES) / sizeof(TABLES[0]));
    return TABLES;
}
