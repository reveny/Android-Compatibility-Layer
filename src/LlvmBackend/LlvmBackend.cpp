#include <chrono>
#include <thread>
#include "LlvmBackend/LlvmBackend.hpp"

#include "Interpreter/IrSemantics.hpp"

#include <algorithm>
#include <cmath>
#include <cstddef>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <malloc.h>
#include <memory>
#include <mutex>
#include <string>
#include <unordered_map>
#include <unordered_set>
#include <vector>

#include <llvm/ExecutionEngine/Orc/LLJIT.h>
#include <llvm/ExecutionEngine/Orc/ThreadSafeModule.h>
#include <llvm/IR/BasicBlock.h>
#include <llvm/IR/Function.h>
#include <llvm/IR/IRBuilder.h>
#include <llvm/IR/IntrinsicsX86.h>
#include <llvm/IR/LLVMContext.h>
#include <llvm/IR/Module.h>
#include <llvm/IR/LegacyPassManager.h>
#include <llvm/IR/Verifier.h>
#include <llvm/MC/TargetRegistry.h>
#include <llvm/Passes/PassBuilder.h>
#include <llvm/Support/FileSystem.h>
#include <llvm/Target/TargetMachine.h>
#include <llvm/Target/TargetOptions.h>
#include <llvm/TargetParser/Host.h>
#include <llvm/Support/Error.h>
#include <llvm/Support/TargetSelect.h>

#include "Settings/Settings.hpp"

const std::string& LlvmBackend::HostCpuName()
{
    static const std::string name = llvm::sys::getHostCPUName().str();
    return name;
}

static const std::string& HostCpuFeatures()
{
    static const std::string features = []() {
        llvm::SubtargetFeatures collected;

        for (const auto& feature : llvm::sys::getHostCPUFeatures()) collected.AddFeature(feature.first(), feature.second);

        return collected.getString();
    }();

    return features;
}

extern "C" uint32_t AclComputeAddFlags(uint64_t left, uint64_t right, uint64_t carryIn, int32_t bits)
{
    return IrSemantics::ComputeAddFlags(left, right, carryIn, bits);
}

extern "C" uint64_t AclReverseBytesInLanes(uint64_t value, uint8_t laneWidth, int32_t bits)
{
    return IrSemantics::ReverseBytesInLanes(value, (Ir::LaneWidth)laneWidth, bits);
}

extern "C" uint64_t AclReverseBitsIn(uint64_t value, int32_t bits)
{
    return IrSemantics::ReverseBitsIn(value, bits);
}

extern "C" uint64_t aclForeignAccessTally = 0;

uint64_t LlvmBackend::ForeignAccessCount()
{
    return aclForeignAccessTally;
}

extern "C" void AclVectorOperation(uint64_t descriptor, uint64_t immediate, const uint64_t* left, const uint64_t* right,
                                   const uint64_t* third, uint64_t* result, uint32_t* floatingPointStatus)
{
    IrSemantics::VectorRequest request;
    request.opcode = (Ir::Opcode)(uint16_t)(descriptor & 0xffffull);
    request.laneWidth = (Ir::LaneWidth)(uint8_t)((descriptor >> 16) & 0xffull);
    request.laneCount = (int)(uint8_t)((descriptor >> 24) & 0xffull);
    request.laneIndex = (int)(uint8_t)((descriptor >> 32) & 0xffull);
    request.immediate = immediate;

    GuestState::VectorRegister leftValue;
    leftValue.low = left[0];
    leftValue.high = left[1];

    GuestState::VectorRegister rightValue;
    rightValue.low = right[0];
    rightValue.high = right[1];

    GuestState::VectorRegister thirdValue;
    thirdValue.low = third[0];
    thirdValue.high = third[1];

    GuestState::VectorRegister produced;
    bool wide = false;

    uint32_t pending = IrSemantics::CaptureHostFloatExceptions();
    IrSemantics::ClearHostFloatExceptions();

    bool applied = IrSemantics::ApplyVectorOperation(request, leftValue, rightValue, thirdValue, produced, wide, *floatingPointStatus);

    IrSemantics::ClearHostFloatExceptions();
    *floatingPointStatus |= pending;

    if (!applied)
    {
        result[0] = 0;
        result[1] = 0;
        return;
    }

    result[0] = produced.low;
    result[1] = wide ? produced.high : 0;
}

extern "C" uint64_t AclFloatOperation(uint64_t descriptor, uint64_t immediate, uint64_t left, uint64_t right, uint64_t third,
                                      uint32_t* floatingPointStatus)
{
    IrSemantics::FloatRequest request;
    request.opcode = (Ir::Opcode)(uint16_t)(descriptor & 0xffffull);
    request.operandType = (Ir::ValueType)(uint8_t)((descriptor >> 16) & 0xffull);
    request.resultType = (Ir::ValueType)(uint8_t)((descriptor >> 24) & 0xffull);
    request.sourceType = (Ir::ValueType)(uint8_t)((descriptor >> 32) & 0xffull);
    request.immediate = immediate;

    uint32_t pending = IrSemantics::CaptureHostFloatExceptions();
    IrSemantics::ClearHostFloatExceptions();

    uint64_t produced = 0;
    bool applied = IrSemantics::ApplyFloatOperation(request, left, right, third, produced, *floatingPointStatus);

    IrSemantics::ClearHostFloatExceptions();
    *floatingPointStatus |= pending;

    return applied ? produced : 0;
}

extern "C" uint32_t AclAtomicOperation(uint64_t descriptor, LlvmBackend::RuntimeContext* runtime, GuestState::Registers* registers,
                                       uint64_t address, uint64_t operand, uint64_t third, uint64_t* produced)
{
    IrSemantics::AtomicRequest request;
    request.opcode = (Ir::Opcode)(uint16_t)(descriptor & 0xffffull);
    request.operandType = (Ir::ValueType)(uint8_t)((descriptor >> 16) & 0xffull);
    request.atomicOperation = (Ir::AtomicOperation)(uint8_t)((descriptor >> 24) & 0xffull);

    return (uint32_t)IrSemantics::ApplyAtomicOperation(request, runtime->memory, registers->exclusive, address, operand, third, *produced);
}

extern "C" bool AclConditionHolds(uint32_t flagsNzcv, uint8_t condition)
{
    return GuestState::ConditionHolds(flagsNzcv, condition);
}

extern "C" uint64_t AclReadVirtualCounter()
{
    return Ir::ReadVirtualCounter();
}

extern "C" bool AclMemoryRead(GuestState::MemoryAccessor* memory, uint64_t address, void* destination, uint64_t length)
{
    if (!memory->Read) return false;
    return memory->Read(memory->context, (GuestState::GuestAddress)address, destination, length);
}

extern "C" bool AclMemoryWrite(GuestState::MemoryAccessor* memory, uint64_t address, const void* source, uint64_t length)
{
    if (!memory->Write) return false;
    return memory->Write(memory->context, (GuestState::GuestAddress)address, source, length);
}

struct LlvmBackendSession
{
    LlvmBackend::Configuration configuration;
    LlvmBackend::Statistics statistics;
    std::unique_ptr<llvm::orc::LLJIT> jit;
    std::mutex mutex;
    uint64_t nextModule = 0;
};

struct LoweredValue
{
    llvm::Value* low = nullptr;
    llvm::Value* high = nullptr;
};

struct RegisterSlot
{
    uint64_t offset = 0;
    uint32_t byteWidth = 8;
    llvm::Value* storage = nullptr;
};

struct Lowering
{
    Lowering(llvm::LLVMContext& usedContext, llvm::Module& usedModule, llvm::IRBuilder<>& usedBuilder)
        : context(usedContext), module(usedModule), builder(usedBuilder)
    {
    }

    llvm::LLVMContext& context;
    llvm::Module& module;
    llvm::IRBuilder<>& builder;
    llvm::Function* function = nullptr;
    llvm::Value* registers = nullptr;
    llvm::Value* runtime = nullptr;
    llvm::Value* status = nullptr;
    llvm::Value* scratch = nullptr;
    llvm::Value* vectorScratch = nullptr;
    llvm::Value* floatModeScratch = nullptr;
    bool usesFloat = false;
    bool raisedNativeFloat = false;
    std::vector<LoweredValue> values;
    uint64_t instructionsLowered = 0;
    uint64_t fallthroughTarget = 0;
    llvm::BasicBlock* entryBlock = nullptr;
    llvm::Value* stepsSlot = nullptr;
    std::vector<RegisterSlot> slots;
    std::unordered_map<uint64_t, size_t> slotByOffset;
    std::unordered_map<uint64_t, llvm::BasicBlock*> blockByAddress;
    const std::unordered_set<uint64_t>* moduleFunctions = nullptr;
    uint64_t directCalls = 0;
    bool directMemoryAccess = false;
    bool traceBlocks = false;
    bool reportForeignAccess = false;
    uint64_t arenaBase = 0;
    uint64_t arenaLength = 0;
};

static LlvmBackendSession* Implementation(const LlvmBackend::Session& session)
{
    return (LlvmBackendSession*)session.implementation;
}

static bool LowerFunction(Lowering& lowering, const Ir::Function& function);

static llvm::FunctionType* TranslatedFunctionType(llvm::LLVMContext& context)
{
    llvm::Type* voidType = llvm::Type::getVoidTy(context);
    llvm::Type* pointerType = llvm::PointerType::getUnqual(context);

    return llvm::FunctionType::get(voidType, {pointerType, pointerType, pointerType, llvm::Type::getInt32Ty(context)}, false);
}

static llvm::Function* BuildFunctionInto(llvm::LLVMContext& context, llvm::Module& module, llvm::Function* entry,
                                         const Ir::Function& function, uint64_t& instructionsLowered, uint64_t& directCalls,
                                         const std::unordered_set<uint64_t>* moduleFunctions,
                                         const LlvmBackend::Configuration& configuration)
{
    llvm::IRBuilder<> builder(context);
    llvm::BasicBlock* entryBlock = llvm::BasicBlock::Create(context, "entry", entry);
    builder.SetInsertPoint(entryBlock);

    Lowering lowering(context, module, builder);
    lowering.function = entry;
    lowering.registers = entry->getArg(0);
    lowering.runtime = entry->getArg(1);
    lowering.status = entry->getArg(2);
    lowering.entryBlock = entryBlock;
    lowering.moduleFunctions = moduleFunctions;
    lowering.directMemoryAccess = configuration.directMemoryAccess;
    lowering.traceBlocks = configuration.traceBlocks;
    lowering.reportForeignAccess = configuration.reportForeignAccess;
    lowering.arenaBase = configuration.arenaBase;
    lowering.arenaLength = configuration.arenaLength;

    if (!LowerFunction(lowering, function))
    {
        entry->deleteBody();
        return nullptr;
    }

    instructionsLowered += lowering.instructionsLowered;
    directCalls += lowering.directCalls;

    return entry;
}

static void OptimizeModule(llvm::Module& module, LlvmBackend::Tier tier, llvm::TargetMachine* machine)
{
    llvm::LoopAnalysisManager loopAnalyses;
    llvm::FunctionAnalysisManager functionAnalyses;
    llvm::CGSCCAnalysisManager callGraphAnalyses;
    llvm::ModuleAnalysisManager moduleAnalyses;

    llvm::PassBuilder passBuilder(machine);
    passBuilder.registerModuleAnalyses(moduleAnalyses);
    passBuilder.registerCGSCCAnalyses(callGraphAnalyses);
    passBuilder.registerFunctionAnalyses(functionAnalyses);
    passBuilder.registerLoopAnalyses(loopAnalyses);
    passBuilder.crossRegisterProxies(loopAnalyses, functionAnalyses, callGraphAnalyses, moduleAnalyses);

    llvm::OptimizationLevel level = tier == LlvmBackend::Tier::Optimized ? llvm::OptimizationLevel::O3 : llvm::OptimizationLevel::O2;

    llvm::ModulePassManager passes = passBuilder.buildPerModuleDefaultPipeline(level);

    passes.run(module, moduleAnalyses);
}

struct FunctionOptimizer
{
    llvm::LoopAnalysisManager loopAnalyses;
    llvm::FunctionAnalysisManager functionAnalyses;
    llvm::CGSCCAnalysisManager callGraphAnalyses;
    llvm::ModuleAnalysisManager moduleAnalyses;
    llvm::PassBuilder passBuilder;
    llvm::FunctionPassManager passes;

    explicit FunctionOptimizer(LlvmBackend::Tier tier)
    {
        passBuilder.registerModuleAnalyses(moduleAnalyses);
        passBuilder.registerCGSCCAnalyses(callGraphAnalyses);
        passBuilder.registerFunctionAnalyses(functionAnalyses);
        passBuilder.registerLoopAnalyses(loopAnalyses);
        passBuilder.crossRegisterProxies(loopAnalyses, functionAnalyses, callGraphAnalyses, moduleAnalyses);

        llvm::OptimizationLevel level = tier == LlvmBackend::Tier::Optimized ? llvm::OptimizationLevel::O3 : llvm::OptimizationLevel::O2;
        passes = passBuilder.buildFunctionSimplificationPipeline(level, llvm::ThinOrFullLTOPhase::None);
    }

    void Run(llvm::Function& function)
    {
        passes.run(function, functionAnalyses);
        functionAnalyses.clear();
    }
};

static void DropUnwindTables(llvm::Module& module)
{
    for (llvm::Function& function : module)
    {
        if (function.isDeclaration()) continue;

        function.addFnAttr(llvm::Attribute::NoUnwind);
        function.setUWTableKind(llvm::UWTableKind::None);
    }

    module.setUwtable(llvm::UWTableKind::None);
}

static void OptimizeFunction(llvm::Function& function, LlvmBackend::Tier tier)
{
    FunctionOptimizer optimizer(tier);
    optimizer.Run(function);
}

static bool IsLowerableOpcode(Ir::Opcode opcode)
{
    switch (opcode)
    {
        case Ir::Opcode::ReadGeneral:
        case Ir::Opcode::WriteGeneral:
        case Ir::Opcode::ReadStackPointer:
        case Ir::Opcode::WriteStackPointer:
        case Ir::Opcode::ReadFlags:
        case Ir::Opcode::WriteFlags:
        case Ir::Opcode::ReadVector:
        case Ir::Opcode::WriteVector:
        case Ir::Opcode::ReadSystemRegister:
        case Ir::Opcode::WriteSystemRegister:
        case Ir::Opcode::FloatAdd:
        case Ir::Opcode::FloatSubtract:
        case Ir::Opcode::FloatMultiply:
        case Ir::Opcode::FloatDivide:
        case Ir::Opcode::FloatCompare:
        case Ir::Opcode::FloatCompareSignalling:
        case Ir::Opcode::FloatConvert:
        case Ir::Opcode::FloatSquareRoot:
        case Ir::Opcode::FloatMultiplyAdd:
        case Ir::Opcode::FloatMinimum:
        case Ir::Opcode::FloatMaximum:
        case Ir::Opcode::FloatRound:
        case Ir::Opcode::FloatConvertRounded:
        case Ir::Opcode::LoadExclusive:
        case Ir::Opcode::StoreExclusive:
        case Ir::Opcode::LoadExclusivePair:
        case Ir::Opcode::StoreExclusivePair:
        case Ir::Opcode::AtomicCompareExchangePair:
        case Ir::Opcode::ReadExclusiveHigh:
        case Ir::Opcode::SetExclusiveExpected:
        case Ir::Opcode::ClearExclusive:
        case Ir::Opcode::AtomicCompareExchange:
        case Ir::Opcode::AtomicReadModifyWrite:
        case Ir::Opcode::VectorSaturatingAddSigned:
        case Ir::Opcode::VectorSaturatingAddUnsigned:
        case Ir::Opcode::VectorSaturatingSubtractSigned:
        case Ir::Opcode::VectorSaturatingSubtractUnsigned:
        case Ir::Opcode::VectorShiftLeftSigned:
        case Ir::Opcode::VectorShiftLeftUnsigned:
        case Ir::Opcode::VectorAdd:
        case Ir::Opcode::VectorSubtract:
        case Ir::Opcode::VectorMultiply:
        case Ir::Opcode::VectorAnd:
        case Ir::Opcode::VectorOr:
        case Ir::Opcode::VectorXor:
        case Ir::Opcode::VectorDuplicate:
        case Ir::Opcode::VectorExtractLane:
        case Ir::Opcode::VectorExtract:
        case Ir::Opcode::VectorInsertLane:
        case Ir::Opcode::VectorReduceAdd:
        case Ir::Opcode::VectorReduceAddLong:
        case Ir::Opcode::VectorFloatCompare:
        case Ir::Opcode::VectorFloatMinimum:
        case Ir::Opcode::VectorFloatMaximum:
        case Ir::Opcode::VectorFloatPairwiseAdd:
        case Ir::Opcode::VectorFloatEstimate:
        case Ir::Opcode::VectorNarrow:
        case Ir::Opcode::VectorPermute:
        case Ir::Opcode::VectorMinMax:
        case Ir::Opcode::VectorPairwise:
        case Ir::Opcode::VectorAbsoluteDifference:
        case Ir::Opcode::VectorAbsolute:
        case Ir::Opcode::VectorCountLeadingZeros:
        case Ir::Opcode::VectorPairwiseAddLong:
        case Ir::Opcode::VectorFloatConvertWidth:
        case Ir::Opcode::VectorTableLookup:
        case Ir::Opcode::VectorReduceMinMax:
        case Ir::Opcode::VectorCompare:
        case Ir::Opcode::VectorReverseBytes:
        case Ir::Opcode::VectorFloatAdd:
        case Ir::Opcode::VectorFloatSubtract:
        case Ir::Opcode::VectorFloatMultiply:
        case Ir::Opcode::VectorFloatMultiplyAdd:
        case Ir::Opcode::VectorFloatRound:
        case Ir::Opcode::VectorFloatDivide:
        case Ir::Opcode::VectorFloatSquareRoot:
        case Ir::Opcode::VectorFloatNegate:
        case Ir::Opcode::VectorFloatAbsolute:
        case Ir::Opcode::VectorConvertFloat:
        case Ir::Opcode::VectorCountBits:
        case Ir::Opcode::VectorWiden:
        case Ir::Opcode::Constant:
        case Ir::Opcode::Add:
        case Ir::Opcode::Subtract:
        case Ir::Opcode::Multiply:
        case Ir::Opcode::MultiplyHighSigned:
        case Ir::Opcode::MultiplyHighUnsigned:
        case Ir::Opcode::And:
        case Ir::Opcode::Or:
        case Ir::Opcode::Xor:
        case Ir::Opcode::Not:
        case Ir::Opcode::ShiftLeft:
        case Ir::Opcode::ShiftRightLogical:
        case Ir::Opcode::ShiftRightArithmetic:
        case Ir::Opcode::RotateRight:
        case Ir::Opcode::CountLeadingZeros:
        case Ir::Opcode::ReverseBits:
        case Ir::Opcode::ReverseBytes:
        case Ir::Opcode::ExtendSigned:
        case Ir::Opcode::ExtendUnsigned:
        case Ir::Opcode::Truncate:
        case Ir::Opcode::Select:
        case Ir::Opcode::Compare:
        case Ir::Opcode::ComputeNzcvAdd:
        case Ir::Opcode::ComputeNzcvSubtract:
        case Ir::Opcode::ComputeNzcvLogical:
        case Ir::Opcode::AddWithCarry:
        case Ir::Opcode::SubtractWithBorrow:
        case Ir::Opcode::DivideSigned:
        case Ir::Opcode::DivideUnsigned:
        case Ir::Opcode::Load:
        case Ir::Opcode::Store:
        case Ir::Opcode::MemoryBarrier:
        case Ir::Opcode::Hint:
        case Ir::Opcode::PointerAuthSign:
        case Ir::Opcode::PointerAuthAuthenticate:
        case Ir::Opcode::PointerAuthStrip:
        case Ir::Opcode::Branch:
        case Ir::Opcode::BranchConditional:
        case Ir::Opcode::BranchIndirect:
        case Ir::Opcode::Call:
        case Ir::Opcode::Return:
        case Ir::Opcode::Undefined:
        case Ir::Opcode::InvalidateInstructionCache: return true;
        default: return false;
    }
}

static bool IsWideType(Ir::ValueType type)
{
    return type == Ir::ValueType::Vector128 || type == Ir::ValueType::Int128;
}

static uint8_t RequiredOperands(Ir::Opcode opcode)
{
    switch (opcode)
    {
        case Ir::Opcode::ReadGeneral:
        case Ir::Opcode::ReadStackPointer:
        case Ir::Opcode::ReadFlags:
        case Ir::Opcode::Constant:
        case Ir::Opcode::Branch:
        case Ir::Opcode::Call:
        case Ir::Opcode::Undefined:
        case Ir::Opcode::MemoryBarrier:
        case Ir::Opcode::Hint:
        case Ir::Opcode::PointerAuthSign:
        case Ir::Opcode::PointerAuthAuthenticate:
        case Ir::Opcode::PointerAuthStrip:
        case Ir::Opcode::ReadVector:
        case Ir::Opcode::ReadSystemRegister:
        case Ir::Opcode::ClearExclusive:
        case Ir::Opcode::ReadExclusiveHigh:
        case Ir::Opcode::BranchConditional: return 0;

        case Ir::Opcode::ComputeNzcvAdd:
        case Ir::Opcode::ComputeNzcvSubtract:
        case Ir::Opcode::AddWithCarry:
        case Ir::Opcode::VectorFloatMultiplyAdd:
        case Ir::Opcode::SubtractWithBorrow: return 3;

        case Ir::Opcode::Add:
        case Ir::Opcode::Subtract:
        case Ir::Opcode::Multiply:
        case Ir::Opcode::MultiplyHighSigned:
        case Ir::Opcode::MultiplyHighUnsigned:
        case Ir::Opcode::And:
        case Ir::Opcode::Or:
        case Ir::Opcode::Xor:
        case Ir::Opcode::ShiftLeft:
        case Ir::Opcode::ShiftRightLogical:
        case Ir::Opcode::ShiftRightArithmetic:
        case Ir::Opcode::RotateRight:
        case Ir::Opcode::Compare:
        case Ir::Opcode::DivideSigned:
        case Ir::Opcode::DivideUnsigned:
        case Ir::Opcode::Select:
        case Ir::Opcode::Store: return 2;

        default: return 1;
    }
}

static bool HandlesWideTypes(Ir::Opcode opcode)
{
    switch (opcode)
    {
        case Ir::Opcode::ReadVector:
        case Ir::Opcode::WriteVector:
        case Ir::Opcode::Load:
        case Ir::Opcode::Store:
        case Ir::Opcode::VectorShiftLeftSigned:
        case Ir::Opcode::VectorShiftLeftUnsigned:
        case Ir::Opcode::VectorAdd:
        case Ir::Opcode::VectorSubtract:
        case Ir::Opcode::VectorMultiply:
        case Ir::Opcode::VectorAnd:
        case Ir::Opcode::VectorOr:
        case Ir::Opcode::VectorXor:
        case Ir::Opcode::VectorDuplicate:
        case Ir::Opcode::VectorExtractLane:
        case Ir::Opcode::VectorExtract:
        case Ir::Opcode::VectorInsertLane:
        case Ir::Opcode::VectorReduceAdd:
        case Ir::Opcode::VectorReduceAddLong:
        case Ir::Opcode::VectorFloatCompare:
        case Ir::Opcode::VectorFloatMinimum:
        case Ir::Opcode::VectorFloatMaximum:
        case Ir::Opcode::VectorFloatPairwiseAdd:
        case Ir::Opcode::VectorFloatEstimate:
        case Ir::Opcode::VectorNarrow:
        case Ir::Opcode::VectorPermute:
        case Ir::Opcode::VectorMinMax:
        case Ir::Opcode::VectorPairwise:
        case Ir::Opcode::VectorAbsoluteDifference:
        case Ir::Opcode::VectorAbsolute:
        case Ir::Opcode::VectorCountLeadingZeros:
        case Ir::Opcode::VectorPairwiseAddLong:
        case Ir::Opcode::VectorFloatConvertWidth:
        case Ir::Opcode::VectorTableLookup:
        case Ir::Opcode::VectorReduceMinMax:
        case Ir::Opcode::VectorCompare:
        case Ir::Opcode::VectorReverseBytes:
        case Ir::Opcode::VectorFloatAdd:
        case Ir::Opcode::VectorFloatSubtract:
        case Ir::Opcode::VectorFloatMultiply:
        case Ir::Opcode::VectorFloatMultiplyAdd:
        case Ir::Opcode::VectorFloatRound:
        case Ir::Opcode::VectorFloatDivide:
        case Ir::Opcode::VectorFloatSquareRoot:
        case Ir::Opcode::VectorFloatNegate:
        case Ir::Opcode::VectorFloatAbsolute:
        case Ir::Opcode::VectorConvertFloat:
        case Ir::Opcode::VectorCountBits:
        case Ir::Opcode::VectorWiden: return true;
        default: break;
    }

    return false;
}

bool LlvmBackend::CanLowerInstruction(const Ir::Instruction& instruction)
{
    if (!IsLowerableOpcode(instruction.opcode)) return false;
    if (instruction.operandCount < RequiredOperands(instruction.opcode)) return false;

    if (IsWideType(instruction.operandType) || IsWideType(instruction.result.type))
        return HandlesWideTypes(instruction.opcode);

    return true;
}

bool LlvmBackend::CanLower(const Ir::Function& function, Ir::Opcode& firstUnsupported)
{
    if (function.blocks.empty())
    {
        firstUnsupported = Ir::Opcode::Invalid;
        return false;
    }

    for (const Ir::Block& block : function.blocks)
    {
        for (const Ir::Instruction& instruction : block.instructions)
        {
            if (CanLowerInstruction(instruction)) continue;

            firstUnsupported = instruction.opcode;
            return false;
        }
    }

    return true;
}

static llvm::Value* FieldPointer(Lowering& lowering, llvm::Value* base, uint64_t offset)
{
    llvm::Type* byteType = llvm::Type::getInt8Ty(lowering.context);
    return lowering.builder.CreateGEP(byteType, base, lowering.builder.getInt64(offset));
}

