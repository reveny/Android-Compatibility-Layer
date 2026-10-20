#include "PackageRegistry/PackageRegistry.hpp"

#include <cerrno>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <dirent.h>
#include <sys/stat.h>
#include <unistd.h>

#include "Apk/Apk.hpp"

static std::string Trim(const std::string& text)
{
    size_t first = 0;
    size_t last = text.size();

    while (first < last && (text[first] == ' ' || text[first] == '\t' || text[first] == '\r' || text[first] == '\n')) first += 1;
    while (last > first && (text[last - 1] == ' ' || text[last - 1] == '\t' || text[last - 1] == '\r' || text[last - 1] == '\n')) last -= 1;

    return text.substr(first, last - first);
}

static int IntegerFrom(const std::string& text, int fallback)
{
    if (text.empty()) return fallback;

    char* end = nullptr;
    long parsed = strtol(text.c_str(), &end, 10);

    if (end == text.c_str()) return fallback;
    return (int)parsed;
}

std::string PackageRegistry::IndexPath(const std::string& dataRoot)
{
    if (dataRoot.empty()) return std::string();

    return dataRoot + "/" + INDEX_LEAF;
}

std::string PackageRegistry::IconDirectory(const std::string& dataRoot)
{
    if (dataRoot.empty()) return std::string();

    return dataRoot + "/" + ICON_DIRECTORY_LEAF;
}

PackageRegistry::Result PackageRegistry::Load(const std::string& indexPath, std::vector<Entry>& entries)
{
    entries.clear();

    if (indexPath.empty()) return Result::NotFound;

    FILE* file = fopen(indexPath.c_str(), "rb");
    if (!file) return Result::NotFound;

    char line[MAX_LINE_BYTES];
    Entry current;
    bool open = false;

    while (fgets(line, sizeof(line), file))
    {
        std::string text = Trim(line);

        const size_t comment = text.find('#');
        if (comment != std::string::npos) text = Trim(text.substr(0, comment));
        if (text.empty()) continue;

        if (text.front() == '[' && text.back() == ']')
        {
            if (open && !current.packageName.empty() && entries.size() < MAX_PACKAGES) entries.push_back(current);

            current = Entry{};
            open = false;

            const std::string section = text.substr(1, text.size() - 2);
            const size_t prefix = strlen(SECTION_PREFIX);

            if (section.size() <= prefix || section.compare(0, prefix, SECTION_PREFIX) != 0) continue;

            current.packageName = section.substr(prefix);
            open = !current.packageName.empty();
            continue;
        }

        if (!open) continue;

        const size_t equals = text.find('=');
        if (equals == std::string::npos) continue;

        const std::string key = Trim(text.substr(0, equals));
        const std::string value = Trim(text.substr(equals + 1));

        if (key == "label") current.label = value;
        else if (key == "apk") current.apkPath = value;
        else if (key == "icon") current.iconPath = value;
        else if (key == "data") current.dataPath = value;
        else if (key == "min_sdk") current.minSdkVersion = IntegerFrom(value, 0);
        else if (key == "target_sdk") current.targetSdkVersion = IntegerFrom(value, 0);
        else if (key == "activities") current.activityCount = IntegerFrom(value, 0);
    }

    if (open && !current.packageName.empty() && entries.size() < MAX_PACKAGES) entries.push_back(current);

    fclose(file);

    return Result::Ok;
}

bool PackageRegistry::Find(const std::vector<Entry>& entries, const std::string& packageName, Entry& found)
{
    if (packageName.empty()) return false;

    for (const Entry& entry : entries)
    {
        if (entry.packageName != packageName) continue;

        found = entry;
        return true;
    }

    return false;
}

std::string PackageRegistry::InstalledApkPath(const std::string& dataRoot, const std::string& packageName)
{
    if (dataRoot.empty() || packageName.empty()) return std::string();

    return dataRoot + "/" + packageName + "/base.apk";
}

