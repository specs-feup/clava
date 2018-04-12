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
import pt.up.fe.specs.clava.ast.decl.data2.DeclDataV2;

/**
 * Represents one declaration (or definition).
 * 
 * @author JoaoBispo
 *
 */
public abstract class Decl extends ClavaNode {

    private final DeclData data;

    public Decl(DeclDataV2 data, Collection<? extends ClavaNode> children) {
        super(data, children);

        this.data = null;
    }

    @Override
    public DeclDataV2 getData() {
        return (DeclDataV2) super.getData();
    }

    /**
     * Legacy support.
     * 
     * @param data
     * @param info
     * @param children
     */
    public Decl(DeclData data, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(info, children);

        this.data = data;
    }

    public DeclData getDeclData() {
        return data;
    }

}
