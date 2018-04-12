/**
 * Copyright 2018 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.ast.lang;

import pt.up.fe.specs.util.enums.EnumHelper;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

public enum AttributeKind implements StringProvider {

    Annotate,
    CFConsumed,
    CarriesDependency,
    NSConsumed,
    PassObjectSize,

    AMDGPUNumSGPR,
    AMDGPUNumVGPR,
    ARMInterrupt,
    AcquireCapability,
    AcquiredAfter,
    AcquiredBefore,
    AlignMac68k,
    Aligned,
    AlwaysInline,
    AnalyzerNoReturn,
    ArcWeakrefUnavailable,
    ArgumentWithTypeTag,
    AsmLabel,
    AssertCapability,
    AssertExclusiveLock,
    AssertSharedLock,
    AssumeAligned,
    Availability,
    Blocks,
    C11NoReturn,
    CDecl,
    CFAuditedTransfer,
    CFReturnsNotRetained,
    CFReturnsRetained,
    CFUnknownTransfer,
    CUDAConstant,
    CUDADevice,
    CUDAGlobal,
    CUDAHost,
    CUDAInvalidTarget,
    CUDALaunchBounds,
    CUDAShared,
    CXX11NoReturn,
    CallableWhen,
    Capability,
    CapturedRecord,
    Cleanup,
    Cold,
    Common,
    Const,
    Constructor,
    Consumable,
    ConsumableAutoCast,
    ConsumableSetOnRead,
    DLLExport,
    DLLImport,
    Deprecated,
    Destructor,
    DisableTailCalls,
    EnableIf,
    ExclusiveTrylockFunction,
    FastCall,
    Final("final"),
    FlagEnum,
    Flatten,
    Format,
    FormatArg,
    GNUInline,
    GuardedBy,
    GuardedVar,
    Hot,
    IBAction,
    IBOutlet,
    IBOutletCollection,
    InitPriority,
    IntelOclBicc,
    InternalLinkage,
    LockReturned,
    LocksExcluded,
    MSABI,
    MSInheritance,
    MSNoVTable,
    MSP430Interrupt,
    MSStruct,
    MSVtorDisp,
    MaxFieldAlignment,
    MayAlias,
    MinSize,
    Mips16,
    MipsInterrupt,
    NSConsumesSelf,
    NSReturnsAutoreleased,
    NSReturnsNotRetained,
    NSReturnsRetained,
    Naked,
    NoAlias,
    NoCommon,
    NoDebug,
    NoDuplicate,
    NoInline,
    NoInstrumentFunction,
    NoMips16,
    NoReturn,
    NoSanitize,
    NoSplitStack,
    NoThreadSafetyAnalysis,
    NoThrow,
    NonNull,
    NotTailCalled,
    OMPThreadPrivateDecl,
    ObjCBridge,
    ObjCBridgeMutable,
    ObjCBridgeRelated,
    ObjCException,
    ObjCExplicitProtocolImpl,
    ObjCIndependentClass,
    ObjCMethodFamily,
    ObjCNSObject,
    ObjCPreciseLifetime,
    ObjCRequiresPropertyDefs,
    ObjCRequiresSuper,
    ObjCReturnsInnerPointer,
    ObjCRootClass,
    OpenCLKernel("__kernel"),
    OptimizeNone,
    Override,
    Ownership,
    Packed,
    ParamTypestate,
    Pascal,
    Pcs,
    PtGuardedBy,
    PtGuardedVar,
    Pure,
    ReleaseCapability,
    ReqdWorkGroupSize,
    RequiresCapability,
    Restrict,
    ReturnTypestate,
    ReturnsNonNull,
    ReturnsTwice,
    ScopedLockable,
    Section,
    SelectAny,
    Sentinel,
    SetTypestate,
    SharedTrylockFunction,
    StdCall,
    SysVABI,
    TLSModel,
    Target,
    TestTypestate,
    ThisCall,
    TransparentUnion,
    TryAcquireCapability,
    TypeTagForDatatype,
    TypeVisibility,
    Unavailable,
    Unused,
    Used,
    Uuid,
    VecReturn,
    VecTypeHint,
    VectorCall,
    Visibility,
    WarnUnused,
    WarnUnusedResult,
    Weak,
    WeakImport,
    WeakRef,
    WorkGroupSizeHint,
    X86ForceAlignArgPointer,

    Alias,
    AlignValue,
    FallThrough,
    InitSeg,
    LoopHint,
    Mode,
    ObjCBoxable,
    ObjCDesignatedInitializer,
    ObjCRuntimeName,
    OpenCLImageAccess,
    Overloadable,
    Thread;

    private static final Lazy<EnumHelper<AttributeKind>> ENUM_HELPER = EnumHelper.newLazyHelper(AttributeKind.class);

    public static EnumHelper<AttributeKind> getHelper() {
        return ENUM_HELPER.get();
    }

    private final String code;

    private AttributeKind() {
        code = null;
    }

    private AttributeKind(String code) {
        this.code = code;
    }

    @Override
    public String getString() {
        // return name().toLowerCase();
        return name();
    }

    public String getCode() {
        if (code == null) {
            throw new RuntimeException("Code not defined for attribute '" + this + "'");
        }

        return code;
    }

}