static void EmitBlockTrace(Lowering& lowering, uint64_t guestAddress)
{
    if (!lowering.traceBlocks) return;

    llvm::IRBuilder<>& builder = lowering.builder;
    llvm::Type* wordType = llvm::Type::getInt64Ty(lowering.context);
    llvm::Type* pointerType = llvm::PointerType::getUnqual(lowering.context);

    llvm::Value* entries = builder.CreateLoad(pointerType, FieldPointer(lowering, lowering.runtime,
                                                                       offsetof(LlvmBackend::RuntimeContext, blockTraceEntries)));
    llvm::Value* cursorSlot = builder.CreateLoad(pointerType, FieldPointer(lowering, lowering.runtime,
                                                                          offsetof(LlvmBackend::RuntimeContext, blockTraceCursor)));

    llvm::Value* cursor = builder.CreateLoad(wordType, cursorSlot);
    llvm::Value* index = builder.CreateAnd(cursor, builder.getInt64(LlvmBackend::BLOCK_TRACE_ENTRIES - 1));

    builder.CreateStore(builder.getInt64(guestAddress), builder.CreateGEP(wordType, entries, index));
    builder.CreateStore(builder.CreateAdd(cursor, builder.getInt64(1)), cursorSlot);
}

static llvm::Value* MaskToType(Lowering& lowering, llvm::Value* value, Ir::ValueType type)
{
    switch (type)
    {
        case Ir::ValueType::Bool: return lowering.builder.CreateAnd(value, lowering.builder.getInt64(1));
        case Ir::ValueType::Int8: return lowering.builder.CreateAnd(value, lowering.builder.getInt64(0xffull));
        case Ir::ValueType::Int16: return lowering.builder.CreateAnd(value, lowering.builder.getInt64(0xffffull));
        case Ir::ValueType::Int32: return lowering.builder.CreateAnd(value, lowering.builder.getInt64(0xffffffffull));
        default: return value;
    }
}

static llvm::Value* SignExtendTo64(Lowering& lowering, llvm::Value* value, int bits)
{
    if (bits >= 64 || bits <= 0) return value;

    uint64_t mask = (1ull << bits) - 1;
    uint64_t sign = 1ull << (bits - 1);

    llvm::Value* masked = lowering.builder.CreateAnd(value, lowering.builder.getInt64(mask));
    llvm::Value* flipped = lowering.builder.CreateXor(masked, lowering.builder.getInt64(sign));
    return lowering.builder.CreateSub(flipped, lowering.builder.getInt64(sign));
}

static llvm::Type* SlotType(Lowering& lowering, uint32_t byteWidth)
{
    if (byteWidth == 16) return llvm::VectorType::get(llvm::Type::getInt64Ty(lowering.context), 2, false);

    return byteWidth == 4 ? (llvm::Type*)llvm::Type::getInt32Ty(lowering.context) : (llvm::Type*)llvm::Type::getInt64Ty(lowering.context);
}

static void RequireSlot(Lowering& lowering, uint64_t offset, uint32_t byteWidth)
{
    if (lowering.slotByOffset.count(offset) != 0) return;

    RegisterSlot slot;
    slot.offset = offset;
    slot.byteWidth = byteWidth;

    lowering.slotByOffset.emplace(offset, lowering.slots.size());
    lowering.slots.push_back(slot);
}

static const RegisterSlot* SlotFor(Lowering& lowering, uint64_t offset)
{
    auto found = lowering.slotByOffset.find(offset);
    if (found == lowering.slotByOffset.end()) return nullptr;

    return &lowering.slots[found->second];
}

static void ReadVectorSlot(Lowering& lowering, uint64_t offset, llvm::Value*& low, llvm::Value*& high)
{
    llvm::IRBuilder<>& builder = lowering.builder;
    llvm::Type* wordType = llvm::Type::getInt64Ty(lowering.context);
    const RegisterSlot* slot = SlotFor(lowering, offset);

    if (!slot || !slot->storage)
    {
        low = builder.CreateLoad(wordType, FieldPointer(lowering, lowering.registers, offset));
        high = builder.CreateLoad(wordType, FieldPointer(lowering, lowering.registers, offset + sizeof(uint64_t)));
        return;
    }

    llvm::Value* pair = builder.CreateLoad(SlotType(lowering, 16), slot->storage);
    low = builder.CreateExtractElement(pair, builder.getInt32(0));
    high = builder.CreateExtractElement(pair, builder.getInt32(1));
}

static void WriteVectorSlot(Lowering& lowering, uint64_t offset, llvm::Value* low, llvm::Value* high)
{
    llvm::IRBuilder<>& builder = lowering.builder;
    const RegisterSlot* slot = SlotFor(lowering, offset);

    if (!slot || !slot->storage)
    {
        builder.CreateStore(low, FieldPointer(lowering, lowering.registers, offset));
        builder.CreateStore(high, FieldPointer(lowering, lowering.registers, offset + sizeof(uint64_t)));
        return;
    }

    llvm::Value* pair = llvm::UndefValue::get(SlotType(lowering, 16));
    pair = builder.CreateInsertElement(pair, low, builder.getInt32(0));
    pair = builder.CreateInsertElement(pair, high, builder.getInt32(1));

    builder.CreateStore(pair, slot->storage);
}

static llvm::Value* ReadRegisterSlot(Lowering& lowering, uint64_t offset)
{
    llvm::Type* wordType = llvm::Type::getInt64Ty(lowering.context);
    const RegisterSlot* slot = SlotFor(lowering, offset);

    if (!slot || !slot->storage) return lowering.builder.CreateLoad(wordType, FieldPointer(lowering, lowering.registers, offset));

    llvm::Value* loaded = lowering.builder.CreateLoad(SlotType(lowering, slot->byteWidth), slot->storage);
    if (slot->byteWidth == 4) return lowering.builder.CreateZExt(loaded, wordType);

    return loaded;
}

static void WriteRegisterSlot(Lowering& lowering, uint64_t offset, llvm::Value* value)
{
    const RegisterSlot* slot = SlotFor(lowering, offset);

    if (!slot || !slot->storage)
    {
        lowering.builder.CreateStore(value, FieldPointer(lowering, lowering.registers, offset));
        return;
    }

    llvm::Value* narrowed = slot->byteWidth == 4 ? lowering.builder.CreateTrunc(value, llvm::Type::getInt32Ty(lowering.context)) : value;
    lowering.builder.CreateStore(narrowed, slot->storage);
}

static void ReloadSlots(Lowering& lowering)
{
    for (const RegisterSlot& slot : lowering.slots)
    {
        if (!slot.storage) continue;

        llvm::Type* type = SlotType(lowering, slot.byteWidth);
        llvm::Value* loaded = lowering.builder.CreateAlignedLoad(type, FieldPointer(lowering, lowering.registers, slot.offset),
                                                                 llvm::MaybeAlign(alignof(GuestState::VectorRegister)));
        lowering.builder.CreateStore(loaded, slot.storage);
    }
}

static void StoreSlotsBack(Lowering& lowering)
{
    for (const RegisterSlot& slot : lowering.slots)
    {
        if (!slot.storage) continue;

        llvm::Value* loaded = lowering.builder.CreateLoad(SlotType(lowering, slot.byteWidth), slot.storage);
        lowering.builder.CreateAlignedStore(loaded, FieldPointer(lowering, lowering.registers, slot.offset),
                                            llvm::MaybeAlign(alignof(GuestState::VectorRegister)));
    }
}

static constexpr uint32_t HOST_FLOAT_EXCEPTIONS = 0x3fu;

static llvm::Value* EmitHostFloatFlags(Lowering& lowering, int slot);
static void EmitHostFloatMode(Lowering& lowering, llvm::Value* mode);

static void EmitFloatStatusCapture(Lowering& lowering)
{
    if (!lowering.usesFloat || !lowering.floatModeScratch) return;

    llvm::IRBuilder<>& builder = lowering.builder;
    llvm::Value* raised = builder.CreateAnd(EmitHostFloatFlags(lowering, 0), builder.getInt32(HOST_FLOAT_EXCEPTIONS));

    struct Mapping
    {
        uint32_t host;
        uint32_t guest;
    };

    static const Mapping MAPPINGS[] = {{0x01u, GuestState::FPSR_INVALID_OPERATION}, {0x04u, GuestState::FPSR_DIVIDE_BY_ZERO},
                                       {0x08u, GuestState::FPSR_OVERFLOW},          {0x10u, GuestState::FPSR_UNDERFLOW},
                                       {0x20u, GuestState::FPSR_INEXACT}};

    llvm::Value* mapped = builder.getInt32(0);

    for (const Mapping& mapping : MAPPINGS)
    {
        llvm::Value* present = builder.CreateICmpNE(builder.CreateAnd(raised, builder.getInt32(mapping.host)), builder.getInt32(0));
        mapped = builder.CreateOr(mapped, builder.CreateSelect(present, builder.getInt32(mapping.guest), builder.getInt32(0)));
    }

    llvm::Value* statusPointer = FieldPointer(lowering, lowering.registers, offsetof(GuestState::Registers, floatingPointStatus));
    llvm::Value* status = builder.CreateLoad(builder.getInt32Ty(), statusPointer);
    builder.CreateStore(builder.CreateOr(status, mapped), statusPointer);
}

static void EmitExit(Lowering& lowering, Interpreter::ExitReason reason, llvm::Value* target, llvm::Value* fault, uint64_t steps)
{
    llvm::IRBuilder<>& builder = lowering.builder;

    EmitFloatStatusCapture(lowering);
    StoreSlotsBack(lowering);

    llvm::Value* stepCount = lowering.stepsSlot ? (llvm::Value*)builder.CreateLoad(builder.getInt64Ty(), lowering.stepsSlot)
                                                : (llvm::Value*)builder.getInt64(steps);

    builder.CreateStore(builder.getInt8((uint8_t)reason), FieldPointer(lowering, lowering.status, offsetof(Interpreter::ExitStatus, reason)));
    builder.CreateStore(stepCount, FieldPointer(lowering, lowering.status, offsetof(Interpreter::ExitStatus, stepsExecuted)));

    if (target)
    {
        builder.CreateStore(target, FieldPointer(lowering, lowering.status, offsetof(Interpreter::ExitStatus, targetAddress)));
        builder.CreateStore(builder.getInt8(1), FieldPointer(lowering, lowering.status, offsetof(Interpreter::ExitStatus, hasTargetAddress)));
    }

    if (fault)
    {
        builder.CreateStore(fault, FieldPointer(lowering, lowering.status, offsetof(Interpreter::ExitStatus, faultAddress)));
        builder.CreateStore(builder.getInt8(1), FieldPointer(lowering, lowering.status, offsetof(Interpreter::ExitStatus, hasFaultAddress)));
    }

    builder.CreateRetVoid();
}

static llvm::BasicBlock* TargetBlock(Lowering& lowering, uint64_t address, uint64_t steps);
static llvm::FunctionCallee HelperCallee(Lowering& lowering, const char* name, llvm::Type* returnType, llvm::ArrayRef<llvm::Type*> arguments);
static bool OrderIsAcquire(Ir::MemoryOrder order);
static bool OrderNeedsStoreLoadFence(Ir::MemoryOrder order);

static llvm::FunctionCallee HelperCallee(Lowering& lowering, const char* name, llvm::Type* returnType, llvm::ArrayRef<llvm::Type*> arguments);

static llvm::Value* EmitGuestPointer(Lowering& lowering, llvm::Value* address, GuestState::GuestAddress guestAddress)
{
    (void)guestAddress;

    llvm::IRBuilder<>& builder = lowering.builder;
    llvm::Type* pointerType = llvm::PointerType::getUnqual(lowering.context);

    if (!lowering.reportForeignAccess || lowering.arenaLength == 0) return builder.CreateIntToPtr(address, pointerType);

    llvm::Type* wordType = builder.getInt64Ty();

    llvm::GlobalVariable* tally = lowering.module.getGlobalVariable("aclForeignAccessTally");
    if (!tally)
    {
        tally = new llvm::GlobalVariable(lowering.module, wordType, false, llvm::GlobalValue::ExternalLinkage, nullptr,
                                         "aclForeignAccessTally");
    }

    llvm::Value* offset = builder.CreateSub(address, builder.getInt64(lowering.arenaBase));
    llvm::Value* foreign = builder.CreateICmpUGE(offset, builder.getInt64(lowering.arenaLength));

    llvm::Value* seen = builder.CreateAlignedLoad(wordType, tally, llvm::MaybeAlign(8));
    llvm::Value* raised = builder.CreateAdd(seen, builder.CreateZExt(foreign, wordType));
    builder.CreateAlignedStore(raised, tally, llvm::MaybeAlign(8));

    return builder.CreateIntToPtr(address, pointerType);
}

static void EmitHostFloatMode(Lowering& lowering, llvm::Value* mode)
{
    llvm::IRBuilder<>& builder = lowering.builder;

    llvm::Value* target = FieldPointer(lowering, lowering.floatModeScratch, sizeof(uint32_t));
    llvm::StoreInst* stored = builder.CreateStore(mode, target);
    stored->setVolatile(true);

    llvm::Function* load = llvm::Intrinsic::getOrInsertDeclaration(&lowering.module, llvm::Intrinsic::x86_sse_ldmxcsr);
    builder.CreateCall(load, {target});
}

static llvm::Value* EmitHostFloatFlags(Lowering& lowering, int slot)
{
    llvm::IRBuilder<>& builder = lowering.builder;

    llvm::Value* target = slot == 0 ? lowering.floatModeScratch : FieldPointer(lowering, lowering.floatModeScratch, sizeof(uint32_t));

    llvm::Function* store = llvm::Intrinsic::getOrInsertDeclaration(&lowering.module, llvm::Intrinsic::x86_sse_stmxcsr);
    builder.CreateCall(store, {target});

    llvm::LoadInst* loaded = builder.CreateLoad(builder.getInt32Ty(), target);
    loaded->setVolatile(true);

    return loaded;
}

static bool EmitNativeFloatSquareRoot(Lowering& lowering, const Ir::Instruction& instruction, llvm::Value* left, llvm::Value*& produced)
{
    if (instruction.operandType != Ir::ValueType::Float32 && instruction.operandType != Ir::ValueType::Float64) return false;
    if (instruction.result.type != instruction.operandType) return false;
    if (!left || !lowering.floatModeScratch) return false;

    llvm::IRBuilder<>& builder = lowering.builder;
    bool single = instruction.operandType == Ir::ValueType::Float32;

    llvm::Type* wordType = llvm::Type::getInt64Ty(lowering.context);
    llvm::Type* narrowType = llvm::Type::getInt32Ty(lowering.context);
    llvm::Type* bitsType = single ? narrowType : wordType;
    llvm::Type* realType = single ? llvm::Type::getFloatTy(lowering.context) : llvm::Type::getDoubleTy(lowering.context);

    llvm::Value* leftReal = builder.CreateBitCast(single ? builder.CreateTrunc(left, narrowType) : left, realType);
    llvm::Value* outcome = builder.CreateUnaryIntrinsic(llvm::Intrinsic::sqrt, leftReal);

    llvm::Value* leftNan = builder.CreateFCmpUNO(leftReal, leftReal);
    llvm::Value* outcomeNan = builder.CreateFCmpUNO(outcome, outcome);
    llvm::Value* generatedNan = builder.CreateAnd(outcomeNan, builder.CreateNot(leftNan));

    llvm::Value* defaultNan = single ? (llvm::Value*)builder.getInt32(0x7fc00000u) : (llvm::Value*)builder.getInt64(0x7ff8000000000000ull);
    llvm::Value* bits = builder.CreateSelect(generatedNan, defaultNan, builder.CreateBitCast(outcome, bitsType));

    lowering.raisedNativeFloat = true;
    produced = single ? builder.CreateZExt(bits, wordType) : bits;

    return true;
}

static bool EmitNativeFloatCompare(Lowering& lowering, const Ir::Instruction& instruction, llvm::Value* left, llvm::Value* right,
                                   llvm::Value*& produced)
{
    if (instruction.operandType != Ir::ValueType::Float32 && instruction.operandType != Ir::ValueType::Float64) return false;
    if (!left || !right) return false;

    llvm::IRBuilder<>& builder = lowering.builder;
    bool single = instruction.operandType == Ir::ValueType::Float32;
    bool signalling = instruction.opcode == Ir::Opcode::FloatCompareSignalling;

    llvm::Type* narrowType = llvm::Type::getInt32Ty(lowering.context);
    llvm::Type* realType = single ? llvm::Type::getFloatTy(lowering.context) : llvm::Type::getDoubleTy(lowering.context);

    llvm::Value* leftReal = builder.CreateBitCast(single ? builder.CreateTrunc(left, narrowType) : left, realType);
    llvm::Value* rightReal = builder.CreateBitCast(single ? builder.CreateTrunc(right, narrowType) : right, realType);

    llvm::Value* unordered = signalling ? builder.CreateFCmpS(llvm::CmpInst::FCMP_UNO, leftReal, rightReal)
                                        : builder.CreateFCmpUNO(leftReal, rightReal);
    llvm::Value* equal = signalling ? builder.CreateFCmpS(llvm::CmpInst::FCMP_OEQ, leftReal, rightReal)
                                    : builder.CreateFCmpOEQ(leftReal, rightReal);
    llvm::Value* less = signalling ? builder.CreateFCmpS(llvm::CmpInst::FCMP_OLT, leftReal, rightReal)
                                   : builder.CreateFCmpOLT(leftReal, rightReal);

    llvm::Value* ordered = builder.CreateSelect(less, builder.getInt64(GuestState::FLAG_NEGATIVE),
                                                builder.getInt64(GuestState::FLAG_CARRY));
    ordered = builder.CreateSelect(equal, builder.getInt64(GuestState::FLAG_ZERO | GuestState::FLAG_CARRY), ordered);

    produced = builder.CreateSelect(unordered, builder.getInt64(GuestState::FLAG_CARRY | GuestState::FLAG_OVERFLOW), ordered);

    lowering.raisedNativeFloat = true;

    return true;
}

static bool EmitNativeFloatMultiplyAdd(Lowering& lowering, const Ir::Instruction& instruction, llvm::Value* left, llvm::Value* right,
                                       llvm::Value* third, llvm::Value*& produced)
{
    if (instruction.operandType != Ir::ValueType::Float32 && instruction.operandType != Ir::ValueType::Float64) return false;
    if (instruction.result.type != instruction.operandType) return false;
    if (!left || !right || !third) return false;

    llvm::IRBuilder<>& builder = lowering.builder;
    bool single = instruction.operandType == Ir::ValueType::Float32;

    llvm::Type* wordType = llvm::Type::getInt64Ty(lowering.context);
    llvm::Type* narrowType = llvm::Type::getInt32Ty(lowering.context);
    llvm::Type* bitsType = single ? narrowType : wordType;
    llvm::Type* realType = single ? llvm::Type::getFloatTy(lowering.context) : llvm::Type::getDoubleTy(lowering.context);

    llvm::Value* factor = builder.CreateBitCast(single ? builder.CreateTrunc(left, narrowType) : left, realType);
    llvm::Value* other = builder.CreateBitCast(single ? builder.CreateTrunc(right, narrowType) : right, realType);
    llvm::Value* addend = builder.CreateBitCast(single ? builder.CreateTrunc(third, narrowType) : third, realType);

    if ((instruction.immediate & 1) != 0) factor = builder.CreateFNeg(factor);
    if ((instruction.immediate & 2) != 0) addend = builder.CreateFNeg(addend);

    llvm::Value* outcome = builder.CreateIntrinsic(llvm::Intrinsic::fma, {realType}, {factor, other, addend});

    llvm::Value* anyNan = builder.CreateOr(builder.CreateFCmpUNO(factor, factor),
                                           builder.CreateOr(builder.CreateFCmpUNO(other, other), builder.CreateFCmpUNO(addend, addend)));
    llvm::Value* generatedNan = builder.CreateAnd(builder.CreateFCmpUNO(outcome, outcome), builder.CreateNot(anyNan));

    llvm::Value* defaultNan = single ? (llvm::Value*)builder.getInt32(0x7fc00000u) : (llvm::Value*)builder.getInt64(0x7ff8000000000000ull);
    llvm::Value* bits = builder.CreateSelect(generatedNan, defaultNan, builder.CreateBitCast(outcome, bitsType));

    lowering.raisedNativeFloat = true;
    produced = single ? builder.CreateZExt(bits, wordType) : bits;

    return true;
}

static bool EmitNativeFloatConvert(Lowering& lowering, const Ir::Instruction& instruction, llvm::Value* left, llvm::Value*& produced)
{
    if (!left) return false;

    llvm::IRBuilder<>& builder = lowering.builder;
    llvm::Type* wordType = llvm::Type::getInt64Ty(lowering.context);
    llvm::Type* narrowType = llvm::Type::getInt32Ty(lowering.context);
    llvm::Type* floatType = llvm::Type::getFloatTy(lowering.context);
    llvm::Type* doubleType = llvm::Type::getDoubleTy(lowering.context);

    if (instruction.immediate == 1)
    {
        produced = left;
        return true;
    }

    if (instruction.immediate == 2 || instruction.immediate == 3)
    {
        bool wideSource = instruction.operands[0].type == Ir::ValueType::Int64;
        bool wideResult = instruction.result.type == Ir::ValueType::Float64;

        llvm::Type* sourceType = wideSource ? wordType : narrowType;
        llvm::Value* source = wideSource ? left : builder.CreateTrunc(left, narrowType);

        llvm::Value* real = instruction.immediate == 3 ? builder.CreateUIToFP(source, wideResult ? doubleType : floatType)
                                                       : builder.CreateSIToFP(source, wideResult ? doubleType : floatType);

        (void)sourceType;

        llvm::Value* bits = builder.CreateBitCast(real, wideResult ? wordType : narrowType);

        lowering.raisedNativeFloat = true;
        produced = wideResult ? bits : builder.CreateZExt(bits, wordType);

        return true;
    }

    if (instruction.immediate == 4 || instruction.immediate == 5)
    {
        bool wideSource = instruction.operands[0].type == Ir::ValueType::Float64;
        bool signedForm = instruction.immediate == 4;
        int resultBits = instruction.result.type == Ir::ValueType::Int64 ? 64 : 32;

        llvm::Type* realType = wideSource ? doubleType : floatType;
        llvm::Type* resultType = llvm::Type::getIntNTy(lowering.context, (unsigned)resultBits);

        EmitFloatStatusCapture(lowering);

        llvm::Value* value = builder.CreateBitCast(wideSource ? left : builder.CreateTrunc(left, narrowType), realType);
        llvm::Value* truncated = builder.CreateUnaryIntrinsic(llvm::Intrinsic::trunc, value);

        llvm::Value* saturated = builder.CreateIntrinsic(signedForm ? llvm::Intrinsic::fptosi_sat : llvm::Intrinsic::fptoui_sat,
                                                         {resultType, realType}, {value});

        double lowest = signedForm ? -std::ldexp(1.0, resultBits - 1) : 0.0;
        double highest = std::ldexp(1.0, signedForm ? resultBits - 1 : resultBits);

        llvm::Value* belowRange = builder.CreateFCmpOLT(truncated, llvm::ConstantFP::get(realType, lowest));
        llvm::Value* aboveRange = builder.CreateFCmpOGE(truncated, llvm::ConstantFP::get(realType, highest));

        llvm::Value* invalid = builder.CreateOr(builder.CreateFCmpUNO(value, value), builder.CreateOr(belowRange, aboveRange));
        llvm::Value* inexact = builder.CreateFCmpONE(truncated, value);

        llvm::Value* raised = builder.CreateSelect(invalid, builder.getInt32(GuestState::FPSR_INVALID_OPERATION),
                                                   builder.CreateSelect(inexact, builder.getInt32(GuestState::FPSR_INEXACT),
                                                                        builder.getInt32(0)));

        llvm::Value* statusPointer = FieldPointer(lowering, lowering.registers, offsetof(GuestState::Registers, floatingPointStatus));
        llvm::Value* status = builder.CreateLoad(narrowType, statusPointer);
        builder.CreateStore(builder.CreateOr(status, raised), statusPointer);

        EmitHostFloatMode(lowering, builder.CreateAnd(EmitHostFloatFlags(lowering, 0), builder.getInt32(~HOST_FLOAT_EXCEPTIONS)));

        produced = resultBits == 64 ? saturated : builder.CreateZExt(saturated, wordType);
        return true;
    }

    if (instruction.immediate == 6)
    {
        bool wideSource = instruction.operands[0].type == Ir::ValueType::Float64;
        bool wideResult = instruction.result.type == Ir::ValueType::Float64;
        if (wideSource == wideResult) return false;

        llvm::Value* source = builder.CreateBitCast(wideSource ? left : builder.CreateTrunc(left, narrowType),
                                                    wideSource ? doubleType : floatType);

        llvm::Value* converted = wideResult ? builder.CreateFPExt(source, doubleType) : builder.CreateFPTrunc(source, floatType);
        llvm::Value* bits = builder.CreateBitCast(converted, wideResult ? wordType : narrowType);

        lowering.raisedNativeFloat = true;
        produced = wideResult ? bits : builder.CreateZExt(bits, wordType);

        return true;
    }

    return false;
}

