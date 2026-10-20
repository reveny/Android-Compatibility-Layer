#include <cstdio>
#include <cstdlib>
#include <cstring>

#include "Rasterizer/Rasterizer.hpp"

static int failures = 0;
static int checks = 0;

static void Expect(bool condition, const char* description)
{
    checks += 1;
    if (condition)
    {
        printf("  ok    %s\n", description);
        return;
    }

    printf("  FAIL  %s\n", description);
    failures += 1;
}

static uint32_t PixelAt(const Rasterizer::Surface& surface, int32_t x, int32_t y)
{
    const uint8_t* row = surface.pixels + static_cast<size_t>(y) * surface.stride;
    return reinterpret_cast<const uint32_t*>(row)[x];
}

static uint32_t AlphaOf(uint32_t pixel)
{
    return (pixel >> 24) & 0xFFu;
}

static void PrepareCanvas(Rasterizer::Canvas& canvas, Rasterizer::Surface& surface, uint32_t size)
{
    Rasterizer::CreateSurface(size, size, surface);
    std::memset(surface.pixels, 0, static_cast<size_t>(surface.stride) * surface.height);

    Rasterizer::Begin(surface, canvas);
}

static void FinishCanvas(Rasterizer::Canvas& canvas, Rasterizer::Surface& surface)
{
    Rasterizer::End(canvas);
    Rasterizer::DestroySurface(surface);
}

static void CheckTriangleFill()
{
    Rasterizer::Surface surface;
    Rasterizer::Canvas canvas;
    PrepareCanvas(canvas, surface, 64);

    Rasterizer::Path* path = Rasterizer::CreatePath();
    Rasterizer::MoveTo(*path, 32.0f, 4.0f);
    Rasterizer::LineTo(*path, 60.0f, 60.0f);
    Rasterizer::LineTo(*path, 4.0f, 60.0f);
    Rasterizer::CloseContour(*path);

    Rasterizer::Source source;
    source.color = 0xFFFF0000u;

    Rasterizer::Result drawn = Rasterizer::FillPath(canvas, *path, source, Rasterizer::FillRule::NonZero,
                                                    Rasterizer::BlendMode::SourceOver, 255);

    Expect(drawn == Rasterizer::Result::Ok, "a triangle fills without error");
    Expect(AlphaOf(PixelAt(surface, 32, 50)) == 255, "the middle of the triangle is opaque");
    Expect(AlphaOf(PixelAt(surface, 4, 8)) == 0, "a corner outside the triangle is untouched");

    uint32_t edge = AlphaOf(PixelAt(surface, 32, 4));
    Expect(edge > 0 && edge < 255, "the apex is antialiased rather than hard");

    Rasterizer::DestroyPath(path);
    FinishCanvas(canvas, surface);
}

static void CheckEvenOddRule()
{
    Rasterizer::Surface surface;
    Rasterizer::Canvas canvas;
    PrepareCanvas(canvas, surface, 64);

    Rasterizer::Path* path = Rasterizer::CreatePath();
    Rasterizer::AddRectangle(*path, 8.0f, 8.0f, 56.0f, 56.0f);
    Rasterizer::AddRectangle(*path, 20.0f, 20.0f, 44.0f, 44.0f);

    Rasterizer::Source source;
    source.color = 0xFF00FF00u;

    Rasterizer::FillPath(canvas, *path, source, Rasterizer::FillRule::EvenOdd, Rasterizer::BlendMode::SourceOver, 255);

    Expect(AlphaOf(PixelAt(surface, 12, 32)) == 255, "the ring is filled under the even odd rule");
    Expect(AlphaOf(PixelAt(surface, 32, 32)) == 0, "the hole is left empty under the even odd rule");

    Rasterizer::DestroyPath(path);
    FinishCanvas(canvas, surface);
}

