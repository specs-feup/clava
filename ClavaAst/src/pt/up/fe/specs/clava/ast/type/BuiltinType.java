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

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.enums.BuiltinKind;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.clava.ast.type.data2.BuiltinTypeData;

public class BuiltinType extends Type {

    public BuiltinType(BuiltinTypeData data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // public BuiltinType(String builtinType) {
    //
    // }

    public BuiltinType(BuiltinKind kind) {
        this(new BuiltinTypeData(kind), Collections.emptyList());
    }

    /**
     * @deprecated for legacy support
     * @param data
     * @param info
     * @param children
     */
    @Deprecated
    protected BuiltinType(TypeData data, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(data, info, children);
    }

    @Override
    public BuiltinTypeData getData() {
        return (BuiltinTypeData) super.getData();
    }

    @Override
    public String getCode(String name) {
        // boolean isCxx = getApp().getAppData().get(ClavaOptions.STANDARD).isCxx();
        // boolean isCxx = getData().getStandard().isCxx();
        String type = getData().getKind().getCode();

        String varName = name == null ? "" : " " + name;
        return type + varName;
    }

    /**
     * TODO: Remove this method, move to IntegerLiteral (only use), used BuiltinKind
     */
    @Override
    public String getConstantCode(String constant) {
        boolean isUnsigned = getData().getKind().isUnsigned();

        if (isUnsigned) {
            // if (getBareType().startsWith("unsigned")) {
            return constant + "u";
        }

        return constant;

    }

    public boolean isVoid() {
        return getData().getKind() == BuiltinKind.VOID;
    }

}