static bool EmitNativeFloatRound(Lowering& lowering, const Ir::Instruction& instruction, llvm::Value* left, llvm::Value*& produced)
{
    if (instruction.operandType != Ir::ValueType::Float32 && instruction.operandType != Ir::ValueType::Float64) return false;
    if (!left) return false;

    llvm::Intrinsic::ID rounding = llvm::Intrinsic::nearbyint;

    if (instruction.immediate == 1) rounding = llvm::Intrinsic::ceil;
    else if (instruction.immediate == 2) rounding = llvm::Intrinsic::floor;
    else if (instruction.immediate == 3) rounding = llvm::Intrinsic::trunc;
    else if (instruction.immediate == 4) rounding = llvm::Intrinsic::round;
    else if (instruction.immediate == 5) rounding = llvm::Intrinsic::rint;
    else if (instruction.immediate != 0) return false;

    llvm::IRBuilder<>& builder = lowering.builder;
    bool single = instruction.operandType == Ir::ValueType::Float32;

    llvm::Type* wordType = llvm::Type::getInt64Ty(lowering.context);
    llvm::Type* narrowType = llvm::Type::getInt32Ty(lowering.context);
    llvm::Type* realType = single ? llvm::Type::getFloatTy(lowering.context) : llvm::Type::getDoubleTy(lowering.context);

    llvm::Value* value = builder.CreateBitCast(single ? builder.CreateTrunc(left, narrowType) : left, realType);
    llvm::Value* outcome = builder.CreateUnaryIntrinsic(rounding, value);

    llvm::Value* bits = builder.CreateBitCast(outcome, single ? narrowType : wordType);

    lowering.raisedNativeFloat = true;
    produced = single ? builder.CreateZExt(bits, wordType) : bits;

    return true;
}

static bool EmitNativeFloatExtremum(Lowering& lowering, const Ir::Instruction& instruction, llvm::Value* left, llvm::Value* right,
                                    llvm::Value*& produced)
{
    if (instruction.operandType != Ir::ValueType::Float32 && instruction.operandType != Ir::ValueType::Float64) return false;
    if (instruction.result.type != instruction.operandType) return false;
    if (!left || !right) return false;

    llvm::IRBuilder<>& builder = lowering.builder;
    bool single = instruction.operandType == Ir::ValueType::Float32;
    bool minimum = instruction.opcode == Ir::Opcode::FloatMinimum;
    bool numeric = (instruction.immediate & 1) != 0;

    llvm::Type* wordType = llvm::Type::getInt64Ty(lowering.context);
    llvm::Type* narrowType = llvm::Type::getInt32Ty(lowering.context);
    llvm::Type* bitsType = single ? narrowType : wordType;
    llvm::Type* realType = single ? llvm::Type::getFloatTy(lowering.context) : llvm::Type::getDoubleTy(lowering.context);

    llvm::Value* leftBits = single ? builder.CreateTrunc(left, narrowType) : left;
    llvm::Value* rightBits = single ? builder.CreateTrunc(right, narrowType) : right;

    llvm::Value* leftReal = builder.CreateBitCast(leftBits, realType);
    llvm::Value* rightReal = builder.CreateBitCast(rightBits, realType);

    llvm::Value* leftNan = builder.CreateFCmpUNO(leftReal, leftReal);
    llvm::Value* rightNan = builder.CreateFCmpUNO(rightReal, rightReal);

    llvm::Value* bothZero = builder.CreateAnd(builder.CreateFCmpOEQ(leftReal, llvm::ConstantFP::get(realType, 0.0)),
                                              builder.CreateFCmpOEQ(rightReal, llvm::ConstantFP::get(realType, 0.0)));

    llvm::Value* signMask = single ? (llvm::Value*)builder.getInt32(0x80000000u) : (llvm::Value*)builder.getInt64(0x8000000000000000ull);
    llvm::Value* leftNegativeZero =
        builder.CreateICmpNE(builder.CreateAnd(leftBits, signMask), llvm::ConstantInt::get(bitsType, 0));

    llvm::Value* ordered = minimum ? builder.CreateSelect(builder.CreateFCmpOLT(leftReal, rightReal), leftBits, rightBits)
                                   : builder.CreateSelect(builder.CreateFCmpOGT(leftReal, rightReal), leftBits, rightBits);

    llvm::Value* zeroChoice = minimum ? builder.CreateSelect(leftNegativeZero, leftBits, rightBits)
                                      : builder.CreateSelect(leftNegativeZero, rightBits, leftBits);

    llvm::Value* chosen = builder.CreateSelect(bothZero, zeroChoice, ordered);

    llvm::Value* defaultNan = single ? (llvm::Value*)builder.getInt32(0x7fc00000u) : (llvm::Value*)builder.getInt64(0x7ff8000000000000ull);
    llvm::Value* onlyLeftNan = builder.CreateAnd(leftNan, builder.CreateNot(rightNan));
    llvm::Value* onlyRightNan = builder.CreateAnd(rightNan, builder.CreateNot(leftNan));

    llvm::Value* singleNanResult = numeric ? builder.CreateSelect(onlyLeftNan, rightBits, leftBits) : defaultNan;

    chosen = builder.CreateSelect(builder.CreateOr(onlyLeftNan, onlyRightNan), singleNanResult, chosen);
    chosen = builder.CreateSelect(builder.CreateAnd(leftNan, rightNan), leftBits, chosen);

    lowering.raisedNativeFloat = true;
    produced = single ? builder.CreateZExt(chosen, wordType) : chosen;

    return true;
}

static bool EmitNativeFloatArithmetic(Lowering& lowering, const Ir::Instruction& instruction, llvm::Value* left, llvm::Value* right,
                                      llvm::Value* third, llvm::Value*& produced)
{
    if (instruction.opcode == Ir::Opcode::FloatCompare || instruction.opcode == Ir::Opcode::FloatCompareSignalling)
        return EmitNativeFloatCompare(lowering, instruction, left, right, produced);

    if (instruction.opcode == Ir::Opcode::FloatMultiplyAdd)
        return EmitNativeFloatMultiplyAdd(lowering, instruction, left, right, third, produced);

    if (instruction.opcode == Ir::Opcode::FloatMinimum || instruction.opcode == Ir::Opcode::FloatMaximum)
        return EmitNativeFloatExtremum(lowering, instruction, left, right, produced);

    if (instruction.opcode == Ir::Opcode::FloatRound)
        return EmitNativeFloatRound(lowering, instruction, left, produced);

    if (instruction.opcode == Ir::Opcode::FloatConvert)
        return EmitNativeFloatConvert(lowering, instruction, left, produced);

    if (instruction.opcode == Ir::Opcode::FloatSquareRoot) return EmitNativeFloatSquareRoot(lowering, instruction, left, produced);


    if (instruction.operandType != Ir::ValueType::Float32 && instruction.operandType != Ir::ValueType::Float64) return false;
    if (instruction.result.type != instruction.operandType) return false;
    if (!left) return false;

    if (!right) return false;

    bool binary = true;

    llvm::IRBuilder<>& builder = lowering.builder;
    bool single = instruction.operandType == Ir::ValueType::Float32;

    llvm::Type* wordType = llvm::Type::getInt64Ty(lowering.context);
    llvm::Type* narrowType = llvm::Type::getInt32Ty(lowering.context);
    llvm::Type* realType = single ? llvm::Type::getFloatTy(lowering.context) : llvm::Type::getDoubleTy(lowering.context);

    llvm::Value* leftReal = builder.CreateBitCast(single ? builder.CreateTrunc(left, narrowType) : left, realType);
    llvm::Value* rightReal = binary ? builder.CreateBitCast(single ? builder.CreateTrunc(right, narrowType) : right, realType) : nullptr;

    llvm::Value* outcome = nullptr;

    switch (instruction.opcode)
    {
        case Ir::Opcode::FloatAdd: outcome = builder.CreateFAdd(leftReal, rightReal); break;
        case Ir::Opcode::FloatSubtract: outcome = builder.CreateFSub(leftReal, rightReal); break;
        case Ir::Opcode::FloatMultiply: outcome = builder.CreateFMul(leftReal, rightReal); break;
        case Ir::Opcode::FloatDivide: outcome = builder.CreateFDiv(leftReal, rightReal); break;
        default: return false;
    }

    llvm::Value* leftNan = builder.CreateFCmpUNO(leftReal, leftReal);
    llvm::Value* rightNan = builder.CreateFCmpUNO(rightReal, rightReal);
    llvm::Value* outcomeNan = builder.CreateFCmpUNO(outcome, outcome);

    llvm::Value* generatedNan =
        builder.CreateAnd(outcomeNan, builder.CreateAnd(builder.CreateNot(leftNan), builder.CreateNot(rightNan)));

    llvm::Type* bitsType = single ? narrowType : wordType;
    llvm::Value* defaultNan = single ? (llvm::Value*)builder.getInt32(0x7fc00000u) : (llvm::Value*)builder.getInt64(0x7ff8000000000000ull);
    llvm::Value* bits = builder.CreateSelect(generatedNan, defaultNan, builder.CreateBitCast(outcome, bitsType));

    lowering.raisedNativeFloat = true;

    produced = single ? builder.CreateZExt(bits, wordType) : bits;

    return true;
}

static llvm::Value* PackVectorLanes(Lowering& lowering, llvm::Value* low, llvm::Value* high, llvm::Type* laneType, int laneCount,
                                    int totalBytes)
{
    llvm::IRBuilder<>& builder = lowering.builder;
    llvm::Type* wordType = llvm::Type::getInt64Ty(lowering.context);
    llvm::Type* packedType = llvm::Type::getIntNTy(lowering.context, (unsigned)totalBytes * 8);

    llvm::Value* packed = nullptr;

    if (totalBytes == 8) packed = low;
    else
    {
        llvm::Value* wide = llvm::UndefValue::get(llvm::VectorType::get(wordType, 2, false));
        wide = builder.CreateInsertElement(wide, low, builder.getInt32(0));
        wide = builder.CreateInsertElement(wide, high ? high : builder.getInt64(0), builder.getInt32(1));

        packed = builder.CreateBitCast(wide, packedType);
    }

    return builder.CreateBitCast(packed, llvm::VectorType::get(laneType, (unsigned)laneCount, false));
}

static void UnpackVectorLanes(Lowering& lowering, llvm::Value* lanes, int totalBytes, llvm::Value*& low, llvm::Value*& high)
{
    llvm::IRBuilder<>& builder = lowering.builder;
    llvm::Type* wordType = llvm::Type::getInt64Ty(lowering.context);
    llvm::Type* packedType = llvm::Type::getIntNTy(lowering.context, (unsigned)totalBytes * 8);

    llvm::Value* packed = builder.CreateBitCast(lanes, packedType);

    if (totalBytes == 8)
    {
        low = packed;
        high = builder.getInt64(0);
        return;
    }

    llvm::Value* asWords = builder.CreateBitCast(packed, llvm::VectorType::get(wordType, 2, false));
    low = builder.CreateExtractElement(asWords, builder.getInt32(0));
    high = builder.CreateExtractElement(asWords, builder.getInt32(1));
}

static llvm::AtomicOrdering GuestOrdering(Ir::MemoryOrder order)
{
    switch (order)
    {
        case Ir::MemoryOrder::Relaxed: return llvm::AtomicOrdering::Monotonic;
        case Ir::MemoryOrder::Acquire:
        case Ir::MemoryOrder::AcquireReadOnly: return llvm::AtomicOrdering::Acquire;
        case Ir::MemoryOrder::Release: return llvm::AtomicOrdering::Release;
        case Ir::MemoryOrder::AcquireRelease: return llvm::AtomicOrdering::AcquireRelease;
        default: return llvm::AtomicOrdering::SequentiallyConsistent;
    }
}

static bool EmitNativeAtomic(Lowering& lowering, const Ir::Instruction& instruction, llvm::Value* address, llvm::Value* operand,
                             llvm::Value* replacement, llvm::Value*& produced)
{
    if (!lowering.directMemoryAccess) return false;
    if (!address) return false;

    int width = Ir::SizeInBytes(instruction.operandType);
    if (width != 1 && width != 2 && width != 4 && width != 8) return false;

    llvm::IRBuilder<>& builder = lowering.builder;
    llvm::Type* wordType = llvm::Type::getInt64Ty(lowering.context);
    llvm::Type* accessType = llvm::Type::getIntNTy(lowering.context, (unsigned)width * 8);

    llvm::Value* pointer = EmitGuestPointer(lowering, address, instruction.guestAddress);
    llvm::AtomicOrdering ordering = GuestOrdering(instruction.order);

    if (instruction.opcode == Ir::Opcode::AtomicCompareExchange)
    {
        if (!operand || !replacement) return false;

        llvm::Value* wanted = width == 8 ? operand : builder.CreateTrunc(operand, accessType);
        llvm::Value* stored = width == 8 ? replacement : builder.CreateTrunc(replacement, accessType);

        llvm::AtomicCmpXchgInst* exchanged =
            builder.CreateAtomicCmpXchg(pointer, wanted, stored, llvm::MaybeAlign(width), ordering,
                                        llvm::AtomicCmpXchgInst::getStrongestFailureOrdering(ordering));

        llvm::Value* observed = builder.CreateExtractValue(exchanged, 0);
        produced = width == 8 ? observed : builder.CreateZExt(observed, wordType);

        return true;
    }

    if (instruction.opcode == Ir::Opcode::LoadExclusivePair || instruction.opcode == Ir::Opcode::StoreExclusivePair)
    {
        if (width != 4 && width != 8) return false;

        int pairBytes = width * 2;
        llvm::Type* pairType = llvm::Type::getIntNTy(lowering.context, (unsigned)pairBytes * 8);
        llvm::Value* monitor = FieldPointer(lowering, lowering.registers, offsetof(GuestState::Registers, exclusive));

        if (instruction.opcode == Ir::Opcode::LoadExclusivePair)
        {
            llvm::LoadInst* loaded = builder.CreateAlignedLoad(pairType, pointer, llvm::MaybeAlign(pairBytes));
            loaded->setAtomic(ordering == llvm::AtomicOrdering::Monotonic ? llvm::AtomicOrdering::Monotonic
                                                                         : llvm::AtomicOrdering::Acquire);

            llvm::Value* low = builder.CreateZExtOrTrunc(loaded, wordType);
            llvm::Value* high = builder.CreateZExtOrTrunc(builder.CreateLShr(loaded, llvm::ConstantInt::get(pairType, width * 8)),
                                                          wordType);

            builder.CreateStore(address, FieldPointer(lowering, monitor, offsetof(GuestState::ExclusiveMonitor, address)));
            builder.CreateStore(low, FieldPointer(lowering, monitor, offsetof(GuestState::ExclusiveMonitor, valueLow)));
            builder.CreateStore(high, FieldPointer(lowering, monitor, offsetof(GuestState::ExclusiveMonitor, valueHigh)));
            builder.CreateStore(builder.getInt32((uint32_t)pairBytes),
                                FieldPointer(lowering, monitor, offsetof(GuestState::ExclusiveMonitor, widthBytes)));
            builder.CreateStore(builder.getInt8(1), FieldPointer(lowering, monitor, offsetof(GuestState::ExclusiveMonitor, valid)));

            produced = low;
            return true;
        }

        if (!operand || !replacement) return false;

        llvm::Value* recorded =
            builder.CreateLoad(wordType, FieldPointer(lowering, monitor, offsetof(GuestState::ExclusiveMonitor, address)));
        llvm::Value* recordedWidth =
            builder.CreateLoad(builder.getInt32Ty(), FieldPointer(lowering, monitor, offsetof(GuestState::ExclusiveMonitor, widthBytes)));
        llvm::Value* held =
            builder.CreateLoad(builder.getInt8Ty(), FieldPointer(lowering, monitor, offsetof(GuestState::ExclusiveMonitor, valid)));
        llvm::Value* expectedLow =
            builder.CreateLoad(wordType, FieldPointer(lowering, monitor, offsetof(GuestState::ExclusiveMonitor, valueLow)));
        llvm::Value* expectedHigh =
            builder.CreateLoad(wordType, FieldPointer(lowering, monitor, offsetof(GuestState::ExclusiveMonitor, valueHigh)));

        llvm::Value* usable = builder.CreateAnd(builder.CreateICmpNE(held, builder.getInt8(0)), builder.CreateICmpEQ(recorded, address));
        usable = builder.CreateAnd(usable, builder.CreateICmpEQ(recordedWidth, builder.getInt32((uint32_t)pairBytes)));

        llvm::Value* shift = llvm::ConstantInt::get(pairType, width * 8);
        llvm::Value* mask = llvm::ConstantInt::get(pairType, width == 8 ? ~0ull : 0xffffffffull);

        llvm::Value* wanted =
            builder.CreateOr(builder.CreateAnd(builder.CreateZExtOrTrunc(expectedLow, pairType), mask),
                             builder.CreateShl(builder.CreateAnd(builder.CreateZExtOrTrunc(expectedHigh, pairType), mask), shift));
        llvm::Value* stored =
            builder.CreateOr(builder.CreateAnd(builder.CreateZExtOrTrunc(operand, pairType), mask),
                             builder.CreateShl(builder.CreateAnd(builder.CreateZExtOrTrunc(replacement, pairType), mask), shift));

        llvm::BasicBlock* attempt = llvm::BasicBlock::Create(lowering.context, "pair.attempt", lowering.function);
        llvm::BasicBlock* refused = llvm::BasicBlock::Create(lowering.context, "pair.refused", lowering.function);
        llvm::BasicBlock* settled = llvm::BasicBlock::Create(lowering.context, "pair.settled", lowering.function);

        builder.CreateCondBr(usable, attempt, refused);

        builder.SetInsertPoint(attempt);
        llvm::AtomicCmpXchgInst* exchanged =
            builder.CreateAtomicCmpXchg(pointer, wanted, stored, llvm::MaybeAlign(pairBytes), ordering,
                                        llvm::AtomicCmpXchgInst::getStrongestFailureOrdering(ordering));
        llvm::Value* swapped = builder.CreateExtractValue(exchanged, 1);
        builder.CreateBr(settled);

        builder.SetInsertPoint(refused);
        builder.CreateBr(settled);

        builder.SetInsertPoint(settled);
        llvm::PHINode* outcome = builder.CreatePHI(builder.getInt1Ty(), 2);
        outcome->addIncoming(swapped, attempt);
        outcome->addIncoming(builder.getInt1(false), refused);

        builder.CreateMemSet(monitor, builder.getInt8(0), builder.getInt64(sizeof(GuestState::ExclusiveMonitor)),
                             llvm::MaybeAlign(alignof(GuestState::ExclusiveMonitor)));

        produced = builder.CreateSelect(outcome, builder.getInt64(0), builder.getInt64(1));
        return true;
    }

    if (instruction.opcode == Ir::Opcode::LoadExclusive)
    {
        llvm::Value* monitor = FieldPointer(lowering, lowering.registers, offsetof(GuestState::Registers, exclusive));

        llvm::LoadInst* loaded = builder.CreateAlignedLoad(accessType, pointer, llvm::MaybeAlign(width));
        loaded->setAtomic(ordering == llvm::AtomicOrdering::Monotonic ? llvm::AtomicOrdering::Monotonic : llvm::AtomicOrdering::Acquire);

        llvm::Value* widened = width == 8 ? (llvm::Value*)loaded : builder.CreateZExt(loaded, wordType);

        builder.CreateStore(address, FieldPointer(lowering, monitor, offsetof(GuestState::ExclusiveMonitor, address)));
        builder.CreateStore(widened, FieldPointer(lowering, monitor, offsetof(GuestState::ExclusiveMonitor, valueLow)));
        builder.CreateStore(builder.getInt64(0), FieldPointer(lowering, monitor, offsetof(GuestState::ExclusiveMonitor, valueHigh)));
        builder.CreateStore(builder.getInt32((uint32_t)width), FieldPointer(lowering, monitor, offsetof(GuestState::ExclusiveMonitor, widthBytes)));
        builder.CreateStore(builder.getInt8(1), FieldPointer(lowering, monitor, offsetof(GuestState::ExclusiveMonitor, valid)));

        produced = widened;
        return true;
    }

    if (instruction.opcode == Ir::Opcode::StoreExclusive)
    {
        if (!operand) return false;

        llvm::Value* monitor = FieldPointer(lowering, lowering.registers, offsetof(GuestState::Registers, exclusive));

        llvm::Value* recorded = builder.CreateLoad(wordType, FieldPointer(lowering, monitor, offsetof(GuestState::ExclusiveMonitor, address)));
        llvm::Value* recordedWidth =
            builder.CreateLoad(builder.getInt32Ty(), FieldPointer(lowering, monitor, offsetof(GuestState::ExclusiveMonitor, widthBytes)));
        llvm::Value* held = builder.CreateLoad(builder.getInt8Ty(), FieldPointer(lowering, monitor, offsetof(GuestState::ExclusiveMonitor, valid)));
        llvm::Value* expected =
            builder.CreateLoad(wordType, FieldPointer(lowering, monitor, offsetof(GuestState::ExclusiveMonitor, valueLow)));

        llvm::Value* usable = builder.CreateAnd(builder.CreateICmpNE(held, builder.getInt8(0)), builder.CreateICmpEQ(recorded, address));
        usable = builder.CreateAnd(usable, builder.CreateICmpEQ(recordedWidth, builder.getInt32((uint32_t)width)));

        llvm::BasicBlock* attempt = llvm::BasicBlock::Create(lowering.context, "exclusive.attempt", lowering.function);
        llvm::BasicBlock* refused = llvm::BasicBlock::Create(lowering.context, "exclusive.refused", lowering.function);
        llvm::BasicBlock* settled = llvm::BasicBlock::Create(lowering.context, "exclusive.settled", lowering.function);

        builder.CreateCondBr(usable, attempt, refused);

        builder.SetInsertPoint(attempt);
        llvm::Value* wanted = width == 8 ? expected : builder.CreateTrunc(expected, accessType);
        llvm::Value* stored = width == 8 ? operand : builder.CreateTrunc(operand, accessType);

        llvm::AtomicCmpXchgInst* exchanged =
            builder.CreateAtomicCmpXchg(pointer, wanted, stored, llvm::MaybeAlign(width), ordering,
                                        llvm::AtomicCmpXchgInst::getStrongestFailureOrdering(ordering));
        llvm::Value* swapped = builder.CreateExtractValue(exchanged, 1);
        builder.CreateBr(settled);

        builder.SetInsertPoint(refused);
        builder.CreateBr(settled);

        builder.SetInsertPoint(settled);
        llvm::PHINode* outcome = builder.CreatePHI(builder.getInt1Ty(), 2);
        outcome->addIncoming(swapped, attempt);
        outcome->addIncoming(builder.getInt1(false), refused);

        builder.CreateMemSet(monitor, builder.getInt8(0), builder.getInt64(sizeof(GuestState::ExclusiveMonitor)),
                             llvm::MaybeAlign(alignof(GuestState::ExclusiveMonitor)));

        produced = builder.CreateSelect(outcome, builder.getInt64(0), builder.getInt64(1));
        return true;
    }

    if (instruction.opcode != Ir::Opcode::AtomicReadModifyWrite) return false;
    if (!operand) return false;

    llvm::AtomicRMWInst::BinOp operation = llvm::AtomicRMWInst::Xchg;

    switch (instruction.atomicOperation)
    {
        case Ir::AtomicOperation::Swap: operation = llvm::AtomicRMWInst::Xchg; break;
        case Ir::AtomicOperation::Add: operation = llvm::AtomicRMWInst::Add; break;
        case Ir::AtomicOperation::Clear: operation = llvm::AtomicRMWInst::And; break;
        case Ir::AtomicOperation::SetBits: operation = llvm::AtomicRMWInst::Or; break;
        case Ir::AtomicOperation::ExclusiveOr: operation = llvm::AtomicRMWInst::Xor; break;
        case Ir::AtomicOperation::MaxSigned: operation = llvm::AtomicRMWInst::Max; break;
        case Ir::AtomicOperation::MaxUnsigned: operation = llvm::AtomicRMWInst::UMax; break;
        case Ir::AtomicOperation::MinSigned: operation = llvm::AtomicRMWInst::Min; break;
        case Ir::AtomicOperation::MinUnsigned: operation = llvm::AtomicRMWInst::UMin; break;
        default: return false;
    }

    llvm::Value* value = width == 8 ? operand : builder.CreateTrunc(operand, accessType);
    if (instruction.atomicOperation == Ir::AtomicOperation::Clear) value = builder.CreateNot(value);

    llvm::Value* observed = builder.CreateAtomicRMW(operation, pointer, value, llvm::MaybeAlign(width), ordering);

    produced = width == 8 ? observed : builder.CreateZExt(observed, wordType);

    return true;
}

