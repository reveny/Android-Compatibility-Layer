#pragma once

#include <cstddef>
#include <cstdint>
#include <string>

namespace HostBridge
{
    void* AllocateBytes(uint64_t size);
    void* AllocateZeroed(uint64_t count, uint64_t size);
    void* ResizeAllocation(void* block, uint64_t size);
    void ReleaseAllocation(void* block);
    void* AllocateAligned(uint64_t alignment, uint64_t size);
    int32_t AllocatePosixAligned(void** out, uint64_t alignment, uint64_t size);

    void* CopyBytes(void* destination, const void* source, uint64_t count);
    void* MoveBytes(void* destination, const void* source, uint64_t count);
    void* FillBytes(void* destination, int32_t value, uint64_t count);
    int32_t CompareBytes(const void* left, const void* right, uint64_t count);
    void* FindByte(const void* block, int32_t value, uint64_t count);
    void* FindLastByte(const void* block, int32_t value, uint64_t count);

    uint64_t StringLength(const char* text);
    uint64_t StringLengthBounded(const char* text, uint64_t limit);
    int32_t StringCompare(const char* left, const char* right);
    int32_t StringCompareBounded(const char* left, const char* right, uint64_t count);
    int32_t StringCompareIgnoringCase(const char* left, const char* right);
    int32_t StringCompareIgnoringCaseBounded(const char* left, const char* right, uint64_t count);
    char* StringCopy(char* destination, const char* source);
    char* StringCopyBounded(char* destination, const char* source, uint64_t count);
    uint64_t StringCopyTerminated(char* destination, const char* source, uint64_t size);
    char* StringCopyReturningEnd(char* destination, const char* source);
    char* StringAppend(char* destination, const char* source);
    char* StringAppendBounded(char* destination, const char* source, uint64_t count);
    char* FindCharacter(const char* text, int32_t value);
    char* FindLastCharacter(const char* text, int32_t value);
    char* FindSubstring(const char* text, const char* pattern);
    char* FindAnyCharacter(const char* text, const char* set);
    uint64_t SpanOfCharacters(const char* text, const char* set);
    uint64_t SpanExcludingCharacters(const char* text, const char* set);
    char* SplitString(char* text, const char* separators, char** state);
    char* DuplicateString(const char* text);
    char* DuplicateStringBounded(const char* text, uint64_t limit);
    char* DescribeErrorNumber(int32_t number);
    int32_t DescribeErrorInto(int32_t number, char* buffer, uint64_t size);
    int32_t StringCollate(const char* left, const char* right);
    uint64_t StringTransform(char* destination, const char* source, uint64_t count);

    uint64_t WideLength(const void* text);
    int32_t WideCompare(const void* left, const void* right);
    int32_t WideCompareBounded(const void* left, const void* right, uint64_t count);
    void* WideCopy(void* destination, const void* source);
    void* WideCopyBounded(void* destination, const void* source, uint64_t count);
    void* WideFindCharacter(const void* text, int32_t value);
    void* WideFindLastCharacter(const void* text, int32_t value);
    void* WideFindSubstring(const void* text, const void* pattern);
    void* WideCopyBytes(void* destination, const void* source, uint64_t count);
    void* WideMoveBytes(void* destination, const void* source, uint64_t count);
    void* WideFill(void* destination, int32_t value, uint64_t count);
    void* WideFindValue(const void* block, int32_t value, uint64_t count);
    int32_t WideCompareValues(const void* left, const void* right, uint64_t count);
    int32_t WideCollate(const void* left, const void* right);
    uint64_t WideTransform(void* destination, const void* source, uint64_t count);

