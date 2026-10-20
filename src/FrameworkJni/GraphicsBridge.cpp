#include "FrameworkJni/GraphicsBridge.hpp"

#include <vector>

#include "Rasterizer/Rasterizer.hpp"

static Rasterizer::Path* PathOf(jlong handle)
{
    return reinterpret_cast<Rasterizer::Path*>(static_cast<intptr_t>(handle));
}

struct BitmapHandle;

Rasterizer::Canvas* FrameworkJniCanvasFrom(jlong handle);
BitmapHandle* FrameworkJniBitmapFrom(jlong handle);
const Rasterizer::Surface* FrameworkJniBitmapSurface(BitmapHandle* bitmap);
const JNINativeMethod* FrameworkJniBitmapMethods(int& count);

static Rasterizer::Canvas* CanvasOf(jlong handle)
{
    return FrameworkJniCanvasFrom(handle);
}

static Rasterizer::Transform TransformFrom(JNIEnv* env, jfloatArray values)
{
    Rasterizer::Transform transform;
    if (!values || env->GetArrayLength(values) < 6) return transform;

    jfloat stored[9];
    env->GetFloatArrayRegion(values, 0, 6, stored);

    transform.scaleX = stored[0];
    transform.skewX = stored[1];
    transform.translateX = stored[2];
    transform.skewY = stored[3];
    transform.scaleY = stored[4];
    transform.translateY = stored[5];

    return transform;
}

static jlong PathCreate(JNIEnv*, jclass)
{
    return reinterpret_cast<jlong>(Rasterizer::CreatePath());
}

static void PathDestroy(JNIEnv*, jclass, jlong handle)
{
    Rasterizer::DestroyPath(PathOf(handle));
}

static void PathReset(JNIEnv*, jclass, jlong handle)
{
    Rasterizer::Path* path = PathOf(handle);
    if (path) Rasterizer::ResetPath(*path);
}

static jboolean PathIsEmpty(JNIEnv*, jclass, jlong handle)
{
    Rasterizer::Path* path = PathOf(handle);
    return path ? Rasterizer::PathIsEmpty(*path) : JNI_TRUE;
}

static void PathMoveTo(JNIEnv*, jclass, jlong handle, jfloat x, jfloat y)
{
    Rasterizer::Path* path = PathOf(handle);
    if (path) Rasterizer::MoveTo(*path, x, y);
}

static void PathLineTo(JNIEnv*, jclass, jlong handle, jfloat x, jfloat y)
{
    Rasterizer::Path* path = PathOf(handle);
    if (path) Rasterizer::LineTo(*path, x, y);
}

static void PathQuadTo(JNIEnv*, jclass, jlong handle, jfloat controlX, jfloat controlY, jfloat x, jfloat y)
{
    Rasterizer::Path* path = PathOf(handle);
    if (path) Rasterizer::QuadraticTo(*path, controlX, controlY, x, y);
}

static void PathCubicTo(JNIEnv*, jclass, jlong handle, jfloat firstX, jfloat firstY, jfloat secondX, jfloat secondY, jfloat x,
                        jfloat y)
{
    Rasterizer::Path* path = PathOf(handle);
    if (path) Rasterizer::CubicTo(*path, firstX, firstY, secondX, secondY, x, y);
}

static void PathClose(JNIEnv*, jclass, jlong handle)
{
    Rasterizer::Path* path = PathOf(handle);
    if (path) Rasterizer::CloseContour(*path);
}

static void PathAddRect(JNIEnv*, jclass, jlong handle, jfloat left, jfloat top, jfloat right, jfloat bottom)
{
    Rasterizer::Path* path = PathOf(handle);
    if (path) Rasterizer::AddRectangle(*path, left, top, right, bottom);
}

static void PathAddRoundRect(JNIEnv*, jclass, jlong handle, jfloat left, jfloat top, jfloat right, jfloat bottom,
                             jfloat radiusX, jfloat radiusY)
{
    Rasterizer::Path* path = PathOf(handle);
    if (path) Rasterizer::AddRoundedRectangle(*path, left, top, right, bottom, radiusX, radiusY);
}

static void PathAddOval(JNIEnv*, jclass, jlong handle, jfloat left, jfloat top, jfloat right, jfloat bottom)
{
    Rasterizer::Path* path = PathOf(handle);
    if (path) Rasterizer::AddOval(*path, left, top, right, bottom);
}

