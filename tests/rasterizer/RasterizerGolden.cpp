#include "Rasterizer/Rasterizer.hpp"

#include <cstdio>
#include <cstdlib>
#include <cstring>

static int failures = 0;

static void Check(const char* name, bool ok)
{
    printf("%s %s\n", ok ? "PASS" : "FAIL", name);
    if (!ok) failures++;
}

static const int RAMP_WIDTH = 60;
static const int RAMP_HEIGHT = 14;

static void BuildCoverageRamp(uint8_t* samples)
{
    for (int row = 0; row < RAMP_HEIGHT; ++row)
    {
        for (int column = 0; column < RAMP_WIDTH; ++column)
        {
            int value = (column * 255) / (RAMP_WIDTH - 1);
            if (row < 2 || row >= RAMP_HEIGHT - 2) value = value / 4;
            samples[row * RAMP_WIDTH + column] = static_cast<uint8_t>(value);
        }
    }
}

static void PaintPattern(Rasterizer::Canvas& canvas)
{
    Rasterizer::DrawColor(canvas, 0xFF101828u);

    Rasterizer::DrawRect(canvas, Rasterizer::Rect{8, 8, 56, 40}, 0xFFE04050u);
    Rasterizer::DrawRect(canvas, Rasterizer::Rect{60, 8, 108, 40}, 0xFF40C060u);

    Rasterizer::Save(canvas);
    Rasterizer::Translate(canvas, 8, 48);
    Rasterizer::ClipRect(canvas, Rasterizer::Rect{0, 0, 40, 24});
    Rasterizer::DrawRect(canvas, Rasterizer::Rect{-10, -10, 100, 100}, 0xFF4080F0u);
    Rasterizer::Restore(canvas);

    Rasterizer::DrawRectOutline(canvas, Rasterizer::Rect{60, 48, 108, 72}, 2, 0xFFF0C040u);

    uint8_t ramp[RAMP_HEIGHT * RAMP_WIDTH];
    BuildCoverageRamp(ramp);

    Rasterizer::Coverage mask;
    mask.samples = ramp;
    mask.width = RAMP_WIDTH;
    mask.height = RAMP_HEIGHT;
    mask.pitch = RAMP_WIDTH;

    Rasterizer::DrawCoverage(canvas, mask, 8, 80, 0xFFFFFFFFu);
    Rasterizer::DrawRectBlend(canvas, Rasterizer::Rect{72, 80, 108, 92}, 0x80FF00FFu);

    Rasterizer::DrawHorizontalLine(canvas, 8, 100, 100, 0xFF808080u);
    Rasterizer::DrawVerticalLine(canvas, 116, 8, 100, 0xFF808080u);
}

static void TestClipping()
{
    Rasterizer::Surface surface;
    Rasterizer::CreateSurface(32, 32, surface);

    Rasterizer::Canvas canvas;
    Rasterizer::Begin(surface, canvas);

    Rasterizer::DrawColor(canvas, 0xFF000000u);
    Rasterizer::Save(canvas);
    Rasterizer::ClipRect(canvas, Rasterizer::Rect{8, 8, 16, 16});
    Rasterizer::DrawRect(canvas, Rasterizer::Rect{0, 0, 32, 32}, 0xFFFFFFFFu);
    Rasterizer::Restore(canvas);

    Check("clip confines the fill", Rasterizer::PixelAt(surface, 10, 10) == 0xFFFFFFFFu && Rasterizer::PixelAt(surface, 7, 10) == 0xFF000000u &&
                                        Rasterizer::PixelAt(surface, 16, 16) == 0xFF000000u);

    Rasterizer::DrawRect(canvas, Rasterizer::Rect{0, 0, 32, 32}, 0xFF223344u);
    Check("restore releases the clip", Rasterizer::PixelAt(surface, 0, 0) == 0xFF223344u);

    Rasterizer::DestroySurface(surface);
}