static bool EnsureDirectory(const std::string& path)
{
    if (path.empty()) return false;
    if (mkdir(path.c_str(), 0755) == 0) return true;

    if (errno != ENOENT) return errno == EEXIST;

    const size_t slash = path.find_last_of('/');
    if (slash == std::string::npos || slash == 0) return false;

    if (!EnsureDirectory(path.substr(0, slash))) return false;

    return mkdir(path.c_str(), 0755) == 0 || errno == EEXIST;
}

static bool CopyFile(const std::string& from, const std::string& to)
{
    FILE* source = fopen(from.c_str(), "rb");
    if (!source) return false;

    FILE* destination = fopen(to.c_str(), "wb");
    if (!destination)
    {
        fclose(source);
        return false;
    }

    char buffer[65536];
    bool ok = true;

    for (;;)
    {
        const size_t read = fread(buffer, 1, sizeof(buffer), source);
        if (read == 0) break;

        if (fwrite(buffer, 1, read, destination) != read)
        {
            ok = false;
            break;
        }
    }

    if (ferror(source)) ok = false;

    fclose(source);
    if (fclose(destination) != 0) ok = false;

    return ok;
}

static bool ExtractIcon(const Apk::Package& package, const std::string& destination)
{
    const std::string entryName = Apk::ResolveRasterIconEntry(package);
    if (entryName.empty()) return false;

    Apk::Asset asset;
    if (Apk::OpenNonAsset(package, entryName, asset) != Apk::Result::Ok) return false;

    FILE* out = fopen(destination.c_str(), "wb");
    if (!out)
    {
        Apk::CloseAsset(asset);
        return false;
    }

    char buffer[65536];
    bool ok = true;

    for (;;)
    {
        const int read = Apk::ReadAsset(asset, buffer, (int)sizeof(buffer));
        if (read <= 0) break;

        if (fwrite(buffer, 1, (size_t)read, out) != (size_t)read)
        {
            ok = false;
            break;
        }
    }

    fclose(out);
    Apk::CloseAsset(asset);

    if (!ok) unlink(destination.c_str());

    return ok;
}

static bool RemoveTree(const std::string& path)
{
    DIR* opened = opendir(path.c_str());

    if (!opened) return unlink(path.c_str()) == 0 || errno == ENOENT;

    for (struct dirent* entry = readdir(opened); entry; entry = readdir(opened))
    {
        const std::string name = entry->d_name;
        if (name == "." || name == "..") continue;

        RemoveTree(path + "/" + name);
    }

    closedir(opened);

    return rmdir(path.c_str()) == 0 || errno == ENOENT;
}

PackageRegistry::Result PackageRegistry::Save(const std::string& indexPath, const std::vector<Entry>& entries)
{
    if (indexPath.empty()) return Result::Unwritable;

    const size_t slash = indexPath.find_last_of('/');
    if (slash != std::string::npos && slash != 0 && !EnsureDirectory(indexPath.substr(0, slash))) return Result::Unwritable;

    const std::string temporary = indexPath + ".new";

    FILE* file = fopen(temporary.c_str(), "wb");
    if (!file) return Result::Unwritable;

    fprintf(file, "# written by acl --install; one section per installed package\n");

    for (const Entry& entry : entries)
    {
        fprintf(file, "\n[%s%s]\n", SECTION_PREFIX, entry.packageName.c_str());
        fprintf(file, "label = %s\n", entry.label.c_str());
        fprintf(file, "apk = %s\n", entry.apkPath.c_str());
        fprintf(file, "icon = %s\n", entry.iconPath.c_str());
        fprintf(file, "data = %s\n", entry.dataPath.c_str());
        fprintf(file, "min_sdk = %d\n", entry.minSdkVersion);
        fprintf(file, "target_sdk = %d\n", entry.targetSdkVersion);
        fprintf(file, "activities = %d\n", entry.activityCount);
    }

    const bool flushed = fflush(file) == 0;
    const bool closed = fclose(file) == 0;

    if (!flushed || !closed)
    {
        unlink(temporary.c_str());
        return Result::Unwritable;
    }

    if (rename(temporary.c_str(), indexPath.c_str()) != 0)
    {
        unlink(temporary.c_str());
        return Result::Unwritable;
    }

    return Result::Ok;
}