    uint64_t DecodeCharacter(void* wide, const char* text, uint64_t limit, void* state);
    uint64_t EncodeCharacter(char* text, int32_t wide, void* state);
    uint64_t DecodeString(void* wide, const char** text, uint64_t limit, void* state);
    uint64_t EncodeString(char* text, const void** wide, uint64_t limit, void* state);
    uint64_t DecodeStringBounded(void* wide, const char** text, uint64_t available, uint64_t limit, void* state);
    uint64_t EncodeStringBounded(char* text, const void** wide, uint64_t available, uint64_t limit, void* state);
    uint64_t DecodedLength(const char* text, uint64_t limit, void* state);
    int32_t DecodeSingle(void* wide, const char* text, uint64_t limit);
    int32_t EncodeSingle(char* text, int32_t wide);
    uint64_t DecodeWholeString(void* wide, const char* text, uint64_t limit);
    uint64_t EncodeWholeString(char* text, const void* wide, uint64_t limit);
    int32_t IsDecodingComplete(const void* state);
    int32_t ByteToWide(int32_t value);
    int32_t WideToByte(int32_t value);
    uint64_t MaximumCharacterBytes();

    void* CreateLocale(int32_t mask, const char* name, void* base);
    void ReleaseLocale(void* locale);
    void* DuplicateLocale(void* locale);
    void* UseLocale(void* locale);
    void* LocaleConventions();
    char* SelectLocale(int32_t category, const char* name);

    int32_t ClassifyWide(int32_t value, uint64_t classification);
    uint64_t LookupWideClass(const char* name);
    int32_t WideToUpper(int32_t value);
    int32_t WideToLower(int32_t value);
    int32_t ToUpper(int32_t value);
    int32_t ToLower(int32_t value);
    int32_t IsUpper(int32_t value);
    int32_t IsLower(int32_t value);
    int32_t IsAlpha(int32_t value);
    int32_t IsDigit(int32_t value);
    int32_t IsAlphaNumeric(int32_t value);
    int32_t IsPunctuation(int32_t value);
    int32_t IsHexDigit(int32_t value);
    int32_t IsControl(int32_t value);
    int32_t IsPrintable(int32_t value);
    int32_t IsGraphical(int32_t value);
    int32_t IsBlank(int32_t value);
    int32_t IsSpace(int32_t value);

    int32_t IsWideAlphabetic(int32_t value, void* locale);
    int32_t IsWideBlank(int32_t value, void* locale);
    int32_t IsWideControl(int32_t value, void* locale);
    int32_t IsWideDigit(int32_t value, void* locale);
    int32_t IsWideLower(int32_t value, void* locale);
    int32_t IsWidePrintable(int32_t value, void* locale);
    int32_t IsWidePunctuation(int32_t value, void* locale);
    int32_t IsWideSpace(int32_t value, void* locale);
    int32_t IsWideUpper(int32_t value, void* locale);
    int32_t IsWideHexDigit(int32_t value, void* locale);
    int32_t WideToUpperIn(int32_t value, void* locale);
    int32_t WideToLowerIn(int32_t value, void* locale);
    int32_t StringCollateIn(const char* left, const char* right, void* locale);
    uint64_t StringTransformIn(char* destination, const char* source, uint64_t count, void* locale);
    int32_t WideCollateIn(const void* left, const void* right, void* locale);
    uint64_t WideTransformIn(void* destination, const void* source, uint64_t count, void* locale);

    int64_t ParseSigned(const char* text, char** end, int32_t base);
    uint64_t ParseUnsigned(const char* text, char** end, int32_t base);
    double ParseDouble(const char* text, char** end);
    float ParseFloat(const char* text, char** end);
    double ParseExtended(const char* text, char** end);
    int64_t ParseSignedIn(const char* text, char** end, int32_t base, void* locale);
    uint64_t ParseUnsignedIn(const char* text, char** end, int32_t base, void* locale);
    double ParseExtendedIn(const char* text, char** end, void* locale);
    int64_t ParseWideSigned(const void* text, void** end, int32_t base);
    uint64_t ParseWideUnsigned(const void* text, void** end, int32_t base);
    double ParseWideDouble(const void* text, void** end);
    float ParseWideFloat(const void* text, void** end);
    double ParseWideExtended(const void* text, void** end);
    int32_t ParseInteger(const char* text);
    int64_t ParseLong(const char* text);
    double ParseNumber(const char* text);

