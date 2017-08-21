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
import pt.up.fe.specs.clava.ast.decl.EnumDecl;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.clava.ast.type.tag.DeclRef;
import pt.up.fe.specs.clava.language.TagKind;
import pt.up.fe.specs.util.exceptions.WrongClassException;

/**
 * Represents an enum.
 * 
 * @author JoaoBispo
 *
 */
public class EnumType extends TagType {

    public EnumType(DeclRef declInfo, TypeData typeData, ClavaNodeInfo info) {
        this(declInfo, typeData, info, Collections.emptyList());
    }

    private EnumType(DeclRef declInfo, TypeData typeData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {
        super(declInfo, TagKind.ENUM, typeData, info, children);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new EnumType(getDeclInfo(), getTypeData(), getInfo(), Collections.emptyList());
    }

    public EnumDecl getEnumDecl(App app) {
        ClavaNode declNode = app.getNode(getDeclInfo().getDeclId());

        if (!(declNode instanceof EnumDecl)) {
            throw new WrongClassException(declNode, EnumDecl.class);
        }

        return (EnumDecl) declNode;
    }

}