static void TestBoundsAndTranslate()
{
    Rasterizer::Surface surface;
    Rasterizer::CreateSurface(16, 16, surface);

    Rasterizer::Canvas canvas;
    Rasterizer::Begin(surface, canvas);
    Rasterizer::DrawColor(canvas, 0u);

    Rasterizer::DrawRect(canvas, Rasterizer::Rect{-100, -100, 100, 100}, 0xFFAABBCCu);
    Check("oversized rect is clamped, not overflowing",
          Rasterizer::PixelAt(surface, 0, 0) == 0xFFAABBCCu && Rasterizer::PixelAt(surface, 15, 15) == 0xFFAABBCCu);

    Rasterizer::DrawColor(canvas, 0u);
    Rasterizer::Translate(canvas, 4, 4);
    Rasterizer::DrawRect(canvas, Rasterizer::Rect{0, 0, 4, 4}, 0xFF010203u);
    Check("translate offsets drawing", Rasterizer::PixelAt(surface, 4, 4) == 0xFF010203u && Rasterizer::PixelAt(surface, 3, 3) == 0u);

    Check("state stack underflow is reported", Rasterizer::Restore(canvas) == Rasterizer::Result::StateStackUnderflow);

    Rasterizer::DestroySurface(surface);
}

static void TestDegenerate()
{
    Rasterizer::Surface surface;
    Check("zero width is rejected", Rasterizer::CreateSurface(0, 8, surface) == Rasterizer::Result::InvalidGeometry);
    Check("oversized is rejected", Rasterizer::CreateSurface(99999, 8, surface) == Rasterizer::Result::InvalidGeometry);

    Rasterizer::Surface real;
    Rasterizer::CreateSurface(8, 8, real);
    Rasterizer::Canvas canvas;
    Rasterizer::Begin(real, canvas);
    Rasterizer::DrawColor(canvas, 0xFF000000u);

    Rasterizer::DrawRect(canvas, Rasterizer::Rect{4, 4, 4, 4}, 0xFFFFFFFFu);
    Check("empty rect draws nothing", Rasterizer::PixelAt(real, 4, 4) == 0xFF000000u);

    Rasterizer::DrawRect(canvas, Rasterizer::Rect{6, 6, 2, 2}, 0xFFFFFFFFu);
    Check("inverted rect draws nothing", Rasterizer::PixelAt(real, 3, 3) == 0xFF000000u);

    Rasterizer::DestroySurface(real);
}

static int SaveReturningPreviousDepth(Rasterizer::Canvas& canvas)
{
    int before = canvas.depth;
    if (Rasterizer::Save(canvas) != Rasterizer::Result::Ok) return -1;
    return before;
}

static void RestoreToCount(Rasterizer::Canvas& canvas, int saveCount)
{
    while (canvas.depth > saveCount)
    {
        if (Rasterizer::Restore(canvas) != Rasterizer::Result::Ok) return;
    }
}

static void DrawSibling(Rasterizer::Canvas& canvas, int left, int top, int width, int height, uint32_t color)
{
    int saveCount = SaveReturningPreviousDepth(canvas);
    Rasterizer::Translate(canvas, left, top);
    Rasterizer::ClipRect(canvas, Rasterizer::Rect{0, 0, width, height});
    Rasterizer::DrawRect(canvas, Rasterizer::Rect{0, 0, width, height}, color);
    RestoreToCount(canvas, saveCount);
}

static void TestSiblingIsolation()
{
    Rasterizer::Surface surface;
    Rasterizer::CreateSurface(200, 200, surface);

    Rasterizer::Canvas canvas;
    Rasterizer::Begin(surface, canvas);
    Rasterizer::DrawColor(canvas, 0xFF000000u);

    int startDepth = canvas.depth;

    DrawSibling(canvas, 0, 0, 100, 100, 0xFFFF0000u);
    DrawSibling(canvas, 100, 0, 100, 100, 0xFF00FF00u);
    DrawSibling(canvas, 0, 100, 100, 100, 0xFF0000FFu);

    Check("first sibling drawn", Rasterizer::PixelAt(surface, 50, 50) == 0xFFFF0000u);
    Check("second sibling drawn, not clipped away by the first", Rasterizer::PixelAt(surface, 150, 50) == 0xFF00FF00u);
    Check("third sibling drawn", Rasterizer::PixelAt(surface, 50, 150) == 0xFF0000FFu);
    Check("siblings do not bleed into each other", Rasterizer::PixelAt(surface, 150, 150) == 0xFF000000u);
    Check("save/restore returns to the starting depth", canvas.depth == startDepth);

    for (int index = 0; index < Rasterizer::MAX_STATE_DEPTH * 2; index++)
    {
        DrawSibling(canvas, 0, 0, 8, 8, 0xFF808080u);
    }
    Check("a long sibling run does not grow the state stack", canvas.depth == startDepth);

    Rasterizer::DestroySurface(surface);
}

