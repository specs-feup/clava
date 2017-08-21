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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.type.data.TypeData;

public class ElaboratedType extends TypeWithKeyword {

    public ElaboratedType(ElaboratedTypeKeyword keyword, TypeData typeData, ClavaNodeInfo info,
            Type namedType) {
        this(keyword, typeData, info, Arrays.asList(namedType));
    }

    private ElaboratedType(ElaboratedTypeKeyword keyword, TypeData typeData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {
        super(keyword, typeData, info, children);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new ElaboratedType(getKeyword(), getTypeData(), getInfo(), Collections.emptyList());
    }

    @Override
    public Type getNamedType() {
        return getChild(Type.class, 0);
    }

    /*
    @Override
    public boolean isAnonymous() {
    // return getNamedType() instanceof NullType;
    return getNamedType().isAnonymous();
    }
    */

    @Override
    public String getCode(String name) {
        String bareType = getBareType();

        // HACK
        // if (getKeyword() != ElaboratedTypeKeyword.STRUCT && ) {
        // if (!bareType.startsWith("struct ")) {
        // System.out.println("BEFORE:" + bareType);
        // bareType = bareType.replaceAll("struct ", "");
        // System.out.println("AFTER:" + bareType);
        // }

        if (name == null) {
            return bareType;
        }

        return bareType + " " + name;
    }

    @Override
    public Type desugar() {
        return getNamedType().desugar();
    }
}
