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
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.DummyNode;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.clava.ast.type.data2.DummyTypeData;

/**
 * Dummy type, for testing purposes.
 * 
 * @author JoaoBispo
 *
 */
public class DummyType extends Type implements DummyNode {

    public DummyType(DummyTypeData data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    /**
     * Legacy support.
     * 
     * @param typeData
     * @param info
     * @param children
     */
    public DummyType(TypeData typeData, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(typeData, info, children);
    }

    @Override
    public DummyTypeData getData() {
        return (DummyTypeData) super.getData();
    }

    public String getNodeCode() {
        return "// Dummy type '" + getData().getContent() + "'";
    }

    @Override
    public String getCode() {
        return ClavaNodes.toCode(getNodeCode(), this);
    }

    @Override
    public String getContent() {
        return getData().getContent();
    }

}
