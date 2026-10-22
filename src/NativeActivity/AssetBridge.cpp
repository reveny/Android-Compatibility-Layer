#include "NativeActivity/NativeActivity.hpp"

#include <algorithm>
#include <cstdio>
#include <cstring>
#include <memory>
#include <mutex>
#include <set>
#include <string>
#include <unistd.h>
#include <vector>

#include "AbiBridge/AbiBridge.hpp"
#include "Apk/Apk.hpp"
#include "GuestRuntime/GuestRuntime.hpp"

static constexpr int32_t ASSET_SEEK_SET = 0;
static constexpr int32_t ASSET_SEEK_CURRENT = 1;
static constexpr int32_t ASSET_SEEK_END = 2;
static constexpr int32_t ASSET_SKIP_CHUNK_BYTES = 65536;
static constexpr uint64_t MAXIMUM_ASSET_PATH_BYTES = 4096;

struct GuestAsset
{
    std::mutex lock;
    std::string path;
    Apk::Asset asset;
    int64_t position = 0;
    std::vector<uint8_t> content;
    bool loaded = false;
};

struct GuestAssetDirectory
{
    std::vector<std::string> names;
    size_t next = 0;
    std::string current;
};

static const Apk::Package* gAssetPackage = nullptr;
static std::mutex gAssetLock;
static std::set<GuestAsset*> gLiveAssets;
static std::set<GuestAssetDirectory*> gLiveDirectories;

static bool ReadGuestPath(const GuestState::MemoryAccessor& memory, uint64_t address, std::string& path)
{
    path.clear();
    if (address == 0) return false;

    for (uint64_t offset = 0; offset < MAXIMUM_ASSET_PATH_BYTES; ++offset)
    {
        char character = 0;
        if (!memory.Read(memory.context, (GuestState::GuestAddress)(address + offset), &character, 1)) return false;
        if (character == '\0') return true;

        path.push_back(character);
    }

    return false;
}

static GuestAsset* LiveAsset(uint64_t handle)
{
    std::lock_guard<std::mutex> guard(gAssetLock);

    GuestAsset* asset = (GuestAsset*)handle;
    return gLiveAssets.count(asset) ? asset : nullptr;
}

static GuestAssetDirectory* LiveDirectory(uint64_t handle)
{
    std::lock_guard<std::mutex> guard(gAssetLock);

    GuestAssetDirectory* directory = (GuestAssetDirectory*)handle;
    return gLiveDirectories.count(directory) ? directory : nullptr;
}

static bool LoadWholeAsset(GuestAsset& asset)
{
    if (asset.loaded) return true;

    Apk::Asset fresh;
    if (Apk::OpenAsset(*gAssetPackage, asset.path, fresh) != Apk::Result::Ok) return false;

    std::vector<uint8_t> content((size_t)(fresh.length > 0 ? fresh.length : 0));
    size_t filled = 0;

    while (filled < content.size())
    {
        int wanted = (int)std::min<size_t>(content.size() - filled, (size_t)ASSET_SKIP_CHUNK_BYTES);
        int produced = Apk::ReadAsset(fresh, content.data() + filled, wanted);
        if (produced <= 0) break;

        filled += (size_t)produced;
    }

    Apk::CloseAsset(fresh);
    content.resize(filled);

    asset.content = std::move(content);
    asset.loaded = true;
    return true;
}

static AbiBridge::Result AssetOpenHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    registers.general[0] = 0;

    std::string path;
    if (!gAssetPackage || !ReadGuestPath(memory, registers.general[1], path)) return AbiBridge::Result::Ok;

    while (!path.empty() && path.front() == '/') path.erase(path.begin());

    std::unique_ptr<GuestAsset> asset = std::make_unique<GuestAsset>();
    asset->path = path;

    if (Apk::OpenAsset(*gAssetPackage, path, asset->asset) != Apk::Result::Ok) return AbiBridge::Result::Ok;

    std::lock_guard<std::mutex> guard(gAssetLock);
    GuestAsset* opened = asset.release();
    gLiveAssets.insert(opened);

    registers.general[0] = (uint64_t)opened;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result AssetCloseHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    GuestAsset* asset = nullptr;
    {
        std::lock_guard<std::mutex> guard(gAssetLock);

        GuestAsset* candidate = (GuestAsset*)registers.general[0];
        if (gLiveAssets.erase(candidate)) asset = candidate;
    }

    if (asset)
    {
        Apk::CloseAsset(asset->asset);
        delete asset;
    }

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result AssetReadHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    GuestAsset* asset = LiveAsset(registers.general[0]);
    uint64_t destination = registers.general[1];
    uint64_t count = registers.general[2];

    if (!asset || (destination == 0 && count != 0))
    {
        registers.general[0] = (uint64_t)(int64_t)-1;
        return AbiBridge::Result::Ok;
    }

    std::lock_guard<std::mutex> guard(asset->lock);

    int64_t remaining = asset->loaded ? (int64_t)asset->content.size() - asset->position : asset->asset.length - asset->position;
    if (remaining < 0) remaining = 0;
    if ((int64_t)count > remaining) count = (uint64_t)remaining;

    std::vector<uint8_t> buffer((size_t)count);
    int64_t produced = 0;

    if (asset->loaded)
    {
        memcpy(buffer.data(), asset->content.data() + asset->position, (size_t)count);
        produced = (int64_t)count;
    }
    else
    {
        while ((uint64_t)produced < count)
        {
            int wanted = (int)std::min<uint64_t>(count - (uint64_t)produced, (uint64_t)ASSET_SKIP_CHUNK_BYTES);
            int read = Apk::ReadAsset(asset->asset, buffer.data() + produced, wanted);
            if (read <= 0) break;

            produced += read;
        }
    }

    if (produced > 0 && !memory.Write(memory.context, (GuestState::GuestAddress)destination, buffer.data(), (uint64_t)produced))
        return AbiBridge::Result::MalformedArguments;

    asset->position += produced;
    registers.general[0] = (uint64_t)produced;
    return AbiBridge::Result::Ok;
}