static void PathAddArc(JNIEnv*, jclass, jlong handle, jfloat left, jfloat top, jfloat right, jfloat bottom, jfloat start,
                       jfloat sweep, jboolean moveToStart)
{
    Rasterizer::Path* path = PathOf(handle);
    if (path) Rasterizer::AddArc(*path, left, top, right, bottom, start, sweep, moveToStart == JNI_TRUE);
}

static void PathAddPath(JNIEnv* env, jclass, jlong handle, jlong otherHandle, jfloatArray values)
{
    Rasterizer::Path* path = PathOf(handle);
    Rasterizer::Path* other = PathOf(otherHandle);
    if (!path || !other) return;

    Rasterizer::AddPath(*path, *other, TransformFrom(env, values));
}

static void PathTransform(JNIEnv* env, jclass, jlong handle, jfloatArray values)
{
    Rasterizer::Path* path = PathOf(handle);
    if (path) Rasterizer::TransformPath(*path, TransformFrom(env, values));
}

static void PathCurrentPoint(JNIEnv* env, jclass, jlong handle, jfloatArray into)
{
    Rasterizer::Path* path = PathOf(handle);
    if (!path || !into || env->GetArrayLength(into) < 2) return;

    float x = 0.0f;
    float y = 0.0f;
    Rasterizer::CurrentPoint(*path, x, y);

    jfloat point[2] = {x, y};
    env->SetFloatArrayRegion(into, 0, 2, point);
}

static jboolean PathComputeBounds(JNIEnv* env, jclass, jlong handle, jfloatArray into)
{
    Rasterizer::Path* path = PathOf(handle);
    if (!path || !into || env->GetArrayLength(into) < 4) return JNI_FALSE;

    float left = 0.0f;
    float top = 0.0f;
    float right = 0.0f;
    float bottom = 0.0f;

    if (!Rasterizer::PathBounds(*path, left, top, right, bottom)) return JNI_FALSE;

    jfloat bounds[4] = {left, top, right, bottom};
    env->SetFloatArrayRegion(into, 0, 4, bounds);

    return JNI_TRUE;
}

static Rasterizer::BlendMode BlendFrom(jint value)
{
    switch (value)
    {
        case 1: return Rasterizer::BlendMode::Source;
        case 2: return Rasterizer::BlendMode::SourceIn;
        case 3: return Rasterizer::BlendMode::SourceAtop;
        case 4: return Rasterizer::BlendMode::DestinationOver;
        case 5: return Rasterizer::BlendMode::DestinationOut;
        case 6: return Rasterizer::BlendMode::Multiply;
        case 7: return Rasterizer::BlendMode::Screen;
        case 8: return Rasterizer::BlendMode::Plus;
        case 9: return Rasterizer::BlendMode::Clear;
        default: return Rasterizer::BlendMode::SourceOver;
    }
}

static Rasterizer::TileMode TileFrom(jint value)
{
    switch (value)
    {
        case 1: return Rasterizer::TileMode::Repeat;
        case 2: return Rasterizer::TileMode::Mirror;
        default: return Rasterizer::TileMode::Clamp;
    }
}

struct PaintSnapshot
{
    Rasterizer::Source source;
    Rasterizer::StrokeStyle stroke;
    Rasterizer::Transform local;
    Rasterizer::BlendMode blend = Rasterizer::BlendMode::SourceOver;
    std::vector<Rasterizer::GradientStop> stops;
    uint8_t alpha = 255;
    int style = GraphicsBridge::STYLE_FILL;
};

static void ResolveShaderSpace(const Rasterizer::Canvas& canvas, PaintSnapshot& snapshot)
{
    if (snapshot.source.kind == Rasterizer::SourceKind::Solid) return;

    Rasterizer::Transform combined = Rasterizer::Multiply(Rasterizer::CurrentTransform(canvas), snapshot.local);
    if (!Rasterizer::Invert(combined, snapshot.source.inverse)) snapshot.source.inverse = Rasterizer::Transform();
}

