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

package pt.up.fe.specs.clava.weaver.joinpoints;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.AccessSpecDecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AAccessSpecifier;

public class CxxAccessSpecifier extends AAccessSpecifier {

    private final AccessSpecDecl accessSpecifier;

    public CxxAccessSpecifier(AccessSpecDecl accessSpecifier) {
        super(new CxxDecl(accessSpecifier));
        this.accessSpecifier = accessSpecifier;
    }

    @Override
    public ClavaNode getNode() {
        return accessSpecifier;
    }

    @Override
    public String getKindImpl() {
        return accessSpecifier.get(AccessSpecDecl.ACCESS_SPECIFIER).name().toLowerCase();
    }

}
