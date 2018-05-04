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

package pt.up.fe.specs.clava.ast;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.type.FunctionProtoType;
import pt.up.fe.specs.clava.ast.type.FunctionType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.data.FunctionProtoTypeData;
import pt.up.fe.specs.clava.ast.type.data.FunctionTypeData;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.clava.ast.type.enums.CallingConvention;
import pt.up.fe.specs.clava.ast.type.enums.ExceptionSpecificationType;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.context.ClavaFactory;
import pt.up.fe.specs.util.SpecsEnums;

public class LegacyToDataStore {
    /**
     * Temporary measure, to provide the possibility to transform legacy nodes into DataStore nodes.
     */
    public static final ThreadLocal<ClavaContext> CLAVA_CONTEXT = new ThreadLocal<>();

    private final DataStore nodeData;

    public LegacyToDataStore() {
        nodeData = DataStore.newInstance("legacy data");
        nodeData.add(ClavaNode.CONTEXT, CLAVA_CONTEXT.get());
    }

    public static ClavaFactory getFactory() {
        return CLAVA_CONTEXT.get().get(ClavaContext.FACTORY);
    }

    public DataStore getData() {
        // If id is not set, add a custom one
        if (!nodeData.hasValue(ClavaNode.ID)) {
            String id = CLAVA_CONTEXT.get().get(ClavaContext.ID_GENERATOR).next("legacy_");
            nodeData.add(ClavaNode.ID, id);
        }
        return nodeData;
    }

    public LegacyToDataStore setNodeInfo(ClavaNodeInfo nodeInfo) {

        nodeData.add(ClavaNode.LOCATION, nodeInfo.getLocationTry().orElse(SourceRange.invalidRange()));

        return this;
    }

    /// TYPES

    public LegacyToDataStore setType(TypeData data) {

        nodeData.add(Type.TYPE_AS_STRING, data.getBareType());
        nodeData.add(Type.HAS_SUGAR, data.hasSugar());

        return this;
    }

    public LegacyToDataStore setFunctionType(FunctionTypeData data) {

        nodeData.add(FunctionType.NO_RETURN, data.hasNoReturn());
        nodeData.add(FunctionType.PRODUCES_RESULT, data.producesResult());
        nodeData.add(FunctionType.HAS_REG_PARM, data.hasRegParm());
        nodeData.add(FunctionType.REG_PARM, (long) data.getRegParm());
        nodeData.add(FunctionType.CALLING_CONVENTION,
                SpecsEnums.values(CallingConvention.class)[data.getCallingConv().ordinal()]);

        return this;
    }

    public LegacyToDataStore setFunctionProtoType(FunctionProtoTypeData data) {

        nodeData.add(FunctionProtoType.HAS_TRAILING_RETURNS, data.isHasTrailingReturn());
        nodeData.add(FunctionProtoType.IS_CONST, data.isConst());
        nodeData.add(FunctionProtoType.IS_VOLATILE, data.isVolatile());
        nodeData.add(FunctionProtoType.IS_RESTRICT, data.isRestrict());
        nodeData.add(FunctionProtoType.REFERENCE_QUALIFIER, data.getReferenceQualifier());
        nodeData.add(FunctionProtoType.EXCEPTION_SPECIFICATION_TYPE,
                SpecsEnums.values(ExceptionSpecificationType.class)[data.getSpecifier().ordinal()]);
        nodeData.add(FunctionProtoType.NOEXCEPT_EXPR,
                getFactory().literalExpr(data.getNoexceptExpr(), getFactory().dummyType("<no type>")));

        return this;
    }

    /// EXPRS

    public LegacyToDataStore setExpr(ExprData data) {

        nodeData.add(Expr.TYPE, data.getType());
        nodeData.add(Expr.VALUE_KIND, data.getValueKind());
        nodeData.add(Expr.OBJECT_KIND, data.getObjectKind());

        return this;
    }

}
