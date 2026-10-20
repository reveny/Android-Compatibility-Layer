#include "WaylandClient/WaylandClient.hpp"

WaylandClient::Result WaylandClient::Connect(Connection&)
{
    return Result::NoDisplay;
}

void WaylandClient::Disconnect(Connection&)
{
}

WaylandClient::Result WaylandClient::CreateSurface(Connection&, uint32_t, uint32_t, const char*, Surface&)
{
    return Result::NotConnected;
}

void WaylandClient::DestroySurface(Surface&)
{
}

WaylandClient::DisplayMode WaylandClient::CurrentDisplayMode(const Connection&)
{
    return DisplayMode{};
}

WaylandClient::Result WaylandClient::Resize(Connection&, Surface&, uint32_t, uint32_t)
{
    return Result::NotConnected;
}

WaylandClient::Result WaylandClient::SetFullscreen(Surface&, bool)
{
    return Result::NotConnected;
}

bool WaylandClient::ConsumeResize(Surface&, uint32_t&, uint32_t&)
{
    return false;
}

WaylandClient::Result WaylandClient::AcquireBuffer(Surface&, Buffer*&)
{
    return Result::NotConnected;
}

WaylandClient::Result WaylandClient::Present(Connection&, Surface&, Buffer&)
{
    return Result::NotConnected;
}

WaylandClient::Result WaylandClient::RequestFrame(Connection&, Surface&, FrameCallback, void*)
{
    return Result::NotConnected;
}

bool WaylandClient::NextInputEvent(Connection& connection, InputEvent& event)
{
    if (connection.queueCount == 0) return false;

    event = connection.queued[connection.queueHead];
    connection.queueHead = (connection.queueHead + 1) % MAX_QUEUED_INPUT;
    connection.queueCount -= 1;

    return true;
}

void WaylandClient::QueueInputEvent(Connection& connection, const InputEvent& event)
{
    if (connection.queueCount == MAX_QUEUED_INPUT)
    {
        connection.queueHead = (connection.queueHead + 1) % MAX_QUEUED_INPUT;
        connection.queueCount -= 1;
        connection.queueDropped += 1;
    }

    int slot = (connection.queueHead + connection.queueCount) % MAX_QUEUED_INPUT;
    connection.queued[slot] = event;
    connection.queueCount += 1;
}

WaylandClient::Result WaylandClient::Dispatch(Connection&)
{
    return Result::NotConnected;
}

WaylandClient::Result WaylandClient::DispatchPending(Connection&)
{
    return Result::NotConnected;
}

WaylandClient::Result WaylandClient::Flush(Connection&)
{
    return Result::NotConnected;
}

WaylandClient::Result WaylandClient::Roundtrip(Connection&)
{
    return Result::NotConnected;
}

bool WaylandClient::IsAvailable()
{
    return false;
}

const char* WaylandClient::ResultName(Result result)
{
    switch (result)
    {
        case Result::Ok: return "Ok";
        case Result::NoDisplay: return "NoDisplay";
        case Result::NoCompositor: return "NoCompositor";
        case Result::NoShell: return "NoShell";
        case Result::NoSharedMemory: return "NoSharedMemory";
        case Result::AlreadyConnected: return "AlreadyConnected";
        case Result::NotConnected: return "NotConnected";
        case Result::InvalidGeometry: return "InvalidGeometry";
        case Result::MappingFailed: return "MappingFailed";
        case Result::NoFreeBuffer: return "NoFreeBuffer";
        case Result::DispatchFailed: return "DispatchFailed";
        case Result::FlushFailed: return "FlushFailed";
    }

    return "Unknown";
}