    double SineOf(double value);
    double CosineOf(double value);
    double TangentOf(double value);
    double ArcSineOf(double value);
    double ArcCosineOf(double value);
    double ArcTangentOf(double value);
    double ArcTangentOfRatio(double y, double x);
    double ExponentialOf(double value);
    double LogarithmOf(double value);
    double BinaryLogarithmOf(double value);
    double DecimalLogarithmOf(double value);
    double PowerOf(double base, double exponent);
    double SquareRootOf(double value);
    double CubeRootOf(double value);
    double RoundUp(double value);
    double RoundDown(double value);
    double RoundNearest(double value);
    double RoundTowardZero(double value);
    double RoundToInteger(double value);
    double Remainder(double left, double right);
    double Magnitude(double x, double y);
    double HyperbolicSine(double value);
    double HyperbolicCosine(double value);
    double HyperbolicTangent(double value);
    double WithSignOf(double value, double sign);
    double ScaleByPowerOfTwo(double value, int32_t exponent);
    double SplitExponent(double value, int32_t* exponent);
    double SplitFraction(double value, double* whole);
    double UnbiasedExponent(double value);
    void SineAndCosine(double value, double* sine, double* cosine);

    float SineOfNarrow(float value);
    float CosineOfNarrow(float value);
    float TangentOfNarrow(float value);
    float ArcSineOfNarrow(float value);
    float ArcCosineOfNarrow(float value);
    float ArcTangentOfNarrow(float value);
    float ArcTangentOfRatioNarrow(float y, float x);
    float ExponentialOfNarrow(float value);
    float BinaryExponentialNarrow(float value);
    float LogarithmOfNarrow(float value);
    float BinaryLogarithmNarrow(float value);
    float DecimalLogarithmNarrow(float value);
    float PowerOfNarrow(float base, float exponent);
    float SquareRootOfNarrow(float value);
    float CubeRootOfNarrow(float value);
    float RemainderNarrow(float left, float right);
    float MagnitudeNarrow(float x, float y);
    float HyperbolicTangentNarrow(float value);
    float ScaleByPowerOfTwoNarrow(float value, int32_t exponent);
    float SplitFractionNarrow(float value, float* whole);
    float RoundToEvenNarrow(float value);
    void SineAndCosineNarrow(float value, float* sine, float* cosine);

    int32_t AbsoluteInteger(int32_t value);
    int64_t AbsoluteLong(int64_t value);
    void SortArray(void* base, uint64_t count, uint64_t width, void* comparator);
    void* SearchArray(const void* key, const void* base, uint64_t count, uint64_t width, void* comparator);
    void SeedRandom(int64_t seed);
    int64_t NextRandom();

