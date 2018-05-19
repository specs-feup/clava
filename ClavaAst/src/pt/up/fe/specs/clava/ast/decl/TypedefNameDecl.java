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

/**
 * Base class for declarations which introduce a typedef-name.
 * 
 * @author JoaoBispo
 *
 */
public abstract class TypedefNameDecl extends TypeDecl {

    private final Type underlyingType;

    public TypedefNameDecl(Type underlyingType, String declName, Type type, DeclData declData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {

        super(declName, type, declData, info, children);

        this.underlyingType = underlyingType;
    }

    public Type getUnderlyingType() {
        return underlyingType;
    }

}