static void CheckTransform()
{
    Rasterizer::Surface surface;
    Rasterizer::Canvas canvas;
    PrepareCanvas(canvas, surface, 64);

    Rasterizer::Transform move;
    move.translateX = 32.0f;
    Rasterizer::Concat(canvas, move);

    Rasterizer::Path* path = Rasterizer::CreatePath();
    Rasterizer::AddRectangle(*path, 0.0f, 0.0f, 16.0f, 16.0f);

    Rasterizer::Source source;
    source.color = 0xFF0000FFu;

    Rasterizer::FillPath(canvas, *path, source, Rasterizer::FillRule::NonZero, Rasterizer::BlendMode::SourceOver, 255);

    Expect(AlphaOf(PixelAt(surface, 40, 8)) == 255, "the translation moved the rectangle");
    Expect(AlphaOf(PixelAt(surface, 8, 8)) == 0, "nothing was drawn where the rectangle started");

    Rasterizer::DestroyPath(path);
    FinishCanvas(canvas, surface);
}

static void CheckClipPath()
{
    Rasterizer::Surface surface;
    Rasterizer::Canvas canvas;
    PrepareCanvas(canvas, surface, 64);

    Rasterizer::Path* clip = Rasterizer::CreatePath();
    Rasterizer::AddRectangle(*clip, 0.0f, 0.0f, 32.0f, 64.0f);
    Rasterizer::ClipPath(canvas, *clip, Rasterizer::FillRule::NonZero, false);

    Rasterizer::Path* path = Rasterizer::CreatePath();
    Rasterizer::AddRectangle(*path, 0.0f, 0.0f, 64.0f, 64.0f);

    Rasterizer::Source source;
    source.color = 0xFFFFFFFFu;

    Rasterizer::FillPath(canvas, *path, source, Rasterizer::FillRule::NonZero, Rasterizer::BlendMode::SourceOver, 255);

    Expect(AlphaOf(PixelAt(surface, 8, 32)) == 255, "the clipped side still draws");
    Expect(AlphaOf(PixelAt(surface, 48, 32)) == 0, "the far side is clipped away");

    Rasterizer::DestroyPath(path);
    Rasterizer::DestroyPath(clip);
    FinishCanvas(canvas, surface);
}

static void CheckLinearGradient()
{
    Rasterizer::Surface surface;
    Rasterizer::Canvas canvas;
    PrepareCanvas(canvas, surface, 64);

    Rasterizer::GradientStop stops[2];
    stops[0].position = 0.0f;
    stops[0].color = 0xFF000000u;
    stops[1].position = 1.0f;
    stops[1].color = 0xFFFFFFFFu;

    Rasterizer::Source source;
    source.kind = Rasterizer::SourceKind::LinearGradient;
    source.startX = 0.0f;
    source.endX = 64.0f;
    source.stops = stops;
    source.stopCount = 2;

    Rasterizer::Path* path = Rasterizer::CreatePath();
    Rasterizer::AddRectangle(*path, 0.0f, 0.0f, 64.0f, 64.0f);

    Rasterizer::FillPath(canvas, *path, source, Rasterizer::FillRule::NonZero, Rasterizer::BlendMode::Source, 255);

    uint32_t left = PixelAt(surface, 2, 32) & 0xFFu;
    uint32_t middle = PixelAt(surface, 32, 32) & 0xFFu;
    uint32_t right = PixelAt(surface, 61, 32) & 0xFFu;

    Expect(left < middle && middle < right, "the gradient runs dark to light across the shape");

    Rasterizer::DestroyPath(path);
    FinishCanvas(canvas, surface);
}

static void CheckStroke()
{
    Rasterizer::Surface surface;
    Rasterizer::Canvas canvas;
    PrepareCanvas(canvas, surface, 64);

    Rasterizer::Path* path = Rasterizer::CreatePath();
    Rasterizer::AddRectangle(*path, 16.0f, 16.0f, 48.0f, 48.0f);

    Rasterizer::Source source;
    source.color = 0xFFFFFFFFu;

    Rasterizer::StrokeStyle style;
    style.width = 4.0f;

    Rasterizer::Result drawn = Rasterizer::StrokePath(canvas, *path, source, style, Rasterizer::BlendMode::SourceOver, 255);

    Expect(drawn == Rasterizer::Result::Ok, "a rectangle strokes without error");
    Expect(AlphaOf(PixelAt(surface, 16, 32)) > 128, "the stroke covers the edge");
    Expect(AlphaOf(PixelAt(surface, 32, 32)) == 0, "the inside of a stroked rectangle stays empty");

    Rasterizer::DestroyPath(path);
    FinishCanvas(canvas, surface);
}

