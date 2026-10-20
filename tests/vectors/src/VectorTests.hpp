#pragma once

#include "VectorHarness.hpp"

namespace VectorTests
{
    void AddCarryOverflow(VectorHarness::State& state);
    void SubtractFlags(VectorHarness::State& state);
    void AddsZeroResult(VectorHarness::State& state);
    void ShiftFamily(VectorHarness::State& state);
    void BitCounting(VectorHarness::State& state);
    void ByteReversal(VectorHarness::State& state);
    void ConditionalSelect(VectorHarness::State& state);
    void MultiplyHigh(VectorHarness::State& state);
    void DivideByZero(VectorHarness::State& state);
    void DivideSignedOverflow(VectorHarness::State& state);
    void ExtendAndTruncate(VectorHarness::State& state);
    void BitfieldExtract(VectorHarness::State& state);
    void LoadAcquireStoreRelease(VectorHarness::State& state);
    void ExclusiveStoreSuccess(VectorHarness::State& state);
    void ExclusiveClearedByClrex(VectorHarness::State& state);
    void CompareAndSwap(VectorHarness::State& state);
    void CompareAndSwapFailure(VectorHarness::State& state);
    void AtomicAdd(VectorHarness::State& state);
    void AtomicSwap(VectorHarness::State& state);
    void CompareAndSwapPair(VectorHarness::State& state);
    void LoadAcquireOrdered(VectorHarness::State& state);
    void VectorAddBytes(VectorHarness::State& state);
    void FloatCompareQuietNan(VectorHarness::State& state);
    void FloatCompareSignallingNan(VectorHarness::State& state);
    void FloatCompareExceptionalQuietNan(VectorHarness::State& state);
    void FloatDivideZeroByZero(VectorHarness::State& state);
    void FloatOverflowAndInexact(VectorHarness::State& state);
    void VectorSaturatingAdd(VectorHarness::State& state);
}
