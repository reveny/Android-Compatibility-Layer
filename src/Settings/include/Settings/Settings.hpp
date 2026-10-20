#pragma once

#include <cstddef>
#include <string>

namespace Settings
{
    const char* Lookup(const char* name);
    bool Has(const char* name);

    void Set(const std::string& name, const std::string& value);
    void Unset(const char* name);
    void Clear();

    size_t Count();
    bool At(size_t index, std::string& name, std::string& value);

    bool Accept(const std::string& assignment);

    bool Tracing(const char* area);

    std::string HomeDirectory();
    std::string RuntimeDirectory();
}