static bool EmitNativeVectorFloat(Lowering& lowering, const Ir::Instruction& instruction, llvm::Value* left, llvm::Value* leftHigh,
                                  llvm::Value* right, llvm::Value* rightHigh, llvm::Value* third, llvm::Value* thirdHigh,
                                  llvm::Value*& produced, llvm::Value*& producedHigh)
{
    if (!left) return false;

    int laneBytes = Ir::LaneSizeInBytes(instruction.laneWidth);
    if (laneBytes != 4 && laneBytes != 8) return false;

    int totalBytes = laneBytes * instruction.laneCount;
    if (totalBytes != 8 && totalBytes != 16) return false;

    llvm::IRBuilder<>& builder = lowering.builder;
    bool single = laneBytes == 4;

    llvm::Type* realType = single ? llvm::Type::getFloatTy(lowering.context) : llvm::Type::getDoubleTy(lowering.context);
    llvm::Type* laneIntType = llvm::Type::getIntNTy(lowering.context, (unsigned)laneBytes * 8);
    llvm::Type* intLanes = llvm::VectorType::get(laneIntType, (unsigned)instruction.laneCount, false);

    if ((instruction.opcode == Ir::Opcode::VectorFloatNegate || instruction.opcode == Ir::Opcode::VectorFloatAbsolute))
    {
        llvm::Value* lanes = PackVectorLanes(lowering, left, leftHigh, laneIntType, instruction.laneCount, totalBytes);
        uint64_t signBit = single ? 0x80000000ull : 0x8000000000000000ull;

        llvm::Value* mask = llvm::ConstantInt::get(laneIntType, instruction.opcode == Ir::Opcode::VectorFloatNegate ? signBit : ~signBit);
        llvm::Value* splat = builder.CreateVectorSplat((unsigned)instruction.laneCount, mask);

        llvm::Value* outcome = instruction.opcode == Ir::Opcode::VectorFloatNegate ? builder.CreateXor(lanes, splat)
                                                                                  : builder.CreateAnd(lanes, splat);

        UnpackVectorLanes(lowering, outcome, totalBytes, produced, producedHigh);
        return true;
    }

    if (instruction.opcode == Ir::Opcode::VectorFloatCompare)
    {
        uint64_t mode = instruction.immediate & 0x7;
        bool againstZero = (instruction.immediate & 0x8) != 0;
        bool absolute = (instruction.immediate & 0x10) != 0;

        if (mode > 4) return false;
        if (!againstZero && !right) return false;

        llvm::Value* leftLanes = PackVectorLanes(lowering, left, leftHigh, realType, instruction.laneCount, totalBytes);
        llvm::Value* rightLanes = againstZero ? llvm::Constant::getNullValue(leftLanes->getType())
                                              : PackVectorLanes(lowering, right, rightHigh, realType, instruction.laneCount, totalBytes);

        if (absolute)
        {
            leftLanes = builder.CreateUnaryIntrinsic(llvm::Intrinsic::fabs, leftLanes);
            rightLanes = builder.CreateUnaryIntrinsic(llvm::Intrinsic::fabs, rightLanes);
        }

        llvm::CmpInst::Predicate predicate = llvm::CmpInst::FCMP_OEQ;

        if (mode == 1) predicate = llvm::CmpInst::FCMP_OGE;
        else if (mode == 2) predicate = llvm::CmpInst::FCMP_OGT;
        else if (mode == 3) predicate = llvm::CmpInst::FCMP_OLE;
        else if (mode == 4) predicate = llvm::CmpInst::FCMP_OLT;

        llvm::Value* outcome = builder.CreateFCmp(predicate, leftLanes, rightLanes);

        lowering.raisedNativeFloat = true;
        UnpackVectorLanes(lowering, builder.CreateSExt(outcome, intLanes), totalBytes, produced, producedHigh);

        return true;
    }

    if (instruction.opcode == Ir::Opcode::VectorFloatMultiplyAdd)
    {
        if (!right || !third) return false;

        bool negateProduct = (instruction.immediate & 1) != 0;
        bool negateAddend = (instruction.immediate & 2) != 0;

        llvm::Value* factor = PackVectorLanes(lowering, left, leftHigh, realType, instruction.laneCount, totalBytes);
        llvm::Value* other = PackVectorLanes(lowering, right, rightHigh, realType, instruction.laneCount, totalBytes);
        llvm::Value* addend = PackVectorLanes(lowering, third, thirdHigh, realType, instruction.laneCount, totalBytes);

        if (negateProduct) factor = builder.CreateFNeg(factor);
        if (negateAddend) addend = builder.CreateFNeg(addend);

        llvm::Value* outcome = builder.CreateIntrinsic(llvm::Intrinsic::fma, {factor->getType()}, {factor, other, addend});

        llvm::Value* anyNan =
            builder.CreateOr(builder.CreateFCmpUNO(factor, factor),
                             builder.CreateOr(builder.CreateFCmpUNO(other, other), builder.CreateFCmpUNO(addend, addend)));
        llvm::Value* generatedNan = builder.CreateAnd(builder.CreateFCmpUNO(outcome, outcome), builder.CreateNot(anyNan));

        uint64_t quiet = single ? 0x7fc00000ull : 0x7ff8000000000000ull;
        llvm::Value* defaultLanes = builder.CreateVectorSplat((unsigned)instruction.laneCount, llvm::ConstantInt::get(laneIntType, quiet));

        lowering.raisedNativeFloat = true;
        UnpackVectorLanes(lowering, builder.CreateSelect(generatedNan, defaultLanes, builder.CreateBitCast(outcome, intLanes)), totalBytes,
                          produced, producedHigh);

        return true;
    }

    if (instruction.opcode == Ir::Opcode::VectorConvertFloat)
    {
        bool toFloat = (instruction.immediate & 1) != 0;
        bool isSigned = (instruction.immediate & 2) != 0;

        llvm::Type* integerLanes = llvm::VectorType::get(laneIntType, (unsigned)instruction.laneCount, false);

        if (toFloat)
        {
            llvm::Value* lanes = PackVectorLanes(lowering, left, leftHigh, laneIntType, instruction.laneCount, totalBytes);
            llvm::Value* real = isSigned ? builder.CreateSIToFP(lanes, PackVectorLanes(lowering, left, leftHigh, realType,
                                                                                       instruction.laneCount, totalBytes)
                                                                          ->getType())
                                         : builder.CreateUIToFP(lanes, PackVectorLanes(lowering, left, leftHigh, realType,
                                                                                       instruction.laneCount, totalBytes)
                                                                          ->getType());

            lowering.raisedNativeFloat = true;
            UnpackVectorLanes(lowering, builder.CreateBitCast(real, integerLanes), totalBytes, produced, producedHigh);

            return true;
        }

        llvm::Value* lanes = PackVectorLanes(lowering, left, leftHigh, realType, instruction.laneCount, totalBytes);
        llvm::Value* saturated = builder.CreateIntrinsic(isSigned ? llvm::Intrinsic::fptosi_sat : llvm::Intrinsic::fptoui_sat,
                                                         {integerLanes, lanes->getType()}, {lanes});

        lowering.raisedNativeFloat = true;
        UnpackVectorLanes(lowering, saturated, totalBytes, produced, producedHigh);

        return true;
    }

    if (instruction.opcode == Ir::Opcode::VectorFloatConvertWidth)
    {
        bool widen = (instruction.immediate & 1) != 0;
        bool highHalf = (instruction.immediate & 2) != 0;
        bool halfPrecision = (instruction.immediate & 4) != 0;
        int offset = highHalf ? instruction.laneCount : 0;

        llvm::Type* wordType = llvm::Type::getInt64Ty(lowering.context);
        llvm::Type* floatType = halfPrecision ? llvm::Type::getHalfTy(lowering.context) : llvm::Type::getFloatTy(lowering.context);
        llvm::Type* doubleType = halfPrecision ? llvm::Type::getFloatTy(lowering.context) : llvm::Type::getDoubleTy(lowering.context);

        int narrowBytes = halfPrecision ? 2 : 4;
        int wideBytes = halfPrecision ? 4 : 8;

        llvm::Type* wordLanes = llvm::VectorType::get(llvm::Type::getIntNTy(lowering.context, (unsigned)wideBytes * 8),
                                                      (unsigned)instruction.laneCount, false);
        llvm::Type* narrowLanes = llvm::VectorType::get(llvm::Type::getIntNTy(lowering.context, (unsigned)narrowBytes * 8),
                                                        (unsigned)instruction.laneCount, false);

        std::vector<int> selection;
        for (int lane = 0; lane < instruction.laneCount; ++lane) selection.push_back(lane + offset);

        if (widen)
        {
            if (instruction.laneCount * wideBytes != 16) return false;

            llvm::Value* narrow = PackVectorLanes(lowering, left, leftHigh, floatType, instruction.laneCount * 2, 16);
            llvm::Value* chosen = builder.CreateShuffleVector(narrow, llvm::UndefValue::get(narrow->getType()), selection);

            llvm::Value* widened =
                builder.CreateFPExt(chosen, llvm::VectorType::get(doubleType, (unsigned)instruction.laneCount, false));

            lowering.raisedNativeFloat = true;
            UnpackVectorLanes(lowering, builder.CreateBitCast(widened, wordLanes), 16, produced, producedHigh);

            return true;
        }

        if (instruction.laneCount * narrowBytes != 8) return false;
        if (highHalf && !right) return false;

        llvm::Value* wide = PackVectorLanes(lowering, left, leftHigh, doubleType, instruction.laneCount, 16);
        llvm::Value* narrowed =
            builder.CreateFPTrunc(wide, llvm::VectorType::get(floatType, (unsigned)instruction.laneCount, false));

        llvm::Value* packed = builder.CreateBitCast(builder.CreateBitCast(narrowed, narrowLanes), wordType);

        lowering.raisedNativeFloat = true;

        if (highHalf)
        {
            produced = right;
            producedHigh = packed;
        }
        else
        {
            produced = packed;
            producedHigh = builder.getInt64(0);
        }

        return true;
    }

    if (instruction.opcode == Ir::Opcode::VectorFloatRound)
    {
        llvm::Intrinsic::ID rounding = llvm::Intrinsic::nearbyint;

        if (instruction.immediate == 1) rounding = llvm::Intrinsic::ceil;
        else if (instruction.immediate == 2) rounding = llvm::Intrinsic::floor;
        else if (instruction.immediate == 3) rounding = llvm::Intrinsic::trunc;
        else if (instruction.immediate == 4) rounding = llvm::Intrinsic::round;
        else if (instruction.immediate == 5) rounding = llvm::Intrinsic::rint;
        else if (instruction.immediate != 0) return false;

        llvm::Value* lanes = PackVectorLanes(lowering, left, leftHigh, realType, instruction.laneCount, totalBytes);
        llvm::Value* outcome = builder.CreateUnaryIntrinsic(rounding, lanes);

        uint64_t quiet = single ? 0x7fc00000ull : 0x7ff8000000000000ull;
        llvm::Value* defaultLanes = builder.CreateVectorSplat((unsigned)instruction.laneCount, llvm::ConstantInt::get(laneIntType, quiet));
        llvm::Value* bits = builder.CreateSelect(builder.CreateFCmpUNO(outcome, outcome), defaultLanes,
                                                 builder.CreateBitCast(outcome, intLanes));

        lowering.raisedNativeFloat = true;
        UnpackVectorLanes(lowering, bits, totalBytes, produced, producedHigh);

        return true;
    }

    bool unary = instruction.opcode == Ir::Opcode::VectorFloatSquareRoot;
    if (!unary && !right) return false;

    llvm::Value* leftLanes = PackVectorLanes(lowering, left, leftHigh, realType, instruction.laneCount, totalBytes);
    llvm::Value* rightLanes = unary ? nullptr : PackVectorLanes(lowering, right, rightHigh, realType, instruction.laneCount, totalBytes);

    llvm::Value* outcome = nullptr;

    switch (instruction.opcode)
    {
        case Ir::Opcode::VectorFloatAdd: outcome = builder.CreateFAdd(leftLanes, rightLanes); break;
        case Ir::Opcode::VectorFloatSubtract: outcome = builder.CreateFSub(leftLanes, rightLanes); break;
        case Ir::Opcode::VectorFloatMultiply: outcome = builder.CreateFMul(leftLanes, rightLanes); break;
        case Ir::Opcode::VectorFloatDivide: outcome = builder.CreateFDiv(leftLanes, rightLanes); break;
        case Ir::Opcode::VectorFloatSquareRoot: outcome = builder.CreateUnaryIntrinsic(llvm::Intrinsic::sqrt, leftLanes); break;
        case Ir::Opcode::VectorFloatMinimum:
            outcome = builder.CreateSelect(builder.CreateFCmpOLT(leftLanes, rightLanes), leftLanes, rightLanes);
            break;
        case Ir::Opcode::VectorFloatMaximum:
            outcome = builder.CreateSelect(builder.CreateFCmpOGT(leftLanes, rightLanes), leftLanes, rightLanes);
            break;
        default: return false;
    }

    llvm::Value* leftNan = builder.CreateFCmpUNO(leftLanes, leftLanes);
    llvm::Value* anyNan = unary ? leftNan : builder.CreateOr(leftNan, builder.CreateFCmpUNO(rightLanes, rightLanes));
    llvm::Value* generatedNan = builder.CreateAnd(builder.CreateFCmpUNO(outcome, outcome), builder.CreateNot(anyNan));

    uint64_t defaultNan = single ? 0x7fc00000ull : 0x7ff8000000000000ull;
    llvm::Value* defaultLanes =
        builder.CreateVectorSplat((unsigned)instruction.laneCount, llvm::ConstantInt::get(laneIntType, defaultNan));

    llvm::Value* bits = builder.CreateSelect(generatedNan, defaultLanes, builder.CreateBitCast(outcome, intLanes));

    lowering.raisedNativeFloat = true;
    UnpackVectorLanes(lowering, bits, totalBytes, produced, producedHigh);

    return true;
}

