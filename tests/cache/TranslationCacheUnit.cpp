#include <cstdint>
#include <cstdio>
#include <cstring>
#include <string>
#include <unistd.h>
#include <vector>

#include "TranslationCache/TranslationCache.hpp"

static int failures = 0;

static void Check(bool condition, const char* label)
{
    if (condition)
    {
        printf("PASS %s\n", label);
        return;
    }

    printf("FAIL %s\n", label);
    failures += 1;
}

static bool WriteFile(const std::string& path, const std::string& contents)
{
    FILE* file = fopen(path.c_str(), "wb");
    if (!file) return false;

    size_t written = fwrite(contents.data(), 1, contents.size(), file);
    fclose(file);

    return written == contents.size();
}

int main(int argc, char** argv)
{
    if (argc < 2)
    {
        fprintf(stderr, "usage: TranslationCacheUnitTest <scratch-directory>\n");
        return 2;
    }

    std::string scratch = argv[1];

    TranslationCache::Configuration configuration;
    configuration.root = scratch + "/cache-" + std::to_string((long)getpid());
    configuration.runtimeVersion = "acl-test-1";

    std::string imageA = scratch + "/imageA.so";
    std::string imageB = scratch + "/imageB.so";

    if (!WriteFile(imageA, "the quick brown fox") || !WriteFile(imageB, "the quick brown fox."))
    {
        printf("translation-cache-unit FAILED: cannot write the fixtures\n");
        return 1;
    }

    std::string keyA;
    std::string keyB;
    Check(TranslationCache::KeyForFile(imageA, configuration.runtimeVersion, keyA) == TranslationCache::Result::Ok,
          "a key is derived from an image");
    Check(TranslationCache::KeyForFile(imageB, configuration.runtimeVersion, keyB) == TranslationCache::Result::Ok,
          "a key is derived from a second image");
    Check(keyA != keyB, "a one-byte difference in the image changes the key");

    std::string keyOtherVersion;
    Check(TranslationCache::KeyForFile(imageA, "acl-test-2", keyOtherVersion) == TranslationCache::Result::Ok,
          "a key is derived for a second runtime version");
    Check(keyA != keyOtherVersion, "the runtime version is part of the key, so a rebuilt acl cannot reuse stale objects");

    std::string repeated;
    Check(TranslationCache::KeyForFile(imageA, configuration.runtimeVersion, repeated) == TranslationCache::Result::Ok &&
              repeated == keyA,
          "the key is stable across repeated derivation");

    std::string missing;
    Check(TranslationCache::KeyForFile(scratch + "/absent.so", configuration.runtimeVersion, missing) == TranslationCache::Result::NotFound,
          "a missing image is NotFound rather than a silent empty key");

    TranslationCache::Entry entry;
    Check(TranslationCache::Lookup(configuration, keyA, entry) == TranslationCache::Result::NotFound,
          "an empty cache reports a miss");

    std::string object = "\x7f"
                         "ELF fake object payload";
    Check(TranslationCache::Store(configuration, keyA, object.data(), object.size(), entry) == TranslationCache::Result::Ok,
          "an object is stored");

    TranslationCache::Entry found;
    Check(TranslationCache::Lookup(configuration, keyA, found) == TranslationCache::Result::Ok, "the stored object is found");
    Check(found.objectBytes == object.size(), "the stored object reports its exact size");

    std::vector<uint8_t> loaded;
    Check(TranslationCache::Load(found, loaded) == TranslationCache::Result::Ok, "the stored object loads back");
    Check(loaded.size() == object.size() && memcmp(loaded.data(), object.data(), object.size()) == 0,
          "the loaded object is byte-identical to what was stored");

    TranslationCache::Entry other;
    Check(TranslationCache::Lookup(configuration, keyB, other) == TranslationCache::Result::NotFound,
          "a different image does not hit the first image's entry");

    TranslationCache::Configuration disabled = configuration;
    disabled.enabled = false;
    TranslationCache::Entry ignored;
    Check(TranslationCache::Lookup(disabled, keyA, ignored) == TranslationCache::Result::Disabled,
          "a disabled cache reports Disabled rather than pretending to miss");
    Check(TranslationCache::Store(disabled, keyA, object.data(), object.size(), ignored) == TranslationCache::Result::Disabled,
          "a disabled cache refuses to store");

    TranslationCache::Entry escaped;
    Check(TranslationCache::Store(configuration, "../../escaped", object.data(), object.size(), escaped) ==
              TranslationCache::Result::KeyMismatch,
          "a key containing a path traversal is refused rather than writing outside the cache root");
    Check(TranslationCache::Store(configuration, "", object.data(), object.size(), escaped) == TranslationCache::Result::KeyMismatch,
          "an empty key is refused");
    Check(TranslationCache::Store(configuration, "has/slash", object.data(), object.size(), escaped) ==
              TranslationCache::Result::KeyMismatch,
          "a key containing a separator is refused");

    std::string adopted = configuration.root + "/" + keyB + ".building." + std::to_string((long)getpid());
    WriteFile(adopted, object);

    TranslationCache::Entry moved;
    Check(TranslationCache::Adopt(configuration, keyB, adopted, moved) == TranslationCache::Result::Ok,
          "a finished object is adopted into the cache without being copied through memory");
    Check(access(adopted.c_str(), F_OK) != 0, "adopting moves the object rather than leaving the scratch copy behind");
    Check(moved.objectBytes == object.size(), "the adopted object reports its exact size");

    TranslationCache::Entry rediscovered;
    Check(TranslationCache::Lookup(configuration, keyB, rediscovered) == TranslationCache::Result::Ok, "the adopted object is found");

    std::vector<uint8_t> readBack;
    Check(TranslationCache::Load(rediscovered, readBack) == TranslationCache::Result::Ok && readBack.size() == object.size() &&
              memcmp(readBack.data(), object.data(), object.size()) == 0,
          "the adopted object is byte-identical to what the translator wrote");

    TranslationCache::Entry refused;
    Check(TranslationCache::Adopt(configuration, "../../escaped", adopted, refused) == TranslationCache::Result::KeyMismatch,
          "adopting refuses a key containing a path traversal");
    Check(TranslationCache::Adopt(configuration, keyA, configuration.root + "/absent.building.1", refused) ==
              TranslationCache::Result::Unwritable,
          "adopting a missing object fails rather than registering an empty entry");

    std::string abandoned = configuration.root + "/" + keyA + ".building.4194304.0.o";
    WriteFile(abandoned, object);
    std::string live = configuration.root + "/" + keyA + ".building." + std::to_string((long)getpid()) + ".0.o";
    WriteFile(live, object);

    uint64_t removed = 0;
    Check(TranslationCache::SweepAbandoned(configuration, removed) == TranslationCache::Result::Ok, "the cache can be swept");
    Check(access(abandoned.c_str(), F_OK) != 0, "an object left by a translation that died is removed");
    Check(access(live.c_str(), F_OK) == 0, "an object a running translation is still writing is left alone");
    Check(access(found.path.c_str(), F_OK) == 0, "sweeping does not touch finished cache entries");

    unlink(live.c_str());
    unlink(rediscovered.path.c_str());
    unlink(found.path.c_str());
    rmdir(configuration.root.c_str());

    printf("%s: %d checks failed\n", failures == 0 ? "translation-cache-unit PASSED" : "translation-cache-unit FAILED", failures);
    return failures == 0 ? 0 : 1;
}
