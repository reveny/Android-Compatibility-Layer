#include "FrameworkJni/GraphicsBridge.hpp"

#include <cstdlib>
#include <cstring>
#include <vector>

#include "ImageCodec/ImageCodec.hpp"
#include "Rasterizer/Rasterizer.hpp"

struct BitmapHandle
{
    Rasterizer::Surface surface;
    bool hasAlpha = false;
};

BitmapHandle* FrameworkJniBitmapFrom(jlong handle)
{
    return reinterpret_cast<BitmapHandle*>(static_cast<intptr_t>(handle));
}

const Rasterizer::Surface* FrameworkJniBitmapSurface(BitmapHandle* bitmap)
{
    return bitmap ? &bitmap->surface : nullptr;
}

static jlong BitmapDecode(JNIEnv* env, jclass, jbyteArray data, jint offset, jint length)
{
    if (!data || length <= 0) return 0;

    jsize available = env->GetArrayLength(data);
    if (offset < 0 || length > available - offset) return 0;

    std::vector<uint8_t> bytes(static_cast<size_t>(length));
    env->GetByteArrayRegion(data, offset, length, reinterpret_cast<jbyte*>(bytes.data()));

    ImageCodec::Image decoded;
    if (ImageCodec::Decode(bytes.data(), bytes.size(), decoded) != ImageCodec::Result::Ok) return 0;

    BitmapHandle* bitmap = new BitmapHandle();
    bitmap->surface.pixels = decoded.pixels;
    bitmap->surface.width = decoded.width;
    bitmap->surface.height = decoded.height;
    bitmap->surface.stride = decoded.stride;
    bitmap->surface.ownsPixels = true;
    bitmap->hasAlpha = decoded.hasAlpha;

    return reinterpret_cast<jlong>(bitmap);
}

static jboolean BitmapMeasure(JNIEnv* env, jclass, jbyteArray data, jint offset, jint length, jintArray into)
{
    if (!data || length <= 0 || !into || env->GetArrayLength(into) < 2) return JNI_FALSE;

    jsize available = env->GetArrayLength(data);
    if (offset < 0 || length > available - offset) return JNI_FALSE;

    std::vector<uint8_t> bytes(static_cast<size_t>(length));
    env->GetByteArrayRegion(data, offset, length, reinterpret_cast<jbyte*>(bytes.data()));

    uint32_t width = 0;
    uint32_t height = 0;

    if (ImageCodec::MeasureOnly(bytes.data(), bytes.size(), width, height) != ImageCodec::Result::Ok) return JNI_FALSE;

    jint size[2] = {static_cast<jint>(width), static_cast<jint>(height)};
    env->SetIntArrayRegion(into, 0, 2, size);

    return JNI_TRUE;
}

static jlong BitmapCreate(JNIEnv*, jclass, jint width, jint height)
{
    if (width <= 0 || height <= 0) return 0;
    if (static_cast<uint32_t>(width) > Rasterizer::MAX_DIMENSION) return 0;
    if (static_cast<uint32_t>(height) > Rasterizer::MAX_DIMENSION) return 0;

    BitmapHandle* bitmap = new BitmapHandle();

    if (Rasterizer::CreateSurface(static_cast<uint32_t>(width), static_cast<uint32_t>(height), bitmap->surface)
        != Rasterizer::Result::Ok)
    {
        delete bitmap;
        return 0;
    }

    std::memset(bitmap->surface.pixels, 0, static_cast<size_t>(bitmap->surface.stride) * bitmap->surface.height);
    bitmap->hasAlpha = true;

    return reinterpret_cast<jlong>(bitmap);
}

static void BitmapDestroy(JNIEnv*, jclass, jlong handle)
{
    BitmapHandle* bitmap = FrameworkJniBitmapFrom(handle);
    if (!bitmap) return;

    Rasterizer::DestroySurface(bitmap->surface);
    delete bitmap;
}

static jint BitmapWidth(JNIEnv*, jclass, jlong handle)
{
    BitmapHandle* bitmap = FrameworkJniBitmapFrom(handle);
    return bitmap ? static_cast<jint>(bitmap->surface.width) : 0;
}

static jint BitmapHeight(JNIEnv*, jclass, jlong handle)
{
    BitmapHandle* bitmap = FrameworkJniBitmapFrom(handle);
    return bitmap ? static_cast<jint>(bitmap->surface.height) : 0;
}

static jboolean BitmapHasAlpha(JNIEnv*, jclass, jlong handle)
{
    BitmapHandle* bitmap = FrameworkJniBitmapFrom(handle);
    return bitmap && bitmap->hasAlpha ? JNI_TRUE : JNI_FALSE;
}

static uint32_t StraightFromPremultiplied(uint32_t pixel)
{
    uint32_t alpha = (pixel >> 24) & 0xFFu;
    if (alpha == 0 || alpha == 255) return pixel;

    uint32_t blue = ((pixel & 0xFFu) * 255u + alpha / 2) / alpha;
    uint32_t green = (((pixel >> 8) & 0xFFu) * 255u + alpha / 2) / alpha;
    uint32_t red = (((pixel >> 16) & 0xFFu) * 255u + alpha / 2) / alpha;

    if (blue > 255) blue = 255;
    if (green > 255) green = 255;
    if (red > 255) red = 255;

    return (alpha << 24) | (red << 16) | (green << 8) | blue;
}

