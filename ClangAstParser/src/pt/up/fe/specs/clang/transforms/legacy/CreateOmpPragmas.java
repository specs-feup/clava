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

package pt.up.fe.specs.clang.transforms.legacy;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.omp.OmpPragma;
import pt.up.fe.specs.clava.ast.pragma.Pragma;
import pt.up.fe.specs.clava.parsing.omp.OmpParser;
import pt.up.fe.specs.clava.transform.SimplePostClavaRule;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

public class CreateOmpPragmas implements SimplePostClavaRule {

    private final OmpParser ompParser;

    public CreateOmpPragmas() {
        ompParser = new OmpParser();
    }

    @Override
    public void applySimple(ClavaNode node, TransformQueue<ClavaNode> queue) {
        if (!(node instanceof Pragma)) {
            return;
        }

        Pragma pragma = (Pragma) node;

        if (!pragma.getName().equals("omp")) {
            return;
        }

        OmpPragma ompPragma = ompParser.parse(pragma);

        queue.replace(node, ompPragma);
    }

}
