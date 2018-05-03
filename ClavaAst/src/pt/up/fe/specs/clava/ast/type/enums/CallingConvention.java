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

public enum CallingConvention {
    C("cdecl"), // __attribute__((cdecl))
    X86StdCall("stdcall"), // __attribute__((stdcall))
    X86FastCall("fastcall"), // __attribute__((fastcall))
    X86ThisCall("thiscall"), // __attribute__((thiscall))
    X86VectorCall("vectorcall"), // __attribute__((vectorcall))
    X86Pascal("pascal"), // __attribute__((pascal))
    X86_64Win64("ms_abi"), // __attribute__((ms_abi))
    X86_64SysV("sysv_abi"), // __attribute__((sysv_abi))
    AAPCS("pcs(\"aapcs\")"), // __attribute__((pcs("aapcs")))
    AAPCS_VFP("pcs(\"aapcs-vfp\")"), // __attribute__((pcs("aapcs-vfp")))
    IntelOclBicc("intel_ocl_bicc"), // __attribute__((intel_ocl_bicc))
    SpirFunction(null), // default for OpenCL functions on SPIR target
    SpirKernel(null); // inferred for OpenCL kernels on SPIR target

    private final String attribute;

    private CallingConvention(String attribute) {
        this.attribute = attribute;
    }

    public String getAttribute() {
        return attribute;
    }

    public String getAttributeCode() {
        if (attribute == null) {
            return "";
        }

        return "__attribute__((" + attribute + "))";
    }

}