static jint BitmapPixel(JNIEnv*, jclass, jlong handle, jint x, jint y)
{
    BitmapHandle* bitmap = FrameworkJniBitmapFrom(handle);
    if (!bitmap || !bitmap->surface.pixels) return 0;
    if (x < 0 || y < 0) return 0;
    if (static_cast<uint32_t>(x) >= bitmap->surface.width || static_cast<uint32_t>(y) >= bitmap->surface.height) return 0;

    const uint8_t* row = bitmap->surface.pixels + static_cast<size_t>(y) * bitmap->surface.stride;
    return static_cast<jint>(reinterpret_cast<const uint32_t*>(row)[x]);
}

static void BitmapSetPixel(JNIEnv*, jclass, jlong handle, jint x, jint y, jint color)
{
    BitmapHandle* bitmap = FrameworkJniBitmapFrom(handle);
    if (!bitmap || !bitmap->surface.pixels) return;
    if (x < 0 || y < 0) return;
    if (static_cast<uint32_t>(x) >= bitmap->surface.width || static_cast<uint32_t>(y) >= bitmap->surface.height) return;

    uint8_t* row = bitmap->surface.pixels + static_cast<size_t>(y) * bitmap->surface.stride;
    reinterpret_cast<uint32_t*>(row)[x] = static_cast<uint32_t>(color);
}

static void BitmapReadPixels(JNIEnv* env, jclass, jlong handle, jintArray into, jint offset, jint stride, jint x, jint y,
                             jint width, jint height)
{
    BitmapHandle* bitmap = FrameworkJniBitmapFrom(handle);
    if (!bitmap || !bitmap->surface.pixels || !into) return;
    if (x < 0 || y < 0 || width <= 0 || height <= 0) return;
    if (static_cast<uint32_t>(x + width) > bitmap->surface.width) return;
    if (static_cast<uint32_t>(y + height) > bitmap->surface.height) return;

    std::vector<jint> row(static_cast<size_t>(width));

    for (jint line = 0; line < height; ++line)
    {
        const uint8_t* source = bitmap->surface.pixels + static_cast<size_t>(y + line) * bitmap->surface.stride;
        const uint32_t* pixels = reinterpret_cast<const uint32_t*>(source) + x;

        for (jint column = 0; column < width; ++column) row[column] = static_cast<jint>(StraightFromPremultiplied(pixels[column]));

        env->SetIntArrayRegion(into, offset + line * stride, width, row.data());
    }
}

static void BitmapErase(JNIEnv*, jclass, jlong handle, jint color)
{
    BitmapHandle* bitmap = FrameworkJniBitmapFrom(handle);
    if (!bitmap || !bitmap->surface.pixels) return;

    for (uint32_t y = 0; y < bitmap->surface.height; ++y)
    {
        uint32_t* row = reinterpret_cast<uint32_t*>(bitmap->surface.pixels + static_cast<size_t>(y) * bitmap->surface.stride);
        for (uint32_t x = 0; x < bitmap->surface.width; ++x) row[x] = static_cast<uint32_t>(color);
    }
}

static jlong BitmapOpenCanvas(JNIEnv*, jclass, jlong handle)
{
    BitmapHandle* bitmap = FrameworkJniBitmapFrom(handle);
    if (!bitmap || !bitmap->surface.pixels) return 0;

    Rasterizer::Canvas* canvas = new Rasterizer::Canvas();

    if (Rasterizer::Begin(bitmap->surface, *canvas) != Rasterizer::Result::Ok)
    {
        delete canvas;
        return 0;
    }

    return reinterpret_cast<jlong>(canvas);
}

static void BitmapCloseCanvas(JNIEnv*, jclass, jlong handle)
{
    Rasterizer::Canvas* canvas = reinterpret_cast<Rasterizer::Canvas*>(static_cast<intptr_t>(handle));
    if (!canvas) return;

    Rasterizer::End(*canvas);
    delete canvas;
}

static const JNINativeMethod BITMAP_METHODS[] = {
    {"nativeOpenCanvas", "(J)J", reinterpret_cast<void*>(&BitmapOpenCanvas)},
    {"nativeCloseCanvas", "(J)V", reinterpret_cast<void*>(&BitmapCloseCanvas)},
    {"nativeDecode", "([BII)J", reinterpret_cast<void*>(&BitmapDecode)},
    {"nativeMeasure", "([BII[I)Z", reinterpret_cast<void*>(&BitmapMeasure)},
    {"nativeCreate", "(II)J", reinterpret_cast<void*>(&BitmapCreate)},
    {"nativeDestroy", "(J)V", reinterpret_cast<void*>(&BitmapDestroy)},
    {"nativeWidth", "(J)I", reinterpret_cast<void*>(&BitmapWidth)},
    {"nativeHeight", "(J)I", reinterpret_cast<void*>(&BitmapHeight)},
    {"nativeHasAlpha", "(J)Z", reinterpret_cast<void*>(&BitmapHasAlpha)},
    {"nativePixel", "(JII)I", reinterpret_cast<void*>(&BitmapPixel)},
    {"nativeSetPixel", "(JIII)V", reinterpret_cast<void*>(&BitmapSetPixel)},
    {"nativeReadPixels", "(J[IIIIIII)V", reinterpret_cast<void*>(&BitmapReadPixels)},
    {"nativeErase", "(JI)V", reinterpret_cast<void*>(&BitmapErase)},
};

const JNINativeMethod* FrameworkJniBitmapMethods(int& count)
{
    count = static_cast<int>(sizeof(BITMAP_METHODS) / sizeof(BITMAP_METHODS[0]));
    return BITMAP_METHODS;
}
