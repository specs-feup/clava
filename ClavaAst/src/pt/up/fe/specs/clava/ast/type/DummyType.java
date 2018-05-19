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

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.DummyNode;
import pt.up.fe.specs.clava.ast.type.data.TypeData;

/**
 * Dummy type, for testing purposes.
 * 
 * @author JoaoBispo
 *
 */
public class DummyType extends Type implements DummyNode {

    public DummyType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);

        // SpecsLogs.msgWarn("DUMMY TYPE DATASTORE");
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

        // SpecsLogs.msgWarn("DUMMY TYPE TYPEDATA");
    }

    public String getNodeCode() {
        return "// Dummy type '" + getContent() + "'";
    }

    @Override
    public String getCode(String name) {
        return ClavaNodes.toCode(getNodeCode(), this);
    }

}