static void CheckBlur()
{
    const int32_t size = 32;
    uint8_t mask[size * size];
    std::memset(mask, 0, sizeof(mask));

    for (int32_t y = 12; y < 20; ++y)
    {
        for (int32_t x = 12; x < 20; ++x) mask[y * size + x] = 255;
    }

    Rasterizer::BlurMask(mask, size, size, size, 3.0f);

    Expect(mask[16 * size + 16] > 128, "the middle of a blurred square stays strong");
    Expect(mask[16 * size + 10] > 0, "the blur spread outside the original square");
    Expect(mask[16 * size + 2] == 0, "the blur did not reach the far edge");
}

static void CheckLayerAlpha()
{
    Rasterizer::Surface surface;
    Rasterizer::CreateSurface(64, 64, surface);
    std::memset(surface.pixels, 0, static_cast<size_t>(surface.stride) * surface.height);

    Rasterizer::Canvas canvas;
    Rasterizer::Begin(surface, canvas);

    Rasterizer::Path* path = Rasterizer::CreatePath();
    Rasterizer::AddRectangle(*path, 0.0f, 0.0f, 64.0f, 64.0f);

    Rasterizer::Source source;
    source.color = 0xFFFFFFFFu;

    Rasterizer::Result opened = Rasterizer::SaveLayer(canvas, Rasterizer::Rect{0, 0, 32, 64}, 128,
                                                      Rasterizer::BlendMode::SourceOver);
    Expect(opened == Rasterizer::Result::Ok, "a layer opens");
    Expect(canvas.surface.pixels != surface.pixels, "drawing is redirected off screen inside a layer");

    Rasterizer::FillPath(canvas, *path, source, Rasterizer::FillRule::NonZero, Rasterizer::BlendMode::SourceOver, 255);

    Expect(AlphaOf(PixelAt(surface, 8, 8)) == 0, "nothing reached the screen while the layer was open");

    Rasterizer::Result closed = Rasterizer::RestoreLayer(canvas);
    Expect(closed == Rasterizer::Result::Ok, "a layer closes");
    Expect(canvas.surface.pixels == surface.pixels, "drawing goes back to the screen after a layer");

    uint32_t inside = AlphaOf(PixelAt(surface, 8, 8));
    Expect(inside > 120 && inside < 136, "the layer composited back at half alpha");
    Expect(AlphaOf(PixelAt(surface, 48, 8)) == 0, "the layer bounds clipped what it composited");

    Rasterizer::DestroyPath(path);
    Rasterizer::End(canvas);
    Rasterizer::DestroySurface(surface);
}

static void CheckDeepStack()
{
    Rasterizer::Surface surface;
    Rasterizer::CreateSurface(64, 64, surface);
    std::memset(surface.pixels, 0, static_cast<size_t>(surface.stride) * surface.height);

    Rasterizer::Canvas canvas;
    Rasterizer::Begin(surface, canvas);

    bool saved = true;
    for (int index = 0; index < 300; ++index)
    {
        if (Rasterizer::Save(canvas) != Rasterizer::Result::Ok) saved = false;

        Rasterizer::Transform step;
        step.translateX = 1.0f;
        Rasterizer::Concat(canvas, step);
    }

    Expect(saved, "the state stack grows past its inline depth");
    Expect(canvas.depth == 301, "every save landed");
    Expect(Rasterizer::CurrentTransform(canvas).translateX == 300.0f, "the deep states carry their own transform");

    Rasterizer::Path* path = Rasterizer::CreatePath();
    Rasterizer::AddRectangle(*path, 0.0f, 0.0f, 8.0f, 8.0f);

    Rasterizer::Source source;
    source.color = 0xFFFFFFFFu;

    Rasterizer::FillPath(canvas, *path, source, Rasterizer::FillRule::NonZero, Rasterizer::BlendMode::SourceOver, 255);
    Expect(AlphaOf(PixelAt(surface, 32, 4)) == 0, "a fill at depth three hundred still respects the transform");

    bool restored = true;
    for (int index = 0; index < 300; ++index)
    {
        if (Rasterizer::Restore(canvas) != Rasterizer::Result::Ok) restored = false;
    }

    Expect(restored, "the deep stack unwinds");
    Expect(canvas.depth == 1, "the stack came back to where it started");

    Rasterizer::DestroyPath(path);
    Rasterizer::End(canvas);
    Rasterizer::DestroySurface(surface);
}

