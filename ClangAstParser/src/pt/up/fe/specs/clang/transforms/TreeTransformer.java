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

package pt.up.fe.specs.clang.transforms;

import java.util.Collection;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaRule;

public class TreeTransformer {

    private final Collection<ClavaRule> clavaRules;

    public TreeTransformer(Collection<ClavaRule> clavaRules) {
        this.clavaRules = clavaRules;
    }

    public void transform(ClavaNode node) {
        // long tic = System.nanoTime();
        clavaRules.stream()
                .forEach(transform -> transform.visit(node));
        // ParseUtils.printTime("Clava AST Post-processing", tic);
    }
}
