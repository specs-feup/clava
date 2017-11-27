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

package pt.up.fe.specs.clava.transform.call;

import java.util.Set;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;

public class CallAnalysis {

    public CallAnalysis() {
        // TODO Auto-generated constructor stub
    }

    public static void checkNodes(CompoundStmt compoundStmt) {
        Set<String> nodesInBody = compoundStmt.getDescendants().stream()
                .map(node -> node.getClass().getSimpleName())
                .collect(Collectors.toSet());

        System.out.println("NODES IN BODY: " + nodesInBody);
    }

}