static void CheckClipSurvivesRestore()
{
    Rasterizer::Surface surface;
    Rasterizer::CreateSurface(64, 64, surface);
    std::memset(surface.pixels, 0, static_cast<size_t>(surface.stride) * surface.height);

    Rasterizer::Canvas canvas;
    Rasterizer::Begin(surface, canvas);

    Rasterizer::Path* clip = Rasterizer::CreatePath();
    Rasterizer::AddRectangle(*clip, 0.0f, 0.0f, 32.0f, 64.0f);
    Rasterizer::ClipPath(canvas, *clip, Rasterizer::FillRule::NonZero, false);

    Rasterizer::Save(canvas);

    Rasterizer::Path* inner = Rasterizer::CreatePath();
    Rasterizer::AddRectangle(*inner, 0.0f, 0.0f, 64.0f, 32.0f);
    Rasterizer::ClipPath(canvas, *inner, Rasterizer::FillRule::NonZero, false);

    Rasterizer::Restore(canvas);

    Rasterizer::Path* path = Rasterizer::CreatePath();
    Rasterizer::AddRectangle(*path, 0.0f, 0.0f, 64.0f, 64.0f);

    Rasterizer::Source source;
    source.color = 0xFFFFFFFFu;

    Rasterizer::FillPath(canvas, *path, source, Rasterizer::FillRule::NonZero, Rasterizer::BlendMode::SourceOver, 255);

    Expect(AlphaOf(PixelAt(surface, 8, 48)) == 255, "the outer clip survived the restore");
    Expect(AlphaOf(PixelAt(surface, 48, 48)) == 0, "the outer clip still clips after the restore");

    Rasterizer::DestroyPath(path);
    Rasterizer::DestroyPath(inner);
    Rasterizer::DestroyPath(clip);
    Rasterizer::End(canvas);
    Rasterizer::DestroySurface(surface);
}

static void CheckArc()
{
    Rasterizer::Surface surface;
    Rasterizer::Canvas canvas;
    PrepareCanvas(canvas, surface, 64);

    Rasterizer::Path* path = Rasterizer::CreatePath();
    Rasterizer::AddArc(*path, 0.0f, 0.0f, 64.0f, 64.0f, 0.0f, 90.0f, true);
    Rasterizer::LineTo(*path, 32.0f, 32.0f);
    Rasterizer::CloseContour(*path);

    Rasterizer::Source source;
    source.color = 0xFFFFFFFFu;

    Rasterizer::FillPath(canvas, *path, source, Rasterizer::FillRule::NonZero, Rasterizer::BlendMode::SourceOver, 255);

    Expect(AlphaOf(PixelAt(surface, 44, 44)) == 255, "the swept quarter is filled");
    Expect(AlphaOf(PixelAt(surface, 20, 44)) == 0, "the quarter before the sweep is empty");
    Expect(AlphaOf(PixelAt(surface, 44, 20)) == 0, "the quarter after the sweep is empty");
    Expect(AlphaOf(PixelAt(surface, 60, 60)) == 0, "the arc bulges outward, leaving the corner clear");

    FinishCanvas(canvas, surface);
    Rasterizer::DestroyPath(path);
}

static void CheckImageFilter()
{
    Rasterizer::Surface image;
    Rasterizer::CreateSurface(2, 2, image);
    for (int32_t y = 0; y < 2; ++y)
    {
        uint32_t* row = reinterpret_cast<uint32_t*>(image.pixels + static_cast<size_t>(y) * image.stride);
        row[0] = 0xFF000000u;
        row[1] = 0xFFFFFFFFu;
    }

    uint32_t nearest = 0;
    uint32_t smooth = 0;

    for (int pass = 0; pass < 2; ++pass)
    {
        Rasterizer::Surface surface;
        Rasterizer::Canvas canvas;
        PrepareCanvas(canvas, surface, 64);

        Rasterizer::Source source;
        source.kind = Rasterizer::SourceKind::Image;
        source.image = &image;
        source.filter = pass == 1;

        Rasterizer::Transform scale;
        scale.scaleX = 32.0f;
        scale.scaleY = 32.0f;
        Rasterizer::Invert(scale, source.inverse);

        Rasterizer::Path* path = Rasterizer::CreatePath();
        Rasterizer::AddRectangle(*path, 0.0f, 0.0f, 64.0f, 64.0f);

        Rasterizer::FillPath(canvas, *path, source, Rasterizer::FillRule::NonZero, Rasterizer::BlendMode::Source, 255);

        uint32_t sampled = PixelAt(surface, 32, 32) & 0xFFu;
        if (pass == 0) nearest = sampled;
        else smooth = sampled;

        Rasterizer::DestroyPath(path);
        FinishCanvas(canvas, surface);
    }

    Expect(nearest == 0 || nearest == 255, "nearest sampling lands on one texel or the other");
    Expect(smooth > 0 && smooth < 255, "bilinear sampling blends across the texel boundary");

    Rasterizer::DestroySurface(image);
}