    int32_t OpenFile(const char* path, int32_t flags, int32_t mode);
    int32_t CloseDescriptor(int32_t descriptor);
    int64_t ReadDescriptor(int32_t descriptor, void* buffer, uint64_t count);
    int64_t WriteDescriptor(int32_t descriptor, const void* buffer, uint64_t count);
    int64_t SeekDescriptor(int32_t descriptor, int64_t offset, int32_t origin);
    int32_t CheckAccess(const char* path, int32_t mode);
    int32_t DuplicateDescriptor(int32_t descriptor);
    int32_t CreatePipe(int32_t* descriptors);
    int32_t DescribePath(const char* path, void* out);
    int32_t DescribeLink(const char* path, void* out);
    int32_t DescribeDescriptor(int32_t descriptor, void* out);
    int64_t ReadLinkTarget(const char* path, char* buffer, uint64_t size);
    char* ResolvePath(const char* path, char* resolved);
    char* WorkingDirectory(char* buffer, uint64_t size);
    int32_t RemovePath(const char* path);
    int32_t RemoveEntry(const char* path);
    int32_t RenamePath(const char* from, const char* to);
    int32_t CreateDirectory(const char* path, int32_t mode);
    int32_t RemoveDirectory(const char* path);
    void* OpenDirectory(const char* path);
    void* ReadDirectory(void* directory);
    int32_t CloseDirectory(void* directory);
    int32_t TruncatePath(const char* path, int64_t length);
    int32_t TruncateDescriptor(int32_t descriptor, int64_t length);
    int32_t ChangeMode(const char* path, int32_t mode);
    int32_t ChangeDescriptorMode(int32_t descriptor, int32_t mode);
    int32_t CreateSymbolicLink(const char* target, const char* path);
    int32_t CreateHardLink(const char* target, const char* path);
    int32_t IsTerminal(int32_t descriptor);
    char* BaseName(char* path);
    int32_t MatchPattern(const char* pattern, const char* text, int32_t flags);
    int32_t DescribeFileSystem(const char* path, void* out);
    int32_t SetTimestamps(const char* path, const void* times);
    int32_t LockDescriptor(int32_t descriptor, int32_t operation);
    int32_t PollDescriptors(void* descriptors, uint64_t count, int32_t timeout);

    void SetGuestMemoryRange(uint64_t base, uint64_t length);

    void MapAndroidTree(const char* dataDirectory, const char* externalDirectory, const char* systemDirectory,
                        const char* packageName);

    const std::string& ProcessorDescription();
    const std::string& MemoryDescription();

    void* MapMemory(void* address, uint64_t length, int32_t protection, int32_t flags, int32_t descriptor, int64_t offset);
    int32_t UnmapMemory(void* address, uint64_t length);
    int32_t ProtectMemory(void* address, uint64_t length, int32_t protection);
    int32_t AdviseMemory(void* address, uint64_t length, int32_t advice);
    int32_t ProcessControl(int32_t option, uint64_t first, uint64_t second, uint64_t third);

    void* OpenStream(const char* path, const char* mode);
    void* AdoptDescriptor(int32_t descriptor, const char* mode);
    int32_t CloseStream(void* stream);
    uint64_t ReadStream(void* buffer, uint64_t size, uint64_t count, void* stream);
    uint64_t WriteStream(const void* buffer, uint64_t size, uint64_t count, void* stream);
    int32_t SeekStream(void* stream, int64_t offset, int32_t origin);
    int64_t StreamPosition(void* stream);
    char* ReadStreamLine(char* buffer, int32_t size, void* stream);
    int32_t ReadStreamCharacter(void* stream);
    int32_t WriteStreamCharacter(int32_t value, void* stream);
    int32_t WriteStreamText(const char* text, void* stream);
    int32_t StreamAtEnd(void* stream);
    int32_t StreamHasError(void* stream);
    void ClearStreamError(void* stream);
    int32_t StreamDescriptor(void* stream);
    void SetStreamBuffer(void* stream, char* buffer);
    int32_t FlushStream(void* stream);

    int64_t CurrentTime(int64_t* out);
    int64_t ProcessorTime();
    double TimeDifference(int64_t later, int64_t earlier);
    int64_t ComposeTime(void* parts);
    void* SplitUniversalTime(const int64_t* value);
    void* SplitUniversalTimeInto(const int64_t* value, void* parts);
    void* SplitLocalTime(const int64_t* value);
    void* SplitLocalTimeInto(const int64_t* value, void* parts);
    uint64_t FormatTime(char* buffer, uint64_t size, const char* format, const void* parts);
    int32_t ReadClock(int32_t clock, void* out);
    int32_t ReadClockResolution(int32_t clock, void* out);
    int32_t ReadWallClock(void* out, void* zone);
    int32_t SleepPrecisely(const void* span, void* remaining);
    int32_t SleepMicroseconds(uint64_t span);
    int32_t YieldProcessor();
    int32_t SetPathTimes(const char* path, const void* times);

