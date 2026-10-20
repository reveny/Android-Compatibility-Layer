#include "Rasterizer/Rasterizer.hpp"
#include "WaylandClient/WaylandClient.hpp"

#include <cstdio>
#include <cstring>

static int failures = 0;

static void Expect(bool condition, const char* label)
{
    if (condition)
    {
        printf("PASS %s\n", label);
        return;
    }

    printf("FAIL %s\n", label);
    failures += 1;
}

static bool PoolHolds(const WaylandClient::Surface& surface, uint32_t width, uint32_t height)
{
    uint32_t stride = width * WaylandClient::BYTES_PER_PIXEL;

    for (int index = 0; index < WaylandClient::BUFFER_COUNT; ++index)
    {
        const WaylandClient::Buffer& buffer = surface.buffers[index];

        if (buffer.buffer == nullptr) return false;
        if (buffer.width != width || buffer.height != height) return false;
        if (buffer.stride != stride) return false;
        if (buffer.sizeBytes != static_cast<size_t>(stride) * height) return false;
        if (buffer.pixels == nullptr) return false;
    }

    return true;
}

static bool BuffersLieInsidePool(const WaylandClient::Surface& surface)
{
    for (int index = 0; index < WaylandClient::BUFFER_COUNT; ++index)
    {
        const WaylandClient::Buffer& buffer = surface.buffers[index];

        if (buffer.pixels < surface.poolBase) return false;
        if (buffer.pixels + buffer.sizeBytes > surface.poolBase + surface.poolSizeBytes) return false;
    }

    return true;
}

int main()
{
    if (!WaylandClient::IsAvailable())
    {
        fprintf(stderr, "skip: WAYLAND_DISPLAY is unset\n");
        return 77;
    }

    WaylandClient::Connection connection;
    WaylandClient::Result connected = WaylandClient::Connect(connection);

    if (connected == WaylandClient::Result::NoDisplay)
    {
        fprintf(stderr, "skip: cannot reach a compositor: %s\n", WaylandClient::ResultName(connected));
        return 77;
    }

    if (connected != WaylandClient::Result::Ok)
    {
        fprintf(stderr, "fail: reached a compositor but it lacks what we need: %s\n", WaylandClient::ResultName(connected));
        return 1;
    }

    WaylandClient::Surface surface;
    WaylandClient::Result created = WaylandClient::CreateSurface(connection, 640, 480, "resize", surface);

    if (created != WaylandClient::Result::Ok)
    {
        fprintf(stderr, "fail: no surface: %s\n", WaylandClient::ResultName(created));
        WaylandClient::Disconnect(connection);
        return 1;
    }

    Expect(PoolHolds(surface, 640, 480), "the pool starts at the size it was asked for");
    Expect(BuffersLieInsidePool(surface), "every buffer starts inside the pool");

    const uint8_t* poolBefore = surface.poolBase;
    size_t poolBytesBefore = surface.poolSizeBytes;

    WaylandClient::Result grown = WaylandClient::Resize(connection, surface, 800, 600);
    Expect(grown == WaylandClient::Result::Ok, "growing the surface succeeds");
    Expect(surface.width == 800 && surface.height == 600, "the surface reports the size it grew to");
    Expect(PoolHolds(surface, 800, 600), "every buffer was rebuilt at the new size");
    Expect(BuffersLieInsidePool(surface), "every buffer lies inside the pool after growing");
    Expect(surface.poolSizeBytes > poolBytesBefore, "the pool actually grew");
    Expect(surface.poolBase != poolBefore || surface.poolSizeBytes != poolBytesBefore, "the old mapping was replaced");

    uint32_t reportedWidth = 0;
    uint32_t reportedHeight = 0;
    Expect(WaylandClient::ConsumeResize(surface, reportedWidth, reportedHeight), "the resize is reported once");
    Expect(reportedWidth == 800 && reportedHeight == 600, "the reported size is the new one");
    Expect(!WaylandClient::ConsumeResize(surface, reportedWidth, reportedHeight), "the resize is not reported twice");

    WaylandClient::Result shrunk = WaylandClient::Resize(connection, surface, 320, 240);
    Expect(shrunk == WaylandClient::Result::Ok, "shrinking the surface succeeds");
    Expect(PoolHolds(surface, 320, 240), "every buffer was rebuilt at the smaller size");
    Expect(BuffersLieInsidePool(surface), "every buffer lies inside the pool after shrinking");
    Expect(WaylandClient::ConsumeResize(surface, reportedWidth, reportedHeight), "shrinking is reported too");
    Expect(reportedWidth == 320 && reportedHeight == 240, "the reported size is the smaller one");

    WaylandClient::Result unchanged = WaylandClient::Resize(connection, surface, 320, 240);
    Expect(unchanged == WaylandClient::Result::Ok, "resizing to the same size is accepted");
    Expect(!WaylandClient::ConsumeResize(surface, reportedWidth, reportedHeight), "resizing to the same size reports nothing");

    WaylandClient::Result tooSmall = WaylandClient::Resize(connection, surface, 0, 240);
    Expect(tooSmall == WaylandClient::Result::InvalidGeometry, "a zero dimension is refused");
    Expect(PoolHolds(surface, 320, 240), "a refused resize leaves the buffers alone");

    WaylandClient::Buffer* buffer = nullptr;
    Expect(WaylandClient::AcquireBuffer(surface, buffer) == WaylandClient::Result::Ok, "a buffer can still be acquired after resizing");

    if (buffer != nullptr)
    {
        Rasterizer::Surface target;
        Rasterizer::WrapSurface(buffer->pixels, buffer->width, buffer->height, buffer->stride, target);

        Rasterizer::Canvas canvas;
        Rasterizer::Begin(target, canvas);
        Rasterizer::DrawColor(canvas, 0xFF204080u);

        uint32_t centre = 0;
        memcpy(&centre, buffer->pixels + static_cast<size_t>(buffer->height / 2) * buffer->stride + (buffer->width / 2) * 4,
               sizeof(centre));

        Expect(centre == 0xFF204080u, "the resized buffer can be drawn into");
    }

    WaylandClient::DestroySurface(surface);
    WaylandClient::Disconnect(connection);

    printf("%s: %d failure(s)\n", failures == 0 ? "window-resize PASSED" : "window-resize FAILED", failures);
    return failures == 0 ? 0 : 1;
}
