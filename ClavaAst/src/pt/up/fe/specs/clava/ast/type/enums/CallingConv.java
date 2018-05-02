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

import pt.up.fe.specs.util.enums.EnumHelper;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

/**
 * Calling conventions.
 * 
 * @author JoaoBispo
 *
 */
public enum CallingConv implements StringProvider {

    C("cdecl"),
    X86_STD_CALL("stdcall"),
    X86_FAST_CALL("fastcall"),
    X86_THIS_CALL("thiscall"),
    X86_VECTOR_CALL("vectorcall"),
    X86_PASCAL("pascal"),
    X86_64_WIN_64("ms_abi"),
    X86_64_SYS_V("sysv_abi"),
    AAPCS("pcs(\"aapcs\")"),
    AAPCS_VFP("pcs(\"aapcs-vfp\")"),
    INTEL_OC_BICC("intel_ocl_bicc"),
    SPIR_FUNCTION("opencl default for SPIR"),
    SPIR_KERNEL("opencl default for SPIR kernels");

    private static final Lazy<EnumHelper<CallingConv>> HELPER = EnumHelper.newLazyHelper(CallingConv.class);

    public static EnumHelper<CallingConv> getEnumHelper() {
        return HELPER.get();
    }

    private final String attribute;

    private CallingConv(String attribute) {
        this.attribute = attribute;
    }

    public String getAttribute() {
        return attribute;
    }

    @Override
    public String getString() {
        return getAttribute();
    }
}