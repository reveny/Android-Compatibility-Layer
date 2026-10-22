#include "FrameworkJni/TextBridge.hpp"

#include <cstdio>
#include <string>
#include <vector>

#include "FontStack/FontStack.hpp"
#include "FrameworkJni/FrameworkJni.hpp"

static bool gStarted = false;
static bool gStartFailed = false;

static void ReportTextFailure(JNIEnv* env, const char* what, FontStack::Result result);

static bool EnsureFontStack()
{
    if (gStarted) return true;
    if (gStartFailed) return false;

    FontStack::Result started = FontStack::Initialize(nullptr);
    if (started == FontStack::Result::Ok || started == FontStack::Result::AlreadyInitialized)
    {
        gStarted = true;
        return true;
    }

    gStartFailed = true;
    fprintf(stderr, "[text] font stack unavailable: %s\n", FontStack::ResultName(started));
    fprintf(stderr, "[text] set RUNTIME_FONT_DIR to a directory containing Roboto-Regular.ttf\n");
    fflush(stderr);
    return false;
}

static bool ReadChars(JNIEnv* env, jcharArray source, jint index, jint count, std::vector<uint16_t>& out)
{
    if (!source || count < 0 || index < 0) return false;

    jsize length = env->GetArrayLength(source);
    if (index > length || count > length - index) return false;

    out.resize(static_cast<size_t>(count));
    if (count == 0) return true;

    env->GetCharArrayRegion(source, index, count, reinterpret_cast<jchar*>(out.data()));
    return true;
}

static jint TypefaceCreate(JNIEnv* env, jclass, jstring familyName, jint weight, jboolean italic)
{
    if (!EnsureFontStack()) return 0;

    const char* chars = familyName ? env->GetStringUTFChars(familyName, nullptr) : nullptr;

    uint32_t font = FontStack::INVALID_FONT;
    FontStack::Result resolved = FontStack::ResolveFamily(chars, weight, italic == JNI_TRUE, font);

    if (chars) env->ReleaseStringUTFChars(familyName, chars);
    if (resolved != FontStack::Result::Ok) return 0;

    return static_cast<jint>(font);
}

static jint TypefaceLoadFromMemory(JNIEnv* env, jclass, jbyteArray data, jint weight, jboolean italic)
{
    if (!data || !EnsureFontStack()) return 0;

    jsize length = env->GetArrayLength(data);
    if (length <= 0) return 0;

    std::vector<uint8_t> bytes(static_cast<size_t>(length));
    env->GetByteArrayRegion(data, 0, length, reinterpret_cast<jbyte*>(bytes.data()));
    if (env->ExceptionCheck()) return 0;

    uint32_t font = FontStack::INVALID_FONT;
    FontStack::Result loaded = FontStack::LoadFontFromMemory(bytes.data(), bytes.size(), weight, italic == JNI_TRUE, font);
    if (loaded != FontStack::Result::Ok)
    {
        fprintf(stderr, "[text] a %d byte app font could not be loaded: %s\n", static_cast<int>(length), FontStack::ResultName(loaded));
        return 0;
    }

    return static_cast<jint>(font);
}

static jint TypefaceWeight(JNIEnv*, jclass, jint font)
{
    int weight = FontStack::WEIGHT_NORMAL;
    FontStack::FontWeight(static_cast<uint32_t>(font), weight);
    return weight;
}

static jboolean TypefaceIsItalic(JNIEnv*, jclass, jint font)
{
    bool italic = false;
    FontStack::FontIsItalic(static_cast<uint32_t>(font), italic);
    return italic ? JNI_TRUE : JNI_FALSE;
}

static jint PaintInternStyle(JNIEnv* env, jclass, jint font, jfloat textSize, jfloat scaleX, jfloat skewX, jfloat letterSpacing, jint flags)
{
    if (!EnsureFontStack())
    {
        FrameworkJni::Unimplemented(env, "android.graphics.Paint text metrics without a usable font directory");
        return 0;
    }

    uint32_t style = FontStack::INVALID_STYLE;
    FontStack::Result interned =
        FontStack::InternStyle(static_cast<uint32_t>(font), textSize, scaleX, skewX, letterSpacing, flags, style);

    if (interned != FontStack::Result::Ok)
    {
        ReportTextFailure(env, "internStyle", interned);
        return 0;
    }

    return static_cast<jint>(style);
}

static void ReportTextFailure(JNIEnv* env, const char* what, FontStack::Result result)
{
    std::string message = std::string("android.graphics.Paint.") + what + " -> " + FontStack::ResultName(result);
    FrameworkJni::Unimplemented(env, message.c_str());
}

static jfloat PaintMeasureText(JNIEnv* env, jclass, jint style, jcharArray text, jint index, jint count)
{
    std::vector<uint16_t> chars;
    if (!ReadChars(env, text, index, count, chars)) return 0.0f;

    float advance = 0.0f;
    FontStack::Result measured = FontStack::MeasureText(static_cast<uint32_t>(style), chars.data(), count, advance);
    if (measured != FontStack::Result::Ok)
    {
        ReportTextFailure(env, "measureText", measured);
        return 0.0f;
    }

    return advance;
}

