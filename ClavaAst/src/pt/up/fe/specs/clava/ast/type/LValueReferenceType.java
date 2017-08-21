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

public class LValueReferenceType extends ReferenceType {

    public LValueReferenceType(TypeData typeData, ClavaNodeInfo info, Type referencee) {
        this(typeData, info, Arrays.asList(referencee));
    }

    private LValueReferenceType(TypeData typeData, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(typeData, info, children);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new LValueReferenceType(getTypeData(), getInfo(), Collections.emptyList());
    }

    // @Override
    // public String getCode() {
    // return getReferencedType().getCode() + "&";
    // }
    //
    @Override
    public String getCode(String name) {

        StringBuilder code = new StringBuilder();
        code.append(getReferencee().getCode(null)).append("&");

        if (name != null) {
            code.append(" ").append(name);
        }

        return code.toString();
    }

    @Override
    public Type getReferencee() {
        return getChild(Type.class, 0);
    }

}