static bool EmitNativeVectorInteger(Lowering& lowering, const Ir::Instruction& instruction, llvm::Value* left, llvm::Value* leftHigh,
                                    llvm::Value* right, llvm::Value* rightHigh, llvm::Value*& produced, llvm::Value*& producedHigh)
{
    if (!left) return false;

    llvm::IRBuilder<>& builder = lowering.builder;
    llvm::Type* wordType = llvm::Type::getInt64Ty(lowering.context);

    llvm::Value* lowLeft = left;
    llvm::Value* highLeft = leftHigh ? leftHigh : builder.getInt64(0);
    llvm::Value* lowRight = right ? right : builder.getInt64(0);
    llvm::Value* highRight = rightHigh ? rightHigh : builder.getInt64(0);

    int logicalBytes = Ir::LaneSizeInBytes(instruction.laneWidth) * instruction.laneCount;

    if (right && logicalBytes == 16 && (instruction.opcode == Ir::Opcode::VectorAnd || instruction.opcode == Ir::Opcode::VectorOr ||
                               instruction.opcode == Ir::Opcode::VectorXor))
    {
        switch (instruction.opcode)
        {
            case Ir::Opcode::VectorAnd:
                produced = builder.CreateAnd(lowLeft, lowRight);
                producedHigh = builder.CreateAnd(highLeft, highRight);
                break;
            case Ir::Opcode::VectorOr:
                produced = builder.CreateOr(lowLeft, lowRight);
                producedHigh = builder.CreateOr(highLeft, highRight);
                break;
            default:
                produced = builder.CreateXor(lowLeft, lowRight);
                producedHigh = builder.CreateXor(highLeft, highRight);
                break;
        }

        return true;
    }

    if (instruction.opcode == Ir::Opcode::VectorTableLookup)
    {
        if (instruction.immediate != 0) return false;
        if (!right) return false;
        if (instruction.laneCount != 8 && instruction.laneCount != 16) return false;

        llvm::Type* byteType = llvm::Type::getInt8Ty(lowering.context);
        llvm::Value* table = PackVectorLanes(lowering, lowLeft, highLeft, byteType, 16, 16);
        llvm::Value* selectors = PackVectorLanes(lowering, lowRight, highRight, byteType, 16, 16);

        llvm::Value* limit = builder.CreateVectorSplat(16, llvm::ConstantInt::get(byteType, 16));
        llvm::Value* zeroing = builder.CreateVectorSplat(16, llvm::ConstantInt::get(byteType, 0x80));

        llvm::Value* inRange = builder.CreateICmpULT(selectors, limit);
        llvm::Value* clamped = builder.CreateSelect(inRange, selectors, zeroing);

        llvm::Function* shuffle = llvm::Intrinsic::getOrInsertDeclaration(&lowering.module, llvm::Intrinsic::x86_ssse3_pshuf_b_128);
        llvm::Value* gathered = builder.CreateCall(shuffle, {table, clamped});

        UnpackVectorLanes(lowering, gathered, 16, produced, producedHigh);

        if (instruction.laneCount == 8) producedHigh = builder.getInt64(0);

        return true;
    }

    if (instruction.opcode == Ir::Opcode::VectorPermute || instruction.opcode == Ir::Opcode::VectorExtract)
    {
        int laneBytes = instruction.opcode == Ir::Opcode::VectorExtract ? 1 : Ir::LaneSizeInBytes(instruction.laneWidth);
        if (laneBytes != 1 && laneBytes != 2 && laneBytes != 4 && laneBytes != 8) return false;
        if (!right) return false;

        int total = instruction.laneCount;
        if (instruction.opcode == Ir::Opcode::VectorExtract && total != 8 && total != 16) total = 16;

        int totalBytes = laneBytes * total;
        if (totalBytes != 8 && totalBytes != 16) return false;

        std::vector<int> selection;
        selection.reserve((size_t)total);

        if (instruction.opcode == Ir::Opcode::VectorExtract)
        {
            int index = instruction.laneIndex;
            if (index < 0 || index >= total) index = 0;

            for (int lane = 0; lane < total; ++lane) selection.push_back(index + lane);
        }
        else
        {
            bool takeOdd = (instruction.immediate & 1) != 0;
            uint64_t kind = instruction.immediate >> 1;

            for (int lane = 0; lane < total; ++lane)
            {
                int source = 0;

                if (kind == 0) source = lane * 2 + (takeOdd ? 1 : 0);
                else if (kind == 1)
                    source = (lane & ~1) + (takeOdd ? 1 : 0) + (lane & 1) * total;
                else
                    source = lane / 2 + (takeOdd ? total / 2 : 0) + (lane & 1) * total;

                if (source < 0 || source >= total * 2) return false;

                selection.push_back(source);
            }
        }

        llvm::Type* laneType = llvm::Type::getIntNTy(lowering.context, (unsigned)laneBytes * 8);
        llvm::Value* lanesLeft = PackVectorLanes(lowering, lowLeft, highLeft, laneType, total, totalBytes);
        llvm::Value* lanesRight = PackVectorLanes(lowering, lowRight, highRight, laneType, total, totalBytes);

        UnpackVectorLanes(lowering, builder.CreateShuffleVector(lanesLeft, lanesRight, selection), totalBytes, produced, producedHigh);
        return true;
    }

    if (instruction.opcode == Ir::Opcode::VectorNarrow)
    {
        int laneBytes = Ir::LaneSizeInBytes(instruction.laneWidth);
        if (laneBytes != 1 && laneBytes != 2 && laneBytes != 4) return false;
        if (laneBytes * instruction.laneCount != 8) return false;

        bool highHalf = (instruction.immediate & 1) != 0;
        if (highHalf && !right) return false;

        llvm::Type* sourceType = llvm::Type::getIntNTy(lowering.context, (unsigned)laneBytes * 16);
        llvm::Type* narrowType = llvm::Type::getIntNTy(lowering.context, (unsigned)laneBytes * 8);

        llvm::Value* sourceLanes = PackVectorLanes(lowering, lowLeft, highLeft, sourceType, instruction.laneCount, 16);
        llvm::Value* narrowed =
            builder.CreateTrunc(sourceLanes, llvm::VectorType::get(narrowType, (unsigned)instruction.laneCount, false));

        llvm::Value* packed = builder.CreateBitCast(narrowed, wordType);

        if (highHalf)
        {
            produced = lowRight;
            producedHigh = packed;
        }
        else
        {
            produced = packed;
            producedHigh = builder.getInt64(0);
        }

        return true;
    }

    if (instruction.opcode == Ir::Opcode::VectorReduceAddLong)
    {
        int laneBytes = Ir::LaneSizeInBytes(instruction.laneWidth);
        if (laneBytes != 1 && laneBytes != 2 && laneBytes != 4) return false;

        int totalBytes = laneBytes * instruction.laneCount;
        if (totalBytes != 8 && totalBytes != 16) return false;

        bool signedSource = (instruction.immediate & 1) != 0;

        llvm::Type* laneType = llvm::Type::getIntNTy(lowering.context, (unsigned)laneBytes * 8);
        llvm::Value* lanes = PackVectorLanes(lowering, lowLeft, highLeft, laneType, instruction.laneCount, totalBytes);

        llvm::Type* wideLanes = llvm::VectorType::get(wordType, (unsigned)instruction.laneCount, false);
        llvm::Value* widened = signedSource ? builder.CreateSExt(lanes, wideLanes) : builder.CreateZExt(lanes, wideLanes);

        llvm::Value* total = builder.CreateAddReduce(widened);
        uint64_t mask = laneBytes >= 4 ? ~0ull : ((1ull << (laneBytes * 16)) - 1);

        produced = builder.CreateAnd(total, builder.getInt64(mask));
        producedHigh = builder.getInt64(0);
        return true;
    }

    if (instruction.opcode == Ir::Opcode::VectorPairwiseAddLong)
    {
        int laneBytes = Ir::LaneSizeInBytes(instruction.laneWidth);
        if (laneBytes != 1 && laneBytes != 2 && laneBytes != 4) return false;

        int totalBytes = laneBytes * instruction.laneCount;
        if (totalBytes != 8 && totalBytes != 16) return false;
        if ((instruction.laneCount & 1) != 0) return false;

        bool unsignedForm = (instruction.immediate & 1) != 0;
        bool accumulate = (instruction.immediate & 2) != 0;
        if (accumulate && !right) return false;

        int pairs = instruction.laneCount / 2;

        llvm::Type* laneType = llvm::Type::getIntNTy(lowering.context, (unsigned)laneBytes * 8);
        llvm::Type* wideType = llvm::Type::getIntNTy(lowering.context, (unsigned)laneBytes * 16);
        llvm::Type* wideLanes = llvm::VectorType::get(wideType, (unsigned)pairs, false);

        llvm::Value* lanes = PackVectorLanes(lowering, lowLeft, highLeft, laneType, instruction.laneCount, totalBytes);

        std::vector<int> evens;
        std::vector<int> odds;

        for (int lane = 0; lane < pairs; ++lane)
        {
            evens.push_back(lane * 2);
            odds.push_back(lane * 2 + 1);
        }

        llvm::Value* undefined = llvm::UndefValue::get(lanes->getType());
        llvm::Value* first = builder.CreateShuffleVector(lanes, undefined, evens);
        llvm::Value* second = builder.CreateShuffleVector(lanes, undefined, odds);

        llvm::Value* wideFirst = unsignedForm ? builder.CreateZExt(first, wideLanes) : builder.CreateSExt(first, wideLanes);
        llvm::Value* wideSecond = unsignedForm ? builder.CreateZExt(second, wideLanes) : builder.CreateSExt(second, wideLanes);

        llvm::Value* outcome = builder.CreateAdd(wideFirst, wideSecond);

        if (accumulate)
            outcome = builder.CreateAdd(outcome, PackVectorLanes(lowering, lowRight, highRight, wideType, pairs, totalBytes));

        UnpackVectorLanes(lowering, outcome, totalBytes, produced, producedHigh);
        return true;
    }

    if (instruction.opcode == Ir::Opcode::VectorReverseBytes)
    {
        int laneBytes = Ir::LaneSizeInBytes(instruction.laneWidth);
        if (laneBytes != 1 && laneBytes != 2 && laneBytes != 4) return false;

        int containerBytes = (int)instruction.immediate;
        if (containerBytes < 2 || containerBytes > 8) return false;

        int perContainer = containerBytes / laneBytes;
        if (perContainer < 2) return false;
        if ((instruction.laneCount % perContainer) != 0) return false;

        int totalBytes = laneBytes * instruction.laneCount;
        if (totalBytes != 8 && totalBytes != 16) return false;

        std::vector<int> selection;
        selection.reserve((size_t)instruction.laneCount);

        for (int lane = 0; lane < instruction.laneCount; lane += perContainer)
            for (int offset = 0; offset < perContainer; ++offset) selection.push_back(lane + perContainer - 1 - offset);

        llvm::Type* laneType = llvm::Type::getIntNTy(lowering.context, (unsigned)laneBytes * 8);
        llvm::Value* lanes = PackVectorLanes(lowering, lowLeft, highLeft, laneType, instruction.laneCount, totalBytes);

        llvm::Value* reversed = builder.CreateShuffleVector(lanes, llvm::UndefValue::get(lanes->getType()), selection);

        UnpackVectorLanes(lowering, reversed, totalBytes, produced, producedHigh);
        return true;
    }

    if (instruction.opcode == Ir::Opcode::VectorReduceMinMax)
    {
        int laneBytes = Ir::LaneSizeInBytes(instruction.laneWidth);
        if (laneBytes != 1 && laneBytes != 2 && laneBytes != 4 && laneBytes != 8) return false;

        int totalBytes = laneBytes * instruction.laneCount;
        if (totalBytes != 8 && totalBytes != 16) return false;

        bool unsignedForm = (instruction.immediate & 1) != 0;
        bool minimum = (instruction.immediate & 2) != 0;

        llvm::Type* laneType = llvm::Type::getIntNTy(lowering.context, (unsigned)laneBytes * 8);
        llvm::Value* lanes = PackVectorLanes(lowering, lowLeft, highLeft, laneType, instruction.laneCount, totalBytes);

        llvm::Intrinsic::ID chosen = minimum ? (unsignedForm ? llvm::Intrinsic::vector_reduce_umin : llvm::Intrinsic::vector_reduce_smin)
                                             : (unsignedForm ? llvm::Intrinsic::vector_reduce_umax : llvm::Intrinsic::vector_reduce_smax);

        llvm::Value* best = builder.CreateIntrinsic(chosen, {lanes->getType()}, {lanes});

        produced = builder.CreateZExt(best, wordType);
        producedHigh = builder.getInt64(0);
        return true;
    }

    if (instruction.opcode == Ir::Opcode::VectorPairwise)
    {
        int laneBytes = Ir::LaneSizeInBytes(instruction.laneWidth);
        if (laneBytes != 1 && laneBytes != 2 && laneBytes != 4 && laneBytes != 8) return false;

        int totalBytes = laneBytes * instruction.laneCount;
        if (totalBytes != 8 && totalBytes != 16) return false;
        if (!right) return false;
        if ((instruction.laneCount & 1) != 0) return false;

        bool unsignedForm = (instruction.immediate & 1) != 0;
        uint64_t kind = (instruction.immediate >> 1) & 0x3;
        if (kind > 2) return false;

        llvm::Type* laneType = llvm::Type::getIntNTy(lowering.context, (unsigned)laneBytes * 8);
        llvm::Value* lanesLeft = PackVectorLanes(lowering, lowLeft, highLeft, laneType, instruction.laneCount, totalBytes);
        llvm::Value* lanesRight = PackVectorLanes(lowering, lowRight, highRight, laneType, instruction.laneCount, totalBytes);

        std::vector<int> evens;
        std::vector<int> odds;

        for (int lane = 0; lane < instruction.laneCount / 2; ++lane)
        {
            evens.push_back(lane * 2);
            odds.push_back(lane * 2 + 1);
        }

        for (int lane = 0; lane < instruction.laneCount / 2; ++lane)
        {
            evens.push_back(instruction.laneCount + lane * 2);
            odds.push_back(instruction.laneCount + lane * 2 + 1);
        }

        llvm::Value* first = builder.CreateShuffleVector(lanesLeft, lanesRight, evens);
        llvm::Value* second = builder.CreateShuffleVector(lanesLeft, lanesRight, odds);

        llvm::Value* outcome = nullptr;

        if (kind == 0) outcome = builder.CreateAdd(first, second);
        else if (kind == 1)
            outcome = builder.CreateBinaryIntrinsic(unsignedForm ? llvm::Intrinsic::umax : llvm::Intrinsic::smax, first, second);
        else
            outcome = builder.CreateBinaryIntrinsic(unsignedForm ? llvm::Intrinsic::umin : llvm::Intrinsic::smin, first, second);

        UnpackVectorLanes(lowering, outcome, totalBytes, produced, producedHigh);
        return true;
    }

    if (instruction.opcode == Ir::Opcode::VectorReduceAdd)
    {
        int laneBytes = Ir::LaneSizeInBytes(instruction.laneWidth);
        if (laneBytes != 1 && laneBytes != 2 && laneBytes != 4 && laneBytes != 8) return false;

        int totalBytes = laneBytes * instruction.laneCount;
        if (totalBytes != 8 && totalBytes != 16) return false;

        llvm::Type* laneType = llvm::Type::getIntNTy(lowering.context, (unsigned)laneBytes * 8);
        llvm::Value* lanes = PackVectorLanes(lowering, lowLeft, highLeft, laneType, instruction.laneCount, totalBytes);

        llvm::Value* total = builder.CreateAddReduce(lanes);

        produced = builder.CreateZExt(total, wordType);
        producedHigh = builder.getInt64(0);
        return true;
    }

    if (instruction.opcode == Ir::Opcode::VectorWiden)
    {
        int laneBytes = Ir::LaneSizeInBytes(instruction.laneWidth);
        if (laneBytes != 1 && laneBytes != 2 && laneBytes != 4) return false;

        int sourceLanes = instruction.laneCount * 2;
        if (laneBytes * sourceLanes != 16) return false;

        bool signedWiden = (instruction.immediate & 1) != 0;
        int offset = (instruction.immediate & 2) != 0 ? instruction.laneCount : 0;

        llvm::Type* laneType = llvm::Type::getIntNTy(lowering.context, (unsigned)laneBytes * 8);
        llvm::Type* wideType = llvm::Type::getIntNTy(lowering.context, (unsigned)laneBytes * 16);

        llvm::Value* lanes = PackVectorLanes(lowering, lowLeft, highLeft, laneType, sourceLanes, 16);

        std::vector<int> selection;
        for (int lane = 0; lane < instruction.laneCount; ++lane) selection.push_back(lane + offset);

        llvm::Value* half = builder.CreateShuffleVector(lanes, llvm::UndefValue::get(lanes->getType()), selection);
        llvm::Type* wideLanes = llvm::VectorType::get(wideType, (unsigned)instruction.laneCount, false);

        llvm::Value* widened = signedWiden ? builder.CreateSExt(half, wideLanes) : builder.CreateZExt(half, wideLanes);

        UnpackVectorLanes(lowering, widened, 16, produced, producedHigh);
        return true;
    }

    if (instruction.opcode == Ir::Opcode::VectorDuplicate || instruction.opcode == Ir::Opcode::VectorExtractLane ||
        instruction.opcode == Ir::Opcode::VectorInsertLane)
    {
        int laneBytes = Ir::LaneSizeInBytes(instruction.laneWidth);
        if (laneBytes != 1 && laneBytes != 2 && laneBytes != 4 && laneBytes != 8) return false;

        int totalBytes = laneBytes * instruction.laneCount;
        llvm::Type* laneType = llvm::Type::getIntNTy(lowering.context, (unsigned)laneBytes * 8);

        if (instruction.opcode == Ir::Opcode::VectorExtractLane)
        {
            int wholeLanes = 16 / laneBytes;
            if (instruction.laneIndex < 0 || instruction.laneIndex >= wholeLanes) return false;

            llvm::Value* lanes = PackVectorLanes(lowering, lowLeft, highLeft, laneType, wholeLanes, 16);
            llvm::Value* lane = builder.CreateExtractElement(lanes, builder.getInt32((unsigned)instruction.laneIndex));

            produced = builder.CreateZExt(lane, wordType);
            producedHigh = builder.getInt64(0);
            return true;
        }

        if (totalBytes != 8 && totalBytes != 16) return false;

        if (instruction.opcode == Ir::Opcode::VectorDuplicate)
        {
            llvm::Value* splat = builder.CreateVectorSplat((unsigned)instruction.laneCount, builder.CreateTrunc(lowLeft, laneType));
            UnpackVectorLanes(lowering, splat, totalBytes, produced, producedHigh);
            return true;
        }

        if (!right) return false;

        int wholeLanes = 16 / laneBytes;
        if (instruction.laneIndex < 0 || instruction.laneIndex >= wholeLanes) return false;

        llvm::Value* lanes = PackVectorLanes(lowering, lowLeft, highLeft, laneType, wholeLanes, 16);
        llvm::Value* inserted =
            builder.CreateInsertElement(lanes, builder.CreateTrunc(lowRight, laneType), builder.getInt32((unsigned)instruction.laneIndex));

        UnpackVectorLanes(lowering, inserted, 16, produced, producedHigh);
        return true;
    }

    if (instruction.opcode == Ir::Opcode::VectorAbsolute || instruction.opcode == Ir::Opcode::VectorCountBits ||
        instruction.opcode == Ir::Opcode::VectorCountLeadingZeros)
    {
        bool counting = instruction.opcode == Ir::Opcode::VectorCountBits;
        if (instruction.opcode == Ir::Opcode::VectorCountLeadingZeros && (instruction.immediate & 1) != 0) return false;

        int laneBytes = counting ? 1 : Ir::LaneSizeInBytes(instruction.laneWidth);
        if (laneBytes != 1 && laneBytes != 2 && laneBytes != 4 && laneBytes != 8) return false;

        int totalBytes = laneBytes * instruction.laneCount;
        if (totalBytes != 8 && totalBytes != 16) return false;

        llvm::Type* laneType = llvm::Type::getIntNTy(lowering.context, (unsigned)laneBytes * 8);
        llvm::Value* lanes = PackVectorLanes(lowering, lowLeft, highLeft, laneType, instruction.laneCount, totalBytes);

        llvm::Value* outcome = nullptr;

        if (instruction.opcode == Ir::Opcode::VectorAbsolute)
            outcome = (instruction.immediate & 1) != 0
                          ? builder.CreateNeg(lanes)
                          : builder.CreateBinaryIntrinsic(llvm::Intrinsic::abs, lanes, builder.getInt1(false));
        else if (counting)
            outcome = builder.CreateUnaryIntrinsic(llvm::Intrinsic::ctpop, lanes);
        else
            outcome = builder.CreateBinaryIntrinsic(llvm::Intrinsic::ctlz, lanes, builder.getInt1(false));

        UnpackVectorLanes(lowering, outcome, totalBytes, produced, producedHigh);
        return true;
    }

    if (instruction.opcode == Ir::Opcode::VectorShiftLeftSigned || instruction.opcode == Ir::Opcode::VectorShiftLeftUnsigned)
    {
        int laneBytes = Ir::LaneSizeInBytes(instruction.laneWidth);
        if (laneBytes != 1 && laneBytes != 2 && laneBytes != 4 && laneBytes != 8) return false;

        int totalBytes = laneBytes * instruction.laneCount;
        if (totalBytes != 8 && totalBytes != 16) return false;
        if (!right) return false;

        bool arithmetic = instruction.opcode == Ir::Opcode::VectorShiftLeftSigned;
        int laneBits = laneBytes * 8;

        llvm::Type* laneType = llvm::Type::getIntNTy(lowering.context, (unsigned)laneBytes * 8);
        llvm::Value* lanes = PackVectorLanes(lowering, lowLeft, highLeft, laneType, instruction.laneCount, totalBytes);
        llvm::Value* amounts = PackVectorLanes(lowering, lowRight, highRight, laneType, instruction.laneCount, totalBytes);

        llvm::Type* byteLanes = llvm::VectorType::get(llvm::Type::getInt8Ty(lowering.context), (unsigned)instruction.laneCount, false);
        llvm::Value* amount = builder.CreateSExt(builder.CreateTrunc(amounts, byteLanes), lanes->getType());

        llvm::Value* zero = llvm::Constant::getNullValue(lanes->getType());
        llvm::Value* width = builder.CreateVectorSplat((unsigned)instruction.laneCount, llvm::ConstantInt::get(laneType, laneBits));
        llvm::Value* widthLessOne =
            builder.CreateVectorSplat((unsigned)instruction.laneCount, llvm::ConstantInt::get(laneType, laneBits - 1));

        llvm::Value* toLeft = builder.CreateICmpSGE(amount, zero);
        llvm::Value* negated = builder.CreateSub(zero, amount);

        llvm::Value* leftFits = builder.CreateICmpSLT(amount, width);
        llvm::Value* leftShifted = builder.CreateShl(lanes, builder.CreateSelect(leftFits, amount, zero));
        leftShifted = builder.CreateSelect(leftFits, leftShifted, zero);

        llvm::Value* rightTooFar = builder.CreateICmpSGE(negated, width);
        llvm::Value* rightShifted = nullptr;

        if (arithmetic)
            rightShifted = builder.CreateAShr(lanes, builder.CreateSelect(rightTooFar, widthLessOne, negated));
        else
        {
            rightShifted = builder.CreateLShr(lanes, builder.CreateSelect(rightTooFar, zero, negated));
            rightShifted = builder.CreateSelect(rightTooFar, zero, rightShifted);
        }

        UnpackVectorLanes(lowering, builder.CreateSelect(toLeft, leftShifted, rightShifted), totalBytes, produced, producedHigh);
        return true;
    }

    if (instruction.opcode == Ir::Opcode::VectorMinMax || instruction.opcode == Ir::Opcode::VectorAbsoluteDifference ||
        instruction.opcode == Ir::Opcode::VectorMultiply)
    {
        int laneBytes = Ir::LaneSizeInBytes(instruction.laneWidth);
        if (laneBytes != 1 && laneBytes != 2 && laneBytes != 4 && laneBytes != 8) return false;

        int totalBytes = laneBytes * instruction.laneCount;
        if (totalBytes != 8 && totalBytes != 16) return false;

        llvm::Type* laneType = llvm::Type::getIntNTy(lowering.context, (unsigned)laneBytes * 8);
        llvm::Value* lanesLeft = PackVectorLanes(lowering, lowLeft, highLeft, laneType, instruction.laneCount, totalBytes);
        llvm::Value* lanesRight = PackVectorLanes(lowering, lowRight, highRight, laneType, instruction.laneCount, totalBytes);

        llvm::Value* outcome = nullptr;

        if (instruction.opcode == Ir::Opcode::VectorMultiply) outcome = builder.CreateMul(lanesLeft, lanesRight);
        else
        {
            bool unsignedForm = (instruction.immediate & 1) != 0;
            bool minimum = (instruction.immediate & 2) != 0;

            if (instruction.opcode == Ir::Opcode::VectorAbsoluteDifference)
            {
                llvm::Intrinsic::ID low = unsignedForm ? llvm::Intrinsic::umin : llvm::Intrinsic::smin;
                llvm::Intrinsic::ID high = unsignedForm ? llvm::Intrinsic::umax : llvm::Intrinsic::smax;

                outcome = builder.CreateSub(builder.CreateBinaryIntrinsic(high, lanesLeft, lanesRight),
                                            builder.CreateBinaryIntrinsic(low, lanesLeft, lanesRight));
            }
            else
            {
                llvm::Intrinsic::ID chosen = minimum ? (unsignedForm ? llvm::Intrinsic::umin : llvm::Intrinsic::smin)
                                                     : (unsignedForm ? llvm::Intrinsic::umax : llvm::Intrinsic::smax);

                outcome = builder.CreateBinaryIntrinsic(chosen, lanesLeft, lanesRight);
            }
        }

        UnpackVectorLanes(lowering, outcome, totalBytes, produced, producedHigh);
        return true;
    }

    if (instruction.opcode == Ir::Opcode::VectorCompare)
    {
        int laneBytes = Ir::LaneSizeInBytes(instruction.laneWidth);
        if (laneBytes != 1 && laneBytes != 2 && laneBytes != 4 && laneBytes != 8) return false;

        int totalBytes = laneBytes * instruction.laneCount;
        if (totalBytes != 8 && totalBytes != 16) return false;

        uint64_t mode = instruction.immediate & 0x7;
        bool againstZero = (instruction.immediate & 0x8) != 0;
        bool unsignedForm = (instruction.immediate & 0x10) != 0;
        bool testBits = (instruction.immediate & 0x20) != 0;

        if (!againstZero && !right) return false;

        llvm::Type* laneType = llvm::Type::getIntNTy(lowering.context, (unsigned)laneBytes * 8);

        llvm::Value* lanesLeft = PackVectorLanes(lowering, lowLeft, highLeft, laneType, instruction.laneCount, totalBytes);
        llvm::Value* lanesRight = againstZero
                                      ? llvm::Constant::getNullValue(lanesLeft->getType())
                                      : PackVectorLanes(lowering, lowRight, highRight, laneType, instruction.laneCount, totalBytes);

        llvm::Value* outcome = nullptr;

        if (testBits)
            outcome = builder.CreateICmpNE(builder.CreateAnd(lanesLeft, lanesRight), llvm::Constant::getNullValue(lanesLeft->getType()));
        else
        {
            llvm::CmpInst::Predicate predicate = llvm::CmpInst::ICMP_EQ;

            if (mode == 0) predicate = llvm::CmpInst::ICMP_EQ;
            else if (mode == 1) predicate = unsignedForm ? llvm::CmpInst::ICMP_UGE : llvm::CmpInst::ICMP_SGE;
            else if (mode == 2) predicate = unsignedForm ? llvm::CmpInst::ICMP_UGT : llvm::CmpInst::ICMP_SGT;
            else if (mode == 3) predicate = unsignedForm ? llvm::CmpInst::ICMP_ULE : llvm::CmpInst::ICMP_SLE;
            else predicate = unsignedForm ? llvm::CmpInst::ICMP_ULT : llvm::CmpInst::ICMP_SLT;

            outcome = builder.CreateICmp(predicate, lanesLeft, lanesRight);
        }

        UnpackVectorLanes(lowering, builder.CreateSExt(outcome, lanesLeft->getType()), totalBytes, produced, producedHigh);
        return true;
    }

    bool saturating = instruction.opcode == Ir::Opcode::VectorSaturatingAddSigned ||
                      instruction.opcode == Ir::Opcode::VectorSaturatingAddUnsigned ||
                      instruction.opcode == Ir::Opcode::VectorSaturatingSubtractSigned ||
                      instruction.opcode == Ir::Opcode::VectorSaturatingSubtractUnsigned;

    if (!saturating && instruction.opcode != Ir::Opcode::VectorAdd && instruction.opcode != Ir::Opcode::VectorSubtract) return false;
    if (!right) return false;

    int laneBytes = Ir::LaneSizeInBytes(instruction.laneWidth);
    if (laneBytes != 1 && laneBytes != 2 && laneBytes != 4 && laneBytes != 8) return false;

    int totalBytes = laneBytes * instruction.laneCount;
    if (totalBytes != 8 && totalBytes != 16) return false;

    llvm::Type* laneType = llvm::Type::getIntNTy(lowering.context, (unsigned)laneBytes * 8);
    llvm::Type* vectorType = llvm::VectorType::get(laneType, (unsigned)instruction.laneCount, false);
    llvm::Type* packedType = llvm::Type::getIntNTy(lowering.context, (unsigned)totalBytes * 8);

    llvm::Value* packedLeft = nullptr;
    llvm::Value* packedRight = nullptr;

    if (totalBytes == 8)
    {
        packedLeft = lowLeft;
        packedRight = lowRight;
    }
    else
    {
        llvm::Value* wide = llvm::UndefValue::get(llvm::VectorType::get(wordType, 2, false));
        packedLeft = builder.CreateInsertElement(builder.CreateInsertElement(wide, lowLeft, builder.getInt32(0)), highLeft,
                                                 builder.getInt32(1));
        packedRight = builder.CreateInsertElement(builder.CreateInsertElement(wide, lowRight, builder.getInt32(0)), highRight,
                                                  builder.getInt32(1));

        packedLeft = builder.CreateBitCast(packedLeft, packedType);
        packedRight = builder.CreateBitCast(packedRight, packedType);
    }

    llvm::Value* lanesLeft = builder.CreateBitCast(packedLeft, vectorType);
    llvm::Value* lanesRight = builder.CreateBitCast(packedRight, vectorType);

    llvm::Value* outcome = nullptr;

    switch (instruction.opcode)
    {
        case Ir::Opcode::VectorAdd: outcome = builder.CreateAdd(lanesLeft, lanesRight); break;
        case Ir::Opcode::VectorSubtract: outcome = builder.CreateSub(lanesLeft, lanesRight); break;
        case Ir::Opcode::VectorSaturatingAddSigned:
            outcome = builder.CreateBinaryIntrinsic(llvm::Intrinsic::sadd_sat, lanesLeft, lanesRight);
            break;
        case Ir::Opcode::VectorSaturatingAddUnsigned:
            outcome = builder.CreateBinaryIntrinsic(llvm::Intrinsic::uadd_sat, lanesLeft, lanesRight);
            break;
        case Ir::Opcode::VectorSaturatingSubtractSigned:
            outcome = builder.CreateBinaryIntrinsic(llvm::Intrinsic::ssub_sat, lanesLeft, lanesRight);
            break;
        default: outcome = builder.CreateBinaryIntrinsic(llvm::Intrinsic::usub_sat, lanesLeft, lanesRight); break;
    }

    llvm::Value* packed = builder.CreateBitCast(outcome, packedType);

    if (totalBytes == 8)
    {
        produced = packed;
        producedHigh = builder.getInt64(0);
        return true;
    }

    llvm::Value* asWords = builder.CreateBitCast(packed, llvm::VectorType::get(wordType, 2, false));
    produced = builder.CreateExtractElement(asWords, builder.getInt32(0));
    producedHigh = builder.CreateExtractElement(asWords, builder.getInt32(1));

    return true;
}

static llvm::Value* EmitConditionHolds(Lowering& lowering, llvm::Value* flags, uint8_t condition)
{
    llvm::IRBuilder<>& builder = lowering.builder;

    llvm::Value* zeroWord = builder.getInt32(0);
    llvm::Value* negative = builder.CreateICmpNE(builder.CreateAnd(flags, builder.getInt32(GuestState::FLAG_NEGATIVE)), zeroWord);
    llvm::Value* zero = builder.CreateICmpNE(builder.CreateAnd(flags, builder.getInt32(GuestState::FLAG_ZERO)), zeroWord);
    llvm::Value* carry = builder.CreateICmpNE(builder.CreateAnd(flags, builder.getInt32(GuestState::FLAG_CARRY)), zeroWord);
    llvm::Value* overflow = builder.CreateICmpNE(builder.CreateAnd(flags, builder.getInt32(GuestState::FLAG_OVERFLOW)), zeroWord);

    llvm::Value* result = nullptr;

    switch ((condition >> 1) & 0x7)
    {
        case 0: result = zero; break;
        case 1: result = carry; break;
        case 2: result = negative; break;
        case 3: result = overflow; break;
        case 4: result = builder.CreateAnd(carry, builder.CreateNot(zero)); break;
        case 5: result = builder.CreateICmpEQ(negative, overflow); break;
        case 6: result = builder.CreateAnd(builder.CreateICmpEQ(negative, overflow), builder.CreateNot(zero)); break;
        default: result = builder.getInt1(true); break;
    }

    if ((condition & 1) != 0 && (condition & 0xF) != 0xF) result = builder.CreateNot(result);

    return result;
}

static bool EmitAccessorLoad(Lowering& lowering, const Ir::Instruction& instruction, llvm::Value* address, int width, uint64_t steps,
                             llvm::Value*& produced, llvm::Value*& producedHigh)
{
    llvm::IRBuilder<>& builder = lowering.builder;
    llvm::Type* wordType = llvm::Type::getInt64Ty(lowering.context);
    llvm::Type* pointerType = llvm::PointerType::getUnqual(lowering.context);

    builder.CreateStore(builder.getInt64(0), lowering.scratch);
    builder.CreateStore(builder.getInt64(0), FieldPointer(lowering, lowering.scratch, sizeof(uint64_t)));

    llvm::FunctionCallee helper =
        HelperCallee(lowering, "AclMemoryRead", builder.getInt1Ty(), {pointerType, wordType, pointerType, wordType});
    llvm::Value* ok = builder.CreateCall(helper, {lowering.runtime, address, lowering.scratch, builder.getInt64((uint64_t)width)});

    llvm::BasicBlock* faulted = llvm::BasicBlock::Create(lowering.context, "load.fault", lowering.function);
    llvm::BasicBlock* continued = llvm::BasicBlock::Create(lowering.context, "load.ok", lowering.function);
    builder.CreateCondBr(ok, continued, faulted);

    builder.SetInsertPoint(faulted);
    EmitExit(lowering, Interpreter::ExitReason::MemoryFault, nullptr, address, steps);

    builder.SetInsertPoint(continued);
    produced = builder.CreateLoad(wordType, lowering.scratch);

    if (IsWideType(instruction.operandType))
        producedHigh = builder.CreateLoad(wordType, FieldPointer(lowering, lowering.scratch, sizeof(uint64_t)));

    if (OrderIsAcquire(instruction.order)) builder.CreateFence(llvm::AtomicOrdering::Acquire);

    return true;
}

static void EmitAccessorStore(Lowering& lowering, const Ir::Instruction& instruction, llvm::Value* address, llvm::Value* value,
                              llvm::Value* high, int width, uint64_t steps)
{
    llvm::IRBuilder<>& builder = lowering.builder;
    llvm::Type* wordType = llvm::Type::getInt64Ty(lowering.context);
    llvm::Type* pointerType = llvm::PointerType::getUnqual(lowering.context);

    llvm::Value* storedHigh = IsWideType(instruction.operandType) && high ? high : builder.getInt64(0);

    builder.CreateStore(value, lowering.scratch);
    builder.CreateStore(storedHigh, FieldPointer(lowering, lowering.scratch, sizeof(uint64_t)));

    llvm::FunctionCallee helper =
        HelperCallee(lowering, "AclMemoryWrite", builder.getInt1Ty(), {pointerType, wordType, pointerType, wordType});
    llvm::Value* ok = builder.CreateCall(helper, {lowering.runtime, address, lowering.scratch, builder.getInt64((uint64_t)width)});

    llvm::BasicBlock* faulted = llvm::BasicBlock::Create(lowering.context, "store.fault", lowering.function);
    llvm::BasicBlock* continued = llvm::BasicBlock::Create(lowering.context, "store.ok", lowering.function);
    builder.CreateCondBr(ok, continued, faulted);

    builder.SetInsertPoint(faulted);
    EmitExit(lowering, Interpreter::ExitReason::MemoryFault, nullptr, address, steps);

    builder.SetInsertPoint(continued);

    if (OrderNeedsStoreLoadFence(instruction.order)) builder.CreateFence(llvm::AtomicOrdering::SequentiallyConsistent);
}

static bool EmitDirectCall(Lowering& lowering, const Ir::Instruction& instruction)
{
    if (!lowering.moduleFunctions) return false;
    if (lowering.moduleFunctions->count(instruction.immediate) == 0) return false;

    llvm::IRBuilder<>& builder = lowering.builder;
    llvm::Type* wordType = llvm::Type::getInt64Ty(lowering.context);
    llvm::Type* pointerType = llvm::PointerType::getUnqual(lowering.context);
    llvm::Type* int32Type = llvm::Type::getInt32Ty(lowering.context);

    llvm::FunctionType* entryType = llvm::FunctionType::get(builder.getVoidTy(), {pointerType, pointerType, pointerType, int32Type}, false);
    llvm::FunctionCallee callee =
        lowering.module.getOrInsertFunction(LlvmBackend::FunctionSymbol((GuestState::GuestAddress)instruction.immediate), entryType);

    uint64_t resumeAddress = (uint64_t)instruction.guestAddress + 4;

    StoreSlotsBack(lowering);
    builder.CreateCall(callee, {lowering.registers, lowering.runtime, lowering.status, builder.getInt32(0)});
    ReloadSlots(lowering);

    llvm::Value* calleeSteps =
        builder.CreateLoad(wordType, FieldPointer(lowering, lowering.status, offsetof(Interpreter::ExitStatus, stepsExecuted)));
    llvm::Value* ourSteps = builder.CreateLoad(wordType, lowering.stepsSlot);
    builder.CreateStore(builder.CreateAdd(ourSteps, calleeSteps), lowering.stepsSlot);

    llvm::Value* reason =
        builder.CreateLoad(builder.getInt8Ty(), FieldPointer(lowering, lowering.status, offsetof(Interpreter::ExitStatus, reason)));
    llvm::Value* hasTarget =
        builder.CreateLoad(builder.getInt8Ty(), FieldPointer(lowering, lowering.status, offsetof(Interpreter::ExitStatus, hasTargetAddress)));
    llvm::Value* target =
        builder.CreateLoad(wordType, FieldPointer(lowering, lowering.status, offsetof(Interpreter::ExitStatus, targetAddress)));

    llvm::Value* returned = builder.CreateICmpEQ(reason, builder.getInt8((uint8_t)Interpreter::ExitReason::Returned));
    llvm::Value* flagged = builder.CreateICmpNE(hasTarget, builder.getInt8(0));
    llvm::Value* matched = builder.CreateICmpEQ(target, builder.getInt64(resumeAddress));

    llvm::Value* resumable = builder.CreateAnd(builder.CreateAnd(returned, flagged), matched);

    llvm::BasicBlock* resume = llvm::BasicBlock::Create(lowering.context, "call.resume", lowering.function);
    llvm::BasicBlock* propagate = llvm::BasicBlock::Create(lowering.context, "call.propagate", lowering.function);
    builder.CreateCondBr(resumable, resume, propagate);

    builder.SetInsertPoint(propagate);
    builder.CreateRetVoid();

    builder.SetInsertPoint(resume);
    builder.CreateStore(builder.getInt8(0), FieldPointer(lowering, lowering.status, offsetof(Interpreter::ExitStatus, reason)));
    builder.CreateStore(builder.getInt8(0), FieldPointer(lowering, lowering.status, offsetof(Interpreter::ExitStatus, hasTargetAddress)));
    builder.CreateStore(builder.getInt8(0), FieldPointer(lowering, lowering.status, offsetof(Interpreter::ExitStatus, hasFaultAddress)));

    lowering.directCalls += 1;
    return true;
}