static bool ReadPaint(JNIEnv* env, jintArray paintInts, jfloatArray paintFloats, jintArray stopColors,
                      jfloatArray stopPositions, PaintSnapshot& snapshot, jlong shaderBitmap = 0)
{
    if (!paintInts || !paintFloats) return false;
    if (env->GetArrayLength(paintInts) < GraphicsBridge::PAINT_INT_COUNT) return false;
    if (env->GetArrayLength(paintFloats) < GraphicsBridge::PAINT_FLOAT_COUNT) return false;

    jint integers[GraphicsBridge::PAINT_INT_COUNT];
    jfloat decimals[GraphicsBridge::PAINT_FLOAT_COUNT];

    env->GetIntArrayRegion(paintInts, 0, GraphicsBridge::PAINT_INT_COUNT, integers);
    env->GetFloatArrayRegion(paintFloats, 0, GraphicsBridge::PAINT_FLOAT_COUNT, decimals);

    snapshot.style = integers[GraphicsBridge::PAINT_STYLE];
    snapshot.blend = BlendFrom(integers[GraphicsBridge::PAINT_BLEND]);
    snapshot.alpha = static_cast<uint8_t>(integers[GraphicsBridge::PAINT_ALPHA] & 0xFF);

    snapshot.source.color = static_cast<uint32_t>(integers[GraphicsBridge::PAINT_COLOR]);
    snapshot.source.tileMode = TileFrom(integers[GraphicsBridge::PAINT_TILE_MODE]);
    snapshot.source.filter = integers[GraphicsBridge::PAINT_FILTER] != 0;
    snapshot.source.tinted = integers[GraphicsBridge::PAINT_TINTED] != 0;
    snapshot.source.tint = static_cast<uint32_t>(integers[GraphicsBridge::PAINT_TINT]);

    snapshot.stroke.width = decimals[GraphicsBridge::PAINT_STROKE_WIDTH];
    snapshot.stroke.miterLimit = decimals[GraphicsBridge::PAINT_MITER];

    switch (integers[GraphicsBridge::PAINT_CAP])
    {
        case 1: snapshot.stroke.cap = Rasterizer::LineCap::Round; break;
        case 2: snapshot.stroke.cap = Rasterizer::LineCap::Square; break;
        default: snapshot.stroke.cap = Rasterizer::LineCap::Butt; break;
    }

    switch (integers[GraphicsBridge::PAINT_JOIN])
    {
        case 1: snapshot.stroke.join = Rasterizer::LineJoin::Round; break;
        case 2: snapshot.stroke.join = Rasterizer::LineJoin::Bevel; break;
        default: snapshot.stroke.join = Rasterizer::LineJoin::Miter; break;
    }

    int shaderKind = integers[GraphicsBridge::PAINT_SHADER_KIND];
    if (shaderKind == GraphicsBridge::SHADER_NONE) return true;

    if (shaderKind == GraphicsBridge::SHADER_LINEAR) snapshot.source.kind = Rasterizer::SourceKind::LinearGradient;
    else if (shaderKind == GraphicsBridge::SHADER_RADIAL) snapshot.source.kind = Rasterizer::SourceKind::RadialGradient;
    else if (shaderKind == GraphicsBridge::SHADER_SWEEP) snapshot.source.kind = Rasterizer::SourceKind::SweepGradient;
    else if (shaderKind == GraphicsBridge::SHADER_BITMAP)
    {
        snapshot.source.kind = Rasterizer::SourceKind::Image;
        snapshot.source.image = FrameworkJniBitmapSurface(FrameworkJniBitmapFrom(shaderBitmap));

        if (!snapshot.source.image) snapshot.source.kind = Rasterizer::SourceKind::Solid;
    }

    snapshot.source.startX = decimals[GraphicsBridge::PAINT_START_X];
    snapshot.source.startY = decimals[GraphicsBridge::PAINT_START_Y];
    snapshot.source.endX = decimals[GraphicsBridge::PAINT_END_X];
    snapshot.source.endY = decimals[GraphicsBridge::PAINT_END_Y];
    snapshot.source.radius = decimals[GraphicsBridge::PAINT_RADIUS];

    snapshot.local.scaleX = decimals[GraphicsBridge::PAINT_LOCAL];
    snapshot.local.skewX = decimals[GraphicsBridge::PAINT_LOCAL + 1];
    snapshot.local.translateX = decimals[GraphicsBridge::PAINT_LOCAL + 2];
    snapshot.local.skewY = decimals[GraphicsBridge::PAINT_LOCAL + 3];
    snapshot.local.scaleY = decimals[GraphicsBridge::PAINT_LOCAL + 4];
    snapshot.local.translateY = decimals[GraphicsBridge::PAINT_LOCAL + 5];

    if (!stopColors || !stopPositions) return true;

    int stopCount = env->GetArrayLength(stopColors);
    if (stopCount > env->GetArrayLength(stopPositions)) stopCount = env->GetArrayLength(stopPositions);
    if (stopCount <= 0) return true;

    std::vector<jint> colors(stopCount);
    std::vector<jfloat> positions(stopCount);

    env->GetIntArrayRegion(stopColors, 0, stopCount, colors.data());
    env->GetFloatArrayRegion(stopPositions, 0, stopCount, positions.data());

    snapshot.stops.resize(stopCount);
    for (int index = 0; index < stopCount; ++index)
    {
        snapshot.stops[index].color = static_cast<uint32_t>(colors[index]);
        snapshot.stops[index].position = positions[index];
    }

    snapshot.source.stops = snapshot.stops.data();
    snapshot.source.stopCount = stopCount;

    return true;
}

