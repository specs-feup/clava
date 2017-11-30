/**
 * Copyright 2017 SPeCS.
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
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.clava.ast.type.tag.DeclRef;

public class InjectedClassNameType extends Type {

    private final DeclRef declInfo;

    public InjectedClassNameType(DeclRef declInfo, TypeData data, ClavaNodeInfo info) {
        this(declInfo, data, info, Collections.emptyList());
    }

    private InjectedClassNameType(DeclRef declInfo, TypeData data, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {

        super(data, info, children);

        this.declInfo = declInfo;
    }

    @Override
    protected ClavaNode copyPrivate() {
        // TODO Auto-generated method stub
        return new InjectedClassNameType(declInfo, getTypeData(), getInfo(), Collections.emptyList());
    }

}