static bool SkipForward(GuestAsset& asset, int64_t target)
{
    std::vector<uint8_t> scratch((size_t)ASSET_SKIP_CHUNK_BYTES);

    while (asset.position < target)
    {
        int wanted = (int)std::min<int64_t>(target - asset.position, (int64_t)ASSET_SKIP_CHUNK_BYTES);
        int read = Apk::ReadAsset(asset.asset, scratch.data(), wanted);
        if (read <= 0) return false;

        asset.position += read;
    }

    return true;
}

static AbiBridge::Result AssetSeekHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    GuestAsset* asset = LiveAsset(registers.general[0]);
    int64_t offset = (int64_t)registers.general[1];
    int32_t whence = (int32_t)(uint32_t)registers.general[2];

    if (!asset)
    {
        registers.general[0] = (uint64_t)(int64_t)-1;
        return AbiBridge::Result::Ok;
    }

    std::lock_guard<std::mutex> guard(asset->lock);

    int64_t length = asset->loaded ? (int64_t)asset->content.size() : asset->asset.length;
    int64_t target = -1;

    if (whence == ASSET_SEEK_SET) target = offset;
    if (whence == ASSET_SEEK_CURRENT) target = asset->position + offset;
    if (whence == ASSET_SEEK_END) target = length + offset;

    if (target < 0 || target > length)
    {
        registers.general[0] = (uint64_t)(int64_t)-1;
        return AbiBridge::Result::Ok;
    }

    if (!asset->loaded && target < asset->position && !LoadWholeAsset(*asset))
    {
        registers.general[0] = (uint64_t)(int64_t)-1;
        return AbiBridge::Result::Ok;
    }

    if (!asset->loaded && !SkipForward(*asset, target))
    {
        registers.general[0] = (uint64_t)(int64_t)-1;
        return AbiBridge::Result::Ok;
    }

    asset->position = target;
    registers.general[0] = (uint64_t)target;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result AssetLengthHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    GuestAsset* asset = LiveAsset(registers.general[0]);
    bool remainingOnly = context != nullptr;

    if (!asset)
    {
        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    std::lock_guard<std::mutex> guard(asset->lock);

    int64_t length = asset->loaded ? (int64_t)asset->content.size() : asset->asset.length;
    int64_t reported = remainingOnly ? length - asset->position : length;

    registers.general[0] = (uint64_t)(reported < 0 ? 0 : reported);
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result AssetBufferHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    GuestAsset* asset = LiveAsset(registers.general[0]);
    registers.general[0] = 0;

    if (!asset) return AbiBridge::Result::Ok;

    std::lock_guard<std::mutex> guard(asset->lock);
    if (!LoadWholeAsset(*asset)) return AbiBridge::Result::Ok;

    registers.general[0] = (uint64_t)asset->content.data();
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result AssetAllocatedHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    GuestAsset* asset = LiveAsset(registers.general[0]);
    registers.general[0] = asset && asset->loaded ? 1 : 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result AssetDescriptorHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    GuestAsset* asset = LiveAsset(registers.general[0]);
    uint64_t startOut = registers.general[1];
    uint64_t lengthOut = registers.general[2];

    registers.general[0] = (uint64_t)(int64_t)-1;
    if (!asset || startOut == 0 || lengthOut == 0) return AbiBridge::Result::Ok;

    Apk::Asset fresh;
    if (Apk::OpenAsset(*gAssetPackage, asset->path, fresh) != Apk::Result::Ok) return AbiBridge::Result::Ok;

    int descriptor = -1;
    int64_t start = 0;
    int64_t length = 0;
    Apk::Result described = Apk::AssetFileDescriptor(fresh, descriptor, start, length);
    Apk::CloseAsset(fresh);

    if (described != Apk::Result::Ok || descriptor < 0) return AbiBridge::Result::Ok;

    if (!memory.Write(memory.context, (GuestState::GuestAddress)startOut, &start, sizeof(start)) ||
        !memory.Write(memory.context, (GuestState::GuestAddress)lengthOut, &length, sizeof(length)))
    {
        close(descriptor);
        return AbiBridge::Result::MalformedArguments;
    }

    registers.general[0] = (uint64_t)(int64_t)descriptor;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result DirectoryOpenHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    registers.general[0] = 0;

    std::string path;
    if (!gAssetPackage || !ReadGuestPath(memory, registers.general[1], path)) return AbiBridge::Result::Ok;

    while (!path.empty() && path.front() == '/') path.erase(path.begin());
    while (!path.empty() && path.back() == '/') path.pop_back();

    std::vector<std::string> names;
    Apk::ListAssets(*gAssetPackage, path, names);

    std::unique_ptr<GuestAssetDirectory> directory = std::make_unique<GuestAssetDirectory>();

    for (const std::string& name : names)
    {
        Apk::Asset probe;
        std::string full = path.empty() ? name : path + "/" + name;
        if (Apk::OpenAsset(*gAssetPackage, full, probe) != Apk::Result::Ok) continue;

        Apk::CloseAsset(probe);
        directory->names.push_back(name);
    }

    std::lock_guard<std::mutex> guard(gAssetLock);
    GuestAssetDirectory* opened = directory.release();
    gLiveDirectories.insert(opened);

    registers.general[0] = (uint64_t)opened;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result DirectoryNextHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    GuestAssetDirectory* directory = LiveDirectory(registers.general[0]);
    registers.general[0] = 0;

    if (!directory || directory->next >= directory->names.size()) return AbiBridge::Result::Ok;

    directory->current = directory->names[directory->next++];
    registers.general[0] = (uint64_t)directory->current.c_str();
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result DirectoryRewindHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    GuestAssetDirectory* directory = LiveDirectory(registers.general[0]);
    if (directory) directory->next = 0;

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result DirectoryCloseHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    GuestAssetDirectory* directory = nullptr;
    {
        std::lock_guard<std::mutex> guard(gAssetLock);

        GuestAssetDirectory* candidate = (GuestAssetDirectory*)registers.general[0];
        if (gLiveDirectories.erase(candidate)) directory = candidate;
    }

    delete directory;
    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

NativeActivity::Result NativeActivity::RegisterAssetFunctions(GuestRuntime::Instance& runtime, const Apk::Package& package)
{
    struct HandlerEntry
    {
        const char* name;
        AbiBridge::GuestCallHandler handler;
        void* context;
    };

    const HandlerEntry handlers[] = {
        {"AAssetManager_open", &AssetOpenHandler, nullptr},
        {"AAssetManager_openDir", &DirectoryOpenHandler, nullptr},
        {"AAssetDir_getNextFileName", &DirectoryNextHandler, nullptr},
        {"AAssetDir_rewind", &DirectoryRewindHandler, nullptr},
        {"AAssetDir_close", &DirectoryCloseHandler, nullptr},
        {"AAsset_close", &AssetCloseHandler, nullptr},
        {"AAsset_read", &AssetReadHandler, nullptr},
        {"AAsset_seek", &AssetSeekHandler, nullptr},
        {"AAsset_seek64", &AssetSeekHandler, nullptr},
        {"AAsset_getLength", &AssetLengthHandler, nullptr},
        {"AAsset_getLength64", &AssetLengthHandler, nullptr},
        {"AAsset_getRemainingLength", &AssetLengthHandler, (void*)1},
        {"AAsset_getRemainingLength64", &AssetLengthHandler, (void*)1},
        {"AAsset_getBuffer", &AssetBufferHandler, nullptr},
        {"AAsset_isAllocated", &AssetAllocatedHandler, nullptr},
        {"AAsset_openFileDescriptor", &AssetDescriptorHandler, nullptr},
        {"AAsset_openFileDescriptor64", &AssetDescriptorHandler, nullptr},
    };

    gAssetPackage = &package;
    AbiBridge::Registry& registry = GuestRuntime::BridgeRegistry(runtime);

    for (const HandlerEntry& entry : handlers)
    {
        AbiBridge::Result registered = AbiBridge::RegisterHostHandler(registry, entry.name, entry.handler, entry.context);

        if (registered != AbiBridge::Result::Ok && registered != AbiBridge::Result::AlreadyRegistered)
        {
            fprintf(stderr, "[assetbridge] cannot register %s: %s\n", entry.name, AbiBridge::ResultName(registered));
            return Result::JniRegistrationFailed;
        }
    }

    return Result::Ok;
}
