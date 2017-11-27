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

package pt.up.fe.specs.clang.clavaparser.extra;

import java.util.List;
import java.util.Map;

import pt.up.fe.specs.clang.ast.ClangNode;

public class NormalizedClangNodes {

    private final List<ClangNode> uniqueNodes;
    private final Map<String, String> repeatedIdsMap;

    public NormalizedClangNodes(List<ClangNode> uniqueNodes, Map<String, String> repeatedIdsMap) {
        this.uniqueNodes = uniqueNodes;
        this.repeatedIdsMap = repeatedIdsMap;
    }

    public List<ClangNode> getUniqueNodes() {
        return uniqueNodes;
    }

    public Map<String, String> getRepeatedIdsMap() {
        return repeatedIdsMap;
    }

}
