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

package pt.up.fe.specs.clava.ast.type.enums;

import pt.up.fe.specs.util.enums.EnumHelperWithValue;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

public enum CallingConvention implements StringProvider {

    C,
    X86StdCall,
    X86FastCall,
    X86ThisCall,
    X86VectorCall,
    X86Pascal,
    Win64,
    X86_64SysV,
    X86RegCall,
    AAPCS,
    AAPCS_VFP,
    IntelOclBicc,
    SpirFunction,
    OpenCLKernel,
    Swift,
    SwiftAsync,
    PreserveMost,
    PreserveAll,
    AArch64VectorCall,
    AArch64SVEPCS,
    AMDGPUKernelCall;

    private static final Lazy<EnumHelperWithValue<CallingConvention>> HELPER = EnumHelperWithValue
            .newLazyHelperWithValue(CallingConvention.class);

    public static EnumHelperWithValue<CallingConvention> getHelper() {
        return HELPER.get();
    }

    // private final String attribute;

    // private CallingConvention(String attribute) {
    // this.attribute = attribute;
    // }
    //
    // public String getAttribute() {
    // return attribute;
    // }

    public String getAttributeCode() {
        switch (this) {
        case C:
            return toAttribute("cdecl"); // __attribute__((cdecl))
        case X86StdCall:
            return toAttribute("stdcall"); // __attribute__((stdcall))
        case X86FastCall:
            return toAttribute("fastcall"); // __attribute__((fastcall))
        case X86ThisCall:
            return toAttribute("thiscall"); // __attribute__((thiscall))
        case X86VectorCall:
            return toAttribute("vectorcall"); // __attribute__((vectorcall))
        case X86Pascal:
            return toAttribute("pascal"); // __attribute__((pascal))
        case Win64:
            return toAttribute("ms_abi"); // __attribute__((ms_abi))
        case X86_64SysV:
            return toAttribute("sysv_abi"); // __attribute__((sysv_abi))
        case X86RegCall:
            return toAttribute("regcall"); // __attribute__((regcall))
        case AAPCS:
            return toAttribute("pcs(\"aapcs\")"); // __attribute__((pcs("aapcs")))
        case AAPCS_VFP:
            return toAttribute("pcs(\"aapcs-vfp\")"); // __attribute__((pcs("aapcs-vfp")))
        case IntelOclBicc:
            return toAttribute("intel_ocl_bicc"); // __attribute__((intel_ocl_bicc))
        case SpirFunction:
            return ""; // default for OpenCL functions on SPIR target
        case OpenCLKernel:
            return ""; // inferred for OpenCL kernels
        case Swift:
            return toAttribute("swiftcall"); // __attribute__((swiftcall))
        case PreserveMost:
            return toAttribute("preserve_most"); // __attribute__((preserve_most))
        case PreserveAll:
            return toAttribute("preserve_all"); // __attribute__((preserve_all))
        default:
            throw new RuntimeException("Not implemented yet");
        }
        // if (attribute == null) {
        // return "";
        // }
        //
        // return "__attribute__((" + attribute + "))";
    }

    private String toAttribute(String attribute) {
        return "__attribute__((" + attribute + "))";
    }

    @Override
    public String getString() {
        return name();
    }

}
