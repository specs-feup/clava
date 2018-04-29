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

package pt.up.fe.specs.clang.streamparserv2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.up.fe.specs.clava.ClavaNode;

public class NormalizedNodes {

    private final List<ClavaNode> uniqueNodes;
    private final Map<String, String> repeatedIdsMap;

    public NormalizedNodes(List<ClavaNode> uniqueNodes, Map<String, String> repeatedIdsMap) {
        this.uniqueNodes = uniqueNodes;
        this.repeatedIdsMap = repeatedIdsMap;
    }

    public List<ClavaNode> getUniqueNodes() {
        return uniqueNodes;
    }

    public Map<String, String> getRepeatedIdsMap() {
        return repeatedIdsMap;
    }

    public static NormalizedNodes newInstance(Collection<? extends ClavaNode> nodes) {

        Map<String, String> fileLocationToId = new HashMap<>();

        List<ClavaNode> uniqueNodes = new ArrayList<>();
        Map<String, String> repeatedIdsMap = new HashMap<>();

        nodes.stream().forEach(node -> {
            // Convert node to a string based on location
            String locationString = node.getLocation().toString();

            String normalizedId = fileLocationToId.get(locationString);

            // If location is not mapped to a normalized node yet, add to map;
            if (normalizedId == null) {
                fileLocationToId.put(locationString, node.getId());
                uniqueNodes.add(node);
                return;
            }

            // Otherwise, map node to already normalized node in the table
            repeatedIdsMap.put(node.getData().getId(), normalizedId);
        });

        return new NormalizedNodes(uniqueNodes, repeatedIdsMap);
    }

    @Override
    public String toString() {
        return uniqueNodes.toString();
    }

}
