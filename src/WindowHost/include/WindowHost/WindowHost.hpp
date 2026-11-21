#pragma once

#include <cstdint>

#include <sys/types.h>

#include "ControlChannel/ControlChannel.hpp"

namespace WindowHost
{
    inline constexpr const char* WINDOW_HOST_SETTING = "window-host";
    inline constexpr int STOP_GRACE_MILLIS = 3000;
    inline constexpr int FENCE_TIMEOUT_MILLIS = 100;
    inline constexpr uint32_t MAX_HOSTED_BUFFERS = 32;
    inline constexpr int POLL_EVENT_CAPACITY = 8;

    bool IsWanted();
    int Run(ControlChannel::Endpoint& channel, pid_t app);
}