static Rasterizer::FillRule RuleFrom(jint fillType)
{
    return fillType == 1 || fillType == 3 ? Rasterizer::FillRule::EvenOdd : Rasterizer::FillRule::NonZero;
}

static void PaintPath(Rasterizer::Canvas& canvas, const Rasterizer::Path& path, const PaintSnapshot& snapshot,
                      Rasterizer::FillRule rule)
{
    if (snapshot.style != GraphicsBridge::STYLE_STROKE)
    {
        Rasterizer::FillPath(canvas, path, snapshot.source, rule, snapshot.blend, snapshot.alpha);
    }

    if (snapshot.style != GraphicsBridge::STYLE_FILL)
    {
        Rasterizer::StrokePath(canvas, path, snapshot.source, snapshot.stroke, snapshot.blend, snapshot.alpha);
    }
}

static void CanvasDrawPath(JNIEnv* env, jclass, jlong canvasHandle, jlong pathHandle, jint fillType, jintArray paintInts,
                           jfloatArray paintFloats, jintArray stopColors, jfloatArray stopPositions, jfloat blurRadius,
                           jfloatArray dashIntervals, jfloat dashPhase, jlong shaderBitmap)
{
    Rasterizer::Canvas* canvas = CanvasOf(canvasHandle);
    Rasterizer::Path* path = PathOf(pathHandle);
    if (!canvas || !path) return;

    PaintSnapshot snapshot;
    if (!ReadPaint(env, paintInts, paintFloats, stopColors, stopPositions, snapshot, shaderBitmap)) return;

    ResolveShaderSpace(*canvas, snapshot);

    Rasterizer::Path* dashed = nullptr;

    if (dashIntervals && env->GetArrayLength(dashIntervals) >= 2)
    {
        int count = env->GetArrayLength(dashIntervals);
        std::vector<jfloat> spans(count);
        env->GetFloatArrayRegion(dashIntervals, 0, count, spans.data());

        dashed = Rasterizer::CreatePath();
        if (dashed && Rasterizer::DashPath(*path, *dashed, spans.data(), count, dashPhase) != Rasterizer::Result::Ok)
        {
            Rasterizer::DestroyPath(dashed);
            dashed = nullptr;
        }
    }

    const Rasterizer::Path& shape = dashed ? *dashed : *path;

    if (blurRadius > 0.0f)
    {
        Rasterizer::FillPathBlurred(*canvas, shape, snapshot.source, RuleFrom(fillType), snapshot.blend, snapshot.alpha,
                                    blurRadius);
    }
    else if (dashed)
    {
        Rasterizer::StrokePath(*canvas, shape, snapshot.source, snapshot.stroke, snapshot.blend, snapshot.alpha);
    }
    else
    {
        PaintPath(*canvas, shape, snapshot, RuleFrom(fillType));
    }

    if (dashed) Rasterizer::DestroyPath(dashed);
}

