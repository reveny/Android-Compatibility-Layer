#include "Rasterizer/Rasterizer.hpp"
#include "WaylandClient/WaylandClient.hpp"

#include <cstdio>
#include <cstdlib>
#include <cstring>

static void PaintFrame(Rasterizer::Canvas& canvas, uint32_t width, uint32_t height, int frame)
{
    Rasterizer::DrawColor(canvas, 0xFF101828u);

    int32_t inset = 24 + frame * 4;
    Rasterizer::DrawRect(canvas, Rasterizer::Rect{inset, inset, static_cast<int32_t>(width) - inset, static_cast<int32_t>(height) - inset},
                         0xFFE04050u);

    Rasterizer::DrawRectOutline(canvas, Rasterizer::Rect{8, 8, static_cast<int32_t>(width) - 8, static_cast<int32_t>(height) - 8}, 4, 0xFF40C060u);
}

int main(int argc, char** argv)
{
    int frames = argc > 1 ? atoi(argv[1]) : 3;
    const char* dumpPath = argc > 2 ? argv[2] : nullptr;

    if (!WaylandClient::IsAvailable())
    {
        fprintf(stderr, "skip: WAYLAND_DISPLAY is unset\n");
        return 77;
    }

    WaylandClient::Connection connection;
    WaylandClient::Result result = WaylandClient::Connect(connection);
    if (result == WaylandClient::Result::NoDisplay)
    {
        fprintf(stderr, "skip: cannot reach a compositor: %s\n", WaylandClient::ResultName(result));
        return 77;
    }

    if (result != WaylandClient::Result::Ok)
    {
        fprintf(stderr, "fail: reached a compositor but it lacks what we need: %s\n", WaylandClient::ResultName(result));
        return 1;
    }

    printf("connected: compositor=%p shell=%p shm=%p presentation=%p\n", (void*)connection.compositor, (void*)connection.shell,
           (void*)connection.sharedMemory, (void*)connection.presentation);

    const uint32_t width = 320;
    const uint32_t height = 240;

    WaylandClient::Surface surface;
    result = WaylandClient::CreateSurface(connection, width, height, "acl window smoke test", surface);
    if (result != WaylandClient::Result::Ok)
    {
        fprintf(stderr, "error: CreateSurface: %s\n", WaylandClient::ResultName(result));
        WaylandClient::Disconnect(connection);
        return 1;
    }

    printf("surface created %ux%u configured=%d\n", surface.width, surface.height, surface.configured ? 1 : 0);

    for (int frame = 0; frame < frames; ++frame)
    {
        WaylandClient::Buffer* buffer = nullptr;
        result = WaylandClient::AcquireBuffer(surface, buffer);
        if (result != WaylandClient::Result::Ok)
        {
            WaylandClient::Roundtrip(connection);
            result = WaylandClient::AcquireBuffer(surface, buffer);
        }
        if (result != WaylandClient::Result::Ok)
        {
            fprintf(stderr, "error: AcquireBuffer: %s\n", WaylandClient::ResultName(result));
            WaylandClient::DestroySurface(surface);
            WaylandClient::Disconnect(connection);
            return 1;
        }

        Rasterizer::Surface target;
        Rasterizer::WrapSurface(buffer->pixels, buffer->width, buffer->height, buffer->stride, target);

        Rasterizer::Canvas canvas;
        Rasterizer::Begin(target, canvas);
        PaintFrame(canvas, width, height, frame);

        if (dumpPath && frame == frames - 1)
        {
            FILE* out = fopen(dumpPath, "wb");
            if (out)
            {
                fwrite(buffer->pixels, 1, buffer->sizeBytes, out);
                fclose(out);
                printf("dumped frame %d to %s\n", frame, dumpPath);
            }
        }

        result = WaylandClient::Present(connection, surface, *buffer);
        if (result != WaylandClient::Result::Ok)
        {
            fprintf(stderr, "error: Present: %s\n", WaylandClient::ResultName(result));
            WaylandClient::DestroySurface(surface);
            WaylandClient::Disconnect(connection);
            return 1;
        }

        WaylandClient::Roundtrip(connection);
        printf("presented frame %d\n", frame);

        if (surface.closeRequested)
        {
            printf("compositor asked us to close\n");
            break;
        }
    }

    WaylandClient::DestroySurface(surface);
    WaylandClient::Disconnect(connection);

    printf("window smoke test finished\n");
    return 0;
}