static llvm::BasicBlock* TargetBlock(Lowering& lowering, uint64_t address, uint64_t steps)
{
    auto found = lowering.blockByAddress.find(address);
    if (found != lowering.blockByAddress.end()) return found->second;

    llvm::BasicBlock* leaving = llvm::BasicBlock::Create(lowering.context, "leave", lowering.function);

    llvm::IRBuilderBase::InsertPoint saved = lowering.builder.saveIP();
    lowering.builder.SetInsertPoint(leaving);
    EmitExit(lowering, Interpreter::ExitReason::IndirectBranch, lowering.builder.getInt64(address), nullptr, steps);
    lowering.builder.restoreIP(saved);

    return leaving;
}

static bool OrderNeedsStoreLoadFence(Ir::MemoryOrder order)
{
    return order == Ir::MemoryOrder::Release || order == Ir::MemoryOrder::AcquireRelease ||
           order == Ir::MemoryOrder::SequentiallyConsistent;
}

static bool OrderIsAcquire(Ir::MemoryOrder order)
{
    return order == Ir::MemoryOrder::Acquire || order == Ir::MemoryOrder::AcquireReadOnly ||
           order == Ir::MemoryOrder::AcquireRelease || order == Ir::MemoryOrder::SequentiallyConsistent;
}

static llvm::FunctionCallee HelperCallee(Lowering& lowering, const char* name, llvm::Type* returnType, llvm::ArrayRef<llvm::Type*> arguments)
{
    llvm::FunctionType* type = llvm::FunctionType::get(returnType, arguments, false);
    return lowering.module.getOrInsertFunction(name, type);
}

static llvm::Value* LowerBinary(Lowering& lowering, const Ir::Instruction& instruction, llvm::Value* left, llvm::Value* right)
{
    llvm::IRBuilder<>& builder = lowering.builder;
    int bits = IrSemantics::BitsInType(instruction.operandType);
    if (bits <= 0) bits = 64;

    switch (instruction.opcode)
    {
        case Ir::Opcode::Add: return builder.CreateAdd(left, right);
        case Ir::Opcode::Subtract: return builder.CreateSub(left, right);
        case Ir::Opcode::Multiply: return builder.CreateMul(left, right);
        case Ir::Opcode::And: return builder.CreateAnd(left, right);
        case Ir::Opcode::Or: return builder.CreateOr(left, right);
        case Ir::Opcode::Xor: return builder.CreateXor(left, right);
        default: break;
    }

    llvm::Value* amount = builder.CreateURem(right, builder.getInt64((uint64_t)bits));

    if (instruction.opcode == Ir::Opcode::ShiftLeft)
    {
        llvm::Value* shifted = builder.CreateShl(left, amount);
        llvm::Value* zero = builder.CreateICmpEQ(amount, builder.getInt64(0));
        return builder.CreateSelect(zero, left, shifted);
    }

    if (instruction.opcode == Ir::Opcode::ShiftRightLogical)
    {
        llvm::Value* masked = MaskToType(lowering, left, instruction.operandType);
        return builder.CreateLShr(masked, amount);
    }

    if (instruction.opcode == Ir::Opcode::ShiftRightArithmetic)
    {
        llvm::Value* extended = SignExtendTo64(lowering, left, bits);
        return builder.CreateAShr(extended, amount);
    }

    if (instruction.opcode == Ir::Opcode::RotateRight)
    {
        llvm::Value* masked = MaskToType(lowering, left, instruction.operandType);
        llvm::Value* right1 = builder.CreateLShr(masked, amount);
        llvm::Value* complement = builder.CreateSub(builder.getInt64((uint64_t)bits), amount);
        llvm::Value* left1 = builder.CreateShl(masked, builder.CreateURem(complement, builder.getInt64((uint64_t)bits)));
        llvm::Value* combined = builder.CreateOr(right1, left1);
        llvm::Value* zero = builder.CreateICmpEQ(amount, builder.getInt64(0));
        return builder.CreateSelect(zero, masked, combined);
    }

    return nullptr;
}

static bool LowerInstruction(Lowering& lowering, const Ir::Instruction& instruction, uint64_t stepsBefore, bool& terminated);

static void CollectSlots(Lowering& lowering, const Ir::Function& function)
{
    for (const Ir::Block& block : function.blocks)
    {
        for (const Ir::Instruction& instruction : block.instructions)
        {
            switch (instruction.opcode)
            {
                case Ir::Opcode::ReadGeneral:
                case Ir::Opcode::WriteGeneral:
                    RequireSlot(lowering, offsetof(GuestState::Registers, general) + instruction.immediate * sizeof(uint64_t), 8);
                    break;

                case Ir::Opcode::ReadStackPointer:
                case Ir::Opcode::WriteStackPointer:
                    RequireSlot(lowering, offsetof(GuestState::Registers, stackPointer), 8);
                    break;

                case Ir::Opcode::ReadFlags:
                case Ir::Opcode::WriteFlags:
                case Ir::Opcode::Select:
                case Ir::Opcode::BranchConditional:
                    RequireSlot(lowering, offsetof(GuestState::Registers, flagsNzcv), 4);
                    break;

                case Ir::Opcode::ReadVector:
                case Ir::Opcode::WriteVector:
                    RequireSlot(lowering,
                                offsetof(GuestState::Registers, vector) + instruction.immediate * sizeof(GuestState::VectorRegister), 16);
                    break;

                default: break;
            }

            const char* name = Ir::OpcodeName(instruction.opcode);
            if (name && (strncmp(name, "Float", 5) == 0 || strncmp(name, "Vector", 6) == 0)) lowering.usesFloat = true;
        }
    }
}

static void CreateSlotStorage(Lowering& lowering)
{
    llvm::IRBuilder<>& builder = lowering.builder;

    for (RegisterSlot& slot : lowering.slots)
    {
        llvm::Type* type = SlotType(lowering, slot.byteWidth);
        slot.storage = builder.CreateAlloca(type, nullptr, "slot");

        llvm::Value* loaded = builder.CreateAlignedLoad(type, FieldPointer(lowering, lowering.registers, slot.offset),
                                                        llvm::MaybeAlign(alignof(GuestState::VectorRegister)));
        builder.CreateStore(loaded, slot.storage);
    }
}

static bool LowerBlockBody(Lowering& lowering, const Ir::Block& block)
{
    uint64_t steps = 0;
    bool terminated = false;

    lowering.fallthroughTarget = (uint64_t)block.successorFallthrough;

    if (lowering.stepsSlot && !block.instructions.empty())
    {
        llvm::Value* current = lowering.builder.CreateLoad(lowering.builder.getInt64Ty(), lowering.stepsSlot);
        llvm::Value* bumped = lowering.builder.CreateAdd(current, lowering.builder.getInt64(block.instructions.size()));
        lowering.builder.CreateStore(bumped, lowering.stepsSlot);
    }

    for (const Ir::Instruction& instruction : block.instructions)
    {
        steps += 1;
        if (!LowerInstruction(lowering, instruction, steps, terminated)) return false;
        if (terminated) return true;
    }

    if (block.successorFallthrough != Ir::NO_SUCCESSOR)
    {
        lowering.builder.CreateBr(TargetBlock(lowering, (uint64_t)block.successorFallthrough, steps));
        return true;
    }

    EmitExit(lowering, Interpreter::ExitReason::BlockExhausted, lowering.builder.getInt64((uint64_t)block.guestEndAddress), nullptr, steps);
    return true;
}

static bool LowerFunction(Lowering& lowering, const Ir::Function& function)
{
    if (function.blocks.empty()) return false;

    llvm::IRBuilder<>& builder = lowering.builder;

    for (const Ir::Block& block : function.blocks)
    {
        uint64_t address = (uint64_t)block.guestAddress;
        if (lowering.blockByAddress.count(address) != 0) return false;

        lowering.blockByAddress.emplace(address, llvm::BasicBlock::Create(lowering.context, "guest", lowering.function));
    }

    CollectSlots(lowering, function);

    if (lowering.usesFloat)
    {
        lowering.function->addFnAttr(llvm::Attribute::StrictFP);
        builder.setIsFPConstrained(true);
        builder.setDefaultConstrainedExcept(llvm::fp::ebStrict);
        builder.setDefaultConstrainedRounding(llvm::RoundingMode::NearestTiesToEven);
    }

    builder.SetInsertPoint(lowering.entryBlock);
    lowering.scratch = builder.CreateAlloca(builder.getInt64Ty(), builder.getInt64(2), "scratch");
    lowering.vectorScratch = builder.CreateAlloca(builder.getInt64Ty(), builder.getInt64(8), "vectors");
    lowering.floatModeScratch = builder.CreateAlloca(builder.getInt32Ty(), builder.getInt64(2), "floatmode");

    if (lowering.usesFloat)
        EmitHostFloatMode(lowering, builder.CreateAnd(EmitHostFloatFlags(lowering, 0), builder.getInt32(~HOST_FLOAT_EXCEPTIONS)));
    lowering.stepsSlot = builder.CreateAlloca(builder.getInt64Ty(), nullptr, "steps");
    builder.CreateStore(builder.getInt64(0), lowering.stepsSlot);
    CreateSlotStorage(lowering);

    llvm::BasicBlock* firstBlock = lowering.blockByAddress[(uint64_t)function.blocks.front().guestAddress];
    llvm::SwitchInst* entrySwitch = builder.CreateSwitch(lowering.function->getArg(3), firstBlock, (unsigned)function.blocks.size());

    for (uint64_t index = 0; index < function.blocks.size(); ++index)
    {
        if (index != 0 && !function.blocks[index].externalEntry) continue;
        entrySwitch->addCase(builder.getInt32((uint32_t)index), lowering.blockByAddress[(uint64_t)function.blocks[index].guestAddress]);
    }

    for (const Ir::Block& block : function.blocks)
    {
        builder.SetInsertPoint(lowering.blockByAddress[(uint64_t)block.guestAddress]);

        lowering.values.clear();
        lowering.values.resize(function.nextValueId);

        EmitBlockTrace(lowering, (uint64_t)block.guestAddress);

        if (!LowerBlockBody(lowering, block)) return false;
    }

    return true;
}