static void CanvasDrawBitmap(JNIEnv* env, jclass, jlong canvasHandle, jlong bitmapHandle, jfloat sourceLeft,
                             jfloat sourceTop, jfloat sourceRight, jfloat sourceBottom, jfloat destinationLeft,
                             jfloat destinationTop, jfloat destinationRight, jfloat destinationBottom, jintArray paintInts,
                             jfloatArray paintFloats)
{
    Rasterizer::Canvas* canvas = CanvasOf(canvasHandle);
    const Rasterizer::Surface* image = FrameworkJniBitmapSurface(FrameworkJniBitmapFrom(bitmapHandle));
    if (!canvas || !image || !image->pixels) return;

    float sourceWidth = sourceRight - sourceLeft;
    float sourceHeight = sourceBottom - sourceTop;
    float destinationWidth = destinationRight - destinationLeft;
    float destinationHeight = destinationBottom - destinationTop;

    if (sourceWidth <= 0.0f || sourceHeight <= 0.0f || destinationWidth <= 0.0f || destinationHeight <= 0.0f) return;

    PaintSnapshot snapshot;
    if (!ReadPaint(env, paintInts, paintFloats, nullptr, nullptr, snapshot)) return;

    Rasterizer::Transform placement;
    placement.scaleX = destinationWidth / sourceWidth;
    placement.scaleY = destinationHeight / sourceHeight;
    placement.translateX = destinationLeft - sourceLeft * placement.scaleX;
    placement.translateY = destinationTop - sourceTop * placement.scaleY;

    Rasterizer::Transform combined = Rasterizer::Multiply(Rasterizer::CurrentTransform(*canvas), placement);

    Rasterizer::Source source;
    source.kind = Rasterizer::SourceKind::Image;
    source.image = image;
    source.filter = snapshot.source.filter;
    source.tileMode = Rasterizer::TileMode::Clamp;
    source.tinted = snapshot.source.tinted;
    source.tint = snapshot.source.tint;

    if (!Rasterizer::Invert(combined, source.inverse)) return;

    Rasterizer::Path* shape = Rasterizer::CreatePath();
    if (!shape) return;

    Rasterizer::AddRectangle(*shape, destinationLeft, destinationTop, destinationRight, destinationBottom);
    Rasterizer::FillPath(*canvas, *shape, source, Rasterizer::FillRule::NonZero, snapshot.blend, snapshot.alpha);
    Rasterizer::DestroyPath(shape);
}

static void CanvasConcat(JNIEnv* env, jclass, jlong handle, jfloatArray values)
{
    Rasterizer::Canvas* canvas = CanvasOf(handle);
    if (canvas) Rasterizer::Concat(*canvas, TransformFrom(env, values));
}

static void CanvasSetMatrix(JNIEnv* env, jclass, jlong handle, jfloatArray values)
{
    Rasterizer::Canvas* canvas = CanvasOf(handle);
    if (canvas) Rasterizer::SetTransform(*canvas, TransformFrom(env, values));
}

static void CanvasGetMatrix(JNIEnv* env, jclass, jlong handle, jfloatArray into)
{
    Rasterizer::Canvas* canvas = CanvasOf(handle);
    if (!canvas || !into || env->GetArrayLength(into) < 6) return;

    const Rasterizer::Transform& transform = Rasterizer::CurrentTransform(*canvas);
    jfloat values[6] = {transform.scaleX, transform.skewX, transform.translateX,
                        transform.skewY, transform.scaleY, transform.translateY};

    env->SetFloatArrayRegion(into, 0, 6, values);
}

static jboolean CanvasClipPath(JNIEnv*, jclass, jlong canvasHandle, jlong pathHandle, jint fillType, jboolean subtract)
{
    Rasterizer::Canvas* canvas = CanvasOf(canvasHandle);
    Rasterizer::Path* path = PathOf(pathHandle);
    if (!canvas || !path) return JNI_FALSE;

    Rasterizer::Result outcome = Rasterizer::ClipPath(*canvas, *path, RuleFrom(fillType), subtract == JNI_TRUE);

    return outcome == Rasterizer::Result::Ok ? JNI_TRUE : JNI_FALSE;
}

static jint CanvasSaveLayer(JNIEnv*, jclass, jlong handle, jint left, jint top, jint right, jint bottom, jint alpha,
                            jint blend)
{
    Rasterizer::Canvas* canvas = CanvasOf(handle);
    if (!canvas) return -1;

    int depth = canvas->depth;

    Rasterizer::Rect bounds{left, top, right, bottom};
    Rasterizer::Result outcome =
        Rasterizer::SaveLayer(*canvas, bounds, static_cast<uint8_t>(alpha & 0xFF), BlendFrom(blend));

    if (outcome != Rasterizer::Result::Ok) return -1;

    return depth;
}

static void CanvasRestoreLayer(JNIEnv*, jclass, jlong handle)
{
    Rasterizer::Canvas* canvas = CanvasOf(handle);
    if (canvas) Rasterizer::RestoreLayer(*canvas);
}

static void CanvasClipBounds(JNIEnv* env, jclass, jlong handle, jintArray into)
{
    Rasterizer::Canvas* canvas = CanvasOf(handle);
    if (!canvas || !into || env->GetArrayLength(into) < 4) return;

    Rasterizer::Rect clip = Rasterizer::CurrentState(*canvas).clip;
    jint edges[4] = {clip.left, clip.top, clip.right, clip.bottom};

    env->SetIntArrayRegion(into, 0, 4, edges);
}

