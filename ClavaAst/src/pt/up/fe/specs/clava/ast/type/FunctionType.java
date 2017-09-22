/**
 * Copyright 2016 SPeCS.
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

package pt.up.fe.specs.clava.ast.type;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.type.data.FunctionTypeData;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.util.enums.EnumHelper;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

/**
 * Represents the type of a Function.
 * 
 * @author JoaoBispo
 *
 */
public abstract class FunctionType extends Type {

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

    /**
     * Data class for Function Type.
     * 
     * @author JoaoBispo
     *
     */
    /*
    static public class FunctionTypeData {
        private final boolean hasNoReturn;
        private final boolean isConst;
        private final CallingConv callingConv;
    
        public FunctionTypeData(boolean hasNoReturn, boolean isConst, CallingConv callingConv) {
            this.hasNoReturn = hasNoReturn;
            this.isConst = isConst;
            this.callingConv = callingConv;
        }
    
        public boolean getHasNoReturn() {
            return hasNoReturn;
        }
    
        public boolean isConst() {
            return isConst;
        }
    
        public CallingConv getCallingConv() {
            return callingConv;
        }
    }
    */

    /**
     * FunctionType fields
     */
    private final FunctionTypeData functionTypeData;

    /**
     * 
     * @param functionTypeData
     * @param type
     * @param info
     * @param children
     */
    public FunctionType(FunctionTypeData functionTypeData, String type, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {
        this(functionTypeData, new TypeData(type), info, children);
    }

    public FunctionType(FunctionTypeData functionTypeData, TypeData typeData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {
        super(typeData, info, children);

        // Cannot do this check, because copies of the node are done without children
        // Preconditions.checkArgument(children.size() > 0, "Expected at least one child, the return argument");

        this.functionTypeData = functionTypeData;
    }

    public Type getReturnType() {
        return getChild(Type.class, 0);
    }

    public List<Type> getParamTypes() {
        if (getNumChildren() == 1) {
            Collections.emptyList();
        }

        return getChildren(Type.class, 1);
    }

    public FunctionTypeData getFunctionTypeData() {
        return functionTypeData;
    }

    @Override
    public String getCode(String name) {
        // A name (*) means that it has been called from PointerType with null name
        // if (name != null && !name.isEmpty() && !name.equals("(*)")) {
        // SpecsLogs.msgWarn(
        // ".getCode() for FunctionType using name, check if ok since we do not have information about parameters
        // names");
        // }

        StringBuilder code = new StringBuilder();

        code.append(getReturnType().getCode(name));

        String paramsCode = getParamTypes().stream()
                .map(type -> type.getCode())
                .collect(Collectors.joining(", ", " (", ")"));

        code.append(paramsCode);

        return code.toString();
    }

}
