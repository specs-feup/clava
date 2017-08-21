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

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.language.TagKind;

/**
 * Represents a declaration of a struct, union, class or enum.
 * 
 * @author JoaoBispo
 *
 */
public abstract class TagDecl extends TypeDecl {

    /**
     * @deprecated
     * @param tagKind
     * @param declName
     * @param type
     * @param info
     * @param children
     */
    @Deprecated
    public TagDecl(TagKind tagKind, String declName, Type type, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {
        super(declName, type, info, children);

    }

    public TagDecl(Type type, DeclData declData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {
        super(null, type, declData, info, children);

    }

    public abstract TagKind getTagKind();

}
