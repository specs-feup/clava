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

package pt.up.fe.specs.clava.ast;

import java.util.Map;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.util.SpecsCheck;

public class ClavaDataPostProcessing {

    private final Map<String, ClavaNode> clavaNodes;

    public ClavaDataPostProcessing(Map<String, ClavaNode> clavaNodes) {
        this.clavaNodes = clavaNodes;
    }

    public ClavaNode getClavaNodes(String id) {
        ClavaNode clavaNode = clavaNodes.get(id);
        SpecsCheck.checkNotNull(clavaNode, () -> "No ClavaNode found for id '" + id + "'");

        return clavaNode;
    }

}
