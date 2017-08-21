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

package pt.up.fe.specs.clava.ast.extra;

import java.util.Collection;
import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.DummyNode;

/**
 * Generic dummy node, for testing purposes.
 * 
 * @author JoaoBispo
 *
 */
public class Undefined extends ClavaNode implements DummyNode {

    private final String name;
    private final String content;

    public Undefined(String name, String content, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(info, children);

        this.name = name;
        this.content = content;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new Undefined(name, content, getInfo(), Collections.emptyList());
    }

    public String getNodeCode() {
        return "// Undefined node '" + name + "', content:" + content;
    }

    @Override
    public String getCode() {
        return ClavaNodes.toCode(getNodeCode(), this);
    }

    @Override
    public String getContent() {
        return content;
    }

    // public String getUndefinedNodeName() {
    // // Get first whitespace
    // int whiteIndex = content.indexOf(' ');
    // if (whiteIndex == -1) {
    // return content;
    // }
    //
    // return content.substring(0, whiteIndex);
    // }

    @Override
    public String toContentString() {
        return content;
        // return super.toContentString() + "Name:" + name + "; Content:" + content;
    }

}
