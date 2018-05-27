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

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.clava.ast.type.tag.DeclRef;
import pt.up.fe.specs.clava.language.TagKind;

public abstract class TagType extends Type {

    private final DeclRef declInfo;
    private final TagKind tagKind;

    public TagType(DeclRef declInfo, TagKind tagKind, TypeData typeData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {
        super(typeData, info, children);

        this.declInfo = declInfo;
        this.tagKind = tagKind;
    }

    public DeclRef getDeclInfo() {
        return declInfo;
    }

    public TagKind getTagKind() {
        return tagKind;
    }

    @Override
    public String toContentString() {
        return "tagKind: " + getTagKind();
    }

    @Override
    public String getCode(ClavaNode sourceNode, String name) {
        // System.out.println("TAGTYPE:" + getTagKind());
        // System.out.println("DECL INFO:" + getDeclInfo());
        // System.out.println("TAG KIND:" + getTagKind());
        // System.out.println("TYPE DATA:" + getTypeData());
        String baseType = getDeclInfo().getDeclType();
        if (baseType.isEmpty()) {
            baseType = getBareType();
        }

        String enumType = getTagKind().getCode() + " " + baseType;
        if (name == null) {
            return enumType;
        }

        return enumType + " " + name;
    }

}