static void CheckNegativeCoordinates()
{
    Rasterizer::Surface surface;
    Rasterizer::Canvas canvas;
    PrepareCanvas(canvas, surface, 64);

    Rasterizer::Path* path = Rasterizer::CreatePath();
    Rasterizer::AddRectangle(*path, -64.0f, -64.0f, 32.0f, 32.0f);

    Rasterizer::Source source;
    source.color = 0xFFFFFFFFu;

    Rasterizer::FillPath(canvas, *path, source, Rasterizer::FillRule::NonZero, Rasterizer::BlendMode::SourceOver, 255);

    Expect(AlphaOf(PixelAt(surface, 4, 4)) == 255, "a shape reaching past the top left still draws what is on screen");
    Expect(AlphaOf(PixelAt(surface, 48, 48)) == 0, "a shape reaching past the top left stops where it should");

    Rasterizer::DestroyPath(path);
    FinishCanvas(canvas, surface);
}

static void CheckClipRectFollowsTransform()
{
    Rasterizer::Surface surface;
    Rasterizer::Canvas canvas;
    PrepareCanvas(canvas, surface, 64);

    Rasterizer::Transform move;
    move.translateX = 32.0f;
    move.translateY = 32.0f;
    Rasterizer::Concat(canvas, move);

    Rasterizer::ClipRect(canvas, Rasterizer::Rect{0, 0, 16, 16});

    Rasterizer::Path* path = Rasterizer::CreatePath();
    Rasterizer::AddRectangle(*path, -64.0f, -64.0f, 64.0f, 64.0f);

    Rasterizer::Source source;
    source.color = 0xFFFFFFFFu;

    Rasterizer::FillPath(canvas, *path, source, Rasterizer::FillRule::NonZero, Rasterizer::BlendMode::SourceOver, 255);

    Expect(AlphaOf(PixelAt(surface, 40, 40)) == 255, "a clip rect lands where the transform put it");
    Expect(AlphaOf(PixelAt(surface, 8, 8)) == 0, "a clip rect does not stay at the origin the transform left");

    Rasterizer::DestroyPath(path);
    FinishCanvas(canvas, surface);
}

static void CheckLayerFollowsTransform()
{
    Rasterizer::Surface surface;
    Rasterizer::Canvas canvas;
    PrepareCanvas(canvas, surface, 64);

    Rasterizer::Transform move;
    move.translateX = 32.0f;
    Rasterizer::Concat(canvas, move);

    Rasterizer::SaveLayer(canvas, Rasterizer::Rect{0, 0, 32, 64}, 255, Rasterizer::BlendMode::SourceOver);

    Rasterizer::Path* path = Rasterizer::CreatePath();
    Rasterizer::AddRectangle(*path, 0.0f, 0.0f, 32.0f, 64.0f);

    Rasterizer::Source source;
    source.color = 0xFFFFFFFFu;

    Rasterizer::FillPath(canvas, *path, source, Rasterizer::FillRule::NonZero, Rasterizer::BlendMode::SourceOver, 255);
    Rasterizer::RestoreLayer(canvas);

    Expect(AlphaOf(PixelAt(surface, 40, 32)) == 255, "a layer keeps what the transform drew inside its bounds");
    Expect(AlphaOf(PixelAt(surface, 8, 32)) == 0, "a layer does not sit at the untransformed bounds");

    Rasterizer::DestroyPath(path);
    FinishCanvas(canvas, surface);
}

