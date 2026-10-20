#include "Settings/Settings.hpp"

#include <cstring>
#include <pwd.h>
#include <unistd.h>
#include <vector>

struct Entry
{
    std::string name;
    std::string value;
};

static std::vector<Entry>& Store()
{
    static std::vector<Entry> store;
    return store;
}

static Entry* Find(const char* name)
{
    if (!name || !*name) return nullptr;

    for (Entry& entry : Store())
    {
        if (entry.name == name) return &entry;
    }

    return nullptr;
}

const char* Settings::Lookup(const char* name)
{
    const Entry* entry = Find(name);
    if (!entry) return nullptr;

    return entry->value.c_str();
}

bool Settings::Has(const char* name)
{
    return Find(name) != nullptr;
}

void Settings::Set(const std::string& name, const std::string& value)
{
    if (name.empty()) return;

    Entry* entry = Find(name.c_str());

    if (entry)
    {
        entry->value = value;
        return;
    }

    Store().push_back(Entry{name, value});
}

void Settings::Unset(const char* name)
{
    if (!name || !*name) return;

    std::vector<Entry>& store = Store();

    for (size_t index = 0; index < store.size(); ++index)
    {
        if (store[index].name != name) continue;

        store.erase(store.begin() + (long)index);
        return;
    }
}

void Settings::Clear()
{
    Store().clear();
}

size_t Settings::Count()
{
    return Store().size();
}

bool Settings::At(size_t index, std::string& name, std::string& value)
{
    if (index >= Store().size()) return false;

    name = Store()[index].name;
    value = Store()[index].value;

    return true;
}

bool Settings::Accept(const std::string& assignment)
{
    size_t separator = assignment.find('=');

    if (separator == std::string::npos)
    {
        Set(assignment, "1");
        return !assignment.empty();
    }

    if (separator == 0) return false;

    Set(assignment.substr(0, separator), assignment.substr(separator + 1));

    return true;
}

bool Settings::Tracing(const char* area)
{
    if (!area || !*area) return false;

    const char* areas = Lookup("trace");
    if (!areas || !*areas) return false;

    const size_t length = strlen(area);
    const char* cursor = areas;

    while (*cursor)
    {
        while (*cursor == ',' || *cursor == ' ') cursor += 1;

        const char* start = cursor;
        while (*cursor && *cursor != ',' && *cursor != ' ') cursor += 1;

        const size_t span = (size_t)(cursor - start);

        if (span == 3 && strncmp(start, "all", 3) == 0) return true;
        if (span == length && strncmp(start, area, length) == 0) return true;
    }

    return false;
}

std::string Settings::HomeDirectory()
{
    const struct passwd* account = getpwuid(getuid());
    if (!account || !account->pw_dir || !*account->pw_dir) return std::string();

    return std::string(account->pw_dir);
}

std::string Settings::RuntimeDirectory()
{
    return "/run/user/" + std::to_string((unsigned)getuid());
}
