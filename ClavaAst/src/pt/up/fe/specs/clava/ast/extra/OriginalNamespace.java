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

public class OriginalNamespace extends ClavaNode {

    private final String namespace;

    public OriginalNamespace(String namespace, ClavaNodeInfo nodeInfo) {
        this(namespace, nodeInfo, Collections.emptyList());
    }

    private OriginalNamespace(String namespace, ClavaNodeInfo nodeInfo, Collection<? extends ClavaNode> children) {
        super(nodeInfo, children);

        this.namespace = namespace;
    }

    public String getNamespace() {
        return namespace;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new OriginalNamespace(namespace, getInfo(), Collections.emptyList());
    }

    @Override
    public String getCode() {
        throw new RuntimeException("Does not have code representation");
    }

}