static void TestStateStackLimits()
{
    Rasterizer::Surface surface;
    Rasterizer::CreateSurface(8, 8, surface);

    Rasterizer::Canvas canvas;
    Rasterizer::Begin(surface, canvas);

    int wanted = Rasterizer::MAX_STATE_DEPTH * 2;
    bool saved = true;

    for (int index = 0; index < wanted; ++index)
    {
        if (Rasterizer::Save(canvas) != Rasterizer::Result::Ok) saved = false;
        Rasterizer::Translate(canvas, 1, 0);
    }

    Check("save keeps working past the inline depth", saved && canvas.depth == wanted + 1);
    Check("a state past the inline depth does not share its parent",
          Rasterizer::CurrentState(canvas).translateX == wanted);

    while (Rasterizer::Restore(canvas) == Rasterizer::Result::Ok)
    {
    }

    Check("restore stops at the base state", canvas.depth == 1);
    Check("the base state was left alone", Rasterizer::CurrentState(canvas).translateX == 0);

    Rasterizer::End(canvas);

    Rasterizer::DestroySurface(surface);
}

int main(int argc, char** argv)
{
    if (argc < 2)
    {
        fprintf(stderr, "usage: %s <golden.raw> [--regenerate]\n", argv[0]);
        return 2;
    }

    const char* goldenPath = argv[1];
    bool regenerate = argc > 2 && strcmp(argv[2], "--regenerate") == 0;

    Rasterizer::Surface surface;
    if (Rasterizer::CreateSurface(128, 112, surface) != Rasterizer::Result::Ok)
    {
        fprintf(stderr, "could not allocate the pattern surface\n");
        return 1;
    }

    Rasterizer::Canvas canvas;
    Rasterizer::Begin(surface, canvas);
    PaintPattern(canvas);

    size_t sizeBytes = static_cast<size_t>(surface.stride) * surface.height;

    if (regenerate)
    {
        FILE* out = fopen(goldenPath, "wb");
        if (!out)
        {
            fprintf(stderr, "cannot write %s\n", goldenPath);
            return 1;
        }
        fwrite(surface.pixels, 1, sizeBytes, out);
        fclose(out);
        printf("regenerated %s (%zu bytes)\n", goldenPath, sizeBytes);
        Rasterizer::DestroySurface(surface);
        return 0;
    }

    FILE* in = fopen(goldenPath, "rb");
    if (!in)
    {
        fprintf(stderr, "golden image missing: %s (run with --regenerate)\n", goldenPath);
        Rasterizer::DestroySurface(surface);
        return 1;
    }

    uint8_t* expected = static_cast<uint8_t*>(malloc(sizeBytes));
    size_t read = fread(expected, 1, sizeBytes, in);
    long extra = fgetc(in) == EOF ? 0 : 1;
    fclose(in);

    if (read != sizeBytes || extra != 0)
    {
        fprintf(stderr, "golden image is %zu bytes, expected %zu\n", read + static_cast<size_t>(extra), sizeBytes);
        free(expected);
        Rasterizer::DestroySurface(surface);
        return 1;
    }

    size_t differing = 0;
    for (size_t index = 0; index < sizeBytes; index += 4)
    {
        if (memcmp(surface.pixels + index, expected + index, 4) != 0) differing++;
    }

    Check("pattern matches the committed golden image", differing == 0);
    if (differing != 0) fprintf(stderr, "%zu of %zu pixels differ\n", differing, sizeBytes / 4);

    free(expected);
    Rasterizer::DestroySurface(surface);

    TestClipping();
    TestBoundsAndTranslate();
    TestDegenerate();
    TestSiblingIsolation();
    TestStateStackLimits();

    printf(failures == 0 ? "ALL PASS\n" : "%d FAILURES\n", failures);
    return failures == 0 ? 0 : 1;
}