static jint CanvasLayerCount(JNIEnv*, jclass, jlong handle)
{
    Rasterizer::Canvas* canvas = CanvasOf(handle);
    return canvas ? canvas->layerCount : 0;
}

static const JNINativeMethod PATH_METHODS[] = {
    {"nativeCreate", "()J", reinterpret_cast<void*>(&PathCreate)},
    {"nativeDestroy", "(J)V", reinterpret_cast<void*>(&PathDestroy)},
    {"nativeReset", "(J)V", reinterpret_cast<void*>(&PathReset)},
    {"nativeIsEmpty", "(J)Z", reinterpret_cast<void*>(&PathIsEmpty)},
    {"nativeMoveTo", "(JFF)V", reinterpret_cast<void*>(&PathMoveTo)},
    {"nativeLineTo", "(JFF)V", reinterpret_cast<void*>(&PathLineTo)},
    {"nativeQuadTo", "(JFFFF)V", reinterpret_cast<void*>(&PathQuadTo)},
    {"nativeCubicTo", "(JFFFFFF)V", reinterpret_cast<void*>(&PathCubicTo)},
    {"nativeClose", "(J)V", reinterpret_cast<void*>(&PathClose)},
    {"nativeAddRect", "(JFFFF)V", reinterpret_cast<void*>(&PathAddRect)},
    {"nativeAddRoundRect", "(JFFFFFF)V", reinterpret_cast<void*>(&PathAddRoundRect)},
    {"nativeAddOval", "(JFFFF)V", reinterpret_cast<void*>(&PathAddOval)},
    {"nativeAddArc", "(JFFFFFFZ)V", reinterpret_cast<void*>(&PathAddArc)},
    {"nativeAddPath", "(JJ[F)V", reinterpret_cast<void*>(&PathAddPath)},
    {"nativeTransform", "(J[F)V", reinterpret_cast<void*>(&PathTransform)},
    {"nativeComputeBounds", "(J[F)Z", reinterpret_cast<void*>(&PathComputeBounds)},
    {"nativeCurrentPoint", "(J[F)V", reinterpret_cast<void*>(&PathCurrentPoint)},
};

static const JNINativeMethod CANVAS_METHODS[] = {
    {"nativeDrawPath", "(JJI[I[F[I[FF[FFJ)V", reinterpret_cast<void*>(&CanvasDrawPath)},
    {"nativeDrawBitmap", "(JJFFFFFFFF[I[F)V", reinterpret_cast<void*>(&CanvasDrawBitmap)},
    {"nativeConcat", "(J[F)V", reinterpret_cast<void*>(&CanvasConcat)},
    {"nativeSetMatrix", "(J[F)V", reinterpret_cast<void*>(&CanvasSetMatrix)},
    {"nativeGetMatrix", "(J[F)V", reinterpret_cast<void*>(&CanvasGetMatrix)},
    {"nativeClipPath", "(JJIZ)Z", reinterpret_cast<void*>(&CanvasClipPath)},
    {"nativeSaveLayer", "(JIIIIII)I", reinterpret_cast<void*>(&CanvasSaveLayer)},
    {"nativeRestoreLayer", "(J)V", reinterpret_cast<void*>(&CanvasRestoreLayer)},
    {"nativeLayerCount", "(J)I", reinterpret_cast<void*>(&CanvasLayerCount)},
    {"nativeClipBounds", "(J[I)V", reinterpret_cast<void*>(&CanvasClipBounds)},
};

static ArtRuntime::NativeMethodTable TABLES[3];

const ArtRuntime::NativeMethodTable* GraphicsBridge::Tables(int& count)
{
    int bitmapCount = 0;
    const JNINativeMethod* bitmapMethods = FrameworkJniBitmapMethods(bitmapCount);

    TABLES[0] = {PATH_CLASS, PATH_METHODS, static_cast<int>(sizeof(PATH_METHODS) / sizeof(PATH_METHODS[0]))};
    TABLES[1] = {CANVAS_CLASS, CANVAS_METHODS, static_cast<int>(sizeof(CANVAS_METHODS) / sizeof(CANVAS_METHODS[0]))};
    TABLES[2] = {BITMAP_CLASS, bitmapMethods, bitmapCount};

    count = 3;
    return TABLES;
}
