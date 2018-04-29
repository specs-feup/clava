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

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.DummyNode;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.decl.data2.DummyDeclData;

/**
 * Dummy declaration, for testing purposes.
 * 
 * @author JoaoBispo
 *
 */
public class DummyDecl extends Decl implements DummyNode {

    public DummyDecl(DataStore clavaData, Collection<? extends ClavaNode> children) {
        super(clavaData, children);
    }

    public DummyDecl(DummyDeclData clavaData, Collection<? extends ClavaNode> children) {
        super(clavaData, children);
    }

    // public DummyDecl(ClavaNode node) {
    // this(new DummyDeclData(node.getClass().getSimpleName(), DeclDataV2.empty(node.getData())), node.getChildren());
    // }

    /**
     * For legacy support.
     * 
     * @param info
     * @param children
     */
    protected DummyDecl(ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(DeclData.empty(), info, children);
    }

    @Override
    public DummyDeclData getData() {
        return (DummyDeclData) super.getData();
    }

    @Override
    public String getNodeName() {
        return super.getNodeName() + " (" + getData().getClassname() + ")";
    }

    public String getNodeCode() {
        return "/* Dummy declaration '" + getData().getClassname() + "' */";
    }

    @Override
    public String getCode() {
        return ClavaNodes.toCode(getNodeCode(), this);
    }

    @Override
    public String getContent() {
        return getData().toString();
    }

}