    int32_t ThreadId();
    int32_t ProcessId();
    int32_t UserId();
    int32_t EffectiveUserId();
    int32_t EffectiveGroupId();
    uint64_t CurrentThread();
    int32_t ThreadsEqual(uint64_t left, uint64_t right);
    int32_t ReadPriority(int32_t which, int32_t who);
    int32_t WritePriority(int32_t which, int32_t who, int32_t priority);
    int32_t PageSize();
    int64_t SystemConfiguration(int32_t name);
    int64_t ProcessorCount();

    int32_t CreateSemaphore(void* semaphore, int32_t shared, uint32_t value);
    int32_t DestroySemaphore(void* semaphore);
    int32_t PostSemaphore(void* semaphore);
    int32_t WaitSemaphore(void* semaphore);
    int32_t WaitSemaphoreUntil(void* semaphore, const void* deadline);
    int32_t ReadSemaphore(void* semaphore, int32_t* value);

    int32_t ClearSignalSet(void* set);
    int32_t FillSignalSet(void* set);
    int32_t AddSignal(void* set, int32_t number);
    int32_t RemoveSignal(void* set, int32_t number);

    int32_t SetEnvironmentValue(const char* name, const char* value, int32_t overwrite);
    int32_t ClearEnvironmentValue(const char* name);

    int32_t ControlDescriptor(int32_t descriptor, int32_t command, int64_t argument);
    int32_t WriteLine(const char* text);
    void* ErrorLocation();

    int32_t OpenFileAt(int32_t directory, const char* path, int32_t flags, int32_t mode);
    int32_t RemovePathAt(int32_t directory, const char* path, int32_t flags);
    int32_t ChangeModeAt(int32_t directory, const char* path, int32_t mode, int32_t flags);
    int32_t SetTimesAt(int32_t directory, const char* path, const void* times, int32_t flags);
    int32_t SetDescriptorTimes(int32_t descriptor, const void* times);
    void* OpenDirectoryDescriptor(int32_t descriptor);
    int32_t ChangeWorkingDirectory(const char* path);
    int32_t DescribeVolume(const char* path, void* out);
    int64_t PathLimit(const char* path, int32_t name);
    int64_t TransferFile(int32_t destination, int32_t source, void* offset, uint64_t count);
    int32_t WaitForDescriptors(int32_t limit, void* readable, void* writable, void* failing, void* timeout);
    int32_t RaiseSignal(int32_t number);
    int32_t SuspendForSignals(const void* mask);
    uint64_t DivideIntegers(int32_t numerator, int32_t denominator);

    char* DescribeErrorReturning(int32_t number, char* buffer, uint64_t size);
    uint64_t FormatTimeIn(char* buffer, uint64_t size, const char* format, const void* parts, void* locale);
    void* PasswordEntry(uint32_t user);
    int32_t PasswordEntryInto(uint32_t user, void* entry, char* buffer, uint64_t size, void** result);

    int32_t StreamPushBack(int32_t value, void* stream);
    int32_t WriteWideCharacter(int32_t value, void* stream);
    int32_t ReadWideCharacter(void* stream);
    int32_t WidePushBack(int32_t value, void* stream);

    void MarkDescriptorSet(int32_t descriptor, void* set, uint64_t size);
    int32_t DescriptorIsSet(int32_t descriptor, const void* set, uint64_t size);

    uint32_t AddressFromText(const char* text);
    const char* AddressToText(int32_t family, const void* address, char* buffer, uint32_t size);
    int32_t AddressFromTextInto(int32_t family, const char* text, void* address);
    uint32_t InterfaceIndex(const char* name);

    int32_t DecompressInitialize(void* stream, int32_t windowBits, const char* version, int32_t size);
    int32_t Decompress(void* stream, int32_t flush);
    int32_t DecompressEnd(void* stream);
}