static bool LowerInstruction(Lowering& lowering, const Ir::Instruction& instruction, uint64_t steps, bool& terminated)
{
    llvm::IRBuilder<>& builder = lowering.builder;
    llvm::Type* wordType = llvm::Type::getInt64Ty(lowering.context);
    llvm::Type* pointerType = llvm::PointerType::getUnqual(lowering.context);

    lowering.instructionsLowered += 1;

    LoweredValue operands[Ir::MAX_OPERANDS];
    for (uint8_t index = 0; index < instruction.operandCount; ++index)
    {
        uint32_t id = instruction.operands[index].id;
        if (id >= lowering.values.size() || !lowering.values[id].low) return false;
        operands[index] = lowering.values[id];
    }

    llvm::Value* left = instruction.operandCount > 0 ? operands[0].low : nullptr;
    llvm::Value* right = instruction.operandCount > 1 ? operands[1].low : nullptr;
    llvm::Value* third = instruction.operandCount > 2 ? operands[2].low : nullptr;

    llvm::Value* produced = nullptr;
    llvm::Value* producedHigh = nullptr;

    switch (instruction.opcode)
    {
        case Ir::Opcode::ReadVector:
        {
            uint64_t base = offsetof(GuestState::Registers, vector) + instruction.immediate * sizeof(GuestState::VectorRegister);

            llvm::Value* low = nullptr;
            llvm::Value* high = nullptr;
            ReadVectorSlot(lowering, base, low, high);

            produced = low;

            if (IsWideType(instruction.operandType))
                producedHigh = high;
            else
                produced = MaskToType(lowering, produced, instruction.operandType);

            break;
        }

        case Ir::Opcode::WriteVector:
        {
            uint64_t base = offsetof(GuestState::Registers, vector) + instruction.immediate * sizeof(GuestState::VectorRegister);
            llvm::Value* high = IsWideType(instruction.operandType) && operands[0].high ? operands[0].high : builder.getInt64(0);

            WriteVectorSlot(lowering, base, left, high);
            break;
        }

        case Ir::Opcode::VectorSaturatingAddSigned:
        case Ir::Opcode::VectorSaturatingAddUnsigned:
        case Ir::Opcode::VectorSaturatingSubtractSigned:
        case Ir::Opcode::VectorSaturatingSubtractUnsigned:
        case Ir::Opcode::VectorShiftLeftSigned:
        case Ir::Opcode::VectorShiftLeftUnsigned:
        case Ir::Opcode::VectorAdd:
        case Ir::Opcode::VectorSubtract:
        case Ir::Opcode::VectorMultiply:
        case Ir::Opcode::VectorAnd:
        case Ir::Opcode::VectorOr:
        case Ir::Opcode::VectorXor:
        case Ir::Opcode::VectorDuplicate:
        case Ir::Opcode::VectorExtractLane:
        case Ir::Opcode::VectorExtract:
        case Ir::Opcode::VectorInsertLane:
        case Ir::Opcode::VectorReduceAdd:
        case Ir::Opcode::VectorReduceAddLong:
        case Ir::Opcode::VectorFloatCompare:
        case Ir::Opcode::VectorFloatMinimum:
        case Ir::Opcode::VectorFloatMaximum:
        case Ir::Opcode::VectorFloatPairwiseAdd:
        case Ir::Opcode::VectorFloatEstimate:
        case Ir::Opcode::VectorNarrow:
        case Ir::Opcode::VectorPermute:
        case Ir::Opcode::VectorMinMax:
        case Ir::Opcode::VectorPairwise:
        case Ir::Opcode::VectorAbsoluteDifference:
        case Ir::Opcode::VectorAbsolute:
        case Ir::Opcode::VectorCountLeadingZeros:
        case Ir::Opcode::VectorPairwiseAddLong:
        case Ir::Opcode::VectorFloatConvertWidth:
        case Ir::Opcode::VectorTableLookup:
        case Ir::Opcode::VectorReduceMinMax:
        case Ir::Opcode::VectorCompare:
        case Ir::Opcode::VectorReverseBytes:
        case Ir::Opcode::VectorFloatAdd:
        case Ir::Opcode::VectorFloatSubtract:
        case Ir::Opcode::VectorFloatMultiply:
        case Ir::Opcode::VectorFloatMultiplyAdd:
        case Ir::Opcode::VectorFloatRound:
        case Ir::Opcode::VectorFloatDivide:
        case Ir::Opcode::VectorFloatSquareRoot:
        case Ir::Opcode::VectorFloatNegate:
        case Ir::Opcode::VectorFloatAbsolute:
        case Ir::Opcode::VectorConvertFloat:
        case Ir::Opcode::VectorCountBits:
        case Ir::Opcode::VectorWiden:
        {
            llvm::Value* rightHigh = instruction.operandCount > 1 ? operands[1].high : nullptr;

            if (EmitNativeVectorInteger(lowering, instruction, left, operands[0].high, right, rightHigh, produced, producedHigh)) break;
            llvm::Value* thirdLow = instruction.operandCount > 2 ? operands[2].low : nullptr;
            llvm::Value* thirdHigh = instruction.operandCount > 2 ? operands[2].high : nullptr;

            if (EmitNativeVectorFloat(lowering, instruction, left, operands[0].high, right, rightHigh, thirdLow, thirdHigh, produced,
                                      producedHigh))
                break;

            uint64_t descriptor = (uint64_t)(uint16_t)instruction.opcode | ((uint64_t)(uint8_t)instruction.laneWidth << 16) |
                                  ((uint64_t)instruction.laneCount << 24) | ((uint64_t)instruction.laneIndex << 32);

            llvm::Value* leftSlot = lowering.vectorScratch;
            llvm::Value* rightSlot = FieldPointer(lowering, lowering.vectorScratch, 2 * sizeof(uint64_t));
            llvm::Value* resultSlot = FieldPointer(lowering, lowering.vectorScratch, 4 * sizeof(uint64_t));
            llvm::Value* thirdSlot = FieldPointer(lowering, lowering.vectorScratch, 6 * sizeof(uint64_t));

            builder.CreateStore(left ? left : builder.getInt64(0), leftSlot);
            builder.CreateStore(operands[0].high ? operands[0].high : builder.getInt64(0),
                                FieldPointer(lowering, lowering.vectorScratch, sizeof(uint64_t)));
            builder.CreateStore(right ? right : builder.getInt64(0), rightSlot);
            builder.CreateStore(operands[1].high ? operands[1].high : builder.getInt64(0),
                                FieldPointer(lowering, lowering.vectorScratch, 3 * sizeof(uint64_t)));
            builder.CreateStore(third ? third : builder.getInt64(0), thirdSlot);
            builder.CreateStore(operands[2].high ? operands[2].high : builder.getInt64(0),
                                FieldPointer(lowering, lowering.vectorScratch, 7 * sizeof(uint64_t)));

            llvm::Value* vectorStatus =
                FieldPointer(lowering, lowering.registers, offsetof(GuestState::Registers, floatingPointStatus));

            llvm::FunctionCallee helper =
                HelperCallee(lowering, "AclVectorOperation", builder.getVoidTy(),
                             {wordType, wordType, pointerType, pointerType, pointerType, pointerType, pointerType});
            builder.CreateCall(helper, {builder.getInt64(descriptor), builder.getInt64(instruction.immediate), leftSlot, rightSlot,
                                        thirdSlot, resultSlot, vectorStatus});

            produced = builder.CreateLoad(wordType, resultSlot);
            producedHigh = builder.CreateLoad(wordType, FieldPointer(lowering, lowering.vectorScratch, 5 * sizeof(uint64_t)));
            break;
        }

        case Ir::Opcode::ClearExclusive:
        {
            llvm::Value* monitor = FieldPointer(lowering, lowering.registers, offsetof(GuestState::Registers, exclusive));
            builder.CreateMemSet(monitor, builder.getInt8(0), builder.getInt64(sizeof(GuestState::ExclusiveMonitor)),
                                 llvm::MaybeAlign(alignof(GuestState::ExclusiveMonitor)));
            break;
        }

        case Ir::Opcode::ReadExclusiveHigh:
        {
            llvm::Value* high = FieldPointer(lowering, lowering.registers,
                                             offsetof(GuestState::Registers, exclusive) + offsetof(GuestState::ExclusiveMonitor, valueHigh));
            produced = builder.CreateLoad(wordType, high);
            break;
        }

        case Ir::Opcode::SetExclusiveExpected:
        {
            llvm::Value* low = FieldPointer(lowering, lowering.registers,
                                            offsetof(GuestState::Registers, exclusive) + offsetof(GuestState::ExclusiveMonitor, valueLow));
            llvm::Value* high = FieldPointer(lowering, lowering.registers,
                                             offsetof(GuestState::Registers, exclusive) + offsetof(GuestState::ExclusiveMonitor, valueHigh));

            builder.CreateStore(left ? left : builder.getInt64(0), low);
            builder.CreateStore(right ? right : builder.getInt64(0), high);
            break;
        }

        case Ir::Opcode::LoadExclusive:
        case Ir::Opcode::StoreExclusive:
        case Ir::Opcode::LoadExclusivePair:
        case Ir::Opcode::StoreExclusivePair:
        case Ir::Opcode::AtomicCompareExchangePair:
        case Ir::Opcode::AtomicCompareExchange:
        case Ir::Opcode::AtomicReadModifyWrite:
        {
            if (EmitNativeAtomic(lowering, instruction, left, right, third, produced)) break;

            uint64_t descriptor = (uint64_t)(uint16_t)instruction.opcode | ((uint64_t)(uint8_t)instruction.operandType << 16) |
                                  ((uint64_t)(uint8_t)instruction.atomicOperation << 24);

            llvm::FunctionCallee helper =
                HelperCallee(lowering, "AclAtomicOperation", builder.getInt32Ty(),
                             {wordType, pointerType, pointerType, wordType, wordType, wordType, pointerType});

            llvm::Value* outcome = builder.CreateCall(
                helper, {builder.getInt64(descriptor), lowering.runtime, lowering.registers, left ? left : builder.getInt64(0),
                         right ? right : builder.getInt64(0), third ? third : builder.getInt64(0), lowering.scratch});

            llvm::BasicBlock* faulted = llvm::BasicBlock::Create(lowering.context, "atomic.fault", lowering.function);
            llvm::BasicBlock* undefined = llvm::BasicBlock::Create(lowering.context, "atomic.undefined", lowering.function);
            llvm::BasicBlock* continued = llvm::BasicBlock::Create(lowering.context, "atomic.ok", lowering.function);

            llvm::SwitchInst* branch = builder.CreateSwitch(outcome, undefined, 2);
            branch->addCase(builder.getInt32((uint32_t)IrSemantics::AtomicOutcome::Ok), continued);
            branch->addCase(builder.getInt32((uint32_t)IrSemantics::AtomicOutcome::MemoryFault), faulted);

            builder.SetInsertPoint(faulted);
            EmitExit(lowering, Interpreter::ExitReason::MemoryFault, nullptr, left ? left : builder.getInt64(0), steps);

            builder.SetInsertPoint(undefined);
            EmitExit(lowering, Interpreter::ExitReason::Undefined, nullptr, builder.getInt64((uint64_t)instruction.guestAddress), steps);

            builder.SetInsertPoint(continued);
            produced = builder.CreateLoad(wordType, lowering.scratch);
            break;
        }

        case Ir::Opcode::FloatAdd:
        case Ir::Opcode::FloatSubtract:
        case Ir::Opcode::FloatMultiply:
        case Ir::Opcode::FloatDivide:
        case Ir::Opcode::FloatCompare:
        case Ir::Opcode::FloatCompareSignalling:
        case Ir::Opcode::FloatConvert:
        case Ir::Opcode::FloatSquareRoot:
        case Ir::Opcode::FloatMultiplyAdd:
        case Ir::Opcode::FloatMinimum:
        case Ir::Opcode::FloatMaximum:
        case Ir::Opcode::FloatRound:
        case Ir::Opcode::FloatConvertRounded:
        {
            if (EmitNativeFloatArithmetic(lowering, instruction, left, right, instruction.operandCount > 2 ? operands[2].low : nullptr,
                                          produced))
                break;

            uint64_t descriptor = (uint64_t)(uint16_t)instruction.opcode | ((uint64_t)(uint8_t)instruction.operandType << 16) |
                                  ((uint64_t)(uint8_t)instruction.result.type << 24) |
                                  ((uint64_t)(uint8_t)instruction.operands[0].type << 32);

            llvm::Value* statusPointer =
                FieldPointer(lowering, lowering.registers, offsetof(GuestState::Registers, floatingPointStatus));

            llvm::Value* addend = instruction.operandCount > 2 ? operands[2].low : nullptr;

            llvm::FunctionCallee helper = HelperCallee(lowering, "AclFloatOperation", wordType,
                                                       {wordType, wordType, wordType, wordType, wordType, pointerType});

            produced = builder.CreateCall(helper, {builder.getInt64(descriptor), builder.getInt64(instruction.immediate),
                                                   left ? left : builder.getInt64(0), right ? right : builder.getInt64(0),
                                                   addend ? addend : builder.getInt64(0), statusPointer});
            break;
        }

        case Ir::Opcode::ReadSystemRegister:
        {
            if (instruction.systemRegister == Ir::SystemRegister::FloatingPointStatus) EmitFloatStatusCapture(lowering);

            uint64_t constantValue = 0;
            if (Ir::DescribeSystemRegister(instruction.systemRegister, constantValue))
            {
                produced = builder.getInt64(constantValue);
                break;
            }

            if (instruction.systemRegister == Ir::SystemRegister::CounterVirtual)
            {
                produced = builder.CreateCall(HelperCallee(lowering, "AclReadVirtualCounter", wordType, {}));
                break;
            }

            if (instruction.systemRegister == Ir::SystemRegister::ThreadPointer ||
                instruction.systemRegister == Ir::SystemRegister::ThreadPointerReadOnly)
            {
                produced = builder.CreateLoad(wordType, FieldPointer(lowering, lowering.registers,
                                                                     offsetof(GuestState::Registers, threadPointer)));
                break;
            }

            uint64_t offset = instruction.systemRegister == Ir::SystemRegister::FloatingPointControl
                                  ? offsetof(GuestState::Registers, floatingPointControl)
                                  : offsetof(GuestState::Registers, floatingPointStatus);

            produced = builder.CreateZExt(builder.CreateLoad(builder.getInt32Ty(), FieldPointer(lowering, lowering.registers, offset)),
                                          wordType);
            break;
        }

        case Ir::Opcode::WriteSystemRegister:
        {
            if (instruction.systemRegister == Ir::SystemRegister::ThreadPointer)
            {
                builder.CreateStore(left, FieldPointer(lowering, lowering.registers, offsetof(GuestState::Registers, threadPointer)));
                break;
            }

            if (instruction.systemRegister != Ir::SystemRegister::FloatingPointControl &&
                instruction.systemRegister != Ir::SystemRegister::FloatingPointStatus)
                break;

            uint64_t offset = instruction.systemRegister == Ir::SystemRegister::FloatingPointControl
                                  ? offsetof(GuestState::Registers, floatingPointControl)
                                  : offsetof(GuestState::Registers, floatingPointStatus);

            builder.CreateStore(builder.CreateTrunc(left, builder.getInt32Ty()), FieldPointer(lowering, lowering.registers, offset));
            break;
        }

        case Ir::Opcode::Hint: produced = builder.getInt64(0); break;

        case Ir::Opcode::PointerAuthSign:
        case Ir::Opcode::PointerAuthAuthenticate:
        case Ir::Opcode::PointerAuthStrip: produced = left; break;

        case Ir::Opcode::Constant: produced = builder.getInt64(instruction.immediate); break;

        case Ir::Opcode::ReadGeneral:
            produced = ReadRegisterSlot(lowering, offsetof(GuestState::Registers, general) + instruction.immediate * sizeof(uint64_t));
            break;

        case Ir::Opcode::WriteGeneral:
            WriteRegisterSlot(lowering, offsetof(GuestState::Registers, general) + instruction.immediate * sizeof(uint64_t),
                              MaskToType(lowering, left, instruction.operandType));
            break;

        case Ir::Opcode::ReadStackPointer: produced = ReadRegisterSlot(lowering, offsetof(GuestState::Registers, stackPointer)); break;
        case Ir::Opcode::WriteStackPointer: WriteRegisterSlot(lowering, offsetof(GuestState::Registers, stackPointer), left); break;

        case Ir::Opcode::ReadFlags: produced = ReadRegisterSlot(lowering, offsetof(GuestState::Registers, flagsNzcv)); break;
        case Ir::Opcode::WriteFlags:
            WriteRegisterSlot(lowering, offsetof(GuestState::Registers, flagsNzcv),
                              builder.CreateAnd(left, builder.getInt64(0xf0000000ull)));
            break;

        case Ir::Opcode::Add:
        case Ir::Opcode::Subtract:
        case Ir::Opcode::Multiply:
        case Ir::Opcode::And:
        case Ir::Opcode::Or:
        case Ir::Opcode::Xor:
        case Ir::Opcode::ShiftLeft:
        case Ir::Opcode::ShiftRightLogical:
        case Ir::Opcode::ShiftRightArithmetic:
        case Ir::Opcode::RotateRight: produced = LowerBinary(lowering, instruction, left, right); break;

        case Ir::Opcode::MultiplyHighSigned:
        case Ir::Opcode::MultiplyHighUnsigned:
        {
            llvm::Type* wideType = llvm::Type::getInt128Ty(lowering.context);
            bool isSigned = instruction.opcode == Ir::Opcode::MultiplyHighSigned;

            llvm::Value* wideLeft = isSigned ? builder.CreateSExt(left, wideType) : builder.CreateZExt(left, wideType);
            llvm::Value* wideRight = isSigned ? builder.CreateSExt(right, wideType) : builder.CreateZExt(right, wideType);

            llvm::Value* product = builder.CreateMul(wideLeft, wideRight);
            produced = builder.CreateTrunc(builder.CreateLShr(product, llvm::ConstantInt::get(wideType, 64)), wordType);
            break;
        }

        case Ir::Opcode::Not: produced = builder.CreateNot(left); break;

        case Ir::Opcode::Truncate: produced = MaskToType(lowering, left, instruction.operandType); break;

        case Ir::Opcode::ExtendSigned: produced = SignExtendTo64(lowering, left, (int)instruction.immediate); break;

        case Ir::Opcode::ExtendUnsigned:
        {
            int bits = (int)instruction.immediate;
            if (bits >= 64) produced = left;
            else if (bits <= 0) produced = builder.getInt64(0);
            else produced = builder.CreateAnd(left, builder.getInt64((1ull << bits) - 1));
            break;
        }

        case Ir::Opcode::CountLeadingZeros:
        {
            int bits = IrSemantics::BitsInType(instruction.operandType);
            llvm::Value* masked = MaskToType(lowering, left, instruction.operandType);
            llvm::Function* intrinsic = llvm::Intrinsic::getOrInsertDeclaration(&lowering.module, llvm::Intrinsic::ctlz, {wordType});
            llvm::Value* counted = builder.CreateCall(intrinsic, {masked, builder.getInt1(false)});
            produced = builder.CreateSub(counted, builder.getInt64((uint64_t)(64 - bits)));
            break;
        }

        case Ir::Opcode::ReverseBits:
        {
            int bits = IrSemantics::BitsInType(instruction.operandType);
            llvm::Type* narrow = llvm::Type::getIntNTy(lowering.context, (unsigned)bits);

            llvm::Value* value = bits == 64 ? left : builder.CreateTrunc(left, narrow);
            llvm::Value* reversed = builder.CreateUnaryIntrinsic(llvm::Intrinsic::bitreverse, value);

            produced = bits == 64 ? reversed : builder.CreateZExt(reversed, wordType);
            break;
        }

        case Ir::Opcode::ReverseBytes:
        {
            int bits = IrSemantics::BitsInType(instruction.operandType);
            int laneBytes = Ir::LaneSizeInBytes(instruction.laneWidth);
            if (laneBytes == 0) laneBytes = bits / 8;

            if (laneBytes == 1)
            {
                produced = left;
                break;
            }

            llvm::Type* laneType = llvm::Type::getIntNTy(lowering.context, (unsigned)laneBytes * 8);
            int laneCount = (bits / 8) / laneBytes;

            llvm::Value* gathered = builder.getInt64(0);

            for (int lane = 0; lane < laneCount; ++lane)
            {
                llvm::Value* shifted = builder.CreateLShr(left, builder.getInt64((uint64_t)lane * laneBytes * 8));
                llvm::Value* swapped = builder.CreateUnaryIntrinsic(llvm::Intrinsic::bswap, builder.CreateTrunc(shifted, laneType));
                llvm::Value* widened = builder.CreateZExt(swapped, wordType);

                gathered = builder.CreateOr(gathered, builder.CreateShl(widened, builder.getInt64((uint64_t)lane * laneBytes * 8)));
            }

            produced = gathered;
            break;
        }

        case Ir::Opcode::ComputeNzcvAdd:
        case Ir::Opcode::ComputeNzcvSubtract:
        {
            llvm::Value* rightOperand = right;
            if (instruction.opcode == Ir::Opcode::ComputeNzcvSubtract) rightOperand = builder.CreateNot(right);

            llvm::Value* carry = third ? builder.CreateAnd(third, builder.getInt64(1)) : builder.getInt64(0);

            int bits = IrSemantics::BitsInType(instruction.operandType);
            uint64_t mask = bits >= 64 ? ~0ull : ((1ull << bits) - 1);

            llvm::Value* maskedLeft = builder.CreateAnd(left, builder.getInt64(mask));
            llvm::Value* maskedRight = builder.CreateAnd(rightOperand, builder.getInt64(mask));

            llvm::Value* total = builder.CreateAdd(builder.CreateAdd(maskedLeft, maskedRight), carry);
            llvm::Value* sum = builder.CreateAnd(total, builder.getInt64(mask));

            llvm::Value* carriedOut = nullptr;

            if (bits >= 64)
            {
                llvm::Value* first = builder.CreateBinaryIntrinsic(llvm::Intrinsic::uadd_with_overflow, maskedLeft, maskedRight);
                llvm::Value* second =
                    builder.CreateBinaryIntrinsic(llvm::Intrinsic::uadd_with_overflow, builder.CreateExtractValue(first, 0), carry);

                carriedOut = builder.CreateOr(builder.CreateExtractValue(first, 1), builder.CreateExtractValue(second, 1));
            }
            else
                carriedOut = builder.CreateICmpUGT(total, builder.getInt64(mask));

            llvm::Value* signBit = builder.getInt64(1ull << (bits - 1));
            llvm::Value* leftNegative = builder.CreateICmpNE(builder.CreateAnd(maskedLeft, signBit), builder.getInt64(0));
            llvm::Value* rightNegative = builder.CreateICmpNE(builder.CreateAnd(maskedRight, signBit), builder.getInt64(0));
            llvm::Value* sumNegative = builder.CreateICmpNE(builder.CreateAnd(sum, signBit), builder.getInt64(0));

            llvm::Value* overflowed = builder.CreateAnd(builder.CreateICmpEQ(leftNegative, rightNegative),
                                                        builder.CreateICmpNE(sumNegative, leftNegative));

            llvm::Value* flags = builder.CreateSelect(sumNegative, builder.getInt64(GuestState::FLAG_NEGATIVE), builder.getInt64(0));
            flags = builder.CreateOr(flags, builder.CreateSelect(builder.CreateICmpEQ(sum, builder.getInt64(0)),
                                                                 builder.getInt64(GuestState::FLAG_ZERO), builder.getInt64(0)));
            flags = builder.CreateOr(
                flags, builder.CreateSelect(carriedOut, builder.getInt64(GuestState::FLAG_CARRY), builder.getInt64(0)));
            flags = builder.CreateOr(
                flags, builder.CreateSelect(overflowed, builder.getInt64(GuestState::FLAG_OVERFLOW), builder.getInt64(0)));

            produced = flags;
            break;
        }

        case Ir::Opcode::ComputeNzcvLogical:
        {
            int bits = IrSemantics::BitsInType(instruction.operandType);
            llvm::Value* masked = MaskToType(lowering, left, instruction.operandType);
            llvm::Value* isZero = builder.CreateICmpEQ(masked, builder.getInt64(0));
            llvm::Value* signBit = builder.CreateAnd(masked, builder.getInt64(1ull << (bits - 1)));
            llvm::Value* isNegative = builder.CreateICmpNE(signBit, builder.getInt64(0));

            llvm::Value* flags = builder.getInt64(0);
            flags = builder.CreateOr(flags, builder.CreateSelect(isNegative, builder.getInt64(GuestState::FLAG_NEGATIVE),
                                                                 builder.getInt64(0)));
            flags = builder.CreateOr(flags, builder.CreateSelect(isZero, builder.getInt64(GuestState::FLAG_ZERO), builder.getInt64(0)));
            produced = flags;
            break;
        }

        case Ir::Opcode::AddWithCarry:
        case Ir::Opcode::SubtractWithBorrow:
        {
            llvm::Value* rightOperand = right;
            if (instruction.opcode == Ir::Opcode::SubtractWithBorrow) rightOperand = builder.CreateNot(right);

            llvm::Value* carryBit = builder.CreateAnd(third, builder.getInt64(GuestState::FLAG_CARRY));
            llvm::Value* carry = builder.CreateZExt(builder.CreateICmpNE(carryBit, builder.getInt64(0)), wordType);
            produced = builder.CreateAdd(builder.CreateAdd(left, rightOperand), carry);
            break;
        }

        case Ir::Opcode::DivideSigned:
        case Ir::Opcode::DivideUnsigned:
        {
            int bits = IrSemantics::BitsInType(instruction.operandType);
            llvm::Value* zero = builder.CreateICmpEQ(MaskToType(lowering, right, instruction.operandType), builder.getInt64(0));

            if (instruction.opcode == Ir::Opcode::DivideUnsigned)
            {
                llvm::Value* safeRight = builder.CreateSelect(zero, builder.getInt64(1), MaskToType(lowering, right, instruction.operandType));
                llvm::Value* quotient = builder.CreateUDiv(MaskToType(lowering, left, instruction.operandType), safeRight);
                produced = builder.CreateSelect(zero, builder.getInt64(0), quotient);
                break;
            }

            llvm::Value* dividend = SignExtendTo64(lowering, left, bits);
            llvm::Value* divisor = SignExtendTo64(lowering, right, bits);
            llvm::Value* minusOne = builder.CreateICmpEQ(divisor, builder.getInt64(~0ull));
            llvm::Value* safeDivisor = builder.CreateSelect(builder.CreateOr(zero, minusOne), builder.getInt64(1), divisor);
            llvm::Value* quotient = builder.CreateSDiv(dividend, safeDivisor);

            llvm::Value* negated = builder.CreateSub(builder.getInt64(0), dividend);
            produced = builder.CreateSelect(zero, builder.getInt64(0), builder.CreateSelect(minusOne, negated, quotient));
            break;
        }

        case Ir::Opcode::Compare:
        {
            int bits = IrSemantics::BitsInType(instruction.operandType);
            llvm::Value* maskedLeft = MaskToType(lowering, left, instruction.operandType);
            llvm::Value* maskedRight = MaskToType(lowering, right, instruction.operandType);
            llvm::Value* signedLeft = SignExtendTo64(lowering, left, bits);
            llvm::Value* signedRight = SignExtendTo64(lowering, right, bits);

            llvm::Value* outcome = nullptr;
            switch (instruction.predicate)
            {
                case Ir::ComparePredicate::Equal: outcome = builder.CreateICmpEQ(maskedLeft, maskedRight); break;
                case Ir::ComparePredicate::NotEqual: outcome = builder.CreateICmpNE(maskedLeft, maskedRight); break;
                case Ir::ComparePredicate::LessSigned: outcome = builder.CreateICmpSLT(signedLeft, signedRight); break;
                case Ir::ComparePredicate::LessEqualSigned: outcome = builder.CreateICmpSLE(signedLeft, signedRight); break;
                case Ir::ComparePredicate::GreaterSigned: outcome = builder.CreateICmpSGT(signedLeft, signedRight); break;
                case Ir::ComparePredicate::GreaterEqualSigned: outcome = builder.CreateICmpSGE(signedLeft, signedRight); break;
                case Ir::ComparePredicate::LessUnsigned: outcome = builder.CreateICmpULT(maskedLeft, maskedRight); break;
                case Ir::ComparePredicate::LessEqualUnsigned: outcome = builder.CreateICmpULE(maskedLeft, maskedRight); break;
                case Ir::ComparePredicate::GreaterUnsigned: outcome = builder.CreateICmpUGT(maskedLeft, maskedRight); break;
                case Ir::ComparePredicate::GreaterEqualUnsigned: outcome = builder.CreateICmpUGE(maskedLeft, maskedRight); break;
            }

            if (!outcome) return false;
            produced = builder.CreateZExt(outcome, wordType);
            break;
        }

        case Ir::Opcode::Select:
        {
            llvm::Value* flags = builder.CreateTrunc(ReadRegisterSlot(lowering, offsetof(GuestState::Registers, flagsNzcv)),
                                                     builder.getInt32Ty());

            llvm::Value* holds = EmitConditionHolds(lowering, flags, (uint8_t)instruction.condition);
            produced = builder.CreateSelect(holds, left, right);
            break;
        }

        case Ir::Opcode::Load:
        {
            int width = Ir::SizeInBytes(instruction.operandType);

            if (!lowering.directMemoryAccess)
            {
                if (!EmitAccessorLoad(lowering, instruction, left, width, steps, produced, producedHigh)) return false;
                break;
            }

            llvm::Value* pointer = EmitGuestPointer(lowering, left, instruction.guestAddress);

            if (IsWideType(instruction.operandType))
            {
                produced = builder.CreateAlignedLoad(wordType, pointer, llvm::MaybeAlign(1));
                producedHigh = builder.CreateAlignedLoad(wordType, FieldPointer(lowering, pointer, sizeof(uint64_t)),
                                                         llvm::MaybeAlign(1));
            }
            else
            {
                llvm::Type* narrow = llvm::Type::getIntNTy(lowering.context, (unsigned)width * 8);
                llvm::Value* loaded = builder.CreateAlignedLoad(narrow, pointer, llvm::MaybeAlign(1));

                produced = width == (int)sizeof(uint64_t) ? loaded : builder.CreateZExt(loaded, wordType);
            }

            if (OrderIsAcquire(instruction.order)) builder.CreateFence(llvm::AtomicOrdering::Acquire);
            break;
        }

        case Ir::Opcode::Store:
        {
            int width = Ir::SizeInBytes(instruction.operandType);

            if (!lowering.directMemoryAccess)
            {
                EmitAccessorStore(lowering, instruction, left, right, operands[1].high, width, steps);
                break;
            }

            llvm::Value* pointer = EmitGuestPointer(lowering, left, instruction.guestAddress);

            if (IsWideType(instruction.operandType))
            {
                llvm::Value* storedHigh = operands[1].high ? operands[1].high : builder.getInt64(0);

                builder.CreateAlignedStore(right, pointer, llvm::MaybeAlign(1));
                builder.CreateAlignedStore(storedHigh, FieldPointer(lowering, pointer, sizeof(uint64_t)), llvm::MaybeAlign(1));
            }
            else
            {
                llvm::Type* narrow = llvm::Type::getIntNTy(lowering.context, (unsigned)width * 8);
                llvm::Value* narrowed = width == (int)sizeof(uint64_t) ? right : builder.CreateTrunc(right, narrow);

                builder.CreateAlignedStore(narrowed, pointer, llvm::MaybeAlign(1));
            }

            if (OrderNeedsStoreLoadFence(instruction.order)) builder.CreateFence(llvm::AtomicOrdering::SequentiallyConsistent);
            break;
        }

        case Ir::Opcode::MemoryBarrier: builder.CreateFence(llvm::AtomicOrdering::SequentiallyConsistent); break;

        case Ir::Opcode::Branch:
            builder.CreateBr(TargetBlock(lowering, instruction.immediate, steps));
            terminated = true;
            return true;

        case Ir::Opcode::Call:
        {
            uint64_t resumeAddress = (uint64_t)instruction.guestAddress + 4;

            if (EmitDirectCall(lowering, instruction)) builder.CreateBr(TargetBlock(lowering, resumeAddress, steps));
            else builder.CreateBr(TargetBlock(lowering, instruction.immediate, steps));

            terminated = true;
            return true;
        }

        case Ir::Opcode::BranchIndirect:
            EmitExit(lowering, Interpreter::ExitReason::IndirectBranch, left, nullptr, steps);
            terminated = true;
            return true;

        case Ir::Opcode::Return:
            EmitExit(lowering, Interpreter::ExitReason::Returned, left, nullptr, steps);
            terminated = true;
            return true;

        case Ir::Opcode::Undefined:
            EmitExit(lowering, Interpreter::ExitReason::Undefined, nullptr, builder.getInt64((uint64_t)instruction.guestAddress), steps);
            terminated = true;
            return true;

        case Ir::Opcode::InvalidateInstructionCache:
            EmitExit(lowering, Interpreter::ExitReason::InstructionCacheInvalidated,
                     builder.getInt64((uint64_t)instruction.guestAddress), left, steps);
            terminated = true;
            return true;

        case Ir::Opcode::BranchConditional:
        {
            llvm::Value* taken = nullptr;
            if (instruction.operandCount > 0)
            {
                taken = builder.CreateICmpNE(left, builder.getInt64(0));
            }
            else
            {
                llvm::Value* flags = builder.CreateTrunc(ReadRegisterSlot(lowering, offsetof(GuestState::Registers, flagsNzcv)),
                                                         builder.getInt32Ty());
                taken = EmitConditionHolds(lowering, flags, (uint8_t)instruction.condition);
            }

            llvm::BasicBlock* takenBlock = TargetBlock(lowering, instruction.immediate, steps);
            llvm::BasicBlock* fallthroughBlock = TargetBlock(lowering, lowering.fallthroughTarget, steps);
            builder.CreateCondBr(taken, takenBlock, fallthroughBlock);

            terminated = true;
            return true;
        }

        default: return false;
    }

    if (instruction.result.id != Ir::INVALID_VALUE_ID)
    {
        if (!produced) return false;
        if (instruction.result.id >= lowering.values.size()) lowering.values.resize(instruction.result.id + 1);

        LoweredValue stored;
        stored.low = IsWideType(instruction.result.type) ? produced : MaskToType(lowering, produced, instruction.result.type);
        stored.high = producedHigh ? producedHigh : builder.getInt64(0);
        lowering.values[instruction.result.id] = stored;
    }

    return true;
}

LlvmBackend::Result LlvmBackend::Initialize(const Configuration& configuration, Session& session)
{
    static std::once_flag targetsInitialized;
    std::call_once(targetsInitialized, []() {
        llvm::InitializeNativeTarget();
        llvm::InitializeNativeTargetAsmPrinter();
        llvm::InitializeNativeTargetAsmParser();
    });

    LlvmBackendSession* implementation = new LlvmBackendSession();
    implementation->configuration = configuration;

    llvm::Expected<std::unique_ptr<llvm::orc::LLJIT>> built = llvm::orc::LLJITBuilder().setSupportConcurrentCompilation(true).create();
    if (!built)
    {
        llvm::consumeError(built.takeError());
        delete implementation;
        return Result::CodegenFailed;
    }

    implementation->jit = std::move(*built);

    llvm::orc::JITDylib& dylib = implementation->jit->getMainJITDylib();
    llvm::orc::MangleAndInterner mangle(implementation->jit->getExecutionSession(), implementation->jit->getDataLayout());

    llvm::orc::SymbolMap helpers;
    helpers[mangle("AclComputeAddFlags")] = {llvm::orc::ExecutorAddr::fromPtr(&AclComputeAddFlags), llvm::JITSymbolFlags::Exported};
    helpers[mangle("AclReverseBytesInLanes")] = {llvm::orc::ExecutorAddr::fromPtr(&AclReverseBytesInLanes), llvm::JITSymbolFlags::Exported};
    helpers[mangle("AclReverseBitsIn")] = {llvm::orc::ExecutorAddr::fromPtr(&AclReverseBitsIn), llvm::JITSymbolFlags::Exported};
    helpers[mangle("aclForeignAccessTally")] = {llvm::orc::ExecutorAddr::fromPtr(&aclForeignAccessTally),
                                                llvm::JITSymbolFlags::Exported};
    helpers[mangle("AclConditionHolds")] = {llvm::orc::ExecutorAddr::fromPtr(&AclConditionHolds), llvm::JITSymbolFlags::Exported};
    helpers[mangle("AclReadVirtualCounter")] = {llvm::orc::ExecutorAddr::fromPtr(&AclReadVirtualCounter),
                                                llvm::JITSymbolFlags::Exported};
    helpers[mangle("AclVectorOperation")] = {llvm::orc::ExecutorAddr::fromPtr(&AclVectorOperation), llvm::JITSymbolFlags::Exported};
    helpers[mangle("AclFloatOperation")] = {llvm::orc::ExecutorAddr::fromPtr(&AclFloatOperation), llvm::JITSymbolFlags::Exported};
    helpers[mangle("AclAtomicOperation")] = {llvm::orc::ExecutorAddr::fromPtr(&AclAtomicOperation), llvm::JITSymbolFlags::Exported};
    helpers[mangle("AclMemoryRead")] = {llvm::orc::ExecutorAddr::fromPtr(&AclMemoryRead), llvm::JITSymbolFlags::Exported};
    helpers[mangle("AclMemoryWrite")] = {llvm::orc::ExecutorAddr::fromPtr(&AclMemoryWrite), llvm::JITSymbolFlags::Exported};

    if (llvm::Error defined = dylib.define(llvm::orc::absoluteSymbols(std::move(helpers))))
    {
        llvm::consumeError(std::move(defined));
        delete implementation;
        return Result::CodegenFailed;
    }

    session.implementation = implementation;
    return Result::Ok;
}

void LlvmBackend::Shutdown(Session& session)
{
    delete Implementation(session);
    session.implementation = nullptr;

    ReturnFreedMemory();
}

void LlvmBackend::ReturnFreedMemory()
{
    malloc_trim(0);
}

static uint64_t ReportedMegabytes(const char* field)
{
    FILE* status = fopen("/proc/self/status", "r");
    if (!status) return 0;

    char line[256];
    unsigned long long kilobytes = 0;
    size_t width = strlen(field);

    while (fgets(line, sizeof(line), status))
    {
        if (strncmp(line, field, width) != 0) continue;
        if (sscanf(line + width, " %llu kB", &kilobytes) == 1) break;
    }

    fclose(status);
    return (uint64_t)(kilobytes >> 10);
}

uint64_t LlvmBackend::PeakResidentMegabytes()
{
    return ReportedMegabytes("VmHWM:");
}

uint64_t LlvmBackend::ResidentMegabytes()
{
    return ReportedMegabytes("VmRSS:");
}

LlvmBackend::Result LlvmBackend::CompileFunction(Session& session, const Ir::Function& function, Tier tier, CompiledFunction& compiled)
{
    LlvmBackendSession* implementation = Implementation(session);
    if (!implementation) return Result::NotInitialized;

    Ir::Opcode unsupported = Ir::Opcode::Invalid;
    bool lowerable = CanLower(function, unsupported);

    if (!lowerable)
    {
        std::lock_guard<std::mutex> guard(implementation->mutex);
        implementation->statistics.functionsRejected += 1;
        compiled.result = Result::UnsupportedOpcode;
        return Result::UnsupportedOpcode;
    }

    uint64_t moduleNumber = 0;

    {
        std::lock_guard<std::mutex> guard(implementation->mutex);
        moduleNumber = implementation->nextModule++;
    }

    auto context = std::make_unique<llvm::LLVMContext>();
    std::string name = "acl_" + std::to_string((uint64_t)function.entryAddress) + "_" + std::to_string(moduleNumber);
    auto module = std::make_unique<llvm::Module>(name, *context);
    module->setDataLayout(implementation->jit->getDataLayout());

    llvm::Function* entry =
        llvm::Function::Create(TranslatedFunctionType(*context), llvm::Function::ExternalLinkage, name, module.get());

    llvm::IRBuilder<> builder(*context);
    llvm::BasicBlock* entryBlock = llvm::BasicBlock::Create(*context, "entry", entry);
    builder.SetInsertPoint(entryBlock);

    Lowering lowering(*context, *module, builder);
    lowering.function = entry;
    lowering.registers = entry->getArg(0);
    lowering.runtime = entry->getArg(1);
    lowering.status = entry->getArg(2);
    lowering.entryBlock = entryBlock;
    lowering.directMemoryAccess = implementation->configuration.directMemoryAccess;
    lowering.reportForeignAccess = implementation->configuration.reportForeignAccess;
    lowering.arenaBase = implementation->configuration.arenaBase;
    lowering.arenaLength = implementation->configuration.arenaLength;

    if (!LowerFunction(lowering, function))
    {
        std::lock_guard<std::mutex> guard(implementation->mutex);
        implementation->statistics.functionsRejected += 1;
        compiled.result = Result::LoweringFailed;
        return Result::LoweringFailed;
    }

    if (implementation->configuration.verifyModule && llvm::verifyFunction(*entry, &llvm::errs()))
    {
        std::lock_guard<std::mutex> guard(implementation->mutex);
        implementation->statistics.functionsRejected += 1;
        compiled.result = Result::VerificationFailed;
        return Result::VerificationFailed;
    }

    OptimizeFunction(*entry, tier);

    if (implementation->configuration.emitIrToStderr)
    {
        std::lock_guard<std::mutex> guard(implementation->mutex);
        module->print(llvm::errs(), nullptr);
    }

    llvm::orc::ResourceTrackerSP tracker = implementation->jit->getMainJITDylib().createResourceTracker();

    DropUnwindTables(*module);
    llvm::orc::ThreadSafeModule safeModule(std::move(module), std::move(context));
    if (llvm::Error added = implementation->jit->addIRModule(tracker, std::move(safeModule)))
    {
        llvm::consumeError(std::move(added));
        llvm::consumeError(tracker->remove());
        compiled.result = Result::CodegenFailed;
        return Result::CodegenFailed;
    }

    llvm::Expected<llvm::orc::ExecutorAddr> address = implementation->jit->lookup(name);
    if (!address)
    {
        llvm::consumeError(address.takeError());
        compiled.result = Result::CodegenFailed;
        return Result::CodegenFailed;
    }

    compiled.guestEntryAddress = function.entryAddress;
    compiled.hostEntryPoint = address->toPtr<void*>();
    compiled.hostCodeSize = 0;
    compiled.tier = tier;
    compiled.result = Result::Ok;
    compiled.release = new llvm::orc::ResourceTrackerSP(tracker);

    std::lock_guard<std::mutex> guard(implementation->mutex);
    implementation->statistics.functionsCompiled += 1;
    implementation->statistics.blocksLowered += function.blocks.size();
    implementation->statistics.instructionsLowered += lowering.instructionsLowered;

    return Result::Ok;
}