static void CheckClearBlend()
{
    Rasterizer::Surface surface;
    Rasterizer::Canvas canvas;
    PrepareCanvas(canvas, surface, 64);

    Rasterizer::Path* path = Rasterizer::CreatePath();
    Rasterizer::AddRectangle(*path, 0.0f, 0.0f, 64.0f, 64.0f);

    Rasterizer::Source white;
    white.color = 0xFFFFFFFFu;

    Rasterizer::FillPath(canvas, *path, white, Rasterizer::FillRule::NonZero, Rasterizer::BlendMode::Source, 255);
    Expect(AlphaOf(PixelAt(surface, 32, 32)) == 255, "the surface starts opaque before it is cleared");

    Rasterizer::ResetPath(*path);
    Rasterizer::AddRectangle(*path, 16.0f, 16.0f, 48.0f, 48.0f);

    Rasterizer::Source black;
    black.color = 0xFF000000u;

    Rasterizer::FillPath(canvas, *path, black, Rasterizer::FillRule::NonZero, Rasterizer::BlendMode::Clear, 255);

    Expect(AlphaOf(PixelAt(surface, 32, 32)) == 0, "clearing with black erases rather than painting black");
    Expect(PixelAt(surface, 32, 32) == 0, "a cleared pixel keeps no colour either");
    Expect(AlphaOf(PixelAt(surface, 4, 4)) == 255, "clearing leaves what it did not cover alone");

    Rasterizer::DestroyPath(path);
    FinishCanvas(canvas, surface);
}

static void CheckImageTint()
{
    Rasterizer::Surface image;
    Rasterizer::CreateSurface(2, 2, image);

    for (int32_t y = 0; y < 2; ++y)
    {
        uint32_t* row = reinterpret_cast<uint32_t*>(image.pixels + static_cast<size_t>(y) * image.stride);
        row[0] = 0xFF000000u;
        row[1] = 0x00000000u;
    }

    Rasterizer::Surface surface;
    Rasterizer::Canvas canvas;
    PrepareCanvas(canvas, surface, 64);

    Rasterizer::Source source;
    source.kind = Rasterizer::SourceKind::Image;
    source.image = &image;
    source.filter = false;
    source.tinted = true;
    source.tint = 0xFF2040FFu;

    Rasterizer::Transform scale;
    scale.scaleX = 32.0f;
    scale.scaleY = 32.0f;
    Rasterizer::Invert(scale, source.inverse);

    Rasterizer::Path* path = Rasterizer::CreatePath();
    Rasterizer::AddRectangle(*path, 0.0f, 0.0f, 64.0f, 64.0f);

    Rasterizer::FillPath(canvas, *path, source, Rasterizer::FillRule::NonZero, Rasterizer::BlendMode::Source, 255);

    uint32_t lit = PixelAt(surface, 8, 8);
    Expect(((lit >> 16) & 0xFF) == 0x20, "a tinted image takes the tint's red where the image is opaque");
    Expect((lit & 0xFF) == 0xFF, "a tinted image takes the tint's blue too");
    Expect(AlphaOf(lit) == 255, "an opaque texel stays opaque through the tint");
    Expect(AlphaOf(PixelAt(surface, 40, 8)) == 0, "a transparent texel stays transparent through the tint");

    Rasterizer::DestroyPath(path);
    FinishCanvas(canvas, surface);
    Rasterizer::DestroySurface(image);
}

int main()
{
    printf("vector rasterizer:\n");

    CheckTriangleFill();
    CheckEvenOddRule();
    CheckTransform();
    CheckClipPath();
    CheckLinearGradient();
    CheckStroke();
    CheckBlur();
    CheckLayerAlpha();
    CheckDeepStack();
    CheckClipSurvivesRestore();
    CheckArc();
    CheckImageFilter();
    CheckNegativeCoordinates();
    CheckClipRectFollowsTransform();
    CheckLayerFollowsTransform();
    CheckClearBlend();
    CheckImageTint();

    if (failures != 0)
    {
        printf("vector-unit FAILED: %d of %d checks failed\n", failures, checks);
        return 1;
    }

    printf("vector-unit PASSED: %d checks\n", checks);
    return 0;
}