PackageRegistry::Result PackageRegistry::Install(const std::string& apkPath, const std::string& dataRoot,
                                                 const std::string& frameworkResources, std::vector<Entry>& entries,
                                                 Entry& installed)
{
    Apk::Package package;

    if (Apk::Open(apkPath, frameworkResources, package) != Apk::Result::Ok) return Result::NotAnArchive;

    const Apk::Manifest& manifest = package.manifest;

    if (manifest.packageName.empty() || !Apk::IsValidPackageName(manifest.packageName))
    {
        Apk::Close(package);
        return Result::NoPackageName;
    }

    Entry entry;
    const std::string label = Apk::ResolveLabel(package);

    entry.packageName = manifest.packageName;
    entry.label = label.empty() ? manifest.packageName : label;
    entry.minSdkVersion = manifest.minSdkVersion;
    entry.targetSdkVersion = manifest.targetSdkVersion;
    entry.activityCount = (int)manifest.activities.size();

    Apk::Close(package);

    const std::string directory = dataRoot + "/" + entry.packageName;
    if (!EnsureDirectory(directory)) return Result::CopyFailed;

    const std::string destination = InstalledApkPath(dataRoot, entry.packageName);

    if (destination != apkPath && !CopyFile(apkPath, destination)) return Result::CopyFailed;

    entry.apkPath = destination;
    entry.dataPath = directory;

    Apk::Package copied;

    if (Apk::Open(destination, frameworkResources, copied) == Apk::Result::Ok)
    {
        const std::string iconDirectory = IconDirectory(dataRoot);

        if (EnsureDirectory(iconDirectory))
        {
            const std::string iconPath = iconDirectory + "/" + entry.packageName;
            if (ExtractIcon(copied, iconPath)) entry.iconPath = iconPath;
        }

        Apk::Close(copied);
    }

    for (Entry& existing : entries)
    {
        if (existing.packageName != entry.packageName) continue;

        existing = entry;
        installed = entry;

        return Save(IndexPath(dataRoot), entries);
    }

    if (entries.size() >= MAX_PACKAGES) return Result::TooMany;

    entries.push_back(entry);
    installed = entry;

    return Save(IndexPath(dataRoot), entries);
}

PackageRegistry::Result PackageRegistry::Uninstall(const std::string& packageName, const std::string& dataRoot,
                                                   std::vector<Entry>& entries, bool keepData)
{
    if (!Apk::IsValidPackageName(packageName)) return Result::NoPackageName;

    size_t found = entries.size();

    for (size_t index = 0; index < entries.size(); index++)
    {
        if (entries[index].packageName != packageName) continue;

        found = index;
        break;
    }

    if (found == entries.size()) return Result::NotFound;

    if (keepData)
    {
        if (unlink(InstalledApkPath(dataRoot, packageName).c_str()) != 0 && errno != ENOENT) return Result::Unwritable;
    }
    else if (!RemoveTree(dataRoot + "/" + packageName))
    {
        return Result::Unwritable;
    }

    unlink((IconDirectory(dataRoot) + "/" + packageName).c_str());

    entries.erase(entries.begin() + (long)found);

    return Save(IndexPath(dataRoot), entries);
}

const char* PackageRegistry::ResultName(Result result)
{
    switch (result)
    {
        case Result::Ok:
            return "Ok";
        case Result::NotFound:
            return "NotFound";
        case Result::Unreadable:
            return "Unreadable";
        case Result::Malformed:
            return "Malformed";
        case Result::NotAnArchive:
            return "NotAnArchive";
        case Result::NoPackageName:
            return "NoPackageName";
        case Result::CopyFailed:
            return "CopyFailed";
        case Result::Unwritable:
            return "Unwritable";
        case Result::TooMany:
            return "TooMany";
    }

    return "Unknown";
}