static llvm::Type* JniTypeFor(llvm::LLVMContext& context, char letter)
{
    switch (letter)
    {
        case 'Z':
        case 'B': return llvm::Type::getInt8Ty(context);
        case 'C':
        case 'S': return llvm::Type::getInt16Ty(context);
        case 'I': return llvm::Type::getInt32Ty(context);
        case 'J': return llvm::Type::getInt64Ty(context);
        case 'F': return llvm::Type::getFloatTy(context);
        case 'D': return llvm::Type::getDoubleTy(context);
        case 'L':
        case '[': return llvm::PointerType::getUnqual(context);
        case 'V': return llvm::Type::getVoidTy(context);
        default: break;
    }

    return nullptr;
}

static bool IsJniValueLetter(char letter)
{
    return letter == 'Z' || letter == 'B' || letter == 'C' || letter == 'S' || letter == 'I' || letter == 'J' || letter == 'F' ||
           letter == 'D' || letter == 'L';
}

static bool SplitJniDescriptor(const char* descriptor, std::vector<char>& argumentLetters, char& returnLetter)
{
    argumentLetters.clear();
    returnLetter = 'V';

    if (!descriptor || descriptor[0] != '(') return false;

    uint64_t cursor = 1;
    while (descriptor[cursor] != '\0' && descriptor[cursor] != ')')
    {
        bool isArray = descriptor[cursor] == '[';
        while (descriptor[cursor] == '[') cursor += 1;
        if (descriptor[cursor] == '\0') return false;

        char letter = descriptor[cursor];
        if (!IsJniValueLetter(letter)) return false;

        if (letter == 'L')
        {
            while (descriptor[cursor] != '\0' && descriptor[cursor] != ';') cursor += 1;
            if (descriptor[cursor] == '\0') return false;
        }

        argumentLetters.push_back(isArray ? '[' : letter);
        cursor += 1;
    }

    if (descriptor[cursor] != ')') return false;

    cursor += 1;
    bool returnIsArray = descriptor[cursor] == '[';
    while (descriptor[cursor] == '[') cursor += 1;
    if (descriptor[cursor] == '\0') return false;

    if (descriptor[cursor] != 'V' && !IsJniValueLetter(descriptor[cursor])) return false;

    returnLetter = returnIsArray ? '[' : descriptor[cursor];
    return true;
}

LlvmBackend::Result LlvmBackend::EmitJniThunk(Session& session, const JniThunkRequest& request, CompiledFunction& compiled)
{
    LlvmBackendSession* implementation = Implementation(session);
    if (!implementation) return Result::NotInitialized;
    if (!request.descriptor || !request.enterGuest) return Result::BadDescriptor;

    std::vector<char> argumentLetters;
    char returnLetter = 'V';
    if (!SplitJniDescriptor(request.descriptor, argumentLetters, returnLetter))
    {
        compiled.result = Result::BadDescriptor;
        return Result::BadDescriptor;
    }

    std::lock_guard<std::mutex> guard(implementation->mutex);

    auto context = std::make_unique<llvm::LLVMContext>();
    std::string name = "acl_jni_" + std::to_string(request.guestFunction) + "_" + std::to_string(implementation->nextModule++);
    auto module = std::make_unique<llvm::Module>(name, *context);
    module->setDataLayout(implementation->jit->getDataLayout());

    llvm::Type* int64Type = llvm::Type::getInt64Ty(*context);
    llvm::Type* pointerType = llvm::PointerType::getUnqual(*context);

    llvm::Type* returnType = JniTypeFor(*context, returnLetter);
    if (!returnType)
    {
        compiled.result = Result::BadDescriptor;
        return Result::BadDescriptor;
    }

    std::vector<llvm::Type*> parameterTypes;
    parameterTypes.push_back(pointerType);
    parameterTypes.push_back(pointerType);

    for (char letter : argumentLetters)
    {
        llvm::Type* parameterType = JniTypeFor(*context, letter);
        if (!parameterType || parameterType->isVoidTy())
        {
            compiled.result = Result::BadDescriptor;
            return Result::BadDescriptor;
        }

        parameterTypes.push_back(parameterType);
    }

    llvm::FunctionType* thunkType = llvm::FunctionType::get(returnType, parameterTypes, false);
    llvm::Function* thunk = llvm::Function::Create(thunkType, llvm::Function::ExternalLinkage, name, module.get());

    llvm::IRBuilder<> builder(*context);
    builder.SetInsertPoint(llvm::BasicBlock::Create(*context, "entry", thunk));

    uint64_t slotCount = parameterTypes.size();
    llvm::Value* slots = builder.CreateAlloca(int64Type, builder.getInt64(slotCount), "slots");

    for (uint64_t index = 0; index < slotCount; ++index)
    {
        llvm::Value* argument = thunk->getArg((unsigned)index);
        llvm::Type* argumentType = argument->getType();
        llvm::Value* widened = nullptr;

        if (argumentType->isPointerTy()) widened = builder.CreatePtrToInt(argument, int64Type);
        else if (argumentType->isFloatTy())
            widened = builder.CreateZExt(builder.CreateBitCast(argument, llvm::Type::getInt32Ty(*context)), int64Type);
        else if (argumentType->isDoubleTy())
            widened = builder.CreateBitCast(argument, int64Type);
        else
            widened = builder.CreateZExt(argument, int64Type);

        builder.CreateStore(widened, builder.CreateGEP(int64Type, slots, builder.getInt64(index)));
    }

    llvm::FunctionType* helperType = llvm::FunctionType::get(int64Type, {pointerType, int64Type, pointerType, pointerType}, false);
    llvm::Value* helper = builder.CreateIntToPtr(builder.getInt64((uint64_t)request.enterGuest), pointerType);

    llvm::Value* descriptorGlobal = builder.CreateGlobalString(request.descriptor, "descriptor");
    llvm::Value* contextValue = builder.CreateIntToPtr(builder.getInt64((uint64_t)request.context), pointerType);

    llvm::Value* raw =
        builder.CreateCall(helperType, helper, {contextValue, builder.getInt64(request.guestFunction), descriptorGlobal, slots});

    if (returnType->isVoidTy()) builder.CreateRetVoid();
    else if (returnType->isPointerTy())
        builder.CreateRet(builder.CreateIntToPtr(raw, pointerType));
    else if (returnType->isFloatTy())
        builder.CreateRet(builder.CreateBitCast(builder.CreateTrunc(raw, llvm::Type::getInt32Ty(*context)), returnType));
    else if (returnType->isDoubleTy())
        builder.CreateRet(builder.CreateBitCast(raw, returnType));
    else
        builder.CreateRet(builder.CreateTrunc(raw, returnType));

    if (implementation->configuration.emitIrToStderr) module->print(llvm::errs(), nullptr);

    if (implementation->configuration.verifyModule && llvm::verifyFunction(*thunk, &llvm::errs()))
    {
        compiled.result = Result::VerificationFailed;
        return Result::VerificationFailed;
    }

    llvm::orc::ResourceTrackerSP tracker = implementation->jit->getMainJITDylib().createResourceTracker();

    DropUnwindTables(*module);
    llvm::orc::ThreadSafeModule safeModule(std::move(module), std::move(context));
    if (llvm::Error added = implementation->jit->addIRModule(tracker, std::move(safeModule)))
    {
        llvm::consumeError(std::move(added));
        llvm::consumeError(tracker->remove());
        compiled.result = Result::CodegenFailed;
        return Result::CodegenFailed;
    }

    llvm::Expected<llvm::orc::ExecutorAddr> address = implementation->jit->lookup(name);
    if (!address)
    {
        llvm::consumeError(address.takeError());
        compiled.result = Result::CodegenFailed;
        return Result::CodegenFailed;
    }

    compiled.guestEntryAddress = (GuestState::GuestAddress)request.guestFunction;
    compiled.hostEntryPoint = address->toPtr<void*>();
    compiled.hostCodeSize = 0;
    compiled.tier = implementation->configuration.tier;
    compiled.result = Result::Ok;
    compiled.release = new llvm::orc::ResourceTrackerSP(tracker);

    implementation->statistics.functionsCompiled += 1;
    return Result::Ok;
}

LlvmBackend::Result LlvmBackend::CompileFunctions(Session& session, const Ir::Function* functions, uint64_t count, Tier tier,
                                                  CompiledFunction* compiled, uint64_t& failureCount)
{
    failureCount = 0;

    for (uint64_t index = 0; index < count; ++index)
    {
        if (CompileFunction(session, functions[index], tier, compiled[index]) != Result::Ok) failureCount += 1;
    }

    return failureCount == 0 ? Result::Ok : Result::LoweringFailed;
}

std::string LlvmBackend::FunctionSymbol(GuestState::GuestAddress entryAddress)
{
    char name[64];
    snprintf(name, sizeof(name), "acl_fn_%016llx", (unsigned long long)entryAddress);

    return name;
}

struct ChunkResult
{
    uint64_t functionsCompiled = 0;
    uint64_t functionsRejected = 0;
    uint64_t instructionsLowered = 0;
    uint64_t blocksLowered = 0;
    uint64_t directCallsEmitted = 0;
    LlvmBackend::Result result = LlvmBackend::Result::Ok;
};

static llvm::CodeGenOptLevel RequestedCodeGenLevel()
{
    return llvm::CodeGenOptLevel::Default;
}

const char* LlvmBackend::CodeGenLevelTag()
{
    return "";
}

static LlvmBackend::ChunkProgressCallback gChunkProgressCallback = nullptr;
static void* gChunkProgressContext = nullptr;

void LlvmBackend::SetChunkProgressCallback(LlvmBackend::ChunkProgressCallback callback, void* context)
{
    gChunkProgressCallback = callback;
    gChunkProgressContext = context;
}

static void ReportChunkProgress(uint64_t completed, uint64_t total)
{
    if (!gChunkProgressCallback) return;

    gChunkProgressCallback(gChunkProgressContext, completed, total);
}

static void EmitChunk(const LlvmBackend::Configuration& configuration, const Ir::Function* functions,
                      const std::vector<uint64_t>& indices, const std::unordered_set<uint64_t>& allFunctions,
                      const std::string& outputPath, ChunkResult& chunk)
{
    llvm::Triple triple(llvm::sys::getDefaultTargetTriple());

    std::string lookupError;
    const llvm::Target* target = llvm::TargetRegistry::lookupTarget(triple, lookupError);
    if (!target)
    {
        chunk.result = LlvmBackend::Result::CodegenFailed;
        return;
    }

    llvm::TargetOptions options;
    std::unique_ptr<llvm::TargetMachine> machine(
        target->createTargetMachine(triple, LlvmBackend::HostCpuName(), HostCpuFeatures(), options, llvm::Reloc::PIC_, std::nullopt,
                                    RequestedCodeGenLevel()));

    if (!machine)
    {
        chunk.result = LlvmBackend::Result::CodegenFailed;
        return;
    }

    auto context = std::make_unique<llvm::LLVMContext>();
    auto module = std::make_unique<llvm::Module>("acl.translated", *context);
    module->setTargetTriple(triple);
    module->setDataLayout(machine->createDataLayout());

    llvm::FunctionType* entryType = TranslatedFunctionType(*context);
    std::unordered_map<uint64_t, llvm::Function*> declarations;

    for (uint64_t index : indices)
    {
        const Ir::Function& function = functions[index];

        Ir::Opcode unsupported = Ir::Opcode::Invalid;
        if (!LlvmBackend::CanLower(function, unsupported)) continue;

        uint64_t address = (uint64_t)function.entryAddress;
        if (declarations.count(address) != 0) continue;

        declarations.emplace(address, llvm::Function::Create(entryType, llvm::Function::ExternalLinkage,
                                                             LlvmBackend::FunctionSymbol(function.entryAddress), module.get()));
    }

    uint64_t built = 0;

    for (uint64_t index : indices)
    {
        const Ir::Function& function = functions[index];

        auto declared = declarations.find((uint64_t)function.entryAddress);
        if (declared == declarations.end())
        {
            chunk.functionsRejected += 1;
            continue;
        }

        if (!declared->second->empty()) continue;

        uint64_t lowered = 0;
        if (!BuildFunctionInto(*context, *module, declared->second, function, lowered, chunk.directCallsEmitted, &allFunctions,
                               configuration))
        {
            declared->second->deleteBody();
            chunk.functionsRejected += 1;
            continue;
        }

        chunk.instructionsLowered += lowered;
        chunk.blocksLowered += function.blocks.size();
        chunk.functionsCompiled += 1;
        built += 1;
    }

    if (built == 0)
    {
        chunk.result = LlvmBackend::Result::LoweringFailed;
        return;
    }

    if (configuration.verifyModule && llvm::verifyModule(*module, &llvm::errs()))
    {
        chunk.result = LlvmBackend::Result::VerificationFailed;
        return;
    }

    OptimizeModule(*module, configuration.tier, machine.get());

    std::error_code failure;
    llvm::raw_fd_ostream output(outputPath, failure, llvm::sys::fs::OF_None);
    if (failure)
    {
        chunk.result = LlvmBackend::Result::IoFailed;
        return;
    }

    llvm::legacy::PassManager emitter;
    if (machine->addPassesToEmitFile(emitter, output, nullptr, llvm::CodeGenFileType::ObjectFile))
    {
        chunk.result = LlvmBackend::Result::CodegenFailed;
        return;
    }

    emitter.run(*module);
    output.flush();

    chunk.result = output.has_error() ? LlvmBackend::Result::IoFailed : LlvmBackend::Result::Ok;
}

LlvmBackend::Result LlvmBackend::EmitObjectFiles(Session& session, const Ir::Function* functions, uint64_t count,
                                                 const GuestState::GuestAddress* knownEntries, uint64_t knownEntryCount,
                                                 const std::string& basePath, std::vector<std::string>& objectPaths)
{
    LlvmBackendSession* implementation = Implementation(session);
    if (!implementation) return Result::NotInitialized;
    if (!functions || count == 0) return Result::LoweringFailed;

    static std::once_flag machineInitialized;
    std::call_once(machineInitialized, []() {
        llvm::InitializeNativeTarget();
        llvm::InitializeNativeTargetAsmPrinter();
        llvm::InitializeNativeTargetAsmParser();
    });

    std::lock_guard<std::mutex> guard(implementation->mutex);

    std::unordered_set<uint64_t> allFunctions;

    if (knownEntries && knownEntryCount != 0)
    {
        allFunctions.reserve((size_t)knownEntryCount);
        for (uint64_t index = 0; index < knownEntryCount; ++index) allFunctions.insert((uint64_t)knownEntries[index]);
    }
    else
    {
        for (uint64_t index = 0; index < count; ++index)
        {
            Ir::Opcode unsupported = Ir::Opcode::Invalid;
            if (LlvmBackend::CanLower(functions[index], unsupported)) allFunctions.insert((uint64_t)functions[index].entryAddress);
        }
    }

    unsigned cores = std::thread::hardware_concurrency();
    if (cores == 0) cores = 1;

    uint64_t split = implementation->configuration.traceBlocks ? TRACED_CHUNK_MULTIPLIER : 1;
    uint64_t workerCount = implementation->configuration.traceBlocks ? (cores + 3) / 4 : cores;
    if (workerCount == 0) workerCount = 1;

    uint64_t instructionBudget = (DEFAULT_MEMORY_BUDGET_MEGABYTES << 20) / (workerCount * BYTES_PER_LOWERED_INSTRUCTION * split);
    if (instructionBudget < MINIMUM_INSTRUCTIONS_PER_CHUNK) instructionBudget = MINIMUM_INSTRUCTIONS_PER_CHUNK;

    std::vector<std::vector<uint64_t>> partitions;

    if (count < MINIMUM_FUNCTIONS_PER_CHUNK)
    {
        partitions.resize(1);
        for (uint64_t index = 0; index < count; ++index) partitions[0].push_back(index);
    }
    else
    {
        uint64_t carried = 0;
        partitions.emplace_back();

        for (uint64_t index = 0; index < count; ++index)
        {
            uint64_t weight = 0;
            for (const Ir::Block& block : functions[index].blocks) weight += block.instructions.size();

            if (carried != 0 && carried + weight > instructionBudget)
            {
                partitions.emplace_back();
                carried = 0;
            }

            partitions.back().push_back(index);
            carried += weight;
        }

        while (partitions.size() < workerCount * split && partitions.size() < count)
        {
            std::vector<uint64_t>& widest = *std::max_element(partitions.begin(), partitions.end(),
                                                              [](const std::vector<uint64_t>& left, const std::vector<uint64_t>& right) {
                                                                  return left.size() < right.size();
                                                              });
            if (widest.size() < 2) break;

            std::vector<uint64_t> half(widest.begin() + widest.size() / 2, widest.end());
            widest.resize(widest.size() / 2);
            partitions.push_back(std::move(half));
        }
    }

    uint64_t chunkCount = partitions.size();

    std::vector<ChunkResult> results((size_t)chunkCount);
    std::vector<std::string> paths((size_t)chunkCount);
    std::vector<std::thread> workers;

    for (size_t chunk = 0; chunk < (size_t)chunkCount; ++chunk) paths[chunk] = basePath + "." + std::to_string(chunk) + ".o";

    std::atomic<uint64_t> nextChunk{0};
    std::atomic<uint64_t> finishedChunks{0};
    if (workerCount > chunkCount) workerCount = chunkCount;

    for (uint64_t worker = 0; worker < workerCount; ++worker)
    {
        workers.emplace_back([&]() {
            for (;;)
            {
                uint64_t chunk = nextChunk.fetch_add(1, std::memory_order_relaxed);
                if (chunk >= chunkCount) return;

                EmitChunk(implementation->configuration, functions, partitions[(size_t)chunk], allFunctions, paths[(size_t)chunk],
                          results[(size_t)chunk]);

                ReturnFreedMemory();

                ReportChunkProgress(finishedChunks.fetch_add(1, std::memory_order_relaxed) + 1, chunkCount);
            }
        });
    }

    for (std::thread& worker : workers) worker.join();
    ReturnFreedMemory();

    Result outcome = Result::Ok;
    uint64_t compiled = 0;

    for (size_t chunk = 0; chunk < (size_t)chunkCount; ++chunk)
    {
        implementation->statistics.functionsCompiled += results[chunk].functionsCompiled;
        implementation->statistics.functionsRejected += results[chunk].functionsRejected;
        implementation->statistics.instructionsLowered += results[chunk].instructionsLowered;
        implementation->statistics.blocksLowered += results[chunk].blocksLowered;
        implementation->statistics.directCallsEmitted += results[chunk].directCallsEmitted;

        compiled += results[chunk].functionsCompiled;

        if (results[chunk].result == Result::Ok)
        {
            objectPaths.push_back(paths[chunk]);
            continue;
        }

        if (results[chunk].result != Result::LoweringFailed) outcome = results[chunk].result;
        remove(paths[chunk].c_str());
    }

    if (outcome != Result::Ok) return outcome;
    return compiled == 0 ? Result::LoweringFailed : Result::Ok;
}

LlvmBackend::Result LlvmBackend::EmitObjectFile(Session& session, const Ir::Function* functions, uint64_t count, const std::string& outputPath)
{
    LlvmBackendSession* implementation = Implementation(session);
    if (!implementation) return Result::NotInitialized;
    if (!functions || count == 0) return Result::LoweringFailed;

    static std::once_flag machineInitialized;
    std::call_once(machineInitialized, []() {
        llvm::InitializeNativeTarget();
        llvm::InitializeNativeTargetAsmPrinter();
        llvm::InitializeNativeTargetAsmParser();
    });

    std::lock_guard<std::mutex> guard(implementation->mutex);

    llvm::Triple triple(llvm::sys::getDefaultTargetTriple());

    std::string lookupError;
    const llvm::Target* target = llvm::TargetRegistry::lookupTarget(triple, lookupError);
    if (!target) return Result::CodegenFailed;

    llvm::TargetOptions options;
    std::unique_ptr<llvm::TargetMachine> machine(
        target->createTargetMachine(triple, LlvmBackend::HostCpuName(), HostCpuFeatures(), options, llvm::Reloc::PIC_, std::nullopt,
                                    RequestedCodeGenLevel()));

    if (!machine) return Result::CodegenFailed;

    auto context = std::make_unique<llvm::LLVMContext>();
    auto module = std::make_unique<llvm::Module>("acl.translated", *context);
    module->setTargetTriple(triple);
    module->setDataLayout(machine->createDataLayout());

    llvm::FunctionType* entryType = TranslatedFunctionType(*context);

    std::unordered_set<uint64_t> moduleFunctions;
    std::unordered_map<uint64_t, llvm::Function*> declarations;

    for (uint64_t index = 0; index < count; ++index)
    {
        const Ir::Function& function = functions[index];

        Ir::Opcode unsupported = Ir::Opcode::Invalid;
        if (!CanLower(function, unsupported)) continue;

        uint64_t address = (uint64_t)function.entryAddress;
        if (declarations.count(address) != 0) continue;

        declarations.emplace(address, llvm::Function::Create(entryType, llvm::Function::ExternalLinkage,
                                                             FunctionSymbol(function.entryAddress), module.get()));
        moduleFunctions.insert(address);
    }

    uint64_t built = 0;
    uint64_t directCalls = 0;

    for (uint64_t index = 0; index < count; ++index)
    {
        const Ir::Function& function = functions[index];

        auto declared = declarations.find((uint64_t)function.entryAddress);
        if (declared == declarations.end())
        {
            implementation->statistics.functionsRejected += 1;
            continue;
        }

        if (!declared->second->empty()) continue;

        uint64_t lowered = 0;
        if (!BuildFunctionInto(*context, *module, declared->second, function, lowered, directCalls, &moduleFunctions,
                               implementation->configuration))
        {
            implementation->statistics.functionsRejected += 1;
            continue;
        }

        implementation->statistics.instructionsLowered += lowered;
        implementation->statistics.blocksLowered += function.blocks.size();
        implementation->statistics.functionsCompiled += 1;
        built += 1;
    }

    if (built == 0) return Result::LoweringFailed;

    implementation->statistics.directCallsEmitted += directCalls;

    if (implementation->configuration.verifyModule && llvm::verifyModule(*module, &llvm::errs())) return Result::VerificationFailed;
    OptimizeModule(*module, implementation->configuration.tier, machine.get());

    if (implementation->configuration.emitIrToStderr) module->print(llvm::errs(), nullptr);

    std::error_code failure;
    llvm::raw_fd_ostream output(outputPath, failure, llvm::sys::fs::OF_None);
    if (failure) return Result::IoFailed;

    llvm::legacy::PassManager emitter;
    if (machine->addPassesToEmitFile(emitter, output, nullptr, llvm::CodeGenFileType::ObjectFile)) return Result::CodegenFailed;

    emitter.run(*module);
    output.flush();

    return output.has_error() ? Result::IoFailed : Result::Ok;
}

void LlvmBackend::ReleaseFunction(Session& session, CompiledFunction& compiled)
{
    if (!compiled.release) return;

    LlvmBackendSession* implementation = Implementation(session);
    llvm::orc::ResourceTrackerSP* tracker = (llvm::orc::ResourceTrackerSP*)compiled.release;

    if (implementation)
    {
        std::lock_guard<std::mutex> guard(implementation->mutex);
        llvm::consumeError((*tracker)->remove());
        implementation->statistics.functionsReleased += 1;
    }

    delete tracker;

    compiled.release = nullptr;
    compiled.hostEntryPoint = nullptr;
}

LlvmBackend::Statistics LlvmBackend::GetStatistics(const Session& session)
{
    LlvmBackendSession* implementation = Implementation(session);
    if (!implementation) return Statistics();

    std::lock_guard<std::mutex> guard(implementation->mutex);
    return implementation->statistics;
}

const char* LlvmBackend::ResultName(Result result)
{
    switch (result)
    {
        case Result::Ok: return "Ok";
        case Result::NotInitialized: return "NotInitialized";
        case Result::LoweringFailed: return "LoweringFailed";
        case Result::VerificationFailed: return "VerificationFailed";
        case Result::CodegenFailed: return "CodegenFailed";
        case Result::OutOfMemory: return "OutOfMemory";
        case Result::IoFailed: return "IoFailed";
        case Result::UnsupportedOpcode: return "UnsupportedOpcode";
        case Result::BadDescriptor: return "BadDescriptor";
    }

    return "Unknown";
}

const char* LlvmBackend::TierName(Tier tier)
{
    switch (tier)
    {
        case Tier::Baseline: return "Baseline";
        case Tier::Optimized: return "Optimized";
    }

    return "Unknown";
}