static jint PaintGetTextWidths(JNIEnv* env, jclass, jint style, jcharArray text, jint index, jint count, jfloatArray outWidths)
{
    std::vector<uint16_t> chars;
    if (!ReadChars(env, text, index, count, chars)) return 0;
    if (!outWidths || env->GetArrayLength(outWidths) < count) return 0;

    std::vector<float> widths(static_cast<size_t>(count), 0.0f);
    int32_t written = 0;
    if (FontStack::TextWidths(static_cast<uint32_t>(style), chars.data(), count, widths.data(), written) != FontStack::Result::Ok)
    {
        return 0;
    }

    env->SetFloatArrayRegion(outWidths, 0, written, widths.data());
    return written;
}

static void PaintGetFontMetrics(JNIEnv* env, jclass, jint style, jfloatArray outMetrics)
{
    if (!outMetrics || env->GetArrayLength(outMetrics) < TextBridge::METRICS_FIELD_COUNT) return;

    FontStack::Metrics metrics;
    if (FontStack::FontMetrics(static_cast<uint32_t>(style), metrics) != FontStack::Result::Ok) return;

    float values[TextBridge::METRICS_FIELD_COUNT] = {metrics.top, metrics.ascent, metrics.descent, metrics.bottom, metrics.leading};
    env->SetFloatArrayRegion(outMetrics, 0, TextBridge::METRICS_FIELD_COUNT, values);
}

static void PaintGetTextBounds(JNIEnv* env, jclass, jint style, jcharArray text, jint index, jint count, jintArray outBounds)
{
    if (!outBounds || env->GetArrayLength(outBounds) < TextBridge::BOUNDS_FIELD_COUNT) return;

    std::vector<uint16_t> chars;
    if (!ReadChars(env, text, index, count, chars)) return;

    FontStack::Bounds bounds;
    if (FontStack::TextBounds(static_cast<uint32_t>(style), chars.data(), count, bounds) != FontStack::Result::Ok) return;

    jint values[TextBridge::BOUNDS_FIELD_COUNT] = {bounds.left, bounds.top, bounds.right, bounds.bottom};
    env->SetIntArrayRegion(outBounds, 0, TextBridge::BOUNDS_FIELD_COUNT, values);
}

static jint PaintBreakText(JNIEnv* env, jclass, jint style, jcharArray text, jint index, jint count, jfloat maxWidth, jboolean forwards,
                           jfloatArray outMeasured)
{
    std::vector<uint16_t> chars;
    if (!ReadChars(env, text, index, count, chars)) return 0;

    int32_t breakAt = 0;
    float measured = 0.0f;
    if (FontStack::BreakText(static_cast<uint32_t>(style), chars.data(), count, maxWidth, forwards == JNI_TRUE, breakAt, measured)
        != FontStack::Result::Ok)
    {
        return 0;
    }

    if (outMeasured && env->GetArrayLength(outMeasured) >= 1) env->SetFloatArrayRegion(outMeasured, 0, 1, &measured);
    return breakAt;
}

static jboolean PaintHasGlyph(JNIEnv* env, jclass, jint style, jstring text)
{
    if (!text) return JNI_FALSE;

    jsize length = env->GetStringLength(text);
    std::vector<uint16_t> chars(static_cast<size_t>(length));
    if (length > 0) env->GetStringRegion(text, 0, length, reinterpret_cast<jchar*>(chars.data()));

    bool present = false;
    if (FontStack::HasGlyph(static_cast<uint32_t>(style), chars.data(), length, present) != FontStack::Result::Ok) return JNI_FALSE;
    return present ? JNI_TRUE : JNI_FALSE;
}

static const JNINativeMethod TYPEFACE_METHODS[] = {
    {"nativeResolveFamily", "(Ljava/lang/String;IZ)I", reinterpret_cast<void*>(&TypefaceCreate)},
    {"nativeWeight", "(I)I", reinterpret_cast<void*>(&TypefaceWeight)},
    {"nativeIsItalic", "(I)Z", reinterpret_cast<void*>(&TypefaceIsItalic)},
    {"nativeLoadFromMemory", "([BIZ)I", reinterpret_cast<void*>(&TypefaceLoadFromMemory)},
};

static const JNINativeMethod PAINT_METHODS[] = {
    {"nativeInternStyle", "(IFFFFI)I", reinterpret_cast<void*>(&PaintInternStyle)},
    {"nativeMeasureText", "(I[CII)F", reinterpret_cast<void*>(&PaintMeasureText)},
    {"nativeGetTextWidths", "(I[CII[F)I", reinterpret_cast<void*>(&PaintGetTextWidths)},
    {"nativeGetFontMetrics", "(I[F)V", reinterpret_cast<void*>(&PaintGetFontMetrics)},
    {"nativeGetTextBounds", "(I[CII[I)V", reinterpret_cast<void*>(&PaintGetTextBounds)},
    {"nativeBreakText", "(I[CIIFZ[F)I", reinterpret_cast<void*>(&PaintBreakText)},
    {"nativeHasGlyph", "(ILjava/lang/String;)Z", reinterpret_cast<void*>(&PaintHasGlyph)},
};

static const ArtRuntime::NativeMethodTable TABLES[] = {
    {TextBridge::TYPEFACE_CLASS, TYPEFACE_METHODS, static_cast<int>(sizeof(TYPEFACE_METHODS) / sizeof(TYPEFACE_METHODS[0]))},
    {TextBridge::PAINT_CLASS, PAINT_METHODS, static_cast<int>(sizeof(PAINT_METHODS) / sizeof(PAINT_METHODS[0]))},
};

const ArtRuntime::NativeMethodTable* TextBridge::Tables(int& count)
{
    count = static_cast<int>(sizeof(TABLES) / sizeof(TABLES[0]));
    return TABLES;
}
